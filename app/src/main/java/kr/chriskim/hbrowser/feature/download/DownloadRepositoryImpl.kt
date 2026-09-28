package kr.chriskim.hbrowser.feature.download

import android.content.Context
import android.os.Build
import kr.chriskim.hbrowser.core.database.AppDatabase
import kr.chriskim.hbrowser.core.database.DownloadItemEntity
import kr.chriskim.hbrowser.core.model.DownloadEngine
import kr.chriskim.hbrowser.core.model.DownloadStatus
import kr.chriskim.hbrowser.feature.download.manager.SystemDownloadManager
import kr.chriskim.hbrowser.feature.download.service.DownloadForegroundService
import kotlinx.coroutines.flow.Flow

/**
 * 다운로드 저장소 구현체
 * - FOREGROUND: ForegroundService 기반 (HTTP Range 이어받기, 진행률 실시간)
 * - SYSTEM: Android DownloadManager fallback
 */
class DownloadRepositoryImpl(
    private val ctx: Context,
    private val db: AppDatabase,
    private val sysDm: SystemDownloadManager
) : DownloadRepository {

    private val dao = db.downloadDao()

    override suspend fun startDownload(req: DownloadRequest): Long {
        val now = System.currentTimeMillis()
        val entity = DownloadItemEntity(
            url = req.url, pageUrl = req.pageUrl, fileName = req.fileName,
            mimeType = req.mimeType, contentLength = req.contentLength,
            status = DownloadStatus.QUEUED, engine = req.engine,
            isHls = req.isHls, wifiOnly = req.wifiOnly,
            userAgent = req.userAgent, referer = req.referer,
            createdAt = now, updatedAt = now
        )
        val id = dao.insert(entity)

        when (req.engine) {
            DownloadEngine.FOREGROUND -> startForegroundService(id, req)
            DownloadEngine.SYSTEM     -> startSystemDownload(id, req)
        }
        return id
    }

    private fun startForegroundService(id: Long, req: DownloadRequest) {
        val intent = DownloadForegroundService.buildStartIntent(
            ctx, id, req.url, req.fileName,
            req.userAgent, req.referer, req.cookies,
            req.wifiOnly, req.isHls
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            ctx.startForegroundService(intent)
        } else {
            ctx.startService(intent)
        }
    }

    private suspend fun startSystemDownload(id: Long, req: DownloadRequest) {
        val dmId = runCatching { sysDm.enqueue(req) }.getOrElse {
            dao.updateProgress(id, DownloadStatus.FAILED, it.message, null, null, 0, null, System.currentTimeMillis())
            return
        }
        dao.updateDmId(id, dmId, DownloadStatus.QUEUED, System.currentTimeMillis())
    }

    override fun observeDownloads(): Flow<List<DownloadItemEntity>> = dao.observeAll()

    override suspend fun syncStatus() {
        dao.getActive().forEach { item ->
            when (item.engine) {
                DownloadEngine.SYSTEM -> {
                    val dmId = item.downloadManagerId ?: return@forEach
                    val res  = sysDm.queryStatus(dmId) ?: return@forEach
                    val st   = DownloadStatus.fromDmStatus(res.dmStatus)
                    val ca   = if (st == DownloadStatus.SUCCESSFUL) System.currentTimeMillis() else null
                    dao.updateByDmId(dmId, st, res.reason.toString().takeIf { res.reason != 0 },
                        res.localUri, ca, System.currentTimeMillis())
                }
                DownloadEngine.FOREGROUND -> { /* ForegroundService가 직접 DB 업데이트 */ }
            }
        }
    }

    override suspend fun updateStatusByDmId(dmId: Long) {
        val res = sysDm.queryStatus(dmId) ?: return
        val st  = DownloadStatus.fromDmStatus(res.dmStatus)
        val ca  = if (st == DownloadStatus.SUCCESSFUL) System.currentTimeMillis() else null
        dao.updateByDmId(dmId, st, res.reason.toString().takeIf { res.reason != 0 },
            res.localUri, ca, System.currentTimeMillis())
    }

    override suspend fun deleteDownload(id: Long) {
        val item = dao.getById(id)
        item?.downloadManagerId?.let { sysDm.remove(it) }
        dao.delete(id)
    }

    override suspend fun getById(id: Long): DownloadItemEntity? = dao.getById(id)

    override suspend fun cancelDownload(id: Long) {
        val item = dao.getById(id) ?: return
        if (item.engine == DownloadEngine.FOREGROUND) {
            ctx.startService(
                android.content.Intent(ctx, DownloadForegroundService::class.java).apply {
                    action = DownloadForegroundService.ACTION_CANCEL
                    putExtra(DownloadForegroundService.EXTRA_ID, id)
                }
            )
        } else {
            item.downloadManagerId?.let { sysDm.remove(it) }
            dao.updateProgress(id, DownloadStatus.CANCELED, null, null, null, 0, null, System.currentTimeMillis())
        }
    }

    override suspend fun pauseDownload(id: Long) {
        val item = dao.getById(id) ?: return
        if (item.engine == DownloadEngine.FOREGROUND) {
            ctx.startService(
                android.content.Intent(ctx, DownloadForegroundService::class.java).apply {
                    action = DownloadForegroundService.ACTION_PAUSE
                    putExtra(DownloadForegroundService.EXTRA_ID, id)
                }
            )
        }
    }

    override suspend fun resumeDownload(id: Long) {
        val item = dao.getById(id) ?: return
        if (item.engine == DownloadEngine.FOREGROUND) {
            ctx.startService(DownloadForegroundService.buildStartIntent(
                ctx, id, item.url, item.fileName,
                item.userAgent, item.referer, null,
                item.wifiOnly, item.isHls
            ).apply { action = DownloadForegroundService.ACTION_RESUME })
        }
    }
}

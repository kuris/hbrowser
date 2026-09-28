package kr.chriskim.hbrowser.feature.download

import kr.chriskim.hbrowser.core.database.DownloadItemEntity
import kotlinx.coroutines.flow.Flow

/**
 * 다운로드 저장소 인터페이스
 * - ForegroundServiceDownloadRepository (기본)
 * - SystemDownloadRepository (DownloadManager fallback)
 */
interface DownloadRepository {
    suspend fun startDownload(req: DownloadRequest): Long
    fun observeDownloads(): Flow<List<DownloadItemEntity>>
    suspend fun syncStatus()
    suspend fun updateStatusByDmId(dmId: Long)
    suspend fun deleteDownload(id: Long)
    suspend fun getById(id: Long): DownloadItemEntity?
    suspend fun cancelDownload(id: Long)
    suspend fun pauseDownload(id: Long)
    suspend fun resumeDownload(id: Long)
}

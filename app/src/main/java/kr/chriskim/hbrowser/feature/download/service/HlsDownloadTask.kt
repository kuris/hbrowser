package kr.chriskim.hbrowser.feature.download.service

import android.content.Context
import android.os.Environment
import androidx.core.app.NotificationManagerCompat
import kr.chriskim.hbrowser.core.database.DownloadDao
import kr.chriskim.hbrowser.core.model.DownloadStatus
import kr.chriskim.hbrowser.core.util.FileNameUtils
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

/**
 * HLS (m3u8) 다운로드 태스크
 *
 * 동작 방식:
 * 1. m3u8 플레이리스트 파일 다운로드
 * 2. 세그먼트 URL 목록(.ts 파일) 파싱
 * 3. 각 세그먼트를 순서대로 다운로드하여 하나의 .ts 파일로 합침
 *
 * 제한:
 * - 암호화된 HLS (EXT-X-KEY) → 키 없이는 세그먼트가 암호화되어 있음
 *   → 암호화 세그먼트 감지 시 다운로드 중단하고 사용자에게 안내
 * - DRM 우회 없음
 */
class HlsDownloadTask(
    private val ctx: Context,
    private val id: Long,
    private val m3u8Url: String,
    private val fileName: String,
    private val userAgent: String?,
    private val cookies: String?,
    private val dao: DownloadDao,
    private val nm: NotificationManagerCompat,
    private val notifId: Int,
    private val wifiOnly: Boolean
) {
    suspend fun execute() {
        dao.updateProgress(id, DownloadStatus.RUNNING, null, null, null,
            0, null, System.currentTimeMillis())

        // 1. 플레이리스트 다운로드
        val playlist = fetchText(m3u8Url) ?: run {
            dao.updateProgress(id, DownloadStatus.FAILED, "m3u8 플레이리스트 로드 실패",
                null, null, 0, null, System.currentTimeMillis())
            return
        }

        // 2. 암호화 감지 (DRM/AES 보호)
        if (playlist.contains("#EXT-X-KEY")) {
            dao.updateProgress(id, DownloadStatus.FAILED,
                "암호화된 HLS 스트림 - 다운로드 불가 (DRM/AES 보호)",
                null, null, 0, null, System.currentTimeMillis())
            nm.notify(notifId, DownloadNotificationHelper.buildFailed(ctx, fileName,
                "암호화된 스트림은 다운로드할 수 없습니다"))
            return
        }

        // 3. 세그먼트 URL 파싱
        val baseUrl = m3u8Url.substringBeforeLast("/") + "/"
        val segments = playlist.lines()
            .filter { it.isNotBlank() && !it.startsWith("#") }
            .map { if (it.startsWith("http")) it else baseUrl + it }

        if (segments.isEmpty()) {
            dao.updateProgress(id, DownloadStatus.FAILED, "세그먼트 없음",
                null, null, 0, null, System.currentTimeMillis())
            return
        }

        // 4. 출력 파일 준비
        val baseName = FileNameUtils.sanitize(fileName.removeSuffix(".m3u8"))
        val outName  = "$baseName.ts"
        val destDir  = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        destDir.mkdirs()
        val outFile  = File(destDir, outName)

        // 5. 세그먼트 순차 다운로드 + 합치기
        FileOutputStream(outFile, false).use { out ->
            segments.forEachIndexed { idx, segUrl ->
                val pct = ((idx + 1) * 100) / segments.size
                nm.notify(notifId, DownloadNotificationHelper.buildProgress(ctx,
                    "$fileName ($pct%)", pct, id))
                dao.updateProgress(id, DownloadStatus.RUNNING, null, null,
                    outFile.absolutePath, outFile.length(), null, System.currentTimeMillis())

                val conn = (URL(segUrl).openConnection() as HttpURLConnection).apply {
                    requestMethod = "GET"; connectTimeout = 15_000; readTimeout = 30_000
                    userAgent?.let { setRequestProperty("User-Agent", it) }
                    cookies?.let  { setRequestProperty("Cookie", it) }
                }
                try {
                    conn.connect()
                    if (conn.responseCode == HttpURLConnection.HTTP_OK) {
                        conn.inputStream.use { it.copyTo(out, bufferSize = 8192) }
                    }
                } finally { conn.disconnect() }
            }
        }

        val fileUri = "file://${outFile.absolutePath}"
        val now = System.currentTimeMillis()
        dao.updateProgress(id, DownloadStatus.SUCCESSFUL, null, fileUri,
            outFile.absolutePath, outFile.length(), now, now)
        nm.notify(notifId, DownloadNotificationHelper.buildComplete(ctx, fileName, id))
    }

    private fun fetchText(url: String): String? = runCatching {
        val conn = (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"; connectTimeout = 10_000; readTimeout = 15_000
            userAgent?.let { setRequestProperty("User-Agent", it) }
            cookies?.let  { setRequestProperty("Cookie", it) }
        }
        conn.inputStream.bufferedReader().use { it.readText() }.also { conn.disconnect() }
    }.getOrNull()
}

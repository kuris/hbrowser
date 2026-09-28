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
 * 일반 HTTP(S) 파일 다운로드 태스크
 * - Range 헤더로 이어받기 지원 (localPath에 기존 파일 존재 시)
 * - 512ms 마다 진행률 DB 업데이트
 */
class HttpDownloadTask(
    private val ctx: Context,
    private val id: Long,
    private val url: String,
    private val fileName: String,
    private val userAgent: String?,
    private val referer: String?,
    private val cookies: String?,
    private val dao: DownloadDao,
    private val nm: NotificationManagerCompat,
    private val notifId: Int
) {
    suspend fun execute() {
        val safeFileName = FileNameUtils.sanitize(fileName)
        val destDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        destDir.mkdirs()
        val destFile = uniqueFile(destDir, safeFileName)

        // 기존 파일이 있으면 이어받기
        val startByte = if (destFile.exists()) destFile.length() else 0L

        dao.updateProgress(id, DownloadStatus.RUNNING, null, null, destFile.absolutePath,
            startByte, null, System.currentTimeMillis())

        val conn = (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 15_000
            readTimeout = 30_000
            userAgent?.let { setRequestProperty("User-Agent", it) }
            referer?.let  { setRequestProperty("Referer", it) }
            cookies?.let  { setRequestProperty("Cookie", it) }
            if (startByte > 0) setRequestProperty("Range", "bytes=$startByte-")
            setRequestProperty("Accept-Encoding", "identity")
        }

        try {
            conn.connect()
            val code = conn.responseCode
            if (code != HttpURLConnection.HTTP_OK && code != HttpURLConnection.HTTP_PARTIAL) {
                dao.updateProgress(id, DownloadStatus.FAILED, "HTTP $code", null, null,
                    0, null, System.currentTimeMillis())
                return
            }
            val totalLength = conn.contentLengthLong + startByte
            val append = (code == HttpURLConnection.HTTP_PARTIAL)

            conn.inputStream.use { input ->
                FileOutputStream(destFile, append).use { output ->
                    val buf = ByteArray(8192)
                    var downloaded = startByte
                    var lastNotif  = System.currentTimeMillis()
                    var read: Int
                    while (input.read(buf).also { read = it } != -1) {
                        output.write(buf, 0, read)
                        downloaded += read
                        val now = System.currentTimeMillis()
                        if (now - lastNotif > 512) {
                            lastNotif = now
                            val pct = if (totalLength > 0) ((downloaded * 100) / totalLength).toInt() else -1
                            dao.updateProgress(id, DownloadStatus.RUNNING, null, null,
                                destFile.absolutePath, downloaded, null, now)
                            nm.notify(notifId, DownloadNotificationHelper.buildProgress(ctx, fileName, pct, id))
                        }
                    }
                }
            }
            val fileUri = "file://${destFile.absolutePath}"
            val now = System.currentTimeMillis()
            dao.updateProgress(id, DownloadStatus.SUCCESSFUL, null, fileUri,
                destFile.absolutePath, destFile.length(), now, now)
            nm.notify(notifId, DownloadNotificationHelper.buildComplete(ctx, fileName, id))

        } finally {
            conn.disconnect()
        }
    }

    private fun uniqueFile(dir: File, name: String): File {
        var f = File(dir, name)
        if (!f.exists()) return f
        val dot = name.lastIndexOf('.')
        val base = if (dot > 0) name.substring(0, dot) else name
        val ext  = if (dot > 0) name.substring(dot)  else ""
        var i = 1
        while (f.exists()) { f = File(dir, "${base}_($i)${ext}"); i++ }
        return f
    }
}

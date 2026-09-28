package kr.chriskim.hbrowser.feature.download.manager

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Environment
import kr.chriskim.hbrowser.core.model.DownloadStatus
import kr.chriskim.hbrowser.core.util.FileNameUtils
import kr.chriskim.hbrowser.core.util.MimeUtils
import kr.chriskim.hbrowser.feature.download.DownloadRequest

/**
 * Android DownloadManager 래퍼 (fallback용)
 */
class SystemDownloadManager(private val ctx: Context) {
    private val dm = ctx.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager

    fun enqueue(req: DownloadRequest): Long {
        val name = FileNameUtils.sanitize(req.fileName)
        val mime = req.mimeType ?: MimeUtils.guess(name)
        val r = DownloadManager.Request(Uri.parse(req.url)).apply {
            setTitle(name)
            setDescription("HBrowser 다운로드")
            setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, name)
            setMimeType(mime)
            req.userAgent?.let { addRequestHeader("User-Agent", it) }
            req.referer?.let  { addRequestHeader("Referer", it) }
            req.cookies?.let  { addRequestHeader("Cookie", it) }
            if (req.wifiOnly) setAllowedNetworkTypes(DownloadManager.Request.NETWORK_WIFI)
            else { setAllowedOverMetered(true); setAllowedOverRoaming(true) }
        }
        return dm.enqueue(r)
    }

    fun queryStatus(dmId: Long): StatusResult? {
        val c = dm.query(DownloadManager.Query().setFilterById(dmId))
        return try {
            if (!c.moveToFirst()) return null
            StatusResult(
                dmStatus = c.getInt(c.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS)),
                reason   = c.getInt(c.getColumnIndexOrThrow(DownloadManager.COLUMN_REASON)),
                localUri = c.getString(c.getColumnIndexOrThrow(DownloadManager.COLUMN_LOCAL_URI))
            )
        } finally { c.close() }
    }

    fun remove(dmId: Long) = dm.remove(dmId)

    data class StatusResult(val dmStatus: Int, val reason: Int, val localUri: String?)
}

package kr.chriskim.hbrowser.core.database

import androidx.room.Entity
import androidx.room.PrimaryKey
import kr.chriskim.hbrowser.core.model.DownloadEngine
import kr.chriskim.hbrowser.core.model.DownloadStatus

@Entity(tableName = "download_items")
data class DownloadItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val downloadManagerId: Long? = null,   // Android DownloadManager ID
    val url: String,
    val finalUrl: String? = null,
    val pageUrl: String? = null,
    val fileName: String,
    val mimeType: String? = null,
    val contentLength: Long? = null,
    val downloadedBytes: Long = 0,         // ForegroundService 전용 진행 바이트
    val localUri: String? = null,
    val localPath: String? = null,         // ForegroundService 전용 경로
    val status: DownloadStatus = DownloadStatus.QUEUED,
    val reason: String? = null,
    val engine: DownloadEngine = DownloadEngine.SYSTEM,
    val isHls: Boolean = false,            // HLS 세그먼트 다운로드 여부
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val completedAt: Long? = null,
    val userAgent: String? = null,
    val referer: String? = null,
    val wifiOnly: Boolean = false
)

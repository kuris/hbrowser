package kr.chriskim.hbrowser.feature.download

import kr.chriskim.hbrowser.core.model.DownloadEngine

/** 다운로드 요청 모델 */
data class DownloadRequest(
    val url: String,
    val fileName: String,
    val mimeType: String?,
    val pageUrl: String?,
    val userAgent: String?,
    val referer: String?,
    val cookies: String?,
    val contentLength: Long?,
    val isHls: Boolean = false,
    val wifiOnly: Boolean = false,
    val engine: DownloadEngine = DownloadEngine.FOREGROUND  // 기본: ForegroundService
)

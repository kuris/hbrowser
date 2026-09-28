package kr.chriskim.hbrowser.core.model

/**
 * 다운로드 엔진 종류
 * MVP: SYSTEM(DownloadManager), FOREGROUND(ForegroundService)
 */
enum class DownloadEngine {
    SYSTEM,      // Android DownloadManager (기본)
    FOREGROUND   // 자체 ForegroundService 다운로더
}

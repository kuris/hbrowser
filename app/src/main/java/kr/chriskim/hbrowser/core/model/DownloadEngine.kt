package kr.chriskim.hbrowser.core.model

/**
 * 다운로드 엔진 종류
 * - SYSTEM: Android DownloadManager (기본)
 * - FOREGROUND: 자체 ForegroundService 다운로더 (HTTP Range 지원)
 * - YOUTUBE: yt-dlp 기반 YouTube 다운로드
 */
enum class DownloadEngine {
    SYSTEM,      // Android DownloadManager (기본)
    FOREGROUND,   // 자체 ForegroundService 다운로더
    YOUTUBE       // yt-dlp 기반 YouTube 다운로드
}

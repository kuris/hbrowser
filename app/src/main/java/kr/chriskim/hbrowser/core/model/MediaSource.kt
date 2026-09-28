package kr.chriskim.hbrowser.core.model

enum class MediaSource {
    RESOURCE_REQUEST,   // 네트워크 요청에서 감지
    VIDEO_TAG,          // video 태그에서 추출
    DOWNLOAD_LISTENER,  // WebView 다운로드 리스너
    HLS_MASTER_PLAYLIST // HLS 마스터 플레이리스트 URL
}

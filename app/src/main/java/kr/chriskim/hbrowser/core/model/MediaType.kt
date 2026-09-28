package kr.chriskim.hbrowser.core.model

enum class MediaType {
    VIDEO, AUDIO, IMAGE, HLS, OTHER, UNKNOWN;

    companion object {
        fun fromUrl(url: String): MediaType {
            val l = url.lowercase()
            return when {
                l.contains(".m3u8") -> HLS
                l.contains(".mp4") || l.contains(".webm") || l.contains(".mov") ||
                l.contains(".m4v") || l.contains(".avi") || l.contains(".mkv") -> VIDEO
                l.contains(".mp3") || l.contains(".m4a") || l.contains(".aac") ||
                l.contains(".ogg") || l.contains(".flac") || l.contains(".wav") -> AUDIO
                else -> UNKNOWN
            }
        }
        fun fromMime(m: String?): MediaType = when {
            m == null -> UNKNOWN
            m.startsWith("video/") -> VIDEO
            m.startsWith("audio/") -> AUDIO
            m.contains("mpegurl") || m.contains("x-mpegurl") -> HLS
            else -> UNKNOWN
        }
    }
}

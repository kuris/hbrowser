package kr.chriskim.hbrowser.core.model

data class MediaCandidate(
    val id: String,
    val url: String,
    val pageUrl: String,
    val type: MediaType,
    val fileName: String,
    val mimeType: String? = null,
    val detectedAt: Long = System.currentTimeMillis(),
    val source: MediaSource
) {
    companion object {
        fun fromUrl(url: String, pageUrl: String, source: MediaSource, mimeType: String? = null): MediaCandidate {
            val type = mimeType?.let { MediaType.fromMime(it).takeIf { t -> t != MediaType.UNKNOWN } }
                ?: MediaType.fromUrl(url)
            val fileName = url.substringBefore("?").substringBefore("#").substringAfterLast("/")
                .ifBlank { "media_${System.currentTimeMillis()}" }
            return MediaCandidate(
                id = (url + pageUrl).hashCode().toString(),
                url = url, pageUrl = pageUrl, type = type,
                fileName = fileName, mimeType = mimeType, source = source
            )
        }
    }
}

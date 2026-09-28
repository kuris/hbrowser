package kr.chriskim.hbrowser.feature.media

import kr.chriskim.hbrowser.core.model.MediaCandidate
import kr.chriskim.hbrowser.core.model.MediaSource
import kr.chriskim.hbrowser.core.model.MediaType

class MediaDetector(private val store: MediaCandidateStore) {

    private val mediaExts   = setOf(".mp4",".webm",".mov",".m4v",".mp3",".m4a",".aac",".ogg",".flac",".wav",".m3u8",".ts")
    private val mediaMimes  = setOf("video/","audio/","application/x-mpegurl","application/vnd.apple.mpegurl")
    private val excludePats = listOf("doubleclick","googlesyndication","google-analytics",
        "facebook.net/tr","tracking","beacon","pixel","analytics","metric","omtrdc","moatads")

    fun checkAndAdd(url: String, pageUrl: String, mimeType: String? = null) {
        if (url.isBlank() || isExcluded(url)) return
        if (!isMedia(url, mimeType)) return
        store.addCandidate(MediaCandidate.fromUrl(url, pageUrl, MediaSource.RESOURCE_REQUEST, mimeType))
    }

    fun isMedia(url: String, mime: String?): Boolean {
        if (mime != null) { val m = mime.lowercase(); if (mediaMimes.any { m.startsWith(it) }) return true }
        val lower = url.lowercase().substringBefore("?").substringBefore("#")
        if (mediaExts.any { lower.endsWith(it) }) return true
        if (url.lowercase().contains(".m3u8")) return true
        return false
    }

    private fun isExcluded(url: String): Boolean { val l = url.lowercase(); return excludePats.any { l.contains(it) } }
}

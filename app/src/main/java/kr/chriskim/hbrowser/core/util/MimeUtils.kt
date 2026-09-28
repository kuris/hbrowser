package kr.chriskim.hbrowser.core.util

object MimeUtils {
    fun guess(fileName: String): String = map[fileName.substringAfterLast('.', "").lowercase()] ?: "application/octet-stream"
    fun label(mime: String?): String = when {
        mime == null -> "파일"
        mime.startsWith("video/") -> "동영상"
        mime.startsWith("audio/") -> "오디오"
        mime.startsWith("image/") -> "이미지"
        mime == "application/pdf" -> "PDF"
        else -> "파일"
    }
    private val map = mapOf(
        "mp4" to "video/mp4", "webm" to "video/webm", "mov" to "video/quicktime",
        "m4v" to "video/x-m4v", "avi" to "video/x-msvideo", "mkv" to "video/x-matroska",
        "mp3" to "audio/mpeg", "m4a" to "audio/mp4", "aac" to "audio/aac",
        "ogg" to "audio/ogg", "flac" to "audio/flac", "wav" to "audio/wav",
        "m3u8" to "application/x-mpegURL", "ts" to "video/MP2T",
        "jpg" to "image/jpeg", "jpeg" to "image/jpeg", "png" to "image/png",
        "gif" to "image/gif", "webp" to "image/webp",
        "pdf" to "application/pdf", "zip" to "application/zip",
        "apk" to "application/vnd.android.package-archive"
    )
}

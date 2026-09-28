package kr.chriskim.hbrowser.core.util

import android.webkit.URLUtil

object FileNameUtils {
    fun guess(url: String, contentDisposition: String?, mimeType: String?): String =
        sanitize(runCatching { URLUtil.guessFileName(url, contentDisposition, mimeType) }.getOrElse { fromUrl(url) })

    fun fromUrl(url: String): String {
        val name = url.substringBefore("?").substringBefore("#").substringAfterLast("/")
        return sanitize(name.ifBlank { "download_${System.currentTimeMillis()}" })
    }

    fun sanitize(name: String): String {
        if (name.isBlank()) return "download_${System.currentTimeMillis()}"
        return name.replace(Regex("[\\\\/:*?\"<>|]"), "_").replace(Regex("\\s+"), "_").trim('.').take(200)
            .ifBlank { "download_${System.currentTimeMillis()}" }
    }

    fun ext(name: String): String { val i = name.lastIndexOf('.'); return if (i > 0) name.substring(i) else "" }
}

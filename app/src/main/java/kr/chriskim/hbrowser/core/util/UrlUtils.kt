package kr.chriskim.hbrowser.core.util

object UrlUtils {
    const val HOME_URL   = "https://www.google.com"
    const val SEARCH_URL = "https://www.google.com/search?q="

    fun processInput(input: String): String {
        val t = input.trim()
        if (t.isBlank()) return HOME_URL
        if (t.startsWith("http://") || t.startsWith("https://") || t.startsWith("file://")) return t
        if (looksLikeDomain(t)) return "https://$t"
        return SEARCH_URL + java.net.URLEncoder.encode(t, "UTF-8")
    }

    private fun looksLikeDomain(s: String): Boolean {
        if (s.contains(" ")) return false
        if (!s.contains(".")) return false
        return Regex("^[a-zA-Z0-9.-]+(/[^\\s]*)?$").matches(s)
    }

    fun displayUrl(url: String): String = url.removePrefix("https://").removePrefix("http://")

    fun extractHost(url: String): String? = try { java.net.URI(url).host?.lowercase() } catch (e: Exception) { null }
}

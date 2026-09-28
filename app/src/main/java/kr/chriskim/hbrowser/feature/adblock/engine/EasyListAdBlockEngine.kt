package kr.chriskim.hbrowser.feature.adblock.engine

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * EasyList/EasyPrivacy ABP 형식 파싱 광고차단 엔진
 *
 * assets/easylist.txt 와 assets/easyprivacy.txt 를 읽어 규칙 로드.
 * 파일이 없으면 내장 fallback 패턴으로 동작.
 *
 * 성능:
 *  - 규칙을 블랙/화이트리스트로 분리
 *  - 도메인 규칙은 Set<String>으로 O(1) 조회
 *  - 키워드/경로 규칙은 List 순회 (필요 시 Trie로 개선)
 */
class EasyListAdBlockEngine(private val context: Context) : AdBlockEngine {

    private val domainBlacklist  = mutableSetOf<String>()
    private val domainWhitelist  = mutableSetOf<String>()
    private val keywordBlacklist = mutableListOf<AbpRule>()
    private val keywordWhitelist = mutableListOf<AbpRule>()
    private val exceptions       = mutableSetOf<String>()  // 사이트별 예외 호스트

    @Volatile private var initialized = false
    @Volatile private var _ruleCount  = 0

    override fun ruleCount() = _ruleCount

    override suspend fun initialize() = withContext(Dispatchers.IO) {
        val lines = mutableListOf<String>()
        // assets 파일 로드 시도
        listOf("easylist.txt", "easyprivacy.txt").forEach { assetName ->
            runCatching {
                context.assets.open(assetName).bufferedReader().use { lines.addAll(it.readLines()) }
            }
        }
        // assets 파일이 없으면 내장 패턴 사용
        if (lines.isEmpty()) lines.addAll(BUILT_IN_PATTERNS)

        parseRules(lines)
        initialized = true
    }

    private fun parseRules(lines: List<String>) {
        domainBlacklist.clear(); domainWhitelist.clear()
        keywordBlacklist.clear(); keywordWhitelist.clear()
        var count = 0
        for (line in lines) {
            val rule = AbpRule.parse(line) ?: continue
            count++
            when {
                rule.isWhitelist && rule.isDomain  -> domainWhitelist.add(rule.pattern)
                rule.isWhitelist                   -> keywordWhitelist.add(rule)
                rule.isDomain                      -> domainBlacklist.add(rule.pattern)
                else                               -> keywordBlacklist.add(rule)
            }
        }
        _ruleCount = count
    }

    override fun shouldBlock(url: String, pageUrl: String?): Boolean {
        if (url.isBlank()) return false

        // 사이트별 예외 호스트
        val reqHost = extractHost(url)
        if (reqHost != null && exceptions.contains(reqHost)) return false

        val lower = url.lowercase()

        // 화이트리스트 우선 확인
        if (reqHost != null && domainWhitelist.any { lower.contains(it) }) return false
        if (keywordWhitelist.any { it.matches(lower) }) return false

        // 블랙리스트 확인
        if (reqHost != null && domainBlacklist.any { lower.contains(it) }) return true
        return keywordBlacklist.any { it.matches(lower) }
    }

    override fun addException(host: String)  { exceptions.add(host.lowercase()) }
    override fun removeException(host: String) { exceptions.remove(host.lowercase()) }
    override fun setExceptions(hosts: Set<String>) { exceptions.clear(); exceptions.addAll(hosts.map { it.lowercase() }) }

    private fun extractHost(url: String): String? =
        runCatching { java.net.URI(url).host?.lowercase() }.getOrNull()

    // ── 내장 Fallback 패턴 (assets 없을 때) ──────────────────
    companion object {
        val BUILT_IN_PATTERNS = listOf(
            "||doubleclick.net^", "||googlesyndication.com^", "||google-analytics.com^",
            "||googletagmanager.com^", "||googletagservices.com^", "||adservice.google.com^",
            "||adservice.google.co.jp^", "||facebook.net/en_US/fbevents.js^",
            "||ads.yahoo.com^", "||adnxs.com^", "||adsrvr.org^", "||advertising.com^",
            "||adbrite.com^", "||adform.net^", "||criteo.com^", "||quantserve.com^",
            "||scorecardresearch.com^", "||omtrdc.net^", "||2mdn.net^", "||moatads.com^",
            "||amazon-adsystem.com^", "||media.net^", "||taboola.com^", "||outbrain.com^",
            "||revcontent.com^", "||mgid.com^", "||popads.net^", "||popcash.net^",
            "||popunder.net^", "||rubiconproject.com^", "||openx.net^", "||pubmatic.com^",
            "||appnexus.com^", "||33across.com^", "||bidswitch.net^", "||casalemedia.com^",
            "/ads/", "/adserver/", "/adserve/", "adsystem", "/banner", "/banners",
            "/tracking", "/tracker", "/analytics", "/telemetry", "/beacon",
            "/pixel.", "sponsored", "promo."
        )
    }
}

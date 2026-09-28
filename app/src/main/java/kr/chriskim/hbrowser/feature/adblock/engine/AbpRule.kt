package kr.chriskim.hbrowser.feature.adblock.engine

/**
 * ABP(Adblock Plus) 형식 규칙 파싱 결과
 *
 * 지원 형식:
 *   - 도메인 기반:  ||doubleclick.net^
 *   - 키워드 기반:  /ads/
 *   - 화이트리스트: @@||example.com^
 *   - 주석:        ! ...
 *   - 옵션 제외:   규칙에 $ 이후 옵션은 무시 (MVP)
 */
data class AbpRule(
    val pattern: String,   // 실제 비교할 패턴 문자열
    val isWhitelist: Boolean,
    val isDomain: Boolean, // ||domain^ 형식
    val isRegex: Boolean
) {
    /** 컴파일된 Regex (isRegex == true일 때만 비null) */
    val regex: Regex? = if (isRegex) runCatching { Regex(pattern, RegexOption.IGNORE_CASE) }.getOrNull() else null

    fun matches(url: String): Boolean = when {
        isRegex && regex != null -> regex.containsMatchIn(url)
        isDomain -> url.contains(pattern)
        else     -> url.contains(pattern)
    }

    companion object {
        /**
         * 한 줄을 파싱하여 AbpRule 반환.
         * 파싱 불가 / 무시할 줄이면 null.
         */
        fun parse(line: String): AbpRule? {
            val t = line.trim()
            if (t.isBlank() || t.startsWith("!") || t.startsWith("[")) return null

            val isWhitelist = t.startsWith("@@")
            val body = if (isWhitelist) t.removePrefix("@@") else t

            // 옵션($) 이후 제거
            val noOpt = body.substringBefore("$")

            // /regex/ 형식
            if (noOpt.startsWith("/") && noOpt.endsWith("/") && noOpt.length > 2) {
                val pattern = noOpt.drop(1).dropLast(1)
                return AbpRule(pattern, isWhitelist, isDomain = false, isRegex = true)
            }

            // ||domain^ 형식
            if (noOpt.startsWith("||")) {
                val domain = noOpt.removePrefix("||").trimEnd('^', '/')
                if (domain.isBlank()) return null
                return AbpRule(domain, isWhitelist, isDomain = true, isRegex = false)
            }

            // 단순 키워드 / 경로 패턴
            val clean = noOpt.trimStart('|').trimEnd('^')
            if (clean.isBlank() || clean.length < 3) return null
            return AbpRule(clean, isWhitelist, isDomain = false, isRegex = false)
        }
    }
}

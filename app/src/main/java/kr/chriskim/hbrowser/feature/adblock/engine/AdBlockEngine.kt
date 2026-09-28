package kr.chriskim.hbrowser.feature.adblock.engine

/**
 * 광고차단 엔진 인터페이스
 * SimpleAdBlockEngine (내장 패턴) 또는 EasyListAdBlockEngine (ABP 파일) 교체 가능
 */
interface AdBlockEngine {
    /** 해당 URL을 차단해야 하면 true */
    fun shouldBlock(url: String, pageUrl: String? = null): Boolean

    /** 사이트별 예외 호스트 추가 */
    fun addException(host: String)

    /** 사이트별 예외 호스트 제거 */
    fun removeException(host: String)

    /** 예외 목록 일괄 교체 (DB 로드 시 사용) */
    fun setExceptions(hosts: Set<String>)

    /** 필터 목록 초기화 (assets 로드 등) */
    suspend fun initialize()

    /** 현재 로드된 규칙 수 */
    fun ruleCount(): Int
}

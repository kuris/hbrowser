package kr.chriskim.hbrowser.feature.adblock

import kr.chriskim.hbrowser.core.database.AdBlockRuleDao
import kr.chriskim.hbrowser.core.database.AdBlockRuleEntity
import kr.chriskim.hbrowser.feature.adblock.engine.AdBlockEngine
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * 광고차단 규칙 저장소
 * Room DB ↔ AdBlockEngine 사이트별 예외 목록을 동기화
 */
class AdBlockRepository(
    private val dao: AdBlockRuleDao,
    private val engine: AdBlockEngine
) {
    /** 예외 목록 Flow */
    fun observeExceptions(): Flow<List<AdBlockRuleEntity>> =
        dao.observeAll().map { it.filter { r -> r.isException } }

    /** DB에서 예외 목록 로드 후 엔진에 반영 */
    suspend fun loadExceptionsToEngine() {
        val hosts = dao.getAll().filter { it.isException }.map { it.host }.toSet()
        engine.setExceptions(hosts)
    }

    /** 현재 페이지 호스트를 광고차단 예외로 추가 */
    suspend fun addException(host: String) {
        dao.insert(AdBlockRuleEntity(host = host, isException = true))
        engine.addException(host)
    }

    /** 예외 제거 */
    suspend fun removeException(host: String) {
        dao.deleteByHost(host)
        engine.removeException(host)
    }

    /** 특정 호스트가 예외인지 확인 */
    suspend fun isException(host: String): Boolean = dao.getByHost(host)?.isException == true
}

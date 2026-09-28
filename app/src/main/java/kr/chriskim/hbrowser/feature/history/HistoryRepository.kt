package kr.chriskim.hbrowser.feature.history

import kr.chriskim.hbrowser.core.database.AppDatabase
import kr.chriskim.hbrowser.core.database.HistoryEntity
import kotlinx.coroutines.flow.Flow

class HistoryRepository(db: AppDatabase) {
    private val dao = db.historyDao()

    fun observeAll(): Flow<List<HistoryEntity>> = dao.observeAll()
    suspend fun add(url: String, title: String)  = dao.insert(HistoryEntity(url = url, title = title))
    suspend fun search(q: String)                = dao.search(q)
    suspend fun delete(id: Long)                 = dao.delete(id)
    suspend fun clearAll()                       = dao.clearAll()
}

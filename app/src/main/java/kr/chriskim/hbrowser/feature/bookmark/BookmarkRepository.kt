package kr.chriskim.hbrowser.feature.bookmark

import kr.chriskim.hbrowser.core.database.AppDatabase
import kr.chriskim.hbrowser.core.database.BookmarkEntity
import kotlinx.coroutines.flow.Flow

class BookmarkRepository(db: AppDatabase) {
    private val dao = db.bookmarkDao()

    fun observeAll(): Flow<List<BookmarkEntity>> = dao.observeAll()
    suspend fun add(url: String, title: String)  = dao.insert(BookmarkEntity(url = url, title = title))
    suspend fun isBookmarked(url: String)         = dao.getByUrl(url) != null
    suspend fun remove(id: Long)                  = dao.delete(id)
    suspend fun removeByUrl(url: String)          = dao.deleteByUrl(url)
    suspend fun toggle(url: String, title: String): Boolean {
        return if (isBookmarked(url)) { removeByUrl(url); false }
        else { add(url, title); true }
    }
}

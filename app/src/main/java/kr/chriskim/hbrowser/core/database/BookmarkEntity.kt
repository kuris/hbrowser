package kr.chriskim.hbrowser.core.database

import androidx.room.Entity
import androidx.room.PrimaryKey

/** 북마크 엔티티 */
@Entity(tableName = "bookmarks")
data class BookmarkEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val url: String,
    val title: String,
    val faviconUrl: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)

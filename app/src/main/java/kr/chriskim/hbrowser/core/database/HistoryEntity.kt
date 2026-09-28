package kr.chriskim.hbrowser.core.database

import androidx.room.Entity
import androidx.room.PrimaryKey

/** 방문 기록 엔티티 */
@Entity(tableName = "history")
data class HistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val url: String,
    val title: String,
    val faviconUrl: String? = null,
    val visitedAt: Long = System.currentTimeMillis()
)

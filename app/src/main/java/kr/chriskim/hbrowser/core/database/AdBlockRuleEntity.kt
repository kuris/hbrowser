package kr.chriskim.hbrowser.core.database

import androidx.room.Entity
import androidx.room.PrimaryKey

/** 사이트별 광고차단 예외 / 추가 규칙 엔티티 */
@Entity(tableName = "adblock_rules")
data class AdBlockRuleEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val host: String,          // 대상 호스트 (예: "example.com")
    val isException: Boolean,  // true = 이 사이트는 광고차단 제외
    val createdAt: Long = System.currentTimeMillis()
)

package kr.chriskim.hbrowser.core.database

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface AdBlockRuleDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(rule: AdBlockRuleEntity): Long

    @Query("SELECT * FROM adblock_rules")
    fun observeAll(): Flow<List<AdBlockRuleEntity>>

    @Query("SELECT * FROM adblock_rules")
    suspend fun getAll(): List<AdBlockRuleEntity>

    @Query("SELECT * FROM adblock_rules WHERE host = :host LIMIT 1")
    suspend fun getByHost(host: String): AdBlockRuleEntity?

    @Query("DELETE FROM adblock_rules WHERE host = :host")
    suspend fun deleteByHost(host: String)

    @Query("DELETE FROM adblock_rules WHERE id = :id")
    suspend fun delete(id: Long)
}

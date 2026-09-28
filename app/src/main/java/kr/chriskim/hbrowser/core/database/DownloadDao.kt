package kr.chriskim.hbrowser.core.database

import androidx.room.*
import kr.chriskim.hbrowser.core.model.DownloadStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface DownloadDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: DownloadItemEntity): Long

    @Update
    suspend fun update(item: DownloadItemEntity)

    @Query("SELECT * FROM download_items ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<DownloadItemEntity>>

    @Query("SELECT * FROM download_items ORDER BY createdAt DESC")
    suspend fun getAll(): List<DownloadItemEntity>

    @Query("SELECT * FROM download_items WHERE id = :id")
    suspend fun getById(id: Long): DownloadItemEntity?

    @Query("SELECT * FROM download_items WHERE downloadManagerId = :dmId LIMIT 1")
    suspend fun getByDmId(dmId: Long): DownloadItemEntity?

    @Query("SELECT * FROM download_items WHERE status IN ('QUEUED','RUNNING','PAUSED')")
    suspend fun getActive(): List<DownloadItemEntity>

    @Query("DELETE FROM download_items WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("""UPDATE download_items SET downloadManagerId=:dmId, status=:s, updatedAt=:t WHERE id=:id""")
    suspend fun updateDmId(id: Long, dmId: Long, s: DownloadStatus, t: Long)

    @Query("""UPDATE download_items SET status=:s, reason=:r, localUri=:uri, localPath=:path,
              downloadedBytes=:bytes, completedAt=:ca, updatedAt=:t WHERE id=:id""")
    suspend fun updateProgress(id: Long, s: DownloadStatus, r: String?, uri: String?,
                               path: String?, bytes: Long, ca: Long?, t: Long)

    @Query("""UPDATE download_items SET status=:s, reason=:r, localUri=:uri,
              completedAt=:ca, updatedAt=:t WHERE downloadManagerId=:dmId""")
    suspend fun updateByDmId(dmId: Long, s: DownloadStatus, r: String?, uri: String?, ca: Long?, t: Long)
}

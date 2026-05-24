package com.example.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.model.ScanHistory
import kotlinx.coroutines.flow.Flow

@Dao
interface ScanHistoryDao {
    @Query("SELECT * FROM scan_history ORDER BY timestamp DESC")
    fun getAllHistoryFlow(): Flow<List<ScanHistory>>

    @Query("SELECT * FROM scan_history WHERE id = :id LIMIT 1")
    suspend fun getScanHistoryById(id: Long): ScanHistory?

    @Query("SELECT * FROM scan_history WHERE hashSha256 = :hash LIMIT 1")
    suspend fun getScanHistoryByHash(hash: String): ScanHistory?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertScanHistory(record: ScanHistory): Long

    @Query("DELETE FROM scan_history WHERE id = :id")
    suspend fun deleteScanHistoryId(id: Long)

    @Query("DELETE FROM scan_history")
    suspend fun clearAllHistory()
}

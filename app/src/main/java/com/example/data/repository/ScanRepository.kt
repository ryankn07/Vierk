package com.example.data.repository

import com.example.data.dao.ScanHistoryDao
import com.example.data.model.ScanHistory
import kotlinx.coroutines.flow.Flow

class ScanRepository(private val scanHistoryDao: ScanHistoryDao) {
    val allHistoryFlow: Flow<List<ScanHistory>> = scanHistoryDao.getAllHistoryFlow()

    suspend fun getScanHistoryById(id: Long): ScanHistory? {
        return scanHistoryDao.getScanHistoryById(id)
    }

    suspend fun getScanHistoryByHash(hash: String): ScanHistory? {
        return scanHistoryDao.getScanHistoryByHash(hash)
    }

    suspend fun insertScanHistory(record: ScanHistory): Long {
        return scanHistoryDao.insertScanHistory(record)
    }

    suspend fun deleteScanHistoryId(id: Long) {
        scanHistoryDao.deleteScanHistoryId(id)
    }

    suspend fun clearAllHistory() {
        scanHistoryDao.clearAllHistory()
    }
}

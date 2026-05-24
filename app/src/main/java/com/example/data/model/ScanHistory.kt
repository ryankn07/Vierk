package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "scan_history")
data class ScanHistory(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val fileName: String,
    val filePath: String,
    val realFileType: String,
    val declaredExtension: String,
    val hashSha256: String,
    val fileSize: Long,
    val riskLevel: String, // "Safe", "Suspicious", "High Risk", "Unknown"
    val riskReasons: String, // Newline separated risk explanation messages
    val timestamp: Long = System.currentTimeMillis(),
    val sourceLastModified: Long = 0L,
    val packageId: String? = null,
    val permissions: String? = null, // Comma-separated list of permissions (APKs Only)
    val extractedUrls: String? = null // Comma-separated list of URLs
)

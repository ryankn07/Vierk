package com.example.util

import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.provider.DocumentsContract
import android.util.Log
import com.example.data.analyzer.FileAnalyzer
import com.example.data.model.ScanHistory
import com.example.data.preferences.SettingsManager
import com.example.data.repository.ScanRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

object FolderMonitor {
    private const val TAG = "FolderMonitor"

    data class DocumentInfo(
        val docId: String,
        val displayName: String,
        val mimeType: String,
        val lastModified: Long,
        val size: Long,
        val docUri: Uri
    )

    suspend fun scanFolderForNewFiles(
        context: Context,
        treeUriString: String,
        settingsManager: SettingsManager,
        repository: ScanRepository,
        onFoundNewFile: (String, String) -> Unit = { _, _ -> }
    ): Int = withContext(Dispatchers.IO) {
        if (treeUriString.isEmpty()) return@withContext 0

        val treeUri = Uri.parse(treeUriString)
        val files = listFiles(context, treeUri)
        if (files.isEmpty()) return@withContext 0

        Log.d(TAG, "Found ${files.size} raw files in SAF folder")

        // Read all existing records to determine what to skip
        val allExisting = repository.allHistoryFlow.first()
        val scannedMap = allExisting.associateBy { it.fileName + "_" + it.fileSize }

        var newScansCount = 0

        for (file in files) {
            val nameLower = file.displayName.lowercase()
            val ext = file.displayName.substringAfterLast('.', "").lowercase()

            // Filter enabled extensions
            val isEnabled = when {
                ext == "apk" || ext == "xapk" || ext == "apks" -> settingsManager.scanApk
                ext == "zip" -> settingsManager.scanZip
                ext == "pdf" -> settingsManager.scanPdf
                ext == "docx" || ext == "xlsx" || ext == "pptx" -> settingsManager.scanDoc
                ext == "html" || ext == "htm" || ext == "js" -> settingsManager.scanHtmlJs
                ext == "txt" || ext == "log" || ext == "json" -> settingsManager.scanTxt
                else -> false // Skip unsupported extensions
            }

            if (!isEnabled) {
                continue
            }

            // Check if file is already scanned (filename + size match fits SAF very well)
            val identifier = file.displayName + "_" + file.size
            if (scannedMap.containsKey(identifier)) {
                // Already scanned, skip and do not waste operations
                continue
            }

            Log.d(TAG, "New or updated file found: ${file.displayName}, Scanning...")
            onFoundNewFile(file.displayName, "Analyzing details...")

            // Scan using our core engine
            val result = FileAnalyzer.scanUri(context, file.docUri, file.displayName)

            // Convert and Insert history record
            val entity = ScanHistory(
                fileName = result.fileName,
                filePath = result.filePath,
                realFileType = result.realFileType,
                declaredExtension = result.declaredExtension,
                hashSha256 = result.hashSha256,
                fileSize = result.fileSize,
                riskLevel = result.riskLevel,
                riskReasons = result.riskReasons.joinToString("\n"),
                timestamp = file.lastModified.coerceAtLeast(System.currentTimeMillis()),
                packageId = result.packageId,
                permissions = result.permissions.joinToString(","),
                extractedUrls = result.extractedUrls.joinToString(",")
            )

            repository.insertScanHistory(entity)
            newScansCount++

            // Dispatch alert notifying the user if threat is flagged
            if (settingsManager.isNotificationsEnabled && 
                (result.riskLevel == "High Risk" || result.riskLevel == "Suspicious")
            ) {
                val primaryReason = result.riskReasons.firstOrNull() ?: "Potential security indicators flagged."
                NotificationHelper.showRiskAlert(
                    context = context,
                    fileName = file.displayName,
                    riskLevel = result.riskLevel,
                    reason = primaryReason
                )
            }
        }

        return@withContext newScansCount
    }

    private fun listFiles(context: Context, treeUri: Uri): List<DocumentInfo> {
        val fileList = mutableListOf<DocumentInfo>()
        val childrenUri = try {
            DocumentsContract.buildChildDocumentsUriUsingTree(
                treeUri,
                DocumentsContract.getTreeDocumentId(treeUri)
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed resolving DocumentId from treeUri: $treeUri", e)
            return emptyList()
        }

        val projection = arrayOf(
            DocumentsContract.Document.COLUMN_DOCUMENT_ID,
            DocumentsContract.Document.COLUMN_DISPLAY_NAME,
            DocumentsContract.Document.COLUMN_MIME_TYPE,
            DocumentsContract.Document.COLUMN_LAST_MODIFIED,
            DocumentsContract.Document.COLUMN_SIZE
        )

        var cursor: Cursor? = null
        try {
            cursor = context.contentResolver.query(childrenUri, projection, null, null, null)
            if (cursor != null) {
                val idxId = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DOCUMENT_ID)
                val idxName = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
                val idxMime = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_MIME_TYPE)
                val idxModified = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_LAST_MODIFIED)
                val idxSize = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_SIZE)

                while (cursor.moveToNext()) {
                    val docId = cursor.getString(idxId)
                    val displayName = cursor.getString(idxName) ?: "unknown_file"
                    val mimeType = cursor.getString(idxMime) ?: ""
                    val lastModified = cursor.getLong(idxModified)
                    val size = cursor.getLong(idxSize)

                    if (mimeType != DocumentsContract.Document.MIME_TYPE_DIR) {
                        val docUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, docId)
                        fileList.add(
                            DocumentInfo(
                                docId = docId,
                                displayName = displayName,
                                mimeType = mimeType,
                                lastModified = lastModified,
                                size = size,
                                docUri = docUri
                            )
                        )
                    }
                }
            }
        } catch (e: SecurityException) {
            Log.e(TAG, "Security exception listing folder. Permission may have been revoked.", e)
        } catch (e: Exception) {
            Log.e(TAG, "Error listing children for tree URI $treeUri", e)
        } finally {
            cursor?.close()
        }
        return fileList
    }
}

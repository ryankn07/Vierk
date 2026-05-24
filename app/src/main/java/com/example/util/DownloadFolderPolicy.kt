package com.example.util

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract

object DownloadFolderPolicy {
    fun isDownloadsTree(context: Context, uri: Uri): Boolean {
        val treeDocumentId = try {
            DocumentsContract.getTreeDocumentId(uri)
        } catch (_: Exception) {
            return false
        }

        if (treeDocumentId.equals("primary:Download", ignoreCase = true) ||
            treeDocumentId.equals("primary:Downloads", ignoreCase = true) ||
            treeDocumentId.endsWith(":Download", ignoreCase = true) ||
            treeDocumentId.endsWith(":Downloads", ignoreCase = true)
        ) {
            return true
        }

        val folderName = resolveFolderName(context, uri)?.trim()
        return folderName.equals("Download", ignoreCase = true) ||
            folderName.equals("Downloads", ignoreCase = true)
    }

    fun resolveFolderName(context: Context, uri: Uri): String? {
        val documentUri = try {
            DocumentsContract.buildDocumentUriUsingTree(
                uri,
                DocumentsContract.getTreeDocumentId(uri)
            )
        } catch (_: Exception) {
            return null
        }

        var cursor: android.database.Cursor? = null
        return try {
            cursor = context.contentResolver.query(
                documentUri,
                arrayOf(DocumentsContract.Document.COLUMN_DISPLAY_NAME),
                null,
                null,
                null
            )
            if (cursor != null && cursor.moveToFirst()) {
                cursor.getString(0)
            } else {
                null
            }
        } catch (_: Exception) {
            null
        } finally {
            cursor?.close()
        }
    }
}

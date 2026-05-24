package com.example.util

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.data.database.AppDatabase
import com.example.data.preferences.SettingsManager
import com.example.data.repository.ScanRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class DownloadScanReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val pendingResult = goAsync()
        val appContext = context.applicationContext

        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                val settings = SettingsManager(appContext)
                if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
                    if (settings.isMonitoringEnabled && !settings.grantedFolderUri.isNullOrBlank()) {
                        DownloadScanScheduler.schedule(appContext)
                    }
                    return@launch
                }

                if (!settings.isMonitoringEnabled || settings.grantedFolderUri.isNullOrBlank()) {
                    return@launch
                }

                val repository = ScanRepository(AppDatabase.getDatabase(appContext).scanHistoryDao())
                FolderMonitor.scanFolderForNewFiles(
                    context = appContext,
                    treeUriString = settings.grantedFolderUri.orEmpty(),
                    settingsManager = settings,
                    repository = repository
                )
            } catch (e: Exception) {
                Log.e("DownloadScanReceiver", "Scheduled Downloads scan failed", e)
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        const val ACTION_SCAN_DOWNLOADS = "com.example.DOWNLOAD_SCAN"
    }
}

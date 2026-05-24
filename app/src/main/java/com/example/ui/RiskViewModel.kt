package com.example.ui

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.model.ScanHistory
import com.example.data.preferences.SettingsManager
import com.example.data.repository.ScanRepository
import com.example.util.DownloadFolderPolicy
import com.example.util.DownloadScanScheduler
import com.example.util.FolderMonitor
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface Screen {
    object Home : Screen
    data class Detail(val scanId: Long) : Screen
    object Settings : Screen
}

class RiskViewModel(
    private val repository: ScanRepository,
    private val settingsManager: SettingsManager,
    private val appContext: Context
) : ViewModel() {

    private val TAG = "RiskViewModel"
    init {
        val savedUri = settingsManager.grantedFolderUri
        if (!savedUri.isNullOrBlank() &&
            !DownloadFolderPolicy.isDownloadsTree(appContext, Uri.parse(savedUri))
        ) {
            settingsManager.grantedFolderUri = null
            settingsManager.grantedFolderName = null
            DownloadScanScheduler.cancel(appContext)
        } else if (settingsManager.isMonitoringEnabled && !savedUri.isNullOrBlank()) {
            DownloadScanScheduler.schedule(appContext)
        }
    }

    // Backed by Flow for reactive UI state
    val scanHistoryState: StateFlow<List<ScanHistory>> = repository.allHistoryFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Navigation state
    var currentScreen by mutableStateOf<Screen>(Screen.Home)
        private set

    // Scanning states for visual feedback
    var isScanning by mutableStateOf(false)
        private set

    var scanStatusMessage by mutableStateOf("")
        private set

    // Preference bindings as reactive states
    var grantedFolderUri by mutableStateOf(settingsManager.grantedFolderUri ?: "")
        private set

    var grantedFolderName by mutableStateOf(settingsManager.grantedFolderName ?: "Not Selected")
        private set

    var isMonitoringEnabled by mutableStateOf(settingsManager.isMonitoringEnabled)
        private set

    var isNotificationsEnabled by mutableStateOf(settingsManager.isNotificationsEnabled)
        private set

    var scanApk by mutableStateOf(settingsManager.scanApk)
        private set

    var scanZip by mutableStateOf(settingsManager.scanZip)
        private set

    var scanPdf by mutableStateOf(settingsManager.scanPdf)
        private set

    var scanDoc by mutableStateOf(settingsManager.scanDoc)
        private set

    var scanHtmlJs by mutableStateOf(settingsManager.scanHtmlJs)
        private set

    var scanTxt by mutableStateOf(settingsManager.scanTxt)
        private set

    fun navigateTo(screen: Screen) {
        currentScreen = screen
    }

    fun updateGrantedFolder(uri: String, name: String) {
        settingsManager.grantedFolderUri = uri
        settingsManager.grantedFolderName = name
        grantedFolderUri = uri
        grantedFolderName = name
        DownloadScanScheduler.schedule(appContext)
        
        // Trigger an automatic introductory scan of the newly added folder!
        triggerFolderScan()
    }

    fun rejectFolderSelection(message: String) {
        scanStatusMessage = message
    }

    fun toggleMonitoring(enabled: Boolean) {
        settingsManager.isMonitoringEnabled = enabled
        isMonitoringEnabled = enabled
        if (enabled && grantedFolderUri.isNotEmpty()) {
            DownloadScanScheduler.schedule(appContext)
        } else {
            DownloadScanScheduler.cancel(appContext)
        }
    }

    fun toggleNotifications(enabled: Boolean) {
        settingsManager.isNotificationsEnabled = enabled
        isNotificationsEnabled = enabled
    }

    fun toggleScanApk(enabled: Boolean) {
        settingsManager.scanApk = enabled
        scanApk = enabled
    }

    fun toggleScanZip(enabled: Boolean) {
        settingsManager.scanZip = enabled
        scanZip = enabled
    }

    fun toggleScanPdf(enabled: Boolean) {
        settingsManager.scanPdf = enabled
        scanPdf = enabled
    }

    fun toggleScanDoc(enabled: Boolean) {
        settingsManager.scanDoc = enabled
        scanDoc = enabled
    }

    fun toggleScanHtmlJs(enabled: Boolean) {
        settingsManager.scanHtmlJs = enabled
        scanHtmlJs = enabled
    }

    fun toggleScanTxt(enabled: Boolean) {
        settingsManager.scanTxt = enabled
        scanTxt = enabled
    }

    fun triggerFolderScan() {
        if (grantedFolderUri.isEmpty()) {
            scanStatusMessage = "No Downloads folder selected. Grant folder access first."
            return
        }

        if (!isMonitoringEnabled) {
            scanStatusMessage = "Monitoring and manual scans are turned off in settings."
            return
        }

        viewModelScope.launch {
            try {
                isScanning = true
                scanStatusMessage = "Scanning Downloads folder..."
                Log.d(TAG, "Starting folder scan for $grantedFolderUri")
                
                val resultsCount = FolderMonitor.scanFolderForNewFiles(
                    context = appContext,
                    treeUriString = grantedFolderUri,
                    settingsManager = settingsManager,
                    repository = repository,
                    onFoundNewFile = { name, status ->
                        scanStatusMessage = "Analyzing: $name\n$status"
                    }
                )

                scanStatusMessage = if (resultsCount > 0) {
                    "Scan complete: analyzed $resultsCount new or modified files."
                } else {
                    "Scan complete: No new files identified."
                }
            } catch (e: Exception) {
                Log.e(TAG, "Folder scan failed", e)
                scanStatusMessage = "Scan failed: ${e.localizedMessage}"
            } finally {
                isScanning = false
            }
        }
    }

    fun clearHistory() {
        viewModelScope.launch {
            repository.clearAllHistory()
            scanStatusMessage = "History cleared successfully."
        }
    }

    fun deleteScan(id: Long) {
        viewModelScope.launch {
            repository.deleteScanHistoryId(id)
            scanStatusMessage = "Scan history record deleted. The file itself was not removed."
        }
    }
}

class RiskViewModelFactory(
    private val repository: ScanRepository,
    private val settingsManager: SettingsManager,
    private val context: Context
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(RiskViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return RiskViewModel(repository, settingsManager, context.applicationContext) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

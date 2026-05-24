package com.example

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import com.example.data.database.AppDatabase
import com.example.data.preferences.SettingsManager
import com.example.data.repository.ScanRepository
import com.example.ui.RiskViewModel
import com.example.ui.RiskViewModelFactory
import com.example.ui.Screen
import com.example.ui.screens.DetailScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.util.DownloadFolderPolicy
import com.example.util.NotificationHelper

class MainActivity : ComponentActivity() {

    private val TAG = "MainActivity"
    private lateinit var viewModel: RiskViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // 1. Initialize core dependencies
        val database = AppDatabase.getDatabase(applicationContext)
        val repository = ScanRepository(database.scanHistoryDao())
        val settingsManager = SettingsManager(applicationContext)

        // 2. Initialize ViewModel and Factory
        val factory = RiskViewModelFactory(repository, settingsManager, applicationContext)
        viewModel = ViewModelProvider(this, factory)[RiskViewModel::class.java]

        // 3. Setup notification channel
        NotificationHelper.createNotificationChannel(applicationContext)

        setContent {
            MyApplicationTheme {
                val context = LocalContext.current

                // 4. Register folder permission launcher
                val folderLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.OpenDocumentTree()
                ) { uri ->
                    uri?.let { selectedUri ->
                        Log.d(TAG, "User selected folder Uri: $selectedUri")

                        if (!DownloadFolderPolicy.isDownloadsTree(context, selectedUri)) {
                            viewModel.rejectFolderSelection("Please select the system Downloads folder. Other folders are rejected by design.")
                            return@let
                        }
                        
                        // Grant persistable permission so it lives beyond reboot
                        val takeFlags: Int = Intent.FLAG_GRANT_READ_URI_PERMISSION
                        try {
                            context.contentResolver.takePersistableUriPermission(selectedUri, takeFlags)
                        } catch (e: Exception) {
                            Log.e(TAG, "Failed taking persistable permission", e)
                        }

                        val folderName = DownloadFolderPolicy.resolveFolderName(context, selectedUri) ?: "Downloads"
                        viewModel.updateGrantedFolder(selectedUri.toString(), folderName)
                    }
                }

                // 5. Ask for notifications permission dynamically if Android 13/Tiramisu +
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    val permissionLauncher = rememberLauncherForActivityResult(
                        contract = ActivityResultContracts.RequestPermission()
                    ) { isGranted ->
                        Log.d(TAG, "POST_NOTIFICATIONS permission status: $isGranted")
                    }

                    LaunchedEffect(Unit) {
                        if (ContextCompat.checkSelfPermission(
                                context,
                                android.Manifest.permission.POST_NOTIFICATIONS
                            ) != PackageManager.PERMISSION_GRANTED
                        ) {
                            permissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                        }
                    }
                }

                // Trigger a folder scan on load if granted
                LaunchedEffect(Unit) {
                    if (viewModel.grantedFolderUri.isNotEmpty()) {
                        viewModel.triggerFolderScan()
                    }
                }

                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    // 6. Navigation router implementing modern slide animations
                    AnimatedContent(
                        targetState = viewModel.currentScreen,
                        transitionSpec = {
                            if (targetState is Screen.Home) {
                                (slideInHorizontally { width -> -width } + fadeIn())
                                    .togetherWith(slideOutHorizontally { width -> width } + fadeOut())
                            } else {
                                (slideInHorizontally { width -> width } + fadeIn())
                                    .togetherWith(slideOutHorizontally { width -> -width } + fadeOut())
                            }
                        },
                        label = "ScreenNavigation"
                    ) { screen ->
                        when (screen) {
                            Screen.Home -> {
                                HomeScreen(
                                    viewModel = viewModel,
                                    onSelectFolderClick = { folderLauncher.launch(null) },
                                    modifier = Modifier.padding(innerPadding)
                                )
                            }
                            is Screen.Detail -> {
                                DetailScreen(
                                    scanId = screen.scanId,
                                    viewModel = viewModel,
                                    modifier = Modifier.padding(innerPadding)
                                )
                            }
                            Screen.Settings -> {
                                SettingsScreen(
                                    viewModel = viewModel,
                                    modifier = Modifier.padding(innerPadding)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

}

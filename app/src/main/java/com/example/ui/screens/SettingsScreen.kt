package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.RiskViewModel
import com.example.ui.Screen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: RiskViewModel,
    modifier: Modifier = Modifier
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Scanner Settings", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color(0xFF1D1B1E)) },
                navigationIcon = {
                    IconButton(
                        onClick = { viewModel.navigateTo(Screen.Home) },
                        modifier = Modifier
                            .testTag("back_button")
                            .size(44.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back to dashboard",
                            tint = Color(0xFF1D1B1E)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFFFDFCFB),
                    titleContentColor = Color(0xFF1D1B1E)
                )
            )
        },
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFFDFCFB))
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // 1. General Scanner Behavior Row
            SettingsSectionHeader(title = "CORE BEHAVIORS")

            OutlinedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE0E0E0))
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    SettingsToggleRow(
                        title = "Monitoring Enabled",
                        subtitle = "Perform threat evaluation on folder items.",
                        checked = viewModel.isMonitoringEnabled,
                        onCheckedChange = { viewModel.toggleMonitoring(it) },
                        modifier = Modifier.testTag("toggle_monitoring")
                    )
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = Color(0xFFF2F0F4))
                    SettingsToggleRow(
                        title = "Push Notifications On-Risk",
                        subtitle = "Alert instantly if suspicious features are flagged.",
                        checked = viewModel.isNotificationsEnabled,
                        onCheckedChange = { viewModel.toggleNotifications(it) },
                        modifier = Modifier.testTag("toggle_notifications")
                    )
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = Color(0xFFF2F0F4))
                    SettingsToggleRow(
                        title = "Scan Downloads Directory Only",
                        subtitle = "Strictly folder-scoped storage sandbox.",
                        checked = true, // Force locked
                        enabled = false, // Locked to prevent unnecessary permission requests
                        onCheckedChange = {},
                        modifier = Modifier.testTag("toggle_downloads_only")
                    )
                }
            }

            // 2. Technical Background Standby Card
            OutlinedCard(
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFFE2F3E5).copy(alpha = 0.3f)
                ),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFC8E6C9)),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "Info icon",
                            tint = Color(0xFF1B5E20),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "Android Standby Optimization",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = Color(0xFF1B5E20)
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Because modern Android aggressively manages resources, background sweeps may experience latency. Opening the app or tapping [Scan Now] on the Home screen initiates immediate real-time synchronization.",
                        fontSize = 11.sp,
                        color = Color(0xFF49454E),
                        lineHeight = 15.sp
                    )
                }
            }

            // 3. Supported File Types Section
            SettingsSectionHeader(title = "SCAN TARGETS FILTER")

            OutlinedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE0E0E0))
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    SettingsToggleRow(
                        title = "Android Installers (.apk, .xapk, .apks)",
                        subtitle = "Diligently evaluates binary manifest & permission risk combos.",
                        checked = viewModel.scanApk,
                        onCheckedChange = { viewModel.toggleScanApk(it) }
                    )
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = Color(0xFFF2F0F4))
                    SettingsToggleRow(
                        title = "Compressed Archives (.zip)",
                        subtitle = "Inspects entry mappings for nested droppers/exploits.",
                        checked = viewModel.scanZip,
                        onCheckedChange = { viewModel.toggleScanZip(it) }
                    )
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = Color(0xFFF2F0F4))
                    SettingsToggleRow(
                        title = "PDF Documents (.pdf)",
                        subtitle = "Checks automated javascript & shell command triggers.",
                        checked = viewModel.scanPdf,
                        onCheckedChange = { viewModel.toggleScanPdf(it) }
                    )
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = Color(0xFFF2F0F4))
                    SettingsToggleRow(
                        title = "Word / Office Charts (.docx, .xlsx)",
                        subtitle = "Searches for embedded macros & suspicious external hyper-links.",
                        checked = viewModel.scanDoc,
                        onCheckedChange = { viewModel.toggleScanDoc(it) }
                    )
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = Color(0xFFF2F0F4))
                    SettingsToggleRow(
                        title = "Scripts & Markdowns (.html, .js)",
                        subtitle = "Screens script files for obfuscators, eval() triggers, and base64 payloads.",
                        checked = viewModel.scanHtmlJs,
                        onCheckedChange = { viewModel.toggleScanHtmlJs(it) }
                    )
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = Color(0xFFF2F0F4))
                    SettingsToggleRow(
                        title = "Plain Text Files (.txt, .log)",
                        subtitle = "Scans files for social-engineering triggers combined with links.",
                        checked = viewModel.scanTxt,
                        onCheckedChange = { viewModel.toggleScanTxt(it) }
                    )
                }
            }
        }
    }
}

@Composable
fun SettingsSectionHeader(title: String) {
    Text(
        text = title,
        fontWeight = FontWeight.Bold,
        fontSize = 11.sp,
        color = Color(0xFF6750A4),
        modifier = Modifier.padding(horizontal = 4.dp),
        letterSpacing = 1.sp
    )
}

@Composable
fun SettingsToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = if (enabled) Color(0xFF1D1B1E) else Color(0xFF1D1B1E).copy(alpha = 0.5f)
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                fontSize = 11.sp,
                color = Color(0xFF49454E),
                lineHeight = 14.sp
            )
        }
        Spacer(modifier = Modifier.width(16.dp))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            enabled = enabled,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = Color(0xFF6750A4),
                uncheckedThumbColor = Color(0xFF625B71),
                uncheckedTrackColor = Color(0xFFF2F0F4)
            ),
            modifier = Modifier.minimumInteractiveComponentSize()
        )
    }
}

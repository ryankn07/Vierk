package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ScanHistory
import com.example.ui.RiskViewModel
import com.example.ui.Screen
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: RiskViewModel,
    onSelectFolderClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scanHistory by viewModel.scanHistoryState.collectAsState()
    val context = LocalContext.current

    // Compute stats
    val totalScanned = scanHistory.size
    val highRiskCount = scanHistory.count { it.riskLevel == "High Risk" }
    val suspiciousCount = scanHistory.count { it.riskLevel == "Suspicious" }
    val safeCount = scanHistory.count { it.riskLevel == "Safe" }
    val unknownCount = scanHistory.count { it.riskLevel == "Unknown" }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(vertical = 4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFFE8DEF8)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = "Sentinel Logo",
                                tint = Color(0xFF21005D),
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                "Sentinel",
                                fontWeight = FontWeight.Bold,
                                fontSize = 19.sp,
                                color = Color(0xFF1D1B1E)
                            )
                            Text(
                                "Downloads Monitor",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF6750A4)
                            )
                        }
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.navigateTo(Screen.Settings) },
                        modifier = Modifier
                            .testTag("settings_button")
                            .size(44.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Open Settings",
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
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFFDFCFB))
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            // 1. Folder Connection Status Card
            item {
                FolderStatusCard(
                    folderName = viewModel.grantedFolderName,
                    isGranted = viewModel.grantedFolderUri.isNotEmpty(),
                    onSelectFolderClick = onSelectFolderClick
                )
            }

            // 2. Active Analyzer Trigger Panel
            item {
                ScanControlCard(
                    isScanning = viewModel.isScanning,
                    statusMessage = viewModel.scanStatusMessage,
                    isFolderConnected = viewModel.grantedFolderUri.isNotEmpty(),
                    onScanClick = { viewModel.triggerFolderScan() }
                )
            }

            // 3. Stats Dashboard Counts Grid
            item {
                StatsGrid(
                    total = totalScanned,
                    highRisk = highRiskCount,
                    suspicious = suspiciousCount,
                    safe = safeCount
                )
            }

            // 4. Scanned File History Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "RECENT ACTIVITIES",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1D1B1E),
                        letterSpacing = 0.5.sp
                    )
                    if (scanHistory.isNotEmpty()) {
                        TextButton(
                            onClick = { viewModel.clearHistory() },
                            colors = ButtonDefaults.textButtonColors(
                                contentColor = Color(0xFF6750A4)
                            )
                        ) {
                            Text("Clear All", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // 5. Scanned items list
            if (scanHistory.isEmpty()) {
                item {
                    EmptyHistoryPlaceholder(isFolderConnected = viewModel.grantedFolderUri.isNotEmpty())
                }
            } else {
                items(scanHistory, key = { it.id }) { scan ->
                    ScanHistoryItem(
                        scan = scan,
                        onItemClick = { viewModel.navigateTo(Screen.Detail(scan.id)) },
                        onDeleteClick = { viewModel.deleteScan(scan.id) }
                    )
                }
            }

            // 6. Advisory Footer
            item {
                OutlinedCard(
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFFFFFBFF)
                    ),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFCAC4D0)),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp)
                    ) {
                        Text(
                            text = "Analysis based on heuristic scanning. This tool reduces risk but cannot guarantee 100% safety. Please exercise caution before installing untrusted files.",
                            fontSize = 10.sp,
                            color = Color(0xFF49454E),
                            lineHeight = 14.sp,
                            fontWeight = FontWeight.Normal,
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun FolderStatusCard(
    folderName: String,
    isGranted: Boolean,
    onSelectFolderClick: () -> Unit
) {
    val cardBg = if (isGranted) GuardedGreenContainer else HighRiskContainer
    val cardBorder = if (isGranted) GuardedGreenBorder else HighRiskBorder
    val textColor = if (isGranted) GuardedGreenText else HighRiskText

    OutlinedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = cardBg
        ),
        border = androidx.compose.foundation.BorderStroke(1.dp, cardBorder)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(if (isGranted) GuardedGreenIndicator else Color(0xFFBA1A1A))
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isGranted) "Monitoring Active" else "Action Required",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleSmall,
                        color = textColor
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = if (isGranted) "/storage/emulated/0/$folderName" else "Grant Folder permission to start scanning.",
                    fontSize = 13.sp,
                    color = textColor.copy(alpha = 0.85f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Button(
                onClick = onSelectFolderClick,
                modifier = Modifier
                    .testTag("choose_folder_button")
                    .heightIn(min = 38.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White,
                    contentColor = textColor
                ),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 1.dp)
            ) {
                Text(
                    if (isGranted) "Change" else "Grant",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun ScanControlCard(
    isScanning: Boolean,
    statusMessage: String,
    isFolderConnected: Boolean,
    onScanClick: () -> Unit
) {
    val outlineColor = Color(0xFFE0E0E0)
    OutlinedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        border = androidx.compose.foundation.BorderStroke(1.dp, outlineColor)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        "Sentinel Scanner",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color(0xFF1D1B1E)
                    )
                    Text(
                        "Scans only newly appended documents",
                        fontSize = 11.sp,
                        color = Color(0xFF49454E)
                    )
                }
                Button(
                    onClick = onScanClick,
                    enabled = !isScanning && isFolderConnected,
                    modifier = Modifier
                        .testTag("scan_now_button")
                        .height(44.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFE8DEF8),
                        contentColor = Color(0xFF21005D),
                        disabledContainerColor = Color(0xFFF2F0F4),
                        disabledContentColor = Color(0xFFCAC4D0)
                    )
                ) {
                    if (isScanning) {
                        CircularProgressIndicator(
                            color = Color(0xFF21005D),
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Scan icon",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Scan Now", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }

            if (statusMessage.isNotEmpty() || isScanning) {
                Spacer(modifier = Modifier.height(12.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFFF2F0F4))
                        .padding(12.dp)
                ) {
                    Text(
                        text = statusMessage.ifEmpty { "Waking scanners..." },
                        fontSize = 12.sp,
                        color = Color(0xFF49454E),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}

@Composable
fun StatsGrid(
    total: Int,
    highRisk: Int,
    suspicious: Int,
    safe: Int
) {
    Column {
        Text(
            "Scan Statistics",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF49454E),
            modifier = Modifier.padding(bottom = 8.dp, start = 4.dp),
            letterSpacing = 0.5.sp
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            StatsBox(
                label = "Scanned",
                count = total,
                containerColor = Color(0xFFF2F0F4),
                contentColor = Color(0xFF1D1B1E),
                modifier = Modifier.weight(1f)
            )
            StatsBox(
                label = "High Risk",
                count = highRisk,
                containerColor = Color(0xFFFFEBEE),
                contentColor = Color(0xFFC62828),
                modifier = Modifier.weight(1f)
            )
            StatsBox(
                label = "Suspicious",
                count = suspicious,
                containerColor = Color(0xFFFFF3E0),
                contentColor = Color(0xFFEF6C00),
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
fun StatsBox(
    label: String,
    count: Int,
    containerColor: Color,
    contentColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = containerColor
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp, horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = count.toString(),
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = contentColor
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = label.uppercase(),
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                color = contentColor.copy(alpha = 0.85f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun ScanHistoryItem(
    scan: ScanHistory,
    onItemClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    val (backColor, textColor, iconColor, icon) = when (scan.riskLevel) {
        "High Risk" -> Quadruple(HighRiskContainer, HighRiskText, HighRiskText, Icons.Default.Warning)
        "Suspicious" -> Quadruple(SuspiciousContainer, SuspiciousText, SuspiciousText, Icons.Default.Info)
        "Safe" -> Quadruple(SafeContainer, SafeText, SafeText, Icons.Default.Lock)
        else -> Quadruple(UnknownContainer, UnknownText, UnknownText, Icons.Default.Info)
    }

    val format = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault())
    val formattedTime = format.format(Date(scan.timestamp))

    OutlinedCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("task_item_card")
            .clickable(onClick = onItemClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE0E0E0))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left Status Icon Box
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(backColor),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = scan.riskLevel,
                    tint = iconColor,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            // Main Text Block
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = scan.fileName,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = Color(0xFF1D1B1E),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                
                Spacer(modifier = Modifier.height(2.dp))
                
                Text(
                    text = "${scan.riskLevel}: ${scan.realFileType.uppercase()}",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = textColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = "$formattedTime • ${formatSize(scan.fileSize)}",
                    fontSize = 10.sp,
                    color = Color(0xFF49454E)
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Action: Delete button
            IconButton(
                onClick = onDeleteClick,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete from history",
                    tint = Color(0xFF49454E).copy(alpha = 0.6f),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
fun EmptyHistoryPlaceholder(isFolderConnected: Boolean) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFF2F0F4).copy(alpha = 0.5f)
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = "Shield Icon",
                tint = Color(0xFF6750A4).copy(alpha = 0.5f),
                modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                "No documents parsed yet.",
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = Color(0xFF1D1B1E)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = if (isFolderConnected) {
                    "Press [Scan Now] above to list and evaluate materials in your selected Downloads directory!"
                } else {
                    "Please tap [Grant Folder] at the top and select your Downloads directory to begin monitoring."
                },
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                fontSize = 12.sp,
                color = Color(0xFF49454E),
                lineHeight = 16.sp
            )
        }
    }
}

fun formatSize(size: Long): String {
    if (size <= 0) return "0 B"
    val units = arrayOf("B", "KB", "MB", "GB")
    val digitGroups = (Math.log10(size.toDouble()) / Math.log10(1024.0)).toInt()
    return String.format("%.1f %s", size / Math.pow(1024.0, digitGroups.toDouble()), units[digitGroups])
}

// Simple Quadruple container helper for multi-valued mapping
data class Quadruple<out A, out B, out C, out D>(
    val first: A,
    val second: B,
    val third: C,
    val fourth: D
)

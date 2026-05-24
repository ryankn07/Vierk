package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.RiskViewModel
import com.example.ui.Screen
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun DetailScreen(
    scanId: Long,
    viewModel: RiskViewModel,
    modifier: Modifier = Modifier
) {
    val scanHistory by viewModel.scanHistoryState.collectAsState()
    val scan = scanHistory.find { it.id == scanId }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Analysis Detail", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color(0xFF1D1B1E)) },
                navigationIcon = {
                    IconButton(
                        onClick = { viewModel.navigateTo(Screen.Home) },
                        modifier = Modifier
                            .testTag("back_button")
                            .size(44.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
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
        if (scan == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFFFDFCFB))
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Text("Analysis record not found.", color = MaterialTheme.colorScheme.error)
            }
        } else {
            val (riskColor, riskBgColor, riskBorderColor) = when (scan.riskLevel) {
                "High Risk" -> Triple(HighRiskText, HighRiskContainer, HighRiskBorder)
                "Suspicious" -> Triple(SuspiciousText, SuspiciousContainer, Color(0xFFFFE082))
                "Safe" -> Triple(SafeText, SafeContainer, GuardedGreenBorder)
                else -> Triple(UnknownText, UnknownContainer, Color(0xFFE0E0E0))
            }

            val format = SimpleDateFormat("MMM dd, yyyy HH:mm:ss", Locale.getDefault())
            val formattedTime = format.format(Date(scan.timestamp))

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFFFDFCFB))
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 1. Hero Card (Filename + Risk Badge)
                OutlinedCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color.White
                    ),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE0E0E0))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = riskColor.copy(alpha = 0.15f)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(riskColor)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "${scan.riskLevel} Case",
                                        color = riskColor,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp
                                    )
                                }
                            }
                            Text(
                                text = formattedTime,
                                fontSize = 11.sp,
                                color = Color(0xFF49454E),
                                fontWeight = FontWeight.Medium
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Text(
                            text = scan.fileName,
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp,
                            color = Color(0xFF1D1B1E),
                            modifier = Modifier.testTag("detail_title")
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = "Scoped location: Downloads/${scan.fileName}",
                            fontSize = 11.sp,
                            color = Color(0xFF49454E),
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                // 2. Install / Run Threat Warning (Required)
                if (scan.riskLevel == "High Risk" || scan.riskLevel == "Suspicious") {
                    OutlinedCard(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = riskBgColor.copy(alpha = 0.6f)
                        ),
                        border = androidx.compose.foundation.BorderStroke(1.dp, riskBorderColor)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = "Security Alert Sign",
                                tint = riskColor,
                                modifier = Modifier.size(26.dp)
                            )
                            Spacer(modifier = Modifier.width(14.dp))
                            Column {
                                Text(
                                    "SECURITY ADVISORY",
                                    fontWeight = FontWeight.Black,
                                    fontSize = 12.sp,
                                    color = riskColor,
                                    letterSpacing = 1.sp
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "This file matched one or more local risk rules. Treat this result as a warning signal and verify the source before opening or installing it.",
                                    fontSize = 12.sp,
                                    color = Color(0xFF1D1B1E),
                                    lineHeight = 16.sp
                                )
                            }
                        }
                    }
                }

                // 3. Exact Flagged Security Reasons
                OutlinedCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color.White
                    ),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE0E0E0))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp)
                    ) {
                        Text(
                            "Heuristic Insights",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = Color(0xFF1D1B1E)
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        val reasonsList = scan.riskReasons.split("\n")
                        reasonsList.filter { it.trim().isNotEmpty() }.forEach { reason ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                Text(
                                    text = "-",
                                    fontWeight = FontWeight.Bold,
                                    color = riskColor,
                                    modifier = Modifier.width(16.dp)
                                )
                                Text(
                                    text = reason,
                                    fontSize = 13.sp,
                                    color = Color(0xFF49454E),
                                    lineHeight = 18.sp
                                )
                            }
                        }
                    }
                }

                // 4. File Metadata Grid
                OutlinedCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color.White
                    ),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE0E0E0))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp)
                    ) {
                        Text(
                            "File Specifications",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = Color(0xFF1D1B1E)
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        val metas = listOf(
                            "Declared Ext:" to ".${scan.declaredExtension}",
                            "Real Magic Type:" to scan.realFileType,
                            "File Size:" to formatSize(scan.fileSize),
                            "SHA-256 Digest:" to scan.hashSha256
                        )

                        metas.forEach { (label, valStr) ->
                            Column(modifier = Modifier.padding(vertical = 6.dp)) {
                                Text(label, fontSize = 11.sp, color = Color(0xFF6750A4), fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(valStr, fontSize = 12.sp, color = Color(0xFF1D1B1E), fontFamily = FontFamily.Monospace, maxLines = 2, lineHeight = 16.sp)
                            }
                        }
                    }
                }

                // 5. Extracted Package & Permissions (APKs Only)
                if (scan.realFileType == "APK" && !scan.permissions.isNullOrEmpty()) {
                    val permList = scan.permissions.split(",").filter { it.isNotBlank() }
                    if (permList.isNotEmpty()) {
                        OutlinedCard(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = Color.White
                            ),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE0E0E0))
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(18.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Lock,
                                        contentDescription = "Lock",
                                        tint = Color(0xFF6750A4),
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        "Manifest Permissions (${permList.size})",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = Color(0xFF1D1B1E)
                                    )
                                }
                                Spacer(modifier = Modifier.height(12.dp))
                                FlowRow(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    permList.forEach { perm ->
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(Color(0xFFF2F0F4))
                                                .padding(horizontal = 8.dp, vertical = 4.dp)
                                        ) {
                                            Text(
                                                text = perm.replace("android.permission.", ""),
                                                fontSize = 11.sp,
                                                color = Color(0xFF49454E),
                                                fontFamily = FontFamily.Monospace
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // 6. Extracted URLs/Domains List
                if (!scan.extractedUrls.isNullOrEmpty()) {
                    val urlList = scan.extractedUrls.split(",").filter { it.isNotBlank() }
                    if (urlList.isNotEmpty()) {
                        OutlinedCard(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = Color.White
                            ),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE0E0E0))
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(18.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Info,
                                        contentDescription = "Link",
                                        tint = Color(0xFF6750A4),
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        "Extracted URLs / Domain Triggers (${urlList.size})",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = Color(0xFF1D1B1E)
                                    )
                                }
                                Spacer(modifier = Modifier.height(12.dp))
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    urlList.forEach { url ->
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(Color(0xFFF2F0F4))
                                                .padding(8.dp)
                                        ) {
                                            Text(
                                                text = url,
                                                fontSize = 11.sp,
                                                color = Color(0xFF6750A4),
                                                fontFamily = FontFamily.Monospace,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FlowRow(
    modifier: Modifier = Modifier,
    horizontalArrangement: Arrangement.Horizontal = Arrangement.Start,
    verticalArrangement: Arrangement.Vertical = Arrangement.Top,
    content: @Composable FlowRowScope.() -> Unit
) {
    androidx.compose.foundation.layout.FlowRow(
        modifier = modifier,
        horizontalArrangement = horizontalArrangement,
        verticalArrangement = verticalArrangement,
        content = content
    )
}

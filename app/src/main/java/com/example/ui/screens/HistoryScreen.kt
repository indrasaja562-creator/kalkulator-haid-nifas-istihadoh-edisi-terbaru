package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.entities.CalculationHistoryEntity
import com.example.ui.theme.EmeraldDeep
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.FiqihHaidColor
import com.example.ui.theme.FiqihSuciColor
import com.example.ui.theme.GoldTertiary
import com.example.ui.viewmodel.FiqihViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    viewModel: FiqihViewModel,
    onOpenProfileSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val historyList by viewModel.calculationHistory.collectAsState()
    var selectedItemForDetail by remember { mutableStateOf<CalculationHistoryEntity?>(null) }
    var showClearAllConfirm by remember { mutableStateOf(false) }
    var selectedFilter by remember { mutableStateOf("ALL") } // "ALL", "HAID", "NIFAS"

    val dateFormat = remember { SimpleDateFormat("dd MMM yyyy", Locale("id", "ID")) }
    val timeFormat = remember { SimpleDateFormat("HH:mm", Locale("id", "ID")) }

    val filteredList = remember(historyList, selectedFilter) {
        when (selectedFilter) {
            "HAID" -> historyList.filter { it.caseType == "HAID" }
            "NIFAS" -> historyList.filter { it.caseType == "NIFAS" }
            else -> historyList
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFFAF7F2))
            .testTag("history_screen")
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Hero Banner: Deep Forest Green Arched Card
            item {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("history_hero_banner"),
                    shape = RoundedCornerShape(22.dp),
                    color = EmeraldDeep,
                    border = BorderStroke(1.dp, GoldTertiary.copy(alpha = 0.5f)),
                    shadowElevation = 3.dp
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.horizontalGradient(
                                    colors = listOf(
                                        Color(0xFF0D3D32),
                                        Color(0xFF134E3F),
                                        Color(0xFF185A49)
                                    )
                                )
                            )
                            .padding(20.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.Top
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Riwayat Analisis Siklus",
                                        style = MaterialTheme.typography.titleLarge.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 20.sp,
                                            letterSpacing = (-0.3).sp
                                        ),
                                        color = Color.White
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Arsip & kesimpulan hukum fiqih dari perhitungan Anda",
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontSize = 12.5.sp,
                                            lineHeight = 18.sp
                                        ),
                                        color = Color(0xFFF9F3E5).copy(alpha = 0.9f)
                                    )
                                }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    IconButton(
                                        onClick = onOpenProfileSettings,
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(CircleShape)
                                            .background(GoldTertiary.copy(alpha = 0.2f))
                                            .border(1.dp, GoldTertiary, CircleShape)
                                            .testTag("open_profile_adat_btn")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.Tune,
                                            contentDescription = "Pengaturan Adat",
                                            tint = GoldTertiary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            }

                            // Mazhab & Count Pill
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = Color.White.copy(alpha = 0.12f),
                                    border = BorderStroke(0.5.dp, GoldTertiary.copy(alpha = 0.4f))
                                ) {
                                    Text(
                                        text = "${historyList.size} Siklus Tersimpan",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 11.sp
                                        ),
                                        color = Color(0xFFF9F3E5),
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 2. Filter Pills & Clear All Action
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        HistoryFilterChip(
                            label = "Semua (${historyList.size})",
                            isSelected = selectedFilter == "ALL",
                            onClick = { selectedFilter = "ALL" }
                        )
                        HistoryFilterChip(
                            label = "Haid (${historyList.count { it.caseType == "HAID" }})",
                            isSelected = selectedFilter == "HAID",
                            onClick = { selectedFilter = "HAID" }
                        )
                        HistoryFilterChip(
                            label = "Nifas (${historyList.count { it.caseType == "NIFAS" }})",
                            isSelected = selectedFilter == "NIFAS",
                            onClick = { selectedFilter = "NIFAS" }
                        )
                    }

                    if (historyList.isNotEmpty()) {
                        IconButton(
                            onClick = { showClearAllConfirm = true },
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(Color.White)
                                .border(1.dp, Color(0xFFEBE5DC), CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.DeleteSweep,
                                contentDescription = "Hapus Semua",
                                tint = Color(0xFF9E2A2B),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            // 3. History List
            if (filteredList.isEmpty()) {
                item {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 10.dp),
                        shape = RoundedCornerShape(20.dp),
                        color = Color.White,
                        border = BorderStroke(1.dp, Color(0xFFEBE5DC))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(36.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(54.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFF9F3E5))
                                    .border(1.dp, GoldTertiary.copy(alpha = 0.4f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.HistoryEdu,
                                    contentDescription = null,
                                    tint = GoldTertiary,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(14.dp))
                            Text(
                                text = "Belum Ada Riwayat Perhitungan",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp
                                ),
                                color = Color(0xFF1E211F)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Hasil perhitungan kalkulator haid dan nifas akan tersimpan secara otomatis di sini.",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    lineHeight = 18.sp
                                ),
                                color = Color(0xFF6B726C),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            } else {
                items(filteredList, key = { it.id }) { item ->
                    HistoryItemCard(
                        item = item,
                        dateFormat = dateFormat,
                        onClick = { selectedItemForDetail = item }
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(56.dp))
            }
        }
    }

    // Detail Dialog
    selectedItemForDetail?.let { item ->
        HistoryDetailDialog(
            item = item,
            dateFormat = dateFormat,
            onDismiss = { selectedItemForDetail = null },
            onDelete = {
                viewModel.deleteHistory(item.id)
                selectedItemForDetail = null
            }
        )
    }

    // Clear All Dialog
    if (showClearAllConfirm) {
        AlertDialog(
            onDismissRequest = { showClearAllConfirm = false },
            shape = RoundedCornerShape(20.dp),
            containerColor = Color.White,
            title = {
                Text(
                    "Hapus Semua Riwayat?",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = Color(0xFF1E211F)
                )
            },
            text = {
                Text(
                    "Semua arsip perhitungan siklus akan dihapus permanen dari penyimpanan perangkat Anda.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF555B55)
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.clearAllHistory()
                        showClearAllConfirm = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF9E2A2B)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Hapus Semua", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showClearAllConfirm = false },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Batal", color = Color(0xFF6B726C))
                }
            }
        )
    }
}

@Composable
fun HistoryFilterChip(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) EmeraldPrimary else Color.White,
        border = BorderStroke(
            1.dp,
            if (isSelected) GoldTertiary.copy(alpha = 0.6f) else Color(0xFFEBE5DC)
        ),
        shadowElevation = if (isSelected) 1.dp else 0.dp
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                fontSize = 12.sp
            ),
            color = if (isSelected) Color.White else Color(0xFF454B46),
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp)
        )
    }
}

@Composable
fun HistoryItemCard(
    item: CalculationHistoryEntity,
    dateFormat: SimpleDateFormat,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val startDateStr = remember(item.startEpochMillis) { dateFormat.format(Date(item.startEpochMillis)) }
    val endDateStr = remember(item.endEpochMillis) { dateFormat.format(Date(item.endEpochMillis)) }
    val isNifas = item.caseType == "NIFAS"

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("history_item_${item.id}"),
        shape = RoundedCornerShape(18.dp),
        color = Color.White,
        border = BorderStroke(1.dp, Color(0xFFEBE5DC)),
        shadowElevation = 1.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Icon Badge
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(
                        if (isNifas) Color(0xFFF7EBF0) else Color(0xFFE8F4EE)
                    )
                    .border(
                        1.dp,
                        if (isNifas) Color(0xFF882944).copy(alpha = 0.3f) else EmeraldPrimary.copy(alpha = 0.3f),
                        RoundedCornerShape(14.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isNifas) Icons.Outlined.ChildCare else Icons.Outlined.WaterDrop,
                    contentDescription = null,
                    tint = if (isNifas) Color(0xFF882944) else EmeraldPrimary,
                    modifier = Modifier.size(22.dp)
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "$startDateStr – $endDateStr",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.5.sp
                        ),
                        color = Color(0xFF1E211F)
                    )

                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = if (isNifas) Color(0xFFF7EBF0) else Color(0xFFE8F4EE)
                    ) {
                        Text(
                            text = if (isNifas) "NIFAS" else "HAID",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 10.5.sp
                            ),
                            color = if (isNifas) Color(0xFF882944) else EmeraldPrimary,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = item.statusSummary,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = 12.sp,
                        lineHeight = 16.sp
                    ),
                    color = Color(0xFF6B726C),
                    maxLines = 1
                )

                if (item.categoryName.isNotBlank()) {
                    Text(
                        text = item.categoryName,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        ),
                        color = EmeraldPrimary,
                        maxLines = 1
                    )
                }
            }

            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = "Lihat Detail",
                tint = Color(0xFFB0A99F),
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
fun HistoryDetailDialog(
    item: CalculationHistoryEntity,
    dateFormat: SimpleDateFormat,
    onDismiss: () -> Unit,
    onDelete: () -> Unit
) {
    val startDateStr = remember(item.startEpochMillis) { dateFormat.format(Date(item.startEpochMillis)) }
    val endDateStr = remember(item.endEpochMillis) { dateFormat.format(Date(item.endEpochMillis)) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(22.dp),
        containerColor = Color(0xFFFAF7F2),
        title = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "Rincian Riwayat Siklus",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    ),
                    color = Color(0xFF1E211F)
                )
                Text(
                    text = "$startDateStr – $endDateStr",
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = EmeraldPrimary
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Kesimpulan Status
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Color.White,
                    border = BorderStroke(1.dp, Color(0xFFEBE5DC)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "STATUS & KESIMPULAN",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.5.sp,
                                letterSpacing = 0.8.sp
                            ),
                            color = GoldTertiary
                        )
                        Text(
                            text = item.statusSummary,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.Bold
                            ),
                            color = Color(0xFF1E211F)
                        )
                        if (item.categoryName.isNotBlank()) {
                            Text(
                                text = "Kategori: ${item.categoryName}",
                                style = MaterialTheme.typography.bodySmall,
                                color = EmeraldPrimary
                            )
                        }
                    }
                }

                // Kewajiban Ibadah
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Color.White,
                    border = BorderStroke(1.dp, Color(0xFFEBE5DC)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "KEWAJIBAN IBADAH",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.5.sp,
                                letterSpacing = 0.8.sp
                            ),
                            color = EmeraldPrimary
                        )
                        Text(
                            text = "• Shalat: ${item.shalatNote}",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                            color = Color(0xFF454B46)
                        )
                        Text(
                            text = "• Puasa: ${item.puasaNote}",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                            color = Color(0xFF454B46)
                        )
                        Text(
                            text = "• Mandi: ${item.mandiNote}",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                            color = Color(0xFF454B46)
                        )
                    }
                }

                if (item.note.isNotBlank()) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFFFAF7F2),
                        border = BorderStroke(0.5.dp, Color(0xFFEBE5DC)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Catatan: ${item.note}",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                            color = Color(0xFF6B726C),
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
            ) {
                Text("Tutup", color = Color.White)
            }
        },
        dismissButton = {
            TextButton(
                onClick = { showDeleteConfirm = true },
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Hapus Riwayat", color = Color(0xFF9E2A2B))
            }
        }
    )

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            shape = RoundedCornerShape(20.dp),
            containerColor = Color.White,
            title = {
                Text(
                    "Hapus Riwayat Ini?",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = Color(0xFF1E211F)
                )
            },
            text = {
                Text(
                    "Data siklus $startDateStr – $endDateStr akan dihapus dari riwayat.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF555B55)
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onDelete()
                        showDeleteConfirm = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF9E2A2B)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Hapus", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showDeleteConfirm = false },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Batal", color = Color(0xFF6B726C))
                }
            }
        )
    }
}

package com.example.ui.screens

import android.app.DatePickerDialog
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.entities.QadhaPrayerEntity
import com.example.ui.theme.EmeraldDeep
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.GoldTertiary
import com.example.ui.viewmodel.FiqihViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QadhaScreen(
    viewModel: FiqihViewModel,
    modifier: Modifier = Modifier
) {
    val qadhaList by viewModel.qadhaPrayers.collectAsState()
    var selectedFilter by remember { mutableIntStateOf(1) } // 0: Semua, 1: Belum Lunas, 2: Selesai
    var showAddDialog by remember { mutableStateOf(false) }

    val pendingCount = remember(qadhaList) { qadhaList.count { !it.isCompleted } }
    val completedCount = remember(qadhaList) { qadhaList.count { it.isCompleted } }
    val totalCount = qadhaList.size

    val filteredList = remember(qadhaList, selectedFilter) {
        when (selectedFilter) {
            1 -> qadhaList.filter { !it.isCompleted }
            2 -> qadhaList.filter { it.isCompleted }
            else -> qadhaList
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFFAF7F2))
            .testTag("qadha_screen")
    ) {
        Scaffold(
            containerColor = Color.Transparent,
            floatingActionButton = {
                ExtendedFloatingActionButton(
                    onClick = { showAddDialog = true },
                    icon = {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = null,
                            tint = Color(0xFFF9F3E5)
                        )
                    },
                    text = {
                        Text(
                            "Catat Qadha Baru",
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White
                        )
                    },
                    containerColor = EmeraldPrimary,
                    contentColor = Color.White,
                    shape = RoundedCornerShape(16.dp),
                    elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 4.dp),
                    modifier = Modifier.testTag("add_qadha_fab")
                )
            }
        ) { padding ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 20.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                // 1. Simple Page Header (No large banner)
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .testTag("qadha_hero_banner"),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "Tanggungan Qadha Shalat",
                            style = MaterialTheme.typography.headlineSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 22.sp,
                                letterSpacing = (-0.3).sp
                            ),
                            color = Color(0xFF1E211F)
                        )
                        Text(
                            text = "Pantau & catat pelunasan shalat yang terlewat karena haid atau nifas",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontSize = 13.sp,
                                lineHeight = 19.sp
                            ),
                            color = Color(0xFF6B726C)
                        )
                    }
                }

                // 2. Summary Status Card: Warm Ivory Card with Gold accents
                item {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        color = Color.White,
                        border = BorderStroke(1.dp, Color(0xFFEBE5DC)),
                        shadowElevation = 1.dp
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = "Tanggungan Belum Lunas",
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            fontWeight = FontWeight.Medium
                                        ),
                                        color = Color(0xFF6B726C)
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Row(verticalAlignment = Alignment.Bottom) {
                                        Text(
                                            text = "$pendingCount",
                                            style = MaterialTheme.typography.headlineMedium.copy(
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 28.sp
                                            ),
                                            color = if (pendingCount > 0) Color(0xFF9E2A2B) else EmeraldPrimary
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "Waktu Shalat",
                                            style = MaterialTheme.typography.bodyMedium.copy(
                                                fontWeight = FontWeight.Medium
                                            ),
                                            color = Color(0xFF6B726C),
                                            modifier = Modifier.padding(bottom = 4.dp)
                                        )
                                    }
                                }

                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (pendingCount > 0) Color(0xFFF9ECEE) else Color(0xFFE8F4EE)
                                        )
                                        .border(
                                            1.dp,
                                            if (pendingCount > 0) Color(0xFF9E2A2B).copy(alpha = 0.3f) else EmeraldPrimary.copy(alpha = 0.3f),
                                            CircleShape
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = if (pendingCount == 0) Icons.Default.CheckCircle else Icons.Outlined.AccessTime,
                                        contentDescription = null,
                                        tint = if (pendingCount > 0) Color(0xFF9E2A2B) else EmeraldPrimary,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }

                            HorizontalDivider(
                                color = Color(0xFFEBE5DC).copy(alpha = 0.6f),
                                thickness = 0.8.dp
                            )

                            // Quick Stats Pill Row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                // Belum Lunas Stat
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = Color(0xFFFBF9F5),
                                    border = BorderStroke(0.5.dp, Color(0xFFEBE5DC)),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(10.dp)
                                                .clip(CircleShape)
                                                .background(Color(0xFF9E2A2B))
                                        )
                                        Column {
                                            Text(
                                                text = "Hutang",
                                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                                                color = Color(0xFF6B726C)
                                            )
                                            Text(
                                                text = "$pendingCount Shalat",
                                                style = MaterialTheme.typography.bodySmall.copy(
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 13.sp
                                                ),
                                                color = Color(0xFF1E211F)
                                            )
                                        }
                                    }
                                }

                                // Selesai Stat
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = Color(0xFFFBF9F5),
                                    border = BorderStroke(0.5.dp, Color(0xFFEBE5DC)),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(10.dp)
                                                .clip(CircleShape)
                                                .background(EmeraldPrimary)
                                        )
                                        Column {
                                            Text(
                                                text = "Terlunasi",
                                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                                                color = Color(0xFF6B726C)
                                            )
                                            Text(
                                                text = "$completedCount Shalat",
                                                style = MaterialTheme.typography.bodySmall.copy(
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 13.sp
                                                ),
                                                color = Color(0xFF1E211F)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // 3. Filter Chips: Luxury Pill Styling
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        QadhaFilterChip(
                            label = "Belum Lunas ($pendingCount)",
                            isSelected = selectedFilter == 1,
                            onClick = { selectedFilter = 1 }
                        )
                        QadhaFilterChip(
                            label = "Selesai ($completedCount)",
                            isSelected = selectedFilter == 2,
                            onClick = { selectedFilter = 2 }
                        )
                        QadhaFilterChip(
                            label = "Semua ($totalCount)",
                            isSelected = selectedFilter == 0,
                            onClick = { selectedFilter = 0 }
                        )
                    }
                }

                // 4. List Items
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
                                    .padding(32.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(54.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFFE8F4EE)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.CheckCircle,
                                        contentDescription = null,
                                        tint = EmeraldPrimary,
                                        modifier = Modifier.size(30.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.height(14.dp))
                                Text(
                                    text = if (selectedFilter == 1) "Alhamdulillah, tidak ada tanggungan!" else "Belum ada catatan qadha",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp
                                    ),
                                    color = Color(0xFF1E211F),
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Semua shalat telah selesai ditunaikan atau Anda belum menambahkan catatan baru.",
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
                    items(filteredList, key = { it.id }) { prayer ->
                        QadhaPrayerItem(
                            prayer = prayer,
                            onToggle = { viewModel.toggleQadhaPrayer(prayer) },
                            onDelete = { viewModel.deleteQadhaPrayer(prayer.id) }
                        )
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(72.dp))
                }
            }
        }
    }

    if (showAddDialog) {
        AddQadhaDialog(
            onDismiss = { showAddDialog = false },
            onAdd = { prayerName, dateStr, reason ->
                viewModel.addQadhaPrayer(
                    QadhaPrayerEntity(
                        prayerName = prayerName,
                        dateString = dateStr,
                        reason = reason.ifBlank { "Tercatat manual oleh pengguna" },
                        isCompleted = false
                    )
                )
                showAddDialog = false
            }
        )
    }
}

@Composable
fun QadhaFilterChip(
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
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
        )
    }
}

@Composable
fun QadhaPrayerItem(
    prayer: QadhaPrayerEntity,
    onToggle: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showDeleteConfirm by remember { mutableStateOf(false) }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onToggle() },
        shape = RoundedCornerShape(18.dp),
        color = if (prayer.isCompleted) Color(0xFFF7F5F0) else Color.White,
        border = BorderStroke(
            1.dp,
            if (prayer.isCompleted) Color(0xFFEBE5DC).copy(alpha = 0.6f) else Color(0xFFEBE5DC)
        ),
        shadowElevation = if (prayer.isCompleted) 0.dp else 1.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Checkbox Circle / Pill
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(
                        if (prayer.isCompleted) EmeraldPrimary else Color.Transparent
                    )
                    .border(
                        1.5.dp,
                        if (prayer.isCompleted) EmeraldPrimary else Color(0xFFB0A99F),
                        CircleShape
                    )
                    .clickable { onToggle() },
                contentAlignment = Alignment.Center
            ) {
                if (prayer.isCompleted) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Lunas",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Shalat ${prayer.prayerName}",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            textDecoration = if (prayer.isCompleted) TextDecoration.LineThrough else TextDecoration.None
                        ),
                        color = if (prayer.isCompleted) Color(0xFF8A908A) else Color(0xFF1E211F)
                    )

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (prayer.isCompleted)
                            Color(0xFFE8F4EE)
                        else
                            Color(0xFFF9ECEE)
                    ) {
                        Text(
                            text = if (prayer.isCompleted) "Lunas" else "Belum",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 11.sp
                            ),
                            color = if (prayer.isCompleted)
                                EmeraldPrimary
                            else
                                Color(0xFF9E2A2B),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.5.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = "${prayer.dateString} • ${prayer.reason}",
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = 12.sp,
                        lineHeight = 16.sp
                    ),
                    color = Color(0xFF6B726C),
                    maxLines = 2
                )
            }

            IconButton(
                onClick = { showDeleteConfirm = true },
                modifier = Modifier.size(34.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.DeleteOutline,
                    contentDescription = "Hapus Qadha",
                    tint = Color(0xFF9E2A2B).copy(alpha = 0.7f),
                    modifier = Modifier.size(19.dp)
                )
            }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            shape = RoundedCornerShape(20.dp),
            containerColor = Color.White,
            title = {
                Text(
                    "Hapus Catatan Qadha?",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = Color(0xFF1E211F)
                )
            },
            text = {
                Text(
                    "Hapus catatan shalat ${prayer.prayerName} (${prayer.dateString}) dari daftar tanggungan?",
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddQadhaDialog(
    onDismiss: () -> Unit,
    onAdd: (prayerName: String, dateStr: String, reason: String) -> Unit
) {
    val context = LocalContext.current
    val prayerOptions = listOf("Subuh", "Dzuhur", "Ashar", "Maghrib", "Isya")
    var selectedPrayer by remember { mutableStateOf(prayerOptions[1]) }
    val sdf = remember { SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()) }
    var dateString by remember { mutableStateOf(sdf.format(Date())) }
    var reasonText by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(22.dp),
        containerColor = Color(0xFFFAF7F2),
        title = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "Catat Tanggungan Qadha",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    ),
                    color = Color(0xFF1E211F)
                )
                Text(
                    text = "Masukkan waktu shalat dan tanggal terlewat",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF6B726C)
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "Pilih Waktu Shalat:",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = Color(0xFF1E211F)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    prayerOptions.forEach { p ->
                        val isSelected = selectedPrayer == p
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSelected) EmeraldPrimary else Color.White,
                            border = BorderStroke(
                                1.dp,
                                if (isSelected) GoldTertiary else Color(0xFFEBE5DC)
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .clickable { selectedPrayer = p }
                        ) {
                            Box(
                                modifier = Modifier.padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = p,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        fontSize = 11.sp
                                    ),
                                    color = if (isSelected) Color.White else Color(0xFF454B46)
                                )
                            }
                        }
                    }
                }

                // Tanggal
                Text(
                    text = "Tanggal Shalat Terlewat:",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = Color(0xFF1E211F)
                )
                Surface(
                    onClick = {
                        val cal = Calendar.getInstance()
                        DatePickerDialog(
                            context,
                            { _, y, m, d ->
                                val selected = Calendar.getInstance().apply { set(y, m, d) }
                                dateString = sdf.format(selected.time)
                            },
                            cal.get(Calendar.YEAR),
                            cal.get(Calendar.MONTH),
                            cal.get(Calendar.DAY_OF_MONTH)
                        ).show()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    color = Color.White,
                    border = BorderStroke(1.dp, Color(0xFFEBE5DC))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = dateString,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.SemiBold
                            ),
                            color = Color(0xFF1E211F)
                        )
                        Icon(
                            Icons.Outlined.CalendarToday,
                            contentDescription = null,
                            tint = EmeraldPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                // Alasan
                Text(
                    text = "Keterangan / Sebab:",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = Color(0xFF1E211F)
                )
                OutlinedTextField(
                    value = reasonText,
                    onValueChange = { reasonText = it },
                    placeholder = { Text("Cth: Darah keluar saat waktu shalat tiba", fontSize = 13.sp) },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White,
                        focusedBorderColor = EmeraldPrimary,
                        unfocusedBorderColor = Color(0xFFEBE5DC)
                    ),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onAdd(selectedPrayer, dateString, reasonText) },
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
            ) {
                Text("Simpan Catatan", color = Color.White, fontWeight = FontWeight.SemiBold)
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                shape = RoundedCornerShape(14.dp)
            ) {
                Text("Batal", color = Color(0xFF6B726C))
            }
        }
    )
}

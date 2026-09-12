package com.example.ui.screens

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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.entities.QadhaPrayerEntity
import com.example.reminder.PrayerReminderHelper
import com.example.ui.components.AppHeroHeader
import com.example.ui.viewmodel.FiqihViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrayerAndQadhaScreen(
    viewModel: FiqihViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val prayerSchedule by viewModel.prayerSchedule.collectAsState()
    val remindersEnabled by viewModel.remindersEnabled.collectAsState()
    val qadhaList by viewModel.qadhaPrayers.collectAsState()
    val calculationResult by viewModel.calculationResult.collectAsState()

    var showAddQadhaDialog by remember { mutableStateOf(false) }
    var manualPrayerName by remember { mutableStateOf("Dzuhur") }
    var manualReason by remember { mutableStateOf("Haid datang setelah masuk waktu") }

    val completedCount = qadhaList.count { it.isCompleted }
    val remainingCount = qadhaList.size - completedCount

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("prayer_qadha_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            AppHeroHeader(
                title = "Pengingat Ibadah & Qadha",
                subtitle = "Jadwal shalat, pengingat mandi wajib, & pencatat hutang qadha"
            )
        }

        // 1. Prayer Times Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = "Waktu Shalat Hari Ini",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = prayerSchedule.dateString,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        IconButton(onClick = { viewModel.refreshPrayerTimes() }) {
                            Icon(Icons.Default.Refresh, contentDescription = "Segarkan Jadwal", tint = MaterialTheme.colorScheme.primary)
                        }
                    }

                    // Next Prayer Banner
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    text = "Shalat Berikutnya:",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Text(
                                    text = "${prayerSchedule.nextPrayerName} (${prayerSchedule.nextPrayerTime} WIB)",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                            Icon(
                                imageVector = Icons.Default.Schedule,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    // 5 Prayer Times Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        PrayerTimePill("Subuh", prayerSchedule.subuh)
                        PrayerTimePill("Dzuhur", prayerSchedule.dzuhur)
                        PrayerTimePill("Ashar", prayerSchedule.ashar)
                        PrayerTimePill("Maghrib", prayerSchedule.maghrib)
                        PrayerTimePill("Isya", prayerSchedule.isya)
                    }

                    HorizontalDivider()

                    // Reminder Notification Toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Notifikasi Pengingat Shalat & Mandi",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                            )
                            Text(
                                text = "Mengingatkan saat masuk waktu shalat (bagi mustahadhah & suci) serta saat batas darah berhenti.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = remindersEnabled,
                            onCheckedChange = {
                                viewModel.toggleReminders(it)
                                if (it) {
                                    PrayerReminderHelper.showMandiReminderNotification(
                                        context,
                                        "Pengingat Fiqih Aktif",
                                        "Pengingat waktu shalat & mandi wajib telah diaktifkan sesuai aturan syariat."
                                    )
                                }
                            },
                            modifier = Modifier.testTag("reminders_toggle_switch")
                        )
                    }
                }
            }
        }

        // 2. Mandi Wajib Alert Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.45f)
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.WaterDrop,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Kewajiban Mandi Wajib Terkait",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = calculationResult?.mandiWajibNote
                                ?: "Mandi wajib dilakukan segera setelah darah haid/nifas dipastikan berhenti total (kapas dimasukkan bersih tanpa bercak). Bagi mustahadhah, wudhu diperbarui setiap masuk waktu shalat fardhu.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
            }
        }

        // 3. Qadha Shalat Tracker
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = "Pelacak Hutang Qadha Shalat",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "$remainingCount Belum Diqadha • $completedCount Sudah Selesai",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        FilledTonalButton(
                            onClick = { showAddQadhaDialog = true },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.testTag("add_qadha_button")
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Tambah", fontSize = 12.sp)
                        }
                    }

                    if (qadhaList.isEmpty()) {
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(20.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircleOutline,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(36.dp)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Tidak Ada Hutang Shalat Qadha",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                                )
                                Text(
                                    text = "Shalat yang perlu diqadha akibat datang/berhentinya haid akan otomatis dicatat di sini saat Anda melakukan kalkulasi.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Qadha Items
        items(qadhaList) { prayer ->
            QadhaPrayerCardItem(
                prayer = prayer,
                onToggle = { viewModel.toggleQadhaPrayer(prayer) },
                onDelete = { viewModel.deleteQadhaPrayer(prayer.id) }
            )
        }

        // 4. Puasa Ramadhan Qadha Guide
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.NightsStay,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Hukum Qadha Puasa Ramadhan",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Text(
                        text = "Wanita haid dan nifas HARAM berpuasa, dan seluruh hari puasa Ramadhan yang ditinggalkan WAJIB DIQADHA di luar bulan Ramadhan sebelum datangnya Ramadhan berikutnya. Shalat selama haid sah gugur dan tidak diqadha, sedangkan puasa wajib diqadha (HR. Muslim).",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }

    // Add Qadha Dialog
    if (showAddQadhaDialog) {
        AlertDialog(
            onDismissRequest = { showAddQadhaDialog = false },
            title = { Text("Tambah Catatan Qadha Shalat") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    var expandedP by remember { mutableStateOf(false) }
                    Text("Pilih Shalat:", style = MaterialTheme.typography.labelMedium)
                    Box {
                        OutlinedButton(onClick = { expandedP = true }, modifier = Modifier.fillMaxWidth()) {
                            Text(manualPrayerName)
                            Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                        }
                        DropdownMenu(expanded = expandedP, onDismissRequest = { expandedP = false }) {
                            listOf("Subuh", "Dzuhur", "Ashar", "Maghrib", "Isya").forEach { p ->
                                DropdownMenuItem(
                                    text = { Text(p) },
                                    onClick = {
                                        manualPrayerName = p
                                        expandedP = false
                                    }
                                )
                            }
                        }
                    }

                    Text("Alasan / Catatan:", style = MaterialTheme.typography.labelMedium)
                    OutlinedTextField(
                        value = manualReason,
                        onValueChange = { manualReason = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Contoh: Suci di waktu Ashar") }
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.addManualQadha(manualPrayerName, manualReason)
                        showAddQadhaDialog = false
                    }
                ) {
                    Text("Simpan")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddQadhaDialog = false }) {
                    Text("Batal")
                }
            }
        )
    }
}

@Composable
fun PrayerTimePill(name: String, time: String) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        shape = RoundedCornerShape(10.dp)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = name, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = time,
                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
fun QadhaPrayerCardItem(
    prayer: QadhaPrayerEntity,
    onToggle: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (prayer.isCompleted) {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (prayer.isCompleted) 0.dp else 1.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = prayer.isCompleted,
                onCheckedChange = { onToggle() },
                modifier = Modifier.testTag("qadha_checkbox_${prayer.id}")
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Shalat ${prayer.prayerName}",
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = FontWeight.Bold,
                        textDecoration = if (prayer.isCompleted) TextDecoration.LineThrough else TextDecoration.None
                    ),
                    color = if (prayer.isCompleted) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = prayer.reason,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = prayer.dateString,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Default.DeleteOutline,
                    contentDescription = "Hapus",
                    tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

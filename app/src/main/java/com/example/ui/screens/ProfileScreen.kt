package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.patrykandpatrick.vico.compose.chart.Chart
import com.patrykandpatrick.vico.compose.chart.column.columnChart
import com.patrykandpatrick.vico.compose.axis.horizontal.rememberBottomAxis
import com.patrykandpatrick.vico.compose.axis.vertical.rememberStartAxis
import com.patrykandpatrick.vico.core.entry.entryModelOf
import com.patrykandpatrick.vico.core.entry.FloatEntry
import com.patrykandpatrick.vico.core.axis.AxisPosition
import com.patrykandpatrick.vico.core.axis.formatter.AxisValueFormatter
import com.example.data.entities.CalculationHistoryEntity
import com.example.data.entities.UserAdatProfileEntity
import com.example.model.AvatarTemplate
import com.example.model.PRESET_AVATAR_TEMPLATES
import com.example.ui.viewmodel.FiqihViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    viewModel: FiqihViewModel,
    modifier: Modifier = Modifier
) {
    val userProfile by viewModel.userProfile.collectAsState()
    val historyList by viewModel.calculationHistory.collectAsState()
    val dateFormat = remember { SimpleDateFormat("dd MMM yyyy, HH:mm", Locale("id", "ID")) }
    val shortDateFormat = remember { SimpleDateFormat("dd MMM yyyy", Locale("id", "ID")) }

    var showEditProfileDialog by remember { mutableStateOf(false) }
    var selectedCycle1Id by remember { mutableStateOf<Long?>(null) }
    var selectedCycle2Id by remember { mutableStateOf<Long?>(null) }
    var filterType by remember { mutableStateOf<String?>("ALL") }

    // Auto select the first two cycles if available
    LaunchedEffect(historyList) {
        if (historyList.size >= 2) {
            if (selectedCycle1Id == null || historyList.none { it.id == selectedCycle1Id }) {
                selectedCycle1Id = historyList[1].id // Older cycle
            }
            if (selectedCycle2Id == null || historyList.none { it.id == selectedCycle2Id }) {
                selectedCycle2Id = historyList[0].id // Latest cycle
            }
        }
    }

    val filteredHistory = remember(historyList, filterType) {
        when (filterType) {
            "HAID" -> historyList.filter { it.caseType == "HAID" }
            "NIFAS" -> historyList.filter { it.caseType == "NIFAS" }
            else -> historyList
        }
    }

    val cycle1 = remember(historyList, selectedCycle1Id) {
        historyList.firstOrNull { it.id == selectedCycle1Id }
    }
    val cycle2 = remember(historyList, selectedCycle2Id) {
        historyList.firstOrNull { it.id == selectedCycle2Id }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("profile_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. User Profile Header Card
        item {
            UserProfileCard(
                profile = userProfile,
                onEditClick = { showEditProfileDialog = true }
            )
        }

        // 2. Cycle Comparison Section ("Bandingkan Siklus Antar Periode")
        item {
            CycleComparisonSection(
                historyList = historyList,
                cycle1 = cycle1,
                cycle2 = cycle2,
                shortDateFormat = shortDateFormat,
                onSelectCycle1 = { selectedCycle1Id = it },
                onSelectCycle2 = { selectedCycle2Id = it },
                onSeedSample = { viewModel.seedSampleCyclesIfEmpty() }
            )
        }

        // Vico Chart Dashboard
        item {
            CycleDashboard(historyList = historyList)
        }

        // 3. Cycle History Header & Filter
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Riwayat Siklus Tersimpan",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Tersimpan aman di Room Database (${historyList.size} entri)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (historyList.isNotEmpty()) {
                    var showClearConfirm by remember { mutableStateOf(false) }
                    TextButton(
                        onClick = { showClearConfirm = true },
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) {
                        Icon(Icons.Default.DeleteSweep, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Kosongkan", style = MaterialTheme.typography.labelSmall)
                    }

                    if (showClearConfirm) {
                        AlertDialog(
                            onDismissRequest = { showClearConfirm = false },
                            title = { Text("Hapus Semua Riwayat?") },
                            text = { Text("Semua data riwayat siklus yang tersimpan di Room Database akan dihapus permanen.") },
                            confirmButton = {
                                TextButton(
                                    onClick = {
                                        viewModel.clearAllHistory()
                                        showClearConfirm = false
                                    },
                                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                                ) {
                                    Text("Ya, Hapus Semua")
                                }
                            },
                            dismissButton = {
                                TextButton(onClick = { showClearConfirm = false }) {
                                    Text("Batal")
                                }
                            }
                        )
                    }
                }
            }
        }

        // Filter chips: Semua, Haid, Nifas
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = filterType == "ALL",
                    onClick = { filterType = "ALL" },
                    label = { Text("Semua (${historyList.size})") },
                    leadingIcon = if (filterType == "ALL") {
                        { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                    } else null
                )
                FilterChip(
                    selected = filterType == "HAID",
                    onClick = { filterType = "HAID" },
                    label = { Text("Haid (${historyList.count { it.caseType == "HAID" }})") },
                    leadingIcon = if (filterType == "HAID") {
                        { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                    } else null
                )
                FilterChip(
                    selected = filterType == "NIFAS",
                    onClick = { filterType = "NIFAS" },
                    label = { Text("Nifas (${historyList.count { it.caseType == "NIFAS" }})") },
                    leadingIcon = if (filterType == "NIFAS") {
                        { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                    } else null
                )
            }
        }

        // History items list
        if (filteredHistory.isEmpty()) {
            item {
                EmptyHistoryCard(
                    onGoToCalculator = { viewModel.setTab(0) }
                )
            }
        } else {
            items(filteredHistory, key = { it.id }) { item ->
                HistoryItemCard(
                    item = item,
                    dateFormat = dateFormat,
                    onCompareSelect = {
                        if (selectedCycle1Id == null || selectedCycle1Id == item.id) {
                            selectedCycle1Id = item.id
                        } else {
                            selectedCycle2Id = item.id
                        }
                    },
                    onDelete = { viewModel.deleteHistory(item.id) }
                )
            }
        }
    }

    // Edit Profile & Avatar BottomSheet / Dialog
    if (showEditProfileDialog) {
        EditProfileDialog(
            currentProfile = userProfile,
            onDismiss = { showEditProfileDialog = false },
            onSave = { name, bio, usePersonal, photoUri, avatarIndex, haidDays, suciDays, nifasDays ->
                viewModel.updateUserName(name)
                viewModel.updateUserBio(bio)
                if (usePersonal) {
                    viewModel.setPhotoUri(photoUri)
                } else {
                    viewModel.setAvatarTemplateIndex(avatarIndex)
                }
                viewModel.updateAdatDays(haidDays, suciDays, nifasDays)
                showEditProfileDialog = false
            }
        )
    }
}

// -------------------------------------------------------------
// USER PROFILE HEADER CARD
// -------------------------------------------------------------
@Composable
fun UserProfileCard(
    profile: UserAdatProfileEntity,
    onEditClick: () -> Unit
) {
    val template = PRESET_AVATAR_TEMPLATES.getOrElse(profile.avatarTemplateIndex) {
        PRESET_AVATAR_TEMPLATES[0]
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("user_profile_card"),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Avatar Display
                Box(
                    modifier = Modifier
                        .size(76.dp)
                        .clip(CircleShape)
                        .border(
                            width = 2.5.dp,
                            color = MaterialTheme.colorScheme.primary,
                            shape = CircleShape
                        )
                        .clickable { onEditClick() },
                    contentAlignment = Alignment.Center
                ) {
                    if (profile.usePersonalPhoto && !profile.photoUri.isNullOrBlank()) {
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(profile.photoUri)
                                .crossfade(true)
                                .build(),
                            contentDescription = "Foto Profil",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        // Template Avatar
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.linearGradient(
                                        template.backgroundColors.map { Color(it) }
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = template.initial,
                                style = MaterialTheme.typography.headlineMedium.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color.White
                                )
                            )
                        }
                    }
                }

                // Name & Bio
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = profile.userName.ifBlank { "Muslimah" },
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (!profile.usePersonalPhoto) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                            ) {
                                Text(
                                    text = template.name,
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = profile.userBio.ifBlank { "Menjaga Ibadah Sesuai Mazhab Syafi'i" },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                IconButton(
                    onClick = onEditClick,
                    modifier = Modifier.testTag("edit_profile_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit Profil",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            // Quick Adat Stats Pills
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AdatStatPill(
                    label = "Adat Haid",
                    value = "${profile.usualHaidDays} Hari",
                    modifier = Modifier.weight(1f)
                )
                AdatStatPill(
                    label = "Adat Suci",
                    value = "${profile.usualSuciDays} Hari",
                    modifier = Modifier.weight(1f)
                )
                AdatStatPill(
                    label = "Panjang Siklus",
                    value = "${profile.usualHaidDays + profile.usualSuciDays} Hari",
                    modifier = Modifier.weight(1.2f)
                )
            }
        }
    }
}

@Composable
fun AdatStatPill(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
        tonalElevation = 1.dp
    ) {
        Column(
            modifier = Modifier.padding(vertical = 8.dp, horizontal = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

// -------------------------------------------------------------
// CYCLE COMPARISON SECTION ("Bandingkan Siklus Antar Periode")
// -------------------------------------------------------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CycleComparisonSection(
    historyList: List<CalculationHistoryEntity>,
    cycle1: CalculationHistoryEntity?,
    cycle2: CalculationHistoryEntity?,
    shortDateFormat: SimpleDateFormat,
    onSelectCycle1: (Long) -> Unit,
    onSelectCycle2: (Long) -> Unit,
    onSeedSample: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("cycle_comparison_card"),
        shape = RoundedCornerShape(22.dp),
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
            // Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.CompareArrows,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Column {
                    Text(
                        text = "Bandingkan Siklus Antar Periode",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Analisis komparatif durasi darah, istihadhah, & kesesuaian adat",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (historyList.size < 2) {
                // Not enough data banner
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Memerlukan minimal 2 riwayat siklus untuk membandingkan.",
                            style = MaterialTheme.typography.bodySmall,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        OutlinedButton(
                            onClick = onSeedSample,
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.AutoFixHigh, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Muat Contoh 2 Periode Siklus")
                        }
                    }
                }
            } else {
                // Selectors for Cycle 1 and Cycle 2
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Dropdown Cycle 1 (Older / Reference)
                    CycleSelectorDropdown(
                        label = "Siklus A (Acuan)",
                        selectedCycle = cycle1,
                        historyList = historyList,
                        shortDateFormat = shortDateFormat,
                        onSelect = onSelectCycle1,
                        modifier = Modifier.weight(1f)
                    )

                    // Dropdown Cycle 2 (Newer / Comparison)
                    CycleSelectorDropdown(
                        label = "Siklus B (Pembanding)",
                        selectedCycle = cycle2,
                        historyList = historyList,
                        shortDateFormat = shortDateFormat,
                        onSelect = onSelectCycle2,
                        modifier = Modifier.weight(1f)
                    )
                }

                // Comparison Content
                if (cycle1 != null && cycle2 != null) {
                    CycleComparisonDetail(cycle1 = cycle1, cycle2 = cycle2)
                }
            }
        }
    }
}

@Composable
fun CycleSelectorDropdown(
    label: String,
    selectedCycle: CalculationHistoryEntity?,
    historyList: List<CalculationHistoryEntity>,
    shortDateFormat: SimpleDateFormat,
    onSelect: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    Column(modifier = modifier) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(4.dp))
        Surface(
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = true }
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = selectedCycle?.let { "${it.caseType}: ${shortDateFormat.format(Date(it.startEpochMillis))}" }
                            ?: "Pilih Siklus",
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = selectedCycle?.let { "${it.haidHours / 24}h Haid, ${it.istihadhahHours / 24}h Isti." }
                            ?: "-",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Icon(Icons.Default.ArrowDropDown, contentDescription = null)
            }

            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                historyList.forEach { item ->
                    DropdownMenuItem(
                        text = {
                            Column {
                                Text(
                                    text = "${item.caseType}: ${shortDateFormat.format(Date(item.startEpochMillis))}",
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "${item.statusSummary} (${item.totalDays} hari total)",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        },
                        onClick = {
                            onSelect(item.id)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun CycleComparisonDetail(
    cycle1: CalculationHistoryEntity,
    cycle2: CalculationHistoryEntity
) {
    val totalDays1 = (cycle1.totalHours / 24).toInt()
    val totalDays2 = (cycle2.totalHours / 24).toInt()
    val haidDays1 = (cycle1.haidHours / 24).toInt()
    val haidDays2 = (cycle2.haidHours / 24).toInt()
    val istiDays1 = (cycle1.istihadhahHours / 24).toInt()
    val istiDays2 = (cycle2.istihadhahHours / 24).toInt()

    val diffTotal = totalDays2 - totalDays1
    val diffHaid = haidDays2 - haidDays1
    val diffIsti = istiDays2 - istiDays1

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

        // 1. Total Duration Comparative Bars
        Text(
            text = "1. Perbandingan Durasi Darah Total",
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Siklus A",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "$totalDays1 Hari (${cycle1.totalHours % 24} Jam)",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                )
            }

            Surface(
                shape = RoundedCornerShape(8.dp),
                color = when {
                    diffTotal > 0 -> MaterialTheme.colorScheme.tertiaryContainer
                    diffTotal < 0 -> MaterialTheme.colorScheme.secondaryContainer
                    else -> MaterialTheme.colorScheme.surfaceVariant
                }
            ) {
                Text(
                    text = when {
                        diffTotal > 0 -> "+$diffTotal Hari (Lebih Lama)"
                        diffTotal < 0 -> "$diffTotal Hari (Lebih Singkat)"
                        else -> "Durasi Sama"
                    },
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    color = when {
                        diffTotal > 0 -> MaterialTheme.colorScheme.onTertiaryContainer
                        diffTotal < 0 -> MaterialTheme.colorScheme.onSecondaryContainer
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            }

            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    text = "Siklus B",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "$totalDays2 Hari (${cycle2.totalHours % 24} Jam)",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                )
            }
        }

        // Comparative Visual Stacked Progress Bars
        ComparativeVisualBar(
            label = "Siklus A",
            haidHours = cycle1.haidHours,
            istiHours = cycle1.istihadhahHours
        )
        ComparativeVisualBar(
            label = "Siklus B",
            haidHours = cycle2.haidHours,
            istiHours = cycle2.istihadhahHours
        )

        // 2. Breakdown Matrix
        Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
            )
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Parameter Siklus", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
                    Text("Siklus A", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
                    Text("Siklus B", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                ComparisonRow(label = "Darah Haid Sah", val1 = "$haidDays1 Hari", val2 = "$haidDays2 Hari")
                ComparisonRow(
                    label = "Darah Istihadhah",
                    val1 = if (istiDays1 > 0) "$istiDays1 Hari" else "0 (Bersih)",
                    val2 = if (istiDays2 > 0) "$istiDays2 Hari" else "0 (Bersih)"
                )
                ComparisonRow(
                    label = "Jarak Antar Siklus",
                    val1 = if (cycle1.cycleLengthDays > 0) "${cycle1.cycleLengthDays} Hari" else "-",
                    val2 = if (cycle2.cycleLengthDays > 0) "${cycle2.cycleLengthDays} Hari" else "-"
                )
                ComparisonRow(
                    label = "Golongan Fiqih",
                    val1 = cycle1.categoryName.take(15) + "...",
                    val2 = cycle2.categoryName.take(15) + "..."
                )
            }
        }

        // 3. Fiqih Mazhab Syafi'i Insight Note
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    Icons.Default.Lightbulb,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "Analisis Fiqih Kitab Uyunul Masa-il & Tuhfatun Niswah:",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = when {
                            istiDays2 > 0 && istiDays1 == 0 ->
                                "Pada Siklus B darah keluar melampaui masa normal dan terdapat $istiDays2 hari istihadhah. Pengguna wajib mandi besar setelah hari ke-$haidDays2 dan menjalankan shalat/puasa di masa istihadhah."
                            diffHaid != 0 ->
                                "Terdapat pergeseran durasi haid sebesar ${kotlin.math.abs(diffHaid)} hari. Dalam mazhab Syafi'i, adat kebiasaan baru dapat terbentuk jika siklus ini berulang pada periode berikutnya."
                            else ->
                                "Kedua siklus memiliki pola durasi yang stabil dan konsisten sesuai dengan adat kebiasaan normal Anda."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

@Composable
fun ComparisonRow(label: String, val1: String, val2: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1.3f))
        Text(text = val1, style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold), modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
        Text(text = val2, style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold), modifier = Modifier.weight(1f), textAlign = TextAlign.End)
    }
}

@Composable
fun ComparativeVisualBar(
    label: String,
    haidHours: Long,
    istiHours: Long
) {
    val total = (haidHours + istiHours).coerceAtLeast(1L).toFloat()
    val haidRatio = (haidHours.toFloat() / total).coerceIn(0f, 1f)
    val istiRatio = (istiHours.toFloat() / total).coerceIn(0f, 1f)

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = label, style = MaterialTheme.typography.labelSmall)
            Text(
                text = "Haid: ${haidHours / 24}h ${haidHours % 24}j | Isti: ${istiHours / 24}h ${istiHours % 24}j",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(12.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            if (haidRatio > 0) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .weight(haidRatio)
                        .background(MaterialTheme.colorScheme.primary)
                )
            }
            if (istiRatio > 0) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .weight(istiRatio)
                        .background(MaterialTheme.colorScheme.error)
                )
            }
        }
    }
}

// -------------------------------------------------------------
// HISTORY ITEM CARD
// -------------------------------------------------------------
@Composable
fun HistoryItemCard(
    item: CalculationHistoryEntity,
    dateFormat: SimpleDateFormat,
    onCompareSelect: () -> Unit,
    onDelete: () -> Unit
) {
    var showDeleteConfirm by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("history_item_${item.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Header with case badge & date
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (item.caseType == "HAID") {
                            MaterialTheme.colorScheme.primaryContainer
                        } else {
                            MaterialTheme.colorScheme.tertiaryContainer
                        }
                    ) {
                        Text(
                            text = item.caseType,
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = if (item.caseType == "HAID") {
                                MaterialTheme.colorScheme.onPrimaryContainer
                            } else {
                                MaterialTheme.colorScheme.onTertiaryContainer
                            },
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }

                    Text(
                        text = dateFormat.format(Date(item.startEpochMillis)),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                IconButton(
                    onClick = { showDeleteConfirm = true },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteOutline,
                        contentDescription = "Hapus Riwayat",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            // Summary & Category
            Text(
                text = item.statusSummary,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )

            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            ) {
                Text(
                    text = item.categoryName,
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }

            if (item.categoryDetectionReason.isNotBlank()) {
                Text(
                    text = item.categoryDetectionReason,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Duration stats row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        Text("Haid Sah", style = MaterialTheme.typography.labelSmall)
                        Text(
                            "${item.haidHours / 24}h ${item.haidHours % 24}j",
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                if (item.istihadhahHours > 0) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.error.copy(alpha = 0.08f),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(modifier = Modifier.padding(8.dp)) {
                            Text("Istihadhah", style = MaterialTheme.typography.labelSmall)
                            Text(
                                "${item.istihadhahHours / 24}h ${item.istihadhahHours % 24}j",
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }

                if (item.cycleLengthDays > 0) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.08f),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(modifier = Modifier.padding(8.dp)) {
                            Text("Jarak Siklus", style = MaterialTheme.typography.labelSmall)
                            Text(
                                "${item.cycleLengthDays} Hari",
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.tertiary
                            )
                        }
                    }
                }
            }

            // Compare action button
            OutlinedButton(
                onClick = onCompareSelect,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(Icons.Default.CompareArrows, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Pilih Siklus Ini untuk Dibandingkan", style = MaterialTheme.typography.labelMedium)
            }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Hapus Riwayat Ini?") },
            text = { Text("Riwayat perhitungan siklus ini akan dihapus dari Room Database.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDelete()
                        showDeleteConfirm = false
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Hapus")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Batal")
                }
            }
        )
    }
}

// -------------------------------------------------------------
// EMPTY HISTORY CARD
// -------------------------------------------------------------
@Composable
fun EmptyHistoryCard(
    onGoToCalculator: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = Icons.Default.EventNote,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(44.dp)
            )
            Text(
                text = "Belum Ada Riwayat Siklus Tersimpan",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                textAlign = TextAlign.Center
            )
            Text(
                text = "Hitung siklus haid atau nifas Anda di Kalkulator untuk menyimpan data ke Room Database dan membandingkan polanya.",
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Button(
                onClick = onGoToCalculator,
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Calculate, contentDescription = null)
                Spacer(modifier = Modifier.width(6.dp))
                Text("Buka Kalkulator")
            }
        }
    }
}

// -------------------------------------------------------------
// EDIT PROFILE & AVATAR DIALOG
// -------------------------------------------------------------
@Composable
fun EditProfileDialog(
    currentProfile: UserAdatProfileEntity,
    onDismiss: () -> Unit,
    onSave: (
        name: String,
        bio: String,
        usePersonalPhoto: Boolean,
        photoUri: String?,
        avatarIndex: Int,
        haidDays: Int,
        suciDays: Int,
        nifasDays: Int
    ) -> Unit
) {
    var name by remember { mutableStateOf(currentProfile.userName) }
    var bio by remember { mutableStateOf(currentProfile.userBio) }
    var usePersonalPhoto by remember { mutableStateOf(currentProfile.usePersonalPhoto) }
    var personalPhotoUri by remember { mutableStateOf(currentProfile.photoUri) }
    var selectedAvatarIndex by remember { mutableStateOf(currentProfile.avatarTemplateIndex) }

    var haidDays by remember { mutableStateOf(currentProfile.usualHaidDays) }
    var suciDays by remember { mutableStateOf(currentProfile.usualSuciDays) }
    var nifasDays by remember { mutableStateOf(currentProfile.usualNifasDays) }

    // Photo picker launcher (Android standard zero-permission Photo Picker)
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            personalPhotoUri = uri.toString()
            usePersonalPhoto = true
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Edit Profil & Avatar",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
            )
        },
        text = {
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Name input
                item {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Nama Pengguna") },
                        placeholder = { Text("Contoh: Siti Aisyah") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("profile_name_input"),
                        shape = RoundedCornerShape(12.dp)
                    )
                }

                // Bio input
                item {
                    OutlinedTextField(
                        value = bio,
                        onValueChange = { bio = it },
                        label = { Text("Catatan / Bio Fiqih") },
                        placeholder = { Text("Contoh: Menjaga Ibadah Sesuai Mazhab Syafi'i") },
                        maxLines = 2,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                }

                // Photo Selection Options
                item {
                    Text(
                        text = "Pilihan Foto Profil:",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                // Mode Toggle: Template Avatar vs Personal Photo
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = !usePersonalPhoto,
                            onClick = { usePersonalPhoto = false },
                            label = { Text("Template Avatar") },
                            leadingIcon = if (!usePersonalPhoto) {
                                { Icon(Icons.Default.Check, contentDescription = null) }
                            } else null,
                            modifier = Modifier.weight(1f)
                        )
                        FilterChip(
                            selected = usePersonalPhoto,
                            onClick = { usePersonalPhoto = true },
                            label = { Text("Foto Pribadi") },
                            leadingIcon = if (usePersonalPhoto) {
                                { Icon(Icons.Default.Check, contentDescription = null) }
                            } else null,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // Content based on selected photo mode
                if (!usePersonalPhoto) {
                    // Grid of preset avatar templates
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text(
                                text = "Pilih Template Karakter Muslimah:",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                PRESET_AVATAR_TEMPLATES.chunked(3).forEach { rowItems ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        rowItems.forEach { tmpl ->
                                            AvatarTemplateItem(
                                                template = tmpl,
                                                isSelected = selectedAvatarIndex == tmpl.id,
                                                onClick = {
                                                    selectedAvatarIndex = tmpl.id
                                                    usePersonalPhoto = false
                                                },
                                                modifier = Modifier.weight(1f)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else {
                    // Personal Photo Picker
                    item {
                        Card(
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                            )
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                if (!personalPhotoUri.isNullOrBlank()) {
                                    Box(
                                        modifier = Modifier
                                            .size(72.dp)
                                            .clip(CircleShape)
                                            .border(2.dp, MaterialTheme.colorScheme.primary, CircleShape)
                                    ) {
                                        AsyncImage(
                                            model = ImageRequest.Builder(LocalContext.current)
                                                .data(personalPhotoUri)
                                                .crossfade(true)
                                                .build(),
                                            contentDescription = "Pratinjau Foto",
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    }
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.AddPhotoAlternate,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(48.dp)
                                    )
                                }

                                Button(
                                    onClick = {
                                        photoPickerLauncher.launch(
                                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                        )
                                    },
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Icon(Icons.Default.PhotoLibrary, contentDescription = null)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(if (personalPhotoUri != null) "Ganti Foto dari Galeri" else "Pilih Foto dari Galeri")
                                }
                            }
                        }
                    }
                }

                // Default Adat Habit Settings
                item {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    Text(
                        text = "Pengaturan Kebiasaan (Adat Fiqih):",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                item {
                    CounterSettingRow(
                        label = "Kebiasaan Durasi Haid",
                        value = haidDays,
                        unit = "Hari",
                        min = 1,
                        max = 15,
                        onValueChange = { haidDays = it }
                    )
                }

                item {
                    CounterSettingRow(
                        label = "Kebiasaan Durasi Masa Suci",
                        value = suciDays,
                        unit = "Hari",
                        min = 15,
                        max = 60,
                        onValueChange = { suciDays = it }
                    )
                }

                item {
                    CounterSettingRow(
                        label = "Kebiasaan Durasi Nifas",
                        value = nifasDays,
                        unit = "Hari",
                        min = 1,
                        max = 60,
                        onValueChange = { nifasDays = it }
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(
                        name,
                        bio,
                        usePersonalPhoto,
                        personalPhotoUri,
                        selectedAvatarIndex,
                        haidDays,
                        suciDays,
                        nifasDays
                    )
                },
                modifier = Modifier.testTag("save_profile_button"),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("Simpan Profil")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Batal")
            }
        }
    )
}

@Composable
fun AvatarTemplateItem(
    template: AvatarTemplate,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) {
            MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
        } else {
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        },
        border = BorderStroke(
            width = if (isSelected) 2.dp else 1.dp,
            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            template.backgroundColors.map { Color(it) }
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = template.initial,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                )
            }

            Text(
                text = template.name,
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun CounterSettingRow(
    label: String,
    value: Int,
    unit: String,
    min: Int,
    max: Int,
    onValueChange: (Int) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = label, style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold))
            Text(text = "$value $unit", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            IconButton(
                onClick = { if (value > min) onValueChange(value - 1) },
                enabled = value > min,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(Icons.Default.RemoveCircleOutline, contentDescription = "Kurang")
            }

            Text(
                text = "$value",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                modifier = Modifier.widthIn(min = 28.dp),
                textAlign = TextAlign.Center
            )

            IconButton(
                onClick = { if (value < max) onValueChange(value + 1) },
                enabled = value < max,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(Icons.Default.AddCircleOutline, contentDescription = "Tambah")
            }
        }
    }
}

@Composable
fun CycleDashboard(historyList: List<CalculationHistoryEntity>) {
    val haidCycles = remember(historyList) {
        historyList
            .filter { it.caseType == "HAID" }
            .sortedBy { it.startEpochMillis } // Sort chronologically (oldest to newest)
            .takeLast(7) // Last 7 cycles
    }

    if (haidCycles.size < 2) return // Need at least 2 cycles to show a trend

    val durations = remember(haidCycles) {
        haidCycles.map { it.haidHours / 24f }
    }
    
    val chartEntryModel = remember(durations) {
        val entries = durations.mapIndexed { index, duration ->
            FloatEntry(x = index.toFloat(), y = duration)
        }
        entryModelOf(entries)
    }

    val bottomAxisFormatter = remember(haidCycles) {
        AxisValueFormatter<AxisPosition.Horizontal.Bottom> { value, _ ->
            val index = value.toInt()
            if (index in haidCycles.indices) {
                val cal = Calendar.getInstance().apply { timeInMillis = haidCycles[index].startEpochMillis }
                SimpleDateFormat("dd MMM", Locale("id", "ID")).format(cal.time)
            } else ""
        }
    }

    val startAxisFormatter = remember {
        AxisValueFormatter<AxisPosition.Vertical.Start> { value, _ ->
            "${value.toInt()}h"
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Tren Siklus Haid",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "Grafik lamanya haid (hari) dari siklus-siklus terakhir.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(16.dp))
            
            Chart(
                chart = columnChart(),
                model = chartEntryModel,
                startAxis = rememberStartAxis(valueFormatter = startAxisFormatter),
                bottomAxis = rememberBottomAxis(valueFormatter = bottomAxisFormatter),
                modifier = Modifier.fillMaxWidth().height(200.dp)
            )
        }
    }
}

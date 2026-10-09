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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.tour.tourTarget
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.data.entities.CalculationHistoryEntity
import com.example.data.entities.UserAdatProfileEntity
import com.example.model.AvatarTemplate
import com.example.model.PRESET_AVATAR_TEMPLATES
import com.example.ui.components.ThemeSettingsCard
import com.example.ui.components.FeedbackDialog
import com.example.ui.components.SupportDialog
import com.example.ui.theme.EmeraldDeep
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.GoldTertiary
import com.example.ui.viewmodel.FiqihViewModel
import com.patrykandpatrick.vico.compose.axis.horizontal.rememberBottomAxis
import com.patrykandpatrick.vico.compose.axis.vertical.rememberStartAxis
import com.patrykandpatrick.vico.compose.chart.Chart
import com.patrykandpatrick.vico.compose.chart.column.columnChart
import com.patrykandpatrick.vico.core.axis.AxisPosition
import com.patrykandpatrick.vico.core.axis.formatter.AxisValueFormatter
import com.patrykandpatrick.vico.core.entry.FloatEntry
import com.patrykandpatrick.vico.core.entry.entryModelOf
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    viewModel: FiqihViewModel,
    modifier: Modifier = Modifier,
    onStartGuidedTour: (() -> Unit)? = null
) {
    val userProfile by viewModel.userProfile.collectAsState()
    val historyList by viewModel.calculationHistory.collectAsState()
    val dateFormat = remember { SimpleDateFormat("dd MMM yyyy, HH:mm", Locale("id", "ID")) }
    val shortDateFormat = remember { SimpleDateFormat("dd MMM yyyy", Locale("id", "ID")) }

    var showEditProfileDialog by remember { mutableStateOf(false) }
    var showFeedbackDialog by remember { mutableStateOf(false) }
    var showSupportDialog by remember { mutableStateOf(false) }
    var selectedCycle1Id by remember { mutableStateOf<Long?>(null) }
    var selectedCycle2Id by remember { mutableStateOf<Long?>(null) }
    var filterType by remember { mutableStateOf("ALL") }
    var currentSubTab by remember { mutableIntStateOf(0) } // 0 = Riwayat, 1 = Bandingkan, 2 = Tren, 3 = Tema

    // Otomatis memilih 2 siklus terbaru untuk perbandingan jika tersedia
    LaunchedEffect(historyList) {
        if (historyList.size >= 2) {
            if (selectedCycle1Id == null || historyList.none { it.id == selectedCycle1Id }) {
                selectedCycle1Id = historyList[1].id
            }
            if (selectedCycle2Id == null || historyList.none { it.id == selectedCycle2Id }) {
                selectedCycle2Id = historyList[0].id
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

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFFAF7F2))
            .testTag("profile_screen")
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Simple Page Header (No large banner)
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "Profil & Riwayat",
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 22.sp,
                            letterSpacing = (-0.3).sp
                        ),
                        color = Color(0xFF1E211F)
                    )
                    Text(
                        text = "Pengaturan profil pribadi, riwayat siklus, dan preferensi tema",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontSize = 13.sp,
                            lineHeight = 19.sp
                        ),
                        color = Color(0xFF6B726C)
                    )
                }
            }

            // 2. Kartu Profil Pengguna & Kebiasaan Adat
            item {
                UserProfileCard(
                    profile = userProfile,
                    onEditClick = { showEditProfileDialog = true },
                    onThemeClick = { currentSubTab = 3 }
                )
            }

            // Panduan Tur Interaktif
            item {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onStartGuidedTour?.invoke() }
                        .testTag("profile_open_guide_card"),
                    shape = RoundedCornerShape(18.dp),
                    color = Color.White,
                    border = BorderStroke(1.dp, Color(0xFFEBE5DC)),
                    shadowElevation = 1.dp
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFE8F4EE)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.HelpOutline,
                                    contentDescription = null,
                                    tint = EmeraldPrimary,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = "Tur Panduan Interaktif",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    ),
                                    color = Color(0xFF1E211F)
                                )
                                Text(
                                    text = "Panduan visual langkah demi langkah untuk seluruh fitur",
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                                    color = Color(0xFF6B726C)
                                )
                            }
                        }
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            tint = EmeraldPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            // 2. Sub-Tab Navigasi: Riwayat, Bandingkan, Tren, Tema
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("profile_subtabs_row"),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val subtabs = listOf(
                        0 to "Riwayat (${historyList.size})",
                        1 to "Bandingkan",
                        2 to "Tren",
                        3 to "Tema"
                    )
                    subtabs.forEach { (index, title) ->
                        val isSelected = currentSubTab == index
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clickable { currentSubTab = index }
                                .testTag(when(index) { 0 -> "tab_history" 1 -> "tab_compare" 2 -> "tab_trend" else -> "tab_theme" }),
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) EmeraldPrimary else Color.White,
                            border = BorderStroke(
                                1.dp,
                                if (isSelected) GoldTertiary.copy(alpha = 0.6f) else Color(0xFFEBE5DC)
                            ),
                            shadowElevation = if (isSelected) 1.dp else 0.dp
                        ) {
                            Box(
                                modifier = Modifier.padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = title,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        fontSize = 11.5.sp
                                    ),
                                    color = if (isSelected) Color.White else Color(0xFF454B46),
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }

            // 3. Tampilan Konten Sesuai Sub-Tab
            when (currentSubTab) {
                0 -> {
                    // TAB RIWAYAT
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                FilterChip(
                                    selected = filterType == "ALL",
                                    onClick = { filterType = "ALL" },
                                    label = { Text("Semua (${historyList.size})", fontSize = 12.sp) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = EmeraldPrimary,
                                        selectedLabelColor = Color.White
                                    ),
                                    border = FilterChipDefaults.filterChipBorder(
                                        enabled = true,
                                        selected = filterType == "ALL",
                                        borderColor = if (filterType == "ALL") EmeraldPrimary else Color(0xFFEBE5DC)
                                    )
                                )
                                FilterChip(
                                    selected = filterType == "HAID",
                                    onClick = { filterType = "HAID" },
                                    label = { Text("Haid (${historyList.count { it.caseType == "HAID" }})", fontSize = 12.sp) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = EmeraldPrimary,
                                        selectedLabelColor = Color.White
                                    ),
                                    border = FilterChipDefaults.filterChipBorder(
                                        enabled = true,
                                        selected = filterType == "HAID",
                                        borderColor = if (filterType == "HAID") EmeraldPrimary else Color(0xFFEBE5DC)
                                    )
                                )
                                FilterChip(
                                    selected = filterType == "NIFAS",
                                    onClick = { filterType = "NIFAS" },
                                    label = { Text("Nifas (${historyList.count { it.caseType == "NIFAS" }})", fontSize = 12.sp) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = EmeraldPrimary,
                                        selectedLabelColor = Color.White
                                    ),
                                    border = FilterChipDefaults.filterChipBorder(
                                        enabled = true,
                                        selected = filterType == "NIFAS",
                                        borderColor = if (filterType == "NIFAS") EmeraldPrimary else Color(0xFFEBE5DC)
                                    )
                                )
                            }

                            if (historyList.isNotEmpty()) {
                                var showClearConfirm by remember { mutableStateOf(false) }
                                IconButton(onClick = { showClearConfirm = true }) {
                                    Icon(
                                        Icons.Default.DeleteSweep,
                                        contentDescription = "Hapus Semua Riwayat",
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                }

                                if (showClearConfirm) {
                                    AlertDialog(
                                        onDismissRequest = { showClearConfirm = false },
                                        containerColor = Color.White,
                                        title = { Text("Hapus Semua Riwayat?", fontWeight = FontWeight.Bold, color = Color(0xFF1E211F)) },
                                        text = { Text("Semua data riwayat perhitungan siklus akan dihapus permanen dari penyimpanan lokal.", color = Color(0xFF454B46)) },
                                        confirmButton = {
                                            TextButton(
                                                onClick = {
                                                    viewModel.clearAllHistory()
                                                    showClearConfirm = false
                                                },
                                                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                                            ) {
                                                Text("Hapus Semua", fontWeight = FontWeight.Bold)
                                            }
                                        },
                                        dismissButton = {
                                            TextButton(onClick = { showClearConfirm = false }) {
                                                Text("Batal", color = Color(0xFF6B726C))
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }

                    if (filteredHistory.isEmpty()) {
                        item {
                            EmptyHistoryCard(onGoToCalculator = { viewModel.setTab(0) })
                        }
                    } else {
                        items(filteredHistory, key = { it.id }) { item ->
                            HistoryItemCard(
                                item = item,
                                dateFormat = dateFormat,
                                onCompareSelect = {
                                    selectedCycle1Id = item.id
                                    currentSubTab = 1
                                },
                                onDelete = { viewModel.deleteHistory(item.id) }
                            )
                        }
                    }
                }

                1 -> {
                    // TAB BANDINGKAN SIKLUS
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
                }

                2 -> {
                    // TAB TREN SIKLUS
                    item {
                        CycleDashboard(
                            historyList = historyList,
                            onSeedSample = { viewModel.seedSampleCyclesIfEmpty() }
                        )
                    }
                }

                3 -> {
                    // TAB TEMA & PALET WARNA DINAMIS
                    item {
                        val currentPalette by viewModel.themePalette.collectAsState()
                        val currentMode by viewModel.themeMode.collectAsState()
                        ThemeSettingsCard(
                            currentPalette = currentPalette,
                            currentMode = currentMode,
                            onPaletteChange = { viewModel.setThemePalette(it) },
                            onModeChange = { viewModel.setThemeMode(it) }
                        )
                    }
                }
            }

            // Komunikasi Resmi & Dukungan Partisipasi
            item {
                Spacer(modifier = Modifier.height(4.dp))
                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 8.dp),
                    color = Color(0xFFEBE5DC)
                )
                Text(
                    text = "Komunikasi & Dukungan",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = Color(0xFF6B726C)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(14.dp))
                            .clickable { showFeedbackDialog = true }
                            .testTag("profile_btn_feedback"),
                        shape = RoundedCornerShape(14.dp),
                        color = Color.White,
                        border = BorderStroke(1.dp, Color(0xFFEBE5DC)),
                        shadowElevation = 0.5.dp
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.RateReview,
                                contentDescription = null,
                                tint = EmeraldPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "Kritik & Saran",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = Color(0xFF1E211F)
                            )
                        }
                    }

                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(14.dp))
                            .clickable { showSupportDialog = true }
                            .testTag("profile_btn_support"),
                        shape = RoundedCornerShape(14.dp),
                        color = Color.White,
                        border = BorderStroke(1.dp, Color(0xFFEBE5DC)),
                        shadowElevation = 0.5.dp
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Favorite,
                                contentDescription = null,
                                tint = EmeraldPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "Dukungan Infaq",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = Color(0xFF1E211F)
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    // Dialog Edit Profil
    if (showEditProfileDialog) {
        EditProfileDialog(
            currentProfile = userProfile,
            onDismiss = { showEditProfileDialog = false },
            onSave = { name, bio, usePersonal, photoUri, avatarIndex, haidDays, suciDays, nifasDays ->
                viewModel.saveFullProfile(
                    name = name,
                    bio = bio,
                    usePersonalPhoto = usePersonal,
                    photoUri = photoUri,
                    avatarIndex = avatarIndex,
                    haidDays = haidDays,
                    suciDays = suciDays,
                    nifasDays = nifasDays
                )
                showEditProfileDialog = false
            }
        )
    }

    if (showFeedbackDialog) {
        FeedbackDialog(
            onDismissRequest = { showFeedbackDialog = false }
        )
    }

    if (showSupportDialog) {
        SupportDialog(
            onDismissRequest = { showSupportDialog = false }
        )
    }
}

// -------------------------------------------------------------
// USER PROFILE HEADER CARD
// -------------------------------------------------------------
@Composable
fun UserProfileCard(
    profile: UserAdatProfileEntity,
    onEditClick: () -> Unit,
    onThemeClick: () -> Unit = {}
) {
    val template = PRESET_AVATAR_TEMPLATES.getOrElse(profile.avatarTemplateIndex) {
        PRESET_AVATAR_TEMPLATES[0]
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("user_profile_card")
            .tourTarget("tour_profile_data"),
        shape = RoundedCornerShape(20.dp),
        color = Color.White,
        border = BorderStroke(1.dp, Color(0xFFEBE5DC)),
        shadowElevation = 1.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Avatar
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .border(2.dp, GoldTertiary, CircleShape)
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
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.linearGradient(template.backgroundColors.map { Color(it) })
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = template.initial,
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            )
                        }
                    }
                }

                // Nama & Bio
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = profile.userName.ifBlank { "Muslimah" },
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp
                        ),
                        color = Color(0xFF1E211F)
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = profile.userBio.ifBlank { "Mazhab Syafi'i" },
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF6B726C),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    IconButton(
                        onClick = onThemeClick,
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFFAF7F2))
                            .testTag("profile_theme_shortcut_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Palette,
                            contentDescription = "Pilih Tema",
                            tint = EmeraldPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Button(
                        onClick = onEditClick,
                        modifier = Modifier.testTag("edit_profile_button"),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = EmeraldPrimary,
                            contentColor = Color.White
                        )
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Edit", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
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
                    label = "Adat Nifas",
                    value = "${profile.usualNifasDays} Hari",
                    modifier = Modifier.weight(1f)
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
        color = Color(0xFFFAF7F2),
        border = BorderStroke(1.dp, Color(0xFFEBE5DC))
    ) {
        Column(
            modifier = Modifier.padding(vertical = 8.dp, horizontal = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = Color(0xFF6B726C),
                fontSize = 11.sp
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                color = EmeraldPrimary
            )
        }
    }
}

// -------------------------------------------------------------
// CYCLE COMPARISON SECTION
// -------------------------------------------------------------
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
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("cycle_comparison_card"),
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
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFE8F4EE)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.CompareArrows, contentDescription = null, tint = EmeraldPrimary, modifier = Modifier.size(20.dp))
                }
                Column {
                    Text(
                        text = "Bandingkan Siklus Antar Periode",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = Color(0xFF1E211F)
                    )
                    Text(
                        text = "Bandingkan durasi haid, istihadhah, & kesesuaian adat",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF6B726C)
                    )
                }
            }

            if (historyList.size < 2) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Color(0xFFFAF7F2),
                    border = BorderStroke(1.dp, Color(0xFFEBE5DC)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "Memerlukan minimal 2 riwayat siklus untuk membandingkan.",
                            style = MaterialTheme.typography.bodySmall,
                            textAlign = TextAlign.Center,
                            color = Color(0xFF6B726C)
                        )
                        OutlinedButton(
                            onClick = onSeedSample,
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, EmeraldPrimary)
                        ) {
                            Icon(Icons.Default.AutoFixHigh, contentDescription = null, modifier = Modifier.size(16.dp), tint = EmeraldPrimary)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Muat Contoh 2 Periode Siklus", color = EmeraldPrimary, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CycleSelectorDropdown(
                        label = "Siklus A (Acuan)",
                        selectedCycle = cycle1,
                        historyList = historyList,
                        shortDateFormat = shortDateFormat,
                        onSelect = onSelectCycle1,
                        modifier = Modifier.weight(1f)
                    )
                    CycleSelectorDropdown(
                        label = "Siklus B (Pembanding)",
                        selectedCycle = cycle2,
                        historyList = historyList,
                        shortDateFormat = shortDateFormat,
                        onSelect = onSelectCycle2,
                        modifier = Modifier.weight(1f)
                    )
                }

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
            color = Color(0xFF6B726C)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Surface(
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, Color(0xFFEBE5DC)),
            color = Color(0xFFFAF7F2),
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = true }
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = selectedCycle?.let { "${it.caseType}: ${shortDateFormat.format(Date(it.startEpochMillis))}" }
                            ?: "Pilih Siklus",
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = Color(0xFF1E211F)
                    )
                    Text(
                        text = selectedCycle?.let { "${it.haidHours / 24}h Haid, ${it.istihadhahHours / 24}h Isti." }
                            ?: "-",
                        style = MaterialTheme.typography.labelSmall,
                        color = EmeraldPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = EmeraldPrimary)
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
                                    color = Color(0xFF6B726C)
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

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        HorizontalDivider(color = Color(0xFFEBE5DC))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Siklus A", style = MaterialTheme.typography.labelSmall, color = Color(0xFF6B726C))
                Text("$totalDays1 Hari", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold), color = Color(0xFF1E211F))
            }

            Surface(
                shape = RoundedCornerShape(8.dp),
                color = when {
                    diffTotal > 0 -> Color(0xFFE8F4EE)
                    diffTotal < 0 -> Color(0xFFFBE9E7)
                    else -> Color(0xFFFAF7F2)
                },
                border = BorderStroke(0.5.dp, Color(0xFFEBE5DC))
            ) {
                Text(
                    text = when {
                        diffTotal > 0 -> "+$diffTotal Hari"
                        diffTotal < 0 -> "$diffTotal Hari"
                        else -> "Sama"
                    },
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    color = when {
                        diffTotal > 0 -> EmeraldPrimary
                        diffTotal < 0 -> Color(0xFFD32F2F)
                        else -> Color(0xFF6B726C)
                    }
                )
            }

            Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.End) {
                Text("Siklus B", style = MaterialTheme.typography.labelSmall, color = Color(0xFF6B726C))
                Text("$totalDays2 Hari", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold), color = Color(0xFF1E211F))
            }
        }

        ComparativeVisualBar(label = "Siklus A", haidHours = cycle1.haidHours, istiHours = cycle1.istihadhahHours)
        ComparativeVisualBar(label = "Siklus B", haidHours = cycle2.haidHours, istiHours = cycle2.istihadhahHours)

        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFFAF7F2)),
            border = BorderStroke(1.dp, Color(0xFFEBE5DC))
        ) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                ComparisonRow(label = "Darah Haid Sah", val1 = "$haidDays1 Hari", val2 = "$haidDays2 Hari")
                ComparisonRow(
                    label = "Darah Istihadhah",
                    val1 = if (istiDays1 > 0) "$istiDays1 Hari" else "0 Hari",
                    val2 = if (istiDays2 > 0) "$istiDays2 Hari" else "0 Hari"
                )
                ComparisonRow(
                    label = "Jarak Antar Siklus",
                    val1 = if (cycle1.cycleLengthDays > 0) "${cycle1.cycleLengthDays} Hari" else "-",
                    val2 = if (cycle2.cycleLengthDays > 0) "${cycle2.cycleLengthDays} Hari" else "-"
                )
            }
        }

        Surface(
            shape = RoundedCornerShape(12.dp),
            color = Color(0xFFE8F4EE),
            border = BorderStroke(0.5.dp, EmeraldPrimary.copy(alpha = 0.3f))
        ) {
            Row(
                modifier = Modifier.padding(10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Default.Lightbulb, contentDescription = null, tint = EmeraldPrimary, modifier = Modifier.size(18.dp))
                Text(
                    text = when {
                        istiDays2 > 0 && istiDays1 == 0 ->
                            "Pada Siklus B terdapat $istiDays2 hari istihadhah. Wajib mandi besar setelah hari ke-$haidDays2 dan menjalankan shalat."
                        diffHaid != 0 ->
                            "Pergeseran durasi haid sebesar ${kotlin.math.abs(diffHaid)} hari. Adat baru terbentuk bila siklus ini berulang."
                        else ->
                            "Kedua siklus memiliki pola durasi haid yang stabil dan sesuai adat kebiasaan Anda."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = EmeraldDeep
                )
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
        Text(text = label, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1.3f), color = Color(0xFF454B46))
        Text(text = val1, style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold), modifier = Modifier.weight(1f), textAlign = TextAlign.Center, color = Color(0xFF1E211F))
        Text(text = val2, style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold), modifier = Modifier.weight(1f), textAlign = TextAlign.End, color = Color(0xFF1E211F))
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

    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = label, style = MaterialTheme.typography.labelSmall, color = Color(0xFF6B726C))
            Text(
                text = "Haid: ${haidHours / 24}h | Isti: ${istiHours / 24}h",
                style = MaterialTheme.typography.labelSmall,
                color = EmeraldPrimary,
                fontWeight = FontWeight.Medium
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(10.dp)
                .clip(RoundedCornerShape(5.dp))
                .background(Color(0xFFEBE5DC))
        ) {
            if (haidRatio > 0) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .weight(haidRatio)
                        .background(EmeraldPrimary)
                )
            }
            if (istiRatio > 0) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .weight(istiRatio)
                        .background(Color(0xFFD32F2F))
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

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("history_item_${item.id}"),
        shape = RoundedCornerShape(16.dp),
        color = Color.White,
        border = BorderStroke(1.dp, Color(0xFFEBE5DC)),
        shadowElevation = 0.5.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (item.caseType == "HAID") Color(0xFFE8F4EE) else Color(0xFFFFF8E1),
                        border = BorderStroke(0.5.dp, if (item.caseType == "HAID") EmeraldPrimary.copy(alpha = 0.4f) else GoldTertiary.copy(alpha = 0.5f))
                    ) {
                        Text(
                            text = item.caseType,
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = if (item.caseType == "HAID") EmeraldDeep else Color(0xFFB78103),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }

                    Text(
                        text = dateFormat.format(Date(item.startEpochMillis)),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF6B726C)
                    )
                }

                IconButton(
                    onClick = { showDeleteConfirm = true },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteOutline,
                        contentDescription = "Hapus Riwayat",
                        tint = Color(0xFFD32F2F),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Text(
                text = item.statusSummary,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                color = Color(0xFF1E211F)
            )

            Text(
                text = item.categoryName,
                style = MaterialTheme.typography.labelSmall,
                color = EmeraldPrimary,
                fontWeight = FontWeight.Medium
            )

            // Duration stats
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFFFAF7F2),
                    border = BorderStroke(1.dp, Color(0xFFEBE5DC)),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        Text("Haid Sah", style = MaterialTheme.typography.labelSmall, color = Color(0xFF6B726C))
                        Text(
                            "${item.haidHours / 24}h ${item.haidHours % 24}j",
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                            color = EmeraldPrimary
                        )
                    }
                }

                if (item.istihadhahHours > 0) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFFFAF7F2),
                        border = BorderStroke(1.dp, Color(0xFFEBE5DC)),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(modifier = Modifier.padding(8.dp)) {
                            Text("Istihadhah", style = MaterialTheme.typography.labelSmall, color = Color(0xFF6B726C))
                            Text(
                                "${item.istihadhahHours / 24}h ${item.istihadhahHours % 24}j",
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                color = Color(0xFFD32F2F)
                            )
                        }
                    }
                }

                if (item.cycleLengthDays > 0) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFFFAF7F2),
                        border = BorderStroke(1.dp, Color(0xFFEBE5DC)),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(modifier = Modifier.padding(8.dp)) {
                            Text("Jarak Siklus", style = MaterialTheme.typography.labelSmall, color = Color(0xFF6B726C))
                            Text(
                                "${item.cycleLengthDays} Hari",
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                color = GoldTertiary
                            )
                        }
                    }
                }
            }

            OutlinedButton(
                onClick = onCompareSelect,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, EmeraldPrimary.copy(alpha = 0.5f)),
                contentPadding = PaddingValues(vertical = 6.dp)
            ) {
                Icon(Icons.Default.CompareArrows, contentDescription = null, modifier = Modifier.size(16.dp), tint = EmeraldPrimary)
                Spacer(modifier = Modifier.width(6.dp))
                Text("Bandingkan Siklus Ini", style = MaterialTheme.typography.labelSmall.copy(color = EmeraldPrimary, fontWeight = FontWeight.SemiBold))
            }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            containerColor = Color.White,
            title = { Text("Hapus Riwayat Ini?", fontWeight = FontWeight.Bold, color = Color(0xFF1E211F)) },
            text = { Text("Riwayat perhitungan siklus ini akan dihapus dari penyimpanan.", color = Color(0xFF454B46)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDelete()
                        showDeleteConfirm = false
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFFD32F2F))
                ) {
                    Text("Hapus", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Batal", color = Color(0xFF6B726C))
                }
            }
        )
    }
}

// -------------------------------------------------------------
// EMPTY HISTORY CARD
// -------------------------------------------------------------
@Composable
fun EmptyHistoryCard(onGoToCalculator: () -> Unit) {
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
                .padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFE8F4EE)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.EventNote, contentDescription = null, tint = EmeraldPrimary, modifier = Modifier.size(30.dp))
            }
            Text(
                text = "Belum Ada Riwayat Tersimpan",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                textAlign = TextAlign.Center,
                color = Color(0xFF1E211F)
            )
            Text(
                text = "Lakukan perhitungan di tab Kalkulator lalu simpan hasilnya untuk melihat riwayat dan komparasi di sini.",
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center,
                color = Color(0xFF6B726C)
            )
            Button(
                onClick = onGoToCalculator,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = EmeraldPrimary,
                    contentColor = Color.White
                )
            ) {
                Icon(Icons.Default.Calculate, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Buka Kalkulator", fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

// -------------------------------------------------------------
// VICO CHART DASHBOARD
// -------------------------------------------------------------
@Composable
fun CycleDashboard(
    historyList: List<CalculationHistoryEntity>,
    onSeedSample: () -> Unit
) {
    val haidCycles = remember(historyList) {
        historyList
            .filter { it.caseType == "HAID" }
            .sortedBy { it.startEpochMillis }
            .takeLast(7)
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = Color.White,
        border = BorderStroke(1.dp, Color(0xFFEBE5DC)),
        shadowElevation = 1.dp
    ) {
        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text(
                text = "Tren Durasi Haid Terakhir",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = Color(0xFF1E211F)
            )

            if (haidCycles.size < 2) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Memerlukan minimal 2 siklus haid tersimpan untuk menampilkan grafik tren.",
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.Center,
                        color = Color(0xFF6B726C)
                    )
                    OutlinedButton(
                        onClick = onSeedSample,
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, EmeraldPrimary)
                    ) {
                        Icon(Icons.Default.AutoFixHigh, contentDescription = null, modifier = Modifier.size(16.dp), tint = EmeraldPrimary)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Muat Contoh Siklus", fontSize = 12.sp, color = EmeraldPrimary, fontWeight = FontWeight.SemiBold)
                    }
                }
            } else {
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

                Chart(
                    chart = columnChart(),
                    model = chartEntryModel,
                    startAxis = rememberStartAxis(valueFormatter = startAxisFormatter),
                    bottomAxis = rememberBottomAxis(valueFormatter = bottomAxisFormatter),
                    modifier = Modifier.fillMaxWidth().height(180.dp)
                )
            }
        }
    }
}

// -------------------------------------------------------------
// EDIT PROFILE DIALOG
// -------------------------------------------------------------
@Composable
fun EditProfileDialog(
    currentProfile: UserAdatProfileEntity,
    isFirstSetup: Boolean = false,
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
    var selectedAvatarIndex by remember { mutableIntStateOf(currentProfile.avatarTemplateIndex) }

    var haidDays by remember { mutableIntStateOf(currentProfile.usualHaidDays) }
    var suciDays by remember { mutableIntStateOf(currentProfile.usualSuciDays) }
    var nifasDays by remember { mutableIntStateOf(currentProfile.usualNifasDays) }

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
        containerColor = Color.White,
        title = {
            Text(
                if (isFirstSetup) "Atur Profil & Adat Haid" else "Edit Profil & Adat",
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1E211F)
            )
        },
        text = {
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                item {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Nama Pengguna") },
                        placeholder = { Text("Muslimah") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("profile_name_input"),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = EmeraldPrimary,
                            focusedLabelColor = EmeraldPrimary
                        )
                    )
                }

                item {
                    OutlinedTextField(
                        value = bio,
                        onValueChange = { bio = it },
                        label = { Text("Catatan / Bio") },
                        placeholder = { Text("Menjaga Ibadah Mazhab Syafi'i") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = EmeraldPrimary,
                            focusedLabelColor = EmeraldPrimary
                        )
                    )
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = !usePersonalPhoto,
                            onClick = { usePersonalPhoto = false },
                            label = { Text("Template Karakter", fontSize = 12.sp) },
                            modifier = Modifier.weight(1f),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = EmeraldPrimary,
                                selectedLabelColor = Color.White
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = !usePersonalPhoto,
                                borderColor = if (!usePersonalPhoto) EmeraldPrimary else Color(0xFFEBE5DC)
                            )
                        )
                        FilterChip(
                            selected = usePersonalPhoto,
                            onClick = { usePersonalPhoto = true },
                            label = { Text("Foto Galeri", fontSize = 12.sp) },
                            modifier = Modifier.weight(1f),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = EmeraldPrimary,
                                selectedLabelColor = Color.White
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = usePersonalPhoto,
                                borderColor = if (usePersonalPhoto) EmeraldPrimary else Color(0xFFEBE5DC)
                            )
                        )
                    }
                }

                if (!usePersonalPhoto) {
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            PRESET_AVATAR_TEMPLATES.take(3).forEach { tmpl ->
                                Surface(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable { selectedAvatarIndex = tmpl.id },
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (selectedAvatarIndex == tmpl.id) Color(0xFFE8F4EE) else Color(0xFFFAF7F2),
                                    border = BorderStroke(
                                        if (selectedAvatarIndex == tmpl.id) 2.dp else 1.dp,
                                        if (selectedAvatarIndex == tmpl.id) EmeraldPrimary else Color(0xFFEBE5DC)
                                    )
                                ) {
                                    Column(modifier = Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                        Box(
                                            modifier = Modifier
                                                .size(36.dp)
                                                .clip(CircleShape)
                                                .background(Brush.linearGradient(tmpl.backgroundColors.map { Color(it) })),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(tmpl.initial, fontWeight = FontWeight.Bold, color = Color.White)
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(tmpl.name, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF1E211F))
                                    }
                                }
                            }
                        }
                    }
                } else {
                    item {
                        OutlinedButton(
                            onClick = {
                                photoPickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, EmeraldPrimary)
                        ) {
                            Icon(Icons.Default.PhotoLibrary, contentDescription = null, modifier = Modifier.size(16.dp), tint = EmeraldPrimary)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(if (personalPhotoUri != null) "Ganti Foto" else "Pilih dari Galeri", fontSize = 12.sp, color = EmeraldPrimary, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }

                item {
                    HorizontalDivider(color = Color(0xFFEBE5DC))
                    Text("Pengaturan Kebiasaan (Adat):", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold), color = Color(0xFF1E211F))
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
                        label = "Kebiasaan Masa Suci",
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
                    onSave(name, bio, usePersonalPhoto, personalPhotoUri, selectedAvatarIndex, haidDays, suciDays, nifasDays)
                },
                modifier = Modifier.testTag("save_profile_button"),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = EmeraldPrimary,
                    contentColor = Color.White
                )
            ) {
                Text("Simpan", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Batal", color = Color(0xFF6B726C))
            }
        }
    )
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
            Text(text = label, style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold), color = Color(0xFF1E211F))
            Text(text = "$value $unit", style = MaterialTheme.typography.labelSmall, color = EmeraldPrimary, fontWeight = FontWeight.Bold)
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            IconButton(
                onClick = { if (value > min) onValueChange(value - 1) },
                enabled = value > min,
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFFAF7F2))
            ) {
                Icon(Icons.Default.RemoveCircleOutline, contentDescription = "Kurang", tint = if (value > min) EmeraldPrimary else Color(0xFFB0B5B0))
            }

            Text(
                text = "$value",
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                modifier = Modifier.widthIn(min = 28.dp),
                textAlign = TextAlign.Center,
                color = Color(0xFF1E211F)
            )

            IconButton(
                onClick = { if (value < max) onValueChange(value + 1) },
                enabled = value < max,
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFFAF7F2))
            ) {
                Icon(Icons.Default.AddCircleOutline, contentDescription = "Tambah", tint = if (value < max) EmeraldPrimary else Color(0xFFB0B5B0))
            }
        }
    }
}

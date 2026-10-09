package com.example.ui.screens

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.model.*
import com.example.ui.components.BloodPhaseTimelineView
import com.example.ui.components.CalculationResultCard
import com.example.ui.theme.EmeraldDeep
import com.example.ui.theme.EmeraldMedium
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.GoldTertiary
import com.example.ui.tour.GuidedTourCoordinator
import com.example.ui.tour.tourTarget
import com.example.ui.viewmodel.FiqihViewModel
import java.text.SimpleDateFormat
import java.util.*

enum class CalculatorFlowStep {
    MAIN_HUB,        // Screen 2: Halaman Utama Kalkulator
    STEP_1_INPUT,    // Screen 3: Formulir Input (Step 1)
    STEP_2_TIMELINE, // Screen 4: Input Fase Darah (Step 2)
    STEP_3_RESULT    // Screen 5: Hasil Perhitungan (Step 3)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalculatorScreen(
    viewModel: FiqihViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    // Current screen in the 5-screen flow
    var currentFlowStep by remember { mutableStateOf(CalculatorFlowStep.MAIN_HUB) }

    // Intercept hardware / system back button to step backwards
    BackHandler(enabled = currentFlowStep != CalculatorFlowStep.MAIN_HUB) {
        currentFlowStep = when (currentFlowStep) {
            CalculatorFlowStep.STEP_3_RESULT -> CalculatorFlowStep.STEP_2_TIMELINE
            CalculatorFlowStep.STEP_2_TIMELINE -> CalculatorFlowStep.STEP_1_INPUT
            CalculatorFlowStep.STEP_1_INPUT -> CalculatorFlowStep.MAIN_HUB
            CalculatorFlowStep.MAIN_HUB -> CalculatorFlowStep.MAIN_HUB
        }
    }

    val userProfile by viewModel.userProfile.collectAsState()
    val caseType by viewModel.caseType.collectAsState()
    val intervals by viewModel.intervals.collectAsState()

    // Nifas States
    val deliveryEpochMillis by viewModel.deliveryEpochMillis.collectAsState()
    val deliveryHour by viewModel.deliveryHour.collectAsState()
    val deliveryMinute by viewModel.deliveryMinute.collectAsState()
    val deliveryType by viewModel.deliveryType.collectAsState()
    val adatNifasDays by viewModel.adatNifasDays.collectAsState()
    val hasPreviousHaidBeforeNifas by viewModel.hasPreviousHaidBeforeNifas.collectAsState()
    val previousHaidAdatDays by viewModel.previousHaidAdatDays.collectAsState()

    // Haid & Istihadhah States
    val hasPreviousAdat by viewModel.hasPreviousAdat.collectAsState()
    val adatDurationDays by viewModel.adatDurationDays.collectAsState()
    val adatCycleDays by viewModel.adatCycleDays.collectAsState()
    val adatMemoryType by viewModel.adatMemoryType.collectAsState()
    val rememberedCertainHaidDayOfMonth by viewModel.rememberedCertainHaidDayOfMonth.collectAsState()

    // Category 6 states
    val category6WindowStart by viewModel.category6WindowStart.collectAsState()
    val category6WindowEnd by viewModel.category6WindowEnd.collectAsState()
    val category6PureDays by viewModel.category6PureDays.collectAsState()
    val category6HaidDays by viewModel.category6HaidDays.collectAsState()
    val category6MonthLength by viewModel.category6MonthLength.collectAsState()

    // Takmilatan lit-Tuhri States
    val isTakmilahEnabled by viewModel.isTakmilahEnabled.collectAsState()
    val previousSuciDaysForTakmilah by viewModel.previousSuciDaysForTakmilah.collectAsState()

    // Shalat in & out
    val prayerAtStart by viewModel.prayerAtStart.collectAsState()
    val hadPrayedAtStart by viewModel.hadPrayedAtStart.collectAsState()

    val calculationResult by viewModel.calculationResult.collectAsState()
    val saveMessage by viewModel.saveMessage.collectAsState()

    // State dialogs for special case cards (Puasa, Shalat, Tamyiz)
    var showPuasaDialog by remember { mutableStateOf(false) }
    var showShalatDialog by remember { mutableStateOf(false) }
    var showTamyizDialog by remember { mutableStateOf(false) }
    var showIstihadhahKhususDialog by remember { mutableStateOf(false) }
    var knowsCategory6Window by remember { mutableStateOf(true) }

    val dateFormat = remember { SimpleDateFormat("dd MMMM yyyy", Locale("id", "ID")) }
    val dFormatShort = remember { SimpleDateFormat("dd MMM yyyy", Locale("id", "ID")) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFFAF7F2))
            .testTag("calculator_screen")
    ) {
        when (currentFlowStep) {
            // =================================================================
            // SCREEN 2: HALAMAN UTAMA KALKULATOR (Main Page / Hub)
            // =================================================================
            CalculatorFlowStep.MAIN_HUB -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    // Top Bar: Avatar + Greeting + Bell Icon
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                // Avatar circle with floral icon
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(CircleShape)
                                        .background(EmeraldDeep)
                                        .border(1.dp, GoldTertiary.copy(alpha = 0.6f), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Spa,
                                        contentDescription = null,
                                        tint = GoldTertiary,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }

                                Column {
                                    Text(
                                        text = "Assalamu'alaikum,",
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Normal
                                        ),
                                        color = Color(0xFF6B726C)
                                    )
                                    Text(
                                        text = userProfile.userName.ifBlank { "Muslimah Shalihah" },
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 16.sp
                                        ),
                                        color = Color(0xFF1E211F)
                                    )
                                }
                            }

                            // Notification Bell Icon
                            IconButton(
                                onClick = { showShalatDialog = true },
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(Color.White)
                                    .border(1.dp, Color(0xFFEBE5DC), CircleShape)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Notifications,
                                    contentDescription = "Pengingat Shalat",
                                    tint = Color(0xFF3E433F),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }

                    // Hero Banner Card: Deep forest green with gold arch line art
                    item {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(165.dp)
                                .testTag("app_hero_header"),
                            shape = RoundedCornerShape(24.dp),
                            color = EmeraldDeep,
                            border = BorderStroke(1.dp, GoldTertiary.copy(alpha = 0.5f)),
                            shadowElevation = 4.dp
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
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
                                Column(
                                    modifier = Modifier.fillMaxSize(),
                                    verticalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.Top
                                    ) {
                                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                            Text(
                                                text = "Kalkulator Fiqih Wanita",
                                                style = MaterialTheme.typography.titleLarge.copy(
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 20.sp,
                                                    letterSpacing = (-0.3).sp
                                                ),
                                                color = Color.White
                                            )
                                            Text(
                                                text = "Hitung dengan ilmu, jalani dengan tenang",
                                                style = MaterialTheme.typography.bodyMedium.copy(
                                                    fontSize = 12.5.sp,
                                                    fontWeight = FontWeight.Normal
                                                ),
                                                color = Color(0xFFF9F3E5).copy(alpha = 0.9f)
                                            )
                                        }

                                        // Gold emblem badge
                                        Box(
                                            modifier = Modifier
                                                .size(36.dp)
                                                .clip(CircleShape)
                                                .background(GoldTertiary.copy(alpha = 0.2f))
                                                .border(0.75.dp, GoldTertiary, CircleShape),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Outlined.AutoAwesome,
                                                contentDescription = null,
                                                tint = GoldTertiary,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }

                                    // Mazhab pill tag
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = Color.White.copy(alpha = 0.12f),
                                        border = BorderStroke(0.5.dp, GoldTertiary.copy(alpha = 0.4f))
                                    ) {
                                        Text(
                                            text = "Kaidah Mu'tamad Mazhab Syafi'i",
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

                    // Section Title: "Pilih Jenis Kasus"
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Pilih Jenis Kasus",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp
                                ),
                                color = Color(0xFF1E211F)
                            )
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color(0xFFE8F4EE)
                            ) {
                                Text(
                                    text = "2 Kasus Utama",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 11.sp
                                    ),
                                    color = EmeraldPrimary,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }
                    }

                    // Simplified Selection: Haid & Nifas prominent cards
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .tourTarget("tour_calc_type"),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                // 1. HAID
                                Surface(
                                    onClick = {
                                        viewModel.setCaseType(CaseType.HAID)
                                        currentFlowStep = CalculatorFlowStep.STEP_1_INPUT
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("case_card_haid"),
                                    shape = RoundedCornerShape(20.dp),
                                    color = Color.White,
                                    border = BorderStroke(
                                        width = if (caseType == CaseType.HAID) 1.5.dp else 1.dp,
                                        color = if (caseType == CaseType.HAID) EmeraldPrimary else Color(0xFFEBE5DC)
                                    ),
                                    shadowElevation = 1.dp
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(16.dp),
                                        verticalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.Top
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(44.dp)
                                                    .clip(CircleShape)
                                                    .background(Color(0xFFFDECEF)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Outlined.WaterDrop,
                                                    contentDescription = null,
                                                    tint = Color(0xFFD3455B),
                                                    modifier = Modifier.size(24.dp)
                                                )
                                            }

                                            Surface(
                                                shape = RoundedCornerShape(8.dp),
                                                color = Color(0xFFFAF7F2)
                                            ) {
                                                Text(
                                                    text = "Maks 15 Hari",
                                                    style = MaterialTheme.typography.labelSmall.copy(
                                                        fontSize = 10.sp,
                                                        fontWeight = FontWeight.SemiBold
                                                    ),
                                                    color = Color(0xFF8A908A),
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                        }

                                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                            Text(
                                                text = "Haid",
                                                style = MaterialTheme.typography.titleMedium.copy(
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 16.sp
                                                ),
                                                color = Color(0xFF1E211F)
                                            )
                                            Text(
                                                text = "Siklus bulanan rutin & deteksi istihadhah otomatis",
                                                style = MaterialTheme.typography.bodySmall.copy(
                                                    fontSize = 11.5.sp,
                                                    lineHeight = 16.sp
                                                ),
                                                color = Color(0xFF6B726C)
                                            )
                                        }
                                    }
                                }

                                // 2. NIFAS
                                Surface(
                                    onClick = {
                                        viewModel.setCaseType(CaseType.NIFAS)
                                        currentFlowStep = CalculatorFlowStep.STEP_1_INPUT
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("case_card_nifas"),
                                    shape = RoundedCornerShape(20.dp),
                                    color = Color.White,
                                    border = BorderStroke(
                                        width = if (caseType == CaseType.NIFAS) 1.5.dp else 1.dp,
                                        color = if (caseType == CaseType.NIFAS) EmeraldPrimary else Color(0xFFEBE5DC)
                                    ),
                                    shadowElevation = 1.dp
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(16.dp),
                                        verticalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.Top
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(44.dp)
                                                    .clip(CircleShape)
                                                    .background(Color(0xFFF3EBF9)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Outlined.ChildCare,
                                                    contentDescription = null,
                                                    tint = Color(0xFF8E44AD),
                                                    modifier = Modifier.size(24.dp)
                                                )
                                            }

                                            Surface(
                                                shape = RoundedCornerShape(8.dp),
                                                color = Color(0xFFFAF7F2)
                                            ) {
                                                Text(
                                                    text = "Maks 60 Hari",
                                                    style = MaterialTheme.typography.labelSmall.copy(
                                                        fontSize = 10.sp,
                                                        fontWeight = FontWeight.SemiBold
                                                    ),
                                                    color = Color(0xFF8A908A),
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                        }

                                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                            Text(
                                                text = "Nifas",
                                                style = MaterialTheme.typography.titleMedium.copy(
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 16.sp
                                                ),
                                                color = Color(0xFF1E211F)
                                            )
                                            Text(
                                                text = "Darah persalinan & pemisahan jeda suci 15 hari",
                                                style = MaterialTheme.typography.bodySmall.copy(
                                                    fontSize = 11.5.sp,
                                                    lineHeight = 16.sp
                                                ),
                                                color = Color(0xFF6B726C)
                                            )
                                        }
                                    }
                                }
                            }

                            // Info Callout: Deteksi Otomatis Fiqih
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = Color(0xFFE8F4EE),
                                border = BorderStroke(0.5.dp, EmeraldPrimary.copy(alpha = 0.3f))
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.AutoAwesome,
                                        contentDescription = null,
                                        tint = EmeraldDeep,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(
                                        text = "Deteksi Istihadhah Otomatis: Cukup pilih Haid jika mengalami pendarahan. Bila durasi melebihi 15 hari, sistem kalkulator fiqih akan otomatis menganalisis dan mengklasifikasikan 7 kategori istihadhah.",
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            fontSize = 11.5.sp,
                                            lineHeight = 16.sp
                                        ),
                                        color = EmeraldDeep
                                    )
                                }
                            }

                            // Kaidah & Dialog Pendukung (Pills)
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                item {
                                    SuggestionChip(
                                        onClick = { showTamyizDialog = true },
                                        label = { Text("Tamyiz Darah", fontSize = 11.5.sp) },
                                        icon = {
                                            Icon(
                                                Icons.Outlined.Balance,
                                                contentDescription = null,
                                                modifier = Modifier.size(14.dp),
                                                tint = EmeraldPrimary
                                            )
                                        },
                                        colors = SuggestionChipDefaults.suggestionChipColors(
                                            containerColor = Color.White
                                        ),
                                        border = SuggestionChipDefaults.suggestionChipBorder(
                                            enabled = true,
                                            borderColor = Color(0xFFEBE5DC)
                                        )
                                    )
                                }
                                item {
                                    SuggestionChip(
                                        onClick = { showPuasaDialog = true },
                                        label = { Text("Kaidah Puasa", fontSize = 11.5.sp) },
                                        icon = {
                                            Icon(
                                                Icons.Outlined.NightlightRound,
                                                contentDescription = null,
                                                modifier = Modifier.size(14.dp),
                                                tint = Color(0xFF16A085)
                                            )
                                        },
                                        colors = SuggestionChipDefaults.suggestionChipColors(
                                            containerColor = Color.White
                                        ),
                                        border = SuggestionChipDefaults.suggestionChipBorder(
                                            enabled = true,
                                            borderColor = Color(0xFFEBE5DC)
                                        )
                                    )
                                }
                                item {
                                    SuggestionChip(
                                        onClick = { showShalatDialog = true },
                                        label = { Text("Kaidah Shalat", fontSize = 11.5.sp) },
                                        icon = {
                                            Icon(
                                                Icons.Outlined.Mosque,
                                                contentDescription = null,
                                                modifier = Modifier.size(14.dp),
                                                tint = Color(0xFFE67E22)
                                            )
                                        },
                                        colors = SuggestionChipDefaults.suggestionChipColors(
                                            containerColor = Color.White
                                        ),
                                        border = SuggestionChipDefaults.suggestionChipBorder(
                                            enabled = true,
                                            borderColor = Color(0xFFEBE5DC)
                                        )
                                    )
                                }
                                item {
                                    SuggestionChip(
                                        onClick = { showIstihadhahKhususDialog = true },
                                        label = { Text("7 Kategori Istihadhah", fontSize = 11.5.sp) },
                                        icon = {
                                            Icon(
                                                Icons.Outlined.Stars,
                                                contentDescription = null,
                                                modifier = Modifier.size(14.dp),
                                                tint = Color(0xFFC0392B)
                                            )
                                        },
                                        colors = SuggestionChipDefaults.suggestionChipColors(
                                            containerColor = Color.White
                                        ),
                                        border = SuggestionChipDefaults.suggestionChipBorder(
                                            enabled = true,
                                            borderColor = Color(0xFFEBE5DC)
                                        )
                                    )
                                }
                            }
                        }
                    }

                    // Primary Action Button: "Lanjutkan Perhitungan →"
                    item {
                        Button(
                            onClick = { currentFlowStep = CalculatorFlowStep.STEP_1_INPUT },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .testTag("calculate_button"),
                            shape = RoundedCornerShape(26.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = EmeraldDeep,
                                contentColor = Color.White
                            ),
                            elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
                        ) {
                            Text(
                                text = "Lanjutkan Perhitungan",
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 15.sp,
                                    letterSpacing = 0.3.sp
                                )
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    // Quote Banner below
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "\"Setiap tetes darah ada hukumnya,\nsetiap fase ada tuntunannya.\"",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontStyle = FontStyle.Italic,
                                    fontSize = 12.5.sp,
                                    lineHeight = 18.sp,
                                    fontWeight = FontWeight.Medium
                                ),
                                textAlign = TextAlign.Center,
                                color = Color(0xFF6B726C)
                            )
                        }
                    }
                }
            }

            // =================================================================
            // SCREEN 3: FORMULIR INPUT (Step 1: Input Data)
            // =================================================================
            CalculatorFlowStep.STEP_1_INPUT -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 18.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Header: Back Arrow + Title "Kalkulator Fiqih Wanita"
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            IconButton(
                                onClick = { currentFlowStep = CalculatorFlowStep.MAIN_HUB },
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(Color.White)
                                    .border(1.dp, Color(0xFFEBE5DC), CircleShape)
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Kembali",
                                    tint = Color(0xFF1E211F),
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            Text(
                                text = "Kalkulator Fiqih Wanita",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp
                                ),
                                color = Color(0xFF1E211F)
                            )
                        }
                    }

                    // Step Indicator: (1) Kasus -> (2) Input Data (Active) -> (3) Hasil
                    item {
                        StepIndicatorBar(currentStep = 2)
                    }

                    // Card 1: "Pilih Jenis Kasus & Riwayat"
                    item {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(18.dp),
                            color = Color.White,
                            border = BorderStroke(1.dp, Color(0xFFEBE5DC))
                        ) {
                            Column(
                                modifier = Modifier.padding(18.dp),
                                verticalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                Text(
                                    text = "Jenis Kasus & Riwayat",
                                    style = MaterialTheme.typography.titleSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp
                                    ),
                                    color = Color(0xFF1E211F)
                                )

                                // Pilihan Kasus: Haid vs Nifas
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Surface(
                                        onClick = { viewModel.setCaseType(CaseType.HAID) },
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(14.dp),
                                        color = if (caseType == CaseType.HAID) Color(0xFFE8F4EE) else Color(0xFFFAF7F2),
                                        border = BorderStroke(
                                            1.dp,
                                            if (caseType == CaseType.HAID) EmeraldPrimary else Color(0xFFEBE5DC)
                                        )
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Outlined.WaterDrop,
                                                contentDescription = null,
                                                tint = if (caseType == CaseType.HAID) EmeraldPrimary else Color(0xFF7A807B),
                                                modifier = Modifier.size(20.dp)
                                            )
                                            Text(
                                                text = "Haid",
                                                style = MaterialTheme.typography.bodyMedium.copy(
                                                    fontWeight = if (caseType == CaseType.HAID) FontWeight.Bold else FontWeight.Normal,
                                                    fontSize = 14.sp
                                                ),
                                                color = if (caseType == CaseType.HAID) EmeraldDeep else Color(0xFF1E211F)
                                            )
                                        }
                                    }

                                    Surface(
                                        onClick = { viewModel.setCaseType(CaseType.NIFAS) },
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(14.dp),
                                        color = if (caseType == CaseType.NIFAS) Color(0xFFF3EBF9) else Color(0xFFFAF7F2),
                                        border = BorderStroke(
                                            1.dp,
                                            if (caseType == CaseType.NIFAS) Color(0xFF8E44AD) else Color(0xFFEBE5DC)
                                        )
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Outlined.ChildCare,
                                                contentDescription = null,
                                                tint = if (caseType == CaseType.NIFAS) Color(0xFF8E44AD) else Color(0xFF7A807B),
                                                modifier = Modifier.size(20.dp)
                                            )
                                            Text(
                                                text = "Nifas",
                                                style = MaterialTheme.typography.bodyMedium.copy(
                                                    fontWeight = if (caseType == CaseType.NIFAS) FontWeight.Bold else FontWeight.Normal,
                                                    fontSize = 14.sp
                                                ),
                                                color = if (caseType == CaseType.NIFAS) Color(0xFF8E44AD) else Color(0xFF1E211F)
                                            )
                                        }
                                    }
                                }

                                // Sub-status Riwayat Pengalaman
                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text(
                                        text = if (caseType == CaseType.HAID) "Riwayat Haid:" else "Riwayat Nifas:",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 12.sp
                                        ),
                                        color = Color(0xFF5A605B)
                                    )

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        FilterChip(
                                            selected = hasPreviousAdat,
                                            onClick = {
                                                viewModel.setHasPreviousAdat(true)
                                                if (caseType == CaseType.HAID && adatMemoryType == AdatMemoryType.BELUM_PERNAH_HAID) {
                                                    viewModel.setAdatMemoryType(AdatMemoryType.INGAT_LENGKAP_QADRAN_WAQTAN)
                                                }
                                            },
                                            label = {
                                                Text(if (caseType == CaseType.HAID) "Pernah Haid (Mu'tadah)" else "Pernah Nifas")
                                            },
                                            modifier = Modifier.weight(1f)
                                        )

                                        FilterChip(
                                            selected = !hasPreviousAdat,
                                            onClick = {
                                                viewModel.setHasPreviousAdat(false)
                                                viewModel.setAdatMemoryType(AdatMemoryType.BELUM_PERNAH_HAID)
                                            },
                                            label = {
                                                Text(if (caseType == CaseType.HAID) "Pertama Kali (Mubtadi'ah)" else "Kelahiran Pertama")
                                            },
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Card 2: Tanggal & Waktu Mulai Pendarahan / Persalinan
                    item {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(18.dp),
                            color = Color.White,
                            border = BorderStroke(1.dp, Color(0xFFEBE5DC))
                        ) {
                            Column(
                                modifier = Modifier.padding(18.dp),
                                verticalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                if (caseType == CaseType.HAID) {
                                    Text(
                                        text = "Tanggal Siklus Terakhir / Darah Mulai",
                                        style = MaterialTheme.typography.titleSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.5.sp
                                        ),
                                        color = Color(0xFF1E211F)
                                    )

                                    val firstIntervalStart = intervals.firstOrNull()?.startEpochMillis ?: System.currentTimeMillis()
                                    val startCal = Calendar.getInstance().apply { timeInMillis = firstIntervalStart }

                                    Surface(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                DatePickerDialog(
                                                    context,
                                                    { _, y, m, d ->
                                                        val cal = Calendar.getInstance().apply {
                                                            timeInMillis = firstIntervalStart
                                                            set(Calendar.YEAR, y)
                                                            set(Calendar.MONTH, m)
                                                            set(Calendar.DAY_OF_MONTH, d)
                                                        }
                                                        if (intervals.isNotEmpty()) {
                                                            val dur = intervals[0].durationMillis
                                                            viewModel.updateInterval(0, intervals[0].copy(
                                                                startEpochMillis = cal.timeInMillis,
                                                                endEpochMillis = cal.timeInMillis + dur
                                                            ))
                                                        } else {
                                                            viewModel.setStartDate(cal.timeInMillis)
                                                        }
                                                    },
                                                    startCal.get(Calendar.YEAR),
                                                    startCal.get(Calendar.MONTH),
                                                    startCal.get(Calendar.DAY_OF_MONTH)
                                                ).show()
                                            },
                                        shape = RoundedCornerShape(14.dp),
                                        color = Color(0xFFFAF7F2),
                                        border = BorderStroke(0.75.dp, Color(0xFFEBE5DC))
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 14.dp, vertical = 12.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Outlined.CalendarMonth,
                                                    contentDescription = null,
                                                    tint = EmeraldPrimary,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                                Text(
                                                    text = dateFormat.format(Date(firstIntervalStart)),
                                                    style = MaterialTheme.typography.bodyMedium.copy(
                                                        fontWeight = FontWeight.SemiBold,
                                                        fontSize = 14.sp
                                                    ),
                                                    color = Color(0xFF1E211F)
                                                )
                                            }
                                            Icon(
                                                imageVector = Icons.Default.KeyboardArrowRight,
                                                contentDescription = null,
                                                tint = Color(0xFF7A807B)
                                            )
                                        }
                                    }
                                } else {
                                    // NIFAS INPUTS
                                    Text(
                                        text = "Tanggal & Waktu Melahirkan",
                                        style = MaterialTheme.typography.titleSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.5.sp
                                        ),
                                        color = Color(0xFF1E211F)
                                    )

                                    val deliveryEpoch = deliveryEpochMillis ?: System.currentTimeMillis()
                                    val delCal = Calendar.getInstance().apply { timeInMillis = deliveryEpoch }

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        // Date Picker
                                        Surface(
                                            modifier = Modifier
                                                .weight(1.3f)
                                                .clickable {
                                                    DatePickerDialog(
                                                        context,
                                                        { _, y, m, d ->
                                                            val cal = Calendar.getInstance().apply {
                                                                timeInMillis = deliveryEpoch
                                                                set(Calendar.YEAR, y)
                                                                set(Calendar.MONTH, m)
                                                                set(Calendar.DAY_OF_MONTH, d)
                                                            }
                                                            viewModel.setDeliveryEpochMillis(cal.timeInMillis)
                                                        },
                                                        delCal.get(Calendar.YEAR),
                                                        delCal.get(Calendar.MONTH),
                                                        delCal.get(Calendar.DAY_OF_MONTH)
                                                    ).show()
                                                },
                                            shape = RoundedCornerShape(14.dp),
                                            color = Color(0xFFFAF7F2),
                                            border = BorderStroke(0.75.dp, Color(0xFFEBE5DC))
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Outlined.CalendarMonth,
                                                    contentDescription = null,
                                                    tint = Color(0xFF8E44AD),
                                                    modifier = Modifier.size(18.dp)
                                                )
                                                Text(
                                                    text = dFormatShort.format(Date(deliveryEpoch)),
                                                    style = MaterialTheme.typography.bodyMedium.copy(
                                                        fontWeight = FontWeight.SemiBold,
                                                        fontSize = 13.sp
                                                    ),
                                                    color = Color(0xFF1E211F)
                                                )
                                            }
                                        }

                                        // Time Picker
                                        Surface(
                                            modifier = Modifier
                                                .weight(1f)
                                                .clickable {
                                                    TimePickerDialog(
                                                        context,
                                                        { _, h, m ->
                                                            viewModel.setDeliveryTime(h, m)
                                                        },
                                                        deliveryHour,
                                                        deliveryMinute,
                                                        true
                                                    ).show()
                                                },
                                            shape = RoundedCornerShape(14.dp),
                                            color = Color(0xFFFAF7F2),
                                            border = BorderStroke(0.75.dp, Color(0xFFEBE5DC))
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Outlined.Schedule,
                                                    contentDescription = null,
                                                    tint = Color(0xFF8E44AD),
                                                    modifier = Modifier.size(18.dp)
                                                )
                                                Text(
                                                    text = String.format("%02d:%02d", deliveryHour, deliveryMinute),
                                                    style = MaterialTheme.typography.bodyMedium.copy(
                                                        fontWeight = FontWeight.SemiBold,
                                                        fontSize = 13.sp
                                                    ),
                                                    color = Color(0xFF1E211F)
                                                )
                                            }
                                        }
                                    }

                                    // Jenis Persalinan
                                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Text(
                                            text = "Jenis Persalinan:",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontWeight = FontWeight.SemiBold,
                                                fontSize = 12.sp
                                            ),
                                            color = Color(0xFF5A605B)
                                        )

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            FilterChip(
                                                selected = deliveryType == DeliveryType.TUNGGAL_NORMAL_SESAR,
                                                onClick = { viewModel.setDeliveryType(DeliveryType.TUNGGAL_NORMAL_SESAR) },
                                                label = { Text("Tunggal", fontSize = 11.5.sp) },
                                                modifier = Modifier.weight(1f)
                                            )
                                            FilterChip(
                                                selected = deliveryType == DeliveryType.BAYI_KEMBAR,
                                                onClick = { viewModel.setDeliveryType(DeliveryType.BAYI_KEMBAR) },
                                                label = { Text("Kembar", fontSize = 11.5.sp) },
                                                modifier = Modifier.weight(1f)
                                            )
                                            FilterChip(
                                                selected = deliveryType == DeliveryType.KEGUGURAN,
                                                onClick = { viewModel.setDeliveryType(DeliveryType.KEGUGURAN) },
                                                label = { Text("Keguguran", fontSize = 11.5.sp) },
                                                modifier = Modifier.weight(1f)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Card 3: STATUS INGATAN ADAT HAID (Hanya jika HAID & Pernah Haid / Mu'tadah)
                    if (caseType == CaseType.HAID && hasPreviousAdat) {
                        item {
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(18.dp),
                                color = Color.White,
                                border = BorderStroke(1.dp, Color(0xFFEBE5DC))
                            ) {
                                Column(
                                    modifier = Modifier.padding(18.dp),
                                    verticalArrangement = Arrangement.spacedBy(14.dp)
                                ) {
                                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                        Text(
                                            text = "Status Ingatan Adat Haid",
                                            style = MaterialTheme.typography.titleSmall.copy(
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 15.sp
                                            ),
                                            color = Color(0xFF1E211F)
                                        )
                                        Text(
                                            text = "Pilih kondisi ingatan kebiasaan haid Anda:",
                                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                                            color = Color(0xFF6B726C)
                                        )
                                    }

                                    // Pilihan 1: Ingat Lengkap (Qadran wa Waqtan)
                                    Surface(
                                        onClick = { viewModel.setAdatMemoryType(AdatMemoryType.INGAT_LENGKAP_QADRAN_WAQTAN) },
                                        shape = RoundedCornerShape(14.dp),
                                        color = if (adatMemoryType == AdatMemoryType.INGAT_LENGKAP_QADRAN_WAQTAN) Color(0xFFE8F4EE) else Color(0xFFFAF7F2),
                                        border = BorderStroke(1.dp, if (adatMemoryType == AdatMemoryType.INGAT_LENGKAP_QADRAN_WAQTAN) EmeraldPrimary else Color(0xFFEBE5DC))
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(12.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                                        ) {
                                            RadioButton(
                                                selected = adatMemoryType == AdatMemoryType.INGAT_LENGKAP_QADRAN_WAQTAN,
                                                onClick = { viewModel.setAdatMemoryType(AdatMemoryType.INGAT_LENGKAP_QADRAN_WAQTAN) },
                                                colors = RadioButtonDefaults.colors(selectedColor = EmeraldPrimary)
                                            )
                                            Column {
                                                Text(
                                                    text = "1. Ingat Durasi & Waktu Mulai",
                                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold, fontSize = 13.5.sp),
                                                    color = Color(0xFF1E211F)
                                                )
                                                Text(
                                                    text = "Mengetahui berapa hari haid dan waktu siklusnya.",
                                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp),
                                                    color = Color(0xFF6B726C)
                                                )
                                            }
                                        }
                                    }

                                    // Sub-input Pilihan 1: Durasi & Siklus Normal
                                    if (adatMemoryType == AdatMemoryType.INGAT_LENGKAP_QADRAN_WAQTAN) {
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(start = 8.dp, end = 8.dp, bottom = 4.dp),
                                            verticalArrangement = Arrangement.spacedBy(12.dp)
                                        ) {
                                            // Durasi Haid
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clip(RoundedCornerShape(12.dp))
                                                    .background(Color(0xFFFAF7F2))
                                                    .border(0.75.dp, Color(0xFFEBE5DC), RoundedCornerShape(12.dp))
                                                    .padding(horizontal = 14.dp, vertical = 8.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Column {
                                                    Text("Durasi Kebiasaan Haid", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold, fontSize = 11.5.sp), color = Color(0xFF5A605B))
                                                    Text("$adatDurationDays hari", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, fontSize = 14.sp), color = Color(0xFF1E211F))
                                                }
                                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                                    IconButton(onClick = { viewModel.setAdatDurationDays((adatDurationDays - 1).coerceAtLeast(1)) }, modifier = Modifier.size(32.dp)) {
                                                        Icon(Icons.Default.Remove, contentDescription = "Kurang", modifier = Modifier.size(16.dp))
                                                    }
                                                    IconButton(onClick = { viewModel.setAdatDurationDays((adatDurationDays + 1).coerceAtMost(15)) }, modifier = Modifier.size(32.dp)) {
                                                        Icon(Icons.Default.Add, contentDescription = "Tambah", modifier = Modifier.size(16.dp))
                                                    }
                                                }
                                            }

                                            // Siklus Normal
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clip(RoundedCornerShape(12.dp))
                                                    .background(Color(0xFFFAF7F2))
                                                    .border(0.75.dp, Color(0xFFEBE5DC), RoundedCornerShape(12.dp))
                                                    .padding(horizontal = 14.dp, vertical = 8.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Column {
                                                    Text("Panjang Siklus Normal", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold, fontSize = 11.5.sp), color = Color(0xFF5A605B))
                                                    Text("$adatCycleDays hari", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, fontSize = 14.sp), color = Color(0xFF1E211F))
                                                }
                                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                                    IconButton(onClick = { viewModel.setAdatCycleDays((adatCycleDays - 1).coerceAtLeast(20)) }, modifier = Modifier.size(32.dp)) {
                                                        Icon(Icons.Default.Remove, contentDescription = "Kurang", modifier = Modifier.size(16.dp))
                                                    }
                                                    IconButton(onClick = { viewModel.setAdatCycleDays((adatCycleDays + 1).coerceAtMost(60)) }, modifier = Modifier.size(32.dp)) {
                                                        Icon(Icons.Default.Add, contentDescription = "Tambah", modifier = Modifier.size(16.dp))
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    // Pilihan 2: Lupa Total (Mutahayyirah Mahdhah)
                                    Surface(
                                        onClick = { viewModel.setAdatMemoryType(AdatMemoryType.LUPA_SEMUANYA_MUTAHAYYIRAH) },
                                        shape = RoundedCornerShape(14.dp),
                                        color = if (adatMemoryType == AdatMemoryType.LUPA_SEMUANYA_MUTAHAYYIRAH) Color(0xFFFDECEF) else Color(0xFFFAF7F2),
                                        border = BorderStroke(1.dp, if (adatMemoryType == AdatMemoryType.LUPA_SEMUANYA_MUTAHAYYIRAH) Color(0xFFD3455B) else Color(0xFFEBE5DC))
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(12.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                                        ) {
                                            RadioButton(
                                                selected = adatMemoryType == AdatMemoryType.LUPA_SEMUANYA_MUTAHAYYIRAH,
                                                onClick = { viewModel.setAdatMemoryType(AdatMemoryType.LUPA_SEMUANYA_MUTAHAYYIRAH) },
                                                colors = RadioButtonDefaults.colors(selectedColor = Color(0xFFD3455B))
                                            )
                                            Column {
                                                Text(
                                                    text = "2. Lupa Total (Mutahayyirah Mahdhah)",
                                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold, fontSize = 13.5.sp),
                                                    color = Color(0xFF1E211F)
                                                )
                                                Text(
                                                    text = "Lupa durasi hari maupun tanggal mulainya adat.",
                                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp),
                                                    color = Color(0xFF6B726C)
                                                )
                                            }
                                        }
                                    }

                                    // Sub-info Pilihan 2
                                    if (adatMemoryType == AdatMemoryType.LUPA_SEMUANYA_MUTAHAYYIRAH) {
                                        Surface(
                                            shape = RoundedCornerShape(12.dp),
                                            color = Color(0xFFFFF8E7),
                                            border = BorderStroke(0.5.dp, Color(0xFFE67E22).copy(alpha = 0.4f))
                                        ) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(12.dp),
                                                verticalAlignment = Alignment.Top,
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                Icon(Icons.Outlined.Info, contentDescription = null, tint = Color(0xFFD35400), modifier = Modifier.size(18.dp))
                                                Text(
                                                    text = "Kaidah Fiqih: Karena durasi & waktu adat dilupakan, aplikasi tidak meminta nilai adat dan tidak mengarang hari haid. Seluruh masa pendarahan dihukumi Ihtiyath (tetap wajib shalat, puasa, dan mandi setiap masuk waktu fardhu).",
                                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp, lineHeight = 16.sp),
                                                    color = Color(0xFF7E4A00)
                                                )
                                            }
                                        }
                                    }

                                    // Pilihan 3: Ingat Durasi Saja, Lupa Waktu Mulai (Kategori 6)
                                    Surface(
                                        onClick = { viewModel.setAdatMemoryType(AdatMemoryType.INGAT_QADRAN_LUPA_WAQTAN) },
                                        shape = RoundedCornerShape(14.dp),
                                        color = if (adatMemoryType == AdatMemoryType.INGAT_QADRAN_LUPA_WAQTAN) Color(0xFFEBF5FB) else Color(0xFFFAF7F2),
                                        border = BorderStroke(1.dp, if (adatMemoryType == AdatMemoryType.INGAT_QADRAN_LUPA_WAQTAN) Color(0xFF2980B9) else Color(0xFFEBE5DC))
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(12.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                                        ) {
                                            RadioButton(
                                                selected = adatMemoryType == AdatMemoryType.INGAT_QADRAN_LUPA_WAQTAN,
                                                onClick = { viewModel.setAdatMemoryType(AdatMemoryType.INGAT_QADRAN_LUPA_WAQTAN) },
                                                colors = RadioButtonDefaults.colors(selectedColor = Color(0xFF2980B9))
                                            )
                                            Column {
                                                Text(
                                                    text = "3. Ingat Durasi Saja, Lupa Waktu Mulai",
                                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold, fontSize = 13.5.sp),
                                                    color = Color(0xFF1E211F)
                                                )
                                                Text(
                                                    text = "Ingat berapa hari haidnya, tetapi lupa tanggal mulainya.",
                                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp),
                                                    color = Color(0xFF6B726C)
                                                )
                                            }
                                        }
                                    }

                                    // Sub-input Pilihan 3: Durasi + Pertanyaan Batas Rentang
                                    if (adatMemoryType == AdatMemoryType.INGAT_QADRAN_LUPA_WAQTAN) {
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(start = 8.dp, end = 8.dp, bottom = 4.dp),
                                            verticalArrangement = Arrangement.spacedBy(12.dp)
                                        ) {
                                            // Durasi Haid yang Diingat
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clip(RoundedCornerShape(12.dp))
                                                    .background(Color(0xFFFAF7F2))
                                                    .border(0.75.dp, Color(0xFFEBE5DC), RoundedCornerShape(12.dp))
                                                .padding(horizontal = 14.dp, vertical = 8.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Column {
                                                    Text("Durasi Adat yang Diingat", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold, fontSize = 11.5.sp), color = Color(0xFF5A605B))
                                                    Text("$adatDurationDays hari", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, fontSize = 14.sp), color = Color(0xFF1E211F))
                                                }
                                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                                    IconButton(onClick = { viewModel.setAdatDurationDays((adatDurationDays - 1).coerceAtLeast(1)) }, modifier = Modifier.size(32.dp)) {
                                                        Icon(Icons.Default.Remove, contentDescription = "Kurang", modifier = Modifier.size(16.dp))
                                                    }
                                                    IconButton(onClick = { viewModel.setAdatDurationDays((adatDurationDays + 1).coerceAtMost(15)) }, modifier = Modifier.size(32.dp)) {
                                                        Icon(Icons.Default.Add, contentDescription = "Tambah", modifier = Modifier.size(16.dp))
                                                    }
                                                }
                                            }

                                            // Pertanyaan Batas Posisi Haid
                                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                                Text(
                                                    text = "Apakah Anda masih mengingat perkiraan batas/rentang posisi haid (misal: terjadi di 10 hari pertama)?",
                                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp, fontWeight = FontWeight.Medium),
                                                    color = Color(0xFF1E211F)
                                                )

                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                                ) {
                                                    FilterChip(
                                                        selected = knowsCategory6Window,
                                                        onClick = { knowsCategory6Window = true },
                                                        label = { Text("Ya, Ingat Batas Rentang") },
                                                        modifier = Modifier.weight(1f)
                                                    )
                                                    FilterChip(
                                                        selected = !knowsCategory6Window,
                                                        onClick = { knowsCategory6Window = false },
                                                        label = { Text("Tidak Tahu Sama Sekali") },
                                                        modifier = Modifier.weight(1f)
                                                    )
                                                }
                                            }

                                            if (knowsCategory6Window) {
                                                // Rentang Posisi: Window Start & End
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                                ) {
                                                    // Window Start
                                                    Row(
                                                        modifier = Modifier
                                                            .weight(1f)
                                                            .clip(RoundedCornerShape(12.dp))
                                                            .background(Color(0xFFFAF7F2))
                                                            .border(0.75.dp, Color(0xFFEBE5DC), RoundedCornerShape(12.dp))
                                                            .padding(horizontal = 10.dp, vertical = 6.dp),
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.SpaceBetween
                                                    ) {
                                                        Column {
                                                            Text("Awal Rentang", style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.5.sp), color = Color(0xFF5A605B))
                                                            Text("Hari ke-$category6WindowStart", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, fontSize = 13.sp))
                                                        }
                                                        Row {
                                                            IconButton(onClick = { viewModel.setCategory6WindowStart((category6WindowStart - 1).coerceAtLeast(1)) }, modifier = Modifier.size(26.dp)) {
                                                                Icon(Icons.Default.Remove, contentDescription = null, modifier = Modifier.size(14.dp))
                                                            }
                                                            IconButton(onClick = { viewModel.setCategory6WindowStart((category6WindowStart + 1).coerceAtMost(category6WindowEnd)) }, modifier = Modifier.size(26.dp)) {
                                                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                                                            }
                                                        }
                                                    }

                                                    // Window End
                                                    Row(
                                                        modifier = Modifier
                                                            .weight(1f)
                                                            .clip(RoundedCornerShape(12.dp))
                                                            .background(Color(0xFFFAF7F2))
                                                            .border(0.75.dp, Color(0xFFEBE5DC), RoundedCornerShape(12.dp))
                                                            .padding(horizontal = 10.dp, vertical = 6.dp),
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.SpaceBetween
                                                    ) {
                                                        Column {
                                                            Text("Akhir Rentang", style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.5.sp), color = Color(0xFF5A605B))
                                                            Text("Hari ke-$category6WindowEnd", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, fontSize = 13.sp))
                                                        }
                                                        Row {
                                                            IconButton(onClick = { viewModel.setCategory6WindowEnd((category6WindowEnd - 1).coerceAtLeast(category6WindowStart)) }, modifier = Modifier.size(26.dp)) {
                                                                Icon(Icons.Default.Remove, contentDescription = null, modifier = Modifier.size(14.dp))
                                                            }
                                                            IconButton(onClick = { viewModel.setCategory6WindowEnd((category6WindowEnd + 1).coerceAtMost(category6MonthLength)) }, modifier = Modifier.size(26.dp)) {
                                                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                                                            }
                                                        }
                                                    }
                                                }

                                                // Chip Hari 1 Pasti Suci
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                                ) {
                                                    val isPureDay1 = category6PureDays.contains(1)
                                                    FilterChip(
                                                        selected = isPureDay1,
                                                        onClick = { viewModel.toggleCategory6PureDay(1) },
                                                        label = { Text("Hari ke-1 Pasti Suci", fontSize = 11.5.sp) }
                                                    )
                                                }
                                            } else {
                                                Surface(
                                                    shape = RoundedCornerShape(10.dp),
                                                    color = Color(0xFFFAF7F2)
                                                ) {
                                                    Text(
                                                        text = "Catatan: Tanpa batas rentang yang diingat, algoritma Candidate Intervals tidak akan memaksakan hari 1-10 secara sembarangan. Status pendarahan dijalankan atas dasar ihtiyath umum.",
                                                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp, color = Color(0xFF6B726C)),
                                                        modifier = Modifier.padding(10.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    // Pilihan 4: Ingat Waktu Mulai Saja, Lupa Durasi (Kategori 7)
                                    Surface(
                                        onClick = { viewModel.setAdatMemoryType(AdatMemoryType.INGAT_WAQTAN_LUPA_QADRAN) },
                                        shape = RoundedCornerShape(14.dp),
                                        color = if (adatMemoryType == AdatMemoryType.INGAT_WAQTAN_LUPA_QADRAN) Color(0xFFFEF5E7) else Color(0xFFFAF7F2),
                                        border = BorderStroke(1.dp, if (adatMemoryType == AdatMemoryType.INGAT_WAQTAN_LUPA_QADRAN) Color(0xFFF39C12) else Color(0xFFEBE5DC))
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(12.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                                        ) {
                                            RadioButton(
                                                selected = adatMemoryType == AdatMemoryType.INGAT_WAQTAN_LUPA_QADRAN,
                                                onClick = { viewModel.setAdatMemoryType(AdatMemoryType.INGAT_WAQTAN_LUPA_QADRAN) },
                                                colors = RadioButtonDefaults.colors(selectedColor = Color(0xFFF39C12))
                                            )
                                            Column {
                                                Text(
                                                    text = "4. Ingat Waktu Mulai Saja, Lupa Durasi",
                                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold, fontSize = 13.5.sp),
                                                    color = Color(0xFF1E211F)
                                                )
                                                Text(
                                                    text = "Ingat tanggal/waktu mulai haid, tetapi lupa berapa hari durasinya.",
                                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp),
                                                    color = Color(0xFF6B726C)
                                                )
                                            }
                                        }
                                    }

                                    // Sub-input Pilihan 4: Tanggal Mulai yang Diingat
                                    if (adatMemoryType == AdatMemoryType.INGAT_WAQTAN_LUPA_QADRAN) {
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(start = 8.dp, end = 8.dp, bottom = 4.dp),
                                            verticalArrangement = Arrangement.spacedBy(10.dp)
                                        ) {
                                            val currentHaidDay = rememberedCertainHaidDayOfMonth ?: 1
                                            val displayHaidDayText = if (rememberedCertainHaidDayOfMonth != null) "Tanggal $rememberedCertainHaidDayOfMonth" else "Belum dipilih"
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clip(RoundedCornerShape(12.dp))
                                                    .background(Color(0xFFFAF7F2))
                                                    .border(0.75.dp, Color(0xFFEBE5DC), RoundedCornerShape(12.dp))
                                                    .padding(horizontal = 14.dp, vertical = 8.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Column {
                                                    Text("Tanggal / Hari Mulai yang Diingat", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold, fontSize = 11.5.sp), color = Color(0xFF5A605B))
                                                    Text(displayHaidDayText, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, fontSize = 14.sp), color = Color(0xFF1E211F))
                                                }
                                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                                    IconButton(onClick = {
                                                        if (rememberedCertainHaidDayOfMonth == null) {
                                                            viewModel.setRememberedCertainHaidDayOfMonth(1)
                                                        } else {
                                                            viewModel.setRememberedCertainHaidDayOfMonth((currentHaidDay - 1).coerceAtLeast(1))
                                                        }
                                                    }, modifier = Modifier.size(32.dp)) {
                                                        Icon(Icons.Default.Remove, contentDescription = "Kurang", modifier = Modifier.size(16.dp))
                                                    }
                                                    IconButton(onClick = {
                                                        if (rememberedCertainHaidDayOfMonth == null) {
                                                            viewModel.setRememberedCertainHaidDayOfMonth(1)
                                                        } else {
                                                            viewModel.setRememberedCertainHaidDayOfMonth((currentHaidDay + 1).coerceAtMost(31))
                                                        }
                                                    }, modifier = Modifier.size(32.dp)) {
                                                        Icon(Icons.Default.Add, contentDescription = "Tambah", modifier = Modifier.size(16.dp))
                                                    }
                                                }
                                            }

                                            Surface(
                                                shape = RoundedCornerShape(10.dp),
                                                color = Color(0xFFFEF9E7)
                                            ) {
                                                val explanationDay = if (rememberedCertainHaidDayOfMonth != null) "Tanggal $rememberedCertainHaidDayOfMonth" else "Hari pertama yang diingat"
                                                Text(
                                                    text = "Kaidah Fiqih Syafi'i (Uyunul Masa'il hal. 89): $explanationDay (24 jam pertama) dipastikan Haid Yakin. Hari ke-2 s/d 15 berstatus Ihtiyath (wajib mandi tiap fardhu). Hari ke-16 ke atas Suci Yakin.",
                                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp, color = Color(0xFF7E4A00), lineHeight = 15.sp),
                                                    modifier = Modifier.padding(10.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Card 4: INPUT NIFAS KHUSUS (Hanya jika NIFAS)
                    if (caseType == CaseType.NIFAS) {
                        item {
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(18.dp),
                                color = Color.White,
                                border = BorderStroke(1.dp, Color(0xFFEBE5DC))
                            ) {
                                Column(
                                    modifier = Modifier.padding(18.dp),
                                    verticalArrangement = Arrangement.spacedBy(14.dp)
                                ) {
                                    Text(
                                        text = "Data Adat & Riwayat Nifas",
                                        style = MaterialTheme.typography.titleSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp
                                        ),
                                        color = Color(0xFF8E44AD)
                                    )

                                    if (hasPreviousAdat) {
                                        // Durasi Nifas Sebelumnya
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(12.dp))
                                                .background(Color(0xFFFAF7F2))
                                                .border(0.75.dp, Color(0xFFEBE5DC), RoundedCornerShape(12.dp))
                                                .padding(horizontal = 14.dp, vertical = 8.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Column {
                                                Text("Durasi Adat Nifas Sebelumnya", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold, fontSize = 11.5.sp), color = Color(0xFF5A605B))
                                                Text("$adatNifasDays hari", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, fontSize = 14.sp), color = Color(0xFF1E211F))
                                            }
                                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                                IconButton(onClick = { viewModel.setAdatNifasDays((adatNifasDays - 1).coerceAtLeast(1)) }, modifier = Modifier.size(32.dp)) {
                                                    Icon(Icons.Default.Remove, contentDescription = "Kurang", modifier = Modifier.size(16.dp))
                                                }
                                                IconButton(onClick = { viewModel.setAdatNifasDays((adatNifasDays + 1).coerceAtMost(60)) }, modifier = Modifier.size(32.dp)) {
                                                    Icon(Icons.Default.Add, contentDescription = "Tambah", modifier = Modifier.size(16.dp))
                                                }
                                            }
                                        }
                                    }

                                    // Riwayat Haid Sebelum Hamil
                                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(
                                                text = "Punya riwayat haid sebelum hamil?",
                                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium, fontSize = 12.5.sp),
                                                color = Color(0xFF1E211F)
                                            )
                                            Switch(
                                                checked = hasPreviousHaidBeforeNifas,
                                                onCheckedChange = { viewModel.setHasPreviousHaidBeforeNifas(it) }
                                            )
                                        }

                                        if (hasPreviousHaidBeforeNifas) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clip(RoundedCornerShape(12.dp))
                                                    .background(Color(0xFFFAF7F2))
                                                    .border(0.75.dp, Color(0xFFEBE5DC), RoundedCornerShape(12.dp))
                                                    .padding(horizontal = 14.dp, vertical = 8.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Column {
                                                    Text("Durasi Adat Haid Sebelum Hamil", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold, fontSize = 11.5.sp), color = Color(0xFF5A605B))
                                                    Text("$previousHaidAdatDays hari", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, fontSize = 14.sp), color = Color(0xFF1E211F))
                                                }
                                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                                    IconButton(onClick = { viewModel.setPreviousHaidAdatDays((previousHaidAdatDays - 1).coerceAtLeast(1)) }, modifier = Modifier.size(32.dp)) {
                                                        Icon(Icons.Default.Remove, contentDescription = "Kurang", modifier = Modifier.size(16.dp))
                                                    }
                                                    IconButton(onClick = { viewModel.setPreviousHaidAdatDays((previousHaidAdatDays + 1).coerceAtMost(15)) }, modifier = Modifier.size(32.dp)) {
                                                        Icon(Icons.Default.Add, contentDescription = "Tambah", modifier = Modifier.size(16.dp))
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Card 5: TAKMILATAN LIT-TUHRI (Jeda Suci Pra-Siklus Opsional)
                    item {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(18.dp),
                            color = Color.White,
                            border = BorderStroke(1.dp, Color(0xFFEBE5DC))
                        ) {
                            Column(
                                modifier = Modifier.padding(18.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "Takmilatan lit-Tuhri (Jeda Suci Pra-Siklus)",
                                            style = MaterialTheme.typography.titleSmall.copy(
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.5.sp
                                            ),
                                            color = EmeraldPrimary
                                        )
                                        Text(
                                            text = "Hitung jika jeda suci sebelum darah ini keluar kurang dari 15 hari.",
                                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp),
                                            color = Color(0xFF6B726C)
                                        )
                                    }
                                    Switch(
                                        checked = isTakmilahEnabled,
                                        onCheckedChange = { viewModel.setTakmilahEnabled(it) }
                                    )
                                }

                                if (isTakmilahEnabled) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(Color(0xFFFAF7F2))
                                            .border(0.75.dp, Color(0xFFEBE5DC), RoundedCornerShape(12.dp))
                                            .padding(horizontal = 14.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column {
                                            Text("Durasi Suci Sebelumnya", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold, fontSize = 11.5.sp), color = Color(0xFF5A605B))
                                            Text("$previousSuciDaysForTakmilah hari", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, fontSize = 14.sp), color = Color(0xFF1E211F))
                                        }
                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                            IconButton(onClick = { viewModel.setPreviousSuciDaysForTakmilah((previousSuciDaysForTakmilah - 1).coerceAtLeast(0)) }, modifier = Modifier.size(32.dp)) {
                                                Icon(Icons.Default.Remove, contentDescription = "Kurang", modifier = Modifier.size(16.dp))
                                            }
                                            IconButton(onClick = { viewModel.setPreviousSuciDaysForTakmilah((previousSuciDaysForTakmilah + 1).coerceAtMost(30)) }, modifier = Modifier.size(32.dp)) {
                                                Icon(Icons.Default.Add, contentDescription = "Tambah", modifier = Modifier.size(16.dp))
                                            }
                                        }
                                    }

                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = Color(0xFFE8F4EE)
                                    ) {
                                        Text(
                                            text = "Bila suci sebelumnya kurang dari 15 hari, sebagian darah awal akan diperlakukan sebagai penyempurna suci (Takmilatan lit-Tuhri) sesuai kaidah Fiqih Syafi'i.",
                                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp, color = EmeraldDeep),
                                            modifier = Modifier.padding(10.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Card 6: Pemeriksaan Shalat Saat Darah Mulai
                    item {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(18.dp),
                            color = Color.White,
                            border = BorderStroke(1.dp, Color(0xFFEBE5DC))
                        ) {
                            Column(
                                modifier = Modifier.padding(18.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Text(
                                    text = "Pemeriksaan Shalat Saat Darah Mulai",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = EmeraldPrimary
                                )
                                Text(
                                    text = "Apakah waktu shalat sudah masuk dan sudah dikerjakan saat darah pertama kali keluar?",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFF6B726C)
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    FilterChip(
                                        selected = !hadPrayedAtStart,
                                        onClick = { viewModel.setHadPrayedAtStart(false) },
                                        label = { Text("Belum Shalat (Qadha bila cukup waktu)") },
                                        modifier = Modifier.weight(1f)
                                    )
                                    FilterChip(
                                        selected = hadPrayedAtStart,
                                        onClick = { viewModel.setHadPrayedAtStart(true) },
                                        label = { Text("Sudah Shalat") },
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }
                    }

                    // Bottom Button: "Lanjutkan →"
                    item {
                        Button(
                            onClick = { currentFlowStep = CalculatorFlowStep.STEP_2_TIMELINE },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .padding(top = 4.dp),
                            shape = RoundedCornerShape(25.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = EmeraldDeep,
                                contentColor = Color.White
                            ),
                            elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
                        ) {
                            Text(
                                text = "Lanjutkan ke Input Darah",
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 15.sp,
                                    letterSpacing = 0.3.sp
                                )
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            // =================================================================
            // SCREEN 4: INPUT FASE DARAH (Step 2: Timeline Phase Input)
            // =================================================================
            CalculatorFlowStep.STEP_2_TIMELINE -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 18.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Header: Back Arrow + Title "Input Fase Darah"
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            IconButton(
                                onClick = { currentFlowStep = CalculatorFlowStep.STEP_1_INPUT },
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(Color.White)
                                    .border(1.dp, Color(0xFFEBE5DC), CircleShape)
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Kembali",
                                    tint = Color(0xFF1E211F),
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            Text(
                                text = "Input Fase Darah",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp
                                ),
                                color = Color(0xFF1E211F)
                            )
                        }
                    }

                    // Vertical Timeline & Detailed Phase Editor View
                    item {
                        BloodPhaseTimelineView(
                            intervals = intervals,
                            onAddPhase = { viewModel.addNewNextInterval() },
                            onUpdatePhase = { idx, updated -> viewModel.updateInterval(idx, updated) },
                            onRemovePhase = { idx -> viewModel.removeInterval(idx) },
                            modifier = Modifier.tourTarget("tour_blood_chronology")
                        )
                    }

                    // Big Action Button: "Hitung Kepastian Fiqih →"
                    item {
                        Button(
                            onClick = {
                                viewModel.doCalculate()
                                currentFlowStep = CalculatorFlowStep.STEP_3_RESULT
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .tourTarget("tour_calculate_button")
                                .testTag("calculate_button"),
                            shape = RoundedCornerShape(26.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = EmeraldDeep,
                                contentColor = Color.White
                            ),
                            elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
                        ) {
                            Icon(Icons.Outlined.Calculate, contentDescription = null, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Hitung Kepastian Fiqih",
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 15.sp,
                                    letterSpacing = 0.3.sp
                                )
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }

            // =================================================================
            // SCREEN 5: HASIL PERHITUNGAN (Step 3: Result Page)
            // =================================================================
            CalculatorFlowStep.STEP_3_RESULT -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 18.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Header: Back Arrow + Title "Hasil Perhitungan"
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            IconButton(
                                onClick = { currentFlowStep = CalculatorFlowStep.STEP_2_TIMELINE },
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(Color.White)
                                    .border(1.dp, Color(0xFFEBE5DC), CircleShape)
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Kembali",
                                    tint = Color(0xFF1E211F),
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            Text(
                                text = "Hasil Perhitungan",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp
                                ),
                                color = Color(0xFF1E211F)
                            )
                        }
                    }

                    // Result Content Cards matching Screen 5 in the reference image
                    val res = calculationResult
                    if (res != null) {
                        // 1. Hero Status Card in Deep Forest Green
                        item {
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(22.dp),
                                color = EmeraldDeep,
                                border = BorderStroke(1.dp, GoldTertiary.copy(alpha = 0.6f)),
                                shadowElevation = 4.dp
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(
                                            Brush.verticalGradient(
                                                colors = listOf(
                                                    Color(0xFF0D3D32),
                                                    Color(0xFF134E3F)
                                                )
                                            )
                                        )
                                        .padding(20.dp),
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        // Avatar badge with gold ring
                                        Box(
                                            modifier = Modifier
                                                .size(44.dp)
                                                .clip(CircleShape)
                                                .background(GoldTertiary.copy(alpha = 0.2f))
                                                .border(1.dp, GoldTertiary, CircleShape),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Outlined.Spa,
                                                contentDescription = null,
                                                tint = GoldTertiary,
                                                modifier = Modifier.size(24.dp)
                                            )
                                        }

                                        Column {
                                            Text(
                                                text = "Hukum",
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Medium
                                                ),
                                                color = Color(0xFFF9F3E5).copy(alpha = 0.85f)
                                            )
                                            Text(
                                                text = res.caseCategory.ifBlank { res.statusSummary },
                                                style = MaterialTheme.typography.titleLarge.copy(
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 22.sp
                                                ),
                                                color = Color.White
                                            )
                                        }
                                    }

                                    Text(
                                        text = res.statusSummary,
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontSize = 13.sp,
                                            lineHeight = 19.sp
                                        ),
                                        color = Color(0xFFF9F3E5).copy(alpha = 0.95f)
                                    )
                                }
                            }
                        }

                        // 2. Card "Periode Darah"
                        item {
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                color = Color.White,
                                border = BorderStroke(1.dp, Color(0xFFEBE5DC))
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = "Periode Darah",
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                        color = Color(0xFF1E211F)
                                    )

                                    val startMillis = intervals.minOfOrNull { it.startEpochMillis } ?: System.currentTimeMillis()
                                    val endMillis = intervals.maxOfOrNull { it.endEpochMillis } ?: System.currentTimeMillis()
                                    val totalDays = ((endMillis - startMillis) / (24 * 3600_000L)).coerceAtLeast(1)

                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.CalendarToday,
                                            contentDescription = null,
                                            tint = EmeraldPrimary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Column {
                                            Text(
                                                text = "${dFormatShort.format(Date(startMillis))} – ${dFormatShort.format(Date(endMillis))}",
                                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                                color = Color(0xFF1E211F)
                                            )
                                            Text(
                                                text = "$totalDays hari • ${res.caseCategory}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = Color(0xFF6B726C)
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // 3. Card "Rincian Perhitungan"
                        item {
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                color = Color.White,
                                border = BorderStroke(1.dp, Color(0xFFEBE5DC))
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Text(
                                        text = "Rincian Perhitungan",
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                        color = Color(0xFF1E211F)
                                    )

                                    val calculatedHaidHours = res.haidOrNifasSegment?.durationHours
                                        ?: (res.periodResults.filter { it.status == FiqihStatus.HAID }.sumOf { it.endEpochMillis - it.startEpochMillis } / 3600_000L)
                                    val calculatedHaidDays = calculatedHaidHours / 24
                                    val calculatedHaidRemHours = calculatedHaidHours % 24
                                    val haidDisplayVal = if (calculatedHaidRemHours > 0) "$calculatedHaidDays hari $calculatedHaidRemHours jam" else "$calculatedHaidDays hari"

                                    CalculationDetailRow(label = "Durasi Haid Sah", value = haidDisplayVal)

                                    val calculatedIstiHours = res.istihadhahSegment?.durationHours
                                        ?: (res.periodResults.filter { it.status == FiqihStatus.ISTIHADHAH }.sumOf { it.endEpochMillis - it.startEpochMillis } / 3600_000L)
                                    if (calculatedIstiHours > 0) {
                                        val istiDays = calculatedIstiHours / 24
                                        val istiRemHours = calculatedIstiHours % 24
                                        val istiDisplayVal = if (istiRemHours > 0) "$istiDays hari $istiRemHours jam" else "$istiDays hari"
                                        CalculationDetailRow(label = "Durasi Istihadhah", value = istiDisplayVal)
                                    }

                                    if (hasPreviousAdat) {
                                        if (res.caseCategory.contains("Mumayyizah", ignoreCase = true) && !res.caseCategory.contains("Ghairu", ignoreCase = true)) {
                                            CalculationDetailRow(label = "Adat Sebelumnya", value = "$adatDurationDays hari (dikalahkan tamyiz)")
                                        } else {
                                            CalculationDetailRow(label = "Adat Kebiasaan", value = "$adatDurationDays hari")
                                        }
                                    }
                                    CalculationDetailRow(label = "Lama Siklus", value = "$adatCycleDays hari")
                                    val isIsti = res.istihadhahSegment != null || res.periodResults.any { it.status == FiqihStatus.ISTIHADHAH }
                                    CalculationDetailRow(label = "Status Siklus", value = if (isIsti) "Istihadhah" else if (adatDurationDays in 1..15) "Normal" else "Perlu Perhatian")
                                    CalculationDetailRow(label = "Kategori", value = res.caseCategory)
                                }
                            }
                        }

                        // 4. Card "Takmilatan lit-Tuhri" (Automatic Detection Badge)
                        val isTakmilah = res.caseCategory.contains("TAKMILAT", ignoreCase = true) ||
                                res.statusSummary.contains("TAKMILAT", ignoreCase = true) ||
                                res.phaseBreakdowns.any { it.contains("Takmilat", ignoreCase = true) }
                        if (isTakmilah) {
                            item {
                                Surface(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(16.dp),
                                    color = Color(0xFFF9F3E5),
                                    border = BorderStroke(1.dp, GoldTertiary.copy(alpha = 0.5f))
                                ) {
                                    Row(
                                        modifier = Modifier.padding(16.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(36.dp)
                                                .clip(CircleShape)
                                                .background(EmeraldDeep),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Outlined.Eco,
                                                contentDescription = null,
                                                tint = GoldTertiary,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }

                                        Column {
                                            Text(
                                                text = "Takmilatan lit-Tuhri",
                                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                                color = EmeraldDeep
                                            )
                                            Text(
                                                text = "Terdeteksi otomatis sesuai kaidah jeda suci",
                                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                                                color = Color(0xFF535A55)
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // 5. Card "Keterangan & Kewajiban Ibadah"
                        item {
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                color = Color.White,
                                border = BorderStroke(1.dp, Color(0xFFEBE5DC))
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Text(
                                        text = "Keterangan & Kewajiban Ibadah",
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                        color = Color(0xFF1E211F)
                                    )

                                    val guidanceText = listOf(res.mandiWajibNote, res.shalatConsequence, res.puasaConsequence)
                                        .filter { it.isNotBlank() }
                                        .joinToString("\n\n")

                                    Text(
                                        text = guidanceText.ifBlank { res.medicalAndSpiritualAdvice },
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontSize = 13.sp,
                                            lineHeight = 19.sp
                                        ),
                                        color = Color(0xFF3E433F)
                                    )

                                    if (res.qadhaPrayers.isNotEmpty()) {
                                        Text(
                                            text = "Shalat yang Harus Diqadha: ${res.qadhaPrayers.joinToString(", ")}",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFFB46522)
                                            )
                                        )
                                    }

                                    if (res.references.isNotEmpty()) {
                                        HorizontalDivider(color = Color(0xFFEBE5DC), thickness = 0.5.dp)
                                        Text(
                                            text = "Rujukan Kitab: ${res.references.joinToString(", ") { "${it.kitabName} (${it.volumeAndPage})" }}",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontSize = 11.sp,
                                                fontStyle = FontStyle.Italic
                                            ),
                                            color = Color(0xFF6B726C)
                                        )
                                    }
                                }
                            }
                        }

                        // 6. Bottom Buttons: "Simpan Riwayat" + Share Button
                        item {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 4.dp),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Button(
                                    onClick = { viewModel.saveCurrentCalculation() },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(50.dp)
                                        .testTag("save_to_history_button"),
                                    shape = RoundedCornerShape(25.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = EmeraldDeep,
                                        contentColor = Color.White
                                    ),
                                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
                                ) {
                                    Icon(Icons.Outlined.BookmarkAdd, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Simpan Riwayat",
                                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold)
                                    )
                                }

                                IconButton(
                                    onClick = {
                                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                            type = "text/plain"
                                            putExtra(Intent.EXTRA_SUBJECT, "Hasil Fiqih Wanita - ${res.caseCategory}")
                                            putExtra(
                                                Intent.EXTRA_TEXT,
                                                "Hasil Fiqih Wanita:\nStatus: ${res.caseCategory}\nRingkasan: ${res.statusSummary}\nPanduan: ${res.shalatConsequence}"
                                            )
                                        }
                                        context.startActivity(Intent.createChooser(shareIntent, "Bagikan Hasil"))
                                    },
                                    modifier = Modifier
                                        .size(50.dp)
                                        .clip(CircleShape)
                                        .background(Color.White)
                                        .border(1.dp, Color(0xFFEBE5DC), CircleShape)
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Share,
                                        contentDescription = "Bagikan",
                                        tint = EmeraldPrimary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }

                        // Save confirmation banner
                        if (saveMessage != null) {
                            item {
                                Surface(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp),
                                    color = Color(0xFFE8F4EE),
                                    border = BorderStroke(0.75.dp, Color(0xFF266E52).copy(alpha = 0.3f))
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF266E52), modifier = Modifier.size(18.dp))
                                        Text(
                                            text = saveMessage ?: "Riwayat berhasil disimpan",
                                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                                            color = Color(0xFF143324)
                                        )
                                    }
                                }
                            }
                        }
                    } else {
                        // Empty result fallback
                        item {
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                color = Color.White,
                                border = BorderStroke(1.dp, Color(0xFFEBE5DC))
                            ) {
                                Column(
                                    modifier = Modifier.padding(24.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Text("Belum ada hasil perhitungan", fontWeight = FontWeight.Bold)
                                    Button(onClick = { currentFlowStep = CalculatorFlowStep.STEP_2_TIMELINE }) {
                                        Text("Hitung Sekarang")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Modal Dialogs for Special Cases from Screen 2
    if (showPuasaDialog) {
        AlertDialog(
            onDismissRequest = { showPuasaDialog = false },
            title = { Text("Hukum Puasa & Darah Wanita", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("• Puasa wanita haid dan nifas hukumnya haram dan tidak sah.")
                    Text("• Wajib mengqadha puasa Ramadhan yang ditinggalkan setelah suci.")
                    Text("• Wanita istihadhah tetap wajib berpuasa dan puasanya sah.")
                }
            },
            confirmButton = {
                TextButton(onClick = { showPuasaDialog = false }) { Text("Tutup") }
            }
        )
    }

    if (showShalatDialog) {
        AlertDialog(
            onDismissRequest = { showShalatDialog = false },
            title = { Text("Pemeriksaan Shalat & Qadha", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("• Jika darah keluar saat waktu shalat sudah masuk dan sudah sempat mengerjakan shalat + thaharah (idrak), maka wajib diqadha setelah suci.")
                    Text("• Jika darah berhenti saat waktu shalat tersisa minimal takbiratul ihram, maka shalat tersebut wajib dikerjakan/diqadha.")
                }
            },
            confirmButton = {
                TextButton(onClick = { showShalatDialog = false }) { Text("Paham") }
            }
        )
    }

    if (showTamyizDialog) {
        AlertDialog(
            onDismissRequest = { showTamyizDialog = false },
            title = { Text("Syarat Tamyiz Mazhab Syafi'i", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("1. Darah kuat tidak kurang dari 24 jam.")
                    Text("2. Darah kuat tidak melebihi 15 hari 15 malam.")
                    Text("3. Darah lemah tidak kurang dari 15 hari 15 malam (jika keluar bersambung).")
                    Text("4. Darah keluar secara berurutan tanpa bercampur aduk.")
                }
            },
            confirmButton = {
                TextButton(onClick = { showTamyizDialog = false }) { Text("Tutup") }
            }
        )
    }

    if (showIstihadhahKhususDialog) {
        AlertDialog(
            onDismissRequest = { showIstihadhahKhususDialog = false },
            title = { Text("7 Kategori Istihadhah", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("1. Mubtadi'ah Mumayyizah")
                    Text("2. Mubtadi'ah Ghayru Mumayyizah")
                    Text("3. Mu'tadah Mumayyizah")
                    Text("4. Mu'tadah Ghayru Mumayyizah Dzakirah")
                    Text("5. Mutahayyirah Dzakirah lil-Waqti")
                    Text("6. Mutahayyirah Dzakirah lil-Qadri")
                    Text("7. Mutahayyirah Mahdhah (Lupa Total)")
                }
            },
            confirmButton = {
                Button(onClick = {
                    showIstihadhahKhususDialog = false
                    currentFlowStep = CalculatorFlowStep.STEP_1_INPUT
                }) {
                    Text("Mulai Analisis Kasus")
                }
            }
        )
    }
}

@Composable
private fun CaseGridCard(
    title: String,
    icon: ImageVector,
    badgeBgColor: Color,
    iconColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = modifier
            .aspectRatio(0.85f),
        shape = RoundedCornerShape(16.dp),
        color = Color.White,
        border = BorderStroke(1.dp, Color(0xFFEBE5DC)),
        shadowElevation = 0.5.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(badgeBgColor),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.5.sp,
                    lineHeight = 14.sp
                ),
                textAlign = TextAlign.Center,
                color = Color(0xFF1E211F)
            )
        }
    }
}

@Composable
private fun StepIndicatorBar(
    currentStep: Int,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        StepNode(stepNumber = 1, label = "Kasus", isCurrent = currentStep == 1, isCompleted = currentStep > 1)
        Box(
            modifier = Modifier
                .weight(1f)
                .height(2.dp)
                .background(if (currentStep >= 2) EmeraldDeep else Color(0xFFEBE5DC))
        )
        StepNode(stepNumber = 2, label = "Input Data", isCurrent = currentStep == 2, isCompleted = currentStep > 2)
        Box(
            modifier = Modifier
                .weight(1f)
                .height(2.dp)
                .background(if (currentStep >= 3) EmeraldDeep else Color(0xFFEBE5DC))
        )
        StepNode(stepNumber = 3, label = "Hasil", isCurrent = currentStep == 3, isCompleted = currentStep > 3)
    }
}

@Composable
private fun StepNode(
    stepNumber: Int,
    label: String,
    isCurrent: Boolean,
    isCompleted: Boolean
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(if (isCurrent || isCompleted) EmeraldDeep else Color(0xFFEBE5DC)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = if (isCompleted) "✓" else stepNumber.toString(),
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = if (isCurrent || isCompleted) Color.White else Color(0xFF5A605B)
                )
            )
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 11.sp,
                fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium
            ),
            color = if (isCurrent) EmeraldDeep else Color(0xFF5A605B)
        )
    }
}

@Composable
private fun CalculationDetailRow(
    label: String,
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.5.sp),
            color = Color(0xFF6B726C)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall.copy(
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp
            ),
            color = Color(0xFF1E211F)
        )
    }
}

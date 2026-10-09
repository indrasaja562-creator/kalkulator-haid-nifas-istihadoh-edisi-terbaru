package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import com.example.data.entities.DailyBloodLogEntity
import com.example.model.*
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.compose.ui.platform.LocalContext
import com.example.ui.components.AppHeroHeader
import com.example.ui.theme.EmeraldDeep
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.GoldTertiary
import com.example.ui.tour.GuidedTourCoordinator
import com.example.ui.tour.tourTarget
import com.example.ui.viewmodel.FiqihViewModel
import java.util.*

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun CalendarScreen(
    viewModel: FiqihViewModel,
    modifier: Modifier = Modifier
) {
    val year by viewModel.calendarYear.collectAsState()
    val month by viewModel.calendarMonth.collectAsState()
    val selectedDate by viewModel.selectedDateString.collectAsState()
    val calendarDays by viewModel.calendarDays.collectAsState()
    val cyclePrediction by viewModel.cyclePrediction.collectAsState()
    val bloodLogs by viewModel.allBloodLogs.collectAsState()
    val saveMessage by viewModel.saveMessage.collectAsState()

    // Notification alert states
    val notificationEnabled by viewModel.cycleNotificationEnabled.collectAsState()
    val daysBeforeAlert by viewModel.cycleNotificationDaysBefore.collectAsState()

    val context = LocalContext.current
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            viewModel.setCycleNotificationEnabled(true)
        }
    }

    val requestNotificationPermission = { onGranted: () -> Unit ->
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            } else {
                onGranted()
            }
        } else {
            onGranted()
        }
    }

    val currentDayLog = remember(selectedDate, bloodLogs) {
        bloodLogs.find { it.dateString == selectedDate }
    }

    var showQuickLogSheet by remember { mutableStateOf(false) }
    var showLegendDialog by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }
    val listState = rememberLazyListState()
    val activeTourTarget = GuidedTourCoordinator.currentActiveTargetKey

    LaunchedEffect(activeTourTarget) {
        if (activeTourTarget == "tour_calendar_grid") {
            listState.animateScrollToItem(1)
        }
    }

    LaunchedEffect(saveMessage) {
        saveMessage?.let {
            snackbarHostState.showSnackbar(it)
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFFAF7F2))
            .testTag("calendar_screen")
    ) {
        Scaffold(
            containerColor = Color.Transparent,
            snackbarHost = { SnackbarHost(snackbarHostState) },
            modifier = Modifier.fillMaxSize()
        ) { paddingValues ->
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 20.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                // 1. Simple Page Header (No large banner)
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .testTag("calendar_hero_banner"),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "Kalender Siklus & Fiqih",
                            style = MaterialTheme.typography.headlineSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 22.sp,
                                letterSpacing = (-0.3).sp
                            ),
                            color = Color(0xFF1E211F)
                        )
                        Text(
                            text = "Pencatatan darah harian, prediksi suci, dan pengingat ibadah",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontSize = 13.sp,
                                lineHeight = 19.sp
                            ),
                            color = Color(0xFF6B726C)
                        )
                    }
                }

                // 2. Calendar Month Grid Card (Interactive Date Clicks)
                item {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("calendar_grid_card")
                            .tourTarget("tour_calendar_grid"),
                        shape = RoundedCornerShape(20.dp),
                        color = Color.White,
                        border = BorderStroke(1.dp, Color(0xFFEBE5DC)),
                        shadowElevation = 1.dp
                    ) {
                        Column(modifier = Modifier.padding(18.dp)) {
                            // Month & Navigation Header
                            CalendarMonthHeader(
                                year = year,
                                month = month,
                                onPrev = { viewModel.prevCalendarMonth() },
                                onNext = { viewModel.nextCalendarMonth() },
                                onToday = { viewModel.goToToday() }
                            )

                            Spacer(modifier = Modifier.height(14.dp))

                            // Day of week labels
                            DayOfWeekHeaderRow()

                            Spacer(modifier = Modifier.height(10.dp))

                            // Calendar Grid Days - Clicking opens the simplified quick logging sheet!
                            CalendarGrid(
                                days = calendarDays,
                                selectedDate = selectedDate,
                                onDayClick = { dayItem ->
                                    viewModel.selectCalendarDate(dayItem.dateString)
                                    showQuickLogSheet = true
                                },
                                onDayLongClick = { dayItem ->
                                    viewModel.quickToggleBloodStatusForDate(dayItem.dateString)
                                }
                            )

                            Spacer(modifier = Modifier.height(14.dp))

                            // Mini Indicator Legend Bar
                            CalendarLegendRow(onOpenLegend = { showLegendDialog = true })
                        }
                    }
                }

                // 3. Cycle Information, Prediction & Reminders Card
                item {
                    CyclePredictionCard(
                        prediction = cyclePrediction,
                        notificationEnabled = notificationEnabled,
                        daysBeforeAlert = daysBeforeAlert,
                        onNotificationEnabledChange = { enabled ->
                            if (enabled) {
                                requestNotificationPermission {
                                    viewModel.setCycleNotificationEnabled(true)
                                }
                            } else {
                                viewModel.setCycleNotificationEnabled(false)
                            }
                        },
                        onDaysBeforeChange = { viewModel.setCycleNotificationDaysBefore(it) },
                        onTestNotification = {
                            requestNotificationPermission {
                                viewModel.sendTestCycleNotification()
                            }
                        },
                        onInfoClick = { showLegendDialog = true }
                    )
                }

                // 4. Compact Selected Date Card & Direct Action Buttons
                item {
                    SelectedDateSummaryCard(
                        dateString = selectedDate,
                        currentLog = currentDayLog,
                        onOpenQuickLog = { showQuickLogSheet = true },
                        onQuickToggle = { viewModel.quickToggleBloodStatusForDate(selectedDate) },
                        onSyncToCalculator = {
                            try {
                                val parsed = CalendarDateHelper.ISO_FORMAT.parse(selectedDate)
                                if (parsed != null) {
                                    viewModel.setStartDate(parsed.time)
                                    viewModel.setEndDate(parsed.time + 6 * 24 * 3600_000L)
                                    viewModel.setSelectedTab(0)
                                }
                            } catch (e: Exception) {
                                viewModel.setSelectedTab(0)
                            }
                        }
                    )
                }
            }
        }
    }

    // Simplified Bottom Sheet triggered directly by clicking a date
    if (showQuickLogSheet) {
        QuickBloodLogBottomSheet(
            dateString = selectedDate,
            currentLog = currentDayLog,
            onDismiss = { showQuickLogSheet = false },
            onSaveLog = { caseType, hasBlood, bloodColor, intensity, start, stop, isPaused, notes ->
                viewModel.saveDailyBloodLog(
                    dateString = selectedDate,
                    caseType = caseType,
                    hasBlood = hasBlood,
                    bloodColor = bloodColor,
                    flowIntensity = intensity,
                    startTime = start,
                    stopTime = stop,
                    isPaused = isPaused,
                    notes = notes
                )
                showQuickLogSheet = false
            },
            onDeleteLog = {
                viewModel.deleteDailyBloodLog(selectedDate)
                showQuickLogSheet = false
            }
        )
    }

    // Fiqih & Color Legend Dialog
    if (showLegendDialog) {
        FiqihLegendDialog(onDismiss = { showLegendDialog = false })
    }
}

/**
 * Cycle Prediction Card with integrated Local Notification Scheduler for next cycle alert.
 */
@Composable
fun CyclePredictionCard(
    prediction: CyclePredictionSummary,
    notificationEnabled: Boolean,
    daysBeforeAlert: Int,
    onNotificationEnabledChange: (Boolean) -> Unit,
    onDaysBeforeChange: (Int) -> Unit,
    onTestNotification: () -> Unit,
    onInfoClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("cycle_prediction_card"),
        shape = RoundedCornerShape(20.dp),
        color = Color.White,
        border = BorderStroke(1.dp, Color(0xFFEBE5DC)),
        shadowElevation = 1.dp
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .clip(CircleShape)
                            .background(
                                if (prediction.isActiveHaid) Color(0xFF9E2A2B) else EmeraldPrimary
                            )
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = prediction.currentStatusTitle,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = if (prediction.isActiveHaid)
                                Color(0xFF9E2A2B)
                            else
                                EmeraldPrimary
                        )
                    )
                }

                IconButton(
                    onClick = onInfoClick,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Info,
                        contentDescription = "Penjelasan Fiqih",
                        tint = Color(0xFF6B726C)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = prediction.currentStatusDesc,
                style = MaterialTheme.typography.bodyMedium.copy(
                    lineHeight = 20.sp,
                    fontSize = 13.sp,
                    color = Color(0xFF3E433F)
                )
            )

            Spacer(modifier = Modifier.height(14.dp))
            HorizontalDivider(color = Color(0xFFEBE5DC).copy(alpha = 0.6f))
            Spacer(modifier = Modifier.height(14.dp))

            // Two prediction highlight cards side by side
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Card 1: Prediksi Berhenti (jika aktif) atau Prediksi Keluar Berikutnya
                Surface(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp),
                    color = Color(0xFFFBF9F5),
                    border = BorderStroke(1.dp, Color(0xFFEBE5DC))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (prediction.isActiveHaid) Icons.Filled.StopCircle else Icons.Filled.PlayArrow,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = if (prediction.isActiveHaid) Color(0xFF9E2A2B) else EmeraldPrimary
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (prediction.isActiveHaid) "Prediksi Berhenti" else "Prediksi Datang",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 11.sp),
                                color = Color(0xFF6B726C)
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (prediction.isActiveHaid) {
                                prediction.predictedStopDateText ?: "-"
                            } else {
                                prediction.predictedNextHaidStartText ?: "-"
                            },
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold, fontSize = 13.sp),
                            color = Color(0xFF1E211F)
                        )
                        prediction.daysUntilNextHaid?.let { days ->
                            if (!prediction.isActiveHaid && days > 0) {
                                Text(
                                    text = "$days hari lagi",
                                    style = MaterialTheme.typography.labelSmall.copy(color = EmeraldPrimary, fontWeight = FontWeight.SemiBold)
                                )
                            }
                        }
                    }
                }

                // Card 2: Siklus Berikutnya / Prediksi Selesai
                Surface(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp),
                    color = Color(0xFFFBF9F5),
                    border = BorderStroke(1.dp, Color(0xFFEBE5DC))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Filled.EventRepeat,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = GoldTertiary
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (prediction.isActiveHaid) "Siklus Berikutnya" else "Prediksi Selesai",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 11.sp),
                                color = Color(0xFF6B726C)
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (prediction.isActiveHaid) {
                                prediction.predictedNextHaidStartText ?: "-"
                            } else {
                                prediction.predictedNextHaidStopText ?: "-"
                            },
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold, fontSize = 13.sp),
                            color = Color(0xFF1E211F)
                        )
                        Text(
                            text = "Kaidah Mazhab Syafi'i",
                            style = MaterialTheme.typography.labelSmall.copy(color = Color(0xFF8A908A), fontSize = 10.5.sp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))
            HorizontalDivider(color = Color(0xFFEBE5DC).copy(alpha = 0.6f))
            Spacer(modifier = Modifier.height(12.dp))

            // LOCAL NOTIFICATION SCHEDULER SECTION
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("cycle_notification_card"),
                shape = RoundedCornerShape(14.dp),
                color = Color(0xFFFBF9F5),
                border = BorderStroke(1.dp, Color(0xFFEBE5DC))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (notificationEnabled)
                                            Color(0xFFE8F4EE)
                                        else
                                            Color(0xFFF0ECE6)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (notificationEnabled) Icons.Filled.NotificationsActive else Icons.Filled.NotificationsOff,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp),
                                    tint = if (notificationEnabled) EmeraldPrimary else Color(0xFF8A908A)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Pengingat Siklus Haid",
                                    style = MaterialTheme.typography.titleSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.5.sp
                                    ),
                                    color = Color(0xFF1E211F)
                                )
                                Text(
                                    text = if (notificationEnabled)
                                        "Alarm terjadwal ($daysBeforeAlert hari sebelum haid)"
                                    else
                                        "Pengingat dinonaktifkan",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontSize = 11.5.sp,
                                        color = if (notificationEnabled) EmeraldPrimary else Color(0xFF8A908A)
                                    )
                                )
                            }
                        }

                        Switch(
                            checked = notificationEnabled,
                            onCheckedChange = onNotificationEnabledChange,
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = EmeraldPrimary,
                                uncheckedThumbColor = Color.White,
                                uncheckedTrackColor = Color(0xFFD3CCC3)
                            ),
                            modifier = Modifier.testTag("cycle_notification_switch")
                        )
                    }

                    if (notificationEnabled) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "Beri tahu saya sebelum siklus tiba:",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium, fontSize = 11.sp),
                            color = Color(0xFF6B726C)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf(1, 2, 3, 5).forEach { days ->
                                val isSelected = (daysBeforeAlert == days)
                                Surface(
                                    modifier = Modifier.clickable { onDaysBeforeChange(days) },
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (isSelected) EmeraldPrimary else Color.White,
                                    border = BorderStroke(
                                        1.dp,
                                        if (isSelected) GoldTertiary else Color(0xFFEBE5DC)
                                    )
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        if (isSelected) {
                                            Icon(
                                                Icons.Filled.Check,
                                                contentDescription = null,
                                                modifier = Modifier.size(12.dp),
                                                tint = Color.White
                                            )
                                        }
                                        Text(
                                            text = "$days Hari",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                fontSize = 11.5.sp
                                            ),
                                            color = if (isSelected) Color.White else Color(0xFF454B46)
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Berdasarkan historis Room database",
                                style = MaterialTheme.typography.labelSmall.copy(color = Color(0xFF8A908A), fontSize = 10.5.sp),
                                modifier = Modifier.weight(1f)
                            )
                            Button(
                                onClick = onTestNotification,
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                                modifier = Modifier.testTag("test_cycle_notification_btn")
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Send,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Uji Notifikasi", style = MaterialTheme.typography.labelSmall.copy(color = Color.White))
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Compact Selected Date Summary Card with 1-click action triggers.
 */
@Composable
fun SelectedDateSummaryCard(
    dateString: String,
    currentLog: DailyBloodLogEntity?,
    onOpenQuickLog: () -> Unit,
    onQuickToggle: () -> Unit,
    onSyncToCalculator: () -> Unit
) {
    val formattedDate = remember(dateString) {
        try {
            val parsed = CalendarDateHelper.ISO_FORMAT.parse(dateString)
            if (parsed != null) CalendarDateHelper.DISPLAY_DAY_DATE.format(parsed) else dateString
        } catch (e: Exception) {
            dateString
        }
    }

    val hasBlood = currentLog?.hasBlood == true
    val colorEnum = if (hasBlood) {
        CalendarBloodColor.values().find { it.idName == currentLog?.bloodColor } ?: CalendarBloodColor.MERAH
    } else null

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("selected_date_card"),
        shape = RoundedCornerShape(20.dp),
        color = Color.White,
        border = BorderStroke(1.dp, Color(0xFFEBE5DC)),
        shadowElevation = 1.dp
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "JURNAL TANGGAL",
                        style = MaterialTheme.typography.labelSmall.copy(
                            letterSpacing = 1.2.sp,
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.5.sp
                        ),
                        color = GoldTertiary
                    )
                    Text(
                        text = formattedDate,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        ),
                        color = Color(0xFF1E211F)
                    )
                }

                // Status Badge
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = when {
                        hasBlood -> Color(0xFFF9ECEE)
                        currentLog != null -> Color(0xFFE8F4EE)
                        else -> Color(0xFFFAF7F2)
                    },
                    border = BorderStroke(
                        1.dp,
                        when {
                            hasBlood -> Color(0xFF9E2A2B).copy(alpha = 0.5f)
                            currentLog != null -> EmeraldPrimary.copy(alpha = 0.5f)
                            else -> Color(0xFFEBE5DC)
                        }
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(
                                    when {
                                        hasBlood -> Color(colorEnum?.colorHex ?: 0xFF9E2A2B)
                                        currentLog != null -> EmeraldPrimary
                                        else -> Color(0xFF8A908A)
                                    }
                                )
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = when {
                                hasBlood -> "${currentLog?.caseType} (${colorEnum?.label})"
                                currentLog != null -> "SUCI"
                                else -> "Belum Dicatat"
                            },
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp
                            ),
                            color = when {
                                hasBlood -> Color(0xFF9E2A2B)
                                currentLog != null -> EmeraldPrimary
                                else -> Color(0xFF6B726C)
                            }
                        )
                    }
                }
            }

            if (hasBlood && currentLog != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Waktu keluar: ${currentLog.startTime} - ${currentLog.stopTime} • Aliran: ${currentLog.flowIntensity}" +
                            (if (currentLog.notes.isNotEmpty()) " • \"${currentLog.notes}\"" else ""),
                    style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF6B726C), fontSize = 12.sp)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Action buttons row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onOpenQuickLog,
                    modifier = Modifier
                        .weight(1.3f)
                        .testTag("btn_open_quick_log"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
                ) {
                    Icon(Icons.Filled.EditCalendar, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.White)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Catat / Edit (1-Klik)", style = MaterialTheme.typography.labelMedium.copy(color = Color.White, fontWeight = FontWeight.SemiBold))
                }

                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onQuickToggle() }
                        .testTag("btn_quick_toggle_blood"),
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFFAF7F2),
                    border = BorderStroke(1.dp, Color(0xFFEBE5DC))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = if (hasBlood) Icons.Filled.DoneAll else Icons.Filled.WaterDrop,
                            contentDescription = null,
                            tint = if (hasBlood) EmeraldPrimary else Color(0xFF9E2A2B),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            if (hasBlood) "Set Suci" else "Set Haid",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1E211F)
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedButton(
                onClick = onSyncToCalculator,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("btn_sync_calc_compact"),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, GoldTertiary.copy(alpha = 0.6f))
            ) {
                Icon(Icons.Filled.Calculate, contentDescription = null, modifier = Modifier.size(16.dp), tint = EmeraldPrimary)
                Spacer(modifier = Modifier.width(6.dp))
                Text("Analisis Fiqih Lengkap di Kalkulator", style = MaterialTheme.typography.labelSmall.copy(color = EmeraldPrimary, fontWeight = FontWeight.SemiBold))
            }
        }
    }
}

/**
 * Simplified Bottom Sheet that opens when clicking ANY date on the calendar.
 * Provides intuitive, 1-click controls for daily blood status, color, time presets, and intensity.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickBloodLogBottomSheet(
    dateString: String,
    currentLog: DailyBloodLogEntity?,
    onDismiss: () -> Unit,
    onSaveLog: (
        caseType: String,
        hasBlood: Boolean,
        bloodColor: String,
        intensity: String,
        startTime: String,
        stopTime: String,
        isPaused: Boolean,
        notes: String
    ) -> Unit,
    onDeleteLog: () -> Unit
) {
    val formattedDateText = remember(dateString) {
        try {
            val parsed = CalendarDateHelper.ISO_FORMAT.parse(dateString)
            if (parsed != null) CalendarDateHelper.DISPLAY_DAY_DATE.format(parsed) else dateString
        } catch (e: Exception) {
            dateString
        }
    }

    var hasBlood by remember(dateString, currentLog) {
        mutableStateOf(currentLog?.hasBlood ?: true)
    }
    var caseType by remember(dateString, currentLog) {
        mutableStateOf(currentLog?.caseType ?: "HAID")
    }
    var selectedColor by remember(dateString, currentLog) {
        mutableStateOf(currentLog?.bloodColor ?: "MERAH")
    }
    var flowIntensity by remember(dateString, currentLog) {
        mutableStateOf(currentLog?.flowIntensity ?: "SEDANG")
    }
    var startTime by remember(dateString, currentLog) {
        mutableStateOf(currentLog?.startTime ?: "08:00")
    }
    var stopTime by remember(dateString, currentLog) {
        mutableStateOf(currentLog?.stopTime ?: "20:00")
    }
    var isPaused by remember(dateString, currentLog) {
        mutableStateOf(currentLog?.isPaused ?: false)
    }
    var notes by remember(dateString, currentLog) {
        mutableStateOf(currentLog?.notes ?: "")
    }

    var showCustomTimeDialogForStart by remember { mutableStateOf(false) }
    var showCustomTimeDialogForStop by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
                .testTag("quick_blood_bottom_sheet")
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Catat Darah Harian",
                        style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.primary)
                    )
                    Text(
                        text = formattedDateText,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Filled.Close, contentDescription = "Tutup")
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 1. OPSI STATUS HARIAN (1-KLIK)
            Text(
                text = "1. Kondisi Darah Hari Ini (1-Klik):",
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
            )
            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Tombol Ada Darah
                Surface(
                    onClick = { hasBlood = true },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("btn_status_blood"),
                    shape = RoundedCornerShape(12.dp),
                    color = if (hasBlood) Color(0xFFD32F2F).copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    border = BorderStroke(
                        if (hasBlood) 2.dp else 1.dp,
                        if (hasBlood) Color(0xFFD32F2F) else MaterialTheme.colorScheme.outlineVariant
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.WaterDrop,
                            contentDescription = null,
                            tint = if (hasBlood) Color(0xFFD32F2F) else MaterialTheme.colorScheme.outline,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Ada Darah",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = if (hasBlood) FontWeight.Bold else FontWeight.Normal,
                                color = if (hasBlood) Color(0xFFD32F2F) else MaterialTheme.colorScheme.onSurface
                            )
                        )
                    }
                }

                // Tombol Suci
                Surface(
                    onClick = { hasBlood = false },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("btn_status_suci"),
                    shape = RoundedCornerShape(12.dp),
                    color = if (!hasBlood) Color(0xFF2E7D32).copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    border = BorderStroke(
                        if (!hasBlood) 2.dp else 1.dp,
                        if (!hasBlood) Color(0xFF2E7D32) else MaterialTheme.colorScheme.outlineVariant
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.DoneAll,
                            contentDescription = null,
                            tint = if (!hasBlood) Color(0xFF2E7D32) else MaterialTheme.colorScheme.outline,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Bersih / Suci",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = if (!hasBlood) FontWeight.Bold else FontWeight.Normal,
                                color = if (!hasBlood) Color(0xFF2E7D32) else MaterialTheme.colorScheme.onSurface
                            )
                        )
                    }
                }
            }

            // Expanded details for Blood
            AnimatedVisibility(visible = hasBlood) {
                Column {
                    Spacer(modifier = Modifier.height(14.dp))

                    // 2. PILIHAN WARNA DARAH 1-KLIK (7 Hierarki Kaidah Fiqih)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "2. Warna Darah (Klik langsung):",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = CalendarBloodColor.values().find { it.idName == selectedColor }?.fiqihStrength ?: "",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))

                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(CalendarBloodColor.values().toList()) { colorItem ->
                            val isSelected = (selectedColor == colorItem.idName)
                            Surface(
                                onClick = { selectedColor = colorItem.idName },
                                shape = RoundedCornerShape(10.dp),
                                color = if (isSelected) Color(colorItem.colorHex).copy(alpha = 0.18f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                border = BorderStroke(
                                    if (isSelected) 2.dp else 1.dp,
                                    if (isSelected) Color(colorItem.colorHex) else MaterialTheme.colorScheme.outlineVariant
                                )
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(14.dp)
                                            .clip(CircleShape)
                                            .background(Color(colorItem.colorHex))
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = colorItem.label,
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                        )
                                    )
                                    if (isSelected) {
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Icon(
                                            Icons.Filled.Check,
                                            contentDescription = null,
                                            modifier = Modifier.size(12.dp),
                                            tint = Color(colorItem.colorHex)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // 3. WAKTU KELUAR & WAKTU MAMPET
                    Text(
                        text = "3. Waktu Keluar & Waktu Mampet:",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Mulai
                        Surface(
                            onClick = { showCustomTimeDialogForStart = true },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text("Mulai Keluar", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                                Spacer(modifier = Modifier.height(2.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Filled.AccessTime, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(startTime, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                                }
                            }
                        }

                        // Mampet
                        Surface(
                            onClick = { showCustomTimeDialogForStop = true },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text("Waktu Mampet", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                                Spacer(modifier = Modifier.height(2.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Filled.TimerOff, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color(0xFFD32F2F))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(stopTime, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // Quick 1-tap time presets
                    Text("Pilihan Waktu 1-Klik:", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        val presets = listOf(
                            Triple("Pagi-Sore", "07:00", "17:00"),
                            Triple("Siang-Malam", "12:00", "21:00"),
                            Triple("24 Jam", "00:00", "23:59")
                        )
                        presets.forEach { (label, s, e) ->
                            SuggestionChip(
                                onClick = {
                                    startTime = s
                                    stopTime = e
                                },
                                label = { Text(label, style = MaterialTheme.typography.labelSmall) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // 4. INTENSITAS & KATEGORI CEPAT
                    Text(
                        text = "4. Intensitas & Kategori:",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf("DERAS", "SEDANG", "SEDIKIT", "FLEK").forEach { intensity ->
                            FilterChip(
                                selected = (flowIntensity == intensity),
                                onClick = { flowIntensity = intensity },
                                label = { Text(intensity.lowercase().replaceFirstChar { it.uppercase() }, style = MaterialTheme.typography.labelSmall) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // Quick symptom toggles
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        FilterChip(
                            selected = isPaused,
                            onClick = { isPaused = !isPaused },
                            label = { Text("Sempat Mampet", style = MaterialTheme.typography.labelSmall) },
                            leadingIcon = if (isPaused) {
                                { Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(12.dp)) }
                            } else null
                        )

                        listOf("Kram Perut", "Darah Kental").forEach { tag ->
                            val contains = notes.contains(tag)
                            FilterChip(
                                selected = contains,
                                onClick = {
                                    notes = if (contains) {
                                        notes.replace(tag, "").trim()
                                    } else {
                                        if (notes.isEmpty()) tag else "$notes, $tag"
                                    }
                                },
                                label = { Text(tag, style = MaterialTheme.typography.labelSmall) }
                            )
                        }
                    }
                }
            }

            AnimatedVisibility(visible = !hasBlood) {
                Column {
                    Spacer(modifier = Modifier.height(14.dp))
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFF2E7D32).copy(alpha = 0.08f),
                        border = BorderStroke(1.dp, Color(0xFF2E7D32).copy(alpha = 0.3f))
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Filled.Verified, contentDescription = null, tint = Color(0xFF2E7D32))
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Hari Bersih / Masa Naqa'",
                                    style = MaterialTheme.typography.titleSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF2E7D32)
                                    )
                                )
                                Text(
                                    text = "Hari ini tidak keluar darah. Wajib mandi besar jika haid telah tuntas dan wajib melaksanakan ibadah shalat/puasa.",
                                    style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurface)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Action Buttons
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = {
                        onSaveLog(
                            if (hasBlood) caseType else "SUCI",
                            hasBlood,
                            if (hasBlood) selectedColor else "BENING",
                            if (hasBlood) flowIntensity else "FLEK",
                            startTime,
                            stopTime,
                            isPaused,
                            notes
                        )
                    },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("btn_save_quick_log"),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = EmeraldPrimary,
                        contentColor = Color.White
                    )
                ) {
                    Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Simpan Catatan", fontWeight = FontWeight.Bold)
                }

                if (currentLog != null) {
                    OutlinedButton(
                        onClick = onDeleteLog,
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.testTag("btn_delete_quick_log")
                    ) {
                        Icon(Icons.Filled.Delete, contentDescription = null)
                    }
                }
            }
        }
    }

    if (showCustomTimeDialogForStart) {
        QuickTimePickerDialog(
            title = "Waktu Mulai Keluar Darah",
            initialTime = startTime,
            onDismiss = { showCustomTimeDialogForStart = false },
            onConfirm = {
                startTime = it
                showCustomTimeDialogForStart = false
            }
        )
    }

    if (showCustomTimeDialogForStop) {
        QuickTimePickerDialog(
            title = "Waktu Mampet / Berhenti",
            initialTime = stopTime,
            onDismiss = { showCustomTimeDialogForStop = false },
            onConfirm = {
                stopTime = it
                showCustomTimeDialogForStop = false
            }
        )
    }
}

@Composable
fun CalendarMonthHeader(
    year: Int,
    month: Int,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    onToday: () -> Unit
) {
    val monthCal = Calendar.getInstance().apply {
        set(Calendar.YEAR, year)
        set(Calendar.MONTH, month)
        set(Calendar.DAY_OF_MONTH, 1)
    }
    val monthName = CalendarDateHelper.DISPLAY_MONTH_YEAR.format(monthCal.time)

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(
                onClick = onPrev,
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFFAF7F2))
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Bulan Sebelumnya", tint = EmeraldDeep, modifier = Modifier.size(18.dp))
            }
            Text(
                text = monthName,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, fontSize = 16.sp),
                color = Color(0xFF1E211F),
                modifier = Modifier.padding(horizontal = 8.dp)
            )
            IconButton(
                onClick = onNext,
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFFAF7F2))
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Bulan Berikutnya", tint = EmeraldDeep, modifier = Modifier.size(18.dp))
            }
        }

        Button(
            onClick = onToday,
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
            shape = RoundedCornerShape(10.dp),
            colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
        ) {
            Text("Hari Ini", style = MaterialTheme.typography.labelSmall.copy(color = Color.White, fontWeight = FontWeight.SemiBold))
        }
    }
}

@Composable
fun DayOfWeekHeaderRow() {
    val days = listOf("Min", "Sen", "Sel", "Rab", "Kam", "Jum", "Sab")
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceAround
    ) {
        days.forEachIndexed { index, day ->
            Text(
                text = day,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.SemiBold,
                    color = if (index == 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.outline
                ),
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun CalendarGrid(
    days: List<CalendarDayItem>,
    selectedDate: String,
    onDayClick: (CalendarDayItem) -> Unit,
    onDayLongClick: (CalendarDayItem) -> Unit = {}
) {
    val rows = days.chunked(7)

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        rows.forEach { week ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                week.forEach { dayItem ->
                    CalendarDayCell(
                        item = dayItem,
                        isSelected = (dayItem.dateString == selectedDate),
                        onClick = { onDayClick(dayItem) },
                        onLongClick = { onDayLongClick(dayItem) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CalendarDayCell(
    item: CalendarDayItem,
    isSelected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val hasBlood = item.log?.hasBlood == true
    val bloodColorEnum = if (hasBlood) {
        CalendarBloodColor.values().find { it.idName == item.log?.bloodColor } ?: CalendarBloodColor.MERAH
    } else null

    val isPredicted = item.isPredictedHaid
    val isPredictedStop = item.isPredictedStopDay
    val isRecordedSuci = item.log != null && !item.log.hasBlood
    val isMasaSuci = isRecordedSuci || (item.isPredictedSuci && item.isCurrentMonth && !hasBlood && !isPredicted)
    val hasNotes = item.log?.notes?.isNotBlank() == true

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .aspectRatio(1f)
            .padding(2.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(
                when {
                    isSelected -> GoldTertiary.copy(alpha = 0.20f)
                    hasBlood -> Color(bloodColorEnum?.colorHex ?: 0xFF9E2A2B).copy(alpha = 0.12f)
                    isPredicted -> Color(0xFFF48FB1).copy(alpha = 0.12f)
                    isRecordedSuci -> EmeraldPrimary.copy(alpha = 0.10f)
                    isMasaSuci -> Color(0xFF4CAF50).copy(alpha = 0.05f)
                    item.isToday -> EmeraldPrimary.copy(alpha = 0.12f)
                    else -> Color.Transparent
                }
            )
            .border(
                width = when {
                    isSelected -> 1.5.dp
                    item.isToday -> 1.dp
                    hasBlood -> 0.75.dp
                    isPredictedStop -> 1.dp
                    isPredicted -> 0.5.dp
                    isRecordedSuci -> 0.75.dp
                    else -> 0.dp
                },
                color = when {
                    isSelected -> GoldTertiary
                    item.isToday -> EmeraldPrimary
                    hasBlood -> Color(bloodColorEnum?.colorHex ?: 0xFF9E2A2B).copy(alpha = 0.5f)
                    isPredictedStop -> Color(0xFFD81B60).copy(alpha = 0.6f)
                    isPredicted -> Color(0xFFF06292).copy(alpha = 0.4f)
                    isRecordedSuci -> EmeraldPrimary.copy(alpha = 0.4f)
                    else -> Color.Transparent
                },
                shape = RoundedCornerShape(10.dp)
            )
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxSize()
        ) {
            // Day Number
            Text(
                text = item.dayOfMonth.toString(),
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = if (item.isToday || isSelected || hasBlood || isRecordedSuci) FontWeight.SemiBold else FontWeight.Normal,
                    color = when {
                        !item.isCurrentMonth -> MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                        hasBlood -> Color(bloodColorEnum?.colorHex ?: 0xFFD32F2F)
                        isPredicted -> Color(0xFFC2185B)
                        isRecordedSuci -> Color(0xFF2E7D32)
                        isMasaSuci -> Color(0xFF388E3C)
                        item.isToday -> MaterialTheme.colorScheme.primary
                        else -> MaterialTheme.colorScheme.onSurface
                    }
                )
            )

            Spacer(modifier = Modifier.height(2.dp))

            // Visual Indicators: Refined subtle dots row
            Row(
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (hasBlood && bloodColorEnum != null) {
                    // Haid Tercatat
                    Box(
                        modifier = Modifier
                            .size(5.dp)
                            .clip(CircleShape)
                            .background(Color(bloodColorEnum.colorHex))
                    )
                } else if (isPredictedStop) {
                    // Prediksi Tuntas Haid
                    Box(
                        modifier = Modifier
                            .size(5.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFD81B60))
                    )
                } else if (isPredicted) {
                    // Prediksi Haid
                    Box(
                        modifier = Modifier
                            .size(4.5.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFF06292))
                    )
                } else if (isRecordedSuci) {
                    // Suci Tercatat
                    Box(
                        modifier = Modifier
                            .size(4.5.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF2E7D32))
                    )
                } else if (isMasaSuci) {
                    // Masa Suci Siklus
                    Box(
                        modifier = Modifier
                            .size(4.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF66BB6A).copy(alpha = 0.7f))
                    )
                }

                // Catatan tambahan
                if (hasNotes) {
                    Box(
                        modifier = Modifier
                            .size(3.5.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFFFA000))
                    )
                }
            }
        }
    }
}

@Composable
fun CalendarLegendRow(onOpenLegend: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onOpenLegend)
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            LegendItem(color = Color(0xFFD32F2F), label = "Haid Tercatat")
            LegendItem(color = Color(0xFFF06292), label = "Prediksi Haid")
            LegendItem(color = Color(0xFF2E7D32), label = "Masa Suci")
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = "Panduan & Simbol",
                style = MaterialTheme.typography.labelSmall.copy(
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )
            )
            Icon(
                Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                modifier = Modifier.size(12.dp),
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
fun LegendItem(
    color: Color,
    label: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(color)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium
            )
        )
    }
}

@Composable
fun QuickTimePickerDialog(
    title: String,
    initialTime: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    val parts = initialTime.split(":")
    var selectedHour by remember { mutableStateOf(parts.getOrNull(0)?.toIntOrNull() ?: 8) }
    var selectedMinute by remember { mutableStateOf(parts.getOrNull(1)?.toIntOrNull() ?: 0) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, style = MaterialTheme.typography.titleMedium) },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = String.format(Locale.US, "%02d:%02d", selectedHour, selectedMinute),
                    style = MaterialTheme.typography.displayMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Presets jam umum
                Text("Pilihan Cepat Waktu:", style = MaterialTheme.typography.labelSmall)
                Spacer(modifier = Modifier.height(8.dp))

                val presetTimes = listOf(
                    Pair(5, 0), Pair(7, 30), Pair(9, 0), Pair(12, 0),
                    Pair(15, 30), Pair(18, 0), Pair(20, 0), Pair(23, 0)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    Column {
                        presetTimes.chunked(4).forEach { rowPresets ->
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                rowPresets.forEach { (h, m) ->
                                    val timeStr = String.format(Locale.US, "%02d:%02d", h, m)
                                    val isSelected = (selectedHour == h && selectedMinute == m)
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = {
                                            selectedHour = h
                                            selectedMinute = m
                                        },
                                        label = { Text(timeStr, style = MaterialTheme.typography.labelSmall) }
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Jam Slider
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Jam: $selectedHour", modifier = Modifier.width(60.dp), style = MaterialTheme.typography.bodySmall)
                    Slider(
                        value = selectedHour.toFloat(),
                        onValueChange = { selectedHour = it.toInt() },
                        valueRange = 0f..23f,
                        steps = 22,
                        modifier = Modifier.weight(1f)
                    )
                }

                // Menit Slider
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Mnt: $selectedMinute", modifier = Modifier.width(60.dp), style = MaterialTheme.typography.bodySmall)
                    Slider(
                        value = selectedMinute.toFloat(),
                        onValueChange = { selectedMinute = it.toInt() },
                        valueRange = 0f..59f,
                        steps = 11,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                onConfirm(String.format(Locale.US, "%02d:%02d", selectedHour, selectedMinute))
            }) {
                Text("Pilih")
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
fun FiqihLegendDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                "Panduan Warna & Prediksi Fiqih",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
        },
        text = {
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    Text(
                        text = "1. Indikator Visual Tanggal Kalender:",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(12.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFD32F2F))
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "🔴 Hari Haid Tercatat: Darah telah dicatat keluar (warna sesuai catatan). Shalat & puasa gugur.",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(12.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFF06292))
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "🌸 Hari Prediksi Haid: Perkiraan hari haid berikutnya berdasarkan adat siklus Anda.",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(12.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF2E7D32))
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "🟢 Masa Suci (Naqa'): Hari suci/bersih dari darah. Wajib melaksanakan shalat dan sah berpuasa.",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFFFA000))
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "🟡 Catatan / Gejala: Terdapat catatan khusus atau gejala yang tersimpan pada tanggal ini.",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                }

                item {
                    Text(
                        text = "2. Hierarki Kekuatan Darah (Tamyiz):",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = "Menurut Mazhab Syafi'i, darah kuat diutamakan atas darah lemah jika memenuhi syarat tamyiz (kuat >= 24 jam, kuat <= 15 hari, lemah >= 15 hari).",
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                items(CalendarBloodColor.values().toList()) { item ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(vertical = 2.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(14.dp)
                                .clip(CircleShape)
                                .background(Color(item.colorHex))
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "${item.label} (${item.fiqihStrength})",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                            )
                            Text(
                                text = item.fiqihDesc,
                                style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                            )
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "3. Kaidah Prediksi Kalender:",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = "• Batas Maksimal Haid: 15 hari 15 malam.\n" +
                                "• Batas Minimal Suci Antara 2 Haid: 15 hari 15 malam.\n" +
                                "• Prediksi berhenti dihitung dari hari pertama keluar ditambah adat kebiasaan haid Anda.\n" +
                                "• Prediksi haid berikutnya dihitung berdasarkan siklus (adat haid + adat suci).\n" +
                                "• Pengingat notifikasi otomatis memberi peringatan beberapa hari sebelum tanggal perkiraan tersebut.",
                        style = MaterialTheme.typography.bodySmall,
                        lineHeight = 18.sp
                    )
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text("Mengerti")
            }
        }
    )
}

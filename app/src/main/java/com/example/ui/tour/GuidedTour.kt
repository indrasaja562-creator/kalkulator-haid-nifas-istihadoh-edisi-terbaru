package com.example.ui.tour

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.Calculate
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.EditCalendar
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

/**
 * Data representation for each guided walkthrough step.
 */
data class GuidedTourStep(
    val id: String,
    val stepIndex: Int,
    val tabIndex: Int,
    val targetKey: String,
    val title: String,
    val description: String,
    val icon: ImageVector,
    val cornerRadiusDp: Float = 16f,
    val paddingDp: Float = 8f
)

/**
 * Global coordinator for guided tour target tracking and scroll requests.
 */
@OptIn(ExperimentalFoundationApi::class)
object GuidedTourCoordinator {
    val targetBounds = mutableStateMapOf<String, Rect>()
    private val requesters = mutableMapOf<String, BringIntoViewRequester>()
    var currentActiveTargetKey by mutableStateOf<String?>(null)

    fun registerTarget(key: String, bounds: Rect) {
        val current = targetBounds[key]
        if (current == null || current != bounds) {
            targetBounds[key] = bounds
        }
    }

    fun registerRequester(key: String, requester: BringIntoViewRequester) {
        requesters[key] = requester
    }

    suspend fun scrollToTarget(key: String) {
        requesters[key]?.bringIntoView()
    }

    fun getTargetBounds(key: String): Rect? = targetBounds[key]
}

/**
 * Modifier to attach an element as a target for the interactive guided tour.
 * Enables both automatic spotlight coordinate calculation and smooth auto-scroll into view.
 */
@OptIn(ExperimentalFoundationApi::class)
fun Modifier.tourTarget(key: String): Modifier = composed {
    val requester = remember(key) { BringIntoViewRequester() }
    DisposableEffect(key) {
        GuidedTourCoordinator.registerRequester(key, requester)
        onDispose { }
    }
    this
        .bringIntoViewRequester(requester)
        .onGloballyPositioned { coordinates ->
            if (coordinates.isAttached) {
                GuidedTourCoordinator.registerTarget(key, coordinates.boundsInRoot())
            }
        }
}

/**
 * Predefined list of comprehensive walkthrough steps across the application.
 */
val APP_GUIDED_TOUR_STEPS = listOf(
    GuidedTourStep(
        id = "calc_type",
        stepIndex = 0,
        tabIndex = 0,
        targetKey = "tour_calc_type",
        title = "Pilih Jenis Perhitungan",
        description = "Tentukan jenis kasus: Haid / Istihadhah atau Nifas pasca bersalin untuk mengaktifkan batas hukum fiqih yang tepat.",
        icon = Icons.Outlined.Tune,
        cornerRadiusDp = 18f,
        paddingDp = 6f
    ),
    GuidedTourStep(
        id = "blood_chronology",
        stepIndex = 1,
        tabIndex = 0,
        targetKey = "tour_blood_chronology",
        title = "Kronologi & Warna Darah",
        description = "Catat rentang waktu dan tingkatan warna darah (hitam, merah, cokelat, dsb.). Sistem otomatis menguji syarat Tamyiz kuat dan lemah.",
        icon = Icons.Outlined.EditCalendar,
        cornerRadiusDp = 18f,
        paddingDp = 6f
    ),
    GuidedTourStep(
        id = "calc_button",
        stepIndex = 2,
        tabIndex = 0,
        targetKey = "tour_calculate_button",
        title = "Hitung Kepastian Fiqih",
        description = "Tekan tombol ini untuk menganalisis darah sesuai 7 golongan istihadhah Mazhab Syafi'i, menentukan kewajiban mandi, shalat, dan qadha.",
        icon = Icons.Outlined.Calculate,
        cornerRadiusDp = 20f,
        paddingDp = 6f
    ),
    GuidedTourStep(
        id = "calendar_tab",
        stepIndex = 3,
        tabIndex = 1,
        targetKey = "tour_calendar_grid",
        title = "Kalender Siklus & Jurnal",
        description = "Visualisasi siklus bulanan dengan titik warna darah, perkiraan masa suci minimal (15 hari), dan catatan riwayat harian Anda.",
        icon = Icons.Outlined.CalendarMonth,
        cornerRadiusDp = 18f,
        paddingDp = 6f
    ),
    GuidedTourStep(
        id = "guide_tab",
        stepIndex = 4,
        tabIndex = 3,
        targetKey = "tour_guide_library",
        title = "Perpustakaan Panduan Fiqih",
        description = "Kumpulan kaidah fiqih mu'tamad: wudhu daimul hadats, mandi wajib, serta tata cara shalat dan puasa wanita istihadhah.",
        icon = Icons.AutoMirrored.Outlined.MenuBook,
        cornerRadiusDp = 18f,
        paddingDp = 6f
    ),
    GuidedTourStep(
        id = "profile_tab",
        stepIndex = 5,
        tabIndex = 4,
        targetKey = "tour_profile_data",
        title = "Profil & Riwayat Siklus",
        description = "Simpan kebiasaan (adat) haid dan suci, bandingkan tren siklus antar bulan, dan atur preferensi tema warna aplikasi.",
        icon = Icons.Outlined.Person,
        cornerRadiusDp = 18f,
        paddingDp = 6f
    )
)

/**
 * Fullscreen Interactive Spotlight Overlay and Tooltip Modal Card.
 */
@Composable
fun GuidedTourOverlay(
    isActive: Boolean,
    currentStepIndex: Int,
    currentTab: Int,
    onNavigateToTab: (Int) -> Unit,
    onNextStep: () -> Unit,
    onPreviousStep: () -> Unit,
    onCloseTour: () -> Unit,
    steps: List<GuidedTourStep> = APP_GUIDED_TOUR_STEPS
) {
    if (!isActive || steps.isEmpty()) return

    val stepIndex = currentStepIndex.coerceIn(0, steps.size - 1)
    val step = steps[stepIndex]
    val totalSteps = steps.size
    val isLastStep = stepIndex == totalSteps - 1

    val density = LocalDensity.current
    val configuration = LocalConfiguration.current
    val screenWidthPx = with(density) { configuration.screenWidthDp.dp.toPx() }
    val screenHeightPx = with(density) { configuration.screenHeightDp.dp.toPx() }

    // When step changes, switch tab if necessary, set active key, and auto-scroll target into view
    LaunchedEffect(step.id) {
        GuidedTourCoordinator.currentActiveTargetKey = step.targetKey
        if (currentTab != step.tabIndex) {
            onNavigateToTab(step.tabIndex)
            delay(280) // allow new tab screen to compose and measure
        }
        GuidedTourCoordinator.scrollToTarget(step.targetKey)
    }

    val targetBounds = GuidedTourCoordinator.getTargetBounds(step.targetKey)
    val hasTargetBounds = targetBounds != null

    // Determine target vertical position:
    // If measured, use actual bounds. Otherwise use an informed approximate fallback (near bottom for calculate button, near top otherwise)
    val fallbackRect = remember(step.id, screenWidthPx, screenHeightPx) {
        if (step.id == "calc_button") {
            Rect(
                left = with(density) { 20.dp.toPx() },
                top = screenHeightPx - with(density) { 150.dp.toPx() },
                right = screenWidthPx - with(density) { 20.dp.toPx() },
                bottom = screenHeightPx - with(density) { 98.dp.toPx() }
            )
        } else {
            Rect(
                left = with(density) { 20.dp.toPx() },
                top = with(density) { 120.dp.toPx() },
                right = screenWidthPx - with(density) { 20.dp.toPx() },
                bottom = with(density) { 220.dp.toPx() }
            )
        }
    }

    val spotlightRect = targetBounds ?: fallbackRect

    // Smoothly animate the spotlight viewport between steps
    val animLeft by animateFloatAsState(targetValue = spotlightRect.left, animationSpec = tween(380), label = "anim_left")
    val animTop by animateFloatAsState(targetValue = spotlightRect.top, animationSpec = tween(380), label = "anim_top")
    val animRight by animateFloatAsState(targetValue = spotlightRect.right, animationSpec = tween(380), label = "anim_right")
    val animBottom by animateFloatAsState(targetValue = spotlightRect.bottom, animationSpec = tween(380), label = "anim_bottom")

    val accentColor = MaterialTheme.colorScheme.primary

    Box(
        modifier = Modifier
            .fillMaxSize()
            .testTag("guided_tour_overlay")
    ) {
        // --- 1. SPOTLIGHT CANVAS (DARK SCRIM WITH EVENODD CUTOUT) ---
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    // Consume touch gestures so user interacts via tour controls
                    detectTapGestures { }
                }
        ) {
            if (hasTargetBounds) {
                val padPx = with(density) { step.paddingDp.dp.toPx() }
                val radiusPx = with(density) { step.cornerRadiusDp.dp.toPx() }

                val padded = Rect(
                    left = (animLeft - padPx).coerceAtLeast(0f),
                    top = (animTop - padPx).coerceAtLeast(0f),
                    right = (animRight + padPx).coerceAtMost(size.width),
                    bottom = (animBottom + padPx).coerceAtMost(size.height)
                )

                // EvenOdd cutout path: dims the background while leaving the target 100% clear
                val cutoutPath = Path().apply {
                    fillType = PathFillType.EvenOdd
                    addRect(Rect(0f, 0f, size.width, size.height))
                    addRoundRect(
                        RoundRect(
                            rect = padded,
                            cornerRadius = CornerRadius(radiusPx, radiusPx)
                        )
                    )
                }

                // Dark semi-transparent scrim with cutout
                drawPath(path = cutoutPath, color = Color.Black.copy(alpha = 0.72f))

                // Spotlight glowing accent border
                drawRoundRect(
                    color = accentColor,
                    topLeft = Offset(padded.left, padded.top),
                    size = Size(padded.width, padded.height),
                    cornerRadius = CornerRadius(radiusPx, radiusPx),
                    style = Stroke(width = with(density) { 2.dp.toPx() })
                )
            } else {
                // If element is not yet positioned/measured (e.g. during scroll), dim screen uniformly without showing a misplaced cutout
                drawRect(color = Color.Black.copy(alpha = 0.72f))
            }
        }

        // --- 2. FLOATING TOOLTIP MODAL CARD ---
        // Determine whether to float above or below target based on target vertical center
        val targetCenterY = (animTop + animBottom) / 2f
        val isTargetInUpperHalf = targetCenterY < (screenHeightPx * 0.52f)

        val padDp = step.paddingDp.dp
        val topOffsetDp = with(density) { (animBottom + padDp.toPx()).toDp() + 14.dp }
        val bottomOffsetDp = with(density) { (screenHeightPx - animTop + padDp.toPx()).toDp() + 14.dp }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp)
                .then(
                    if (isTargetInUpperHalf) {
                        Modifier.padding(
                            top = topOffsetDp.coerceIn(24.dp, configuration.screenHeightDp.dp - 280.dp),
                            bottom = 24.dp
                        )
                    } else {
                        Modifier.padding(
                            bottom = bottomOffsetDp.coerceIn(24.dp, configuration.screenHeightDp.dp - 280.dp),
                            top = 24.dp
                        )
                    }
                ),
            contentAlignment = if (isTargetInUpperHalf) Alignment.TopCenter else Alignment.BottomCenter
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("tour_tooltip_card"),
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 6.dp,
                shadowElevation = 10.dp,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                ) {
                    // Header: Feature Icon, Step Title, and Close Button (X)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = step.icon,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            Text(
                                text = step.title,
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = (-0.2).sp
                                ),
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        IconButton(
                            onClick = onCloseTour,
                            modifier = Modifier
                                .size(32.dp)
                                .testTag("tour_close_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Tutup Tur",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Body: Short Feature Description
                    Text(
                        text = step.description,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            lineHeight = 21.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(18.dp))

                    // Footer: Step Sequence Indicator ("3/6"), "KEMBALI", and "LANJUT" / "SELESAI"
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Step sequence indicator
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
                        ) {
                            Text(
                                text = "${step.stepIndex + 1}/$totalSteps",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.5.sp
                                ),
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (step.stepIndex > 0) {
                                OutlinedButton(
                                    onClick = onPreviousStep,
                                    shape = RoundedCornerShape(12.dp),
                                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                                    modifier = Modifier.testTag("tour_back_button")
                                ) {
                                    Text(
                                        text = "KEMBALI",
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    )
                                }
                            }

                            Button(
                                onClick = onNextStep,
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary
                                ),
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                                modifier = Modifier.testTag("tour_next_button")
                            ) {
                                Text(
                                    text = if (isLastStep) "SELESAI" else "LANJUT",
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = FontWeight.SemiBold
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

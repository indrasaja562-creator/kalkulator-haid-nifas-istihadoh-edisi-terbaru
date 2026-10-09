package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.fiqih.FiqihCalculatorEngine
import com.example.model.*
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun AppHeroHeader(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag("app_hero_header")
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall.copy(
                fontWeight = FontWeight.SemiBold,
                letterSpacing = (-0.4).sp
            ),
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 20.sp),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun StatusBadge(
    status: String,
    color: Color = MaterialTheme.colorScheme.primary
) {
    Surface(
        color = color.copy(alpha = 0.12f),
        shape = RoundedCornerShape(8.dp)
    ) {
        Text(
            text = status,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.SemiBold,
                color = color
            )
        )
    }
}

private data class StatusDisplay(
    val title: String,
    val color: Color,
    val containerColor: Color,
    val icon: ImageVector
)

/**
 * Modern Scannable Editorial Calculation Result Card.
 * Structured by:
 * 1. STATUS UTAMA (Clear within 1 second)
 * 2. RINGKASAN HUKUM
 * 3. KEWAJIBAN IBADAH
 * 4. RINCIAN PERHITUNGAN
 * 5. CATATAN FIQIH & RUJUKAN KITAB
 */
@Composable
fun CalculationResultCard(
    result: CalculationResult,
    modifier: Modifier = Modifier
) {
    if (result.category6Calculation != null) {
        Category6ResultContent(
            result = result,
            cat6 = result.category6Calculation,
            modifier = modifier
        )
        return
    }

    var detailsExpanded by remember { mutableStateOf(false) }
    var referencesExpanded by remember { mutableStateOf(false) }
    val dFormat = remember { SimpleDateFormat("dd MMM yyyy, HH:mm", Locale("id", "ID")) }
    val dateOnlyFormat = remember { SimpleDateFormat("dd MMM yyyy", Locale("id", "ID")) }

    // 1. Tentukan Status Utama
    val statusInfo = remember(result) {
        val summaryUpper = result.statusSummary.uppercase()
        val catUpper = result.caseCategory.uppercase()
        val isTakmilah = catUpper.contains("TAKMILAT") || summaryUpper.contains("TAKMILAT")
        val hasHaid = result.haidOrNifasSegment != null && !result.haidOrNifasSegment.title.contains("Nifas", ignoreCase = true)
        val hasNifas = result.haidOrNifasSegment != null && result.haidOrNifasSegment.title.contains("Nifas", ignoreCase = true)
        val hasIstihadhah = result.istihadhahSegment != null

        when {
            isTakmilah -> StatusDisplay(
                title = "TAKMILATAN LIT-TUHRI",
                color = FiqihIstihadhahColor,
                containerColor = FiqihIstihadhahContainer,
                icon = Icons.Outlined.CheckCircle
            )
            hasNifas && hasIstihadhah -> StatusDisplay(
                title = "NIFAS & ISTIHADHAH",
                color = FiqihNifasColor,
                containerColor = FiqihNifasContainer,
                icon = Icons.Outlined.WaterDrop
            )
            hasNifas -> StatusDisplay(
                title = "NIFAS",
                color = FiqihNifasColor,
                containerColor = FiqihNifasContainer,
                icon = Icons.Outlined.WaterDrop
            )
            hasHaid && hasIstihadhah -> StatusDisplay(
                title = "HAID & ISTIHADHAH",
                color = FiqihHaidColor,
                containerColor = FiqihHaidContainer,
                icon = Icons.Outlined.Bloodtype
            )
            hasHaid -> StatusDisplay(
                title = "HAID",
                color = FiqihHaidColor,
                containerColor = FiqihHaidContainer,
                icon = Icons.Outlined.Bloodtype
            )
            hasIstihadhah -> StatusDisplay(
                title = "ISTIHADHAH",
                color = FiqihIstihadhahColor,
                containerColor = FiqihIstihadhahContainer,
                icon = Icons.Outlined.WarningAmber
            )
            summaryUpper.contains("SUCI") || catUpper.contains("SUCI") -> StatusDisplay(
                title = "SUCI",
                color = FiqihSuciColor,
                containerColor = FiqihSuciContainer,
                icon = Icons.Outlined.CheckCircle
            )
            else -> StatusDisplay(
                title = if (result.statusSummary.length > 25) "ANALISIS FIQIH" else result.statusSummary,
                color = EmeraldPrimary,
                containerColor = EmeraldPrimaryContainer.copy(alpha = 0.5f),
                icon = Icons.Outlined.Info
            )
        }
    }

    // 2. Periode / Tanggal
    val primaryDateRange = remember(result) {
        val all = result.allSegments
        if (all.isNotEmpty()) {
            val start = all.minOf { it.startEpochMillis }
            val end = all.maxOf { it.endEpochMillis }
            val totalDays = ((end - start) / (1000 * 60 * 60 * 24)).coerceAtLeast(1)
            "${FiqihCalculatorEngine.formatDisplayRange(start, end)} (${totalDays} Hari)"
        } else {
            val segHaid = result.haidOrNifasSegment
            val segIsti = result.istihadhahSegment
            val segSuci = result.suciSegment
            when {
                segHaid != null && segIsti != null -> {
                    val start = minOf(segHaid.startEpochMillis, segIsti.startEpochMillis)
                    val end = maxOf(segHaid.endEpochMillis, segIsti.endEpochMillis)
                    val totalDays = ((end - start) / (1000 * 60 * 60 * 24)).coerceAtLeast(1)
                    "${FiqihCalculatorEngine.formatDisplayRange(start, end)} (${totalDays} Hari)"
                }
                segHaid != null -> {
                    "${FiqihCalculatorEngine.formatDisplayRange(segHaid.startEpochMillis, segHaid.endEpochMillis)} (${segHaid.durationDays} Hari ${segHaid.remainingHours} Jam)"
                }
                segIsti != null -> {
                    "${FiqihCalculatorEngine.formatDisplayRange(segIsti.startEpochMillis, segIsti.endEpochMillis)} (${segIsti.durationDays} Hari)"
                }
                segSuci != null -> {
                    FiqihCalculatorEngine.formatDisplayRange(segSuci.startEpochMillis, segSuci.endEpochMillis)
                }
                else -> "Periode analisis siklus"
            }
        }
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .testTag("calculation_result_card"),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(
            width = 0.75.dp,
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header: Mazhab Mu'tamad Tag
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "HASIL KEPUTUSAN FIQIH",
                    style = MaterialTheme.typography.labelSmall.copy(
                        letterSpacing = 1.2.sp,
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Mazhab Syafi'i",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // 1. STATUS UTAMA (Instant scannability)
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                color = statusInfo.containerColor
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(statusInfo.color.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = statusInfo.icon,
                            contentDescription = statusInfo.title,
                            tint = statusInfo.color,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = statusInfo.title,
                            style = MaterialTheme.typography.headlineSmall.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.2.sp
                            ),
                            color = statusInfo.color
                        )
                        Text(
                            text = result.statusSummary,
                            style = MaterialTheme.typography.bodySmall.copy(
                                lineHeight = 18.sp,
                                fontWeight = FontWeight.Normal
                            ),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            // REKOMENDASI KHUSUS ISTIHADHAH MUTAHAYYIRAH
            // Kartu rekomendasi ahli fiqih HANYA ditampilkan jika SEDANG menghitung kasus Istihadhah Mutahayyirah
            val isMutahayyirahCase = remember(result) {
                val cat = result.caseCategory.lowercase()
                val sum = result.statusSummary.lowercase()

                // Pastikan bukan golongan non-mutahayyirah (1, 2, 3, 4 ingat adat lengkap, haid normal, nifas normal)
                val isExplicitNonMutahayyirah = cat.startsWith("1.") ||
                    cat.startsWith("2.") ||
                    cat.startsWith("3.") ||
                    cat.startsWith("4.") ||
                    cat.contains("ingat adat lengkap") ||
                    cat.contains("dzakirah (ingat adat lengkap)") ||
                    cat.contains("dzakirah qadran wa waqtan") ||
                    cat.contains("mumayyizah") ||
                    cat.contains("mubtadi'ah") ||
                    cat.contains("haid sah") ||
                    cat.contains("nifas normal") ||
                    cat.contains("takmilatan")

                if (isExplicitNonMutahayyirah) {
                    false
                } else {
                    // Kasus Mutahayyirah murni dalam Fiqih Syafi'i:
                    // - Mutahayyirah Mahdhah (Golongan 5: Lupa Total Kadar & Waktu)
                    // - Mutahayyirah Dzakirah lil-Qadri dunan Waqt / la Waqtan (Golongan 6: Ingat Kadar Saja, Lupa Waktu)
                    // - Mutahayyirah Dzakirah lil-Waqti dunan Qadr / la Qadran (Golongan 7: Ingat Waktu Saja, Lupa Kadar)
                    // - Nasiyah fin-Nifas / Mutahayyirah Nifas
                    val hasMutahayyirahKeyword = cat.contains("mutahayyir") ||
                        cat.contains("nasiyah") ||
                        cat.contains("la waqtan") ||
                        cat.contains("dunan waqt") ||
                        cat.contains("dunan qadr") ||
                        cat.contains("la qadran") ||
                        cat.contains("dzakirah lil-waqti") ||
                        cat.contains("dzakirah lil-qadri") ||
                        cat.contains("dzakirah qadran la waqtan")

                    val hasIhtiyathPeriod = result.periodResults.any { it.status == FiqihStatus.IHTIYATH }

                    hasMutahayyirahKeyword || (hasIhtiyathPeriod && sum.contains("ihtiyath"))
                }
            }

            if (isMutahayyirahCase) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.18f),
                    border = androidx.compose.foundation.BorderStroke(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.error.copy(alpha = 0.3f)
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.Top,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.WarningAmber,
                            contentDescription = "Peringatan Konsultasi Fiqih",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier
                                .size(18.dp)
                                .padding(top = 1.dp)
                        )
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                text = "Rekomendasi Ahli Fiqih:",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.error
                            )
                            Text(
                                text = "Kasus Mutahayyirah berlandaskan kaidah kehati-hatian (ihtiyath). Karena hitungan otomatis dapat keliru, sangat dianjurkan berkonsultasi langsung ke ustadz/ustadzah ahli fiqih wanita.",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontSize = 11.5.sp,
                                    lineHeight = 15.sp,
                                    fontWeight = FontWeight.Normal
                                ),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }

            // 2. PERIODE DARAH (Clean text row, no heavy card)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.CalendarToday,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
                Column {
                    Text(
                        text = "Periode Darah",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = primaryDateRange,
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f),
                thickness = 0.5.dp
            )

            // 3. KEWAJIBAN IBADAH
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Konsekuensi Ibadah",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface
                )

                ActionCleanRow(
                    icon = Icons.Outlined.Mosque,
                    label = "Shalat",
                    content = result.shalatConsequence
                )

                ActionCleanRow(
                    icon = Icons.Outlined.NightsStay,
                    label = "Puasa",
                    content = result.puasaConsequence
                )

                ActionCleanRow(
                    icon = Icons.Outlined.WaterDrop,
                    label = "Mandi Bersuci",
                    content = result.mandiWajibNote
                )
            }

            // TOMBOL: "Lihat Rincian Fiqih"
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                TextButton(
                    onClick = { detailsExpanded = !detailsExpanded },
                    modifier = Modifier.testTag("toggle_details_button")
                ) {
                    Text(
                        text = if (detailsExpanded) "Sembunyikan Rincian" else "Lihat Rincian Fiqih & Kronologi",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = if (detailsExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            // 4. INFORMASI LANJUTAN & KRONOLOGI
            AnimatedVisibility(
                visible = detailsExpanded,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f),
                        thickness = 0.5.dp
                    )

                    // Kategori Mustahadhah / Fiqih
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = "KATEGORI FIQIH TERDETEKSI",
                            style = MaterialTheme.typography.labelSmall.copy(
                                letterSpacing = 1.sp,
                                fontWeight = FontWeight.Bold
                            ),
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = result.caseCategory,
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (result.categoryDetectionReason.isNotBlank()) {
                            Text(
                                text = result.categoryDetectionReason,
                                style = MaterialTheme.typography.bodySmall.copy(lineHeight = 18.sp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Kronologi Fase Perubahan Darah
                    if (result.phaseBreakdowns.isNotEmpty()) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = "KRONOLOGI PERUBAHAN WARNA DARAH",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    letterSpacing = 1.sp,
                                    fontWeight = FontWeight.Bold
                                ),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            result.phaseBreakdowns.forEach { phaseStr ->
                                Text(
                                    text = "• $phaseStr",
                                    style = MaterialTheme.typography.bodySmall.copy(lineHeight = 19.sp),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }

                    // Segmen Fiqih Kronologis Lengkap (Haid, Suci/Naqa', Takmilatan lit-Tuhri, Haid Baru)
                    if (result.allSegments.isNotEmpty()) {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text(
                                text = "RINCIAN FASE HUKUM KRONOLOGIS",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    letterSpacing = 1.sp,
                                    fontWeight = FontWeight.Bold
                                ),
                                color = MaterialTheme.colorScheme.primary
                            )
                            result.allSegments.forEach { seg ->
                                val segColor = when {
                                    seg.title.contains("Takmilat") -> FiqihIstihadhahColor
                                    seg.status.contains("Haid") -> FiqihHaidColor
                                    seg.status.contains("Suci") -> FiqihSuciColor
                                    seg.status.contains("Istihadhah") -> FiqihIstihadhahColor
                                    else -> EmeraldPrimary
                                }
                                val rangeStr = FiqihCalculatorEngine.formatDisplayRange(seg.startEpochMillis, seg.endEpochMillis)
                                val durationDays = ((seg.endEpochMillis - seg.startEpochMillis) / (24 * 3600_000L)).coerceAtLeast(0)
                                val durationHours = (((seg.endEpochMillis - seg.startEpochMillis) % (24 * 3600_000L)) / 3600_000L)
                                val durStr = if (durationHours > 0) "$durationDays Hari $durationHours Jam" else "$durationDays Hari"
                                PeriodSegmentCleanItem(
                                    title = "${seg.title} (${seg.status})",
                                    range = rangeStr,
                                    duration = durStr,
                                    description = seg.description,
                                    color = segColor
                                )
                            }
                        }
                    } else {
                        if (result.haidOrNifasSegment != null) {
                            val seg = result.haidOrNifasSegment
                            PeriodSegmentCleanItem(
                                title = seg.title,
                                range = FiqihCalculatorEngine.formatDisplayRange(seg.startEpochMillis, seg.endEpochMillis),
                                duration = "${seg.durationDays} Hari ${seg.remainingHours} Jam",
                                description = seg.description,
                                color = FiqihHaidColor
                            )
                        }

                        if (result.istihadhahSegment != null) {
                            val seg = result.istihadhahSegment
                            PeriodSegmentCleanItem(
                                title = seg.title,
                                range = FiqihCalculatorEngine.formatDisplayRange(seg.startEpochMillis, seg.endEpochMillis),
                                duration = "${seg.durationDays} Hari ${seg.remainingHours} Jam",
                                description = seg.description,
                                color = FiqihIstihadhahColor
                            )
                        }

                        if (result.suciSegment != null) {
                            val seg = result.suciSegment
                            PeriodSegmentCleanItem(
                                title = seg.title,
                                range = FiqihCalculatorEngine.formatDisplayRange(seg.startEpochMillis, seg.endEpochMillis),
                                duration = "${seg.durationDays} Hari ${seg.remainingHours} Jam",
                                description = seg.description,
                                color = FiqihSuciColor
                            )
                        }
                    }

                    // Shalat yang Perlu Diqadha
                    if (result.qadhaPrayers.isNotEmpty()) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = "SHALAT YANG PERLU DIPERHATIKAN (${result.qadhaPrayers.size} waktu):",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    letterSpacing = 1.sp,
                                    fontWeight = FontWeight.Bold
                                ),
                                color = MaterialTheme.colorScheme.primary
                            )
                            result.qadhaPrayers.forEach { p ->
                                Text(
                                    text = "• $p",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }

                    // Panduan Bersuci Mustahadhah
                    if (result.mustahadhahCareGuide != null) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = "Panduan Bersuci & Ibadah Mustahadhah",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = result.mustahadhahCareGuide,
                                    style = MaterialTheme.typography.bodySmall.copy(lineHeight = 18.sp),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }

                    // Catatan & Saran
                    if (result.medicalAndSpiritualAdvice.isNotBlank()) {
                        Text(
                            text = result.medicalAndSpiritualAdvice,
                            style = MaterialTheme.typography.bodySmall.copy(lineHeight = 18.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // 5. REFERENSI KITAB RUJUKAN
                    if (result.references.isNotEmpty()) {
                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f),
                            thickness = 0.5.dp
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { referencesExpanded = !referencesExpanded }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Rujukan Kitab (${result.references.size})",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primary
                            )
                            Icon(
                                imageVector = if (referencesExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        if (referencesExpanded) {
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                result.references.forEachIndexed { i, ref ->
                                    Text(
                                        text = "${i + 1}. ${ref.kitabName} (${ref.volumeAndPage}): ${ref.explanation}",
                                        style = MaterialTheme.typography.bodySmall.copy(lineHeight = 18.sp),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }

                    Text(
                        text = "Kaidah perhitungan merujuk kitab fiqih mu'tamad: Uyunul Masa'il Linnisa' & Tuhfatun Niswah.",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }
        }
    }
}

@Composable
private fun ActionCleanRow(
    icon: ImageVector,
    label: String,
    content: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .size(18.dp)
                .padding(top = 1.dp)
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = content,
                style = MaterialTheme.typography.bodySmall.copy(lineHeight = 18.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun PeriodSegmentCleanItem(
    title: String,
    range: String,
    duration: String,
    description: String,
    color: Color
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = color.copy(alpha = 0.08f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = color
                )
                Text(
                    text = duration,
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = color
                )
            }
            Text(
                text = range,
                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall.copy(lineHeight = 17.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun PeriodSegmentItem(
    title: String,
    range: String,
    duration: String,
    description: String,
    icon: ImageVector,
    iconColor: Color
) {
    PeriodSegmentCleanItem(
        title = title,
        range = range,
        duration = duration,
        description = description,
        color = iconColor
    )
}

@Composable
fun InfoRowItem(
    label: String,
    value: String,
    icon: ImageVector
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .padding(top = 2.dp)
                .size(18.dp)
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
fun Category6CandidateBreakdownCard(
    cat6: Category6CalculationResult,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
        ),
        border = androidx.compose.foundation.BorderStroke(
            width = 0.75.dp,
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    Icons.Outlined.Analytics,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = "ANALISIS CANDIDATE INTERVALS (GOLONGAN 6)",
                    style = MaterialTheme.typography.labelMedium.copy(
                        letterSpacing = 1.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Text(
                text = "Durasi adat ${cat6.habitDurationDays} hari dalam rentang hari ke-${cat6.positionWindowStart} s/d ke-${cat6.positionWindowEnd}. Total ${cat6.generatedCandidatesCount} interval kemungkinan posisi awal dibentuk.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Eliminasi & Kandidat Valid
            if (cat6.eliminatedPureCandidates.isNotEmpty()) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Kandidat tereliminasi (karena hari yakin suci ${cat6.certainPureDays.sorted()}): ${cat6.eliminatedPureCandidates.joinToString(", ") { "${it.first}..${it.last}" }}",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }

            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Kandidat valid yang dianalisis (${cat6.validCandidates.size} posisi): ${cat6.validCandidates.joinToString(", ") { "${it.first}..${it.last}" }}",
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                )
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f), thickness = 0.5.dp)

            Text(
                text = "Peta Keputusan Fiqih & Tindakan Ibadah:",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )

            cat6.periodResults.forEach { p ->
                val color = when (p.status) {
                    FiqhPeriodStatus.HAID_YAKIN -> FiqihHaidColor
                    FiqhPeriodStatus.SUCI_YAKIN -> FiqihSuciColor
                    FiqhPeriodStatus.SYAK_HAID_Suci,
                    FiqhPeriodStatus.SYAK_HAID_Suci_PUTUS -> GoldTertiary
                }
                val icon = when (p.status) {
                    FiqhPeriodStatus.HAID_YAKIN -> Icons.Outlined.Bloodtype
                    FiqhPeriodStatus.SUCI_YAKIN -> Icons.Outlined.CheckCircle
                    FiqhPeriodStatus.SYAK_HAID_Suci -> Icons.Outlined.WarningAmber
                    FiqhPeriodStatus.SYAK_HAID_Suci_PUTUS -> Icons.Outlined.Shower
                }

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = color.copy(alpha = 0.08f),
                    border = androidx.compose.foundation.BorderStroke(0.5.dp, color.copy(alpha = 0.3f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(16.dp))
                                val dayLabel = if (p.startDay == p.endDay) "Hari ke-${p.startDay}" else "Hari ke-${p.startDay}–${p.endDay}"
                                Text(
                                    text = dayLabel,
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = color.copy(alpha = 0.2f)
                            ) {
                                Text(
                                    text = p.status.label,
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = color,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }

                        Text(
                            text = p.status.arabicText,
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                            color = color
                        )

                        Text(
                            text = p.explanation,
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp, lineHeight = 16.sp),
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        val actionBadgeText = when (p.ihtiyatAction) {
                            IhtiyatAction.NONE -> if (p.status == FiqhPeriodStatus.HAID_YAKIN) "Hukum Haid: Haram shalat & puasa" else "Hukum Suci: Shalat & puasa wajib"
                            IhtiyatAction.WUDHU_SETIAP_FARDHU -> "Ihtiyath: Wudhu setiap waktu fardhu"
                            IhtiyatAction.GHUSL_SETIAP_FARDHU -> "Ihtiyath: Wajib Mandi (Ghusl) setiap fardhu + wudhu"
                        }
                        Text(
                            text = "Tindakan Ibadah: $actionBadgeText",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = color
                        )
                    }
                }
            }
        }
    }
}

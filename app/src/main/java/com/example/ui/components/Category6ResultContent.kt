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
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
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
import com.example.model.*
import com.example.ui.theme.*

/**
 * Penyajian UI Hasil Perhitungan Kategori 6 yang ringkas, modern, elegan,
 * dan mudah dipahami oleh pengguna awam sesuai Fiqih Mazhab Syafi'i.
 *
 * Mengutamakan hierarki visual:
 * 1. Kesimpulan & Parameter Utama
 * 2. Peta Visual & Tabel Rentang Hari
 * 3. Panduan Ibadah Ringkas (tanpa pengulangan berlebih)
 * 4. Detail Matematis, Irisan/Gabungan & Rujukan Kitab (Expandable / Default Tertutup)
 */
@Composable
fun Category6ResultContent(
    result: CalculationResult,
    cat6: Category6CalculationResult,
    modifier: Modifier = Modifier
) {
    var detailsExpanded by remember { mutableStateOf(false) }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .testTag("category6_result_card"),
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
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // =========================================================================
            // 1. HEADER & IDENTITAS KATEGORI
            // =========================================================================
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "HASIL KEPUTUSAN FIQIH",
                    style = MaterialTheme.typography.labelSmall.copy(
                        letterSpacing = 1.1.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    color = MaterialTheme.colorScheme.primary
                )
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                ) {
                    Text(
                        text = "Mazhab Syafi'i",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Medium,
                            fontSize = 11.sp
                        ),
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
            }

            // Kartu Ringkas Nama Kategori
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                border = androidx.compose.foundation.BorderStroke(
                    0.5.dp,
                    MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.EventNote,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Kategori 6: Mu'tadah Ghairu Mumayyizah",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Ingat Kadar Adat, Lupa Waktu Mulai (الذاكرة للقدر لا للوقت)",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontSize = 11.5.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }
                }
            }

            // Chips Parameter Utama
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ParameterChip(
                    label = "Adat",
                    value = "${cat6.habitDurationDays} Hari",
                    modifier = Modifier.weight(1f)
                )
                ParameterChip(
                    label = "Rentang",
                    value = "Hari ${cat6.positionWindowStart}–${cat6.positionWindowEnd}",
                    modifier = Modifier.weight(1.3f)
                )
                if (cat6.certainPureDays.isNotEmpty()) {
                    ParameterChip(
                        label = "Yakin Suci",
                        value = "Hari ${cat6.certainPureDays.sorted().joinToString(",")}",
                        modifier = Modifier.weight(1.1f),
                        accentColor = FiqihSuciColor
                    )
                }
            }

            if (!cat6.isValid) {
                // Tampilan jika terjadi kontradiksi input
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.35f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "Perhatian Input",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.error
                        )
                        Text(
                            text = cat6.errorMessage ?: "Terjadi ketidaksesuaian input.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
                return@Column
            }

            // =========================================================================
            // 2. PETA HASIL PERHITUNGAN (Visual Bar & Tabel Ringkas)
            // =========================================================================
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "PETA HASIL PERHITUNGAN",
                        style = MaterialTheme.typography.labelSmall.copy(
                            letterSpacing = 1.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Bulan ${cat6.monthLength} Hari",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Mini Visual Timeline Bar
                CompactPeriodBar(
                    periodResults = cat6.periodResults,
                    totalDays = cat6.monthLength
                )

                // Tabel Ringkas Rentang Hari & Status
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = androidx.compose.foundation.BorderStroke(
                        0.75.dp,
                        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        cat6.periodResults.forEachIndexed { index, period ->
                            val statusStyle = getCat6StatusPresentation(period.status)
                            val dayLabel = if (period.startDay == period.endDay) {
                                "Hari ${period.startDay}"
                            } else {
                                "Hari ${period.startDay}–${period.endDay}"
                            }

                            val briefAction = when (period.ihtiyatAction) {
                                IhtiyatAction.NONE -> {
                                    if (period.status == FiqhPeriodStatus.HAID_YAKIN) "Haram shalat & puasa" else "Wajib shalat & puasa"
                                }
                                IhtiyatAction.WUDHU_SETIAP_FARDHU -> "Ihtiyath: Wudhu tiap fardhu"
                                IhtiyatAction.GHUSL_SETIAP_FARDHU -> "Ihtiyath: Mandi & wudhu"
                            }

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(if (index % 2 == 1) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f) else Color.Transparent)
                                    .padding(horizontal = 12.dp, vertical = 9.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                // Kolom 1: Rentang Hari
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.weight(1.1f)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(statusStyle.color)
                                    )
                                    Text(
                                        text = dayLabel,
                                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }

                                // Kolom 2: Status & Tindakan Ringkas
                                Column(
                                    horizontalAlignment = Alignment.End,
                                    modifier = Modifier.weight(1.6f)
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = statusStyle.containerColor
                                    ) {
                                        Text(
                                            text = statusStyle.shortLabel,
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 11.sp
                                            ),
                                            color = statusStyle.color,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                    Text(
                                        text = briefAction,
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontSize = 10.5.sp,
                                            fontWeight = FontWeight.Normal
                                        ),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            if (index < cat6.periodResults.size - 1) {
                                HorizontalDivider(
                                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f),
                                    thickness = 0.5.dp
                                )
                            }
                        }
                    }
                }
            }

            // =========================================================================
            // 3. PANDUAN IBADAH (Ringkas, Tanpa Pengulangan Redundan)
            // =========================================================================
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "PANDUAN IBADAH",
                    style = MaterialTheme.typography.labelSmall.copy(
                        letterSpacing = 1.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    color = MaterialTheme.colorScheme.primary
                )

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                    border = androidx.compose.foundation.BorderStroke(
                        0.5.dp,
                        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val presentStatuses = cat6.periodResults.map { it.status }.toSet()

                        if (FiqhPeriodStatus.HAID_YAKIN in presentStatuses) {
                            val days = cat6.periodResults.filter { it.status == FiqhPeriodStatus.HAID_YAKIN }
                                .joinToString(", ") { if (it.startDay == it.endDay) "Hari ${it.startDay}" else "Hari ${it.startDay}–${it.endDay}" }
                            GuidanceItem(
                                icon = Icons.Outlined.Bloodtype,
                                iconColor = FiqihHaidColor,
                                title = "Haid Yakin ($days)",
                                rule = "Tinggalkan shalat & puasa. Diharamkan jima' sampai suci dan mandi wajib."
                            )
                        }

                        if (FiqhPeriodStatus.SUCI_YAKIN in presentStatuses) {
                            val days = cat6.periodResults.filter { it.status == FiqhPeriodStatus.SUCI_YAKIN }
                                .joinToString(", ") { if (it.startDay == it.endDay) "Hari ${it.startDay}" else "Hari ${it.startDay}–${it.endDay}" }
                            GuidanceItem(
                                icon = Icons.Outlined.CheckCircle,
                                iconColor = FiqihSuciColor,
                                title = "Suci Yakin ($days)",
                                rule = "Wajib mengerjakan shalat dan puasa seperti biasa."
                            )
                        }

                        if (FiqhPeriodStatus.SYAK_HAID_Suci in presentStatuses) {
                            val days = cat6.periodResults.filter { it.status == FiqhPeriodStatus.SYAK_HAID_Suci }
                                .joinToString(", ") { if (it.startDay == it.endDay) "Hari ${it.startDay}" else "Hari ${it.startDay}–${it.endDay}" }
                            GuidanceItem(
                                icon = Icons.Outlined.WarningAmber,
                                iconColor = GoldTertiary,
                                title = "Syak Haid & Suci ($days)",
                                rule = "Kaidah Ihtiyath: Wajib berwudhu setiap masuk waktu fardhu (tanpa wajib mandi). Tetap shalat & puasa."
                            )
                        }

                        if (FiqhPeriodStatus.SYAK_HAID_Suci_PUTUS in presentStatuses) {
                            val days = cat6.periodResults.filter { it.status == FiqhPeriodStatus.SYAK_HAID_Suci_PUTUS }
                                .joinToString(", ") { if (it.startDay == it.endDay) "Hari ${it.startDay}" else "Hari ${it.startDay}–${it.endDay}" }
                            GuidanceItem(
                                icon = Icons.Outlined.Shower,
                                iconColor = Color(0xFF7E38B7),
                                title = "Syak Haid, Suci & Kemungkinan Berhenti ($days)",
                                rule = "Kaidah Ihtiyath: Wajib mandi besar (ghusl) setiap masuk waktu fardhu lalu berwudhu, karena ada potensi darah baru terputus. Tetap shalat & puasa."
                            )
                        }
                    }
                }
            }

            // =========================================================================
            // 4. BAGIAN DETAIL (Expandable, Default Tertutup)
            // =========================================================================
            OutlinedCard(
                onClick = { detailsExpanded = !detailsExpanded },
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.outlinedCardColors(
                    containerColor = if (detailsExpanded) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f) else Color.Transparent
                ),
                border = androidx.compose.foundation.BorderStroke(
                    0.75.dp,
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("toggle_category6_details_button")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Analytics,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = if (detailsExpanded) "Sembunyikan Rincian Analisis" else "Lihat Rincian Analisis & Fiqih",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Icon(
                        imageVector = if (detailsExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            AnimatedVisibility(
                visible = detailsExpanded,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // A. Analisis Candidate Intervals
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                        border = androidx.compose.foundation.BorderStroke(
                            0.5.dp,
                            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "ANALISIS CANDIDATE INTERVALS",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.8.sp
                                ),
                                color = MaterialTheme.colorScheme.primary
                            )

                            Text(
                                text = "Berdasarkan durasi adat ${cat6.habitDurationDays} hari yang diketahui berada dalam rentang hari ke-${cat6.positionWindowStart} s/d ${cat6.positionWindowEnd}:",
                                style = MaterialTheme.typography.bodySmall.copy(lineHeight = 17.sp),
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            // Status kandidat
                            Text(
                                text = "• Total kemungkinan awal terbentuk: ${cat6.generatedCandidatesCount} interval",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            if (cat6.eliminatedPureCandidates.isNotEmpty()) {
                                Text(
                                    text = "• Dieliminasi karena hari yakin suci (${cat6.certainPureDays.sorted()}): ${cat6.eliminatedPureCandidates.joinToString(", ") { "${it.first}..${it.last}" }}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }

                            if (cat6.eliminatedHaidCandidates.isNotEmpty()) {
                                Text(
                                    text = "• Dieliminasi karena hari yakin haid (${cat6.certainHaidDays.sorted()}): ${cat6.eliminatedHaidCandidates.joinToString(", ") { "${it.first}..${it.last}" }}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }

                            Text(
                                text = "• Kandidat valid yang diuji (${cat6.validCandidates.size}): ${cat6.validCandidates.joinToString(", ") { "${it.first}..${it.last}" }}",
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.primary
                            )

                            HorizontalDivider(
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                                thickness = 0.5.dp
                            )

                            Text(
                                text = "Kaidah Irisan & Gabungan:\n" +
                                        "1. Hari yang masuk dalam seluruh kandidat valid (irisan) = Haid Yakin.\n" +
                                        "2. Hari di luar seluruh kandidat (di luar gabungan) = Suci Yakin.\n" +
                                        "3. Hari yang masuk sebagian kandidat sebelum ada kandidat yang selesai = Syak Haid & Suci (tanpa inqitha').\n" +
                                        "4. Hari yang masuk sebagian kandidat setelah ada kandidat yang selesai = Syak Haid, Suci & Putus (dengan kemungkinan inqitha').",
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp, lineHeight = 16.sp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // B. Penjelasan Fiqih Setiap Rentang Hari
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                        border = androidx.compose.foundation.BorderStroke(
                            0.5.dp,
                            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "PENJELASAN FIQIH SETIAP RENTANG",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.8.sp
                                ),
                                color = MaterialTheme.colorScheme.primary
                            )

                            cat6.periodResults.forEach { p ->
                                val statusStyle = getCat6StatusPresentation(p.status)
                                val dayLabel = if (p.startDay == p.endDay) "Hari ${p.startDay}" else "Hari ${p.startDay}–${p.endDay}"
                                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(
                                            text = "$dayLabel (${p.status.label} / ${p.status.arabicText}):",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontWeight = FontWeight.Bold,
                                                color = statusStyle.color
                                            )
                                        )
                                    }
                                    Text(
                                        text = p.explanation,
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            fontSize = 11.5.sp,
                                            lineHeight = 16.sp
                                        ),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }

                    // C. Kronologi Perubahan Warna Darah (jika ada input darah)
                    if (result.phaseBreakdowns.isNotEmpty()) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                            border = androidx.compose.foundation.BorderStroke(
                                0.5.dp,
                                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = "KRONOLOGI PERUBAHAN WARNA DARAH",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 0.8.sp
                                    ),
                                    color = MaterialTheme.colorScheme.primary
                                )
                                result.phaseBreakdowns.forEach { phaseStr ->
                                    Text(
                                        text = "• $phaseStr",
                                        style = MaterialTheme.typography.bodySmall.copy(lineHeight = 17.sp),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }

                    // D. Rujukan Kitab & Nomor Halaman
                    if (result.references.isNotEmpty()) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                            border = androidx.compose.foundation.BorderStroke(
                                0.5.dp,
                                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = "RUJUKAN KITAB FIQIH SYAFI'I",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 0.8.sp
                                    ),
                                    color = MaterialTheme.colorScheme.primary
                                )
                                result.references.forEachIndexed { i, ref ->
                                    Text(
                                        text = "${i + 1}. ${ref.kitabName} (${ref.volumeAndPage}): ${ref.explanation}",
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            fontSize = 11.5.sp,
                                            lineHeight = 16.sp
                                        ),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
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

@Composable
private fun ParameterChip(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    accentColor: Color = MaterialTheme.colorScheme.primary
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = accentColor.copy(alpha = 0.08f),
        border = androidx.compose.foundation.BorderStroke(0.5.dp, accentColor.copy(alpha = 0.25f)),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.5.sp
                ),
                color = accentColor
            )
        }
    }
}

@Composable
private fun CompactPeriodBar(
    periodResults: List<Category6PeriodResult>,
    totalDays: Int,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        modifier = modifier
            .fillMaxWidth()
            .height(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
    ) {
        Row(modifier = Modifier.fillMaxSize()) {
            val safeTotal = totalDays.coerceAtLeast(1)
            periodResults.forEach { p ->
                val duration = (p.endDay - p.startDay + 1).coerceAtLeast(1)
                val weight = duration.toFloat() / safeTotal.toFloat()
                val color = getCat6StatusPresentation(p.status).color

                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .weight(weight.coerceAtLeast(0.01f))
                        .background(color)
                )
            }
        }
    }
}

@Composable
private fun GuidanceItem(
    icon: ImageVector,
    iconColor: Color,
    title: String,
    rule: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier
                .padding(top = 2.dp)
                .size(24.dp)
                .clip(CircleShape)
                .background(iconColor.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconColor,
                modifier = Modifier.size(14.dp)
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = iconColor
                )
            )
            Text(
                text = rule,
                style = MaterialTheme.typography.bodySmall.copy(
                    lineHeight = 16.5.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
            )
        }
    }
}

private data class Cat6StatusPresentation(
    val shortLabel: String,
    val color: Color,
    val containerColor: Color
)

private fun getCat6StatusPresentation(status: FiqhPeriodStatus): Cat6StatusPresentation {
    return when (status) {
        FiqhPeriodStatus.HAID_YAKIN -> Cat6StatusPresentation(
            shortLabel = "Haid Yakin",
            color = FiqihHaidColor,
            containerColor = FiqihHaidContainer
        )
        FiqhPeriodStatus.SUCI_YAKIN -> Cat6StatusPresentation(
            shortLabel = "Suci Yakin",
            color = FiqihSuciColor,
            containerColor = FiqihSuciContainer
        )
        FiqhPeriodStatus.SYAK_HAID_Suci -> Cat6StatusPresentation(
            shortLabel = "Syak Haid / Suci",
            color = Color(0xFFC07D1E),
            containerColor = Color(0xFFFFF7ED)
        )
        FiqhPeriodStatus.SYAK_HAID_Suci_PUTUS -> Cat6StatusPresentation(
            shortLabel = "Syak Haid, Suci & Putus",
            color = Color(0xFF7E38B7),
            containerColor = Color(0xFFF7F0FC)
        )
    }
}

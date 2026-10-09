package com.example.ui.components

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.BleedingInterval
import com.example.model.BloodColor
import com.example.ui.theme.EmeraldDeep
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.GoldTertiary
import java.text.SimpleDateFormat
import java.util.*

/**
 * Screen 4: Input Fase Darah
 * Modern vertical timeline view matching the reference design:
 * - Segmented control: [ Timeline ] | [ Daftar ]
 * - Vertical connecting timeline line with colored circular nodes
 * - Phase cards: "Fase X - Status", date range, duration & blood color, chevron
 * - Expandable detailed phase editor (dates, times, color swatches, Takhin/Muntin, delete)
 * - Automatic detection banner for dry breaks (Fatrah Naqa' / Takmilatan lit-Tuhri)
 * - "+ Tambah Fase Darah" capsule button
 */
@Composable
fun BloodPhaseTimelineView(
    intervals: List<BleedingInterval>,
    onAddPhase: () -> Unit,
    onUpdatePhase: (Int, BleedingInterval) -> Unit,
    onRemovePhase: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    var viewMode by remember { mutableStateOf("Timeline") } // "Timeline" or "Daftar"
    var expandedIndex by remember { mutableIntStateOf(-1) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag("blood_phase_timeline_view"),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Segmented Control Switcher: [ Timeline ] | [ Daftar ]
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = Color(0xFFECE7DE),
                border = BorderStroke(0.5.dp, Color(0xFFDCD4C6)),
                modifier = Modifier.padding(vertical = 4.dp)
            ) {
                Row(
                    modifier = Modifier.padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Timeline button
                    Surface(
                        onClick = { viewMode = "Timeline" },
                        shape = RoundedCornerShape(20.dp),
                        color = if (viewMode == "Timeline") EmeraldDeep else Color.Transparent,
                        modifier = Modifier.width(130.dp)
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.padding(vertical = 8.dp)
                        ) {
                            Text(
                                text = "Timeline",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = if (viewMode == "Timeline") FontWeight.Bold else FontWeight.Medium,
                                    color = if (viewMode == "Timeline") Color.White else Color(0xFF5A605B)
                                )
                            )
                        }
                    }

                    // Daftar button
                    Surface(
                        onClick = { viewMode = "Daftar" },
                        shape = RoundedCornerShape(20.dp),
                        color = if (viewMode == "Daftar") EmeraldDeep else Color.Transparent,
                        modifier = Modifier.width(130.dp)
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.padding(vertical = 8.dp)
                        ) {
                            Text(
                                text = "Daftar",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = if (viewMode == "Daftar") FontWeight.Bold else FontWeight.Medium,
                                    color = if (viewMode == "Daftar") Color.White else Color(0xFF5A605B)
                                )
                            )
                        }
                    }
                }
            }
        }

        // 2. Timeline Bar Ribbon (Top Summary)
        if (intervals.isNotEmpty()) {
            BloodTimelineVisualizer(intervals = intervals)
        }

        // 3. Vertical Timeline Phase Items
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            val dFormat = remember { SimpleDateFormat("dd MMM yyyy", Locale("id", "ID")) }

            intervals.forEachIndexed { index, interval ->
                val prevEnd = if (index > 0) intervals[index - 1].endEpochMillis else null
                val nextStart = if (index < intervals.size - 1) intervals[index + 1].startEpochMillis else null

                // Jeda suci banner between phases if there's a dry gap
                val gapDays = if (prevEnd != null && interval.startEpochMillis > prevEnd) {
                    val diff = interval.startEpochMillis - prevEnd
                    diff / (24 * 3600_000L)
                } else null

                if (gapDays != null && gapDays > 0) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 16.dp, end = 4.dp, top = 6.dp, bottom = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Vertical track connector for gap
                        Box(
                            modifier = Modifier
                                .width(2.dp)
                                .height(36.dp)
                                .background(Color(0xFF266E52).copy(alpha = 0.4f))
                        )

                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFFE8F4EE),
                            border = BorderStroke(0.75.dp, Color(0xFF266E52).copy(alpha = 0.3f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Eco,
                                    contentDescription = null,
                                    tint = Color(0xFF266E52),
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "Jeda suci $gapDays hari (Fatrah Naqa' / Takmilatan lit-Tuhri terdeteksi otomatis)",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = Color(0xFF143324)
                                    )
                                )
                            }
                        }
                    }
                }

                // Main Timeline Node + Card Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Top
                ) {
                    // Left Timeline Track & Dot (only in Timeline mode)
                    if (viewMode == "Timeline") {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .width(36.dp)
                                .padding(top = 18.dp)
                        ) {
                            // Circular Node Dot
                            val dotColor = Color(interval.bloodColor.hexColor)
                            Box(
                                modifier = Modifier
                                    .size(18.dp)
                                    .clip(CircleShape)
                                    .background(dotColor.copy(alpha = 0.25f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(dotColor)
                                )
                            }

                            // Vertical track connecting line
                            if (index < intervals.size - 1) {
                                Box(
                                    modifier = Modifier
                                        .width(2.dp)
                                        .height(if (expandedIndex == index) 460.dp else 84.dp)
                                        .background(Color(0xFFD8D0C5))
                                )
                            }
                        }
                    }

                    // Right Phase Card
                    val isExpanded = expandedIndex == index
                    val startDateStr = dFormat.format(Date(interval.startEpochMillis))
                    val endDateStr = dFormat.format(Date(interval.endEpochMillis))

                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .padding(bottom = 12.dp)
                            .clickable { expandedIndex = if (isExpanded) -1 else index },
                        shape = RoundedCornerShape(16.dp),
                        color = Color.White,
                        border = BorderStroke(
                            1.dp,
                            if (isExpanded) EmeraldPrimary.copy(alpha = 0.8f) else Color(0xFFEBE5DC)
                        ),
                        shadowElevation = if (isExpanded) 3.dp else 1.dp
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // Header: Blood drop icon + Phase name & Date range + Chevron
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    // Circular Blood Drop Icon Container
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(CircleShape)
                                            .background(Color(interval.bloodColor.hexColor).copy(alpha = 0.15f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.WaterDrop,
                                            contentDescription = null,
                                            tint = Color(interval.bloodColor.hexColor),
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }

                                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                        Text(
                                            text = "Fase ${index + 1} - ${interval.bloodColor.shortName}",
                                            style = MaterialTheme.typography.titleSmall.copy(
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 14.sp
                                            ),
                                            color = Color(0xFF1E211F)
                                        )
                                        Text(
                                            text = "$startDateStr – $endDateStr",
                                            style = MaterialTheme.typography.bodySmall.copy(
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Medium
                                            ),
                                            color = Color(0xFF5A605B)
                                        )
                                        Text(
                                            text = "${interval.durationDays} hari • ${interval.bloodColor.shortName}${if (interval.isThick) " • Kental" else ""}${if (interval.isOdorous) " • Berbau" else ""}",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Normal
                                            ),
                                            color = Color(0xFF7A807B)
                                        )
                                    }
                                }

                                Icon(
                                    imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowRight,
                                    contentDescription = if (isExpanded) "Tutup" else "Edit Detail",
                                    tint = Color(0xFF9EA39F),
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            // Expanded In-Depth Phase Editor
                            AnimatedVisibility(visible = isExpanded) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 10.dp),
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    HorizontalDivider(color = Color(0xFFEBE5DC), thickness = 0.5.dp)

                                    // Detailed editor using BloodIntervalCard inner logic
                                    PhaseDetailedEditor(
                                        index = index,
                                        interval = interval,
                                        canRemove = intervals.size > 1,
                                        previousEndEpochMillis = prevEnd,
                                        nextStartEpochMillis = nextStart,
                                        onUpdate = { updated -> onUpdatePhase(index, updated) },
                                        onRemove = { onRemovePhase(index) }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // 4. "+ Tambah Fase Darah" Capsule Button
        Button(
            onClick = onAddPhase,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("add_blood_phase_button"),
            shape = RoundedCornerShape(24.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = EmeraldDeep,
                contentColor = Color.White
            ),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
        ) {
            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "Tambah Fase Darah",
                style = MaterialTheme.typography.labelLarge.copy(
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 0.3.sp
                )
            )
        }
    }
}

@Composable
private fun PhaseDetailedEditor(
    index: Int,
    interval: BleedingInterval,
    canRemove: Boolean,
    previousEndEpochMillis: Long? = null,
    nextStartEpochMillis: Long? = null,
    onUpdate: (BleedingInterval) -> Unit,
    onRemove: () -> Unit
) {
    val context = LocalContext.current
    val dFormat = remember { SimpleDateFormat("EEE, dd MMM yyyy", Locale("id", "ID")) }
    val tFormat = remember { SimpleDateFormat("HH:mm 'WIB'", Locale("id", "ID")) }

    val startCal = Calendar.getInstance().apply { timeInMillis = interval.startEpochMillis }
    val endCal = Calendar.getInstance().apply { timeInMillis = interval.endEpochMillis }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // Blood Color Swatch Selection
        Text(
            text = "Pilih Tingkatan Warna Darah:",
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            BloodColor.entries.forEach { color ->
                val isSelected = interval.bloodColor == color
                val swatchColor = Color(color.hexColor)
                val isLight = color == BloodColor.KUNING || color == BloodColor.KERUH

                Surface(
                    onClick = { onUpdate(interval.copy(bloodColor = color)) },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp),
                    color = if (isSelected) swatchColor.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                    border = BorderStroke(
                        width = if (isSelected) 1.5.dp else 0.5.dp,
                        color = if (isSelected) swatchColor else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(16.dp)
                                .clip(CircleShape)
                                .background(swatchColor)
                                .border(0.5.dp, Color.White.copy(alpha = 0.5f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = if (isLight) Color.Black else Color.White,
                                    modifier = Modifier.size(11.dp)
                                )
                            }
                        }
                        Text(
                            text = color.shortName,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 10.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            ),
                            color = if (isSelected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1
                        )
                    }
                }
            }
        }

        // Date and Time Pickers
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Mulai
            Surface(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
            ) {
                Column(
                    modifier = Modifier.padding(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "Mulai Keluar",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = EmeraldPrimary
                        )
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                DatePickerDialog(
                                    context,
                                    { _, y, m, d ->
                                        val cal = Calendar.getInstance().apply {
                                            timeInMillis = interval.startEpochMillis
                                            set(Calendar.YEAR, y)
                                            set(Calendar.MONTH, m)
                                            set(Calendar.DAY_OF_MONTH, d)
                                        }
                                        onUpdate(interval.copy(startEpochMillis = cal.timeInMillis))
                                    },
                                    startCal.get(Calendar.YEAR),
                                    startCal.get(Calendar.MONTH),
                                    startCal.get(Calendar.DAY_OF_MONTH)
                                ).show()
                            },
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(Icons.Outlined.CalendarToday, contentDescription = null, modifier = Modifier.size(12.dp), tint = EmeraldPrimary)
                        Text(dFormat.format(Date(interval.startEpochMillis)), fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                TimePickerDialog(
                                    context,
                                    { _, h, min ->
                                        val cal = Calendar.getInstance().apply {
                                            timeInMillis = interval.startEpochMillis
                                            set(Calendar.HOUR_OF_DAY, h)
                                            set(Calendar.MINUTE, min)
                                        }
                                        onUpdate(interval.copy(startEpochMillis = cal.timeInMillis))
                                    },
                                    startCal.get(Calendar.HOUR_OF_DAY),
                                    startCal.get(Calendar.MINUTE),
                                    true
                                ).show()
                            },
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(Icons.Outlined.AccessTime, contentDescription = null, modifier = Modifier.size(12.dp), tint = EmeraldPrimary)
                        Text(tFormat.format(Date(interval.startEpochMillis)), fontSize = 11.sp)
                    }
                }
            }

            // Selesai
            Surface(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
            ) {
                Column(
                    modifier = Modifier.padding(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "Selesai / Berhenti",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = EmeraldPrimary
                        )
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                DatePickerDialog(
                                    context,
                                    { _, y, m, d ->
                                        val cal = Calendar.getInstance().apply {
                                            timeInMillis = interval.endEpochMillis
                                            set(Calendar.YEAR, y)
                                            set(Calendar.MONTH, m)
                                            set(Calendar.DAY_OF_MONTH, d)
                                        }
                                        onUpdate(interval.copy(endEpochMillis = cal.timeInMillis))
                                    },
                                    endCal.get(Calendar.YEAR),
                                    endCal.get(Calendar.MONTH),
                                    endCal.get(Calendar.DAY_OF_MONTH)
                                ).show()
                            },
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(Icons.Outlined.CalendarToday, contentDescription = null, modifier = Modifier.size(12.dp), tint = EmeraldPrimary)
                        Text(dFormat.format(Date(interval.endEpochMillis)), fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                TimePickerDialog(
                                    context,
                                    { _, h, min ->
                                        val cal = Calendar.getInstance().apply {
                                            timeInMillis = interval.endEpochMillis
                                            set(Calendar.HOUR_OF_DAY, h)
                                            set(Calendar.MINUTE, min)
                                        }
                                        onUpdate(interval.copy(endEpochMillis = cal.timeInMillis))
                                    },
                                    endCal.get(Calendar.HOUR_OF_DAY),
                                    endCal.get(Calendar.MINUTE),
                                    true
                                ).show()
                            },
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(Icons.Outlined.AccessTime, contentDescription = null, modifier = Modifier.size(12.dp), tint = EmeraldPrimary)
                        Text(tFormat.format(Date(interval.endEpochMillis)), fontSize = 11.sp)
                    }
                }
            }
        }

        // Sifat Darah (Takhin & Muntin) & Delete button
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = interval.isThick,
                onClick = { onUpdate(interval.copy(isThick = !interval.isThick)) },
                label = { Text("Kental (Takhin)", fontSize = 10.5.sp) },
                modifier = Modifier.weight(1f)
            )

            FilterChip(
                selected = interval.isOdorous,
                onClick = { onUpdate(interval.copy(isOdorous = !interval.isOdorous)) },
                label = { Text("Berbau (Muntin)", fontSize = 10.5.sp) },
                modifier = Modifier.weight(1f)
            )

            if (canRemove) {
                IconButton(
                    onClick = onRemove,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.DeleteOutline,
                        contentDescription = "Hapus Fase",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun BloodTimelineVisualizer(
    intervals: List<BleedingInterval>,
    modifier: Modifier = Modifier
) {
    if (intervals.isEmpty()) return

    val totalDuration = intervals.sumOf { it.durationMillis }.coerceAtLeast(1L)
    val totalDays = (totalDuration / (24 * 3600_000L)).coerceAtLeast(0)
    val totalHours = ((totalDuration % (24 * 3600_000L)) / 3600_000L)
    val totalDurationFormatted = if (totalHours > 0) "$totalDays hari $totalHours jam" else "$totalDays hari"

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = Color.White,
        border = BorderStroke(
            width = 1.dp,
            color = Color(0xFFEBE5DC)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(EmeraldPrimary)
                    )
                    Text(
                        text = "Visualisasi Kronologi",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        ),
                        color = Color(0xFF1E211F)
                    )
                }
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = EmeraldPrimary.copy(alpha = 0.1f)
                ) {
                    Text(
                        text = "$totalDurationFormatted (${intervals.size} Fase)",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = EmeraldPrimary
                        ),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            // Timeline segmented bar with polished rounded capsules
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(26.dp)
                    .clip(RoundedCornerShape(13.dp))
                    .background(Color(0xFFF0ECE6))
            ) {
                intervals.forEachIndexed { _, interval ->
                    val fraction = (interval.durationMillis.toFloat() / totalDuration.toFloat()).coerceIn(0.05f, 1f)
                    val barColor = Color(interval.bloodColor.hexColor)
                    val isLightColor = interval.bloodColor == BloodColor.KUNING || interval.bloodColor == BloodColor.KERUH

                    Box(
                        modifier = Modifier
                            .weight(fraction)
                            .fillMaxHeight()
                            .background(barColor)
                            .border(
                                width = 0.75.dp,
                                color = Color.White.copy(alpha = 0.4f)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "${interval.bloodColor.shortName} (${interval.durationDays}h)",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isLightColor) Color.Black.copy(alpha = 0.85f) else Color.White
                            ),
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun BloodIntervalCard(
    index: Int,
    interval: BleedingInterval,
    canRemove: Boolean,
    previousEndEpochMillis: Long? = null,
    nextStartEpochMillis: Long? = null,
    onUpdate: (BleedingInterval) -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .testTag("blood_phase_card_$index"),
        shape = RoundedCornerShape(16.dp),
        color = Color.White,
        border = BorderStroke(1.dp, Color(0xFFEBE5DC))
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            PhaseDetailedEditor(
                index = index,
                interval = interval,
                canRemove = canRemove,
                previousEndEpochMillis = previousEndEpochMillis,
                nextStartEpochMillis = nextStartEpochMillis,
                onUpdate = onUpdate,
                onRemove = onRemove
            )
        }
    }
}

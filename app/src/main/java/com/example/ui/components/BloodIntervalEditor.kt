package com.example.ui.components

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.BleedingInterval
import com.example.model.BloodColor
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun BloodTimelineVisualizer(
    intervals: List<BleedingInterval>,
    modifier: Modifier = Modifier
) {
    if (intervals.isEmpty()) return

    val totalDuration = intervals.sumOf { it.durationMillis }.coerceAtLeast(1L)

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
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
                Text(
                    text = "📊 Visualisasi Kronologi Darah",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "${intervals.size} Fase Terdata",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Timeline segmented bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(26.dp)
                    .clip(RoundedCornerShape(13.dp))
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(13.dp))
            ) {
                intervals.forEachIndexed { idx, interval ->
                    val fraction = (interval.durationMillis.toFloat() / totalDuration.toFloat()).coerceIn(0.05f, 1f)
                    val barColor = Color(interval.bloodColor.hexColor)

                    Box(
                        modifier = Modifier
                            .weight(fraction)
                            .fillMaxHeight()
                            .background(barColor),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "${interval.bloodColor.shortName} (${interval.durationDays}h)",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (interval.bloodColor == BloodColor.KUNING || interval.bloodColor == BloodColor.KERUH) Color.Black else Color.White
                            ),
                            maxLines = 1
                        )
                    }
                }
            }

            // Legend
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                intervals.forEachIndexed { i, item ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(Color(item.bloodColor.hexColor))
                        )
                        Text(
                            text = "Fase ${i + 1}: ${item.bloodColor.shortName} (${item.formattedDuration})",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun BloodIntervalCard(
    index: Int,
    interval: BleedingInterval,
    canRemove: Boolean,
    onUpdate: (BleedingInterval) -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val dtFormat = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale("id", "ID"))

    val startCal = Calendar.getInstance().apply { timeInMillis = interval.startEpochMillis }
    val endCal = Calendar.getInstance().apply { timeInMillis = interval.endEpochMillis }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("blood_phase_card_$index"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(Color(interval.bloodColor.hexColor)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "${index + 1}",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = if (interval.bloodColor == BloodColor.KUNING || interval.bloodColor == BloodColor.KERUH) Color.Black else Color.White
                            )
                        )
                    }
                    Column {
                        Text(
                            text = "Fase ${index + 1}: Darah ${interval.bloodColor.shortName}",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Durasi: ${interval.formattedDuration}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                if (canRemove) {
                    IconButton(
                        onClick = onRemove,
                        modifier = Modifier
                            .size(32.dp)
                            .testTag("remove_phase_button_$index")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Hapus Fase",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

            // 1. Pilih Warna Darah (Quick 1-tap chips)
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "Warna Darah (Tingkat Kekuatan Fiqih):",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold)
                )

                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    BloodColor.entries.forEach { col ->
                        val isSelected = interval.bloodColor == col
                        val chipColor = Color(col.hexColor)
                        Surface(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { onUpdate(interval.copy(bloodColor = col)) }
                                .border(
                                    width = if (isSelected) 2.dp else 1.dp,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                                    shape = RoundedCornerShape(10.dp)
                                ),
                            color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(14.dp)
                                        .clip(CircleShape)
                                        .background(chipColor)
                                )
                                Text(
                                    text = col.shortName,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                                    )
                                )
                            }
                        }
                    }
                }
            }

            // 2. Tanggal & Jam Mulai s/d Selesai Fase
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Rentang Waktu Fase Ini:",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold)
                )

                // Mulai
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Mulai:",
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                        modifier = Modifier.width(55.dp)
                    )

                    OutlinedButton(
                        onClick = {
                            DatePickerDialog(
                                context,
                                { _, y, m, d ->
                                    val newCal = Calendar.getInstance().apply {
                                        timeInMillis = interval.startEpochMillis
                                        set(Calendar.YEAR, y)
                                        set(Calendar.MONTH, m)
                                        set(Calendar.DAY_OF_MONTH, d)
                                    }
                                    onUpdate(interval.copy(startEpochMillis = newCal.timeInMillis))
                                },
                                startCal.get(Calendar.YEAR),
                                startCal.get(Calendar.MONTH),
                                startCal.get(Calendar.DAY_OF_MONTH)
                            ).show()
                        },
                        modifier = Modifier
                            .weight(1.3f)
                            .testTag("phase_start_date_btn_$index"),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                    ) {
                        Icon(Icons.Outlined.CalendarToday, contentDescription = null, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            SimpleDateFormat("dd MMM yyyy", Locale("id", "ID")).format(Date(interval.startEpochMillis)),
                            fontSize = 12.sp
                        )
                    }

                    OutlinedButton(
                        onClick = {
                            TimePickerDialog(
                                context,
                                { _, h, min ->
                                    val newCal = Calendar.getInstance().apply {
                                        timeInMillis = interval.startEpochMillis
                                        set(Calendar.HOUR_OF_DAY, h)
                                        set(Calendar.MINUTE, min)
                                    }
                                    onUpdate(interval.copy(startEpochMillis = newCal.timeInMillis))
                                },
                                startCal.get(Calendar.HOUR_OF_DAY),
                                startCal.get(Calendar.MINUTE),
                                true
                            ).show()
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("phase_start_time_btn_$index"),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                    ) {
                        Icon(Icons.Outlined.AccessTime, contentDescription = null, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            SimpleDateFormat("HH:mm", Locale("id", "ID")).format(Date(interval.startEpochMillis)),
                            fontSize = 12.sp
                        )
                    }
                }

                // Berhenti
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Selesai:",
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                        modifier = Modifier.width(55.dp)
                    )

                    OutlinedButton(
                        onClick = {
                            DatePickerDialog(
                                context,
                                { _, y, m, d ->
                                    val newCal = Calendar.getInstance().apply {
                                        timeInMillis = interval.endEpochMillis
                                        set(Calendar.YEAR, y)
                                        set(Calendar.MONTH, m)
                                        set(Calendar.DAY_OF_MONTH, d)
                                    }
                                    onUpdate(interval.copy(endEpochMillis = newCal.timeInMillis))
                                },
                                endCal.get(Calendar.YEAR),
                                endCal.get(Calendar.MONTH),
                                endCal.get(Calendar.DAY_OF_MONTH)
                            ).show()
                        },
                        modifier = Modifier
                            .weight(1.3f)
                            .testTag("phase_end_date_btn_$index"),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                    ) {
                        Icon(Icons.Outlined.CalendarToday, contentDescription = null, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            SimpleDateFormat("dd MMM yyyy", Locale("id", "ID")).format(Date(interval.endEpochMillis)),
                            fontSize = 12.sp
                        )
                    }

                    OutlinedButton(
                        onClick = {
                            TimePickerDialog(
                                context,
                                { _, h, min ->
                                    val newCal = Calendar.getInstance().apply {
                                        timeInMillis = interval.endEpochMillis
                                        set(Calendar.HOUR_OF_DAY, h)
                                        set(Calendar.MINUTE, min)
                                    }
                                    onUpdate(interval.copy(endEpochMillis = newCal.timeInMillis))
                                },
                                endCal.get(Calendar.HOUR_OF_DAY),
                                endCal.get(Calendar.MINUTE),
                                true
                            ).show()
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("phase_end_time_btn_$index"),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                    ) {
                        Icon(Icons.Outlined.AccessTime, contentDescription = null, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            SimpleDateFormat("HH:mm", Locale("id", "ID")).format(Date(interval.endEpochMillis)),
                            fontSize = 12.sp
                        )
                    }
                }
            }

            // 3. Sifat Darah: Kental & Bau Anyir
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Kental chip
                FilterChip(
                    selected = interval.isThick,
                    onClick = { onUpdate(interval.copy(isThick = !interval.isThick)) },
                    label = { Text(if (interval.isThick) "💧 Kental (Kuat)" else "💧 Encer (Lemah)") },
                    leadingIcon = {
                        Icon(
                            imageVector = if (interval.isThick) Icons.Filled.Check else Icons.Outlined.WaterDrop,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    },
                    modifier = Modifier.weight(1f)
                )

                // Berbau Anyir chip
                FilterChip(
                    selected = interval.isOdorous,
                    onClick = { onUpdate(interval.copy(isOdorous = !interval.isOdorous)) },
                    label = { Text(if (interval.isOdorous) "♨️ Berbau Anyir" else "♨️ Tidak Berbau") },
                    leadingIcon = {
                        Icon(
                            imageVector = if (interval.isOdorous) Icons.Filled.Check else Icons.Outlined.Air,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

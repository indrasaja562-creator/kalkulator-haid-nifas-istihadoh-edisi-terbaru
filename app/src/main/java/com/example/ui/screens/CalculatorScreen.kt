package com.example.ui.screens

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.*
import com.example.ui.components.AppHeroHeader
import com.example.ui.components.BloodIntervalCard
import com.example.ui.components.BloodTimelineVisualizer
import com.example.ui.components.CalculationResultCard
import com.example.ui.viewmodel.FiqihViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalculatorScreen(
    viewModel: FiqihViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    val caseType by viewModel.caseType.collectAsState()
    val intervals by viewModel.intervals.collectAsState()

    // Nifas States
    val deliveryEpochMillis by viewModel.deliveryEpochMillis.collectAsState()
    val deliveryHour by viewModel.deliveryHour.collectAsState()
    val deliveryMinute by viewModel.deliveryMinute.collectAsState()
    val deliveryType by viewModel.deliveryType.collectAsState()
    val twinLastDeliveryEpochMillis by viewModel.twinLastDeliveryEpochMillis.collectAsState()
    val nifasAdatCategory by viewModel.nifasAdatCategory.collectAsState()
    val adatNifasDays by viewModel.adatNifasDays.collectAsState()

    // Jeda Bersih (Fatrah Naqa')
    val hasIntermittentPause by viewModel.hasIntermittentPause.collectAsState()
    val intermittentPauseDays by viewModel.intermittentPauseDays.collectAsState()

    // Haid States
    val haidCategory by viewModel.haidCategory.collectAsState()
    val hasPreviousAdat by viewModel.hasPreviousAdat.collectAsState()
    val adatDurationDays by viewModel.adatDurationDays.collectAsState()
    val adatCycleDays by viewModel.adatCycleDays.collectAsState()
    val adatMemoryType by viewModel.adatMemoryType.collectAsState()

    val hasPreviousHaidBeforeNifas by viewModel.hasPreviousHaidBeforeNifas.collectAsState()
    val previousHaidAdatDays by viewModel.previousHaidAdatDays.collectAsState()
    val previousSuciDaysForTakmilah by viewModel.previousSuciDaysForTakmilah.collectAsState()

    // Shalat In & Out
    val prayerAtStart by viewModel.prayerAtStart.collectAsState()
    val hadPrayedAtStart by viewModel.hadPrayedAtStart.collectAsState()
    val prayerAtStop by viewModel.prayerAtStop.collectAsState()

    val calculationResult by viewModel.calculationResult.collectAsState()
    val historyList by viewModel.calculationHistory.collectAsState()
    val saveMessage by viewModel.saveMessage.collectAsState()

    val dateFormat = SimpleDateFormat("dd MMMM yyyy", Locale("id", "ID"))
    val dateTimeShortFormat = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale("id", "ID"))
    val timeFormat = SimpleDateFormat("HH:mm", Locale("id", "ID"))

    val deliveryCal = Calendar.getInstance().apply {
        timeInMillis = deliveryEpochMillis ?: System.currentTimeMillis()
        set(Calendar.HOUR_OF_DAY, deliveryHour)
        set(Calendar.MINUTE, deliveryMinute)
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("calculator_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Hero Header
        item {
            AppHeroHeader(
                title = "Kalkulator Fiqih Haid & Nifas",
                subtitle = "Analisis akurat berdasarkan Kitab Uyunul Masa-il Linnisa' & Tuhfatun Niswah"
            )
        }

        // Quick Preset Chips (Anti-malas: 1-tap loader langsung isi data akurat)
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Bolt,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Pengisian Cepat (Klik Contoh Nyata 1-Tap):",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SuggestionChip(
                        onClick = { viewModel.loadUserExampleTwoPhases() },
                        label = { Text("🔴 Merah (1-10 12:00) + 🟡 Kuning (11-20 10:00)") },
                        icon = { Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp)) },
                        colors = SuggestionChipDefaults.suggestionChipColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
                        )
                    )

                    SuggestionChip(
                        onClick = { viewModel.loadIstihadhahNifasTuhfatunNiswah() },
                        label = { Text("🤱 Nifas 65 Hari Mumayyizah (Tuhfatun Niswah)") },
                        icon = { Icon(Icons.Default.MenuBook, contentDescription = null, modifier = Modifier.size(16.dp)) },
                        colors = SuggestionChipDefaults.suggestionChipColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f)
                        )
                    )

                    SuggestionChip(
                        onClick = { viewModel.loadNifasMubtadiahGhairuMumayyizah() },
                        label = { Text("👶 Nifas Pemula 1 Warna (Qadha 59 Hari)") },
                        icon = { Icon(Icons.Default.ErrorOutline, contentDescription = null, modifier = Modifier.size(16.dp)) }
                    )

                    SuggestionChip(
                        onClick = { viewModel.loadNifasMutadahAdat40Hari() },
                        label = { Text("📅 Nifas Adat 40 Hari (Qadha 20 Hari)") },
                        icon = { Icon(Icons.Default.EventRepeat, contentDescription = null, modifier = Modifier.size(16.dp)) }
                    )

                    SuggestionChip(
                        onClick = { viewModel.loadNifasJedaBersih16Hari() },
                        label = { Text("⏸️ Nifas Jeda Bersih 16 Hari (Darah Kedua = Haid)") },
                        icon = { Icon(Icons.Default.CallSplit, contentDescription = null, modifier = Modifier.size(16.dp)) }
                    )

                    SuggestionChip(
                        onClick = { viewModel.loadNormalHaidPreset() },
                        label = { Text("🌸 Haid Normal 7 Hari") },
                        icon = { Icon(Icons.Default.CheckCircleOutline, contentDescription = null, modifier = Modifier.size(16.dp)) }
                    )
                }
            }
        }

        // 1. Case Type Selector (Haid vs Nifas)
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "1. Jenis Kasus Darah",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary
                    )
                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                        SegmentedButton(
                            selected = caseType == CaseType.HAID,
                            onClick = { viewModel.setCaseType(CaseType.HAID) },
                            shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
                        ) {
                            Text("Darah Haid (7 Golongan)")
                        }
                        SegmentedButton(
                            selected = caseType == CaseType.NIFAS,
                            onClick = { viewModel.setCaseType(CaseType.NIFAS) },
                            shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
                        ) {
                            Text("Darah Nifas (5+2 Golongan)")
                        }
                    }

                    // Educational snippet for Nifas
                    if (caseType == CaseType.NIFAS) {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.35f)
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.Top,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.MenuBook,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.secondary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text(
                                        text = "Kaidah Nifas (Kitab Uyunul Masa'il Linnisa' & Tuhfatun Niswah):",
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                    Text(
                                        text = "• Batas maksimal nifas: 60 hari 60 malam. Jika darah melewati 60 hari, wanita masuk kategori Mustahadhah fin-Nifas (5 golongan + 2 catatan).\n• Nifas dihitung sejak janin keluar sempurna.\n• Jika jeda suci di tengah masa nifas mencapai 15 hari atau lebih, maka nifas terputus sah, dan darah berikutnya dihukumi darah haid baru.",
                                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp),
                                        color = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // 2. Kolom Khusus Data Persalinan & Bayi (Hanya untuk Nifas)
        if (caseType == CaseType.NIFAS) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.ChildCare,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "2. Data Persalinan & Kelahiran",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        // Tanggal & Jam Persalinan
                        Text(
                            text = "Waktu Persalinan (Janin Keluar):",
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold)
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    DatePickerDialog(
                                        context,
                                        { _, y, m, d ->
                                            val cal = Calendar.getInstance().apply {
                                                timeInMillis = deliveryEpochMillis ?: System.currentTimeMillis()
                                                set(Calendar.YEAR, y)
                                                set(Calendar.MONTH, m)
                                                set(Calendar.DAY_OF_MONTH, d)
                                            }
                                            viewModel.setDeliveryEpochMillis(cal.timeInMillis)
                                        },
                                        deliveryCal.get(Calendar.YEAR),
                                        deliveryCal.get(Calendar.MONTH),
                                        deliveryCal.get(Calendar.DAY_OF_MONTH)
                                    ).show()
                                },
                                modifier = Modifier.weight(1.3f),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Outlined.CalendarToday, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    dateFormat.format(Date(deliveryEpochMillis ?: System.currentTimeMillis())),
                                    fontSize = 12.sp
                                )
                            }

                            OutlinedButton(
                                onClick = {
                                    TimePickerDialog(
                                        context,
                                        { _, h, min ->
                                            viewModel.setDeliveryTime(h, min)
                                        },
                                        deliveryHour,
                                        deliveryMinute,
                                        true
                                    ).show()
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Outlined.AccessTime, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    String.format(Locale.getDefault(), "%02d:%02d", deliveryHour, deliveryMinute),
                                    fontSize = 12.sp
                                )
                            }
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                        // Jenis Kelahiran (DeliveryType)
                        Text(
                            text = "Jenis Persalinan / Janin:",
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold)
                        )
                        var expandedDeliveryType by remember { mutableStateOf(false) }
                        Box {
                            OutlinedButton(
                                onClick = { expandedDeliveryType = true },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text(deliveryType.label, fontSize = 13.sp, modifier = Modifier.weight(1f))
                                Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                            }
                            DropdownMenu(
                                expanded = expandedDeliveryType,
                                onDismissRequest = { expandedDeliveryType = false }
                            ) {
                                DeliveryType.entries.forEach { dt ->
                                    DropdownMenuItem(
                                        text = {
                                            Column {
                                                Text(dt.label, fontWeight = FontWeight.Bold)
                                                Text(
                                                    dt.description,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        },
                                        onClick = {
                                            viewModel.setDeliveryType(dt)
                                            expandedDeliveryType = false
                                        }
                                    )
                                }
                            }
                        }

                        // Penjelasan rujukan jika kembar atau keguguran
                        if (deliveryType == DeliveryType.BAYI_KEMBAR) {
                            Text(
                                text = "💡 Fiqih Kitab: Nifas terhitung sah setelah seluruh bayi lahir sempurna. Darah di antara bayi pertama dan kedua berstatus darah rusak/fasad (Uyunul Masa'il hal. 48 & Tuhfatun Niswah hal. 17).",
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                color = MaterialTheme.colorScheme.primary
                            )
                        } else if (deliveryType == DeliveryType.KEGUGURAN) {
                            Text(
                                text = "💡 Fiqih Kitab: Nifas tetap sah meskipun hanya melahirkan segumpal darah ('alaqah) atau segumpal daging (mudghah) jika bidan/dokter menyatakan ada bentukan bakal manusia (Tuhfatun Niswah hal. 17).",
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }

        // 3. Kolom Riwayat Adat & Kebiasaan (Penentu Otomatis Kategori Fiqih)
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.HistoryEdu,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (caseType == CaseType.NIFAS) "3. Riwayat Persalinan & Adat Nifas" else "2. Riwayat Pengalaman & Adat Haid",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
                        ) {
                            Text(
                                text = "Analisis Otomatis",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                ),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Text(
                        text = if (caseType == CaseType.NIFAS) {
                            "Apakah Anda baru pertama kali melahirkan, atau sudah pernah melahirkan sebelumnya?"
                        } else {
                            "Apakah ini darah haid pertama seumur hidup, atau Anda sudah pernah haid sebelumnya?"
                        },
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    // Pilihan: Pertama Kali (Mubtadi'ah) vs Pernah Sebelumnya (Mu'tadah)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedCard(
                            onClick = {
                                viewModel.setHasPreviousAdat(false)
                                viewModel.setAdatMemoryType(AdatMemoryType.BELUM_PERNAH_HAID)
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.outlinedCardColors(
                                containerColor = if (!hasPreviousAdat) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f) else MaterialTheme.colorScheme.surface
                            ),
                            border = CardDefaults.outlinedCardBorder(enabled = true).copy(
                                brush = androidx.compose.ui.graphics.SolidColor(
                                    if (!hasPreviousAdat) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                                )
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = if (caseType == CaseType.NIFAS) "Kelahiran Pertama" else "Pertama Kali",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = if (!hasPreviousAdat) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "(Mubtadi'ah)",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        OutlinedCard(
                            onClick = {
                                viewModel.setHasPreviousAdat(true)
                                if (adatMemoryType == AdatMemoryType.BELUM_PERNAH_HAID) {
                                    viewModel.setAdatMemoryType(AdatMemoryType.INGAT_LENGKAP_QADRAN_WAQTAN)
                                }
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.outlinedCardColors(
                                containerColor = if (hasPreviousAdat) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f) else MaterialTheme.colorScheme.surface
                            ),
                            border = CardDefaults.outlinedCardBorder(enabled = true).copy(
                                brush = androidx.compose.ui.graphics.SolidColor(
                                    if (hasPreviousAdat) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                                )
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = if (caseType == CaseType.NIFAS) "Pernah Melahirkan" else "Pernah Haid",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = if (hasPreviousAdat) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "(Mu'tadah)",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    // Jika Pernah Sebelumnya (Mu'tadah): Tampilkan Kondisi Daya Ingat & Durasi Adat
                    AnimatedVisibility(visible = hasPreviousAdat) {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                            Text(
                                text = "Kondisi Ingatan tentang Adat Sebelumnya:",
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold)
                            )

                            var expandedMemory by remember { mutableStateOf(false) }
                            Box {
                                OutlinedButton(
                                    onClick = { expandedMemory = true },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text(
                                        text = when (adatMemoryType) {
                                            AdatMemoryType.INGAT_LENGKAP_QADRAN_WAQTAN -> "Ingat Lengkap (Durasi Hari & Waktu/Tanggal)"
                                            AdatMemoryType.INGAT_QADRAN_LUPA_WAQTAN -> "Hanya Ingat Durasi Hari (Lupa Waktu Mulai)"
                                            AdatMemoryType.INGAT_WAQTAN_LUPA_QADRAN -> "Hanya Ingat Waktu Mulai (Lupa Durasi Hari)"
                                            AdatMemoryType.LUPA_SEMUANYA_MUTAHAYYIRAH -> "Lupa Total Durasi & Waktu (Mutahayyirah)"
                                            else -> "Pilih Kondisi Ingatan Adat"
                                        },
                                        fontSize = 13.sp,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                                }
                                DropdownMenu(
                                    expanded = expandedMemory,
                                    onDismissRequest = { expandedMemory = false }
                                ) {
                                    DropdownMenuItem(
                                        text = {
                                            Column {
                                                Text("Ingat Lengkap (Durasi & Waktu)", fontWeight = FontWeight.Bold)
                                                Text(
                                                    "Dzahkirah Qadran wa Waqtan: ingat berapa hari dan tanggal mulainya",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        },
                                        onClick = {
                                            viewModel.setAdatMemoryType(AdatMemoryType.INGAT_LENGKAP_QADRAN_WAQTAN)
                                            expandedMemory = false
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = {
                                            Column {
                                                Text("Hanya Ingat Durasi Hari (Lupa Waktu Mulai)", fontWeight = FontWeight.Bold)
                                                Text(
                                                    "Dzakirah Qadran dunan Waqt: ingat durasi (misal 7 hari), tapi lupa tanggal/jam mulainya",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        },
                                        onClick = {
                                            viewModel.setAdatMemoryType(AdatMemoryType.INGAT_QADRAN_LUPA_WAQTAN)
                                            expandedMemory = false
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = {
                                            Column {
                                                Text("Hanya Ingat Waktu Mulai (Lupa Durasi Hari)", fontWeight = FontWeight.Bold)
                                                Text(
                                                    "Dzakirah Waqtan dunan Qadr: ingat tanggal/jam mulai, tapi lupa berapa hari biasanya darah keluar",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        },
                                        onClick = {
                                            viewModel.setAdatMemoryType(AdatMemoryType.INGAT_WAQTAN_LUPA_QADRAN)
                                            expandedMemory = false
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = {
                                            Column {
                                                Text("Lupa Total / Ragu Semuanya (Mutahayyirah)", fontWeight = FontWeight.Bold)
                                                Text(
                                                    "Mutahayyirah Muthlaqah: sama sekali tidak ingat kadar hari maupun waktu mulai adat sebelumnya",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        },
                                        onClick = {
                                            viewModel.setAdatMemoryType(AdatMemoryType.LUPA_SEMUANYA_MUTAHAYYIRAH)
                                            expandedMemory = false
                                        }
                                    )
                                }
                            }

                            // Input Durasi Hari (jika ingat durasi)
                            if (adatMemoryType == AdatMemoryType.INGAT_LENGKAP_QADRAN_WAQTAN ||
                                adatMemoryType == AdatMemoryType.INGAT_QADRAN_LUPA_WAQTAN
                            ) {
                                if (caseType == CaseType.NIFAS) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text("Durasi Adat Nifas Kelahiran Lalu:", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold))
                                            Text("Kadar hari darah nifas persalinan sebelumnya (biasanya 40 hari)", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            IconButton(
                                                onClick = { viewModel.setAdatNifasDays(adatNifasDays - 1) },
                                                enabled = adatNifasDays > 1
                                            ) {
                                                Icon(Icons.Default.RemoveCircleOutline, contentDescription = "Kurang")
                                            }
                                            Text(
                                                text = "$adatNifasDays Hari",
                                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                                modifier = Modifier.padding(horizontal = 6.dp)
                                            )
                                            IconButton(
                                                onClick = { viewModel.setAdatNifasDays(adatNifasDays + 1) },
                                                enabled = adatNifasDays < 60
                                            ) {
                                                Icon(Icons.Default.AddCircleOutline, contentDescription = "Tambah")
                                            }
                                        }
                                    }
                                } else {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text("Durasi Adat Haid Bulan Lalu:", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold))
                                            Text("Berapa hari haid Anda biasanya (misal 6-8 hari)", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            IconButton(
                                                onClick = { viewModel.setAdatDurationDays(adatDurationDays - 1) },
                                                enabled = adatDurationDays > 1
                                            ) {
                                                Icon(Icons.Default.RemoveCircleOutline, contentDescription = "Kurang")
                                            }
                                            Text(
                                                text = "$adatDurationDays Hari",
                                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                                modifier = Modifier.padding(horizontal = 6.dp)
                                            )
                                            IconButton(
                                                onClick = { viewModel.setAdatDurationDays(adatDurationDays + 1) },
                                                enabled = adatDurationDays < 15
                                            ) {
                                                Icon(Icons.Default.AddCircleOutline, contentDescription = "Tambah")
                                            }
                                        }
                                    }

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text("Masa Suci Terakhir (Sebelum Darah Ini):", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold))
                                            Text("Berapa hari Anda suci sejak haid terakhir berhenti (normalnya >=15). Jika <15, darah baru ini akan menjadi Istihadhah Takmilatan lit-Tuhri.", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            IconButton(
                                                onClick = { viewModel.setPreviousSuciDaysForTakmilah(previousSuciDaysForTakmilah - 1) },
                                                enabled = previousSuciDaysForTakmilah > 0
                                            ) {
                                                Icon(Icons.Default.RemoveCircleOutline, contentDescription = "Kurang")
                                            }
                                            Text(
                                                text = "$previousSuciDaysForTakmilah Hari",
                                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                                modifier = Modifier.padding(horizontal = 6.dp)
                                            )
                                            IconButton(
                                                onClick = { viewModel.setPreviousSuciDaysForTakmilah(previousSuciDaysForTakmilah + 1) },
                                                enabled = previousSuciDaysForTakmilah < 365
                                            ) {
                                                Icon(Icons.Default.AddCircleOutline, contentDescription = "Tambah")
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Tambahan untuk Nifas Mubtadi'ah (Pertama Melahirkan)
                    AnimatedVisibility(visible = !hasPreviousAdat && caseType == CaseType.NIFAS) {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                            Text(
                                text = "Apakah Anda sudah pernah mengalami Haid sebelumnya?",
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold)
                            )
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                OutlinedCard(
                                    onClick = { viewModel.setHasPreviousHaidBeforeNifas(true) },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.outlinedCardColors(
                                        containerColor = if (hasPreviousHaidBeforeNifas) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f) else MaterialTheme.colorScheme.surface
                                    ),
                                    border = CardDefaults.outlinedCardBorder(enabled = true).copy(
                                        brush = androidx.compose.ui.graphics.SolidColor(
                                            if (hasPreviousHaidBeforeNifas) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                                        )
                                    )
                                ) {
                                    Column(
                                        modifier = Modifier.padding(12.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text(
                                            text = "Sudah Pernah",
                                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                            color = if (hasPreviousHaidBeforeNifas) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                        )
                                        Text("Poin B (Mu'tadah Haid)", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }

                                OutlinedCard(
                                    onClick = { viewModel.setHasPreviousHaidBeforeNifas(false) },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.outlinedCardColors(
                                        containerColor = if (!hasPreviousHaidBeforeNifas) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f) else MaterialTheme.colorScheme.surface
                                    ),
                                    border = CardDefaults.outlinedCardBorder(enabled = true).copy(
                                        brush = androidx.compose.ui.graphics.SolidColor(
                                            if (!hasPreviousHaidBeforeNifas) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                                        )
                                    )
                                ) {
                                    Column(
                                        modifier = Modifier.padding(12.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text(
                                            text = "Belum Pernah",
                                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                            color = if (!hasPreviousHaidBeforeNifas) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                        )
                                        Text("Poin A (Mubtadi'ah Haid)", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            }

                            AnimatedVisibility(visible = hasPreviousHaidBeforeNifas) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("Adat Haid Sebelumnya:", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold))
                                        Text("Berapa hari haid Anda biasanya (misal 6-8 hari)", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        IconButton(
                                            onClick = { viewModel.setPreviousHaidAdatDays(previousHaidAdatDays - 1) },
                                            enabled = previousHaidAdatDays > 1
                                        ) {
                                            Icon(Icons.Default.RemoveCircleOutline, contentDescription = "Kurang")
                                        }
                                        Text(
                                            text = "$previousHaidAdatDays Hari",
                                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                            modifier = Modifier.padding(horizontal = 6.dp)
                                        )
                                        IconButton(
                                            onClick = { viewModel.setPreviousHaidAdatDays(previousHaidAdatDays + 1) },
                                            enabled = previousHaidAdatDays < 15
                                        ) {
                                            Icon(Icons.Default.AddCircleOutline, contentDescription = "Tambah")
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Notice Card: Penjelasan Otomatisasi
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Kategori istihadhah Anda (7 jenis haid atau 5+2 nifas) akan dianalisis secara otomatis berdasarkan data di atas serta karakteristik fase darah yang Anda masukkan di bawah.",
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp, lineHeight = 16.sp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        // 4. Kolom Pengecekan Jeda Bersih / Terputus-putus (Fatrah Naqa')
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Pengecekan Jeda Bersih (Fatrah Naqa')",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "Apakah darah sempat berhenti/bersih di tengah rentang waktu?",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = hasIntermittentPause,
                            onCheckedChange = { viewModel.setHasIntermittentPause(it) }
                        )
                    }

                    AnimatedVisibility(visible = hasIntermittentPause) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Lama Jeda Bersih:", style = MaterialTheme.typography.bodySmall)
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    IconButton(
                                        onClick = { viewModel.setIntermittentPauseDays(intermittentPauseDays - 1.0) },
                                        enabled = intermittentPauseDays > 1.0
                                    ) {
                                        Icon(Icons.Default.RemoveCircleOutline, contentDescription = null)
                                    }
                                    Text(
                                        text = "${intermittentPauseDays.toInt()} Hari",
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                        modifier = Modifier.padding(horizontal = 6.dp)
                                    )
                                    IconButton(
                                        onClick = { viewModel.setIntermittentPauseDays(intermittentPauseDays + 1.0) },
                                        enabled = intermittentPauseDays < 40.0
                                    ) {
                                        Icon(Icons.Default.AddCircleOutline, contentDescription = null)
                                    }
                                }
                            }

                            if (caseType == CaseType.NIFAS && intermittentPauseDays >= 15.0) {
                                Card(
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                                    ),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Text(
                                        text = "⚠️ Hukum Fiqih Kitab Uyunul Masa'il hal. 51: Karena jeda bersih mencapai 15 hari atau lebih, maka masa nifas terputus secara sah! Darah yang keluar setelah jeda 15 hari tersebut BUKAN NIFAS LAGI, melainkan berstatus DARAH HAID BARU.",
                                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp),
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.padding(10.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // 5. Kronologi Detail Fase Darah (Tanggal, Jam, Warna & Sifat)
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "5. Rincian Tanggal, Jam & Sifat Darah",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Detail perubahan warna, kental & bau darah per tanggal/jam",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    FilledTonalButton(
                        onClick = { viewModel.addNewNextInterval() },
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("+ Fase", fontSize = 12.sp)
                    }
                }

                // Interactive visual timeline bar
                BloodTimelineVisualizer(intervals = intervals)
            }
        }

        // List of blood phases with custom time and date pickers
        items(intervals.size) { index ->
            val item = intervals[index]
            BloodIntervalCard(
                index = index,
                interval = item,
                canRemove = intervals.size > 1,
                onUpdate = { updated -> viewModel.updateInterval(index, updated) },
                onRemove = { viewModel.removeInterval(index) }
            )
        }

        // Button to add another phase
        item {
            OutlinedButton(
                onClick = { viewModel.addNewNextInterval() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("add_blood_phase_button"),
                shape = RoundedCornerShape(14.dp)
            ) {
                Icon(Icons.Default.AddCircleOutline, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Tambah Perubahan Warna / Sifat Darah Lainnya")
            }
        }

        // 6. Shalat Terkait di Awal & Akhir Darah (Idrak al-Waqt & Qadha)
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "6. Pengecekan Waktu Shalat Awal & Akhir",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary
                    )

                    // Shalat at start
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Masuk waktu shalat saat darah mulai keluar?",
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.weight(1f)
                        )
                        var expandedPrayer by remember { mutableStateOf(false) }
                        Box {
                            TextButton(onClick = { expandedPrayer = true }) {
                                Text(prayerAtStart?.label ?: "Di luar shalat")
                                Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                            }
                            DropdownMenu(expanded = expandedPrayer, onDismissRequest = { expandedPrayer = false }) {
                                DropdownMenuItem(
                                    text = { Text("Di luar waktu shalat") },
                                    onClick = {
                                        viewModel.setPrayerAtStart(null)
                                        expandedPrayer = false
                                    }
                                )
                                PrayerName.entries.forEach { p ->
                                    DropdownMenuItem(
                                        text = { Text(p.label) },
                                        onClick = {
                                            viewModel.setPrayerAtStart(p)
                                            expandedPrayer = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    if (prayerAtStart != null) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Sudah sempat shalat ${prayerAtStart?.label} sebelum darah keluar?",
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.weight(1f)
                            )
                            Switch(
                                checked = hadPrayedAtStart,
                                onCheckedChange = { viewModel.setHadPrayedAtStart(it) },
                                modifier = Modifier.testTag("had_prayed_switch")
                            )
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                    // Shalat at stop
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Darah berhenti saat masuk waktu shalat:",
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.weight(1f)
                        )
                        var expandedStopPrayer by remember { mutableStateOf(false) }
                        Box {
                            TextButton(onClick = { expandedStopPrayer = true }) {
                                Text(prayerAtStop?.label ?: "Pilih Waktu")
                                Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                            }
                            DropdownMenu(
                                expanded = expandedStopPrayer,
                                onDismissRequest = { expandedStopPrayer = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Di luar waktu shalat") },
                                    onClick = {
                                        viewModel.setPrayerAtStop(null)
                                        expandedStopPrayer = false
                                    }
                                )
                                PrayerName.entries.forEach { p ->
                                    DropdownMenuItem(
                                        text = { Text(p.label) },
                                        onClick = {
                                            viewModel.setPrayerAtStop(p)
                                            expandedStopPrayer = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Action Button: Hitung Status Fiqih & Ibadah
        item {
            Button(
                onClick = { viewModel.doCalculate() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .testTag("calculate_button"),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Icon(Icons.Default.CheckCircle, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Hitung Status Fiqih & Ibadah",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            }
        }

        // Save feedback banner
        item {
            saveMessage?.let { msg ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("save_message_banner"),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = msg,
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                        IconButton(
                            onClick = { viewModel.clearSaveMessage() },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Tutup", modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
        }

        // Calculation Result Card Display
        item {
            calculationResult?.let { res ->
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    CalculationResultCard(result = res)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { viewModel.saveCurrentCalculation() },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("save_to_profile_button"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.BookmarkAdd, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Simpan ke Profil", style = MaterialTheme.typography.labelMedium)
                        }

                        Button(
                            onClick = { viewModel.setTab(3) },
                            modifier = Modifier
                                .weight(1.1f)
                                .testTag("compare_cycle_button"),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                        ) {
                            Icon(Icons.Default.CompareArrows, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Bandingkan Siklus", style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }
            }
        }

        // History Section
        if (historyList.isNotEmpty()) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Riwayat Analisis (${historyList.size})",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary
                    )

                    TextButton(onClick = { viewModel.setTab(3) }) {
                        Text("Buka Profil & Bandingkan", style = MaterialTheme.typography.labelSmall)
                        Spacer(modifier = Modifier.width(2.dp))
                        Icon(Icons.Default.ArrowForward, contentDescription = null, modifier = Modifier.size(14.dp))
                    }
                }
            }

            items(historyList.take(5)) { item ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = item.statusSummary,
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = item.categoryName,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = dateFormat.format(Date(item.timestamp)),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        IconButton(onClick = { viewModel.deleteHistory(item.id) }) {
                            Icon(Icons.Default.DeleteOutline, contentDescription = "Hapus Riwayat", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
        }
    }
}

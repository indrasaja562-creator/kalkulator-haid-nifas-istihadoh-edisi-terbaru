package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.model.AdatMemoryType

@Composable
fun MutahayyirahGuideModuleDialog(
    onDismissRequest: () -> Unit,
    onApplyDiagnosis: (isMuTadah: Boolean, isMumayyizah: Boolean, memoryType: AdatMemoryType, adatDays: Int) -> Unit
) {
    var currentStep by remember { mutableIntStateOf(1) }
    var isMuTadah by remember { mutableStateOf(true) }
    var isMumayyizah by remember { mutableStateOf(false) }
    var memoryType by remember { mutableStateOf(AdatMemoryType.LUPA_SEMUANYA_MUTAHAYYIRAH) }
    var adatDays by remember { mutableIntStateOf(7) }

    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.96f)
                .fillMaxHeight(0.92f)
                .testTag("mutahayyirah_guide_dialog"),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                // Header Dialog
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Explore,
                                contentDescription = "Panduan Diagnosa",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                        Column {
                            Text(
                                text = "Panduan Diagnosa Mutahayyirah",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Kaidah Mazhab Syafi'i (Uyunul Masa'il)",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    IconButton(
                        onClick = onDismissRequest,
                        modifier = Modifier.testTag("btn_close_guide_dialog")
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Tutup")
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Stepper Indicator (1 - 2 - 3 - Hasil)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val stepLabels = listOf("Pengalaman", "Sifat Darah", "Ingatan Adat", "Hasil Fikih")
                    stepLabels.forEachIndexed { index, label ->
                        val stepNum = index + 1
                        val isActive = currentStep == stepNum
                        val isPassed = currentStep > stepNum
                        val circleColor by animateColorAsState(
                            targetValue = when {
                                isActive -> MaterialTheme.colorScheme.primary
                                isPassed -> MaterialTheme.colorScheme.secondary
                                else -> MaterialTheme.colorScheme.surfaceVariant
                            },
                            label = "stepColor"
                        )
                        val textColor = when {
                            isActive || isPassed -> MaterialTheme.colorScheme.onPrimary
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        }

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .background(circleColor),
                                contentAlignment = Alignment.Center
                            ) {
                                if (isPassed) {
                                    Icon(
                                        Icons.Default.Check,
                                        contentDescription = null,
                                        tint = textColor,
                                        modifier = Modifier.size(16.dp)
                                    )
                                } else {
                                    Text(
                                        text = "$stepNum",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = textColor
                                    )
                                }
                            }
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 10.sp,
                                    fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal
                                ),
                                color = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                // Body Content Scrollable
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    when (currentStep) {
                        1 -> Step1Experience(
                            isMuTadah = isMuTadah,
                            onSelect = { isMuTadah = it }
                        )
                        2 -> Step2BloodCharacteristics(
                            isMumayyizah = isMumayyizah,
                            onSelect = { isMumayyizah = it }
                        )
                        3 -> Step3MemoryAdat(
                            isMuTadah = isMuTadah,
                            isMumayyizah = isMumayyizah,
                            memoryType = memoryType,
                            adatDays = adatDays,
                            onSelectMemory = { memoryType = it },
                            onAdatDaysChange = { adatDays = it }
                        )
                        4 -> Step4DiagnosisResult(
                            isMuTadah = isMuTadah,
                            isMumayyizah = isMumayyizah,
                            memoryType = memoryType,
                            adatDays = adatDays
                        )
                    }
                }

                // Footer Buttons
                HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (currentStep > 1) {
                        OutlinedButton(
                            onClick = { currentStep-- },
                            modifier = Modifier.testTag("btn_guide_prev")
                        ) {
                            Icon(Icons.Default.ArrowBack, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Kembali")
                        }
                    } else {
                        TextButton(onClick = onDismissRequest) {
                            Text("Batal")
                        }
                    }

                    if (currentStep < 4) {
                        Button(
                            onClick = {
                                if (currentStep == 2 && isMumayyizah) {
                                    // If Mumayyizah, skip to diagnosis result because Tamyiz overrides memory/habit!
                                    currentStep = 4
                                } else if (currentStep == 1 && !isMuTadah) {
                                    // If Mubtadi'ah, still check tamyiz in step 2
                                    currentStep++
                                } else {
                                    currentStep++
                                }
                            },
                            modifier = Modifier.testTag("btn_guide_next")
                        ) {
                            Text(if (currentStep == 2 && isMumayyizah) "Lihat Hasil (Tamyiz)" else "Lanjut")
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(Icons.Default.ArrowForward, contentDescription = null, modifier = Modifier.size(16.dp))
                        }
                    } else {
                        Button(
                            onClick = {
                                onApplyDiagnosis(isMuTadah, isMumayyizah, memoryType, adatDays)
                                onDismissRequest()
                            },
                            modifier = Modifier.testTag("btn_apply_diagnosis_to_calculator")
                        ) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Terapkan ke Kalkulator")
                        }
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------------------------------------------
// Step 1: Pengalaman Haid Masa Lalu (Mubtadi'ah vs Mu'tadah)
// -------------------------------------------------------------------------------------------------
@Composable
private fun Step1Experience(
    isMuTadah: Boolean,
    onSelect: (Boolean) -> Unit
) {
    Text(
        text = "Langkah 1: Riwayat Haid",
        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
        color = MaterialTheme.colorScheme.primary
    )
    Text(
        text = "Apakah Anda sudah pernah mengalami siklus haid dan suci sebelumnya?",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurface
    )

    Spacer(modifier = Modifier.height(8.dp))

    // Card 1: Mubtadi'ah
    GuideSelectableCard(
        title = "Belum Pernah (Mubtadi'ah)",
        subtitle = "Ini adalah pengalaman haid pertama saya.",
        isSelected = !isMuTadah,
        icon = Icons.Default.FiberNew,
        onClick = { onSelect(false) }
    )

    // Card 2: Mu'tadah
    GuideSelectableCard(
        title = "Sudah Pernah (Mu'tadah)",
        subtitle = "Saya sudah pernah haid dan suci sebelumnya.",
        isSelected = isMuTadah,
        icon = Icons.Default.EventRepeat,
        onClick = { onSelect(true) }
    )
}

// -------------------------------------------------------------------------------------------------
// Step 2: Sifat Darah (Mumayyizah vs Ghairu Mumayyizah)
// -------------------------------------------------------------------------------------------------
@Composable
private fun Step2BloodCharacteristics(
    isMumayyizah: Boolean,
    onSelect: (Boolean) -> Unit
) {
    Text(
        text = "Langkah 2: Sifat Darah",
        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
        color = MaterialTheme.colorScheme.primary
    )
    Text(
        text = "Apakah darah Anda dapat dibedakan antara darah kuat (kental/gelap) dan lemah (encer/terang)?",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurface
    )

    Spacer(modifier = Modifier.height(8.dp))

    // Card 1: Mumayyizah
    GuideSelectableCard(
        title = "Bisa Dibedakan (Mumayyizah)",
        subtitle = "Ada darah kuat (hitam/merah kental) & darah lemah (merah muda/kuning).",
        isSelected = isMumayyizah,
        icon = Icons.Default.Palette,
        onClick = { onSelect(true) }
    )

    // Card 2: Ghairu Mumayyizah
    GuideSelectableCard(
        title = "Satu Warna Seragam",
        subtitle = "Sulit dibedakan warnanya, atau hanya keluar 1 warna seragam.",
        isSelected = !isMumayyizah,
        icon = Icons.Default.InvertColors,
        onClick = { onSelect(false) }
    )
}

// -------------------------------------------------------------------------------------------------
// Step 3: Status Ingatan Adat (Dzakirah vs Nasiyah / Mutahayyirah)
// -------------------------------------------------------------------------------------------------
@Composable
private fun Step3MemoryAdat(
    isMuTadah: Boolean,
    isMumayyizah: Boolean,
    memoryType: AdatMemoryType,
    adatDays: Int,
    onSelectMemory: (AdatMemoryType) -> Unit,
    onAdatDaysChange: (Int) -> Unit
) {
    Text(
        text = "Langkah 3: Daya Ingat Kebiasaan Haid",
        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
        color = MaterialTheme.colorScheme.primary
    )
    Text(
        text = "Berapa banyak yang Anda ingat tentang siklus haid terakhir Anda?",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurface
    )
    Spacer(modifier = Modifier.height(8.dp))

    // Option 1: Dzakirah Lengkap
    GuideSelectableCard(
        title = "1. Ingat Sepenuhnya",
        subtitle = "Ingat tanggal mulai DAN ingat berapa hari biasanya.",
        isSelected = memoryType == AdatMemoryType.INGAT_LENGKAP_QADRAN_WAQTAN,
        icon = Icons.Default.CheckCircleOutline,
        onClick = { onSelectMemory(AdatMemoryType.INGAT_LENGKAP_QADRAN_WAQTAN) }
    )

    // Option 2: Nasiyah Lupa Total (Mutahayyirah Mahdhah)
    GuideSelectableCard(
        title = "2. Lupa Total (Mutahayyirah)",
        subtitle = "Sama sekali tidak ingat tanggal mulai maupun durasinya.",
        isSelected = memoryType == AdatMemoryType.LUPA_SEMUANYA_MUTAHAYYIRAH,
        icon = Icons.Default.HelpOutline,
        onClick = { onSelectMemory(AdatMemoryType.LUPA_SEMUANYA_MUTAHAYYIRAH) }
    )

    // Option 3: Dzakirah Waqtan
    GuideSelectableCard(
        title = "3. Ingat Tanggal Saja",
        subtitle = "Hanya ingat tanggal mulainya, tapi lupa durasinya.",
        isSelected = memoryType == AdatMemoryType.INGAT_WAQTAN_LUPA_QADRAN,
        icon = Icons.Default.Schedule,
        onClick = { onSelectMemory(AdatMemoryType.INGAT_WAQTAN_LUPA_QADRAN) }
    )

    // Option 4: Dzakirah Qadran
    GuideSelectableCard(
        title = "4. Ingat Durasi Saja",
        subtitle = "Hanya ingat durasi (berapa hari), tapi lupa tanggal mulainya.",
        isSelected = memoryType == AdatMemoryType.INGAT_QADRAN_LUPA_WAQTAN,
        icon = Icons.Default.DateRange,
        onClick = { onSelectMemory(AdatMemoryType.INGAT_QADRAN_LUPA_WAQTAN) }
    )

    // Counter jika ingat durasi
    if (memoryType == AdatMemoryType.INGAT_LENGKAP_QADRAN_WAQTAN || memoryType == AdatMemoryType.INGAT_QADRAN_LUPA_WAQTAN) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("Lama Haid yang Diingat", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold))
                    Text("Maksimal 15 hari", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = { onAdatDaysChange((adatDays - 1).coerceAtLeast(1)) },
                        enabled = adatDays > 1,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(Icons.Default.RemoveCircleOutline, contentDescription = "Kurang")
                    }
                    Text(
                        text = "$adatDays Hari",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        modifier = Modifier.padding(horizontal = 6.dp)
                    )
                    IconButton(
                        onClick = { onAdatDaysChange((adatDays + 1).coerceAtMost(15)) },
                        enabled = adatDays < 15,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(Icons.Default.AddCircleOutline, contentDescription = "Tambah")
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------------------------------------------
// Step 4: Hasil Diagnosa Lengkap (Diagnosis Summary)
// -------------------------------------------------------------------------------------------------
@Composable
private fun Step4DiagnosisResult(
    isMuTadah: Boolean,
    isMumayyizah: Boolean,
    memoryType: AdatMemoryType,
    adatDays: Int
) {
    val categoryTitle: String
    val categoryBadge: String
    val confusionStatus: String
    val shalatRule: String
    val mandiRule: String
    val puasaRule: String
    val jimaRule: String
    val ibaratQuote: String
    val kitabSource: String

    if (!isMuTadah) {
        if (isMumayyizah) {
            categoryTitle = "Golongan 1: Mubtadi'ah Mumayyizah"
            categoryBadge = "Bukan Mutahayyirah (Pemula dengan Tamyiz)"
            confusionStatus = "Tidak mengalami kebingungan karena memiliki darah pembeda (Tamyiz) yang sah."
            shalatRule = "Shalat gugur pada masa darah kuat (haid). Shalat di masa darah lemah wajib diqadha jika sempat ditinggalkan, lalu wajib shalat adā' (tepat waktu)."
            mandiRule = "Wajib mandi segera saat darah beralih dari kuat ke lemah (atau setelah hari ke-15)."
            puasaRule = "Puasa batal pada masa darah kuat (qadha di luar Ramadhan). Puasa pada masa darah lemah sah."
            jimaRule = "Haram jima' pada masa darah kuat. Halal jima' pada masa darah lemah (setelah mandi wajib)."
            ibaratQuote = "المميزة ترد إلى التمييز فالقوي حيض والضعيف استحاضة"
            kitabSource = "Uyunul Masa-il Linnisa' hal. 66 & Tuhfatun Niswah hal. 27"
        } else {
            categoryTitle = "Golongan 2: Mubtadi'ah Ghairu Mumayyizah"
            categoryBadge = "Bukan Mutahayyirah (Pemula Tanpa Tamyiz)"
            confusionStatus = "Pemula tanpa pembeda darah. Dihukumi dengan batas minimal haid menurut ijma' Syafi'i."
            shalatRule = "Haid hanya 24 jam pertama. Shalat dari hari ke-2 s/d ke-15 yang ditinggalkan WAJIB DIQADHA seluruhnya (14 hari)."
            mandiRule = "Wajib mandi jinabat pada hari ke-15 (saat mengetahui istihadhah)."
            puasaRule = "Puasa sah mulai hari ke-2, puasa hari ke-1 wajib diqadha."
            jimaRule = "Haram jima' hanya di 24 jam pertama. Halal mulai hari ke-2 (setelah mandi)."
            ibaratQuote = "المبتدأة غير المميزة ترد إلى أقل الحيض وهو يوم وليلة وطهرها تسعة وعشرون يوما"
            kitabSource = "Uyunul Masa-il Linnisa' hal. 70"
        }
    } else {
        if (isMumayyizah) {
            categoryTitle = "Golongan 3: Mu'tadah Mumayyizah"
            categoryBadge = "Bukan Mutahayyirah (Tamyiz Mengalahkan Adat)"
            confusionStatus = "Tidak tergolong Mutahayyirah! Sifat darah kuat & lemah (Tamyiz) mengalahkan riwayat adat lama."
            shalatRule = "Shalat gugur di masa darah kuat. Shalat di masa darah lemah wajib diqadha jika sempat ditinggalkan."
            mandiRule = "Wajib mandi setelah masa darah kuat berakhir."
            puasaRule = "Puasa batal di masa darah kuat, sah di masa darah lemah."
            jimaRule = "Haram jima' di masa darah kuat, halal di masa darah lemah sesudah mandi."
            ibaratQuote = "التمييز مقدم على العادة لأن التمييز علامة في الدم نفسه"
            kitabSource = "Uyunul Masa-il Linnisa' hal. 70 & Al-Ibanah wal-Ifadhah"
        } else {
            when (memoryType) {
                AdatMemoryType.INGAT_LENGKAP_QADRAN_WAQTAN -> {
                    categoryTitle = "Golongan 4: Mu'tadah Ghairu Mumayyizah Dzakirah"
                    categoryBadge = "Bukan Mutahayyirah (Ingat Adat Lengkap)"
                    confusionStatus = "Tidak bingung. Haid dikembalikan persis kepada ukuran kebiasaan haid bulan lalu ($adatDays hari)."
                    shalatRule = "Haid sah adalah $adatDays hari. Shalat hari ke-${adatDays + 1} s/d ke-15 yang sempat ditinggalkan WAJIB DIQADHA."
                    mandiRule = "Wajib mandi pada hari ke-15 lalu mengqadha shalat yang terutang."
                    puasaRule = "Puasa selama $adatDays hari haid wajib diqadha. Puasa selebihnya sah."
                    jimaRule = "Haram jima' selama $adatDays hari. Halal sesudahnya setelah mandi."
                    ibaratQuote = "المعتادة غير المميزة الذاكرة لقدر عادتها ووقتها ترد إلى قدر عادتها ووقتها"
                    kitabSource = "Uyunul Masa-il Linnisa' hal. 73"
                }
                AdatMemoryType.LUPA_SEMUANYA_MUTAHAYYIRAH -> {
                    categoryTitle = "Golongan 5: Mu'tadah Nasiyah (Mutahayyirah Mahdhah / Muthlaqah)"
                    categoryBadge = "MUTAHAYYIRAH MAHDHAH (Kebingungan Total)"
                    confusionStatus = "Lupa durasi dan lupa waktu mulai. Berlaku hukum IHTIYAT (kehati-hatian) penuh."
                    shalatRule = "WAJIB SHALAT 5 WAKTU: Wajib shalat tepat waktu. Bersuci (mandi/wudhu istibahah) setiap masuk waktu shalat fardhu. Shalat tidak boleh dijamak."
                    mandiRule = "Wajib bersuci (mandi atau wudhu) di tiap waktu shalat fardhu selama 15 hari pertama."
                    puasaRule = "Wajib puasa penuh sebulan Ramadhan (sah 14 hari). Sisa 16 hari diqadha di luar Ramadhan dengan rumus qadha Mutahayyirah Syafi'i."
                    jimaRule = "HARAM JIMA': Suami dilarang menyetubuhi istri selama 15 hari pertama masa ihtiyath. Haram juga membaca Al-Qur'an di luar shalat."
                    ibaratQuote = "المتحيرة المحضة تصلي وتصوم احتياطا وتغتسل لكل فرض ولا يحل لزوجها وطؤها وما بعد الخمسة عشر طهر يقين"
                    kitabSource = "Uyunul Masa-il Linnisa' hal. 84-88 & Al-Ibanah wal-Ifadhah hal. 70"
                }
                AdatMemoryType.INGAT_WAQTAN_LUPA_QADRAN -> {
                    categoryTitle = "Golongan 6: Mu'tadah Dzakirah lil-Waqti dūnal Qadr"
                    categoryBadge = "MUTAHAYYIRAH DZAKIRAH WAQTAN (Ingat Waktu Saja)"
                    confusionStatus = "Ingat kapan mulai, lupa berapa hari durasinya. 24 jam pertama haid yakin, sisa 14 hari ihtiyath."
                    shalatRule = "Hari ke-1 (24 jam pertama): Shalat gugur (Haid Yakin, tidak perlu diqadha). Hari ke-2 s/d 15: Masa Ihtiyath, wajib shalat dan puasa. Hari 16+: Istihadhah murni."
                    mandiRule = "Wajib mandi pada setiap waktu yang dimungkinkan putusnya darah haid adat."
                    puasaRule = "Puasa hari ke-1 batal (qadha). Puasa hari ke-2 s/d 15 wajib dikerjakan atas dasar ihtiyath."
                    jimaRule = "Haram jima' pada hari ke-1 dan selama masa ihtiyath (hari ke-2 s/d 15). Halal mulai hari ke-16."
                    ibaratQuote = "الذاكرة للوقت دون القدر تجعل أول دمها حيضا يقينا يوما وليلة وتحتاط في الباقي إلى خمسة عشر"
                    kitabSource = "Uyunul Masa-il Linnisa' hal. 80-82"
                }
                AdatMemoryType.INGAT_QADRAN_LUPA_WAQTAN -> {
                    categoryTitle = "Golongan 7: Mu'tadah Dzakirah lil-Qadri dūnan Waqt"
                    categoryBadge = "MUTAHAYYIRAH DZAKIRAH QADRAN (Ingat Durasi Saja)"
                    confusionStatus = "Ingat durasi ($adatDays hari), lupa kapan waktu mulainya. Berlaku hukum Ihtiyath sepanjang 15 hari."
                    shalatRule = "Wajib shalat 5 waktu sepanjang rentang 15 hari pertama atas dasar ihtiyath. Bersuci di tiap waktu fardhu."
                    mandiRule = "Mandi wajib dilakukan pada waktu yang diperkirakan sebagai akhir dari durasi adat $adatDays hari tersebut."
                    puasaRule = "Wajib puasa penuh Ramadhan, lalu mengqadha sisa hari sesuai perhitungan kadar adat Syafi'i."
                    jimaRule = "Haram jima' selama rentang 15 hari masa kemungkinan haid. Halal mulai hari ke-16."
                    ibaratQuote = "الذاكرة للقدر دون الوقت تحتاط في جميع مدة الإمكان لأن كل جزء يحتمل الحيض والطهر"
                    kitabSource = "Uyunul Masa-il Linnisa' hal. 82-84"
                }
                else -> {
                    categoryTitle = "Istihadhah Mutahayyirah"
                    categoryBadge = "Mutahayyirah"
                    confusionStatus = "Berlaku kaidah ihtiyath mazhab Syafi'i."
                    shalatRule = "Wajib shalat lima waktu."
                    mandiRule = "Wajib mandi di akhir masa kemungkinan."
                    puasaRule = "Wajib puasa dan qadha."
                    jimaRule = "Berhati-hati dalam hubungan suami istri."
                    ibaratQuote = "المتحيرة تحتاط في جميع الأحكام"
                    kitabSource = "Uyunul Masa-il Linnisa'"
                }
            }
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // Status Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.primary
                ) {
                    Text(
                        text = categoryBadge,
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }

                Text(
                    text = categoryTitle,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )

                Text(
                    text = confusionStatus,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }

        // Rincian Konsekuensi Ibadah Praktis
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Ringkasan Hukum & Ibadah:",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary
                )

                IbadahRuleRow(
                    icon = Icons.Default.DoneAll,
                    label = "Hukum Shalat",
                    desc = shalatRule
                )
                IbadahRuleRow(
                    icon = Icons.Default.WaterDrop,
                    label = "Mandi & Bersuci",
                    desc = mandiRule
                )
                IbadahRuleRow(
                    icon = Icons.Default.WbSunny,
                    label = "Ibadah Puasa",
                    desc = puasaRule
                )
                IbadahRuleRow(
                    icon = Icons.Default.FavoriteBorder,
                    label = "Jima' & Larangan",
                    desc = jimaRule
                )
            }
        }
    }
}

@Composable
private fun IbadahRuleRow(
    icon: ImageVector,
    label: String,
    desc: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.Top
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .size(20.dp)
                .padding(top = 2.dp)
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = desc,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun GuideSelectableCard(
    title: String,
    subtitle: String,
    isSelected: Boolean,
    icon: ImageVector,
    onClick: () -> Unit
) {
    val borderColor by animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
        label = "borderColor"
    )
    val containerColor by animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f) else MaterialTheme.colorScheme.surface,
        label = "containerColor"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(if (isSelected) 2.dp else 1.dp, borderColor),
        colors = CardDefaults.cardColors(containerColor = containerColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            RadioButton(
                selected = isSelected,
                onClick = onClick
            )
        }
    }
}

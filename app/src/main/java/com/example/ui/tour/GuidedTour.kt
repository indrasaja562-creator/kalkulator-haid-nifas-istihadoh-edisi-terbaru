package com.example.ui.tour

import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.automirrored.outlined.*

data class GuidedTourStep(
    val id: String,
    val stepIndex: Int,
    val tabIndex: Int,
    val targetKey: String,
    val title: String,
    val description: String,
    val icon: ImageVector,
    val cornerRadiusDp: Float = 18.0f,
    val paddingDp: Float = 6.0f
)

val APP_GUIDED_TOUR_STEPS: List<GuidedTourStep> = listOf(
    GuidedTourStep("calc_type", 0, 0, "tour_calc_type", "Pilih Jenis Perhitungan", "Tentukan jenis kasus: Haid / Istihadhah atau Nifas pasca bersalin untuk mengaktifkan batas hukum fiqih yang tepat.", Icons.Outlined.Tune, 18.0f, 6.0f),
    GuidedTourStep("blood_chronology", 1, 0, "tour_blood_chronology", "Kronologi & Warna Darah", "Catat rentang waktu dan tingkatan warna darah (hitam, merah, cokelat, dsb.). Sistem otomatis menguji syarat Tamyiz kuat dan lemah.", Icons.Outlined.EditCalendar, 18.0f, 6.0f),
    GuidedTourStep("calc_button", 2, 0, "tour_calculate_button", "Hitung Kepastian Fiqih", "Tekan tombol ini untuk menganalisis darah sesuai 7 golongan istihadhah Mazhab Syafi'i, menentukan kewajiban mandi, shalat, dan qadha.", Icons.Outlined.Calculate, 20.0f, 6.0f),
    GuidedTourStep("calendar_tab", 3, 1, "tour_calendar_grid", "Kalender Siklus & Jurnal", "Visualisasi siklus bulanan dengan titik warna darah, perkiraan masa suci minimal (15 hari), dan catatan riwayat harian Anda.", Icons.Outlined.CalendarMonth, 18.0f, 6.0f),
    GuidedTourStep("guide_tab", 4, 3, "tour_guide_library", "Perpustakaan Panduan Fiqih", "Kumpulan kaidah fiqih mu'tamad: wudhu daimul hadats, mandi wajib, serta tata cara shalat dan puasa wanita istihadhah.", Icons.AutoMirrored.Outlined.MenuBook, 18.0f, 6.0f),
    GuidedTourStep("profile_tab", 5, 4, "tour_profile_data", "Profil & Riwayat Siklus", "Simpan kebiasaan (adat) haid dan suci, bandingkan tren siklus antar bulan, dan atur preferensi tema warna aplikasi.", Icons.Outlined.Person, 18.0f, 6.0f)
)

fun Modifier.tourTarget(key: String): Modifier = this

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
    if (!isActive || currentStepIndex !in steps.indices) return
    val step = steps[currentStepIndex]
    
    LaunchedEffect(step.tabIndex) {
        if (currentTab != step.tabIndex) {
            onNavigateToTab(step.tabIndex)
        }
    }
    
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onCloseTour,
        icon = {
            androidx.compose.material3.Icon(
                imageVector = step.icon,
                contentDescription = null,
                tint = com.example.ui.theme.EmeraldPrimary
            )
        },
        title = {
            androidx.compose.material3.Text(
                text = "${step.stepIndex + 1}/${steps.size}: ${step.title}",
                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
            )
        },
        text = {
            androidx.compose.material3.Text(
                text = step.description,
                style = androidx.compose.material3.MaterialTheme.typography.bodyMedium
            )
        },
        confirmButton = {
            androidx.compose.material3.Button(
                onClick = onNextStep,
                colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                    containerColor = com.example.ui.theme.EmeraldPrimary
                )
            ) {
                androidx.compose.material3.Text(
                    if (currentStepIndex == steps.size - 1) "Selesai" else "Lanjut"
                )
            }
        },
        dismissButton = {
            if (currentStepIndex > 0) {
                androidx.compose.material3.TextButton(onClick = onPreviousStep) {
                    androidx.compose.material3.Text("Kembali")
                }
            } else {
                androidx.compose.material3.TextButton(onClick = onCloseTour) {
                    androidx.compose.material3.Text("Tutup")
                }
            }
        }
    )
}

package com.example.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Calendar

/**
 * 30 tips fiqih harian seputar haid, nifas, dan istihadhah.
 * Diverifikasi dengan kitab "Risalatul Mahid" karya Ahmad Syadzirin Amin (2007).
 * Tips diganti otomatis setiap hari berdasarkan tanggal (siklus 30 hari).
 */
object DailyTips {

    val tips: List<Pair<String, String>> = listOf(
        "Minimal masa haid adalah 24 jam. Darah yang keluar kurang dari itu bukan haid, melainkan istihadhah." to "Risalatul Mahid",
        "Maksimal haid 15 hari. Jika darah melebihi itu, perlu dianalisis mana yang haid dan mana yang istihadhah berdasarkan kebiasaan atau sifat darah." to "Risalatul Mahid",
        "Minimal masa suci antara dua haid adalah 15 hari." to "Risalatul Mahid",
        "Maksimal nifas 60 hari. Jika darah melebihi itu, perlu dianalisis mana yang nifas dan mana yang istihadhah berdasarkan kebiasaan atau sifat darah." to "Risalatul Mahid",
        "Minimal nifas tidak ada batasnya \u2014 bisa cuma sekejap (lahzhah)." to "Risalatul Mahid",
        "Wanita yang istihadhah tetap wajib shalat dan puasa." to "Risalatul Mahid",
        "Sebelum shalat, wanita istihadhah wajib membersihkan darah, menyumpal bila perlu, lalu wudhu setelah masuk waktu shalat." to "Risalatul Mahid",
        "Darah haid umumnya berwarna hitam pekat, terasa panas, dan berbau menyengat." to "Risalatul Mahid",
        "Jika lupa tanggal mulai haid, gunakan kebiasaan (adat) bulan-bulan sebelumnya." to "Risalatul Mahid",
        "Wanita haid haram shalat, puasa, thawaf, menyentuh mushaf, dan berdiam di masjid." to "Risalatul Mahid",
        "Puasa yang ditinggalkan saat haid wajib diqadha, tapi shalat tidak." to "Risalatul Mahid",
        "Jika darah berhenti sebelum 24 jam lalu keluar lagi, digabungkan hingga mencapai 24 jam." to "Risalatul Mahid",
        "Masa suci di sela-sela haid (naqa') dihukumi suci \u2014 wajib shalat dan puasa. Tapi jika darah keluar lagi, puasa yang dikerjakan saat itu wajib diqadha." to "Risalatul Mahid",
        "Jika ragu apakah darah masih keluar, masukkan kapas ke kemaluan \u2014 jika masih ada darah berarti masih haid." to "Risalatul Mahid",
        "Jangan terburu-buru mandi wajib \u2014 pastikan darah benar-benar berhenti." to "Risalatul Mahid",
        "Jika ragu apakah masih haid atau sudah suci, masukkan kapas \u2014 jika bersih berarti suci." to "Risalatul Mahid",
        "Darah yang keluar saat hamil dihukumi haid bila mencapai 24 jam dan tidak lebih dari 15 hari. Jika tidak memenuhi syarat, maka istihadhah." to "Fathul Wahhab 1/27",
        "Darah yang keluar sebelum melahirkan (thalaq) bukan nifas." to "Risalatul Mahid",
        "Batas maksimal nifas 60 hari dihitung sejak lahirnya bayi. Tapi darah nifasnya dihitung sejak keluarnya darah \u2014 jeda di antaranya dihukumi suci, tetap wajib shalat." to "Risalatul Mahid",
        "Tenggang waktu antara kelahiran bayi kembar maksimal 6 bulan." to "Risalatul Mahid",
        "Wanita nifas hukumnya sama dengan wanita haid dalam hal larangan." to "Risalatul Mahid",
        "Istihadhah bukan penghalang hubungan suami istri." to "Risalatul Mahid",
        "Wudhu wanita istihadhah hanya berlaku untuk satu shalat fardhu." to "Risalatul Mahid",
        "Jika darah istihadhah terus mengalir saat shalat, shalatnya tetap sah." to "Risalatul Mahid",
        "Sebelum shalat fardhu, wanita istihadhah wajib mensucikan kemaluan, menyumbat, membalut, lalu wudhu setelah masuk waktu \u2014 dilakukan setiap akan shalat." to "Risalatul Mahid",
        "Siklus haid normal umumnya 6-7 hari, tapi tiap wanita bisa berbeda." to "Risalatul Mahid",
        "Perubahan warna darah (dari merah ke kuning/keruh) masih dihitung haid selama dalam masa 15 hari." to "Risalatul Mahid",
        "Darah yang keluar melebihi 15 hari, kelebihannya adalah istihadhah \u2014 wajib shalat dan puasa seperti biasa." to "Risalatul Mahid",
        "Wanita yang baru pertama haid (mubtadi'ah) dan darahnya lebih 15 hari: yang dihukumi haid hanya 1 hari 1 malam menurut satu pendapat." to "Risalatul Mahid",
        "Catat setiap tanggal mulai dan berhenti darah \u2014 ini kunci utama perhitungan fiqih haid." to "Risalatul Mahid",
    )

    /** Ambil tips hari ini berdasarkan tanggal (siklus 30 hari). */
    fun today(): Pair<String, String> {
        val dayOfYear = Calendar.getInstance().get(Calendar.DAY_OF_YEAR)
        return tips[dayOfYear % tips.size]
    }
}

/**
 * Kartu "Tips Fiqih Hari Ini" untuk halaman depan.
 * Panggil dari CalculatorScreen.kt di bawah tombol "Lanjutkan Perhitungan".
 */
@Composable
fun DailyTipCard(
    modifier: Modifier = Modifier
) {
    val (tipText, source) = remember { DailyTips.today() }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFFFF8E1)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Outlined.Lightbulb,
                    contentDescription = null,
                    tint = Color(0xFF2E7D32),
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Tips Fiqih Hari Ini",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF2E7D32)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = tipText,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF212121),
                lineHeight = 20.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Sumber: $source",
                fontSize = 11.sp,
                fontStyle = FontStyle.Italic,
                color = Color(0xFF9E9E9E)
            )
        }
    }
}

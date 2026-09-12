package com.example.fiqih

import com.example.model.*
import java.text.SimpleDateFormat
import java.util.*

data class ChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val sender: MessageSender,
    val text: String,
    val timestamp: Long = System.currentTimeMillis(),
    val structuredResult: CalculationResult? = null,
    val promptSuggestions: List<String> = emptyList()
)

enum class MessageSender {
    USER, ASSISTANT
}

data class FiqihCasePreset(
    val title: String,
    val description: String,
    val sampleInput: String,
    val caseType: CaseType,
    val startDurationHoursAgo: Long,
    val durationHours: Long,
    val adatDays: Int,
    val isIntermittent: Boolean,
    val bloodColor: BloodColor
)

object FiqihConsultationEngine {

    val samplePresets = listOf(
        FiqihCasePreset(
            title = "Haid Normal 7 Hari (Ghalib)",
            description = "Darah keluar lancar selama 7 hari penuh sesuai siklus kebiasaan bulan lalu.",
            sampleInput = "Saya mengalami haid mulai tanggal 1 jam 07:00 dan darah berhenti total tanggal 8 jam 07:00 (7 hari). Adat bulan lalu 7 hari. Tidak ada jeda mampet.",
            caseType = CaseType.HAID,
            startDurationHoursAgo = 24 * 8,
            durationHours = 24 * 7,
            adatDays = 7,
            isIntermittent = false,
            bloodColor = BloodColor.MERAH
        ),
        FiqihCasePreset(
            title = "Darah Terputus-putus 10 Hari (Qoul Sahbi)",
            description = "Keluar 3 hari, bersih 3 hari, keluar lagi 4 hari (masih dalam 15 hari).",
            sampleInput = "Darah saya keluar 3 hari, lalu mampet/bersih 3 hari, kemudian keluar lagi 4 hari. Total rentang dari awal 10 hari. Adat saya 7 hari.",
            caseType = CaseType.HAID,
            startDurationHoursAgo = 24 * 12,
            durationHours = 24 * 10,
            adatDays = 7,
            isIntermittent = true,
            bloodColor = BloodColor.MERAH
        ),
        FiqihCasePreset(
            title = "Pendarahan 20 Hari (Mubtadi'ah Mumayyizah)",
            description = "Pemula haid, keluar darah 20 hari: 6 hari hitam kental, 14 hari merah encer.",
            sampleInput = "Ini pertama kali saya haid, darah keluar 20 hari. 6 hari pertama darah hitam kental berbau, lalu 14 hari sisanya darah merah encer.",
            caseType = CaseType.HAID,
            startDurationHoursAgo = 24 * 21,
            durationHours = 24 * 20,
            adatDays = 0,
            isIntermittent = false,
            bloodColor = BloodColor.HITAM
        ),
        FiqihCasePreset(
            title = "Suci di Waktu Ashar (Kewajiban Qadha)",
            description = "Haid 6 hari dan darah baru berhenti saat azan Ashar pukul 16:00.",
            sampleInput = "Darah haid saya berhenti pukul 16:00 sore (masuk waktu Ashar). Shalat apa saja yang wajib saya tunaikan menurut mazhab Syafi'i?",
            caseType = CaseType.HAID,
            startDurationHoursAgo = 24 * 7,
            durationHours = 24 * 6,
            adatDays = 6,
            isIntermittent = false,
            bloodColor = BloodColor.MERAH
        ),
        FiqihCasePreset(
            title = "Nifas Pasca Melahirkan 42 Hari",
            description = "Darah nifas keluar selama 42 hari setelah persalinan normal.",
            sampleInput = "Saya baru melahirkan 42 hari lalu dan darah nifas baru saja bersih total hari ini. Bagaimana status ibadah dan mandinya?",
            caseType = CaseType.NIFAS,
            startDurationHoursAgo = 24 * 43,
            durationHours = 24 * 42,
            adatDays = 40,
            isIntermittent = false,
            bloodColor = BloodColor.MERAH
        ),
        FiqihCasePreset(
            title = "Darah Kurang 24 Jam (Darah Rusak/Fasad)",
            description = "Keluar bercak darah hanya selama 10 jam lalu bersih tuntas.",
            sampleInput = "Keluar flek darah merah jam 08:00 pagi dan berhenti jam 18:00 sore (10 jam), setelah itu bersih sama sekali.",
            caseType = CaseType.HAID,
            startDurationHoursAgo = 24 * 2,
            durationHours = 10,
            adatDays = 7,
            isIntermittent = false,
            bloodColor = BloodColor.MERAH
        )
    )

    fun processUserQuery(query: String): ChatMessage {
        val lower = query.lowercase(Locale.ROOT)

        // Check if query lacks critical details
        val lacksType = !lower.contains("haid") && !lower.contains("nifas") && !lower.contains("mens") && !lower.contains("melahirkan")
        val lacksDuration = !lower.contains("hari") && !lower.contains("jam") && !lower.contains("tanggal")

        if (lacksType && lacksDuration) {
            val replyText = """
                Assalamu’alaikum warahmatullahi wabarakatuh, Ukhti yang dimuliakan Allah. 🌸

                Sebagai asisten fiqih kewanitaan dan medis Anda, saya siap membantu menghitung dan menganalisis status darah Anda secara teliti berdasarkan pedoman Mazhab Syafi'i (*Uyunul Masa'il Linnisa'* Lirboyo & *Tuhfatun Niswah*).

                Agar kalkulasi hukum dan konsekuensi ibadah Anda akurat, mohon berikan rincian berikut:
                1. **Jenis kasus**: Apakah terkait **Haid** atau **Nifas** (pasca persalinan)?
                2. **Tanggal dan jam mulai** darah keluar.
                3. **Tanggal dan jam darah berhenti** (atau apakah masih terus keluar?).
                4. **Riwayat (adat) kebiasaan** haid bulan-bulan sebelumnya (berapa hari biasanya haid & berapa hari masa suci?).
                5. **Apakah ada jeda berhenti (terputus-putus)?** Jika ada, mohon rincikan tanggal/jam mampet dan keluar kembali.
                6. **Warna & sifat darah** (apakah hitam, merah, cokelat, kuning, atau keruh; apakah kental atau berbau?).

                *Anda juga dapat memilih tombol simulasi contoh kasus di bawah ini untuk melihat contoh perhitungan lengkap.*
            """.trimIndent()

            return ChatMessage(
                sender = MessageSender.ASSISTANT,
                text = replyText,
                promptSuggestions = listOf(
                    "Haid 7 hari normal",
                    "Darah keluar 10 hari terputus-putus",
                    "Darah keluar lebih dari 15 hari",
                    "Suci di waktu Ashar",
                    "Nifas 40 hari pasca melahirkan"
                )
            )
        }

        // Determine parameters from text
        val isNifas = lower.contains("nifas") || lower.contains("melahirkan") || lower.contains("bayi")
        val caseType = if (isNifas) CaseType.NIFAS else CaseType.HAID

        val now = System.currentTimeMillis()
        var durationHours = 24L * 7 // default 7 days
        var adatDays = 7
        var bloodColor = BloodColor.MERAH
        var isIntermittent = lower.contains("putus") || lower.contains("jeda") || lower.contains("mampet") || lower.contains("bersih")

        // Parse days if mentioned
        val dayRegex = "(\\d+)\\s*(hari|hr)".toRegex()
        val matchDays = dayRegex.findAll(lower).toList()
        if (matchDays.isNotEmpty()) {
            val d = matchDays.first().groupValues[1].toLongOrNull() ?: 7L
            durationHours = d * 24
        }

        // Parse hours if specified
        val hourRegex = "(\\d+)\\s*(jam)".toRegex()
        val matchHours = hourRegex.findAll(lower).toList()
        if (matchHours.isNotEmpty() && matchDays.isEmpty()) {
            val h = matchHours.first().groupValues[1].toLongOrNull() ?: 10L
            durationHours = h
        }

        // Check blood color
        if (lower.contains("hitam")) bloodColor = BloodColor.HITAM
        else if (lower.contains("cokelat") || lower.contains("coklat")) bloodColor = BloodColor.COKLAT
        else if (lower.contains("kuning")) bloodColor = BloodColor.KUNING
        else if (lower.contains("keruh")) bloodColor = BloodColor.KERUH

        // Check prayer at stop
        var prayerAtStop: PrayerName? = null
        if (lower.contains("ashar") || lower.contains("asar")) prayerAtStop = PrayerName.ASHAR
        else if (lower.contains("isya") || lower.contains("isya'")) prayerAtStop = PrayerName.ISYA
        else if (lower.contains("dzuhur") || lower.contains("zuhur")) prayerAtStop = PrayerName.DZUHUR
        else if (lower.contains("subuh") || lower.contains("shubuh")) prayerAtStop = PrayerName.SUBUH
        else if (lower.contains("maghrib") || lower.contains("magrib")) prayerAtStop = PrayerName.MAGHRIB

        val startMs = now - (durationHours * 3600_000L)
        val endMs = now

        val calcResult = FiqihCalculatorEngine.calculate(
            caseType = caseType,
            startEpochMillis = startMs,
            endEpochMillis = endMs,
            hasPreviousAdat = true,
            adatDurationDays = adatDays,
            adatCycleDays = 28,
            adatMemoryType = AdatMemoryType.INGAT_LENGKAP_QADRAN_WAQTAN,
            bloodColor = bloodColor,
            isThick = true,
            isOdorous = true,
            intervals = emptyList(),
            prayerAtStart = null,
            hadPrayedAtStart = true,
            prayerAtStop = prayerAtStop
        )

        val formattedResponse = buildStructuredResponseText(calcResult, isIntermittent)

        return ChatMessage(
            sender = MessageSender.ASSISTANT,
            text = formattedResponse,
            structuredResult = calcResult,
            promptSuggestions = listOf(
                "Bagaimana niat mandi wajibnya?",
                "Bagaimana tata cara wudhu mustahadhah?",
                "Apa saja yang diharamkan saat haid?",
                "Kapan shalat wajib diqadha saat suci?"
            )
        )
    }

    private fun buildStructuredResponseText(result: CalculationResult, isIntermittent: Boolean): String {
        val sb = StringBuilder()
        val dFormat = SimpleDateFormat("dd MMM yyyy HH:mm", Locale("id", "ID"))

        sb.append("Bismillāhirrahmānirrahīm.\n\n")
        sb.append("📋 **RINGKASAN STATUS**\n")
        sb.append("• Status: **${result.statusSummary}**\n")
        sb.append("• Kategori Fiqih: *${result.caseCategory}*\n\n")

        sb.append("⏱️ **RINCIAN KALKULASI & JANGKA WAKTU**\n")
        if (result.haidOrNifasSegment != null) {
            val seg = result.haidOrNifasSegment
            val startStr = dFormat.format(Date(seg.startEpochMillis))
            val endStr = dFormat.format(Date(seg.endEpochMillis))
            sb.append("• **Masa Haid/Nifas SAH**: $startStr s/d $endStr (Total: ${seg.durationDays} Hari ${seg.remainingHours} Jam)\n")
        }
        if (result.istihadhahSegment != null) {
            val seg = result.istihadhahSegment
            val startStr = dFormat.format(Date(seg.startEpochMillis))
            val endStr = dFormat.format(Date(seg.endEpochMillis))
            sb.append("• **Masa Istihadhah**: $startStr s/d $endStr (Total: ${seg.durationDays} Hari ${seg.remainingHours} Jam)\n")
        }
        if (isIntermittent) {
            sb.append("• *Catatan Darah Terputus-putus*: Berdasarkan **Qoul Sahbi** (pendapat mu'tamad), masa mampet di sela-sela darah dalam lingkup 15 hari dihukumi satu kesatuan haid.\n")
        }
        sb.append("\n")

        sb.append("🕌 **KONSEKUENSI IBADAH**\n")
        sb.append("• **Status Shalat**: ${result.shalatConsequence}\n")
        if (result.qadhaPrayers.isNotEmpty()) {
            sb.append("• **Daftar Shalat Terkait**:\n")
            result.qadhaPrayers.forEach { p ->
                sb.append("   - $p\n")
            }
        }
        sb.append("• **Status Puasa**: ${result.puasaConsequence}\n")
        sb.append("• **Catatan Mandi Wajib**: ${result.mandiWajibNote}\n\n")

        if (result.mustahadhahCareGuide != null) {
            sb.append("💧 **PANDUAN THOHAROH MUSTAHADHAH**\n")
            sb.append("${result.mustahadhahCareGuide}\n\n")
        }

        sb.append("🩺 **SARAN KEAGAMAAN & MEDIS**\n")
        sb.append("${result.medicalAndSpiritualAdvice}\n\n")

        sb.append("📚 **REFERENSI KITAB OTORITATIF MAZHAB SYAFI'I**\n")
        result.references.forEachIndexed { i, ref ->
            sb.append("${i + 1}. *${ref.kitabName}* (${ref.volumeAndPage})\n")
            sb.append("   \"${ref.ibaratSnippet}\"\n")
            sb.append("   Artinya: ${ref.explanation}\n")
        }
        sb.append("\n")

        sb.append("⚠️ *Peringatan: Perhitungan ini berbasis kaidah fikih standar Mazhab Syafi'i. Jika terdapat kondisi fisik khusus atau keraguan mendalam, sangat dianjurkan untuk berkonsultasi langsung dengan ustazah, ahli fiqih setempat, dan dokter spesialis.*")

        return sb.toString()
    }
}

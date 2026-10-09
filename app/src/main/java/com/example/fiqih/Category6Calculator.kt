package com.example.fiqih

import com.example.model.Category6CalculationResult
import com.example.model.Category6PeriodResult
import com.example.model.FiqhPeriodStatus
import com.example.model.IhtiyatAction

/**
 * Implementasi Fiqih Mazhab Syafi'i untuk Mustahadhah Kategori 6:
 * Mu'tadah Ghairu Mumayyizah Dzakirah li 'Adatiha Qadran La Waqtan
 * (المعتادة غير المميزة الذاكرة لعادتها قدراً لا وقتاً)
 *
 * Menggunakan pendekatan Matematis CANDIDATE INTERVALS:
 * 1. Bentuk seluruh kemungkinan posisi haid berdasarkan qadr (durasi) + rentang posisi (window).
 * 2. Eliminasi kandidat yang bertentangan dengan hari yang diyakini suci (certainPureDays).
 * 3. Eliminasi kandidat yang bertentangan dengan hari yang diyakini haid (certainHaidDays).
 * 4. Tentukan status setiap hari:
 *    - HAID_YAKIN: hari berada di SEMUA candidate intervals (intersection).
 *    - SUCI_YAKIN: hari TIDAK berada di kandidat mana pun (di luar union).
 *    - UNCERTAIN:
 *      * SYAK_HAID_Suci: hari berada di sebagian kandidat, dan TIDAK ADA kemungkinan inqitha' (belum ada kandidat yang selesai sebelum hari ini).
 *      * SYAK_HAID_Suci_PUTUS: hari berada di sebagian kandidat, dan ADA kemungkinan inqitha' (ada kandidat yang selesai sebelum hari ini).
 * 5. Menentukan tindakan ibadah (IhtiyatAction):
 *    - HAID_YAKIN: NONE (haram shalat/puasa).
 *    - SUCI_YAKIN: NONE (wajib shalat/puasa normal).
 *    - SYAK_HAID_Suci: WUDHU_SETIAP_FARDHU (wajib wudhu setiap fardhu, tanpa mandi).
 *    - SYAK_HAID_Suci_PUTUS: GHUSL_SETIAP_FARDHU (wajib mandi fardhu setiap fardhu + wudhu).
 */
object Category6Calculator {

    fun calculateCategory6(
        monthLength: Int = 30,
        habitDurationDays: Int,
        positionWindowStart: Int,
        positionWindowEnd: Int,
        certainPureDays: Set<Int> = emptySet(),
        certainHaidDays: Set<Int> = emptySet()
    ): Category6CalculationResult {
        // 1. Validasi Input
        if (monthLength < 1 || monthLength > 31) {
            return Category6CalculationResult(
                isValid = false,
                errorMessage = "Jumlah hari dalam bulan ($monthLength hari) tidak valid. Harus antara 1 sampai 31 hari.",
                monthLength = monthLength,
                habitDurationDays = habitDurationDays,
                positionWindowStart = positionWindowStart,
                positionWindowEnd = positionWindowEnd
            )
        }
        if (habitDurationDays < 1 || habitDurationDays > 15) {
            return Category6CalculationResult(
                isValid = false,
                errorMessage = "Durasi kebiasaan haid ($habitDurationDays hari) tidak valid. Sesuai fiqih Mazhab Syafi'i (Tuhfatun Niswah & Uyunul Masa'il), batas maksimal haid adalah 15 hari.",
                monthLength = monthLength,
                habitDurationDays = habitDurationDays,
                positionWindowStart = positionWindowStart,
                positionWindowEnd = positionWindowEnd
            )
        }
        if (positionWindowStart < 1) {
            return Category6CalculationResult(
                isValid = false,
                errorMessage = "Awal rentang posisi yang diketahui ($positionWindowStart) minimal hari ke-1.",
                monthLength = monthLength,
                habitDurationDays = habitDurationDays,
                positionWindowStart = positionWindowStart,
                positionWindowEnd = positionWindowEnd
            )
        }
        if (positionWindowEnd > monthLength) {
            return Category6CalculationResult(
                isValid = false,
                errorMessage = "Akhir rentang posisi yang diketahui ($positionWindowEnd) tidak boleh melebihi jumlah hari dalam bulan ($monthLength hari).",
                monthLength = monthLength,
                habitDurationDays = habitDurationDays,
                positionWindowStart = positionWindowStart,
                positionWindowEnd = positionWindowEnd
            )
        }
        if (positionWindowStart > positionWindowEnd) {
            return Category6CalculationResult(
                isValid = false,
                errorMessage = "Awal rentang posisi ($positionWindowStart) tidak boleh lebih besar dari akhir rentang posisi ($positionWindowEnd).",
                monthLength = monthLength,
                habitDurationDays = habitDurationDays,
                positionWindowStart = positionWindowStart,
                positionWindowEnd = positionWindowEnd
            )
        }
        val windowLength = positionWindowEnd - positionWindowStart + 1
        if (habitDurationDays > windowLength) {
            return Category6CalculationResult(
                isValid = false,
                errorMessage = "Durasi kebiasaan haid ($habitDurationDays hari) tidak boleh melebihi panjang rentang posisi yang diketahui ($windowLength hari: hari $positionWindowStart–$positionWindowEnd).",
                monthLength = monthLength,
                habitDurationDays = habitDurationDays,
                positionWindowStart = positionWindowStart,
                positionWindowEnd = positionWindowEnd
            )
        }

        // 2. Generate Candidate Intervals
        val allCandidates = mutableListOf<IntRange>()
        val maxStart = positionWindowEnd - habitDurationDays + 1
        for (start in positionWindowStart..maxStart) {
            val end = start + habitDurationDays - 1
            allCandidates.add(start..end)
        }

        // 3. Filter kandidat berdasarkan certainPureDays (buang jika memuat hari yang yakin suci)
        val eliminatedPure = mutableListOf<IntRange>()
        val validAfterPure = mutableListOf<IntRange>()
        for (candidate in allCandidates) {
            val overlapsPure = certainPureDays.any { it in candidate }
            if (overlapsPure) {
                eliminatedPure.add(candidate)
            } else {
                validAfterPure.add(candidate)
            }
        }

        // 4. Filter kandidat berdasarkan certainHaidDays (buang jika TIDAK memuat hari yang yakin haid)
        val eliminatedHaid = mutableListOf<IntRange>()
        val finalValidCandidates = mutableListOf<IntRange>()
        for (candidate in validAfterPure) {
            val includesAllHaid = certainHaidDays.all { it in candidate }
            if (!includesAllHaid) {
                eliminatedHaid.add(candidate)
            } else {
                finalValidCandidates.add(candidate)
            }
        }

        // Jika tidak ada kandidat yang tersisa (terjadi kontradiksi input)
        if (finalValidCandidates.isEmpty()) {
            return Category6CalculationResult(
                isValid = false,
                errorMessage = "Kontradiksi input: seluruh kemungkinan posisi haid tereliminasi oleh hari yang diyakini suci ($certainPureDays) atau yakin haid ($certainHaidDays). Mohon periksa kembali input hari.",
                monthLength = monthLength,
                habitDurationDays = habitDurationDays,
                positionWindowStart = positionWindowStart,
                positionWindowEnd = positionWindowEnd,
                certainPureDays = certainPureDays,
                certainHaidDays = certainHaidDays,
                generatedCandidatesCount = allCandidates.size,
                validCandidates = emptyList(),
                eliminatedPureCandidates = eliminatedPure,
                eliminatedHaidCandidates = eliminatedHaid
            )
        }

        // 5. Hitung Status Hari demi Hari (1 s/d monthLength)
        val dayStatuses = mutableMapOf<Int, Pair<FiqhPeriodStatus, IhtiyatAction>>()

        for (d in 1..monthLength) {
            val matchingCandidates = finalValidCandidates.filter { d in it }
            val nonMatchingCandidates = finalValidCandidates.filter { d !in it }

            when {
                // A. Hari berada di SEMUA candidate intervals -> HAID_YAKIN
                matchingCandidates.size == finalValidCandidates.size -> {
                    dayStatuses[d] = FiqhPeriodStatus.HAID_YAKIN to IhtiyatAction.NONE
                }
                // B. Hari TIDAK berada di candidate intervals mana pun -> SUCI_YAKIN
                matchingCandidates.isEmpty() -> {
                    dayStatuses[d] = FiqhPeriodStatus.SUCI_YAKIN to IhtiyatAction.NONE
                }
                // C & D & E. Hari berada di SEBAGIAN kandidat -> UNCERTAIN
                else -> {
                    // Cek apakah ada kemungkinan inqitha' (berhentinya haid)
                    // Terjadi jika ada kandidat di mana haid telah berakhir sebelum hari d (d > candidate.last)
                    val hasCessationPossibility = nonMatchingCandidates.any { d > it.last }

                    if (hasCessationPossibility) {
                        dayStatuses[d] = FiqhPeriodStatus.SYAK_HAID_Suci_PUTUS to IhtiyatAction.GHUSL_SETIAP_FARDHU
                    } else {
                        dayStatuses[d] = FiqhPeriodStatus.SYAK_HAID_Suci to IhtiyatAction.WUDHU_SETIAP_FARDHU
                    }
                }
            }
        }

        // 6. Kelompokkan hari-hari berturutan yang sama status & tindakannya
        val periodResults = mutableListOf<Category6PeriodResult>()
        var rangeStart = 1
        var currentStatus = dayStatuses[1]!!.first
        var currentAction = dayStatuses[1]!!.second

        for (d in 2..monthLength) {
            val (status, action) = dayStatuses[d]!!
            if (status != currentStatus || action != currentAction) {
                val expl = buildPeriodExplanation(rangeStart, d - 1, currentStatus, currentAction)
                periodResults.add(Category6PeriodResult(rangeStart, d - 1, currentStatus, currentAction, expl))
                rangeStart = d
                currentStatus = status
                currentAction = action
            }
        }
        val lastExpl = buildPeriodExplanation(rangeStart, monthLength, currentStatus, currentAction)
        periodResults.add(Category6PeriodResult(rangeStart, monthLength, currentStatus, currentAction, lastExpl))

        val summary = buildSummary(
            habitDurationDays = habitDurationDays,
            positionWindowStart = positionWindowStart,
            positionWindowEnd = positionWindowEnd,
            validCandidatesCount = finalValidCandidates.size,
            validCandidates = finalValidCandidates,
            certainPureDays = certainPureDays,
            certainHaidDays = certainHaidDays,
            periodResults = periodResults
        )

        return Category6CalculationResult(
            isValid = true,
            errorMessage = null,
            monthLength = monthLength,
            habitDurationDays = habitDurationDays,
            positionWindowStart = positionWindowStart,
            positionWindowEnd = positionWindowEnd,
            certainPureDays = certainPureDays,
            certainHaidDays = certainHaidDays,
            generatedCandidatesCount = allCandidates.size,
            validCandidates = finalValidCandidates,
            eliminatedPureCandidates = eliminatedPure,
            eliminatedHaidCandidates = eliminatedHaid,
            periodResults = periodResults,
            summaryExplanation = summary
        )
    }

    private fun buildPeriodExplanation(
        startDay: Int,
        endDay: Int,
        status: FiqhPeriodStatus,
        action: IhtiyatAction
    ): String {
        val daysLabel = if (startDay == endDay) "Hari ke-$startDay" else "Hari ke-$startDay s/d ke-$endDay"
        return when (status) {
            FiqhPeriodStatus.HAID_YAKIN ->
                "$daysLabel: Berstatus HAID YAKIN (حيض بيقين) karena terliput oleh seluruh kemungkinan posisi haid. Tindakan: tidak boleh shalat dan puasa, diharamkan jima'."
            FiqhPeriodStatus.SUCI_YAKIN ->
                "$daysLabel: Berstatus SUCI YAKIN (طهر بيقين) karena berada di luar seluruh kemungkinan posisi haid. Tindakan: wajib shalat dan puasa seperti biasa."
            FiqhPeriodStatus.SYAK_HAID_Suci ->
                "$daysLabel: Berstatus SYAK HAID & SUCI TANPA KEMUNGKINAN INQITHA' (محتمل للحيض والطهر دون الانقطاع). Tindakan ihtiyath: berwudhu setiap kali masuk waktu shalat fardhu (tanpa wajib mandi), wajib shalat dan puasa."
            FiqhPeriodStatus.SYAK_HAID_Suci_PUTUS ->
                "$daysLabel: Berstatus SYAK HAID, SUCI & KEMUNGKINAN INQITHA' (محتمل للحيض والطهر والانقطاع). Tindakan ihtiyath: wajib mandi besar (ghusl) setiap kali masuk waktu shalat fardhu karena ada potensi saat itu darah baru berhenti, lalu berwudhu, wajib shalat dan puasa."
        }
    }

    private fun buildSummary(
        habitDurationDays: Int,
        positionWindowStart: Int,
        positionWindowEnd: Int,
        validCandidatesCount: Int,
        validCandidates: List<IntRange>,
        certainPureDays: Set<Int>,
        certainHaidDays: Set<Int>,
        periodResults: List<Category6PeriodResult>
    ): String {
        val sb = StringBuilder()
        sb.append("Analisis Fiqih Mazhab Syafi'i (Tuhfatun Niswah / Uyunul Masa'il):\n")
        sb.append("Kategori 6: Mu'tadah Ghairu Mumayyizah Dzakirah li 'Adatiha Qadran La Waqtan.\n")
        sb.append("Durasi haid adat: $habitDurationDays hari, berada dalam rentang hari ke-$positionWindowStart s/d ke-$positionWindowEnd.\n")
        if (certainPureDays.isNotEmpty()) {
            sb.append("Hari yang diyakini suci: ${certainPureDays.sorted().joinToString(", ", prefix = "Hari ke-")}.\n")
        }
        if (certainHaidDays.isNotEmpty()) {
            sb.append("Hari yang diyakini haid: ${certainHaidDays.sorted().joinToString(", ", prefix = "Hari ke-")}.\n")
        }
        sb.append("Kemungkinan posisi haid yang valid ($validCandidatesCount kandidat): ${validCandidates.joinToString(", ") { "${it.first}..${it.last}" }}.\n\n")
        sb.append("Hasil Keputusan Fiqih per Fase:\n")
        for (p in periodResults) {
            val label = if (p.startDay == p.endDay) "• Hari ke-${p.startDay}" else "• Hari ke-${p.startDay}–${p.endDay}"
            val actionText = when (p.ihtiyatAction) {
                IhtiyatAction.NONE -> if (p.status == FiqhPeriodStatus.HAID_YAKIN) "Meninggalkan shalat/puasa (Hukum Haid)" else "Ibadah normal"
                IhtiyatAction.WUDHU_SETIAP_FARDHU -> "Ihtiyath: Wudhu setiap fardhu"
                IhtiyatAction.GHUSL_SETIAP_FARDHU -> "Ihtiyath: Mandi (ghusl) setiap fardhu + wudhu"
            }
            sb.append("$label: ${p.status.label} [${p.status.arabicText}] → $actionText\n")
        }
        return sb.toString().trim()
    }
}

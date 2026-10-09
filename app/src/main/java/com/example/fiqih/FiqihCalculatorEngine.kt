package com.example.fiqih

import com.example.model.*
import java.text.SimpleDateFormat
import java.util.*

object FiqihCalculatorEngine {

    private const val ONE_HOUR_MS = 3600_000L
    private const val ONE_DAY_MS = 24 * ONE_HOUR_MS
    private const val FIFTEEN_DAYS_MS = 15 * ONE_DAY_MS
    private const val SIXTY_DAYS_MS = 60 * ONE_DAY_MS

    private val dateTimeFormat = SimpleDateFormat("dd MMM yyyy 'pukul' HH:mm", Locale("id", "ID"))
    private val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale("id", "ID"))

    private data class BleedingBlock(
        val startEpochMillis: Long,
        val endEpochMillis: Long,
        val intervals: List<BleedingInterval>
    ) {
        val durationMillis: Long get() = (endEpochMillis - startEpochMillis).coerceAtLeast(0)
    }

    fun formatDisplayRange(startMs: Long, endMs: Long): String {
        val calStart = Calendar.getInstance().apply { timeInMillis = startMs }
        val calEnd = Calendar.getInstance().apply { timeInMillis = endMs }
        val isMidnightStart = calStart.get(Calendar.HOUR_OF_DAY) == 0 && calStart.get(Calendar.MINUTE) == 0
        val isMidnightEnd = calEnd.get(Calendar.HOUR_OF_DAY) == 0 && calEnd.get(Calendar.MINUTE) == 0
        val isEndOfDayEnd = calEnd.get(Calendar.HOUR_OF_DAY) == 23 && calEnd.get(Calendar.MINUTE) >= 59

        val effectiveEndMs = if (isMidnightEnd && endMs > startMs) endMs - 1000L else endMs

        return if (isMidnightStart && (isMidnightEnd || isEndOfDayEnd)) {
            val startStr = dateFormat.format(Date(startMs))
            val endStr = dateFormat.format(Date(effectiveEndMs))
            if (startStr == endStr) startStr else "$startStr – $endStr"
        } else {
            "${dateTimeFormat.format(Date(startMs))} s/d ${dateTimeFormat.format(Date(endMs))}"
        }
    }

    fun formatRange(startMs: Long, endMs: Long): String = formatDisplayRange(startMs, endMs)

    fun calculate(
        caseType: CaseType,
        startEpochMillis: Long,
        endEpochMillis: Long,
        hasPreviousAdat: Boolean,
        adatDurationDays: Int,
        adatCycleDays: Int,
        adatMemoryType: AdatMemoryType,
        haidCategory: HaidCategory? = null,
        deliveryEpochMillis: Long? = null,
        deliveryType: DeliveryType = DeliveryType.TUNGGAL_NORMAL_SESAR,
        twinLastDeliveryEpochMillis: Long? = null,
        nifasAdatCategory: NifasAdatCategory? = null,
        hasIntermittentPause: Boolean = false,
        intermittentPauseDays: Double = 0.0,
        bloodColor: BloodColor,
        isThick: Boolean,
        isOdorous: Boolean,
        intervals: List<BleedingInterval>,
        prayerAtStart: PrayerName?,
        hadPrayedAtStart: Boolean,
        prayerAtStop: PrayerName?,
        hasPreviousHaidBeforeNifas: Boolean = false,
        previousHaidAdatDays: Int = 7,
        isTakmilahEnabled: Boolean = false,
        previousSuciDaysForTakmilah: Int = 15,
        previousHaidDurationDaysForTakmilah: Int? = null,
        previousHaidContinuousForTakmilah: Boolean = false,
        rememberedCertainPureDayOfMonth: Int? = null,
        rememberedCertainHaidDayOfMonth: Int? = null,
        category6WindowStart: Int = 1,
        category6WindowEnd: Int? = null,
        category6PureDays: Set<Int>? = null,
        category6HaidDays: Set<Int>? = null,
        category6MonthLength: Int = 30
    ): CalculationResult {
        // Resolve active intervals. If intervals is empty, construct a single phase from start/end and blood attributes
        val activeIntervals: List<BleedingInterval> = if (intervals.isNotEmpty()) {
            intervals.sortedBy { it.startEpochMillis }
        } else {
            listOf(
                BleedingInterval(
                    startEpochMillis = startEpochMillis,
                    endEpochMillis = endEpochMillis,
                    bloodColor = bloodColor,
                    isThick = isThick,
                    isOdorous = isOdorous
                )
            )
        }

        val effectiveStart = activeIntervals.minOf { it.startEpochMillis }
        val effectiveEnd = activeIntervals.maxOf { it.endEpochMillis }
        val totalDurationMs = (effectiveEnd - effectiveStart).coerceAtLeast(0)
        val totalHours = totalDurationMs / ONE_HOUR_MS
        val totalDays = totalHours / 24
        val remainingHours = totalHours % 24

        val qadhaList = mutableListOf<String>()

        // Check shalat at start of bleeding
        if (prayerAtStart != null && !hadPrayedAtStart) {
            qadhaList.add("Shalat ${prayerAtStart.label} saat darah mulai keluar (wajib diqadha setelah suci/mandi)")
        }

        // Check shalat at stop of bleeding
        if (prayerAtStop != null) {
            when (prayerAtStop) {
                PrayerName.ASHAR -> {
                    qadhaList.add("Shalat Ashar (wajib ditunaikan secara adā' / langsung setelah mandi wajib)")
                    qadhaList.add("Shalat Dzuhur (wajib diqadha karena suci di waktu Ashar, berdasarkan kaidah jamak takhir fiqih Syafi'i)")
                }
                PrayerName.ISYA -> {
                    qadhaList.add("Shalat Isya (wajib ditunaikan secara adā' / langsung setelah mandi wajib)")
                    qadhaList.add("Shalat Maghrib (wajib diqadha karena suci di waktu Isya, berdasarkan kaidah jamak takhir fiqih Syafi'i)")
                }
                PrayerName.SUBUH -> {
                    qadhaList.add("Shalat Subuh (wajib ditunaikan secara adā' / langsung setelah mandi wajib)")
                }
                PrayerName.DZUHUR -> {
                    qadhaList.add("Shalat Dzuhur (wajib ditunaikan secara adā' / langsung setelah mandi wajib)")
                }
                PrayerName.MAGHRIB -> {
                    qadhaList.add("Shalat Maghrib (wajib ditunaikan secara adā' / langsung setelah mandi wajib)")
                }
            }
        }

        // Generate phase breakdown descriptions
        val phaseBreakdowns = activeIntervals.mapIndexed { idx, p ->
            val startStr = dateTimeFormat.format(Date(p.startEpochMillis))
            val endStr = dateTimeFormat.format(Date(p.endEpochMillis))
            val strengthLabel = if (p.isStrongBlood) "Darah Kuat (Qawi)" else "Darah Lemah (Dha'if)"
            "Fase ${idx + 1}: Darah ${p.bloodColor.shortName} | $startStr s/d $endStr (${p.formattedDuration}) -> $strengthLabel"
        }

        return if (caseType == CaseType.HAID) {
            calculateHaidWithPhases(
                startEpochMillis = effectiveStart,
                endEpochMillis = effectiveEnd,
                totalDurationMs = totalDurationMs,
                totalDays = totalDays,
                remainingHours = remainingHours,
                intervals = activeIntervals,
                hasPreviousAdat = hasPreviousAdat,
                adatDurationDays = adatDurationDays,
                adatCycleDays = adatCycleDays,
                adatMemoryType = adatMemoryType,
                haidCategory = haidCategory,
                hasIntermittentPause = hasIntermittentPause,
                intermittentPauseDays = intermittentPauseDays,
                qadhaList = qadhaList,
                phaseBreakdowns = phaseBreakdowns,
                isTakmilahEnabled = isTakmilahEnabled,
                previousSuciDaysForTakmilah = previousSuciDaysForTakmilah,
                previousHaidDurationDaysForTakmilah = previousHaidDurationDaysForTakmilah,
                previousHaidContinuousForTakmilah = previousHaidContinuousForTakmilah,
                rememberedCertainPureDayOfMonth = rememberedCertainPureDayOfMonth,
                rememberedCertainHaidDayOfMonth = rememberedCertainHaidDayOfMonth,
                category6WindowStart = category6WindowStart,
                category6WindowEnd = category6WindowEnd,
                category6PureDays = category6PureDays,
                category6HaidDays = category6HaidDays,
                category6MonthLength = category6MonthLength
            )
        } else {
            calculateNifasWithPhases(
                startEpochMillis = effectiveStart,
                endEpochMillis = effectiveEnd,
                totalDurationMs = totalDurationMs,
                totalDays = totalDays,
                remainingHours = remainingHours,
                intervals = activeIntervals,
                hasPreviousAdat = hasPreviousAdat,
                adatDurationDays = adatDurationDays,
                adatMemoryType = adatMemoryType,
                deliveryEpochMillis = deliveryEpochMillis,
                deliveryType = deliveryType,
                twinLastDeliveryEpochMillis = twinLastDeliveryEpochMillis,
                nifasAdatCategory = nifasAdatCategory,
                hasIntermittentPause = hasIntermittentPause,
                intermittentPauseDays = intermittentPauseDays,
                qadhaList = qadhaList,
                phaseBreakdowns = phaseBreakdowns,
                hasPreviousHaidBeforeNifas = hasPreviousHaidBeforeNifas,
                previousHaidAdatDays = previousHaidAdatDays,
                previousSuciAdatDays = (adatCycleDays - previousHaidAdatDays).takeIf { it >= 15 }
            )
        }
    }

    private data class HaidPhaseRange(
        val startEpochMillis: Long,
        val endEpochMillis: Long
    )

    private data class StrongGroup(val start: Long, val end: Long, val duration: Long)

    private data class TamyizPhasesEvaluation(
        val isValid: Boolean,
        val firstStrongStart: Long,
        val firstStrongEnd: Long,
        val firstStrongDuration: Long,
        val haidRanges: List<HaidPhaseRange>,
        val hasSubsequentStrong: Boolean,
        val firstWeakGapDuration: Long,
        val reason: String
    ) {
        fun isHaidPhase(phase: BleedingInterval): Boolean {
            return haidRanges.any { phase.startEpochMillis >= it.startEpochMillis && phase.endEpochMillis <= it.endEpochMillis }
        }
    }

    /**
     * Fungsi bersama untuk menentukan fase darah kuat mana yang sah menjadi haid
     * sesuai kaidah Tamyiz dalam Tuhfatun Niswah (hal. 29-31).
     *
     * Kaidah:
     * - Fase darah kuat pertama sah sebagai haid bila berdurasi minimal 24 jam dan maksimal 15 hari.
     * - Fase darah kuat berikutnya HANYA sah menjadi haid bila jarak suci pemisah
     *   dari AKHIR HAID SAH TERAKHIR mencapai sekurang-kurangnya 15 hari (360 jam)
     *   dan durasi fase kuat tersebut tidak melebihi 15 hari.
     */
    private fun evaluateTamyizStrongPhases(intervals: List<BleedingInterval>): TamyizPhasesEvaluation {
        val ordered = intervals.sortedBy { it.startEpochMillis }
        if (ordered.isEmpty()) {
            return TamyizPhasesEvaluation(false, 0L, 0L, 0L, emptyList(), false, 0L, "Tidak ada data fase darah.")
        }

        val distinctScores = ordered.map { it.strengthScore }.distinct().sortedDescending()
        val kuatScore = distinctScores[0]
        val hasTingkat2 = distinctScores.size >= 2
        val lemahScore = if (hasTingkat2) distinctScores[1] else -1

        val firstKuatIndex = ordered.indexOfFirst { it.strengthScore == kuatScore }
        if (firstKuatIndex == -1) {
            return TamyizPhasesEvaluation(false, 0L, 0L, 0L, emptyList(), false, 0L, "Tidak ada darah kuat.")
        }

        var kuatEndIndex = firstKuatIndex
        while (kuatEndIndex + 1 < ordered.size &&
            ordered[kuatEndIndex + 1].strengthScore == kuatScore &&
            ordered[kuatEndIndex + 1].startEpochMillis == ordered[kuatEndIndex].endEpochMillis
        ) {
            kuatEndIndex++
        }
        val firstKuatStart = ordered[firstKuatIndex].startEpochMillis
        val firstKuatEnd = ordered[kuatEndIndex].endEpochMillis
        val firstKuatDuration = firstKuatEnd - firstKuatStart

        val condKuatMin = firstKuatDuration >= ONE_DAY_MS
        val condKuatMax = firstKuatDuration <= FIFTEEN_DAYS_MS
        if (!condKuatMin || !condKuatMax) {
            val reason = if (!condKuatMin) {
                "Tamyiz tidak sah: total darah kuat pertama kurang dari 24 jam."
            } else {
                "Tamyiz tidak sah: total darah kuat pertama melebihi 15 hari."
            }
            return TamyizPhasesEvaluation(false, firstKuatStart, firstKuatEnd, firstKuatDuration, emptyList(), false, 0L, reason)
        }

        var haidPrimaryEnd = firstKuatEnd
        var haidPrimaryDuration = firstKuatDuration
        var combinedReason = ""

        // Kaidah Tiga Tingkat (Nihayat al-Muhtaj 1/341-342 & Hasyiyah al-Jamal 1/248):
        // Jika setelah kelompok darah kuat pertama langsung bersambung rangkaian darah tingkat 2,
        // dan total durasi kuat pertama + rangkaian tingkat 2 sesudahnya <= 15 hari,
        // maka rangkaian tingkat 2 tersebut ikut dihukumi haid.
        // Darah tingkat 2 yang keluar SEBELUM darah kuat tetap istihadhah (karena haid hanya mulai dari firstKuatStart),
        // dan sama sekali TIDAK menghalangi penggabungan tingkat 2 yang bersambung langsung sesudah darah kuat.
        if (hasTingkat2 &&
            kuatEndIndex + 1 < ordered.size &&
            ordered[kuatEndIndex + 1].startEpochMillis == firstKuatEnd &&
            ordered[kuatEndIndex + 1].strengthScore == lemahScore
        ) {
            var lemahEndIndex = kuatEndIndex + 1
            while (lemahEndIndex + 1 < ordered.size &&
                ordered[lemahEndIndex + 1].strengthScore == lemahScore &&
                ordered[lemahEndIndex + 1].startEpochMillis == ordered[lemahEndIndex].endEpochMillis
            ) {
                lemahEndIndex++
            }
            val contiguousLemahEnd = ordered[lemahEndIndex].endEpochMillis
            val contiguousLemahDuration = contiguousLemahEnd - firstKuatEnd
            val combinedDuration = firstKuatDuration + contiguousLemahDuration

            if (combinedDuration <= FIFTEEN_DAYS_MS) {
                haidPrimaryEnd = contiguousLemahEnd
                haidPrimaryDuration = combinedDuration
                combinedReason = "darah kuat ${formatDuration(firstKuatDuration)} dan darah lemah ${formatDuration(contiguousLemahDuration)} dihukumi haid bersama (total ${formatDuration(combinedDuration)}) karena lemah keluar setelah kuat, bersambung langsung, dan total kuat+lemah <= 15 hari (Uyunul Masa'il hal. 75, Nihayat al-Muhtaj 1/341-342 & Hasyiyah al-Jamal 1/248)."
            } else {
                combinedReason = "haid hanya darah kuat ${formatDuration(firstKuatDuration)} karena total darah kuat dan lemah (${formatDuration(combinedDuration)}) melebihi 15 hari."
            }
        } else {
            combinedReason = "haid hanya darah kuat ${formatDuration(firstKuatDuration)}."
        }

        val subsequentGroups = mutableListOf<StrongGroup>()
        var curStart = -1L
        var curEnd = -1L
        var curDur = 0L

        for (phase in ordered) {
            if (phase.startEpochMillis >= haidPrimaryEnd && phase.strengthScore == kuatScore) {
                if (curStart == -1L) {
                    curStart = phase.startEpochMillis
                    curEnd = phase.endEpochMillis
                    curDur = phase.durationMillis
                } else if (phase.startEpochMillis == curEnd) {
                    curEnd = phase.endEpochMillis
                    curDur += phase.durationMillis
                } else {
                    subsequentGroups.add(StrongGroup(curStart, curEnd, curDur))
                    curStart = phase.startEpochMillis
                    curEnd = phase.endEpochMillis
                    curDur = phase.durationMillis
                }
            } else {
                if (curStart != -1L) {
                    subsequentGroups.add(StrongGroup(curStart, curEnd, curDur))
                    curStart = -1L
                    curEnd = -1L
                    curDur = 0L
                }
            }
        }
        if (curStart != -1L) {
            subsequentGroups.add(StrongGroup(curStart, curEnd, curDur))
        }

        val hasSubsequent = subsequentGroups.isNotEmpty()
        val firstGap = if (hasSubsequent) subsequentGroups.first().start - haidPrimaryEnd else 0L

        val haidRanges = mutableListOf<HaidPhaseRange>()
        haidRanges.add(HaidPhaseRange(firstKuatStart, haidPrimaryEnd))
        var lastValidHaidEnd = haidPrimaryEnd

        for (group in subsequentGroups) {
            val gapFromLastHaid = group.start - lastValidHaidEnd
            if (gapFromLastHaid >= FIFTEEN_DAYS_MS && group.duration >= ONE_DAY_MS && group.duration <= FIFTEEN_DAYS_MS) {
                haidRanges.add(HaidPhaseRange(group.start, group.end))
                lastValidHaidEnd = group.end
            }
        }

        val reason = when {
            haidRanges.size > 1 ->
                "Tamyiz sah: ${haidRanges.size} fase darah kuat memenuhi syarat haid karena masing-masing terpisah minimal 15 hari dari akhir haid sah sebelumnya."
            hasSubsequent ->
                "Tamyiz sah: $combinedReason Fase darah kuat berikutnya dihukumi istihadhah karena jarak pemisah dari akhir haid sah terakhir kurang dari 15 hari."
            haidPrimaryDuration > firstKuatDuration ->
                "Tamyiz sah: $combinedReason"
            else ->
                "Tamyiz sah: darah kuat ${formatDuration(firstKuatDuration)} memenuhi minimal 24 jam dan maksimal 15 hari. Tidak ada darah kuat kedua sejenis, sehingga syarat darah lemah pemisah tidak diberlakukan."
        }

        return TamyizPhasesEvaluation(
            isValid = true,
            firstStrongStart = firstKuatStart,
            firstStrongEnd = haidPrimaryEnd,
            firstStrongDuration = haidPrimaryDuration,
            haidRanges = haidRanges,
            hasSubsequentStrong = hasSubsequent,
            firstWeakGapDuration = firstGap,
            reason = reason
        )
    }

    /** Pemeriksaan tamyiz berbasis warna darah sesuai kerangka Tuhfatun Niswah (hal. 29-31). */
    private fun checkTamyiz(intervals: List<BleedingInterval>): TamyizCheckResult {
        val evaluation = evaluateTamyizStrongPhases(intervals)
        return TamyizCheckResult(
            valid = evaluation.isValid,
            strongDurationMillis = evaluation.firstStrongDuration,
            weakContinuousDurationMillis = evaluation.firstWeakGapDuration,
            hasSecondStrongOfSameType = evaluation.hasSubsequentStrong,
            reason = evaluation.reason
        )
    }

    private fun formatDuration(ms: Long): String {
        val hours = ms / ONE_HOUR_MS
        val days = hours / 24
        val remaining = hours % 24
        return if (days > 0) "${days} hari ${remaining} jam" else "${hours} jam"
    }

    /**
     * Mu'tadah Mumayyizah menurut Tuhfatun Niswah: tamyiz berlaku sebagai dasar,
     * kecuali adat haid dapat menjadi haid kembali bila terpisah dari darah kuat
     * sekurang-kurangnya 15 hari 15 malam.
     */
    private fun buildMutadahMumayyizahResults(
        startEpochMillis: Long,
        endEpochMillis: Long,
        intervals: List<BleedingInterval>,
        adatDurationDays: Int
    ): List<FiqihPeriodResult> {
        val ordered = intervals.sortedBy { it.startEpochMillis }
        if (ordered.isEmpty()) return emptyList()
        val evaluation = evaluateTamyizStrongPhases(ordered)
        if (!evaluation.isValid) return emptyList()

        val adatMs = adatDurationDays.coerceIn(1, 15).toLong() * ONE_DAY_MS
        // Jarak dihitung dari AKHIR adat ke awal darah kuat (Tuhfatun Niswah hal. 38).
        // Adat diasumsikan mulai bersamaan dengan awal darah keluar (sesuai contoh kitab).
        val separatedByMinimalPurity = evaluation.firstStrongStart - (startEpochMillis + adatMs) >= FIFTEEN_DAYS_MS

        return if (separatedByMinimalPurity) {
            buildList {
                val adatEnd = minOf(startEpochMillis + adatMs, endEpochMillis)
                if (adatEnd > startEpochMillis) add(
                    FiqihPeriodResult(
                        startEpochMillis, adatEnd, FiqihStatus.HAID,
                        "Bagian awal mengikuti adat haid karena darah kuat berikutnya terpisah minimal 15 hari 15 malam.",
                        needsGhusl = false, needsWudhu = false, needsQadha = false
                    )
                )
                ordered.forEach { phase ->
                    // Jangan biarkan fase darah menimpa bagian awal yang sudah
                    // ditetapkan sebagai adat haid. Pada contoh sumber:
                    // adat 3 hari + lemah 24 hari + kuat 3 hari =
                    // haid 3 | istihadhah 21 | haid 3.
                    val clippedStart = maxOf(phase.startEpochMillis, adatEnd)
                    val clippedEnd = phase.endEpochMillis
                    if (clippedEnd > clippedStart) {
                        val isHaid = evaluation.isHaidPhase(phase)
                        val status = if (isHaid) FiqihStatus.HAID else FiqihStatus.ISTIHADHAH
                        add(
                            FiqihPeriodResult(
                                clippedStart,
                                clippedEnd,
                                status,
                                if (status == FiqihStatus.HAID) "Darah kuat dihukumi haid." else "Darah lemah di antara dua masa haid dihukumi istihadhah.",
                                needsGhusl = status == FiqihStatus.ISTIHADHAH,
                                needsWudhu = status == FiqihStatus.ISTIHADHAH,
                                needsQadha = false
                            )
                        )
                    }
                }
            }.mergeOverlaps()
        } else {
            val strongest = ordered.maxOf { it.strengthScore }
            ordered.map { phase ->
                val isHaid = evaluation.isHaidPhase(phase)
                val status = if (isHaid) FiqihStatus.HAID else FiqihStatus.ISTIHADHAH
                val reason = when {
                    status == FiqihStatus.HAID -> "Darah kuat/lemah yang sah dihukumi haid berdasarkan tamyiz."
                    phase.strengthScore == strongest -> "Darah kuat dihukumi istihadhah karena jarak pemisah dari akhir haid sah terakhir kurang dari 15 hari."
                    else -> "Darah lemah dihukumi istihadhah berdasarkan tamyiz."
                }
                FiqihPeriodResult(
                    phase.startEpochMillis, phase.endEpochMillis, status,
                    reason,
                    needsGhusl = status == FiqihStatus.ISTIHADHAH,
                    needsWudhu = status == FiqihStatus.ISTIHADHAH,
                    needsQadha = false
                )
            }
        }
    }

    /**
     * Qadha bulan pertama untuk mustahadhah mumayyizah mengikuti contoh Tuhfatun Niswah:
     * setelah diketahui darah berubah dari kuat menjadi lemah, masa sampai genap 15 hari
     * yang sebelumnya ditinggalkan shalat menjadi hutang. Setelah lewat 15 hari, shalat
     * ditunaikan ada'an sebagai mustahadhah.
     */
    private fun markFirstMonthMumayyizahQadha(
        periods: List<FiqihPeriodResult>,
        startEpochMillis: Long,
        endOfStrongEpochMillis: Long
    ): List<FiqihPeriodResult> {
        val qadhaBoundary = minOf(startEpochMillis + FIFTEEN_DAYS_MS, periods.maxOfOrNull { it.endEpochMillis } ?: startEpochMillis)
        val out = mutableListOf<FiqihPeriodResult>()
        periods.forEach { period ->
            val qadhaEligible = period.status == FiqihStatus.ISTIHADHAH &&
                period.startEpochMillis < qadhaBoundary &&
                period.endEpochMillis > endOfStrongEpochMillis
            if (!qadhaEligible) {
                out += period
                return@forEach
            }

            val qadhaEnd = minOf(period.endEpochMillis, qadhaBoundary)
            val qadhaStart = maxOf(period.startEpochMillis, endOfStrongEpochMillis)
            if (qadhaEnd > qadhaStart) {
                if (period.startEpochMillis < qadhaStart) {
                    out += period.copy(endEpochMillis = qadhaStart, needsQadha = false)
                }
                out += period.copy(
                    startEpochMillis = qadhaStart,
                    endEpochMillis = qadhaEnd,
                    needsQadha = true,
                    reason = period.reason + " Pada bulan pertama, bagian setelah perubahan darah sampai batas 15 hari termasuk masa yang perlu diqadha sesuai contoh Tuhfatun Niswah."
                )
                if (qadhaEnd < period.endEpochMillis) {
                    out += period.copy(startEpochMillis = qadhaEnd, needsQadha = false)
                }
            } else {
                out += period
            }
        }
        return out
    }

    private fun List<FiqihPeriodResult>.mergeOverlaps(): List<FiqihPeriodResult> =
        sortedBy { it.startEpochMillis }.fold(mutableListOf()) { acc, item ->
            val last = acc.lastOrNull()
            if (last != null && last.status == item.status && last.endEpochMillis == item.startEpochMillis) {
                acc[acc.lastIndex] = last.copy(endEpochMillis = item.endEpochMillis)
            } else acc.add(item)
            acc
        }

    // =========================================================================
    // PERHITUNGAN HAID (Kitab Uyunul Masa'il Linnisa' & Tuhfatun Niswah)
    // 7 Golongan Mustahadhah fil-Haid
    // =========================================================================
    private fun calculateHaidWithPhases(
        startEpochMillis: Long,
        endEpochMillis: Long,
        totalDurationMs: Long,
        totalDays: Long,
        remainingHours: Long,
        intervals: List<BleedingInterval>,
        hasPreviousAdat: Boolean,
        adatDurationDays: Int,
        adatCycleDays: Int,
        adatMemoryType: AdatMemoryType,
        haidCategory: HaidCategory?,
        hasIntermittentPause: Boolean,
        intermittentPauseDays: Double,
        qadhaList: List<String>,
        phaseBreakdowns: List<String>,
        isTakmilahEnabled: Boolean = false,
        previousSuciDaysForTakmilah: Int,
        previousHaidDurationDaysForTakmilah: Int?,
        previousHaidContinuousForTakmilah: Boolean,
        rememberedCertainPureDayOfMonth: Int?,
        rememberedCertainHaidDayOfMonth: Int? = null,
        category6WindowStart: Int = 1,
        category6WindowEnd: Int? = null,
        category6PureDays: Set<Int>? = null,
        category6HaidDays: Set<Int>? = null,
        category6MonthLength: Int = 30
    ): CalculationResult {
        // Kasus 1: Darah kurang dari 24 jam (aqallu al-haid)
        if (totalDurationMs < ONE_DAY_MS) {
            val totalHoursNum = totalDurationMs / ONE_HOUR_MS
            val summary = "Darah Rusak / Bukan Haid (Total: $totalHoursNum Jam)"
            val category = "Darah Fasad (Kurang dari batas minimal haid 24 jam)"
            val istihadhahSegment = PeriodSegment(
                title = "Masa Darah Rusak (Darah Fasad)",
                startEpochMillis = startEpochMillis,
                endEpochMillis = endEpochMillis,
                status = "Darah Rusak (Bukan Haid)",
                description = "Darah keluar total kurang dari 24 jam (sehari semalam), sehingga belum memenuhi syarat minimal haid menurut mazhab Syafi'i."
            )

            val shalatConsequence = "Shalat yang sempat ditinggalkan selama darah keluar WAJIB DIQADHA seluruhnya, karena ternyata darah tersebut tidak sah sebagai haid."
            val puasaConsequence = "Jika sedang berpuasa fardhu (Ramadhan) dan darah tidak mencapai 24 jam, puasanya dihukumi tetap sah."
            val mandiNote = "TIDAK WAJIB mandi besar jinabat. Cukup bersihkan najis (istinja), berwudhu, dan menunaikan shalat."
            val advice = "Bercak singkat di bawah 24 jam sering disebabkan oleh ketidakseimbangan hormon ringan atau kelelahan. Jika berulang atau disertai nyeri hebat, disarankan berkonsultasi dengan dokter spesialis kebidanan dan kandungan (Sp.OG)."

            val refs = listOf(
                KitabReference(
                    kitabName = "Uyunul Masa-il Linnisa' (Lirboyo)",
                    volumeAndPage = "Bab I: Batas Minimal Haid, hal. 11",
                    ibaratSnippet = "أقل الحيض يوم وليلة أي أربع وعشرون ساعة على الاتصال المعتاد... فإذا نقص عن ذلك فهو دم فساد",
                    explanation = "Paling sedikitnya masa haid adalah sehari semalam (24 jam) secara bersambung yang lumrah. Jika kurang dari itu, maka dihukumi darah rusak (dam fasad)."
                ),
                KitabReference(
                    kitabName = "Tuhfatun Niswah",
                    volumeAndPage = "Bab Syarat-syarat Haid, hal. 5",
                    ibaratSnippet = "Syarat sah darah dinamakan haid adalah tidak kurang dari 24 jam dan tidak lebih dari 15 hari 15 malam.",
                    explanation = "Darah di bawah 24 jam tidak menggugurkan kewajiban shalat dan shalat yang tertinggal wajib diqadha."
                )
            )

            return CalculationResult(
                statusSummary = summary,
                caseCategory = category,
                categoryDetectionReason = "Otomatis terdeteksi sebagai Darah Fasad (Rusak/Bukan Haid) karena total keluarnya darah ($totalHoursNum jam) belum mencapai syarat minimal haid sehari semalam (24 jam penuh) menurut Mazhab Syafi'i.",
                haidOrNifasSegment = null,
                istihadhahSegment = istihadhahSegment,
                suciSegment = null,
                shalatConsequence = shalatConsequence,
                qadhaPrayers = qadhaList,
                puasaConsequence = puasaConsequence,
                mandiWajibNote = mandiNote,
                mustahadhahCareGuide = null,
                medicalAndSpiritualAdvice = advice,
                references = refs,
                phaseBreakdowns = phaseBreakdowns
            )
        }

        // =========================================================================
        // Deteksi Jeda Bersih (Fatrah Naqa') di Antara Dua Fase Darah
        // Sumber:
        // A. Jeda antar-interval dalam daftar intervals (intervals.size >= 2 dengan gap > 0)
        // B. Switch hasIntermittentPause dan intermittentPauseDays
        // =========================================================================
        val sortedIntervals = intervals.filter { it.durationMillis > 0 }.sortedBy { it.startEpochMillis }
        val blocks = mutableListOf<BleedingBlock>()
        for (interval in sortedIntervals) {
            val last = blocks.lastOrNull()
            if (last != null && interval.startEpochMillis <= last.endEpochMillis) {
                blocks[blocks.lastIndex] = BleedingBlock(
                    startEpochMillis = last.startEpochMillis,
                    endEpochMillis = maxOf(last.endEpochMillis, interval.endEpochMillis),
                    intervals = last.intervals + interval
                )
            } else {
                blocks.add(
                    BleedingBlock(
                        startEpochMillis = interval.startEpochMillis,
                        endEpochMillis = interval.endEpochMillis,
                        intervals = listOf(interval)
                    )
                )
            }
        }
        val hasMultiIntervalGap = blocks.size >= 2

        val internalGapStart: Long
        val internalGapEnd: Long
        val internalGapDays: Double
        val firstBloodStart: Long
        val firstBloodEnd: Long
        val secondBloodStart: Long
        val secondBloodEnd: Long
        val hasDetectedInternalGap: Boolean

        if (hasMultiIntervalGap) {
            // 1. Cek apakah ada jeda suci pemisah sempurna (>= 15 hari) di antara blok mana pun
            val majorGapIndex = (0 until blocks.size - 1).firstOrNull { i ->
                (blocks[i + 1].startEpochMillis - blocks[i].endEpochMillis).toDouble() / ONE_DAY_MS >= 15.0
            }

            if (majorGapIndex != null) {
                firstBloodStart = blocks[0].startEpochMillis
                firstBloodEnd = blocks[majorGapIndex].endEpochMillis
                internalGapStart = blocks[majorGapIndex].endEpochMillis
                internalGapEnd = blocks[majorGapIndex + 1].startEpochMillis
                internalGapDays = (internalGapEnd - internalGapStart).toDouble() / ONE_DAY_MS
                secondBloodStart = blocks[majorGapIndex + 1].startEpochMillis
                secondBloodEnd = blocks.last().endEpochMillis
                hasDetectedInternalGap = true
            } else {
                // Seluruh jeda < 15 hari.
                // Cari blok yang menandai akhir siklus haid pertama (maksimal 15 hari dari awal darah)
                val firstHaidEndBlockIndex = (0 until blocks.size - 1).lastOrNull { i ->
                    (blocks[i].endEpochMillis - blocks.first().startEpochMillis) <= FIFTEEN_DAYS_MS
                } ?: 0

                firstBloodStart = blocks.first().startEpochMillis
                firstBloodEnd = blocks[firstHaidEndBlockIndex].endEpochMillis
                internalGapStart = blocks[firstHaidEndBlockIndex].endEpochMillis
                internalGapEnd = blocks[firstHaidEndBlockIndex + 1].startEpochMillis
                internalGapDays = (internalGapEnd - internalGapStart).toDouble() / ONE_DAY_MS
                secondBloodStart = blocks[firstHaidEndBlockIndex + 1].startEpochMillis
                secondBloodEnd = blocks.last().endEpochMillis
                hasDetectedInternalGap = true
            }
        } else if (hasIntermittentPause && intermittentPauseDays > 0.0) {
            val firstDays = previousHaidDurationDaysForTakmilah
                ?: (if (blocks.isNotEmpty()) ((blocks[0].durationMillis) / ONE_DAY_MS).toInt().coerceAtLeast(1)
                    else (if (adatDurationDays in 1..15) adatDurationDays else 7))
            val firstMs = minOf(
                firstDays.toLong() * ONE_DAY_MS,
                (totalDurationMs - (intermittentPauseDays * ONE_DAY_MS).toLong() - ONE_DAY_MS).coerceAtLeast(ONE_DAY_MS)
            )
            firstBloodStart = startEpochMillis
            firstBloodEnd = startEpochMillis + firstMs
            internalGapStart = firstBloodEnd
            val gapMs = (intermittentPauseDays * ONE_DAY_MS).toLong()
            internalGapEnd = internalGapStart + gapMs
            secondBloodStart = internalGapEnd
            secondBloodEnd = endEpochMillis
            internalGapDays = intermittentPauseDays
            hasDetectedInternalGap = internalGapEnd < endEpochMillis
        } else {
            firstBloodStart = startEpochMillis
            firstBloodEnd = startEpochMillis
            internalGapStart = startEpochMillis
            internalGapEnd = startEpochMillis
            internalGapDays = 0.0
            secondBloodStart = startEpochMillis
            secondBloodEnd = endEpochMillis
            hasDetectedInternalGap = false
        }

        // Kasus 2: Jeda suci di sela haid (Fatrah Naqa') >= 15 hari (Pemisah Dua Siklus Haid Sah)
        if (hasDetectedInternalGap && internalGapDays >= 15.0) {
            val d1Days = ((firstBloodEnd - firstBloodStart) / ONE_DAY_MS).toInt().coerceAtLeast(1)
            val d2Days = ((secondBloodEnd - secondBloodStart) / ONE_DAY_MS).toInt().coerceAtLeast(1)
            val summary = "Darah Terputus Jeda Suci Sah (Pemisah Dua Siklus Haid)"
            val category = "Pemisah Dua Haid Berdasarkan Batas Minimal Suci 15 Hari"
            val haidSeg1 = PeriodSegment(
                title = "Masa Haid Pertama",
                startEpochMillis = firstBloodStart,
                endEpochMillis = firstBloodEnd,
                status = "Haid Sah",
                description = "Darah pertama keluar selama $d1Days hari sebelum jeda suci memenuhi syarat minimal haid dan dihukumi haid pertama yang sah."
            )
            val suciSeg = PeriodSegment(
                title = "Masa Suci Pemisah (Naqa')",
                startEpochMillis = internalGapStart,
                endEpochMillis = internalGapEnd,
                status = "Suci Sah",
                description = "Jeda suci bersih ${"%.1f".format(internalGapDays).replace(".0", "")} hari memenuhi syarat minimal suci 15 hari (aqalluth-thuhr baynal-haidhatayn)."
            )
            val haidSeg2 = PeriodSegment(
                title = "Masa Haid Kedua (Siklus Baru)",
                startEpochMillis = secondBloodStart,
                endEpochMillis = secondBloodEnd,
                status = "Haid Sah",
                description = "Darah setelah jeda suci 15 hari berlangsung selama $d2Days hari sehingga sah menjadi siklus haid baru."
            )

            val segments = listOf(haidSeg1, suciSeg, haidSeg2)
            val shalatConsequence = "Selama masa haid pertama dan kedua shalat gugur. Selama jeda suci ${"%.1f".format(internalGapDays).replace(".0", "")} hari, seluruh shalat fardhu dan puasa wajib dikerjakan."
            val mandiNote = "Wajib mandi besar jinabat saat darah pertama berhenti masuk jeda suci, dan wajib mandi kembali setelah darah kedua berhenti tuntas."

            val refs = listOf(
                KitabReference(
                    kitabName = "Uyunul Masa-il Linnisa' (Lirboyo)",
                    volumeAndPage = "Bab II: Batas Minimal Suci Antara Dua Haid, hal. 23",
                    ibaratSnippet = "أقل الطهر بين الحيضتين خمسة عشر يوما ولياليها ولا حد لأكثره",
                    explanation = "Paling sedikitnya masa suci antara dua siklus haid adalah 15 hari 15 malam. Jeda 15 hari memisahkan dua siklus haid secara sah."
                ),
                KitabReference(
                    kitabName = "Tuhfatun Niswah",
                    volumeAndPage = "Fashl fi al-Fatrah wa an-Naqa', hal. 12",
                    ibaratSnippet = "Bila darah terputus selama 15 hari atau lebih, maka darah yang keluar setelahnya dihukumi darah haid baru.",
                    explanation = "Darah sesudah jeda suci sempurna bukan kelanjutan haid lama, melainkan haid baru."
                )
            )

            return CalculationResult(
                statusSummary = summary,
                caseCategory = category,
                categoryDetectionReason = "Otomatis terdeteksi sebagai Pemisah Dua Haid karena terdapat jeda bersih ${"%.1f".format(internalGapDays).replace(".0", "")} hari di sela darah (memenuhi batas minimal suci 15 hari). Darah kedua berstatus siklus haid baru.",
                haidOrNifasSegment = haidSeg1,
                istihadhahSegment = null,
                suciSegment = suciSeg,
                shalatConsequence = shalatConsequence,
                qadhaPrayers = qadhaList,
                puasaConsequence = "Puasa pada masa jeda suci sah dan wajib dikerjakan. Puasa saat darah keluar wajib diqadha.",
                mandiWajibNote = mandiNote,
                mustahadhahCareGuide = null,
                medicalAndSpiritualAdvice = "Siklus haid dengan jeda suci bersih normal adalah proses fisiologis yang sehat.",
                references = refs,
                phaseBreakdowns = phaseBreakdowns + segments.map { "${it.title} (${formatDisplayRange(it.startEpochMillis, it.endEpochMillis)}): ${it.status}" },
                periodResults = segments.map { segment ->
                    val status = if (segment.status.contains("Haid")) FiqihStatus.HAID else FiqihStatus.SUCI
                    FiqihPeriodResult(
                        startEpochMillis = segment.startEpochMillis,
                        endEpochMillis = segment.endEpochMillis,
                        status = status,
                        reason = segment.description,
                        needsGhusl = status == FiqihStatus.HAID,
                        needsWudhu = false,
                        needsQadha = false
                    )
                },
                allSegments = segments
            )
        }

        // Kasus 3A: Takmilatan lit-Tuhri di Sela Dua Darah (In-Cycle Takmilah)
        // Terjadi ketika jeda suci < 15 hari dan total rentang darah melebihi 15 hari.
        // Sesuai Mazhab Syafi'i (Tuhfatun Niswah & Uyunul Masa'il Linnisa'):
        // Darah pertama = Haid, Jeda = Suci (Naqa'), awal darah kedua = Istihadhah (Takmilah), sisa darah kedua = Haid Baru.
        if (hasDetectedInternalGap && internalGapDays in 0.05..14.99 && totalDurationMs > FIFTEEN_DAYS_MS) {
            val d1Ms = firstBloodEnd - firstBloodStart
            val d1Days = (d1Ms / ONE_DAY_MS).toInt().coerceAtLeast(1)
            val d2Ms = secondBloodEnd - secondBloodStart
            val gapDaysFormatted = "%.1f".format(internalGapDays).replace(".0", "")

            // Validasi Syarat Pokok Takmilah menurut Tuhfatun Niswah hal. 14:
            // Darah pertama wajib sah sebagai haid (minimal 24 jam dan maksimal 15 hari).
            if (d1Ms < ONE_DAY_MS) {
                val segFasad = PeriodSegment(
                    title = "Darah Pertama (Darah Fasad)",
                    startEpochMillis = firstBloodStart,
                    endEpochMillis = firstBloodEnd,
                    status = "Darah Fasad",
                    description = "Darah pertama keluar kurang dari 24 jam (${"%.1f".format(d1Ms.toDouble() / 3600_000L)} jam) sehingga berstatus darah fasad (bukan haid)."
                )
                val segNaqa = PeriodSegment(
                    title = "Masa Bersih (Suci)",
                    startEpochMillis = internalGapStart,
                    endEpochMillis = internalGapEnd,
                    status = "Suci Sah",
                    description = "Masa jeda tanpa darah berstatus suci."
                )
                val d2Days = (d2Ms / ONE_DAY_MS).toInt().coerceAtLeast(1)
                val segHaid2 = if (d2Ms in ONE_DAY_MS..FIFTEEN_DAYS_MS) {
                    PeriodSegment(
                        title = "Masa Haid Sah",
                        startEpochMillis = secondBloodStart,
                        endEpochMillis = secondBloodEnd,
                        status = "Haid Sah",
                        description = "Karena darah pertama fasad, darah kedua ($d2Days hari) inilah yang memenuhi syarat minimal haid 24 jam dan sah sebagai haid."
                    )
                } else {
                    PeriodSegment(
                        title = "Darah Kedua (Melampaui 15 Hari)",
                        startEpochMillis = secondBloodStart,
                        endEpochMillis = secondBloodEnd,
                        status = "Haid / Istihadhah",
                        description = "Darah kedua melebihi 15 hari dan memerlukan penentuan hukum berdasarkan adat/tamyiz."
                    )
                }
                val segs = listOf(segFasad, segNaqa, segHaid2)
                return CalculationResult(
                    statusSummary = "Darah Pertama Fasad (< 24 Jam) → Darah Kedua: ${segHaid2.status}",
                    caseCategory = "Darah Pertama Fasad (Takmilah Tidak Berlaku)",
                    categoryDetectionReason = "Takmilatan lit-Tuhri tidak berlaku karena darah pertama (${"%.1f".format(d1Ms.toDouble() / 3600_000L)} jam) tidak memenuhi syarat minimal haid 24 jam (Tuhfatun Niswah hal. 14). Darah pertama berstatus darah fasad, dan darah kedua dihukumi mandiri.",
                    haidOrNifasSegment = if (segHaid2.status == "Haid Sah") segHaid2 else null,
                    istihadhahSegment = segFasad,
                    suciSegment = segNaqa,
                    shalatConsequence = "Shalat yang ditinggalkan saat darah fasad pertama wajib diqadha. Shalat selama jeda bersih wajib dikerjakan. Shalat saat haid kedua gugur.",
                    qadhaPrayers = qadhaList,
                    puasaConsequence = "Puasa sah pada masa suci, dan wajib diqadha jika ditinggalkan saat darah fasad.",
                    mandiWajibNote = "Wajib mandi jinabat setelah darah kedua berhenti jika sah sebagai haid.",
                    mustahadhahCareGuide = null,
                    medicalAndSpiritualAdvice = "Keluarnya flek/darah singkat di bawah 24 jam adalah darah fasad (bukan haid).",
                    references = listOf(
                        KitabReference(
                            kitabName = "Tuhfatun Niswah",
                            volumeAndPage = "Bab Menyempurnakan Masa Suci, hal. 14",
                            ibaratSnippet = "Konsep menyempurnakan suci hanya berlaku jika darah pertama tidak putus-putus dan tidak kurang dari 24 jam serta tidak lebih dari 15 hari.",
                            explanation = "Darah pertama wajib sah sebagai haid agar konsep Takmilatan lit-Tuhri dapat diterapkan."
                        )
                    ),
                    phaseBreakdowns = phaseBreakdowns + segs.map { "${it.title} (${formatDisplayRange(it.startEpochMillis, it.endEpochMillis)}): ${it.status}" },
                    periodResults = segs.map { seg ->
                        val status = if (seg.status.contains("Haid")) FiqihStatus.HAID else if (seg.status.contains("Suci")) FiqihStatus.SUCI else FiqihStatus.ISTIHADHAH
                        FiqihPeriodResult(
                            startEpochMillis = seg.startEpochMillis,
                            endEpochMillis = seg.endEpochMillis,
                            status = status,
                            reason = seg.description,
                            needsGhusl = seg.status == "Haid Sah",
                            needsWudhu = false,
                            needsQadha = false
                        )
                    },
                    allSegments = segs
                )
            }

            val segments = mutableListOf<PeriodSegment>()

            // 1) Darah Pertama: Haid Sah jika 1-15 hari
            val firstHaidSeg = PeriodSegment(
                title = "Masa Haid Pertama",
                startEpochMillis = firstBloodStart,
                endEpochMillis = firstBloodEnd,
                status = "Haid Sah",
                description = "Darah pertama keluar selama $d1Days hari (memenuhi syarat minimal 24 jam dan maksimal 15 hari) sehingga sah sebagai haid pertama."
            )
            segments += firstHaidSeg

            // 2) Jeda Suci (Naqa'): Suci Sah
            val naqaSeg = PeriodSegment(
                title = "Masa Suci Pemisah (Naqa')",
                startEpochMillis = internalGapStart,
                endEpochMillis = internalGapEnd,
                status = "Suci Sah (Naqa')",
                description = "Jeda tanpa darah selama $gapDaysFormatted hari berstatus suci murni (wajib shalat dan puasa). Karena belum mencapai batas minimal suci 15 hari, masa suci ini memerlukan penyempurna dari darah berikutnya."
            )
            segments += naqaSeg

            // 3) Darah Kedua: Takmilatan lit-Tuhri (Istihadhah Penyempurna Suci)
            val completionDays = 15.0 - internalGapDays
            val completionMs = (completionDays * ONE_DAY_MS).toLong()
            val takmilahDurationMs = minOf(completionMs, d2Ms)
            val takmilahEnd = secondBloodStart + takmilahDurationMs
            val completionDaysFormatted = "%.1f".format(completionDays).replace(".0", "")

            val takmilahSeg = PeriodSegment(
                title = "Takmilatan lit-Tuhri",
                startEpochMillis = secondBloodStart,
                endEpochMillis = takmilahEnd,
                status = "Istihadhah (Penyempurna Suci)",
                description = "$completionDaysFormatted hari pertama darah kedua menyempurnakan masa suci dari $gapDaysFormatted hari menjadi 15 hari. Selama fase ini berlaku hukum istihadhah (wajib shalat dan puasa dengan bersuci setiap fardhu)."
            )
            segments += takmilahSeg

            // 4) Sisa Darah Kedua setelah Suci Genap 15 Hari
            val remainingD2Ms = secondBloodEnd - takmilahEnd
            if (remainingD2Ms > 0L) {
                val remDays = (remainingD2Ms / ONE_DAY_MS).toInt()
                if (remainingD2Ms in ONE_DAY_MS..FIFTEEN_DAYS_MS) {
                    segments += PeriodSegment(
                        title = "Haid Kedua (Haid Baru)",
                        startEpochMillis = takmilahEnd,
                        endEpochMillis = secondBloodEnd,
                        status = "Haid Sah",
                        description = "Setelah masa suci genap 15 hari (jeda suci + takmilah), sisa darah berlangsung selama $remDays hari (memenuhi syarat minimal 24 jam dan maksimal 15 hari) sehingga dihukumi sebagai siklus haid baru."
                    )
                } else if (remainingD2Ms < ONE_DAY_MS) {
                    segments += PeriodSegment(
                        title = "Darah Fasad Setelah Takmilah",
                        startEpochMillis = takmilahEnd,
                        endEpochMillis = secondBloodEnd,
                        status = "Darah Fasad",
                        description = "Sisa darah setelah Takmilah kurang dari 24 jam sehingga tidak memenuhi syarat minimal haid."
                    )
                } else {
                    val refHaidMs = (if (adatDurationDays in 1..15) adatDurationDays else 7).toLong() * ONE_DAY_MS
                    val hMs = minOf(refHaidMs, remainingD2Ms)
                    segments += PeriodSegment(
                        title = "Haid Kedua (Berdasarkan Adat)",
                        startEpochMillis = takmilahEnd,
                        endEpochMillis = takmilahEnd + hMs,
                        status = "Haid Sah",
                        description = "Sisa darah setelah Takmilah melebihi 15 hari sehingga dihukumi mengikuti adat haid."
                    )
                    if (remainingD2Ms > hMs) {
                        segments += PeriodSegment(
                            title = "Istihadhah Siklus Baru",
                            startEpochMillis = takmilahEnd + hMs,
                            endEpochMillis = secondBloodEnd,
                            status = "Istihadhah",
                            description = "Kelebihan darah di atas adat dihukumi istihadhah."
                        )
                    }
                }
            }

            val summaryFlow = segments.joinToString(" → ") { "${it.title}: ${formatDisplayRange(it.startEpochMillis, it.endEpochMillis)} (${it.status})" }
            val shalatConsequence = "Shalat gugur pada masa haid (Masa Haid Pertama dan Haid Kedua). Selama jeda suci (Naqa') dan fase Takmilatan lit-Tuhri, shalat fardhu dan puasa WAJIB dikerjakan (pada fase Takmilah menggunakan tata cara bersuci mustahadhah)."
            val puasaConsequence = "Puasa pada masa jeda suci (Naqa') dan fase Takmilatan lit-Tuhri SAH dan wajib dikerjakan. Puasa pada masa haid tidak sah dan wajib diqadha di luar bulan Ramadhan."
            val mandiNote = "Wajib mandi besar jinabat saat darah pertama berhenti masuk jeda suci, dan wajib mandi kembali setelah darah kedua berhenti tuntas."

            val refs = listOf(
                KitabReference(
                    kitabName = "Tuhfatun Niswah",
                    volumeAndPage = "Bab Menyempurnakan Masa Suci (Takmilatan lit-Tuhri), hal. 14",
                    ibaratSnippet = "Contoh: 10 hari darah, 8 hari suci, 12 hari darah: 10 darah = haid, 8 suci = naqa', 7 hari awal darah kedua = penyempurna suci (istihadhah), 5 hari sisa darah kedua = haid baru.",
                    explanation = "Darah yang keluar sebelum genap masa suci 15 hari wajib menyempurnakan masa suci menjadi 15 hari terlebih dahulu sebagai istihadhah, sebelum sisanya dapat dihukumi sebagai haid baru."
                ),
                KitabReference(
                    kitabName = "Uyunul Masa-il Linnisa' (Lirboyo)",
                    volumeAndPage = "Bab II: Batas Minimal Suci Antara Dua Haid, hal. 24",
                    ibaratSnippet = "فإن رأت دما دون أقل الطهر كملت به الطهر وكان ما بقي حيضا إن صلح له",
                    explanation = "Jika seorang wanita melihat darah sebelum genapnya masa minimal suci (15 hari), maka darah tersebut mula-mula menyempurnakan masa suci (sebagai istihadhah), dan darah sisanya adalah haid baru jika memenuhi syarat haid."
                )
            )

            return CalculationResult(
                statusSummary = "Takmilatan lit-Tuhri: $summaryFlow",
                caseCategory = "Istihadhah Takmilatan lit-Tuhri",
                categoryDetectionReason = "Terdeteksi adanya jeda suci $gapDaysFormatted hari di sela dua darah dalam siklus melampaui 15 hari. Karena jeda suci kurang dari 15 hari, $completionDaysFormatted hari pertama darah kedua dihukumi Takmilatan lit-Tuhri (Istihadhah) untuk menggenapkan masa suci menjadi 15 hari, dan sisa darah dihukumi sesuai kaidah fiqih Mazhab Syafi'i (Tuhfatun Niswah & Uyunul Masa'il Linnisa').",
                haidOrNifasSegment = segments.firstOrNull { it.status == "Haid Sah" },
                istihadhahSegment = segments.firstOrNull { it.status.contains("Istihadhah") },
                suciSegment = segments.firstOrNull { it.status.contains("Suci") },
                shalatConsequence = shalatConsequence,
                qadhaPrayers = qadhaList,
                puasaConsequence = puasaConsequence,
                mandiWajibNote = mandiNote,
                mustahadhahCareGuide = "Pada fase Takmilatan lit-Tuhri, gunakan tata cara bersuci dan shalat mustahadhah (bersihkan kemaluan, sumbat, balut, berwudhu setelah masuk waktu fardhu, dan langsung menunaikan shalat).",
                medicalAndSpiritualAdvice = "Keluarnya darah setelah jeda bersih singkat sering terjadi akibat fluktuasi hormonal ovulasi atau perdarahan disfungsi. Jika sering berulang, konsultasikan dengan dokter spesialis kebidanan dan kandungan (Sp.OG).",
                references = refs,
                phaseBreakdowns = phaseBreakdowns + segments.map { "${it.title} (${formatDisplayRange(it.startEpochMillis, it.endEpochMillis)}): ${it.status}" },
                periodResults = segments.map { segment ->
                    val status = when {
                        segment.title.contains("Takmilat") -> FiqihStatus.ISTIHADHAH
                        segment.status.contains("Haid") -> FiqihStatus.HAID
                        segment.status.contains("Suci") -> FiqihStatus.SUCI
                        segment.status.contains("Istihadhah") -> FiqihStatus.ISTIHADHAH
                        else -> FiqihStatus.IHTIYATH
                    }
                    FiqihPeriodResult(
                        startEpochMillis = segment.startEpochMillis,
                        endEpochMillis = segment.endEpochMillis,
                        status = status,
                        reason = segment.description,
                        needsGhusl = segment.status == "Haid Sah",
                        needsWudhu = status == FiqihStatus.ISTIHADHAH,
                        needsQadha = false
                    )
                },
                allSegments = segments
            )
        }

        // Kasus 3B: Takmilatan lit-Tuhri Karena Masa Suci Sebelum Siklus Ini < 15 Hari (Pre-Cycle Takmilah)
        // Diterapkan HANYA jika darah bersambung dari startEpochMillis dan masa suci sebelum darah pertama kurang dari 15 hari.
        // Jika masa suci sebelum siklus ini sudah mencapai minimal 15 hari, maka tidak ada defisit masa suci (syarat Takmilah tidak terpenuhi).
        if (!hasDetectedInternalGap && previousSuciDaysForTakmilah in 1..14) {
            val firstBloodDays = previousHaidDurationDaysForTakmilah
                ?: (if (adatDurationDays in 1..15) adatDurationDays else 7)
            val sourceConditionsMet = previousHaidContinuousForTakmilah && firstBloodDays in 1..15

            if (!sourceConditionsMet) {
                val explanation = buildString {
                    append("Takmilatan lit-Tuhri belum dapat dipastikan otomatis. ")
                    append("Sumber mensyaratkan darah pertama keluar terus-menerus dan berlangsung 24 jam sampai 15 hari. ")
                    append("Pastikan data darah pertama diverifikasi.")
                }
                val seg = PeriodSegment(
                    title = "Takmilah Belum Dapat Dipastikan",
                    startEpochMillis = startEpochMillis,
                    endEpochMillis = endEpochMillis,
                    status = "Perlu Verifikasi",
                    description = explanation
                )
                return CalculationResult(
                    statusSummary = "Takmilatan lit-Tuhri: Perlu Verifikasi Data",
                    caseCategory = "Takmilatan lit-Tuhri (Syarat Belum Lengkap)",
                    categoryDetectionReason = explanation,
                    haidOrNifasSegment = null,
                    istihadhahSegment = seg,
                    suciSegment = null,
                    shalatConsequence = "Belum menentukan hukum haid/istihadhah secara final karena syarat Takmilatan lit-Tuhri belum lengkap.",
                    qadhaPrayers = qadhaList,
                    puasaConsequence = "Belum menentukan secara final sampai data darah pertama diverifikasi.",
                    mandiWajibNote = "Jangan menjadikan hasil ini sebagai keputusan mandi wajib sebelum data lengkap.",
                    mustahadhahCareGuide = null,
                    medicalAndSpiritualAdvice = explanation,
                    references = listOf(
                        KitabReference(
                            kitabName = "Tuhfatun Niswah",
                            volumeAndPage = "Bab Menyempurnakan Masa Suci",
                            ibaratSnippet = "Konsep menyempurnakan suci hanya berlaku jika darah pertama tidak putus-putus dan tidak kurang dari 24 jam serta tidak lebih dari 15 hari.",
                            explanation = "Syarat Takmilatan lit-Tuhri harus diverifikasi sebelum algoritma menentukan fase berikutnya."
                        )
                    ),
                    phaseBreakdowns = phaseBreakdowns,
                    allSegments = listOf(seg)
                )
            }

            val completionDays = 15 - previousSuciDaysForTakmilah
            val completionMs = completionDays * ONE_DAY_MS
            val segments = mutableListOf<PeriodSegment>()
            var cursor = startEpochMillis
            var remaining = totalDurationMs

            val firstCompletionMs = minOf(completionMs, remaining)
            segments += PeriodSegment(
                title = "Takmilatan lit-Tuhri",
                startEpochMillis = cursor,
                endEpochMillis = cursor + firstCompletionMs,
                status = "Istihadhah (Penyempurna Suci)",
                description = "$completionDays hari (${formatDisplayRange(cursor, cursor + firstCompletionMs)}) diperlukan untuk menyempurnakan masa suci sebelumnya dari $previousSuciDaysForTakmilah hari menjadi 15 hari."
            )
            cursor += firstCompletionMs
            remaining -= firstCompletionMs

            if (remaining > 0L) {
                if (remaining <= FIFTEEN_DAYS_MS) {
                    if (remaining >= ONE_DAY_MS) {
                        segments += PeriodSegment(
                            title = "Haid Setelah Takmilah",
                            startEpochMillis = cursor,
                            endEpochMillis = endEpochMillis,
                            status = "Haid Sah",
                            description = "Setelah masa suci genap 15 hari, sisa darah (${formatDisplayRange(cursor, endEpochMillis)}) berlangsung tidak lebih dari 15 hari dan mencapai minimal 24 jam, sehingga dihukumi haid."
                        )
                    } else {
                        segments += PeriodSegment(
                            title = "Darah Fasad Setelah Takmilah",
                            startEpochMillis = cursor,
                            endEpochMillis = endEpochMillis,
                            status = "Darah Fasad",
                            description = "Sisa darah setelah Takmilah (${formatDisplayRange(cursor, endEpochMillis)}) kurang dari 24 jam sehingga tidak memenuhi minimal haid."
                        )
                    }
                } else {
                    val referenceHaidMs = firstBloodDays.toLong() * ONE_DAY_MS
                    val firstHaidMs = minOf(referenceHaidMs, remaining)
                    if (firstHaidMs >= ONE_DAY_MS) {
                        segments += PeriodSegment(
                            title = "Haid Setelah Takmilah",
                            startEpochMillis = cursor,
                            endEpochMillis = cursor + firstHaidMs,
                            status = "Haid Sah",
                            description = "Haid setelah Takmilah mengikuti durasi darah pertama sebagai acuan pada pola berkelanjutan yang melebihi 15 hari (${formatDisplayRange(cursor, cursor + firstHaidMs)})."
                        )
                    }
                    cursor += firstHaidMs
                    remaining -= firstHaidMs

                    if (remaining > 0L) {
                        val pureMs = minOf(FIFTEEN_DAYS_MS, remaining)
                        segments += PeriodSegment(
                            title = "Suci / Istihadhah di Antara Dua Haid",
                            startEpochMillis = cursor,
                            endEpochMillis = cursor + pureMs,
                            status = "Suci / Istihadhah",
                            description = "Lima belas hari berikutnya (${formatDisplayRange(cursor, cursor + pureMs)}) menjadi masa suci pemisah; pada pola darah terus-menerus status darahnya dijelaskan sebagai istihadhah."
                        )
                        cursor += pureMs
                        remaining -= pureMs
                    }
                    if (remaining > 0L) {
                        val nextHaidMs = minOf(referenceHaidMs, remaining)
                        if (nextHaidMs >= ONE_DAY_MS) {
                            segments += PeriodSegment(
                                title = "Haid Berikutnya",
                                startEpochMillis = cursor,
                                endEpochMillis = cursor + nextHaidMs,
                                status = "Haid Sah",
                                description = "Sisa darah setelah masa suci 15 hari berikutnya dihukumi haid sesuai pola yang dijelaskan dalam konsep Takmilah (${formatDisplayRange(cursor, cursor + nextHaidMs)})."
                            )
                        }
                    }
                }
            }

            val summary = segments.joinToString(" → ") { "${it.title}: ${formatDisplayRange(it.startEpochMillis, it.endEpochMillis)} (${it.status})" }
            return CalculationResult(
                statusSummary = "Takmilatan lit-Tuhri: $summary",
                caseCategory = "Istihadhah Takmilatan lit-Tuhri",
                categoryDetectionReason = "Masa suci sebelumnya $previousSuciDaysForTakmilah hari, darah pertama $firstBloodDays hari dan tidak terputus. Algoritma menerapkan konsep penyempurnaan suci 15 hari sesuai Tuhfatun Niswah.",
                haidOrNifasSegment = segments.firstOrNull { it.status == "Haid Sah" },
                istihadhahSegment = segments.firstOrNull { it.status.contains("Istihadhah") },
                suciSegment = segments.firstOrNull { it.status.contains("Suci") },
                shalatConsequence = "Shalat wajib dikerjakan pada fase istihadhah/suci dan gugur pada fase haid. Rincian qadha mengikuti status masing-masing fase.",
                qadhaPrayers = qadhaList,
                puasaConsequence = "Puasa wajib dikerjakan pada fase suci/istihadhah dan tidak dilakukan pada fase haid.",
                mandiWajibNote = "Mandi mengikuti perpindahan ke status haid/suci sesuai fase yang dihasilkan.",
                mustahadhahCareGuide = "Pada fase istihadhah, gunakan tata cara shalat mustahadhah.",
                medicalAndSpiritualAdvice = "Hasil ini menggunakan konsep Takmilatan lit-Tuhri dari Tuhfatun Niswah dan hanya aktif setelah dua syarat darah pertama diverifikasi.",
                references = listOf(
                    KitabReference(
                        kitabName = "Tuhfatun Niswah",
                        volumeAndPage = "Bab Menyempurnakan Masa Suci",
                        ibaratSnippet = "3 hari darah, 12 hari suci, 10 hari darah: 3 darah = haid, 12 suci, 3 penyempurna suci/istihadhah, 7 haid.",
                        explanation = "Konsep Takmilatan lit-Tuhri menyempurnakan masa suci menjadi 15 hari sebelum darah berikutnya dihukumi haid."
                    )
                ),
                phaseBreakdowns = phaseBreakdowns + segments.map { "${it.title} (${formatDisplayRange(it.startEpochMillis, it.endEpochMillis)}): ${it.status}" },
                periodResults = segments.map { segment ->
                    val status = when {
                        segment.status.contains("Haid") -> FiqihStatus.HAID
                        segment.status.contains("Suci") -> FiqihStatus.SUCI
                        segment.status.contains("Istihadhah") -> FiqihStatus.ISTIHADHAH
                        else -> FiqihStatus.IHTIYATH
                    }
                    FiqihPeriodResult(
                        startEpochMillis = segment.startEpochMillis,
                        endEpochMillis = segment.endEpochMillis,
                        status = status,
                        reason = segment.description,
                        needsGhusl = segment.status == "Haid Sah",
                        needsWudhu = status == FiqihStatus.ISTIHADHAH,
                        needsQadha = false
                    )
                },
                allSegments = segments
            )
        }

        // Kasus 4: Darah 24 Jam s/d 15 Hari (Haid Normal Sah)
        if (totalDurationMs <= FIFTEEN_DAYS_MS) {
            val summary = "Haid Sah: $totalDays Hari $remainingHours Jam (0 Jam Istihadhah)"
            val category = if (hasDetectedInternalGap && blocks.size >= 2) "Haid Sah dengan Jeda Bersih (Qaul Sahbi)" else (if (totalDays in 6..7) "Haid Normal (Ghalib 6-7 Hari)" else "Haid Sah (Batas 1-15 Hari)")
            val pauseNote = if (hasDetectedInternalGap || hasIntermittentPause) " (Termasuk jeda bersih < 15 hari dihukumi haid menurut Qaul Sahbi)" else ""
            val haidSegment = PeriodSegment(
                title = "Masa Haid Sah",
                startEpochMillis = startEpochMillis,
                endEpochMillis = endEpochMillis,
                status = "Haid Sah",
                description = "Darah keluar memenuhi syarat minimal 24 jam dan tidak melampaui batas maksimal 15 hari 15 malam$pauseNote."
            )

            val segments = if (hasDetectedInternalGap && blocks.size >= 2) {
                val segList = mutableListOf<PeriodSegment>()
                for (i in blocks.indices) {
                    val b = blocks[i]
                    val bDays = ((b.endEpochMillis - b.startEpochMillis) / ONE_DAY_MS).toInt().coerceAtLeast(1)
                    segList += PeriodSegment(
                        title = "Fase Haid ${i + 1}",
                        startEpochMillis = b.startEpochMillis,
                        endEpochMillis = b.endEpochMillis,
                        status = "Haid Sah",
                        description = "Darah keluar selama $bDays hari dalam rentang siklus yang tidak melampaui 15 hari."
                    )
                    if (i < blocks.size - 1) {
                        val gStart = b.endEpochMillis
                        val gEnd = blocks[i + 1].startEpochMillis
                        val gDays = "%.1f".format((gEnd - gStart).toDouble() / ONE_DAY_MS).replace(".0", "")
                        segList += PeriodSegment(
                            title = "Jeda Bersih (Naqa' ${i + 1})",
                            startEpochMillis = gStart,
                            endEpochMillis = gEnd,
                            status = "Suci Sah (Naqa')",
                            description = "Jeda tanpa darah selama $gDays hari berstatus suci menurut Qaul Sahbi."
                        )
                    }
                }
                segList
            } else {
                listOf(haidSegment)
            }

            val shalatConsequence = "Seluruh shalat fardhu selama masa haid ini GUGUR dan TIDAK WAJIB DIQADHA. Shalat wajib kembali ditunaikan setelah darah berhenti dan mandi wajib."
            val puasaConsequence = "Puasa fardhu (Ramadhan) yang ditinggalkan selama masa haid WAJIB DIQADHA di hari lain di luar bulan Ramadhan."
            val mandiNote = "WAJIB MANDI BESAR (Mandi Jinabat Haid) setelah darah berhenti tuntas dengan niat: 'Nawaitul ghusla liraf'il hadatsil akbari minal haidhi fardhan lillahi ta'ala'."
            val advice = "Pastikan istirahat cukup, konsumsi makanan kaya zat besi, dan jaga hidrasi tubuh. Gunakan pembalut higienis dan ganti setiap 4-6 jam."

            val refs = listOf(
                KitabReference(
                    kitabName = "Uyunul Masa-il Linnisa' (Lirboyo)",
                    volumeAndPage = "Bab I, hal. 11 & Bab IV hal. 62",
                    ibaratSnippet = "وأكثره خمسة عشر يوما بلياليها وغالبه ست أو سبع... وحيث لم يجاوز الخمسة عشر فالكل حيض سحبا",
                    explanation = "Paling banyaknya masa haid adalah 15 hari 15 malam, dan lumrahnya 6 atau 7 hari. Selagi tidak melebihi 15 hari, maka seluruh masa dihukumi haid menurut Qaul Sahbi."
                ),
                KitabReference(
                    kitabName = "Tuhfatun Niswah",
                    volumeAndPage = "Fashl fi Ahkam al-Haidh, hal. 6-7",
                    ibaratSnippet = "Semua darah yang keluar dalam rentang 15 hari dan mencapai 24 jam adalah darah haid.",
                    explanation = "Kewajiban mandi wajib berlaku segera setelah darah berhenti bersih."
                )
            )

            return CalculationResult(
                statusSummary = summary,
                caseCategory = category,
                categoryDetectionReason = "Otomatis terdeteksi sebagai Haid Sah / Normal karena total keluarnya darah ($totalDays Hari $remainingHours Jam) memenuhi batas minimal 24 jam dan TIDAK melampaui batas maksimal haid 15 hari 15 malam menurut Mazhab Syafi'i.",
                haidOrNifasSegment = haidSegment,
                istihadhahSegment = null,
                suciSegment = null,
                shalatConsequence = shalatConsequence,
                qadhaPrayers = qadhaList,
                puasaConsequence = puasaConsequence,
                mandiWajibNote = mandiNote,
                mustahadhahCareGuide = null,
                medicalAndSpiritualAdvice = advice,
                references = refs,
                phaseBreakdowns = phaseBreakdowns + if (segments.size > 1) segments.map { "${it.title} (${formatDisplayRange(it.startEpochMillis, it.endEpochMillis)}): ${it.status}" } else emptyList(),
                allSegments = segments
            )
        }

        // =========================================================================
        // Kasus 4: ISTIHADHAH FIL-HAID (Darah Melebihi 15 Hari 15 Malam)
        // 7 Golongan Mustahadhah fil-Haid menurut Uyunul Masa-il Linnisa' Bab IV & Tuhfatun Niswah
        // =========================================================================
        val distinctScores = intervals.map { it.strengthScore }.distinct()
        val hasVariedCharacteristics = distinctScores.size > 1
        val isMubtadiah = !hasPreviousAdat || adatMemoryType == AdatMemoryType.BELUM_PERNAH_HAID || haidCategory == HaidCategory.MUBTADIAH_MUMAYYIZAH || haidCategory == HaidCategory.MUBTADIAH_GHAIRU_MUMAYYIZAH

        var isTamyizValid = false
        var tamyizCheck: TamyizCheckResult? = null
        var strongSegmentStart = startEpochMillis
        var strongSegmentEnd = startEpochMillis
        var weakSegmentStart = startEpochMillis
        var weakSegmentEnd = endEpochMillis

        if (hasVariedCharacteristics) {
            tamyizCheck = checkTamyiz(intervals)
            isTamyizValid = tamyizCheck.valid

            val evaluation = evaluateTamyizStrongPhases(intervals)
            val nonHaidPhases = intervals.filter { !evaluation.isHaidPhase(it) }

            if (isTamyizValid && evaluation.firstStrongDuration > 0L) {
                // Segmen haid mewakili darah kuat/sah pertama
                strongSegmentStart = evaluation.firstStrongStart
                strongSegmentEnd = evaluation.firstStrongEnd
                if (nonHaidPhases.isNotEmpty()) {
                    val firstNonHaid = nonHaidPhases.first()
                    weakSegmentStart = firstNonHaid.startEpochMillis
                    weakSegmentEnd = firstNonHaid.endEpochMillis
                }
            }
        }

        val tamyizPeriodResults = if (isTamyizValid) {
            val evaluation = evaluateTamyizStrongPhases(intervals)
            val strongest = intervals.maxOf { it.strengthScore }
            intervals.sortedBy { it.startEpochMillis }.map { phase ->
                val isHaid = evaluation.isHaidPhase(phase)
                val status = if (isHaid) FiqihStatus.HAID else FiqihStatus.ISTIHADHAH
                val reason = when {
                    status == FiqihStatus.HAID -> "Darah kuat/lemah yang sah dihukumi haid berdasarkan tamyiz."
                    phase.strengthScore == strongest -> "Darah kuat dihukumi istihadhah karena jarak pemisah dari akhir haid sah terakhir kurang dari 15 hari."
                    else -> "Darah lemah berdasarkan warna; pada pola tamyiz sah dihukumi istihadhah."
                }
                FiqihPeriodResult(
                    startEpochMillis = phase.startEpochMillis,
                    endEpochMillis = phase.endEpochMillis,
                    status = status,
                    reason = reason,
                    needsGhusl = status == FiqihStatus.ISTIHADHAH,
                    needsWudhu = status == FiqihStatus.ISTIHADHAH,
                    needsQadha = isMubtadiah &&
                        phase.startEpochMillis < minOf(startEpochMillis + FIFTEEN_DAYS_MS, endEpochMillis) &&
                        phase.endEpochMillis > startEpochMillis + ONE_DAY_MS
                )
            }
        } else emptyList()

        val haidDurationMs: Long
        val istihadhahDurationMs: Long
        val categoryName: String
        val categoryReason: String
        val shalatNote: String
        val mandiNote: String
        val haidSeg: PeriodSegment
        val istiSeg: PeriodSegment
        val customRefs = mutableListOf<KitabReference>()
        var customAdvice: String? = null

        // Evaluasi 7 Golongan:
        if (isTamyizValid && !isMubtadiah) {
            // Golongan 3 memiliki pengecualian: adat tidak selalu gugur.
            // Jika darah kuat baru muncul setelah jeda minimal 15 hari 15 malam,
            // bagian adat tetap dihukumi haid dan darah lemah di antaranya istihadhah.
            val rawResults = buildMutadahMumayyizahResults(startEpochMillis, endEpochMillis, intervals, adatDurationDays)
            val results = markFirstMonthMumayyizahQadha(
                rawResults,
                startEpochMillis,
                rawResults.firstOrNull { it.status == FiqihStatus.HAID }?.endEpochMillis ?: startEpochMillis
            )
            val haidResults = results.filter { it.status == FiqihStatus.HAID }
            val istiResults = results.filter { it.status == FiqihStatus.ISTIHADHAH }
            val firstHaid = haidResults.firstOrNull()
            val lastHaid = haidResults.lastOrNull()
            haidDurationMs = haidResults.sumOf { it.endEpochMillis - it.startEpochMillis }
            istihadhahDurationMs = istiResults.sumOf { it.endEpochMillis - it.startEpochMillis }

            val haidDays = haidDurationMs / ONE_DAY_MS
            val haidRemHours = (haidDurationMs % ONE_DAY_MS) / ONE_HOUR_MS
            val istiDays = istihadhahDurationMs / ONE_DAY_MS
            val istiRemHours = (istihadhahDurationMs % ONE_DAY_MS) / ONE_HOUR_MS

            categoryName = "3. Mu'tadah Mumayyizah"
            val summaryText = if (haidResults.size > 1) {
                "Haid Sah: $haidDays Hari $haidRemHours Jam (Adat & Tamyiz) • Istihadhah: $istiDays Hari $istiRemHours Jam"
            } else {
                "Haid Sah: $haidDays Hari $haidRemHours Jam (Tamyiz Mengalahkan Adat) • Istihadhah: $istiDays Hari $istiRemHours Jam"
            }

            categoryReason = if (haidResults.size > 1) {
                "Otomatis terdeteksi sebagai 3. Mu'tadah Mumayyizah: Tamyiz sah, tetapi adat haid tetap berlaku pada bagian awal karena darah kuat berikutnya terpisah minimal 15 hari 15 malam, sesuai contoh Mu'tadah Mumayyizah dalam Tuhfatun Niswah."
            } else {
                "Otomatis terdeteksi sebagai 3. Mu'tadah Mumayyizah karena: Anda sudah pernah haid sebelumnya (Mu'tadah, kebiasaan haid $adatDurationDays hari), darah keluar melampaui 15 hari ($totalDays Hari), dan terdapat perbedaan darah kuat ($haidDays Hari $haidRemHours Jam) serta darah lemah yang memenuhi 3 syarat Tamyiz sah. Berdasarkan kaidah fiqih Mazhab Syafi'i (Uyunul Masa-il Linnisa' hal. 78-79 & Tuhfatun Niswah hal. 27-29), hukum Tamyiz mengalahkan adat kebiasaan ($adatDurationDays hari). Darah kuat dihukumi haid sah, dan darah lemah dihukumi istihadhah."
            }

            val haidTitle = if (haidResults.size > 1) "Masa Haid Sah (Adat & Tamyiz)" else "Masa Haid Sah (Darah Kuat / Tamyiz)"
            val haidStart = firstHaid?.startEpochMillis ?: startEpochMillis
            val haidEnd = if (haidResults.size == 1) (firstHaid?.endEpochMillis ?: (startEpochMillis + haidDurationMs)) else (lastHaid?.endEpochMillis ?: (startEpochMillis + haidDurationMs))

            haidSeg = PeriodSegment(
                title = haidTitle,
                startEpochMillis = haidStart,
                endEpochMillis = haidEnd,
                status = "Haid Sah",
                description = if (haidResults.size > 1) {
                    "Darah kuat dan adat yang terpisah >= 15 hari dihukumi haid sah ($haidDays Hari $haidRemHours Jam)."
                } else {
                    "Darah kuat ($haidDays Hari $haidRemHours Jam) dihukumi haid sah karena hukum Tamyiz mengalahkan kebiasaan adat ($adatDurationDays hari)."
                }
            )
            istiSeg = PeriodSegment(
                title = "Fase Istihadhah (Darah Lemah)",
                startEpochMillis = istiResults.firstOrNull()?.startEpochMillis ?: endEpochMillis,
                endEpochMillis = istiResults.lastOrNull()?.endEpochMillis ?: endEpochMillis,
                status = "Istihadhah",
                description = "Darah lemah ($istiDays Hari $istiRemHours Jam) yang keluar sesudah darah kuat dihukumi istihadhah."
            )
            shalatNote = "Shalat pada masa darah kuat ($haidDays Hari $haidRemHours Jam) GUGUR. Shalat pada masa darah lemah yang sempat ditinggalkan sebelum batas hari ke-15 WAJIB DIQADHA. Selanjutnya wajib shalat tepat waktu sebagai mustahadhah."
            mandiNote = "Bulan pertama: Wajib mandi besar jinabat saat menyadari darah melampaui 15 hari, dan wajib mengqadha shalat sejak darah beralih ke lemah sampai batas 15 hari. Bulan kedua dan seterusnya: Wajib langsung mandi saat darah beralih dari kuat ke lemah, lalu shalat sebagai mustahadhah tanpa qadha (Tuhfatun Niswah hal. 37-38)."
            customRefs.add(KitabReference("Tuhfatun Niswah", "Penjelasan Mustahadhah Haid, Mu'tadah Mumayyizah", "Adat haid dapat tetap menjadi haid bila terpisah dari darah kuat minimal 15 hari 15 malam.", "Contoh adat 3 hari + 24 hari lemah + 3 hari kuat: 3 hari awal haid, 21 hari tengah istihadhah, 3 hari terakhir haid."))
            customRefs.add(KitabReference("Uyunul Masa-il Linnisa' (Lirboyo)", "Bab IV, hal. 78-79", "المعتادة المميزة ترد إلى التمييز لا إلى العادة", "Mu'tadah Mumayyizah dikembalikan kepada Tamyiz, bukan kepada adat kebiasaan (kecuali adat terpisah 15 hari dari darah kuat)."))
            customRefs.add(KitabReference("Nihayat al-Muhtaj ila Syarh al-Minhaj (ar-Ramli)", "Juz 1, hal. 341-342", "فإن رأت أسود ثم أحmer ثم أصفر... فالأسود ثم الأحمر حيض إن لم يجاوزا خمسة عشر يوما", "Aturan tiga tingkat: darah kuat dan tingkat kedua yang bersambung dengannya dihukumi haid selama total keduanya <= 15 hari."))
            customRefs.add(KitabReference("Hasyiyah al-Jamal 'ala Syarh al-Manhaj", "Juz 1, hal. 248", "وضابط المراتب الثلاث أن يخرج القوي ثم المتوسط ثم الضعيف فيكون القوي والمتوسط حيضا", "Kaidah maratib tsalatsah: darah kuat dan tingkat kedua yang bersambung menjadi haid."))
            customRefs.add(KitabReference("Tuhfat al-Muhtaj fi Syarh al-Minhaj (Ibnu Hajar al-Haitami)", "Juz 1, hal. 403 (Catatan Khilaf)", "وخالف ابن حجر في التحفة فيما إذا خرج الضعيف غير مناسب لقوته", "Catatan khilaf: Ibnu Hajar berbeda bila urutan kelemahan darah tidak runtut (hitam 5, kuning 5, merah terus)."))

            val segmentsList = mutableListOf<PeriodSegment>()
            segmentsList.add(haidSeg)
            if (istiResults.isNotEmpty()) {
                segmentsList.add(istiSeg)
            }
            if (haidResults.size > 1) {
                val secondHaid = haidResults.last()
                segmentsList.add(
                    PeriodSegment(
                        title = "Masa Haid Kedua (Darah Kuat)",
                        startEpochMillis = secondHaid.startEpochMillis,
                        endEpochMillis = secondHaid.endEpochMillis,
                        status = "Haid Sah",
                        description = "Darah kuat kedua sah sebagai haid baru karena terpisah >= 15 hari dari akhir haid sebelumnya."
                    )
                )
            }

            return CalculationResult(
                statusSummary = summaryText,
                caseCategory = categoryName,
                categoryDetectionReason = categoryReason,
                haidOrNifasSegment = haidSeg,
                istihadhahSegment = istiSeg,
                suciSegment = null,
                shalatConsequence = shalatNote,
                qadhaPrayers = qadhaList,
                puasaConsequence = "Puasa yang ditinggalkan saat darah kuat wajib diqadha di luar bulan Ramadhan.",
                mandiWajibNote = mandiNote,
                mustahadhahCareGuide = "Pada fase istihadhah, bersuci dan shalat sesuai ketentuan mustahadhah (bersihkan, sumbat, balut, wudhu tiap masuk waktu fardhu).",
                medicalAndSpiritualAdvice = "Darah haid Anda adalah darah kuat berdasarkan hukum Tamyiz. Hukum Tamyiz menduduki peringkat lebih tinggi daripada kebiasaan adat.",
                references = customRefs,
                phaseBreakdowns = phaseBreakdowns,
                periodResults = results,
                tamyizCheck = tamyizCheck,
                allSegments = segmentsList
            )
        } else if (isTamyizValid) {
            // Golongan 1: Mubtadi'ah Mumayyizah (tamyiz)
            haidDurationMs = tamyizPeriodResults.filter { it.status == FiqihStatus.HAID }.sumOf { it.endEpochMillis - it.startEpochMillis }
            istihadhahDurationMs = totalDurationMs - haidDurationMs

            val strongDays = haidDurationMs / ONE_DAY_MS
            val strongRemHours = (haidDurationMs % ONE_DAY_MS) / ONE_HOUR_MS
            val istiDays = istihadhahDurationMs / ONE_DAY_MS
            val istiRemHours = (istihadhahDurationMs % ONE_DAY_MS) / ONE_HOUR_MS

            if (isMubtadiah) {
                categoryName = "1. Mubtadi'ah Mumayyizah (Pemula dengan Tamyiz)"
                categoryReason = "Otomatis terdeteksi sebagai 1. Mubtadi'ah Mumayyizah karena: Anda baru pertama kali haid (Mubtadi'ah), darah keluar melebihi 15 hari ($totalDays Hari), dan terdapat perbedaan darah kuat ($strongDays Hari $strongRemHours Jam) serta darah lemah yang sah memenuhi 3 syarat Tamyiz menurut Mazhab Syafi'i (darah kuat dihukumi haid sah, darah lemah dihukumi istihadhah)."
            } else {
                categoryName = "3. Mu'tadah Mumayyizah (Pernah Haid & Tamyiz Mengalahkan Adat)"
                categoryReason = "Otomatis terdeteksi sebagai 3. Mu'tadah Mumayyizah karena: Anda sudah pernah haid sebelumnya (Mu'tadah), darah melebihi 15 hari ($totalDays Hari), dan darah Anda memenuhi 3 syarat Tamyiz sah. Berdasarkan kaidah fiqih kitab Uyunul Masa-il Linnisa' & Tuhfatun Niswah, hukum Tamyiz mengalahkan hukum adat bulan sebelumnya."
            }

            haidSeg = PeriodSegment(
                title = "Masa Haid Sah (Darah Kuat / Qawi)",
                startEpochMillis = strongSegmentStart,
                endEpochMillis = strongSegmentEnd,
                status = "Haid Sah",
                description = "Darah kuat ($strongDays Hari $strongRemHours Jam) dihukumi haid sah karena memenuhi 3 syarat Tamyiz menurut Mazhab Syafi'i."
            )

            istiSeg = PeriodSegment(
                title = "Masa Istihadhah (Darah Lemah / Dha'if)",
                startEpochMillis = weakSegmentStart,
                endEpochMillis = weakSegmentEnd,
                status = "Istihadhah",
                description = "Darah lemah ($istiDays Hari $istiRemHours Jam) dihukumi ISTIHADHAH. Wajib wudhu istibahah setiap shalat fardhu."
            )

            shalatNote = "Shalat di masa darah kuat ($strongDays Hari) GUGUR. Shalat di masa darah lemah yang sempat ditinggalkan sebelum hari ke-15 WAJIB DIQADHA. Selanjutnya wajib shalat tepat waktu."
            mandiNote = if (isMubtadiah) {
                "Bulan pertama: Wajib mandi besar sesudah hari ke-15 (saat menyadari darah melampaui 15 hari), dan wajib mengqadha shalat sejak darah berubah ke lemah sampai batas 15 hari. Bulan kedua dan seterusnya: Wajib segera mandi besar saat darah beralih dari kuat ke lemah, lalu langsung shalat sebagai mustahadhah tanpa utang shalat (Tuhfatun Niswah hal. 30)."
            } else {
                "Bulan pertama: Wajib mandi besar sesudah hari ke-15 (saat menyadari darah melampaui 15 hari), dan wajib mengqadha shalat sejak darah berubah ke lemah sampai batas 15 hari. Bulan kedua dan seterusnya: Wajib segera mandi besar saat darah beralih dari kuat ke lemah, lalu langsung shalat sebagai mustahadhah tanpa utang shalat (Tuhfatun Niswah hal. 37-38)."
            }

            val uyunulVolPage = if (isMubtadiah) "Bab IV, hal. 72" else "Bab IV, hal. 78-79"
            customRefs.add(
                KitabReference(
                    kitabName = "Uyunul Masa-il Linnisa' (Lirboyo)",
                    volumeAndPage = uyunulVolPage,
                    ibaratSnippet = "المميزة ترد إلى التمييز فالقوي حيض والضعيف استحاضة بشرط أن لا ينقص القوي عن يوم وليلة ولا يزيد على خمسة عشر يوما",
                    explanation = "Wanita Mumayyizah dikembalikan kepada Tamyiz: darah kuat adalah haid dan darah lemah adalah istihadhah, dengan syarat darah kuat tidak kurang dari 24 jam dan tidak lebih dari 15 hari."
                )
            )
            customRefs.add(
                KitabReference(
                    kitabName = "Tuhfatun Niswah",
                    volumeAndPage = "Fashl fi al-Istihadhah, hal. 27-29",
                    ibaratSnippet = "Bila darah melebihi 15 hari dan warnanya berbeda (kuat dan lemah), maka darah kuat adalah haid dan darah lemah adalah istihadhah.",
                    explanation = "Tamyiz didahulukan atas adat kebiasaan. Shalat yang tertinggal di masa darah lemah wajib diqadha."
                )
            )
            customRefs.add(
                KitabReference(
                    kitabName = "Nihayat al-Muhtaj ila Syarh al-Minhaj (ar-Ramli)",
                    volumeAndPage = "Juz 1, hal. 341-342",
                    ibaratSnippet = "فإن رأت أسود ثم أحمر ثم أصفر... فالأسود ثم الأحمر حيض إن لم يجاوزا خمسة عشر يوما",
                    explanation = "Aturan tiga tingkat sifat darah (qawi, mutawassith, dha'if): jika setelah darah paling kuat bersambung darah tingkat kedua dan jumlah keduanya tidak melebihi 15 hari, maka keduanya dihukumi haid. Darah tingkat ketiga ke bawah dihukumi istihadhah."
                )
            )
            customRefs.add(
                KitabReference(
                    kitabName = "Hasyiyah al-Jamal 'ala Syarh al-Manhaj",
                    volumeAndPage = "Juz 1, hal. 248",
                    ibaratSnippet = "وضابط المراتب الثلاث أن يخرج القوي ثم المتوسط ثم الضعيف فيكون القوي والمتوسط حيضا إن لم يجاوزا خمسة عشر يوما",
                    explanation = "Kaidah maratib tsalatsah (tiga tingkatan darah): darah kuat pertama dan darah sedang yang bersambung dengannya dihukumi haid selama total keduanya tidak melebihi 15 hari."
                )
            )
            customRefs.add(
                KitabReference(
                    kitabName = "Tuhfat al-Muhtaj fi Syarh al-Minhaj (Ibnu Hajar al-Haitami)",
                    volumeAndPage = "Juz 1, hal. 403 (Catatan Khilaf)",
                    ibaratSnippet = "وخالف ابن حجر في التحفة فيما إذا خرج الضعيف غير مناسب لقوته كأن خرج أسود خمسة ثم أصفر خمسة ثم استمر أحمر",
                    explanation = "Catatan: Ibnu Hajar dalam Tuhfat al-Muhtaj berbeda pada kasus darah lemah pertama yang tidak sepadan kekuatannya (hitam 5, kuning 5, merah terus), di mana menurutnya susunan harus runtut kekuatannya agar dapat digabung."
                )
            )
        } else if (isMubtadiah) {
            // Golongan 2: Mubtadi'ah Ghairu Mumayyizah
            // Haid = sehari semalam (24 jam), Suci = 29 hari.
            haidDurationMs = ONE_DAY_MS
            istihadhahDurationMs = totalDurationMs - haidDurationMs

            categoryName = "2. Mubtadi'ah Ghairu Mumayyizah (Pemula Tanpa Tamyiz)"
            categoryReason = "Otomatis terdeteksi sebagai 2. Mubtadi'ah Ghairu Mumayyizah karena: Anda baru pertama kali haid (Mubtadi'ah), darah melebihi 15 hari ($totalDays Hari), dan darah keluar seragam / tidak memenuhi syarat Tamyiz. Fiqih menetapkan haid Anda hanya 24 jam pertama, dan shalat 14 hari sisanya wajib diqadha."

            haidSeg = PeriodSegment(
                title = "Masa Haid Sah (Batas Minimal: 1 Hari 1 Malam)",
                startEpochMillis = startEpochMillis,
                endEpochMillis = startEpochMillis + ONE_DAY_MS,
                status = "Haid Sah",
                description = "Pemula tanpa tamyiz dikembalikan ke batas minimal haid: 1 hari 1 malam (24 jam)."
            )

            istiSeg = PeriodSegment(
                title = "Masa Istihadhah",
                startEpochMillis = startEpochMillis + ONE_DAY_MS,
                endEpochMillis = endEpochMillis,
                status = "Istihadhah",
                description = "Darah mulai jam ke-25 dan masa yang belum diketahui sebagai istihadhah dihukumi istihadhah. Pada bulan pertama, shalat hari ke-2 sampai ke-15 yang ditinggalkan WAJIB DIQADHA (14 hari)."
            )

            shalatNote = "PERINGATAN FIQIH: Haid Anda hanya 24 jam pertama. Shalat dari hari ke-2 sampai hari ke-15 yang kemarin ditinggalkan WAJIB DIQADHA (14 hari shalat fardhu). Mulai hari ke-16 wajib shalat tepat waktu."
            mandiNote = "Wajib mandi besar jinabat pada hari ke-15 (saat menyadari istihadhah), lalu segera mengqadha shalat 14 hari yang terutang."

            customRefs.add(
                KitabReference(
                    kitabName = "Uyunul Masa-il Linnisa' (Lirboyo)",
                    volumeAndPage = "Bab IV, hal. 76",
                    ibaratSnippet = "المبتدأة غير المميزة ترد إلى أقل الحيض وهو يوم وليلة وطهرها تسعة وعشرون يوما وتقضي صلاة أربعة عشر يوما",
                    explanation = "Mubtadi'ah Ghairu Mumayyizah dikembalikan kepada minimal haid yaitu sehari semalam (24 jam), sucinya 29 hari, dan WAJIB MENGQADHA SHALAT 14 HARI yang sempat ia tinggalkan."
                )
            )
            customRefs.add(
                KitabReference(
                    kitabName = "Tuhfatun Niswah",
                    volumeAndPage = "Hal. 29",
                    ibaratSnippet = "Haidnya sehari semalam dan sucinya 29 hari, wajib mengqadha shalat 14 hari.",
                    explanation = "Ketetapan hukum bagi wanita pemula yang darahnya satu warna dan melebihi 15 hari."
                )
            )
        } else if (adatMemoryType == AdatMemoryType.INGAT_LENGKAP_QADRAN_WAQTAN || haidCategory == HaidCategory.MUTADAH_GHAIRU_MUMAYYIZAH_DZAKIRAH_QADRAN_WAQTAN) {
            // Golongan 4: Mu'tadah Ghairu Mumayyizah Dzakirah Qadran wa Waqtan
            val validAdatDays = (adatDurationDays.takeIf { it in 1..15 } ?: 7).toLong()
            haidDurationMs = validAdatDays * ONE_DAY_MS
            istihadhahDurationMs = totalDurationMs - haidDurationMs

            categoryName = "4. Mu'tadah Ghairu Mumayyizah Dzakirah (Ingat Adat Lengkap)"
            categoryReason = "Otomatis terdeteksi sebagai 4. Mu'tadah Ghairu Mumayyizah Dzakirah karena: Anda pernah haid sebelumnya (Mu'tadah), darah keluar melebihi 15 hari tanpa tamyiz, dan Anda mengingat lengkap kebiasaan haid Anda ($validAdatDays hari). Haid ditetapkan sebesar adat bulan lalu ($validAdatDays hari), dan shalat dari hari ke-${validAdatDays + 1} s/d 15 wajib diqadha."

            haidSeg = PeriodSegment(
                title = "Masa Haid Sah (Mengikuti Adat: $validAdatDays Hari)",
                startEpochMillis = startEpochMillis,
                endEpochMillis = startEpochMillis + haidDurationMs,
                status = "Haid Sah",
                description = "Haid ditetapkan sesuai adat kebiasaan bulan sebelumnya yaitu $validAdatDays hari."
            )

            istiSeg = PeriodSegment(
                title = "Masa Istihadhah",
                startEpochMillis = startEpochMillis + haidDurationMs,
                endEpochMillis = endEpochMillis,
                status = "Istihadhah",
                description = "Darah hari ke-${validAdatDays + 1} s/d ke-15 dan seterusnya adalah ISTIHADHAH. Shalat hari ke-${validAdatDays + 1} s/d 15 WAJIB DIQADHA."
            )

            shalatNote = "Haid sah adalah $validAdatDays hari. Shalat dari hari ke-${validAdatDays + 1} sampai hari ke-15 yang sempat ditinggalkan WAJIB DIQADHA (${15 - validAdatDays} hari shalat fardhu)."
            mandiNote = "Wajib mandi besar jinabat pada hari ke-15, lalu segera mengqadha shalat yang ditinggalkan."

            customRefs.add(
                KitabReference(
                    kitabName = "Uyunul Masa-il Linnisa' (Lirboyo)",
                    volumeAndPage = "Bab IV, hal. 79-82",
                    ibaratSnippet = "المعتادة غير المميزة الذاكرة لقدر عادتها ووقتها ترد إلى قدر عادتها ووقتها وتقضي ما زاد على عادتها إلى خمسة عشر يوما",
                    explanation = "Mu'tadah Ghairu Mumayyizah yang ingat kadar dan waktu adatnya dikembalikan kepada adatnya, dan wajib mengqadha shalat di atas adatnya hingga 15 hari."
                )
            )
            customRefs.add(
                KitabReference(
                    kitabName = "Tuhfatun Niswah",
                    volumeAndPage = "Hal. 30",
                    ibaratSnippet = "Haidnya dikembalikan kepada adat bulan sebelumnya, selebihnya istihadhah dan shalatnya diqadha.",
                    explanation = "Adat menjadi penentu sah bagi wanita yang tidak bisa membedakan sifat darah."
                )
            )
        } else if (adatMemoryType == AdatMemoryType.LUPA_SEMUANYA_MUTAHAYYIRAH || haidCategory == HaidCategory.MUTADAH_NASIYAH_MUTAHAYYIRAH) {
            // Golongan 5: Mu'tadah Nasiyah (Mutahayyirah Mahdhah / Muthlaqah)
            haidDurationMs = 0L
            istihadhahDurationMs = 0L

            categoryName = "5. Mu'tadah Nasiyah (Mutahayyirah Mahdhah / Lupa Semuanya)"
            categoryReason = "Otomatis terdeteksi sebagai 5. Mu'tadah Nasiyah (Mutahayyirah Mahdhah) karena: Anda pernah haid sebelumnya, darah melebihi 15 hari tanpa tamyiz, dan Anda lupa sama sekali durasi maupun waktu jatuhnya haid. Anda berstatus Mutahayyirah dan wajib berhati-hati (ihtiyath) dengan bersuci dan shalat di tiap waktu fardhu."

            haidSeg = PeriodSegment(
                title = "Masa Ihtiyath Mutahayyirah",
                startEpochMillis = startEpochMillis,
                endEpochMillis = endEpochMillis,
                status = "Ihtiyath",
                description = "Karena kadar dan waktu adat terlupakan, sumber menetapkan hukum ihtiyath; tidak dipaksakan menjadi haid 24 jam."
            )

            istiSeg = PeriodSegment(
                title = "Kewajiban Ibadah Mutahayyirah",
                startEpochMillis = startEpochMillis,
                endEpochMillis = endEpochMillis,
                status = "Istihadhah Mutahayyirah",
                description = "Wajib bersuci (mandi/wudhu) setiap masuk waktu shalat fardhu dan tetap wajib shalat serta puasa."
            )

            shalatNote = "Wajib IHTIYATH: Tetap wajib shalat lima waktu dengan bersuci setiap masuk waktu fardhu. Dilarang jima' dan tidak membaca Al-Qur'an di luar shalat."
            mandiNote = "Wajib mandi setiap kali masuk waktu shalat fardhu apabila tidak mengingat waktu berhentinya haid, sebagaimana penjelasan mustahadhah mutahayyirah dalam Tuhfatun Niswah."
            customAdvice = "Kasus Mutahayyirah Mahdhah berlandaskan kaidah ihtiyath. Sangat dianjurkan berkonsultasi langsung dengan ustadz/ustadzah ahli fiqih wanita untuk kepastian ibadah."

            customRefs.add(
                KitabReference(
                    kitabName = "Uyunul Masa-il Linnisa' (Lirboyo)",
                    volumeAndPage = "Bab IV, hal. 83-86",
                    ibaratSnippet = "المتحيرة المحضة وهي التي نسيت قدر عادتها ووقتها تصلي وتصوم احتياطا وتغتسل لكل فرض ولا يحل لزوجها وطؤها",
                    explanation = "Mutahayyirah Mahdhah (lupa waktu dan kadar) wajib shalat dan puasa atas dasar ihtiyath, mandi untuk tiap shalat fardhu, dan diharamkan jima'."
                )
            )
            customRefs.add(
                KitabReference(
                    kitabName = "Tuhfatun Niswah",
                    volumeAndPage = "Hal. 33-35",
                    ibaratSnippet = "Mutahayyirah wajib bersikap ihtiyath dalam semua ibadah.",
                    explanation = "Kaidah kehati-hatian karena ketidakpastian status hadats."
                )
            )
        } else if (adatMemoryType == AdatMemoryType.INGAT_QADRAN_LUPA_WAQTAN || haidCategory == HaidCategory.MUTADAH_DZAKIRAH_QADRAN_DUNAN_WAQT) {
            // =========================================================================
            // GOLONGAN 6: Mu'tadah Ghairu Mumayyizah (Dzakirah li 'Adatiha Qadran La Waqtan)
            // (المعتادة غير المميزة الذاكرة لعادتها قدراً لا وقتاً)
            // Menggunakan algoritma Fiqih Mazhab Syafi'i: CANDIDATE INTERVALS
            // =========================================================================
            val pureDays = category6PureDays ?: if (rememberedCertainPureDayOfMonth != null) setOf(rememberedCertainPureDayOfMonth) else emptySet()
            val haidDays = category6HaidDays ?: if (rememberedCertainHaidDayOfMonth != null) setOf(rememberedCertainHaidDayOfMonth) else emptySet()
            val monthLen = category6MonthLength.coerceIn(1, 31)
            val endWindow = category6WindowEnd ?: monthLen

            val cat6Calc = if (category6WindowStart > monthLen) {
                Category6CalculationResult(
                    isValid = false,
                    errorMessage = "Awal rentang posisi ($category6WindowStart) tidak boleh melebihi jumlah hari dalam bulan ($monthLen hari).",
                    monthLength = monthLen,
                    habitDurationDays = adatDurationDays,
                    positionWindowStart = category6WindowStart,
                    positionWindowEnd = endWindow
                )
            } else {
                Category6Calculator.calculateCategory6(
                    monthLength = monthLen,
                    habitDurationDays = adatDurationDays,
                    positionWindowStart = category6WindowStart.coerceAtLeast(1),
                    positionWindowEnd = endWindow.coerceIn(category6WindowStart, monthLen),
                    certainPureDays = pureDays,
                    certainHaidDays = haidDays
                )
            }

            categoryName = "6. Mu'tadah Ghairu Mumayyizah (Dzakirah li 'Adatiha Qadran La Waqtan)"

            if (!cat6Calc.isValid) {
                categoryReason = cat6Calc.errorMessage ?: "Terjadi pertentangan pada data hari yang diyakini."
                return CalculationResult(
                    statusSummary = "Kesalahan Input: ${cat6Calc.errorMessage}",
                    caseCategory = categoryName,
                    categoryDetectionReason = categoryReason,
                    haidOrNifasSegment = null,
                    istihadhahSegment = null,
                    suciSegment = null,
                    shalatConsequence = "Perbaiki rentang posisi dan hari yakin suci/haid agar tidak terjadi kontradiksi kemungkinan posisi haid.",
                    qadhaPrayers = emptyList(),
                    puasaConsequence = "Belum dapat dihitung karena terjadi kontradiksi kemungkinan posisi haid.",
                    mandiWajibNote = "Belum dapat dihitung.",
                    mustahadhahCareGuide = "Seluruh kemungkinan interval haid tereliminasi karena hari yang dimasukkan saling bertentangan.",
                    medicalAndSpiritualAdvice = "Konsultasikan data haid dan suci Anda dengan ahli fiqih wanita.",
                    references = customRefs,
                    phaseBreakdowns = phaseBreakdowns,
                    periodResults = emptyList(),
                    tamyizCheck = tamyizCheck,
                    category6Calculation = cat6Calc
                )
            }

            // Petakan periode ke FiqihPeriodResult dengan timestamp
            val results = cat6Calc.periodResults.map { p ->
                val pStart = startEpochMillis + (p.startDay - 1) * ONE_DAY_MS
                val pEnd = startEpochMillis + p.endDay * ONE_DAY_MS
                val status = when (p.status) {
                    FiqhPeriodStatus.HAID_YAKIN -> FiqihStatus.HAID
                    FiqhPeriodStatus.SUCI_YAKIN -> FiqihStatus.ISTIHADHAH
                    FiqhPeriodStatus.SYAK_HAID_Suci,
                    FiqhPeriodStatus.SYAK_HAID_Suci_PUTUS -> FiqihStatus.IHTIYATH
                }
                FiqihPeriodResult(
                    startEpochMillis = pStart,
                    endEpochMillis = minOf(pEnd, maxOf(pEnd, endEpochMillis)),
                    status = status,
                    reason = p.explanation,
                    needsGhusl = p.ihtiyatAction == IhtiyatAction.GHUSL_SETIAP_FARDHU,
                    needsWudhu = p.status != FiqhPeriodStatus.HAID_YAKIN,
                    needsQadha = false
                )
            }

            val haidPeriod = cat6Calc.periodResults.firstOrNull { it.status == FiqhPeriodStatus.HAID_YAKIN }
            haidSeg = if (haidPeriod != null) {
                val hStart = startEpochMillis + (haidPeriod.startDay - 1) * ONE_DAY_MS
                val hEnd = startEpochMillis + haidPeriod.endDay * ONE_DAY_MS
                val label = if (haidPeriod.startDay == haidPeriod.endDay) "Hari ke-${haidPeriod.startDay}" else "Hari ke-${haidPeriod.startDay}–${haidPeriod.endDay}"
                PeriodSegment("Haid Yakin ($label)", hStart, hEnd, "Haid", "Hari $label berada pada seluruh kandidat posisi haid sehingga dihukumi yakin haid.")
            } else {
                PeriodSegment("Tidak Ada Fase Haid Yakin", startEpochMillis, startEpochMillis, "Ihtiyath", "Tidak ada hari yang menjadi irisan seluruh kemungkinan posisi haid.")
            }

            val suciPeriods = cat6Calc.periodResults.filter { it.status == FiqhPeriodStatus.SUCI_YAKIN }
            istiSeg = if (suciPeriods.isNotEmpty()) {
                val firstSuci = suciPeriods.first()
                val sStart = startEpochMillis + (firstSuci.startDay - 1) * ONE_DAY_MS
                val sEnd = startEpochMillis + firstSuci.endDay * ONE_DAY_MS
                PeriodSegment("Zona Suci Yakin", sStart, sEnd, "Istihadhah", "Zona di luar seluruh kemungkinan posisi haid, dihukumi suci yakin.")
            } else {
                PeriodSegment("Ihtiyath", startEpochMillis, endEpochMillis, "Ihtiyath", "Tidak ada zona suci yakin pada rentang yang dimasukkan.")
            }

            categoryReason = "Durasi kebiasaan haid $adatDurationDays hari diingat, posisi awal dilupakan. Dihitung menggunakan metode matematis Candidate Intervals (irisan seluruh kemungkinan posisi haid) sesuai kaidah Fiqih Mazhab Syafi'i (Tuhfatun Niswah)."

            val summaryParts = cat6Calc.periodResults.map { p ->
                val dLabel = if (p.startDay == p.endDay) "Hari ${p.startDay}" else "Hari ${p.startDay}–${p.endDay}"
                when (p.status) {
                    FiqhPeriodStatus.HAID_YAKIN -> "$dLabel: Haid Yakin"
                    FiqhPeriodStatus.SUCI_YAKIN -> "$dLabel: Suci Yakin"
                    FiqhPeriodStatus.SYAK_HAID_Suci -> "$dLabel: Syak (Wudhu)"
                    FiqhPeriodStatus.SYAK_HAID_Suci_PUTUS -> "$dLabel: Syak Inqitha' (Mandi)"
                }
            }
            val summaryText = "Golongan 6: " + summaryParts.joinToString(", ")

            shalatNote = buildString {
                append("• Hari Haid Yakin: Shalat haram dan gugur.\n")
                append("• Hari Suci Yakin: Wajib shalat seperti biasa.\n")
                append("• Hari Syak Haid & Suci (tanpa inqitha'): Wajib wudhu setiap masuk fardhu, tetap wajib shalat dan puasa atas dasar ihtiyath.\n")
                append("• Hari Syak & Kemungkinan Inqitha': Wajib mandi besar (ghusl) setiap masuk fardhu, lalu berwudhu, serta wajib shalat dan puasa atas dasar ihtiyath.")
            }

            val hasGhusl = cat6Calc.periodResults.any { it.ihtiyatAction == IhtiyatAction.GHUSL_SETIAP_FARDHU }
            mandiNote = if (hasGhusl) {
                val ghuslDays = cat6Calc.periodResults.filter { it.ihtiyatAction == IhtiyatAction.GHUSL_SETIAP_FARDHU }
                    .joinToString(", ") { if (it.startDay == it.endDay) "Hari ${it.startDay}" else "Hari ${it.startDay}–${it.endDay}" }
                "Wajib mandi besar (ghusl) untuk setiap shalat fardhu pada rentang $ghuslDays karena ada potensi saat itu darah baru berhenti (inqitha'), sebagaimana kaidah Fiqih Syafi'i."
            } else {
                "Tidak ada kewajiban mandi fardhu berkala selama belum masuk kemungkinan berakhirnya masa adat haid."
            }

            customRefs.add(KitabReference(
                "Tuhfatun Niswah (Syaikh Muhammad bin Ahmad As-Syathiri)",
                "Hal. 47-48",
                "الصورة السادسة: هي الذاكرة لعادتها قدراً لا وقتاً... كان حيضي خمسة في العشر الأول من الشهر لا أعلم ابتداءها وأعلم أني في اليوم الأول طاهرة بيقين: فالسادس حيض بيقين، والأول طهر بيقين، والثاني إلى آخر الخامس محتمل للحيض والطهر دون الانقطاع، والسابع إلى آخر العاشر محتمل للحيض والطهر والانقطاع، ومن الحادي عشر إلى آخر الشهر طهر بيقين.",
                "Contoh standar Tuhfatun Niswah: adat 5 hari dalam 10 hari pertama, hari 1 yakin suci -> hari 1 suci yakin, hari 2-5 syak tanpa inqitha' (wudhu), hari 6 haid yakin (irisan semua kandidat), hari 7-10 syak dengan inqitha' (mandi tiap fardhu), hari 11-akhir suci yakin."
            ))
            customRefs.add(KitabReference(
                "Uyunul Masa-il Linnisa' (Lirboyo)",
                "Bab IV, hal. 87-88",
                "المعتادة الذاكرة لقدر عادتها الناسي لوقتها تجعل حيضها في زمن الإمكان بحسب ما تتيقنه وتحتاط في المحتمل",
                "Mu'tadah yang mengingat durasi kebiasaannya namun lupa waktunya: waktu yang dipastikan haid dihukumi haid, waktu yang dipastikan suci dihukumi suci, dan waktu yang diragukan diamalkan dengan kaidah ihtiyath."
            ))

            return CalculationResult(
                statusSummary = summaryText,
                caseCategory = categoryName,
                categoryDetectionReason = categoryReason,
                haidOrNifasSegment = haidSeg,
                istihadhahSegment = istiSeg,
                suciSegment = null,
                shalatConsequence = shalatNote,
                qadhaPrayers = qadhaList,
                puasaConsequence = "Puasa haram pada hari haid yakin, sah pada hari suci yakin, dan wajib dijalankan atas dasar ihtiyath pada hari syak (dengan qadha' di kemudian hari sesuai kaidah mutahayyirah).",
                mandiWajibNote = mandiNote,
                mustahadhahCareGuide = "Algoritma Candidate Intervals memetakan status setiap hari secara objektif berdasarkan informasi yang Anda ingat, tanpa menebak atau memaksakan tanggal mulai haid.",
                medicalAndSpiritualAdvice = "Kasus Golongan 6 (Dzakirah Qadran La Waqtan) berlandaskan kehati-hatian (ihtiyath). Konsultasikan langsung kepada ustadz/ustadzah ahli fiqih wanita.",
                references = customRefs,
                phaseBreakdowns = phaseBreakdowns,
                periodResults = results,
                tamyizCheck = tamyizCheck,
                category6Calculation = cat6Calc
            )
        } else {
            // Golongan 7: Mu'tadah Dzakirah lil-Waqti dunan Qadr (Ingat Waktu Saja, Lupa Kadar Durasi)
            val anchorHaidDay = rememberedCertainHaidDayOfMonth
            if (anchorHaidDay == null) {
                return CalculationResult(
                    statusSummary = "Tanggal mulai haid belum dipilih",
                    caseCategory = "7. Mu'tadah Dzakirah lil-Waqti dunan Qadr (Ingat Waktu Saja)",
                    categoryDetectionReason = "Tanggal mulai haid belum dipilih. Silakan pilih tanggal atau hari mulai haid yang Anda ingat.",
                    haidOrNifasSegment = null,
                    istihadhahSegment = null,
                    suciSegment = null,
                    shalatConsequence = "Tanggal mulai haid belum dipilih.",
                    qadhaPrayers = emptyList(),
                    puasaConsequence = "Tanggal mulai haid belum dipilih.",
                    mandiWajibNote = "Tanggal mulai haid belum dipilih.",
                    mustahadhahCareGuide = "Harap tentukan tanggal mulai haid yang Anda ingat terlebih dahulu.",
                    medicalAndSpiritualAdvice = "Pilih tanggal mulai haid untuk menghitung masa haid yakin, ihtiyath, dan suci.",
                    references = customRefs,
                    periodResults = emptyList()
                )
            }
            val results = buildWaqtOnlyPeriodResults(
                startEpochMillis = startEpochMillis,
                endEpochMillis = endEpochMillis,
                certainHaidDayOfMonth = anchorHaidDay
            )

            if (results.isNotEmpty()) {
                val haidResults = results.filter { it.status == FiqihStatus.HAID }
                val istiResults = results.filter { it.status == FiqihStatus.ISTIHADHAH || it.status == FiqihStatus.SUCI }
                val ihtiyathResults = results.filter { it.status == FiqihStatus.IHTIYATH }
                haidDurationMs = haidResults.sumOf { it.endEpochMillis - it.startEpochMillis }
                istihadhahDurationMs = istiResults.sumOf { it.endEpochMillis - it.startEpochMillis }
                categoryName = "7. Mu'tadah Dzakirah lil-Waqti dunan Qadr (Ingat Waktu Saja)"
                categoryReason = "Waktu mulai haid diingat (tanggal $anchorHaidDay), namun kadar durasi kebiasaan dilupakan. Sesuai fiqih Mazhab Syafi'i (Uyunul Masa-il Linnisa' hal. 89 & Tuhfatun Niswah hal. 49): 24 jam pertama (tanggal $anchorHaidDay) dihukumi Haid Yakin karena batas minimal haid adalah sehari semalam; hari ke-2 sampai ke-15 berada pada kemungkinan haid/suci dan kemungkinan terputusnya haid sehingga dihukumi IHTIYATH (wajib shalat, puasa, dan mandi untuk setiap fardhu); hari ke-16 ke atas dihukumi Suci Yakin / Istihadhah murni."
                
                haidSeg = haidResults.firstOrNull()?.let {
                    PeriodSegment("Tanggal $anchorHaidDay: Haid Yakin", it.startEpochMillis, it.endEpochMillis, "Haid", "Hari pertama (24 jam) dihukumi haid yakin karena waktu mulai diingat dan batas minimal haid adalah sehari semalam.")
                } ?: PeriodSegment("Hari 1: Haid Yakin", startEpochMillis, minOf(startEpochMillis + ONE_DAY_MS, endEpochMillis), "Haid", "Hari pertama dihukumi haid yakin.")
                
                istiSeg = ihtiyathResults.firstOrNull()?.let {
                    PeriodSegment("Hari 2-15: Masa Ihtiyath", it.startEpochMillis, minOf(it.startEpochMillis + 14 * ONE_DAY_MS, endEpochMillis), "Ihtiyath", "Hari ke-2 sampai 15 sejak tanggal mulai berstatus kehati-hatian (ihtiyath).")
                } ?: PeriodSegment("Ihtiyath", startEpochMillis, endEpochMillis, "Ihtiyath", "Zona kehati-hatian.")
                
                shalatNote = "Hari ke-1 (tanggal $anchorHaidDay): Haid yakin (shalat gugur, haram puasa). Hari ke-2 sampai 15: Wajib IHTIYATH (shalat dan puasa tetap wajib, wajib bersuci dan mandi setiap kali masuk waktu shalat fardhu). Hari ke-16 ke atas: Suci yakin / Istihadhah murni (wajib shalat & puasa, cukup wudhu istihadhah)."
                mandiNote = "Wajib mandi besar setiap kali masuk waktu shalat fardhu pada hari ke-2 sampai ke-15 (masa ihtiyath) karena setiap waktu dimungkinkan sebagai saat terputusnya darah haid."
                
                customRefs.add(KitabReference(
                    kitabName = "Uyunul Masa-il Linnisa' (Lirboyo)",
                    volumeAndPage = "Bab IV, hal. 89",
                    ibaratSnippet = "الذاكرة للوقت دون القدر: كأن تذكر أنها كانت تحيض في أول الشهر ولا تدري كم يوماً. فيكون اليوم الأول حيضاً بيقين... وما بين اليوم الثاني إلى الخامس عشر محتملاً للحيض والطهر وانقطاع الحيض فتحتاط فيه وتغتسل لكل فرض، ومن السادس عشر إلى آخر الشهر طهراً بيقين",
                    explanation = "Wanita yang ingat waktu mulai tetapi lupa kadar durasi: hari ke-1 haid yakin, hari ke-2 s/d 15 ihtiyath (mandi untuk tiap fardhu), hari ke-16 ke atas suci yakin."
                ))
                customRefs.add(KitabReference(
                    kitabName = "Tuhfatun Niswah",
                    volumeAndPage = "Hal. 49",
                    ibaratSnippet = "Tanggal 1 yakin haid; tanggal 2-15 ihtiyath; tanggal 16 sampai akhir bulan yakin suci.",
                    explanation = "Pemetaan waktu mulai haid yang diingat: 24 jam pertama haid sah, 14 hari berikutnya ihtiyath, selebihnya istihadhah."
                ))

                val haidDays = haidDurationMs / ONE_DAY_MS
                val haidRemHours = (haidDurationMs % ONE_DAY_MS) / ONE_HOUR_MS
                val istiDays = istihadhahDurationMs / ONE_DAY_MS
                val istiRemHours = (istihadhahDurationMs % ONE_DAY_MS) / ONE_HOUR_MS

                return CalculationResult(
                    statusSummary = "Haid Yakin: Tanggal $anchorHaidDay (24 Jam) | Hari 2–15: Ihtiyath | Hari 16+: Suci Yakin / Istihadhah",
                    caseCategory = categoryName,
                    categoryDetectionReason = categoryReason,
                    haidOrNifasSegment = haidSeg,
                    istihadhahSegment = istiSeg,
                    suciSegment = null,
                    shalatConsequence = shalatNote,
                    qadhaPrayers = qadhaList,
                    puasaConsequence = "Puasa pada hari ke-1 (haid yakin) haram dan wajib diqadha. Puasa pada hari 2–15 wajib dijalankan atas dasar ihtiyath (diqadha sesuai kaidah fiqih). Puasa hari ke-16 ke atas sah dan wajib.",
                    mandiWajibNote = mandiNote,
                    mustahadhahCareGuide = "Pada masa ihtiyath (hari 2–15), wajib bersuci/mandi untuk tiap shalat fardhu dan berwudhu istihadhah setelah masuk waktu.",
                    medicalAndSpiritualAdvice = "Pola tanggal mulai diingat ($anchorHaidDay). Kasus Mutahayyirah berlandaskan ihtiyath; dianjurkan berkonsultasi ke ustadz/ustadzah ahli fiqih wanita.",
                    references = customRefs,
                    phaseBreakdowns = phaseBreakdowns,
                    periodResults = results,
                    tamyizCheck = tamyizCheck
                )
            }

            val validAdatDays = 1L
            haidDurationMs = minOf(ONE_DAY_MS, totalDurationMs)
            istihadhahDurationMs = (totalDurationMs - haidDurationMs).coerceAtLeast(0L)

            categoryName = "7. Mu'tadah Dzakirah lil-Waqti dunan Qadr (Ingat Waktu Saja)"
            categoryReason = "Otomatis terdeteksi sebagai 7. Mu'tadah Dzakirah lil-Waqti dunan Qadr karena waktu mulai haid diingat tetapi kadar/durasi adat dilupakan. Hari 1 yakin haid, hari 2-15 mungkin haid/suci dan mungkin mulai putusnya haid sehingga ihtiyath, sedangkan hari 16 ke atas yakin suci."

            haidSeg = PeriodSegment(
                title = "Hari 1: Haid Yakin",
                startEpochMillis = startEpochMillis,
                endEpochMillis = startEpochMillis + haidDurationMs,
                status = "Haid Yakin",
                description = "Karena waktu mulai diingat tetapi kadar adat dilupakan, hari pertama dihukumi haid yakin; hari 2-15 ihtiyath; hari 16 ke atas yakin suci."
            )

            istiSeg = PeriodSegment(
                title = "Hari 2-15: Ihtiyath",
                startEpochMillis = startEpochMillis + haidDurationMs,
                endEpochMillis = minOf(startEpochMillis + FIFTEEN_DAYS_MS, endEpochMillis),
                status = "Ihtiyath",
                description = "Hari 2 sampai 15 berada pada kemungkinan haid/suci dan kemungkinan mulai putusnya haid; wajib ihtiyath."
            )

            shalatNote = "Hari 1 dihukumi haid yakin. Hari 2-15 wajib ihtiyath seperti mustahadhah mutahayyirah. Hari 16 sampai akhir bulan yakin suci dan diperlakukan seperti perempuan suci."
            mandiNote = "Mandi wajib dilakukan pada hari yang dimungkinkan sebagai akhir dari putusnya darah haid adat."

            customRefs.add(
                KitabReference(
                    kitabName = "Uyunul Masa-il Linnisa' (Lirboyo)",
                    volumeAndPage = "Bab IV, hal. 89",
                    ibaratSnippet = "Tanggal 1 yakin haid; tanggal 2-15 ihtiyath; tanggal 16 sampai akhir bulan yakin suci.",
                    explanation = "Wanita yang ingat waktu mulai tetapi lupa kadar adat: hari pertama haid yakin, hari 2-15 ihtiyath, dan hari 16 ke atas yakin suci."
                )
            )
        }

        val haidDays = haidDurationMs / ONE_DAY_MS
        val haidRemHours = (haidDurationMs % ONE_DAY_MS) / ONE_HOUR_MS
        val istiDays = istihadhahDurationMs / ONE_DAY_MS
        val istiRemHours = (istihadhahDurationMs % ONE_DAY_MS) / ONE_HOUR_MS

        val summary = "Haid Sah: $haidDays Hari $haidRemHours Jam | Istihadhah: $istiDays Hari $istiRemHours Jam"

        val mustahadhahGuide = """
            TATA CARA THOHAROH & SHALAT BAGI MUSTAHADHAH (DA'IMUL HADATS):
            1. Bersihkan kemaluan dari najis (istinja').
            2. Sumbat kemaluan dengan kapas/kain (hasywu), KECUALI sedang puasa Ramadhan (karena memasukkan benda ke rongga membatalkan puasa).
            3. Balut dengan pembalut/celana ketat ('ashbu) agar darah terminimalisir.
            4. Berwudhu SETELAH MASUK WAKTU SHALAT FARDHU dengan niat istibahah:
               نَوَيْتُ الْوُضُوْءَ لِاسْتِبَاحَةِ الصَّلَاةِ فَرْضًا لِلَّهِ تَعَالَى
               ('Nawaitul wudhu'a li-istibaahatish sholaati fardhan lillahi ta'ala').
            5. Bersegera menunaikan shalat fardhu (satu wudhu berlaku untuk 1 shalat fardhu dan boleh banyak shalat sunnah).
        """.trimIndent()

        val medicalAdvice = "Pendarahan yang melebihi 15 hari secara medis tergolong Pendarahan Uterus Abnormal (Abnormal Uterine Bleeding / AUB). Hal ini bisa disebabkan oleh ketidakseimbangan hormon, polip, kista, efek alat kontrasepsi (IUD/KB), atau faktor stres fisik. Sangat dianjurkan berkonsultasi dengan dokter spesialis obstetri & ginekologi (Sp.OG) untuk pemeriksaan USG dan evaluasi kadar hemoglobin (HB)."

        return CalculationResult(
            statusSummary = summary,
            caseCategory = categoryName,
            categoryDetectionReason = categoryReason,
            haidOrNifasSegment = haidSeg,
            istihadhahSegment = istiSeg,
            suciSegment = null,
            shalatConsequence = shalatNote,
            qadhaPrayers = qadhaList,
            puasaConsequence = "Puasa selama hari-hari haid sah wajib diqadha di luar bulan Ramadhan. Puasa selama masa istihadhah tetap sah dan wajib dijalankan.",
            mandiWajibNote = mandiNote,
            mustahadhahCareGuide = mustahadhahGuide,
            medicalAndSpiritualAdvice = customAdvice ?: medicalAdvice,
            references = customRefs,
            phaseBreakdowns = phaseBreakdowns,
            periodResults = when {
                tamyizPeriodResults.isNotEmpty() && isMubtadiah -> markFirstMonthMumayyizahQadha(
                    tamyizPeriodResults,
                    startEpochMillis,
                    strongSegmentEnd
                )
                tamyizPeriodResults.isNotEmpty() -> tamyizPeriodResults
                adatMemoryType == AdatMemoryType.INGAT_LENGKAP_QADRAN_WAQTAN || haidCategory == HaidCategory.MUTADAH_GHAIRU_MUMAYYIZAH_DZAKIRAH_QADRAN_WAQTAN -> listOf(
                    FiqihPeriodResult(startEpochMillis, minOf(startEpochMillis + adatDurationDays.coerceIn(1, 15) * ONE_DAY_MS, endEpochMillis), FiqihStatus.HAID, "Mengikuti adat haid yang diingat.", false, false, false),
                    FiqihPeriodResult(minOf(startEpochMillis + adatDurationDays.coerceIn(1, 15) * ONE_DAY_MS, endEpochMillis), endEpochMillis, FiqihStatus.ISTIHADHAH, "Sisa masa darah di luar adat dihukumi istihadhah.", true, true, false)
                ).filter { it.endEpochMillis > it.startEpochMillis }
                adatMemoryType == AdatMemoryType.LUPA_SEMUANYA_MUTAHAYYIRAH || haidCategory == HaidCategory.MUTADAH_NASIYAH_MUTAHAYYIRAH -> listOf(
                    FiqihPeriodResult(startEpochMillis, endEpochMillis, FiqihStatus.IHTIYATH, "Mutahayyirah: status haid tidak dipaksakan tanpa penentuan waktu/kadar adat; ibadah dilakukan atas dasar ihtiyath.", true, true, false)
                )
                adatMemoryType == AdatMemoryType.INGAT_QADRAN_LUPA_WAQTAN || haidCategory == HaidCategory.MUTADAH_DZAKIRAH_QADRAN_DUNAN_WAQT -> listOf(
                    FiqihPeriodResult(startEpochMillis, minOf(startEpochMillis + FIFTEEN_DAYS_MS, endEpochMillis), FiqihStatus.IHTIYATH, "Kadar adat diingat namun waktu mulai dilupakan. Hari 1 s/d 15 berstatus ihtiyath.", true, true, false),
                    FiqihPeriodResult(minOf(startEpochMillis + FIFTEEN_DAYS_MS, endEpochMillis), endEpochMillis, FiqihStatus.ISTIHADHAH, "Hari ke-16 ke atas yakin suci/istihadhah.", false, false, false)
                ).filter { it.endEpochMillis > it.startEpochMillis }
                haidCategory == HaidCategory.MUTADAH_DZAKIRAH_WAQTAN_DUNAN_QADR || adatMemoryType == AdatMemoryType.INGAT_WAQTAN_LUPA_QADRAN -> listOf(
                    FiqihPeriodResult(startEpochMillis, minOf(startEpochMillis + ONE_DAY_MS, endEpochMillis), FiqihStatus.HAID, "Hari 1 yakin haid karena waktu mulai diingat.", false, false, false),
                    FiqihPeriodResult(startEpochMillis + ONE_DAY_MS, minOf(startEpochMillis + FIFTEEN_DAYS_MS, endEpochMillis), FiqihStatus.IHTIYATH, "Hari 2-15 mungkin haid, suci, dan mungkin mulai putusnya haid; wajib ihtiyath.", true, true, false),
                    FiqihPeriodResult(minOf(startEpochMillis + FIFTEEN_DAYS_MS, endEpochMillis), endEpochMillis, FiqihStatus.SUCI, "Hari 16 ke atas yakin suci menurut Tuhfatun Niswah.", false, false, false)
                ).filter { it.endEpochMillis > it.startEpochMillis }
                else -> emptyList()
            },
            tamyizCheck = tamyizCheck
        )
    }

    // =========================================================================
    // PERHITUNGAN NIFAS (Tuhfatun Niswah + Uyunul Masa'il Linnisa')
    // Fokus V2.0: 5 golongan utama + 2 catatan, kronologi darah, dan warna-only.
    // =========================================================================
    /**
     * Menghitung kasus Mustahadhah Kategori 6 (Dzakirah li 'Adatiha Qadran La Waqtan)
     * menggunakan metode CANDIDATE INTERVALS berdasarkan Fiqih Mazhab Syafi'i.
     */
    fun calculateCategory6(
        monthLength: Int = 30,
        habitDurationDays: Int,
        positionWindowStart: Int = 1,
        positionWindowEnd: Int = 10,
        certainPureDays: Set<Int> = emptySet(),
        certainHaidDays: Set<Int> = emptySet()
    ): Category6CalculationResult {
        return Category6Calculator.calculateCategory6(
            monthLength = monthLength,
            habitDurationDays = habitDurationDays,
            positionWindowStart = positionWindowStart,
            positionWindowEnd = positionWindowEnd,
            certainPureDays = certainPureDays,
            certainHaidDays = certainHaidDays
        )
    }

    /** Nomor hari sipil (tanpa zona waktu/DST). month = 1..12. */
    private fun waqtCivilDay(year: Int, month: Int, day: Int): Long {
        val y = if (month <= 2) year - 1 else year
        val era = (if (y >= 0) y else y - 399) / 400
        val yoe = y - era * 400
        val mp = (month + 9) % 12
        val doy = (153 * mp + 2) / 5 + day - 1
        val doe = yoe * 365 + yoe / 4 - yoe / 100 + doy
        return era.toLong() * 146097L + doe.toLong() - 719468L
    }

    private fun waqtDaysInMonth(year: Int, month: Int): Int = when (month) {
        1, 3, 5, 7, 8, 10, 12 -> 31
        4, 6, 9, 11 -> 30
        else -> if ((year % 4 == 0 && year % 100 != 0) || year % 400 == 0) 29 else 28
    }

    private fun buildWaqtOnlyPeriodResults(
        startEpochMillis: Long,
        endEpochMillis: Long,
        certainHaidDayOfMonth: Int
    ): List<FiqihPeriodResult> {
        if (certainHaidDayOfMonth !in 1..31) return emptyList()
        val out = mutableListOf<FiqihPeriodResult>()
        var cursor = startEpochMillis
        while (cursor < endEpochMillis) {
            val cal = Calendar.getInstance().apply { timeInMillis = cursor }
            val year = cal.get(Calendar.YEAR)
            val month = cal.get(Calendar.MONTH) + 1
            val day = cal.get(Calendar.DAY_OF_MONTH)

            // Jangkar = tanggal haid terakhir yang <= hari ini. Bila tanggal itu tidak ada
            // di bulan pendek (mis. 31 di bulan 30 hari), dijepit ke hari terakhir bulan itu.
            val anchorThisMonth = minOf(certainHaidDayOfMonth, waqtDaysInMonth(year, month))
            val anchorCivil: Long = if (day >= anchorThisMonth) {
                waqtCivilDay(year, month, anchorThisMonth)
            } else {
                val prevYear = if (month == 1) year - 1 else year
                val prevMonth = if (month == 1) 12 else month - 1
                waqtCivilDay(
                    prevYear, prevMonth,
                    minOf(certainHaidDayOfMonth, waqtDaysInMonth(prevYear, prevMonth))
                )
            }
            // Selisih hari kalender riil sejak jangkar (bukan modulo panjang bulan berjalan).
            val offset = (waqtCivilDay(year, month, day) - anchorCivil).toInt()

            val status: FiqihStatus
            val reason: String
            val needsGhusl: Boolean
            when {
                offset == 0 -> {
                    status = FiqihStatus.HAID
                    reason = "Tanggal $certainHaidDayOfMonth: 24 jam pertama dihukumi Haid Yakin karena waktu mulai diingat dan batas minimal haid adalah sehari semalam."
                    needsGhusl = false
                }
                offset in 1..14 -> {
                    status = FiqihStatus.IHTIYATH
                    reason = "Hari ke-${offset + 1} sejak tanggal mulai haid: Masa IHTIYATH (kemungkinan haid/suci & putusnya haid; wajib mandi untuk tiap fardhu)."
                    needsGhusl = true
                }
                else -> {
                    status = FiqihStatus.ISTIHADHAH
                    reason = "Hari ke-${offset + 1} sejak tanggal mulai haid: Melebihi batas maksimal haid 15 hari, dihukumi Suci Yakin / Istihadhah murni."
                    needsGhusl = false
                }
            }
            val next = Calendar.getInstance().apply {
                timeInMillis = cursor
                add(Calendar.DAY_OF_MONTH, 1)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis
            val segmentEnd = minOf(next, endEpochMillis)
            val current = FiqihPeriodResult(
                cursor,
                segmentEnd,
                status,
                reason,
                needsGhusl,
                status == FiqihStatus.ISTIHADHAH || status == FiqihStatus.IHTIYATH,
                status == FiqihStatus.HAID
            )
            val previous = out.lastOrNull()
            if (previous != null &&
                previous.endEpochMillis == current.startEpochMillis &&
                previous.status == current.status &&
                previous.reason == current.reason &&
                previous.needsGhusl == current.needsGhusl &&
                previous.needsWudhu == current.needsWudhu
            ) {
                out[out.lastIndex] = previous.copy(endEpochMillis = current.endEpochMillis)
            } else {
                out.add(current)
            }
            cursor = segmentEnd
        }
        return out
    }

    private fun calculateNifasWithPhases(
        startEpochMillis: Long,
        endEpochMillis: Long,
        totalDurationMs: Long,
        totalDays: Long,
        remainingHours: Long,
        intervals: List<BleedingInterval>,
        hasPreviousAdat: Boolean,
        adatDurationDays: Int,
        adatMemoryType: AdatMemoryType,
        deliveryEpochMillis: Long?,
        deliveryType: DeliveryType,
        twinLastDeliveryEpochMillis: Long?,
        nifasAdatCategory: NifasAdatCategory?,
        hasIntermittentPause: Boolean,
        intermittentPauseDays: Double,
        qadhaList: List<String>,
        phaseBreakdowns: List<String>,
        hasPreviousHaidBeforeNifas: Boolean,
        previousHaidAdatDays: Int,
        previousSuciAdatDays: Int?
    ): CalculationResult {
        val effectiveDeliveryTime = if (deliveryType == DeliveryType.BAYI_KEMBAR && twinLastDeliveryEpochMillis != null) {
            twinLastDeliveryEpochMillis
        } else {
            deliveryEpochMillis ?: startEpochMillis
        }

        // Jeda 15 hari atau lebih dari persalinan memutus kemungkinan nifas.
        if (deliveryEpochMillis != null && (startEpochMillis - effectiveDeliveryTime) >= FIFTEEN_DAYS_MS) {
            val gapDays = (startEpochMillis - effectiveDeliveryTime) / ONE_DAY_MS
            return CalculationResult(
                statusSummary = "Bukan Nifas (jeda $gapDays hari dari persalinan)",
                caseCategory = "Darah Pasca-Wiladah, Bukan Nifas",
                categoryDetectionReason = "Jeda darah pertama dari persalinan mencapai 15 hari atau lebih; sumber yang dipakai menjelaskan darah tersebut bukan nifas.",
                haidOrNifasSegment = null,
                istihadhahSegment = PeriodSegment("Darah Bukan Nifas", startEpochMillis, endEpochMillis, "Bukan Nifas", "Perlu dihukumi kembali dengan bab haid/istihadhah."),
                suciSegment = null,
                shalatConsequence = "Status darah ini tidak dihitung sebagai nifas; hukum shalat mengikuti hasil bab haid/istihadhah.",
                qadhaPrayers = qadhaList,
                puasaConsequence = "Puasa tidak gugur hanya karena status nifas; tentukan kembali status darahnya.",
                mandiWajibNote = "Mandi wiladah tetap diperhatikan; bila darah ternyata haid, mandi haid dilakukan setelah darah berhenti.",
                mustahadhahCareGuide = null,
                medicalAndSpiritualAdvice = "Perdarahan pasca persalinan yang muncul setelah jeda panjang perlu dikonsultasikan kepada tenaga kesehatan.",
                references = listOf(
                    KitabReference("Tuhfatun Niswah", "Fashl fi an-Nifas", "Darah yang datang setelah jeda suci 15 hari dari wiladah tidak dihukumi nifas.", "Status nifas terputus oleh jeda tersebut.")
                ),
                phaseBreakdowns = phaseBreakdowns
            )
        }

        // Jeda suci di tengah masa nifas yang mencapai minimal 15 hari memutus nifas secara sah.
        // Darah berikutnya dihukumi sebagai darah haid baru menurut Mazhab Syafi'i (Tuhfatun Niswah & Uyunul Masa-il Linnisa').
        if (hasIntermittentPause && intermittentPauseDays >= 15.0) {
            val orderedIntervals = intervals.sortedBy { it.startEpochMillis }
            val firstPhase = orderedIntervals.firstOrNull()
            val nifasEnd = firstPhase?.endEpochMillis ?: (startEpochMillis + 20 * ONE_DAY_MS)
            val secondPhase = orderedIntervals.getOrNull(1)
            val secondPhaseStart = secondPhase?.startEpochMillis ?: (nifasEnd + (intermittentPauseDays.toLong() * ONE_DAY_MS))
            val secondPhaseEnd = secondPhase?.endEpochMillis ?: endEpochMillis

            val nifasSeg = PeriodSegment(
                title = "Masa Nifas Sah (Fase Pertama)",
                startEpochMillis = startEpochMillis,
                endEpochMillis = nifasEnd,
                status = "Nifas",
                description = "Darah nifas sah fase pertama sebelum terputus oleh jeda suci minimal 15 hari."
            )
            val haidSeg = PeriodSegment(
                title = "Masa Haid Baru (Darah Setelah Jeda Suci)",
                startEpochMillis = secondPhaseStart,
                endEpochMillis = secondPhaseEnd,
                status = "Haid",
                description = "Darah yang keluar setelah jeda suci minimal 15 hari bukan lagi nifas, melainkan dihukumi sebagai darah haid baru."
            )

            return CalculationResult(
                statusSummary = "Nifas Terputus Jeda Suci ≥ 15 Hari: Darah Kedua Dihukumi Haid Baru",
                caseCategory = "Nifas Terputus Jeda Suci (Darah Baru = Haid)",
                categoryDetectionReason = "Terdapat jeda suci $intermittentPauseDays hari (≥ 15 hari). Berdasarkan kaidah Mazhab Syafi'i, jeda suci minimal 15 hari memutus masa nifas secara tuntas sehingga darah yang keluar berikutnya adalah darah haid baru.",
                haidOrNifasSegment = nifasSeg,
                istihadhahSegment = haidSeg,
                suciSegment = null,
                shalatConsequence = "Shalat gugur pada masa nifas dan haid baru. Selama masa jeda suci, shalat fardhu wajib ditunaikan.",
                qadhaPrayers = qadhaList,
                puasaConsequence = "Puasa Ramadhan yang ditinggalkan selama nifas dan haid wajib diqadha.",
                mandiWajibNote = "Wajib mandi setelah nifas fase pertama berhenti, dan wajib mandi kembali setelah darah haid baru berhenti.",
                mustahadhahCareGuide = null,
                medicalAndSpiritualAdvice = "Kondisi ini normal secara biologis bila siklus haid baru mulai aktif kembali pasca nifas.",
                references = listOf(
                    KitabReference("Tuhfatun Niswah", "Fashl fi an-Nifas", "Jika antara dua darah terdapat masa suci 15 hari atau lebih, maka darah kedua adalah haid bukan nifas.", "Jeda 15 hari adalah batas pemisah sah antara nifas dan haid baru."),
                    KitabReference("Uyunul Masa'il Linnisa'", "Bab Nifas", "فإن تخلل بين الدمين طهر تام خمسة عشر يوما فالدم الثاني حيض", "Bila diselingi suci sempurna 15 hari maka darah kedua adalah haid.")
                ),
                phaseBreakdowns = phaseBreakdowns
            )
        }

        // Jika seluruh darah selesai dalam 60 hari dari wiladah, sumber menyatakan seluruhnya nifas.
        val durationFromDeliveryMs = (endEpochMillis - effectiveDeliveryTime).coerceAtLeast(totalDurationMs)
        if (durationFromDeliveryMs <= SIXTY_DAYS_MS && totalDurationMs <= SIXTY_DAYS_MS) {
            val seg = PeriodSegment(
                title = "Masa Nifas Sah",
                startEpochMillis = startEpochMillis,
                endEpochMillis = endEpochMillis,
                status = "Nifas",
                description = "Darah masih berada dalam batas maksimal 60 hari 60 malam."
            )
            return CalculationResult(
                statusSummary = "Nifas Sah: $totalDays Hari $remainingHours Jam",
                caseCategory = if (totalDays <= 40) "Nifas Normal (Ghalib 40 Hari)" else "Nifas Sah (≤ 60 Hari)",
                categoryDetectionReason = "Seluruh rentang darah yang dihitung masih berada dalam batas maksimal nifas 60 hari 60 malam.",
                haidOrNifasSegment = seg,
                istihadhahSegment = null,
                suciSegment = null,
                shalatConsequence = "Shalat pada masa nifas gugur dan tidak diqadha.",
                qadhaPrayers = qadhaList,
                puasaConsequence = "Puasa Ramadhan yang tertinggal karena nifas wajib diqadha.",
                mandiWajibNote = "Mandi nifas/wiladah dilakukan setelah darah berhenti.",
                mustahadhahCareGuide = null,
                medicalAndSpiritualAdvice = "Perdarahan pasca persalinan tetap perlu dipantau secara medis bila banyak, menetap, atau disertai keluhan.",
                references = listOf(
                    KitabReference("Tuhfatun Niswah", "Fashl fi an-Nifas", "Paling banyak nifas 60 hari 60 malam.", "Selama tidak melampaui batas tersebut, darah dihukumi nifas."),
                    KitabReference("Uyunul Masa'il Linnisa'", "Bab Nifas", "أكثره ستون يوما", "Batas maksimal nifas adalah 60 hari 60 malam.")
                ),
                phaseBreakdowns = phaseBreakdowns
            )
        }

        // Mulai bab mustahadhah fin-nifas: darah telah melampaui 60 hari.
        val ordered = intervals.sortedBy { it.startEpochMillis }
        val strongestScore = ordered.maxOfOrNull { it.strengthScore }
        val strong = if (strongestScore == null) emptyList() else ordered.filter { it.strengthScore == strongestScore }
        val weak = if (strongestScore == null) emptyList() else ordered.filter { it.strengthScore < strongestScore }
        val strongDuration = strong.sumOf { it.durationMillis }
        val firstStrong = strong.firstOrNull()
        val secondStrong = strong.drop(1).firstOrNull()
        val hasSecondStrong = secondStrong != null
        val firstStrongDuration = firstStrong?.durationMillis ?: 0L

        // Tamyiz fin-nifas menurut Tuhfatun Niswah lebih ringan daripada tamyiz haid:
        // cukup darah kuat berada dalam rentang tidak lebih dari 60 hari. Catatan kronologi
        // tetap dipakai: lemah sebelum/sesudah kuat dapat menjadi suci; lemah di sela kuat
        // menurut pendapat muktamad termasuk nifas.
        val tamyizValid = firstStrongDuration > 0 && firstStrongDuration <= SIXTY_DAYS_MS
        val tamyizCheck = TamyizCheckResult(
            valid = tamyizValid,
            strongDurationMillis = strongDuration,
            weakContinuousDurationMillis = weak.maxOfOrNull { it.durationMillis } ?: 0L,
            hasSecondStrongOfSameType = hasSecondStrong,
            reason = if (tamyizValid) {
                "Tamyiz fin-nifas terpenuhi: terdapat darah kuat yang totalnya tidak melampaui 60 hari 60 malam."
            } else {
                "Tamyiz fin-nifas tidak terpenuhi: tidak ada darah kuat yang sah dalam batas 60 hari."
            }
        )

        fun segment(
            title: String,
            start: Long,
            end: Long,
            status: String,
            description: String
        ) = PeriodSegment(title, start, end.coerceAtLeast(start), status, description)

        fun makePeriods(): List<FiqihPeriodResult> = ordered.map { p ->
            val status = when {
                tamyizValid && p.strengthScore == strongestScore -> FiqihStatus.NIFAS
                tamyizValid -> {
                    // Catatan Tuhfatun Niswah: darah lemah sebelum darah kuat yang
                    // berlangsung 15 hari atau lebih ikut dihukumi nifas; darah lemah
                    // yang berada di sela dua darah kuat juga termasuk nifas menurut
                    // pendapat muktamad. Darah lemah sesudah satu darah kuat tanpa
                    // pola pemisah tersebut tetap istihadhah.
                    val firstStrongStart = firstStrong?.startEpochMillis
                    val beforeStrong = firstStrongStart != null && p.endEpochMillis <= firstStrongStart
                    val weakBeforeDuration = if (beforeStrong) p.durationMillis else 0L
                    val betweenStrong = ordered.any { left ->
                        left.strengthScore == strongestScore && left.endEpochMillis <= p.startEpochMillis &&
                            ordered.any { right ->
                                right.strengthScore == strongestScore && right.startEpochMillis >= p.endEpochMillis &&
                                    right.startEpochMillis > left.endEpochMillis
                            }
                    }
                    if ((beforeStrong && weakBeforeDuration >= FIFTEEN_DAYS_MS) || betweenStrong) {
                        FiqihStatus.NIFAS
                    } else {
                        FiqihStatus.ISTIHADHAH
                    }
                }
                else -> FiqihStatus.ISTIHADHAH
            }
            FiqihPeriodResult(
                p.startEpochMillis,
                p.endEpochMillis,
                status,
                reason = if (status == FiqihStatus.NIFAS) "Masuk fase nifas menurut pola darah." else "Masuk fase istihadhah/suci menurut kategori yang dipilih.",
                needsGhusl = status == FiqihStatus.NIFAS,
                needsWudhu = status == FiqihStatus.ISTIHADHAH
            )
        }

        fun baseResult(
            category: String,
            reason: String,
            nifasSeg: PeriodSegment?,
            istiSeg: PeriodSegment?,
            shalat: String,
            mandi: String,
            guide: String?,
            refs: List<KitabReference>,
            summary: String = category,
            customPeriodResults: List<FiqihPeriodResult>? = null
        ): CalculationResult = CalculationResult(
            statusSummary = summary,
            caseCategory = category,
            categoryDetectionReason = reason,
            haidOrNifasSegment = nifasSeg,
            istihadhahSegment = istiSeg,
            suciSegment = null,
            shalatConsequence = shalat,
            qadhaPrayers = qadhaList,
            puasaConsequence = "Puasa pada masa nifas gugur dan wajib diqadha; puasa pada masa istihadhah tetap mengikuti hukum istihadhah.",
            mandiWajibNote = mandi,
            mustahadhahCareGuide = guide,
            medicalAndSpiritualAdvice = "Perdarahan lebih dari 60 hari pasca persalinan memerlukan perhatian medis.",
            references = refs,
            phaseBreakdowns = phaseBreakdowns,
            periodResults = customPeriodResults ?: makePeriods(),
            tamyizCheck = tamyizCheck
        )

        val standardGuide = """
            TATA CARA BERSUCI MUSTAHADHAH FIN-NIFAS:
            1. Bersihkan najis darah.
            2. Gunakan pembalut/pengaman agar darah tidak menyebar.
            3. Berwudhu setelah masuk waktu shalat fardhu.
            4. Segera tunaikan shalat fardhu.
        """.trimIndent()

        // 1. Mubtadi'ah Mumayyizah fin-Nifas
        if ((nifasAdatCategory == NifasAdatCategory.MUBTADIAH_MUMAYYIZAH || !hasPreviousAdat) && tamyizValid) {
            val nifasStart = firstStrong!!.startEpochMillis
            val nifasEnd = firstStrong.endEpochMillis.coerceAtMost(startEpochMillis + SIXTY_DAYS_MS)
            val nifasSeg = segment("Nifas — Darah Kuat", nifasStart, nifasEnd, "Nifas", "Darah kuat menjadi nifas menurut tamyiz fin-nifas.")
            val istiStart = weak.firstOrNull()?.startEpochMillis ?: nifasEnd
            val istiEnd = weak.lastOrNull()?.endEpochMillis ?: endEpochMillis
            val istiSeg = if (istiEnd > istiStart) segment("Istihadhah — Darah Lemah", istiStart, istiEnd, "Istihadhah", "Darah lemah sebelum/sesudah darah kuat dihukumi sesuai catatan tamyiz fin-nifas.") else null
            val periods = ordered.map { p ->
                val status = when {
                    p.strengthScore == strongestScore -> FiqihStatus.NIFAS
                    p.startEpochMillis < firstStrong!!.startEpochMillis && p.durationMillis >= FIFTEEN_DAYS_MS -> FiqihStatus.NIFAS
                    else -> FiqihStatus.ISTIHADHAH
                }
                FiqihPeriodResult(p.startEpochMillis, p.endEpochMillis, status,
                    reason = if (status == FiqihStatus.NIFAS) "Masuk hukum nifas berdasarkan tamyiz fin-nifas." else "Darah lemah di luar pengecualian muktamad dihukumi istihadhah.",
                    needsGhusl = status == FiqihStatus.NIFAS,
                    needsWudhu = status == FiqihStatus.ISTIHADHAH)
            }
            return baseResult(
                "1. Mubtadi'ah Mumayyizah fin-Nifas",
                "Kelahiran pertama dan darah memenuhi tamyiz fin-nifas; darah kuat menjadi nifas, darah lemah menjadi istihadhah, dengan memperhatikan kronologi darah.",
                nifasSeg,
                istiSeg,
                "Shalat pada fase nifas gugur. Shalat pada fase istihadhah wajib ditunaikan; jika sempat ditinggalkan, periksa qadha.",
                "Mandi ketika darah nifas berhenti dan masuk fase suci/istihadhah.",
                standardGuide,
                listOf(
                    KitabReference("Tuhfatun Niswah", "Istihadhah Nifas, hal. 36", "Nifas = darah kuat; Istihadhah = darah lemah.", "Syarat tamyiz fin-nifas lebih ringan: darah kuat tidak lebih dari 60 hari 60 malam.")
                ),
                "Nifas $strongDuration / 24 jam berdasarkan darah kuat; istihadhah pada darah lemah.",
                periods
            )
        }

        // 2. Mubtadi'ah Ghairu Mumayyizah fin-Nifas
        if (nifasAdatCategory == NifasAdatCategory.MUBTADIAH_GHAIRU_MUMAYYIZAH || !hasPreviousAdat) {
            val lahzhah = 1L
            val nifasSeg = segment("Nifas — Lahzhah", startEpochMillis, startEpochMillis + lahzhah, "Nifas", "Nifas pemula tanpa tamyiz dikembalikan kepada minimal nifas, yaitu lahzhah.")
            val istiSeg = segment("Istihadhah setelah Lahzhah", startEpochMillis + lahzhah, endEpochMillis, "Istihadhah", "Setelah nifas minimal, darah sisanya dihukumi istihadhah menurut kategori ini.")
            val periods = listOf(
                FiqihPeriodResult(startEpochMillis, startEpochMillis + lahzhah, FiqihStatus.NIFAS, "Nifas minimal: lahzhah pertama.", needsGhusl = true),
                FiqihPeriodResult(startEpochMillis + lahzhah, endEpochMillis, FiqihStatus.ISTIHADHAH, "Setelah nifas minimal, masuk istihadhah.", needsGhusl = false, needsWudhu = true)
            )
            return baseResult(
                "2. Mubtadi'ah Ghairu Mumayyizah fin-Nifas",
                "Kelahiran pertama, darah melebihi 60 hari dan tidak memenuhi tamyiz; nifas dikembalikan kepada minimalnya (lahzhah).",
                nifasSeg,
                istiSeg,
                "Nifas ditetapkan pada lahzhah pertama. Status shalat dan qadha setelahnya perlu mengikuti rincian kondisi yang dihitung; kalkulator tidak menambahkan jumlah qadha tanpa data pendukung.",
                "Mandi setelah berakhirnya nifas minimal/ketika memasuki hukum istihadhah.",
                standardGuide,
                listOf(
                    KitabReference("Tuhfatun Niswah", "Istihadhah Nifas, hal. 36-37", "Mubtadiah Ghairu Mumayyizah: nifas = darah setetes pertama.", "Bila darah pemula melewati 60 hari tanpa tamyiz, nifasnya dikembalikan kepada minimal nifas.")
                ),
                "2. Mubtadi'ah Ghairu Mumayyizah fin-Nifas",
                periods
            )
        }

        // 3. Mu'tadah Mumayyizah fin-Nifas
        if ((nifasAdatCategory == NifasAdatCategory.MUTADAH_MUMAYYIZAH || hasPreviousAdat) && tamyizValid) {
            val nifasStart = firstStrong!!.startEpochMillis
            val nifasEnd = firstStrong.endEpochMillis.coerceAtMost(startEpochMillis + SIXTY_DAYS_MS)
            val nifasSeg = segment("Nifas — Darah Kuat", nifasStart, nifasEnd, "Nifas", "Darah kuat dihukumi nifas karena tamyiz fin-nifas terpenuhi.")
            val istiStart = weak.firstOrNull()?.startEpochMillis ?: nifasEnd
            val istiEnd = weak.lastOrNull()?.endEpochMillis ?: endEpochMillis
            val istiSeg = if (istiEnd > istiStart) segment("Istihadhah — Darah Lemah", istiStart, istiEnd, "Istihadhah", "Darah lemah yang berada sebelum/sesudah darah kuat dihukumi istihadhah; lemah di sela kuat mengikuti pendapat muktamad.") else null
            val periods = ordered.map { p ->
                val status = when {
                    p.strengthScore == strongestScore -> FiqihStatus.NIFAS
                    p.startEpochMillis < firstStrong!!.startEpochMillis && p.durationMillis >= FIFTEEN_DAYS_MS -> FiqihStatus.NIFAS
                    ordered.any { left -> left.strengthScore == strongestScore && left.endEpochMillis <= p.startEpochMillis && ordered.any { right -> right.strengthScore == strongestScore && right.startEpochMillis >= p.endEpochMillis } } -> FiqihStatus.NIFAS
                    else -> FiqihStatus.ISTIHADHAH
                }
                FiqihPeriodResult(p.startEpochMillis, p.endEpochMillis, status, "Klasifikasi mengikuti tamyiz fin-nifas.", needsGhusl = status == FiqihStatus.NIFAS, needsWudhu = status == FiqihStatus.ISTIHADHAH)
            }
            return baseResult(
                "3. Mu'tadah Mumayyizah fin-Nifas",
                "Pernah nifas dan darah memenuhi tamyiz fin-nifas. Dalam bab ini, tamyiz dipakai untuk menentukan darah kuat sebagai nifas.",
                nifasSeg,
                istiSeg,
                "Shalat pada darah nifas gugur. Shalat pada darah istihadhah wajib; qadha mengikuti fase yang sempat ditinggalkan.",
                "Mandi saat berpindah dari nifas ke fase yang dihukumi suci/istihadhah.",
                standardGuide,
                listOf(
                    KitabReference("Tuhfatun Niswah", "Istihadhah Nifas, hal. 36", "Mu'tadah Mumayyizah: Nifas = darah kuat; Istihadhah = darah lemah.", "Tamyiz dipakai pada darah yang melampaui 60 hari.")
                )
            )
        }

        // 4. Mu'tadah Ghairu Mumayyizah Dzakirah: ingat adat nifas.
        val rememberedNifasDays = adatDurationDays.takeIf { it in 1..60 }
        if (nifasAdatCategory == NifasAdatCategory.MUTADAH_GHAIRU_MUMAYYIZAH_DZAKIRAH ||
            (hasPreviousAdat && adatMemoryType == AdatMemoryType.INGAT_LENGKAP_QADRAN_WAQTAN)) {
            if (rememberedNifasDays == null) {
                return baseResult(
                    "4. Mu'tadah Ghairu Mumayyizah Dzakirah — Perlu Verifikasi Data",
                    "Adat nifas diperlukan tetapi durasinya belum tersedia dalam rentang 1–60 hari.",
                    null, null,
                    "Jangan menetapkan qadha dari kalkulator sebelum durasi adat nifas dilengkapi.",
                    "Perlu verifikasi waktu akhir nifas.", null,
                    emptyList(),
                    "Perlu Verifikasi Data"
                )
            }
            val nifasMs = rememberedNifasDays * ONE_DAY_MS
            val nifasSeg = segment("Nifas — Sesuai Adat $rememberedNifasDays Hari", startEpochMillis, startEpochMillis + nifasMs, "Nifas", "Nifas mengikuti kadar adat nifas sebelumnya.")
            val periods = mutableListOf<FiqihPeriodResult>()
            val nifasEnd = (startEpochMillis + nifasMs).coerceAtMost(endEpochMillis)
            if (nifasEnd > startEpochMillis) periods.add(FiqihPeriodResult(startEpochMillis, nifasEnd, FiqihStatus.NIFAS, "Mengikuti adat nifas sebelumnya.", needsGhusl = true))

            val refs = mutableListOf(
                KitabReference("Tuhfatun Niswah", "Istihadhah Nifas, hal. 37-38", "Nifas = sesuai adat nifasnya.", "Mu'tadah Ghairu Mumayyizah dikembalikan kepada adat nifas ketika tamyiz tidak sah.")
            )

            if (!hasPreviousHaidBeforeNifas) {
                // Sumber: bila belum pernah haid dan suci: setelah nifas, 29 hari istihadhah lalu 1 hari haid, berulang.
                val after = endEpochMillis - (startEpochMillis + nifasMs)
                val firstAfter = startEpochMillis + nifasMs
                val istiEnd = (firstAfter + 29 * ONE_DAY_MS).coerceAtMost(endEpochMillis)
                val istiSeg = if (istiEnd > firstAfter) segment("Istihadhah — 29 Hari", firstAfter, istiEnd, "Istihadhah", "Setelah kadar nifas adat, sumber menetapkan 29 hari istihadhah sebelum haid sehari semalam.") else null
                val cycleHaidStart = istiEnd
                val cycleHaidEnd = (cycleHaidStart + ONE_DAY_MS).coerceAtMost(endEpochMillis)
                val cycleSeg = if (cycleHaidEnd > cycleHaidStart) segment("Haid — Sehari Semalam", cycleHaidStart, cycleHaidEnd, "Haid", "Siklus haid sehari semalam setelah 29 hari istihadhah.") else null
                val combined = if (cycleSeg != null && istiSeg != null) segment("Setelah Nifas: 29 Hari Istihadhah + Haid 1 Hari", firstAfter, cycleHaidEnd, "Istihadhah/Haid", "Pola pertama setelah nifas adat.") else istiSeg
                if (istiEnd > firstAfter) periods.add(FiqihPeriodResult(firstAfter, istiEnd, FiqihStatus.ISTIHADHAH, "Mengikuti 29 hari istihadhah setelah adat nifas.", needsWudhu = true))
                if (cycleHaidEnd > cycleHaidStart) periods.add(FiqihPeriodResult(cycleHaidStart, cycleHaidEnd, FiqihStatus.HAID, "Haid sehari semalam setelah 29 hari istihadhah.", needsGhusl = true))
                return baseResult(
                    "4. Mu'tadah Ghairu Mumayyizah Dzakirah fin-Nifas",
                    "Adat nifas diingat. Karena belum pernah haid dan suci, setelah nifas berlaku pola 29 hari istihadhah lalu 1 hari haid sebagaimana contoh dalam sumber.",
                    nifasSeg,
                    combined,
                    "Shalat pada masa nifas gugur. Pada masa istihadhah wajib shalat; pada hari haid shalat gugur.",
                    "Mandi setelah nifas, dan mandi lagi setelah fase haid yang muncul dalam pola berikutnya.",
                    standardGuide,
                    refs,
                    "Nifas $rememberedNifasDays hari; setelahnya 29 hari istihadhah lalu haid sehari semalam bila rentang data mencukupi.",
                    periods
                )
            }

            val suciAdatDays = previousSuciAdatDays
            if (suciAdatDays == null) {
                return baseResult(
                    "4. Mu'tadah Ghairu Mumayyizah Dzakirah — Perlu Verifikasi Data",
                    "Adat suci sebelumnya diperlukan untuk meneruskan pola setelah nifas, tetapi data suci yang valid (minimal 15 hari) belum tersedia.",
                    nifasSeg, null,
                    "Jangan menentukan fase setelah nifas sebelum adat suci dilengkapi.",
                    "Perlu verifikasi akhir nifas dan adat suci.", null, refs,
                    "Perlu Verifikasi Data"
                )
            }
            val firstAfter = startEpochMillis + nifasMs
            val istiEnd = (firstAfter + suciAdatDays * ONE_DAY_MS).coerceAtMost(endEpochMillis)
            val haidStart = istiEnd
            val haidEnd = (haidStart + previousHaidAdatDays * ONE_DAY_MS).coerceAtMost(endEpochMillis)
            val combinedEnd = if (haidEnd > haidStart) haidEnd else istiEnd
            val combined = if (combinedEnd > firstAfter) segment(
                "Setelah Nifas: Suci/istihadhah $suciAdatDays Hari + Haid $previousHaidAdatDays Hari",
                firstAfter, combinedEnd, "Istihadhah/Haid",
                "Sumber menetapkan setelah nifas adat: istihadhah mengikuti adat suci, lalu haid mengikuti adat haid."
            ) else null
            if (istiEnd > firstAfter) periods.add(FiqihPeriodResult(firstAfter, istiEnd, FiqihStatus.ISTIHADHAH, "Mengikuti adat suci setelah nifas.", needsWudhu = true))
            if (haidEnd > haidStart) periods.add(FiqihPeriodResult(haidStart, haidEnd, FiqihStatus.HAID, "Mengikuti adat haid setelah adat suci.", needsGhusl = true))
            refs.add(KitabReference("Tuhfatun Niswah", "Istihadhah Nifas, hal. 37", "Jika pernah haid dan suci: Istihadhah sesuai adat suci, Haid sesuai adat haid.", "Adat suci dan adat haid diperlukan untuk meneruskan pola setelah nifas."))
            return baseResult(
                "4. Mu'tadah Ghairu Mumayyizah Dzakirah fin-Nifas",
                "Adat nifas diingat dan tamyiz tidak sah. Karena sebelumnya sudah pernah haid dan suci, setelah kadar nifas mengikuti adat suci lalu adat haid.",
                nifasSeg,
                combined,
                "Shalat pada nifas gugur. Setelahnya ikuti fase istihadhah/suci dan haid sesuai adat.",
                "Mandi setelah nifas; kemudian mengikuti mandi haid ketika fase haid adat berakhir.",
                standardGuide,
                refs,
                "Nifas $rememberedNifasDays hari; setelahnya adat suci $suciAdatDays hari lalu adat haid $previousHaidAdatDays hari.",
                periods
            )
        }

        // 5. Mu'tadah Nasiyah fin-Nifas + dua catatan: qadr/waqt yang terlupa
        val nasiyah = nifasAdatCategory == NifasAdatCategory.MUTADAH_NASIYAH_MUTAHAYYIRAH || adatMemoryType == AdatMemoryType.LUPA_SEMUANYA_MUTAHAYYIRAH
        val lupaWaqt = nifasAdatCategory == NifasAdatCategory.CATATAN_DZAKIRAH_QADRAN_LUPA_WAQTAN || adatMemoryType == AdatMemoryType.INGAT_QADRAN_LUPA_WAQTAN
        val lupaQadr = nifasAdatCategory == NifasAdatCategory.CATATAN_DZAKIRAH_WAQTAN_LUPA_QADRAN || adatMemoryType == AdatMemoryType.INGAT_WAQTAN_LUPA_QADRAN

        if (nasiyah || lupaWaqt || lupaQadr) {
            val ihtiyathStart = startEpochMillis + 1L
            val nifasSeg = segment("Nifas Yakin — Lahzhah", startEpochMillis, ihtiyathStart, "Nifas", "Yang diyakini nifas adalah darah setetes pertama/lahzhah.")
            val ihtiyathSeg = if (endEpochMillis > ihtiyathStart) segment(
                "Ihtiyath fin-Nifas",
                ihtiyathStart,
                endEpochMillis,
                "Ihtiyath",
                "Berlaku kehati-hatian sampai batas 60 hari sesuai keterangan sumber."
            ) else null
            val sixtyDayBoundary = (startEpochMillis + SIXTY_DAYS_MS).coerceAtMost(endEpochMillis)
            val periods = buildList {
                add(FiqihPeriodResult(startEpochMillis, ihtiyathStart, FiqihStatus.NIFAS, "Nifas yang yakin hanya lahzhah pertama.", needsGhusl = true))
                if (sixtyDayBoundary > ihtiyathStart) {
                    add(FiqihPeriodResult(ihtiyathStart, sixtyDayBoundary, FiqihStatus.IHTIYATH, "Ihtiyath: wajib mandi setiap shalat fardhu sampai mendekati batas 60 hari.", needsGhusl = true, needsWudhu = true))
                }
                if (endEpochMillis > sixtyDayBoundary) {
                    add(FiqihPeriodResult(sixtyDayBoundary, endEpochMillis, FiqihStatus.ISTIHADHAH, "Setelah batas 60 hari, sumber menyebut kewajiban beralih menjadi wudhu setiap shalat fardhu.", needsGhusl = false, needsWudhu = true))
                }
            }
            val detail = when {
                lupaWaqt -> "Catatan: ingat kadar adat nifas tetapi lupa waktunya. Sumber mengembalikan kepada kadar adat dengan kehati-hatian pada waktunya."
                lupaQadr -> "Catatan: ingat waktu tetapi lupa kadar adat nifas. Sumber menetapkan nifas yakin pada awal waktu lalu ihtiyath hingga batas 60 hari."
                else -> "Lupa kadar dan waktu adat nifas; hukum mengikuti ihtiyath fin-nifas."
            }
            val mandi = if (nasiyah) {
                "Ihtiyath menurut sumber: wajib mandi setiap hendak shalat fardhu sampai 60 hari kurang sedikit; setelah itu wudhu setiap hendak shalat fardhu."
            } else {
                "Wajib mandi pada waktu yang memungkinkan berakhirnya nifas; untuk rincian ihtiyath ikuti keterangan kategori ini."
            }
            val refs = listOf(
                KitabReference("Tuhfatun Niswah", "Istihadhah Nifas, hal. 38", "Nifas = darah setetes pertama; ihtiyath: mandi setiap shalat fardhu sampai 60 hari kurang sedikit, lalu wudhu setiap shalat.", detail),
                KitabReference("Uyunul Masa'il Linnisa'", "Tanbih Istihadhah fin-Nifas", "Lupa qadr atau waqt memiliki hukum seperti nasiyah qadran wa waqtan.", "Catatan sumber memperlakukan lupa salah satu dari kadar/waktu dengan hukum ihtiyath.")
            )
            return baseResult(
                when {
                    nasiyah -> "5. Mu'tadah Ghairu Mumayyizah fin-Nifas Nasiyah"
                    lupaWaqt -> "Catatan: Dzakirah lil-Qadri dunan Waqt fin-Nifas"
                    else -> "Catatan: Dzakirah lil-Waqti dunan Qadr fin-Nifas"
                },
                detail,
                nifasSeg,
                ihtiyathSeg,
                "Hari/masa nifas yakin tidak dibebani shalat. Setelahnya berlaku ihtiyath: tetap shalat dan bersuci sesuai ketentuan kategori.",
                mandi,
                "IHTIYATH: $mandi",
                refs,
                "Nifas yakin = lahzhah; selanjutnya ihtiyath sampai batas 60 hari.",
                periods
            )
        }

        // Bila kategori belum dipilih secara eksplisit, jangan menebak adat nifas.
        return baseResult(
            "Perlu Verifikasi Data Nifas",
            "Data belum cukup untuk memilih salah satu golongan mustahadhah fin-nifas dari sumber yang digunakan.",
            null,
            null,
            "Jangan menentukan qadha hanya dari kalkulator sebelum kategori dan adat yang relevan dilengkapi.",
            "Perlu verifikasi akhir nifas.",
            null,
            listOf(
                KitabReference("Tuhfatun Niswah", "Istihadhah Nifas", "Mustahadhah nifas memiliki beberapa golongan dengan hukum berbeda.", "Kategori perlu ditentukan berdasarkan riwayat nifas dan kemampuan membedakan darah.")
            ),
            "Perlu Verifikasi Data"
        )
    }
}

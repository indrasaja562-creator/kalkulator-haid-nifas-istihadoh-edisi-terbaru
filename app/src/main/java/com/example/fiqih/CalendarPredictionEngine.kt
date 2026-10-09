package com.example.fiqih

import com.example.data.entities.DailyBloodLogEntity
import com.example.data.entities.UserAdatProfileEntity
import com.example.model.*
import java.util.*

object CalendarPredictionEngine {

    fun generateDaysForMonth(
        year: Int,
        month: Int, // 0..11
        logs: List<DailyBloodLogEntity>,
        userProfile: UserAdatProfileEntity
    ): List<CalendarDayItem> {
        val logMap = logs.associateBy { it.dateString }
        val prediction = computeCyclePrediction(logs, userProfile)
        val todayEpochDay = CalendarDateHelper.getTodayEpochDay()

        val cal = Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month)
            set(Calendar.DAY_OF_MONTH, 1)
        }

        val firstDayOfWeek = cal.get(Calendar.DAY_OF_WEEK) // 1=Sunday, 2=Monday, etc.
        val daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)

        // Previous month days to fill leading row (starting on Sunday)
        val leadingDaysCount = firstDayOfWeek - Calendar.SUNDAY
        val prevCal = (cal.clone() as Calendar).apply {
            add(Calendar.MONTH, -1)
        }
        val prevMonthDays = prevCal.getActualMaximum(Calendar.DAY_OF_MONTH)

        val result = mutableListOf<CalendarDayItem>()

        // 1. Leading days from previous month
        for (i in (prevMonthDays - leadingDaysCount + 1)..prevMonthDays) {
            val dCal = (prevCal.clone() as Calendar).apply {
                set(Calendar.DAY_OF_MONTH, i)
            }
            val iso = CalendarDateHelper.formatIso(dCal)
            val epochDay = CalendarDateHelper.toEpochDay(dCal)
            result.add(
                buildDayItem(
                    iso = iso,
                    day = i,
                    month = prevCal.get(Calendar.MONTH),
                    year = prevCal.get(Calendar.YEAR),
                    epochDay = epochDay,
                    isCurrentMonth = false,
                    todayEpochDay = todayEpochDay,
                    log = logMap[iso],
                    prediction = prediction
                )
            )
        }

        // 2. Days of current month
        for (day in 1..daysInMonth) {
            val dCal = (cal.clone() as Calendar).apply {
                set(Calendar.DAY_OF_MONTH, day)
            }
            val iso = CalendarDateHelper.formatIso(dCal)
            val epochDay = CalendarDateHelper.toEpochDay(dCal)
            result.add(
                buildDayItem(
                    iso = iso,
                    day = day,
                    month = month,
                    year = year,
                    epochDay = epochDay,
                    isCurrentMonth = true,
                    todayEpochDay = todayEpochDay,
                    log = logMap[iso],
                    prediction = prediction
                )
            )
        }

        // 3. Trailing days from next month to complete 5 or 6 weeks (total multiple of 7)
        val remainingSlots = (7 - (result.size % 7)) % 7
        val nextCal = (cal.clone() as Calendar).apply {
            add(Calendar.MONTH, 1)
        }
        for (day in 1..remainingSlots) {
            val dCal = (nextCal.clone() as Calendar).apply {
                set(Calendar.DAY_OF_MONTH, day)
            }
            val iso = CalendarDateHelper.formatIso(dCal)
            val epochDay = CalendarDateHelper.toEpochDay(dCal)
            result.add(
                buildDayItem(
                    iso = iso,
                    day = day,
                    month = nextCal.get(Calendar.MONTH),
                    year = nextCal.get(Calendar.YEAR),
                    epochDay = epochDay,
                    isCurrentMonth = false,
                    todayEpochDay = todayEpochDay,
                    log = logMap[iso],
                    prediction = prediction
                )
            )
        }

        return result
    }

    private fun buildDayItem(
        iso: String,
        day: Int,
        month: Int,
        year: Int,
        epochDay: Long,
        isCurrentMonth: Boolean,
        todayEpochDay: Long,
        log: DailyBloodLogEntity?,
        prediction: CyclePredictionData
    ): CalendarDayItem {
        val isToday = (epochDay == todayEpochDay)

        // Check if this date falls within predicted periods
        val isPredictedHaidActive = prediction.activeCycleStopEpochDay?.let { stopEpoch ->
            val startEpoch = prediction.activeCycleStartEpochDay ?: (stopEpoch - 6)
            epochDay in startEpoch..stopEpoch && (log == null || !log.hasBlood)
        } ?: false

        val isPredictedNextHaid = prediction.nextCycleStartEpochDay?.let { nextStart ->
            val nextStop = prediction.nextCycleStopEpochDay ?: (nextStart + 6)
            epochDay in nextStart..nextStop
        } ?: false

        val isPredictedStop = (epochDay == prediction.activeCycleStopEpochDay) ||
                (epochDay == prediction.nextCycleStopEpochDay)

        val isPredicted = (isPredictedHaidActive || isPredictedNextHaid) && (log == null || !log.hasBlood)

        return CalendarDayItem(
            dateString = iso,
            dayOfMonth = day,
            month = month,
            year = year,
            epochDay = epochDay,
            isCurrentMonth = isCurrentMonth,
            isToday = isToday,
            log = log,
            isPredictedHaid = isPredicted,
            isPredictedStopDay = isPredictedStop,
            isPredictedSuci = !isPredicted && (log == null || !log.hasBlood)
        )
    }

    data class CyclePredictionData(
        val activeCycleStartEpochDay: Long?,
        val activeCycleStopEpochDay: Long?,
        val nextCycleStartEpochDay: Long?,
        val nextCycleStopEpochDay: Long?,
        val summary: CyclePredictionSummary
    )

    fun computeCyclePrediction(
        logs: List<DailyBloodLogEntity>,
        userProfile: UserAdatProfileEntity
    ): CyclePredictionData {
        val todayEpochDay = CalendarDateHelper.getTodayEpochDay()
        val usualHaid = userProfile.usualHaidDays.coerceIn(1, 15)
        val usualSuci = userProfile.usualSuciDays.coerceAtLeast(15) // Minimal suci Syafi'i 15 hari
        val cycleLength = usualHaid + usualSuci

        val bloodLogs = logs.filter { it.hasBlood && it.caseType == "HAID" }
            .sortedBy { it.epochDay }

        if (bloodLogs.isEmpty()) {
            // No history logged yet: fallback to baseline estimation
            val estLastStart = todayEpochDay - 10
            val estNextStart = estLastStart + cycleLength
            val estNextStop = estNextStart + usualHaid - 1

            val summary = CyclePredictionSummary(
                currentStatusTitle = "Masa Suci (Belum Ada Catatan)",
                currentStatusDesc = "Silakan catat darah haid harian Anda untuk memulai prediksi otomatis siklus.",
                isActiveHaid = false,
                activeHaidDaysSoFar = 0,
                predictedStopDateText = null,
                daysUntilPredictedStop = null,
                predictedNextHaidStartText = formatDateFromEpoch(estNextStart),
                daysUntilNextHaid = (estNextStart - todayEpochDay).toInt().coerceAtLeast(0),
                predictedNextHaidStopText = formatDateFromEpoch(estNextStop),
                fiqihNote = "Adat haid: $usualHaid hari, Adat suci: $usualSuci hari. Batas maksimal haid menurut mazhab Syafi'i adalah 15 hari 15 malam."
            )
            return CyclePredictionData(
                activeCycleStartEpochDay = null,
                activeCycleStopEpochDay = null,
                nextCycleStartEpochDay = estNextStart,
                nextCycleStopEpochDay = estNextStop,
                summary = summary
            )
        }

        // Group into bleeding clusters (gap <= 15 days is within same potential cycle in Syafi'i)
        val clusters = mutableListOf<MutableList<DailyBloodLogEntity>>()
        var currentCluster = mutableListOf<DailyBloodLogEntity>()

        for (log in bloodLogs) {
            if (currentCluster.isEmpty()) {
                currentCluster.add(log)
            } else {
                val lastLog = currentCluster.last()
                if (log.epochDay - lastLog.epochDay <= 15) {
                    currentCluster.add(log)
                } else {
                    clusters.add(currentCluster)
                    currentCluster = mutableListOf(log)
                }
            }
        }
        if (currentCluster.isNotEmpty()) {
            clusters.add(currentCluster)
        }

        val lastCluster = clusters.last()
        val clusterStartEpoch = lastCluster.first().epochDay
        val clusterLastBloodEpoch = lastCluster.last().epochDay

        // Is user currently in active bleeding cluster?
        // If today is within [clusterStartEpoch..clusterLastBloodEpoch + 1], consider it active
        val isCurrentlyActive = todayEpochDay in clusterStartEpoch..(clusterLastBloodEpoch + 1)

        val activeStart = if (isCurrentlyActive) clusterStartEpoch else null
        val activeStop = if (isCurrentlyActive) {
            clusterStartEpoch + usualHaid - 1
        } else null

        // Next cycle prediction
        val nextStart = if (isCurrentlyActive) {
            clusterStartEpoch + cycleLength
        } else {
            val baseStart = clusterStartEpoch
            var projected = baseStart + cycleLength
            while (projected < todayEpochDay - 10) {
                projected += cycleLength
            }
            projected
        }
        val nextStop = nextStart + usualHaid - 1

        val summary = if (isCurrentlyActive) {
            val daysSoFar = (todayEpochDay - clusterStartEpoch + 1).toInt().coerceAtLeast(1)
            val daysRemainingToAdat = (activeStop!! - todayEpochDay).toInt()
            val predictedStopText = formatDateFromEpoch(activeStop)

            val statusTitle = "Hari ke-$daysSoFar Haid Aktif"
            val statusDesc = if (daysRemainingToAdat > 0) {
                "Berdasarkan adat ($usualHaid hari), darah diprediksi akan berhenti pada $predictedStopText (sekitar $daysRemainingToAdat hari lagi)."
            } else if (daysRemainingToAdat == 0) {
                "Hari ini adalah hari prediksi terakhir masa haid Anda ($usualHaid hari). Periksa tanda suci (cairan putih/kering)."
            } else if (daysSoFar <= 15) {
                "Darah telah melewati adat $usualHaid hari, namun masih dalam batas maksimal 15 hari mazhab Syafi'i. Tetap tinggalkan shalat & puasa hingga suci atau mencapai 15 hari."
            } else {
                "PERINGATAN: Melebihi 15 hari! Status darah adalah Istihadhah. Wajib mandi besar dan qadha shalat sesuai ketentuan fiqih."
            }

            CyclePredictionSummary(
                currentStatusTitle = statusTitle,
                currentStatusDesc = statusDesc,
                isActiveHaid = true,
                activeHaidDaysSoFar = daysSoFar,
                predictedStopDateText = predictedStopText,
                daysUntilPredictedStop = daysRemainingToAdat.coerceAtLeast(0),
                predictedNextHaidStartText = formatDateFromEpoch(nextStart),
                daysUntilNextHaid = (nextStart - todayEpochDay).toInt().coerceAtLeast(0),
                predictedNextHaidStopText = formatDateFromEpoch(nextStop),
                fiqihNote = "Kaidah Syafi'i: Masa suci minimal antar dua haid wajib 15 hari 15 malam. Maksimal haid adalah 15 hari 15 malam."
            )
        } else {
            val daysSinceStop = (todayEpochDay - clusterLastBloodEpoch).toInt().coerceAtLeast(0)
            val daysUntilNext = (nextStart - todayEpochDay).toInt()
            val predictedNextStartText = formatDateFromEpoch(nextStart)
            val predictedNextStopText = formatDateFromEpoch(nextStop)

            val statusTitle = "Masa Suci (Hari ke-$daysSinceStop)"
            val statusDesc = if (daysUntilNext > 0) {
                "Haid berikutnya diprediksi akan keluar pada $predictedNextStartText (sekitar $daysUntilNext hari lagi) dan diprediksi berhenti pada $predictedNextStopText."
            } else if (daysUntilNext == 0) {
                "Haid berikutnya diprediksi mulai keluar HARI INI ($predictedNextStartText). Bersiap dan catat jika darah muncul."
            } else {
                "Siklus mendekati/melewati perkiraan tanggal ($predictedNextStartText). Segera catat saat darah pertama kali keluar."
            }

            CyclePredictionSummary(
                currentStatusTitle = statusTitle,
                currentStatusDesc = statusDesc,
                isActiveHaid = false,
                activeHaidDaysSoFar = 0,
                predictedStopDateText = null,
                daysUntilPredictedStop = null,
                predictedNextHaidStartText = predictedNextStartText,
                daysUntilNextHaid = daysUntilNext.coerceAtLeast(0),
                predictedNextHaidStopText = predictedNextStopText,
                fiqihNote = "Masa suci minimal 15 hari menurut Syafi'i. Jika ada darah keluar sebelum lewat 15 hari masa suci, maka darah tersebut dihukumi Istihadhah (darah rusak/fasid)."
            )
        }

        return CyclePredictionData(
            activeCycleStartEpochDay = activeStart,
            activeCycleStopEpochDay = activeStop,
            nextCycleStartEpochDay = nextStart,
            nextCycleStopEpochDay = nextStop,
            summary = summary
        )
    }

    private fun formatDateFromEpoch(epochDay: Long): String {
        val cal = CalendarDateHelper.fromEpochDay(epochDay)
        return CalendarDateHelper.DISPLAY_SHORT_DATE.format(cal.time)
    }
}

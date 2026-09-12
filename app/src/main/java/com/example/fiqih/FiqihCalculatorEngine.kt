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
        previousSuciDaysForTakmilah: Int = 15
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
            val thickStr = if (p.isThick) "Kental" else "Encer"
            val odorStr = if (p.isOdorous) "Berbau Anyir" else "Tidak Berbau"
            val strengthLabel = if (p.isStrongBlood) "Darah Kuat (Qawi)" else "Darah Lemah (Dha'if)"
            "Fase ${idx + 1}: Darah ${p.bloodColor.shortName} ($thickStr, $odorStr) | $startStr s/d $endStr (${p.formattedDuration}) -> $strengthLabel"
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
                previousSuciDaysForTakmilah = previousSuciDaysForTakmilah
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
                previousHaidAdatDays = previousHaidAdatDays
            )
        }
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
        previousSuciDaysForTakmilah: Int
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

        // Kasus 2: Jeda suci di sela haid (Fatrah Naqa') >= 15 hari
        if (hasIntermittentPause && intermittentPauseDays >= 15.0) {
            val summary = "Darah Terputus Jeda Suci Sah (Pemisah Dua Siklus Haid)"
            val category = "Pemisah Dua Haid Berdasarkan Batas Minimal Suci 15 Hari"
            val haidSeg = PeriodSegment(
                title = "Masa Haid Pertama",
                startEpochMillis = startEpochMillis,
                endEpochMillis = startEpochMillis + (intervals.firstOrNull()?.durationMillis ?: totalDurationMs / 2),
                status = "Haid Sah",
                description = "Darah sebelum jeda suci 15 hari dihukumi haid pertama yang sempurna."
            )
            val suciSeg = PeriodSegment(
                title = "Masa Suci Pemisah (Naqa')",
                startEpochMillis = startEpochMillis + (intervals.firstOrNull()?.durationMillis ?: totalDurationMs / 2),
                endEpochMillis = endEpochMillis - (intervals.lastOrNull()?.durationMillis ?: totalDurationMs / 2),
                status = "Suci Sah",
                description = "Jeda suci bersih $intermittentPauseDays hari memenuhi syarat minimal suci 15 hari (aqalluth thuhr)."
            )

            val shalatConsequence = "Selama masa haid shalat gugur. Selama jeda suci $intermittentPauseDays hari, seluruh shalat fardhu wajib dikerjakan. Darah berikutnya setelah jeda 15 hari dihukumi haid siklus baru."
            val mandiNote = "Wajib mandi besar jinabat saat darah pertama berhenti masuk jeda suci, dan wajib mandi kembali setelah darah kedua berhenti."

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
                categoryDetectionReason = "Otomatis terdeteksi sebagai Pemisah Dua Haid karena terdapat jeda bersih $intermittentPauseDays hari di sela darah (memenuhi batas minimal suci 15 hari). Darah kedua berstatus siklus haid baru.",
                haidOrNifasSegment = haidSeg,
                istihadhahSegment = null,
                suciSegment = suciSeg,
                shalatConsequence = shalatConsequence,
                qadhaPrayers = qadhaList,
                puasaConsequence = "Puasa pada masa jeda suci sah dan wajib dikerjakan. Puasa saat darah keluar wajib diqadha.",
                mandiWajibNote = mandiNote,
                mustahadhahCareGuide = null,
                medicalAndSpiritualAdvice = "Siklus haid dengan jeda suci bersih normal adalah proses fisiologis yang sehat.",
                references = refs,
                phaseBreakdowns = phaseBreakdowns
            )
        }

        // Kasus 3: Istihadhah Takmilatan lit-Tuhri (Suci Belum Genap 15 Hari)
        if (previousSuciDaysForTakmilah in 1..14) {
            val takmilahDaysRequired = 15 - previousSuciDaysForTakmilah
            val takmilahMs = takmilahDaysRequired * ONE_DAY_MS

            if (totalDurationMs <= takmilahMs) {
                // Seluruh darah adalah Istihadhah penyempurna masa suci
                val summary = "Istihadhah Takmilatan lit-Tuhri ($totalDays Hari $remainingHours Jam)"
                val category = "Istihadhah Penyempurna Masa Suci (Takmilatan lit-Tuhri)"
                val istihadhahSegment = PeriodSegment(
                    title = "Masa Istihadhah (Penyempurna Suci)",
                    startEpochMillis = startEpochMillis,
                    endEpochMillis = endEpochMillis,
                    status = "Istihadhah (Takmilah)",
                    description = "Darah keluar sebelum masa suci mencapai 15 hari. Seluruh darah dihukumi Istihadhah untuk menyempurnakan masa suci menjadi 15 hari."
                )

                val shalatConsequence = "WAJIB SHALAT. Darah ini dihukumi Istihadhah (darah penyakit), bukan haid. Anda wajib shalat dengan tata cara mustahadhah (wudhu tiap masuk waktu). Shalat yang terlanjur ditinggalkan WAJIB DIQADHA."
                val puasaConsequence = "Puasa fardhu sah dan wajib dijalankan."
                val mandiNote = "Tidak wajib mandi besar, karena ini bukan darah haid."
                val advice = "Darah ini merupakan Istihadhah karena masa suci Anda sebelumnya kurang dari 15 hari."

                val refs = listOf(
                    KitabReference(
                        kitabName = "Tuhfatun Niswah",
                        volumeAndPage = "Bab Istihadhah Takmilatan lit-Tuhri",
                        ibaratSnippet = "Darah yang keluar sebelum masa suci genap 15 hari maka ia berstatus sebagai istihadhah penyempurna masa suci (takmilatan lit-tuhri).",
                        explanation = "Masa suci minimal adalah 15 hari. Jika suci kurang dari 15 hari lalu darah keluar lagi, darah itu dihukumi istihadhah sampai genap 15 hari suci."
                    )
                )

                return CalculationResult(
                    statusSummary = summary,
                    caseCategory = category,
                    categoryDetectionReason = "Otomatis terdeteksi karena masa suci Anda sebelumnya hanya $previousSuciDaysForTakmilah hari (kurang dari 15 hari). Darah yang keluar selama $totalDays Hari $remainingHours Jam ini seluruhnya dihukumi Istihadhah sebagai penyempurna masa suci.",
                    haidOrNifasSegment = null,
                    istihadhahSegment = istihadhahSegment,
                    suciSegment = null,
                    shalatConsequence = shalatConsequence,
                    qadhaPrayers = qadhaList,
                    puasaConsequence = puasaConsequence,
                    mandiWajibNote = mandiNote,
                    mustahadhahCareGuide = "TATA CARA SHALAT MUSTAHADHAH:\n1. Bersihkan kemaluan.\n2. Balut dengan pembalut.\n3. Wudhu SETELAH masuk waktu shalat (niat istibahah).\n4. Segera kerjakan shalat.",
                    medicalAndSpiritualAdvice = advice,
                    references = refs,
                    phaseBreakdowns = phaseBreakdowns
                )
            } else {
                // Sebagian takmilah, sebagian haid (atau mustahadhah jika total sisanya > 15)
                val remainingMsAfterTakmilah = totalDurationMs - takmilahMs
                val takmilahEndEpoch = startEpochMillis + takmilahMs
                
                val takmilahDays = takmilahMs / ONE_DAY_MS
                val remainingDays = remainingMsAfterTakmilah / ONE_DAY_MS
                val remainingHoursAfter = (remainingMsAfterTakmilah % ONE_DAY_MS) / ONE_HOUR_MS

                if (remainingMsAfterTakmilah <= FIFTEEN_DAYS_MS) {
                    if (remainingMsAfterTakmilah >= ONE_DAY_MS) {
                        // Takmilah + Haid Sah
                        val summary = "Istihadhah Takmilah ($takmilahDays Hari) & Haid ($remainingDays Hari $remainingHoursAfter Jam)"
                        val category = "Istihadhah Takmilah + Haid Normal"
                        val istihadhahSegment = PeriodSegment(
                            title = "Masa Istihadhah (Penyempurna Suci)",
                            startEpochMillis = startEpochMillis,
                            endEpochMillis = takmilahEndEpoch,
                            status = "Istihadhah (Takmilah)",
                            description = "Darah selama $takmilahDays hari dihukumi Istihadhah untuk menggenapkan masa suci sebelumnya ($previousSuciDaysForTakmilah hari) menjadi 15 hari."
                        )
                        val haidSegment = PeriodSegment(
                            title = "Masa Haid Sah",
                            startEpochMillis = takmilahEndEpoch,
                            endEpochMillis = endEpochMillis,
                            status = "Haid Sah",
                            description = "Setelah genap masa suci 15 hari, sisa darah mencapai 24 jam dan tidak lebih dari 15 hari, maka dihukumi HAID."
                        )
                        return CalculationResult(
                            statusSummary = summary,
                            caseCategory = category,
                            categoryDetectionReason = "Masa suci sebelumnya $previousSuciDaysForTakmilah hari. $takmilahDays hari pertama adalah Istihadhah Takmilah. Sisa darah ($remainingDays Hari $remainingHoursAfter Jam) memenuhi syarat haid.",
                            haidOrNifasSegment = haidSegment,
                            istihadhahSegment = istihadhahSegment,
                            suciSegment = null,
                            shalatConsequence = "Shalat WAJIB DIQADHA selama masa Istihadhah $takmilahDays hari. Selama masa Haid, shalat gugur.",
                            qadhaPrayers = qadhaList,
                            puasaConsequence = "Puasa sah pada saat istihadhah, batal/wajib qadha pada saat haid.",
                            mandiWajibNote = "Mandi wajib dilakukan setelah masa darah selesai (karena berstatus haid di bagian akhir).",
                            mustahadhahCareGuide = "TATA CARA SHALAT MUSTAHADHAH (Pada masa Istihadhah):\n1. Bersihkan kemaluan.\n2. Balut dengan pembalut.\n3. Wudhu SETELAH masuk waktu shalat.\n4. Segera kerjakan shalat.",
                            medicalAndSpiritualAdvice = "Siklus Anda mengalami pemendekan masa suci.",
                            references = listOf(),
                            phaseBreakdowns = phaseBreakdowns
                        )
                    } else {
                        // Takmilah + Fasad (kurang dari 24 jam)
                        val summary = "Istihadhah Takmilah ($takmilahDays Hari) & Darah Fasad"
                        val category = "Istihadhah Takmilah + Darah Rusak"
                        val istihadhahSegment = PeriodSegment(
                            title = "Masa Istihadhah (Takmilah & Fasad)",
                            startEpochMillis = startEpochMillis,
                            endEpochMillis = endEpochMillis,
                            status = "Istihadhah Total",
                            description = "Sisa darah setelah masa suci genap kurang dari 24 jam, sehingga seluruh darah dihukumi bukan haid."
                        )
                        return CalculationResult(
                            statusSummary = summary,
                            caseCategory = category,
                            categoryDetectionReason = "Setelah dikurangi Istihadhah Takmilah $takmilahDays hari, sisa darah kurang dari 24 jam, maka seluruhnya adalah darah fasad/istihadhah.",
                            haidOrNifasSegment = null,
                            istihadhahSegment = istihadhahSegment,
                            suciSegment = null,
                            shalatConsequence = "Shalat WAJIB DIQADHA untuk seluruh periode darah ini.",
                            qadhaPrayers = qadhaList,
                            puasaConsequence = "Puasa fardhu sah.",
                            mandiWajibNote = "Tidak wajib mandi besar.",
                            mustahadhahCareGuide = "Gunakan wudhu untuk tiap waktu shalat (niat istibahah).",
                            medicalAndSpiritualAdvice = "Seluruh darah ini bukan haid.",
                            references = listOf(),
                            phaseBreakdowns = phaseBreakdowns
                        )
                    }
                }
                // Jika setelah takmilah, sisanya > 15 hari, itu sangat kompleks (Mustahadhah yang melewati Takmilah). 
                // Kita akan biarkan jatuh ke Kasus 4 (Mustahadhah reguler) dengan memberikan fallback logis di sini
                // Untuk simplifikasi aplikasi pada tahap ini, jika melebihi 15 hari setelah takmilah, kita kembalikan ke adat.
                // Namun untuk tidak membuat function ini terlalu panjang, kita cukupkan penanganan spesifik di sini, dan abaikan jika lebih dari 15 (jatuh ke bawah).
            }
        }

        // Kasus 4: Darah 24 Jam s/d 15 Hari (Haid Normal Sah)
        if (totalDurationMs <= FIFTEEN_DAYS_MS) {
            val summary = "Haid Sah: $totalDays Hari $remainingHours Jam (0 Jam Istihadhah)"
            val category = if (totalDays in 6..7) "Haid Normal (Ghalib 6-7 Hari)" else "Haid Sah (Batas 1-15 Hari)"
            val pauseNote = if (hasIntermittentPause) " (Termasuk jeda bersih < 15 hari dihukumi haid menurut Qaul Sahbi)" else ""
            val haidSegment = PeriodSegment(
                title = "Masa Haid Sah",
                startEpochMillis = startEpochMillis,
                endEpochMillis = endEpochMillis,
                status = "Haid Sah",
                description = "Darah keluar memenuhi syarat minimal 24 jam dan tidak melampaui batas maksimal 15 hari 15 malam$pauseNote."
            )

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
                phaseBreakdowns = phaseBreakdowns
            )
        }

        // =========================================================================
        // Kasus 4: ISTIHADHAH FIL-HAID (Darah Melebihi 15 Hari 15 Malam)
        // 7 Golongan Mustahadhah fil-Haid menurut Uyunul Masa-il Linnisa' Bab IV & Tuhfatun Niswah
        // =========================================================================
        val distinctScores = intervals.map { it.totalScore }.distinct()
        val hasVariedCharacteristics = distinctScores.size > 1
        val isMubtadiah = !hasPreviousAdat || adatMemoryType == AdatMemoryType.BELUM_PERNAH_HAID || haidCategory == HaidCategory.MUBTADIAH_MUMAYYIZAH || haidCategory == HaidCategory.MUBTADIAH_GHAIRU_MUMAYYIZAH

        var isTamyizValid = false
        var strongSegmentStart = startEpochMillis
        var strongSegmentEnd = startEpochMillis
        var weakSegmentStart = startEpochMillis
        var weakSegmentEnd = endEpochMillis

        if (hasVariedCharacteristics) {
            val maxScore = intervals.maxOf { it.totalScore }
            val strongPhases = intervals.filter { it.totalScore == maxScore }
            val weakPhases = intervals.filter { it.totalScore < maxScore }

            val strongDurationMs = strongPhases.sumOf { it.durationMillis }
            val weakDurationMs = weakPhases.sumOf { it.durationMillis }

            // 4 Syarat Tamyiz Sah menurut Uyunul Masa-il Linnisa' hal. 67 & Tuhfatun Niswah hal. 28:
            // 1. Darah kuat tidak kurang dari 24 jam
            // 2. Darah kuat tidak melebihi 15 hari
            // 3. Darah lemah tidak kurang dari 15 hari jika bersambung di tengah
            // 4. Darah kuat bersambung (tidak terputus-putus)
            val cond1 = strongDurationMs >= ONE_DAY_MS
            val cond2 = strongDurationMs <= FIFTEEN_DAYS_MS
            val cond3 = weakDurationMs >= FIFTEEN_DAYS_MS || (weakDurationMs > 0 && intervals.last().totalScore < maxScore)
            val cond4 = strongPhases.size == 1 || (strongPhases.last().endEpochMillis - strongPhases.first().startEpochMillis == strongDurationMs)

            if (cond1 && cond2 && cond3 && cond4) {
                isTamyizValid = true
                strongSegmentStart = strongPhases.minOf { it.startEpochMillis }
                strongSegmentEnd = strongPhases.maxOf { it.endEpochMillis }
                weakSegmentStart = weakPhases.minOf { it.startEpochMillis }
                weakSegmentEnd = weakPhases.maxOf { it.endEpochMillis }
            }
        }

        val haidDurationMs: Long
        val istihadhahDurationMs: Long
        val categoryName: String
        val categoryReason: String
        val shalatNote: String
        val mandiNote: String
        val haidSeg: PeriodSegment
        val istiSeg: PeriodSegment
        val customRefs = mutableListOf<KitabReference>()

        // Evaluasi 7 Golongan:
        if (isTamyizValid) {
            // Golongan 1 atau 3: Mumayyizah (Tamyiz didahulukan)
            haidDurationMs = strongSegmentEnd - strongSegmentStart
            istihadhahDurationMs = totalDurationMs - haidDurationMs

            val strongDays = haidDurationMs / ONE_DAY_MS
            val strongRemHours = (haidDurationMs % ONE_DAY_MS) / ONE_HOUR_MS
            val istiDays = istihadhahDurationMs / ONE_DAY_MS
            val istiRemHours = (istihadhahDurationMs % ONE_DAY_MS) / ONE_HOUR_MS

            if (isMubtadiah) {
                categoryName = "1. Mubtadi'ah Mumayyizah (Pemula dengan Tamyiz)"
                categoryReason = "Otomatis terdeteksi sebagai 1. Mubtadi'ah Mumayyizah karena: Anda baru pertama kali haid (Mubtadi'ah), darah keluar melebihi 15 hari ($totalDays Hari), dan terdapat perbedaan darah kuat ($strongDays Hari $strongRemHours Jam) serta darah lemah yang sah memenuhi 4 syarat Tamyiz menurut Mazhab Syafi'i (darah kuat dihukumi haid sah, darah lemah dihukumi istihadhah)."
            } else {
                categoryName = "3. Mu'tadah Mumayyizah (Pernah Haid & Tamyiz Mengalahkan Adat)"
                categoryReason = "Otomatis terdeteksi sebagai 3. Mu'tadah Mumayyizah karena: Anda sudah pernah haid sebelumnya (Mu'tadah), darah melebihi 15 hari ($totalDays Hari), dan darah Anda memenuhi 4 syarat Tamyiz sah. Berdasarkan kaidah fiqih kitab Uyunul Masa-il Linnisa' & Tuhfatun Niswah, hukum Tamyiz mengalahkan hukum adat bulan sebelumnya."
            }

            haidSeg = PeriodSegment(
                title = "Masa Haid Sah (Darah Kuat / Qawi)",
                startEpochMillis = strongSegmentStart,
                endEpochMillis = strongSegmentEnd,
                status = "Haid Sah",
                description = "Darah kuat ($strongDays Hari $strongRemHours Jam) dihukumi haid sah karena memenuhi 4 syarat Tamyiz menurut Mazhab Syafi'i."
            )

            istiSeg = PeriodSegment(
                title = "Masa Istihadhah (Darah Lemah / Dha'if)",
                startEpochMillis = weakSegmentStart,
                endEpochMillis = weakSegmentEnd,
                status = "Istihadhah",
                description = "Darah lemah ($istiDays Hari $istiRemHours Jam) dihukumi ISTIHADHAH. Wajib wudhu istibahah setiap shalat fardhu."
            )

            shalatNote = "Shalat di masa darah kuat ($strongDays Hari) GUGUR. Shalat di masa darah lemah yang sempat ditinggalkan sebelum hari ke-15 WAJIB DIQADHA. Selanjutnya wajib shalat tepat waktu."
            mandiNote = "Wajib mandi besar segera ketika darah kuat beralih ke darah lemah (atau setelah hari ke-15 saat baru mengetahui istihadhah)."

            customRefs.add(
                KitabReference(
                    kitabName = "Uyunul Masa-il Linnisa' (Lirboyo)",
                    volumeAndPage = "Bab IV: Tujuh Golongan Mustahadhah, hal. 66-70",
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
                description = "Darah mulai jam ke-25 sampai hari ke-15 dan seterusnya adalah ISTIHADHAH. Shalat 14 hari yang sempat ditinggalkan WAJIB DIQADHA seluruhnya!"
            )

            shalatNote = "PERINGATAN FIQIH: Haid Anda hanya 24 jam pertama. Shalat dari hari ke-2 sampai hari ke-15 yang kemarin ditinggalkan WAJIB DIQADHA (14 hari shalat fardhu). Mulai hari ke-16 wajib shalat tepat waktu."
            mandiNote = "Wajib mandi besar jinabat pada hari ke-15 (saat menyadari istihadhah), lalu segera mengqadha shalat 14 hari yang terutang."

            customRefs.add(
                KitabReference(
                    kitabName = "Uyunul Masa-il Linnisa' (Lirboyo)",
                    volumeAndPage = "Bab IV, hal. 70-73",
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
                    volumeAndPage = "Bab IV, hal. 73-77",
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
            haidDurationMs = ONE_DAY_MS
            istihadhahDurationMs = totalDurationMs - haidDurationMs

            categoryName = "5. Mu'tadah Nasiyah (Mutahayyirah Mahdhah / Lupa Semuanya)"
            categoryReason = "Otomatis terdeteksi sebagai 5. Mu'tadah Nasiyah (Mutahayyirah Mahdhah) karena: Anda pernah haid sebelumnya, darah melebihi 15 hari tanpa tamyiz, dan Anda lupa sama sekali durasi maupun waktu jatuhnya haid. Anda berstatus Mutahayyirah dan wajib berhati-hati (ihtiyath) dengan bersuci dan shalat di tiap waktu fardhu."

            haidSeg = PeriodSegment(
                title = "Masa Ihtiyath (Kehati-hatian: 24 Jam Pertama)",
                startEpochMillis = startEpochMillis,
                endEpochMillis = startEpochMillis + ONE_DAY_MS,
                status = "Ihtiyath",
                description = "24 jam pertama diyakini sebagai batas minimal haid, wajib berhati-hati (ihtiyath)."
            )

            istiSeg = PeriodSegment(
                title = "Masa Istihadhah Mutahayyirah",
                startEpochMillis = startEpochMillis + ONE_DAY_MS,
                endEpochMillis = endEpochMillis,
                status = "Istihadhah Mutahayyirah",
                description = "Wajib bersuci (mandi/wudhu) setiap masuk waktu shalat fardhu dan tetap wajib shalat serta puasa."
            )

            shalatNote = "Wajib IHTIYATH: Tetap wajib shalat lima waktu dengan bersuci setiap masuk waktu fardhu. Dilarang jima' dan tidak membaca Al-Qur'an di luar shalat."
            mandiNote = "Wajib bersuci (mandi jinabat atau wudhu menurut khilaf ulama mu'tamad) setiap kali masuk waktu shalat fardhu."

            customRefs.add(
                KitabReference(
                    kitabName = "Uyunul Masa-il Linnisa' (Lirboyo)",
                    volumeAndPage = "Bab IV, hal. 84-88",
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
        } else if (adatMemoryType == AdatMemoryType.INGAT_WAQTAN_LUPA_QADRAN || haidCategory == HaidCategory.MUTADAH_DZAKIRAH_WAQTAN_DUNAN_QADR) {
            // Golongan 6: Mu'tadah Dzakirah lil-Waqti dunan Qadr
            haidDurationMs = ONE_DAY_MS
            istihadhahDurationMs = totalDurationMs - haidDurationMs

            categoryName = "6. Mu'tadah Dzakirah lil-Waqti dunan Qadr (Ingat Waktu Saja)"
            categoryReason = "Otomatis terdeteksi sebagai 6. Mu'tadah Dzakirah lil-Waqti dunan Qadr karena: Anda pernah haid sebelumnya, darah melebihi 15 hari tanpa tamyiz, dan Anda mengingat waktu mulai haid namun lupa jumlah harinya. 24 jam pertama berstatus haid yakin, dan hari ke-2 s/d 15 berstatus ihtiyath."

            haidSeg = PeriodSegment(
                title = "Masa Haid Yakin (24 Jam dari Waktu Mulai)",
                startEpochMillis = startEpochMillis,
                endEpochMillis = startEpochMillis + ONE_DAY_MS,
                status = "Haid Yakin",
                description = "Waktu mulai diingat, sehingga 24 jam pertama diyakini haid."
            )

            istiSeg = PeriodSegment(
                title = "Masa Ihtiyath & Istihadhah",
                startEpochMillis = startEpochMillis + ONE_DAY_MS,
                endEpochMillis = endEpochMillis,
                status = "Ihtiyath",
                description = "Hari ke-2 sampai ke-15 berstatus ihtiyat (wajib shalat), hari ke-16 ke atas istihadhah murni."
            )

            shalatNote = "Hari ke-1 shalat gugur. Hari ke-2 sampai ke-15 wajib shalat atas dasar ihtiyath (mandi di akhir kemungkinan haid). Hari ke-16 ke atas istihadhah murni."
            mandiNote = "Wajib mandi pada setiap waktu yang dimungkinkan putusnya darah haid."

            customRefs.add(
                KitabReference(
                    kitabName = "Uyunul Masa-il Linnisa' (Lirboyo)",
                    volumeAndPage = "Bab IV, hal. 80-82",
                    ibaratSnippet = "الذاكرة للوقت دون القدر تجعل أول دمها حيضا يقينا يوما وليلة وتحتاط في الباقي إلى خمسة عشر",
                    explanation = "Wanita yang ingat waktu saja menetapkan awal darah sebagai haid yakin (24 jam) dan berhati-hati pada sisa hari sampai hari ke-15."
                )
            )
        } else {
            // Golongan 7: Mu'tadah Dzakirah lil-Qadri dunan Waqt
            val validAdatDays = (adatDurationDays.takeIf { it in 1..15 } ?: 7).toLong()
            haidDurationMs = validAdatDays * ONE_DAY_MS
            istihadhahDurationMs = totalDurationMs - haidDurationMs

            categoryName = "7. Mu'tadah Dzakirah lil-Qadri dunan Waqt (Ingat Durasi Saja)"
            categoryReason = "Otomatis terdeteksi sebagai 7. Mu'tadah Dzakirah lil-Qadri dunan Waqt karena: Anda pernah haid sebelumnya, darah melebihi 15 hari tanpa tamyiz, dan Anda hanya mengingat durasi adat harinya ($validAdatDays hari) namun lupa kapan tanggal mulainya. Berlaku hukum ihtiyath sepanjang 15 hari."

            haidSeg = PeriodSegment(
                title = "Masa Ihtiyath Kadar Adat ($validAdatDays Hari)",
                startEpochMillis = startEpochMillis,
                endEpochMillis = startEpochMillis + haidDurationMs,
                status = "Ihtiyath Kadar Adat",
                description = "Durasi $validAdatDays hari diyakini sebagai kadar haid, namun harinya diragukan sehingga berlaku hukum ihtiyath."
            )

            istiSeg = PeriodSegment(
                title = "Masa Istihadhah",
                startEpochMillis = startEpochMillis + haidDurationMs,
                endEpochMillis = endEpochMillis,
                status = "Istihadhah",
                description = "Darah sesudah masa kadar adat dihukumi istihadhah."
            )

            shalatNote = "Wajib berhati-hati (ihtiyath) sepanjang rentang 15 hari pertama: tetap shalat dan berwudhu setiap masuk waktu shalat."
            mandiNote = "Mandi wajib dilakukan pada hari yang dimungkinkan sebagai akhir dari putusnya darah haid adat."

            customRefs.add(
                KitabReference(
                    kitabName = "Uyunul Masa-il Linnisa' (Lirboyo)",
                    volumeAndPage = "Bab IV, hal. 82-84",
                    ibaratSnippet = "الذاكرة للقدر دون الوقت تحتاط في جميع مدة الإمكان لأن كل جزء يحتمل الحيض والطهر",
                    explanation = "Wanita yang ingat kadar saja wajib ihtiyath sepanjang masa kemungkinan karena tiap bagian berpotensi antara haid dan suci."
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
            medicalAndSpiritualAdvice = medicalAdvice,
            references = customRefs,
            phaseBreakdowns = phaseBreakdowns
        )
    }

    // =========================================================================
    // PERHITUNGAN NIFAS (Kitab Uyunul Masa'il Linnisa' & Tuhfatun Niswah)
    // 5 Golongan Utama + 2 Catatan Mustahadhah fin-Nifas
    // =========================================================================
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
        previousHaidAdatDays: Int
    ): CalculationResult {
        // Tentukan waktu patokan persalinan:
        val effectiveDeliveryTime = if (deliveryType == DeliveryType.BAYI_KEMBAR && twinLastDeliveryEpochMillis != null) {
            twinLastDeliveryEpochMillis
        } else {
            deliveryEpochMillis ?: startEpochMillis
        }

        // 1. KAIDAH JEDA MELAHIRKAN KE DARAH PERTAMA (Fashl baina al-wiladah wad dam):
        // Jika darah baru keluar >= 15 hari dari melahirkan, TIDAK ADA NIFAS!
        if (deliveryEpochMillis != null && (startEpochMillis - effectiveDeliveryTime) >= FIFTEEN_DAYS_MS) {
            val gapDays = (startEpochMillis - effectiveDeliveryTime) / ONE_DAY_MS
            val summary = "Bukan Nifas (Jeda Bersih dari Melahirkan $gapDays Hari >= 15 Hari)"
            val category = "Gugur Status Nifas (Fashl baina al-Wiladah wad Dam >= 15 Hari)"
            val seg = PeriodSegment(
                title = "Masa Darah Haid / Istihadhah (Bukan Nifas)",
                startEpochMillis = startEpochMillis,
                endEpochMillis = endEpochMillis,
                status = "Bukan Nifas",
                description = "Karena jeda antara melahirkan dan keluarnya darah mencapai $gapDays hari (>= 15 hari), maka darah ini tidak dihukumi nifas menurut Mazhab Syafi'i, melainkan darah haid baru (jika memenuhi syarat haid) atau istihadhah."
            )

            val shalatConsequence = "Ibu wajib mandi wiladah (karena melahirkan) sejak persalinan. Darah yang keluar sekarang dihukumi sebagai darah haid baru bila mencapai 24 jam dan <= 15 hari (shalat gugur), atau istihadhah bila melebihi 15 hari."
            val mandiNote = "WAJIB MANDI WILADAH (karena telah melahirkan). Jika darah saat ini adalah haid, wajib mandi haid setelah darah berhenti."

            val refs = listOf(
                KitabReference(
                    kitabName = "Uyunul Masa-il Linnisa' (Lirboyo)",
                    volumeAndPage = "Bab III: Definisi & Syarat Nifas, hal. 49",
                    ibaratSnippet = "فإن رأت الدم بعد مضي خمسة عشر يوما من الولادة فليس بنفاس بل هو حيض إن صلح له وإلا فاستحاضة وتغتسل للولادة",
                    explanation = "Bila wanita melihat darah setelah lewat 15 hari dari melahirkan, maka itu bukan nifas melainkan haid jika memenuhi syarat, atau istihadhah, dan ia wajib mandi wiladah."
                ),
                KitabReference(
                    kitabName = "Tuhfatun Niswah",
                    volumeAndPage = "Fashl fi an-Nifas, hal. 18",
                    ibaratSnippet = "Syarat darah nifas adalah keluarnya tidak berselang 15 hari atau lebih dari persalinan.",
                    explanation = "Jeda 15 hari suci menggugurkan status nifas dan mengembalikan darah ke hukum haid."
                )
            )

            return CalculationResult(
                statusSummary = summary,
                caseCategory = category,
                categoryDetectionReason = "Otomatis terdeteksi: Status nifas gugur karena terdapat jeda bersih $gapDays hari (>= 15 hari) pasca persalinan sebelum darah pertama keluar. Darah ini berstatus haid jika memenuhi syarat, dan Anda wajib mandi wiladah.",
                haidOrNifasSegment = null,
                istihadhahSegment = seg,
                suciSegment = null,
                shalatConsequence = shalatConsequence,
                qadhaPrayers = qadhaList,
                puasaConsequence = "Puasa selama masa jeda suci sah. Puasa saat darah keluar dihukumi sesuai status haid.",
                mandiWajibNote = mandiNote,
                mustahadhahCareGuide = null,
                medicalAndSpiritualAdvice = "Keluarnya darah setelah 2 pekan lebih bersih pasca melahirkan memerlukan pemeriksaan USG oleh dokter Sp.OG untuk memastikan kebersihan rahim.",
                references = refs,
                phaseBreakdowns = phaseBreakdowns
            )
        }

        // 2. KAIDAH JEDA BERSIH DI TENGAH NIFAS (Fatrah Naqa' di sela nifas):
        // Jika jeda bersih >= 15 hari di tengah nifas, maka nifas terputus dan darah kedua adalah HAID BARU!
        if (hasIntermittentPause && intermittentPauseDays >= 15.0) {
            val summary = "Nifas Terputus Jeda Suci 15 Hari (Darah Kedua Adalah Haid Baru)"
            val category = "Pemisah Nifas dan Haid Berdasarkan Jeda Suci 15 Hari"
            val nifasSeg = PeriodSegment(
                title = "Masa Nifas Sah Pertama",
                startEpochMillis = startEpochMillis,
                endEpochMillis = startEpochMillis + (intervals.firstOrNull()?.durationMillis ?: (totalDurationMs / 2)),
                status = "Nifas Sah",
                description = "Darah sebelum jeda suci 15 hari adalah darah nifas sah."
            )
            val haidBaruSeg = PeriodSegment(
                title = "Masa Darah Haid Baru (Bukan Nifas)",
                startEpochMillis = endEpochMillis - (intervals.lastOrNull()?.durationMillis ?: (totalDurationMs / 2)),
                endEpochMillis = endEpochMillis,
                status = "Haid Baru",
                description = "Karena diselingi masa bersih $intermittentPauseDays hari (>= 15 hari), maka darah kedua bukan lagi nifas melainkan darah haid baru."
            )

            val shalatConsequence = "Shalat selama nifas pertama gugur. Shalat selama masa suci $intermittentPauseDays hari wajib ditunaikan. Darah kedua dihukumi darah haid baru (shalat gugur jika 24 jam - 15 hari)."
            val mandiNote = "Wajib mandi nifas saat darah pertama berhenti masuk jeda suci, dan wajib mandi haid setelah darah kedua berhenti."

            val refs = listOf(
                KitabReference(
                    kitabName = "Uyunul Masa-il Linnisa' (Lirboyo)",
                    volumeAndPage = "Bab III, hal. 51",
                    ibaratSnippet = "إذا تخلل بين الدمين في النفاس نقاء خمسة عشر يوما فأكثر فالدم الثاني حيض لانقطاع النفاس بالطهر التام",
                    explanation = "Bila di antara dua darah nifas diselingi masa suci 15 hari atau lebih, maka darah kedua adalah haid karena nifas telah terputus oleh masa suci yang sempurna."
                ),
                KitabReference(
                    kitabName = "Tuhfatun Niswah",
                    volumeAndPage = "Fashl fi an-Nifas, hal. 19",
                    ibaratSnippet = "Jeda suci 15 hari di tengah nifas memutus masa nifas secara mutlak.",
                    explanation = "Darah berikutnya berstatus haid baru."
                )
            )

            return CalculationResult(
                statusSummary = summary,
                caseCategory = category,
                categoryDetectionReason = "Otomatis terdeteksi: Nifas terputus oleh jeda suci sempurna $intermittentPauseDays hari (>= 15 hari). Darah kedua dihukumi sebagai darah haid baru.",
                haidOrNifasSegment = nifasSeg,
                istihadhahSegment = haidBaruSeg,
                suciSegment = null,
                shalatConsequence = shalatConsequence,
                qadhaPrayers = qadhaList,
                puasaConsequence = "Puasa selama jeda suci sah. Puasa saat nifas dan haid wajib diqadha.",
                mandiWajibNote = mandiNote,
                mustahadhahCareGuide = null,
                medicalAndSpiritualAdvice = "Siklus haid telah kembali pasca melahirkan, merupakan tanda kembalinya fungsi ovulasi tubuh.",
                references = refs,
                phaseBreakdowns = phaseBreakdowns
            )
        }

        // 3. KAIDAH NIFAS SAH (Darah <= 60 Hari 60 Malam dari Melahirkan)
        val durationFromDeliveryMs = (endEpochMillis - effectiveDeliveryTime).coerceAtLeast(totalDurationMs)
        if (durationFromDeliveryMs <= SIXTY_DAYS_MS && totalDurationMs <= SIXTY_DAYS_MS) {
            val summary = "Nifas Sah: $totalDays Hari $remainingHours Jam (0 Jam Istihadhah)"
            val twinText = if (deliveryType == DeliveryType.BAYI_KEMBAR) " (Dihitung pasca lahirnya bayi terakhir)" else ""
            val miscarriageText = if (deliveryType == DeliveryType.KEGUGURAN) " (Pasca keguguran/mudhghah tetap sah nifas)" else ""
            val category = if (totalDays <= 40) "Nifas Normal (Ghalib 40 Hari)$twinText$miscarriageText" else "Nifas Sah (Dalam Batas Maksimal 60 Hari)$twinText"

            val nifasSegment = PeriodSegment(
                title = "Masa Nifas Sah",
                startEpochMillis = startEpochMillis,
                endEpochMillis = endEpochMillis,
                status = "Nifas Sah",
                description = "Darah keluar pasca melahirkan dalam rentang tidak melebihi batas maksimal nifas 60 hari 60 malam (Qaul Sahbi menurut Mazhab Syafi'i)."
            )

            val shalatConsequence = "Seluruh shalat selama masa nifas ini GUGUR dan TIDAK WAJIB DIQADHA. Shalat wajib dilakukan kembali setelah darah bersih tuntas dan mandi wajib."
            val puasaConsequence = "Puasa Ramadhan yang terlewat selama masa nifas WAJIB DIQADHA di hari lain di luar bulan Ramadhan."
            val mandiNote = "WAJIB MANDI WILADAH & NIFAS setelah darah bersih tuntas dengan niat: 'Nawaitul ghusla liraf'il hadatsil akbari minan nifasi wal wiladati fardhan lillahi ta'ala'."
            val advice = "Masa nifas memerlukan pemulihan fisik yang prima. Jaga asupan nutrisi protein tinggi, cairan cukup untuk kelancaran ASI, dan kebersihan perineum secara lembut."

            val refs = listOf(
                KitabReference(
                    kitabName = "Uyunul Masa-il Linnisa' (Lirboyo)",
                    volumeAndPage = "Bab III, hal. 48-51",
                    ibaratSnippet = "أقل النفاس لحظة وأكثره ستون يوما وغالبه أربعون يوما... وحيث لم يجاوز الستين فالكل نفاس سحبا",
                    explanation = "Paling sedikitnya nifas adalah sekejap (lahzhah), paling banyaknya 60 hari 60 malam, dan lumrahnya 40 hari. Selagi tidak melampaui 60 hari, seluruh darah dihukumi nifas."
                ),
                KitabReference(
                    kitabName = "Tuhfatun Niswah",
                    volumeAndPage = "Fashl fi an-Nifas, hal. 17 & 20",
                    ibaratSnippet = "Bila darah terputus-putus dalam rentang 60 hari dan masa sucinya kurang dari 15 hari, maka seluruhnya dihukumi darah nifas.",
                    explanation = "Mandi wiladah dan nifas dapat digabungkan dalam satu niat mandi besar begitu darah berhenti bersih."
                )
            )

            return CalculationResult(
                statusSummary = summary,
                caseCategory = category,
                categoryDetectionReason = "Otomatis terdeteksi sebagai Nifas Sah / Normal karena total keluarnya darah ($totalDays Hari $remainingHours Jam) tidak melampaui batas maksimal nifas 60 hari 60 malam menurut Mazhab Syafi'i.",
                haidOrNifasSegment = nifasSegment,
                istihadhahSegment = null,
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
        // 4. ISTIHADHAH FIN-NIFAS (Darah Melebihi 60 Hari 60 Malam)
        // Menurut Kitab Uyunul Masa'il Linnisa' (hal. 91-94) & Tuhfatun Niswah (hal. 36-38):
        // 5 Golongan Utama + 2 Tambahan dalam Catatan (Tanbih):
        // 1. Mubtadi'ah Mumayyizah fin-Nifas
        // 2. Mubtadi'ah Ghairu Mumayyizah fin-Nifas
        // 3. Mu'tadah Mumayyizah fin-Nifas
        // 4. Mu'tadah Ghairu Mumayyizah Dzakirah fin-Nifas
        // 5. Mu'tadah Ghairu Mumayyizah Nasiyah (Mutahayyirah fin-Nifas)
        // Catatan 1: Dzakirah lil-Waqti dunan Qadr fin-Nifas
        // Catatan 2: Dzakirah lil-Qadri dunan Waqt fin-Nifas
        // =========================================================================
        val distinctScores = intervals.map { it.totalScore }.distinct()
        val hasVariedCharacteristics = distinctScores.size > 1
        val isMubtadiahNifas = !hasPreviousAdat ||
                adatMemoryType == AdatMemoryType.BELUM_PERNAH_HAID ||
                nifasAdatCategory == NifasAdatCategory.MUBTADIAH_MUMAYYIZAH ||
                nifasAdatCategory == NifasAdatCategory.MUBTADIAH_GHAIRU_MUMAYYIZAH

        var isTamyizValid = false
        var strongSegmentStart = startEpochMillis
        var strongSegmentEnd = startEpochMillis
        var weakSegmentStart = startEpochMillis
        var weakSegmentEnd = endEpochMillis

        if (hasVariedCharacteristics) {
            val maxScore = intervals.maxOf { it.totalScore }
            val strongPhases = intervals.filter { it.totalScore == maxScore }
            val weakPhases = intervals.filter { it.totalScore < maxScore }

            val strongDurationMs = strongPhases.sumOf { it.durationMillis }
            val weakDurationMs = weakPhases.sumOf { it.durationMillis }

            // Syarat Tamyiz fin-Nifas menurut Uyunul Masa-il Linnisa' hal. 91 & Tuhfatun Niswah hal. 36:
            // 1. Darah kuat tidak kurang dari lahzhah (sekejap)
            // 2. Darah kuat tidak melebihi 60 hari
            // 3. Darah lemah tidak kurang dari 15 hari jika di tengah atau bersambung sampai akhir
            val cond1 = strongDurationMs > 0
            val cond2 = strongDurationMs <= SIXTY_DAYS_MS
            val cond3 = weakDurationMs >= FIFTEEN_DAYS_MS || (weakDurationMs > 0 && intervals.last().totalScore < maxScore)

            if (cond1 && cond2 && cond3) {
                isTamyizValid = true
                strongSegmentStart = strongPhases.minOf { it.startEpochMillis }
                strongSegmentEnd = strongPhases.maxOf { it.endEpochMillis }
                weakSegmentStart = weakPhases.minOf { it.startEpochMillis }
                weakSegmentEnd = weakPhases.maxOf { it.endEpochMillis }
            }
        }

        val nifasDurationMs: Long
        val istihadhahDurationMs: Long
        val categoryName: String
        val categoryReason: String
        val shalatNote: String
        val mandiNote: String
        val nifasSeg: PeriodSegment
        val istiSeg: PeriodSegment
        val customRefs = mutableListOf<KitabReference>()

        if (isTamyizValid) {
            // Golongan 1 atau 3: Mumayyizah fin-Nifas (Tamyiz Mengalahkan Adat)
            nifasDurationMs = strongSegmentEnd - strongSegmentStart
            istihadhahDurationMs = totalDurationMs - nifasDurationMs

            val strongDays = nifasDurationMs / ONE_DAY_MS
            val strongRemHours = (nifasDurationMs % ONE_DAY_MS) / ONE_HOUR_MS
            val istiDays = istihadhahDurationMs / ONE_DAY_MS
            val istiRemHours = (istihadhahDurationMs % ONE_DAY_MS) / ONE_HOUR_MS

            if (isMubtadiahNifas) {
                categoryName = "1. Mubtadi'ah Mumayyizah fin-Nifas (Kelahiran Pertama & Ada Tamyiz)"
                categoryReason = "Otomatis terdeteksi sebagai 1. Mubtadi'ah Mumayyizah fin-Nifas karena: Ini kelahiran pertama Anda (Mubtadi'ah fin-nifas), darah melebihi 60 hari ($totalDays Hari), dan terdapat perbedaan darah kuat ($strongDays Hari $strongRemHours Jam) serta darah lemah yang sah memenuhi syarat Tamyiz fin-Nifas (darah kuat dihukumi nifas sah, darah lemah dihukumi istihadhah)."
            } else {
                categoryName = "3. Mu'tadah Mumayyizah fin-Nifas (Pernah Nifas & Tamyiz Mengalahkan Adat)"
                categoryReason = "Otomatis terdeteksi sebagai 3. Mu'tadah Mumayyizah fin-Nifas karena: Anda pernah melahirkan sebelumnya (Mu'tadah fin-nifas), darah melebihi 60 hari ($totalDays Hari), dan darah Anda memenuhi syarat Tamyiz sah. Berdasarkan kitab Uyunul Masa-il Linnisa' & Tuhfatun Niswah, hukum Tamyiz mengalahkan adat nifas anak sebelumnya."
            }

            nifasSeg = PeriodSegment(
                title = "Masa Nifas Sah (Darah Kuat / Qawi)",
                startEpochMillis = strongSegmentStart,
                endEpochMillis = strongSegmentEnd,
                status = "Nifas Sah",
                description = "Darah kuat ($strongDays Hari $strongRemHours Jam) dihukumi NIFAS SAH berdasarkan kaidah Tamyiz Istihadhah fin-Nifas."
            )

            istiSeg = PeriodSegment(
                title = "Masa Istihadhah fin-Nifas (Darah Lemah / Dha'if)",
                startEpochMillis = weakSegmentStart,
                endEpochMillis = weakSegmentEnd,
                status = "Istihadhah fin-Nifas",
                description = "Darah lemah ($istiDays Hari $istiRemHours Jam) dihukumi ISTIHADHAH FIN-NIFAS. Shalat di masa darah lemah wajib diqadha jika sempat ditinggalkan."
            )

            shalatNote = "Shalat selama masa darah kuat ($strongDays Hari) GUGUR. Shalat selama masa darah lemah yang sempat ditinggalkan sebelum hari ke-60 WAJIB DIQADHA seluruhnya. Selanjutnya wajib shalat dengan tata cara bersuci mustahadhah."
            mandiNote = "Wajib mandi besar segera ketika darah kuat beralih ke darah lemah (atau setelah genap 60 hari pada persalinan pertama saat menyadari istihadhah)."

            customRefs.add(
                KitabReference(
                    kitabName = "Uyunul Masa-il Linnisa' (Lirboyo)",
                    volumeAndPage = "Bab V: Istihadhah fin-Nifas, hal. 91",
                    ibaratSnippet = "المبتدأة المميزة في النفاس ترد إلى التمييز فالقوي نفاس والضعيف استحاضة بشرط أن لا ينقص القوي عن لحظة ولا يزيد على ستين يوما",
                    explanation = "Mubtadi'ah Mumayyizah fin-Nifas dikembalikan pada tamyiz: darah kuat adalah nifas dan darah lemah adalah istihadhah, dengan syarat darah kuat tidak kurang dari lahzhah dan tidak melampaui 60 hari."
                )
            )
            customRefs.add(
                KitabReference(
                    kitabName = "Tuhfatun Niswah",
                    volumeAndPage = "Fashl fi Istihadhah an-Nifas, hal. 36",
                    ibaratSnippet = "Bila pemula nifas mengeluarkan darah melebihi 60 hari dan darahnya berbeda (kuat dan lemah), maka darah kuat adalah nifas dan darah lemah adalah istihadhah.",
                    explanation = "Tamyiz didahulukan atas adat kebiasaan. Shalat yang tertinggal di masa darah lemah wajib diqadha."
                )
            )
        } else if (isMubtadiahNifas) {
            // Golongan 2: Mubtadi'ah Ghairu Mumayyizah fin-Nifas
            // Nifasnya hanya LAHZHAH (sekejap / 1 hari) pasca melahirkan!
            nifasDurationMs = ONE_DAY_MS // 1 hari / lahzhah (representasi di kalkulator)
            istihadhahDurationMs = totalDurationMs - nifasDurationMs

            if (hasPreviousHaidBeforeNifas) {
                // Poin B: Sudah pernah haid (Mu'tadah fil-haid)
                categoryName = "2. Mubtadi'ah Ghairu Mumayyizah fin-Nifas (Poin B: Sudah Pernah Haid)"
                categoryReason = "Otomatis terdeteksi (Poin B): Ini kelahiran pertama tanpa tamyiz, dan Anda sudah pernah haid. Nifas dikembalikan ke lahzhah (sekejap). Darah sesudahnya dihukumi istihadhah selama masa adat suci Anda sebelumnya, kemudian dihukumi haid selama adat haid Anda ($previousHaidAdatDays hari)."
                
                nifasSeg = PeriodSegment(
                    title = "Masa Nifas Sah (Majjah / Lahzhah)",
                    startEpochMillis = startEpochMillis,
                    endEpochMillis = startEpochMillis + ONE_DAY_MS,
                    status = "Nifas Sah",
                    description = "Nifas pemula tanpa tamyiz hanya majjah (sekejap tetesan pasca bersalin / 1 hari)."
                )

                istiSeg = PeriodSegment(
                    title = "Masa Istihadhah (Masa Suci) & Siklus Haid",
                    startEpochMillis = startEpochMillis + ONE_DAY_MS,
                    endEpochMillis = endEpochMillis,
                    status = "Istihadhah & Haid Berulang",
                    description = "Setelah nifas sekejap, darah dihukumi istihadhah selama masa adat suci Anda sebelumnya, kemudian baru dihukumi haid selama $previousHaidAdatDays hari (siklus adat kembali)."
                )

                shalatNote = "PERINGATAN BERAT: Anda WAJIB MENGQADHA SHALAT selama masa istihadhah (yang sempat ditinggalkan karena dikira nifas)."
            } else {
                // Poin A: Belum pernah haid sama sekali (Mubtadi'ah fil-haid)
                categoryName = "2. Mubtadi'ah Ghairu Mumayyizah fin-Nifas (Poin A: Belum Pernah Haid)"
                categoryReason = "Otomatis terdeteksi (Poin A): Ini kelahiran pertama tanpa tamyiz, dan Anda belum pernah haid. Nifas dikembalikan ke lahzhah (sekejap). Darah sesudahnya dihukumi dengan siklus: 29 hari istihadhah (dihukumi suci) dan 1 hari haid, berulang seterusnya."
                
                nifasSeg = PeriodSegment(
                    title = "Masa Nifas Sah (Majjah / Lahzhah)",
                    startEpochMillis = startEpochMillis,
                    endEpochMillis = startEpochMillis + ONE_DAY_MS,
                    status = "Nifas Sah",
                    description = "Nifas pemula tanpa tamyiz hanya majjah (sekejap tetesan pasca bersalin / 1 hari)."
                )

                istiSeg = PeriodSegment(
                    title = "Masa Istihadhah (29 Hari) & Haid (1 Hari)",
                    startEpochMillis = startEpochMillis + ONE_DAY_MS,
                    endEpochMillis = endEpochMillis,
                    status = "Istihadhah (29 Hari) & Haid (1 Hari)",
                    description = "Setelah nifas sekejap, diberlakukan siklus 29 hari istihadhah (suci) lalu 1 hari haid, dan terus berulang."
                )

                shalatNote = "PERINGATAN BERAT: Anda WAJIB MENGQADHA SHALAT selama masa istihadhah (29 hari pertama dan siklus suci selanjutnya) yang sempat ditinggalkan karena dikira nifas."
            }

            mandiNote = "Wajib mandi besar jinabat pada hari ke-60 (saat baru terbukti darah melampaui batas maksimal nifas), lalu segera mengqadha shalat."

            customRefs.add(
                KitabReference(
                    kitabName = "Uyunul Masa-il Linnisa' (Lirboyo)",
                    volumeAndPage = "Bab V: Istihadhah fin-Nifas, hal. 92",
                    ibaratSnippet = "المبتدأة غير المميزة في النفاس ترد إلى أقل النفاس وهو لحظة وقيل يوم وليلة وتقضي صلاة تسعة وخمسين يوما وتغتسل عند انقضاء الستين",
                    explanation = "Mubtadi'ah Ghairu Mumayyizah fin-Nifas dikembalikan kepada batas minimal nifas yaitu sekejap (lahzhah), dan WAJIB MENGQADHA SHALAT 59 HARI yang ia tinggalkan, serta mandi di akhir 60 hari."
                )
            )
            customRefs.add(
                KitabReference(
                    kitabName = "Tuhfatun Niswah",
                    volumeAndPage = "Hal. 36-37",
                    ibaratSnippet = "Bila darah pemula nifas melewati 60 hari dan warnanya satu rupa, maka nifasnya hanya sekejap (lahzhah) dan wajib mengqadha shalat 59 hari.",
                    explanation = "Ketetapan hukum shalat 59 hari wajib diqadha karena terbukti darah tersebut bukan nifas melainkan istihadhah."
                )
            )
        } else if (nifasAdatCategory == NifasAdatCategory.MUTADAH_NASIYAH_MUTAHAYYIRAH || adatMemoryType == AdatMemoryType.LUPA_SEMUANYA_MUTAHAYYIRAH) {
            // Golongan 5: Mu'tadah Ghairu Mumayyizah Nasiyah (Mutahayyirah fin-Nifas / Lupa Adat Nifasnya)
            nifasDurationMs = ONE_DAY_MS // Mengambil 1 hari untuk nifas yakin
            istihadhahDurationMs = totalDurationMs - nifasDurationMs

            categoryName = "5. Mu'tadah Nasiyah (Mutahayyirah fin-Nifas / Lupa Adat Total)"
            categoryReason = "Otomatis terdeteksi sebagai 5. Mu'tadah Nasiyah (Mutahayyirah fin-Nifas) karena: Pernah melahirkan sebelumnya, darah keluar melebihi 60 hari tanpa tamyiz, dan Anda lupa sama sekali durasi maupun waktu nifas sebelumnya. Berlaku hukum ihtiyath (kehati-hatian) mutahayyirah fin-nifas."

            nifasSeg = PeriodSegment(
                title = "Masa Nifas Yakin (1 Hari / Lahzhah)",
                startEpochMillis = startEpochMillis,
                endEpochMillis = startEpochMillis + ONE_DAY_MS,
                status = "Nifas Yakin",
                description = "1 hari pasca persalinan diyakini nifas, selebihnya berstatus ihtiyath."
            )

            istiSeg = PeriodSegment(
                title = "Masa Ihtiyath Mutahayyirah fin-Nifas",
                startEpochMillis = startEpochMillis + ONE_DAY_MS,
                endEpochMillis = endEpochMillis,
                status = "Ihtiyath fin-Nifas",
                description = "Wajib berhati-hati: mandi/wudhu tiap shalat fardhu, tetap shalat dan puasa, dilarang jima'."
            )

            shalatNote = "Wajib IHTIYATH: Hari ke-1 shalat gugur. Hari ke-2 sampai ke-60 wajib shalat fardhu dengan bersuci tiap waktu shalat, dan shalat yang sempat ditinggalkan diqadha atas dasar kehati-hatian."
            mandiNote = "Wajib bersuci (mandi jinabat atau wudhu) setiap kali masuk waktu shalat fardhu menurut kaidah ihtiyath mutahayyirah."

            customRefs.add(
                KitabReference(
                    kitabName = "Uyunul Masa-il Linnisa' (Lirboyo)",
                    volumeAndPage = "Bab V: Istihadhah fin-Nifas, hal. 94",
                    ibaratSnippet = "المعتادة الناسية لعادتها في النفاس حكمها حكم المتحيرة في الحيض فتأخذ باليقين وتحتاط في الباقي",
                    explanation = "Mu'tadah Nasiyah dalam nifas hukumnya seperti Mutahayyirah dalam haid: mengambil yang yakin (lahzhah) dan berhati-hati (ihtiyath) pada sisa hari."
                )
            )
            customRefs.add(
                KitabReference(
                    kitabName = "Tuhfatun Niswah",
                    volumeAndPage = "Hal. 38",
                    ibaratSnippet = "Bila lupa adat nifasnya, maka ia menjadi mutahayyirah dalam nifas dan wajib ihtiyath.",
                    explanation = "Kewajiban ihtiyath berlaku sama seperti mutahayyirah haid."
                )
            )
        } else if (nifasAdatCategory == NifasAdatCategory.CATATAN_DZAKIRAH_WAQTAN_LUPA_QADRAN || adatMemoryType == AdatMemoryType.INGAT_WAQTAN_LUPA_QADRAN) {
            // Catatan 1: Dzakirah lil-Waqti dunan Qadr fin-Nifas
            nifasDurationMs = ONE_DAY_MS
            istihadhahDurationMs = totalDurationMs - nifasDurationMs

            categoryName = "Catatan 1: Dzakirah lil-Waqti dunan Qadr fin-Nifas (Ingat Waktu Mulai Saja)"
            categoryReason = "Otomatis terdeteksi sebagai Catatan 1: Dzakirah lil-Waqti dunan Qadr fin-Nifas karena: Pernah melahirkan sebelumnya, darah melebihi 60 hari tanpa tamyiz, dan Anda mengingat waktu melahirkan namun lupa durasi adat harinya. Awal waktu dihukumi nifas yakin (1 hari) dan sisa hingga 60 hari berstatus ihtiyath."

            nifasSeg = PeriodSegment(
                title = "Masa Nifas Yakin (1 Hari dari Waktu Mulai)",
                startEpochMillis = startEpochMillis,
                endEpochMillis = startEpochMillis + ONE_DAY_MS,
                status = "Nifas Yakin",
                description = "Waktu mulai nifas diingat, sehingga 1 hari pertama diyakini nifas."
            )

            istiSeg = PeriodSegment(
                title = "Masa Ihtiyath & Istihadhah fin-Nifas",
                startEpochMillis = startEpochMillis + ONE_DAY_MS,
                endEpochMillis = endEpochMillis,
                status = "Ihtiyath fin-Nifas",
                description = "Hari ke-2 sampai ke-60 berstatus ihtiyath (wajib shalat), hari ke-61 ke atas istihadhah murni."
            )

            shalatNote = "Hari ke-1 shalat gugur. Hari ke-2 sampai ke-60 wajib shalat fardhu atas dasar ihtiyath. Shalat yang kemarin ditinggalkan diqadha."
            mandiNote = "Wajib mandi pada hari yang dimungkinkan sebagai akhir dari nifasnya."

            customRefs.add(
                KitabReference(
                    kitabName = "Uyunul Masa-il Linnisa' (Lirboyo)",
                    volumeAndPage = "Bab V: Tanbih Istihadhah fin-Nifas, hal. 94",
                    ibaratSnippet = "تنبيه: إن ذكرت الوقت دون القدر جعلت أول وقتها نفاسا بيقين لحظة واحتاطت إلى ستين",
                    explanation = "Catatan Penting: Jika mengingat waktu saja tanpa kadar, jadikan awal waktunya sebagai nifas yakin sekejap (lahzhah) dan berhati-hati hingga 60 hari."
                )
            )
            customRefs.add(
                KitabReference(
                    kitabName = "Tuhfatun Niswah",
                    volumeAndPage = "Catatan Istihadhah Nifas, hal. 38",
                    ibaratSnippet = "Ingat waktu nifas saja tanpa kadar harinya: mengambil nifas yakin dan ihtiyath hingga batas 60 hari.",
                    explanation = "Ketetapan hukum catatan kitab untuk kasus ingat waktu saja."
                )
            )
        } else if (nifasAdatCategory == NifasAdatCategory.CATATAN_DZAKIRAH_QADRAN_LUPA_WAQTAN || adatMemoryType == AdatMemoryType.INGAT_QADRAN_LUPA_WAQTAN) {
            // Catatan 2: Dzakirah lil-Qadri dunan Waqt fin-Nifas
            val validAdatDays = (adatDurationDays.takeIf { it in 1..60 } ?: 40).toLong()
            nifasDurationMs = validAdatDays * ONE_DAY_MS
            istihadhahDurationMs = totalDurationMs - nifasDurationMs

            categoryName = "Catatan 2: Dzakirah lil-Qadri dunan Waqt fin-Nifas (Ingat Durasi Saja)"
            categoryReason = "Otomatis terdeteksi sebagai Catatan 2: Dzakirah lil-Qadri dunan Waqt fin-Nifas karena: Pernah melahirkan sebelumnya, darah melebihi 60 hari tanpa tamyiz, dan Anda mengingat durasi adat nifas ($validAdatDays hari) namun lupa tanggal pastinya. Nifas ditetapkan sesuai durasi adat ($validAdatDays hari)."

            nifasSeg = PeriodSegment(
                title = "Masa Nifas Adat Yakin ($validAdatDays Hari)",
                startEpochMillis = startEpochMillis,
                endEpochMillis = startEpochMillis + nifasDurationMs,
                status = "Nifas Adat",
                description = "Durasi $validAdatDays hari diyakini sebagai kadar adat nifas, namun waktu mulainya diragukan sehingga berlaku hukum ihtiyath."
            )

            istiSeg = PeriodSegment(
                title = "Masa Istihadhah fin-Nifas",
                startEpochMillis = startEpochMillis + nifasDurationMs,
                endEpochMillis = endEpochMillis,
                status = "Istihadhah fin-Nifas",
                description = "Darah sesudah kadar adat dihukumi istihadhah fin-nifas."
            )

            shalatNote = "Shalat selama $validAdatDays hari adat gugur. Hari ke-${validAdatDays + 1} sampai ke-60 yang ditinggalkan WAJIB DIQADHA (${60 - validAdatDays} hari shalat fardhu)."
            mandiNote = "Wajib mandi pada akhir hari ke-60 dan mengqadha shalat yang ditinggalkan."

            customRefs.add(
                KitabReference(
                    kitabName = "Uyunul Masa-il Linnisa' (Lirboyo)",
                    volumeAndPage = "Bab V: Tanbih Istihadhah fin-Nifas, hal. 94",
                    ibaratSnippet = "تنبيه: وإن ذكرت القدر دون الوقت ردت إلى قدر عادتها وتتحرى في وقتها وتحتاط في المشكوك فيه",
                    explanation = "Catatan Penting: Jika mengingat kadar saja tanpa waktu, dikembalikan ke kadar adatnya, memperkirakan waktunya, dan berhati-hati pada yang diragukan."
                )
            )
            customRefs.add(
                KitabReference(
                    kitabName = "Tuhfatun Niswah",
                    volumeAndPage = "Catatan Istihadhah Nifas, hal. 38",
                    ibaratSnippet = "Ingat kadar hari nifas saja tanpa waktu: nifas sesuai kadar hari yang diingat.",
                    explanation = "Ketetapan hukum catatan kitab untuk kasus ingat kadar saja."
                )
            )
        } else {
            // Golongan 4: Mu'tadah Ghairu Mumayyizah Dzakirah (Ingat Adat Lengkap)
            val validAdatDays = (adatDurationDays.takeIf { it in 1..60 } ?: 40).toLong()
            nifasDurationMs = validAdatDays * ONE_DAY_MS
            istihadhahDurationMs = totalDurationMs - nifasDurationMs

            categoryName = "4. Mu'tadah Ghairu Mumayyizah Dzakirah (Pernah Nifas & Ingat Adat: $validAdatDays Hari)"
            categoryReason = "Otomatis terdeteksi sebagai 4. Mu'tadah Ghairu Mumayyizah Dzakirah fin-Nifas karena: Pernah melahirkan sebelumnya, darah melebihi 60 hari tanpa tamyiz, dan Anda mengingat adat nifas kelahiran sebelumnya ($validAdatDays hari). Nifas ditetapkan $validAdatDays hari sesuai adat, dan shalat dari hari ke-${validAdatDays + 1} s/d 60 wajib diqadha."

            nifasSeg = PeriodSegment(
                title = "Masa Nifas Sah (Sesuai Adat Nifas Sebelumnya)",
                startEpochMillis = startEpochMillis,
                endEpochMillis = startEpochMillis + nifasDurationMs,
                status = "Nifas Sah",
                description = "Nifas ditetapkan sesuai adat nifas persalinan sebelumnya yaitu $validAdatDays hari."
            )

            istiSeg = PeriodSegment(
                title = "Masa Istihadhah fin-Nifas",
                startEpochMillis = startEpochMillis + nifasDurationMs,
                endEpochMillis = endEpochMillis,
                status = "Istihadhah fin-Nifas",
                description = "Darah dari hari ke-${validAdatDays + 1} sampai hari ke-60 dan seterusnya dihukumi ISTIHADHAH. Shalat yang sempat ditinggalkan di hari ke-${validAdatDays + 1} s/d 60 WAJIB DIQADHA."
            )

            shalatNote = "Nifas sah adalah $validAdatDays hari (sesuai adat). Shalat dari hari ke-${validAdatDays + 1} sampai hari ke-60 yang sempat ditinggalkan WAJIB DIQADHA (${60 - validAdatDays} hari shalat fardhu). Hari ke-61 ke atas wajib shalat tepat waktu."
            mandiNote = "Mandi besar dilakukan di akhir hari ke-60 saat terbukti terjadi istihadhah (pada persalinan ini), dan selanjutnya langsung mengqadha shalat yang terutang."

            customRefs.add(
                KitabReference(
                    kitabName = "Uyunul Masa-il Linnisa' (Lirboyo)",
                    volumeAndPage = "Bab V: Istihadhah fin-Nifas, hal. 94",
                    ibaratSnippet = "المعتادة غير المميزة في النفاس ترد إلى قدر عادتها وتقضي ما زاد على عادتها إلى ستين يوما",
                    explanation = "Mu'tadah Ghairu Mumayyizah fin-Nifas dikembalikan kepada kadar adat nifas sebelumnya, dan wajib mengqadha shalat yang ia tinggalkan di atas masa adatnya hingga genap 60 hari."
                )
            )
            customRefs.add(
                KitabReference(
                    kitabName = "Tuhfatun Niswah",
                    volumeAndPage = "Hal. 37-38",
                    ibaratSnippet = "Dikembalikan kepada adat nifas sebelumnya, selebihnya adalah istihadhah dan shalatnya wajib diqadha.",
                    explanation = "Bila adat nifasnya 40 hari, maka dari hari ke-41 s/d 60 wajib diqadha shalatnya."
                )
            )
        }

        val nifasDays = nifasDurationMs / ONE_DAY_MS
        val nifasRemHours = (nifasDurationMs % ONE_DAY_MS) / ONE_HOUR_MS
        val istiDays = istihadhahDurationMs / ONE_DAY_MS
        val istiRemHours = (istihadhahDurationMs % ONE_DAY_MS) / ONE_HOUR_MS

        val summary = "Nifas Sah: $nifasDays Hari $nifasRemHours Jam | Istihadhah Nifas: $istiDays Hari $istiRemHours Jam"

        val mustahadhahGuide = """
            TATA CARA BERSUCI MUSTAHADHAH FIN-NIFAS (DA'IMUL HADATS):
            1. Bersihkan area intim dari najis darah (istinja').
            2. Balut rapat dengan pembalut bersih ('ashbu).
            3. Berwudhu SETELAH MASUK WAKTU SHALAT FARDHU dengan niat istibahah:
               نَوَيْتُ الْوُضُوْءَ لِاسْتِبَاحَةِ الصَّلَاةِ فَرْضًا لِلَّهِ تَعَالَى
               ('Nawaitul wudhu'a li-istibaahatish sholaati fardhan lillahi ta'ala').
            4. Segera tunaikan shalat fardhu (1 wudhu untuk 1 shalat fardhu).
        """.trimIndent()

        val medicalAdvice = "Pendarahan lebih dari 60 hari pasca persalinan memerlukan perhatian medis segera dari dokter spesialis obstetri & ginekologi (Sp.OG). Pemeriksaan USG sangat penting untuk mengevaluasi involusi uterus, kemungkinan retensi sisa plasenta, infeksi endometrium, atau luka jalan lahir."

        return CalculationResult(
            statusSummary = summary,
            caseCategory = categoryName,
            categoryDetectionReason = categoryReason,
            haidOrNifasSegment = nifasSeg,
            istihadhahSegment = istiSeg,
            suciSegment = null,
            shalatConsequence = shalatNote,
            qadhaPrayers = qadhaList,
            puasaConsequence = "Puasa Ramadhan pada masa nifas sah wajib diqadha di luar bulan Ramadhan. Puasa pada masa istihadhah nifas tetap sah.",
            mandiWajibNote = mandiNote,
            mustahadhahCareGuide = mustahadhahGuide,
            medicalAndSpiritualAdvice = medicalAdvice,
            references = customRefs,
            phaseBreakdowns = phaseBreakdowns
        )
    }
}

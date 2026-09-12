package com.example.model

enum class CaseType(val displayName: String) {
    HAID("Haid (Menstruasi)"),
    NIFAS("Nifas (Pasca Melahirkan)")
}

enum class DeliveryType(val label: String, val description: String) {
    TUNGGAL_NORMAL_SESAR("Bayi Tunggal (Normal / Sesar)", "Kelahiran bayi tunggal normal atau operasi sesar"),
    BAYI_KEMBAR("Bayi Kembar (2 atau Lebih)", "Nifas dihitung pasca lahirnya bayi terakhir menurut mazhab Syafi'i"),
    KEGUGURAN("Keguguran (Mudhghah / 'Alaqah)", "Keluarnya janin belum sempurna, segumpal daging atau darah; darah sesudahnya tetap nifas")
}

enum class AdatMemoryType(val label: String) {
    INGAT_LENGKAP_QADRAN_WAQTAN("Ingat Lengkap (Durasi Hari & Waktu Mulai)"),
    INGAT_QADRAN_LUPA_WAQTAN("Ingat Durasi Hari Saja, Lupa Waktu Mulai"),
    INGAT_WAQTAN_LUPA_QADRAN("Ingat Waktu Mulai Saja, Lupa Durasi Hari"),
    LUPA_SEMUANYA_MUTAHAYYIRAH("Lupa Semuanya (Mutahayyirah / Kebingungan)"),
    BELUM_PERNAH_HAID("Pemula (Mubtadi'ah / Kelahiran Pertama)")
}

// 7 Golongan Mustahadhah fil-Haid menurut Uyunul Masa-il Linnisa' & Tuhfatun Niswah
enum class HaidCategory(val label: String, val shortDesc: String) {
    MUBTADIAH_MUMAYYIZAH("1. Mubtadi'ah Mumayyizah", "Pemula dan darah memenuhi syarat tamyiz"),
    MUBTADIAH_GHAIRU_MUMAYYIZAH("2. Mubtadi'ah Ghairu Mumayyizah", "Pemula dan darah tidak memenuhi tamyiz (1 warna/tidak sah tamyiz)"),
    MUTADAH_MUMAYYIZAH("3. Mu'tadah Mumayyizah", "Pernah haid dan darah memenuhi syarat tamyiz"),
    MUTADAH_GHAIRU_MUMAYYIZAH_DZAKIRAH_QADRAN_WAQTAN("4. Mu'tadah Ghairu Mumayyizah (Ingat Lengkap)", "Pernah haid, darah satu warna, ingat durasi dan waktu siklus"),
    MUTADAH_NASIYAH_MUTAHAYYIRAH("5. Mu'tadah Nasiyah (Mutahayyirah Mahdhah)", "Pernah haid, darah satu warna, lupa total durasi & waktu adat"),
    MUTADAH_DZAKIRAH_WAQTAN_DUNAN_QADR("6. Mu'tadah Dzakirah lil-Waqti dunan Qadr", "Ingat waktu mulai haid saja, lupa durasi jumlah harinya"),
    MUTADAH_DZAKIRAH_QADRAN_DUNAN_WAQT("7. Mu'tadah Dzakirah lil-Qadri dunan Waqt", "Ingat durasi jumlah hari haid saja, lupa kapan waktu mulainya")
}

// 5 Golongan + 2 Catatan Mustahadhah fin-Nifas menurut Uyunul Masa-il Linnisa' & Tuhfatun Niswah
enum class NifasAdatCategory(val label: String, val shortDesc: String) {
    MUBTADIAH_MUMAYYIZAH("1. Mubtadi'ah Mumayyizah fin-Nifas", "Kelahiran pertama & darah memenuhi syarat tamyiz (kuat/lemah)"),
    MUBTADIAH_GHAIRU_MUMAYYIZAH("2. Mubtadi'ah Ghairu Mumayyizah fin-Nifas", "Kelahiran pertama & darah satu warna / tidak memenuhi syarat tamyiz"),
    MUTADAH_MUMAYYIZAH("3. Mu'tadah Mumayyizah fin-Nifas", "Pernah melahirkan sebelumnya & darah memenuhi syarat tamyiz"),
    MUTADAH_GHAIRU_MUMAYYIZAH_DZAKIRAH("4. Mu'tadah Ghairu Mumayyizah Dzakirah", "Pernah nifas, darah seragam, dan ingat durasi adat nifas sebelumnya"),
    MUTADAH_NASIYAH_MUTAHAYYIRAH("5. Mu'tadah Nasiyah (Mutahayyirah fin-Nifas)", "Pernah nifas, darah seragam, dan lupa total adat nifas sebelumnya"),
    // 2 Tambahan dalam Catatan (Tanbih Penting Kitab Uyunul Masail & Tuhfatun Niswah):
    CATATAN_DZAKIRAH_WAQTAN_LUPA_QADRAN("Catatan 1: Dzakirah lil-Waqti dunan Qadri", "Ingat kapan mulai nifas, namun lupa berapa hari adat nifasnya"),
    CATATAN_DZAKIRAH_QADRAN_LUPA_WAQTAN("Catatan 2: Dzakirah lil-Qadri dunan Waqti", "Ingat durasi hari adat nifasnya (misal 40 hari), namun lupa tanggal/waktu jatuhnya")
}

enum class BloodColor(val label: String, val shortName: String, val score: Int, val hexColor: Long) {
    HITAM("Hitam (Paling Kuat - Nilai: 5)", "Hitam", 5, 0xFF212121),
    MERAH("Merah (Kuat - Nilai: 4)", "Merah", 4, 0xFFC62828),
    COKLAT("Cokelat (Sedang - Nilai: 3)", "Cokelat", 3, 0xFF6D4C41),
    KUNING("Kuning (Lemah - Nilai: 2)", "Kuning", 2, 0xFFFBC02D),
    KERUH("Keruh (Paling Lemah - Nilai: 1)", "Keruh", 1, 0xFFB0BEC5)
}

enum class PrayerName(val label: String, val approxStart: String, val approxEnd: String) {
    SUBUH("Subuh", "04:30", "05:45"),
    DZUHUR("Dzuhur", "11:55", "15:10"),
    ASHAR("Ashar", "15:15", "17:55"),
    MAGHRIB("Maghrib", "18:00", "19:10"),
    ISYA("Isya", "19:15", "04:25")
}

data class BleedingInterval(
    val id: String = java.util.UUID.randomUUID().toString(),
    val startEpochMillis: Long,
    val endEpochMillis: Long,
    val bloodColor: BloodColor = BloodColor.MERAH,
    val isThick: Boolean = true,
    val isOdorous: Boolean = true,
    val note: String = ""
) {
    val durationMillis: Long get() = (endEpochMillis - startEpochMillis).coerceAtLeast(0)
    val durationHours: Double get() = durationMillis / (1000.0 * 60.0 * 60.0)
    val durationDays: Long get() = (durationMillis / (1000L * 60 * 60 * 24))
    val remainingHours: Long get() = ((durationMillis / (1000L * 60 * 60)) % 24)

    val formattedDuration: String
        get() = if (durationDays > 0) {
            "$durationDays Hari $remainingHours Jam"
        } else {
            "${(durationMillis / (1000L * 60 * 60))} Jam"
        }

    // Comprehensive fiqih score: Color score (primary) + thickness (secondary) + odor (tertiary)
    val totalScore: Int get() = (bloodColor.score * 10) + (if (isThick) 2 else 0) + (if (isOdorous) 1 else 0)

    val isStrongBlood: Boolean get() = bloodColor.score >= 4 || isThick
}

data class PeriodSegment(
    val title: String,
    val startEpochMillis: Long,
    val endEpochMillis: Long,
    val status: String, // "Haid", "Nifas", "Istihadhah", "Suci"
    val description: String
) {
    val durationHours: Long get() = (endEpochMillis - startEpochMillis) / (1000 * 60 * 60)
    val durationDays: Long get() = durationHours / 24
    val remainingHours: Long get() = durationHours % 24
}

data class KitabReference(
    val kitabName: String,
    val volumeAndPage: String,
    val ibaratSnippet: String,
    val explanation: String
)

data class CalculationResult(
    val statusSummary: String,
    val caseCategory: String,
    val categoryDetectionReason: String = "",
    val haidOrNifasSegment: PeriodSegment?,
    val istihadhahSegment: PeriodSegment?,
    val suciSegment: PeriodSegment?,
    val shalatConsequence: String,
    val qadhaPrayers: List<String>,
    val puasaConsequence: String,
    val mandiWajibNote: String,
    val mustahadhahCareGuide: String?,
    val medicalAndSpiritualAdvice: String,
    val references: List<KitabReference>,
    val phaseBreakdowns: List<String> = emptyList(),
    val isWarningIncluded: Boolean = true
)

data class AvatarTemplate(
    val id: Int,
    val name: String,
    val subtitle: String,
    val backgroundColors: List<Long>,
    val accentColor: Long,
    val initial: String
)

val PRESET_AVATAR_TEMPLATES = listOf(
    AvatarTemplate(
        id = 0,
        name = "Aisyah",
        subtitle = "Lembut & Anggun",
        backgroundColors = listOf(0xFFFCE7F3, 0xFFF472B6),
        accentColor = 0xFF9D174D,
        initial = "A"
    ),
    AvatarTemplate(
        id = 1,
        name = "Khadijah",
        subtitle = "Bijak & Wibawa",
        backgroundColors = listOf(0xFFD1FAE5, 0xFF34D399),
        accentColor = 0xFF065F46,
        initial = "K"
    ),
    AvatarTemplate(
        id = 2,
        name = "Fathimah",
        subtitle = "Sholehah & Mulia",
        backgroundColors = listOf(0xFFEDE9FE, 0xFFA78BFA),
        accentColor = 0xFF5B21B6,
        initial = "F"
    ),
    AvatarTemplate(
        id = 3,
        name = "Maryam",
        subtitle = "Tenteram & Suci",
        backgroundColors = listOf(0xFFCCFBF1, 0xFF2DD4BF),
        accentColor = 0xFF115E59,
        initial = "M"
    ),
    AvatarTemplate(
        id = 4,
        name = "Zahra",
        subtitle = "Hangat & Bersinar",
        backgroundColors = listOf(0xFFFFEDD5, 0xFFFB923C),
        accentColor = 0xFF9A3412,
        initial = "Z"
    ),
    AvatarTemplate(
        id = 5,
        name = "Ruqayyah",
        subtitle = "Sejuk & Damai",
        backgroundColors = listOf(0xFFE0F2FE, 0xFF38BDF8),
        accentColor = 0xFF075985,
        initial = "R"
    )
)


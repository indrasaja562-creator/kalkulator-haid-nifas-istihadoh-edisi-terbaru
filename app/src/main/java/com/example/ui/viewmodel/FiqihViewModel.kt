package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.entities.CalculationHistoryEntity
import com.example.data.entities.QadhaPrayerEntity
import com.example.data.entities.UserAdatProfileEntity
import com.example.data.repository.FiqihRepository
import com.example.fiqih.FiqihCalculatorEngine
import com.example.model.*
import com.example.reminder.PrayerReminderHelper
import com.example.reminder.PrayerSchedule
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.*

class FiqihViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: FiqihRepository

    // Current Navigation Tab (0=Kalkulator, 1=Pengingat & Qadha, 2=Panduan)
    private val _selectedTab = MutableStateFlow(0)
    val selectedTab: StateFlow<Int> = _selectedTab.asStateFlow()

    // Calculator Form State
    private val _caseType = MutableStateFlow(CaseType.HAID)
    val caseType: StateFlow<CaseType> = _caseType.asStateFlow()

    // Start Date & Time
    private val _startDateMillis = MutableStateFlow(System.currentTimeMillis() - 7 * 24 * 3600_000L)
    val startDateMillis: StateFlow<Long> = _startDateMillis.asStateFlow()

    private val _startHour = MutableStateFlow(8)
    val startHour: StateFlow<Int> = _startHour.asStateFlow()

    private val _startMinute = MutableStateFlow(0)
    val startMinute: StateFlow<Int> = _startMinute.asStateFlow()

    // End Date & Time
    private val _isBleedingOngoing = MutableStateFlow(false)
    val isBleedingOngoing: StateFlow<Boolean> = _isBleedingOngoing.asStateFlow()

    private val _endDateMillis = MutableStateFlow(System.currentTimeMillis())
    val endDateMillis: StateFlow<Long> = _endDateMillis.asStateFlow()

    private val _endHour = MutableStateFlow(16)
    val endHour: StateFlow<Int> = _endHour.asStateFlow()

    private val _endMinute = MutableStateFlow(30)
    val endMinute: StateFlow<Int> = _endMinute.asStateFlow()

    // Data Persalinan & Bayi (Kasus Nifas)
    private val _deliveryEpochMillis = MutableStateFlow<Long?>(System.currentTimeMillis() - 40 * 24 * 3600_000L)
    val deliveryEpochMillis: StateFlow<Long?> = _deliveryEpochMillis.asStateFlow()

    private val _deliveryHour = MutableStateFlow(6)
    val deliveryHour: StateFlow<Int> = _deliveryHour.asStateFlow()

    private val _deliveryMinute = MutableStateFlow(0)
    val deliveryMinute: StateFlow<Int> = _deliveryMinute.asStateFlow()

    private val _deliveryType = MutableStateFlow(DeliveryType.TUNGGAL_NORMAL_SESAR)
    val deliveryType: StateFlow<DeliveryType> = _deliveryType.asStateFlow()

    private val _twinLastDeliveryEpochMillis = MutableStateFlow<Long?>(null)
    val twinLastDeliveryEpochMillis: StateFlow<Long?> = _twinLastDeliveryEpochMillis.asStateFlow()

    // Riwayat Adat Nifas & 5 Golongan + 2 Catatan
    private val _nifasAdatCategory = MutableStateFlow(NifasAdatCategory.MUBTADIAH_MUMAYYIZAH)
    val nifasAdatCategory: StateFlow<NifasAdatCategory> = _nifasAdatCategory.asStateFlow()

    private val _adatNifasDays = MutableStateFlow(40)
    val adatNifasDays: StateFlow<Int> = _adatNifasDays.asStateFlow()

    // Jeda Bersih / Terputus-putus (Fatrah Naqa')
    private val _hasIntermittentPause = MutableStateFlow(false)
    val hasIntermittentPause: StateFlow<Boolean> = _hasIntermittentPause.asStateFlow()

    private val _intermittentPauseDays = MutableStateFlow(2.0)
    val intermittentPauseDays: StateFlow<Double> = _intermittentPauseDays.asStateFlow()

    // Mubtadi'ah Ghairu Mumayyizah fin-Nifas states
    private val _hasPreviousHaidBeforeNifas = MutableStateFlow(false)
    val hasPreviousHaidBeforeNifas: StateFlow<Boolean> = _hasPreviousHaidBeforeNifas.asStateFlow()

    private val _previousHaidAdatDays = MutableStateFlow(7)
    val previousHaidAdatDays: StateFlow<Int> = _previousHaidAdatDays.asStateFlow()

    // Istihadhah Takmilatan lit-Tuhri state
    private val _previousSuciDaysForTakmilah = MutableStateFlow(15)
    val previousSuciDaysForTakmilah: StateFlow<Int> = _previousSuciDaysForTakmilah.asStateFlow()

    // Adat history Haid
    private val _hasPreviousAdat = MutableStateFlow(true)
    val hasPreviousAdat: StateFlow<Boolean> = _hasPreviousAdat.asStateFlow()

    private val _haidCategory = MutableStateFlow(HaidCategory.MUTADAH_GHAIRU_MUMAYYIZAH_DZAKIRAH_QADRAN_WAQTAN)
    val haidCategory: StateFlow<HaidCategory> = _haidCategory.asStateFlow()

    private val _adatDurationDays = MutableStateFlow(7)
    val adatDurationDays: StateFlow<Int> = _adatDurationDays.asStateFlow()

    private val _adatCycleDays = MutableStateFlow(28)
    val adatCycleDays: StateFlow<Int> = _adatCycleDays.asStateFlow()

    private val _adatMemoryType = MutableStateFlow(AdatMemoryType.INGAT_LENGKAP_QADRAN_WAQTAN)
    val adatMemoryType: StateFlow<AdatMemoryType> = _adatMemoryType.asStateFlow()

    // Blood characteristics
    private val _bloodColor = MutableStateFlow(BloodColor.MERAH)
    val bloodColor: StateFlow<BloodColor> = _bloodColor.asStateFlow()

    private val _isThick = MutableStateFlow(true)
    val isThick: StateFlow<Boolean> = _isThick.asStateFlow()

    private val _isOdorous = MutableStateFlow(true)
    val isOdorous: StateFlow<Boolean> = _isOdorous.asStateFlow()

    private val _intervals = MutableStateFlow<List<BleedingInterval>>(emptyList())
    val intervals: StateFlow<List<BleedingInterval>> = _intervals.asStateFlow()

    // Prayer at start / stop
    private val _prayerAtStart = MutableStateFlow<PrayerName?>(PrayerName.DZUHUR)
    val prayerAtStart: StateFlow<PrayerName?> = _prayerAtStart.asStateFlow()

    private val _hadPrayedAtStart = MutableStateFlow(true)
    val hadPrayedAtStart: StateFlow<Boolean> = _hadPrayedAtStart.asStateFlow()

    private val _prayerAtStop = MutableStateFlow<PrayerName?>(PrayerName.ASHAR)
    val prayerAtStop: StateFlow<PrayerName?> = _prayerAtStop.asStateFlow()

    // Calculation result
    private val _calculationResult = MutableStateFlow<CalculationResult?>(null)
    val calculationResult: StateFlow<CalculationResult?> = _calculationResult.asStateFlow()

    // Prayer Schedule
    private val _prayerSchedule = MutableStateFlow(PrayerReminderHelper.calculatePrayerTimes())
    val prayerSchedule: StateFlow<PrayerSchedule> = _prayerSchedule.asStateFlow()

    private val _remindersEnabled = MutableStateFlow(true)
    val remindersEnabled: StateFlow<Boolean> = _remindersEnabled.asStateFlow()

    // Database Flows
    val userProfile: StateFlow<UserAdatProfileEntity>
    val calculationHistory: StateFlow<List<CalculationHistoryEntity>>
    val qadhaPrayers: StateFlow<List<QadhaPrayerEntity>>

    private val _saveMessage = MutableStateFlow<String?>(null)
    val saveMessage: StateFlow<String?> = _saveMessage.asStateFlow()

    init {
        val db = AppDatabase.getDatabase(application)
        repository = FiqihRepository(db.fiqihDao())
        PrayerReminderHelper.initNotificationChannels(application)

        userProfile = repository.userProfile
            .map { it ?: UserAdatProfileEntity() }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UserAdatProfileEntity())

        calculationHistory = repository.allHistory
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

        qadhaPrayers = repository.allQadhaPrayers
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

        loadUserProfile()
        loadNormalHaidPreset()
        seedSampleCyclesIfEmpty()
    }

    private fun loadUserProfile() {
        viewModelScope.launch {
            repository.userProfile.collect { profile ->
                profile?.let {
                    _adatDurationDays.value = it.usualHaidDays
                    _adatCycleDays.value = it.usualHaidDays + it.usualSuciDays
                    _adatNifasDays.value = it.usualNifasDays
                }
            }
        }
    }

    fun setSelectedTab(tabIndex: Int) {
        _selectedTab.value = tabIndex
    }

    fun setTab(tabIndex: Int) {
        setSelectedTab(tabIndex)
    }

    fun setCaseType(type: CaseType) {
        _caseType.value = type
        if (type == CaseType.NIFAS) {
            if (_intervals.value.isEmpty()) {
                loadIstihadhahNifasTuhfatunNiswah()
            } else {
                doCalculate()
            }
        } else {
            doCalculate()
        }
    }

    fun setStartDate(millis: Long) {
        _startDateMillis.value = millis
        doCalculate()
    }

    fun setStartTime(hour: Int, minute: Int) {
        _startHour.value = hour
        _startMinute.value = minute
        doCalculate()
    }

    fun setEndDate(millis: Long) {
        _endDateMillis.value = millis
        doCalculate()
    }

    fun setEndTime(hour: Int, minute: Int) {
        _endHour.value = hour
        _endMinute.value = minute
        doCalculate()
    }

    fun setIsBleedingOngoing(ongoing: Boolean) {
        _isBleedingOngoing.value = ongoing
        doCalculate()
    }

    // Persalinan & Nifas setters
    fun setDeliveryEpochMillis(millis: Long?) {
        _deliveryEpochMillis.value = millis
        doCalculate()
    }

    fun setDeliveryTime(hour: Int, minute: Int) {
        _deliveryHour.value = hour
        _deliveryMinute.value = minute
        doCalculate()
    }

    fun setDeliveryType(type: DeliveryType) {
        _deliveryType.value = type
        doCalculate()
    }

    fun setTwinLastDeliveryEpochMillis(millis: Long?) {
        _twinLastDeliveryEpochMillis.value = millis
        doCalculate()
    }

    fun setNifasAdatCategory(cat: NifasAdatCategory) {
        _nifasAdatCategory.value = cat
        when (cat) {
            NifasAdatCategory.MUBTADIAH_MUMAYYIZAH, NifasAdatCategory.MUBTADIAH_GHAIRU_MUMAYYIZAH -> {
                _hasPreviousAdat.value = false
                _adatMemoryType.value = AdatMemoryType.BELUM_PERNAH_HAID
            }
            NifasAdatCategory.MUTADAH_MUMAYYIZAH, NifasAdatCategory.MUTADAH_GHAIRU_MUMAYYIZAH_DZAKIRAH -> {
                _hasPreviousAdat.value = true
                _adatMemoryType.value = AdatMemoryType.INGAT_LENGKAP_QADRAN_WAQTAN
            }
            NifasAdatCategory.MUTADAH_NASIYAH_MUTAHAYYIRAH -> {
                _hasPreviousAdat.value = true
                _adatMemoryType.value = AdatMemoryType.LUPA_SEMUANYA_MUTAHAYYIRAH
            }
            NifasAdatCategory.CATATAN_DZAKIRAH_WAQTAN_LUPA_QADRAN -> {
                _hasPreviousAdat.value = true
                _adatMemoryType.value = AdatMemoryType.INGAT_WAQTAN_LUPA_QADRAN
            }
            NifasAdatCategory.CATATAN_DZAKIRAH_QADRAN_LUPA_WAQTAN -> {
                _hasPreviousAdat.value = true
                _adatMemoryType.value = AdatMemoryType.INGAT_QADRAN_LUPA_WAQTAN
            }
        }
        doCalculate()
    }

    fun setHaidCategory(cat: HaidCategory) {
        _haidCategory.value = cat
        when (cat) {
            HaidCategory.MUBTADIAH_MUMAYYIZAH, HaidCategory.MUBTADIAH_GHAIRU_MUMAYYIZAH -> {
                _hasPreviousAdat.value = false
                _adatMemoryType.value = AdatMemoryType.BELUM_PERNAH_HAID
            }
            HaidCategory.MUTADAH_MUMAYYIZAH, HaidCategory.MUTADAH_GHAIRU_MUMAYYIZAH_DZAKIRAH_QADRAN_WAQTAN -> {
                _hasPreviousAdat.value = true
                _adatMemoryType.value = AdatMemoryType.INGAT_LENGKAP_QADRAN_WAQTAN
            }
            HaidCategory.MUTADAH_NASIYAH_MUTAHAYYIRAH -> {
                _hasPreviousAdat.value = true
                _adatMemoryType.value = AdatMemoryType.LUPA_SEMUANYA_MUTAHAYYIRAH
            }
            HaidCategory.MUTADAH_DZAKIRAH_WAQTAN_DUNAN_QADR -> {
                _hasPreviousAdat.value = true
                _adatMemoryType.value = AdatMemoryType.INGAT_WAQTAN_LUPA_QADRAN
            }
            HaidCategory.MUTADAH_DZAKIRAH_QADRAN_DUNAN_WAQT -> {
                _hasPreviousAdat.value = true
                _adatMemoryType.value = AdatMemoryType.INGAT_QADRAN_LUPA_WAQTAN
            }
        }
        doCalculate()
    }

    fun setAdatNifasDays(days: Int) {
        _adatNifasDays.value = days.coerceIn(1, 60)
        doCalculate()
    }

    fun setHasIntermittentPause(hasPause: Boolean) {
        _hasIntermittentPause.value = hasPause
        doCalculate()
    }

    fun setIntermittentPauseDays(days: Double) {
        _intermittentPauseDays.value = days.coerceAtLeast(0.5)
        doCalculate()
    }

    fun setHasPreviousHaidBeforeNifas(has: Boolean) {
        _hasPreviousHaidBeforeNifas.value = has
        doCalculate()
    }

    fun setPreviousHaidAdatDays(days: Int) {
        _previousHaidAdatDays.value = days.coerceIn(1, 15)
        doCalculate()
    }

    fun setPreviousSuciDaysForTakmilah(days: Int) {
        _previousSuciDaysForTakmilah.value = days.coerceIn(0, 365)
        doCalculate()
    }

    fun setHasPreviousAdat(has: Boolean) {
        _hasPreviousAdat.value = has
        doCalculate()
    }

    fun setAdatDurationDays(days: Int) {
        _adatDurationDays.value = days.coerceIn(1, 15)
        doCalculate()
    }

    fun setAdatCycleDays(days: Int) {
        _adatCycleDays.value = days.coerceIn(15, 60)
        doCalculate()
    }

    fun setAdatMemoryType(type: AdatMemoryType) {
        _adatMemoryType.value = type
        doCalculate()
    }

    fun setBloodColor(color: BloodColor) {
        _bloodColor.value = color
        doCalculate()
    }

    fun setIsThick(thick: Boolean) {
        _isThick.value = thick
        doCalculate()
    }

    fun setIsOdorous(odorous: Boolean) {
        _isOdorous.value = odorous
        doCalculate()
    }

    fun setPrayerAtStart(prayer: PrayerName?) {
        _prayerAtStart.value = prayer
        doCalculate()
    }

    fun setHadPrayedAtStart(hadPrayed: Boolean) {
        _hadPrayedAtStart.value = hadPrayed
        doCalculate()
    }

    fun setPrayerAtStop(prayer: PrayerName?) {
        _prayerAtStop.value = prayer
        doCalculate()
    }

    fun toggleReminders(enabled: Boolean) {
        _remindersEnabled.value = enabled
    }

    // Interval Management
    fun addInterval(interval: BleedingInterval) {
        _intervals.value = _intervals.value + interval
        syncDateTimesFromIntervals()
        doCalculate()
    }

    fun updateInterval(index: Int, updated: BleedingInterval) {
        val current = _intervals.value.toMutableList()
        if (index in current.indices) {
            current[index] = updated
            _intervals.value = current
            syncDateTimesFromIntervals()
            doCalculate()
        }
    }

    fun removeInterval(index: Int) {
        val current = _intervals.value.toMutableList()
        if (current.size > 1 && index in current.indices) {
            current.removeAt(index)
            _intervals.value = current
            syncDateTimesFromIntervals()
            doCalculate()
        }
    }

    fun addNewNextInterval() {
        val current = _intervals.value
        val lastEnd = current.lastOrNull()?.endEpochMillis ?: System.currentTimeMillis()
        val nextEnd = lastEnd + (5 * 24 * 3600_000L)
        val nextColor = if (current.any { it.bloodColor == BloodColor.MERAH }) BloodColor.KUNING else BloodColor.MERAH
        val newInterval = BleedingInterval(
            startEpochMillis = lastEnd,
            endEpochMillis = nextEnd,
            bloodColor = nextColor,
            isThick = false,
            isOdorous = false,
            note = "Fase ${current.size + 1}"
        )
        _intervals.value = current + newInterval
        syncDateTimesFromIntervals()
        doCalculate()
    }

    private fun syncDateTimesFromIntervals() {
        val list = _intervals.value
        if (list.isNotEmpty()) {
            val minStart = list.minOf { it.startEpochMillis }
            val maxEnd = list.maxOf { it.endEpochMillis }
            _startDateMillis.value = minStart
            _endDateMillis.value = maxEnd
            val startCal = Calendar.getInstance().apply { timeInMillis = minStart }
            _startHour.value = startCal.get(Calendar.HOUR_OF_DAY)
            _startMinute.value = startCal.get(Calendar.MINUTE)
            val endCal = Calendar.getInstance().apply { timeInMillis = maxEnd }
            _endHour.value = endCal.get(Calendar.HOUR_OF_DAY)
            _endMinute.value = endCal.get(Calendar.MINUTE)
        }
    }

    // =========================================================================
    // PRESETS FOR ACCURATE TESTING AND USER CONVENIENCE
    // =========================================================================

    // 1. Preset Haid Tamyiz: Merah tgl 1-10 jam 12:00 -> Kuning tgl 10-20 jam 10:00
    fun loadUserExampleTwoPhases() {
        _caseType.value = CaseType.HAID
        _hasPreviousAdat.value = false
        _haidCategory.value = HaidCategory.MUBTADIAH_MUMAYYIZAH
        _adatMemoryType.value = AdatMemoryType.BELUM_PERNAH_HAID

        val cal = Calendar.getInstance()
        cal.set(Calendar.DAY_OF_MONTH, 1)
        cal.set(Calendar.HOUR_OF_DAY, 12)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val phase1Start = cal.timeInMillis

        cal.set(Calendar.DAY_OF_MONTH, 10)
        cal.set(Calendar.HOUR_OF_DAY, 12)
        val phase1End = cal.timeInMillis

        cal.set(Calendar.DAY_OF_MONTH, 20)
        cal.set(Calendar.HOUR_OF_DAY, 10)
        val phase2End = cal.timeInMillis

        _intervals.value = listOf(
            BleedingInterval(
                startEpochMillis = phase1Start,
                endEpochMillis = phase1End,
                bloodColor = BloodColor.MERAH,
                isThick = true,
                isOdorous = true,
                note = "Darah Merah Kental (Tgl 1-10 jam 12:00)"
            ),
            BleedingInterval(
                startEpochMillis = phase1End,
                endEpochMillis = phase2End,
                bloodColor = BloodColor.KUNING,
                isThick = false,
                isOdorous = false,
                note = "Darah Kuning Encer (Tgl 10-20 jam 10:00)"
            )
        )
        syncDateTimesFromIntervals()
        doCalculate()
    }

    // 2. Preset Haid Normal 7 Hari
    fun loadNormalHaidPreset() {
        _caseType.value = CaseType.HAID
        _hasPreviousAdat.value = true
        _adatDurationDays.value = 7
        _haidCategory.value = HaidCategory.MUTADAH_GHAIRU_MUMAYYIZAH_DZAKIRAH_QADRAN_WAQTAN
        _adatMemoryType.value = AdatMemoryType.INGAT_LENGKAP_QADRAN_WAQTAN

        val now = System.currentTimeMillis()
        val start = now - (7 * 24 * 3600_000L)
        _intervals.value = listOf(
            BleedingInterval(
                startEpochMillis = start,
                endEpochMillis = now,
                bloodColor = BloodColor.MERAH,
                isThick = true,
                isOdorous = true,
                note = "Haid Normal 7 Hari"
            )
        )
        syncDateTimesFromIntervals()
        doCalculate()
    }

    // 3. Preset Nifas Mumayyizah 65 Hari (Kitab Tuhfatun Niswah & Uyunul Masail)
    fun loadIstihadhahNifasTuhfatunNiswah() {
        _caseType.value = CaseType.NIFAS
        _deliveryType.value = DeliveryType.TUNGGAL_NORMAL_SESAR
        _hasPreviousAdat.value = false
        _nifasAdatCategory.value = NifasAdatCategory.MUBTADIAH_MUMAYYIZAH
        _adatMemoryType.value = AdatMemoryType.BELUM_PERNAH_HAID

        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, -65)
        cal.set(Calendar.HOUR_OF_DAY, 8)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        val deliveryTime = cal.timeInMillis

        _deliveryEpochMillis.value = deliveryTime

        cal.add(Calendar.DAY_OF_YEAR, 40)
        cal.set(Calendar.HOUR_OF_DAY, 8)
        val phase1End = cal.timeInMillis

        cal.add(Calendar.DAY_OF_YEAR, 25)
        cal.set(Calendar.HOUR_OF_DAY, 12)
        val phase2End = cal.timeInMillis

        _intervals.value = listOf(
            BleedingInterval(
                startEpochMillis = deliveryTime,
                endEpochMillis = phase1End,
                bloodColor = BloodColor.MERAH,
                isThick = true,
                isOdorous = true,
                note = "Darah Kuat Nifas (Hari 1-40)"
            ),
            BleedingInterval(
                startEpochMillis = phase1End,
                endEpochMillis = phase2End,
                bloodColor = BloodColor.KUNING,
                isThick = false,
                isOdorous = false,
                note = "Darah Lemah Istihadhah fin-Nifas (Hari 41-65)"
            )
        )
        syncDateTimesFromIntervals()
        doCalculate()
    }

    // 4. Preset Nifas Mubtadi'ah Ghairu Mumayyizah 65 Hari (Nifas 1 hari, qadha 59 hari shalat)
    fun loadNifasMubtadiahGhairuMumayyizah() {
        _caseType.value = CaseType.NIFAS
        _deliveryType.value = DeliveryType.TUNGGAL_NORMAL_SESAR
        _hasPreviousAdat.value = false
        _nifasAdatCategory.value = NifasAdatCategory.MUBTADIAH_GHAIRU_MUMAYYIZAH
        _adatMemoryType.value = AdatMemoryType.BELUM_PERNAH_HAID

        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, -65)
        cal.set(Calendar.HOUR_OF_DAY, 6)
        cal.set(Calendar.MINUTE, 0)
        val deliveryTime = cal.timeInMillis
        _deliveryEpochMillis.value = deliveryTime

        val end = deliveryTime + (65 * 24 * 3600_000L)
        _intervals.value = listOf(
            BleedingInterval(
                startEpochMillis = deliveryTime,
                endEpochMillis = end,
                bloodColor = BloodColor.MERAH,
                isThick = true,
                isOdorous = true,
                note = "Darah Merah 1 Warna 65 Hari (Mubtadi'ah Ghairu Mumayyizah)"
            )
        )
        syncDateTimesFromIntervals()
        doCalculate()
    }

    // 5. Preset Nifas Mu'tadah Ingat Adat 40 Hari (Darah 65 hari, qadha 20 hari)
    fun loadNifasMutadahAdat40Hari() {
        _caseType.value = CaseType.NIFAS
        _deliveryType.value = DeliveryType.TUNGGAL_NORMAL_SESAR
        _hasPreviousAdat.value = true
        _adatNifasDays.value = 40
        _nifasAdatCategory.value = NifasAdatCategory.MUTADAH_GHAIRU_MUMAYYIZAH_DZAKIRAH
        _adatMemoryType.value = AdatMemoryType.INGAT_LENGKAP_QADRAN_WAQTAN

        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, -65)
        cal.set(Calendar.HOUR_OF_DAY, 7)
        cal.set(Calendar.MINUTE, 0)
        val deliveryTime = cal.timeInMillis
        _deliveryEpochMillis.value = deliveryTime

        val end = deliveryTime + (65 * 24 * 3600_000L)
        _intervals.value = listOf(
            BleedingInterval(
                startEpochMillis = deliveryTime,
                endEpochMillis = end,
                bloodColor = BloodColor.MERAH,
                isThick = true,
                isOdorous = true,
                note = "Darah Merah 65 Hari (Adat Kelahiran Lalu: 40 Hari)"
            )
        )
        syncDateTimesFromIntervals()
        doCalculate()
    }

    // 6. Preset Nifas Jeda Bersih 16 Hari (>= 15 Hari memutus nifas & darah kedua adalah haid baru)
    fun loadNifasJedaBersih16Hari() {
        _caseType.value = CaseType.NIFAS
        _deliveryType.value = DeliveryType.TUNGGAL_NORMAL_SESAR
        _hasIntermittentPause.value = true
        _intermittentPauseDays.value = 16.0

        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, -50)
        cal.set(Calendar.HOUR_OF_DAY, 8)
        val deliveryTime = cal.timeInMillis
        _deliveryEpochMillis.value = deliveryTime

        val phase1End = deliveryTime + (20 * 24 * 3600_000L)
        val phase2Start = phase1End + (16 * 24 * 3600_000L)
        val phase2End = phase2Start + (7 * 24 * 3600_000L)

        _intervals.value = listOf(
            BleedingInterval(
                startEpochMillis = deliveryTime,
                endEpochMillis = phase1End,
                bloodColor = BloodColor.MERAH,
                isThick = true,
                isOdorous = true,
                note = "Nifas Pertama (20 Hari)"
            ),
            BleedingInterval(
                startEpochMillis = phase2Start,
                endEpochMillis = phase2End,
                bloodColor = BloodColor.MERAH,
                isThick = true,
                isOdorous = true,
                note = "Darah Kedua Pasca Jeda Suci 16 Hari (Dihukumi Haid Baru)"
            )
        )
        syncDateTimesFromIntervals()
        doCalculate()
    }

    fun doCalculate() {
        val currentIntervals = _intervals.value
        val startMs: Long
        val endMs: Long

        if (currentIntervals.isNotEmpty()) {
            startMs = currentIntervals.minOf { it.startEpochMillis }
            endMs = currentIntervals.maxOf { it.endEpochMillis }
        } else {
            val startCal = Calendar.getInstance().apply {
                timeInMillis = _startDateMillis.value
                set(Calendar.HOUR_OF_DAY, _startHour.value)
                set(Calendar.MINUTE, _startMinute.value)
                set(Calendar.SECOND, 0)
            }
            val endCal = Calendar.getInstance().apply {
                if (_isBleedingOngoing.value) {
                    timeInMillis = System.currentTimeMillis()
                } else {
                    timeInMillis = _endDateMillis.value
                    set(Calendar.HOUR_OF_DAY, _endHour.value)
                    set(Calendar.MINUTE, _endMinute.value)
                    set(Calendar.SECOND, 0)
                }
            }
            startMs = startCal.timeInMillis
            endMs = if (endCal.timeInMillis > startMs) endCal.timeInMillis else startMs + (7 * 24 * 3600_000L)
        }

        // Resolusi waktu delivery
        val delCal = _deliveryEpochMillis.value?.let {
            Calendar.getInstance().apply {
                timeInMillis = it
                set(Calendar.HOUR_OF_DAY, _deliveryHour.value)
                set(Calendar.MINUTE, _deliveryMinute.value)
                set(Calendar.SECOND, 0)
            }.timeInMillis
        }

        val result = FiqihCalculatorEngine.calculate(
            caseType = _caseType.value,
            startEpochMillis = startMs,
            endEpochMillis = endMs,
            hasPreviousAdat = _hasPreviousAdat.value,
            adatDurationDays = if (_caseType.value == CaseType.NIFAS) _adatNifasDays.value else _adatDurationDays.value,
            adatCycleDays = _adatCycleDays.value,
            adatMemoryType = _adatMemoryType.value,
            haidCategory = _haidCategory.value,
            deliveryEpochMillis = delCal,
            deliveryType = _deliveryType.value,
            twinLastDeliveryEpochMillis = _twinLastDeliveryEpochMillis.value,
            nifasAdatCategory = _nifasAdatCategory.value,
            hasIntermittentPause = _hasIntermittentPause.value,
            intermittentPauseDays = _intermittentPauseDays.value,
            bloodColor = _bloodColor.value,
            isThick = _isThick.value,
            isOdorous = _isOdorous.value,
            intervals = _intervals.value,
            prayerAtStart = _prayerAtStart.value,
            hadPrayedAtStart = _hadPrayedAtStart.value,
            prayerAtStop = _prayerAtStop.value
        )

        _calculationResult.value = result

        // Automatically persist calculation to Room DB
        viewModelScope.launch {
            val totalHours = ((endMs - startMs) / 3600_000L).coerceAtLeast(1L)
            val totalDays = (totalHours / 24).toInt()

            val historyList = repository.allHistory.firstOrNull() ?: emptyList()
            val prevSameType = historyList.filter { it.caseType == _caseType.value.name }
                .minByOrNull { kotlin.math.abs(it.startEpochMillis - startMs) }
            val cycleLength = if (prevSameType != null && startMs != prevSameType.startEpochMillis) {
                kotlin.math.abs((startMs - prevSameType.startEpochMillis) / (24 * 3600_000L)).toInt()
            } else {
                _adatCycleDays.value
            }

            val entity = CalculationHistoryEntity(
                caseType = _caseType.value.name,
                startEpochMillis = startMs,
                endEpochMillis = endMs,
                statusSummary = result.statusSummary,
                categoryName = result.caseCategory,
                categoryDetectionReason = result.categoryDetectionReason,
                haidHours = result.haidOrNifasSegment?.durationHours ?: 0L,
                istihadhahHours = result.istihadhahSegment?.durationHours ?: 0L,
                suciHours = result.suciSegment?.durationHours ?: 0L,
                totalHours = totalHours,
                totalDays = totalDays,
                shalatNote = result.shalatConsequence,
                puasaNote = result.puasaConsequence,
                mandiNote = result.mandiWajibNote,
                cycleLengthDays = cycleLength,
                note = if (_caseType.value == CaseType.NIFAS) "Perhitungan Nifas" else "Siklus Haid"
            )
            repository.saveCalculation(entity)

            // Auto-populate qadha prayers
            val todayStr = java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault()).format(java.util.Date())
            result.qadhaPrayers.forEach { prayerNameText ->
                val qadha = QadhaPrayerEntity(
                    prayerName = prayerNameText,
                    dateString = todayStr,
                    reason = "Otomatis dicatat dari kalkulator: ${result.caseCategory}",
                    isCompleted = false
                )
                repository.addQadhaPrayer(qadha)
            }
        }
    }

    fun saveCurrentCalculation(customNote: String = "") {
        val result = _calculationResult.value ?: return
        viewModelScope.launch {
            val currentIntervals = _intervals.value
            val startMs: Long
            val endMs: Long

            if (currentIntervals.isNotEmpty()) {
                startMs = currentIntervals.minOf { it.startEpochMillis }
                endMs = currentIntervals.maxOf { it.endEpochMillis }
            } else {
                val startCal = Calendar.getInstance().apply {
                    timeInMillis = _startDateMillis.value
                    set(Calendar.HOUR_OF_DAY, _startHour.value)
                    set(Calendar.MINUTE, _startMinute.value)
                    set(Calendar.SECOND, 0)
                }
                val endCal = Calendar.getInstance().apply {
                    if (_isBleedingOngoing.value) {
                        timeInMillis = System.currentTimeMillis()
                    } else {
                        timeInMillis = _endDateMillis.value
                        set(Calendar.HOUR_OF_DAY, _endHour.value)
                        set(Calendar.MINUTE, _endMinute.value)
                        set(Calendar.SECOND, 0)
                    }
                }
                startMs = startCal.timeInMillis
                endMs = if (endCal.timeInMillis > startMs) endCal.timeInMillis else startMs + (7 * 24 * 3600_000L)
            }

            val totalHours = ((endMs - startMs) / 3600_000L).coerceAtLeast(1L)
            val totalDays = (totalHours / 24).toInt()

            val historyList = repository.allHistory.firstOrNull() ?: emptyList()
            val prevSameType = historyList.filter { it.caseType == _caseType.value.name }
                .minByOrNull { kotlin.math.abs(it.startEpochMillis - startMs) }
            val cycleLength = if (prevSameType != null && startMs != prevSameType.startEpochMillis) {
                kotlin.math.abs((startMs - prevSameType.startEpochMillis) / (24 * 3600_000L)).toInt()
            } else {
                _adatCycleDays.value
            }

            val entity = CalculationHistoryEntity(
                caseType = _caseType.value.name,
                startEpochMillis = startMs,
                endEpochMillis = endMs,
                statusSummary = result.statusSummary,
                categoryName = result.caseCategory,
                categoryDetectionReason = result.categoryDetectionReason,
                haidHours = result.haidOrNifasSegment?.durationHours ?: 0L,
                istihadhahHours = result.istihadhahSegment?.durationHours ?: 0L,
                suciHours = result.suciSegment?.durationHours ?: 0L,
                totalHours = totalHours,
                totalDays = totalDays,
                shalatNote = result.shalatConsequence,
                puasaNote = result.puasaConsequence,
                mandiNote = result.mandiWajibNote,
                cycleLengthDays = cycleLength,
                note = customNote.ifBlank { if (_caseType.value == CaseType.NIFAS) "Catatan Nifas" else "Siklus Haid" }
            )
            repository.saveCalculation(entity)
            _saveMessage.value = "Siklus berhasil disimpan ke Room Database!"
        }
    }

    fun clearSaveMessage() {
        _saveMessage.value = null
    }

    // User Profile Management
    fun updateUserName(name: String) {
        viewModelScope.launch {
            val current = userProfile.value
            repository.saveProfile(current.copy(userName = name.trim()))
        }
    }

    fun updateUserBio(bio: String) {
        viewModelScope.launch {
            val current = userProfile.value
            repository.saveProfile(current.copy(userBio = bio.trim()))
        }
    }

    fun setUsePersonalPhoto(usePersonal: Boolean) {
        viewModelScope.launch {
            val current = userProfile.value
            repository.saveProfile(current.copy(usePersonalPhoto = usePersonal))
        }
    }

    fun setPhotoUri(uriString: String?) {
        viewModelScope.launch {
            val current = userProfile.value
            repository.saveProfile(current.copy(photoUri = uriString, usePersonalPhoto = uriString != null))
        }
    }

    fun setAvatarTemplateIndex(index: Int) {
        viewModelScope.launch {
            val current = userProfile.value
            repository.saveProfile(current.copy(avatarTemplateIndex = index, usePersonalPhoto = false))
        }
    }

    fun updateAdatDays(haidDays: Int, suciDays: Int, nifasDays: Int) {
        viewModelScope.launch {
            val current = userProfile.value
            repository.saveProfile(
                current.copy(
                    usualHaidDays = haidDays,
                    usualSuciDays = suciDays,
                    usualNifasDays = nifasDays
                )
            )
            _adatDurationDays.value = haidDays
            _adatCycleDays.value = haidDays + suciDays
            _adatNifasDays.value = nifasDays
        }
    }

    fun clearAllHistory() {
        viewModelScope.launch {
            repository.clearHistory()
        }
    }

    fun seedSampleCyclesIfEmpty() {
        viewModelScope.launch {
            val currentList = repository.allHistory.firstOrNull() ?: emptyList()
            if (currentList.isEmpty()) {
                val cal = Calendar.getInstance()
                cal.add(Calendar.DAY_OF_YEAR, -35)
                val cycle1Start = cal.timeInMillis
                val cycle1End = cycle1Start + (7 * 24 * 3600_000L) // 7 hari haid

                cal.add(Calendar.DAY_OF_YEAR, 28) // 28 hari jeda siklus
                val cycle2Start = cal.timeInMillis
                val cycle2End = cycle2Start + (9 * 24 * 3600_000L) // 9 hari total darah

                val entry1 = CalculationHistoryEntity(
                    caseType = "HAID",
                    startEpochMillis = cycle1Start,
                    endEpochMillis = cycle1End,
                    statusSummary = "Haid Sah 7 Hari (Adat Normal Sesuai Kebiasaan)",
                    categoryName = "3. Mu'tadah Mumayyizah",
                    categoryDetectionReason = "Darah merah kental selama 7 hari tepat sesuai adat kebiasaan bulan sebelumnya.",
                    haidHours = 7 * 24L,
                    istihadhahHours = 0L,
                    suciHours = 21 * 24L,
                    totalHours = 7 * 24L,
                    totalDays = 7,
                    shalatNote = "Tidak wajib shalat selama 7 hari masa haid.",
                    puasaNote = "Haram puasa, wajib qadha puasa jika dalam bulan Ramadhan.",
                    mandiNote = "Wajib mandi besar setelah darah berhenti di hari ke-7.",
                    cycleLengthDays = 28,
                    note = "Siklus Periode Bulan Lalu (Normal)"
                )

                val entry2 = CalculationHistoryEntity(
                    caseType = "HAID",
                    startEpochMillis = cycle2Start,
                    endEpochMillis = cycle2End,
                    statusSummary = "Haid 7 Hari & Istihadhah 2 Hari",
                    categoryName = "4. Mu'tadah Ghairu Mumayyizah (Ingat Lengkap)",
                    categoryDetectionReason = "Darah melampaui adat kebiasaan 7 hari menjadi 9 hari satu sifat warna, dikembalikan ke adat 7 hari haid dan sisanya 2 hari istihadhah.",
                    haidHours = 7 * 24L,
                    istihadhahHours = 2 * 24L,
                    suciHours = 19 * 24L,
                    totalHours = 9 * 24L,
                    totalDays = 9,
                    shalatNote = "Wajib bersuci dan shalat di hari ke-8 & 9 (status istihadhah).",
                    puasaNote = "Wajib puasa di hari ke-8 & 9 setelah bersuci.",
                    mandiNote = "Mandi wajib di akhir hari ke-7 sesuai masa adat.",
                    cycleLengthDays = 28,
                    note = "Siklus Periode Terakhir (Ada Istihadhah)"
                )

                repository.saveCalculation(entry1)
                repository.saveCalculation(entry2)
            }
        }
    }

    fun completeQadhaPrayer(id: Long, completed: Boolean) {
        viewModelScope.launch {
            repository.toggleQadhaCompleted(id, completed)
        }
    }

    fun toggleQadhaPrayer(prayer: QadhaPrayerEntity) {
        viewModelScope.launch {
            repository.toggleQadhaCompleted(prayer.id, !prayer.isCompleted)
        }
    }

    fun deleteQadhaPrayer(id: Long) {
        viewModelScope.launch {
            repository.deleteQadhaPrayer(id)
        }
    }

    fun deleteHistory(id: Long) {
        viewModelScope.launch {
            repository.deleteCalculation(id)
        }
    }

    fun addManualQadha(prayerName: String, reason: String) {
        viewModelScope.launch {
            val todayStr = java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault()).format(java.util.Date())
            repository.addQadhaPrayer(
                QadhaPrayerEntity(
                    prayerName = prayerName,
                    dateString = todayStr,
                    reason = reason,
                    isCompleted = false
                )
            )
        }
    }

    fun refreshPrayerTimes() {
        _prayerSchedule.value = PrayerReminderHelper.calculatePrayerTimes()
    }
}

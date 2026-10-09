package com.example.data.repository

import com.example.data.dao.FiqihDao
import com.example.data.entities.CalculationHistoryEntity
import com.example.data.entities.DailyBloodLogEntity
import com.example.data.entities.QadhaPrayerEntity
import com.example.data.entities.UserAdatProfileEntity
import kotlinx.coroutines.flow.Flow

class FiqihRepository(private val dao: FiqihDao) {

    val allHistory: Flow<List<CalculationHistoryEntity>> = dao.getAllHistory()
    val allQadhaPrayers: Flow<List<QadhaPrayerEntity>> = dao.getAllQadhaPrayers()
    val userProfile: Flow<UserAdatProfileEntity?> = dao.getUserProfile()
    val allBloodLogs: Flow<List<DailyBloodLogEntity>> = dao.getAllBloodLogs()

    suspend fun saveCalculation(entity: CalculationHistoryEntity): Long {
        val existing = dao.findHistory(entity.caseType, entity.startEpochMillis, entity.endEpochMillis)
        return existing?.id ?: dao.insertHistory(entity)
    }

    suspend fun deleteCalculation(id: Long) {
        dao.deleteHistory(id)
    }

    suspend fun clearHistory() {
        dao.clearHistory()
    }

    suspend fun addQadhaPrayer(prayer: QadhaPrayerEntity): Long {
        val existing = dao.findQadhaPrayer(prayer.prayerName, prayer.dateString, prayer.reason)
        return existing?.id ?: dao.insertQadhaPrayer(prayer)
    }

    suspend fun toggleQadhaCompleted(id: Long, isCompleted: Boolean) {
        dao.updateQadhaStatus(id, isCompleted)
    }

    suspend fun deleteQadhaPrayer(id: Long) {
        dao.deleteQadhaPrayer(id)
    }

    suspend fun saveProfile(profile: UserAdatProfileEntity) {
        dao.saveUserProfile(profile)
    }

    suspend fun getBloodLogByDate(dateString: String): DailyBloodLogEntity? {
        return dao.getBloodLogByDate(dateString)
    }

    suspend fun saveBloodLog(entity: DailyBloodLogEntity): Long {
        return dao.insertBloodLog(entity)
    }

    suspend fun deleteBloodLog(dateString: String) {
        dao.deleteBloodLog(dateString)
    }
}

package com.example.data.repository

import com.example.data.dao.FiqihDao
import com.example.data.entities.CalculationHistoryEntity
import com.example.data.entities.QadhaPrayerEntity
import com.example.data.entities.UserAdatProfileEntity
import kotlinx.coroutines.flow.Flow

class FiqihRepository(private val dao: FiqihDao) {

    val allHistory: Flow<List<CalculationHistoryEntity>> = dao.getAllHistory()
    val allQadhaPrayers: Flow<List<QadhaPrayerEntity>> = dao.getAllQadhaPrayers()
    val userProfile: Flow<UserAdatProfileEntity?> = dao.getUserProfile()

    suspend fun saveCalculation(entity: CalculationHistoryEntity): Long {
        return dao.insertHistory(entity)
    }

    suspend fun deleteCalculation(id: Long) {
        dao.deleteHistory(id)
    }

    suspend fun clearHistory() {
        dao.clearHistory()
    }

    suspend fun addQadhaPrayer(prayer: QadhaPrayerEntity): Long {
        return dao.insertQadhaPrayer(prayer)
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
}

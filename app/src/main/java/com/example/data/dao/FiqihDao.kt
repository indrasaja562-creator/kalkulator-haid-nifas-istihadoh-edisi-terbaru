package com.example.data.dao

import androidx.room.*
import com.example.data.entities.CalculationHistoryEntity
import com.example.data.entities.DailyBloodLogEntity
import com.example.data.entities.QadhaPrayerEntity
import com.example.data.entities.UserAdatProfileEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FiqihDao {

    // Calculation History
    @Query("SELECT * FROM calculation_history ORDER BY timestamp DESC")
    fun getAllHistory(): Flow<List<CalculationHistoryEntity>>

    @Query("SELECT * FROM calculation_history WHERE caseType = :caseType AND startEpochMillis = :startEpochMillis AND endEpochMillis = :endEpochMillis LIMIT 1")
    suspend fun findHistory(caseType: String, startEpochMillis: Long, endEpochMillis: Long): CalculationHistoryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHistory(entity: CalculationHistoryEntity): Long

    @Query("DELETE FROM calculation_history WHERE id = :id")
    suspend fun deleteHistory(id: Long)

    @Query("DELETE FROM calculation_history")
    suspend fun clearHistory()

    // Qadha Prayers
    @Query("SELECT * FROM qadha_prayers ORDER BY isCompleted ASC, timestamp DESC")
    fun getAllQadhaPrayers(): Flow<List<QadhaPrayerEntity>>

    @Query("SELECT * FROM qadha_prayers WHERE prayerName = :prayerName AND dateString = :dateString AND reason = :reason LIMIT 1")
    suspend fun findQadhaPrayer(prayerName: String, dateString: String, reason: String): QadhaPrayerEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQadhaPrayer(prayer: QadhaPrayerEntity): Long

    @Query("UPDATE qadha_prayers SET isCompleted = :completed WHERE id = :id")
    suspend fun updateQadhaStatus(id: Long, completed: Boolean)

    @Query("DELETE FROM qadha_prayers WHERE id = :id")
    suspend fun deleteQadhaPrayer(id: Long)

    // User Profile
    @Query("SELECT * FROM user_adat_profile WHERE id = 1 LIMIT 1")
    fun getUserProfile(): Flow<UserAdatProfileEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveUserProfile(profile: UserAdatProfileEntity)

    // Daily Blood Logs
    @Query("SELECT * FROM daily_blood_logs ORDER BY epochDay ASC")
    fun getAllBloodLogs(): Flow<List<DailyBloodLogEntity>>

    @Query("SELECT * FROM daily_blood_logs WHERE dateString = :dateString LIMIT 1")
    suspend fun getBloodLogByDate(dateString: String): DailyBloodLogEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBloodLog(entity: DailyBloodLogEntity): Long

    @Query("DELETE FROM daily_blood_logs WHERE dateString = :dateString")
    suspend fun deleteBloodLog(dateString: String)
}

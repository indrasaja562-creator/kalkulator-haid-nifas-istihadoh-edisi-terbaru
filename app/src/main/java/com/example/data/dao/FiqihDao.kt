package com.example.data.dao

import androidx.room.*
import com.example.data.entities.CalculationHistoryEntity
import com.example.data.entities.QadhaPrayerEntity
import com.example.data.entities.UserAdatProfileEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FiqihDao {

    // Calculation History
    @Query("SELECT * FROM calculation_history ORDER BY timestamp DESC")
    fun getAllHistory(): Flow<List<CalculationHistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHistory(entity: CalculationHistoryEntity): Long

    @Query("DELETE FROM calculation_history WHERE id = :id")
    suspend fun deleteHistory(id: Long)

    @Query("DELETE FROM calculation_history")
    suspend fun clearHistory()

    // Qadha Prayers
    @Query("SELECT * FROM qadha_prayers ORDER BY isCompleted ASC, timestamp DESC")
    fun getAllQadhaPrayers(): Flow<List<QadhaPrayerEntity>>

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
}

package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.dao.FiqihDao
import com.example.data.entities.CalculationHistoryEntity
import com.example.data.entities.QadhaPrayerEntity
import com.example.data.entities.UserAdatProfileEntity

@Database(
    entities = [
        CalculationHistoryEntity::class,
        QadhaPrayerEntity::class,
        UserAdatProfileEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun fiqihDao(): FiqihDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "kalkulator_haid_db"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}

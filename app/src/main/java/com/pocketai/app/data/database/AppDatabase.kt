package com.pocketai.app.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.pocketai.app.data.dao.ExpenseDao
import com.pocketai.app.data.model.Expense

@Database(
    entities = [Expense::class],
    version = 7, // Phase 17: Veryfi-style enriched fields
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun expenseDao(): ExpenseDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "pocket_ai_database"
                )
                .fallbackToDestructiveMigration() // Recreates database on schema changes
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
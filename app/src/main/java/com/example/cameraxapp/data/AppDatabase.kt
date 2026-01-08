package com.example.cameraxapp.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.cameraxapp.data.converter.DateConverter
import com.example.cameraxapp.data.converter.StringListConverter
import com.example.cameraxapp.data.dao.PhotoBoothDao
import com.example.cameraxapp.data.model.PhotoBoothEntity

@Database(entities = [PhotoBoothEntity::class], version = 1)
@TypeConverters(StringListConverter::class, DateConverter::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun photoBoothDao(): PhotoBoothDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "app_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
} 
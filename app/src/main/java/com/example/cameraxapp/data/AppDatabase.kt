package com.example.cameraxapp.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.cameraxapp.data.converter.DateConverter
import com.example.cameraxapp.data.converter.StringListConverter
import com.example.cameraxapp.data.dao.PhotoBoothDao
import com.example.cameraxapp.data.dao.RecipeDao
import com.example.cameraxapp.data.migration.PhotoBoothMigrations
import com.example.cameraxapp.data.migration.RecipeMigrations
import com.example.cameraxapp.data.model.PhotoBoothEntity
import com.example.cameraxapp.data.model.RecipeEntity

@Database(entities = [PhotoBoothEntity::class, RecipeEntity::class], version = 3)
@TypeConverters(StringListConverter::class, DateConverter::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun photoBoothDao(): PhotoBoothDao

    abstract fun recipeDao(): RecipeDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "app_database"
                ).addMigrations(
                    PhotoBoothMigrations.MIGRATION_1_2,
                    RecipeMigrations.MIGRATION_2_3,
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
} 
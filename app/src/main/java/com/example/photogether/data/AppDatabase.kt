package com.example.photogether.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.photogether.data.converter.DateConverter
import com.example.photogether.data.converter.StringListConverter
import com.example.photogether.data.dao.PhotoBoothDao
import com.example.photogether.data.dao.RecipeDao
import com.example.photogether.data.migration.PhotoBoothMigrations
import com.example.photogether.data.migration.RecipeMigrations
import com.example.photogether.data.model.PhotoBoothEntity
import com.example.photogether.data.model.RecipeEntity

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
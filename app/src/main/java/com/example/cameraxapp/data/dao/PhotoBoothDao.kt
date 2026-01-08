package com.example.cameraxapp.data.dao

import androidx.room.*
import com.example.cameraxapp.data.model.PhotoBoothEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PhotoBoothDao {
    @Query("SELECT * FROM photo_booth ORDER BY createdAt DESC")
    fun getAllPhotoBooths(): Flow<List<PhotoBoothEntity>>

    @Query("SELECT * FROM photo_booth WHERE id = :id")
    suspend fun getPhotoBoothById(id: Long): PhotoBoothEntity?

    @Insert
    suspend fun insertPhotoBooth(photoBooth: PhotoBoothEntity): Long

    @Delete
    suspend fun deletePhotoBooth(photoBooth: PhotoBoothEntity)
} 
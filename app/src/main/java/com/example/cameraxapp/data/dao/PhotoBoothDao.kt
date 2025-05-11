package com.example.cameraxapp.data.dao

import androidx.room.*
import com.example.cameraxapp.data.model.PhotoBooth
import kotlinx.coroutines.flow.Flow

@Dao
interface PhotoBoothDao {
    @Query("SELECT * FROM photo_booth ORDER BY createdAt DESC")
    fun getAllPhotoBooths(): Flow<List<PhotoBooth>>

    @Query("SELECT * FROM photo_booth WHERE id = :id")
    suspend fun getPhotoBoothById(id: Long): PhotoBooth?

    @Insert
    suspend fun insertPhotoBooth(photoBooth: PhotoBooth): Long

    @Delete
    suspend fun deletePhotoBooth(photoBooth: PhotoBooth)
} 
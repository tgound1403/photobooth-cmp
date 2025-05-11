package com.example.cameraxapp.data.repository

import android.content.Context
import com.example.cameraxapp.data.AppDatabase
import com.example.cameraxapp.data.model.PhotoBooth
import kotlinx.coroutines.flow.Flow

class PhotoBoothRepository(private val context: Context) {
    private val database = AppDatabase.getDatabase(context)
    private val photoBoothDao = database.photoBoothDao()

    suspend fun insertPhotoBooth(photoBooth: PhotoBooth): Long {
        return photoBoothDao.insertPhotoBooth(photoBooth)
    }

    suspend fun getPhotoBoothById(id: Long): PhotoBooth? {
        return photoBoothDao.getPhotoBoothById(id)
    }

    fun getAllPhotoBooths(): Flow<List<PhotoBooth>> {
        return photoBoothDao.getAllPhotoBooths()
    }

    suspend fun deletePhotoBooth(photoBooth: PhotoBooth) {
        photoBoothDao.deletePhotoBooth(photoBooth)
    }
} 
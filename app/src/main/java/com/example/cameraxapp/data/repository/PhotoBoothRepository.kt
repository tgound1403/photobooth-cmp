package com.example.cameraxapp.data.repository

import android.content.Context
import com.example.cameraxapp.data.AppDatabase
import com.example.cameraxapp.data.model.PhotoBoothEntity
import com.example.cameraxapp.shared.domain.model.PhotoBooth
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class PhotoBoothRepository(private val context: Context) {
    private val database = AppDatabase.getDatabase(context)
    private val photoBoothDao = database.photoBoothDao()

    suspend fun insertPhotoBooth(photoBooth: PhotoBooth): Long {
        return photoBoothDao.insertPhotoBooth(PhotoBoothEntity.fromDomain(photoBooth))
    }

    suspend fun getPhotoBoothById(id: Long): PhotoBooth? {
        return photoBoothDao.getPhotoBoothById(id)?.toDomain()
    }

    fun getAllPhotoBooths(): Flow<List<PhotoBooth>> {
        return photoBoothDao.getAllPhotoBooths().map { list ->
            list.map { it.toDomain() }
        }
    }

    suspend fun deletePhotoBooth(photoBooth: PhotoBooth) {
        photoBoothDao.deletePhotoBooth(PhotoBoothEntity.fromDomain(photoBooth))
    }
} 

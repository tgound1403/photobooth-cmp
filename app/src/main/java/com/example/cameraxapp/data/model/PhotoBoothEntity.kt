package com.example.cameraxapp.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.example.cameraxapp.data.converter.StringListConverter
import com.example.cameraxapp.shared.domain.model.PhotoBooth
import java.util.Date

@Entity(tableName = "photo_booth")
@TypeConverters(StringListConverter::class)
data class PhotoBoothEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val imagePaths: List<String>,
    val createdAt: Date
) {
    fun toDomain(): PhotoBooth {
        return PhotoBooth(
            id = id,
            imagePaths = imagePaths,
            createdAt = createdAt.time
        )
    }

    companion object {
        fun fromDomain(domain: PhotoBooth): PhotoBoothEntity {
            return PhotoBoothEntity(
                id = domain.id,
                imagePaths = domain.imagePaths,
                createdAt = Date(domain.createdAt)
            )
        }
    }
} 

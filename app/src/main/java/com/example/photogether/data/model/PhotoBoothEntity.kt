package com.example.photogether.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.example.photogether.core.render.EditParams
import com.example.photogether.data.converter.EditParamsConverter
import com.example.photogether.data.converter.StringListConverter
import com.example.photogether.shared.domain.model.PhotoBooth
import java.util.Date

@Entity(tableName = "photo_booth")
@TypeConverters(StringListConverter::class, EditParamsConverter::class)
data class PhotoBoothEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val imagePaths: List<String>,
    val createdAt: Date,
    val editParams: EditParams
) {
    fun toDomain(): PhotoBooth {
        return PhotoBooth(
            id = id,
            imagePaths = imagePaths,
            createdAt = createdAt.time
        )
    }

    companion object {
        fun fromDomain(domain: PhotoBooth, editParams: EditParams = EditParams()): PhotoBoothEntity {
            return PhotoBoothEntity(
                id = domain.id,
                imagePaths = domain.imagePaths,
                createdAt = Date(domain.createdAt),
                editParams = editParams
            )
        }
    }
} 

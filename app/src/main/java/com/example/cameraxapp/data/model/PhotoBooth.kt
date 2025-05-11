package com.example.cameraxapp.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.example.cameraxapp.data.converter.StringListConverter
import java.util.Date

@Entity(tableName = "photo_booth")
@TypeConverters(StringListConverter::class)
data class PhotoBooth(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val imagePaths: List<String>,
    val createdAt: Date
) 
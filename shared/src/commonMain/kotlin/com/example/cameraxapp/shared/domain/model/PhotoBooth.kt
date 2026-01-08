package com.example.cameraxapp.shared.domain.model

data class PhotoBooth(
    val id: Long = 0,
    val imagePaths: List<String>,
    val createdAt: Long // Timestamp
)

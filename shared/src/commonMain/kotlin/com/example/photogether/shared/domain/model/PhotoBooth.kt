package com.example.photogether.shared.domain.model

data class PhotoBooth(
    val id: Long = 0,
    val imagePaths: List<String>,
    val createdAt: Long // Timestamp
)

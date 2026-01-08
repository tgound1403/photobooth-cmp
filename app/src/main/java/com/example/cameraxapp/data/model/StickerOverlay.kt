package com.example.cameraxapp.data.model

data class StickerOverlay(
    val id: String = java.util.UUID.randomUUID().toString(),
    val type: OverlayType,
    val content: String, // Emoji for sticker, text content for text
    var offsetX: Float = 0f,
    var offsetY: Float = 0f,
    var scale: Float = 1f,
    var rotation: Float = 0f
)

enum class OverlayType {
    STICKER,
    TEXT
}

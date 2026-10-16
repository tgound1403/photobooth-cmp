package com.example.cameraxapp.core.share

/**
 * Đích chia sẻ ảnh. Thuần Kotlin, không phụ thuộc Android, để unit test trên JVM.
 *
 * [packageName] là null với đích không gắn với app cụ thể (OTHER).
 * [action] là action của Intent dùng để mở đích đó.
 */
enum class ShareTarget(val label: String, val packageName: String?, val action: String) {
    TIKTOK("TikTok", "com.zhiliaoapp.musically", SEND),
    INSTAGRAM("Instagram", "com.instagram.android", SEND),
    IG_STORY("IG Story", "com.instagram.android", "com.instagram.share.ADD_TO_STORY"),
    FACEBOOK("Facebook", "com.facebook.katana", SEND),
    MESSENGER("Messenger", "com.facebook.orca", SEND),
    OTHER("Khác", null, SEND),
}

private const val SEND = "android.intent.action.SEND"

/**
 * True khi không nên mở đích trực tiếp mà phải đi qua chooser chung:
 * đích không gắn package, hoặc package chưa có trong [installed].
 */
fun usesChooser(target: ShareTarget, installed: Set<String>): Boolean {
    val pkg = target.packageName ?: return true
    return pkg !in installed
}

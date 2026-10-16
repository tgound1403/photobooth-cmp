package com.example.photogether.core.share

import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri

private const val CHOOSER_TITLE = "Chia sẻ ảnh"

/** Intent gửi ảnh JPEG tới một package cụ thể, hoặc để null để hệ thống hỏi. */
fun imageSend(uri: Uri, pkg: String?): Intent =
    Intent(Intent.ACTION_SEND).apply {
        type = "image/jpeg"
        putExtra(Intent.EXTRA_STREAM, uri)
        // ClipData giúp quyền đọc URI được chuyển tiếp qua chooser trên các máy cũ.
        clipData = ClipData.newRawUri("", uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        setPackage(pkg)
    }

/** Intent đăng ảnh lên Instagram Story, theo mẫu của PhotoBoothViewModel. */
fun instagramStory(uri: Uri, ctx: Context): Intent =
    Intent(ShareTarget.IG_STORY.action).apply {
        setDataAndType(uri, "image/*")
        clipData = ClipData.newRawUri("", uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        putExtra("source_application", ctx.packageName)
    }

/** Chooser chung, dùng khi đích chưa cài hoặc cần fallback. */
fun chooser(uri: Uri): Intent = Intent.createChooser(imageSend(uri, null), CHOOSER_TITLE)

/**
 * Chọn Intent cho một đích. Đích chưa cài (hoặc không xác định được) thì trả chooser.
 */
fun forTarget(target: ShareTarget, uri: Uri, ctx: Context): Intent {
    val installed = installedPackages(ctx)
    return when {
        usesChooser(target, installed) -> chooser(uri)
        target == ShareTarget.IG_STORY -> instagramStory(uri, ctx)
        else -> imageSend(uri, target.packageName)
    }
}

/**
 * Các package trong enum đang được cài. Trên Android 11 trở lên, nếu manifest
 * không có khai báo `<queries>` thì getPackageInfo trả NameNotFound cho cả app
 * đã cài, nên kết quả sẽ thiếu và đích đó đi qua chooser.
 */
@Suppress("DEPRECATION")
fun installedPackages(ctx: Context): Set<String> {
    val pm = ctx.packageManager
    return ShareTarget.entries
        .mapNotNull { it.packageName }
        .distinct()
        .filter { pkg ->
            try {
                pm.getPackageInfo(pkg, 0)
                true
            } catch (e: PackageManager.NameNotFoundException) {
                false
            }
        }
        .toSet()
}

/**
 * Mở đích chia sẻ. Nếu hệ thống không tìm được activity (ActivityNotFoundException)
 * thì mở chooser chung thay thế.
 */
fun launch(ctx: Context, uri: Uri, target: ShareTarget) {
    try {
        ctx.startActivity(forTarget(target, uri, ctx))
    } catch (e: ActivityNotFoundException) {
        ctx.startActivity(chooser(uri))
    }
}

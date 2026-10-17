package com.example.photogether.core.render

/**
 * A camera "body" shown on the picker screen. While a camera is selected, the viewfinder offers only
 * its own [filterIds]; choosing the camera applies the first one ([filterId]).
 * [name] is a temporary label, pending the final naming decision. [subtitle] is the user-facing
 * description under it: the camera's look, then the scenes it suits. It is shown on up to two lines.
 */
data class CameraProfile(
    val id: String,
    val name: String,
    val subtitle: String,
    /** Catalog ids this camera can use, in the order the viewfinder strip lists them. Never empty. */
    val filterIds: List<String>,
    /** ARGB colour the 3D body is painted with. */
    val modelTint: Int,
    /** Asset path of the glTF/GLB body model. All cameras share one model for now. */
    val modelAsset: String = DEFAULT_MODEL,
    /** Film effects applied when this camera is chosen. */
    val recipe: CameraRecipe = CameraRecipe(),
) {
    /** The filter applied when the camera is chosen. */
    val filterId: String get() = filterIds.first()

    companion object {
        const val DEFAULT_MODEL = "models/nikon_s3.glb"
    }
}

object CameraProfiles {

    /** Key used in savedStateHandle to return the chosen camera id to the camera screen. */
    const val RESULT_KEY = "picked_camera_id"

    /** Key the camera screen sets on its own entry so the picker can open on the camera in use. */
    const val CURRENT_KEY = "current_camera_id"

    val ALL: List<CameraProfile> =
        listOf(
            CameraProfile(
                id = "classic35",
                name = "Classic 35",
                subtitle = "Màu film ấm, hạt nhẹ, khung ngày cam. Hợp chân dung, đường phố, kỷ niệm",
                filterIds = listOf("kodak_portra", "original", "kodak_colorplus", "kodachrome"),
                modelTint = 0xFFC9A15B.toInt(),
                recipe = CameraRecipe(leak = 0.15f, dust = 0.10f, halation = 0f, grain = 0.10f, vignette = 0.10f, dateStamp = DateStampStyle.ORANGE_LCD, aspect = CameraAspect.FOUR_THREE, defaultFlash = FlashMode.AUTO, flashStyle = FlashStyle.HARSH, skin = ViewfinderSkin.FILM_FRAME, shutterSound = ShutterSound.DISPOSABLE),
            ),
            CameraProfile(
                id = "fuji_mini",
                name = "Fuji Mini",
                subtitle = "Xanh lá tươi, bão hòa nhẹ, rò sáng mềm. Hợp du lịch, cây cối, bạn bè",
                filterIds = listOf("fuji_classic_neg", "fuji_forest", "fuji_superia", "fuji_velvia"),
                modelTint = 0xFFEFE6D2.toInt(),
                recipe = CameraRecipe(leak = 0f, dust = 0f, halation = 0.10f, grain = 0.05f, vignette = 0f, dateStamp = DateStampStyle.NONE, aspect = CameraAspect.FOUR_THREE, defaultFlash = FlashMode.AUTO, flashStyle = FlashStyle.WARM, skin = ViewfinderSkin.RANGEFINDER, shutterSound = ShutterSound.SOFT),
            ),
            CameraProfile(
                id = "golden",
                name = "Golden",
                subtitle = "Nắng vàng ấm, rò sáng, nâng bóng nhẹ. Hợp hoàng hôn, picnic, chân dung ngoài trời",
                filterIds = listOf("kodak_gold", "vsco_faded_warm", "airy_cream", "seoul_soft", "pastel_sea"),
                modelTint = 0xFFE0A91C.toInt(),
                recipe = CameraRecipe(leak = 0.30f, dust = 0.05f, halation = 0.15f, grain = 0.05f, vignette = 0.15f, dateStamp = DateStampStyle.ORANGE_LCD, aspect = CameraAspect.FOUR_THREE, defaultFlash = FlashMode.OFF, flashStyle = FlashStyle.WARM, skin = ViewfinderSkin.SOFT_GLASS, shutterSound = ShutterSound.SOFT),
            ),
            CameraProfile(
                id = "night",
                name = "Night Teal",
                subtitle = "Tông trung tính, bóng nâng nhẹ, hạt dày, flash gắt. Hợp đêm phố, đèn neon, trời mưa",
                filterIds = listOf("fuji_classic_chrome", "vsco_cool_fade", "vsco_soft_film", "tokyo_rain", "neon_night"),
                modelTint = 0xFF1F9AA3.toInt(),
                recipe = CameraRecipe(leak = 0f, dust = 0f, halation = 0.35f, grain = 0.20f, vignette = 0.25f, dateStamp = DateStampStyle.NONE, aspect = CameraAspect.FOUR_THREE, defaultFlash = FlashMode.ON, flashStyle = FlashStyle.HARSH, skin = ViewfinderSkin.LCD_DIGICAM, shutterSound = ShutterSound.MECH),
            ),
            CameraProfile(
                id = "silver",
                name = "Silver",
                subtitle = "Đen trắng hạt thô, tương phản cao, khung vuông. Hợp chân dung, kiến trúc, phố",
                filterIds = listOf("bw_tri_x", "bw_noir", "bw_soft_mono"),
                modelTint = 0xFFC9CED6.toInt(),
                recipe = CameraRecipe(leak = 0f, dust = 0.15f, halation = 0f, grain = 0.25f, vignette = 0.20f, dateStamp = DateStampStyle.WHITE_DIGITAL, aspect = CameraAspect.ONE_ONE, defaultFlash = FlashMode.OFF, flashStyle = FlashStyle.NONE, skin = ViewfinderSkin.RANGEFINDER_MONO, shutterSound = ShutterSound.MECH),
            ),
        ) + JAPAN_CAMERAS

    /** Returns the profile with [id], or the first profile when the id is null or unknown. */
    fun byId(id: String?): CameraProfile = ALL.firstOrNull { it.id == id } ?: ALL.first()

    /** Returns the profile that offers [filterId], or null when no profile does. */
    fun forFilter(filterId: String?): CameraProfile? =
        if (filterId == null) null else ALL.firstOrNull { filterId in it.filterIds }
}

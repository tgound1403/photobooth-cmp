package com.example.photogether.core.render

/**
 * A camera "body" shown on the picker screen. While a camera is selected, the viewfinder offers only
 * its own [filterIds]; choosing the camera applies the first one ([filterId]).
 * [name] and [subtitle] are temporary labels, pending the final naming decision.
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
                subtitle = "Kodak",
                filterIds = listOf("kodak", "original", "nikon", "pentax", "minolta"),
                modelTint = 0xFFC9A15B.toInt(),
                recipe = CameraRecipe(leak = 0.15f, dust = 0.10f, halation = 0f, grain = 0.10f, vignette = 0.10f, dateStamp = DateStampStyle.ORANGE_LCD, aspect = CameraAspect.FOUR_THREE),
            ),
            CameraProfile(
                id = "fuji_mini",
                name = "Fuji Mini",
                subtitle = "FujiFilm",
                filterIds = listOf("fujifilm", "soft_glow", "rose_skin", "honey_light", "clear_tone", "peach_glow", "cream_matte"),
                modelTint = 0xFFEFE6D2.toInt(),
                recipe = CameraRecipe(leak = 0f, dust = 0f, halation = 0.10f, grain = 0.05f, vignette = 0f, dateStamp = DateStampStyle.NONE, aspect = CameraAspect.FOUR_THREE),
            ),
            CameraProfile(
                id = "golden",
                name = "Golden",
                subtitle = "Golden Hour",
                filterIds = listOf("golden_hour", "faded_mint", "amber_roll", "cool_negative", "sunday_film", "dusk_print"),
                modelTint = 0xFFE0A91C.toInt(),
                recipe = CameraRecipe(leak = 0.30f, dust = 0.05f, halation = 0.15f, grain = 0.05f, vignette = 0.15f, dateStamp = DateStampStyle.ORANGE_LCD, aspect = CameraAspect.FOUR_THREE),
            ),
            CameraProfile(
                id = "night",
                name = "Night Teal",
                subtitle = "Teal Night",
                filterIds = listOf("teal_night", "moody_blue", "autumn_haze", "velvet_dusk", "pastel_dream", "winter_fog"),
                modelTint = 0xFF1F9AA3.toInt(),
                recipe = CameraRecipe(leak = 0f, dust = 0f, halation = 0.35f, grain = 0.20f, vignette = 0.25f, dateStamp = DateStampStyle.NONE, aspect = CameraAspect.FOUR_THREE),
            ),
            CameraProfile(
                id = "silver",
                name = "Silver",
                subtitle = "Silver Gelatin",
                filterIds = listOf("silver_gelatin", "bnw", "noir_contrast", "soft_mono", "crimson_mono", "ink_wash", "light_mono"),
                modelTint = 0xFFC9CED6.toInt(),
                recipe = CameraRecipe(leak = 0f, dust = 0.15f, halation = 0f, grain = 0.25f, vignette = 0.20f, dateStamp = DateStampStyle.WHITE_DIGITAL, aspect = CameraAspect.ONE_ONE),
            ),
        )

    /** Returns the profile with [id], or the first profile when the id is null or unknown. */
    fun byId(id: String?): CameraProfile = ALL.firstOrNull { it.id == id } ?: ALL.first()

    /** Returns the profile that offers [filterId], or null when no profile does. */
    fun forFilter(filterId: String?): CameraProfile? =
        if (filterId == null) null else ALL.firstOrNull { filterId in it.filterIds }
}

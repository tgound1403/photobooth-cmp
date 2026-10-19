package com.example.photogether.core.moments

/**
 * Builds the MediaStore selection for the Moments gallery.
 *
 * Pure Kotlin on purpose: only string literals are used, so the logic runs
 * on the JVM in unit tests without any Android classes.
 */
object MomentQuery {

    const val SORT = "date_added DESC"

    /** SQL text `ESCAPE '\'`, so that `\_` matches a literal underscore. */
    private const val ESC = "ESCAPE '\\'"

    private const val CAMERA_DIR_Q = "Pictures/CameraX-Image%"
    private const val CAMERA_DIR_LEGACY = "%/CameraX-Image/%"
    private const val PICTURES_DIR_LEGACY = "%/Pictures/%"
    private const val EDITED_NAME = "Edited\\_%"
    private const val PHOTOBOOTH_NAME = "PhotoBooth\\_%"
    private const val CAMERA_NAME_GLOB = "[0-9][0-9][0-9][0-9]-[0-9][0-9]-[0-9][0-9]-*"
    private const val MIME_JPEG = "image/jpeg"
    private const val MIME_GIF = "image/gif"

    /**
     * Returns the WHERE clause and its arguments.
     *
     * API 29 and above: match on RELATIVE_PATH (the column exists there).
     * Below 29: RELATIVE_PATH is not reliable, so match on the _data path.
     * Photobooth GIFs are excluded unless [includeGif] is true.
     */
    fun selection(sdkInt: Int, includeGif: Boolean = false): Pair<String, Array<String>> {
        val mimes: Array<String> = if (includeGif) arrayOf(MIME_JPEG, MIME_GIF) else arrayOf(MIME_JPEG)
        val mimeClause = "mime_type IN (" + mimes.joinToString(",") { "?" } + ")"

        return if (sdkInt >= 29) {
            val where = "(relative_path LIKE ? OR _display_name LIKE ? $ESC OR _display_name LIKE ? $ESC)"
            Pair(
                "$where AND $mimeClause",
                arrayOf(CAMERA_DIR_Q, EDITED_NAME, PHOTOBOOTH_NAME) + mimes,
            )
        } else {
            val where = "(_data LIKE ? OR _display_name LIKE ? $ESC OR _display_name LIKE ? $ESC " +
                "OR (_data LIKE ? AND _display_name GLOB ?))"
            Pair(
                "$where AND $mimeClause",
                arrayOf(
                    CAMERA_DIR_LEGACY,
                    EDITED_NAME,
                    PHOTOBOOTH_NAME,
                    PICTURES_DIR_LEGACY,
                    CAMERA_NAME_GLOB,
                ) + mimes,
            )
        }
    }

    /** First [max] items of [list], in the same order. Returns empty when [max] is not positive. */
    fun stackPreview(list: List<Moment>, max: Int = 4): List<Moment> =
        if (max <= 0) emptyList() else list.take(max)

    /** Header label for the grid, for example "ALL PHOTOS · 12". Negative counts are shown as 0. */
    fun countLabel(n: Int): String = "ALL PHOTOS · ${n.coerceAtLeast(0)}"

    /** Display name for a camera shot: the camera id, then the time stamp, for example "classic35_2026-10-17-12-00-00-000.jpg". */
    fun cameraShotName(cameraId: String, stamp: String): String = "${cameraId}_$stamp.jpg"

    /**
     * Camera id read from a display name made by [cameraShotName]. Null for any other name, such as
     * "Edited_..." or "PhotoBooth_..." files, and for camera shots saved before the id was added.
     * Camera ids can contain underscores ("jp_001"), so the id ends at the last underscore: the time
     * stamp has none.
     */
    fun cameraIdOf(displayName: String?): String? {
        val name = displayName ?: return null
        if (name.startsWith("Edited_") || name.startsWith("PhotoBooth_")) return null
        val underscore = name.lastIndexOf('_')
        if (underscore <= 0) return null
        return name.substring(0, underscore)
    }
}

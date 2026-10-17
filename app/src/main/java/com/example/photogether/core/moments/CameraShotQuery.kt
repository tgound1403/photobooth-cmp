package com.example.photogether.core.moments

/**
 * MediaStore selection for the photos taken on the camera screen only, so the camera's gallery button
 * does not list every image on the device. The folder matches where CameraViewModel saves captures.
 *
 * Pure Kotlin on purpose: only string literals are used, so the logic runs on the JVM in unit tests.
 */
object CameraShotQuery {

    private const val CAMERA_DIR_Q = "Pictures/CameraX-Image%"
    private const val CAMERA_DIR_LEGACY = "%/CameraX-Image/%"
    private const val PICTURES_DIR_LEGACY = "%/Pictures/%"
    private const val CAMERA_NAME_GLOB = "[0-9][0-9][0-9][0-9]-[0-9][0-9]-[0-9][0-9]-*"
    private const val MIME_JPEG = "image/jpeg"

    /**
     * Returns the WHERE clause and its arguments.
     *
     * API 29 and above: the capture folder, by RELATIVE_PATH.
     * Below 29: RELATIVE_PATH is not reliable, so match the _data path. Captures saved before the
     * folder was set land in Pictures, so those are matched by their date-style file name.
     */
    fun selection(sdkInt: Int): Pair<String, Array<String>> =
        if (sdkInt >= 29) {
            Pair("relative_path LIKE ? AND mime_type IN (?)", arrayOf(CAMERA_DIR_Q, MIME_JPEG))
        } else {
            Pair(
                "(_data LIKE ? OR (_data LIKE ? AND _display_name GLOB ?)) AND mime_type IN (?)",
                arrayOf(CAMERA_DIR_LEGACY, PICTURES_DIR_LEGACY, CAMERA_NAME_GLOB, MIME_JPEG),
            )
        }
}

package com.example.cameraxapp.core.render

/**
 * A camera "body" shown on the picker screen. Selecting one applies the filter [filterId].
 * [name] and [subtitle] are temporary labels, pending the final naming decision.
 */
data class CameraProfile(
    val id: String,
    val name: String,
    val subtitle: String,
    val filterId: String,
    val bodyColor: Int,
    val topColor: Int,
)

object CameraProfiles {

    /** Key used in savedStateHandle to return the chosen filterId to the camera screen. */
    const val RESULT_KEY = "picked_filter_id"

    val ALL: List<CameraProfile> =
        listOf(
            CameraProfile(
                id = "classic35",
                name = "Classic 35",
                subtitle = "Kodak",
                filterId = "kodak",
                bodyColor = 0xFF2A2A2A.toInt(),
                topColor = 0xFF111111.toInt(),
            ),
            CameraProfile(
                id = "fuji_mini",
                name = "Fuji Mini",
                subtitle = "FujiFilm",
                filterId = "fujifilm",
                bodyColor = 0xFFE9E4D8.toInt(),
                topColor = 0xFFB9B3A6.toInt(),
            ),
            CameraProfile(
                id = "golden",
                name = "Golden",
                subtitle = "Golden Hour",
                filterId = "golden_hour",
                bodyColor = 0xFFB8862F.toInt(),
                topColor = 0xFF7A5A1E.toInt(),
            ),
            CameraProfile(
                id = "night",
                name = "Night Teal",
                subtitle = "Teal Night",
                filterId = "teal_night",
                bodyColor = 0xFF1E5A5F.toInt(),
                topColor = 0xFF0F3437.toInt(),
            ),
            CameraProfile(
                id = "silver",
                name = "Silver",
                subtitle = "Silver Gelatin",
                filterId = "silver_gelatin",
                bodyColor = 0xFFBFC3C8.toInt(),
                topColor = 0xFF8C9096.toInt(),
            ),
        )

    /** Returns the profile with [id], or the first profile when the id is null or unknown. */
    fun byId(id: String?): CameraProfile = ALL.firstOrNull { it.id == id } ?: ALL.first()

    /** Returns the profile that uses [filterId], or null when no profile uses it. */
    fun forFilter(filterId: String?): CameraProfile? =
        if (filterId == null) null else ALL.firstOrNull { it.filterId == filterId }
}

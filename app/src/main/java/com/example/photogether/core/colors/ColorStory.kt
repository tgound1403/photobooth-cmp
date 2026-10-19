package com.example.photogether.core.colors

import com.example.photogether.core.locale.AppLanguage

/**
 * One colour group on Home: a Vietnamese name, the app filter that gives it its look, and the
 * landmark photos it is shown with. Pure data, so it is usable from JVM unit tests.
 *
 * [photos] are file names inside `assets/landmarks/` (see `credits.json` there for attribution).
 */
data class ColorStory(
    val id: String,
    val name: String,
    val code: String,
    val filterId: String,
    val tagline: String,
    val description: String,
    val photos: List<String>,
)

object ColorStories {

    const val PHOTOS_PER_STORY = 5

    val ALL: List<ColorStory> = listOf(
        ColorStory(
            id = "saigon",
            name = "Sài Gòn",
            code = "SGN",
            filterId = "jp007_007",
            tagline = "Nắng vàng, phố không ngủ",
            description = "Gam màu của thành phố không ngủ: nắng gắt được kéo về sắc vàng ấm, " +
                "bóng đổ đậm và da người lên màu mật ong. Hợp với những con phố đêm, " +
                "chợ đông và những buổi chiều kẹt xe.",
            photos = shots("saigon"),
        ),
        ColorStory(
            id = "hanoi",
            name = "Hà Nội",
            code = "HAN",
            filterId = "jp014_011",
            tagline = "Thu trầm, rêu phong, mưa phùn",
            description = "Màu của phố cổ vào cuối thu: xanh rêu trầm, tường vàng loang và một " +
                "chút mờ như sương sớm. Hợp với mặt tiền cũ, quán cà phê nhỏ và " +
                "những góc phố có nhiều thời gian.",
            photos = shots("hanoi"),
        ),
        ColorStory(
            id = "danang",
            name = "Đà Nẵng",
            code = "DAD",
            filterId = "jp002_071",
            tagline = "Biển xanh, cầu rồng, gió mặn",
            description = "Màu của thành phố bên biển: xanh ngọc sáng, trời trong và trắng sạch. " +
                "Hợp với sóng biển, bãi cát buổi sớm và đường chân trời của thành phố.",
            photos = shots("danang"),
        ),
        ColorStory(
            id = "hue",
            name = "Huế",
            code = "HUI",
            filterId = "jp021_001",
            tagline = "Tím nhạt, rêu cũ, cổ kính",
            description = "Màu của cố đô: sắc tím nhạt phai, rêu xanh xám và ánh sáng dịu như " +
                "sau cơn mưa. Hợp với kiến trúc cổ, hoa lá và những con đường chậm.",
            photos = shots("hue"),
        ),
        ColorStory(
            id = "hoian",
            name = "Hội An",
            code = "HOI",
            filterId = "jp006_015",
            tagline = "Đèn lồng, tường vàng, hoàng hôn",
            description = "Màu của phố cổ lên đèn: tường vàng nghệ, ánh đỏ cam và bóng tối " +
                "ấm. Hợp với hoàng hôn trên biển, những con hẻm nhỏ và chợ đêm.",
            photos = shots("hoian"),
        ),
        ColorStory(
            id = "dalat",
            name = "Đà Lạt",
            code = "DLI",
            filterId = "jp014_004",
            tagline = "Sương sớm, thông xanh, hồng phấn",
            description = "Màu của cao nguyên: xanh thông mát, sương mỏng và hồng phấn của hoa. " +
                "Hợp với rừng thông, đồi núi và những buổi sáng cần thêm một chiếc áo len.",
            // dalat_3 leads: misty pine hills read better as the Home cover than the flat lake shot.
            photos = listOf("dalat_3.jpg") + shots("dalat") - "dalat_3.jpg",
        ),
        ColorStory(
            id = "halong",
            name = "Hạ Long",
            code = "HLG",
            filterId = "jp027_023",
            tagline = "Ngọc bích, đá vôi, mây thấp",
            description = "Màu của vịnh di sản: xanh ngọc bích đậm, đá vôi xám lạnh và những " +
                "đám mây thấp. Hợp với mặt nước, núi đá và bình minh trên biển.",
            photos = shots("halong"),
        ),
    )

    fun byId(id: String): ColorStory? = ALL.firstOrNull { it.id == id }

    /**
     * The photos a story is shown with in [language]: its Vietnamese landmarks for Vietnamese, and
     * a slice of the generic samples in `assets/samples/` otherwise.
     */
    fun photosFor(story: ColorStory, language: AppLanguage): List<String> = when (language) {
        AppLanguage.VIETNAMESE -> story.photos
        AppLanguage.ENGLISH -> {
            val start = ALL.indexOf(story).coerceAtLeast(0) * PHOTOS_PER_STORY
            (0 until PHOTOS_PER_STORY).map { SAMPLE_FILES[(start + it) % SAMPLE_FILES.size] }
        }
    }

    /** Generic sample photos in `assets/samples/`, `sample_01.jpg` … `sample_15.jpg`. */
    val SAMPLE_FILES: List<String> = (1..15).map { "sample_%02d.jpg".format(it) }

    /** File names of a story's landmark photos: `<id>_1.jpg` … `<id>_5.jpg`. */
    private fun shots(id: String): List<String> = (1..PHOTOS_PER_STORY).map { "${id}_$it.jpg" }
}

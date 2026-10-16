package com.example.photogether.core.moments

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MomentQueryTest {

    private fun moment(i: Int) = Moment(uri = "content://media/external/images/media/$i", takenAt = i.toLong(), width = 1, height = 1)

    private fun questionMarks(where: String): Int = where.count { it == '?' }

    @Test
    fun selection_api29_and_34_use_relative_path_and_not_data() {
        for (sdk in listOf(29, 34)) {
            val (where, _) = MomentQuery.selection(sdk)
            assertTrue("sdk $sdk should contain relative_path", where.contains("relative_path"))
            assertFalse("sdk $sdk should not contain _data", where.contains("_data"))
        }
    }

    @Test
    fun selection_api28_and_24_use_data_and_not_relative_path() {
        for (sdk in listOf(28, 24)) {
            val (where, _) = MomentQuery.selection(sdk)
            assertTrue("sdk $sdk should contain _data", where.contains("_data"))
            assertFalse("sdk $sdk should not contain relative_path", where.contains("relative_path"))
        }
    }

    @Test
    fun selection_question_marks_match_args_size_for_every_sdk_and_gif_flag() {
        for (sdk in listOf(24, 28, 29, 30, 34)) {
            for (includeGif in listOf(false, true)) {
                val (where, args) = MomentQuery.selection(sdk, includeGif)
                assertEquals(
                    "sdk $sdk includeGif $includeGif",
                    args.size,
                    questionMarks(where),
                )
            }
        }
    }

    @Test
    fun selection_args_contain_camera_edited_and_photobooth_patterns() {
        val (_, argsQ) = MomentQuery.selection(34)
        assertTrue(argsQ.contains("Pictures/CameraX-Image%"))
        assertTrue(argsQ.contains("Edited\\_%"))
        assertTrue(argsQ.contains("PhotoBooth\\_%"))

        val (_, argsLegacy) = MomentQuery.selection(28)
        assertTrue(argsLegacy.contains("%/CameraX-Image/%"))
        assertTrue(argsLegacy.contains("Edited\\_%"))
        assertTrue(argsLegacy.contains("PhotoBooth\\_%"))
    }

    @Test
    fun selection_gif_flag_controls_image_gif_mime() {
        val (whereOff, argsOff) = MomentQuery.selection(34, includeGif = false)
        assertFalse(whereOff.contains("image/gif"))
        assertFalse(argsOff.contains("image/gif"))
        assertTrue(argsOff.contains("image/jpeg"))

        val (whereOn, argsOn) = MomentQuery.selection(34, includeGif = true)
        assertTrue(whereOn.contains("mime_type IN (?,?)"))
        assertTrue(argsOn.contains("image/gif"))
    }

    @Test
    fun selection_default_excludes_gif() {
        val (_, args) = MomentQuery.selection(29)
        assertFalse(args.contains("image/gif"))
    }

    @Test
    fun stackPreview_takes_first_four_in_order_by_default() {
        val list = (1..10).map { moment(it) }
        val preview = MomentQuery.stackPreview(list)
        assertEquals(listOf(moment(1), moment(2), moment(3), moment(4)), preview)
    }

    @Test
    fun stackPreview_returns_all_when_list_is_shorter_than_max() {
        val list = listOf(moment(1), moment(2))
        assertEquals(list, MomentQuery.stackPreview(list, max = 2))
    }

    @Test
    fun stackPreview_with_zero_max_is_empty() {
        assertTrue(MomentQuery.stackPreview((1..5).map { moment(it) }, max = 0).isEmpty())
    }

    @Test
    fun countLabel_formats_count_and_clamps_negative_to_zero() {
        assertEquals("ALL PHOTOS · 0", MomentQuery.countLabel(0))
        assertEquals("ALL PHOTOS · 0", MomentQuery.countLabel(-3))
        assertEquals("ALL PHOTOS · 12", MomentQuery.countLabel(12))
    }
}

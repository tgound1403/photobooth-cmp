package com.example.photogether.core.moments

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CameraShotQueryTest {

    @Test
    fun api29_and_up_match_only_the_capture_folder() {
        for (sdk in listOf(29, 34)) {
            val (where, args) = CameraShotQuery.selection(sdk)
            assertTrue("sdk $sdk should use relative_path", where.contains("relative_path"))
            assertFalse("sdk $sdk should not use _data", where.contains("_data"))
            assertEquals("Pictures/CameraX-Image%", args[0])
            assertEquals("image/jpeg", args.last())
        }
    }

    @Test
    fun below_29_matches_the_capture_folder_or_a_date_named_file_in_pictures() {
        val (where, args) = CameraShotQuery.selection(28)
        assertTrue(where.contains("_data"))
        assertTrue(where.contains("GLOB"))
        assertEquals("%/CameraX-Image/%", args[0])
        assertEquals("%/Pictures/%", args[1])
        assertEquals("image/jpeg", args.last())
    }

    @Test
    fun edited_and_photobooth_files_are_not_selected() {
        for (sdk in listOf(28, 34)) {
            val (_, args) = CameraShotQuery.selection(sdk)
            assertFalse(args.any { it.startsWith("Edited") || it.startsWith("PhotoBooth") })
        }
    }

    @Test
    fun placeholder_count_matches_arguments() {
        for (sdk in listOf(28, 29, 34)) {
            val (where, args) = CameraShotQuery.selection(sdk)
            assertEquals(args.size, where.count { it == '?' })
        }
    }
}

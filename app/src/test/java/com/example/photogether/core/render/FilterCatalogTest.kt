package com.example.photogether.core.render

import java.io.File
import java.io.RandomAccessFile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FilterCatalogTest {

    private val assetsDir = File("src/main/assets/filters")
    private val catalogFile = File(assetsDir, "catalog.json")
    private val filters by lazy { FilterCatalog.parse(catalogFile.readText()) }

    @Test
    fun catalogHasAtLeast16EntriesAndUniqueIds() {
        assertTrue("entries: ${filters.size}", filters.size >= 16)
        assertEquals(filters.size, filters.map { it.id }.toSet().size)
    }

    @Test
    fun originalIsFirstWithoutLut() {
        val first = filters.first()
        assertEquals(FilterCatalog.ORIGINAL_ID, first.id)
        assertNull(first.lut)
    }

    @Test
    fun everyLutFileExists() {
        filters.mapNotNull { it.lut }.forEach { lut ->
            assertTrue("missing $lut", File(assetsDir, lut).isFile)
        }
    }

    @Test
    fun groupsAreKnown() {
        val allowed = setOf("Fuji", "Kodak", "Korea", "Japan Pack", "Vintage")
        filters.forEach { assertTrue("unknown group ${it.group}", it.group in allowed) }
    }

    @Test
    fun everyLutIs512x512() {
        filters.mapNotNull { it.lut }.forEach { lut ->
            val (width, height) = readPngSize(File(assetsDir, lut))
            assertEquals("$lut width", 512, width)
            assertEquals("$lut height", 512, height)
        }
    }

    @Test
    fun parseReadsFieldsAndDefaults() {
        val json = """{"version":1,"filters":[{"id":"a","name":"A","group":"Film"}]}"""
        val parsed = FilterCatalog.parse(json)
        assertEquals(listOf(FilterInfo("a", "A", "Film", null, false)), parsed)
    }

    /** Reads width and height from the IHDR chunk: bytes 16..23, big-endian. */
    private fun readPngSize(file: File): Pair<Int, Int> {
        RandomAccessFile(file, "r").use { raf ->
            val header = ByteArray(24)
            raf.readFully(header)
            val signature = byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A)
            assertTrue("not a PNG: ${file.name}", header.copyOfRange(0, 8).contentEquals(signature))
            fun int32(offset: Int) =
                ((header[offset].toInt() and 0xFF) shl 24) or
                    ((header[offset + 1].toInt() and 0xFF) shl 16) or
                    ((header[offset + 2].toInt() and 0xFF) shl 8) or
                    (header[offset + 3].toInt() and 0xFF)
            return int32(16) to int32(20)
        }
    }
}

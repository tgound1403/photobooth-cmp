package com.example.cameraxapp.core.render

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.LruCache
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class FilterInfo(
    val id: String,
    val name: String,
    val group: String,
    val lut: String? = null,
    val premium: Boolean = false,
)

@Serializable
private data class CatalogFile(val version: Int, val filters: List<FilterInfo>)

/**
 * Lists the available filters from assets/filters/catalog.json and decodes their 3D LUT images.
 */
class FilterCatalog(private val context: Context) {

    companion object {
        const val ORIGINAL_ID = "original"
        private const val CATALOG_ASSET = "filters/catalog.json"
        private const val LUT_CACHE_ENTRIES = 8

        private val json = Json { ignoreUnknownKeys = true }

        /** Pure JSON parsing, usable from JVM unit tests. */
        fun parse(json: String): List<FilterInfo> =
            Companion.json.decodeFromString(CatalogFile.serializer(), json).filters
    }

    val filters: List<FilterInfo> by lazy {
        val text = context.assets.open(CATALOG_ASSET).bufferedReader().use { it.readText() }
        parse(text)
    }

    val groups: List<String> by lazy { filters.map { it.group }.distinct() }

    private val byId: Map<String, FilterInfo> by lazy { filters.associateBy { it.id } }

    private val lutCache = LruCache<String, Bitmap>(LUT_CACHE_ENTRIES)

    fun get(id: String): FilterInfo? = byId[id]

    /** Returns the decoded 512x512 LUT, or null for the original filter or an unknown id. */
    fun loadLut(id: String): Bitmap? {
        if (id == ORIGINAL_ID) return null
        val path = get(id)?.lut ?: return null
        lutCache.get(id)?.let { return it }
        val options =
            BitmapFactory.Options().apply {
                inScaled = false
                inPremultiplied = false
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }
        val bitmap =
            context.assets.open("filters/$path").use { BitmapFactory.decodeStream(it, null, options) }
                ?: return null
        lutCache.put(id, bitmap)
        return bitmap
    }
}

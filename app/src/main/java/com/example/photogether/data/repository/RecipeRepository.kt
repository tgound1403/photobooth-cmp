package com.example.photogether.data.repository

import android.graphics.Bitmap
import com.example.photogether.core.render.EditParams
import com.example.photogether.core.render.Recipe
import com.example.photogether.data.dao.RecipeDao
import com.example.photogether.data.model.RecipeEntity
import java.io.File
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

/** Saved recipes. Thumbnails are written as JPEG files under [thumbnailDir]. */
class RecipeRepository(
    private val dao: RecipeDao,
    private val thumbnailDir: File,
) {

    /** Recipes, newest first. Emits again after every save. */
    fun observeRecipes(): Flow<List<Recipe>> =
        dao.getAllRecipes().map { rows -> rows.map { it.toDomain() } }

    /**
     * Stores a recipe and returns its id. A failed thumbnail write saves the recipe without a
     * thumbnail rather than failing the save.
     */
    suspend fun saveRecipe(name: String, params: EditParams, thumbnail: Bitmap?): Long {
        val thumbnailPath = thumbnail?.let { writeThumbnail(it) }
        return dao.insertRecipe(
            RecipeEntity(
                name = name,
                paramsJson = params.toJson(),
                thumbnailPath = thumbnailPath,
                createdAt = System.currentTimeMillis(),
            ),
        )
    }

    private suspend fun writeThumbnail(bitmap: Bitmap): String? = withContext(Dispatchers.IO) {
        runCatching {
            thumbnailDir.mkdirs()
            val file = File(thumbnailDir, "${UUID.randomUUID()}.jpg")
            val written = file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.JPEG, THUMB_QUALITY, it) }
            if (written) file.absolutePath else null
        }.getOrNull()
    }

    private companion object {
        const val THUMB_QUALITY = 90
    }
}

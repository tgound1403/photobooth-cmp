package com.example.photogether.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.photogether.core.render.EditParams
import com.example.photogether.core.render.Recipe

/**
 * One saved recipe. [paramsJson] is [EditParams.toJson]. [thumbnailPath] points at a JPEG file in
 * app storage, not a blob, so list queries do not load image bytes into every row.
 */
@Entity(tableName = "recipe")
data class RecipeEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val paramsJson: String,
    val thumbnailPath: String?,
    val createdAt: Long,
) {
    fun toDomain(): Recipe =
        Recipe(
            id = id,
            name = name,
            // A row that no longer parses becomes an identity recipe instead of crashing the list.
            params = runCatching { EditParams.fromJson(paramsJson).clamped() }.getOrDefault(EditParams()),
            thumbnailPath = thumbnailPath,
            createdAt = createdAt,
        )
}

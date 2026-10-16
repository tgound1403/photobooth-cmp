package com.example.photogether.core.render

/** A saved set of edit values. [params] is the whole recipe; the other fields are for display. */
data class Recipe(
    val id: Long,
    val name: String,
    val params: EditParams,
    /** Absolute path of a small JPEG preview, or null when none was rendered. */
    val thumbnailPath: String?,
    val createdAt: Long,
)

/**
 * Recipes appear in the filter strips next to the catalog filters. Each recipe gets a filter id
 * with a prefix so the strips can tell the two sources apart. Pure Kotlin, usable from JVM tests.
 */
object RecipeFilters {
    const val PREFIX = "recipe:"

    fun filterId(recipe: Recipe): String = PREFIX + recipe.id

    fun toFilterInfo(recipe: Recipe): FilterInfo =
        FilterInfo(id = filterId(recipe), name = recipe.name, group = "Recipe")

    /** The recipe that [filterId] points at, or null when [filterId] is a catalog id. */
    fun recipeFor(filterId: String, recipes: List<Recipe>): Recipe? {
        if (!filterId.startsWith(PREFIX)) return null
        return recipes.firstOrNull { filterId(it) == filterId }
    }
}

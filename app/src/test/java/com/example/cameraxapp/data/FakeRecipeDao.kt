package com.example.cameraxapp.data

import com.example.cameraxapp.data.dao.RecipeDao
import com.example.cameraxapp.data.model.RecipeEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

/** In-memory [RecipeDao] for JVM tests. */
class FakeRecipeDao(initial: List<RecipeEntity> = emptyList()) : RecipeDao {
    private val rows = MutableStateFlow(initial)
    private var nextId = (initial.maxOfOrNull { it.id } ?: 0L) + 1

    override fun getAllRecipes(): Flow<List<RecipeEntity>> =
        rows.map { list -> list.sortedWith(compareByDescending<RecipeEntity> { it.createdAt }.thenByDescending { it.id }) }

    override suspend fun getRecipeById(id: Long): RecipeEntity? = rows.value.firstOrNull { it.id == id }

    override suspend fun insertRecipe(recipe: RecipeEntity): Long {
        val id = if (recipe.id != 0L) recipe.id else nextId++
        rows.value = rows.value + recipe.copy(id = id)
        return id
    }
}

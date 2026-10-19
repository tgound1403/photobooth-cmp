package com.example.photogether.data.repository

import com.example.photogether.core.render.EditParams
import com.example.photogether.data.FakeRecipeDao
import com.example.photogether.data.model.RecipeEntity
import java.io.File
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RecipeRepositoryTest {

    private val dir = File(System.getProperty("java.io.tmpdir"), "recipes-test")

    @Test
    fun savedRecipeComesBackWithTheSameParams() = runBlocking {
        val repo = RecipeRepository(FakeRecipeDao(), dir)
        val params = EditParams(filterId = "kodak_portra", filterIntensity = 0.4f, grain = 0.2f)

        val id = repo.saveRecipe("Warm", params, thumbnail = null)
        val recipes = repo.observeRecipes().first()

        assertEquals(1, recipes.size)
        assertEquals(id, recipes[0].id)
        assertEquals("Warm", recipes[0].name)
        assertEquals(params, recipes[0].params)
        assertNull(recipes[0].thumbnailPath)
    }

    @Test
    fun newestRecipeIsFirst() = runBlocking {
        val repo = RecipeRepository(FakeRecipeDao(), dir)
        repo.saveRecipe("First", EditParams(), null)
        repo.saveRecipe("Second", EditParams(), null)

        assertEquals(listOf("Second", "First"), repo.observeRecipes().first().map { it.name })
    }

    @Test
    fun rowWithBrokenJsonShowsIdentityInsteadOfCrashing() = runBlocking {
        val row = RecipeEntity(id = 7, name = "Broken", paramsJson = "{not json", thumbnailPath = null, createdAt = 1)
        val recipes = RecipeRepository(FakeRecipeDao(listOf(row)), dir).observeRecipes().first()

        assertEquals(EditParams(), recipes.single().params)
    }

    @Test
    fun rowWithOutOfRangeValuesIsClamped() = runBlocking {
        val row = RecipeEntity(id = 3, name = "Wild", paramsJson = """{"exposure":5}""", thumbnailPath = null, createdAt = 1)
        val recipes = RecipeRepository(FakeRecipeDao(listOf(row)), dir).observeRecipes().first()

        assertEquals(1f, recipes.single().params.exposure, 0f)
    }
}

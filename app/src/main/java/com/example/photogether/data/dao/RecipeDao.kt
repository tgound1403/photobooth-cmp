package com.example.photogether.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.photogether.data.model.RecipeEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RecipeDao {
    @Query("SELECT * FROM recipe ORDER BY createdAt DESC, id DESC")
    fun getAllRecipes(): Flow<List<RecipeEntity>>

    @Query("SELECT * FROM recipe WHERE id = :id")
    suspend fun getRecipeById(id: Long): RecipeEntity?

    @Insert
    suspend fun insertRecipe(recipe: RecipeEntity): Long
}

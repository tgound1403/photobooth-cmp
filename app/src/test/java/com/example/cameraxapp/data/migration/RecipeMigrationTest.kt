package com.example.cameraxapp.data.migration

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RecipeMigrationTest {

    @Test
    fun createTableSqlMatchesRoomGeneratedColumns() {
        assertEquals(
            "CREATE TABLE IF NOT EXISTS `recipe` (" +
                "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`name` TEXT NOT NULL, " +
                "`paramsJson` TEXT NOT NULL, " +
                "`thumbnailPath` TEXT, " +
                "`createdAt` INTEGER NOT NULL)",
            RecipeMigrations.CREATE_RECIPE_TABLE_SQL,
        )
    }

    @Test
    fun createTableSqlIsIdempotent() {
        assertTrue(RecipeMigrations.CREATE_RECIPE_TABLE_SQL.startsWith("CREATE TABLE IF NOT EXISTS"))
    }
}

package com.example.photogether.data.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

object PhotoBoothMigrations {

    /** Adds the EditParams JSON column. '{}' decodes to EditParams() (identity edit). */
    const val ADD_EDIT_PARAMS_COLUMN_SQL =
        "ALTER TABLE photo_booth ADD COLUMN editParams TEXT NOT NULL DEFAULT '{}'"

    val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(ADD_EDIT_PARAMS_COLUMN_SQL)
        }
    }
}

object RecipeMigrations {

    /**
     * Creates the recipe table. Column definitions match what Room generates for RecipeEntity,
     * so Room's schema validation passes on databases that went through this migration.
     */
    const val CREATE_RECIPE_TABLE_SQL =
        "CREATE TABLE IF NOT EXISTS `recipe` (" +
            "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
            "`name` TEXT NOT NULL, " +
            "`paramsJson` TEXT NOT NULL, " +
            "`thumbnailPath` TEXT, " +
            "`createdAt` INTEGER NOT NULL)"

    val MIGRATION_2_3 = object : Migration(2, 3) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(CREATE_RECIPE_TABLE_SQL)
        }
    }
}

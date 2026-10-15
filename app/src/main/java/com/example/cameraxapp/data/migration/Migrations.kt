package com.example.cameraxapp.data.migration

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

package com.example.cameraxapp.data.migration

import com.example.cameraxapp.core.render.EditParams
import org.junit.Assert.assertEquals
import org.junit.Test

class PhotoBoothMigrationTest {

    @Test
    fun addColumnSqlAddsNotNullTextColumnWithIdentityDefault() {
        assertEquals(
            "ALTER TABLE photo_booth ADD COLUMN editParams TEXT NOT NULL DEFAULT '{}'",
            PhotoBoothMigrations.ADD_EDIT_PARAMS_COLUMN_SQL,
        )
    }

    @Test
    fun emptyJsonObjectDecodesToIdentityEditParams() {
        assertEquals(EditParams(), EditParams.fromJson("{}"))
    }
}

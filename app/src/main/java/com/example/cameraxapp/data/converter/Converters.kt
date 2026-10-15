package com.example.cameraxapp.data.converter

import androidx.room.TypeConverter
import com.example.cameraxapp.core.render.EditParams
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.util.Date

class StringListConverter {
    @TypeConverter
    fun fromString(value: String): List<String> {
        val listType = object : TypeToken<List<String>>() {}.type
        return Gson().fromJson(value, listType)
    }

    @TypeConverter
    fun fromList(list: List<String>): String {
        return Gson().toJson(list)
    }
}

class EditParamsConverter {
    @TypeConverter
    fun fromJsonString(value: String): EditParams = EditParams.fromJson(value)

    @TypeConverter
    fun toJsonString(params: EditParams): String = params.toJson()
}

class DateConverter {
    @TypeConverter
    fun fromTimestamp(value: Long?): Date? {
        return value?.let { Date(it) }
    }

    @TypeConverter
    fun dateToTimestamp(date: Date?): Long? {
        return date?.time
    }
} 
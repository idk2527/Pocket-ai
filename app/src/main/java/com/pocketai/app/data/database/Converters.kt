package com.pocketai.app.data.database

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.pocketai.app.data.model.Item

class Converters {
    @TypeConverter
    fun fromItemList(items: List<Item>?): String? {
        if (items == null) {
            return null
        }
        val gson = Gson()
        val type = object : TypeToken<List<Item>>() {}.type
        return gson.toJson(items, type)
    }

    @TypeConverter
    fun toItemList(itemsString: String?): List<Item>? {
        if (itemsString == null) {
            return null
        }
        val gson = Gson()
        val type = object : TypeToken<List<Item>>() {}.type
        return gson.fromJson(itemsString, type)
    }
}

package com.agal.assistant.lifestyle

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import org.json.JSONArray
import org.json.JSONObject

data class BucketItem(
    val id: String = java.util.UUID.randomUUID().toString(),
    val text: String,
    val isCompleted: Boolean = false,
    val easeAnalysis: String = "",
    val timestamp: Long = System.currentTimeMillis()
) {
    fun toJson(): JSONObject {
        return JSONObject().apply {
            put("id", id)
            put("text", text)
            put("isCompleted", isCompleted)
            put("easeAnalysis", easeAnalysis)
            put("timestamp", timestamp)
        }
    }

    companion object {
        fun fromJson(obj: JSONObject): BucketItem {
            return BucketItem(
                id = obj.optString("id", java.util.UUID.randomUUID().toString()),
                text = obj.optString("text", ""),
                isCompleted = obj.optBoolean("isCompleted", false),
                easeAnalysis = obj.optString("easeAnalysis", ""),
                timestamp = obj.optLong("timestamp", System.currentTimeMillis())
            )
        }
    }
}

class BucketListRepository(private val context: Context) {
    companion object {
        private const val PREFS_NAME = "agal_bucket_list_prefs"
        private const val KEY_ITEMS = "bucket_items_json"
    }

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun getItems(): List<BucketItem> {
        val raw = prefs.getString(KEY_ITEMS, null) ?: return emptyList()
        return try {
            val array = JSONArray(raw)
            val list = mutableListOf<BucketItem>()
            for (i in 0 until array.length()) {
                list.add(BucketItem.fromJson(array.getJSONObject(i)))
            }
            // If stored list only contains legacy predefined mock items, clear them
            val isLegacyMock = list.all { item ->
                item.text.contains("Scuba diving", ignoreCase = true) ||
                item.text.contains("Read 12 books", ignoreCase = true) ||
                item.text.contains("guitar chords", ignoreCase = true)
            }
            if (isLegacyMock && list.isNotEmpty()) {
                clearAll()
            } else {
                list
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun clearAll(): List<BucketItem> {
        prefs.edit().remove(KEY_ITEMS).apply()
        return emptyList()
    }

    fun addItem(rawText: String): Pair<BucketItem, List<BucketItem>> {
        val analysis = analyzeEase(rawText)
        val newItem = BucketItem(
            text = rawText.trim().replaceFirstChar { it.uppercase() },
            easeAnalysis = analysis
        )
        val current = getItems().toMutableList()
        current.add(newItem)
        saveItems(current)
        return Pair(newItem, current)
    }

    fun toggleComplete(id: String): List<BucketItem> {
        val current = getItems().map {
            if (it.id == id) it.copy(isCompleted = !it.isCompleted) else it
        }
        saveItems(current)
        return current
    }

    fun deleteItem(id: String): List<BucketItem> {
        val current = getItems().filterNot { it.id == id }
        saveItems(current)
        return current
    }

    private fun saveItems(items: List<BucketItem>) {
        val array = JSONArray()
        items.forEach { array.put(it.toJson()) }
        prefs.edit().putString(KEY_ITEMS, array.toString()).apply()
    }

    private fun analyzeEase(goal: String): String {
        val lower = goal.lowercase()
        return when {
            lower.contains("read") || lower.contains("book") || lower.contains("movie") || lower.contains("cook") || lower.contains("try food") ->
                "Very Easy — You can complete this over a relaxing weekend at home."
            lower.contains("gym") || lower.contains("run") || lower.contains("workout") || lower.contains("diet") || lower.contains("habit") ->
                "Easy with consistency — Dedicate 20 minutes daily for 3 weeks to achieve this."
            lower.contains("guitar") || lower.contains("piano") || lower.contains("code") || lower.contains("swim") || lower.contains("skill") ->
                "Moderate Effort — Practice 30 minutes 4 days a week, achievable within 2 months."
            lower.contains("scuba") || lower.contains("skydiving") || lower.contains("trek") || lower.contains("hike") || lower.contains("camp") ->
                "Fun Adventure — Plan a weekend getaway, book a slot 2 weeks in advance."
            lower.contains("travel") || lower.contains("paris") || lower.contains("japan") || lower.contains("world") || lower.contains("car") || lower.contains("house") ->
                "Major Dream — Break it into monthly savings and plan 3-6 months ahead."
            else ->
                "Achievable — Break this goal into 3 simple milestones and start with step one this week."
        }
    }

    private fun getDefaultItems(): List<BucketItem> {
        return emptyList()
    }
}

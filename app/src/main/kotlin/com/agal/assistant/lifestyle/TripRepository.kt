package com.agal.assistant.lifestyle

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject

data class TripDestination(
    val id: String = java.util.UUID.randomUUID().toString(),
    val destination: String,
    val estimatedExpense: Double,
    val currency: String = "₹",
    val notes: String = "",
    val timestamp: Long = System.currentTimeMillis()
) {
    fun toJson(): JSONObject {
        return JSONObject().apply {
            put("id", id)
            put("destination", destination)
            put("estimatedExpense", estimatedExpense)
            put("currency", currency)
            put("notes", notes)
            put("timestamp", timestamp)
        }
    }

    companion object {
        fun fromJson(obj: JSONObject): TripDestination {
            return TripDestination(
                id = obj.optString("id", java.util.UUID.randomUUID().toString()),
                destination = obj.optString("destination", ""),
                estimatedExpense = obj.optDouble("estimatedExpense", 0.0),
                currency = obj.optString("currency", "₹"),
                notes = obj.optString("notes", ""),
                timestamp = obj.optLong("timestamp", System.currentTimeMillis())
            )
        }
    }
}

class TripRepository(private val context: Context) {
    companion object {
        private const val PREFS_NAME = "agal_trips_prefs"
        private const val KEY_TRIPS = "trips_list_json"
    }

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun getTrips(): List<TripDestination> {
        val raw = prefs.getString(KEY_TRIPS, null) ?: return emptyList()
        return try {
            val array = JSONArray(raw)
            val list = mutableListOf<TripDestination>()
            for (i in 0 until array.length()) {
                list.add(TripDestination.fromJson(array.getJSONObject(i)))
            }
            // If stored list only contains legacy predefined mock items, clear them
            val isLegacyMock = list.all { item ->
                item.destination.contains("Manali", ignoreCase = true) ||
                item.destination.contains("Goa", ignoreCase = true)
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

    fun clearAll(): List<TripDestination> {
        prefs.edit().remove(KEY_TRIPS).apply()
        return emptyList()
    }

    fun addTrip(destination: String, customExpense: Double? = null): Pair<TripDestination, List<TripDestination>> {
        val cleanDest = destination.trim().replaceFirstChar { it.uppercase() }
        val expense = customExpense ?: calculateEstimatedExpense(cleanDest)
        val newTrip = TripDestination(
            destination = cleanDest,
            estimatedExpense = expense,
            notes = "Estimated budget including travel & stay: ₹${expense.toInt()}"
        )
        val current = getTrips().toMutableList()
        current.add(0, newTrip)
        saveTrips(current)
        return Pair(newTrip, current)
    }

    fun deleteTrip(id: String): List<TripDestination> {
        val current = getTrips().filterNot { it.id == id }
        saveTrips(current)
        return current
    }

    private fun saveTrips(trips: List<TripDestination>) {
        val array = JSONArray()
        trips.forEach { array.put(it.toJson()) }
        prefs.edit().putString(KEY_TRIPS, array.toString()).apply()
    }

    fun calculateEstimatedExpense(place: String): Double {
        val lower = place.lowercase()
        return when {
            // International destinations
            lower.contains("paris") || lower.contains("france") || lower.contains("europe") || lower.contains("switzerland") -> 140000.0
            lower.contains("japan") || lower.contains("tokyo") || lower.contains("korea") -> 110000.0
            lower.contains("dubai") || lower.contains("uae") || lower.contains("singapore") || lower.contains("malaysia") || lower.contains("thailand") || lower.contains("bali") -> 45000.0
            lower.contains("usa") || lower.contains("new york") || lower.contains("london") || lower.contains("uk") -> 160000.0

            // Domestic hill stations & resorts (India)
            lower.contains("manali") || lower.contains("shimla") || lower.contains("ladakh") || lower.contains("kashmir") -> 22000.0
            lower.contains("goa") || lower.contains("andaman") || lower.contains("kerala") || lower.contains("munnar") || lower.contains("ooty") || lower.contains("kodaikanal") -> 12000.0
            lower.contains("pondicherry") || lower.contains("mysore") || lower.contains("coorg") || lower.contains("wayanad") -> 7500.0
            lower.contains("varanasi") || lower.contains("jaipur") || lower.contains("agra") || lower.contains("delhi") -> 9500.0
            else -> 8000.0
        }
    }

    private fun getDefaultTrips(): List<TripDestination> {
        return emptyList()
    }
}

package com.agal.assistant.lifestyle

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject

enum class FinanceType {
    EXPENSE,
    INCOME
}

data class FinanceTransaction(
    val id: String = java.util.UUID.randomUUID().toString(),
    val title: String,
    val amount: Double,
    val category: String,
    val type: FinanceType = FinanceType.EXPENSE,
    val timestamp: Long = System.currentTimeMillis()
) {
    fun toJson(): JSONObject {
        return JSONObject().apply {
            put("id", id)
            put("title", title)
            put("amount", amount)
            put("category", category)
            put("type", type.name)
            put("timestamp", timestamp)
        }
    }

    companion object {
        fun fromJson(obj: JSONObject): FinanceTransaction {
            val typeStr = obj.optString("type", FinanceType.EXPENSE.name)
            val type = try {
                FinanceType.valueOf(typeStr)
            } catch (e: Exception) {
                FinanceType.EXPENSE
            }
            return FinanceTransaction(
                id = obj.optString("id", java.util.UUID.randomUUID().toString()),
                title = obj.optString("title", ""),
                amount = obj.optDouble("amount", 0.0),
                category = obj.optString("category", "General"),
                type = type,
                timestamp = obj.optLong("timestamp", System.currentTimeMillis())
            )
        }
    }
}

class FinanceRepository(private val context: Context) {
    companion object {
        private const val PREFS_NAME = "agal_finance_prefs"
        private const val KEY_TRANSACTIONS = "transactions_list_json"
    }

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun getTransactions(): List<FinanceTransaction> {
        val raw = prefs.getString(KEY_TRANSACTIONS, null) ?: return getDefaultTransactions()
        return try {
            val array = JSONArray(raw)
            val list = mutableListOf<FinanceTransaction>()
            for (i in 0 until array.length()) {
                list.add(FinanceTransaction.fromJson(array.getJSONObject(i)))
            }
            list
        } catch (e: Exception) {
            getDefaultTransactions()
        }
    }

    fun addTransaction(title: String, amount: Double, category: String = "General", type: FinanceType = FinanceType.EXPENSE): Pair<FinanceTransaction, List<FinanceTransaction>> {
        val item = FinanceTransaction(
            title = title.trim().replaceFirstChar { it.uppercase() },
            amount = amount,
            category = category,
            type = type
        )
        val current = getTransactions().toMutableList()
        current.add(0, item)
        saveTransactions(current)
        return Pair(item, current)
    }

    fun deleteTransaction(id: String): List<FinanceTransaction> {
        val current = getTransactions().filterNot { it.id == id }
        saveTransactions(current)
        return current
    }

    fun getTotalBalance(): Double {
        val list = getTransactions()
        var total = 0.0
        for (item in list) {
            if (item.type == FinanceType.INCOME) total += item.amount
            else total -= item.amount
        }
        return total
    }

    fun getTotalExpenses(): Double {
        return getTransactions().filter { it.type == FinanceType.EXPENSE }.sumOf { it.amount }
    }

    fun getTotalIncome(): Double {
        return getTransactions().filter { it.type == FinanceType.INCOME }.sumOf { it.amount }
    }

    private fun saveTransactions(list: List<FinanceTransaction>) {
        val array = JSONArray()
        list.forEach { array.put(it.toJson()) }
        prefs.edit().putString(KEY_TRANSACTIONS, array.toString()).apply()
    }

    private fun getDefaultTransactions(): List<FinanceTransaction> {
        return listOf(
            FinanceTransaction(
                title = "Monthly Salary",
                amount = 45000.0,
                category = "Salary",
                type = FinanceType.INCOME
            ),
            FinanceTransaction(
                title = "Groceries & Supermarket",
                amount = 2400.0,
                category = "Food",
                type = FinanceType.EXPENSE
            ),
            FinanceTransaction(
                title = "Fuel & Petrol",
                amount = 1200.0,
                category = "Travel",
                type = FinanceType.EXPENSE
            )
        )
    }
}

package com.agal.assistant.engine.rules

import android.content.Context
import com.agal.assistant.engine.CommandRule
import com.agal.assistant.engine.models.CommandResult
import com.agal.assistant.engine.models.CommandType
import com.agal.assistant.lifestyle.FinanceRepository
import com.agal.assistant.lifestyle.FinanceType

class FinanceCommandRule : CommandRule {
    override val name: String = "Finance Tracking Command"

    override fun matches(input: String): Boolean {
        val lower = input.lowercase().trim()
        return lower.contains("spent") ||
                lower.contains("expense") ||
                lower.contains("finance") ||
                lower.contains("income") ||
                lower.contains("salary") ||
                lower.startsWith("add expense") ||
                lower.startsWith("track expense") ||
                lower.startsWith("how much did i spend") ||
                lower.startsWith("my balance") ||
                lower.startsWith("total expense")
    }

    override suspend fun execute(input: String, context: Context): CommandResult {
        val lower = input.lowercase().trim()
        val repo = FinanceRepository(context)

        // Inquiry: Check total expenses or balance
        if (lower.contains("balance") || lower.contains("total expense") || lower.contains("how much did i spend") || lower.contains("my finance")) {
            val totalSpent = repo.getTotalExpenses()
            val totalIncome = repo.getTotalIncome()
            val balance = repo.getTotalBalance()
            val reply = "Your total expenses are ₹${totalSpent.toInt()}. Total income is ₹${totalIncome.toInt()}, leaving a net balance of ₹${balance.toInt()}."
            return CommandResult(
                type = CommandType.FINANCE_TRACKER,
                rawInput = input,
                spokenReply = reply,
                displayTitle = "Finance Summary",
                displayDescription = "Balance: ₹${balance.toInt()} | Expenses: ₹${totalSpent.toInt()}",
                isSuccess = true
            )
        }

        // Add expense or income transaction
        // Example: "spent 500 on groceries", "add expense 250 for petrol", "received 5000 salary"
        val isIncome = lower.contains("received") || lower.contains("income") || lower.contains("got salary") || lower.contains("earned")
        val amount = extractAmount(lower)

        if (amount == null || amount <= 0) {
            return CommandResult(
                type = CommandType.FINANCE_TRACKER,
                rawInput = input,
                spokenReply = "Please specify the amount you want to track, for example: 'spent 500 on food' or 'my balance'.",
                displayTitle = "Finance Tracker",
                displayDescription = "Amount not recognized",
                isSuccess = false
            )
        }

        val title = extractTitle(input, isIncome)
        val category = guessCategory(title)
        val type = if (isIncome) FinanceType.INCOME else FinanceType.EXPENSE

        val (tx, allTx) = repo.addTransaction(title, amount, category, type)
        val actionWord = if (isIncome) "income of" else "expense of"
        val reply = "Recorded $actionWord ₹${tx.amount.toInt()} for ${tx.title}. Total expenses are now ₹${repo.getTotalExpenses().toInt()}."

        return CommandResult(
            type = CommandType.FINANCE_TRACKER,
            rawInput = input,
            spokenReply = reply,
            displayTitle = "Finance: ₹${tx.amount.toInt()} (${tx.category})",
            displayDescription = "${tx.title} • Recorded successfully (${allTx.size} entries)",
            isSuccess = true
        )
    }

    private fun extractAmount(text: String): Double? {
        val regex = Regex("(?:rs\\.?|inr|₹|spent|paid|for|of)?\\s*(\\d+(?:\\.\\d+)?)\\s*(?:rs|rupees|bucks)?", RegexOption.IGNORE_CASE)
        val match = regex.find(text)
        return match?.groups?.get(1)?.value?.toDoubleOrNull()
    }

    private fun extractTitle(input: String, isIncome: Boolean): String {
        var clean = input
            .replace(Regex("^(?:hey|hello|hi)?\\s*agal[,\\s]*", RegexOption.IGNORE_CASE), "")
            .replace(Regex("(?:spent|paid|added|received|earned)\\s*\\d+(?:\\.\\d+)?\\s*(?:rs|rupees)?\\s*(?:on|for|from)?", RegexOption.IGNORE_CASE), "")
            .replace(Regex("(?:add expense|track expense)\\s*\\d*(?:\\.\\d+)?\\s*", RegexOption.IGNORE_CASE), "")
            .trim()
        if (clean.isBlank()) {
            clean = if (isIncome) "General Income" else "General Expense"
        }
        return clean
    }

    private fun guessCategory(title: String): String {
        val lower = title.lowercase()
        return when {
            lower.contains("food") || lower.contains("grocery") || lower.contains("groceries") || lower.contains("snack") || lower.contains("tea") || lower.contains("coffee") || lower.contains("dinner") || lower.contains("lunch") -> "Food"
            lower.contains("fuel") || lower.contains("petrol") || lower.contains("diesel") || lower.contains("cab") || lower.contains("uber") || lower.contains("ola") || lower.contains("bus") || lower.contains("auto") -> "Transport"
            lower.contains("bill") || lower.contains("recharge") || lower.contains("electricity") || lower.contains("wifi") || lower.contains("rent") -> "Bills"
            lower.contains("shopping") || lower.contains("cloth") || lower.contains("dress") || lower.contains("shoe") || lower.contains("amazon") -> "Shopping"
            lower.contains("salary") || lower.contains("freelance") || lower.contains("client") -> "Income"
            else -> "Personal"
        }
    }
}

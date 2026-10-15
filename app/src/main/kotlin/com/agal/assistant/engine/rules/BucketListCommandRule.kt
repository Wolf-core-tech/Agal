package com.agal.assistant.engine.rules

import android.content.Context
import com.agal.assistant.engine.CommandRule
import com.agal.assistant.engine.models.CommandResult
import com.agal.assistant.engine.models.CommandType
import com.agal.assistant.lifestyle.BucketListRepository

class BucketListCommandRule : CommandRule {
    override val name: String = "Bucket List Command"

    override fun matches(input: String): Boolean {
        val lower = input.lowercase().trim()
        return lower.contains("bucket list") ||
                lower.startsWith("add to bucket") ||
                lower.startsWith("add this to bucket")
    }

    override suspend fun execute(input: String, context: Context): CommandResult {
        val lower = input.lowercase().trim()
        val repo = BucketListRepository(context)

        // If user asks to view or read bucket list
        if (lower.startsWith("what is in my bucket list") ||
            lower.startsWith("show bucket list") ||
            lower.startsWith("read bucket list") ||
            lower == "bucket list") {
            val items = repo.getItems()
            if (items.isEmpty()) {
                return CommandResult(
                    type = CommandType.BUCKET_LIST,
                    rawInput = input,
                    spokenReply = "Your bucket list is currently empty. Tell me what you'd like to add!",
                    displayTitle = "Bucket List",
                    displayDescription = "0 items in your list"
                )
            }
            val formattedSpeech = items.mapIndexed { index, item ->
                "${index + 1}. ${item.text}"
            }.joinToString(". ")
            return CommandResult(
                type = CommandType.BUCKET_LIST,
                rawInput = input,
                spokenReply = "Here is your bucket list: $formattedSpeech",
                displayTitle = "Bucket List (${items.size})",
                displayDescription = items.mapIndexed { index, it -> "${index + 1}. ${it.text}" }.joinToString("\n")
            )
        }

        // Add to bucket list: e.g. "add scuba diving in goa to bucket list" or "add this skydiving to my bucket list"
        val cleanTask = extractGoal(input)
        if (cleanTask.isBlank()) {
            return CommandResult(
                type = CommandType.BUCKET_LIST,
                rawInput = input,
                spokenReply = "What would you like to add to your bucket list?",
                displayTitle = "Bucket List",
                displayDescription = "Item text missing",
                isSuccess = false
            )
        }

        val (newItem, allItems) = repo.addItem(cleanTask)
        val itemNumber = allItems.size
        val reply = "Added #$itemNumber to your bucket list: ${newItem.text}. Ease analysis: ${newItem.easeAnalysis}"

        return CommandResult(
            type = CommandType.BUCKET_LIST,
            rawInput = input,
            spokenReply = reply,
            displayTitle = "Bucket List (#$itemNumber)",
            displayDescription = "${newItem.text}\n💡 ${newItem.easeAnalysis}",
            isSuccess = true
        )
    }

    private fun extractGoal(input: String): String {
        return input
            .replace(Regex("^(?:hey|hello|hi)?\\s*agal[,\\s]*", RegexOption.IGNORE_CASE), "")
            .replace(Regex("^(?:please\\s+)?(?:add\\s+this|add)\\s+", RegexOption.IGNORE_CASE), "")
            .replace(Regex("\\s+(?:to\\s+my\\s+bucket\\s+list|to\\s+bucket\\s+list|in\\s+bucket\\s+list)$", RegexOption.IGNORE_CASE), "")
            .replace(Regex("^(?:to\\s+bucket\\s+list\\s+add|in\\s+bucket\\s+list\\s+add)\\s+", RegexOption.IGNORE_CASE), "")
            .trim()
    }
}

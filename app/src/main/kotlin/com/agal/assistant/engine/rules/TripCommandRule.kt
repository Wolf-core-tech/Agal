package com.agal.assistant.engine.rules

import android.content.Context
import com.agal.assistant.engine.CommandRule
import com.agal.assistant.engine.models.CommandResult
import com.agal.assistant.engine.models.CommandType
import com.agal.assistant.lifestyle.TripRepository

class TripCommandRule : CommandRule {
    override val name: String = "Trip Destination Command"

    override fun matches(input: String): Boolean {
        val lower = input.lowercase().trim()
        return lower.contains("to trip") ||
                lower.startsWith("add to trip") ||
                lower.startsWith("add this to trip") ||
                lower.contains("trip destination") ||
                lower.contains("plan trip to")
    }

    override suspend fun execute(input: String, context: Context): CommandResult {
        val lower = input.lowercase().trim()
        val repo = TripRepository(context)

        // Show trips
        if (lower.startsWith("show trips") || lower.startsWith("my trips") || lower == "trip destinations") {
            val trips = repo.getTrips()
            if (trips.isEmpty()) {
                return CommandResult(
                    type = CommandType.TRIP_PLANNER,
                    rawInput = input,
                    spokenReply = "You have no saved trip destinations yet. Tell me where you'd like to travel!",
                    displayTitle = "Trip Destinations",
                    displayDescription = "0 trips planned"
                )
            }
            val reply = trips.joinToString(", ") { "${it.destination} (approx ₹${it.estimatedExpense.toInt()})" }
            return CommandResult(
                type = CommandType.TRIP_PLANNER,
                rawInput = input,
                spokenReply = "Here are your saved trips: $reply",
                displayTitle = "Trip Destinations (${trips.size})",
                displayDescription = trips.joinToString("\n") { "• ${it.destination} - ₹${it.estimatedExpense.toInt()}" }
            )
        }

        val destination = extractDestination(input)
        if (destination.isBlank()) {
            return CommandResult(
                type = CommandType.TRIP_PLANNER,
                rawInput = input,
                spokenReply = "Which destination would you like me to add to your trips?",
                displayTitle = "Trip Planner",
                displayDescription = "Destination name missing",
                isSuccess = false
            )
        }

        val (newTrip, allTrips) = repo.addTrip(destination)
        val reply = "Saved ${newTrip.destination} to your trip destinations! Estimated travel expense is ₹${newTrip.estimatedExpense.toInt()}."

        return CommandResult(
            type = CommandType.TRIP_PLANNER,
            rawInput = input,
            spokenReply = reply,
            displayTitle = "Trip: ${newTrip.destination}",
            displayDescription = "Estimated Expense: ₹${newTrip.estimatedExpense.toInt()} (Total trips: ${allTrips.size})",
            isSuccess = true
        )
    }

    private fun extractDestination(input: String): String {
        return input
            .replace(Regex("^(?:hey|hello|hi)?\\s*agal[,\\s]*", RegexOption.IGNORE_CASE), "")
            .replace(Regex("^(?:please\\s+)?(?:add\\s+this|add)\\s+", RegexOption.IGNORE_CASE), "")
            .replace(Regex("\\s+(?:to\\s+my\\s+trip|to\\s+trip|in\\s+trip|to\\s+trip\\s+destination)$", RegexOption.IGNORE_CASE), "")
            .replace(Regex("^(?:plan\\s+trip\\s+to|trip\\s+to)\\s+", RegexOption.IGNORE_CASE), "")
            .trim()
    }
}

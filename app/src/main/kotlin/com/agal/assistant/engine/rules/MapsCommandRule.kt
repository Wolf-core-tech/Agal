package com.agal.assistant.engine.rules

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.agal.assistant.engine.CommandRule
import com.agal.assistant.engine.models.CommandResult
import com.agal.assistant.engine.models.CommandType

/**
 * MapsCommandRule — handles:
 *  - "open map" / "open google maps"             → launches Google Maps
 *  - "show route from X to Y"                    → opens directions from X to Y
 *  - "navigate from X to Y"                      → same
 *  - "directions from X to Y"                    → same
 *  - "navigate to Y"                             → directions to Y (current location as origin)
 *  - "take me to Y"                              → directions to Y
 *  - "how to go to Y"                            → directions to Y
 *  - "where is Y" / "find Y on map"              → search for Y on map
 */
class MapsCommandRule : CommandRule {
    override val name: String = "Maps & Navigation Command"

    // Regex patterns for "from X to Y" extraction
    private val fromToRegex = Regex(
        """(?:from|starting from|origin)\s+(.+?)\s+(?:to|towards|toward|destination)\s+(.+)""",
        RegexOption.IGNORE_CASE
    )

    // Regex for "navigate to Y" / "directions to Y" / "take me to Y"
    private val toOnlyRegex = Regex(
        """(?:navigate to|go to|take me to|directions to|route to|how to go to|way to|get to)\s+(.+)""",
        RegexOption.IGNORE_CASE
    )

    // Regex for "where is Y" / "find Y" / "search Y on map"
    private val searchRegex = Regex(
        """(?:where is|find|search for|show me|locate|look up)\s+(.+?)(?:\s+on\s+(?:the\s+)?map)?$""",
        RegexOption.IGNORE_CASE
    )

    override fun matches(input: String): Boolean {
        val lower = input.lowercase().trim()
        return lower.contains("map") ||
                lower.contains("maps") ||
                lower.contains("google map") ||
                lower.contains("navigate") ||
                lower.contains("navigation") ||
                lower.contains("route") ||
                lower.contains("direction") ||
                lower.contains("take me to") ||
                lower.contains("how to go to") ||
                lower.contains("get me to") ||
                lower.contains("show the way") ||
                lower.contains("find location") ||
                lower.contains("locate") ||
                lower.contains("where is")
    }

    override suspend fun execute(input: String, context: Context): CommandResult {
        val lower = input.lowercase().trim()

        // ─── 1. Route from X to Y ─────────────────────────────────────────────
        val fromToMatch = fromToRegex.find(lower)
        if (fromToMatch != null) {
            val origin = fromToMatch.groupValues[1].trim().beautify()
            val destination = fromToMatch.groupValues[2].trim().beautify()

            if (origin.isNotBlank() && destination.isNotBlank()) {
                return openDirections(context, input, origin, destination)
            }
        }

        // ─── 2. Navigate/Directions to Y only (current location → Y) ─────────
        val toOnlyMatch = toOnlyRegex.find(lower)
        if (toOnlyMatch != null) {
            val destination = toOnlyMatch.groupValues[1].trim().beautify()
            if (destination.isNotBlank()) {
                return openDirectionsToDestination(context, input, destination)
            }
        }

        // ─── 3. Search / "where is" ───────────────────────────────────────────
        val searchMatch = searchRegex.find(lower)
        if (searchMatch != null) {
            val query = searchMatch.groupValues[1].trim().beautify()
            if (query.isNotBlank()) {
                return searchOnMap(context, input, query)
            }
        }

        // ─── 4. Bare "open map" / "open google maps" ─────────────────────────
        return openGoogleMaps(context, input)
    }

    // ─── Open Google Maps (no destination) ────────────────────────────────────
    private fun openGoogleMaps(context: Context, rawInput: String): CommandResult {
        return try {
            val pm = context.packageManager

            // Try native Google Maps app first
            val nativeIntent = pm.getLaunchIntentForPackage("com.google.android.apps.maps")
            if (nativeIntent != null) {
                nativeIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(nativeIntent)
            } else {
                // Fallback: open maps.google.com in browser
                val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://maps.google.com")).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(webIntent)
            }
            CommandResult(
                type = CommandType.OPEN_MAPS,
                rawInput = rawInput,
                spokenReply = "Opening Google Maps.",
                displayTitle = "Google Maps",
                displayDescription = "Maps application launched",
                isSuccess = true
            )
        } catch (e: Exception) {
            CommandResult(
                type = CommandType.OPEN_MAPS,
                rawInput = rawInput,
                spokenReply = "Could not open Maps: ${e.localizedMessage}",
                displayTitle = "Maps Error",
                displayDescription = e.localizedMessage ?: "Unknown error",
                isSuccess = false
            )
        }
    }

    // ─── Directions from origin → destination ─────────────────────────────────
    private fun openDirections(
        context: Context,
        rawInput: String,
        origin: String,
        destination: String
    ): CommandResult {
        return try {
            // Google Maps Directions URI:
            // https://maps.google.com/maps?saddr=ORIGIN&daddr=DESTINATION&dirflg=d
            val encodedOrigin = Uri.encode(origin)
            val encodedDest = Uri.encode(destination)

            // Try native Google Maps geo intent first (works inside the app)
            val geoUri = Uri.parse("google.navigation:q=${encodedDest}&origin=${encodedOrigin}")
            val nativeIntent = Intent(Intent.ACTION_VIEW, geoUri).apply {
                setPackage("com.google.android.apps.maps")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            val pm = context.packageManager
            if (nativeIntent.resolveActivity(pm) != null) {
                context.startActivity(nativeIntent)
            } else {
                // Fallback to web URL with directions
                val webUrl = "https://maps.google.com/maps?saddr=${encodedOrigin}&daddr=${encodedDest}&dirflg=d"
                val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse(webUrl)).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(webIntent)
            }

            CommandResult(
                type = CommandType.NAVIGATE_MAPS,
                rawInput = rawInput,
                spokenReply = "Showing route from $origin to $destination.",
                displayTitle = "Navigation",
                displayDescription = "Route: $origin → $destination",
                isSuccess = true
            )
        } catch (e: Exception) {
            CommandResult(
                type = CommandType.NAVIGATE_MAPS,
                rawInput = rawInput,
                spokenReply = "Could not open directions: ${e.localizedMessage}",
                displayTitle = "Navigation Error",
                displayDescription = e.localizedMessage ?: "Unknown error",
                isSuccess = false
            )
        }
    }

    // ─── Directions from current location → destination ───────────────────────
    private fun openDirectionsToDestination(
        context: Context,
        rawInput: String,
        destination: String
    ): CommandResult {
        return try {
            val encodedDest = Uri.encode(destination)

            // google.navigation:q=DESTINATION  starts turn-by-turn from current location
            val geoUri = Uri.parse("google.navigation:q=${encodedDest}")
            val nativeIntent = Intent(Intent.ACTION_VIEW, geoUri).apply {
                setPackage("com.google.android.apps.maps")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            val pm = context.packageManager
            if (nativeIntent.resolveActivity(pm) != null) {
                context.startActivity(nativeIntent)
            } else {
                // Fallback web URL: daddr only (Google Maps uses current location as saddr)
                val webUrl = "https://maps.google.com/maps?daddr=${encodedDest}&dirflg=d"
                val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse(webUrl)).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(webIntent)
            }

            CommandResult(
                type = CommandType.NAVIGATE_MAPS,
                rawInput = rawInput,
                spokenReply = "Navigating to $destination.",
                displayTitle = "Navigation",
                displayDescription = "Navigating → $destination",
                isSuccess = true
            )
        } catch (e: Exception) {
            CommandResult(
                type = CommandType.NAVIGATE_MAPS,
                rawInput = rawInput,
                spokenReply = "Could not open navigation: ${e.localizedMessage}",
                displayTitle = "Navigation Error",
                displayDescription = e.localizedMessage ?: "Unknown error",
                isSuccess = false
            )
        }
    }

    // ─── Search a location on Maps ────────────────────────────────────────────
    private fun searchOnMap(
        context: Context,
        rawInput: String,
        query: String
    ): CommandResult {
        return try {
            val encodedQuery = Uri.encode(query)

            // geo:0,0?q=QUERY  performs a map search
            val geoUri = Uri.parse("geo:0,0?q=${encodedQuery}")
            val nativeIntent = Intent(Intent.ACTION_VIEW, geoUri).apply {
                setPackage("com.google.android.apps.maps")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            val pm = context.packageManager
            if (nativeIntent.resolveActivity(pm) != null) {
                context.startActivity(nativeIntent)
            } else {
                val webUrl = "https://maps.google.com/maps?q=${encodedQuery}"
                val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse(webUrl)).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(webIntent)
            }

            CommandResult(
                type = CommandType.OPEN_MAPS,
                rawInput = rawInput,
                spokenReply = "Searching for $query on the map.",
                displayTitle = "Maps Search",
                displayDescription = "Searching: $query",
                isSuccess = true
            )
        } catch (e: Exception) {
            CommandResult(
                type = CommandType.OPEN_MAPS,
                rawInput = rawInput,
                spokenReply = "Could not search on map: ${e.localizedMessage}",
                displayTitle = "Maps Error",
                displayDescription = e.localizedMessage ?: "Unknown error",
                isSuccess = false
            )
        }
    }

    /**
     * Cleans up extracted location text:
     * - Removes trailing noise words ("please", "now", "quickly")
     * - Capitalises first letter of each word for nicer TTS reading
     */
    private fun String.beautify(): String {
        val noiseWords = setOf("please", "now", "quickly", "fast", "immediately")
        return this.split(" ")
            .filter { it.lowercase() !in noiseWords }
            .joinToString(" ") { word ->
                word.replaceFirstChar { it.uppercase() }
            }
            .trim()
    }
}

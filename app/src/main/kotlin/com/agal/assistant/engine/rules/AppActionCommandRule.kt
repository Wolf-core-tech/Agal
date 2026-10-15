package com.agal.assistant.engine.rules

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.AlarmClock
import android.provider.MediaStore
import android.util.Log
import com.agal.assistant.engine.CommandRule
import com.agal.assistant.engine.models.CommandResult
import com.agal.assistant.engine.models.CommandType
import java.net.URLEncoder

/**
 * Universal App Opener & Action Dispatcher Rule.
 *
 * Handles voice commands of patterns:
 * - "open [app]" (e.g., "open calculator", "open instagram", "open whatsapp", "open settings")
 * - "open [app] and [action / search / message]"
 *   (e.g., "open youtube and search chill lofi",
 *          "open whatsapp and send message to rahul",
 *          "open spotify and play coldplay",
 *          "open chrome and search best coffee near me",
 *          "open google and find pizza recipe",
 *          "open camera and take selfie")
 * - "search [something] on [app]" / "play [something] on [app]"
 */
class AppActionCommandRule : CommandRule {
    override val name: String = "App Action Command Rule"

    companion object {
        private const val TAG = "AppActionCommandRule"

        // Common package mappings for fast and reliable lookup
        private val WELL_KNOWN_APPS = mapOf(
            "whatsapp" to listOf("com.whatsapp", "com.whatsapp.w4b"),
            "instagram" to listOf("com.instagram.android"),
            "telegram" to listOf("org.telegram.messenger", "org.thunderdog.challegram"),
            "youtube" to listOf("com.google.android.youtube"),
            "spotify" to listOf("com.spotify.music"),
            "chrome" to listOf("com.android.chrome"),
            "browser" to listOf("com.android.chrome", "org.mozilla.firefox"),
            "firefox" to listOf("org.mozilla.firefox"),
            "maps" to listOf("com.google.android.apps.maps"),
            "google maps" to listOf("com.google.android.apps.maps"),
            "gmail" to listOf("com.google.android.gm"),
            "mail" to listOf("com.google.android.gm"),
            "calculator" to listOf("com.google.android.calculator", "com.sec.android.app.popupcalculator", "com.miui.calculator"),
            "calendar" to listOf("com.google.android.calendar", "com.samsung.android.calendar"),
            "clock" to listOf("com.google.android.deskclock", "com.sec.android.app.clockpackage"),
            "camera" to listOf("com.google.android.GoogleCamera", "com.sec.android.app.camera"),
            "gallery" to listOf("com.google.android.apps.photos", "com.sec.android.gallery3d"),
            "photos" to listOf("com.google.android.apps.photos"),
            "play store" to listOf("com.android.vending"),
            "app store" to listOf("com.android.vending"),
            "store" to listOf("com.android.vending"),
            "files" to listOf("com.google.android.apps.nbu.files", "com.sec.android.app.myfiles"),
            "facebook" to listOf("com.facebook.katana", "com.facebook.lite"),
            "twitter" to listOf("com.twitter.android"),
            "x" to listOf("com.twitter.android"),
            "linkedin" to listOf("com.linkedin.android"),
            "netflix" to listOf("com.netflix.mediaclient"),
            "amazon" to listOf("in.amazon.mShop.android.shopping", "com.amazon.mShop.android.shopping"),
            "flipkart" to listOf("com.flipkart.android"),
            "swiggy" to listOf("in.swiggy.android"),
            "zomato" to listOf("com.application.zomato"),
            "uber" to listOf("com.ubercab"),
            "ola" to listOf("com.olacabs.customer"),
            "paytm" to listOf("net.one97.paytm"),
            "phonepe" to listOf("com.phonepe.app"),
            "gpay" to listOf("com.google.android.apps.nbu.paisa.user"),
            "google pay" to listOf("com.google.android.apps.nbu.paisa.user")
        )
    }

    override fun matches(input: String): Boolean {
        val lower = input.lowercase().trim()
        if (lower.isEmpty()) return false

        // Match "open [app] ..."
        if (lower.startsWith("open ") || lower.startsWith("launch ") || lower.startsWith("start ")) {
            return true
        }

        // Match "search [query] on [app]" or "play [query] on [app]"
        if ((lower.startsWith("search ") || lower.startsWith("find ") || lower.startsWith("play ")) &&
            (lower.contains(" on ") || lower.contains(" in "))) {
            return true
        }

        return false
    }

    override suspend fun execute(input: String, context: Context): CommandResult {
        val lower = input.lowercase().trim()
        val pm = context.packageManager

        // Parse App Name and Action / Task
        val (appName, actionTask) = parseAppAndAction(lower)

        if (appName.isBlank()) {
            return CommandResult(
                type = CommandType.OPEN_APP,
                rawInput = input,
                spokenReply = "Which app would you like me to open?",
                displayTitle = "Open App",
                displayDescription = "App name not specified",
                isSuccess = false
            )
        }

        // 1. Resolve package
        val targetPackage = findPackageForApp(appName, pm)

        // 2. Execute app + action
        return if (!actionTask.isNullOrBlank()) {
            handleAppWithAction(context, pm, appName, targetPackage, actionTask, input)
        } else {
            handleOpenAppDirectly(context, pm, appName, targetPackage, input)
        }
    }

    private fun parseAppAndAction(input: String): Pair<String, String?> {
        // Pattern 1: "open <app> and <action>" / "launch <app> and <action>"
        val andRegex = Regex("^(?:open|launch|start)\\s+([a-zA-Z0-9\\s]+?)\\s+(?:and|to|then|do)\\s+(.+)$", RegexOption.IGNORE_CASE)
        val andMatch = andRegex.find(input)
        if (andMatch != null) {
            val app = andMatch.groups[1]?.value?.trim().orEmpty()
            val action = andMatch.groups[2]?.value?.trim().orEmpty()
            return Pair(app, action)
        }

        // Pattern 2: "search <query> on/in <app>" / "play <query> on/in <app>"
        val searchOnRegex = Regex("^(?:search|find|play|listen to)\\s+(.+?)\\s+(?:on|in)\\s+([a-zA-Z0-9\\s]+)$", RegexOption.IGNORE_CASE)
        val searchOnMatch = searchOnRegex.find(input)
        if (searchOnMatch != null) {
            val query = searchOnMatch.groups[1]?.value?.trim().orEmpty()
            val app = searchOnMatch.groups[2]?.value?.trim().orEmpty()
            return Pair(app, "search $query")
        }

        // Pattern 3: "open <app>" / "launch <app>"
        val openRegex = Regex("^(?:open|launch|start)\\s+(.+)$", RegexOption.IGNORE_CASE)
        val openMatch = openRegex.find(input)
        if (openMatch != null) {
            val app = openMatch.groups[1]?.value?.trim().orEmpty()
            return Pair(app, null)
        }

        return Pair(input, null)
    }

    private fun findPackageForApp(appName: String, pm: PackageManager): String? {
        val cleanName = appName.lowercase().trim()

        // 1. Check well-known map
        WELL_KNOWN_APPS[cleanName]?.forEach { pkg ->
            if (isPackageInstalled(pkg, pm)) return pkg
        }

        // Also check if any key in WELL_KNOWN_APPS is a substring
        for ((key, pkgList) in WELL_KNOWN_APPS) {
            if (cleanName.contains(key) || key.contains(cleanName)) {
                pkgList.forEach { pkg ->
                    if (isPackageInstalled(pkg, pm)) return pkg
                }
            }
        }

        // 2. Scan installed launchable applications
        try {
            val launchIntent = Intent(Intent.ACTION_MAIN, null).apply {
                addCategory(Intent.CATEGORY_LAUNCHER)
            }
            val resolveInfos = pm.queryIntentActivities(launchIntent, 0)
            
            // Exact label match
            for (info in resolveInfos) {
                val label = info.loadLabel(pm).toString().lowercase().trim()
                if (label == cleanName) {
                    return info.activityInfo.packageName
                }
            }

            // Starts with match
            for (info in resolveInfos) {
                val label = info.loadLabel(pm).toString().lowercase().trim()
                if (label.startsWith(cleanName) || cleanName.startsWith(label)) {
                    return info.activityInfo.packageName
                }
            }

            // Substring match
            for (info in resolveInfos) {
                val label = info.loadLabel(pm).toString().lowercase().trim()
                if (label.contains(cleanName) || cleanName.contains(label)) {
                    return info.activityInfo.packageName
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error resolving installed applications: ${e.localizedMessage}")
        }

        return null
    }

    private fun isPackageInstalled(packageName: String, pm: PackageManager): Boolean {
        return try {
            pm.getPackageInfo(packageName, 0)
            true
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }
    }

    private fun handleOpenAppDirectly(
        context: Context,
        pm: PackageManager,
        appName: String,
        targetPackage: String?,
        rawInput: String
    ): CommandResult {
        return try {
            if (targetPackage != null) {
                val launchIntent = pm.getLaunchIntentForPackage(targetPackage)?.apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                if (launchIntent != null) {
                    context.startActivity(launchIntent)
                    val label = getAppLabel(pm, targetPackage) ?: appName.replaceFirstChar { it.uppercase() }
                    return CommandResult(
                        type = CommandType.OPEN_APP,
                        rawInput = rawInput,
                        spokenReply = "Opening $label.",
                        displayTitle = label,
                        displayDescription = "App launched successfully",
                        isSuccess = true
                    )
                }
            }

            // If not found installed, fall back to web search or Play Store
            val playStoreIntent = Intent(Intent.ACTION_VIEW, Uri.parse("market://search?q=${Uri.encode(appName)}")).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            if (playStoreIntent.resolveActivity(pm) != null) {
                context.startActivity(playStoreIntent)
                return CommandResult(
                    type = CommandType.OPEN_APP,
                    rawInput = rawInput,
                    spokenReply = "Could not find $appName installed. Opening Play Store to install it.",
                    displayTitle = appName.replaceFirstChar { it.uppercase() },
                    displayDescription = "App not installed — Opened Play Store",
                    isSuccess = true
                )
            }

            CommandResult(
                type = CommandType.OPEN_APP,
                rawInput = rawInput,
                spokenReply = "Sorry, I couldn't find an app named $appName on your device.",
                displayTitle = "App Not Found",
                displayDescription = "No installed application matches '$appName'",
                isSuccess = false
            )
        } catch (e: Exception) {
            CommandResult(
                type = CommandType.OPEN_APP,
                rawInput = rawInput,
                spokenReply = "Error opening $appName: ${e.localizedMessage}",
                displayTitle = "Open App Error",
                displayDescription = e.localizedMessage ?: "Unknown error",
                isSuccess = false
            )
        }
    }

    private fun handleAppWithAction(
        context: Context,
        pm: PackageManager,
        appName: String,
        targetPackage: String?,
        actionTask: String,
        rawInput: String
    ): CommandResult {
        val lowerAction = actionTask.lowercase().trim()
        val appLower = appName.lowercase().trim()

        try {
            // 1. Specific App Deep-Link Actions:
            
            // --- YouTube: search or play ---
            if (appLower.contains("youtube") || targetPackage == "com.google.android.youtube") {
                val query = extractQuery(lowerAction, listOf("search", "search for", "play", "play on", "watch", "find"))
                val uri = if (query.isNotBlank()) {
                    Uri.parse("vnd.youtube://results?q=${Uri.encode(query)}")
                } else {
                    Uri.parse("https://www.youtube.com")
                }
                val intent = Intent(Intent.ACTION_VIEW, uri).apply {
                    setPackage("com.google.android.youtube")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                if (intent.resolveActivity(pm) != null) {
                    context.startActivity(intent)
                } else {
                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.youtube.com/results?search_query=${Uri.encode(query)}")).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    })
                }
                return CommandResult(
                    type = CommandType.OPEN_APP,
                    rawInput = rawInput,
                    spokenReply = "Opening YouTube and searching for $query.",
                    displayTitle = "YouTube Search",
                    displayDescription = "Query: $query",
                    isSuccess = true
                )
            }

            // --- WhatsApp: send message or chat ---
            if (appLower.contains("whatsapp") || targetPackage == "com.whatsapp") {
                // Examples: "send message to rahul hello", "message mom where are you"
                val messageText = extractWhatsAppMessage(lowerAction)
                val intent = if (messageText.isNotBlank()) {
                    Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, messageText)
                        setPackage("com.whatsapp")
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                } else {
                    pm.getLaunchIntentForPackage("com.whatsapp")?.apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                }

                if (intent != null && intent.resolveActivity(pm) != null) {
                    context.startActivity(intent)
                    return CommandResult(
                        type = CommandType.OPEN_APP,
                        rawInput = rawInput,
                        spokenReply = if (messageText.isNotBlank()) "Opening WhatsApp to send message: $messageText" else "Opening WhatsApp.",
                        displayTitle = "WhatsApp",
                        displayDescription = if (messageText.isNotBlank()) "Message: $messageText" else "Opened WhatsApp",
                        isSuccess = true
                    )
                }
            }

            // --- Spotify / Music players: play song/artist ---
            if (appLower.contains("spotify") || appLower.contains("music") || targetPackage == "com.spotify.music") {
                val query = extractQuery(lowerAction, listOf("play", "search", "listen to"))
                val intent = Intent(MediaStore.INTENT_ACTION_MEDIA_PLAY_FROM_SEARCH).apply {
                    putExtra(MediaStore.EXTRA_MEDIA_FOCUS, MediaStore.Audio.Artists.ENTRY_CONTENT_TYPE)
                    putExtra(android.app.SearchManager.QUERY, query)
                    if (targetPackage != null) setPackage(targetPackage)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                if (intent.resolveActivity(pm) != null) {
                    context.startActivity(intent)
                } else if (targetPackage != null) {
                    pm.getLaunchIntentForPackage(targetPackage)?.apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        context.startActivity(this)
                    }
                }
                return CommandResult(
                    type = CommandType.OPEN_APP,
                    rawInput = rawInput,
                    spokenReply = "Opening $appName and playing $query.",
                    displayTitle = appName.replaceFirstChar { it.uppercase() },
                    displayDescription = "Playing: $query",
                    isSuccess = true
                )
            }

            // --- Chrome / Browser / Google: search query ---
            if (appLower.contains("chrome") || appLower.contains("browser") || appLower.contains("google")) {
                val query = extractQuery(lowerAction, listOf("search", "search for", "find", "look up", "google", "browse"))
                val searchUrl = "https://www.google.com/search?q=${Uri.encode(query)}"
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(searchUrl)).apply {
                    if (targetPackage != null) setPackage(targetPackage)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
                return CommandResult(
                    type = CommandType.OPEN_APP,
                    rawInput = rawInput,
                    spokenReply = "Searching Google for $query.",
                    displayTitle = "Web Search",
                    displayDescription = "Query: $query",
                    isSuccess = true
                )
            }

            // --- Clock / Alarm: set alarm or timer ---
            if (appLower.contains("clock") || appLower.contains("alarm") || lowerAction.contains("alarm") || lowerAction.contains("timer")) {
                val intent = Intent(AlarmClock.ACTION_SHOW_ALARMS).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
                return CommandResult(
                    type = CommandType.OPEN_APP,
                    rawInput = rawInput,
                    spokenReply = "Opening Clock for your alarm.",
                    displayTitle = "Clock",
                    displayDescription = "Opened Clock & Alarms",
                    isSuccess = true
                )
            }

            // --- Camera: take photo / video ---
            if (appLower.contains("camera")) {
                val isVideo = lowerAction.contains("video") || lowerAction.contains("record")
                val intent = if (isVideo) {
                    Intent(MediaStore.ACTION_VIDEO_CAPTURE)
                } else {
                    Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA)
                }.apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
                return CommandResult(
                    type = CommandType.OPEN_APP,
                    rawInput = rawInput,
                    spokenReply = if (isVideo) "Opening Camera for video recording." else "Opening Camera to take a photo.",
                    displayTitle = "Camera",
                    displayDescription = if (isVideo) "Video Capture mode" else "Still Photo mode",
                    isSuccess = true
                )
            }

            // --- General Fallback: Launch the app, with WEB search or implicit query if available ---
            if (targetPackage != null) {
                val launchIntent = pm.getLaunchIntentForPackage(targetPackage)?.apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                if (launchIntent != null) {
                    context.startActivity(launchIntent)
                    val label = getAppLabel(pm, targetPackage) ?: appName.replaceFirstChar { it.uppercase() }
                    return CommandResult(
                        type = CommandType.OPEN_APP,
                        rawInput = rawInput,
                        spokenReply = "Opening $label to $actionTask.",
                        displayTitle = label,
                        displayDescription = "Action requested: $actionTask",
                        isSuccess = true
                    )
                }
            }

            // If app isn't installed directly, try performing the query directly in browser
            val webUrl = "https://www.google.com/search?q=${Uri.encode("$appName $actionTask")}"
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(webUrl)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            })

            return CommandResult(
                type = CommandType.OPEN_APP,
                rawInput = rawInput,
                spokenReply = "App $appName not installed. Performing web search for $actionTask.",
                displayTitle = "Web Action",
                displayDescription = "Search: $appName $actionTask",
                isSuccess = true
            )

        } catch (e: Exception) {
            Log.e(TAG, "Error executing app action: ${e.localizedMessage}")
            return CommandResult(
                type = CommandType.OPEN_APP,
                rawInput = rawInput,
                spokenReply = "Could not complete the action on $appName: ${e.localizedMessage}",
                displayTitle = "Action Failed",
                displayDescription = e.localizedMessage ?: "Unknown error",
                isSuccess = false
            )
        }
    }

    private fun extractQuery(action: String, prefixes: List<String>): String {
        var result = action.trim()
        for (prefix in prefixes) {
            if (result.startsWith("$prefix ")) {
                result = result.removePrefix("$prefix ").trim()
                break
            }
        }
        return result.ifBlank { action }
    }

    private fun extractWhatsAppMessage(action: String): String {
        // e.g. "send message hello how are you" -> "hello how are you"
        val regex = Regex("(?:send message to|send message|message|chat)\\s*(?:[a-zA-Z0-9_]+)?\\s*(.*)", RegexOption.IGNORE_CASE)
        val match = regex.find(action)
        return match?.groups?.get(1)?.value?.trim().orEmpty()
    }

    private fun getAppLabel(pm: PackageManager, packageName: String): String? {
        return try {
            val appInfo = pm.getApplicationInfo(packageName, 0)
            pm.getApplicationLabel(appInfo).toString()
        } catch (e: Exception) {
            null
        }
    }
}

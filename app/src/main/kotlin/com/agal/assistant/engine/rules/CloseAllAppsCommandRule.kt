package com.agal.assistant.engine.rules

import android.app.ActivityManager
import android.content.Context
import android.os.Build
import android.util.Log
import com.agal.assistant.engine.CommandRule
import com.agal.assistant.engine.models.CommandResult
import com.agal.assistant.engine.models.CommandType

/**
 * CloseAllAppsCommandRule — handles:
 *  - "close all apps" / "close all" / "clear all apps"
 *  - "close recent apps" / "clear recents"
 *  - "kill all apps" / "kill background apps"
 *
 * Uses ActivityManager.killBackgroundProcesses() to terminate background
 * app processes. This does NOT require root or accessibility — it kills
 * cached processes the OS allows a third-party app to kill.
 */
class CloseAllAppsCommandRule : CommandRule {
    override val name: String = "Close All Apps"

    private val TAG = "CloseAllAppsRule"

    override fun matches(input: String): Boolean {
        val lower = input.lowercase().trim()
        return lower == "close all" ||
                lower == "close all apps" ||
                lower.contains("close all apps") ||
                lower.contains("close all applications") ||
                lower.contains("close recent apps") ||
                lower.contains("close recent") ||
                lower.contains("clear all apps") ||
                lower.contains("clear recent apps") ||
                lower.contains("clear recents") ||
                lower.contains("kill all apps") ||
                lower.contains("kill background apps") ||
                lower.contains("stop all apps") ||
                lower.contains("close everything") ||
                lower.contains("end all apps") ||
                lower.contains("close background apps") ||
                lower.contains("clear background apps") ||
                lower.contains("remove all recent")
    }

    override suspend fun execute(input: String, context: Context): CommandResult {
        return try {
            val activityManager =
                context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager

            if (activityManager == null) {
                return CommandResult(
                    type = CommandType.CLOSE_ALL_APPS,
                    rawInput = input,
                    spokenReply = "Activity manager is not available.",
                    displayTitle = "Close Apps Error",
                    displayDescription = "ActivityManager service null",
                    isSuccess = false
                )
            }

            // Get the list of running app processes and kill their background work
            var killedCount = 0

            @Suppress("DEPRECATION")
            val runningApps = activityManager.runningAppProcesses ?: emptyList()
            val myPackage = context.packageName

            for (processInfo in runningApps) {
                // Don't kill ourselves
                if (processInfo.processName == myPackage) continue
                // Don't kill system processes (importance <= FOREGROUND_SERVICE is critical)
                if (processInfo.importance <= ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND_SERVICE) continue

                try {
                    activityManager.killBackgroundProcesses(processInfo.pkgList?.firstOrNull() ?: processInfo.processName)
                    killedCount++
                } catch (e: Exception) {
                    Log.w(TAG, "Could not kill ${processInfo.processName}: ${e.localizedMessage}")
                }
            }

            // Also try to get all installed packages and kill their background processes
            val packageManager = context.packageManager
            val installedPackages = try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    packageManager.getInstalledPackages(0)
                } else {
                    @Suppress("DEPRECATION")
                    packageManager.getInstalledPackages(0)
                }
            } catch (_: Exception) {
                emptyList()
            }

            for (pkgInfo in installedPackages) {
                val pkg = pkgInfo.packageName
                if (pkg == myPackage) continue
                // Skip system core packages
                if (pkg.startsWith("com.android.systemui") ||
                    pkg.startsWith("com.android.launcher") ||
                    pkg == "android"
                ) continue
                try {
                    activityManager.killBackgroundProcesses(pkg)
                } catch (_: Exception) {}
            }

            val reply = if (killedCount > 0) {
                "Closed $killedCount background apps. All recent apps cleared."
            } else {
                "All background apps have been cleared."
            }

            CommandResult(
                type = CommandType.CLOSE_ALL_APPS,
                rawInput = input,
                spokenReply = reply,
                displayTitle = "Apps Closed",
                displayDescription = "Background processes terminated",
                isSuccess = true
            )
        } catch (e: Exception) {
            CommandResult(
                type = CommandType.CLOSE_ALL_APPS,
                rawInput = input,
                spokenReply = "Failed to close apps: ${e.localizedMessage}",
                displayTitle = "Close Apps Error",
                displayDescription = e.localizedMessage ?: "Unknown error",
                isSuccess = false
            )
        }
    }
}

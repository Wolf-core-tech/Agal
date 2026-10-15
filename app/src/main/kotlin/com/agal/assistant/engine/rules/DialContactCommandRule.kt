package com.agal.assistant.engine.rules

import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.provider.ContactsContract
import android.telecom.PhoneAccountHandle
import android.telecom.TelecomManager
import android.telephony.SubscriptionManager
import android.util.Log
import com.agal.assistant.engine.CommandRule
import com.agal.assistant.engine.models.CommandResult
import com.agal.assistant.engine.models.CommandType

/**
 * DialContactCommandRule — handles:
 *  - "call [name]"           → looks up contact, asks SIM 1 or SIM 2
 *  - "sim 1" / "sim 2"       → dials the pending contact on that SIM
 *  - "dial [number]"         → direct dial a number
 *
 * Flow:
 *  1. User says "call mom" → finds "Mom" in contacts → speaks phone number
 *     → asks "SIM 1 or SIM 2?"
 *  2. User says "sim 1" → dials via SIM 1
 *
 * The pending contact is stored in a companion object so the follow-up
 * "sim 1" / "sim 2" command can complete the call.
 */
class DialContactCommandRule : CommandRule {
    override val name: String = "Dial Contact Command"

    private val TAG = "DialContactRule"

    companion object {
        // Pending call state — set when user says "call [name]" awaiting SIM choice
        var pendingContactName: String? = null
        var pendingPhoneNumber: String? = null
        var isAwaitingSimChoice: Boolean = false
    }

    // Match patterns for initiating a call or choosing a SIM
    private val callRegex = Regex(
        """^(?:call|phone|dial|ring|make a call to|call to)\s+(.+)""",
        RegexOption.IGNORE_CASE
    )

    override fun matches(input: String): Boolean {
        val lower = input.lowercase().trim()

        // SIM selection follow-up
        if (isAwaitingSimChoice) {
            if (lower == "sim 1" || lower == "sim1" || lower == "sim one" ||
                lower == "sim 2" || lower == "sim2" || lower == "sim two" ||
                lower.contains("first sim") || lower.contains("second sim") ||
                lower == "1" || lower == "2"
            ) {
                return true
            }
        }

        // "call [name]" / "dial [number]"
        return callRegex.containsMatchIn(lower) ||
                lower.startsWith("call ") ||
                lower.startsWith("phone ") ||
                lower.startsWith("dial ")
    }

    override suspend fun execute(input: String, context: Context): CommandResult {
        val lower = input.lowercase().trim()

        // ─── SIM selection follow-up ──────────────────────────────────────────
        if (isAwaitingSimChoice && pendingPhoneNumber != null) {
            val simSlot = when {
                lower == "sim 1" || lower == "sim1" || lower == "sim one" ||
                lower.contains("first sim") || lower == "1" -> 0
                lower == "sim 2" || lower == "sim2" || lower == "sim two" ||
                lower.contains("second sim") || lower == "2" -> 1
                else -> 0 // Default to SIM 1
            }

            val number = pendingPhoneNumber!!
            val name = pendingContactName ?: number
            isAwaitingSimChoice = false
            pendingContactName = null
            pendingPhoneNumber = null

            return dialNumber(context, input, number, name, simSlot)
        }

        // ─── Parse contact name / number from "call [...]" ────────────────────
        val match = callRegex.find(lower)
        val target = (match?.groupValues?.get(1)
            ?: lower.removePrefix("call ").removePrefix("phone ").removePrefix("dial ")).trim()

        if (target.isBlank()) {
            return CommandResult(
                type = CommandType.DIAL_CONTACT,
                rawInput = input,
                spokenReply = "Please say a contact name or phone number after call.",
                displayTitle = "Call",
                displayDescription = "No contact specified",
                isSuccess = false
            )
        }

        // ─── Check if target is a raw phone number ───────────────────────────
        val digitOnly = target.replace(Regex("[^0-9+]"), "")
        if (digitOnly.length >= 7) {
            // It's a phone number — ask SIM choice
            pendingContactName = null
            pendingPhoneNumber = digitOnly
            isAwaitingSimChoice = true

            return CommandResult(
                type = CommandType.DIAL_CONTACT,
                rawInput = input,
                spokenReply = "Calling $digitOnly. Would you like to use SIM 1 or SIM 2?",
                displayTitle = "📞 Call $digitOnly",
                displayDescription = "Say \"SIM 1\" or \"SIM 2\" to dial",
                isSuccess = true
            )
        }

        // ─── Look up contact by name ──────────────────────────────────────────
        val contactResult = lookupContact(context, target)

        if (contactResult == null) {
            return CommandResult(
                type = CommandType.DIAL_CONTACT,
                rawInput = input,
                spokenReply = "I couldn't find a contact named $target. Please check the name and try again.",
                displayTitle = "Contact Not Found",
                displayDescription = "No match for \"$target\"",
                isSuccess = false
            )
        }

        val (contactName, phoneNumber) = contactResult
        pendingContactName = contactName
        pendingPhoneNumber = phoneNumber
        isAwaitingSimChoice = true

        return CommandResult(
            type = CommandType.DIAL_CONTACT,
            rawInput = input,
            spokenReply = "Found $contactName, number $phoneNumber. Would you like to use SIM 1 or SIM 2?",
            displayTitle = "📞 Call $contactName",
            displayDescription = "$phoneNumber — Say \"SIM 1\" or \"SIM 2\"",
            isSuccess = true
        )
    }

    /**
     * Looks up a contact by display name (case-insensitive partial match).
     * Returns Pair(displayName, phoneNumber) or null if not found.
     */
    private fun lookupContact(context: Context, name: String): Pair<String, String>? {
        var cursor: Cursor? = null
        try {
            // Search contacts whose display name contains the spoken name
            val selection = "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} LIKE ?"
            val selectionArgs = arrayOf("%$name%")
            val sortOrder = "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} ASC"

            cursor = context.contentResolver.query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                arrayOf(
                    ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                    ContactsContract.CommonDataKinds.Phone.NUMBER
                ),
                selection,
                selectionArgs,
                sortOrder
            )

            if (cursor != null && cursor.moveToFirst()) {
                val displayName = cursor.getString(
                    cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
                )
                val number = cursor.getString(
                    cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.NUMBER)
                )
                return Pair(displayName, number)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Contact lookup error: ${e.localizedMessage}")
        } finally {
            cursor?.close()
        }
        return null
    }

    /**
     * Dials the given phone number on the chosen SIM slot.
     * @param simSlot 0 = SIM 1, 1 = SIM 2
     */
    private fun dialNumber(
        context: Context,
        rawInput: String,
        phoneNumber: String,
        contactName: String,
        simSlot: Int
    ): CommandResult {
        return try {
            val callUri = Uri.parse("tel:$phoneNumber")
            val callIntent = Intent(Intent.ACTION_CALL, callUri).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            // Try to set the specific SIM/phone account
            try {
                val telecomManager = context.getSystemService(Context.TELECOM_SERVICE) as? TelecomManager
                val phoneAccountHandles: List<PhoneAccountHandle> =
                    telecomManager?.callCapablePhoneAccounts ?: emptyList()

                if (phoneAccountHandles.size > simSlot) {
                    callIntent.putExtra(
                        "android.telecom.extra.PHONE_ACCOUNT_HANDLE",
                        phoneAccountHandles[simSlot]
                    )
                }
            } catch (e: Exception) {
                Log.w(TAG, "Could not set SIM slot preference: ${e.localizedMessage}")
            }

            context.startActivity(callIntent)

            val simLabel = "SIM ${simSlot + 1}"
            CommandResult(
                type = CommandType.DIAL_CONTACT,
                rawInput = rawInput,
                spokenReply = "Calling $contactName on $simLabel.",
                displayTitle = "📞 Calling $contactName",
                displayDescription = "$phoneNumber via $simLabel",
                isSuccess = true
            )
        } catch (e: Exception) {
            CommandResult(
                type = CommandType.DIAL_CONTACT,
                rawInput = rawInput,
                spokenReply = "Failed to make the call: ${e.localizedMessage}",
                displayTitle = "Call Error",
                displayDescription = e.localizedMessage ?: "Unknown error",
                isSuccess = false
            )
        }
    }
}

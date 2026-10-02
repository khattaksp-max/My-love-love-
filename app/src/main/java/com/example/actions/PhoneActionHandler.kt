package com.example.actions

import android.app.SearchManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.AlarmClock
import android.provider.MediaStore
import android.provider.Settings
import android.util.Log

sealed class ActionResult {
    data class Success(val message: String, val appName: String? = null) : ActionResult()
    data class NeedsConfirmation(val message: String, val actionToConfirm: () -> Unit) : ActionResult()
    data class NotAvailable(val message: String) : ActionResult()
    data class Error(val message: String) : ActionResult()
}

data class ParsedAction(
    val type: ActionType,
    val target: String = "",
    val details: String = ""
)

enum class ActionType {
    OPEN_APP,
    SEARCH_WEB,
    OPEN_CAMERA,
    SET_TIMER,
    OPEN_DIALER,
    OPEN_MAPS,
    OPEN_SETTINGS,
    NONE
}

class PhoneActionHandler(private val context: Context) {

    companion object {
        private const val TAG = "PhoneActionHandler"
    }

    fun parseActionFromText(text: String): ParsedAction? {
        val lower = text.lowercase().trim()

        // Explicit structured tag if Gemini returned one: [ACTION:TYPE, param=value]
        val regex = Regex("""\[ACTION:([A-Z_]+)(?:,\s*([^\]]+))?\]""")
        val match = regex.find(text)
        if (match != null) {
            val typeStr = match.groupValues[1]
            val param = match.groupValues.getOrNull(2) ?: ""
            return when (typeStr) {
                "OPEN_APP" -> ParsedAction(ActionType.OPEN_APP, target = param)
                "SEARCH_WEB" -> ParsedAction(ActionType.SEARCH_WEB, target = param)
                "OPEN_CAMERA" -> ParsedAction(ActionType.OPEN_CAMERA)
                "SET_TIMER" -> ParsedAction(ActionType.SET_TIMER, target = param)
                "OPEN_DIALER" -> ParsedAction(ActionType.OPEN_DIALER, target = param)
                "OPEN_MAPS" -> ParsedAction(ActionType.OPEN_MAPS, target = param)
                "OPEN_SETTINGS" -> ParsedAction(ActionType.OPEN_SETTINGS)
                else -> null
            }
        }

        // Natural language detection fallback:
        return when {
            lower.contains("open whatsapp") -> ParsedAction(ActionType.OPEN_APP, "whatsapp")
            lower.contains("open youtube") -> ParsedAction(ActionType.OPEN_APP, "youtube")
            lower.contains("open spotify") -> ParsedAction(ActionType.OPEN_APP, "spotify")
            lower.contains("open camera") || lower.contains("take a picture") || lower.contains("take a photo") -> ParsedAction(ActionType.OPEN_CAMERA)
            lower.contains("open maps") || lower.contains("show map") || lower.startsWith("directions to") -> {
                val loc = lower.removePrefix("directions to").removePrefix("open maps to").trim()
                ParsedAction(ActionType.OPEN_MAPS, loc)
            }
            lower.contains("open calculator") -> ParsedAction(ActionType.OPEN_APP, "calculator")
            lower.contains("open clock") || lower.contains("open timer") || lower.contains("set a timer") -> {
                val minutes = Regex("""\b(\d+)\s*(?:minute|min)""").find(lower)?.groupValues?.get(1) ?: "5"
                ParsedAction(ActionType.SET_TIMER, minutes)
            }
            lower.contains("open dialer") || lower.contains("open phone") || lower.startsWith("call ") -> {
                val number = Regex("""\d+""").find(lower)?.value ?: ""
                ParsedAction(ActionType.OPEN_DIALER, number)
            }
            lower.contains("open settings") -> ParsedAction(ActionType.OPEN_SETTINGS)
            lower.startsWith("search for ") || lower.startsWith("google ") -> {
                val query = lower.removePrefix("search for ").removePrefix("google ").trim()
                ParsedAction(ActionType.SEARCH_WEB, query)
            }
            else -> null
        }
    }

    fun executeAction(action: ParsedAction): ActionResult {
        return try {
            when (action.type) {
                ActionType.OPEN_APP -> openApplication(action.target)
                ActionType.SEARCH_WEB -> searchWeb(action.target)
                ActionType.OPEN_CAMERA -> openCamera()
                ActionType.SET_TIMER -> setTimer(action.target.toIntOrNull() ?: 5)
                ActionType.OPEN_DIALER -> openDialer(action.target)
                ActionType.OPEN_MAPS -> openMaps(action.target)
                ActionType.OPEN_SETTINGS -> openSettings()
                ActionType.NONE -> ActionResult.Success("No action needed.")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error executing action ${action.type}", e)
            ActionResult.Error("Unable to complete action: ${e.localizedMessage ?: "Unknown error"}")
        }
    }

    private fun openApplication(appNameOrPackage: String): ActionResult {
        val target = appNameOrPackage.lowercase().trim()
        val packageName = when {
            target.contains("whatsapp") -> "com.whatsapp"
            target.contains("youtube") -> "com.google.android.youtube"
            target.contains("spotify") -> "com.spotify.music"
            target.contains("maps") -> "com.google.android.apps.maps"
            target.contains("calculator") -> "com.google.android.calculator"
            target.contains("clock") -> "com.google.android.deskclock"
            target.contains("chrome") -> "com.android.chrome"
            else -> appNameOrPackage
        }

        val pm = context.packageManager
        val launchIntent = pm.getLaunchIntentForPackage(packageName)
        return if (launchIntent != null) {
            launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(launchIntent)
            ActionResult.Success("Opened $appNameOrPackage", appNameOrPackage)
        } else {
            // Check if generic intent exists or open store/web
            if (target.contains("calculator")) {
                val calcIntent = Intent(Intent.ACTION_MAIN).apply {
                    addCategory(Intent.CATEGORY_APP_CALCULATOR)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                if (calcIntent.resolveActivity(pm) != null) {
                    context.startActivity(calcIntent)
                    return ActionResult.Success("Opened Calculator", "Calculator")
                }
            }
            ActionResult.NotAvailable("$appNameOrPackage is not installed on this device.")
        }
    }

    private fun searchWeb(query: String): ActionResult {
        val intent = Intent(Intent.ACTION_WEB_SEARCH).apply {
            putExtra(SearchManager.QUERY, query)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return if (intent.resolveActivity(context.packageManager) != null) {
            context.startActivity(intent)
            ActionResult.Success("Searching web for \"$query\"")
        } else {
            val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/search?q=${Uri.encode(query)}")).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(browserIntent)
            ActionResult.Success("Browsing web for \"$query\"")
        }
    }

    private fun openCamera(): ActionResult {
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return if (intent.resolveActivity(context.packageManager) != null) {
            context.startActivity(intent)
            ActionResult.Success("Opened Camera", "Camera")
        } else {
            ActionResult.NotAvailable("Camera app is not available on this device.")
        }
    }

    private fun setTimer(minutes: Int): ActionResult {
        val intent = Intent(AlarmClock.ACTION_SET_TIMER).apply {
            putExtra(AlarmClock.EXTRA_LENGTH, minutes * 60)
            putExtra(AlarmClock.EXTRA_MESSAGE, "Aura Timer")
            putExtra(AlarmClock.EXTRA_SKIP_UI, false)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return if (intent.resolveActivity(context.packageManager) != null) {
            context.startActivity(intent)
            ActionResult.Success("Set a $minutes-minute timer")
        } else {
            ActionResult.NotAvailable("Timer app is not available.")
        }
    }

    private fun openDialer(phoneNumber: String): ActionResult {
        val uri = if (phoneNumber.isNotBlank()) {
            Uri.parse("tel:$phoneNumber")
        } else {
            Uri.parse("tel:")
        }
        val intent = Intent(Intent.ACTION_DIAL, uri).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return if (intent.resolveActivity(context.packageManager) != null) {
            context.startActivity(intent)
            ActionResult.Success(if (phoneNumber.isNotBlank()) "Opening dialer for $phoneNumber" else "Opened dialer")
        } else {
            ActionResult.NotAvailable("Phone dialer is not available on this device.")
        }
    }

    private fun openMaps(query: String): ActionResult {
        val uri = if (query.isNotBlank()) {
            Uri.parse("geo:0,0?q=${Uri.encode(query)}")
        } else {
            Uri.parse("geo:0,0")
        }
        val intent = Intent(Intent.ACTION_VIEW, uri).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return if (intent.resolveActivity(context.packageManager) != null) {
            context.startActivity(intent)
            ActionResult.Success("Opened Maps" + if (query.isNotBlank()) " for \"$query\"" else "")
        } else {
            val webMaps = Intent(Intent.ACTION_VIEW, Uri.parse("https://maps.google.com/?q=${Uri.encode(query)}")).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(webMaps)
            ActionResult.Success("Opened Google Maps online")
        }
    }

    private fun openSettings(): ActionResult {
        val intent = Intent(Settings.ACTION_SETTINGS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
        return ActionResult.Success("Opened System Settings")
    }
}

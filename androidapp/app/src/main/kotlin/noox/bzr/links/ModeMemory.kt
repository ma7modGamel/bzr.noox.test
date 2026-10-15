package noox.bzr.links

import android.content.SharedPreferences

/** Where to start after sign-in: a provider screen, the customer flow, or the pending link (DEC-067). */
data class ModeRestore(val target: String, val forget: Boolean)

/** DEC-067: the last mode each account used on this device, restored on the next launch or sign-in. */
object ModeMemory {
    const val CUSTOMER = "CUSTOMER"
    const val PROVIDER = "PROVIDER"
    const val LINK = "LINK"

    /** [providerStatus] is ACTIVE, PENDING, NONE (no provider profile) or UNKNOWN (the check failed). */
    fun restoreTarget(lastMode: String?, providerStatus: String, hasPendingLink: Boolean): ModeRestore = when {
        hasPendingLink -> ModeRestore(LINK, forget = false)
        lastMode != PROVIDER -> ModeRestore(CUSTOMER, forget = false)
        providerStatus == "ACTIVE" -> ModeRestore("SCR-P08", forget = false)
        providerStatus == "PENDING" -> ModeRestore("SCR-P07", forget = false)
        providerStatus == "NONE" -> ModeRestore(CUSTOMER, forget = true)
        else -> ModeRestore(CUSTOMER, forget = false)
    }
}

/** Kept per account (by email), so another account on the same phone starts from its own mode. */
class ModeMemoryStore(private val preferences: SharedPreferences) {
    fun lastMode(email: String?): String? = email?.let { preferences.getString(key(it), null) }

    fun remember(email: String?, mode: String) {
        if (email.isNullOrBlank()) return
        preferences.edit().putString(key(email), mode).apply()
    }

    private fun key(email: String) = "last_mode:" + email.trim().lowercase()
}

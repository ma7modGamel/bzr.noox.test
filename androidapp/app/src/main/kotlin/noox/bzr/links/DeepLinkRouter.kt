package noox.bzr.links

/** Where a notification link opens (17 §الروابط العميقة، DEC-058). `orderId` is 0 when the target has none. */
data class DeepLinkTarget(val mode: String, val target: String, val orderId: Int = 0) {
    companion object {
        const val CUSTOMER = "CUSTOMER"
        const val PROVIDER = "PROVIDER"
        val customerNotifications = DeepLinkTarget(CUSTOMER, "SCR-C32")
    }
}

/**
 * Matches a server link against the closed list in 17. The app never builds a link from a code, and any link it
 * does not know opens C32. `providerStatus` is ACTIVE, PENDING, or NONE for the signed-in account.
 */
object DeepLinkRouter {
    private const val SCHEME = "bremo://"
    private val webPrefix = Regex("^https://[^/]+/app/")

    fun route(link: String, providerStatus: String): DeepLinkTarget {
        val path = when {
            link.startsWith(SCHEME) -> link.removePrefix(SCHEME)
            webPrefix.containsMatchIn(link) -> link.replaceFirst(webPrefix, "")
            else -> return DeepLinkTarget.customerNotifications
        }.substringBefore('?').substringBefore('#')
        val parts = path.split('/').filter(String::isNotBlank)

        if (parts.firstOrNull() == "provider") return provider(parts.drop(1), providerStatus)
        return customer(parts) ?: DeepLinkTarget.customerNotifications
    }

    private fun customer(parts: List<String>): DeepLinkTarget? {
        if (parts == listOf("notifications")) return DeepLinkTarget(DeepLinkTarget.CUSTOMER, "SCR-C32")
        if (parts.firstOrNull() != "orders") return null
        val id = parts.getOrNull(1)?.toIntOrNull()?.takeIf { it > 0 } ?: return null
        val target = when (parts.getOrNull(2)) {
            null -> "ORDER"
            "offers" -> "SCR-C06"
            "chat" -> "SCR-C19"
            "rating" -> "SCR-C24"
            "dispute" -> "SCR-C27"
            else -> return null
        }
        return if (parts.size <= 3) DeepLinkTarget(DeepLinkTarget.CUSTOMER, target, id) else null
    }

    private fun provider(parts: List<String>, providerStatus: String): DeepLinkTarget {
        if (parts == listOf("application") && providerStatus in setOf("ACTIVE", "PENDING")) {
            return DeepLinkTarget(DeepLinkTarget.PROVIDER, "SCR-P07")
        }
        if (providerStatus != "ACTIVE") return DeepLinkTarget.customerNotifications
        val notifications = DeepLinkTarget(DeepLinkTarget.PROVIDER, "SCR-C32")
        return when (parts.firstOrNull()) {
            "offers" -> if (parts.size == 1) DeepLinkTarget(DeepLinkTarget.PROVIDER, "SCR-P11") else notifications
            "earnings" -> if (parts.size == 1) DeepLinkTarget(DeepLinkTarget.PROVIDER, "SCR-P18") else notifications
            "notifications" -> notifications
            "requests" -> parts.getOrNull(1)?.toIntOrNull()?.takeIf { it > 0 && parts.size == 2 }
                ?.let { DeepLinkTarget(DeepLinkTarget.PROVIDER, "SCR-P09", it) } ?: notifications
            "orders" -> {
                val id = parts.getOrNull(1)?.toIntOrNull()?.takeIf { it > 0 } ?: return notifications
                val target = when (parts.getOrNull(2)) {
                    null -> "PROVIDER_ORDER"
                    "chat" -> "SCR-C19"
                    "rating" -> "SCR-P17"
                    else -> return notifications
                }
                if (parts.size <= 3) DeepLinkTarget(DeepLinkTarget.PROVIDER, target, id) else notifications
            }
            else -> notifications
        }
    }
}

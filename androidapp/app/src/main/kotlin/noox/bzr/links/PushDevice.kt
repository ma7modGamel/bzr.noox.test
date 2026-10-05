package noox.bzr.links

/** The device side of push (DEC-058): the current FCM token and whether the system lets notifications show. */
interface PushDevice {
    val token: String?
    fun notificationsAllowed(): Boolean

    /** Snapshots and unit tests: no Firebase, notifications allowed. */
    object None : PushDevice {
        override val token: String? = null
        override fun notificationsAllowed() = true
    }
}

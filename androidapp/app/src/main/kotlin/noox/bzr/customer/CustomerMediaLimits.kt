package noox.bzr.customer

object CustomerMediaLimits {
    const val VIDEO_SECONDS = 30.0
    fun videoError(durationSeconds: Double?): String? = when {
        durationSeconds == null || !durationSeconds.isFinite() || durationSeconds <= 0 -> "media.validation.video_unreadable"
        durationSeconds > VIDEO_SECONDS -> "media.validation.video_duration"
        else -> null
    }
}

package noox.bzr.network

import com.google.firebase.crashlytics.FirebaseCrashlytics
import java.util.concurrent.CancellationException
import java.util.Collections
import java.util.WeakHashMap
import noox.bzr.gallery.BuildConfig

/** Crashlytics is the only mobile error transport. Slack credentials never belong in the APK. */
object BremoDiagnostics {
    private var crashlytics: FirebaseCrashlytics? = null
    private val recorded = Collections.synchronizedMap(WeakHashMap<Throwable, Boolean>())

    fun initialize() {
        runCatching {
            crashlytics = FirebaseCrashlytics.getInstance().apply {
                setCustomKey("platform", "Android")
                setCustomKey("app_version", BuildConfig.VERSION_NAME)
                setCustomKey("app_build", BuildConfig.VERSION_CODE)
                setCustomKey("build_type", BuildConfig.BUILD_TYPE)
            }
        }
    }

    fun screen(screen: String, mode: String) {
        runCatching {
            crashlytics?.setCustomKey("screen", screen)
            crashlytics?.setCustomKey("mode", mode)
            crashlytics?.log("screen=$screen mode=$mode")
        }
    }

    fun record(error: Throwable) {
        if (crashlytics == null) return
        val sanitized = safeFailure(error) ?: return
        if (recorded.put(error, true) != null) return
        runCatching { crashlytics?.recordException(sanitized) }
    }
}

/** Preserve the useful original stack without copying response bodies, URLs or exception messages. */
internal fun safeFailure(error: Throwable): Throwable? {
    if (error is CancellationException || error is NetworkException ||
        (error is ApiException && error.status < 500)) return null
    val kind = if (error is ApiException) "HTTP_${error.status}" else error.javaClass.simpleName
    return ReportedAppFailure(kind).apply { stackTrace = error.stackTrace }
}

private class ReportedAppFailure(kind: String) : RuntimeException(kind)

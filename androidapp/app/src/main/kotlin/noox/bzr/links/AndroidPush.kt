package noox.bzr.links

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import noox.bzr.design.R as DesignR
import noox.bzr.gallery.MainActivity
import noox.bzr.gallery.R

/** The FCM token cache and the system notification switch (DEC-058). The token is not a credential. */
class AndroidPushDevice(private val context: Context) : PushDevice {
    private val preferences = context.getSharedPreferences("push", Context.MODE_PRIVATE)

    override val token: String? get() = preferences.getString(TOKEN, null)

    fun store(token: String) = preferences.edit().putString(TOKEN, token).apply()

    override fun notificationsAllowed(): Boolean = NotificationManagerCompat.from(context).areNotificationsEnabled()

    /** 17 §إذن الإشعارات: the system prompt is shown once, after the first C01 or P08. */
    fun shouldAskPermission(): Boolean =
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            !preferences.getBoolean(ASKED, false) &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED

    fun markPermissionAsked() = preferences.edit().putBoolean(ASKED, true).apply()

    companion object {
        const val DEEP_LINK = "deep_link"
        private const val TOKEN = "fcm_token"
        private const val ASKED = "permission_asked"
    }
}

/** 17 §حمولة Push — the channel ids match `android.notification.channel_id` from the server. */
object NotificationChannels {
    fun create(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannels(
            listOf(
                NotificationChannel("orders", context.getString(DesignR.string.orders_title), NotificationManager.IMPORTANCE_HIGH),
                NotificationChannel("messages", context.getString(DesignR.string.messages_title), NotificationManager.IMPORTANCE_HIGH),
                NotificationChannel("account", context.getString(DesignR.string.customer_account_title), NotificationManager.IMPORTANCE_DEFAULT),
            ),
        )
    }
}

fun openNotificationSettings(context: Context) {
    val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
        .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    context.startActivity(intent)
}

/**
 * Background pushes are drawn by the system from the `notification` payload, and a tap opens MainActivity with the
 * data keys as extras. In the foreground FCM hands the message here, so it is drawn the same way.
 */
class BremoMessagingService : FirebaseMessagingService() {
    override fun onNewToken(token: String) {
        AndroidPushDevice(this).store(token)
        onTokenRefreshed?.invoke(token)
    }

    override fun onMessageReceived(message: RemoteMessage) {
        val notification = message.notification ?: return
        if (!NotificationManagerCompat.from(this).areNotificationsEnabled()) return
        val link = message.data[AndroidPushDevice.DEEP_LINK]
        val intent = Intent(this, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            .putExtra(AndroidPushDevice.DEEP_LINK, link)
        val id = (message.data["notification_id"] ?: message.messageId ?: link ?: "").hashCode()
        val pending = PendingIntent.getActivity(this, id, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        val built = NotificationCompat.Builder(this, notification.channelId ?: "orders")
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(notification.title)
            .setContentText(notification.body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(notification.body))
            .setAutoCancel(true)
            .setContentIntent(pending)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        NotificationManagerCompat.from(this).notify(notification.tag, id, built)
    }

    companion object {
        /** Set by MainActivity so a rotated token reaches the server while the app runs. */
        @Volatile var onTokenRefreshed: ((String) -> Unit)? = null
    }
}

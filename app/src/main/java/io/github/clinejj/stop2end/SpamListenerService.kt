package io.github.clinejj.stop2end

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import io.github.clinejj.stop2end.filter.Decision
import io.github.clinejj.stop2end.filter.MessageFilter

/** Dismisses Google Messages / Google Voice notifications for political spam texts. */
class SpamListenerService : NotificationListenerService() {

    private val settings by lazy { AppSettings(this) }
    private val contacts by lazy { ContactsChecker(this) }

    override fun onListenerConnected() {
        // Clean up spam that arrived while the listener was disconnected.
        try {
            activeNotifications?.forEach(::handle)
        } catch (e: SecurityException) {
            Log.w(TAG, "Could not read active notifications", e)
        }
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) = handle(sbn)

    private fun handle(sbn: StatusBarNotification) {
        if (sbn.packageName !in TARGET_PACKAGES || !settings.enabled) return
        // A crash here would unbind the listener until the next reboot or re-grant, so never let one escape.
        try {
            val message = NotificationParser.parse(sbn.notification) ?: return
            val decision = MessageFilter(settings.allowlist, contacts::isContact).evaluate(message)
            Log.d(TAG, "${sbn.packageName}: $decision")
            if (decision == Decision.DISMISS) {
                cancelNotification(sbn.key)
                settings.recordDismissal()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to handle notification from ${sbn.packageName}", e)
        }
    }

    companion object {
        private const val TAG = "Stop2End"

        val TARGET_PACKAGES = setOf(
            "com.google.android.apps.messaging", // Google Messages
            "com.google.android.apps.googlevoice", // Google Voice
        )
    }
}

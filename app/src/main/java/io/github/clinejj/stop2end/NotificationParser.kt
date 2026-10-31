package io.github.clinejj.stop2end

import android.app.Notification
import android.net.Uri
import androidx.core.app.NotificationCompat
import io.github.clinejj.stop2end.filter.IncomingMessage

/** Pulls the sender and text out of a Google Messages / Google Voice notification. */
object NotificationParser {

    fun parse(notification: Notification): IncomingMessage? {
        // The group summary repeats its children's content; the children are handled individually.
        if (notification.flags and Notification.FLAG_GROUP_SUMMARY != 0) return null

        val extras = notification.extras
        val title = extras.getCharSequence(NotificationCompat.EXTRA_TITLE)?.toString()

        val style = NotificationCompat.MessagingStyle.extractMessagingStyleFromNotification(notification)
        if (style != null && style.messages.isNotEmpty()) {
            // Leave out your own replies, which conversation notifications include as history.
            val userName = style.user.name?.toString()
            val incoming = style.messages
                .filter { it.person != null && it.person?.name?.toString() != userName }
                .ifEmpty { style.messages }
            val sender = incoming.last().person
            val uri = sender?.uri
            return IncomingMessage(
                senderName = sender?.name?.toString() ?: style.conversationTitle?.toString() ?: title,
                senderNumber = uri?.let(::numberFromUri),
                senderUri = uri,
                text = incoming.mapNotNull { it.text }.joinToString("\n"),
                isGroup = style.isGroupConversation,
            )
        }

        val text = listOfNotNull(
            extras.getCharSequence(NotificationCompat.EXTRA_BIG_TEXT),
            extras.getCharSequence(NotificationCompat.EXTRA_TEXT),
            *extras.getCharSequenceArray(NotificationCompat.EXTRA_TEXT_LINES).orEmpty(),
        ).joinToString("\n")
        if (text.isBlank()) return null
        return IncomingMessage(senderName = title, senderNumber = null, senderUri = null, text = text)
    }

    private fun numberFromUri(uri: String): String? {
        val parsed = Uri.parse(uri)
        return parsed.schemeSpecificPart.takeIf { parsed.scheme in setOf("tel", "sms", "smsto") }
    }
}

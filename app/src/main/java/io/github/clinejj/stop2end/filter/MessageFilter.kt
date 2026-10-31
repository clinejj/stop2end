package io.github.clinejj.stop2end.filter

/** A message as seen through its notification. */
data class IncomingMessage(
    /** What the notification shows as the sender: a contact name, a business name, or a formatted number. */
    val senderName: String?,
    /** The sender's phone number, when the notification exposes one. */
    val senderNumber: String?,
    /** The sender's Person URI (tel:, or a contacts content URI), when the notification exposes one. */
    val senderUri: String?,
    val text: String,
    val isGroup: Boolean = false,
)

enum class Decision {
    DISMISS,
    NO_MATCH,
    GROUP_CONVERSATION,
    SHORT_CODE,
    ALLOWLISTED,
    CONTACT,
}

class MessageFilter(
    allowlist: Collection<String>,
    private val isContact: (IncomingMessage) -> Boolean,
) {
    private val allowlist = allowlist.map { it.trim() }.filter { it.isNotEmpty() }

    fun evaluate(message: IncomingMessage): Decision {
        // Text match first: it's cheap, and most messages stop here without a contacts lookup.
        if (!SpamMatcher.matches(message.text)) return Decision.NO_MATCH
        if (message.isGroup) return Decision.GROUP_CONVERSATION
        if (message.senderIdentifiers().any(PhoneNumbers::isShortCode)) return Decision.SHORT_CODE
        if (isAllowlisted(message)) return Decision.ALLOWLISTED
        if (isContact(message)) return Decision.CONTACT
        return Decision.DISMISS
    }

    private fun isAllowlisted(message: IncomingMessage): Boolean {
        val senderDigits = message.senderIdentifiers().mapNotNull(PhoneNumbers::digitsOf)
        return allowlist.any { entry ->
            val entryDigits = PhoneNumbers.digitsOf(entry)
            if (entryDigits != null) {
                senderDigits.any { PhoneNumbers.sameNumber(it, entryDigits) }
            } else {
                message.senderName?.trim().equals(entry, ignoreCase = true)
            }
        }
    }

    private fun IncomingMessage.senderIdentifiers() = listOfNotNull(senderNumber, senderName)
}

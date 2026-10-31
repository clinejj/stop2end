package io.github.clinejj.stop2end.filter

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MessageFilterTest {

    private val spamText = "Vote early this weekend! Reply STOP to end"

    private fun message(
        name: String? = "(555) 123-4567",
        number: String? = null,
        text: String = spamText,
        isGroup: Boolean = false,
    ) = IncomingMessage(senderName = name, senderNumber = number, senderUri = null, text = text, isGroup = isGroup)

    private fun filter(allowlist: List<String> = emptyList(), contact: Boolean = false) =
        MessageFilter(allowlist) { contact }

    @Test
    fun dismissesMatchingMessageFromUnknownNumber() {
        assertEquals(Decision.DISMISS, filter().evaluate(message()))
    }

    @Test
    fun keepsNonMatchingMessage() {
        assertEquals(Decision.NO_MATCH, filter().evaluate(message(text = "Running 5 min late")))
    }

    @Test
    fun keepsFiveDigitShortCodes() {
        assertEquals(Decision.SHORT_CODE, filter().evaluate(message(name = "12345")))
        assertEquals(Decision.SHORT_CODE, filter().evaluate(message(name = "CVS", number = "28732")))
    }

    @Test
    fun dismissesOtherLengthNumbers() {
        assertEquals(Decision.DISMISS, filter().evaluate(message(name = "1234")))
        assertEquals(Decision.DISMISS, filter().evaluate(message(name = "+1 (202) 555-0100")))
    }

    @Test
    fun keepsContacts() {
        assertEquals(Decision.CONTACT, filter(contact = true).evaluate(message(name = "Mom")))
    }

    @Test
    fun keepsGroupConversations() {
        assertEquals(Decision.GROUP_CONVERSATION, filter().evaluate(message(isGroup = true)))
    }

    @Test
    fun allowlistMatchesNumbersIgnoringFormattingAndCountryCode() {
        val allowlist = listOf("+1 555-123-4567")
        assertEquals(Decision.ALLOWLISTED, filter(allowlist).evaluate(message(name = "(555) 123-4567")))
        assertEquals(Decision.ALLOWLISTED, filter(allowlist).evaluate(message(name = "Someone", number = "5551234567")))
        assertEquals(Decision.DISMISS, filter(allowlist).evaluate(message(name = "(555) 123-4568")))
    }

    @Test
    fun allowlistMatchesNamesCaseInsensitively() {
        val allowlist = listOf("  City of Springfield ")
        assertEquals(Decision.ALLOWLISTED, filter(allowlist).evaluate(message(name = "city of springfield")))
        assertEquals(Decision.DISMISS, filter(allowlist).evaluate(message(name = "Springfield PAC")))
    }

    @Test
    fun blankAllowlistEntriesAreIgnored() {
        assertEquals(Decision.DISMISS, filter(listOf("", "   ")).evaluate(message(name = null)))
    }

    @Test
    fun phoneNumberParsing() {
        assertEquals("15551234567", PhoneNumbers.digitsOf("+1 (555) 123-4567"))
        assertEquals("12345", PhoneNumbers.digitsOf("12345"))
        assertNull(PhoneNumbers.digitsOf("Mom"))
        assertNull(PhoneNumbers.digitsOf("Room 101"))
        assertNull(PhoneNumbers.digitsOf("  "))
        assertTrue(PhoneNumbers.isShortCode("12345"))
        assertFalse(PhoneNumbers.isShortCode("123456"))
        assertTrue(PhoneNumbers.sameNumber("15551234567", "5551234567"))
        assertFalse(PhoneNumbers.sameNumber("12345", "112345"))
    }
}

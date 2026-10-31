package io.github.clinejj.stop2end.filter

object PhoneNumbers {

    const val SHORT_CODE_LENGTH = 5

    private val formatting = Regex("""[\s().\-+]""")

    /**
     * The digits of [value] if it is written as a phone number or short code
     * ("(555) 123-4567", "+1 555 123 4567", "12345"), or null if it is anything else, such as a name.
     */
    fun digitsOf(value: String?): String? {
        if (value.isNullOrBlank()) return null
        val stripped = value.replace(formatting, "")
        return stripped.takeIf { it.isNotEmpty() && it.all { c -> c in '0'..'9' } }
    }

    fun isShortCode(value: String?): Boolean = digitsOf(value)?.length == SHORT_CODE_LENGTH

    /** Compares two digit strings, ignoring a country code on either side ("15551234567" == "5551234567"). */
    fun sameNumber(a: String, b: String): Boolean {
        if (a == b) return true
        return a.length >= 10 && b.length >= 10 && a.takeLast(10) == b.takeLast(10)
    }
}

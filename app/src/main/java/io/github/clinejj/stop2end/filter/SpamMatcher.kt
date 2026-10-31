package io.github.clinejj.stop2end.filter

import java.text.Normalizer

/**
 * Detects the opt-out boilerplate that political text campaigns append to their messages:
 * "stop to end", "STOP2END", "Stop 2 End", "STOP=END", "st0p to quit", "stop to opt-out", ...
 */
object SpamMatcher {

    private val pattern = Regex(
        """\bst[o0]p[\W_]*(?:(?:to|2)[\W_]*)?(?:end|quit|opt[\W_]*out|unsub(?:scribe)?)\b""",
        RegexOption.IGNORE_CASE,
    )

    // Non-Latin letters that look identical to the Latin ones in the phrases above.
    private val lookalikes = mapOf(
        'а' to 'a', 'е' to 'e', 'о' to 'o', 'р' to 'p', 'с' to 'c', 'ѕ' to 's', 'і' to 'i', 'ԛ' to 'q', 'ԁ' to 'd',
        'А' to 'A', 'Е' to 'E', 'О' to 'O', 'Р' to 'P', 'С' to 'C', 'Ѕ' to 'S', 'Т' to 'T', 'І' to 'I',
        'ο' to 'o', 'Ο' to 'O', 'Ρ' to 'P', 'Τ' to 'T', 'Ε' to 'E', 'Ι' to 'I', 'Ν' to 'N',
    )

    fun matches(text: String): Boolean = pattern.containsMatchIn(normalize(text))

    /** Folds styled Unicode (fullwidth, 𝐛𝐨𝐥𝐝, ...) and lookalike letters to ASCII and drops invisible characters. */
    internal fun normalize(text: String): String {
        val folded = Normalizer.normalize(text, Normalizer.Form.NFKC)
        return buildString(folded.length) {
            for (c in folded) {
                if (isInvisible(c)) continue
                append(lookalikes[c] ?: c)
            }
        }
    }

    private fun isInvisible(c: Char) =
        c == '­' || c == '﻿' || c in '​'..'‏' || c in '‪'..'‮' || c in '⁠'..'⁤'
}

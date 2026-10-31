package io.github.clinejj.stop2end.filter

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SpamMatcherTest {

    @Test
    fun matchesCommonVariations() {
        listOf(
            "Hi Jane, it's Sam with the campaign. Can we count on your vote Tuesday? Reply STOP to end",
            "Chip in \$5 before midnight! stop2end",
            "Polls close at 8pm. Stop 2 End",
            "Donate now: example.com/x STOP=END",
            "Text STOP TO END",
            "Txt STOP->END",
            "stoptoend",
            "Reply ST0P to end",
            "Reply STOP to quit",
            "Stop2Quit",
            "Reply STOP to opt-out",
            "Reply STOP to opt out",
            "STOP 2 optout",
            "Reply STOP to unsubscribe",
            "stop to unsub",
            "Reply stop\nto end",
            "Reply \"STOP\" to end.",
            "Reply STOP_to_END",
        ).forEach { assertTrue("expected match: $it", SpamMatcher.matches(it)) }
    }

    @Test
    fun matchesObfuscatedText() {
        listOf(
            "Reply ＳＴＯＰ ｔｏ ｅｎｄ", // fullwidth
            "Reply 𝐒𝐓𝐎𝐏 to end", // mathematical bold
            "Reply S​T​O​P to end", // zero-width spaces
            "Reply SТОР to end", // Cyrillic Т, О, Р
            "Reply st­op to e­nd", // soft hyphens
        ).forEach { assertTrue("expected match: $it", SpamMatcher.matches(it)) }
    }

    @Test
    fun ignoresOrdinaryMessages() {
        listOf(
            "Can you stop by the store on your way home?",
            "Don't stop! We're almost to the end of the trail",
            "That movie was unstoppable to the end",
            "Reply STOP to stop receiving messages",
            "Reply HELP for help",
            "I'll be at the bus stop at 5",
            "stop sending me memes lol",
            "Meeting will end at 3, stop by after",
            "Reply STOP to ending soon", // "end" must be a whole word
        ).forEach { assertFalse("expected no match: $it", SpamMatcher.matches(it)) }
    }
}

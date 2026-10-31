package io.github.clinejj.stop2end

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit

class AppSettings(context: Context) {

    private val prefs = context.applicationContext.getSharedPreferences("settings", Context.MODE_PRIVATE)

    var enabled: Boolean
        get() = prefs.getBoolean(KEY_ENABLED, true)
        set(value) = prefs.edit { putBoolean(KEY_ENABLED, value) }

    val allowlist: Set<String>
        get() = prefs.getStringSet(KEY_ALLOWLIST, null).orEmpty().toSet()

    val dismissedCount: Int
        get() = prefs.getInt(KEY_DISMISSED_COUNT, 0)

    fun addToAllowlist(entry: String) {
        val trimmed = entry.trim()
        if (trimmed.isEmpty()) return
        prefs.edit { putStringSet(KEY_ALLOWLIST, allowlist + trimmed) }
    }

    fun removeFromAllowlist(entry: String) {
        prefs.edit { putStringSet(KEY_ALLOWLIST, allowlist - entry) }
    }

    fun recordDismissal() {
        prefs.edit { putInt(KEY_DISMISSED_COUNT, dismissedCount + 1) }
    }

    fun resetDismissedCount() {
        prefs.edit { putInt(KEY_DISMISSED_COUNT, 0) }
    }

    /** Calls [onChange] whenever a setting changes, including from the listener service. Returns an unsubscribe function. */
    fun observe(onChange: () -> Unit): () -> Unit {
        // SharedPreferences holds listeners weakly; the returned lambda keeps this one alive.
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, _ -> onChange() }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        return { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
    }

    private companion object {
        const val KEY_ENABLED = "enabled"
        const val KEY_ALLOWLIST = "allowlist"
        const val KEY_DISMISSED_COUNT = "dismissed_count"
    }
}

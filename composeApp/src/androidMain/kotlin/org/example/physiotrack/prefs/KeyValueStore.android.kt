package org.example.physiotrack.prefs

import android.content.Context
import org.example.physiotrack.AndroidContextHolder

actual object KeyValueStore {
    private const val PREFS_NAME = "physiotrack_prefs"

    private fun prefs() =
        (AndroidContextHolder.appContext ?: error("AndroidContextHolder.appContext is null"))
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    actual fun getString(key: String): String? {
        return prefs().getString(key, null)
    }

    actual fun putString(key: String, value: String) {
        prefs().edit().putString(key, value).apply()
    }

    actual fun remove(key: String) {
        prefs().edit().remove(key).apply()
    }
}

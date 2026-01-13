package org.example.physiotrack.prefs

import java.util.prefs.Preferences

actual object KeyValueStore {
    private val prefs: Preferences = Preferences.userRoot().node("org.example.physiotrack")

    actual fun getString(key: String): String? = prefs.get(key, null)

    actual fun putString(key: String, value: String) {
        prefs.put(key, value)
    }

    actual fun remove(key: String) {
        prefs.remove(key)
    }
}

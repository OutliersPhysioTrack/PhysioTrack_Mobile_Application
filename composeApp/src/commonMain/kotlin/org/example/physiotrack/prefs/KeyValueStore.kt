package org.example.physiotrack.prefs

/**
 * Minimal cross-platform key-value storage for this prototype.
 *
 * Android: SharedPreferences
 * Desktop (JVM): java.util.prefs.Preferences
 */
expect object KeyValueStore {
    fun getString(key: String): String?
    fun putString(key: String, value: String)
    fun remove(key: String)
}

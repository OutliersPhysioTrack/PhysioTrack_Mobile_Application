package org.example.physiotrack

import android.content.Context

/** Holds application context for simple non-Compose helpers in this prototype. */
object AndroidContextHolder {
    @Volatile
    var appContext: Context? = null
}

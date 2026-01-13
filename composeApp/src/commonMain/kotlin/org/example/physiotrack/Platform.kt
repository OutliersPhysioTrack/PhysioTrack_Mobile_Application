package org.example.physiotrack

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform
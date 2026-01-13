package org.example.physiotrack.network

import io.ktor.client.HttpClient

/** Platform-specific HttpClient with JSON configured. */
expect fun createHttpClient(): HttpClient

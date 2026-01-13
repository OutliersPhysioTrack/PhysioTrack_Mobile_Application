package org.example.physiotrack.network

import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import java.util.concurrent.TimeUnit

actual fun createHttpClient(): HttpClient {
    return HttpClient(OkHttp) {

        install(ContentNegotiation) {
            json(
                Json {
                    ignoreUnknownKeys = true
                    isLenient = true
                    explicitNulls = false
                }
            )
        }

        install(HttpTimeout) {
            connectTimeoutMillis = 30_000
            socketTimeoutMillis = 120_000
            requestTimeoutMillis = 120_000
        }

        engine {
            config {
                connectTimeout(30, TimeUnit.SECONDS)
                readTimeout(120, TimeUnit.SECONDS)
                writeTimeout(120, TimeUnit.SECONDS)
                callTimeout(120, TimeUnit.SECONDS)
            }
        }
    }
}
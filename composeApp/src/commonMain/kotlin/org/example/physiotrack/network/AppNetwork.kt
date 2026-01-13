package org.example.physiotrack.network

import io.ktor.client.HttpClient
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Simple global network locator for this prototype.
 *
 * Rationale:
 * - avoids refactoring every screen to pass api/config plumbing.
 * - baseUrl changes frequently (tunnel), so we keep config mutable.
 */
object AppNetwork {
    private val mutex = Mutex()

    private var _config: ApiConfig = ApiConfig()
    val config: ApiConfig get() = _config

    private var client: HttpClient? = null

    val api: PhysioApi by lazy {
        PhysioApi(
            client = getClient(),
            config = { _config },
        )
    }

    fun setConfig(newConfig: ApiConfig) {
        _config = newConfig
    }

    private fun getClient(): HttpClient {
        val existing = client
        if (existing != null) return existing
        val created = createHttpClient()
        client = created
        return created
    }

    suspend fun close() {
        mutex.withLock {
            client?.close()
            client = null
        }
    }
}
package org.example.physiotrack.network

/**
 * Runtime-configurable API settings.
 *
 * Notes:
 * - baseUrl should include scheme + host + port, e.g. "http://192.168.1.40:8000".
 * - deviceId follows your ingest payload (e.g. "dev-001").
 */
data class ApiConfig(
    val baseUrl: String = "https://divide-dating-beside-western.trycloudflare.com/" +
            "",
    val deviceId: String = "dev-001",
    val patientId: String = "",
)

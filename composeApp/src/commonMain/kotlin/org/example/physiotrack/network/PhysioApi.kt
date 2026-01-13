package org.example.physiotrack.network

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.patch
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.request.forms.formData
import io.ktor.client.request.forms.submitFormWithBinaryData
import io.ktor.http.ContentType
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.http.headersOf

class PhysioApi(
    private val client: HttpClient,
    private val config: () -> ApiConfig,
) {
    private fun baseUrl(): String = config().baseUrl.trimEnd('/')

    suspend fun health(): Boolean {
        val resp = client.get("${baseUrl()}/health").body<HealthResponse>()
        return resp.ok
    }

    suspend fun listExercises(): List<ExerciseDto> {
        return client.get("${baseUrl()}/exercises").body()
    }

    suspend fun getPatient(patientId: String): PatientDto {
        return client.get("${baseUrl()}/patients/${patientId}").body()
    }

    suspend fun register(payload: AuthRegisterDto): PatientDto {
        return client.post("${baseUrl()}/auth/register") {
            contentType(ContentType.Application.Json)
            setBody(payload)
        }.body()
    }

    suspend fun login(payload: AuthLoginDto): PatientDto {
        return client.post("${baseUrl()}/auth/login") {
            contentType(ContentType.Application.Json)
            setBody(payload)
        }.body()
    }

    suspend fun latestReadings(
        deviceId: String,
        metricsCsv: String? = null,
    ): List<LatestReadingDto> {
        return client.get("${baseUrl()}/devices/${deviceId}/sensor-readings/latest") {
            if (!metricsCsv.isNullOrBlank()) {
                parameter("metrics", metricsCsv)
            }
        }.body()
    }

    suspend fun listDevices(): List<DeviceDto> {
        return client.get("${baseUrl()}/devices").body()
    }

    suspend fun latestAi(deviceId: String): AiLatestDto {
        return client.get("${baseUrl()}/devices/${deviceId}/ai/latest").body()
    }

    suspend fun createSession(payload: SessionCreateDto): String {
        val resp = client.post("${baseUrl()}/sessions") {
            contentType(ContentType.Application.Json)
            setBody(payload)
        }.body<SessionCreateResp>()
        return resp.session_id
    }

    suspend fun listSessions(patientId: String): List<SessionDto> {
        return client.get("${baseUrl()}/sessions") {
            parameter("patient_id", patientId)
        }.body()
    }

    suspend fun listAssignments(patientId: String): List<AssignmentDto> {
        return client.get("${baseUrl()}/assignments") {
            parameter("patient_id", patientId)
        }.body()
    }

    suspend fun listNotes(patientId: String, onlyNew: Boolean = false): List<NoteDto> {
        return client.get("${baseUrl()}/notes") {
            parameter("patient_id", patientId)
            if (onlyNew) parameter("only_new", true)
        }.body()
    }

    suspend fun markNoteSeen(noteId: String): Boolean {
        val resp = client.patch("${baseUrl()}/notes/${noteId}/seen").body<OkResp>()
        return resp.ok
    }

    suspend fun patchSession(sessionId: String, payload: SessionPatchDto): Boolean {
        val resp = client.patch("${baseUrl()}/sessions/${sessionId}") {
            contentType(ContentType.Application.Json)
            setBody(payload)
        }.body<OkResp>()
        return resp.ok
    }

    suspend fun uploadHighlight(
        sessionId: String,
        patientId: String,
        startMs: Int,
        endMs: Int,
        fileBytes: ByteArray,
        fileName: String = "auto_clip.mp4",
    ): HighlightUploadResp {
        return client.submitFormWithBinaryData(
            url = "${baseUrl()}/highlights/upload",
            formData = formData {
                append("session_id", sessionId)
                append("patient_id", patientId)
                append("start_ms", startMs.toString())
                append("end_ms", endMs.toString())
                append(
                    key = "file",
                    value = fileBytes,
                    headers = headersOf(
                        HttpHeaders.ContentType to listOf("video/mp4"),
                        HttpHeaders.ContentDisposition to listOf("form-data; name=\"file\"; filename=\"$fileName\"")
                    )
                )
            }
        ).body()
    }
}

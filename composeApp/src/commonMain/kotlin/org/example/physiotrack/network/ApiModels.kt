package org.example.physiotrack.network

import kotlinx.datetime.Instant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class HealthResponse(
    val ok: Boolean = true,
)

@Serializable
data class ExerciseDto(
    @SerialName("exercise_id") val exerciseId: String,
    @SerialName("exercise_name") val exerciseName: String,
    @SerialName("default_sets") val defaultSets: Int? = null,
    @SerialName("default_reps") val defaultReps: Int? = null,
    val notes: String? = null,
)

@Serializable
data class LatestReadingDto(
    val device_id: String,
    val patient_id: String? = null,
    val metric: String,
    val ts: String,
    val value: Double? = null,
    val unit: String? = null,
)

@Serializable
data class DeviceDto(
    val device_id: String,
    val patient_id: String? = null,
    val label: String? = null,
    val status: String? = null,
    val last_seen_at: String? = null,
    val patient_name: String? = null,
)

@Serializable
data class AiLatestDto(
    val device_id: String? = null,
    @SerialName("ai_label") val aiLabel: String? = null,
    @SerialName("ai_conf") val aiConf: Double? = null,
    val label: String? = null,
    val conf: Double? = null,
)

@Serializable
data class SessionCreateDto(
    val patient_id: String,
    val exercise_id: String? = null,
    val assignment_id: String? = null,
    val started_at: String,
    val ended_at: String? = null,
    val duration_sec: Int? = null,
    val rep_count: Int? = null,
    val adherence: Double? = null,
    val rom_avg_deg: Double? = null,
    val grip_avg_kg: Double? = null,
)

@Serializable
data class SessionCreateResp(
    val session_id: String,
)

@Serializable
data class SessionPatchDto(
    val ended_at: String? = null,
    val duration_sec: Int? = null,
    val rep_count: Int? = null,
    val adherence: Double? = null,
    val rom_avg_deg: Double? = null,
    val grip_avg_kg: Double? = null,
    val pain_score: Int? = null,
    val pain_raw_score: Int? = null,
    val pain_notes: String? = null,
)

@Serializable
data class OkResp(
    val ok: Boolean = true,
)

// ---------------- Assignments / Sessions / Notes ----------------

@Serializable
data class AssignmentDto(
    val assignment_id: String,
    val patient_id: String,
    val exercise_id: String,
    val sets: Int? = null,
    val reps: Int? = null,
    val notes: String? = null,
    val status: String? = null,
    val start_at: String? = null,
    val due_at: String? = null,
)

@Serializable
data class SessionDto(
    val session_id: String,
    val patient_id: String,
    val exercise_id: String? = null,
    val assignment_id: String? = null,
    val started_at: String,
    val ended_at: String? = null,
    val duration_sec: Int? = null,
    val rep_count: Int? = null,
    val adherence: Double? = null,
    val rom_avg_deg: Double? = null,
    val grip_avg_kg: Double? = null,
)

@Serializable
data class NoteDto(
    val note_id: String,
    val patient_id: String,
    val session_id: String? = null,
    val therapist_id: String? = null,
    val title: String? = null,
    val body: String,
    val is_new: Boolean,
    val created_at: String,
    val seen_at: String? = null,
)

// ---------------- Auth / Patient ----------------

@Serializable
data class PatientDto(
    val patient_id: String,
    val name: String,
    val age: Int? = null,
    val height_cm: Int? = null,
    val weight_kg: Double? = null,
    val primary_condition: String? = null,
    val phone: String? = null,
    val email: String? = null,
    val assigned_therapist_id: String? = null,
    val created_at: String? = null,
)

@Serializable
data class AuthLoginDto(
    val email: String,
    val password: String,
)

@Serializable
data class AuthRegisterDto(
    val name: String,
    val age: Int? = null,
    val height_cm: Int? = null,
    val weight_kg: Double? = null,
    val primary_condition: String? = null,
    val phone: String? = null,
    val email: String,
    val password: String,
)

/** Small helper for status computed on-device from latest readings. */
data class DeviceHealth(
    val isOnline: Boolean,
    val needsCalibration: Boolean,
    val lastSeen: Instant?,
    val invalidRatio: Double,
)

@Serializable
data class HighlightUploadResp(
    val highlight_id: String,
    val stream_path: String,
)

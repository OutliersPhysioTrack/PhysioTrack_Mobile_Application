package org.example.physiotrack.prefs

object AuthSession {
    private const val KEY_PATIENT_ID = "patient_id"

    fun getPatientId(): String? = KeyValueStore.getString(KEY_PATIENT_ID)?.trim().takeIf { !it.isNullOrBlank() }

    fun setPatientId(patientId: String) {
        KeyValueStore.putString(KEY_PATIENT_ID, patientId.trim())
    }

    fun clear() {
        KeyValueStore.remove(KEY_PATIENT_ID)
    }
}

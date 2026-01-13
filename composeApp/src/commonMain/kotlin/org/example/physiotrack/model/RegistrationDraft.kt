package org.example.physiotrack.model

data class RegistrationDraft(
    val name: String,
    val age: Int,
    val heightCm: Int,
    val weightKg: Double,
    val primaryCondition: String,
    val phone: String,
)

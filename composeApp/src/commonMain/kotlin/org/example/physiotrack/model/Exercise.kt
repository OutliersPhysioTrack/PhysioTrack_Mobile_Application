package org.example.physiotrack.model

enum class Difficulty { Easy, Medium, Hard }

enum class SensorTag { Camera, HR, SpO2, Grip, Effort }

data class Exercise(
    val id: String,
    val title: String,
    val durationMin: Int,
    val difficulty: Difficulty,
    val tags: List<SensorTag>,
    val about: String,
    val steps: List<String>,
    val commonMistakes: List<String>,
    val tutorialVideoAsset: String? = null,
)

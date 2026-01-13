package org.example.physiotrack.model

object SampleData {
    val exercises: List<Exercise> = listOf(
        Exercise(
            id = "shoulder_rotation",
            title = "Shoulder Rotation",
            durationMin = 4,
            difficulty = Difficulty.Easy,
            tags = listOf(SensorTag.Camera, SensorTag.HR),
            about = "Gentle shoulder rotation exercise to improve range of motion and reduce stiffness.",
            steps = listOf(
                "Sit or stand in a comfortable position with your back straight",
                "Keep your arms relaxed at your sides",
                "Slowly rotate your shoulders forward in a circular motion",
                "Complete 5 rotations, then reverse direction",
                "Repeat for 3 sets with 30-second rest between sets",
            ),
            commonMistakes = listOf(
                "Moving too quickly — maintain slow, controlled movements",
                "Hunching shoulders — keep them relaxed",
                "Holding your breath — breathe naturally throughout",
            ),
            tutorialVideoAsset = "exercise_videos/shoulder_rotation.mp4",
        ),
        Exercise(
            id = "wrist_flexion",
            title = "Wrist Flexion",
            durationMin = 3,
            difficulty = Difficulty.Easy,
            tags = listOf(SensorTag.Camera, SensorTag.Grip),
            about = "Improve wrist mobility with gentle flexion and extension movements.",
            steps = listOf(
                "Rest your forearm on a table with your hand over the edge",
                "Slowly bend your wrist upward, then downward",
                "Keep the movement smooth and pain-free",
            ),
            commonMistakes = listOf(
                "Using shoulder movement — isolate the wrist",
                "Bouncing at end range — move smoothly",
            ),
            tutorialVideoAsset = "exercise_videos/wrist_flexion.mp4",
        ),
        Exercise(
            id = "grip_squeeze",
            title = "Grip Squeeze",
            durationMin = 2,
            difficulty = Difficulty.Medium,
            tags = listOf(SensorTag.Grip, SensorTag.HR),
            about = "Strengthen hand and forearm muscles with controlled squeezing.",
            steps = listOf(
                "Hold the grip device (or soft ball) comfortably",
                "Squeeze steadily for 2 seconds",
                "Release slowly and repeat",
            ),
            commonMistakes = listOf(
                "Squeezing too hard — aim for steady force",
                "Holding your breath — breathe normally",
            ),
            tutorialVideoAsset = "exercise_videos/grip_squeeze.mp4",
        ),
        Exercise(
            id = "ankle_rotation",
            title = "Ankle Rotation",
            durationMin = 3,
            difficulty = Difficulty.Easy,
            tags = listOf(SensorTag.Camera),
            about = "Ankle circles to improve lower limb mobility.",
            steps = listOf(
                "Sit with one leg extended",
                "Rotate your ankle slowly clockwise for 10 circles",
                "Repeat counter-clockwise",
            ),
            commonMistakes = listOf(
                "Moving knee/hip instead of ankle",
            ),
            tutorialVideoAsset = "exercise_videos/ankle_rotation.mp4",
        ),
    )

    fun exerciseById(id: String): Exercise = exercises.firstOrNull { it.id == id }
        ?: exercises.first()
}

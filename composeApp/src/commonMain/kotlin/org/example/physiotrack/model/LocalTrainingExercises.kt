package org.example.physiotrack.model

object LocalTrainingExercises {
    val items: List<Exercise> = listOf(
        Exercise(
            id = "local_elbow_flexion",
            title = "Elbow Flexion",
            durationMin = 5,
            difficulty = Difficulty.Easy,
            tags = listOf(SensorTag.Camera),
            about = "Bend and straighten your elbow in a slow, controlled motion to improve range of motion.",
            steps = listOf(
                "Sit or stand with your arm relaxed by your side.",
                "Slowly bend the elbow bringing the hand toward the shoulder.",
                "Pause briefly, then straighten the elbow back to the start."
            ),
            commonMistakes = listOf(
                "Moving too fast or using momentum.",
                "Lifting the shoulder instead of isolating the elbow.",
                "Locking the elbow aggressively at the end range."
            ),
            tutorialVideoAsset = "exercise_videos/exercise_elbow_flexion.gif",
        ),
        Exercise(
            id = "local_arm_raise",
            title = "Arm Raise",
            durationMin = 5,
            difficulty = Difficulty.Easy,
            tags = listOf(SensorTag.Camera),
            about = "Raise your arm with controlled movement to build shoulder mobility and strength.",
            steps = listOf(
                "Stand tall with your arm by your side.",
                "Raise your arm to a comfortable height (aim shoulder level).",
                "Lower slowly back down."
            ),
            commonMistakes = listOf(
                "Shrugging the shoulder toward the ear.",
                "Arching the lower back to compensate.",
                "Dropping the arm quickly on the way down."
            ),
            tutorialVideoAsset = "exercise_videos/exercise_arm_raise.gif",
        ),
        Exercise(
            id = "local_hip_flexion",
            title = "Hip Flexion",
            durationMin = 6,
            difficulty = Difficulty.Easy,
            tags = listOf(SensorTag.Camera),
            about = "Lift your knee toward your chest to train hip flexors and improve gait mechanics.",
            steps = listOf(
                "Stand holding a support if needed.",
                "Lift one knee upward toward the chest (comfortable height).",
                "Lower the foot back down with control and repeat."
            ),
            commonMistakes = listOf(
                "Leaning backward to lift higher.",
                "Twisting the pelvis instead of lifting straight.",
                "Slamming the foot down between reps."
            ),
            tutorialVideoAsset = "exercise_videos/exercise_hip_flexion.gif",
        ),
        Exercise(
            id = "local_knee_extension",
            title = "Knee Extension",
            durationMin = 6,
            difficulty = Difficulty.Easy,
            tags = listOf(SensorTag.Camera),
            about = "Straighten the knee from a seated position to strengthen the quadriceps and improve knee control.",
            steps = listOf(
                "Sit with your back supported and feet flat.",
                "Slowly straighten one knee until almost fully extended.",
                "Pause briefly, then lower the foot back down."
            ),
            commonMistakes = listOf(
                "Kicking the leg up quickly.",
                "Locking the knee hard at the top.",
                "Leaning back to compensate instead of using the thigh."
            ),
            tutorialVideoAsset = "exercise_videos/exercise_knee_extension.gif",
        ),
    )
}

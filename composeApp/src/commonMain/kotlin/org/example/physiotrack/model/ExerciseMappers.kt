package org.example.physiotrack.model

import org.example.physiotrack.network.ExerciseDto

private fun slugify(s: String): String {
    return s.trim().lowercase()
        .replace(Regex("[^a-z0-9]+"), "_")
        .trim('_')
}


fun ExerciseDto.toUiExercise(): Exercise {
    val slug = slugify(exerciseName)
    val inferredVideo = "exercise_videos/${slug}.mp4"

    val tags = buildList {
        add(SensorTag.Camera)
        if (exerciseName.contains("grip", true) || exerciseName.contains("squeeze", true)) add(SensorTag.Grip)
        if (exerciseName.contains("heart", true) || exerciseName.contains("cardio", true)) add(SensorTag.HR)
    }.distinct()

    return Exercise(
        id = exerciseId,
        title = exerciseName,
        durationMin = ((defaultSets ?: 3) * (defaultReps ?: 10) / 10).coerceIn(2, 15),
        difficulty = Difficulty.Easy,
        tags = tags,
        about = notes ?: "",
        steps = listOf(
            "Follow the therapist instructions for this exercise.",
            "Keep movement slow and controlled.",
            "Stop if you feel sharp pain."
        ),
        commonMistakes = listOf(
            "Moving too quickly",
            "Holding your breath"
        ),
        tutorialVideoAsset = inferredVideo,
    )
}

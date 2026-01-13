package org.example.physiotrack.training

import androidx.compose.runtime.Composable
import org.example.physiotrack.model.Exercise

@Composable
expect fun TrainingSessionScreen(
    exercise: Exercise,
    onBack: () -> Unit,
    onFinish: (sessionId: String?, setIndex: Int, repsDone: Int) -> Unit,
)

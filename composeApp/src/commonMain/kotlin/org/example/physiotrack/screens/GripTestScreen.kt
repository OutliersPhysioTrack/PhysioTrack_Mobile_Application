package org.example.physiotrack.screens

import androidx.compose.runtime.Composable

@Composable
expect fun GripTestScreen(
    onBack: () -> Unit,
    onFinish: () -> Unit,
)

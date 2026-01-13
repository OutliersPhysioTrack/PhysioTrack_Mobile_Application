package org.example.physiotrack.media

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
expect fun AssetGif(
    assetPath: String,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
)

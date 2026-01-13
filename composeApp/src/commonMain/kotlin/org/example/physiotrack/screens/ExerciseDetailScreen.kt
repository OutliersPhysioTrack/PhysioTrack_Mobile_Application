@file:OptIn(ExperimentalMaterial3Api::class)

package org.example.physiotrack.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.PanTool
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.example.physiotrack.media.AssetGif
import org.example.physiotrack.model.Exercise
import org.example.physiotrack.model.SensorTag
import org.example.physiotrack.ui.components.PremiumCard
import org.example.physiotrack.ui.components.PremiumCardClickable
import org.example.physiotrack.ui.components.PrimaryButton
import org.example.physiotrack.video.playAssetVideo

@Composable
fun ExerciseDetailScreen(
    exercise: Exercise,
    onBack: () -> Unit,
    onStartSession: () -> Unit,
) {
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(exercise.title, fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(horizontal = 20.dp, vertical = 16.dp)
                .fillMaxSize()
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                item {
                    val asset = exercise.tutorialVideoAsset
                    val isGif = asset?.endsWith(".gif", ignoreCase = true) == true
                    val isMp4 = asset?.endsWith(".mp4", ignoreCase = true) == true

                    val card: @Composable (@Composable () -> Unit) -> Unit = { content ->
                        if (asset != null && isMp4) {
                            PremiumCardClickable(
                                modifier = Modifier.fillMaxWidth(),
                                onClick = { playAssetVideo(asset) }
                            ) { content() }
                        } else {
                            PremiumCard(modifier = Modifier.fillMaxWidth()) { content() }
                        }
                    }

                    card {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(180.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            if (asset != null && isGif) {
                                AssetGif(
                                    assetPath = asset,
                                    modifier = Modifier.fillMaxSize(),
                                    contentDescription = "${exercise.title} tutorial"
                                )
                            } else {
                                Icon(
                                    Icons.Filled.PlayArrow,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(48.dp),
                                )
                            }
                        }
                        Text(
                            "Tutorial",
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                item {
                    Text("About", fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(4.dp))
                    Text(exercise.about, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }

                item {
                    Spacer(Modifier.height(2.dp))
                    Text("Required Sensors", fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(8.dp))
                    exercise.tags.forEach { tag ->
                        SensorRow(tag)
                        Spacer(Modifier.height(10.dp))
                    }
                }

                item {
                    Text("Step-by-Step Instructions", fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(8.dp))
                }

                itemsIndexed(exercise.steps) { idx, step ->
                    PremiumCard(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Box(
                                modifier = Modifier.size(26.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    "${idx + 1}",
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            Spacer(Modifier.size(12.dp))
                            Text(step)
                        }
                    }
                }

                item {
                    Spacer(Modifier.height(6.dp))
                    CommonMistakesCard(mistakes = exercise.commonMistakes)
                }
            }

            PrimaryButton(text = "Start Session", onClick = onStartSession)
        }
    }
}

@Composable
private fun SensorRow(tag: SensorTag) {
    val (icon, title, subtitle) = when (tag) {
        SensorTag.Camera -> Triple(Icons.Filled.CameraAlt, "Camera", "Motion tracking")
        SensorTag.HR -> Triple(Icons.Filled.Favorite, "Heart Rate", "Monitor effort")
        SensorTag.SpO2 -> Triple(Icons.Filled.Favorite, "SpO₂", "Oxygen saturation")
        SensorTag.Grip -> Triple(Icons.Filled.PanTool, "Grip", "Hand strength")
        SensorTag.Effort -> Triple(Icons.Filled.Bolt, "Effort", "Fatigue estimation")
    }

    PremiumCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
            Spacer(Modifier.size(12.dp))
            Column {
                Text(title, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(2.dp))
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun CommonMistakesCard(
    mistakes: List<String>,
) {
    val bg = Color(0xFFFFF4D6)
    val border = Color(0xFFE9C978)
    val text = Color(0xFF8A6A1F)

    Text("Common Mistakes to Avoid", fontWeight = FontWeight.SemiBold)
    Spacer(Modifier.height(10.dp))

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, border, RoundedCornerShape(14.dp))
            .background(bg, RoundedCornerShape(14.dp))
            .padding(14.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            mistakes.forEach { m ->
                Row(verticalAlignment = Alignment.Top) {
                    Icon(
                        imageVector = Icons.Filled.WarningAmber,
                        contentDescription = null,
                        tint = text,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.size(10.dp))
                    Text(
                        text = m,
                        style = MaterialTheme.typography.bodySmall,
                        color = text
                    )
                }
            }
        }
    }
}

@file:OptIn(ExperimentalMaterial3Api::class)

package org.example.physiotrack.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.example.physiotrack.ui.components.PremiumCard
import org.example.physiotrack.ui.components.PrimaryButton
import org.example.physiotrack.ui.components.SecondaryButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.foundation.layout.navigationBarsPadding

@Composable
fun BaselineAssessmentScreen(
    onBack: () -> Unit,
    onStartAssessment: () -> Unit,
    onSkip: () -> Unit,
) {
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Baseline Assessment", fontWeight = FontWeight.SemiBold) },
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
                .padding(24.dp)
                .fillMaxSize()
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Column {
                Text(
                    "We'll measure a few key metrics to track your progress over time. This will only take a few minutes.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(16.dp))

                BaselineItem(
                    title = "Range of Motion",
                    desc = "Basic movement assessment for affected areas",
                    eta = "~3 minutes",
                    icon = Icons.Filled.Timeline,
                )
                Spacer(Modifier.height(12.dp))
                BaselineItem(
                    title = "Grip Strength",
                    desc = "Measure baseline hand and forearm strength",
                    eta = "~1 minute",
                    icon = Icons.Filled.Fingerprint,
                )
                Spacer(Modifier.height(12.dp))
                BaselineItem(
                    title = "Resting Vitals",
                    desc = "Heart rate, SpO₂, and baseline effort level",
                    eta = "~2 minutes",
                    icon = Icons.Filled.Favorite,
                )

                Spacer(Modifier.height(16.dp))
                PremiumCard(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        "You can skip this step, but baseline measurements help you track meaningful progress.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Column {
                PrimaryButton(text = "Start Assessment", onClick = onStartAssessment)
                Spacer(Modifier.height(10.dp))
                SecondaryButton(text = "Skip for Now", onClick = onSkip)
            }
        }
    }
}

@Composable
private fun BaselineItem(
    title: String,
    desc: String,
    eta: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
) {
    PremiumCard(modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth()) {
            Column(Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(4.dp))
                Text(desc, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(6.dp))
                Text(eta, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
            }
            Spacer(Modifier.width(12.dp))
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

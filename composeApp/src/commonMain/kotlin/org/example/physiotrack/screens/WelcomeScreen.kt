package org.example.physiotrack.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.example.physiotrack.ui.components.IconBadge
import org.example.physiotrack.ui.components.PrimaryButton
import androidx.compose.foundation.layout.navigationBarsPadding

@Composable
fun WelcomeScreen(
    onGetStarted: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
                .navigationBarsPadding(),
        verticalArrangement = Arrangement.SpaceBetween,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Spacer(Modifier.height(24.dp))
            IconBadge(
                icon = Icons.Filled.Favorite,
                background = MaterialTheme.colorScheme.primaryContainer,
            )
            Spacer(Modifier.height(16.dp))
            Text(
                "Welcome to PhysioTrack",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "Your personal rehabilitation companion for safe, guided at-home physiotherapy exercises",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(24.dp))
            Bullet("AI-guided motion correction", "Real-time feedback using your phone camera")
            Bullet("Connected biometric sensors", "Monitor heart rate, effort, and grip strength")
            Bullet("Safety-first approach", "Automatic alerts when you need to rest")
        }

        Column {
            PrimaryButton(text = "Get Started", onClick = onGetStarted)
            Spacer(Modifier.height(10.dp))
            Text(
                "This app does not provide medical diagnosis",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.align(Alignment.CenterHorizontally),
            )
        }
    }
}

@Composable
private fun Bullet(title: String, subtitle: String) {
    Column(modifier = Modifier.padding(vertical = 10.dp)) {
        Text(title, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(2.dp))
        Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

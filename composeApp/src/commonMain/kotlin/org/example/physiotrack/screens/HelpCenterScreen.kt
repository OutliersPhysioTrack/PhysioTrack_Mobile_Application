@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package org.example.physiotrack.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Email
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.example.physiotrack.ui.components.PremiumCard
import org.example.physiotrack.ui.components.PremiumCardClickable
import androidx.compose.foundation.layout.width

@Composable
fun HelpCenterScreen(
    onBack: () -> Unit,
    onContactEmail: () -> Unit = {},
    onContactCall: () -> Unit = {},
    onContactChat: () -> Unit = {},
) {
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Help Center", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(top = 14.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                "Frequently Asked Questions",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )

            FaqCard(
                q = "Why does my camera session not detect my full body?",
                a = "Pose tracking needs your full body to be visible and well-lit. Try stepping back, improving lighting, and avoiding extreme angles."
            )
            FaqCard(
                q = "How is adherence calculated?",
                a = "Adherence is the ratio of completed sessions to scheduled sessions in a given week or program period."
            )
            FaqCard(
                q = "Can my therapist adjust my program remotely?",
                a = "Yes. Your therapist can update your plan through the web dashboard. Updates appear in the app after sync."
            )
            FaqCard(
                q = "Is this app a medical diagnosis tool?",
                a = "No. It helps track rehabilitation progress and provides guidance, but does not replace clinical assessment."
            )

            Spacer(Modifier.height(8.dp))

            Text(
                "Contact Support",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )

            PremiumCardClickable(modifier = Modifier.fillMaxWidth(), onClick = onContactEmail) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Filled.Email, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Email Customer Service", fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.height(2.dp))
                        Text("support@physiohome.example", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            PremiumCardClickable(modifier = Modifier.fillMaxWidth(), onClick = onContactCall) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Filled.Call, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Call Hospital Support", fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.height(2.dp))
                        Text("+62 21 0000 0000", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            PremiumCardClickable(modifier = Modifier.fillMaxWidth(), onClick = onContactChat) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Filled.Chat, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Chat With Support", fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.height(2.dp))
                        Text("Typically replies within 24–48 hours", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            PremiumCard(modifier = Modifier.fillMaxWidth()) {
                Text(
                    "For urgent symptoms or emergencies, contact your local emergency services immediately.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(14.dp)
                )
            }
        }
    }
}

@Composable
private fun FaqCard(q: String, a: String) {
    PremiumCard(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp)) {
            Text(q, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(6.dp))
            Text(a, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

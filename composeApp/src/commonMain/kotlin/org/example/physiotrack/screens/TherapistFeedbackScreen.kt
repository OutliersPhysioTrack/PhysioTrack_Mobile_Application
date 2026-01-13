@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package org.example.physiotrack.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.example.physiotrack.ui.components.PremiumCard
import org.example.physiotrack.network.AppNetwork
import org.example.physiotrack.network.NoteDto
import kotlinx.coroutines.launch


@Composable
fun TherapistFeedbackScreen(
    onBack: () -> Unit,
) {
    val cfg = AppNetwork.config
    val scope = rememberCoroutineScope()
    var notes by remember { mutableStateOf<List<NoteDto>>(emptyList()) }
    var loadErr by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(cfg.baseUrl, cfg.patientId) {
        val pid = cfg.patientId.trim()
        if (pid.isBlank()) {
            notes = emptyList(); loadErr = null
            return@LaunchedEffect
        }
        val resp = runCatching { AppNetwork.api.listNotes(pid) }
        notes = resp.getOrNull().orEmpty()
        loadErr = resp.exceptionOrNull()?.message
    }

    fun markSeenAndReload(noteId: String) {
        scope.launch {
            val pid = cfg.patientId.trim()
            if (pid.isBlank()) return@launch
            runCatching { AppNetwork.api.markNoteSeen(noteId) }
            val resp = runCatching { AppNetwork.api.listNotes(pid) }
            notes = resp.getOrNull().orEmpty()
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Therapist Feedback", fontWeight = FontWeight.SemiBold) },
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
        ) {
            // header subtitle
            Text(
                "Asynchronous reviews & notes",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 20.dp)
            )
            Spacer(Modifier.height(12.dp))

            // Blue info banner (mirip desain)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFEAF1FF))
                    .padding(horizontal = 20.dp, vertical = 14.dp)
            ) {
                Row(verticalAlignment = Alignment.Top) {
                    Icon(
                        imageVector = Icons.Filled.ChatBubbleOutline,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text("Therapist Review Process", fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "Your therapist reviews your session data and provides feedback within 24–48 hours. This is not real-time messaging.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            Spacer(Modifier.height(14.dp))

            Column(
                modifier = Modifier.padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                fun fmtDate(iso: String): String {
                    // FastAPI returns ISO string; keep it human-readable without locale dependencies.
                    return iso.substringBefore('T')
                }

                if (notes.isEmpty()) {
                    PremiumCard(modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(14.dp)) {
                            Text("No feedback yet", fontWeight = FontWeight.SemiBold)
                            Spacer(Modifier.height(4.dp))
                            Text(
                                loadErr?.let { "Unable to load notes: $it" }
                                    ?: "Your therapist notes will appear here after reviewing your sessions.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    notes.forEach { n ->
                        NoteCard(
                            title = n.title?.takeIf { it.isNotBlank() } ?: "Therapist note",
                            therapist = n.therapist_id?.let { "Therapist: ${it.take(8)}" } ?: "Therapist",
                            date = fmtDate(n.created_at),
                            isNew = n.is_new,
                            body = n.body,
                            onMarkRead = if (n.is_new) ({ markSeenAndReload(n.note_id) }) else null,
                        )
                    }
                }

                // Disclaimer card
                PremiumCard(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(10.dp))
                        Text(
                            "This feedback is for educational purposes and does not replace in-person clinical assessment. Contact your therapist directly for urgent concerns.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Request follow-up card
                PremiumCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text("Request Follow-up Appointment", fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "Schedule an in-person or video session",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(Modifier.height(18.dp))
            }
        }
    }
}

/* ---------------- internal UI blocks ---------------- */

private data class Metric(val title: String, val value: String, val color: Color)

@Composable
private fun NoteCard(
    title: String,
    therapist: String,
    date: String,
    isNew: Boolean,
    body: String,
    onMarkRead: (() -> Unit)? = null,
) {
    PremiumCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(title, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(6.dp))
                    Text(
                        therapist,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(Modifier.width(10.dp))

                Column(horizontalAlignment = Alignment.End) {
                    if (isNew) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(999.dp))
                                .background(MaterialTheme.colorScheme.primary)
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(
                                "New",
                                color = MaterialTheme.colorScheme.onPrimary,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Filled.CalendarMonth,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            date,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(Modifier.height(12.dp))
            Text(body)

            if (onMarkRead != null) {
                Spacer(Modifier.height(12.dp))
                Text(
                    text = "Mark as read",
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { onMarkRead() }
                        .padding(horizontal = 10.dp, vertical = 8.dp)
                )
            }
        }
    }
}

@Composable
private fun FeedbackCard(
    title: String,
    therapist: String,
    date: String,
    isNew: Boolean,
    body: String,
    metrics: List<Metric>,
    nextSteps: List<String>,
) {
    PremiumCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(title, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(6.dp))
                    Text(therapist, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }

                Spacer(Modifier.width(10.dp))

                Column(horizontalAlignment = Alignment.End) {
                    if (isNew) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(999.dp))
                                .background(MaterialTheme.colorScheme.primary)
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text("New", color = MaterialTheme.colorScheme.onPrimary, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                        }
                        Spacer(Modifier.height(8.dp))
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.CalendarMonth, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(date, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            Spacer(Modifier.height(12.dp))
            Text(body)

            if (metrics.isNotEmpty()) {
                Spacer(Modifier.height(14.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    metrics.forEach {
                        MetricMiniCard(
                            modifier = Modifier.weight(1f),
                            title = it.title,
                            value = it.value,
                            valueColor = it.color
                        )
                    }
                }
            }

            Spacer(Modifier.height(14.dp))
            NextStepsCard(steps = nextSteps)
        }
    }
}

@Composable
private fun MetricMiniCard(
    modifier: Modifier,
    title: String,
    value: String,
    valueColor: Color,
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(12.dp)
    ) {
        Column {
            Text(title, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(8.dp))
            Text(value, fontWeight = FontWeight.SemiBold, color = valueColor)
        }
    }
}

@Composable
private fun NextStepsCard(
    steps: List<String>
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(12.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Next Steps", fontWeight = FontWeight.SemiBold)
            steps.forEach { s ->
                Row(verticalAlignment = Alignment.Top) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary)
                            .padding(0.dp)
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        s,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

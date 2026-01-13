package org.example.physiotrack.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.datetime.Clock
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.toLocalDateTime
import org.example.physiotrack.model.Exercise
import org.example.physiotrack.network.AppNetwork
import org.example.physiotrack.network.SessionDto
import org.example.physiotrack.ui.components.IconBadge
import org.example.physiotrack.ui.components.PremiumCard
import org.example.physiotrack.ui.components.PremiumCardClickable
import org.example.physiotrack.util.parseInstantOrNull

@Composable
fun TrainScreen(
    onOpenExerciseLibrary: () -> Unit,
    onOpenExerciseDetail: (String) -> Unit,
    onOpenGripTest: () -> Unit,
    onStartTraining: (String) -> Unit,
    exercises: List<Exercise>,
) {
    val cfg = AppNetwork.config
    val tz = TimeZone.currentSystemDefault()
    val today = Clock.System.now().toLocalDateTime(tz).date

    var sessions by remember { mutableStateOf<List<SessionDto>>(emptyList()) }
    var loadErr by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(cfg.baseUrl, cfg.patientId) {
        val pid = cfg.patientId.trim()
        if (pid.isBlank()) {
            sessions = emptyList()
            loadErr = null
            return@LaunchedEffect
        }

        val resp = runCatching { AppNetwork.api.listSessions(pid) }
        val data = resp.getOrNull()
        if (data != null) {
            sessions = data
            loadErr = null
        } else {
            sessions = emptyList()
            loadErr = resp.exceptionOrNull()?.message
        }
    }

    fun fmtWhen(iso: String): String {
        val inst = parseInstantOrNull(iso) ?: return iso
        val dt = inst.toLocalDateTime(tz)
        val time = "%02d:%02d".format(dt.hour, dt.minute)
        return when (dt.date) {
            today -> "Today, $time"
            today.minus(DatePeriod(days = 1)) -> "Yesterday, $time"
            else -> "${dt.date} $time"
        }
    }

    fun sessionTitle(s: SessionDto): String {
        val exId = s.exercise_id
        val ex = exId?.let { id -> exercises.firstOrNull { it.id == id } }
        return ex?.title ?: "Session"
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .padding(top = 18.dp, bottom = 28.dp),
    ) {
        Text("Training", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(4.dp))
        Text(
            "Start a session or browse exercises",
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(Modifier.height(18.dp))

        val topCardMinHeight = 110.dp

        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            PremiumCardClickable(
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = topCardMinHeight),
                onClick = onOpenExerciseLibrary
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconBadge(
                        icon = Icons.Filled.List,
                        background = MaterialTheme.colorScheme.primaryContainer,
                    )
                    Spacer(Modifier.width(12.dp))

                    Column(Modifier.weight(1f)) {
                        Text(
                            text = "Exercise\nLibrary",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 2,
                            overflow = TextOverflow.Clip
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = "Browse all exercises",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            PremiumCardClickable(
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = topCardMinHeight),
                onClick = onOpenGripTest
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFFDFF7E7)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.TrendingUp,
                            contentDescription = null,
                            tint = Color(0xFF1E8E3E),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(Modifier.width(12.dp))

                    Column(Modifier.weight(1f)) {
                        Text(
                            text = "Quick\nTest",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 2,
                            overflow = TextOverflow.Clip
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = "Grip strength check",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(22.dp))

        Text("Quick Start", fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(12.dp))

        exercises.forEach { ex ->
            PremiumCardClickable(
                modifier = Modifier.fillMaxWidth(),
                onClick = { onStartTraining(ex.id) }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.PlayArrow,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(Modifier.width(12.dp))

                    Column(Modifier.weight(1f)) {
                        Text(ex.title, fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.height(2.dp))
                        Text(
                            "${ex.durationMin} min  •  ${ex.difficulty}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }

                    Spacer(Modifier.width(12.dp))

                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.PlayArrow,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(Modifier.height(12.dp))
        }

        Spacer(Modifier.height(8.dp))

    }
}

@Composable
private fun RecentSessionCard(
    title: String,
    time: String,
    minutes: Int,
    progress: Float,
) {
    val p = progress.coerceIn(0f, 1f)
    val percent = (p * 100).toInt()

    PremiumCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(title, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(6.dp))
                    Text(
                        time,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.AccessTime,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        "$minutes min",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.14f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(p)
                        .height(6.dp)
                        .clip(RoundedCornerShape(999.dp))
                        .background(Color(0xFF1E8E3E))
                )
            }

            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                Text(
                    "$percent%",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

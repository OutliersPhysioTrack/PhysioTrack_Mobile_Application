package org.example.physiotrack.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.LocalFireDepartment
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.datetime.Clock
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import org.example.physiotrack.network.AppNetwork
import org.example.physiotrack.network.AssignmentDto
import org.example.physiotrack.network.SessionDto
import org.example.physiotrack.ui.components.PremiumCard
import org.example.physiotrack.util.currentStreakDays
import org.example.physiotrack.util.sessionLocalDate
import org.example.physiotrack.util.weekStartMonday

@Composable
fun ProgramScreen(
    onStartTraining: (String) -> Unit,
    exercises: List<org.example.physiotrack.model.Exercise>,
) {
    val cfg = AppNetwork.config
    val tz = TimeZone.currentSystemDefault()
    val today = Clock.System.now().toLocalDateTime(tz).date

    var sessions by remember { mutableStateOf<List<SessionDto>>(emptyList()) }
    var assignments by remember { mutableStateOf<List<AssignmentDto>>(emptyList()) }
    var patientCondition by remember { mutableStateOf<String?>(null) }
    var err by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(cfg.baseUrl, cfg.patientId) {
        val pid = cfg.patientId.trim()
        if (pid.isBlank()) {
            sessions = emptyList()
            assignments = emptyList()
            patientCondition = null
            err = null
            return@LaunchedEffect
        }

        val p = runCatching { AppNetwork.api.getPatient(pid) }
        patientCondition = p.getOrNull()?.primary_condition

        val s = runCatching { AppNetwork.api.listSessions(pid) }
        sessions = s.getOrNull().orEmpty()

        val a = runCatching { AppNetwork.api.listAssignments(pid) }
        assignments = a.getOrNull()
            .orEmpty()
            .filter { (it.status ?: "assigned") != "archived" }

        err = listOfNotNull(p.exceptionOrNull(), s.exceptionOrNull(), a.exceptionOrNull())
            .firstOrNull()
            ?.message
    }

    val sessionDates = sessions
        .mapNotNull { sessionLocalDate(it.started_at, tz) }
        .toSet()

    val currentStreak = currentStreakDays(sessionDates, today)

    val weekStart = weekStartMonday(today)
    val weekEnd = weekStart.plus(DatePeriod(days = 6))

    val sessionsThisWeek = sessions.count { s ->
        val d = sessionLocalDate(s.started_at, tz)
        d != null && d >= weekStart && d <= weekEnd
    }

    val totalSessions = sessions.size

    val doneDatesThisWeek = sessionDates.filter { it >= weekStart && it <= weekEnd }.toSet()

    val weekAdherenceAvgOrNull = run {
        val weekVals = sessions.mapNotNull { s ->
            val d = sessionLocalDate(s.started_at, tz) ?: return@mapNotNull null
            if (d < weekStart || d > weekEnd) return@mapNotNull null
            s.adherence
        }
        if (weekVals.isEmpty()) null else (weekVals.sum() / weekVals.size)
    }

    val weekProgress = (
            weekAdherenceAvgOrNull
                ?: (doneDatesThisWeek.size / 7.0)
            ).coerceIn(0.0, 1.0)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .padding(top = 18.dp, bottom = 24.dp),
    ) {
        // Header
        Text("My Program", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(4.dp))
        Text(
            (patientCondition ?: "Your assigned program"),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(Modifier.height(16.dp))

        // KPI row
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            KpiCard(
                modifier = Modifier.weight(1f),
                icon = Icons.Filled.LocalFireDepartment,
                iconTint = Color(0xFFFF6D3D),
                label = "Streak",
                value = "${currentStreak} days",
            )
            KpiCard(
                modifier = Modifier.weight(1f),
                icon = Icons.Filled.CalendarMonth,
                iconTint = MaterialTheme.colorScheme.primary,
                label = "This Week",
                value = "$sessionsThisWeek",
            )
            KpiCard(
                modifier = Modifier.weight(1f),
                icon = Icons.Filled.TrendingUp,
                iconTint = MaterialTheme.colorScheme.primary,
                label = "Total",
                value = "$totalSessions",
            )
        }

        Spacer(Modifier.height(18.dp))

        // This Week section
        Text("This Week", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(10.dp))

        PremiumCard(modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(14.dp)) {
                val days = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")

                val states = (0..6).map { offset ->
                    val d = weekStart.plus(DatePeriod(days = offset))
                    val done = doneDatesThisWeek.contains(d)
                    when {
                        d == today && done -> DayState.TodayDone
                        d == today -> DayState.Today
                        done -> DayState.Done
                        else -> DayState.Pending
                    }
                }

                WeekDayScroller(days = days, state = states)

                Spacer(Modifier.height(12.dp))

                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "Weekly Progress",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        "${(weekProgress * 100).toInt()}%",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Spacer(Modifier.height(8.dp))
                RoundedProgressBar(progress = weekProgress.toFloat().coerceIn(0f, 1f))
            }
        }

        Spacer(Modifier.height(18.dp))

        // Today's sessions (Assignments)
        Text("Today's Sessions", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(10.dp))

        val todayAssignments = assignments.take(2)
        if (todayAssignments.isEmpty()) {
            PremiumCard(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(14.dp)) {
                    Text("No assigned sessions", fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        err?.let { "Unable to load assignments: $it" } ?: "Create an assignment from the dashboard.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            todayAssignments.forEachIndexed { idx, a ->
                val ex = exercises.firstOrNull { it.id == a.exercise_id }
                val title = ex?.title ?: "Assigned exercise"
                val meta = buildString {
                    val sets = a.sets?.toString() ?: "—"
                    val reps = a.reps?.toString() ?: "—"
                    append("$sets sets • $reps reps")
                    a.notes?.takeIf { it.isNotBlank() }?.let {
                        append("   •   ")
                        append(it.take(18))
                    }
                }
                SessionCard(
                    time = "Today",
                    title = title,
                    meta = meta,
                    onStart = { onStartTraining(a.exercise_id) }
                )
                if (idx != todayAssignments.lastIndex) Spacer(Modifier.height(12.dp))
            }
        }

        Spacer(Modifier.height(18.dp))

        // Recovery suggestion (gap derived from session history)
        val lastSessionDate = sessions.mapNotNull { sessionLocalDate(it.started_at, tz) }.maxOrNull()
        val gapDays = lastSessionDate?.let { today.toEpochDays() - it.toEpochDays() } ?: 0
        if (gapDays >= 1) {
            Spacer(Modifier.height(18.dp))
            Text("Recovery Suggestion", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(10.dp))
            RecoverySuggestionCard(
                title = "${gapDays} day gap since last session",
                body = "Resume with your assigned exercises when you're ready. Consistency matters more than perfection.",
                cta = "Start today"
            )
        }
    }
}

/* ---------------- KPI ---------------- */

@Composable
private fun KpiCard(
    modifier: Modifier,
    icon: ImageVector,
    iconTint: Color,
    label: String,
    value: String,
) {
    PremiumCard(modifier = modifier.height(150.dp)) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 14.dp, horizontal = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(18.dp))
            Spacer(Modifier.height(8.dp))
            Text(
                label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                textAlign = TextAlign.Center,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 14.sp
            )
            Spacer(Modifier.height(8.dp))
            Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        }
    }
}

/* ---------------- This Week scroller ---------------- */

private enum class DayState { Done, Today, TodayDone, Pending }

@Composable
private fun WeekDayScroller(
    days: List<String>,
    state: List<DayState>,
) {
    val scroll = rememberScrollState()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(scroll),
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        days.forEachIndexed { idx, day ->
            val s = state.getOrNull(idx) ?: DayState.Pending
            WeekDayChip(day = day, state = s)
        }
    }
}

@Composable
private fun WeekDayChip(
    day: String,
    state: DayState,
) {
    val ringColor = when (state) {
        DayState.Done -> Color(0xFF34C759)
        DayState.Today -> MaterialTheme.colorScheme.primary
        DayState.TodayDone -> MaterialTheme.colorScheme.primary
        DayState.Pending -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.25f)
    }
    val fillColor = when (state) {
        DayState.Done -> Color(0xFFE7F7EC)
        DayState.Today -> MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
        DayState.TodayDone -> MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
        DayState.Pending -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.08f)
    }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            day,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(8.dp))
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(fillColor)
                .border(2.dp, ringColor, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            if (state == DayState.Done || state == DayState.TodayDone) {
                Icon(
                    imageVector = Icons.Filled.Check,
                    contentDescription = null,
                    tint = ringColor,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

/* ---------------- Progress bar ---------------- */

@Composable
private fun RoundedProgressBar(progress: Float) {
    val p = progress.coerceIn(0f, 1f)
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .height(8.dp)
            .clip(RoundedCornerShape(999.dp))
            .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.12f))
    ) {
        val w = maxWidth * p
        Box(
            modifier = Modifier
                .height(8.dp)
                .width(w)
                .clip(RoundedCornerShape(999.dp))
                .background(MaterialTheme.colorScheme.primary)
        )
    }
}

/* ---------------- Session cards ---------------- */

@Composable
private fun SessionCard(
    time: String,
    title: String,
    meta: String,
    onStart: () -> Unit,
) {
    PremiumCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(time, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(4.dp))
                Text(title, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(6.dp))
                Text(meta, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            Spacer(Modifier.width(12.dp))

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.primary)
                    .padding(horizontal = 14.dp, vertical = 8.dp)
                    .clickable { onStart() },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "Start",
                    color = MaterialTheme.colorScheme.onPrimary,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

/* ---------------- Recovery suggestion ---------------- */

@Composable
private fun RecoverySuggestionCard(
    title: String,
    body: String,
    cta: String,
) {
    val bg = Color(0xFFFFF4D6)
    val border = Color(0xFFE9C978)
    val text = Color(0xFF8A6A1F)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(bg)
            .border(1.dp, border, RoundedCornerShape(14.dp))
            .padding(14.dp)
    ) {
        Row(verticalAlignment = Alignment.Top) {
            Icon(
                imageVector = Icons.Filled.CalendarMonth,
                contentDescription = null,
                tint = text,
                modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.SemiBold, color = text)
                Spacer(Modifier.height(4.dp))
                Text(body, style = MaterialTheme.typography.bodySmall, color = text)
                Spacer(Modifier.height(10.dp))
                Text(
                    cta,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = text
                )
            }
        }
    }
}

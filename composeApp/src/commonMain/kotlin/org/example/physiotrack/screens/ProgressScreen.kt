package org.example.physiotrack.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.ChatBubbleOutline
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.datetime.Clock
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.toLocalDateTime
import org.example.physiotrack.network.AppNetwork
import org.example.physiotrack.network.SessionDto
import org.example.physiotrack.ui.components.PremiumCard
import org.example.physiotrack.ui.components.PremiumCardClickable
import org.example.physiotrack.util.currentStreakDays
import org.example.physiotrack.util.lastNWeeksBuckets
import org.example.physiotrack.util.longestStreakDays
import org.example.physiotrack.util.parseInstantOrNull
import org.example.physiotrack.util.sessionLocalDate
import kotlin.math.roundToInt

@Composable
fun ProgressScreen(
    onOpenTherapistFeedback: () -> Unit,
) {
    val cfg = AppNetwork.config
    val tz = TimeZone.currentSystemDefault()
    val today = Clock.System.now().toLocalDateTime(tz).date

    var sessions by remember { mutableStateOf<List<SessionDto>>(emptyList()) }
    var newFeedbackCount by remember { mutableStateOf(0) }
    var loadErr by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(cfg.baseUrl, cfg.patientId) {
        val pid = cfg.patientId.trim()
        if (pid.isBlank()) {
            sessions = emptyList(); newFeedbackCount = 0; loadErr = null
            return@LaunchedEffect
        }

        val s = runCatching { AppNetwork.api.listSessions(pid) }
        sessions = s.getOrNull().orEmpty()

        val n = runCatching { AppNetwork.api.listNotes(pid, onlyNew = true) }
        newFeedbackCount = n.getOrNull().orEmpty().size

        loadErr = listOfNotNull(s.exceptionOrNull(), n.exceptionOrNull()).firstOrNull()?.message
    }

    val totalSessions = sessions.size

    val sessionDates = sessions.mapNotNull { sessionLocalDate(it.started_at, tz) }.toSet()
    val currentStreak = currentStreakDays(sessionDates, today)
    val longestStreak = longestStreakDays(sessionDates)

    val adherenceWindowDays = 7
    val windowStart = today.minus(DatePeriod(days = adherenceWindowDays - 1))
    val completedDaysInWindow = sessionDates.count { d -> d >= windowStart && d <= today }
    val adherencePct = (completedDaysInWindow.toFloat() / adherenceWindowDays.toFloat()).coerceIn(0f, 1f)
    val adherenceText = "${(adherencePct * 100f).roundToInt()}%"

    val gripTestsChrono: List<Pair<kotlinx.datetime.Instant, Float>> = sessions
        .mapNotNull { s ->
            val inst = parseInstantOrNull(s.started_at) ?: return@mapNotNull null
            val v = s.grip_avg_kg ?: return@mapNotNull null
            inst to v.toFloat()
        }
        .sortedBy { it.first }

    val gripSeries: List<Float> = gripTestsChrono
        .map { it.second }
        .takeLast(12)

    val gripBaselineVal = gripTestsChrono.firstOrNull()?.second
    val gripBestVal = gripTestsChrono.maxOfOrNull { it.second }
    val gripLatestVal = gripTestsChrono.lastOrNull()?.second

    val gripDelta = if (gripBaselineVal != null && gripLatestVal != null) (gripLatestVal - gripBaselineVal) else 0f
    val gripDeltaText = "${if (gripDelta >= 0) "+" else ""}${"%.1f".format(gripDelta)} kg"

    val gripBaselineText = gripBaselineVal?.let { "${"%.1f".format(it)} kg" } ?: "—"
    val bestGripText = gripBestVal?.let { "${"%.1f".format(it)} kg" } ?: "—"

    val weekBuckets = lastNWeeksBuckets(today, 5)
    val weeklyCompleted = weekBuckets.map { b ->
        sessions.count { s ->
            val d = sessionLocalDate(s.started_at, tz) ?: return@count false
            d >= b.start && d <= b.end
        }
    }
    val weeklyPeak = (weeklyCompleted.maxOrNull() ?: 0).coerceAtLeast(1)
    val weekLabels = weekBuckets.map { b -> formatWeekRangeLabel(b.start, b.end) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .padding(top = 18.dp, bottom = 28.dp),
    ) {
        Text(
            "Progress",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
        )
        Spacer(Modifier.height(4.dp))
        Text(
            "Track your rehabilitation journey",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(Modifier.height(18.dp))


        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            BigKpiCard(
                modifier = Modifier.weight(1f),
                icon = Icons.Filled.CalendarMonth,
                iconBg = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                iconTint = MaterialTheme.colorScheme.primary,
                label = "Total Sessions",
                value = "$totalSessions"
            )
            BigKpiCard(
                modifier = Modifier.weight(1f),
                icon = Icons.Filled.TrendingUp,
                iconBg = Color(0xFFDFF7E7),
                iconTint = Color(0xFF1E8E3E),
                label = "Adherence",
                value = adherenceText
            )
        }

        Spacer(Modifier.height(22.dp))

        // ===== GRIP TREND =====
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Grip Strength Trend",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
                modifier = Modifier.weight(1f)
            )
            if (gripTestsChrono.size >= 2) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.TrendingUp,
                        contentDescription = null,
                        tint = Color(0xFF1E8E3E),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = gripDeltaText,
                        color = Color(0xFF1E8E3E),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold
                    )
                }
            }
        }

        Spacer(Modifier.height(10.dp))

        PremiumCard(modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(14.dp)) {
                if (gripSeries.size < 2) {
                    Text(
                        "Not enough quick grip test data yet.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )
                    loadErr?.let {
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "Last error: $it",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center
                        )
                    }
                    Spacer(Modifier.height(10.dp))
                } else {
                    GripLineChart(
                        values = gripSeries,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                    )
                    Spacer(Modifier.height(10.dp))
                    Text(
                        text = "Quick grip test results (kg)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        Spacer(Modifier.height(22.dp))

        // ===== WEEKLY ADHERENCE =====
        Text(
            "Weekly Adherence",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold
        )
        Spacer(Modifier.height(10.dp))

        PremiumCard(modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(14.dp)) {
                WeeklyAdherenceChart(
                    completed = weeklyCompleted,
                    target = weeklyPeak,
                    weekLabels = weekLabels,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(190.dp)
                )

                Spacer(Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    LegendDot(color = Color(0xFF34C759))
                    Spacer(Modifier.width(8.dp))
                    Text("Completed", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.width(18.dp))
                    LegendDot(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.25f))
                    Spacer(Modifier.width(8.dp))
                    Text("Peak", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        Spacer(Modifier.height(22.dp))

        // ===== HIGHLIGHTS =====
        Text(
            "Highlights",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold
        )
        Spacer(Modifier.height(10.dp))

        PremiumCard(modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(14.dp)) {
                StatRow(label = "Current streak", value = "${currentStreak} days")
                Spacer(Modifier.height(10.dp))
                StatRow(label = "Longest streak", value = "${longestStreak} days")
                Spacer(Modifier.height(10.dp))

                StatRow(label = "Grip base line", value = gripBaselineText)
                Spacer(Modifier.height(10.dp))
                StatRow(label = "Best grip", value = bestGripText)
            }
        }

        Spacer(Modifier.height(22.dp))

        // ===== THERAPIST FEEDBACK =====
        Text(
            "Therapist Feedback",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold
        )
        Spacer(Modifier.height(10.dp))

        TherapistFeedbackEntryCard(
            count = newFeedbackCount,
            onClick = onOpenTherapistFeedback
        )

        Spacer(Modifier.height(10.dp))
    }
}

/* ---------------- Helpers ---------------- */

private fun formatWeekRangeLabel(start: kotlinx.datetime.LocalDate, end: kotlinx.datetime.LocalDate): String {
    val s = "${start.monthNumber}/${start.dayOfMonth}"
    val e = "${end.monthNumber}/${end.dayOfMonth}"
    return "$s - $e"
}

/* ---------------- KPI big card ---------------- */

@Composable
private fun BigKpiCard(
    modifier: Modifier,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconBg: Color,
    iconTint: Color,
    label: String,
    value: String,
) {
    PremiumCard(modifier = modifier) {
        Column(Modifier.padding(16.dp)) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(iconBg),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(20.dp))
            }

            Spacer(Modifier.height(12.dp))
            Text(
                label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(6.dp))
            Text(
                value,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
            )
        }
    }
}

/* ---------------- Weekly adherence chart (Completed vs Target) ---------------- */

@Composable
private fun WeeklyAdherenceChart(
    completed: List<Int>,
    target: Int,
    weekLabels: List<String>,
    modifier: Modifier = Modifier
) {
    val yMax = target.coerceAtLeast((completed.maxOrNull() ?: target)).coerceAtLeast(1)
    val n = completed.size.coerceAtMost(weekLabels.size)

    Row(modifier = modifier) {
        val ticks: List<Int> = if (yMax <= 1) listOf(yMax, 0) else listOf(yMax, ((yMax + 1) / 2), 0)

        Column(
            modifier = Modifier
                .width(24.dp)
                .height(170.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.End
        ) {
            ticks.forEach { t ->
                Text(
                    "$t",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(Modifier.width(8.dp))

        Column(Modifier.weight(1f)) {
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(145.dp)
            ) {
                val w = size.width
                val h = size.height
                if (n <= 0) return@Canvas

                val gap = 14f
                val barW = ((w - gap * (n - 1)) / n).coerceAtLeast(10f)

                val trackCol = Color(0xFFE8E8E8).copy(alpha = 0.45f)
                val targetCol = Color(0xFFBDBDBD).copy(alpha = 0.30f)
                val doneCol = Color(0xFF34C759)

                for (i in 0 until n) {
                    val x = i * (barW + gap)
                    drawRoundRect(
                        color = trackCol,
                        topLeft = Offset(x, 0f),
                        size = Size(barW, h),
                        cornerRadius = CornerRadius(12f, 12f)
                    )
                }

                for (i in 0 until n) {
                    val x = i * (barW + gap)
                    val tH = (target.toFloat() / yMax.toFloat()).coerceIn(0f, 1f) * h
                    val cH = (completed[i].toFloat() / yMax.toFloat()).coerceIn(0f, 1f) * h

                    drawRoundRect(
                        color = targetCol,
                        topLeft = Offset(x, h - tH),
                        size = Size(barW, tH),
                        cornerRadius = CornerRadius(12f, 12f)
                    )
                    drawRoundRect(
                        color = doneCol,
                        topLeft = Offset(x, h - cH),
                        size = Size(barW, cH),
                        cornerRadius = CornerRadius(12f, 12f)
                    )
                }

                drawLine(
                    color = Color(0xFFBDBDBD).copy(alpha = 0.65f),
                    start = Offset(0f, h),
                    end = Offset(w, h),
                    strokeWidth = 1.4f
                )
            }

            Spacer(Modifier.height(8.dp))

            BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                val perBar: Dp = if (n > 0) maxWidth / n else maxWidth
                val labelW = (perBar - 8.dp).coerceAtLeast(28.dp)

                Row(modifier = Modifier.fillMaxWidth()) {
                    for (i in 0 until n) {
                        WeekRangeLabel(
                            label = weekLabels[i],
                            maxWidth = labelW,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun WeekRangeLabel(
    label: String,
    maxWidth: Dp,
    modifier: Modifier = Modifier
) {
    val parts = label.split(" - ")
    val start = parts.getOrNull(0).orEmpty()
    val end = parts.getOrNull(1).orEmpty()

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        AutoSizeSingleLineText(
            text = start,
            maxWidth = maxWidth,
            style = MaterialTheme.typography.labelSmall
        )
        if (end.isNotBlank()) {
            AutoSizeSingleLineText(
                text = "- $end",
                maxWidth = maxWidth,
                style = MaterialTheme.typography.labelSmall
            )
        }
    }
}

@Composable
private fun AutoSizeSingleLineText(
    text: String,
    maxWidth: Dp,
    style: TextStyle,
    maxSp: Float = 12f,
    minSp: Float = 8f,
    stepSp: Float = 0.5f
) {
    val measurer = rememberTextMeasurer()
    val maxPx = with(androidx.compose.ui.platform.LocalDensity.current) { maxWidth.toPx() }

    var size = maxSp
    while (size > minSp) {
        val result = measurer.measure(
            text = AnnotatedString(text),
            style = style.copy(fontSize = size.sp)
        )
        if (result.size.width <= maxPx) break
        size -= stepSp
    }

    Text(
        text = text,
        style = style.copy(fontSize = size.sp),
        maxLines = 1,
        softWrap = false,
        overflow = TextOverflow.Ellipsis,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

@Composable
private fun LegendDot(color: Color) {
    Box(
        modifier = Modifier
            .size(10.dp)
            .clip(CircleShape)
            .background(color)
    )
}

@Composable
private fun StatRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            value,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

/* ---------------- Therapist Feedback entry card ---------------- */

@Composable
private fun TherapistFeedbackEntryCard(
    count: Int,
    onClick: () -> Unit,
) {
    PremiumCardClickable(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick
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
                    imageVector = Icons.Filled.ChatBubbleOutline,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(Modifier.width(12.dp))

            Column(Modifier.weight(1f)) {
                Text("Therapist Feedback", fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold)
                Spacer(Modifier.height(2.dp))
                Text(
                    "$count new reviews",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "$count",
                    color = MaterialTheme.colorScheme.onPrimary,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                )
            }
        }
    }
}

/* ---------------- Grip line chart (simple) ---------------- */

@Composable
private fun GripLineChart(
    values: List<Float>,
    modifier: Modifier = Modifier,
) {
    val grid = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.18f)
    val axis = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f)
    val line = MaterialTheme.colorScheme.primary

    val minY = (values.minOrNull() ?: 0f)
    val maxY = (values.maxOrNull() ?: 1f)
    val pad = 1.2f
    val yMin = minY - pad
    val yMax = maxY + pad

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        val left = 12f
        val right = 12f
        val top = 12f
        val bottom = 18f

        val plotW = w - left - right
        val plotH = h - top - bottom

        val gridLines = 4
        for (i in 0..gridLines) {
            val y = top + (plotH * i / gridLines)
            drawLine(
                color = grid,
                start = Offset(left, y),
                end = Offset(left + plotW, y),
                strokeWidth = 1.2f
            )
        }

        drawLine(
            color = axis,
            start = Offset(left, top + plotH),
            end = Offset(left + plotW, top + plotH),
            strokeWidth = 1.4f
        )

        if (values.size < 2) return@Canvas

        fun xAt(i: Int): Float = left + plotW * (i.toFloat() / (values.size - 1).toFloat())
        fun yAt(v: Float): Float {
            val t = ((v - yMin) / (yMax - yMin)).coerceIn(0f, 1f)
            return top + plotH * (1f - t)
        }

        val pts = values.mapIndexed { i, v -> Offset(xAt(i), yAt(v)) }

        for (i in 0 until pts.lastIndex) {
            drawLine(
                color = line,
                start = pts[i],
                end = pts[i + 1],
                strokeWidth = 3.5f,
                cap = StrokeCap.Round
            )
        }

        pts.forEach {
            drawCircle(color = line, radius = 5.5f, center = it)
        }
    }
}

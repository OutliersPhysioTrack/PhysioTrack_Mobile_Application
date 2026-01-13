package org.example.physiotrack.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.PanTool
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt
import kotlinx.coroutines.delay
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.example.physiotrack.network.AiLatestDto
import org.example.physiotrack.network.AppNetwork
import org.example.physiotrack.network.LatestReadingDto
import org.example.physiotrack.network.computeDeviceHealth
import org.example.physiotrack.ui.PhysioTokens
import org.example.physiotrack.ui.components.DeviceBanner
import org.example.physiotrack.ui.components.PlanHeroCard
import org.example.physiotrack.ui.components.PremiumCard
import org.example.physiotrack.ui.components.VitalMiniCard

@Composable
fun HomeScreen(
    onOpenExerciseLibrary: () -> Unit,
    exercises: List<org.example.physiotrack.model.Exercise>,
) {
    var showAllSensors by rememberSaveable { mutableStateOf(false) }

    val cfg = AppNetwork.config
    var patientName by remember { mutableStateOf("—") }
    var latest by remember { mutableStateOf<List<LatestReadingDto>>(emptyList()) }
    var online by remember { mutableStateOf(false) }
    var needsCalibration by remember { mutableStateOf(false) }
    var lastSeenText by remember { mutableStateOf<String?>(null) }


    var greeting by remember { mutableStateOf(greetingByHour(currentHour())) }
    LaunchedEffect(Unit) {
        while (true) {
            greeting = greetingByHour(currentHour())
            delay(60_000) // update per menit (ringan)
        }
    }

    var assignments by remember { mutableStateOf(emptyList<org.example.physiotrack.network.AssignmentDto>()) }
    var assignmentsErr by remember { mutableStateOf<String?>(null) }

    var aiSnap by remember { mutableStateOf<AiLatestDto?>(null) }

    LaunchedEffect(cfg.baseUrl, cfg.deviceId) {
        while (true) {
            val data = runCatching { AppNetwork.api.latestReadings(cfg.deviceId) }
                .getOrNull()
                .orEmpty()

            latest = data
            val health = computeDeviceHealth(data)
            online = health.isOnline
            needsCalibration = health.needsCalibration
            lastSeenText = health.lastSeen?.toString()

            delay(2000)
        }
    }

    LaunchedEffect(cfg.baseUrl, cfg.deviceId) {
        while (true) {
            val did = cfg.deviceId.trim()
            aiSnap =
                if (did.isNotBlank()) runCatching { AppNetwork.api.latestAi(did) }.getOrNull()
                else null
            delay(2000)
        }
    }

    LaunchedEffect(cfg.baseUrl, cfg.patientId) {
        val pid = cfg.patientId.trim()
        if (pid.isBlank()) {
            patientName = "—"
            return@LaunchedEffect
        }
        val p = runCatching { AppNetwork.api.getPatient(pid) }.getOrNull()
        if (p != null) patientName = p.name
    }

    LaunchedEffect(cfg.baseUrl, cfg.patientId) {
        val pid = cfg.patientId.trim()
        if (pid.isBlank()) {
            assignments = emptyList()
            return@LaunchedEffect
        }
        val resp = runCatching { AppNetwork.api.listAssignments(pid) }
        assignments = resp.getOrNull().orEmpty().filter { (it.status ?: "assigned") != "archived" }
        assignmentsErr = resp.exceptionOrNull()?.message
    }

    fun metricValue(metric: String): String {
        val r = latest.firstOrNull { it.metric == metric }
        val v = r?.value
        return if (v == null || v.isNaN()) "—" else {
            val unit = r.unit?.let { " $it" } ?: ""
            "${"%.2f".format(v)}$unit"
        }
    }

    val sensorCardHeight = 112.dp

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .padding(top = 18.dp, bottom = 24.dp),
    ) {
        Text(
            greeting,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            patientName,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )

        Spacer(Modifier.height(16.dp))

        val bannerTitle = buildString {
            append("PhysioTrack device")
            append(if (online) " • Connected" else " • Not Connected")
            if (needsCalibration) append(" • Needs calibration")
        }
        DeviceBanner(title = bannerTitle, connected = online)

        Spacer(Modifier.height(22.dp))
        Text("Today's Plan", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(12.dp))
        val first = assignments.firstOrNull()
        val ex = first?.let { a -> exercises.firstOrNull { it.id == a.exercise_id } }
        val title = ex?.title ?: if (assignments.isNotEmpty()) "Assigned exercise" else "No assigned exercises"
        val subtitle = when {
            ex != null -> (first?.notes?.takeIf { it.isNotBlank() } ?: "Start your next session")
            assignmentsErr != null -> "Unable to load plan: ${assignmentsErr}"
            else -> "Create an assignment from the dashboard"
        }
        val meta = first?.let { a ->
            val sets = a.sets?.toString() ?: "—"
            val reps = a.reps?.toString() ?: "—"
            "$sets sets • $reps reps"
        } ?: "—"
        PlanHeroCard(
            title = title,
            subtitle = subtitle,
            meta = meta,
            icon = Icons.Filled.PlayArrow,
        )

        Spacer(Modifier.height(24.dp))
        Text(
            "AI Prediction",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(Modifier.height(12.dp))
        // =========================
        // AI Health Prediction
        // =========================
        AiHealthPredictionCard(snap = aiSnap)

        Spacer(Modifier.height(18.dp))

        // =========================
        // Quick Vitals
        // =========================
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Quick Vitals",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = if (showAllSensors) "Hide" else "View All",
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.clickable { showAllSensors = !showAllSensors }
            )
        }

        Spacer(Modifier.height(12.dp))

        fun readValue(metric: String): LatestReadingDto? = latest.firstOrNull { it.metric == metric }
        fun fmt(metric: String, suffix: String = "", decimals: Int = 0): String {
            val v = readValue(metric)?.value
            if (v == null || v.isNaN()) return "—"
            val shown = when (decimals) {
                0 -> v.roundToInt().toString()
                1 -> ((v * 10).roundToInt() / 10.0).toString()
                2 -> ((v * 100).roundToInt() / 100.0).toString()
                else -> v.toString()
            }
            return shown + suffix
        }

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            VitalMiniCard(
                modifier = Modifier.weight(1f).height(sensorCardHeight),
                icon = Icons.Filled.Favorite, // HR
                badgeBg = PhysioTokens.DangerSoft,
                label = "Heart Rate",
                value = fmt("HR", " bpm", 0),
            )
            VitalMiniCard(
                modifier = Modifier.weight(1f).height(sensorCardHeight),
                icon = Icons.Filled.WaterDrop, // SpO2
                badgeBg = PhysioTokens.PrimarySoft,
                label = "SpO₂",
                value = fmt("SPO2", "%", 0),
            )
        }

        Spacer(Modifier.height(12.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            VitalMiniCard(
                modifier = Modifier.weight(1f).height(sensorCardHeight),
                icon = Icons.Filled.ShowChart,
                badgeBg = ColorSoftGreen,
                label = "ECG",
                value = fmt("ECG", "", 2),
            )
            VitalMiniCard(
                modifier = Modifier.weight(1f).height(sensorCardHeight),
                icon = Icons.Filled.Thermostat, // DS18B20
                badgeBg = ColorSoftOrange,
                label = "Body Temperature",
                value = fmt("TEMP_DS18B20", "°C", 1),
            )
        }


        if (showAllSensors) {
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                VitalMiniCard(
                    modifier = Modifier.weight(1f).height(sensorCardHeight),
                    icon = Icons.Filled.Thermostat, // DHT Temp
                    badgeBg = ColorSoftOrange,
                    label = "Room Temperature",
                    value = fmt("DHT_TEMP", "°C", 1),
                )
                VitalMiniCard(
                    modifier = Modifier.weight(1f).height(sensorCardHeight),
                    icon = Icons.Filled.WaterDrop, // DHT Hum
                    badgeBg = PhysioTokens.PrimarySoft,
                    label = "Room Humidity",
                    value = fmt("DHT_HUM", "%", 0),
                )
            }

            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                VitalMiniCard(
                    modifier = Modifier.weight(1f).height(sensorCardHeight),
                    icon = Icons.Filled.WaterDrop,
                    badgeBg = PhysioTokens.PrimarySoft,
                    label = "Sweat",
                    value = fmt("GSR", "", 0),
                )
                VitalMiniCard(
                    modifier = Modifier.weight(1f).height(sensorCardHeight),
                    icon = Icons.Filled.PanTool,
                    badgeBg = ColorSoftGreen,
                    label = "Grip Strength",
                    value = fmt("LOADCELL_KG", " kg", 1),
                )
            }
        }

        Spacer(Modifier.height(22.dp))

    }
}

private fun currentHour(): Int =
    Clock.System.now()
        .toLocalDateTime(TimeZone.currentSystemDefault())
        .hour

private fun greetingByHour(hour: Int): String {
    return when (hour) {
        in 5..10 -> "Good Morning"
        in 11..14 -> "Good Afternoon"
        in 15..17 -> "Good Evening"
        else -> "Good Evening"
    }
}


private enum class AiHealthStatus { SAFE, WARNING, DANGER, UNKNOWN }

private fun normalizeAiStatus(labelRaw: String?): AiHealthStatus {
    val norm = labelRaw?.trim()?.lowercase().orEmpty()
    return when (norm) {
        "safe" -> AiHealthStatus.SAFE
        "warning" -> AiHealthStatus.WARNING
        "danger" -> AiHealthStatus.DANGER
        else -> AiHealthStatus.UNKNOWN
    }
}

private fun normalizeConfToPct(raw: Double?): Int {
    if (raw == null) return 0
    val v = raw.toFloat()
    val pct = if (v in 0f..1f) (v * 100f) else v
    return pct.coerceIn(0f, 100f).roundToInt()
}

@Composable
private fun AiHealthPredictionCard(
    snap: AiLatestDto?,
    title: String = "AI Health Prediction",
) {
    val labelRaw = snap?.aiLabel ?: snap?.label
    val confRaw = snap?.aiConf ?: snap?.conf

    val status = normalizeAiStatus(labelRaw)
    val confPct = normalizeConfToPct(confRaw)

    val statusText = when (status) {
        AiHealthStatus.SAFE -> "SAFE"
        AiHealthStatus.WARNING -> "WARNING"
        AiHealthStatus.DANGER -> "DANGER"
        AiHealthStatus.UNKNOWN ->
            (labelRaw?.trim()?.takeIf { it.isNotBlank() } ?: "UNKNOWN").uppercase()
    }

    val (cTop, cBottom) = when (status) {
        AiHealthStatus.SAFE -> Color(0xFF22C55E) to Color(0xFF16A34A)
        AiHealthStatus.WARNING -> Color(0xFFFBBF24) to Color(0xFFF59E0B)
        AiHealthStatus.DANGER -> Color(0xFFEF4444) to Color(0xFFDC2626)
        AiHealthStatus.UNKNOWN -> MaterialTheme.colorScheme.primary to MaterialTheme.colorScheme.primary.copy(alpha = 0.85f)
    }

    val shape = RoundedCornerShape(20.dp)
    val bg = Brush.verticalGradient(listOf(cTop, cBottom))

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(bg)
            .padding(16.dp)
    ) {
        Canvas(modifier = Modifier.matchParentSize()) {
            drawCircle(
                color = Color.White.copy(alpha = 0.14f),
                radius = size.minDimension * 0.42f,
                center = Offset(size.width * 0.92f, size.height * 0.10f),
            )
            drawCircle(
                color = Color.White.copy(alpha = 0.10f),
                radius = size.minDimension * 0.55f,
                center = Offset(size.width * 0.10f, size.height * 1.05f),
            )
        }

        Column {
            Text(
                title,
                color = Color.White,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold
            )
            Spacer(Modifier.height(2.dp))
            Text(

                "Current status & confidence snapshot",
                color = Color.White.copy(alpha = 0.88f),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(Modifier.height(14.dp))

            val panelShape = RoundedCornerShape(16.dp)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(panelShape)
                    .background(Color.White.copy(alpha = 0.16f))
                    .padding(horizontal = 14.dp, vertical = 12.dp)
            ) {
                Column {
                    Text(
                        "Current Status",
                        color = Color.White.copy(alpha = 0.92f),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        statusText,
                        color = Color.White,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Black
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "Confidence: $confPct%",
                        color = Color.White.copy(alpha = 0.92f),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@Composable
private fun QuickGripTestCard(
    lastKg: Double?,
    deltaKg: Double?,
) {
    PremiumCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(PhysioTokens.PrimarySoft),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.PanTool,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text("Quick Grip Test", fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(2.dp))
                Text(
                    "Last: ${lastKg?.let { "%.1f".format(it) } ?: "—"} kg",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (deltaKg != null) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.TrendingUp,
                        contentDescription = null,
                        tint = Color(0xFF2E7D32),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "+${"%.1f".format(deltaKg)} kg",
                        color = Color(0xFF2E7D32),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@Composable
private fun ProgressRing(
    progress: Float,
    percent: Int,
    modifier: Modifier = Modifier,
) {
    val trackColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.18f)
    val ringColor = MaterialTheme.colorScheme.primary
    val textStyle = MaterialTheme.typography.labelSmall
    val textColor = MaterialTheme.colorScheme.onSurface

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val stroke = 6.dp.toPx()
            val diameter = size.minDimension
            val inset = stroke / 2f
            val arcSize = androidx.compose.ui.geometry.Size(diameter - stroke, diameter - stroke)

            drawArc(
                color = trackColor,
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = androidx.compose.ui.geometry.Offset(inset, inset),
                size = arcSize,
                style = Stroke(width = stroke, cap = StrokeCap.Round)
            )

            drawArc(
                color = ringColor,
                startAngle = -90f,
                sweepAngle = 360f * progress.coerceIn(0f, 1f),
                useCenter = false,
                topLeft = androidx.compose.ui.geometry.Offset(inset, inset),
                size = arcSize,
                style = Stroke(width = stroke, cap = StrokeCap.Round)
            )
        }

        Text(
            text = "$percent%",
            style = textStyle,
            fontWeight = FontWeight.SemiBold,
            color = textColor
        )
    }
}

@Composable
private fun RoundedProgressBar(
    progress: Float,
    modifier: Modifier = Modifier,
) {
    val p = progress.coerceIn(0f, 1f)

    BoxWithConstraints(
        modifier = modifier
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

private val ColorSoftGreen = Color(0xFFE9F8EE)
private val ColorSoftOrange = Color(0xFFFFF4DB)

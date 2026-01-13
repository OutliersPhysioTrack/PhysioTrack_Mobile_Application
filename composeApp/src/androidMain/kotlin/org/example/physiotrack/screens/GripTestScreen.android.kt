@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package org.example.physiotrack.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlin.math.max
import org.example.physiotrack.network.AppNetwork
import org.example.physiotrack.training.mediapipe.audio.GripTestAudioCue
import org.example.physiotrack.ui.components.PremiumCard
import org.example.physiotrack.ui.components.PremiumCardClickable

private enum class GripPhase { IDLE, COUNTDOWN, MEASURING, DONE }

@Composable
actual fun GripTestScreen(
    onBack: () -> Unit,
    onFinish: () -> Unit,
) {
    val context = LocalContext.current
    val audioCue = remember(context) { GripTestAudioCue(context) }
    DisposableEffect(Unit) {
        onDispose { audioCue.release() }
    }

    val cfg = AppNetwork.config
    val deviceId = cfg.deviceId.trim()

    val attemptsMax = 3
    val measureMs = 3000L
    val pollMs = 150L

    var phase by remember { mutableStateOf(GripPhase.IDLE) }
    var countdown by remember { mutableIntStateOf(0) }
    var attemptIndex by remember { mutableIntStateOf(1) }

    var currentForce by remember { mutableFloatStateOf(0f) }
    var hasSensorData by remember { mutableStateOf(true) }

    var baselineKey by remember { mutableStateOf<String?>(null) }
    var waitingFreshReading by remember { mutableStateOf(false) }

    var bestInAttempt by remember { mutableFloatStateOf(0f) }
    var bestOverall by remember { mutableFloatStateOf(0f) }
    var lastResultText by remember { mutableStateOf("Press Start to begin") }

    LaunchedEffect(phase, cfg.baseUrl, deviceId) {
        if (phase != GripPhase.MEASURING) {
            currentForce = 0f
            waitingFreshReading = false
            return@LaunchedEffect
        }

        if (deviceId.isBlank()) {
            hasSensorData = false
            currentForce = 0f
            waitingFreshReading = false
            return@LaunchedEffect
        }

        waitingFreshReading = true
        currentForce = 0f

        while (phase == GripPhase.MEASURING) {
            val latest = runCatching {
                val items = AppNetwork.api.latestReadings(deviceId, "LOADCELL_KG")
                items.firstOrNull { it.metric == "LOADCELL_KG" }
            }.getOrNull()

            val kg = latest?.value?.toFloat()
            val key = if (latest != null) "${latest.ts}|${latest.value}" else null

            if (kg == null || key == null) {
                hasSensorData = false
                // tetap 0 biar tidak nampilin value lama
                currentForce = 0f
            } else {
                hasSensorData = true

                // Kalau baselineKey kosong (misalnya gagal fetch saat countdown),
                // terima saja reading pertama.
                if (baselineKey.isNullOrBlank()) {
                    baselineKey = key
                    waitingFreshReading = false
                    currentForce = kg.coerceAtLeast(0f)
                } else {
                    // kalau masih nunggu fresh reading, hanya terima kalau key berubah
                    if (waitingFreshReading) {
                        if (key != baselineKey) {
                            waitingFreshReading = false
                            baselineKey = key
                            currentForce = kg.coerceAtLeast(0f)
                        } else {
                            // masih reading lama -> tampilkan 0
                            currentForce = 0f
                        }
                    } else {
                        // sudah running normal
                        baselineKey = key
                        currentForce = kg.coerceAtLeast(0f)
                    }
                }
            }

            delay(pollMs)
        }
    }

    // ===== COUNTDOWN flow =====
    LaunchedEffect(phase, attemptIndex) {
        if (phase != GripPhase.COUNTDOWN) return@LaunchedEffect

        audioCue.reset()

        // sebelum mulai countdown, ambil snapshot reading terakhir sebagai baseline
        baselineKey = runCatching {
            if (deviceId.isBlank()) null
            else {
                val items = AppNetwork.api.latestReadings(deviceId, "LOADCELL_KG")
                val x = items.firstOrNull { it.metric == "LOADCELL_KG" }
                if (x != null) "${x.ts}|${x.value}" else null
            }
        }.getOrNull()

        // reset UI supaya tetap 0 selama countdown
        currentForce = 0f
        bestInAttempt = 0f
        waitingFreshReading = true

        lastResultText = if (deviceId.isBlank()) "Device not set (device_id empty)"
        else "Get ready..."

        countdown = 0

        val isFirstAttempt = (attemptIndex == 1)
        audioCue.playStartAttempt(isFirstAttempt = isFirstAttempt)
        delay(audioCue.msBeforeCountdown(isFirstAttempt = isFirstAttempt))

        countdown = 3
        while (countdown > 0) {
            delay(1000L)
            countdown -= 1
        }
        countdown = 0

        phase = GripPhase.MEASURING
    }

    LaunchedEffect(phase, attemptIndex) {
        if (phase != GripPhase.MEASURING) return@LaunchedEffect

        val start = System.currentTimeMillis()
        while (System.currentTimeMillis() - start < measureMs) {
            // currentForce akan 0 sampai reading baru muncul (fresh)
            bestInAttempt = max(bestInAttempt, currentForce)
            bestOverall = max(bestOverall, bestInAttempt)
            delay(50L)
        }

        lastResultText = "Attempt $attemptIndex done • Peak ${bestInAttempt.toInt()} kg"

        val isLast = attemptIndex >= attemptsMax
        audioCue.playRelease(isLastAttempt = isLast)

        // keluar measuring -> reset live reading agar tidak “nyangkut” value terakhir
        currentForce = 0f
        waitingFreshReading = false

        phase = if (attemptIndex >= attemptsMax) GripPhase.DONE else GripPhase.IDLE
        if (phase == GripPhase.DONE) {
            lastResultText = "Finished • Best ${bestOverall.toInt()} kg"
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Grip Strength Test", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            audioCue.reset()
                            onBack()
                        }
                    ) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            PremiumCard(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Try to beat your best", fontWeight = FontWeight.SemiBold)
                    Text(
                        "Squeeze as hard as you can for ~3 seconds. Do 3 attempts.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(4.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Attempt: $attemptIndex / $attemptsMax", fontWeight = FontWeight.SemiBold)
                        Text("Best: ${bestOverall.toInt()} kg", fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            PremiumCard(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Live reading", fontWeight = FontWeight.SemiBold)

                    val shownForce = if (phase == GripPhase.MEASURING && !waitingFreshReading) currentForce else 0f
                    val p = (shownForce / 60f).coerceIn(0f, 1f)

                    LinearProgressIndicator(
                        progress = { p },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(
                            "Now: ${shownForce.toInt()} kg",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            "Peak: ${bestInAttempt.toInt()} kg",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    if (phase == GripPhase.COUNTDOWN && countdown > 0) {
                        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                            Text(
                                text = countdown.toString(),
                                style = MaterialTheme.typography.displayMedium,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }
                }
            }

            PremiumCard(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Status", fontWeight = FontWeight.SemiBold)
                    Text(
                        when {
                            deviceId.isBlank() -> "No device_id set"
                            !hasSensorData -> "No loadcell data (check MQTT/backend)"
                            phase == GripPhase.MEASURING && waitingFreshReading -> "Waiting for new grip reading..."
                            else -> lastResultText
                        },
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(Modifier.height(6.dp))

            val actionCardHeight = 104.dp

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                PremiumCardClickable(
                    modifier = Modifier
                        .weight(1f)
                        .height(actionCardHeight),
                    onClick = {
                        if (phase == GripPhase.DONE) {
                            audioCue.reset()
                            phase = GripPhase.IDLE
                            attemptIndex = 1
                            bestInAttempt = 0f
                            bestOverall = 0f
                            currentForce = 0f
                            baselineKey = null
                            waitingFreshReading = false
                            lastResultText = "Press Start to begin"
                        } else {
                            if (phase == GripPhase.IDLE) {
                                phase = GripPhase.COUNTDOWN
                            }
                        }
                    }
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Filled.PlayArrow,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = when (phase) {
                                GripPhase.DONE -> "Restart"
                                GripPhase.COUNTDOWN, GripPhase.MEASURING -> "Running..."
                                GripPhase.IDLE -> "Start Attempt"
                            },
                            modifier = Modifier.padding(start = 10.dp),
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                PremiumCardClickable(
                    modifier = Modifier
                        .weight(1f)
                        .height(actionCardHeight),
                    onClick = {
                        when (phase) {
                            GripPhase.DONE -> {
                                audioCue.reset()
                                onFinish()
                            }
                            GripPhase.IDLE -> {
                                if (attemptIndex < attemptsMax) {
                                    attemptIndex += 1
                                    lastResultText = "Ready for attempt $attemptIndex"
                                    currentForce = 0f
                                    baselineKey = null
                                    waitingFreshReading = false
                                    phase = GripPhase.COUNTDOWN
                                }
                            }
                            else -> {}
                        }
                    }
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Filled.CheckCircle,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = if (phase == GripPhase.DONE) "Finish" else "Next Attempt",
                            modifier = Modifier.padding(start = 10.dp),
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}

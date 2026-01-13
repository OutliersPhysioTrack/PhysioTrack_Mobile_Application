@file:OptIn(ExperimentalMaterial3Api::class)

package org.example.physiotrack.training

import android.Manifest
import android.content.pm.PackageManager
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.AspectRatio
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.FileOutputOptions
import androidx.camera.video.Quality
import androidx.camera.video.QualitySelector
import androidx.camera.video.Recorder
import androidx.camera.video.Recording
import androidx.camera.video.VideoCapture
import androidx.camera.video.VideoCapture.withOutput
import androidx.camera.video.VideoRecordEvent
import androidx.camera.view.PreviewView
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.google.mediapipe.tasks.vision.core.RunningMode
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.example.physiotrack.model.Exercise
import org.example.physiotrack.network.AppNetwork
import org.example.physiotrack.network.SessionCreateDto
import org.example.physiotrack.network.SessionPatchDto
import org.example.physiotrack.training.mediapipe.ArmRaiseCounter
import org.example.physiotrack.training.mediapipe.ElbowFlexionCounter
import org.example.physiotrack.training.mediapipe.HipFlexionCounter
import org.example.physiotrack.training.mediapipe.KneeExtensionCounter
import org.example.physiotrack.training.mediapipe.OverlayView
import org.example.physiotrack.training.mediapipe.PoseLandmarkerHelper
import org.example.physiotrack.training.mediapipe.audio.ArmRaiseAudioCue
import org.example.physiotrack.training.mediapipe.audio.HipFlexionAudioCue
import org.example.physiotrack.training.mediapipe.audio.HoldAudioCue
import org.example.physiotrack.training.mediapipe.audio.KneeExtensionAudioCue
import org.example.physiotrack.training.mediapipe.audio.PreStartCountdownCue
import org.example.physiotrack.ui.components.PremiumCard
import org.example.physiotrack.ui.components.PremiumCardClickable
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.concurrent.Executor
import java.util.concurrent.Executors
import kotlin.math.roundToInt

private var AUTO_CLIP_SECONDS: Long = 20

// UTC ISO formatter (API 24 safe)
private fun nowIsoUtc(): String {
    val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
    sdf.timeZone = TimeZone.getTimeZone("UTC")
    return sdf.format(Date())
}

private fun nowEpochMs(): Long = System.currentTimeMillis()

private class SessionSyncController(
    private val context: android.content.Context,
    private val scope: kotlinx.coroutines.CoroutineScope,
    private val mainExecutor: Executor,
    private val exercise: Exercise,
    private val videoCaptureState: androidx.compose.runtime.MutableState<VideoCapture<Recorder>?>,
) {
    var startedAtUtc: String? = null
    var startedAtMs: Long? = null
    var remoteSessionId: String? = null

    var posting: Boolean by mutableStateOf(false)
    var postResult: String? by mutableStateOf(null)

    var clipStatus: String? by mutableStateOf(null)
    private var clipUploaded: Boolean = false
    private var activeRecording: Recording? = null

    private var pendingClipStart: Boolean = false
    private var runningStarted: Boolean = false

    fun resetAll() {
        startedAtUtc = null
        startedAtMs = null
        remoteSessionId = null
        postResult = null
        posting = false

        clipStatus = null
        clipUploaded = false
        pendingClipStart = false
        runningStarted = false
        runCatching { activeRecording?.stop() }
        activeRecording = null
    }

    fun onStartPressed() {
        if (startedAtUtc == null) startedAtUtc = nowIsoUtc()
        if (startedAtMs == null) startedAtMs = nowEpochMs()

        pendingClipStart = true

        val cfg = AppNetwork.config
        val patientId = cfg.patientId.trim()
        if (patientId.isBlank()) {
            postResult = "⚠️ patientId not set (session sync & clip skipped)"
            return
        }

        if (!remoteSessionId.isNullOrBlank()) {
            postResult = "✅ Session ready"
            maybeStartClip(patientId)
            return
        }

        val started = startedAtUtc ?: nowIsoUtc()
        val exerciseIdOrNull =
            if (exercise.id.matches(Regex("[0-9a-fA-F-]{36}"))) exercise.id else null

        val payload = SessionCreateDto(
            patient_id = patientId,
            exercise_id = exerciseIdOrNull,
            started_at = started,
        )

        scope.launch {
            val id = runCatching { AppNetwork.api.createSession(payload) }.getOrNull()
            if (!id.isNullOrBlank()) {
                remoteSessionId = id
                postResult = "✅ Session started"
                maybeStartClip(patientId)
            } else {
                postResult = "⚠️ Start sync failed (clip skipped)"
            }
        }
    }

    fun onRunningBeganAfterCountdown() {
        runningStarted = true
        val patientId = AppNetwork.config.patientId.trim()
        if (patientId.isNotBlank()) {
            maybeStartClip(patientId)
        }
    }

    private fun maybeStartClip(patientId: String) {
        if (!pendingClipStart) return
        if (!runningStarted) return
        val sid = remoteSessionId ?: return
        startAutoClipOnce(sessionId = sid, patientId = patientId)
        pendingClipStart = false
    }

    private fun startAutoClipOnce(sessionId: String, patientId: String) {
        if (clipUploaded) return
        if (activeRecording != null) return

        val videoCapture = videoCaptureState.value ?: run {
            clipStatus = "❌ Camera not ready for recording"
            return
        }


        val outFile = File(context.cacheDir, "auto_${sessionId}_${System.currentTimeMillis()}.mp4")
        val outputOptions = FileOutputOptions.Builder(outFile).build()

        activeRecording = videoCapture.output
            .prepareRecording(context, outputOptions)
            .start(mainExecutor) { event ->
                if (event is VideoRecordEvent.Finalize) {
                    activeRecording = null

                    if (event.error != VideoRecordEvent.Finalize.ERROR_NONE) {
                        //clipStatus = "❌ Recording failed"
                        runCatching { outFile.delete() }
                        return@start
                    }

                    scope.launch {
                        //clipStatus = "Uploading clip…"

                        val bytes = runCatching { outFile.readBytes() }.getOrNull()
                        runCatching { outFile.delete() }

                        if (bytes == null) {
                            //clipStatus = "❌ Upload failed (read file)"
                            return@launch
                        }

                        val resp = runCatching {
                            AppNetwork.api.uploadHighlight(
                                sessionId = sessionId,
                                patientId = patientId,
                                startMs = 0,
                                endMs = (AUTO_CLIP_SECONDS * 1000).toInt(),
                                fileBytes = bytes,
                                fileName = outFile.name,
                            )
                        }.getOrNull()

                        if (resp != null) {
                            clipUploaded = true
                            //clipStatus = "✅ Clip uploaded"
                        } else {
                            //clipStatus = "❌ Upload failed"
                        }
                    }
                }
            }

        scope.launch {
            delay(AUTO_CLIP_SECONDS * 1000)
            runCatching { activeRecording?.stop() }
        }
    }

    fun onFinishPressed(
        setIndex: Int,
        repsDone: Int,
        onFinish: (sessionId: String?, setIndex: Int, repsDone: Int) -> Unit
    ) {
        if (posting) return
        posting = true
        postResult = null

        val cfg = AppNetwork.config
        val patientId = cfg.patientId.trim()

        if (patientId.isBlank()) {
            posting = false
            postResult = "Saved locally (patientId not set)"
            onFinish(null, setIndex, repsDone)
            return
        }

        val started = startedAtUtc ?: nowIsoUtc()
        val ended = nowIsoUtc()

        val endMs = nowEpochMs()
        val startMs = startedAtMs ?: endMs
        val durationSec = ((endMs - startMs) / 1000L).toInt().coerceAtLeast(0)

        val sid = remoteSessionId

        scope.launch {
            var resolvedSessionId: String? = sid

            val ok = if (!sid.isNullOrBlank()) {
                runCatching {
                    AppNetwork.api.patchSession(
                        sessionId = sid,
                        payload = SessionPatchDto(
                            ended_at = ended,
                            duration_sec = durationSec,
                            rep_count = repsDone,
                        )
                    )
                }.getOrNull() == true
            } else {
                val exerciseIdOrNull =
                    if (exercise.id.matches(Regex("[0-9a-fA-F-]{36}"))) exercise.id else null

                val payload = SessionCreateDto(
                    patient_id = patientId,
                    exercise_id = exerciseIdOrNull,
                    started_at = started,
                    ended_at = ended,
                    duration_sec = durationSec,
                    rep_count = repsDone,
                )
                val createdId = runCatching { AppNetwork.api.createSession(payload) }.getOrNull()
                if (!createdId.isNullOrBlank()) {
                    resolvedSessionId = createdId
                    remoteSessionId = createdId
                    true
                } else {
                    resolvedSessionId = null
                    false
                }
            }

            posting = false
            postResult = if (ok) "✅ Session saved" else "❌ Failed to save"
            onFinish(resolvedSessionId, setIndex, repsDone)
        }
    }
}

@Composable
actual fun TrainingSessionScreen(
    exercise: Exercise,
    onBack: () -> Unit,
    onFinish: (sessionId: String?, setIndex: Int, repsDone: Int) -> Unit,
) {
    val context = LocalContext.current

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA)
                    == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted -> hasCameraPermission = granted },
    )

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
                .fillMaxSize()
                .padding(16.dp)
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (!hasCameraPermission) {
                PremiumCard(modifier = Modifier.fillMaxWidth()) {
                    Text("Camera access is required for motion tracking.")
                    Spacer(Modifier.height(8.dp))
                    PremiumCardClickable(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Filled.CameraAlt,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(Modifier.size(10.dp))
                            Text("Grant Camera Permission", fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
                return@Column
            }

            SessionBody(exercise = exercise, onFinish = onFinish)
        }
    }
}


@Composable
private fun SessionBody(
    exercise: Exercise,
    onFinish: (sessionId: String?, setIndex: Int, repsDone: Int) -> Unit,
) {
    val id = exercise.id.lowercase(Locale.US)
    when {
        id == "elbow_flexion" || id.contains("elbow") -> SessionBodyElbowFlexion(exercise = exercise, onFinish = onFinish)
        id == "arm_raise" || id.contains("arm_raise") || id.contains("arm") -> SessionBodyArmRaise(exercise = exercise, onFinish = onFinish)
        id == "hip_flexion" || id.contains("hip") -> SessionBodyHipFlexion(exercise = exercise, onFinish = onFinish)
        id == "knee_extension" || id.contains("knee") -> SessionBodyKneeExtension(exercise = exercise, onFinish = onFinish)
        else -> SessionBodyElbowFlexion(exercise = exercise, onFinish = onFinish)
    }
}

/* =========================================================
 *  SESSION 1: ELBOW FLEXION
 * ========================================================= */
@Composable
private fun SessionBodyElbowFlexion(
    exercise: Exercise,
    onFinish: (sessionId: String?, setIndex: Int, repsDone: Int) -> Unit,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    val mainExecutor: Executor = remember { ContextCompat.getMainExecutor(context) }

    val overlayView = remember { OverlayView(context, null) }

    val videoCaptureState = remember { mutableStateOf<VideoCapture<Recorder>?>(null) }
    val sync = remember(exercise.id) {
        SessionSyncController(
            context = context,
            scope = scope,
            mainExecutor = mainExecutor,
            exercise = exercise,
            videoCaptureState = videoCaptureState,
        )
    }

    // pre-start countdown cue (audio)
    val preStartCue = remember(context) { PreStartCountdownCue(context) }
    DisposableEffect(Unit) { onDispose { preStartCue.release() } }

    // countdown UI state
    var isCountingDown by remember { mutableStateOf(false) }
    var countdownSec by remember { mutableStateOf(0) }      // 5..1
    var countdownText by remember { mutableStateOf("") }    // "SIAP-SIAP" / "MULAI!"

    val audioCue = remember(context) { HoldAudioCue(context) }
    DisposableEffect(Unit) { onDispose { audioCue.release() } }

    val repsPerSet = 10
    val setsMax = 3
    val totalTarget = repsPerSet * setsMax

    var running by remember { mutableStateOf(false) }
    var totalReps by remember { mutableStateOf(0) }
    var feedback by remember { mutableStateOf("Press Start to begin") }

    val currentSet = (totalReps / repsPerSet + 1).coerceAtMost(setsMax)
    val repsInSet = (totalReps % repsPerSet)

    val counter = remember {
        ElbowFlexionCounter(
            side = ElbowFlexionCounter.Side.LEFT,
            flexedThresholdDeg = 70f,
            extendedThresholdDeg = 160f,
            smoothingWindow = 5
        )
    }

    val readyLeadMs = 900L
    val goShowMs = 600L

    LaunchedEffect(isCountingDown) {
        if (!isCountingDown) return@LaunchedEffect

        running = false
        audioCue.reset()
        counter.reset()
        totalReps = 0
        feedback = "Get ready..."
        overlayView.post { overlayView.clear() }

        countdownSec = 0
        countdownText = "SIAP-SIAP"

        preStartCue.start(onGo = { /* optional */ })

        delay(readyLeadMs)

        countdownText = ""
        for (i in 5 downTo 1) {
            countdownSec = i
            delay(1000L)
        }

        countdownSec = 0
        countdownText = "MULAI!"
        delay(goShowMs)

        countdownText = ""
        isCountingDown = false

        running = true
        feedback = "Bend your elbow"
        sync.onRunningBeganAfterCountdown()
    }

    LaunchedEffect(running, isCountingDown) {
        if (!running && !isCountingDown) {
            audioCue.reset()
            counter.reset()
            totalReps = 0
            feedback = "Press Start to begin"
            overlayView.post { overlayView.clear() }
            sync.resetAll()
        } else if (running) {
            audioCue.reset()
        }
    }

    PremiumCard(modifier = Modifier.fillMaxWidth()) {
        Text("Live Camera", fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(8.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(360.dp),
        ) {
            CameraXPreviewElbow(
                modifier = Modifier.fillMaxSize(),
                overlayView = overlayView,
                isRunning = running,
                counter = counter,
                audioCue = audioCue,
                videoCaptureState = videoCaptureState,
                onRepUpdate = { reps, phase ->
                    totalReps = reps

                    feedback = when {
                        reps >= totalTarget -> "Session complete!"
                        phase == ElbowFlexionCounter.Phase.READY_EXTENDED -> "Bend your elbow"
                        phase == ElbowFlexionCounter.Phase.HOLDING -> "Hold 2 seconds"
                        phase == ElbowFlexionCounter.Phase.NEED_EXTEND -> "Straighten your arm"
                        else -> "Find a clear arm position"
                    }

                    if (reps >= totalTarget) running = false
                }
            )

            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { overlayView }
            )

            // countdown overlay
            if (isCountingDown) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    when {
                        countdownSec > 0 -> {
                            Text(
                                text = countdownSec.toString(),
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White,
                                fontSize = MaterialTheme.typography.displayLarge.fontSize * 1.3f
                            )
                        }

                        countdownText.isNotBlank() -> {
                            Text(
                                text = countdownText,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White,
                                fontSize = MaterialTheme.typography.headlineLarge.fontSize * 1.2f
                            )
                        }
                    }
                }
            }
        }

        sync.clipStatus?.let {
            Spacer(Modifier.height(10.dp))
            Text(it, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }

    PremiumCard(modifier = Modifier.fillMaxWidth()) {
        Text("Feedback", fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(6.dp))
        Text(feedback, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(10.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Set: $currentSet/$setsMax", fontWeight = FontWeight.SemiBold)
            Text("Reps: $repsInSet/$repsPerSet", fontWeight = FontWeight.SemiBold)
        }
    }

    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        PremiumCardClickable(
            modifier = Modifier.weight(1f),
            onClick = {
                if (running) {
                    running = false
                } else {
                    if (!isCountingDown) {
                        sync.onStartPressed()
                        isCountingDown = true
                    }
                }
            }
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    if (running) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                )
                Spacer(Modifier.size(10.dp))
                Text(
                    when {
                        running -> "Pause"
                        isCountingDown -> "Starting..."
                        else -> "Start"
                    },
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        PremiumCardClickable(
            modifier = Modifier.weight(1f),
            onClick = {
                val setIndex = (currentSet - 1).coerceAtLeast(0)
                val repsDone = totalReps
                sync.onFinishPressed(setIndex, repsDone, onFinish)
            }
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.size(10.dp))
                Text("Finish", fontWeight = FontWeight.SemiBold)
            }
        }
    }

    sync.postResult?.let {
        Spacer(Modifier.height(8.dp))
        Text(it, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun CameraXPreviewElbow(
    modifier: Modifier = Modifier,
    overlayView: OverlayView,
    isRunning: Boolean,
    counter: ElbowFlexionCounter,
    audioCue: HoldAudioCue,
    videoCaptureState: androidx.compose.runtime.MutableState<VideoCapture<Recorder>?>,
    onRepUpdate: (reps: Int, phase: ElbowFlexionCounter.Phase) -> Unit,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }
    var cameraProvider by remember { mutableStateOf<ProcessCameraProvider?>(null) }

    val isRunningState = rememberUpdatedState(isRunning)

    val mirrorForUser = false
    val previewView = remember {
        PreviewView(context).apply {
            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
            scaleType = PreviewView.ScaleType.FILL_CENTER
            scaleX = if (mirrorForUser) -1f else 1f
        }
    }

    val backgroundExecutor = remember { Executors.newSingleThreadExecutor() }

    val poseLandmarkerHelper = remember {
        PoseLandmarkerHelper(
            context = context,
            runningMode = RunningMode.LIVE_STREAM,
            poseLandmarkerHelperListener = object : PoseLandmarkerHelper.LandmarkerListener {
                override fun onResults(resultBundle: PoseLandmarkerHelper.ResultBundle) {
                    if (!isRunningState.value) return
                    val result = resultBundle.results.firstOrNull() ?: return

                    val frame = counter.update(result) ?: return

                    previewView.post {
                        audioCue.onFrame(frame)

                        overlayView.setResults(
                            poseLandmarkerResults = result,
                            imageHeight = resultBundle.inputImageHeight,
                            imageWidth = resultBundle.inputImageWidth,
                            runningMode = RunningMode.LIVE_STREAM,
                            isMirrored = true
                        )

                        val angleText = if (frame.phase == ElbowFlexionCounter.Phase.HOLDING) {
                            val sec = (frame.holdRemainingMs / 100) / 10f
                            "${frame.angleDeg.roundToInt()}°  •  hold ${sec}s"
                        } else {
                            "${frame.angleDeg.roundToInt()}°"
                        }

                        overlayView.setAngleLabel(
                            nx = frame.elbowNx,
                            ny = frame.elbowNy,
                            text = angleText
                        )

                        onRepUpdate(frame.reps, frame.phase)
                    }
                }

                override fun onError(error: String, errorCode: Int) {
                    Log.e("PoseLandmarker", "Error: $error (code=$errorCode)")
                }
            }
        )
    }

    DisposableEffect(Unit) {
        val executor = ContextCompat.getMainExecutor(context)
        val listener = Runnable { cameraProvider = cameraProviderFuture.get() }
        cameraProviderFuture.addListener(listener, executor)
        onDispose { /* no-op */ }
    }

    DisposableEffect(cameraProvider, lifecycleOwner) {
        val provider = cameraProvider ?: return@DisposableEffect onDispose { }

        val preview = Preview.Builder()
            .setTargetAspectRatio(AspectRatio.RATIO_4_3)
            .build()
            .also { it.setSurfaceProvider(previewView.surfaceProvider) }

        val selector = CameraSelector.Builder()
            .requireLensFacing(CameraSelector.LENS_FACING_FRONT)
            .build()

        val imageAnalyzer = ImageAnalysis.Builder()
            .setTargetAspectRatio(AspectRatio.RATIO_4_3)
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
            .build()
            .also {
                it.setAnalyzer(backgroundExecutor) { imageProxy ->
                    if (!isRunningState.value) {
                        imageProxy.close()
                        return@setAnalyzer
                    }

                    poseLandmarkerHelper.detectLiveStream(
                        imageProxy = imageProxy,
                        isFrontCamera = false
                    )
                }
            }

        val recorder = Recorder.Builder()
            .setQualitySelector(QualitySelector.from(Quality.SD))
            .build()
        val videoCapture = withOutput(recorder)
        videoCaptureState.value = videoCapture

        provider.unbindAll()
        runCatching {
            provider.bindToLifecycle(lifecycleOwner, selector, preview, imageAnalyzer, videoCapture)
        }.onFailure {
            Log.e("CameraXPreviewElbow", "bindToLifecycle failed", it)
        }

        onDispose {
            runCatching { provider.unbindAll() }
            runCatching { poseLandmarkerHelper.clearPoseLandmarker() }
            runCatching { overlayView.clear() }
            runCatching { backgroundExecutor.shutdown() }
            videoCaptureState.value = null
        }
    }

    AndroidView(modifier = modifier, factory = { previewView })
}

/* =========================================================
 *  SESSION 2: ARM RAISE (Down -> T -> Overhead)
 * ========================================================= */
@Composable
private fun SessionBodyArmRaise(
    exercise: Exercise,
    onFinish: (sessionId: String?, setIndex: Int, repsDone: Int) -> Unit
) {
    val context = LocalContext.current
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    val mainExecutor: Executor = remember { ContextCompat.getMainExecutor(context) }

    val overlayView = remember { OverlayView(context, null) }
    val videoCaptureState = remember { mutableStateOf<VideoCapture<Recorder>?>(null) }
    val sync = remember(exercise.id) {
        SessionSyncController(
            context = context,
            scope = scope,
            mainExecutor = mainExecutor,
            exercise = exercise,
            videoCaptureState = videoCaptureState,
        )
    }

    val preStartCue = remember(context) { PreStartCountdownCue(context) }
    DisposableEffect(Unit) { onDispose { preStartCue.release() } }

    var isCountingDown by remember { mutableStateOf(false) }
    var countdownSec by remember { mutableStateOf(0) }
    var countdownText by remember { mutableStateOf("") }

    val repsPerSet = 10
    val setsMax = 3
    val totalTarget = repsPerSet * setsMax

    var running by remember { mutableStateOf(false) }
    var totalReps by remember { mutableStateOf(0) }
    var feedback by remember { mutableStateOf("Press Start to begin") }

    val currentSet = (totalReps / repsPerSet + 1).coerceAtMost(setsMax)
    val repsInSet = (totalReps % repsPerSet)

    val counter = remember {
        ArmRaiseCounter(
            side = ArmRaiseCounter.Side.LEFT,
            holdMs = 2000L
        )
    }

    val audioCue = remember(context) { ArmRaiseAudioCue(context, holdMs = 2000L) }
    DisposableEffect(Unit) { onDispose { audioCue.release() } }

    var startFromCountdown by remember { mutableStateOf(false) }

    LaunchedEffect(isCountingDown) {
        if (!isCountingDown) return@LaunchedEffect

        running = false
        audioCue.reset()
        counter.reset()
        totalReps = 0
        feedback = "Get ready..."
        overlayView.post { overlayView.clear() }

        countdownText = "SIAP-SIAP"
        countdownSec = 0

        preStartCue.start(onGo = { })

        delay(900L)
        countdownText = ""

        for (i in 5 downTo 1) {
            countdownSec = i
            delay(1000L)
        }

        countdownSec = 0
        countdownText = "MULAI!"
        delay(600L)
        countdownText = ""

        isCountingDown = false
        startFromCountdown = true
        running = true
        feedback = "Raise to side (T)"
        sync.onRunningBeganAfterCountdown()
    }

    LaunchedEffect(running, isCountingDown) {
        if (!running && !isCountingDown) {
            audioCue.reset()
            counter.reset()
            totalReps = 0
            feedback = "Press Start to begin"
            overlayView.post { overlayView.clear() }
            sync.resetAll()
        } else if (running) {
            audioCue.reset()
            val needKick = startFromCountdown && (totalReps == 0)
            if (needKick) {
                delay(250L)
                audioCue.playFirstInstruction()
            }
            startFromCountdown = false
        }
    }

    PremiumCard(modifier = Modifier.fillMaxWidth()) {
        Text("Live Camera", fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(8.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(360.dp),
        ) {
            CameraXPreviewArmRaise(
                modifier = Modifier.fillMaxSize(),
                overlayView = overlayView,
                isRunning = running,
                counter = counter,
                audioCue = audioCue,
                videoCaptureState = videoCaptureState,
                onUpdate = { frame ->
                    totalReps = frame.reps

                    feedback = when {
                        totalReps >= totalTarget -> "Session complete!"
                        frame.phase == ArmRaiseCounter.Phase.DOWN -> "Raise to side (T)"
                        frame.phase == ArmRaiseCounter.Phase.AT_T -> "Raise overhead"
                        frame.phase == ArmRaiseCounter.Phase.OVERHEAD_HOLDING && frame.holdRemainingMs > 0L -> "Hold 2 seconds"
                        frame.phase == ArmRaiseCounter.Phase.OVERHEAD -> "Lower down to start"
                        else -> "Find a clear arm position"
                    }

                    if (totalReps >= totalTarget) running = false
                }
            )

            AndroidView(modifier = Modifier.fillMaxSize(), factory = { overlayView })

            if (isCountingDown) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        if (countdownText.isNotBlank()) {
                            Text(
                                text = countdownText,
                                color = Color.White,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 36.sp
                            )
                            Spacer(Modifier.height(10.dp))
                        }
                        if (countdownSec > 0) {
                            Text(
                                text = countdownSec.toString(),
                                color = Color.White,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 88.sp
                            )
                        }
                    }
                }
            }
        }

        sync.clipStatus?.let {
            Spacer(Modifier.height(10.dp))
            Text(it, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }

    PremiumCard(modifier = Modifier.fillMaxWidth()) {
        Text("Feedback", fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(6.dp))
        Text(feedback, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(10.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Set: $currentSet/$setsMax", fontWeight = FontWeight.SemiBold)
            Text("Reps: $repsInSet/$repsPerSet", fontWeight = FontWeight.SemiBold)
        }
    }

    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        PremiumCardClickable(
            modifier = Modifier.weight(1f),
            onClick = {
                if (running) running = false
                else if (!isCountingDown) {
                    sync.onStartPressed()
                    isCountingDown = true
                }
            }
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    if (running) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                )
                Spacer(Modifier.size(10.dp))
                Text(
                    when {
                        running -> "Pause"
                        isCountingDown -> "Starting..."
                        else -> "Start"
                    },
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        PremiumCardClickable(
            modifier = Modifier.weight(1f),
            onClick = {
                val setIndex = (currentSet - 1).coerceAtLeast(0)
                val repsDone = totalReps
                sync.onFinishPressed(setIndex, repsDone, onFinish)
            }
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.size(10.dp))
                Text("Finish", fontWeight = FontWeight.SemiBold)
            }
        }
    }

    sync.postResult?.let {
        Spacer(Modifier.height(8.dp))
        Text(it, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun CameraXPreviewArmRaise(
    modifier: Modifier = Modifier,
    overlayView: OverlayView,
    isRunning: Boolean,
    counter: ArmRaiseCounter,
    audioCue: ArmRaiseAudioCue,
    videoCaptureState: androidx.compose.runtime.MutableState<VideoCapture<Recorder>?>,
    onUpdate: (ArmRaiseCounter.Frame) -> Unit,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }
    var cameraProvider by remember { mutableStateOf<ProcessCameraProvider?>(null) }

    val isRunningState = rememberUpdatedState(isRunning)

    val mirrorForUser = false
    val previewView = remember {
        PreviewView(context).apply {
            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
            scaleType = PreviewView.ScaleType.FILL_CENTER
            scaleX = if (mirrorForUser) -1f else 1f
        }
    }

    val backgroundExecutor = remember { Executors.newSingleThreadExecutor() }

    val poseLandmarkerHelper = remember {
        PoseLandmarkerHelper(
            context = context,
            runningMode = RunningMode.LIVE_STREAM,
            poseLandmarkerHelperListener = object : PoseLandmarkerHelper.LandmarkerListener {
                override fun onResults(resultBundle: PoseLandmarkerHelper.ResultBundle) {
                    if (!isRunningState.value) return
                    val result = resultBundle.results.firstOrNull() ?: return

                    val frame = counter.update(result) ?: return

                    previewView.post {
                        audioCue.onFrame(frame)

                        overlayView.setResults(
                            poseLandmarkerResults = result,
                            imageHeight = resultBundle.inputImageHeight,
                            imageWidth = resultBundle.inputImageWidth,
                            runningMode = RunningMode.LIVE_STREAM,
                            isMirrored = true
                        )

                        val label = if (frame.phase == ArmRaiseCounter.Phase.OVERHEAD_HOLDING) {
                            val sec = (frame.holdRemainingMs / 100) / 10f
                            "${frame.shoulderAngleDeg.toInt()}° • hold ${sec}s"
                        } else {
                            "${frame.shoulderAngleDeg.toInt()}°"
                        }

                        overlayView.setAngleLabel(
                            nx = frame.labelNx,
                            ny = frame.labelNy,
                            text = label
                        )

                        onUpdate(frame)
                    }
                }

                override fun onError(error: String, errorCode: Int) {
                    Log.e("PoseLandmarker", "Error: $error (code=$errorCode)")
                }
            }
        )
    }

    DisposableEffect(Unit) {
        val executor = ContextCompat.getMainExecutor(context)
        val listener = Runnable { cameraProvider = cameraProviderFuture.get() }
        cameraProviderFuture.addListener(listener, executor)
        onDispose { /* no-op */ }
    }

    DisposableEffect(cameraProvider, lifecycleOwner) {
        val provider = cameraProvider ?: return@DisposableEffect onDispose { }

        val preview = Preview.Builder()
            .setTargetAspectRatio(AspectRatio.RATIO_4_3)
            .build()
            .also { it.setSurfaceProvider(previewView.surfaceProvider) }

        val selector = CameraSelector.Builder()
            .requireLensFacing(CameraSelector.LENS_FACING_FRONT)
            .build()

        val imageAnalyzer = ImageAnalysis.Builder()
            .setTargetAspectRatio(AspectRatio.RATIO_4_3)
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
            .build()
            .also {
                it.setAnalyzer(backgroundExecutor) { imageProxy ->
                    if (!isRunningState.value) {
                        imageProxy.close()
                        return@setAnalyzer
                    }

                    poseLandmarkerHelper.detectLiveStream(
                        imageProxy = imageProxy,
                        isFrontCamera = false
                    )
                }
            }

        val recorder = Recorder.Builder()
            .setQualitySelector(QualitySelector.from(Quality.SD))
            .build()
        val videoCapture = withOutput(recorder)
        videoCaptureState.value = videoCapture

        provider.unbindAll()
        runCatching {
            provider.bindToLifecycle(lifecycleOwner, selector, preview, imageAnalyzer, videoCapture)
        }.onFailure {
            Log.e("CameraXPreviewArmRaise", "bindToLifecycle failed", it)
        }

        onDispose {
            runCatching { provider.unbindAll() }
            runCatching { poseLandmarkerHelper.clearPoseLandmarker() }
            runCatching { overlayView.clear() }
            runCatching { backgroundExecutor.shutdown() }
            videoCaptureState.value = null
        }
    }

    AndroidView(modifier = modifier, factory = { previewView })
}

/* =========================================================
 *  SESSION 3: HIP FLEXION
 * ========================================================= */
@Composable
private fun SessionBodyHipFlexion(
    exercise: Exercise,
    onFinish: (sessionId: String?, setIndex: Int, repsDone: Int) -> Unit
) {
    val context = LocalContext.current
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    val mainExecutor: Executor = remember { ContextCompat.getMainExecutor(context) }

    val overlayView = remember { OverlayView(context, null) }
    val videoCaptureState = remember { mutableStateOf<VideoCapture<Recorder>?>(null) }
    val sync = remember(exercise.id) {
        SessionSyncController(
            context = context,
            scope = scope,
            mainExecutor = mainExecutor,
            exercise = exercise,
            videoCaptureState = videoCaptureState,
        )
    }

    val preStartCue = remember(context) { PreStartCountdownCue(context) }
    DisposableEffect(Unit) { onDispose { preStartCue.release() } }

    var isCountingDown by remember { mutableStateOf(false) }
    var countdownSec by remember { mutableStateOf(0) }
    var countdownText by remember { mutableStateOf("") }

    val repsPerSet = 10
    val setsMax = 3
    val totalTarget = repsPerSet * setsMax

    var running by remember { mutableStateOf(false) }
    var totalReps by remember { mutableStateOf(0) }
    var feedback by remember { mutableStateOf("Press Start to begin") }

    val currentSet = (totalReps / repsPerSet + 1).coerceAtMost(setsMax)
    val repsInSet = (totalReps % repsPerSet)

    val counter = remember {
        HipFlexionCounter(
            side = HipFlexionCounter.Side.LEFT,
            upThreshold = 0.03f,
            downThreshold = 0.03f,
            holdMs = 2000L
        )
    }

    val audioCue = remember(context) { HipFlexionAudioCue(context, holdMs = 2000L) }
    DisposableEffect(Unit) { onDispose { audioCue.release() } }

    var startFromCountdown by remember { mutableStateOf(false) }

    LaunchedEffect(isCountingDown) {
        if (!isCountingDown) return@LaunchedEffect

        running = false
        audioCue.reset()
        counter.reset()
        totalReps = 0
        feedback = "Get ready..."
        overlayView.post { overlayView.clear() }

        countdownText = "SIAP-SIAP"
        countdownSec = 0

        preStartCue.start(onGo = { })

        delay(900L)
        countdownText = ""

        for (i in 5 downTo 1) {
            countdownSec = i
            delay(1000L)
        }

        countdownSec = 0
        countdownText = "MULAI!"
        delay(600L)
        countdownText = ""

        isCountingDown = false
        startFromCountdown = true
        running = true
        feedback = "Lift your knee"
        sync.onRunningBeganAfterCountdown()
    }

    LaunchedEffect(running, isCountingDown) {
        if (!running && !isCountingDown) {
            audioCue.reset()
            counter.reset()
            totalReps = 0
            feedback = "Press Start to begin"
            overlayView.post { overlayView.clear() }
            sync.resetAll()
        } else if (running) {
            audioCue.reset()
            val needKick = startFromCountdown && (totalReps == 0)
            if (needKick) {
                delay(250L)
                audioCue.playFirstInstruction()
            }
            startFromCountdown = false
        }
    }

    PremiumCard(modifier = Modifier.fillMaxWidth()) {
        Text("Live Camera", fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(8.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(360.dp),
        ) {
            CameraXPreviewHip(
                modifier = Modifier.fillMaxSize(),
                overlayView = overlayView,
                isRunning = running,
                counter = counter,
                audioCue = audioCue,
                videoCaptureState = videoCaptureState,
                onUpdate = { reps, phase, holdRemainingMs, _ ->
                    totalReps = reps

                    feedback = when {
                        reps >= totalTarget -> "Session complete!"
                        phase == HipFlexionCounter.Phase.DOWN -> "Lift your knee"
                        phase == HipFlexionCounter.Phase.UP_HOLDING -> {
                            val sec = (holdRemainingMs / 100) / 10f
                            "Hold ${sec}s"
                        }
                        phase == HipFlexionCounter.Phase.UP -> "Lower your knee"
                        else -> "Find a clear leg position"
                    }

                    if (reps >= totalTarget) running = false
                }
            )

            AndroidView(modifier = Modifier.fillMaxSize(), factory = { overlayView })

            if (isCountingDown) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        if (countdownText.isNotBlank()) {
                            Text(
                                text = countdownText,
                                color = Color.White,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 36.sp
                            )
                            Spacer(Modifier.height(10.dp))
                        }
                        if (countdownSec > 0) {
                            Text(
                                text = countdownSec.toString(),
                                color = Color.White,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 88.sp
                            )
                        }
                    }
                }
            }
        }

        sync.clipStatus?.let {
            Spacer(Modifier.height(10.dp))
            Text(it, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }

    PremiumCard(modifier = Modifier.fillMaxWidth()) {
        Text("Feedback", fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(6.dp))
        Text(feedback, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(10.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Set: $currentSet/$setsMax", fontWeight = FontWeight.SemiBold)
            Text("Reps: $repsInSet/$repsPerSet", fontWeight = FontWeight.SemiBold)
        }
    }

    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        PremiumCardClickable(
            modifier = Modifier.weight(1f),
            onClick = {
                if (running) running = false
                else if (!isCountingDown) {
                    sync.onStartPressed()
                    isCountingDown = true
                }
            }
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    if (running) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                )
                Spacer(Modifier.size(10.dp))
                Text(
                    when {
                        running -> "Pause"
                        isCountingDown -> "Starting..."
                        else -> "Start"
                    },
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        PremiumCardClickable(
            modifier = Modifier.weight(1f),
            onClick = {
                val setIndex = (currentSet - 1).coerceAtLeast(0)
                val repsDone = totalReps
                sync.onFinishPressed(setIndex, repsDone, onFinish)
            }
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.size(10.dp))
                Text("Finish", fontWeight = FontWeight.SemiBold)
            }
        }
    }

    sync.postResult?.let {
        Spacer(Modifier.height(8.dp))
        Text(it, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun CameraXPreviewHip(
    modifier: Modifier = Modifier,
    overlayView: OverlayView,
    isRunning: Boolean,
    counter: HipFlexionCounter,
    audioCue: HipFlexionAudioCue,
    videoCaptureState: androidx.compose.runtime.MutableState<VideoCapture<Recorder>?>,
    onUpdate: (
        reps: Int,
        phase: HipFlexionCounter.Phase,
        holdRemainingMs: Long,
        kneeLift: Float
    ) -> Unit,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }
    var cameraProvider by remember { mutableStateOf<ProcessCameraProvider?>(null) }

    val isRunningState = rememberUpdatedState(isRunning)

    val mirrorForUser = false
    val previewView = remember {
        PreviewView(context).apply {
            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
            scaleType = PreviewView.ScaleType.FILL_CENTER
            scaleX = if (mirrorForUser) -1f else 1f
        }
    }

    val backgroundExecutor = remember { Executors.newSingleThreadExecutor() }

    val poseLandmarkerHelper = remember {
        PoseLandmarkerHelper(
            context = context,
            runningMode = RunningMode.LIVE_STREAM,
            poseLandmarkerHelperListener = object : PoseLandmarkerHelper.LandmarkerListener {
                override fun onResults(resultBundle: PoseLandmarkerHelper.ResultBundle) {
                    if (!isRunningState.value) return
                    val result = resultBundle.results.firstOrNull() ?: return

                    val frame = counter.update(result) ?: return

                    previewView.post {
                        audioCue.onFrame(frame)

                        overlayView.setResults(
                            poseLandmarkerResults = result,
                            imageHeight = resultBundle.inputImageHeight,
                            imageWidth = resultBundle.inputImageWidth,
                            runningMode = RunningMode.LIVE_STREAM,
                            isMirrored = true
                        )

                        val liftPct = (frame.kneeLift * 100f).toInt().coerceAtLeast(0)
                        val labelText = if (frame.phase == HipFlexionCounter.Phase.UP_HOLDING) {
                            val sec = (frame.holdRemainingMs / 100) / 10f
                            "lift ${liftPct}% • hold ${sec}s"
                        } else {
                            "lift ${liftPct}%"
                        }

                        overlayView.setAngleLabel(
                            nx = frame.labelNx,
                            ny = frame.labelNy,
                            text = labelText
                        )

                        onUpdate(
                            frame.reps,
                            frame.phase,
                            frame.holdRemainingMs,
                            frame.kneeLift
                        )
                    }
                }

                override fun onError(error: String, errorCode: Int) {
                    Log.e("PoseLandmarker", "Error: $error (code=$errorCode)")
                }
            }
        )
    }

    DisposableEffect(Unit) {
        val executor = ContextCompat.getMainExecutor(context)
        val listener = Runnable { cameraProvider = cameraProviderFuture.get() }
        cameraProviderFuture.addListener(listener, executor)
        onDispose { /* no-op */ }
    }

    DisposableEffect(cameraProvider, lifecycleOwner) {
        val provider = cameraProvider ?: return@DisposableEffect onDispose { }

        val preview = Preview.Builder()
            .setTargetAspectRatio(AspectRatio.RATIO_4_3)
            .build()
            .also { it.setSurfaceProvider(previewView.surfaceProvider) }

        val selector = CameraSelector.Builder()
            .requireLensFacing(CameraSelector.LENS_FACING_FRONT)
            .build()

        val imageAnalyzer = ImageAnalysis.Builder()
            .setTargetAspectRatio(AspectRatio.RATIO_4_3)
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
            .build()
            .also {
                it.setAnalyzer(backgroundExecutor) { imageProxy ->
                    if (!isRunningState.value) {
                        imageProxy.close()
                        return@setAnalyzer
                    }
                    poseLandmarkerHelper.detectLiveStream(
                        imageProxy = imageProxy,
                        isFrontCamera = false
                    )
                }
            }

        val recorder = Recorder.Builder()
            .setQualitySelector(QualitySelector.from(Quality.SD))
            .build()
        val videoCapture = withOutput(recorder)
        videoCaptureState.value = videoCapture

        provider.unbindAll()
        runCatching {
            provider.bindToLifecycle(lifecycleOwner, selector, preview, imageAnalyzer, videoCapture)
        }.onFailure {
            Log.e("CameraXPreviewHip", "bindToLifecycle failed", it)
        }

        onDispose {
            runCatching { provider.unbindAll() }
            runCatching { poseLandmarkerHelper.clearPoseLandmarker() }
            runCatching { overlayView.clear() }
            runCatching { backgroundExecutor.shutdown() }
            videoCaptureState.value = null
        }
    }

    AndroidView(modifier = modifier, factory = { previewView })
}

/* =========================================================
 *  SESSION 4: KNEE EXTENSION
 * ========================================================= */
@Composable
private fun SessionBodyKneeExtension(
    exercise: Exercise,
    onFinish: (sessionId: String?, setIndex: Int, repsDone: Int) -> Unit
) {
    val context = LocalContext.current
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    val mainExecutor: Executor = remember { ContextCompat.getMainExecutor(context) }

    val overlayView = remember { OverlayView(context, null) }
    val videoCaptureState = remember { mutableStateOf<VideoCapture<Recorder>?>(null) }
    val sync = remember(exercise.id) {
        SessionSyncController(
            context = context,
            scope = scope,
            mainExecutor = mainExecutor,
            exercise = exercise,
            videoCaptureState = videoCaptureState,
        )
    }

    val preStartCue = remember(context) { PreStartCountdownCue(context) }
    DisposableEffect(Unit) { onDispose { preStartCue.release() } }

    var isCountingDown by remember { mutableStateOf(false) }
    var countdownSec by remember { mutableStateOf(0) }
    var countdownText by remember { mutableStateOf("") }

    val repsPerSet = 10
    val setsMax = 3
    val totalTarget = repsPerSet * setsMax

    var running by remember { mutableStateOf(false) }
    var totalReps by remember { mutableStateOf(0) }
    var feedback by remember { mutableStateOf("Press Start to begin") }

    val currentSet = (totalReps / repsPerSet + 1).coerceAtMost(setsMax)
    val repsInSet = (totalReps % repsPerSet)

    val counter = remember {
        KneeExtensionCounter(
            side = KneeExtensionCounter.Side.LEFT,
            holdMs = 2000L
        )
    }

    val audioCue = remember(context) { KneeExtensionAudioCue(context, holdMs = 2000L) }
    DisposableEffect(Unit) { onDispose { audioCue.release() } }

    var startFromCountdown by remember { mutableStateOf(false) }

    LaunchedEffect(isCountingDown) {
        if (!isCountingDown) return@LaunchedEffect

        running = false
        audioCue.reset()
        counter.reset()
        totalReps = 0
        feedback = "Get ready..."
        overlayView.post { overlayView.clear() }

        countdownText = "SIAP-SIAP"
        countdownSec = 0

        preStartCue.start(onGo = { })

        delay(900L)
        countdownText = ""

        for (i in 5 downTo 1) {
            countdownSec = i
            delay(1000L)
        }

        countdownSec = 0
        countdownText = "MULAI!"
        delay(600L)
        countdownText = ""

        isCountingDown = false
        startFromCountdown = true
        running = true
        feedback = "Straighten your knee"
        sync.onRunningBeganAfterCountdown()
    }

    LaunchedEffect(running, isCountingDown) {
        if (!running && !isCountingDown) {
            audioCue.reset()
            counter.reset()
            totalReps = 0
            feedback = "Press Start to begin"
            overlayView.post { overlayView.clear() }
            sync.resetAll()
        } else if (running) {
            audioCue.reset()
            if (startFromCountdown && totalReps == 0) {
                delay(250L)
                audioCue.primeFirstInstruction()
            }
            startFromCountdown = false
        }
    }

    PremiumCard(modifier = Modifier.fillMaxWidth()) {
        Text("Live Camera", fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(8.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(360.dp),
        ) {
            CameraXPreviewKneeExtension(
                modifier = Modifier.fillMaxSize(),
                overlayView = overlayView,
                isRunning = running,
                counter = counter,
                audioCue = audioCue,
                videoCaptureState = videoCaptureState,
                onUpdate = { frame ->
                    totalReps = frame.reps

                    feedback = when {
                        totalReps >= totalTarget -> "Session complete!"
                        frame.phase == KneeExtensionCounter.Phase.DOWN -> "Straighten your knee"
                        frame.phase == KneeExtensionCounter.Phase.UP_HOLDING -> {
                            val sec = (frame.holdRemainingMs / 100) / 10f
                            "Hold ${sec}s"
                        }
                        frame.phase == KneeExtensionCounter.Phase.UP -> "Lower your leg"
                        else -> "Find a clear leg position"
                    }

                    if (totalReps >= totalTarget) running = false
                }
            )

            AndroidView(modifier = Modifier.fillMaxSize(), factory = { overlayView })

            if (isCountingDown) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        if (countdownText.isNotBlank()) {
                            Text(
                                text = countdownText,
                                color = Color.White,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 36.sp
                            )
                            Spacer(Modifier.height(10.dp))
                        }
                        if (countdownSec > 0) {
                            Text(
                                text = countdownSec.toString(),
                                color = Color.White,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 88.sp
                            )
                        }
                    }
                }
            }
        }

        sync.clipStatus?.let {
            Spacer(Modifier.height(10.dp))
            Text(it, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }

    PremiumCard(modifier = Modifier.fillMaxWidth()) {
        Text("Feedback", fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(6.dp))
        Text(feedback, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(10.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Set: $currentSet/$setsMax", fontWeight = FontWeight.SemiBold)
            Text("Reps: $repsInSet/$repsPerSet", fontWeight = FontWeight.SemiBold)
        }
    }

    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        PremiumCardClickable(
            modifier = Modifier.weight(1f),
            onClick = {
                if (running) running = false
                else if (!isCountingDown) {
                    sync.onStartPressed()
                    isCountingDown = true
                }
            }
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    if (running) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                )
                Spacer(Modifier.size(10.dp))
                Text(
                    when {
                        running -> "Pause"
                        isCountingDown -> "Starting..."
                        else -> "Start"
                    },
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        PremiumCardClickable(
            modifier = Modifier.weight(1f),
            onClick = {
                val setIndex = (currentSet - 1).coerceAtLeast(0)
                val repsDone = totalReps
                sync.onFinishPressed(setIndex, repsDone, onFinish)
            }
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.size(10.dp))
                Text("Finish", fontWeight = FontWeight.SemiBold)
            }
        }
    }

    sync.postResult?.let {
        Spacer(Modifier.height(8.dp))
        Text(it, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun CameraXPreviewKneeExtension(
    modifier: Modifier = Modifier,
    overlayView: OverlayView,
    isRunning: Boolean,
    counter: KneeExtensionCounter,
    audioCue: KneeExtensionAudioCue,
    videoCaptureState: androidx.compose.runtime.MutableState<VideoCapture<Recorder>?>,
    onUpdate: (KneeExtensionCounter.Frame) -> Unit,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }
    var cameraProvider by remember { mutableStateOf<ProcessCameraProvider?>(null) }

    val isRunningState = rememberUpdatedState(isRunning)

    val mirrorForUser = false
    val previewView = remember {
        PreviewView(context).apply {
            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
            scaleType = PreviewView.ScaleType.FILL_CENTER
            scaleX = if (mirrorForUser) -1f else 1f
        }
    }

    val backgroundExecutor = remember { Executors.newSingleThreadExecutor() }

    val poseLandmarkerHelper = remember {
        PoseLandmarkerHelper(
            context = context,
            runningMode = RunningMode.LIVE_STREAM,
            poseLandmarkerHelperListener = object : PoseLandmarkerHelper.LandmarkerListener {
                override fun onResults(resultBundle: PoseLandmarkerHelper.ResultBundle) {
                    if (!isRunningState.value) return
                    val result = resultBundle.results.firstOrNull() ?: return

                    val frame = counter.update(result) ?: return

                    previewView.post {
                        audioCue.onFrame(frame)

                        overlayView.setResults(
                            poseLandmarkerResults = result,
                            imageHeight = resultBundle.inputImageHeight,
                            imageWidth = resultBundle.inputImageWidth,
                            runningMode = RunningMode.LIVE_STREAM,
                            isMirrored = true
                        )

                        val labelText = if (frame.phase == KneeExtensionCounter.Phase.UP_HOLDING) {
                            val sec = (frame.holdRemainingMs / 100) / 10f
                            "${frame.kneeAngleDeg.toInt()}° • hold ${sec}s"
                        } else {
                            "${frame.kneeAngleDeg.toInt()}°"
                        }

                        overlayView.setAngleLabel(
                            nx = frame.labelNx,
                            ny = frame.labelNy,
                            text = labelText
                        )

                        onUpdate(frame)
                    }
                }

                override fun onError(error: String, errorCode: Int) {
                    Log.e("PoseLandmarker", "Error: $error (code=$errorCode)")
                }
            }
        )
    }

    DisposableEffect(Unit) {
        val executor = ContextCompat.getMainExecutor(context)
        val listener = Runnable { cameraProvider = cameraProviderFuture.get() }
        cameraProviderFuture.addListener(listener, executor)
        onDispose { /* no-op */ }
    }

    DisposableEffect(cameraProvider, lifecycleOwner) {
        val provider = cameraProvider ?: return@DisposableEffect onDispose { }

        val preview = Preview.Builder()
            .setTargetAspectRatio(AspectRatio.RATIO_4_3)
            .build()
            .also { it.setSurfaceProvider(previewView.surfaceProvider) }

        val selector = CameraSelector.Builder()
            .requireLensFacing(CameraSelector.LENS_FACING_FRONT)
            .build()

        val imageAnalyzer = ImageAnalysis.Builder()
            .setTargetAspectRatio(AspectRatio.RATIO_4_3)
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
            .build()
            .also {
                it.setAnalyzer(backgroundExecutor) { imageProxy ->
                    if (!isRunningState.value) {
                        imageProxy.close()
                        return@setAnalyzer
                    }
                    poseLandmarkerHelper.detectLiveStream(
                        imageProxy = imageProxy,
                        isFrontCamera = false
                    )
                }
            }

        val recorder = Recorder.Builder()
            .setQualitySelector(QualitySelector.from(Quality.SD))
            .build()
        val videoCapture = withOutput(recorder)
        videoCaptureState.value = videoCapture

        provider.unbindAll()
        runCatching {
            provider.bindToLifecycle(lifecycleOwner, selector, preview, imageAnalyzer, videoCapture)
        }.onFailure {
            Log.e("CameraXPreviewKneeExtension", "bindToLifecycle failed", it)
        }

        onDispose {
            runCatching { provider.unbindAll() }
            runCatching { poseLandmarkerHelper.clearPoseLandmarker() }
            runCatching { overlayView.clear() }
            runCatching { backgroundExecutor.shutdown() }
            videoCaptureState.value = null
        }
    }

    AndroidView(modifier = modifier, factory = { previewView })
}

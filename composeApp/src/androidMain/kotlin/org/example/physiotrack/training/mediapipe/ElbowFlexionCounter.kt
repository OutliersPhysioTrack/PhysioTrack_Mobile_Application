package org.example.physiotrack.training.mediapipe

import android.os.SystemClock
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarkerResult
import kotlin.math.acos
import kotlin.math.sqrt

class ElbowFlexionCounter(
    private val side: Side = Side.LEFT,
    private val flexedThresholdDeg: Float = 70f,       // siku dianggap “tekuk”
    private val extendedThresholdDeg: Float = 160f,    // siku dianggap “lurus”
    private val holdMs: Long = 2000L,                  // tahan 2 detik
    private val flexedExitDeg: Float = 85f,            // kalau hold, tapi angle naik > ini -> hold batal (anti jitter)
    private val smoothingWindow: Int = 5,
) {
    enum class Side { LEFT, RIGHT }

    // ✅ State yang dipakai UI + audio
    enum class Phase {
        UNKNOWN,
        READY_EXTENDED, // “posisi awal lurus, siap tekuk”
        HOLDING,        // “sudah tekuk, tahan 2 detik”
        NEED_EXTEND,    // “hold sukses, sekarang harus luruskan”
    }

    data class Frame(
        val angleDeg: Float,
        val elbowNx: Float,
        val elbowNy: Float,
        val reps: Int,
        val phase: Phase,
        val holdRemainingMs: Long = 0L,
    )

    // landmark indices (BlazePose 33)
    private val LEFT_SHOULDER = 11
    private val RIGHT_SHOULDER = 12
    private val LEFT_ELBOW = 13
    private val RIGHT_ELBOW = 14
    private val LEFT_WRIST = 15
    private val RIGHT_WRIST = 16

    private var reps: Int = 0
    private var phase: Phase = Phase.UNKNOWN

    private var holdStartMs: Long = 0L

    // smoothing buffer (biar aman di KMP & ga tergantung ArrayDeque)
    private val win = smoothingWindow.coerceAtLeast(1)
    private val buf = FloatArray(win)
    private var bufCount = 0
    private var bufIdx = 0

    fun reset() {
        reps = 0
        phase = Phase.UNKNOWN
        holdStartMs = 0L
        bufCount = 0
        bufIdx = 0
    }

    fun update(result: PoseLandmarkerResult): Frame? {
        val pose = result.landmarks().firstOrNull() ?: return null

        val sIdx: Int
        val eIdx: Int
        val wIdx: Int
        if (side == Side.LEFT) {
            sIdx = LEFT_SHOULDER
            eIdx = LEFT_ELBOW
            wIdx = LEFT_WRIST
        } else {
            sIdx = RIGHT_SHOULDER
            eIdx = RIGHT_ELBOW
            wIdx = RIGHT_WRIST
        }

        val shoulder = pose.getOrNull(sIdx) ?: return null
        val elbow = pose.getOrNull(eIdx) ?: return null
        val wrist = pose.getOrNull(wIdx) ?: return null

        val rawAngle = angleDeg(
            ax = shoulder.x(), ay = shoulder.y(),
            bx = elbow.x(), by = elbow.y(),
            cx = wrist.x(), cy = wrist.y(),
        )
        val angle = smooth(rawAngle)

        val now = SystemClock.uptimeMillis()
        var holdRemaining = 0L

        when (phase) {
            Phase.UNKNOWN -> {
                // kalau mulai dari posisi tekuk, minta lurus dulu
                phase = when {
                    angle >= extendedThresholdDeg -> Phase.READY_EXTENDED
                    angle <= flexedThresholdDeg -> Phase.NEED_EXTEND
                    else -> Phase.UNKNOWN
                }
                holdStartMs = 0L
            }

            Phase.READY_EXTENDED -> {
                // tunggu user tekuk
                if (angle <= flexedThresholdDeg) {
                    phase = Phase.HOLDING
                    holdStartMs = now
                    holdRemaining = holdMs
                }
            }

            Phase.HOLDING -> {
                // kalau dia “lepas” sebelum selesai hold -> batal
                if (angle > flexedExitDeg) {
                    holdStartMs = 0L
                    phase = if (angle >= extendedThresholdDeg) Phase.READY_EXTENDED else Phase.UNKNOWN
                } else {
                    val elapsed = now - holdStartMs
                    val rem = (holdMs - elapsed).coerceAtLeast(0L)
                    holdRemaining = rem

                    // ✅ hold selesai -> rep dihitung -> minta luruskan
                    if (rem <= 0L) {
                        reps += 1
                        phase = Phase.NEED_EXTEND
                        holdStartMs = 0L
                        holdRemaining = 0L
                    }
                }
            }

            Phase.NEED_EXTEND -> {
                // tunggu user balik lurus
                if (angle >= extendedThresholdDeg) {
                    phase = Phase.READY_EXTENDED
                }
            }
        }

        return Frame(
            angleDeg = angle,
            elbowNx = elbow.x(),
            elbowNy = elbow.y(),
            reps = reps,
            phase = phase,
            holdRemainingMs = holdRemaining
        )
    }

    private fun smooth(v: Float): Float {
        if (win <= 1) return v
        buf[bufIdx] = v
        bufIdx = (bufIdx + 1) % win
        if (bufCount < win) bufCount++

        var s = 0f
        for (i in 0 until bufCount) s += buf[i]
        return s / bufCount
    }

    private fun angleDeg(
        ax: Float, ay: Float,
        bx: Float, by: Float,
        cx: Float, cy: Float
    ): Float {
        val abx = ax - bx
        val aby = ay - by
        val cbx = cx - bx
        val cby = cy - by

        val dot = abx * cbx + aby * cby
        val mag1 = sqrt(abx * abx + aby * aby)
        val mag2 = sqrt(cbx * cbx + cby * cby)
        val denom = (mag1 * mag2).coerceAtLeast(1e-6f)
        val ratio = dot / denom
        val cos = when {
            ratio < -1f -> -1f
            ratio > 1f -> 1f
            else -> ratio
        }
        val rad = acos(cos)
        return (rad * 180f / Math.PI.toFloat()).coerceIn(0f, 180f)
    }
}

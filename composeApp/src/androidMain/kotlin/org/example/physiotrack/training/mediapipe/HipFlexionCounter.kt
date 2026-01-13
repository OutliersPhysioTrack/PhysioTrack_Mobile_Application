package org.example.physiotrack.training.mediapipe

import android.os.SystemClock
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarkerResult

class HipFlexionCounter(
    private val side: Side = Side.LEFT,

    // ✅ threshold lift (delta Y): hipY - kneeY (positif = knee naik)
    private val upThreshold: Float = 0.03f,     // 5% (mulai dianggap naik)
    private val downThreshold: Float = 0.03f,   // harus lebih kecil dari upThreshold (hysteresis)

    // ✅ hold time
    private val holdMs: Long = 2000L,

    // smoothing (EMA)
    private val smoothingAlpha: Float = 0.25f,
) {
    enum class Side { LEFT, RIGHT }

    // DOWN -> UP_HOLDING -> UP(counted) -> DOWN
    enum class Phase { UNKNOWN, DOWN, UP_HOLDING, UP }

    data class Frame(
        val reps: Int,
        val phase: Phase,
        val kneeLift: Float,          // hipY - kneeY
        val holdRemainingMs: Long,    // ✅ countdown untuk UI
        val labelNx: Float,
        val labelNy: Float,
    )

    private var reps = 0
    private var phase: Phase = Phase.UNKNOWN

    // smoothing (EMA)
    private var smHipY: Float? = null
    private var smKneeY: Float? = null
    private var smKneeX: Float? = null

    // hold state
    private var holdStartMs: Long? = null

    fun reset() {
        reps = 0
        phase = Phase.UNKNOWN
        smHipY = null
        smKneeY = null
        smKneeX = null
        holdStartMs = null
    }

    fun update(result: PoseLandmarkerResult): Frame? {
        val pose = result.landmarks().firstOrNull() ?: return null

        // MediaPipe pose index umum:
        // LEFT: hip=23 knee=25
        // RIGHT: hip=24 knee=26
        val (hipIdx, kneeIdx) = when (side) {
            Side.LEFT -> 23 to 25
            Side.RIGHT -> 24 to 26
        }

        val hip = pose.getOrNull(hipIdx) ?: return null
        val knee = pose.getOrNull(kneeIdx) ?: return null

        val hipY = hip.y()
        val kneeY = knee.y()
        val kneeX = knee.x()

        // EMA smoothing
        smHipY = ema(smHipY, hipY)
        smKneeY = ema(smKneeY, kneeY)
        smKneeX = ema(smKneeX, kneeX)

        val sHipY = smHipY ?: hipY
        val sKneeY = smKneeY ?: kneeY
        val sKneeX = smKneeX ?: kneeX

        val kneeLift = (sHipY - sKneeY) // positif kalau knee lebih tinggi dari hip
        val isUp = kneeLift >= upThreshold
        val isDown = kneeLift <= downThreshold

        val now = SystemClock.uptimeMillis()

        when (phase) {
            Phase.UNKNOWN -> {
                phase = if (isUp) Phase.UP else Phase.DOWN
                holdStartMs = null
            }

            Phase.DOWN -> {
                holdStartMs = null
                if (isUp) {
                    // ✅ mulai HOLD saat baru dianggap UP
                    holdStartMs = now
                    phase = Phase.UP_HOLDING
                }
            }

            Phase.UP_HOLDING -> {
                if (!isUp) {
                    // ✅ turun sebelum hold selesai -> batal
                    holdStartMs = null
                    phase = Phase.DOWN
                } else {
                    val start = holdStartMs ?: now.also { holdStartMs = it }
                    val elapsed = now - start
                    if (elapsed >= holdMs) {
                        // ✅ hold selesai -> baru hitung rep
                        reps += 1
                        phase = Phase.UP
                    }
                }
            }

            Phase.UP -> {
                // ✅ harus kembali DOWN dulu supaya rep berikutnya tidak double
                if (isDown) {
                    phase = Phase.DOWN
                    holdStartMs = null
                }
            }
        }

        val holdRemaining = if (phase == Phase.UP_HOLDING) {
            val start = holdStartMs ?: now
            (holdMs - (now - start)).coerceAtLeast(0L)
        } else 0L

        return Frame(
            reps = reps,
            phase = phase,
            kneeLift = kneeLift,
            holdRemainingMs = holdRemaining,
            labelNx = sKneeX,
            labelNy = sKneeY
        )
    }

    private fun ema(prev: Float?, x: Float): Float {
        return if (prev == null) x else (prev + smoothingAlpha * (x - prev))
    }
}

package org.example.physiotrack.training.mediapipe

import android.os.SystemClock
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarkerResult
import kotlin.math.acos
import kotlin.math.sqrt

class KneeExtensionCounter(
    private val side: Side = Side.LEFT,
    private val holdMs: Long = 2000L,
    private val bentThresholdDeg: Float = 120f,     // start position (tekuk)
    private val extendedThresholdDeg: Float = 160f, // lurus
) {
    enum class Side { LEFT, RIGHT }

    // DOWN (tekuk) -> UP_HOLDING (lurus tahan) -> UP (rep counted) -> balik DOWN
    enum class Phase { UNKNOWN, DOWN, UP_HOLDING, UP }

    data class Frame(
        val kneeAngleDeg: Float,
        val reps: Int,
        val phase: Phase,
        val holdRemainingMs: Long,
        val labelNx: Float,
        val labelNy: Float,
    )

    private var reps: Int = 0
    private var phase: Phase = Phase.UNKNOWN
    private var reachedThisRep: Boolean = false
    private var holdStartMs: Long? = null

    fun reset() {
        reps = 0
        phase = Phase.UNKNOWN
        reachedThisRep = false
        holdStartMs = null
    }

    fun update(result: PoseLandmarkerResult): Frame? {
        val pose = result.landmarks().firstOrNull() ?: return null

        // MediaPipe indices:
        // LEFT: hip=23 knee=25 ankle=27
        // RIGHT: hip=24 knee=26 ankle=28
        val (hipIdx, kneeIdx, ankleIdx) = when (side) {
            Side.LEFT -> intArrayOf(23, 25, 27)
            Side.RIGHT -> intArrayOf(24, 26, 28)
        }

        val hip = pose.getOrNull(hipIdx) ?: return null
        val knee = pose.getOrNull(kneeIdx) ?: return null
        val ankle = pose.getOrNull(ankleIdx) ?: return null

        val kneeAngle = angleDeg(
            ax = hip.x(), ay = hip.y(),
            bx = knee.x(), by = knee.y(),
            cx = ankle.x(), cy = ankle.y()
        )

        val isDown = kneeAngle < bentThresholdDeg
        val isUpExtended = kneeAngle > extendedThresholdDeg

        val now = SystemClock.uptimeMillis()

        when (phase) {
            Phase.UNKNOWN -> {
                phase = when {
                    isUpExtended -> Phase.UP
                    isDown -> Phase.DOWN
                    else -> Phase.UNKNOWN
                }
            }

            Phase.DOWN -> {
                reachedThisRep = false
                holdStartMs = null
                if (isUpExtended) {
                    holdStartMs = now
                    phase = Phase.UP_HOLDING
                }
            }

            Phase.UP_HOLDING -> {
                if (!isUpExtended) {
                    // batal sebelum selesai
                    holdStartMs = null
                    phase = if (isDown) Phase.DOWN else Phase.DOWN
                } else {
                    val start = holdStartMs ?: now.also { holdStartMs = it }
                    val elapsed = now - start
                    if (elapsed >= holdMs && !reachedThisRep) {
                        reps += 1
                        reachedThisRep = true
                        phase = Phase.UP
                    }
                }
            }

            Phase.UP -> {
                // rep baru boleh setelah balik DOWN
                if (isDown) {
                    phase = Phase.DOWN
                    reachedThisRep = false
                    holdStartMs = null
                }
            }
        }

        val remaining = if (phase == Phase.UP_HOLDING) {
            val start = holdStartMs ?: now
            (holdMs - (now - start)).coerceAtLeast(0L)
        } else 0L

        return Frame(
            kneeAngleDeg = kneeAngle,
            reps = reps,
            phase = phase,
            holdRemainingMs = remaining,
            labelNx = knee.x(),
            labelNy = knee.y()
        )
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

        var cosv = dot / denom
        if (cosv < -1f) cosv = -1f
        if (cosv > 1f) cosv = 1f

        val rad = acos(cosv)
        return rad * 180f / Math.PI.toFloat()
    }
}

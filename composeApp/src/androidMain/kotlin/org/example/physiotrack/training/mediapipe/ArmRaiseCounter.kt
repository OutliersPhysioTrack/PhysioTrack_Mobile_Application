package org.example.physiotrack.training.mediapipe

import android.os.SystemClock
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarkerResult
import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.sqrt
import kotlin.math.max
import kotlin.math.min

class ArmRaiseCounter(
    private val side: Side = Side.LEFT,
    private val holdMs: Long = 2000L,

    // ✅ TUNING HOLD "lebih rendah"
    private val enterHoldAngleDeg: Float = 120f,   // tadinya >125, turunin
    private val stayHoldAngleDeg: Float = 110f,    // hysteresis biar gak jitter

    // ✅ wrist dibanding nose: makin kecil (lebih negatif) = makin "tinggi"
    // tadinya: wristY < noseY - 0.03f (cukup tinggi)
    // sekarang: masuk hold cukup sedikit di atas nose
    private val enterHoldWristAboveNose: Float = 0.01f,  // wristY < noseY + 0.01f  (lebih mudah)
    private val stayHoldWristAboveNose: Float = 0.03f,   // tetap hold walau turun dikit
) {
    enum class Side { LEFT, RIGHT }

    // DOWN -> AT_T -> OVERHEAD_HOLDING -> OVERHEAD (rep counted) -> back DOWN
    enum class Phase { UNKNOWN, DOWN, AT_T, OVERHEAD_HOLDING, OVERHEAD }

    data class Frame(
        val shoulderAngleDeg: Float,
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

        val (shoulderIdx, elbowIdx, wristIdx, hipIdx, noseIdx) = when (side) {
            Side.LEFT -> intArrayOf(11, 13, 15, 23, 0)
            Side.RIGHT -> intArrayOf(12, 14, 16, 24, 0)
        }

        val sh = pose.getOrNull(shoulderIdx) ?: return null
        val el = pose.getOrNull(elbowIdx) ?: return null
        val wr = pose.getOrNull(wristIdx) ?: return null
        val hip = pose.getOrNull(hipIdx) ?: return null
        val nose = pose.getOrNull(noseIdx) ?: return null

        val shoulderAngle = angleDeg(
            ax = hip.x(), ay = hip.y(),
            bx = sh.x(),  by = sh.y(),
            cx = el.x(),  cy = el.y()
        )

        val wristY = wr.y()
        val shoulderY = sh.y()
        val noseY = nose.y()

        val isDown = (wristY > shoulderY + 0.10f) && (shoulderAngle < 45f)

        val isAtT = (shoulderAngle in 75f..115f) && (abs(wristY - shoulderY) < 0.12f)

        // ✅ OVERHEAD ZONE pakai threshold yang lebih rendah
        val canEnterOverhead =
            (wristY < noseY + enterHoldWristAboveNose) && (shoulderAngle >= enterHoldAngleDeg)

        val canStayOverhead =
            (wristY < noseY + stayHoldWristAboveNose) && (shoulderAngle >= stayHoldAngleDeg)

        val now = SystemClock.uptimeMillis()

        when (phase) {
            Phase.UNKNOWN -> {
                phase = when {
                    canEnterOverhead -> Phase.OVERHEAD
                    isAtT -> Phase.AT_T
                    isDown -> Phase.DOWN
                    else -> Phase.UNKNOWN
                }
            }

            Phase.DOWN -> {
                reachedThisRep = false
                holdStartMs = null
                if (isAtT) phase = Phase.AT_T
            }

            Phase.AT_T -> {
                if (canEnterOverhead && !reachedThisRep) {
                    if (holdStartMs == null) holdStartMs = now
                    phase = Phase.OVERHEAD_HOLDING
                } else if (isDown) {
                    phase = Phase.DOWN
                }
            }

            Phase.OVERHEAD_HOLDING -> {
                if (!canStayOverhead) {
                    // turun sebelum hold selesai
                    holdStartMs = null
                    phase = when {
                        isAtT -> Phase.AT_T
                        isDown -> Phase.DOWN
                        else -> Phase.AT_T
                    }
                } else {
                    val start = holdStartMs ?: now.also { holdStartMs = it }
                    val elapsed = now - start
                    if (elapsed >= holdMs && !reachedThisRep) {
                        reps += 1
                        reachedThisRep = true
                        phase = Phase.OVERHEAD
                    }
                }
            }

            Phase.OVERHEAD -> {
                // rep baru hanya setelah kembali DOWN
                if (isDown) {
                    phase = Phase.DOWN
                    reachedThisRep = false
                    holdStartMs = null
                } else if (isAtT) {
                    phase = Phase.AT_T
                }
            }
        }

        val remaining = if (phase == Phase.OVERHEAD_HOLDING) {
            val start = holdStartMs ?: now
            (holdMs - (now - start)).coerceAtLeast(0L)
        } else 0L

        return Frame(
            shoulderAngleDeg = shoulderAngle,
            reps = reps,
            phase = phase,
            holdRemainingMs = remaining,
            labelNx = sh.x(),
            labelNy = sh.y()
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
        cosv = min(1f, max(-1f, cosv))

        val rad = acos(cosv)
        return rad * 180f / Math.PI.toFloat()
    }
}

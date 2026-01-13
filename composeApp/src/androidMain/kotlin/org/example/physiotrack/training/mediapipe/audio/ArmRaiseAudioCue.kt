package org.example.physiotrack.training.mediapipe.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import org.example.physiotrack.R
import org.example.physiotrack.training.mediapipe.ArmRaiseCounter
import kotlin.math.max

/**
 * Raw yang dibutuhkan:
 * start.mp3, side.mp3, up.mp3, hold.mp3, two.mp3, one.mp3, good.mp3, down.mp3
 *
 * Urutan yang diinginkan:
 * side -> up -> hold -> 2 -> 1 -> down -> (tangan sudah DOWN) good -> side (rep berikutnya)
 */
class ArmRaiseAudioCue(
    context: Context,
    private val holdMs: Long = 2000L,
) {
    private val soundPool: SoundPool = SoundPool.Builder()
        .setMaxStreams(1)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ASSISTANCE_ACCESSIBILITY)
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                .build()
        )
        .build()

//    private val sndStart = soundPool.load(context, R.raw.start, 1)
    private val sndSide  = soundPool.load(context, R.raw.side, 1)
    private val sndUp    = soundPool.load(context, R.raw.up, 1)
    private val sndHold  = soundPool.load(context, R.raw.hold, 1)
    private val sndTwo   = soundPool.load(context, R.raw.two, 1)
    private val sndOne   = soundPool.load(context, R.raw.one, 1)
    private val sndDown  = soundPool.load(context, R.raw.down, 1)
    private val sndGood  = soundPool.load(context, R.raw.good, 1)

    private val handler = Handler(Looper.getMainLooper())

    // ===== TUNING =====
    private val instructionGapMs: Long = 750L
    private val countdownGapMs: Long = 650L
    private val holdWordToTwoDelayMs: Long = 450L

    private val afterDownToGoodDelayMs: Long = 250L
    private val afterGoodToSideDelayMs: Long = 450L

    // queue anti overlap
    private var nextPlayAtMs: Long = 0L

    // ===== state =====
    private var lastPhase: ArmRaiseCounter.Phase? = null
    private var holdToken: Int = 0
    private var countdownEndsAtMs: Long = 0L

    // ✅ rep sukses: sudah selesai hold, kita sudah ngomong "down", tunggu sampai phase DOWN baru ngomong good->side
    private var awaitingGoodWhenDown: Boolean = false

//    fun playStart() {
//        reset()
//        val now = SystemClock.uptimeMillis()
//        enqueue(sndStart, earliestAtMs = now, gapAfterMs = instructionGapMs)
//        enqueue(sndSide, earliestAtMs = now + instructionGapMs, gapAfterMs = instructionGapMs)
//    }

    fun reset() {
        handler.removeCallbacksAndMessages(null)
        holdToken++
        lastPhase = null
        countdownEndsAtMs = 0L
        awaitingGoodWhenDown = false
        nextPlayAtMs = SystemClock.uptimeMillis()
    }

    fun playFirstInstruction(delayMs: Long = 0L) {
        val now = SystemClock.uptimeMillis()
        enqueue(sndSide, earliestAtMs = now + delayMs, gapAfterMs = instructionGapMs)
    }

    fun onFrame(frame: ArmRaiseCounter.Frame) {
        val phase = frame.phase
        val now = SystemClock.uptimeMillis()

        if (phase == lastPhase) return

        // keluar dari holding tapi bukan sukses -> batal countdown
        if (lastPhase == ArmRaiseCounter.Phase.OVERHEAD_HOLDING &&
            phase != ArmRaiseCounter.Phase.OVERHEAD
        ) {
            cancelHoldingCountdown()
        }

        when (phase) {
            ArmRaiseCounter.Phase.DOWN -> {
                // ✅ ini tempat GOOD yang kamu mau: setelah tangan benar2 turun
                if (awaitingGoodWhenDown) {
                    val goodAt = enqueue(
                        sndGood,
                        earliestAtMs = now + afterDownToGoodDelayMs,
                        gapAfterMs = instructionGapMs
                    )
                    enqueue(
                        sndSide,
                        earliestAtMs = goodAt + afterGoodToSideDelayMs,
                        gapAfterMs = instructionGapMs
                    )
                    awaitingGoodWhenDown = false
                }
            }

            ArmRaiseCounter.Phase.AT_T -> {
                // "up" hanya saat naik (AT_T dari DOWN)
                if (lastPhase == ArmRaiseCounter.Phase.DOWN) {
                    enqueue(sndUp, earliestAtMs = now, gapAfterMs = instructionGapMs)
                }
            }

            ArmRaiseCounter.Phase.OVERHEAD_HOLDING -> {
                enterHolding(now)
            }

            ArmRaiseCounter.Phase.OVERHEAD -> {
                // sukses hold: transisi dari HOLDING -> OVERHEAD
                if (lastPhase == ArmRaiseCounter.Phase.OVERHEAD_HOLDING) {
                    val downAt = max(now, countdownEndsAtMs + 120L) // aman setelah countdown selesai
                    enqueue(sndDown, earliestAtMs = downAt, gapAfterMs = instructionGapMs)

                    // ✅ tunggu sampai benar2 DOWN baru ngomong GOOD + SIDE
                    awaitingGoodWhenDown = true
                }
            }

            ArmRaiseCounter.Phase.UNKNOWN -> Unit
        }

        lastPhase = phase
    }

    private fun enterHolding(now: Long) {
        holdToken++
        val token = holdToken

        countdownEndsAtMs = now + holdMs

        // ✅ tahan dulu
        enqueue(sndHold, earliestAtMs = now, gapAfterMs = instructionGapMs)

        // 2 dan 1 dijadwalkan, tapi tetap pakai enqueue biar tidak nubruk
        handler.postDelayed({
            if (token != holdToken) return@postDelayed
            enqueue(sndTwo, earliestAtMs = SystemClock.uptimeMillis(), gapAfterMs = countdownGapMs)
        }, holdWordToTwoDelayMs)

        handler.postDelayed({
            if (token != holdToken) return@postDelayed
            enqueue(sndOne, earliestAtMs = SystemClock.uptimeMillis(), gapAfterMs = countdownGapMs)
        }, holdWordToTwoDelayMs + 1000L)
    }

    private fun cancelHoldingCountdown() {
        holdToken++
    }

    private fun enqueue(soundId: Int, earliestAtMs: Long, gapAfterMs: Long): Long {
        val now = SystemClock.uptimeMillis()
        val startAt = max(earliestAtMs, nextPlayAtMs)
        val delay = max(0L, startAt - now)

        handler.postDelayed({
            soundPool.play(soundId, 1f, 1f, 1, 0, 1f)
        }, delay)

        nextPlayAtMs = startAt + gapAfterMs
        return startAt
    }

    fun release() {
        reset()
        soundPool.release()
    }
}

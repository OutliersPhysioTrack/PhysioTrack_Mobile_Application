package org.example.physiotrack.training.mediapipe.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import org.example.physiotrack.R
import org.example.physiotrack.training.mediapipe.HipFlexionCounter
import kotlin.math.max

/**
 * Audio cue Hip Flexion:
 * start -> (delay) lift -> tahan -> 2 -> 1 -> lower -> (saat sudah DOWN) good -> (delay) lift lagi
 *
 * Raw wajib:
 * start.mp3, lift_knee.mp3, down.mp3, hold.mp3, two.mp3, one.mp3, good.mp3
 */
class HipFlexionAudioCue(
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

    //private val sndStart = soundPool.load(context, R.raw.start, 1)
    private val sndLift  = soundPool.load(context, R.raw.lift_knee, 1) // "angkat paha"
    private val sndLower = soundPool.load(context, R.raw.down, 1)      // "turunkan"
    private val sndHold  = soundPool.load(context, R.raw.hold, 1)      // "tahan"
    private val sndTwo   = soundPool.load(context, R.raw.two, 1)
    private val sndOne   = soundPool.load(context, R.raw.one, 1)
    private val sndGood  = soundPool.load(context, R.raw.good, 1)

    private val handler = Handler(Looper.getMainLooper())

    // ===== TUNING =====
    private val instructionGapMs: Long = 800L
    private val countdownGapMs: Long = 650L

    // delay ekstra biar gak nubruk
    private val startToLiftExtraDelayMs: Long = 450L
    private val afterLowerToGoodDelayMs: Long = 200L     // setelah DOWN, baru "good"
    private val afterGoodToLiftDelayMs: Long = 650L      // setelah "good", baru "angkat" lagi

    // biar "tahan" kedengeran dulu, baru "2"
    private val holdWordToCountdownDelayMs: Long = 350L

    // ===== State =====
    private var lastPhase: HipFlexionCounter.Phase? = null

    // cancel token untuk countdown kalau holding batal
    private var holdToken: Int = 0
    private var countdownEndsAtMs: Long = 0L

    // queue anti overlap
    private var nextPlayAtMs: Long = 0L

    // ✅ setelah hold sukses kita sudah bilang "turunkan",
    // lalu menunggu benar2 DOWN untuk bilang "good"
    private var awaitingGoodOnDown: Boolean = false

//    fun playStart() {
//        clearQueue()
//        val now = SystemClock.uptimeMillis()
//
//        val startAt = enqueue(sndStart, earliestAtMs = now, gapAfterMs = instructionGapMs)
//        enqueue(
//            sndLift,
//            earliestAtMs = startAt + instructionGapMs + startToLiftExtraDelayMs,
//            gapAfterMs = instructionGapMs
//        )
//
//        awaitingGoodOnDown = false
//        lastPhase = null
//    }

    fun reset() {
        handler.removeCallbacksAndMessages(null)
        holdToken++
        lastPhase = null
        countdownEndsAtMs = 0L
        nextPlayAtMs = SystemClock.uptimeMillis()
        awaitingGoodOnDown = false
    }

    fun playFirstInstruction(delayMs: Long = 0L) {
        val now = SystemClock.uptimeMillis()
        enqueue(sndLift, earliestAtMs = now + delayMs, gapAfterMs = instructionGapMs)
    }


    fun onFrame(frame: HipFlexionCounter.Frame) {
        val phase = frame.phase
        val now = SystemClock.uptimeMillis()

        // jangan spam kalau phase sama
        if (phase == lastPhase) return

        // kalau keluar dari UP_HOLDING tapi bukan sukses -> cancel countdown lama
        if (lastPhase == HipFlexionCounter.Phase.UP_HOLDING &&
            phase != HipFlexionCounter.Phase.UP
        ) {
            cancelHoldingCountdown()
        }

        when (phase) {
            HipFlexionCounter.Phase.UP_HOLDING -> {
                enterHolding(now)
            }

            HipFlexionCounter.Phase.UP -> {
                // ✅ hold sukses -> setelah countdown selesai: suruh "turunkan"
                // ✅ GOOD JANGAN DI SINI. Good nanti saat DOWN.
                val safeAt = max(now, countdownEndsAtMs)
                enqueue(sndLower, earliestAtMs = safeAt, gapAfterMs = instructionGapMs)
                awaitingGoodOnDown = true
            }

            HipFlexionCounter.Phase.DOWN -> {
                cancelHoldingCountdown()

                if (awaitingGoodOnDown) {
                    // ✅ sekarang baru "good" (karena sudah benar2 turun)
                    val goodAt = enqueue(
                        sndGood,
                        earliestAtMs = now + afterLowerToGoodDelayMs,
                        gapAfterMs = instructionGapMs
                    )

                    // ✅ setelah good, baru lift lagi (rep berikutnya)
                    enqueue(
                        sndLift,
                        earliestAtMs = goodAt + afterGoodToLiftDelayMs,
                        gapAfterMs = instructionGapMs
                    )

                    awaitingGoodOnDown = false
                }
            }

            HipFlexionCounter.Phase.UNKNOWN -> {
                // no-op
            }
        }

        lastPhase = phase
    }

    // ===== HOLDING: tahan -> (delay) 2 -> 1 =====
    private fun enterHolding(now: Long) {
        holdToken++
        val token = holdToken

        countdownEndsAtMs = now + holdMs

        // "tahan"
        val holdAt = enqueue(sndHold, earliestAtMs = now, gapAfterMs = instructionGapMs)
        val base = holdAt + holdWordToCountdownDelayMs

        // "2"
        handler.postDelayed({
            if (token != holdToken) return@postDelayed
            enqueue(sndTwo, earliestAtMs = SystemClock.uptimeMillis(), gapAfterMs = countdownGapMs)
        }, max(0L, base - SystemClock.uptimeMillis()))

        // "1" (1 detik setelah base)
        handler.postDelayed({
            if (token != holdToken) return@postDelayed
            enqueue(sndOne, earliestAtMs = SystemClock.uptimeMillis(), gapAfterMs = countdownGapMs)
        }, max(0L, (base + 1000L) - SystemClock.uptimeMillis()))
    }

    private fun cancelHoldingCountdown() {
        holdToken++
    }

    // ===== Queue playback helper =====
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

    private fun clearQueue() {
        handler.removeCallbacksAndMessages(null)
        nextPlayAtMs = SystemClock.uptimeMillis()
    }

    fun release() {
        reset()
        soundPool.release()
    }
}

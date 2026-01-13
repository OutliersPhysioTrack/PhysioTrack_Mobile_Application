package org.example.physiotrack.training.mediapipe.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import org.example.physiotrack.R
import org.example.physiotrack.training.mediapipe.KneeExtensionCounter
import kotlin.math.max

/**
 * Needs raw:
 * extend.mp3 (atau pakai extend yang sudah ada)
 * down.mp3
 * hold.mp3
 * two.mp3
 * one.mp3
 * good.mp3
 *
 * Urutan:
 * extend -> hold -> 2 -> 1 -> down -> (ketika balik DOWN) good -> extend (rep berikutnya)
 */
class KneeExtensionAudioCue(
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

    private val sndExtend = soundPool.load(context, R.raw.tekuk, 1) // "luruskan"
    private val sndDown   = soundPool.load(context, R.raw.luruslutut, 1)   // "turunkan"
    private val sndHold   = soundPool.load(context, R.raw.hold, 1)
    private val sndTwo    = soundPool.load(context, R.raw.two, 1)
    private val sndOne    = soundPool.load(context, R.raw.one, 1)
    private val sndGood   = soundPool.load(context, R.raw.good, 1)

    private val handler = Handler(Looper.getMainLooper())

    private val instructionGapMs: Long = 750L
    private val countdownGapMs: Long = 650L
    private val holdWordToTwoDelayMs: Long = 450L
    private val afterDownToGoodDelayMs: Long = 250L
    private val afterGoodToExtendDelayMs: Long = 450L

    private var nextPlayAtMs: Long = 0L
    private var lastPhase: KneeExtensionCounter.Phase? = null
    private var holdToken: Int = 0
    private var countdownEndsAtMs: Long = 0L
    private var awaitingGoodWhenDown: Boolean = false

    fun reset() {
        handler.removeCallbacksAndMessages(null)
        holdToken++
        lastPhase = null
        countdownEndsAtMs = 0L
        awaitingGoodWhenDown = false
        nextPlayAtMs = SystemClock.uptimeMillis()
    }

    /** ✅ dipanggil setelah countdown selesai supaya instruksi pertama langsung keluar */
    fun primeFirstInstruction() {
        val now = SystemClock.uptimeMillis()
        enqueue(sndExtend, earliestAtMs = now + 150L, gapAfterMs = instructionGapMs)
    }

    fun onFrame(frame: KneeExtensionCounter.Frame) {
        val phase = frame.phase
        val now = SystemClock.uptimeMillis()

        if (phase == lastPhase) return

        // keluar dari holding tapi bukan sukses -> batal countdown
        if (lastPhase == KneeExtensionCounter.Phase.UP_HOLDING &&
            phase != KneeExtensionCounter.Phase.UP
        ) {
            cancelHoldingCountdown()
        }

        when (phase) {
            KneeExtensionCounter.Phase.DOWN -> {
                if (awaitingGoodWhenDown) {
                    val goodAt = enqueue(
                        sndGood,
                        earliestAtMs = now + afterDownToGoodDelayMs,
                        gapAfterMs = instructionGapMs
                    )
                    enqueue(
                        sndExtend,
                        earliestAtMs = goodAt + afterGoodToExtendDelayMs,
                        gapAfterMs = instructionGapMs
                    )
                    awaitingGoodWhenDown = false
                }
            }

            KneeExtensionCounter.Phase.UP_HOLDING -> {
                enterHolding(now)
            }

            KneeExtensionCounter.Phase.UP -> {
                if (lastPhase == KneeExtensionCounter.Phase.UP_HOLDING) {
                    val downAt = max(now, countdownEndsAtMs + 120L)
                    enqueue(sndDown, earliestAtMs = downAt, gapAfterMs = instructionGapMs)
                    awaitingGoodWhenDown = true
                }
            }

            KneeExtensionCounter.Phase.UNKNOWN -> Unit
        }

        lastPhase = phase
    }

    private fun enterHolding(now: Long) {
        holdToken++
        val token = holdToken

        countdownEndsAtMs = now + holdMs

        enqueue(sndHold, earliestAtMs = now, gapAfterMs = instructionGapMs)

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

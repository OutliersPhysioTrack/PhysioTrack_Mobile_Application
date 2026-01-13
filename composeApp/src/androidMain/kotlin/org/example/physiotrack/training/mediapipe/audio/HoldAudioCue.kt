package org.example.physiotrack.training.mediapipe.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import org.example.physiotrack.R
import org.example.physiotrack.training.mediapipe.ElbowFlexionCounter
import kotlin.math.max

class HoldAudioCue(context: Context) {

    private val soundPool: SoundPool = SoundPool.Builder()
        .setMaxStreams(1) // 1 stream saja, urutan dijaga pakai queue
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ASSISTANCE_ACCESSIBILITY)
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                .build()
        )
        .build()

    // raw: bend.mp3 extend.mp3 hold.mp3 two.mp3 one.mp3 good.mp3
    private val sndBend   = soundPool.load(context, R.raw.bend, 1)
    private val sndExtend = soundPool.load(context, R.raw.extend, 1)
    private val sndHold   = soundPool.load(context, R.raw.hold, 1)   // ✅ "tahan"
    private val sndTwo    = soundPool.load(context, R.raw.two, 1)
    private val sndOne    = soundPool.load(context, R.raw.one, 1)
    private val sndGood   = soundPool.load(context, R.raw.good, 1)

    private val handler = Handler(Looper.getMainLooper())

    // ===== TUNING =====
    private val holdMs: Long = 2000L

    private val instructionGapMs: Long = 750L
    private val countdownGapMs: Long = 650L

    // ✅ kasih jeda sebentar setelah kata "hold/tahan" sebelum angka "2"
    // kalau file hold kamu pendek, 500-650ms enak.
    private val holdWordLeadMs: Long = 650L

    private val extendAfterCountdownPadMs: Long = 150L

    // ✅ AKTIFKAN: sekarang akan ngomong "tahan" sebelum 2-1
    private val speakHoldWord: Boolean = true

    // ===== State =====
    private var lastPhase: ElbowFlexionCounter.Phase? = null
    private var awaitingGoodAfterExtend: Boolean = false

    // cancel token untuk countdown kalau holding batal (bukan sukses)
    private var holdToken: Int = 0

    // untuk memastikan extend selalu setelah countdown
    private var countdownEndsAtMs: Long = 0L

    // queue playback supaya tidak potong audio
    private var nextPlayAtMs: Long = 0L

    fun reset() {
        handler.removeCallbacksAndMessages(null)
        holdToken++
        lastPhase = null
        awaitingGoodAfterExtend = false
        countdownEndsAtMs = 0L
        nextPlayAtMs = SystemClock.uptimeMillis()
    }

    fun onFrame(frame: ElbowFlexionCounter.Frame) {
        val phase = frame.phase
        val now = SystemClock.uptimeMillis()

        // kalau phase tidak berubah, gak perlu ngomong instruksi lagi
        if (phase == lastPhase) return

        // kalau keluar dari HOLDING tapi bukan sukses (NEED_EXTEND),
        // berarti holding batal -> cancel countdown lama
        if (lastPhase == ElbowFlexionCounter.Phase.HOLDING &&
            phase != ElbowFlexionCounter.Phase.NEED_EXTEND
        ) {
            cancelHoldingCountdown()
        }

        when (phase) {
            ElbowFlexionCounter.Phase.HOLDING -> {
                enterHolding(now)
            }

            ElbowFlexionCounter.Phase.NEED_EXTEND -> {
                // holding sukses -> jangan cancel countdown
                awaitingGoodAfterExtend = true

                // extend HARUS setelah countdown selesai
                val safeAt = max(now, countdownEndsAtMs + extendAfterCountdownPadMs)
                enqueue(sndExtend, earliestAtMs = safeAt, gapAfterMs = instructionGapMs)
            }

            ElbowFlexionCounter.Phase.READY_EXTENDED -> {
                if (awaitingGoodAfterExtend) {
                    // GOOD dulu (setelah countdown selesai), lalu BEND lagi untuk rep berikutnya
                    val safeAt = max(now, countdownEndsAtMs)
                    val goodAt = enqueue(sndGood, earliestAtMs = safeAt, gapAfterMs = instructionGapMs)
                    enqueue(sndBend, earliestAtMs = goodAt + instructionGapMs, gapAfterMs = instructionGapMs)

                    awaitingGoodAfterExtend = false
                } else {
                    // awal / idle extended -> suruh bend
                    enqueue(sndBend, earliestAtMs = now, gapAfterMs = instructionGapMs)
                }
            }

            else -> {
                // phase lain: gak usah ngomong apa2
            }
        }

        lastPhase = phase
    }

    // ===== HOLDING: sekarang urutan "HOLD/Tahan" -> 2 -> 1 =====
    private fun enterHolding(now: Long) {
        holdToken++
        val token = holdToken

        // selesai holding (untuk extend/good timing)
        countdownEndsAtMs = now + holdMs

        // 1) kata "hold/tahan" dulu (kalau aktif)
        if (speakHoldWord) {
            // gapAfterMs dibuat holdWordLeadMs supaya angka 2 tidak start terlalu cepat
            enqueue(sndHold, earliestAtMs = now, gapAfterMs = holdWordLeadMs)
        }

        // 2) "two" setelah holdWordLeadMs (atau langsung kalau speakHoldWord=false)
        val twoDelay = if (speakHoldWord) holdWordLeadMs else 0L
        handler.postDelayed({
            if (token != holdToken) return@postDelayed
            enqueue(sndTwo, earliestAtMs = SystemClock.uptimeMillis(), gapAfterMs = countdownGapMs)
        }, twoDelay)

        // 3) "one" satu detik setelah "two"
        handler.postDelayed({
            if (token != holdToken) return@postDelayed
            enqueue(sndOne, earliestAtMs = SystemClock.uptimeMillis(), gapAfterMs = countdownGapMs)
        }, twoDelay + 1000L)
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

    fun release() {
        reset()
        soundPool.release()
    }
}

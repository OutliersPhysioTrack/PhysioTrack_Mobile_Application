package org.example.physiotrack.training.mediapipe.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import org.example.physiotrack.R
import kotlin.math.max

/**
 * Pre-start countdown:
 * ready -> 5 -> 4 -> 3 -> 2 -> 1 -> go
 *
 * raw wajib:
 * ready.mp3, five.mp3, four.mp3, three.mp3, two.mp3, one.mp3, go.mp3
 */
class PreStartCountdownCue(context: Context) {

    private val soundPool: SoundPool = SoundPool.Builder()
        .setMaxStreams(1)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ASSISTANCE_ACCESSIBILITY)
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                .build()
        )
        .build()

    private val sndReady = soundPool.load(context, R.raw.siap, 1)
    private val snd5 = soundPool.load(context, R.raw.lima, 1)
    private val snd4 = soundPool.load(context, R.raw.empat, 1)
    private val snd3 = soundPool.load(context, R.raw.tiga, 1)
    private val snd2 = soundPool.load(context, R.raw.dua, 1)
    private val snd1 = soundPool.load(context, R.raw.satu, 1)
    private val sndGo = soundPool.load(context, R.raw.start, 1)

    private val handler = Handler(Looper.getMainLooper())

    // queue anti nubruk
    private var nextPlayAtMs: Long = 0L
    private var token: Int = 0

    fun reset() {
        handler.removeCallbacksAndMessages(null)
        token++
        nextPlayAtMs = SystemClock.uptimeMillis()
    }

    /**
     * Menjalankan countdown dan memanggil onGo() tepat setelah audio "go" dijadwalkan.
     */
    fun start(onGo: () -> Unit) {
        reset()
        val now = SystemClock.uptimeMillis()
        val myToken = token

        // atur jarak aman antar angka
        val gap = 650L

        enqueue(sndReady, now, gap)

        enqueue(snd5, now + 1_000L, gap)
        enqueue(snd4, now + 2_000L, gap)
        enqueue(snd3, now + 3_000L, gap)
        enqueue(snd2, now + 4_000L, gap)
        enqueue(snd1, now + 5_000L, gap)

        val goAt = enqueue(sndGo, now + 5_700L, gap)

        // callback setelah "go" mulai diputar (sedikit pad)
        handler.postDelayed({
            if (myToken != token) return@postDelayed
            onGo()
        }, max(0L, (goAt + 100L) - SystemClock.uptimeMillis()))
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

package org.example.physiotrack.training.mediapipe.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.example.physiotrack.R

/**
 * GripTestAudioCue (androidMain)
 *
 * Expected res/raw:
 *  - posisikan_tanganrilek.mp3
 *  - tarik_napassiap.mp3
 *  - tiga.mp3, dua.mp3, satu.mp3
 *  - tekan_sekarng.mp3 (atau sesuai nama kamu)
 *  - selesai_lepaskan.mp3
 *  - siap_nextattemp.mp3
 *  - goodjob.mp3
 */
class GripTestAudioCue(
    context: Context,
    private val gapMs: Long = 220L, // ✅ gap dasar antar clip
) {
    // ✅ TUNING JEDA (ini yang kamu mau “ditambah”)
    private val pauseAfterPosisikanMs: Long = 1200L   // posisikan... -> (jeda) -> tarik napas...
    private val pauseAfterTarikMs: Long = 1100L       // tarik napas... -> (jeda) -> masuk countdown 3
    private val pauseAfterSelesaiBeforeGoodJobMs: Long = 900L // selesai lepaskan -> (jeda) -> good job
    private val pauseAfterSiapNextAttemptMs: Long = 800L      // siap next attempt -> (jeda) -> 3

    // ---- public API ----

    fun reset() {
        seqJob?.cancel()
        seqJob = null
        streamIds.values.forEach { id -> runCatching { soundPool.stop(id) } }
        streamIds.clear()
    }

    fun release() {
        reset()
        runCatching { soundPool.release() }
        scopeJob.cancel()
    }

    /**
     * Play audio untuk memulai attempt.
     * attempt 1: posisikan -> tarik napas -> 3,2,1 -> tekan sekarang
     * attempt 2/3: siap next attempt -> 3,2,1 -> tekan sekarang
     */
    fun playStartAttempt(isFirstAttempt: Boolean) {
        if (isFirstAttempt) {
            playSequence(
                Clip.POSISIKAN_TANGAN_RILEK,
                Clip.TARIK_NAPAS_SIAP,
                Clip.TIGA,
                Clip.DUA,
                Clip.SATU,
                Clip.TEKAN_SEKARANG
            )
        } else {
            playSequence(
                Clip.SIAP_NEXT_ATTEMPT,
                Clip.TIGA,
                Clip.DUA,
                Clip.SATU,
                Clip.TEKAN_SEKARANG
            )
        }
    }

    /**
     * Dipakai di GripTestScreen untuk delay sebelum UI countdown muncul (biar sinkron suara).
     */
    fun msBeforeCountdown(isFirstAttempt: Boolean): Long {
        return if (isFirstAttempt) {
            Clip.POSISIKAN_TANGAN_RILEK.approxMs +
                    gapMs +
                    pauseAfterPosisikanMs +
                    Clip.TARIK_NAPAS_SIAP.approxMs +
                    gapMs +
                    pauseAfterTarikMs
        } else {
            Clip.SIAP_NEXT_ATTEMPT.approxMs +
                    gapMs +
                    pauseAfterSiapNextAttemptMs
        }
    }

    /**
     * Setelah measuring selesai:
     * - bukan terakhir: selesai lepaskan
     * - terakhir: selesai lepaskan -> (jeda) -> good job
     */
    fun playRelease(isLastAttempt: Boolean) {
        if (isLastAttempt) {
            playSequence(Clip.SELESAI_LEPASKAN, Clip.GOOD_JOB)
        } else {
            playSequence(Clip.SELESAI_LEPASKAN)
        }
    }

    // ---- implementation details ----

    private enum class Clip(val resId: Int, val approxMs: Long) {
        POSISIKAN_TANGAN_RILEK(R.raw.posisikan_tanganrilek, 1700L),
        TARIK_NAPAS_SIAP(R.raw.tarik_napassiap, 1700L),

        TIGA(R.raw.tiga, 900L),
        DUA(R.raw.dua, 900L),
        SATU(R.raw.satu, 900L),

        TEKAN_SEKARANG(R.raw.tekan_sekarng, 1400L),

        SELESAI_LEPASKAN(R.raw.selesai_lepaskan, 1400L),
        GOOD_JOB(R.raw.goodjob, 1200L),

        SIAP_NEXT_ATTEMPT(R.raw.siap_nextattemp, 1500L),
    }

    private val scopeJob = SupervisorJob()
    private val scope = CoroutineScope(scopeJob + Dispatchers.Main.immediate)

    private val soundPool: SoundPool
    private val soundIds = mutableMapOf<Clip, Int>()
    private val streamIds = mutableMapOf<Clip, Int>()

    private var seqJob: Job? = null

    private val loaded = CompletableDeferred<Unit>()
    private var pendingLoads = 0

    init {
        val attrs = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ASSISTANCE_ACCESSIBILITY)
            .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
            .build()

        soundPool = SoundPool.Builder()
            .setMaxStreams(2)
            .setAudioAttributes(attrs)
            .build()

        soundPool.setOnLoadCompleteListener { _, _, _ ->
            pendingLoads -= 1
            if (pendingLoads <= 0 && !loaded.isCompleted) loaded.complete(Unit)
        }

        Clip.entries.forEach { clip ->
            pendingLoads += 1
            val id = soundPool.load(context, clip.resId, 1)
            soundIds[clip] = id
        }
    }

    private fun playSequence(vararg clips: Clip) {
        seqJob?.cancel()

        seqJob = scope.launch {
            runCatching { loaded.await() }

            for (i in clips.indices) {
                val clip = clips[i]
                val nextClip: Clip? = clips.getOrNull(i + 1)

                val sid = soundIds[clip] ?: continue

                val streamId = soundPool.play(
                    sid,
                    1f,
                    1f,
                    1,
                    0,
                    1f
                )
                if (streamId != 0) streamIds[clip] = streamId

                // delay dasar = durasi perkiraan + gap
                var waitMs = clip.approxMs + gapMs

                // ✅ JEDA TAMBAHAN ANTAR CLIP TERTENTU
                if (clip == Clip.POSISIKAN_TANGAN_RILEK) {
                    waitMs += pauseAfterPosisikanMs
                }

                if (clip == Clip.TARIK_NAPAS_SIAP) {
                    waitMs += pauseAfterTarikMs
                }

                if (clip == Clip.SIAP_NEXT_ATTEMPT) {
                    waitMs += pauseAfterSiapNextAttemptMs
                }

                // ✅ khusus: selesai lepaskan -> good job (biar gak “good job” nabrak)
                if (clip == Clip.SELESAI_LEPASKAN && nextClip == Clip.GOOD_JOB) {
                    waitMs += pauseAfterSelesaiBeforeGoodJobMs
                }

                delay(waitMs)
            }
        }
    }
}

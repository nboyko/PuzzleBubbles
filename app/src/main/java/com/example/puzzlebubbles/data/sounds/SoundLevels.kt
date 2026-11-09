package com.example.puzzlebubbles.data.sounds

import kotlin.math.abs
import kotlin.math.sqrt

/**
 * Loudness normalization shared by every sound in the app. Loudness is the RMS of
 * the active (non-silent) part of a sound, so pauses before or after a sound do not
 * make it look quieter than it sounds. Every sound is brought to [TARGET_RMS]:
 * recordings by re-encoding with gain, bundled sounds by lowering playback volume.
 */
object SoundLevels {
    /**
     * Common loudness for all sounds (about -22 dBFS). Bundled files can only be
     * turned down at playback, so this stays at or below the quietest bundled sound.
     */
    const val TARGET_RMS = 0.08f
    /** Recordings are never boosted past about -1 dBFS peaks, so gain cannot clip. */
    const val PEAK_LIMIT = 0.89f
    /** Caps the boost (+20 dB) so near-silent recordings do not become loud hiss. */
    const val MAX_GAIN = 10f
    private const val WINDOW_MS = 50
    private const val ACTIVE_RATIO = 0.1f // Windows within 20 dB of the loudest window.

    fun peak(samples: ShortArray): Float =
        samples.maxOfOrNull { abs(it.toInt()) }?.let { it / 32768f } ?: 0f

    fun activeRms(samples: ShortArray, sampleRate: Int, channels: Int): Float {
        val window = (sampleRate * channels * WINDOW_MS / 1000).coerceAtLeast(1)
        val windows = samples.indices.step(window).map { start ->
            var sum = 0.0
            val end = minOf(start + window, samples.size)
            for (i in start until end) { val v = samples[i] / 32768.0; sum += v * v }
            sqrt(sum / (end - start)).toFloat()
        }
        val loudest = windows.maxOrNull() ?: return 0f
        if (loudest == 0f) return 0f
        val active = windows.filter { it >= loudest * ACTIVE_RATIO }
        return sqrt(active.sumOf { (it * it).toDouble() } / active.size).toFloat()
    }

    /** Gain that brings a recording to [TARGET_RMS] without clipping or over-boosting noise. */
    fun recordingGain(peak: Float, activeRms: Float): Float {
        if (peak <= 0f || activeRms <= 0f) return 1f
        return minOf(TARGET_RMS / activeRms, PEAK_LIMIT / peak, MAX_GAIN)
    }

    /** Playback volume (0..1) that brings a bundled sound down to [TARGET_RMS]. */
    fun playbackVolume(activeRms: Float): Float =
        if (activeRms <= 0f) 1f else (TARGET_RMS / activeRms).coerceAtMost(1f)

    fun level(pcm: Pcm): Float = activeRms(pcm.samples, pcm.sampleRate, pcm.channels)

    fun applyGain(samples: ShortArray, gain: Float): ShortArray = ShortArray(samples.size) {
        (samples[it] * gain).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
    }
}

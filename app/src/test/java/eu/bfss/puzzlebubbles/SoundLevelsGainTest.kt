package eu.bfss.puzzlebubbles

import eu.bfss.puzzlebubbles.data.sounds.SoundLevels
import org.junit.Assert.assertEquals
import org.junit.Test

class SoundLevelsGainTest {
    @Test fun quietSpeechIsRaisedToTarget() =
        assertEquals(3.2f, SoundLevels.recordingGain(peak = .1f, activeRms = .025f), 1e-4f)

    @Test fun gainNeverClipsPeaks() =
        assertEquals(.89f / .3f, SoundLevels.recordingGain(peak = .3f, activeRms = .01f), 1e-4f)

    @Test fun nearSilenceIsNotOverBoosted() =
        assertEquals(SoundLevels.MAX_GAIN, SoundLevels.recordingGain(peak = .001f, activeRms = .0005f), 1e-4f)

    @Test fun loudRecordingIsLowered() =
        assertEquals(.4f, SoundLevels.recordingGain(peak = .9f, activeRms = .2f), 1e-4f)

    @Test fun silenceIsUnchanged() = assertEquals(1f, SoundLevels.recordingGain(0f, 0f))

    @Test fun loudBundledSoundIsTurnedDownToTarget() =
        assertEquals(SoundLevels.TARGET_RMS, .12f * SoundLevels.playbackVolume(.12f), 1e-4f)

    @Test fun playbackNeverBoostsAboveFullVolume() =
        assertEquals(1f, SoundLevels.playbackVolume(.02f))

    @Test fun pausesDoNotLowerMeasuredLevel() {
        val rate = 1_000
        val tone = ShortArray(rate) { if (it % 2 == 0) 3277 else -3277 } // 0.1 RMS for one second
        val padded = ShortArray(rate) + tone + ShortArray(rate)
        assertEquals(.1f, SoundLevels.activeRms(padded, rate, 1), .005f)
    }
}

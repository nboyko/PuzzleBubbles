package com.example.puzzlebubbles.data.sounds

import android.media.AudioFormat
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMuxer
import java.io.File
import java.io.IOException
import java.nio.ByteOrder
import kotlin.math.abs

/** Decoded 16-bit PCM, interleaved when [channels] > 1. */
class Pcm(val samples: ShortArray, val sampleRate: Int, val channels: Int)

/** Brings finished recordings to the shared loudness level of [SoundLevels]. */
object SoundNormalizer {
    private const val TIMEOUT_US = 10_000L
    private const val MAX_LOOPS = 20_000

    /**
     * Rewrites [file] with calibrated gain. [validate] must throw for an unusable
     * result; the original file is kept untouched if anything fails.
     */
    fun normalize(file: File, validate: (File) -> Unit) {
        val pcm = decode { it.setDataSource(file.path) }
        val gain = SoundLevels.recordingGain(SoundLevels.peak(pcm.samples),
            SoundLevels.activeRms(pcm.samples, pcm.sampleRate, pcm.channels))
        if (abs(gain - 1f) < 0.05f) return
        val normalized = File(file.parentFile, "${file.name}.level")
        try {
            encodeAac(Pcm(SoundLevels.applyGain(pcm.samples, gain), pcm.sampleRate, pcm.channels), normalized)
            validate(normalized)
            if (!normalized.renameTo(file)) throw IOException("Cannot replace recording")
        } finally { normalized.delete() }
    }

    fun decode(source: (MediaExtractor) -> Unit): Pcm {
        val extractor = MediaExtractor()
        var decoder: MediaCodec? = null
        try {
            source(extractor)
            val track = (0 until extractor.trackCount).firstOrNull {
                extractor.getTrackFormat(it).getString(MediaFormat.KEY_MIME)?.startsWith("audio/") == true
            } ?: throw IOException("No audio track")
            extractor.selectTrack(track)
            val format = extractor.getTrackFormat(track)
            var sampleRate = format.getInteger(MediaFormat.KEY_SAMPLE_RATE)
            var channels = format.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
            var floatPcm = false
            val codec = MediaCodec.createDecoderByType(format.getString(MediaFormat.KEY_MIME)!!)
            decoder = codec
            codec.configure(format, null, null, 0)
            codec.start()
            var samples = ShortArray(sampleRate * channels)
            var count = 0
            val info = MediaCodec.BufferInfo()
            var inputDone = false
            var outputDone = false
            var loops = 0
            while (!outputDone) {
                if (loops++ > MAX_LOOPS) throw IOException("Audio decoder did not finish")
                if (!inputDone) {
                    val index = codec.dequeueInputBuffer(TIMEOUT_US)
                    if (index >= 0) {
                        val size = extractor.readSampleData(codec.getInputBuffer(index)!!, 0)
                        if (size < 0) {
                            codec.queueInputBuffer(index, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                            inputDone = true
                        } else {
                            codec.queueInputBuffer(index, 0, size, extractor.sampleTime, 0)
                            extractor.advance()
                        }
                    }
                }
                val index = codec.dequeueOutputBuffer(info, TIMEOUT_US)
                if (index == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                    val output = codec.outputFormat
                    sampleRate = output.getInteger(MediaFormat.KEY_SAMPLE_RATE)
                    channels = output.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
                    floatPcm = output.containsKey(MediaFormat.KEY_PCM_ENCODING) &&
                        output.getInteger(MediaFormat.KEY_PCM_ENCODING) == AudioFormat.ENCODING_PCM_FLOAT
                } else if (index >= 0) {
                    val buffer = codec.getOutputBuffer(index)!!.order(ByteOrder.nativeOrder())
                    buffer.position(info.offset).limit(info.offset + info.size)
                    val chunk = if (floatPcm) buffer.asFloatBuffer().let { floats ->
                        ShortArray(floats.remaining()) { (floats.get() * 32767f).toInt().coerceIn(-32768, 32767).toShort() }
                    } else buffer.asShortBuffer().let { shorts -> ShortArray(shorts.remaining()).also { shorts.get(it) } }
                    if (count + chunk.size > samples.size) samples = samples.copyOf(maxOf(samples.size * 2, count + chunk.size))
                    chunk.copyInto(samples, count)
                    count += chunk.size
                    outputDone = info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0
                    codec.releaseOutputBuffer(index, false)
                }
            }
            return Pcm(samples.copyOf(count), sampleRate, channels)
        } finally {
            decoder?.let { runCatching { it.stop() }; it.release() }
            extractor.release()
        }
    }

    fun encodeAac(pcm: Pcm, output: File) {
        val format = MediaFormat.createAudioFormat(MediaFormat.MIMETYPE_AUDIO_AAC, pcm.sampleRate, pcm.channels).apply {
            setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC)
            setInteger(MediaFormat.KEY_BIT_RATE, 64_000)
            setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, 16_384)
        }
        val encoder = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_AUDIO_AAC)
        val muxer = MediaMuxer(output.path, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
        var muxing = false
        try {
            encoder.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
            encoder.start()
            var written = 0
            var track = -1
            val info = MediaCodec.BufferInfo()
            var inputDone = false
            var outputDone = false
            var loops = 0
            while (!outputDone) {
                if (loops++ > MAX_LOOPS) throw IOException("AAC encoder did not finish")
                if (!inputDone) {
                    val index = encoder.dequeueInputBuffer(TIMEOUT_US)
                    if (index >= 0) {
                        val input = encoder.getInputBuffer(index)!!.order(ByteOrder.nativeOrder())
                        input.clear()
                        // Whole frames only, so channels stay interleaved correctly.
                        val count = minOf(input.capacity() / 2 / pcm.channels * pcm.channels, pcm.samples.size - written)
                        input.asShortBuffer().put(pcm.samples, written, count)
                        val timeUs = written / pcm.channels * 1_000_000L / pcm.sampleRate
                        written += count
                        inputDone = written >= pcm.samples.size
                        encoder.queueInputBuffer(index, 0, count * 2, timeUs,
                            if (inputDone) MediaCodec.BUFFER_FLAG_END_OF_STREAM else 0)
                    }
                }
                val index = encoder.dequeueOutputBuffer(info, TIMEOUT_US)
                if (index == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                    track = muxer.addTrack(encoder.outputFormat)
                    muxer.start()
                    muxing = true
                } else if (index >= 0) {
                    if (info.size > 0 && info.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG == 0) {
                        check(muxing) { "Encoder output before format" }
                        val encoded = encoder.getOutputBuffer(index)!!
                        encoded.position(info.offset).limit(info.offset + info.size)
                        muxer.writeSampleData(track, encoded, info)
                    }
                    outputDone = info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0
                    encoder.releaseOutputBuffer(index, false)
                }
            }
        } finally {
            runCatching { encoder.stop() }
            encoder.release()
            if (muxing) runCatching { muxer.stop() }
            muxer.release()
        }
    }
}

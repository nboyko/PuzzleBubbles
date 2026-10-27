package com.example.puzzlebubbles

import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.media.MediaMuxer
import java.io.File
import java.nio.ByteOrder
import kotlin.math.sin

/** Test-owned AAC tone, generated without accessing the microphone or bundled content. */
internal fun aacFixture(file: File, durationMs: Int = 800) {
    file.parentFile!!.mkdirs()
    val format = MediaFormat.createAudioFormat("audio/mp4a-latm", 44_100, 1).apply {
        setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC)
        setInteger(MediaFormat.KEY_BIT_RATE, 64_000)
        setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, 4096)
    }
    val encoder = MediaCodec.createEncoderByType("audio/mp4a-latm")
    val muxer = MediaMuxer(file.path, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
    var muxing = false
    try {
        encoder.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
        encoder.start()
        var frame = 0
        val frames = durationMs * 44_100 / 1000
        var inputDone = false
        var outputDone = false
        var track = -1
        val info = MediaCodec.BufferInfo()
        var attempts = 0
        while (!outputDone && attempts++ < 2000) {
            if (!inputDone) {
                val index = encoder.dequeueInputBuffer(10_000)
                if (index >= 0) {
                    val input = encoder.getInputBuffer(index)!!.order(ByteOrder.nativeOrder())
                    input.clear()
                    val count = minOf(1024, frames - frame)
                    for (i in 0 until count) input.putShort((sin((frame + i) * 2 * Math.PI * 440 / 44_100) * 3000).toInt().toShort())
                    encoder.queueInputBuffer(index, 0, count * 2, frame * 1_000_000L / 44_100,
                        if (count == 0) MediaCodec.BUFFER_FLAG_END_OF_STREAM else 0)
                    frame += count
                    inputDone = count == 0
                }
            }
            when (val index = encoder.dequeueOutputBuffer(info, 10_000)) {
                MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> { track = muxer.addTrack(encoder.outputFormat); muxer.start(); muxing = true }
                else -> if (index >= 0) {
                    if (info.size > 0 && info.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG == 0) {
                        check(muxing)
                        val output = encoder.getOutputBuffer(index)!!
                        output.position(info.offset); output.limit(info.offset + info.size)
                        muxer.writeSampleData(track, output, info)
                    }
                    outputDone = info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0
                    encoder.releaseOutputBuffer(index, false)
                }
            }
        }
        check(outputDone) { "AAC encoder did not finish" }
    } finally {
        runCatching { encoder.stop() }; encoder.release()
        if (muxing) muxer.stop()
        muxer.release()
    }
}

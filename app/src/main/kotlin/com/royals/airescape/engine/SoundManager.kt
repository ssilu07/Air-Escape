package com.royals.airescape.engine

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.math.PI
import kotlin.math.sin

/**
 * Programmatic retro sound effects using [AudioTrack].
 * All tone generation runs on a background thread so the game loop is never blocked.
 */
object SoundManager {

    private const val SAMPLE_RATE = 22050
    private var soundEnabled = true
    /** When false, gameplay sound effects are suppressed (game over, ads, etc.). */
    @Volatile var gameActive = false
    private var executor: ExecutorService? = null

    private fun ensureExecutor(): ExecutorService {
        if (executor == null || executor!!.isShutdown) {
            executor = Executors.newSingleThreadExecutor { r ->
                Thread(r, "SoundManager").apply { isDaemon = true }
            }
        }
        return executor!!
    }

    fun setSoundEnabled(enabled: Boolean) {
        soundEnabled = enabled
    }

    fun isSoundEnabled(): Boolean = soundEnabled

    // ---- Cached sound buffers (generated once, reused every play) ----

    private val soundCache = HashMap<String, ShortArray>(8)

    private fun cachedBuffer(key: String, generator: () -> ShortArray): ShortArray {
        return soundCache.getOrPut(key) { generator() }
    }

    // ---- Public sound methods ----

    fun playExplosion() = playAsync {
        playBuffer(cachedBuffer("explosion") {
            generateBuffer(300, 220f, 55f, Waveform.SQUARE, 0.7f, true)
        })
    }

    fun playStarCollect() = playAsync {
        playBuffer(cachedBuffer("star") {
            generateBuffer(120, 880f, 1760f, Waveform.SINE, 0.5f, false)
        })
    }

    fun playPowerUp() = playAsync {
        playBuffer(cachedBuffer("powerup") {
            val buf1 = generateBuffer(80, 660f, 660f, Waveform.SINE, 0.5f, false)
            val buf2 = generateBuffer(140, 990f, 1320f, Waveform.SINE, 0.5f, true)
            val combined = ShortArray(buf1.size + buf2.size)
            buf1.copyInto(combined, 0)
            buf2.copyInto(combined, buf1.size)
            combined
        })
    }

    fun playShieldHit() = playAsync {
        playBuffer(cachedBuffer("shield") {
            generateBuffer(180, 1200f, 600f, Waveform.SINE, 0.6f, true)
        })
    }

    fun playGameOver() = playAsync {
        playBuffer(cachedBuffer("gameover") {
            val buf1 = generateBuffer(200, 440f, 440f, Waveform.SQUARE, 0.5f, false)
            val buf2 = generateBuffer(200, 350f, 350f, Waveform.SQUARE, 0.5f, false)
            val buf3 = generateBuffer(400, 260f, 130f, Waveform.SQUARE, 0.5f, true)
            val combined = ShortArray(buf1.size + buf2.size + buf3.size)
            buf1.copyInto(combined, 0)
            buf2.copyInto(combined, buf1.size)
            buf3.copyInto(combined, buf1.size + buf2.size)
            combined
        })
    }

    /** Very short, quiet "pew" for player bullet fire. */
    fun playBulletFire() = playAsync {
        playBuffer(cachedBuffer("bullet") {
            generateBuffer(50, 1400f, 800f, Waveform.SINE, 0.15f, true)
        })
    }

    fun playMissileCollide() = playAsync {
        playBuffer(cachedBuffer("collide") {
            generateBuffer(200, 300f, 100f, Waveform.SQUARE, 0.5f, true)
        })
    }

    fun playBackgroundHum() = playAsync {
        playBuffer(cachedBuffer("hum") {
            generateBuffer(500, 60f, 60f, Waveform.SINE, 0.08f, false)
        })
    }

    // ---- Helicopter continuous loop (AudioTrack streaming, truly gapless) ----

    @Volatile private var helicopterPcm: ShortArray? = null
    @Volatile private var helicopterTrack: AudioTrack? = null
    @Volatile private var helicopterLooping = false
    private var helicopterThread: Thread? = null
    private var helicopterSampleRate = 44100

    /** Decode a raw MP3 resource into a PCM ShortArray. Call once. */
    fun decodeHelicopterSound(context: Context, rawResId: Int) {
        if (helicopterPcm != null) return
        try {
            val afd = context.resources.openRawResourceFd(rawResId)
            val extractor = MediaExtractor()
            extractor.setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
            afd.close()

            val format = extractor.getTrackFormat(0)
            helicopterSampleRate = format.getInteger(MediaFormat.KEY_SAMPLE_RATE)
            val channelCount = format.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
            val mime = format.getString(MediaFormat.KEY_MIME) ?: return

            extractor.selectTrack(0)
            val codec = MediaCodec.createDecoderByType(mime)
            codec.configure(format, null, null, 0)
            codec.start()

            val pcmBytes = mutableListOf<Byte>()
            val info = MediaCodec.BufferInfo()
            var inputDone = false

            while (true) {
                // Feed input
                if (!inputDone) {
                    val inIdx = codec.dequeueInputBuffer(10_000)
                    if (inIdx >= 0) {
                        val inBuf = codec.getInputBuffer(inIdx)!!
                        val read = extractor.readSampleData(inBuf, 0)
                        if (read < 0) {
                            codec.queueInputBuffer(inIdx, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                            inputDone = true
                        } else {
                            codec.queueInputBuffer(inIdx, 0, read, extractor.sampleTime, 0)
                            extractor.advance()
                        }
                    }
                }
                // Drain output
                val outIdx = codec.dequeueOutputBuffer(info, 10_000)
                if (outIdx >= 0) {
                    val outBuf = codec.getOutputBuffer(outIdx)!!
                    val chunk = ByteArray(info.size)
                    outBuf.get(chunk)
                    pcmBytes.addAll(chunk.toList())
                    codec.releaseOutputBuffer(outIdx, false)
                    if (info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) break
                } else if (outIdx == MediaCodec.INFO_TRY_AGAIN_LATER && inputDone) {
                    break
                }
            }

            codec.stop()
            codec.release()
            extractor.release()

            // Convert bytes to ShortArray (little-endian PCM 16-bit)
            val byteArr = pcmBytes.toByteArray()
            val shortBuf = ByteBuffer.wrap(byteArr).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer()
            val shorts = ShortArray(shortBuf.remaining())
            shortBuf.get(shorts)

            // If stereo, mix down to mono
            if (channelCount == 2) {
                val mono = ShortArray(shorts.size / 2)
                for (i in mono.indices) {
                    mono[i] = ((shorts[i * 2].toInt() + shorts[i * 2 + 1].toInt()) / 2).toShort()
                }
                helicopterPcm = mono
            } else {
                helicopterPcm = shorts
            }
        } catch (_: Exception) { }
    }

    /** Start looping the helicopter sound (gapless). */
    fun startHelicopterLoop(volume: Float = 0.25f) {
        if (!soundEnabled) return
        val pcm = helicopterPcm ?: return
        if (helicopterLooping) return
        helicopterLooping = true

        // Apply volume to a copy of the buffer
        val scaled = ShortArray(pcm.size) { i ->
            (pcm[i] * volume).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }

        val minBuf = AudioTrack.getMinBufferSize(
            helicopterSampleRate,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )
        val track = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_GAME)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setSampleRate(helicopterSampleRate)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .build()
            )
            .setBufferSizeInBytes(maxOf(minBuf, scaled.size * 2))
            .setTransferMode(AudioTrack.MODE_STREAM)
            .build()

        helicopterTrack = track
        track.play()

        helicopterThread = Thread({
            try {
                while (helicopterLooping) {
                    val t = helicopterTrack ?: break
                    val written = t.write(scaled, 0, scaled.size)
                    if (written < 0) break
                }
            } catch (_: Exception) { }
        }, "HelicopterLoop").apply { isDaemon = true; start() }
    }

    /** Stop the helicopter loop. */
    fun stopHelicopterLoop() {
        helicopterLooping = false
        val thread = helicopterThread
        helicopterThread = null
        try {
            // Wait for the streaming thread to exit before touching the track
            thread?.join(1000)
        } catch (_: Exception) { }
        // Now that the thread is done, safely stop and release
        val track = helicopterTrack
        helicopterTrack = null
        try {
            track?.stop()
            track?.release()
        } catch (_: Exception) { }
    }

    fun release() {
        stopHelicopterLoop()
        executor?.shutdownNow()
        executor = null
        soundCache.clear()
    }

    // ---- Internal helpers ----

    private enum class Waveform { SINE, SQUARE }

    private fun playAsync(block: () -> Unit) {
        if (!soundEnabled || !gameActive) return
        try {
            ensureExecutor().submit {
                try {
                    if (gameActive) block()
                } catch (_: Exception) {
                    // Swallow audio errors silently
                }
            }
        } catch (_: Exception) {
            // Executor may be shut down
        }
    }

    private fun generateBuffer(
        durationMs: Int,
        startFreq: Float,
        endFreq: Float,
        waveform: Waveform,
        volume: Float,
        fadeOut: Boolean
    ): ShortArray {
        val numSamples = (SAMPLE_RATE * durationMs / 1000f).toInt()
        val buffer = ShortArray(numSamples)
        var phase = 0.0

        for (i in 0 until numSamples) {
            val t = i.toFloat() / numSamples
            val freq = startFreq + (endFreq - startFreq) * t
            val increment = 2.0 * PI * freq / SAMPLE_RATE

            val sample = when (waveform) {
                Waveform.SINE -> sin(phase)
                Waveform.SQUARE -> if (sin(phase) >= 0.0) 1.0 else -1.0
            }

            val envelope = if (fadeOut) (1.0f - t) else 1.0f
            val value = (sample * volume * envelope * Short.MAX_VALUE).toInt()
                .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt())
            buffer[i] = value.toShort()

            phase += increment
            if (phase > 2.0 * PI) phase -= 2.0 * PI
        }
        return buffer
    }

    private fun playBuffer(buffer: ShortArray) {
        val bufferSize = buffer.size * 2 // 2 bytes per short
        val minBuf = AudioTrack.getMinBufferSize(
            SAMPLE_RATE,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )
        val trackBufSize = maxOf(bufferSize, minBuf)

        val track = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_GAME)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setSampleRate(SAMPLE_RATE)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .build()
            )
            .setBufferSizeInBytes(trackBufSize)
            .setTransferMode(AudioTrack.MODE_STATIC)
            .build()

        track.write(buffer, 0, buffer.size)
        track.play()

        // Block until playback finishes, then release
        val durationMs = (buffer.size * 1000L) / SAMPLE_RATE + 50
        Thread.sleep(durationMs)
        track.stop()
        track.release()
    }
}

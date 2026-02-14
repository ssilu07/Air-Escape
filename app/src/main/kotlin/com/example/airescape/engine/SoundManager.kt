package com.example.airescape.engine

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
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

    // ---- Public sound methods ----

    fun playExplosion() = playAsync {
        // Low rumble noise burst - descending square wave
        generateTone(
            durationMs = 300,
            startFreq = 220f,
            endFreq = 55f,
            waveform = Waveform.SQUARE,
            volume = 0.7f,
            fadeOut = true
        )
    }

    fun playStarCollect() = playAsync {
        // Quick ascending ding
        generateTone(
            durationMs = 120,
            startFreq = 880f,
            endFreq = 1760f,
            waveform = Waveform.SINE,
            volume = 0.5f,
            fadeOut = false
        )
    }

    fun playPowerUp() = playAsync {
        // Rising sparkle - two quick notes
        val buf1 = generateBuffer(80, 660f, 660f, Waveform.SINE, 0.5f, false)
        val buf2 = generateBuffer(140, 990f, 1320f, Waveform.SINE, 0.5f, true)
        val combined = ShortArray(buf1.size + buf2.size)
        buf1.copyInto(combined, 0)
        buf2.copyInto(combined, buf1.size)
        playBuffer(combined)
    }

    fun playShieldHit() = playAsync {
        // Metallic ping
        generateTone(
            durationMs = 180,
            startFreq = 1200f,
            endFreq = 600f,
            waveform = Waveform.SINE,
            volume = 0.6f,
            fadeOut = true
        )
    }

    fun playGameOver() = playAsync {
        // Sad descending tones
        val buf1 = generateBuffer(200, 440f, 440f, Waveform.SQUARE, 0.5f, false)
        val buf2 = generateBuffer(200, 350f, 350f, Waveform.SQUARE, 0.5f, false)
        val buf3 = generateBuffer(400, 260f, 130f, Waveform.SQUARE, 0.5f, true)
        val combined = ShortArray(buf1.size + buf2.size + buf3.size)
        buf1.copyInto(combined, 0)
        buf2.copyInto(combined, buf1.size)
        buf3.copyInto(combined, buf1.size + buf2.size)
        playBuffer(combined)
    }

    fun playMissileCollide() = playAsync {
        // Short mid-frequency burst for missile-missile collision
        generateTone(
            durationMs = 200,
            startFreq = 300f,
            endFreq = 100f,
            waveform = Waveform.SQUARE,
            volume = 0.5f,
            fadeOut = true
        )
    }

    fun playBackgroundHum() = playAsync {
        // Very low, quiet sine hum (short burst; caller can loop)
        generateTone(
            durationMs = 500,
            startFreq = 60f,
            endFreq = 60f,
            waveform = Waveform.SINE,
            volume = 0.08f,
            fadeOut = false
        )
    }

    fun release() {
        executor?.shutdownNow()
        executor = null
    }

    // ---- Internal helpers ----

    private enum class Waveform { SINE, SQUARE }

    private fun playAsync(block: () -> Unit) {
        if (!soundEnabled) return
        try {
            ensureExecutor().submit {
                try {
                    block()
                } catch (_: Exception) {
                    // Swallow audio errors silently
                }
            }
        } catch (_: Exception) {
            // Executor may be shut down
        }
    }

    private fun generateTone(
        durationMs: Int,
        startFreq: Float,
        endFreq: Float,
        waveform: Waveform,
        volume: Float,
        fadeOut: Boolean
    ) {
        val buffer = generateBuffer(durationMs, startFreq, endFreq, waveform, volume, fadeOut)
        playBuffer(buffer)
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

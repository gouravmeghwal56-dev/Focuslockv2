package com.focuslock.app

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack

/** Ambient sounds made live on the phone (no audio files): rain, waves, city, birds. */
object Ambient {
    const val OFF = 0
    const val RAIN = 1
    const val WAVES = 2
    const val CITY = 3
    const val BIRDS = 4

    @Volatile
    var mode = OFF
        private set
    @Volatile
    private var running = false
    private var gen = 0
    private const val SR = 22050

    fun set(m: Int) {
        if (m == OFF) {
            stop()
            return
        }
        mode = m
        if (!running) start()
    }

    fun stop() {
        mode = OFF
        running = false
    }

    private fun start() {
        running = true
        val g = ++gen
        val t = Thread { loop(g) }
        t.isDaemon = true
        t.start()
    }

    private fun loop(g: Int) {
        var track: AudioTrack? = null
        try {
            val minBuf = AudioTrack.getMinBufferSize(SR, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT)
            track = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(SR)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setBufferSizeInBytes(maxOf(minBuf, SR))
                .setTransferMode(AudioTrack.MODE_STREAM)
                .build()
            track.play()
            val s = Synth()
            val chunk = ShortArray(1024)
            var last = mode
            var fade = 1f
            while (running && gen == g) {
                val m = mode
                if (m != last) {
                    last = m
                    fade = 0f
                }
                for (i in chunk.indices) {
                    if (fade < 1f) fade = minOf(1f, fade + 1f / (SR * 0.4f))
                    var v = s.next(m) * fade
                    if (v > 1f) v = 1f
                    if (v < -1f) v = -1f
                    chunk[i] = (v * 30000f).toInt().toShort()
                }
                track.write(chunk, 0, chunk.size)
            }
        } catch (e: Exception) {
        } finally {
            try {
                track?.stop()
            } catch (e: Exception) {
            }
            track?.release()
            if (gen == g) running = false
        }
    }

    private class Synth {
        private val r = java.util.Random()
        private val twoPi = (2.0 * Math.PI).toFloat()
        private var t = 0L
        private var lpA = 0f
        private var lpB = 0f
        private var drop = 0f
        // city
        private var swoosh = 0
        private var swooshLen = 1
        private var nextSwoosh = SR * 2
        // birds
        private var chirpLeft = 0
        private var chirpTotal = 1
        private var chirpF = 3000f
        private var chirpSlope = 0f
        private var phase = 0f
        private var nextChirp = SR
        private var trill = 0
        private var gap = 0

        private fun startChirp() {
            chirpTotal = SR * (60 + r.nextInt(100)) / 1000
            chirpLeft = chirpTotal
            chirpF = 2200f + r.nextInt(2200)
            chirpSlope = (r.nextInt(2400) - 800).toFloat()
        }

        fun next(mode: Int): Float {
            t++
            val w = r.nextFloat() * 2f - 1f
            var out = 0f
            when (mode) {
                RAIN -> {
                    lpA += 0.5f * (w - lpA)
                    lpB += 0.04f * (w - lpB)
                    if (r.nextInt(900) == 0) drop = 0.5f + r.nextFloat() * 0.4f
                    out = (lpA - lpB) * 0.7f + lpB * 0.8f + drop * w * 0.4f
                    drop *= 0.985f
                }
                WAVES -> {
                    lpB += 0.01f * (w - lpB)
                    lpA += 0.25f * (w - lpA)
                    val ph = (t.toFloat() / SR) * (twoPi / 9f)
                    val s = 0.5f + 0.5f * Math.sin(ph.toDouble()).toFloat()
                    val env = s * s
                    out = (lpB * 5f * (0.3f + 0.7f * env) + lpA * 0.35f * env) * 1.8f
                }
                CITY -> {
                    lpB += 0.006f * (w - lpB)
                    lpA += 0.08f * (w - lpA)
                    if (swoosh > 0) {
                        swoosh--
                    } else if (--nextSwoosh <= 0) {
                        swooshLen = SR * (2 + r.nextInt(3))
                        swoosh = swooshLen
                        nextSwoosh = SR * (3 + r.nextInt(6))
                    }
                    val e = if (swoosh > 0) Math.sin(Math.PI * (1.0 - swoosh.toDouble() / swooshLen)).toFloat() else 0f
                    out = (lpB * 7f + (lpA - lpB) * 0.3f * e + lpA * 0.05f) * 1.8f
                }
                BIRDS -> {
                    lpB += 0.02f * (w - lpB)
                    out = lpB * 1.2f
                    if (chirpLeft > 0) {
                        val prog = 1f - chirpLeft.toFloat() / chirpTotal
                        val f = chirpF + chirpSlope * prog
                        phase += twoPi * f / SR
                        if (phase > twoPi) phase -= twoPi
                        val env = Math.sin(Math.PI * prog).toFloat()
                        out += (Math.sin(phase.toDouble()).toFloat() * 0.26f + Math.sin(2.0 * phase).toFloat() * 0.07f) * env
                        chirpLeft--
                        if (chirpLeft == 0 && trill > 0) gap = SR * 30 / 1000
                    } else if (gap > 0) {
                        gap--
                        if (gap == 0 && trill > 0) {
                            trill--
                            startChirp()
                        }
                    } else if (--nextChirp <= 0) {
                        trill = r.nextInt(4)
                        startChirp()
                        nextChirp = SR / 2 + r.nextInt(SR * 2)
                    }
                }
            }
            return out
        }
    }
}

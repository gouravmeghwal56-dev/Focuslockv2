package com.focuslock.app

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack

/**
 * Calm ambient sounds made live on the phone (no audio files).
 * Har sound ka gain alag se tune kiya hai (A-weighted loudness barabar), taaki rain sabse upar na ho.
 */
object Ambient {
    const val OFF = 0
    const val RAIN = 1
    const val WAVES = 2
    const val WIND = 3
    const val BIRDS = 4
    const val FIRE = 5
    const val NIGHT = 6
    const val COUNT = 6

    val NAMES = arrayOf("", "RAIN", "WAVES", "WIND", "BIRDS", "FIRE", "NIGHT")

    // per-sound loudness balance
    private val GAIN = floatArrayOf(0f, 0.33f, 0.41f, 0.76f, 1.75f, 1.54f, 2.3f)

    @Volatile
    var mode = OFF
        private set
    @Volatile
    private var running = false
    private var gen = 0
    private const val SR = 22050

    fun set(m: Int) {
        if (m == OFF) {
            fadeOut()
            return
        }
        mode = m
        if (!running) start()
    }

    /** Dheere dheere band (tap se close). */
    fun fadeOut() {
        mode = OFF
    }

    /** Turant band (screen band / exit). */
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
            var last = if (mode == OFF) RAIN else mode
            var fade = 1f
            var master = 0f
            while (running && gen == g) {
                val m = mode
                if (m != OFF && m != last) {
                    last = m
                    fade = 0f
                }
                val target = if (m == OFF) 0f else 1f
                if (m == OFF && master <= 0f) break
                val gain = GAIN[last]
                for (i in chunk.indices) {
                    if (master < target) master = minOf(target, master + 1f / (SR * 0.9f))
                    else if (master > target) master = maxOf(target, master - 1f / (SR * 0.7f))
                    if (fade < 1f) fade = minOf(1f, fade + 1f / (SR * 0.8f))
                    val v = Math.tanh((s.next(last) * gain).toDouble()).toFloat() * fade * master
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
            if (gen == g) {
                running = false
                // fade-out ke beech user ne dobara tap kiya ho to phir se chalu karo
                if (mode != OFF) start()
            }
        }
    }

    private class Synth {
        private val r = java.util.Random()
        private val twoPi = (2.0 * Math.PI).toFloat()
        private var t = 0L

        // rain
        private var rA = 0f
        private var rB = 0f
        private var rC = 0f
        private val dPh = FloatArray(4)
        private val dInc = FloatArray(4)
        private val dAmp = FloatArray(4)
        private val dEnv = FloatArray(4)

        // waves
        private var wPh = 0f
        private var wP = 9f
        private var wA = 0f
        private var wB = 0f
        private var wC = 0f

        // wind
        private var nA = 0f
        private var nB = 0f
        private var g = 0.5f
        private var gT = 0.5f
        private var gLeft = 0

        // birds
        private var bA = 0f
        private var chLeft = 0
        private var chTotal = 1
        private var chF = 3000f
        private var chSlope = 0f
        private var phase = 0f
        private var nextChirp = SR
        private var trill = 0
        private var gap = 0

        // fire
        private var fA = 0f
        private var fB = 0f
        private var fC = 0f
        private var cr = 0f

        // night (crickets)
        private var kA = 0f
        private val cP = intArrayOf((SR * 0.62).toInt(), (SR * 0.81).toInt(), (SR * 0.97).toInt())
        private val cF = doubleArrayOf(3900.0, 4350.0, 4700.0)
        private val cO = intArrayOf(0, (SR * 0.3).toInt(), (SR * 0.55).toInt())
        private val cL = (SR * 0.034).toInt()

        private fun startChirp() {
            chTotal = SR * (70 + r.nextInt(110)) / 1000
            chLeft = chTotal
            chF = 2300f + r.nextInt(2000)
            chSlope = (r.nextInt(2200) - 700).toFloat()
        }

        fun next(mode: Int): Float {
            t++
            val w = r.nextFloat() * 2f - 1f
            var out = 0f
            when (mode) {
                RAIN -> {
                    rA += 0.30f * (w - rA)
                    rB += 0.02f * (w - rB)
                    rC += 0.65f * (w - rC)
                    val tt = (t % (SR * 1000L)).toDouble() / SR
                    val mod = 1f + 0.12f * Math.sin(2.0 * Math.PI * 0.13 * tt).toFloat() +
                        0.08f * Math.sin(2.0 * Math.PI * 0.31 * tt).toFloat()
                    out = ((rA - rB) + (rC - rA) * 0.25f) * mod
                    if (r.nextInt(650) == 0) {
                        for (i in 0 until 4) {
                            if (dEnv[i] < 0.02f) {
                                dPh[i] = 0f
                                dInc[i] = twoPi * (1100f + r.nextFloat() * 2300f) / SR
                                dAmp[i] = 0.10f + r.nextFloat() * 0.18f
                                dEnv[i] = 1f
                                break
                            }
                        }
                    }
                    for (i in 0 until 4) {
                        if (dEnv[i] > 0.001f) {
                            out += Math.sin(dPh[i].toDouble()).toFloat() * dAmp[i] * dEnv[i]
                            dPh[i] += dInc[i]
                            dEnv[i] *= 0.9965f
                        }
                    }
                }
                WAVES -> {
                    wPh += 1f / (SR * wP)
                    if (wPh >= 1f) {
                        wPh -= 1f
                        wP = 7f + r.nextFloat() * 4f
                    }
                    val c = 0.5f - 0.5f * Math.cos((twoPi * wPh).toDouble()).toFloat()
                    val e = Math.pow(c.toDouble(), 1.6).toFloat()
                    wA += (0.10f + 0.35f * e) * (w - wA)
                    wB += 0.012f * (w - wB)
                    wC += 0.55f * (w - wC)
                    out = (wA - wB) * (0.25f + 1.0f * e) + (wC - wA) * 0.25f * e * e
                }
                WIND -> {
                    if (gLeft <= 0) {
                        gT = r.nextFloat()
                        gLeft = (SR * (2f + r.nextFloat() * 3f)).toInt()
                    }
                    gLeft--
                    g += 0.00006f * (gT - g)
                    nA += (0.03f + 0.13f * g) * (w - nA)
                    nB += 0.006f * (w - nB)
                    out = (nA - nB) * (0.5f + 1.1f * g)
                }
                BIRDS -> {
                    bA += 0.05f * (w - bA)
                    out = bA * 0.55f
                    if (chLeft > 0) {
                        val prog = 1f - chLeft.toFloat() / chTotal
                        val f = chF + chSlope * prog
                        phase += twoPi * f / SR
                        if (phase > twoPi) phase -= twoPi
                        val env = Math.pow(Math.sin(Math.PI * prog), 1.5).toFloat()
                        out += (Math.sin(phase.toDouble()).toFloat() * 0.30f +
                            Math.sin(2.0 * phase).toFloat() * 0.06f) * env
                        chLeft--
                        if (chLeft == 0 && trill > 0) gap = SR * 30 / 1000
                    } else if (gap > 0) {
                        gap--
                        if (gap == 0 && trill > 0) {
                            trill--
                            startChirp()
                        }
                    } else if (--nextChirp <= 0) {
                        trill = r.nextInt(4)
                        startChirp()
                        nextChirp = SR * 2 + r.nextInt(SR * 4)
                    }
                }
                FIRE -> {
                    fA += 0.06f * (w - fA)
                    fB += 0.004f * (w - fB)
                    fC += 0.5f * (w - fC)
                    out = (fA - fB) * 0.9f
                    if (r.nextInt(2200) == 0) {
                        val u = r.nextFloat()
                        cr = 0.25f + u * u * u * 0.9f
                    }
                    if (r.nextInt(26000) == 0) cr = 1f
                    out += cr * (w - fC) * 0.7f
                    cr *= 0.994f
                }
                NIGHT -> {
                    kA += 0.02f * (w - kA)
                    out = kA * 0.35f
                    val ts = (t % SR).toInt()
                    for (i in 0 until 3) {
                        val tt = ((t + cO[i]) % cP[i]).toInt()
                        if (tt < 3 * cL) {
                            val ph = (tt % cL).toFloat() / cL
                            val s = Math.sin(Math.PI * ph).toFloat()
                            out += Math.sin(2.0 * Math.PI * cF[i] * ts / SR).toFloat() * s * s * 0.12f
                        }
                    }
                }
            }
            return out
        }
    }
}

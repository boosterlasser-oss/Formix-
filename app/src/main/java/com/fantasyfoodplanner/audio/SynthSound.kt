package com.fantasyfoodplanner.audio

/**
 * SynthSound — Pooled AudioTrack Synthesis System
 *
 * Wiederverwendbares Audio-System für Android-Apps.
 * Generiert Sounds rein per Code (keine Asset-Dateien nötig).
 *
 * Quelle: NeonKidsGames (com.stefan.neonkidsgames)
 * Lizenz: Eigener Code — frei für alle eigenen Apps verwendbar
 *
 * VERWENDUNG:
 *   SynthSound.play(SynthSound.pop)      // Kurzer Pop-Sound
 *   SynthSound.play(SynthSound.bigWin)   // Gewinn-Fanfare
 *   SynthSound.play(SynthSound.bounce)   // Hüpf-Sound
 *   SynthSound.play(SynthSound.drumKick) // Kick-Drum
 *   SynthSound.play(SynthSound.xylophoneNotes[3]) // Xylophon Note F4
 *
 * FEATURES:
 *   - Keine Sound-Dateien benötigt — alles wird per Synthese generiert
 *   - AudioTrack-Pool (max 8) für Null-Allocation Wiedergabe
 *   - Lazy-initialisierte PCM-Buffer (werden erst bei erster Nutzung berechnet)
 *   - 22050Hz Mono 16-Bit — minimaler Speicherverbrauch
 *   - Unterstützt gleichzeitige Wiedergabe mehrerer Sounds
 *
 * KEINE Permissions benötigt. Funktioniert offline.
 */

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import kotlin.math.*

object SynthSound {
    private const val SAMPLE_RATE = 22050
    private val audioAttrs = AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_GAME)
        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
        .build()
    private val audioFormat = AudioFormat.Builder()
        .setSampleRate(SAMPLE_RATE)
        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
        .build()

    // ── Pre-computed PCM buffers ───────────────────────────────

    /** Short pop/tap sound – noise burst with exponential decay */
    val pop: ShortArray by lazy { genPop() }

    /** Bounce – short sine burst at ~300Hz */
    val bounce: ShortArray by lazy { genSine(300f, 0.04f, decay = 12f) }

    /** Wall bounce – very short click at ~500Hz */
    val wallBounce: ShortArray by lazy { genSine(500f, 0.025f, decay = 20f) }

    /** Correct / success – ascending 3-note arpeggio C5-E5-G5 */
    val correct: ShortArray by lazy { genArpeggio(floatArrayOf(523f, 659f, 784f), 0.08f) }

    /** Big win – major chord A4+C#5+E5 held for 0.3s */
    val bigWin: ShortArray by lazy { genChord(floatArrayOf(440f, 554f, 659f), 0.35f) }

    /** Error / wrong – dissonant dual-tone 200Hz+233Hz sawtooth */
    val error: ShortArray by lazy { genError() }

    /** Goal scored – rising sweep from 400Hz to 800Hz */
    val goal: ShortArray by lazy { genSweep(400f, 800f, 0.25f) }

    /** Flap / light tap – very short sine blip */
    val flap: ShortArray by lazy { genSine(440f, 0.015f, decay = 30f) }

    /** Spin whoosh – filtered noise sweep */
    val spin: ShortArray by lazy { genNoiseSweep(0.04f) }

    /** Cinematic whoosh – longer sweep for fly-in effects */
    val cinematicWhoosh: ShortArray by lazy { genCinematicWhoosh() }

    /** Power-up rising tone */
    val powerUp: ShortArray by lazy { genSweep(200f, 1200f, 0.3f) }

    /** Musical notes for xylophone – C4 to C5 (8 keys) */
    val xylophoneNotes: Array<ShortArray> by lazy {
        val freqs = floatArrayOf(262f, 294f, 330f, 349f, 392f, 440f, 494f, 523f)
        Array(8) { genBellTone(freqs[it], 0.5f) }
    }

    /** Drum sounds */
    val drumKick: ShortArray by lazy { genKick() }
    val drumSnare: ShortArray by lazy { genSnare() }
    val drumHihat: ShortArray by lazy { genHihat() }
    val drumTomLow: ShortArray by lazy { genSine(100f, 0.15f, decay = 8f) }
    val drumTomMid: ShortArray by lazy { genSine(150f, 0.12f, decay = 10f) }
    val drumTomHigh: ShortArray by lazy { genSine(200f, 0.10f, decay = 12f) }
    val drumClap: ShortArray by lazy { genClap() }
    val drumRim: ShortArray by lazy { genSine(800f, 0.03f, decay = 25f) }
    val drumShaker: ShortArray by lazy { genShaker() }

    /** Color pad sounds – 4 distinct pitched tones */
    val padTones: Array<ShortArray> by lazy {
        val freqs = floatArrayOf(262f, 330f, 392f, 523f)
        Array(4) { genBellTone(freqs[it], 0.3f) }
    }

    /** Pattern memory tones – 9 distinct pitched tones */
    val patternTones: Array<ShortArray> by lazy {
        val freqs = floatArrayOf(262f, 294f, 330f, 349f, 392f, 440f, 494f, 523f, 587f)
        Array(9) { genBellTone(freqs[it], 0.25f) }
    }

    // ── Playback (pooled AudioTracks) ──────────────────────────

    private const val POOL_SIZE = 8
    private val trackPool = ArrayDeque<AudioTrack>(POOL_SIZE)
    private val poolLock = Any()

    private fun obtainTrack(bufSize: Int): AudioTrack? {
        synchronized(poolLock) {
            while (trackPool.isNotEmpty()) {
                val track = trackPool.removeFirst()
                try {
                    if (track.state == AudioTrack.STATE_INITIALIZED) {
                        if (track.bufferSizeInFrames >= bufSize / 2) {
                            track.stop()
                            track.reloadStaticData()
                            return track
                        }
                    }
                    track.release()
                } catch (_: Throwable) {
                    try { track.release() } catch (_: Throwable) {}
                }
            }
        }
        return try {
            val minBuf = AudioTrack.getMinBufferSize(
                SAMPLE_RATE, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT
            )
            AudioTrack(
                audioAttrs, audioFormat, maxOf(bufSize, minBuf),
                AudioTrack.MODE_STATIC, AudioManager.AUDIO_SESSION_ID_GENERATE
            )
        } catch (_: Throwable) { null }
    }

    private fun returnTrack(track: AudioTrack) {
        synchronized(poolLock) {
            if (trackPool.size < POOL_SIZE) {
                trackPool.addLast(track)
            } else {
                track.release()
            }
        }
    }

    fun play(buf: ShortArray) {
        try {
            val bufSize = buf.size * 2
            val track = obtainTrack(bufSize) ?: return
            track.write(buf, 0, buf.size)
            track.setNotificationMarkerPosition(buf.size)
            track.setPlaybackPositionUpdateListener(object : AudioTrack.OnPlaybackPositionUpdateListener {
                override fun onMarkerReached(t: AudioTrack?) { if (t != null) returnTrack(t) }
                override fun onPeriodicNotification(t: AudioTrack?) {}
            })
            track.play()
        } catch (_: Throwable) { }
    }

    // ── Sound generation helpers ──────────────────────────────

    private fun genSine(freq: Float, durSec: Float, decay: Float = 5f, volume: Float = 0.7f): ShortArray {
        val n = (SAMPLE_RATE * durSec).toInt()
        val buf = ShortArray(n)
        for (i in 0 until n) {
            val t = i.toFloat() / SAMPLE_RATE
            val env = exp(-decay * t)
            val sample = sin(2.0 * PI * freq * t).toFloat() * env * volume
            buf[i] = (sample * Short.MAX_VALUE).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }
        return buf
    }

    private fun genBellTone(freq: Float, durSec: Float): ShortArray {
        val n = (SAMPLE_RATE * durSec).toInt()
        val buf = ShortArray(n)
        for (i in 0 until n) {
            val t = i.toFloat() / SAMPLE_RATE
            val attack = minOf(t / 0.003f, 1f)
            val env = attack * exp(-4f * t)
            val s = (sin(2.0 * PI * freq * t).toFloat() * 0.6f +
                    sin(2.0 * PI * freq * 2.0 * t).toFloat() * 0.25f +
                    sin(2.0 * PI * freq * 3.71 * t).toFloat() * 0.15f) * env * 0.65f
            buf[i] = (s * Short.MAX_VALUE).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }
        return buf
    }

    private fun genPop(): ShortArray {
        val n = (SAMPLE_RATE * 0.05f).toInt()
        val buf = ShortArray(n)
        for (i in 0 until n) {
            val t = i.toFloat() / SAMPLE_RATE
            val env = exp(-30f * t)
            val noise = (Math.random().toFloat() * 2f - 1f)
            val sine = sin(2.0 * PI * 600.0 * t).toFloat()
            val sample = (noise * 0.5f + sine * 0.5f) * env * 0.6f
            buf[i] = (sample * Short.MAX_VALUE).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }
        return buf
    }

    private fun genError(): ShortArray {
        val n = (SAMPLE_RATE * 0.25f).toInt()
        val buf = ShortArray(n)
        for (i in 0 until n) {
            val t = i.toFloat() / SAMPLE_RATE
            val env = exp(-4f * t)
            val s1 = sin(2.0 * PI * 200.0 * t).toFloat()
            val s2 = sin(2.0 * PI * 233.0 * t).toFloat()
            val saw = ((200f * t) % 1f) * 2f - 1f
            val sample = (s1 * 0.35f + s2 * 0.35f + saw * 0.15f) * env * 0.5f
            buf[i] = (sample * Short.MAX_VALUE).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }
        return buf
    }

    private fun genArpeggio(freqs: FloatArray, noteDur: Float): ShortArray {
        val totalDur = noteDur * freqs.size + 0.1f
        val n = (SAMPLE_RATE * totalDur).toInt()
        val buf = ShortArray(n)
        for (i in 0 until n) {
            val t = i.toFloat() / SAMPLE_RATE
            var sample = 0f
            for ((noteIdx, freq) in freqs.withIndex()) {
                val noteStart = noteIdx * noteDur
                val noteT = t - noteStart
                if (noteT >= 0f) {
                    val env = exp(-5f * noteT)
                    sample += sin(2.0 * PI * freq * noteT).toFloat() * env * 0.5f
                }
            }
            buf[i] = (sample * Short.MAX_VALUE).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }
        return buf
    }

    private fun genChord(freqs: FloatArray, durSec: Float): ShortArray {
        val n = (SAMPLE_RATE * durSec).toInt()
        val buf = ShortArray(n)
        val vol = 0.35f / freqs.size
        for (i in 0 until n) {
            val t = i.toFloat() / SAMPLE_RATE
            val env = exp(-3f * t)
            var sample = 0f
            for (freq in freqs) {
                sample += sin(2.0 * PI * freq * t).toFloat() * vol
            }
            sample *= env
            buf[i] = (sample * Short.MAX_VALUE).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }
        return buf
    }

    private fun genSweep(startFreq: Float, endFreq: Float, durSec: Float): ShortArray {
        val n = (SAMPLE_RATE * durSec).toInt()
        val buf = ShortArray(n)
        var phase = 0.0
        for (i in 0 until n) {
            val t = i.toFloat() / SAMPLE_RATE
            val frac = t / durSec
            val freq = startFreq + (endFreq - startFreq) * frac
            val env = exp(-2f * t)
            phase += 2.0 * PI * freq / SAMPLE_RATE
            val sample = sin(phase).toFloat() * env * 0.6f
            buf[i] = (sample * Short.MAX_VALUE).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }
        return buf
    }

    private fun genNoiseSweep(durSec: Float): ShortArray {
        val n = (SAMPLE_RATE * durSec).toInt()
        val buf = ShortArray(n)
        var prev = 0f
        for (i in 0 until n) {
            val t = i.toFloat() / SAMPLE_RATE
            val env = exp(-15f * t)
            val noise = (Math.random().toFloat() * 2f - 1f)
            val alpha = 0.3f + 0.5f * (t / durSec)
            prev = prev * (1f - alpha) + noise * alpha
            val sample = prev * env * 0.5f
            buf[i] = (sample * Short.MAX_VALUE).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }
        return buf
    }

    /** Cinematic whoosh – longer filtered noise with pitch sweep for fly-in */
    private fun genCinematicWhoosh(): ShortArray {
        val durSec = 0.5f
        val n = (SAMPLE_RATE * durSec).toInt()
        val buf = ShortArray(n)
        var prev = 0f
        var phase = 0.0
        for (i in 0 until n) {
            val t = i.toFloat() / SAMPLE_RATE
            val frac = t / durSec
            // Envelope: schnell rein, langsam raus
            val env = if (frac < 0.15f) frac / 0.15f else exp(-3f * (frac - 0.15f))
            // Noise-Komponente (gefilterter Wind)
            val noise = (Math.random().toFloat() * 2f - 1f)
            val alpha = 0.2f + 0.6f * frac
            prev = prev * (1f - alpha) + noise * alpha
            // Tonale Komponente (aufsteigend 150→600 Hz)
            val freq = 150f + 450f * frac
            phase += 2.0 * PI * freq / SAMPLE_RATE
            val tonal = sin(phase).toFloat() * 0.15f
            val sample = (prev * 0.5f + tonal) * env * 0.7f
            buf[i] = (sample * Short.MAX_VALUE).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }
        return buf
    }

    private fun genKick(): ShortArray {
        val durSec = 0.2f
        val n = (SAMPLE_RATE * durSec).toInt()
        val buf = ShortArray(n)
        var phase = 0.0
        for (i in 0 until n) {
            val t = i.toFloat() / SAMPLE_RATE
            val freq = 150f * exp(-8f * t) + 50f
            val env = exp(-6f * t)
            phase += 2.0 * PI * freq / SAMPLE_RATE
            val sample = (sin(phase).toFloat() * 0.8f +
                    sin(phase * 0.5).toFloat() * 0.2f) * env * 0.8f
            buf[i] = (sample * Short.MAX_VALUE).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }
        return buf
    }

    private fun genSnare(): ShortArray {
        val durSec = 0.15f
        val n = (SAMPLE_RATE * durSec).toInt()
        val buf = ShortArray(n)
        for (i in 0 until n) {
            val t = i.toFloat() / SAMPLE_RATE
            val envBody = exp(-15f * t)
            val envNoise = exp(-10f * t)
            val body = sin(2.0 * PI * 180.0 * t).toFloat() * envBody * 0.4f
            val noise = (Math.random().toFloat() * 2f - 1f) * envNoise * 0.5f
            val sample = (body + noise) * 0.65f
            buf[i] = (sample * Short.MAX_VALUE).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }
        return buf
    }

    private fun genHihat(): ShortArray {
        val durSec = 0.06f
        val n = (SAMPLE_RATE * durSec).toInt()
        val buf = ShortArray(n)
        var prev = 0f
        for (i in 0 until n) {
            val t = i.toFloat() / SAMPLE_RATE
            val env = exp(-25f * t)
            val noise = (Math.random().toFloat() * 2f - 1f)
            val hp = noise - prev
            prev = noise
            val sample = hp * env * 0.5f
            buf[i] = (sample * Short.MAX_VALUE).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }
        return buf
    }

    private fun genClap(): ShortArray {
        val durSec = 0.12f
        val n = (SAMPLE_RATE * durSec).toInt()
        val buf = ShortArray(n)
        for (i in 0 until n) {
            val t = i.toFloat() / SAMPLE_RATE
            var env = 0f
            for (offset in listOf(0f, 0.01f, 0.02f)) {
                val bt = t - offset
                if (bt >= 0f) env += exp(-20f * bt) * 0.4f
            }
            val noise = (Math.random().toFloat() * 2f - 1f)
            val sample = noise * env * 0.5f
            buf[i] = (sample * Short.MAX_VALUE).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }
        return buf
    }

    private fun genShaker(): ShortArray {
        val durSec = 0.1f
        val n = (SAMPLE_RATE * durSec).toInt()
        val buf = ShortArray(n)
        var prev = 0f
        for (i in 0 until n) {
            val t = i.toFloat() / SAMPLE_RATE
            val env = if (t < 0.01f) t / 0.01f else exp(-12f * (t - 0.01f))
            val noise = (Math.random().toFloat() * 2f - 1f)
            val alpha = 0.6f
            prev = prev * (1f - alpha) + noise * alpha
            val sample = prev * env * 0.4f
            buf[i] = (sample * Short.MAX_VALUE).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }
        return buf
    }
}

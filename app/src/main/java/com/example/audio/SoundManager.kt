package com.example.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin

enum class SoundEffect {
    UI_CLICK,
    JUMP,
    LAND,
    SLIDE,
    COIN,
    POWER_UP,
    NEAR_MISS,
    COLLISION,
    GAME_OVER,
    ACHIEVEMENT,
    FOOTSTEP
}

/**
 * Original procedural synthesizer audio engine for RUNVANTA.
 * Generates zero-copyright retro-modern arcade sound effects and looping background music
 * cleanly in memory using Android AudioTrack with independent music/SFX volume controls.
 */
class SoundManager(private val context: Context) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val sampleRate = 22050

    @Volatile var musicVolume: Float = 0.65f
    @Volatile var sfxVolume: Float = 0.85f
    @Volatile var vibrationEnabled: Boolean = true
    @Volatile var isAppForeground: Boolean = true

    private var musicJob: Job? = null
    private var musicTrack: AudioTrack? = null
    @Volatile private var currentTempoBpm: Int = 124
    @Volatile private var inGameplayMusic: Boolean = false

    private val sfxBuffers: Map<SoundEffect, ShortArray> by lazy { buildSfxLibrary() }

    fun updateAudioConfig(musicVol: Float, sfxVol: Float, vibrate: Boolean) {
        val wasMusicOff = musicVolume <= 0.01f
        musicVolume = musicVol.coerceIn(0f, 1f)
        sfxVolume = sfxVol.coerceIn(0f, 1f)
        vibrationEnabled = vibrate

        if (musicVolume <= 0.01f) {
            stopMusic()
        } else if (wasMusicOff && isAppForeground) {
            startMusic(inGameplayMusic, currentTempoBpm)
        } else {
            runCatching {
                musicTrack?.setVolume(musicVolume * 0.45f)
            }
        }
    }

    fun setGameplayMode(gameplay: Boolean, tempoBpm: Int = 124) {
        inGameplayMusic = gameplay
        currentTempoBpm = tempoBpm.coerceIn(95, 160)
        if (musicVolume > 0.01f && isAppForeground) {
            if (musicJob?.isActive != true) {
                startMusic(gameplay, currentTempoBpm)
            }
        }
    }

    fun onPause() {
        isAppForeground = false
        stopMusic()
    }

    fun onResume() {
        isAppForeground = true
        if (musicVolume > 0.01f) {
            startMusic(inGameplayMusic, currentTempoBpm)
        }
    }

    fun playSfx(effect: SoundEffect) {
        val vol = sfxVolume
        if (vol <= 0.01f) return
        val samples = sfxBuffers[effect] ?: return
        scope.launch {
            playOneShotSamples(samples, vol)
        }
    }

    fun vibrateImpact(heavy: Boolean = false) {
        if (!vibrationEnabled) return
        runCatching {
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vm?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            } ?: return
            if (!vibrator.hasVibrator()) return
            val ms = if (heavy) 95L else 24L
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val amp = if (heavy) 210 else 90
                vibrator.vibrate(VibrationEffect.createOneShot(ms, amp))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(ms)
            }
        }
    }

    private fun startMusic(gameplay: Boolean, bpm: Int) {
        stopMusic()
        if (musicVolume <= 0.01f || !isAppForeground) return

        musicJob = scope.launch {
            runCatching {
                val minBuf = AudioTrack.getMinBufferSize(
                    sampleRate,
                    AudioFormat.CHANNEL_OUT_MONO,
                    AudioFormat.ENCODING_PCM_16BIT
                ).coerceAtLeast(2048)

                val track = AudioTrack.Builder()
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_GAME)
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                            .build()
                    )
                    .setAudioFormat(
                        AudioFormat.Builder()
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setSampleRate(sampleRate)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                            .build()
                    )
                    .setBufferSizeInBytes(minBuf * 2)
                    .setTransferMode(AudioTrack.MODE_STREAM)
                    .build()

                musicTrack = track
                track.setVolume(musicVolume * 0.45f)
                track.play()

                // Original 16-step synthwave/arcade arpeggio pattern
                val menuNotes = floatArrayOf(
                    220f, 261.63f, 329.63f, 392f,
                    220f, 261.63f, 329.63f, 440f,
                    196f, 246.94f, 293.66f, 392f,
                    174.61f, 220f, 261.63f, 349.23f
                )
                val gameNotes = floatArrayOf(
                    261.63f, 329.63f, 392f, 523.25f,
                    293.66f, 349.23f, 440f, 587.33f,
                    246.94f, 329.63f, 392f, 493.88f,
                    220f, 261.63f, 329.63f, 440f
                )

                var step = 0
                while (isActive && isAppForeground && musicVolume > 0.01f) {
                    val activeBpm = currentTempoBpm
                    val stepDurationSec = (60f / activeBpm) / 2f // eighth-note pulse
                    val sampleCount = (sampleRate * stepDurationSec).toInt().coerceIn(512, sampleRate)
                    val notes = if (inGameplayMusic) gameNotes else menuNotes
                    val leadFreq = notes[step % notes.size]
                    val bassFreq = leadFreq * 0.5f

                    val chunk = ShortArray(sampleCount)
                    for (i in 0 until sampleCount) {
                        val t = i.toFloat() / sampleRate
                        val env = exp((-t * 5.2f).toDouble()).toFloat()
                        val lead = sin(2.0 * PI * leadFreq * t).toFloat() * 0.35f * env
                        val bass = if (step % 2 == 0) {
                            sin(2.0 * PI * bassFreq * t).toFloat() * 0.30f * exp((-t * 3.5f).toDouble()).toFloat()
                        } else 0f
                        val mix = (lead + bass).coerceIn(-0.9f, 0.9f)
                        chunk[i] = (mix * Short.MAX_VALUE).toInt().toShort()
                    }
                    track.write(chunk, 0, chunk.size)
                    step++
                }
            }
        }
    }

    private fun stopMusic() {
        musicJob?.cancel()
        musicJob = null
        runCatching {
            musicTrack?.pause()
            musicTrack?.flush()
            musicTrack?.release()
        }
        musicTrack = null
    }

    private fun playOneShotSamples(samples: ShortArray, volume: Float) {
        runCatching {
            val byteSize = samples.size * 2
            val track = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_GAME)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(sampleRate)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setBufferSizeInBytes(byteSize.coerceAtLeast(1024))
                .setTransferMode(AudioTrack.MODE_STATIC)
                .build()
            track.write(samples, 0, samples.size)
            track.setVolume(volume.coerceIn(0f, 1f))
            track.play()
            Thread.sleep(((samples.size * 1000L) / sampleRate) + 40L)
            track.release()
        }
    }

    private fun buildSfxLibrary(): Map<SoundEffect, ShortArray> = mapOf(
        SoundEffect.UI_CLICK to synthSweep(520f, 780f, 0.055f, 0.38f),
        SoundEffect.JUMP to synthSweep(260f, 640f, 0.14f, 0.55f),
        SoundEffect.LAND to synthSweep(180f, 75f, 0.09f, 0.45f),
        SoundEffect.SLIDE to synthSweep(390f, 160f, 0.16f, 0.42f),
        SoundEffect.COIN to synthTwoTone(880f, 1318.5f, 0.11f, 0.50f),
        SoundEffect.POWER_UP to synthArpeggio(floatArrayOf(523.25f, 659.25f, 783.99f, 1046.5f), 0.24f, 0.55f),
        SoundEffect.NEAR_MISS to synthSweep(620f, 980f, 0.12f, 0.48f),
        SoundEffect.COLLISION to synthNoiseImpact(0.18f, 0.65f),
        SoundEffect.GAME_OVER to synthArpeggio(floatArrayOf(440f, 349.23f, 293.66f, 220f), 0.42f, 0.55f),
        SoundEffect.ACHIEVEMENT to synthArpeggio(floatArrayOf(587.33f, 739.99f, 880f, 1174.66f), 0.32f, 0.55f),
        SoundEffect.FOOTSTEP to synthSweep(140f, 90f, 0.04f, 0.20f)
    )

    private fun synthSweep(startHz: Float, endHz: Float, durationSec: Float, amp: Float): ShortArray {
        val count = (sampleRate * durationSec).toInt().coerceAtLeast(256)
        val out = ShortArray(count)
        var phase = 0.0
        for (i in 0 until count) {
            val p = i.toFloat() / count
            val freq = startHz + (endHz - startHz) * p
            phase += 2.0 * PI * freq / sampleRate
            val env = (1f - p)
            out[i] = (sin(phase).toFloat() * amp * env * Short.MAX_VALUE).toInt().toShort()
        }
        return out
    }

    private fun synthTwoTone(f1: Float, f2: Float, durationSec: Float, amp: Float): ShortArray {
        val count = (sampleRate * durationSec).toInt().coerceAtLeast(256)
        val out = ShortArray(count)
        var phase = 0.0
        for (i in 0 until count) {
            val p = i.toFloat() / count
            val freq = if (p < 0.45f) f1 else f2
            phase += 2.0 * PI * freq / sampleRate
            val env = (1f - p * 0.7f)
            out[i] = (sin(phase).toFloat() * amp * env * Short.MAX_VALUE).toInt().toShort()
        }
        return out
    }

    private fun synthArpeggio(notes: FloatArray, durationSec: Float, amp: Float): ShortArray {
        val count = (sampleRate * durationSec).toInt().coerceAtLeast(256)
        val out = ShortArray(count)
        var phase = 0.0
        for (i in 0 until count) {
            val p = i.toFloat() / count
            val idx = (p * notes.size).toInt().coerceIn(0, notes.lastIndex)
            phase += 2.0 * PI * notes[idx] / sampleRate
            val env = (1f - p * 0.6f)
            out[i] = (sin(phase).toFloat() * amp * env * Short.MAX_VALUE).toInt().toShort()
        }
        return out
    }

    private fun synthNoiseImpact(durationSec: Float, amp: Float): ShortArray {
        val count = (sampleRate * durationSec).toInt().coerceAtLeast(256)
        val out = ShortArray(count)
        var seed = 1337
        for (i in 0 until count) {
            seed = (seed * 1103515245 + 12345) and 0x7fffffff
            val noise = ((seed % 2000) / 1000f) - 1f
            val p = i.toFloat() / count
            val thud = sin(2.0 * PI * (110.0 - 70.0 * p) * (i.toDouble() / sampleRate)).toFloat()
            val env = exp((-p * 4.5f).toDouble()).toFloat()
            val mix = (noise * 0.45f + thud * 0.55f) * amp * env
            out[i] = (mix.coerceIn(-1f, 1f) * Short.MAX_VALUE).toInt().toShort()
        }
        return out
    }
}

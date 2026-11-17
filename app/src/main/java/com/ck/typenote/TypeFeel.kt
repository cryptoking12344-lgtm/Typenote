package com.ck.typenote

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin
import kotlin.random.Random

/**
 * Typewriter sounds and haptics. The sounds are synthesised once at start-up
 * (no audio files to license) and played through a low-latency SoundPool.
 */
class TypeFeel(context: Context) {
    private val rate = 44100
    private val pool: SoundPool = SoundPool.Builder()
        .setMaxStreams(8)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
        ).build()

    private val keys: List<Int>
    private val space: Int
    private val back: Int
    private val bell: Int
    private val carriage: Int
    private val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= 31) {
        (context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager)?.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    }

    init {
        val dir = File(context.cacheDir, "sounds").apply { mkdirs() }
        keys = (0 until 4).map { seed -> load(dir, "key$seed", keyStrike(seed, 0.030, 1.0)) }
        space = load(dir, "space", keyStrike(9, 0.050, 0.6))
        back = load(dir, "back", keyStrike(5, 0.022, 0.55))
        bell = load(dir, "bell", bellTone())
        carriage = load(dir, "carriage", carriageReturn())
    }

    fun key(sound: Boolean, haptic: Boolean) {
        if (sound) pool.play(keys.random(), 0.9f, 0.9f, 1, 0, Random.nextDouble(0.94, 1.06).toFloat())
        if (haptic) tick()
    }

    fun space(sound: Boolean, haptic: Boolean) {
        if (sound) pool.play(space, 0.8f, 0.8f, 1, 0, 1f)
        if (haptic) tick()
    }

    fun backspace(sound: Boolean, haptic: Boolean) {
        if (sound) pool.play(back, 0.7f, 0.7f, 1, 0, 1f)
        if (haptic) tick()
    }

    fun bell(sound: Boolean, haptic: Boolean) {
        if (sound) pool.play(bell, 0.6f, 0.6f, 2, 0, 1f)
        if (haptic) heavy()
    }

    fun carriageReturn(sound: Boolean, haptic: Boolean) {
        if (sound) pool.play(carriage, 0.85f, 0.85f, 2, 0, 1f)
        if (haptic) vibrate(VibrationEffect.createWaveform(longArrayOf(0, 12, 30, 12, 30, 12, 40, 28), -1))
    }

    fun thud(haptic: Boolean) {
        if (haptic) heavy()
    }

    private fun tick() {
        if (Build.VERSION.SDK_INT >= 29) vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_TICK))
        else vibrate(VibrationEffect.createOneShot(10, VibrationEffect.DEFAULT_AMPLITUDE))
    }

    private fun heavy() {
        if (Build.VERSION.SDK_INT >= 29) vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_HEAVY_CLICK))
        else vibrate(VibrationEffect.createOneShot(25, VibrationEffect.DEFAULT_AMPLITUDE))
    }

    private fun vibrate(effect: VibrationEffect) {
        try {
            vibrator?.vibrate(effect)
        } catch (_: Exception) {
        }
    }

    // ---- synthesis -------------------------------------------------------

    /** A type bar hitting the platen: a sharp noise transient plus a short wooden thump. */
    private fun keyStrike(seed: Int, seconds: Double, weight: Double): ShortArray {
        val rnd = Random(seed * 7919 + 13)
        val n = (rate * seconds).toInt()
        val thumpHz = 150.0 + seed * 22
        return ShortArray(n) { i ->
            val t = i.toDouble() / rate
            val noise = (rnd.nextDouble() * 2 - 1) * exp(-t / 0.0028)
            val body = sin(2 * PI * thumpHz * t) * exp(-t / 0.010) * 0.55 * weight
            val ring = sin(2 * PI * 2300.0 * t) * exp(-t / 0.004) * 0.18
            pcm((noise * 0.8 + body + ring) * 0.85)
        }
    }

    private fun bellTone(): ShortArray {
        val n = (rate * 1.1).toInt()
        return ShortArray(n) { i ->
            val t = i.toDouble() / rate
            val env = exp(-t / 0.32)
            val strike = if (t < 0.003) t / 0.003 else 1.0
            val v = sin(2 * PI * 2093.0 * t) * 0.55 + sin(2 * PI * 5274.0 * t) * 0.18 * exp(-t / 0.08) +
                sin(2 * PI * 3135.0 * t) * 0.12
            pcm(v * env * strike * 0.7)
        }
    }

    /** Ratchet clicks of the carriage sliding back, then the thunk of the line feed. */
    private fun carriageReturn(): ShortArray {
        val total = (rate * 0.42).toInt()
        val out = DoubleArray(total)
        val rnd = Random(42)
        val ticks = 11
        for (k in 0 until ticks) {
            val start = (rate * (0.012 + k * 0.022)).toInt()
            val len = (rate * 0.012).toInt()
            for (i in 0 until len) {
                val idx = start + i
                if (idx >= total) break
                val t = i.toDouble() / rate
                out[idx] += (rnd.nextDouble() * 2 - 1) * exp(-t / 0.0018) * 0.45
            }
        }
        val thunkStart = (rate * 0.27).toInt()
        for (i in 0 until total - thunkStart) {
            val t = i.toDouble() / rate
            out[thunkStart + i] += sin(2 * PI * 110.0 * t) * exp(-t / 0.03) * 0.8 +
                (rnd.nextDouble() * 2 - 1) * exp(-t / 0.004) * 0.6
        }
        return ShortArray(total) { pcm(out[it] * 0.8) }
    }

    private fun pcm(v: Double): Short = (v.coerceIn(-1.0, 1.0) * Short.MAX_VALUE).toInt().toShort()

    private fun load(dir: File, name: String, samples: ShortArray): Int {
        val f = File(dir, "$name.wav")
        writeWav(f, samples)
        return pool.load(f.absolutePath, 1)
    }

    private fun writeWav(file: File, samples: ShortArray) {
        val dataLen = samples.size * 2
        val buf = ByteBuffer.allocate(44 + dataLen).order(ByteOrder.LITTLE_ENDIAN)
        buf.put("RIFF".toByteArray()); buf.putInt(36 + dataLen); buf.put("WAVE".toByteArray())
        buf.put("fmt ".toByteArray()); buf.putInt(16); buf.putShort(1); buf.putShort(1)
        buf.putInt(rate); buf.putInt(rate * 2); buf.putShort(2); buf.putShort(16)
        buf.put("data".toByteArray()); buf.putInt(dataLen)
        samples.forEach { buf.putShort(it) }
        FileOutputStream(file).use { it.write(buf.array()) }
    }
}

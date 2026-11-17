package com.unibud.app.core.audio

import java.util.Locale

object EqBands {
    val FREQUENCIES_HZ: List<Int> = listOf(60, 230, 910, 3600, 14000)
    val COUNT: Int get() = FREQUENCIES_HZ.size
    const val MIN_DB = -12f
    const val MAX_DB = 12f

    fun clamp(gainDb: Float): Float = gainDb.coerceIn(MIN_DB, MAX_DB)

    fun label(hz: Int): String = if (hz >= 1000) "${hz / 1000.0}".removeSuffix(".0") + " kHz" else "$hz Hz"
}

data class EqPreset(
    val id: Long,
    val name: String,
    val gainsDb: List<Float>,
    val builtIn: Boolean = false,
) {
    init {
        require(gainsDb.size == EqBands.COUNT) { "A preset needs exactly ${EqBands.COUNT} bands" }
    }
}

/** Compact text form of the band gains, stored in Room and (later) in the cloud. */
object EqCodec {
    fun encode(gainsDb: List<Float>): String =
        gainsDb.joinToString(",") { String.format(Locale.ROOT, "%.1f", EqBands.clamp(it)) }

    fun decode(text: String): List<Float>? {
        val parts = text.split(",")
        if (parts.size != EqBands.COUNT) return null
        val values = parts.map { it.trim().toFloatOrNull() ?: return null }
        return values.map(EqBands::clamp)
    }
}

object EqPresetRules {
    const val MAX_NAME_LENGTH = 30

    /** Returns the cleaned name, or null if it cannot be used. */
    fun cleanName(raw: String): String? = raw.trim().takeIf { it.isNotEmpty() && it.length <= MAX_NAME_LENGTH }
}

object BuiltInPresets {
    val all: List<EqPreset> = listOf(
        EqPreset(-1, "Flat", listOf(0f, 0f, 0f, 0f, 0f), builtIn = true),
        EqPreset(-2, "Bass boost", listOf(6f, 4f, 0f, 0f, 0f), builtIn = true),
        EqPreset(-3, "Treble boost", listOf(0f, 0f, 0f, 3f, 5f), builtIn = true),
        EqPreset(-4, "Vocal", listOf(-2f, 0f, 4f, 3f, 0f), builtIn = true),
        EqPreset(-5, "Late night", listOf(3f, 1f, 0f, -1f, -3f), builtIn = true),
        EqPreset(-6, "Pop", listOf(-1f, 2f, 4f, 2f, -1f), builtIn = true),
        EqPreset(-7, "Rock", listOf(4f, 2f, -2f, 2f, 4f), builtIn = true),
    )
}

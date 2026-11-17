package com.unibud.app.ui

import android.text.format.DateUtils
import com.unibud.app.core.model.Brand
import com.unibud.app.core.model.BrandSource
import com.unibud.app.core.model.Classification
import com.unibud.app.core.model.Confidence
import com.unibud.app.core.model.DeviceKind

fun kindLabel(c: Classification): String = when (c.kind) {
    DeviceKind.TWS_EARBUDS -> if (c.confidence == Confidence.LOW) "Possibly earbuds" else "Looks like earbuds"
    DeviceKind.HEADSET -> "Headset or earbuds"
    DeviceKind.SPEAKER -> "Speaker"
    DeviceKind.OTHER_AUDIO -> "Audio device"
    DeviceKind.NOT_AUDIO -> "Not an audio device"
    DeviceKind.UNKNOWN -> "Unknown device"
}

/** Short brand text for lists, with a hint when it is only a guess. */
fun brandLabel(brand: Brand?): String? = brand?.let {
    if (it.source == BrandSource.GUESS) "${it.displayName} (guessed)" else it.displayName
}

fun brandSourceLabel(brand: Brand?): String = when (brand?.source) {
    null -> "Not detected"
    BrandSource.USER -> "Set by you"
    BrandSource.DEVICE_NAME -> "From the device name"
    BrandSource.MANUFACTURER_DATA -> "From Bluetooth manufacturer data"
    BrandSource.DEVICE_INFO -> "Reported by the device"
    BrandSource.GUESS -> "Guessed from the first word of the name"
}

fun signalLabel(rssi: Int?): String? = rssi?.let {
    when {
        it >= -60 -> "Strong signal"
        it >= -80 -> "Medium signal"
        else -> "Weak signal"
    }
}

fun relativeTime(timestamp: Long): String =
    DateUtils.getRelativeTimeSpanString(timestamp, System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS).toString()

fun channelLabel(channels: List<Int>): String? = when {
    channels.isEmpty() -> null
    2 in channels -> "Stereo"
    channels == listOf(1) -> "Mono"
    else -> channels.joinToString(", ") + " channels"
}

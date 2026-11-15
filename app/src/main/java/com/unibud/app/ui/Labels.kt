package com.unibud.app.ui

import android.text.format.DateUtils
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

fun signalLabel(rssi: Int?): String? = rssi?.let {
    when {
        it >= -60 -> "Strong signal"
        it >= -80 -> "Medium signal"
        else -> "Weak signal"
    }
}

fun relativeTime(timestamp: Long): String =
    DateUtils.getRelativeTimeSpanString(timestamp, System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS).toString()

package com.unibud.app.core.registry

import com.unibud.app.core.model.Classification
import com.unibud.app.core.model.Confidence
import com.unibud.app.core.model.DeviceKind
import com.unibud.app.core.model.DiscoveredDevice

/**
 * Best-effort device classification from the Bluetooth class-of-device, advertised/SDP services
 * and the device name. Bluetooth does not expose a "this is a true-wireless earbud" flag, so a
 * TWS result is never HIGH confidence and the UI should say so.
 */
object DeviceClassifier {
    private const val MAJOR_MASK = 0x1F00
    private const val MAJOR_AUDIO_VIDEO = 0x0400
    private const val MAJOR_MISC = 0x0000
    private const val MAJOR_UNCATEGORIZED = 0x1F00

    // Minor classes (full values): wearable headset, hands-free, headphones.
    private val WEARABLE_AUDIO = setOf(0x0404, 0x0408, 0x0418)
    // Loudspeaker, portable audio, HiFi audio, car audio.
    private val SPEAKER_LIKE = setOf(0x0414, 0x041C, 0x0428, 0x0420)

    // A2DP sink, A2DP, Headset, Hands-free, LE Audio ASCS, LE Audio PACS.
    private val AUDIO_SERVICES = setOf(
        "0000110b-0000-1000-8000-00805f9b34fb",
        "0000110d-0000-1000-8000-00805f9b34fb",
        "00001108-0000-1000-8000-00805f9b34fb",
        "0000111e-0000-1000-8000-00805f9b34fb",
        "0000184e-0000-1000-8000-00805f9b34fb",
        "00001850-0000-1000-8000-00805f9b34fb",
    )

    private val TWS_NAME_HINT = Regex(
        "buds|pods|earbud|tws|airdopes|liberty|linkbuds|\\bwf-|nothing ear|\\bear\\s*\\(|\\benco\\b|jabra elite|\\belite \\d",
        RegexOption.IGNORE_CASE,
    )

    fun classify(name: String?, deviceClass: Int?, serviceUuids: Set<String>): Classification {
        val major = deviceClass?.and(MAJOR_MASK)
        val audioClass = major == MAJOR_AUDIO_VIDEO
        val audioService = serviceUuids.any { it.lowercase() in AUDIO_SERVICES }
        val audioEvidence = audioClass || audioService
        val nameHint = name != null && TWS_NAME_HINT.containsMatchIn(name)
        val wearable = deviceClass != null && deviceClass in WEARABLE_AUDIO
        val speaker = deviceClass != null && deviceClass in SPEAKER_LIKE

        return when {
            nameHint && audioEvidence -> Classification(
                DeviceKind.TWS_EARBUDS, Confidence.MEDIUM,
                "Audio device whose name suggests earbuds. Bluetooth cannot confirm the true-wireless form factor.",
            )
            nameHint -> Classification(
                DeviceKind.TWS_EARBUDS, Confidence.LOW,
                "Name suggests earbuds, but no audio class or profile has been observed yet.",
            )
            wearable -> Classification(
                DeviceKind.HEADSET, Confidence.MEDIUM,
                "Reports a headset or headphones class. It may still be earbuds.",
            )
            speaker -> Classification(DeviceKind.SPEAKER, Confidence.HIGH, "Reports a speaker-type audio class.")
            audioEvidence -> Classification(DeviceKind.OTHER_AUDIO, Confidence.LOW, "Audio profile or class observed.")
            major != null && major != MAJOR_MISC && major != MAJOR_UNCATEGORIZED ->
                Classification(DeviceKind.NOT_AUDIO, Confidence.HIGH, "Reports a non-audio device class.")
            else -> Classification(DeviceKind.UNKNOWN, Confidence.LOW, "Not enough information to classify.")
        }
    }
}

/** Keeps the Devices list focused on audio accessories instead of every nearby BLE beacon. */
object DeviceListPolicy {
    fun isRelevant(device: DiscoveredDevice): Boolean {
        val kind = device.classification.kind
        if (kind == DeviceKind.NOT_AUDIO) return false
        if (kind == DeviceKind.UNKNOWN && device.name.isNullOrBlank() && !device.isBonded) return false
        return true
    }
}

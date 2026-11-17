package com.unibud.app.core.capability

import com.unibud.app.core.model.DeviceInfoDetails
import com.unibud.app.core.model.DiscoveredDevice
import com.unibud.app.core.model.FeatureId
import com.unibud.app.core.model.FeatureStatus

/** What one integration learned about a device. Integrations only report what they verified. */
data class IntegrationFindings(
    val features: List<FeatureStatus>,
    val info: DeviceInfoDetails? = null,
    val notes: List<String> = emptyList(),
)

/** Read side of a device integration (Layers 1-3). */
interface DeviceIntegration {
    val id: String
    suspend fun discover(device: DiscoveredDevice): IntegrationFindings
}

enum class SoundMode { NORMAL, ANC, TRANSPARENCY, ADAPTIVE }

sealed class ControlCommand(val feature: FeatureId) {
    data class SetSoundMode(val mode: SoundMode) : ControlCommand(
        when (mode) {
            SoundMode.NORMAL -> FeatureId.NORMAL_MODE
            SoundMode.ANC -> FeatureId.ANC
            SoundMode.TRANSPARENCY -> FeatureId.TRANSPARENCY
            SoundMode.ADAPTIVE -> FeatureId.ADAPTIVE_NOISE
        },
    )

    data class SetLowLatency(val enabled: Boolean) : ControlCommand(FeatureId.LOW_LATENCY)

    /** Band gains in dB, in the order of [com.unibud.app.core.audio.EqBands.FREQUENCIES_HZ]. */
    data class SetEqualizer(val gainsDb: List<Float>) : ControlCommand(FeatureId.EQUALIZER)

    /** Android media volume, 0 to 100. Drives any Bluetooth earbuds that are the active audio output. */
    data class SetVolume(val percent: Int) : ControlCommand(FeatureId.VOLUME)

    /** Transport key sent to whichever media app is playing. Works with any earbuds. */
    data class MediaAction(val key: MediaKey) : ControlCommand(FeatureId.MEDIA_PLAYBACK)
}

enum class MediaKey { PLAY_PAUSE, NEXT, PREVIOUS }

sealed class ControlResult {
    /** [confirmedBy] describes how the device confirmed the change (for example a read-back). */
    data class Success(val confirmedBy: String) : ControlResult()
    data class NotSupported(val reason: String) : ControlResult()
    data class Failed(val reason: String) : ControlResult()
}

/** Write side: implemented only by integrations that talk to the real device protocol. */
interface DeviceController {
    val integrationId: String
    fun canControl(device: DiscoveredDevice, feature: FeatureId): Boolean
    suspend fun execute(device: DiscoveredDevice, command: ControlCommand): ControlResult
}

/**
 * Single entry point for every control action in the app. If no controller can handle the
 * feature, the answer is NotSupported. A success is never fabricated here.
 */
class ControlGateway(private val controllers: List<DeviceController> = emptyList()) {
    suspend fun execute(device: DiscoveredDevice, command: ControlCommand): ControlResult {
        val controller = controllers.firstOrNull { it.canControl(device, command.feature) }
            ?: return ControlResult.NotSupported(
                "No integration can control \"${command.feature.label}\" on ${device.name ?: device.address}.",
            )
        return controller.execute(device, command)
    }
}

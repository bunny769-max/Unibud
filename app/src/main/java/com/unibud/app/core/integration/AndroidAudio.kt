package com.unibud.app.core.integration

import android.content.Context
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.Build
import android.view.KeyEvent
import com.unibud.app.core.capability.ControlCommand
import com.unibud.app.core.capability.ControlResult
import com.unibud.app.core.capability.DeviceController
import com.unibud.app.core.capability.DeviceIntegration
import com.unibud.app.core.capability.IntegrationFindings
import com.unibud.app.core.capability.MediaKey
import com.unibud.app.core.model.DeviceInfoDetails
import com.unibud.app.core.model.DiscoveredDevice
import com.unibud.app.core.model.FeatureId
import com.unibud.app.core.model.FeatureStatus
import com.unibud.app.core.model.SupportLevel
import com.unibud.app.core.model.SupportSource
import kotlin.math.roundToInt

/**
 * Brand-independent facts and controls from Android's own audio system. They work with any
 * Bluetooth earbuds while those earbuds are the active audio output.
 */
class AudioRouteInspector(context: Context) {
    private val audio = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    data class Snapshot(
        val active: Boolean,
        val typeLabel: String?,
        val channelCounts: List<Int>,
        val sampleRates: List<Int>,
    )

    fun inspect(address: String): Snapshot {
        val none = Snapshot(false, null, emptyList(), emptyList())
        // AudioDeviceInfo.getAddress() needs Android 9. Older phones cannot match a specific device.
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) return none
        val match = audio.getDevices(AudioManager.GET_DEVICES_OUTPUTS).firstOrNull { d ->
            typeLabel(d.type) != null && d.address.equals(address, ignoreCase = true)
        } ?: return none
        return Snapshot(true, typeLabel(match.type), match.channelCounts.toList(), match.sampleRates.toList())
    }

    private fun typeLabel(type: Int): String? = when {
        type == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP -> "Bluetooth stereo (A2DP)"
        type == AudioDeviceInfo.TYPE_BLUETOOTH_SCO -> "Bluetooth calls (SCO)"
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && type == AudioDeviceInfo.TYPE_BLE_HEADSET -> "Bluetooth LE Audio"
        else -> null
    }

    fun volumePercent(): Int {
        val max = audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        return if (max <= 0) 0 else (audio.getStreamVolume(AudioManager.STREAM_MUSIC) * 100f / max).roundToInt()
    }

    /** Sets the media volume and returns what Android reports afterwards. */
    fun setVolumePercent(percent: Int): Int {
        val max = audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        val target = (percent.coerceIn(0, 100) / 100f * max).roundToInt().coerceIn(0, max)
        audio.setStreamVolume(AudioManager.STREAM_MUSIC, target, 0)
        return volumePercent()
    }

    fun sendMediaKey(key: MediaKey) {
        val code = when (key) {
            MediaKey.PLAY_PAUSE -> KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE
            MediaKey.NEXT -> KeyEvent.KEYCODE_MEDIA_NEXT
            MediaKey.PREVIOUS -> KeyEvent.KEYCODE_MEDIA_PREVIOUS
        }
        audio.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, code))
        audio.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_UP, code))
    }
}

/** Read side: is this device the active audio output, and what does Android know about that output? */
class AudioRouteIntegration(context: Context) : DeviceIntegration {
    override val id = "android-audio-route"
    private val inspector = AudioRouteInspector(context)

    override suspend fun discover(device: DiscoveredDevice): IntegrationFindings {
        val s = inspector.inspect(device.address)
        val features = if (s.active) {
            listOf(
                FeatureStatus(
                    FeatureId.VOLUME, SupportLevel.SUPPORTED, SupportSource.ANDROID_STANDARD,
                    "Sets Android's media volume, which drives these earbuds while they are the audio output.",
                ),
                FeatureStatus(
                    FeatureId.MEDIA_PLAYBACK, SupportLevel.SUPPORTED, SupportSource.ANDROID_STANDARD,
                    "Sends play, pause, next and previous to the media app that is playing.",
                ),
            )
        } else {
            val why = "These earbuds are not the active audio output right now. Connect them and play something."
            listOf(
                FeatureStatus(FeatureId.VOLUME, SupportLevel.UNAVAILABLE, SupportSource.ANDROID_STANDARD, why),
                FeatureStatus(FeatureId.MEDIA_PLAYBACK, SupportLevel.UNAVAILABLE, SupportSource.ANDROID_STANDARD, why),
            )
        }
        val info = DeviceInfoDetails(
            audioOutputActive = s.active,
            audioOutputType = s.typeLabel,
            audioChannels = s.channelCounts,
            audioSampleRates = s.sampleRates,
        )
        return IntegrationFindings(features, info)
    }
}

/** Write side: the only controller registered today. It never touches earbud firmware. */
class AndroidAudioController(context: Context) : DeviceController {
    override val integrationId = "android-audio"
    private val inspector = AudioRouteInspector(context)

    override fun canControl(device: DiscoveredDevice, feature: FeatureId): Boolean =
        (feature == FeatureId.VOLUME || feature == FeatureId.MEDIA_PLAYBACK) && inspector.inspect(device.address).active

    override suspend fun execute(device: DiscoveredDevice, command: ControlCommand): ControlResult = try {
        when (command) {
            is ControlCommand.SetVolume -> {
                val now = inspector.setVolumePercent(command.percent)
                ControlResult.Success("Android reports media volume at $now%")
            }
            is ControlCommand.MediaAction -> {
                inspector.sendMediaKey(command.key)
                ControlResult.Success("key sent to the active media app, playback state is not read back")
            }
            else -> ControlResult.NotSupported("This control needs the earbuds' own protocol.")
        }
    } catch (e: SecurityException) {
        ControlResult.Failed(e.message ?: "Android refused the request.")
    }
}

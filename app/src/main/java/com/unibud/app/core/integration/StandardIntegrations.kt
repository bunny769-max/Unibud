package com.unibud.app.core.integration

import com.unibud.app.core.bluetooth.GattProbe
import com.unibud.app.core.capability.DeviceIntegration
import com.unibud.app.core.capability.GattFindingsInterpreter
import com.unibud.app.core.capability.IntegrationFindings
import com.unibud.app.core.model.DiscoveredDevice
import com.unibud.app.core.model.FeatureId
import com.unibud.app.core.model.FeatureStatus
import com.unibud.app.core.model.SupportLevel
import com.unibud.app.core.model.SupportSource
import com.unibud.app.core.model.Transport

/** Layer 1: what Android's public Bluetooth APIs genuinely provide. */
class AndroidStandardIntegration : DeviceIntegration {
    override val id = "android-standard"

    override suspend fun discover(device: DiscoveredDevice) = IntegrationFindings(
        listOf(
            FeatureStatus(
                FeatureId.DEVICE_IDENTITY, SupportLevel.SUPPORTED, SupportSource.ANDROID_STANDARD,
                "Name, address and class come from Android Bluetooth.",
            ),
            FeatureStatus(
                FeatureId.CONNECTION_STATUS, SupportLevel.SUPPORTED, SupportSource.ANDROID_STANDARD,
                "Read from the A2DP and Headset profile state.",
            ),
            FeatureStatus(
                FeatureId.CODEC_INFO, SupportLevel.UNAVAILABLE, SupportSource.ANDROID_STANDARD,
                "The active audio codec is not available through public Android APIs.",
            ),
        ),
    )
}

/** Layer 2: standard BLE GATT services (Battery, Device Information), read-only. */
class GattStandardIntegration(private val probe: GattProbe) : DeviceIntegration {
    override val id = "ble-standard-gatt"

    override suspend fun discover(device: DiscoveredDevice): IntegrationFindings {
        val eligible = Transport.BLE in device.seenOver || device.isBonded
        if (!eligible) {
            return IntegrationFindings(
                GattFindingsInterpreter.features(null),
                notes = listOf("BLE probe skipped: this device has not been seen over BLE and is not paired."),
            )
        }
        val result = probe.probe(device.address)
        val info = GattFindingsInterpreter.details(result)
        val notes = if (result == null) {
            listOf("A BLE (GATT) connection was not possible. Some earbuds do not accept BLE connections from other apps.")
        } else {
            emptyList()
        }
        return IntegrationFindings(GattFindingsInterpreter.features(info), info, notes)
    }
}

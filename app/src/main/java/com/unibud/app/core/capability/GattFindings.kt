package com.unibud.app.core.capability

import com.unibud.app.core.model.DeviceInfoDetails
import com.unibud.app.core.model.FeatureId
import com.unibud.app.core.model.FeatureStatus
import com.unibud.app.core.model.SupportLevel
import com.unibud.app.core.model.SupportSource
import java.util.Locale
import java.util.UUID

/** Bluetooth SIG standard 16-bit UUIDs used by the Layer 2 probe. */
object SigUuids {
    fun uuid16(short: Int): UUID =
        UUID.fromString(String.format(Locale.ROOT, "0000%04x-0000-1000-8000-00805f9b34fb", short))

    val BATTERY_SERVICE: UUID = uuid16(0x180F)
    val BATTERY_LEVEL: UUID = uuid16(0x2A19)
    val DEVICE_INFO_SERVICE: UUID = uuid16(0x180A)
    val MANUFACTURER_NAME: UUID = uuid16(0x2A29)
    val MODEL_NUMBER: UUID = uuid16(0x2A24)
    val HARDWARE_REVISION: UUID = uuid16(0x2A27)
    val FIRMWARE_REVISION: UUID = uuid16(0x2A26)
    val SOFTWARE_REVISION: UUID = uuid16(0x2A28)
}

class GattProbeResult(val services: Set<UUID>, val values: Map<UUID, ByteArray>)

object GattFindingsInterpreter {

    fun details(result: GattProbeResult?): DeviceInfoDetails? {
        if (result == null) return null
        fun text(uuid: UUID): String? = result.values[uuid]
            ?.let { String(it, Charsets.UTF_8).trim { c -> c <= ' ' } }
            ?.takeIf { it.isNotEmpty() }

        val battery = result.values[SigUuids.BATTERY_LEVEL]
            ?.firstOrNull()?.toInt()?.and(0xFF)?.takeIf { it in 0..100 }

        val info = DeviceInfoDetails(
            manufacturerName = text(SigUuids.MANUFACTURER_NAME),
            modelNumber = text(SigUuids.MODEL_NUMBER),
            firmwareRevision = text(SigUuids.FIRMWARE_REVISION),
            hardwareRevision = text(SigUuids.HARDWARE_REVISION),
            softwareRevision = text(SigUuids.SOFTWARE_REVISION),
            batteryPercent = battery,
        )
        return if (info == DeviceInfoDetails()) null else info
    }

    fun features(info: DeviceInfoDetails?): List<FeatureStatus> = listOf(
        if (info?.batteryPercent != null) {
            FeatureStatus(
                FeatureId.BATTERY_OVERALL, SupportLevel.PARTIAL, SupportSource.BLE_STANDARD_GATT,
                "Single value from the standard BLE Battery Service. Per-earbud and case battery need a manufacturer protocol.",
            )
        } else {
            FeatureStatus(
                FeatureId.BATTERY_OVERALL, SupportLevel.UNAVAILABLE, SupportSource.BLE_STANDARD_GATT,
                "The device did not expose the standard BLE Battery Service.",
            )
        },
        if (info?.firmwareRevision != null) {
            FeatureStatus(
                FeatureId.FIRMWARE_INFO, SupportLevel.SUPPORTED, SupportSource.BLE_STANDARD_GATT,
                "Read from the standard Device Information Service.",
            )
        } else {
            FeatureStatus(
                FeatureId.FIRMWARE_INFO, SupportLevel.UNAVAILABLE, SupportSource.BLE_STANDARD_GATT,
                "The device did not expose a firmware revision through the standard Device Information Service.",
            )
        },
    )
}

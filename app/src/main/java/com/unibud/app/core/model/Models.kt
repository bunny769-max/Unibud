package com.unibud.app.core.model

enum class DeviceKind { TWS_EARBUDS, HEADSET, SPEAKER, OTHER_AUDIO, NOT_AUDIO, UNKNOWN }

enum class Confidence { HIGH, MEDIUM, LOW }

/** How a device was observed. A device can be seen over several transports. */
enum class Transport { CLASSIC, BLE, BONDED }

enum class SupportLevel(val rank: Int) { UNAVAILABLE(0), PARTIAL(1), SUPPORTED(2) }

/** Where a capability finding came from. Higher priority wins when sources disagree. */
enum class SupportSource(val priority: Int) {
    NONE(0),
    ANDROID_STANDARD(1),
    BLE_STANDARD_GATT(2),
    MANUFACTURER(3),
}

enum class FeatureId(val label: String) {
    DEVICE_IDENTITY("Device identification"),
    CONNECTION_STATUS("Connection status"),
    VOLUME("Volume"),
    MEDIA_PLAYBACK("Media controls"),
    ANC("Active noise cancellation"),
    TRANSPARENCY("Transparency mode"),
    NORMAL_MODE("Normal mode"),
    ADAPTIVE_NOISE("Adaptive noise control"),
    LOW_LATENCY("Low latency / gaming mode"),
    SPATIAL_AUDIO("Spatial audio"),
    EQUALIZER("Equalizer"),
    TOUCH_CONTROLS("Touch controls"),
    IN_EAR_DETECTION("In-ear detection"),
    AUTO_PAUSE("Automatic pause / play"),
    DUAL_CONNECTION("Dual device connection"),
    MICROPHONE_SETTINGS("Microphone settings"),
    CODEC_INFO("Audio codec information"),
    FIRMWARE_INFO("Firmware information"),
    BATTERY_OVERALL("Battery (single value)"),
    BATTERY_LEFT("Left earbud battery"),
    BATTERY_RIGHT("Right earbud battery"),
    BATTERY_CASE("Charging case battery"),
}

/** How a brand was worked out. GUESS means it was only inferred from the first word of the device name. */
enum class BrandSource { USER, DEVICE_NAME, MANUFACTURER_DATA, DEVICE_INFO, GUESS }

data class Brand(val id: String, val displayName: String, val source: BrandSource = BrandSource.DEVICE_NAME)

data class Classification(val kind: DeviceKind, val confidence: Confidence, val reason: String)

data class DiscoveredDevice(
    val address: String,
    val name: String?,
    val rssi: Int?,
    /** Full Bluetooth class-of-device value (e.g. 0x0418 = headphones), if known. */
    val deviceClass: Int?,
    val isBonded: Boolean,
    val isConnected: Boolean,
    val seenOver: Set<Transport>,
    /** BLE manufacturer company identifiers seen in advertising data. */
    val manufacturerIds: Set<Int>,
    val advertisedServices: Set<String>,
    val brand: Brand?,
    val classification: Classification,
)

/**
 * One feature's support status. A feature can only be SUPPORTED or PARTIAL if a real
 * source reported it: this is how the app avoids ever showing a feature the device lacks.
 */
data class FeatureStatus(
    val feature: FeatureId,
    val level: SupportLevel,
    val source: SupportSource,
    val reason: String,
) {
    init {
        require(level == SupportLevel.UNAVAILABLE || source != SupportSource.NONE) {
            "$feature cannot be $level without a reporting source"
        }
    }
}

data class DeviceInfoDetails(
    val manufacturerName: String? = null,
    val modelNumber: String? = null,
    val firmwareRevision: String? = null,
    val hardwareRevision: String? = null,
    val softwareRevision: String? = null,
    val batteryPercent: Int? = null,
    /** Filled only by manufacturer integrations that can really read them. */
    val leftBatteryPercent: Int? = null,
    val rightBatteryPercent: Int? = null,
    val caseBatteryPercent: Int? = null,
    /** Android audio route facts. These work for any Bluetooth audio device, whatever the brand. */
    val audioOutputActive: Boolean? = null,
    val audioOutputType: String? = null,
    val audioChannels: List<Int> = emptyList(),
    val audioSampleRates: List<Int> = emptyList(),
    /** Only a manufacturer integration can read this. */
    val activeSoundMode: String? = null,
) {
    /** Keeps this value where it has one and fills the gaps from [other]. */
    fun mergedWith(other: DeviceInfoDetails): DeviceInfoDetails = DeviceInfoDetails(
        manufacturerName = manufacturerName ?: other.manufacturerName,
        modelNumber = modelNumber ?: other.modelNumber,
        firmwareRevision = firmwareRevision ?: other.firmwareRevision,
        hardwareRevision = hardwareRevision ?: other.hardwareRevision,
        softwareRevision = softwareRevision ?: other.softwareRevision,
        batteryPercent = batteryPercent ?: other.batteryPercent,
        leftBatteryPercent = leftBatteryPercent ?: other.leftBatteryPercent,
        rightBatteryPercent = rightBatteryPercent ?: other.rightBatteryPercent,
        caseBatteryPercent = caseBatteryPercent ?: other.caseBatteryPercent,
        audioOutputActive = audioOutputActive ?: other.audioOutputActive,
        audioOutputType = audioOutputType ?: other.audioOutputType,
        audioChannels = audioChannels.ifEmpty { other.audioChannels },
        audioSampleRates = audioSampleRates.ifEmpty { other.audioSampleRates },
        activeSoundMode = activeSoundMode ?: other.activeSoundMode,
    )
}

data class CapabilityReport(
    val device: DiscoveredDevice,
    val features: List<FeatureStatus>,
    val info: DeviceInfoDetails?,
    val limitations: List<String>,
) {
    fun status(feature: FeatureId): FeatureStatus = features.first { it.feature == feature }
}

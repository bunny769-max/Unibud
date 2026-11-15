package com.unibud.app.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.unibud.app.core.audio.EqCodec
import com.unibud.app.core.audio.EqPreset
import com.unibud.app.core.model.Confidence
import com.unibud.app.core.model.DeviceKind
import com.unibud.app.core.model.SavedDevice

@Entity(tableName = "device_profiles")
data class DeviceProfileEntity(
    @PrimaryKey val address: String,
    val systemName: String?,
    val customName: String?,
    val brandId: String?,
    val brandName: String?,
    val kind: String,
    val confidence: String,
    val modelNumber: String?,
    val firmwareRevision: String?,
    val firstSeenAt: Long,
    val lastSeenAt: Long,
)

@Entity(
    tableName = "device_features",
    primaryKeys = ["address", "feature"],
    foreignKeys = [
        ForeignKey(
            entity = DeviceProfileEntity::class,
            parentColumns = ["address"],
            childColumns = ["address"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("address")],
)
data class DeviceFeatureEntity(
    val address: String,
    val feature: String,
    val level: String,
    val source: String,
    val reason: String,
)

@Entity(tableName = "eq_presets")
data class EqPresetEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    /** Comma separated band gains, see [EqCodec]. */
    val gains: String,
    val createdAt: Long,
    val updatedAt: Long,
)

@Entity(tableName = "settings")
data class SettingEntity(
    @PrimaryKey val settingKey: String,
    val settingValue: String,
)

fun DeviceProfileEntity.toDomain() = SavedDevice(
    address = address,
    systemName = systemName,
    customName = customName,
    brandName = brandName,
    kind = DeviceKind.entries.firstOrNull { it.name == kind } ?: DeviceKind.UNKNOWN,
    confidence = Confidence.entries.firstOrNull { it.name == confidence } ?: Confidence.LOW,
    modelNumber = modelNumber,
    firmwareRevision = firmwareRevision,
    lastSeenAt = lastSeenAt,
)

fun EqPresetEntity.toDomain(): EqPreset? =
    EqCodec.decode(gains)?.let { EqPreset(id = id, name = name, gainsDb = it) }

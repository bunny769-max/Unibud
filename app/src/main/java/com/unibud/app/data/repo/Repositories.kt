package com.unibud.app.data.repo

import androidx.room.withTransaction
import com.unibud.app.core.audio.EqCodec
import com.unibud.app.core.audio.EqPreset
import com.unibud.app.core.audio.EqPresetRules
import com.unibud.app.core.model.AppSettings
import com.unibud.app.core.model.CapabilityReport
import com.unibud.app.core.model.SavedDevice
import com.unibud.app.core.model.ThemeMode
import com.unibud.app.data.local.AppDatabase
import com.unibud.app.data.local.DeviceFeatureEntity
import com.unibud.app.data.local.DeviceProfileEntity
import com.unibud.app.data.local.EqPresetEntity
import com.unibud.app.data.local.SettingEntity
import com.unibud.app.data.local.toDomain
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

/** Remembers devices and what they were found to support. Stores no pairing keys or raw Bluetooth data. */
class DeviceRepository(private val db: AppDatabase) {
    private val dao = db.deviceDao()

    fun observeSaved(): Flow<List<SavedDevice>> = dao.observeProfiles().map { list -> list.map { it.toDomain() } }

    /** Saves the device and its latest capability findings. Only call this for audio devices the user chose to inspect. */
    suspend fun saveReport(report: CapabilityReport, now: Long = System.currentTimeMillis()) {
        val d = report.device
        db.withTransaction {
            val existing = dao.getProfile(d.address)
            dao.upsertProfile(
                DeviceProfileEntity(
                    address = d.address,
                    systemName = d.name ?: existing?.systemName,
                    customName = existing?.customName,
                    brandId = d.brand?.id,
                    brandName = d.brand?.displayName,
                    kind = d.classification.kind.name,
                    confidence = d.classification.confidence.name,
                    modelNumber = report.info?.modelNumber ?: existing?.modelNumber,
                    firmwareRevision = report.info?.firmwareRevision ?: existing?.firmwareRevision,
                    firstSeenAt = existing?.firstSeenAt ?: now,
                    lastSeenAt = now,
                ),
            )
            dao.clearFeatures(d.address)
            dao.upsertFeatures(
                report.features.map {
                    DeviceFeatureEntity(d.address, it.feature.name, it.level.name, it.source.name, it.reason)
                },
            )
        }
    }

    suspend fun rename(address: String, name: String?) =
        dao.setCustomName(address, name?.trim()?.takeIf { it.isNotEmpty() })

    suspend fun forget(address: String) = dao.deleteProfile(address)
}

class PresetRepository(db: AppDatabase) {
    private val dao = db.presetDao()

    fun observeCustom(): Flow<List<EqPreset>> =
        dao.observeAll().map { list -> list.mapNotNull { it.toDomain() } }

    /** Returns false if the name is not usable. */
    suspend fun save(id: Long?, rawName: String, gainsDb: List<Float>, now: Long = System.currentTimeMillis()): Boolean {
        val name = EqPresetRules.cleanName(rawName) ?: return false
        val encoded = EqCodec.encode(gainsDb)
        if (id == null || id <= 0) {
            dao.insert(EqPresetEntity(name = name, gains = encoded, createdAt = now, updatedAt = now))
        } else {
            dao.update(id, name, encoded, now)
        }
        return true
    }

    suspend fun delete(id: Long) = dao.delete(id)
}

class SettingsRepository(db: AppDatabase) {
    private val dao = db.settingDao()

    fun observe(): Flow<AppSettings> = combine(
        dao.observe(THEME), dao.observe(ONBOARDED), dao.observe(LAST_SYNC),
    ) { theme, onboarded, lastSync ->
        AppSettings(ThemeMode.parse(theme), onboarded == "true", lastSync?.toLongOrNull())
    }

    suspend fun setTheme(mode: ThemeMode, now: Long = System.currentTimeMillis()) {
        dao.put(SettingEntity(THEME, mode.name))
        dao.put(SettingEntity(THEME_UPDATED, now.toString()))
    }

    suspend fun setOnboarded(done: Boolean) = dao.put(SettingEntity(ONBOARDED, done.toString()))

    /** A brand the user typed in for a device, kept on this phone only. */
    suspend fun brandOverride(address: String): String? = dao.get(brandKey(address))

    suspend fun setBrandOverride(address: String, brand: String?) =
        dao.put(SettingEntity(brandKey(address), brand?.trim().orEmpty()))

    private fun brandKey(address: String) = "brand_override:" + address.lowercase()

    companion object {
        const val THEME = "theme_mode"
        const val THEME_UPDATED = "theme_updated_at"
        const val ONBOARDED = "onboarded"
        const val LAST_SYNC = "last_synced_at"
    }
}

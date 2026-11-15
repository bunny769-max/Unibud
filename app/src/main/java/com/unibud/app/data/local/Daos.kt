package com.unibud.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface DeviceDao {
    @Query("SELECT * FROM device_profiles ORDER BY lastSeenAt DESC")
    fun observeProfiles(): Flow<List<DeviceProfileEntity>>

    @Query("SELECT * FROM device_profiles WHERE address = :address")
    suspend fun getProfile(address: String): DeviceProfileEntity?

    @Upsert
    suspend fun upsertProfile(profile: DeviceProfileEntity)

    @Query("UPDATE device_profiles SET customName = :name WHERE address = :address")
    suspend fun setCustomName(address: String, name: String?)

    @Query("DELETE FROM device_profiles WHERE address = :address")
    suspend fun deleteProfile(address: String)

    @Query("DELETE FROM device_features WHERE address = :address")
    suspend fun clearFeatures(address: String)

    @Upsert
    suspend fun upsertFeatures(features: List<DeviceFeatureEntity>)
}

@Dao
interface PresetDao {
    @Query("SELECT * FROM eq_presets ORDER BY updatedAt DESC")
    fun observeAll(): Flow<List<EqPresetEntity>>

    @Insert
    suspend fun insert(preset: EqPresetEntity): Long

    @Query("UPDATE eq_presets SET name = :name, gains = :gains, updatedAt = :updatedAt WHERE id = :id")
    suspend fun update(id: Long, name: String, gains: String, updatedAt: Long)

    @Query("DELETE FROM eq_presets WHERE id = :id")
    suspend fun delete(id: Long)
}

@Dao
interface SettingDao {
    @Query("SELECT settingValue FROM settings WHERE settingKey = :key")
    fun observe(key: String): Flow<String?>

    @Upsert
    suspend fun put(setting: SettingEntity)
}

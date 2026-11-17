package com.unibud.app.data.cloud

import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.unibud.app.core.audio.EqCodec
import com.unibud.app.data.local.AppDatabase
import com.unibud.app.data.local.EqPresetEntity
import com.unibud.app.data.local.SettingEntity
import com.unibud.app.data.repo.SettingsRepository
import kotlinx.coroutines.tasks.await
import java.security.MessageDigest

/**
 * Syncs equalizer presets, device names and the theme under users/{uid}. Last write wins.
 *
 * Privacy: Bluetooth addresses never leave the phone. A device is stored under a one-way hash of
 * (uid + address), and pairing keys or raw Bluetooth data are never read, let alone uploaded.
 */
class CloudSyncRepository(private val db: AppDatabase) {
    private val firestore get() = FirebaseFirestore.getInstance()

    suspend fun sync(uid: String, now: Long = System.currentTimeMillis()) {
        val root = firestore.collection("users").document(uid)
        syncPresets(root)
        syncDevices(root, uid)
        syncTheme(root, now)
        db.settingDao().put(SettingEntity(SettingsRepository.LAST_SYNC, now.toString()))
    }

    suspend fun deletePreset(uid: String, name: String) {
        firestore.collection("users").document(uid).collection("presets").document(presetDocId(name)).delete().await()
    }

    private suspend fun syncPresets(root: DocumentReference) {
        val dao = db.presetDao()
        val local = dao.getAll()
        for (doc in root.collection("presets").get().await().documents) {
            val name = doc.getString("name") ?: continue
            val gains = doc.getString("gains") ?: continue
            if (EqCodec.decode(gains) == null) continue
            val updated = doc.getLong("updatedAt") ?: 0L
            val match = local.firstOrNull { it.name.equals(name, ignoreCase = true) }
            if (match == null) {
                dao.insert(EqPresetEntity(name = name, gains = gains, createdAt = updated, updatedAt = updated))
            } else if (updated > match.updatedAt) {
                dao.update(match.id, name, gains, updated)
            }
        }
        val batch = firestore.batch()
        for (p in dao.getAll()) {
            val data = mapOf<String, Any?>("name" to p.name, "gains" to p.gains, "updatedAt" to p.updatedAt)
            batch.set(root.collection("presets").document(presetDocId(p.name)), data, SetOptions.merge())
        }
        batch.commit().await()
    }

    private suspend fun syncDevices(root: DocumentReference, uid: String) {
        val dao = db.deviceDao()
        val remote = root.collection("devices").get().await().documents.associateBy { it.id }
        val batch = firestore.batch()
        for (p in dao.getAllProfiles()) {
            val docId = deviceDocId(uid, p.address)
            var customName = p.customName
            if (customName == null) {
                customName = remote[docId]?.getString("customName")
                if (customName != null) dao.setCustomName(p.address, customName)
            }
            val data = mapOf<String, Any?>(
                "customName" to customName,
                "brand" to p.brandName,
                "kind" to p.kind,
                "model" to p.modelNumber,
                "firmware" to p.firmwareRevision,
                "lastSeenAt" to p.lastSeenAt,
            )
            batch.set(root.collection("devices").document(docId), data, SetOptions.merge())
        }
        batch.commit().await()
    }

    private suspend fun syncTheme(root: DocumentReference, now: Long) {
        val dao = db.settingDao()
        val ref = root.collection("settings").document("app")
        val remote = ref.get().await()
        val localTheme = dao.get(SettingsRepository.THEME)
        val localUpdated = dao.get(SettingsRepository.THEME_UPDATED)?.toLongOrNull() ?: 0L
        val remoteTheme = remote.getString("themeMode")
        val remoteUpdated = remote.getLong("updatedAt") ?: 0L
        if (remoteTheme != null && remoteUpdated > localUpdated) {
            dao.put(SettingEntity(SettingsRepository.THEME, remoteTheme))
            dao.put(SettingEntity(SettingsRepository.THEME_UPDATED, remoteUpdated.toString()))
        } else if (localTheme != null) {
            val stamp = if (localUpdated > 0) localUpdated else now
            ref.set(mapOf<String, Any?>("themeMode" to localTheme, "updatedAt" to stamp), SetOptions.merge()).await()
        }
    }

    private fun presetDocId(name: String) = sha256(name.trim().lowercase()).take(24)

    private fun deviceDocId(uid: String, address: String) = sha256("$uid:${address.lowercase()}").take(24)

    private fun sha256(text: String): String =
        MessageDigest.getInstance("SHA-256").digest(text.toByteArray()).joinToString("") { "%02x".format(it) }
}

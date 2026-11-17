package com.unibud.app.core.model

enum class ThemeMode {
    SYSTEM, LIGHT, DARK;

    companion object {
        fun parse(value: String?): ThemeMode = entries.firstOrNull { it.name == value } ?: SYSTEM
    }
}

data class AppSettings(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val onboarded: Boolean = false,
    val lastSyncedAt: Long? = null,
)

/** A device the user has connected before, as remembered on this phone. */
data class SavedDevice(
    val address: String,
    val systemName: String?,
    val customName: String?,
    val brandName: String?,
    val kind: DeviceKind,
    val confidence: Confidence,
    val modelNumber: String?,
    val firmwareRevision: String?,
    val lastSeenAt: Long,
) {
    val displayName: String get() = customName?.takeIf { it.isNotBlank() } ?: systemName ?: address
}

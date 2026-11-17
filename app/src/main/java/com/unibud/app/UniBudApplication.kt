package com.unibud.app

import android.app.Application
import com.unibud.app.data.cloud.AuthRepository
import com.unibud.app.data.cloud.CloudSyncRepository
import com.unibud.app.data.local.AppDatabase
import com.unibud.app.data.repo.DeviceRepository
import com.unibud.app.data.repo.PresetRepository
import com.unibud.app.data.repo.SettingsRepository

/** Manual dependency container. Small on purpose; swap for Hilt later if the app grows. */
class AppContainer(app: Application) {
    private val database: AppDatabase by lazy { AppDatabase.create(app) }
    val devices: DeviceRepository by lazy { DeviceRepository(database) }
    val presets: PresetRepository by lazy { PresetRepository(database) }
    val settings: SettingsRepository by lazy { SettingsRepository(database) }
    val auth: AuthRepository by lazy { AuthRepository(app) }
    val cloud: CloudSyncRepository by lazy { CloudSyncRepository(database) }
}

class UniBudApplication : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}

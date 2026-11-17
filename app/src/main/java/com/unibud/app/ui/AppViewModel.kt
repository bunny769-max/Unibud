package com.unibud.app.ui

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.unibud.app.UniBudApplication
import com.unibud.app.core.bluetooth.BluetoothActions
import com.unibud.app.core.bluetooth.BluetoothPermissions
import com.unibud.app.core.bluetooth.BluetoothScanner
import com.unibud.app.core.bluetooth.ConnectionMonitor
import com.unibud.app.core.bluetooth.GattProbe
import com.unibud.app.core.capability.CapabilityDiscovery
import com.unibud.app.core.capability.ControlCommand
import com.unibud.app.core.capability.ControlGateway
import com.unibud.app.core.capability.ControlResult
import com.unibud.app.core.capability.MediaKey
import com.unibud.app.core.integration.AndroidAudioController
import com.unibud.app.core.integration.AndroidStandardIntegration
import com.unibud.app.core.integration.AudioRouteInspector
import com.unibud.app.core.integration.AudioRouteIntegration
import com.unibud.app.core.integration.GattStandardIntegration
import com.unibud.app.core.model.AppSettings
import com.unibud.app.core.model.CapabilityReport
import com.unibud.app.core.model.DeviceKind
import com.unibud.app.core.model.DiscoveredDevice
import com.unibud.app.core.model.ThemeMode
import com.unibud.app.core.registry.BrandResolver
import com.unibud.app.core.registry.DeviceListPolicy
import com.unibud.app.data.cloud.AuthUser
import com.unibud.app.data.cloud.SignInResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.coroutines.cancellation.CancellationException

data class AppUiState(
    val permissionGranted: Boolean = false,
    val scanning: Boolean = false,
    val scanResults: List<DiscoveredDevice> = emptyList(),
    val bonded: List<DiscoveredDevice> = emptyList(),
    val connected: Set<String> = emptySet(),
    val selected: DiscoveredDevice? = null,
    val report: CapabilityReport? = null,
    val discovering: Boolean = false,
    val volumePercent: Int = 0,
    val signingIn: Boolean = false,
    val syncing: Boolean = false,
) {
    val selectedConnected: Boolean get() = selected != null && selected.address in connected

    /** Fresh scan results first, then paired devices that were not seen in the scan. */
    val visibleDevices: List<DiscoveredDevice>
        get() = (scanResults + bonded).distinctBy { it.address }.filter(DeviceListPolicy::isRelevant)
}

class AppViewModel(private val app: Application) : AndroidViewModel(app) {
    private val container = (app as UniBudApplication).container
    private val scanner = BluetoothScanner(app)
    private val monitor = ConnectionMonitor(app)
    private val audio = AudioRouteInspector(app)
    private val discovery = CapabilityDiscovery(
        listOf(AndroidStandardIntegration(), AudioRouteIntegration(app), GattStandardIntegration(GattProbe(app))),
    )

    // The only controller today talks to Android's audio system. Brand-specific controllers will be added here.
    private val gateway = ControlGateway(listOf(AndroidAudioController(app)))

    private val _state = MutableStateFlow(
        AppUiState(permissionGranted = BluetoothPermissions.hasAll(app), volumePercent = audio.volumePercent()),
    )
    val state: StateFlow<AppUiState> = _state.asStateFlow()

    private val _events = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val events: SharedFlow<String> = _events.asSharedFlow()

    val authConfigured: Boolean get() = container.auth.isConfigured

    val settings: StateFlow<AppSettings?> =
        container.settings.observe().stateIn(viewModelScope, SharingStarted.Eagerly, null)
    val authUser: StateFlow<AuthUser?> =
        container.auth.user.stateIn(viewModelScope, SharingStarted.Eagerly, null)
    val savedDevices = container.devices.observeSaved()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val customPresets = container.presets.observeCustom()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private var scanJob: Job? = null
    private var monitorJob: Job? = null
    private var discoverJob: Job? = null

    init {
        if (_state.value.permissionGranted) onPermissionChanged(true)
    }

    // ------------------------------------------------------------------ permissions and connection

    fun onPermissionChanged(granted: Boolean) {
        _state.update { it.copy(permissionGranted = granted) }
        if (granted) {
            startMonitoring()
            refreshBonded()
        }
    }

    /** Called when the app returns to the foreground, for example from Android's pairing dialog or settings. */
    fun onResume() {
        _state.update { it.copy(volumePercent = audio.volumePercent()) }
        val granted = BluetoothPermissions.hasAll(app)
        if (granted != _state.value.permissionGranted) onPermissionChanged(granted) else if (granted) refreshBonded()
    }

    private fun startMonitoring() {
        if (monitorJob?.isActive == true) return
        monitorJob = viewModelScope.launch {
            monitor.connectedAddresses().collect { connected ->
                val before = _state.value
                val selected = before.selected
                val wasConnected = selected != null && selected.address in before.connected
                _state.update { it.copy(connected = connected) }
                val isConnected = selected != null && selected.address in connected
                // Picking a new device wins. Otherwise re-read the current one when its connection changed.
                if (!autoSelect() && selected != null && wasConnected != isConnected) refreshReport()
            }
        }
    }

    fun refreshBonded() {
        if (!BluetoothPermissions.hasAll(app)) return
        // Reading paired devices is a binder call, so keep it off the main thread.
        viewModelScope.launch(Dispatchers.Default) {
            val bonded = scanner.bonded().filter(DeviceListPolicy::isRelevant)
            _state.update { it.copy(bonded = bonded) }
            autoSelect()
        }
    }

    /** When a paired audio device connects, identify it automatically. Returns true if it picked a new device. */
    private fun autoSelect(): Boolean {
        val s = _state.value
        val current = s.selected
        if (current != null && current.address in s.connected) return false
        val candidate = (s.bonded + s.scanResults).distinctBy { it.address }
            .filter { it.address in s.connected && DeviceListPolicy.isRelevant(it) }
            .minByOrNull { if (it.classification.kind == DeviceKind.TWS_EARBUDS) 0 else 1 }
        if (candidate == null) return false
        select(candidate)
        return true
    }

    fun scan() {
        scanJob?.cancel()
        _state.update { it.copy(scanning = true) }
        scanJob = viewModelScope.launch {
            try {
                scanner.scan().collect { list ->
                    _state.update { s -> s.copy(scanResults = list.filter(DeviceListPolicy::isRelevant)) }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _events.emit(e.message ?: "The scan failed.")
            } finally {
                _state.update { it.copy(scanning = false) }
            }
        }
    }

    fun select(device: DiscoveredDevice) {
        discoverJob?.cancel()
        _state.update { it.copy(selected = device, report = null, discovering = true) }
        discoverJob = viewModelScope.launch {
            try {
                val override = container.settings.brandOverride(device.address)
                val brand = BrandResolver.resolve(device.name, device.manufacturerIds, userBrand = override) ?: device.brand
                val live = device.copy(brand = brand, isConnected = device.address in _state.value.connected)
                val report = discovery.discover(live)
                _state.update { it.copy(selected = report.device, report = report, discovering = false, volumePercent = audio.volumePercent()) }
                container.devices.saveReport(report)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.update { it.copy(discovering = false) }
                _events.emit("Could not read this device: ${e.message ?: "unknown error"}")
            }
        }
    }

    fun refreshReport() {
        _state.value.selected?.let(::select)
    }

    /** Lets the user name the brand when it cannot be detected. Empty text clears the override. */
    fun setBrand(address: String, brand: String) {
        viewModelScope.launch {
            container.settings.setBrandOverride(address, brand)
            if (_state.value.selected?.address == address) refreshReport()
        }
    }

    fun pair(device: DiscoveredDevice) {
        val started = BluetoothActions.startPairing(app, device.address)
        _events.tryEmit(
            if (started) "Pairing started. Confirm on the Android pairing dialog."
            else "Pairing could not start. Put the earbuds in pairing mode and check Bluetooth permission.",
        )
    }

    // ------------------------------------------------------------------ controls

    fun execute(command: ControlCommand, quiet: Boolean = false) {
        val device = _state.value.selected ?: return
        viewModelScope.launch {
            when (val result = gateway.execute(device, command)) {
                is ControlResult.Success -> {
                    _state.update { it.copy(volumePercent = audio.volumePercent()) }
                    if (!quiet) _events.emit("Done. Confirmed by ${result.confirmedBy}.")
                }
                is ControlResult.NotSupported -> _events.emit(result.reason)
                is ControlResult.Failed -> _events.emit("That did not work: ${result.reason}")
            }
        }
    }

    fun setVolume(percent: Int) {
        _state.update { it.copy(volumePercent = percent) }
        execute(ControlCommand.SetVolume(percent), quiet = true)
    }

    fun media(key: MediaKey) = execute(ControlCommand.MediaAction(key), quiet = true)

    // ------------------------------------------------------------------ data

    fun rename(address: String, name: String) {
        viewModelScope.launch {
            container.devices.rename(address, name)
            autoSync()
        }
    }

    fun forget(address: String) {
        viewModelScope.launch {
            container.devices.forget(address)
            _events.emit("Removed from this phone. Android pairing is unchanged.")
        }
    }

    fun savePreset(id: Long?, name: String, gainsDb: List<Float>) {
        viewModelScope.launch {
            val saved = container.presets.save(id, name, gainsDb)
            _events.emit(if (saved) "Preset saved." else "Preset names need 1 to 30 characters.")
            if (saved) autoSync()
        }
    }

    fun deletePreset(id: Long) {
        viewModelScope.launch {
            val name = customPresets.value.firstOrNull { it.id == id }?.name
            container.presets.delete(id)
            val uid = authUser.value?.uid
            if (name != null && uid != null) {
                try {
                    container.cloud.deletePreset(uid, name)
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    // Offline: the preset may reappear on the next sync.
                }
            }
        }
    }

    fun setTheme(mode: ThemeMode) {
        viewModelScope.launch {
            container.settings.setTheme(mode)
            autoSync()
        }
    }

    fun completeOnboarding() {
        viewModelScope.launch { container.settings.setOnboarded(true) }
    }

    // ------------------------------------------------------------------ account and sync

    /** [activityContext] must be an Activity, because Google shows its account chooser on top of it. */
    fun signIn(activityContext: Context, onSuccess: () -> Unit = {}) {
        if (_state.value.signingIn) return
        _state.update { it.copy(signingIn = true) }
        viewModelScope.launch {
            try {
                when (val result = container.auth.signIn(activityContext)) {
                    is SignInResult.Success -> {
                        _events.emit("Signed in as ${result.user.email ?: result.user.name ?: "your Google account"}.")
                        onSuccess()
                        syncNow()
                    }
                    SignInResult.NotConfigured -> _events.emit(
                        "Google sign-in needs this build to include google-services.json. See docs/FIREBASE_SETUP.md.",
                    )
                    SignInResult.Cancelled -> Unit
                    is SignInResult.Failed -> _events.emit(result.message)
                }
            } finally {
                _state.update { it.copy(signingIn = false) }
            }
        }
    }

    fun signOut(activityContext: Context) {
        viewModelScope.launch {
            container.auth.signOut(activityContext)
            _events.emit("Signed out. Your data stays on this phone.")
        }
    }

    fun syncNow() {
        val uid = authUser.value?.uid
        if (uid == null) {
            _events.tryEmit("Sign in with Google to sync.")
            return
        }
        if (_state.value.syncing) return
        _state.update { it.copy(syncing = true) }
        viewModelScope.launch {
            try {
                container.cloud.sync(uid)
                _events.emit("Synced.")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _events.emit("Could not sync. Check your connection and try again.")
            } finally {
                _state.update { it.copy(syncing = false) }
            }
        }
    }

    /** Quiet background sync after a local change, only when signed in. */
    private fun autoSync() {
        val uid = authUser.value?.uid ?: return
        viewModelScope.launch {
            try {
                container.cloud.sync(uid)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                // Stays local until the next successful sync.
            }
        }
    }
}

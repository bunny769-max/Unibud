package com.unibud.app.ui

import android.app.Application
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
import com.unibud.app.core.integration.AndroidStandardIntegration
import com.unibud.app.core.integration.GattStandardIntegration
import com.unibud.app.core.model.AppSettings
import com.unibud.app.core.model.CapabilityReport
import com.unibud.app.core.model.DeviceKind
import com.unibud.app.core.model.DiscoveredDevice
import com.unibud.app.core.model.ThemeMode
import com.unibud.app.core.registry.DeviceListPolicy
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
    private val discovery = CapabilityDiscovery(
        listOf(AndroidStandardIntegration(), GattStandardIntegration(GattProbe(app))),
    )

    // No manufacturer controller is registered yet, so every control request honestly returns "not supported".
    private val gateway = ControlGateway()

    private val _state = MutableStateFlow(AppUiState(permissionGranted = BluetoothPermissions.hasAll(app)))
    val state: StateFlow<AppUiState> = _state.asStateFlow()

    private val _events = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val events: SharedFlow<String> = _events.asSharedFlow()

    val settings: StateFlow<AppSettings?> =
        container.settings.observe().stateIn(viewModelScope, SharingStarted.Eagerly, null)
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

    fun onPermissionChanged(granted: Boolean) {
        _state.update { it.copy(permissionGranted = granted) }
        if (granted) {
            startMonitoring()
            refreshBonded()
        }
    }

    /** Called when the app returns to the foreground, for example from Android's pairing dialog or settings. */
    fun onResume() {
        val granted = BluetoothPermissions.hasAll(app)
        if (granted != _state.value.permissionGranted) onPermissionChanged(granted) else if (granted) refreshBonded()
    }

    private fun startMonitoring() {
        if (monitorJob?.isActive == true) return
        monitorJob = viewModelScope.launch {
            monitor.connectedAddresses().collect { connected ->
                _state.update { it.copy(connected = connected) }
                autoSelect()
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

    /** When a paired audio device connects, identify it automatically. */
    private fun autoSelect() {
        val s = _state.value
        val current = s.selected
        if (current != null && current.address in s.connected) return
        val candidate = (s.bonded + s.scanResults).distinctBy { it.address }
            .filter { it.address in s.connected && DeviceListPolicy.isRelevant(it) }
            .minByOrNull { if (it.classification.kind == DeviceKind.TWS_EARBUDS) 0 else 1 }
        if (candidate != null) select(candidate)
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
                val live = device.copy(isConnected = device.address in _state.value.connected)
                val report = discovery.discover(live)
                _state.update { it.copy(report = report, discovering = false) }
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

    fun pair(device: DiscoveredDevice) {
        val started = BluetoothActions.startPairing(app, device.address)
        _events.tryEmit(
            if (started) {
                "Pairing started. Confirm on the Android pairing dialog."
            } else {
                "Pairing could not start. Put the earbuds in pairing mode and check Bluetooth permission."
            },
        )
    }

    fun execute(command: ControlCommand) {
        val device = _state.value.selected ?: return
        viewModelScope.launch {
            val message = when (val result = gateway.execute(device, command)) {
                is ControlResult.Success -> "Done. Confirmed by ${result.confirmedBy}."
                is ControlResult.NotSupported -> result.reason
                is ControlResult.Failed -> "That did not work: ${result.reason}"
            }
            _events.emit(message)
        }
    }

    fun rename(address: String, name: String) {
        viewModelScope.launch { container.devices.rename(address, name) }
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
        }
    }

    fun deletePreset(id: Long) {
        viewModelScope.launch { container.presets.delete(id) }
    }

    fun setTheme(mode: ThemeMode) {
        viewModelScope.launch { container.settings.setTheme(mode) }
    }

    fun completeOnboarding() {
        viewModelScope.launch { container.settings.setOnboarded(true) }
    }
}

package com.unibud.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.unibud.app.core.bluetooth.BluetoothActions
import com.unibud.app.core.model.DiscoveredDevice
import com.unibud.app.core.model.SavedDevice
import com.unibud.app.ui.AppUiState
import com.unibud.app.ui.brandLabel
import com.unibud.app.ui.components.GhostButton
import com.unibud.app.ui.components.GradientButton
import com.unibud.app.ui.components.NeoPanel
import com.unibud.app.ui.components.Pill
import com.unibud.app.ui.components.RadarScan
import com.unibud.app.ui.components.SectionLabel
import com.unibud.app.ui.kindLabel
import com.unibud.app.ui.relativeTime
import com.unibud.app.ui.signalLabel
import com.unibud.app.ui.theme.Neo

@Composable
fun DevicesScreen(
    state: AppUiState,
    saved: List<SavedDevice>,
    onScan: () -> Unit,
    onRequestPermission: () -> Unit,
    onPair: (DiscoveredDevice) -> Unit,
    onInspect: (DiscoveredDevice) -> Unit,
) {
    val context = LocalContext.current
    val c = Neo.colors

    Column(
        Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        NeoPanel(Modifier.fillMaxWidth(), corner = 28.dp, glow = state.scanning) {
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                RadarScan(active = state.scanning)
                Spacer(Modifier.height(6.dp))
                Text(if (state.scanning) "Looking for audio devices" else "Ready to scan", style = MaterialTheme.typography.titleLarge, color = c.text)
                Spacer(Modifier.height(4.dp))
                Text(
                    "Put your earbuds in pairing mode to find new ones. A scan stops by itself after 12 seconds.",
                    style = MaterialTheme.typography.bodySmall, color = c.textDim,
                )
                Spacer(Modifier.height(14.dp))
                if (state.permissionGranted) {
                    GradientButton(if (state.scanning) "Scanning" else "Scan for devices", onScan, enabled = !state.scanning)
                } else {
                    GradientButton("Allow Bluetooth access", onRequestPermission)
                }
            }
        }

        SectionLabel("Nearby and paired")
        val devices = remember(state.scanResults, state.bonded) { state.visibleDevices }
        if (devices.isEmpty()) {
            Text(
                if (state.permissionGranted) "No audio devices yet. Scan to look nearby." else "Allow Bluetooth access to see devices.",
                style = MaterialTheme.typography.bodyMedium, color = c.textDim,
            )
        }
        devices.forEach { device ->
            DeviceCard(
                device = device,
                connected = device.address in state.connected,
                onPair = { onPair(device) },
                onInspect = { onInspect(device) },
                onSettings = { BluetoothActions.openBluetoothSettings(context) },
            )
        }

        val shown = devices.map { it.address }.toSet()
        val others = saved.filter { it.address !in shown }
        if (others.isNotEmpty()) {
            SectionLabel("Used before on this phone")
            Text(
                "Android reconnects audio devices itself. Reconnect opens Bluetooth settings.",
                style = MaterialTheme.typography.bodySmall, color = c.textDim,
            )
            others.forEach { device ->
                NeoPanel(Modifier.fillMaxWidth()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(device.displayName, style = MaterialTheme.typography.titleMedium, color = c.text)
                            Text("Last seen " + relativeTime(device.lastSeenAt), style = MaterialTheme.typography.bodySmall, color = c.textDim)
                        }
                        GhostButton("Reconnect", { BluetoothActions.openBluetoothSettings(context) })
                    }
                }
            }
        }
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun DeviceCard(
    device: DiscoveredDevice,
    connected: Boolean,
    onPair: () -> Unit,
    onInspect: () -> Unit,
    onSettings: () -> Unit,
) {
    val c = Neo.colors
    NeoPanel(Modifier.fillMaxWidth(), glow = connected) {
        Row(verticalAlignment = Alignment.Top) {
            Column(Modifier.weight(1f)) {
                Text(device.name ?: "Unnamed device", style = MaterialTheme.typography.titleMedium, color = c.text)
                Spacer(Modifier.height(2.dp))
                Text(
                    listOfNotNull(kindLabel(device.classification), brandLabel(device.brand)).joinToString(", "),
                    style = MaterialTheme.typography.bodySmall, color = c.textDim,
                )
                signalLabel(device.rssi)?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = c.textDim) }
            }
            when {
                connected -> Pill("Connected", c.green)
                device.isBonded -> Pill("Paired", c.violet)
                else -> Pill("Nearby", c.textDim)
            }
        }
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            if (!device.isBonded) {
                GradientButton("Pair", onPair)
            } else {
                GradientButton("See details", onInspect)
                GhostButton(if (connected) "Disconnect" else "Connect", onSettings)
            }
        }
        if (device.isBonded) {
            Text(
                "Android connects and disconnects audio, so Connect and Disconnect open Bluetooth settings.",
                style = MaterialTheme.typography.labelSmall, color = c.textDim, modifier = Modifier.padding(top = 6.dp),
            )
        }
    }
}

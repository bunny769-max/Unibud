package com.unibud.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.unibud.app.core.bluetooth.BluetoothActions
import com.unibud.app.core.model.DiscoveredDevice
import com.unibud.app.core.model.SavedDevice
import com.unibud.app.core.model.SupportLevel
import com.unibud.app.ui.AppUiState
import com.unibud.app.ui.components.GlassCard
import com.unibud.app.ui.components.RadarScan
import com.unibud.app.ui.components.SectionTitle
import com.unibud.app.ui.components.StatusPill
import com.unibud.app.ui.kindLabel
import com.unibud.app.ui.relativeTime
import com.unibud.app.ui.signalLabel
import com.unibud.app.ui.theme.LocalIsDark
import com.unibud.app.ui.theme.supportColor

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
    val muted = MaterialTheme.colorScheme.onSurfaceVariant

    Column(
        Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        GlassCard(Modifier.fillMaxWidth(), corner = 32.dp) {
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                RadarScan(active = state.scanning)
                Spacer(Modifier.height(8.dp))
                Text(
                    if (state.scanning) "Looking for audio devices" else "Ready to scan",
                    style = MaterialTheme.typography.titleLarge,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "Put your earbuds in pairing mode to find new ones. Scans stop on their own after 12 seconds.",
                    style = MaterialTheme.typography.bodySmall,
                    color = muted,
                )
                Spacer(Modifier.height(14.dp))
                if (state.permissionGranted) {
                    Button(onClick = onScan, enabled = !state.scanning) {
                        Text(if (state.scanning) "Scanning" else "Scan for devices")
                    }
                } else {
                    Button(onClick = onRequestPermission) { Text("Allow Bluetooth access") }
                }
            }
        }

        SectionTitle("Nearby and paired")
        val devices = state.visibleDevices
        if (devices.isEmpty()) {
            Text(
                if (state.permissionGranted) "No audio devices yet. Scan to look nearby." else "Allow Bluetooth access to see devices.",
                style = MaterialTheme.typography.bodyMedium,
                color = muted,
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
            SectionTitle("Used before on this phone")
            Text(
                "Android reconnects audio devices itself. Reconnect opens Bluetooth settings.",
                style = MaterialTheme.typography.bodySmall,
                color = muted,
            )
            others.forEach { device ->
                GlassCard(Modifier.fillMaxWidth()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(device.displayName, style = MaterialTheme.typography.titleMedium)
                            Text(
                                "Last seen " + relativeTime(device.lastSeenAt),
                                style = MaterialTheme.typography.bodySmall,
                                color = muted,
                            )
                        }
                        OutlinedButton(onClick = { BluetoothActions.openBluetoothSettings(context) }) { Text("Reconnect") }
                    }
                }
            }
        }
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
    val dark = LocalIsDark.current
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    GlassCard(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.Top) {
            Column(Modifier.weight(1f)) {
                Text(device.name ?: "Unnamed device", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(2.dp))
                Text(
                    listOfNotNull(kindLabel(device.classification), device.brand?.displayName).joinToString(", "),
                    style = MaterialTheme.typography.bodySmall,
                    color = muted,
                )
                signalLabel(device.rssi)?.let {
                    Text(it, style = MaterialTheme.typography.bodySmall, color = muted)
                }
            }
            when {
                connected -> StatusPill("Connected", supportColor(SupportLevel.SUPPORTED, dark))
                device.isBonded -> StatusPill("Paired", MaterialTheme.colorScheme.primary)
                else -> StatusPill("Nearby", supportColor(SupportLevel.UNAVAILABLE, dark))
            }
        }
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            if (!device.isBonded) {
                Button(onClick = onPair) { Text("Pair") }
            } else {
                Button(onClick = onInspect) { Text("See details") }
                TextButton(onClick = onSettings) { Text(if (connected) "Disconnect" else "Connect") }
            }
        }
        if (device.isBonded) {
            Text(
                "Android connects and disconnects audio, so this opens Bluetooth settings.",
                style = MaterialTheme.typography.labelSmall,
                color = muted,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
    }
}

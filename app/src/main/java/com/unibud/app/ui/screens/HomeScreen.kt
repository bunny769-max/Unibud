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
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.unibud.app.core.bluetooth.BluetoothActions
import com.unibud.app.core.capability.ControlCommand
import com.unibud.app.core.capability.SoundMode
import com.unibud.app.core.model.CapabilityReport
import com.unibud.app.core.model.FeatureId
import com.unibud.app.core.model.SupportLevel
import com.unibud.app.ui.AppUiState
import com.unibud.app.ui.components.BatteryGauge
import com.unibud.app.ui.components.GlassCard
import com.unibud.app.ui.components.StatusPill
import com.unibud.app.ui.kindLabel
import com.unibud.app.ui.theme.LocalIsDark
import com.unibud.app.ui.theme.supportColor

private data class ModeOption(val label: String, val mode: SoundMode)

private val modeOptions = listOf(
    ModeOption("Normal", SoundMode.NORMAL),
    ModeOption("Noise cancelling", SoundMode.ANC),
    ModeOption("Transparency", SoundMode.TRANSPARENCY),
    ModeOption("Adaptive", SoundMode.ADAPTIVE),
)

@Composable
fun HomeScreen(
    state: AppUiState,
    displayName: String?,
    onGoToDevices: () -> Unit,
    onRequestPermission: () -> Unit,
    onExecute: (ControlCommand) -> Unit,
) {
    val context = LocalContext.current
    val dark = LocalIsDark.current
    val selected = state.selected
    val report = state.report

    Column(
        Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        GlassCard(Modifier.fillMaxWidth(), corner = 32.dp) {
            when {
                !state.permissionGranted -> {
                    Text("Bluetooth access needed", style = MaterialTheme.typography.titleLarge)
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "UniBud needs this permission to find and read your earbuds.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(14.dp))
                    Button(onClick = onRequestPermission) { Text("Allow Bluetooth access") }
                }
                selected == null -> {
                    Text("No earbuds connected", style = MaterialTheme.typography.headlineMedium)
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "Connect your earbuds in Android's Bluetooth settings and they will show up here on their own.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(14.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = onGoToDevices) { Text("Find devices") }
                        OutlinedButton(onClick = { BluetoothActions.openBluetoothSettings(context) }) {
                            Text("Bluetooth settings")
                        }
                    }
                }
                else -> {
                    val connected = state.selectedConnected
                    Text(
                        displayName ?: selected.name ?: selected.address,
                        style = MaterialTheme.typography.headlineMedium,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        listOfNotNull(kindLabel(selected.classification), selected.brand?.displayName).joinToString(", "),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        StatusPill(
                            if (connected) "Connected" else "Not connected",
                            if (connected) supportColor(SupportLevel.SUPPORTED, dark) else supportColor(SupportLevel.UNAVAILABLE, dark),
                        )
                        if (selected.isBonded) StatusPill("Paired", MaterialTheme.colorScheme.primary)
                    }
                    if (state.discovering) {
                        Spacer(Modifier.height(14.dp))
                        LinearProgressIndicator(Modifier.fillMaxWidth())
                        Spacer(Modifier.height(6.dp))
                        Text("Checking what this device supports", style = MaterialTheme.typography.bodySmall)
                    }
                    if (!connected) {
                        TextButton(onClick = { BluetoothActions.openBluetoothSettings(context) }) {
                            Text("Open Bluetooth settings")
                        }
                    }
                }
            }
        }

        if (selected != null) {
            BatteryCard(report)
            SoundModeCard(report, onExecute)
        }
    }
}

@Composable
private fun BatteryCard(report: CapabilityReport?) {
    val info = report?.info
    val overall = info?.batteryPercent
    val perBud = info?.leftBatteryPercent != null || info?.rightBatteryPercent != null || info?.caseBatteryPercent != null
    GlassCard(Modifier.fillMaxWidth()) {
        Text("Battery", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(14.dp))
        if (!perBud && overall != null) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                BatteryGauge("Battery", overall)
            }
        } else {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                BatteryGauge("Left", info?.leftBatteryPercent)
                BatteryGauge("Right", info?.rightBatteryPercent)
                BatteryGauge("Case", info?.caseBatteryPercent)
            }
        }
        if (report != null && !perBud) {
            Spacer(Modifier.height(12.dp))
            Text(
                if (overall != null) {
                    "These earbuds share one battery value over standard Bluetooth. Left, right and case levels need a manufacturer integration."
                } else {
                    "These earbuds did not share battery levels over standard Bluetooth."
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun SoundModeCard(report: CapabilityReport?, onExecute: (ControlCommand) -> Unit) {
    fun enabled(option: ModeOption): Boolean {
        val level = report?.status(ControlCommand.SetSoundMode(option.mode).feature)?.level
        return level != null && level != SupportLevel.UNAVAILABLE
    }

    GlassCard(Modifier.fillMaxWidth()) {
        Text("Sound mode", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(4.dp))
        Text(
            when {
                report == null -> "Waiting for device details"
                modeOptions.any(::enabled) -> "Pick a mode. The current mode cannot be read back from this device."
                else -> report.status(FeatureId.ANC).reason
            },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(12.dp))
        modeOptions.chunked(2).forEach { rowOptions ->
            Row(Modifier.fillMaxWidth().padding(bottom = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                rowOptions.forEach { option ->
                    FilledTonalButton(
                        onClick = { onExecute(ControlCommand.SetSoundMode(option.mode)) },
                        enabled = enabled(option),
                        modifier = Modifier.weight(1f),
                    ) { Text(option.label) }
                }
            }
        }
    }
}

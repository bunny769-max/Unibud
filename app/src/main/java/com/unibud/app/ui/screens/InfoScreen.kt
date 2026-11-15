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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.unibud.app.core.model.SavedDevice
import com.unibud.app.core.model.SupportLevel
import com.unibud.app.ui.AppUiState
import com.unibud.app.ui.components.FeatureRow
import com.unibud.app.ui.components.GlassCard
import com.unibud.app.ui.components.SectionTitle
import com.unibud.app.ui.kindLabel

@Composable
fun InfoScreen(
    state: AppUiState,
    saved: SavedDevice?,
    onRename: (address: String, name: String) -> Unit,
    onForget: (address: String) -> Unit,
    onRefresh: () -> Unit,
) {
    val device = state.selected
    val report = state.report
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    var renaming by remember { mutableStateOf(false) }

    Column(
        Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        if (device == null) {
            GlassCard(Modifier.fillMaxWidth(), corner = 32.dp) {
                Text("No device selected", style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(6.dp))
                Text(
                    "Connect earbuds, or open a paired device from Devices, to see its details.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = muted,
                )
            }
            return@Column
        }

        val displayName = saved?.displayName ?: device.name ?: device.address

        GlassCard(Modifier.fillMaxWidth(), corner = 32.dp) {
            Text(displayName, style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.height(4.dp))
            Text(kindLabel(device.classification), style = MaterialTheme.typography.titleMedium)
            Text(device.classification.reason, style = MaterialTheme.typography.bodySmall, color = muted)
            Spacer(Modifier.height(12.dp))
            DetailLine("Brand", device.brand?.displayName ?: "Not recognised")
            DetailLine("Address", device.address)
            DetailLine("Paired", if (device.isBonded) "Yes" else "No")
            DetailLine("Connected", if (state.selectedConnected) "Yes" else "No")
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { renaming = true }) { Text("Rename") }
                OutlinedButton(onClick = onRefresh) { Text("Check again") }
            }
        }

        SectionTitle("From the device")
        GlassCard(Modifier.fillMaxWidth()) {
            val info = report?.info
            val lines = listOfNotNull(
                info?.manufacturerName?.let { "Manufacturer" to it },
                info?.modelNumber?.let { "Model" to it },
                info?.firmwareRevision?.let { "Firmware" to it },
                info?.hardwareRevision?.let { "Hardware" to it },
                info?.softwareRevision?.let { "Software" to it },
                info?.batteryPercent?.let { "Battery" to "$it%" },
            )
            when {
                state.discovering -> Text("Reading device details", style = MaterialTheme.typography.bodyMedium, color = muted)
                lines.isEmpty() -> Text(
                    "These earbuds did not share model or firmware details over standard Bluetooth.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = muted,
                )
                else -> lines.forEach { (label, value) -> DetailLine(label, value) }
            }
        }

        if (report != null) {
            listOf(SupportLevel.SUPPORTED, SupportLevel.PARTIAL, SupportLevel.UNAVAILABLE).forEach { level ->
                val items = report.features.filter { it.level == level }
                if (items.isNotEmpty()) {
                    SectionTitle(
                        when (level) {
                            SupportLevel.SUPPORTED -> "Supported"
                            SupportLevel.PARTIAL -> "Partly supported"
                            SupportLevel.UNAVAILABLE -> "Not available"
                        },
                    )
                    GlassCard(Modifier.fillMaxWidth()) {
                        items.forEachIndexed { index, status ->
                            if (index > 0) {
                                HorizontalDivider(
                                    Modifier.padding(vertical = 12.dp),
                                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f),
                                )
                            }
                            FeatureRow(status)
                        }
                    }
                }
            }
            if (report.limitations.isNotEmpty()) {
                SectionTitle("Good to know")
                GlassCard(Modifier.fillMaxWidth()) {
                    report.limitations.forEach {
                        Text(it, style = MaterialTheme.typography.bodySmall, color = muted, modifier = Modifier.padding(bottom = 8.dp))
                    }
                }
            }
        }

        if (saved != null) {
            TextButton(onClick = { onForget(device.address) }) { Text("Remove from this phone") }
        }
    }

    if (renaming && device != null) {
        var text by remember { mutableStateOf(saved?.customName ?: "") }
        AlertDialog(
            onDismissRequest = { renaming = false },
            title = { Text("Rename device") },
            text = {
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    label = { Text("Name") },
                    singleLine = true,
                    supportingText = { Text("Leave empty to use the name from the device.") },
                )
            },
            confirmButton = {
                TextButton(onClick = { onRename(device.address, text); renaming = false }) { Text("Save name") }
            },
            dismissButton = { TextButton(onClick = { renaming = false }) { Text("Cancel") } },
        )
    }
}

@Composable
private fun DetailLine(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(0.35f))
        Text(value, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(0.65f))
    }
}

package com.unibud.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.unibud.app.core.model.BrandSource
import com.unibud.app.core.model.SavedDevice
import com.unibud.app.core.model.SupportLevel
import com.unibud.app.ui.AppUiState
import com.unibud.app.ui.brandLabel
import com.unibud.app.ui.brandSourceLabel
import com.unibud.app.ui.channelLabel
import com.unibud.app.ui.components.EarbudsIllustration
import com.unibud.app.ui.components.FeatureStatusRow
import com.unibud.app.ui.components.GhostButton
import com.unibud.app.ui.components.NeoPanel
import com.unibud.app.ui.components.Pill
import com.unibud.app.ui.components.SectionLabel
import com.unibud.app.ui.kindLabel
import com.unibud.app.ui.theme.Neo

@Composable
fun InfoScreen(
    state: AppUiState,
    saved: SavedDevice?,
    onRename: (address: String, name: String) -> Unit,
    onSetBrand: (address: String, brand: String) -> Unit,
    onForget: (address: String) -> Unit,
    onRefresh: () -> Unit,
) {
    val c = Neo.colors
    val device = state.selected
    val report = state.report
    var renaming by remember { mutableStateOf(false) }
    var settingBrand by remember { mutableStateOf(false) }
    var showAll by remember { mutableStateOf(false) }

    Column(
        Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        if (device == null) {
            NeoPanel(Modifier.fillMaxWidth(), corner = 28.dp, glow = true) {
                Text("No device selected", style = MaterialTheme.typography.titleLarge, color = c.text)
                Spacer(Modifier.height(6.dp))
                Text("Connect earbuds, or open a paired device from Devices, to see its details.", style = MaterialTheme.typography.bodyMedium, color = c.textDim)
            }
            return@Column
        }

        val displayName = saved?.displayName ?: device.name ?: device.address
        val info = report?.info

        NeoPanel(Modifier.fillMaxWidth(), corner = 28.dp, glow = state.selectedConnected) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                EarbudsIllustration(Modifier.size(96.dp), animated = false)
                Spacer(Modifier.width(8.dp))
                Column(Modifier.weight(1f)) {
                    Text(displayName, style = MaterialTheme.typography.titleLarge, color = c.text)
                    Text(brandLabel(device.brand) ?: "Brand not detected", style = MaterialTheme.typography.bodyMedium, color = c.textDim)
                    Spacer(Modifier.height(6.dp))
                    Pill(kindLabel(device.classification), c.violet)
                }
            }
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                GhostButton("Rename", { renaming = true })
                GhostButton(if (device.brand == null || device.brand?.source == BrandSource.GUESS) "Set brand" else "Change brand", { settingBrand = true })
                GhostButton("Check again", onRefresh)
            }
        }

        SectionLabel("Device details")
        NeoPanel(Modifier.fillMaxWidth()) {
            DetailLine("Brand", brandLabel(device.brand) ?: "Not detected")
            DetailLine("Brand source", brandSourceLabel(device.brand))
            DetailLine("Made by", info?.manufacturerName ?: "Not reported")
            DetailLine("Model", info?.modelNumber ?: "Not reported")
            DetailLine("Firmware", info?.firmwareRevision ?: "Not reported")
            info?.hardwareRevision?.let { DetailLine("Hardware", it) }
            info?.softwareRevision?.let { DetailLine("Software", it) }
            DetailLine("Audio output", when (info?.audioOutputActive) {
                true -> info?.audioOutputType ?: "Active"
                false -> "Not the active output"
                null -> "Unknown"
            })
            channelLabel(info?.audioChannels.orEmpty())?.let { DetailLine("Channels", it) }
            if (info?.audioSampleRates.orEmpty().isNotEmpty()) {
                DetailLine("Sample rates", info?.audioSampleRates.orEmpty().joinToString(", ") { "${it / 1000.0}".removeSuffix(".0") + " kHz" })
            }
            DetailLine("Audio codec", "Not shared by Android")
            DetailLine("Address", device.address)
            DetailLine("Paired", if (device.isBonded) "Yes" else "No")
            DetailLine("Connected", if (state.selectedConnected) "Yes" else "No")
            if (state.discovering) {
                Spacer(Modifier.height(6.dp))
                Text("Reading device details", style = MaterialTheme.typography.bodySmall, color = c.textDim)
            }
        }

        if (report != null) {
            SectionLabel("Supported features")
            NeoPanel(Modifier.fillMaxWidth()) {
                val ordered = report.features.sortedByDescending { it.level.rank }
                val shown = if (showAll) ordered else ordered.take(6)
                shown.forEachIndexed { index, status ->
                    if (index > 0) Spacer(Modifier.height(12.dp))
                    FeatureStatusRow(status)
                }
                TextButton(onClick = { showAll = !showAll }) {
                    Text(if (showAll) "Show less" else "See all ${ordered.size}", color = c.cyan)
                }
            }
            if (report.limitations.isNotEmpty()) {
                SectionLabel("Good to know")
                NeoPanel(Modifier.fillMaxWidth()) {
                    report.limitations.forEach {
                        Text(it, style = MaterialTheme.typography.bodySmall, color = c.textDim, modifier = Modifier.padding(bottom = 8.dp))
                    }
                }
            }
        }

        if (saved != null) {
            TextButton(onClick = { onForget(device.address) }) { Text("Remove from this phone", color = c.red) }
        }
        Spacer(Modifier.height(8.dp))
    }

    if (renaming && device != null) {
        var text by remember { mutableStateOf(saved?.customName ?: "") }
        AlertDialog(
            onDismissRequest = { renaming = false },
            title = { Text("Rename device") },
            text = {
                OutlinedTextField(
                    value = text, onValueChange = { text = it }, label = { Text("Name") }, singleLine = true,
                    supportingText = { Text("Leave empty to use the name from the device.") },
                )
            },
            confirmButton = { TextButton(onClick = { onRename(device.address, text); renaming = false }) { Text("Save name") } },
            dismissButton = { TextButton(onClick = { renaming = false }) { Text("Cancel") } },
        )
    }

    if (settingBrand && device != null) {
        var text by remember { mutableStateOf(if (device.brand?.source == BrandSource.USER) device.brand?.displayName.orEmpty() else "") }
        AlertDialog(
            onDismissRequest = { settingBrand = false },
            title = { Text("Set the brand") },
            text = {
                OutlinedTextField(
                    value = text, onValueChange = { text = it }, label = { Text("Brand") }, singleLine = true,
                    supportingText = { Text("Used only on this phone. Leave empty to detect it automatically.") },
                )
            },
            confirmButton = { TextButton(onClick = { onSetBrand(device.address, text); settingBrand = false }) { Text("Save brand") } },
            dismissButton = { TextButton(onClick = { settingBrand = false }) { Text("Cancel") } },
        )
    }
}

@Composable
private fun DetailLine(label: String, value: String) {
    val c = Neo.colors
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = c.textDim, modifier = Modifier.weight(0.38f))
        Text(value, style = MaterialTheme.typography.bodyMedium, color = c.text, modifier = Modifier.weight(0.62f))
    }
}

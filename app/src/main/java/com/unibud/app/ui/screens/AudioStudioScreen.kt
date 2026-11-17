package com.unibud.app.ui.screens

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.unibud.app.core.audio.BuiltInPresets
import com.unibud.app.core.audio.EqBands
import com.unibud.app.core.audio.EqPreset
import com.unibud.app.core.capability.ControlCommand
import com.unibud.app.core.model.CapabilityReport
import com.unibud.app.core.model.FeatureId
import com.unibud.app.core.model.SupportLevel
import com.unibud.app.ui.components.GhostButton
import com.unibud.app.ui.components.GradientButton
import com.unibud.app.ui.components.NeoPanel
import com.unibud.app.ui.components.NeoSlider
import com.unibud.app.ui.components.SectionLabel
import com.unibud.app.ui.components.VerticalBandSlider
import com.unibud.app.ui.theme.Neo
import java.util.Locale

@Composable
fun AudioStudioScreen(
    report: CapabilityReport?,
    customPresets: List<EqPreset>,
    onSave: (id: Long?, name: String, gainsDb: List<Float>) -> Unit,
    onDelete: (Long) -> Unit,
    onApply: (ControlCommand) -> Unit,
) {
    val c = Neo.colors
    val presets = BuiltInPresets.all + customPresets
    var selectedId by rememberSaveable { mutableStateOf(BuiltInPresets.all.first().id) }
    val selected = presets.firstOrNull { it.id == selectedId } ?: presets.first()
    var gains by remember(selected.id, selected.gainsDb) { mutableStateOf(selected.gainsDb) }
    var name by remember(selected.id) { mutableStateOf(if (selected.builtIn) "" else selected.name) }

    val eq = report?.status(FeatureId.EQUALIZER)
    val canApply = eq != null && eq.level != SupportLevel.UNAVAILABLE

    fun setBand(index: Int, value: Float) {
        gains = gains.toMutableList().also { it[index] = EqBands.clamp(value) }
    }
    fun bass() = (gains[0] + gains[1]) / 2f
    fun treble() = (gains[3] + gains[4]) / 2f

    Column(
        Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        NeoPanel(Modifier.fillMaxWidth(), corner = 28.dp, glow = canApply) {
            Text("Equalizer", style = MaterialTheme.typography.titleLarge, color = c.text)
            Text("Tune your sound", style = MaterialTheme.typography.bodySmall, color = c.textDim)
            Spacer(Modifier.height(10.dp))
            Row(Modifier.fillMaxWidth().height(190.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                EqBands.FREQUENCIES_HZ.forEachIndexed { index, hz ->
                    Column(Modifier.weight(1f).fillMaxHeight(), horizontalAlignment = Alignment.CenterHorizontally) {
                        VerticalBandSlider(gains[index], { setBand(index, it) }, Modifier.weight(1f).fillMaxWidth())
                        Spacer(Modifier.height(4.dp))
                        Text(EqBands.label(hz), style = MaterialTheme.typography.labelSmall, color = c.textDim)
                    }
                }
            }
        }

        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            presets.forEach { preset ->
                FilterChip(
                    selected = preset.id == selected.id,
                    onClick = { selectedId = preset.id },
                    label = { Text(preset.name) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = c.violet, selectedLabelColor = androidx.compose.ui.graphics.Color.White,
                        labelColor = c.text, containerColor = c.panel,
                    ),
                )
            }
        }

        NeoPanel(Modifier.fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Bass", style = MaterialTheme.typography.titleMedium, color = c.text, modifier = Modifier.weight(1f))
                Text(String.format(Locale.ROOT, "%+.0f dB", bass()), style = MaterialTheme.typography.labelMedium, color = c.textDim)
            }
            NeoSlider(bass(), { v -> setBand(0, v); setBand(1, v) }, valueRange = EqBands.MIN_DB..EqBands.MAX_DB)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Treble", style = MaterialTheme.typography.titleMedium, color = c.text, modifier = Modifier.weight(1f))
                Text(String.format(Locale.ROOT, "%+.0f dB", treble()), style = MaterialTheme.typography.labelMedium, color = c.textDim)
            }
            NeoSlider(treble(), { v -> setBand(3, v); setBand(4, v) }, valueRange = EqBands.MIN_DB..EqBands.MAX_DB)
        }

        NeoPanel(Modifier.fillMaxWidth()) {
            OutlinedTextField(
                value = name, onValueChange = { name = it }, label = { Text("Preset name") }, singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = c.violet, unfocusedBorderColor = c.textDim.copy(alpha = 0.5f),
                    focusedTextColor = c.text, unfocusedTextColor = c.text, focusedLabelColor = c.violet, unfocusedLabelColor = c.textDim,
                    cursorColor = c.violet,
                ),
            )
            Spacer(Modifier.height(12.dp))
            GradientButton(
                "Save preset",
                {
                    val updateId = if (!selected.builtIn && name.trim() == selected.name) selected.id else null
                    onSave(updateId, name, gains)
                },
                Modifier.fillMaxWidth(), icon = Icons.Rounded.Save,
            )
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                GhostButton("Reset", { gains = selected.gainsDb }, Modifier.weight(1f))
                GhostButton("Apply to earbuds", { onApply(ControlCommand.SetEqualizer(gains)) }, Modifier.weight(1f), enabled = canApply)
            }
            if (!selected.builtIn) {
                TextButton(onClick = { onDelete(selected.id); selectedId = BuiltInPresets.all.first().id }) { Text("Delete this preset", color = c.red) }
            }
        }

        NeoPanel(Modifier.fillMaxWidth()) {
            Text(
                when {
                    report == null -> "Connect earbuds to apply a preset."
                    canApply -> "Your earbuds can take an equalizer. Pick a preset and apply it."
                    else -> (eq?.reason ?: "") + " Presets are saved on this phone for when your earbuds can take them."
                },
                style = MaterialTheme.typography.bodySmall, color = c.textDim,
            )
        }
        Spacer(Modifier.height(8.dp))
    }
}

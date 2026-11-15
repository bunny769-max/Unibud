package com.unibud.app.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.unibud.app.core.audio.BuiltInPresets
import com.unibud.app.core.audio.EqBands
import com.unibud.app.core.audio.EqPreset
import com.unibud.app.core.capability.ControlCommand
import com.unibud.app.core.model.CapabilityReport
import com.unibud.app.core.model.FeatureId
import com.unibud.app.core.model.SupportLevel
import com.unibud.app.ui.components.GlassCard
import com.unibud.app.ui.components.SectionTitle
import java.util.Locale

@Composable
fun AudioStudioScreen(
    report: CapabilityReport?,
    customPresets: List<EqPreset>,
    onSave: (id: Long?, name: String, gainsDb: List<Float>) -> Unit,
    onDelete: (Long) -> Unit,
    onApply: (ControlCommand) -> Unit,
) {
    val presets = BuiltInPresets.all + customPresets
    var selectedId by rememberSaveable { mutableStateOf(BuiltInPresets.all.first().id) }
    val selected = presets.firstOrNull { it.id == selectedId } ?: presets.first()
    var gains by remember(selected.id, selected.gainsDb) { mutableStateOf(selected.gainsDb) }
    var name by remember(selected.id) { mutableStateOf(if (selected.builtIn) "" else selected.name) }

    val eq = report?.status(FeatureId.EQUALIZER)
    val canApply = eq != null && eq.level != SupportLevel.UNAVAILABLE
    val muted = MaterialTheme.colorScheme.onSurfaceVariant

    Column(
        Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        GlassCard(Modifier.fillMaxWidth(), corner = 32.dp) {
            Text(
                when {
                    report == null -> "Connect earbuds to apply sound"
                    canApply -> "Your earbuds can take an equalizer"
                    else -> "Your earbuds' equalizer is out of reach for now"
                },
                style = MaterialTheme.typography.titleLarge,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                when {
                    report == null -> "You can still build and save presets. They stay on this phone."
                    canApply -> "Pick a preset and apply it."
                    else -> "${eq?.reason ?: ""} You can still build and save presets for later."
                },
                style = MaterialTheme.typography.bodyMedium,
                color = muted,
            )
        }

        SectionTitle("Presets")
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            presets.forEach { preset ->
                FilterChip(
                    selected = preset.id == selected.id,
                    onClick = { selectedId = preset.id },
                    label = { Text(preset.name) },
                )
            }
        }

        GlassCard(Modifier.fillMaxWidth()) {
            EqCurve(gains, Modifier.fillMaxWidth().height(120.dp))
            Spacer(Modifier.height(8.dp))
            EqBands.FREQUENCIES_HZ.forEachIndexed { index, hz ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(EqBands.label(hz), style = MaterialTheme.typography.labelMedium, modifier = Modifier.weight(0.28f))
                    Slider(
                        value = gains[index],
                        onValueChange = { v -> gains = gains.toMutableList().also { it[index] = v } },
                        valueRange = EqBands.MIN_DB..EqBands.MAX_DB,
                        steps = 23,
                        modifier = Modifier.weight(0.52f),
                    )
                    Text(
                        String.format(Locale.ROOT, "%+.0f dB", gains[index]),
                        style = MaterialTheme.typography.labelMedium,
                        color = muted,
                        modifier = Modifier.weight(0.2f),
                    )
                }
            }
        }

        GlassCard(Modifier.fillMaxWidth()) {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Preset name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = {
                    val updateId = if (!selected.builtIn && name.trim() == selected.name) selected.id else null
                    onSave(updateId, name, gains)
                }) { Text("Save preset") }
                OutlinedButton(onClick = { gains = selected.gainsDb }) { Text("Reset") }
            }
            Spacer(Modifier.height(8.dp))
            FilledTonalButton(onClick = { onApply(ControlCommand.SetEqualizer(gains)) }, enabled = canApply) {
                Text("Apply to earbuds")
            }
            if (!selected.builtIn) {
                TextButton(onClick = { onDelete(selected.id); selectedId = BuiltInPresets.all.first().id }) {
                    Text("Delete this preset")
                }
            }
        }
    }
}

@Composable
private fun EqCurve(gains: List<Float>, modifier: Modifier) {
    val line = MaterialTheme.colorScheme.primary
    val grid = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        drawLine(grid, Offset(0f, h / 2), Offset(w, h / 2), strokeWidth = 1.dp.toPx())
        val pad = 10.dp.toPx()
        val points = gains.mapIndexed { i, g ->
            Offset(w * (i + 0.5f) / gains.size, h / 2 - (g / EqBands.MAX_DB) * (h / 2 - pad))
        }
        val path = Path().apply {
            moveTo(points[0].x, points[0].y)
            for (i in 1 until points.size) {
                val prev = points[i - 1]
                val cur = points[i]
                val midX = (prev.x + cur.x) / 2
                cubicTo(midX, prev.y, midX, cur.y, cur.x, cur.y)
            }
        }
        drawPath(path, line, style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round))
        points.forEach { drawCircle(line, 5.dp.toPx(), it) }
    }
}

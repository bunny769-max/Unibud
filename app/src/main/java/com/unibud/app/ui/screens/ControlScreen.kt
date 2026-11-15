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
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.unibud.app.core.capability.ControlCommand
import com.unibud.app.core.capability.SoundMode
import com.unibud.app.core.model.CapabilityReport
import com.unibud.app.core.model.FeatureId
import com.unibud.app.core.model.FeatureStatus
import com.unibud.app.core.model.SupportLevel
import com.unibud.app.ui.components.FeatureRow
import com.unibud.app.ui.components.GlassCard
import com.unibud.app.ui.components.SectionTitle

private val groups: List<Pair<String, List<FeatureId>>> = listOf(
    "Noise control" to listOf(FeatureId.ANC, FeatureId.TRANSPARENCY, FeatureId.NORMAL_MODE, FeatureId.ADAPTIVE_NOISE),
    "Audio" to listOf(FeatureId.EQUALIZER, FeatureId.SPATIAL_AUDIO, FeatureId.MICROPHONE_SETTINGS, FeatureId.CODEC_INFO),
    "Gaming" to listOf(FeatureId.LOW_LATENCY),
    "Touch and sensors" to listOf(
        FeatureId.TOUCH_CONTROLS, FeatureId.IN_EAR_DETECTION, FeatureId.AUTO_PAUSE, FeatureId.DUAL_CONNECTION,
    ),
)

private fun soundModeFor(feature: FeatureId): SoundMode? = when (feature) {
    FeatureId.ANC -> SoundMode.ANC
    FeatureId.TRANSPARENCY -> SoundMode.TRANSPARENCY
    FeatureId.NORMAL_MODE -> SoundMode.NORMAL
    FeatureId.ADAPTIVE_NOISE -> SoundMode.ADAPTIVE
    else -> null
}

@Composable
fun ControlScreen(report: CapabilityReport?, onExecute: (ControlCommand) -> Unit) {
    Column(
        Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        if (report == null) {
            GlassCard(Modifier.fillMaxWidth(), corner = 32.dp) {
                Text("Nothing to control yet", style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(6.dp))
                Text(
                    "Connect your earbuds and their controls will appear here.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            return@Column
        }

        GlassCard(Modifier.fillMaxWidth(), corner = 32.dp) {
            Text("Controls that really work", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(6.dp))
            Text(
                "A control is only active when your earbuds' brand has a supported integration. Everything else stays switched off and says why.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        groups.forEach { (title, features) ->
            SectionTitle(title)
            GlassCard(Modifier.fillMaxWidth()) {
                features.forEachIndexed { index, feature ->
                    if (index > 0) {
                        HorizontalDivider(
                            Modifier.padding(vertical = 12.dp),
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f),
                        )
                    }
                    ControlRow(report.status(feature), onExecute)
                }
            }
        }
    }
}

@Composable
private fun ControlRow(status: FeatureStatus, onExecute: (ControlCommand) -> Unit) {
    val usable = status.level != SupportLevel.UNAVAILABLE
    FeatureRow(status) {
        val mode = soundModeFor(status.feature)
        when {
            mode != null -> {
                Spacer(Modifier.height(8.dp))
                FilledTonalButton(onClick = { onExecute(ControlCommand.SetSoundMode(mode)) }, enabled = usable) {
                    Text("Switch to this mode")
                }
            }
            status.feature == FeatureId.LOW_LATENCY -> {
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    FilledTonalButton(onClick = { onExecute(ControlCommand.SetLowLatency(true)) }, enabled = usable) {
                        Text("Turn on")
                    }
                    OutlinedButton(onClick = { onExecute(ControlCommand.SetLowLatency(false)) }, enabled = usable) {
                        Text("Turn off")
                    }
                }
            }
        }
    }
}

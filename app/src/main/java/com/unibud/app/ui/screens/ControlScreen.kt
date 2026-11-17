package com.unibud.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Equalizer
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Hearing
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.RadioButtonChecked
import androidx.compose.material.icons.rounded.SportsEsports
import androidx.compose.material.icons.rounded.TouchApp
import androidx.compose.material.icons.rounded.ViewInAr
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.unibud.app.core.capability.ControlCommand
import com.unibud.app.core.capability.SoundMode
import com.unibud.app.core.model.CapabilityReport
import com.unibud.app.core.model.FeatureId
import com.unibud.app.core.model.SupportLevel
import com.unibud.app.ui.components.FeatureStatusRow
import com.unibud.app.ui.components.ModeTile
import com.unibud.app.ui.components.NeoPanel
import com.unibud.app.ui.components.SectionLabel
import com.unibud.app.ui.components.ToggleRow
import com.unibud.app.ui.theme.Neo

private class ControlItem(val feature: FeatureId, val icon: ImageVector, val title: String, val subtitle: String)

private val items = listOf(
    ControlItem(FeatureId.LOW_LATENCY, Icons.Rounded.SportsEsports, "Gaming mode", "Low latency for seamless gaming"),
    ControlItem(FeatureId.SPATIAL_AUDIO, Icons.Rounded.ViewInAr, "Spatial audio", "Immersive 3D sound"),
    ControlItem(FeatureId.IN_EAR_DETECTION, Icons.Rounded.Hearing, "In-ear detection", "Knows when you wear them"),
    ControlItem(FeatureId.AUTO_PAUSE, Icons.Rounded.Pause, "Auto pause", "Pause when removed"),
    ControlItem(FeatureId.TOUCH_CONTROLS, Icons.Rounded.TouchApp, "Touch controls", "Choose what taps and holds do"),
    ControlItem(FeatureId.MICROPHONE_SETTINGS, Icons.Rounded.Mic, "Microphone", "Microphone behaviour"),
    ControlItem(FeatureId.DUAL_CONNECTION, Icons.Rounded.Equalizer, "Dual connection", "Connect to two devices"),
)

@Composable
fun ControlCenterScreen(report: CapabilityReport?, onExecute: (ControlCommand) -> Unit) {
    val c = Neo.colors
    Column(
        Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (report == null) {
            NeoPanel(Modifier.fillMaxWidth(), corner = 28.dp, glow = true) {
                Text("Nothing to control yet", style = MaterialTheme.typography.titleLarge, color = c.text)
                Spacer(Modifier.height(6.dp))
                Text("Connect your earbuds and their controls will appear here.", style = MaterialTheme.typography.bodyMedium, color = c.textDim)
            }
            return@Column
        }

        NeoPanel(Modifier.fillMaxWidth(), corner = 28.dp, glow = true) {
            Text("Noise control", style = MaterialTheme.typography.titleLarge, color = c.text)
            Spacer(Modifier.height(10.dp))
            val modes = listOf(
                Triple("ANC", Icons.Rounded.GraphicEq, SoundMode.ANC),
                Triple("Transparency", Icons.Rounded.WbSunny, SoundMode.TRANSPARENCY),
                Triple("Normal", Icons.Rounded.RadioButtonChecked, SoundMode.NORMAL),
                Triple("Adaptive", Icons.Rounded.AutoAwesome, SoundMode.ADAPTIVE),
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                modes.forEach { (label, icon, mode) ->
                    val feature = ControlCommand.SetSoundMode(mode).feature
                    ModeTile(
                        label, icon, selected = false, enabled = report.status(feature).level != SupportLevel.UNAVAILABLE,
                        onClick = { onExecute(ControlCommand.SetSoundMode(mode)) }, modifier = Modifier.weight(1f),
                    )
                }
            }
            if (modes.none { (_, _, m) -> report.status(ControlCommand.SetSoundMode(m).feature).level != SupportLevel.UNAVAILABLE }) {
                Spacer(Modifier.height(8.dp))
                Text(report.status(FeatureId.ANC).reason, style = MaterialTheme.typography.bodySmall, color = c.textDim)
            }
        }

        items.forEach { item ->
            val status = report.status(item.feature)
            val usable = status.level != SupportLevel.UNAVAILABLE
            var checked by remember { mutableStateOf(false) }
            ToggleRow(
                icon = item.icon, title = item.title,
                subtitle = if (usable) item.subtitle else status.reason,
                checked = checked, enabled = usable,
                onCheckedChange = {
                    checked = it
                    if (item.feature == FeatureId.LOW_LATENCY) onExecute(ControlCommand.SetLowLatency(it))
                },
            )
        }

        SectionLabel("What works on any earbuds")
        NeoPanel(Modifier.fillMaxWidth()) {
            listOf(FeatureId.VOLUME, FeatureId.MEDIA_PLAYBACK, FeatureId.CONNECTION_STATUS, FeatureId.FIRMWARE_INFO).forEachIndexed { index, feature ->
                if (index > 0) Spacer(Modifier.height(12.dp))
                FeatureStatusRow(report.status(feature))
            }
        }

        NeoPanel(Modifier.fillMaxWidth()) {
            Text("Why are some controls off?", style = MaterialTheme.typography.titleMedium, color = c.text)
            Spacer(Modifier.height(6.dp))
            Text(
                "Noise cancelling, equalizer, touch controls and similar features live inside the earbuds' own firmware. Each maker talks to them in a private language that Android does not share with other apps. " +
                    "UniBud only shows a control as active when it can really send it, and it never sends commands it does not have documentation for. " +
                    "Volume and playback use Android itself, so they work on every pair.",
                style = MaterialTheme.typography.bodySmall, color = c.textDim,
            )
        }
        Spacer(Modifier.height(8.dp))
    }
}

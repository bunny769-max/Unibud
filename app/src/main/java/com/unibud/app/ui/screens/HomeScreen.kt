package com.unibud.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Bluetooth
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.CloudDone
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.Equalizer
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.RadioButtonChecked
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material.icons.rounded.SportsEsports
import androidx.compose.material.icons.rounded.TouchApp
import androidx.compose.material.icons.rounded.ViewInAr
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.unibud.app.core.capability.ControlCommand
import com.unibud.app.core.capability.MediaKey
import com.unibud.app.core.capability.SoundMode
import com.unibud.app.core.model.CapabilityReport
import com.unibud.app.core.model.FeatureId
import com.unibud.app.core.model.SupportLevel
import com.unibud.app.ui.AppUiState
import com.unibud.app.ui.components.BatteryGlyph
import com.unibud.app.ui.components.EarbudsIllustration
import com.unibud.app.ui.components.GradientButton
import com.unibud.app.ui.components.LogoMark
import com.unibud.app.ui.components.ModeTile
import com.unibud.app.ui.components.NeoPanel
import com.unibud.app.ui.components.NeoSlider
import com.unibud.app.ui.components.Pill
import com.unibud.app.ui.components.QuickTile
import com.unibud.app.ui.components.IconBadge
import com.unibud.app.ui.theme.Neo
import kotlin.math.roundToInt

private data class ModeOption(val label: String, val icon: ImageVector, val mode: SoundMode)

private val modeOptions = listOf(
    ModeOption("ANC", Icons.Rounded.GraphicEq, SoundMode.ANC),
    ModeOption("Transparency", Icons.Rounded.WbSunny, SoundMode.TRANSPARENCY),
    ModeOption("Normal", Icons.Rounded.RadioButtonChecked, SoundMode.NORMAL),
    ModeOption("Adaptive", Icons.Rounded.AutoAwesome, SoundMode.ADAPTIVE),
)

private fun CapabilityReport?.usable(feature: FeatureId): Boolean {
    val level = this?.status(feature)?.level
    return level != null && level != SupportLevel.UNAVAILABLE
}

@Composable
fun HomeScreen(
    state: AppUiState,
    displayName: String?,
    signedIn: Boolean,
    onProfile: () -> Unit,
    onDevices: () -> Unit,
    onControl: () -> Unit,
    onAudio: () -> Unit,
    onRequestPermission: () -> Unit,
    onExecute: (ControlCommand) -> Unit,
    onVolume: (Int) -> Unit,
    onMedia: (MediaKey) -> Unit,
) {
    val c = Neo.colors
    val selected = state.selected
    val report = state.report
    val connected = state.selectedConnected

    Column(
        Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            LogoMark(Modifier.size(38.dp), animateIn = false)
            Spacer(Modifier.width(8.dp))
            Text("UniBud", style = MaterialTheme.typography.titleLarge, color = c.text, modifier = Modifier.weight(1f))
            Row(
                Modifier.clip(CircleShape).clickable(onClick = onProfile).padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Icon(if (signedIn) Icons.Rounded.CloudDone else Icons.Rounded.CloudOff, null, tint = if (signedIn) c.green else c.textDim, modifier = Modifier.size(20.dp))
                Text(if (signedIn) "Synced" else "Not synced", color = c.textDim, style = MaterialTheme.typography.labelMedium)
            }
        }

        // Connection card
        when {
            !state.permissionGranted -> NeoPanel(Modifier.fillMaxWidth(), corner = 28.dp, glow = true) {
                Text("Bluetooth access needed", style = MaterialTheme.typography.titleLarge, color = c.text)
                Spacer(Modifier.height(6.dp))
                Text("UniBud needs this permission to find and read your earbuds.", style = MaterialTheme.typography.bodyMedium, color = c.textDim)
                Spacer(Modifier.height(14.dp))
                GradientButton("Allow Bluetooth access", onRequestPermission)
            }
            else -> NeoPanel(Modifier.fillMaxWidth(), corner = 28.dp, glow = connected, onClick = if (selected == null) onDevices else onControl) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier.size(44.dp).clip(CircleShape).background(Brush.linearGradient(listOf(if (connected) c.green else c.textDim, c.blue))),
                        contentAlignment = Alignment.Center,
                    ) { Icon(Icons.Rounded.Bluetooth, null, tint = Color.White, modifier = Modifier.size(24.dp)) }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            when {
                                selected == null -> "No earbuds connected"
                                connected -> "Connected"
                                else -> "Not connected"
                            },
                            style = MaterialTheme.typography.titleMedium, color = c.text,
                        )
                        Text(
                            displayName ?: selected?.name ?: "Connect in Android Bluetooth settings",
                            style = MaterialTheme.typography.bodySmall, color = c.textDim,
                        )
                    }
                    Icon(Icons.Rounded.ChevronRight, null, tint = c.textDim)
                }
                if (state.discovering) {
                    Spacer(Modifier.height(12.dp))
                    LinearProgressIndicator(Modifier.fillMaxWidth(), color = c.violet, trackColor = c.panelStrong)
                    Spacer(Modifier.height(4.dp))
                    Text("Checking what this device can really do", style = MaterialTheme.typography.bodySmall, color = c.textDim)
                }
            }
        }

        if (selected != null) {
            Hero(report)
            ActiveModePanel(report, onControl)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                modeOptions.forEach { option ->
                    ModeTile(
                        label = option.label, icon = option.icon, selected = false,
                        enabled = report.usable(ControlCommand.SetSoundMode(option.mode).feature),
                        onClick = { onExecute(ControlCommand.SetSoundMode(option.mode)) },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
            if (report != null && modeOptions.none { report.usable(ControlCommand.SetSoundMode(it.mode).feature) }) {
                Text(report.status(FeatureId.ANC).reason, style = MaterialTheme.typography.bodySmall, color = c.textDim)
            }
            QuickControls(report, onControl, onAudio)
            PlaybackPanel(state, report, onVolume, onMedia)
        }
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun Hero(report: CapabilityReport?) {
    val c = Neo.colors
    val info = report?.info
    val perBud = info?.leftBatteryPercent != null || info?.rightBatteryPercent != null || info?.caseBatteryPercent != null
    val overall = info?.batteryPercent
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            BudBattery("L", info?.leftBatteryPercent, Modifier.width(78.dp))
            EarbudsIllustration(Modifier.weight(1f).height(210.dp))
            BudBattery("R", info?.rightBatteryPercent, Modifier.width(78.dp))
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(if (!perBud && overall != null) "Battery" else "Case", color = c.textDim, style = MaterialTheme.typography.bodyMedium)
            BatteryGlyph(if (!perBud && overall != null) overall else info?.caseBatteryPercent)
        }
        if (report != null && !perBud) {
            Spacer(Modifier.height(6.dp))
            Text(
                if (overall != null) "These earbuds share one battery value. Left, right and case levels need the maker's protocol."
                else "Battery levels were not shared over standard Bluetooth.",
                style = MaterialTheme.typography.bodySmall, color = c.textDim, textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun BudBattery(letter: String, percent: Int?, modifier: Modifier) {
    val c = Neo.colors
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(letter, style = MaterialTheme.typography.titleLarge, color = c.text)
        Spacer(Modifier.height(4.dp))
        BatteryGlyph(percent)
    }
}

@Composable
private fun ActiveModePanel(report: CapabilityReport?, onControl: () -> Unit) {
    val c = Neo.colors
    NeoPanel(Modifier.fillMaxWidth(), onClick = onControl) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconBadge(Icons.Rounded.GraphicEq, c.violet)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text("Active mode", style = MaterialTheme.typography.bodySmall, color = c.textDim)
                Text(report?.info?.activeSoundMode ?: "Not reported by this device", style = MaterialTheme.typography.titleMedium, color = c.text)
            }
            Icon(Icons.Rounded.ChevronRight, null, tint = c.textDim)
        }
    }
}

@Composable
private fun QuickControls(report: CapabilityReport?, onControl: () -> Unit, onAudio: () -> Unit) {
    val c = Neo.colors
    NeoPanel(Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("Quick controls", style = MaterialTheme.typography.titleMedium, color = c.text, modifier = Modifier.weight(1f))
            Text("See all", color = c.cyan, style = MaterialTheme.typography.labelMedium, modifier = Modifier.clickable(onClick = onControl))
        }
        Spacer(Modifier.height(10.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            QuickTile("Equalizer", Icons.Rounded.Equalizer, locked = false, onClick = onAudio, modifier = Modifier.weight(1f))
            QuickTile("Spatial audio", Icons.Rounded.ViewInAr, locked = !report.usable(FeatureId.SPATIAL_AUDIO), onClick = onControl, modifier = Modifier.weight(1f))
            QuickTile("Gaming mode", Icons.Rounded.SportsEsports, locked = !report.usable(FeatureId.LOW_LATENCY), onClick = onControl, modifier = Modifier.weight(1f))
            QuickTile("Touch control", Icons.Rounded.TouchApp, locked = !report.usable(FeatureId.TOUCH_CONTROLS), onClick = onControl, modifier = Modifier.weight(1f))
        }
    }
}

/** Brand-independent controls: Android's own volume and media keys, which work with any earbuds. */
@Composable
private fun PlaybackPanel(state: AppUiState, report: CapabilityReport?, onVolume: (Int) -> Unit, onMedia: (MediaKey) -> Unit) {
    val c = Neo.colors
    val enabled = report.usable(FeatureId.VOLUME)
    var local by remember(state.volumePercent) { mutableFloatStateOf(state.volumePercent / 100f) }
    NeoPanel(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Playback and volume", style = MaterialTheme.typography.titleMedium, color = c.text, modifier = Modifier.weight(1f))
            Pill(if (enabled) "Works on any earbuds" else "Not active", if (enabled) c.green else c.textDim)
        }
        Spacer(Modifier.height(10.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.VolumeUp, null, tint = if (enabled) c.cyan else c.textDim)
            NeoSlider(local, { local = it }, Modifier.weight(1f), enabled = enabled, onFinished = { onVolume((local * 100).roundToInt()) })
            Text("${(local * 100).roundToInt()}%", style = MaterialTheme.typography.labelMedium, color = c.textDim, modifier = Modifier.width(40.dp), textAlign = TextAlign.End)
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
            RoundIconButton(Icons.Rounded.SkipPrevious, "Previous", enabled, 48.dp, false) { onMedia(MediaKey.PREVIOUS) }
            RoundIconButton(Icons.Rounded.PlayArrow, "Play or pause", enabled, 60.dp, true) { onMedia(MediaKey.PLAY_PAUSE) }
            RoundIconButton(Icons.Rounded.SkipNext, "Next", enabled, 48.dp, false) { onMedia(MediaKey.NEXT) }
        }
        if (!enabled) {
            Spacer(Modifier.height(8.dp))
            Text(
                report?.status(FeatureId.VOLUME)?.reason ?: "Connect your earbuds to control playback.",
                style = MaterialTheme.typography.bodySmall, color = c.textDim,
            )
        }
    }
}

@Composable
private fun RoundIconButton(icon: ImageVector, description: String, enabled: Boolean, size: Dp, filled: Boolean, onClick: () -> Unit) {
    val c = Neo.colors
    val fill: Brush = if (filled) c.accent else Brush.linearGradient(listOf(c.panelStrong, c.panelStrong))
    Box(
        Modifier.size(size).alpha(if (enabled) 1f else 0.4f).clip(CircleShape).background(fill)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { Icon(icon, description, tint = if (filled) Color.White else c.text, modifier = Modifier.size(size * 0.5f)) }
}

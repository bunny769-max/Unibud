package com.unibud.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bluetooth
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Cloud
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.unibud.app.core.model.AppSettings
import com.unibud.app.core.model.ThemeMode
import com.unibud.app.data.cloud.AuthUser
import com.unibud.app.ui.relativeTime
import com.unibud.app.ui.components.GhostButton
import com.unibud.app.ui.components.GradientButton
import com.unibud.app.ui.components.IconBadge
import com.unibud.app.ui.components.NeoPanel
import com.unibud.app.ui.components.Pill
import com.unibud.app.ui.theme.Neo

@Composable
fun ProfileScreen(
    user: AuthUser?,
    settings: AppSettings,
    deviceCount: Int,
    permissionGranted: Boolean,
    signingIn: Boolean,
    syncing: Boolean,
    onSignIn: () -> Unit,
    onSignOut: () -> Unit,
    onSync: () -> Unit,
    onTheme: (ThemeMode) -> Unit,
    onDevices: () -> Unit,
    onRequestPermission: () -> Unit,
) {
    val c = Neo.colors
    var privacy by remember { mutableStateOf(false) }

    Column(
        Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        NeoPanel(Modifier.fillMaxWidth(), corner = 28.dp, glow = user != null) {
            if (user != null) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier.size(52.dp).clip(CircleShape).background(Brush.linearGradient(listOf(c.blue, c.violet))),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text((user.name ?: user.email ?: "U").take(1).uppercase(), color = Color.White, style = MaterialTheme.typography.titleLarge)
                    }
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text(user.name ?: "Google account", style = MaterialTheme.typography.titleMedium, color = c.text)
                        Text(user.email ?: "", style = MaterialTheme.typography.bodySmall, color = c.textDim)
                    }
                    Pill("Signed in", c.green)
                }
            } else {
                Text("Sign in to sync", style = MaterialTheme.typography.titleLarge, color = c.text)
                Spacer(Modifier.height(4.dp))
                Text(
                    "Keep presets, device names and your theme on every phone. Pairing keys never leave Android.",
                    style = MaterialTheme.typography.bodySmall, color = c.textDim,
                )
                Spacer(Modifier.height(12.dp))
                GradientButton(if (signingIn) "Signing in" else "Sign in with Google", onSignIn, Modifier.fillMaxWidth(), enabled = !signingIn)
            }
        }

        NeoPanel(Modifier.fillMaxWidth(), padding = 4.dp) {
            SettingRow(Icons.Rounded.Bluetooth, "My devices", "$deviceCount ${if (deviceCount == 1) "device" else "devices"}", onClick = onDevices)
            SettingRow(
                Icons.Rounded.Cloud, "Cloud sync",
                when {
                    user == null -> "Sign in to turn on"
                    syncing -> "Syncing"
                    settings.lastSyncedAt != null -> "Last synced " + relativeTime(settings.lastSyncedAt)
                    else -> "Not synced yet"
                },
                onClick = if (user != null && !syncing) onSync else null,
            )
            SettingRow(Icons.Rounded.Bluetooth, "Bluetooth permission", if (permissionGranted) "Allowed" else "Not allowed yet", onClick = if (permissionGranted) null else onRequestPermission)
            SettingRow(Icons.Rounded.Security, "Privacy and security", "What UniBud stores", onClick = { privacy = true })
            SettingRow(Icons.Rounded.Info, "About UniBud", "Version 0.3.0", onClick = null)
        }

        NeoPanel(Modifier.fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconBadge(Icons.Rounded.DarkMode, c.violet)
                Spacer(Modifier.width(12.dp))
                Text("App appearance", style = MaterialTheme.typography.titleMedium, color = c.text)
            }
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                listOf(ThemeMode.SYSTEM to "Match phone", ThemeMode.DARK to "Dark", ThemeMode.LIGHT to "Light").forEach { (mode, label) ->
                    if (settings.themeMode == mode) {
                        GradientButton(label, { onTheme(mode) }, Modifier.weight(1f))
                    } else {
                        GhostButton(label, { onTheme(mode) }, Modifier.weight(1f))
                    }
                }
            }
        }

        if (user != null) {
            GhostButton("Sign out", onSignOut, Modifier.fillMaxWidth(), color = c.red)
        }
        Spacer(Modifier.height(8.dp))
    }

    if (privacy) {
        AlertDialog(
            onDismissRequest = { privacy = false },
            title = { Text("Privacy and security") },
            text = {
                Text(
                    "On this phone: device names, presets and settings.\n\n" +
                        "In your cloud account, only if you sign in: presets, device names, device model and firmware text, and your theme. " +
                        "Bluetooth addresses are never uploaded, and each device is stored under a one-way hash. Each account can only read its own data.\n\n" +
                        "Pairing keys stay inside Android. UniBud cannot read them.",
                )
            },
            confirmButton = { TextButton(onClick = { privacy = false }) { Text("Got it") } },
        )
    }
}

@Composable
private fun SettingRow(icon: ImageVector, title: String, value: String, onClick: (() -> Unit)?) {
    val c = Neo.colors
    val base = Modifier.fillMaxWidth().clip(MaterialTheme.shapes.medium)
    Row(
        (if (onClick != null) base.clickable(onClick = onClick) else base).padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconBadge(icon, c.violet, size = 38.dp)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = c.text)
            Text(value, style = MaterialTheme.typography.bodySmall, color = c.textDim)
        }
        if (onClick != null) Icon(Icons.Rounded.ChevronRight, null, tint = c.textDim)
    }
}

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
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.unibud.app.core.model.AppSettings
import com.unibud.app.core.model.ThemeMode
import com.unibud.app.ui.components.GlassCard
import com.unibud.app.ui.components.SectionTitle

@Composable
fun SettingsScreen(
    settings: AppSettings,
    permissionGranted: Boolean,
    onTheme: (ThemeMode) -> Unit,
    onRequestPermission: () -> Unit,
) {
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    Column(
        Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        SectionTitle("Appearance")
        GlassCard(Modifier.fillMaxWidth()) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(ThemeMode.SYSTEM to "Match phone", ThemeMode.LIGHT to "Light", ThemeMode.DARK to "Dark").forEach { (mode, label) ->
                    FilterChip(selected = settings.themeMode == mode, onClick = { onTheme(mode) }, label = { Text(label) })
                }
            }
        }

        SectionTitle("Account and sync")
        GlassCard(Modifier.fillMaxWidth()) {
            Text("Google sign-in is coming", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(4.dp))
            Text(
                "Soon you can sign in to keep device names, presets and preferences on every phone you use. Until then everything stays on this phone.",
                style = MaterialTheme.typography.bodySmall,
                color = muted,
            )
            Spacer(Modifier.height(12.dp))
            OutlinedButton(onClick = {}, enabled = false) { Text("Sign in with Google") }
        }

        SectionTitle("Privacy")
        GlassCard(Modifier.fillMaxWidth()) {
            Text(
                "Device names, presets and settings are stored only on this phone. Bluetooth pairing keys never leave Android, and UniBud cannot read them.",
                style = MaterialTheme.typography.bodyMedium,
                color = muted,
            )
        }

        SectionTitle("Bluetooth permission")
        GlassCard(Modifier.fillMaxWidth()) {
            Text(
                if (permissionGranted) "Allowed" else "Not allowed yet",
                style = MaterialTheme.typography.titleMedium,
            )
            if (!permissionGranted) {
                Spacer(Modifier.height(12.dp))
                Button(onClick = onRequestPermission) { Text("Allow Bluetooth access") }
            }
        }

        Text("UniBud 0.2.0", style = MaterialTheme.typography.bodySmall, color = muted, modifier = Modifier.padding(bottom = 16.dp))
    }
}

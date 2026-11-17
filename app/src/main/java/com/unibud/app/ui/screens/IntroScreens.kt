package com.unibud.app.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Devices
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.unibud.app.ui.components.EarbudsIllustration
import com.unibud.app.ui.components.GhostButton
import com.unibud.app.ui.components.IconBadge
import com.unibud.app.ui.components.LogoMark
import com.unibud.app.ui.components.NeoPanel
import com.unibud.app.ui.components.ParticleField
import com.unibud.app.ui.components.WaveBackdrop
import com.unibud.app.ui.theme.Neo
import kotlinx.coroutines.launch

@Composable
fun SplashScreen(durationMs: Int = 1800) {
    val c = Neo.colors
    val entrance = remember { Animatable(0f) }
    val progress = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        launch { entrance.animateTo(1f, tween(900, easing = FastOutSlowInEasing)) }
        progress.animateTo(1f, tween(durationMs, easing = LinearEasing))
    }
    Box(Modifier.fillMaxSize()) {
        ParticleField(Modifier.fillMaxSize())
        Column(Modifier.fillMaxSize().padding(horizontal = 28.dp, vertical = 40.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Column(
                Modifier.graphicsLayer { alpha = entrance.value; translationY = (1f - entrance.value) * 24.dp.toPx() },
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                LogoMark(Modifier.size(92.dp))
                Spacer(Modifier.height(8.dp))
                Text(
                    "UniBud",
                    style = MaterialTheme.typography.headlineLarge.copy(brush = Brush.horizontalGradient(listOf(c.text, c.violet))),
                )
                Spacer(Modifier.height(4.dp))
                Text("Connect • Explore • Control", color = c.textDim, style = MaterialTheme.typography.bodyMedium)
            }
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                EarbudsIllustration(Modifier.fillMaxWidth().aspectRatio(1f))
            }
            Text("Discover More. Do More.", color = c.text, style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(14.dp))
            Box(Modifier.fillMaxWidth().height(4.dp).clip(CircleShape).background(c.panelStrong)) {
                Box(Modifier.fillMaxWidth(progress.value).fillMaxHeight().clip(CircleShape).background(c.accent))
            }
        }
    }
}

@Composable
fun SignInScreen(busy: Boolean, configured: Boolean, onGoogle: () -> Unit, onSkip: () -> Unit) {
    val c = Neo.colors
    Box(Modifier.fillMaxSize()) {
        ParticleField(Modifier.fillMaxSize(), count = 18)
        WaveBackdrop(Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(280.dp))
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 28.dp, vertical = 36.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            LogoMark(Modifier.size(150.dp))
            Spacer(Modifier.height(8.dp))
            Text("Welcome to UniBud", style = MaterialTheme.typography.headlineMedium, color = c.text, textAlign = TextAlign.Center)
            Spacer(Modifier.height(8.dp))
            Text(
                "Sign in to sync your earbud presets and names across all your devices.",
                style = MaterialTheme.typography.bodyMedium, color = c.textDim, textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(28.dp))
            Box(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(50)).background(Color.White)
                    .clickable(enabled = !busy, onClick = onGoogle).padding(vertical = 14.dp),
                contentAlignment = Alignment.Center,
            ) {
                if (busy) {
                    CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp, color = c.violet)
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            "G",
                            style = MaterialTheme.typography.titleLarge.copy(
                                brush = Brush.linearGradient(listOf(Color(0xFF4285F4), Color(0xFFEA4335), Color(0xFFFBBC05), Color(0xFF34A853))),
                            ),
                        )
                        Text("Sign in with Google", color = Color(0xFF1F1F2E), style = MaterialTheme.typography.titleMedium)
                    }
                }
            }
            if (!configured) {
                Spacer(Modifier.height(8.dp))
                Text(
                    "This build has no Firebase config yet, so sign-in will explain what is missing.",
                    style = MaterialTheme.typography.bodySmall, color = c.textDim, textAlign = TextAlign.Center,
                )
            }
            Spacer(Modifier.height(20.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                listOf(Icons.Rounded.Sync to "Sync settings", Icons.Rounded.Star to "Save presets", Icons.Rounded.Devices to "Manage devices").forEach { (icon, label) ->
                    NeoPanel(Modifier.weight(1f), padding = 12.dp) {
                        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                            IconBadge(icon, c.cyan)
                            Spacer(Modifier.height(8.dp))
                            Text(label, style = MaterialTheme.typography.labelSmall, color = c.text, textAlign = TextAlign.Center)
                        }
                    }
                }
            }
            Spacer(Modifier.height(24.dp))
            GhostButton("Continue without signing in", onSkip, Modifier.fillMaxWidth())
            Spacer(Modifier.height(16.dp))
            Text(
                "Your data stays on this phone unless you sign in. Pairing keys never leave Android.",
                style = MaterialTheme.typography.bodySmall, color = c.textDim, textAlign = TextAlign.Center,
            )
        }
    }
}

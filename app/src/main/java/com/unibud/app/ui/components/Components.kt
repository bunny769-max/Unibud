package com.unibud.app.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bluetooth
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.unibud.app.core.model.FeatureStatus
import com.unibud.app.core.model.SupportLevel
import com.unibud.app.ui.theme.LocalIsDark
import com.unibud.app.ui.theme.supportColor

/** Translucent card with a hairline border. Larger [corner] marks the more important card on a screen. */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    corner: Dp = 22.dp,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val dark = LocalIsDark.current
    val shape = RoundedCornerShape(corner)
    val fill = if (dark) Color.White.copy(alpha = 0.07f) else Color.White.copy(alpha = 0.72f)
    val edge = if (dark) Color.White.copy(alpha = 0.14f) else Color(0xFF5B47E0).copy(alpha = 0.10f)
    val base = modifier.clip(shape).background(fill).border(1.dp, edge, shape)
    val tappable = if (onClick != null) base.clickable(onClick = onClick) else base
    Column(tappable.padding(18.dp), content = content)
}

@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(text, style = MaterialTheme.typography.titleMedium, modifier = modifier)
}

@Composable
fun StatusPill(text: String, color: Color, modifier: Modifier = Modifier) {
    Box(modifier.clip(CircleShape).background(color.copy(alpha = 0.16f)).padding(horizontal = 10.dp, vertical = 4.dp)) {
        Text(text, color = color, style = MaterialTheme.typography.labelMedium)
    }
}

@Composable
fun SupportChip(level: SupportLevel, modifier: Modifier = Modifier) {
    val text = when (level) {
        SupportLevel.SUPPORTED -> "Supported"
        SupportLevel.PARTIAL -> "Partial"
        SupportLevel.UNAVAILABLE -> "Unavailable"
    }
    StatusPill(text, supportColor(level, LocalIsDark.current), modifier)
}

/** One capability: name, honest status, and the reason behind it. */
@Composable
fun FeatureRow(status: FeatureStatus, modifier: Modifier = Modifier, trailing: @Composable () -> Unit = {}) {
    Column(modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(status.feature.label, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            SupportChip(status.level)
        }
        Spacer(Modifier.height(4.dp))
        Text(status.reason, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        trailing()
    }
}

/** Ring gauge. A null [percent] means the value cannot be read, and it is shown as such. */
@Composable
fun BatteryGauge(label: String, percent: Int?, modifier: Modifier = Modifier) {
    val progress by animateFloatAsState(
        targetValue = (percent ?: 0) / 100f,
        animationSpec = tween(900),
        label = "battery",
    )
    val colors = MaterialTheme.colorScheme
    val track = colors.outline.copy(alpha = 0.28f)
    val ring = when {
        percent == null -> colors.outline
        percent <= 20 -> colors.error
        else -> colors.secondary
    }
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.size(76.dp), contentAlignment = Alignment.Center) {
            Canvas(Modifier.fillMaxSize()) {
                val stroke = Stroke(width = 8.dp.toPx(), cap = StrokeCap.Round)
                val inset = stroke.width / 2
                val topLeft = Offset(inset, inset)
                val arcSize = Size(size.width - stroke.width, size.height - stroke.width)
                drawArc(track, 135f, 270f, false, topLeft, arcSize, style = stroke)
                if (percent != null) drawArc(ring, 135f, 270f * progress, false, topLeft, arcSize, style = stroke)
            }
            Text(
                text = percent?.let { "$it%" } ?: "n/a",
                style = MaterialTheme.typography.titleMedium,
                color = if (percent == null) colors.onSurfaceVariant else colors.onSurface,
                textAlign = TextAlign.Center,
            )
        }
        Spacer(Modifier.height(6.dp))
        Text(label, style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
    }
}

/** Expanding rings while [active]. The animation only exists while a scan is really running. */
@Composable
fun RadarScan(active: Boolean, modifier: Modifier = Modifier) {
    val color = MaterialTheme.colorScheme.primary
    Box(modifier.size(170.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            drawCircle(color.copy(alpha = 0.10f), radius = size.minDimension / 2 * 0.34f)
        }
        if (active) RadarRings(color)
        Icon(Icons.Rounded.Bluetooth, contentDescription = null, tint = color, modifier = Modifier.size(36.dp))
    }
}

@Composable
private fun RadarRings(color: Color) {
    val transition = rememberInfiniteTransition(label = "radar")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(2600, easing = LinearEasing), RepeatMode.Restart),
        label = "phase",
    )
    Canvas(Modifier.fillMaxSize()) {
        val maxRadius = size.minDimension / 2
        repeat(3) { i ->
            val p = (phase + i / 3f) % 1f
            drawCircle(
                color = color.copy(alpha = (1f - p) * 0.55f),
                radius = maxRadius * (0.34f + 0.66f * p),
                style = Stroke(width = 2.dp.toPx()),
            )
        }
    }
}

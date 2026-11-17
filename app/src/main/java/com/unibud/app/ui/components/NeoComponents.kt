package com.unibud.app.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
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
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Bluetooth
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.unibud.app.core.model.FeatureStatus
import com.unibud.app.core.model.SupportLevel
import com.unibud.app.ui.theme.Neo
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.random.Random

// ------------------------------------------------------------------ panels and buttons

/** Glass panel with a violet-to-blue hairline border. Larger [corner] and [glow] mark the hero panel. */
@Composable
fun NeoPanel(
    modifier: Modifier = Modifier,
    corner: Dp = 22.dp,
    glow: Boolean = false,
    padding: Dp = 16.dp,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val c = Neo.colors
    val shape = RoundedCornerShape(corner)
    val edge = Brush.linearGradient(
        listOf(c.violet.copy(alpha = if (glow) 0.95f else 0.55f), c.blue.copy(alpha = if (glow) 0.55f else 0.20f)),
    )
    val base = modifier.clip(shape).background(c.panel).border(1.dp, edge, shape)
    Column((if (onClick != null) base.clickable(onClick = onClick) else base).padding(padding), content = content)
}

@Composable
fun GradientButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: ImageVector? = null,
) {
    val c = Neo.colors
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.97f else 1f, label = "press")
    val fill: Brush = if (enabled) c.accent else SolidColor(c.panelStrong)
    Box(
        modifier
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clip(RoundedCornerShape(18.dp))
            .background(fill)
            .clickable(interactionSource = interaction, indication = null, enabled = enabled, onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 14.dp),
        contentAlignment = Alignment.Center,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (icon != null) Icon(icon, null, tint = if (enabled) Color.White else c.textDim, modifier = Modifier.size(20.dp))
            Text(text, color = if (enabled) Color.White else c.textDim, style = MaterialTheme.typography.labelLarge)
        }
    }
}

@Composable
fun GhostButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, color: Color = Neo.colors.violet, enabled: Boolean = true) {
    val shape = RoundedCornerShape(18.dp)
    Box(
        modifier.alpha(if (enabled) 1f else 0.45f).clip(shape).border(1.dp, color.copy(alpha = 0.7f), shape)
            .clickable(enabled = enabled, onClick = onClick).padding(horizontal = 18.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center,
    ) { Text(text, color = color, style = MaterialTheme.typography.labelLarge) }
}

@Composable
fun Pill(text: String, color: Color, modifier: Modifier = Modifier) {
    Box(modifier.clip(CircleShape).background(color.copy(alpha = 0.18f)).padding(horizontal = 10.dp, vertical = 4.dp)) {
        Text(text, color = color, style = MaterialTheme.typography.labelMedium)
    }
}

@Composable
fun IconBadge(icon: ImageVector, tint: Color, modifier: Modifier = Modifier, size: Dp = 44.dp) {
    Box(
        modifier.size(size).clip(RoundedCornerShape(14.dp)).background(tint.copy(alpha = 0.16f)),
        contentAlignment = Alignment.Center,
    ) { Icon(icon, null, tint = tint, modifier = Modifier.size(size * 0.5f)) }
}

@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(text, style = MaterialTheme.typography.titleMedium, color = Neo.colors.text, modifier = modifier)
}

@Composable
fun NeoTopBar(title: String, onBack: (() -> Unit)?, onTrailing: (() -> Unit)? = null, trailing: ImageVector = Icons.Rounded.Settings) {
    val c = Neo.colors
    Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(48.dp), contentAlignment = Alignment.Center) {
            if (onBack != null) IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Back", tint = c.text) }
        }
        Text(title, style = MaterialTheme.typography.titleLarge, color = c.text, textAlign = TextAlign.Center, modifier = Modifier.weight(1f))
        Box(Modifier.size(48.dp), contentAlignment = Alignment.Center) {
            if (onTrailing != null) IconButton(onClick = onTrailing) { Icon(trailing, "More", tint = c.text) }
        }
    }
}

// ------------------------------------------------------------------ tiles and rows

/** One sound mode. Dimmed and inert when the earbuds cannot do it. */
@Composable
fun ModeTile(label: String, icon: ImageVector, selected: Boolean, enabled: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val c = Neo.colors
    val shape = RoundedCornerShape(18.dp)
    val fill: Brush = if (selected) c.accent else SolidColor(c.panel)
    val glow by animateFloatAsState(if (selected) 1f else 0f, label = "glow")
    Column(
        modifier.alpha(if (enabled) 1f else 0.45f).clip(shape).background(fill)
            .border(1.dp, c.violet.copy(alpha = 0.25f + 0.5f * glow), shape)
            .clickable(enabled = enabled, onClick = onClick).padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(icon, null, tint = if (selected) Color.White else c.cyan, modifier = Modifier.size(26.dp))
        Spacer(Modifier.height(6.dp))
        Text(label, color = if (selected) Color.White else c.text, fontSize = 11.sp, textAlign = TextAlign.Center, maxLines = 1)
    }
}

@Composable
fun QuickTile(label: String, icon: ImageVector, locked: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val c = Neo.colors
    val shape = RoundedCornerShape(16.dp)
    Box(modifier.clip(shape).background(c.panel).border(1.dp, c.violet.copy(alpha = 0.3f), shape).clickable(onClick = onClick)) {
        Column(Modifier.fillMaxWidth().padding(vertical = 12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, null, tint = if (locked) c.textDim else c.cyan, modifier = Modifier.size(26.dp))
            Spacer(Modifier.height(6.dp))
            Text(label, color = if (locked) c.textDim else c.text, fontSize = 11.sp, textAlign = TextAlign.Center, maxLines = 1)
        }
        if (locked) Icon(Icons.Rounded.Lock, "Needs device support", tint = c.textDim, modifier = Modifier.align(Alignment.TopEnd).padding(6.dp).size(12.dp))
    }
}

@Composable
fun ToggleRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    enabled: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    val c = Neo.colors
    NeoPanel(Modifier.fillMaxWidth(), padding = 14.dp) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconBadge(icon, if (enabled) c.cyan else c.textDim)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium, color = if (enabled) c.text else c.textDim)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = c.textDim)
            }
            Spacer(Modifier.width(8.dp))
            if (enabled) {
                Switch(
                    checked = checked, onCheckedChange = onCheckedChange,
                    colors = SwitchDefaults.colors(checkedTrackColor = c.violet, checkedThumbColor = Color.White),
                )
            } else {
                Icon(Icons.Rounded.Lock, "Needs device support", tint = c.textDim, modifier = Modifier.size(20.dp))
            }
        }
    }
}

@Composable
fun SupportPill(level: SupportLevel, modifier: Modifier = Modifier) {
    val c = Neo.colors
    when (level) {
        SupportLevel.SUPPORTED -> Pill("Supported", c.green, modifier)
        SupportLevel.PARTIAL -> Pill("Partial", c.amber, modifier)
        SupportLevel.UNAVAILABLE -> Pill("Not available", c.textDim, modifier)
    }
}

/** A feature with its honest status and the reason behind it. */
@Composable
fun FeatureStatusRow(status: FeatureStatus, modifier: Modifier = Modifier) {
    val c = Neo.colors
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
        val tint = when (status.level) {
            SupportLevel.SUPPORTED -> c.green
            SupportLevel.PARTIAL -> c.amber
            SupportLevel.UNAVAILABLE -> c.textDim
        }
        Icon(
            if (status.level == SupportLevel.UNAVAILABLE) Icons.Rounded.Lock else Icons.Rounded.CheckCircle,
            null, tint = tint, modifier = Modifier.padding(top = 2.dp).size(20.dp),
        )
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(status.feature.label, style = MaterialTheme.typography.titleMedium, color = c.text, modifier = Modifier.weight(1f))
                SupportPill(status.level)
            }
            Text(status.reason, style = MaterialTheme.typography.bodySmall, color = c.textDim)
        }
    }
}

/** Battery glyph with percentage. A null value means it cannot be read and is shown as "n/a". */
@Composable
fun BatteryGlyph(percent: Int?, modifier: Modifier = Modifier) {
    val c = Neo.colors
    val fraction by animateFloatAsState((percent ?: 0) / 100f, tween(900), label = "battery")
    val tint = when {
        percent == null -> c.textDim
        percent <= 20 -> c.red
        else -> c.green
    }
    Row(modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Canvas(Modifier.size(width = 24.dp, height = 12.dp)) {
            drawRoundRect(tint.copy(alpha = 0.35f), size = Size(size.width - 3.dp.toPx(), size.height), cornerRadius = CornerRadius(3.dp.toPx()), style = Stroke(1.5.dp.toPx()))
            drawRoundRect(tint.copy(alpha = 0.35f), topLeft = Offset(size.width - 2.5.dp.toPx(), size.height * 0.3f), size = Size(2.5.dp.toPx(), size.height * 0.4f), cornerRadius = CornerRadius(1.dp.toPx()))
            if (percent != null) {
                drawRoundRect(tint, topLeft = Offset(2.dp.toPx(), 2.dp.toPx()), size = Size((size.width - 7.dp.toPx()) * fraction, size.height - 4.dp.toPx()), cornerRadius = CornerRadius(1.5.dp.toPx()))
            }
        }
        Text(percent?.let { "$it%" } ?: "n/a", color = if (percent == null) c.textDim else c.text, style = MaterialTheme.typography.titleMedium)
    }
}

@Composable
fun NeoSlider(value: Float, onValueChange: (Float) -> Unit, modifier: Modifier = Modifier, valueRange: ClosedFloatingPointRange<Float> = 0f..1f, enabled: Boolean = true, onFinished: (() -> Unit)? = null) {
    val c = Neo.colors
    Slider(
        value = value, onValueChange = onValueChange, modifier = modifier, valueRange = valueRange, enabled = enabled,
        onValueChangeFinished = onFinished,
        colors = SliderDefaults.colors(
            thumbColor = c.violet, activeTrackColor = c.violet, inactiveTrackColor = c.panelStrong,
            disabledThumbColor = c.textDim, disabledActiveTrackColor = c.panelStrong, disabledInactiveTrackColor = c.panel,
        ),
    )
}

// ------------------------------------------------------------------ brand art

/** The UniBud mark: a W inside an orbit ring. It draws itself in when [animateIn] is true. */
@Composable
fun LogoMark(modifier: Modifier = Modifier, animateIn: Boolean = true) {
    val c = Neo.colors
    val progress = remember { Animatable(if (animateIn) 0f else 1f) }
    LaunchedEffect(Unit) { if (animateIn) progress.animateTo(1f, tween(1200, easing = FastOutSlowInEasing)) }
    val pulse = rememberInfiniteTransition(label = "logo")
    val glow by pulse.animateFloat(0.35f, 0.75f, infiniteRepeatable(tween(2000, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "glow")
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        drawCircle(
            Brush.radialGradient(listOf(c.blue.copy(alpha = glow * 0.6f), Color.Transparent), center = center, radius = w * 0.55f),
            radius = w * 0.55f,
        )
        rotate(-16f, pivot = center) {
            drawArc(
                brush = Brush.sweepGradient(listOf(c.violet, c.blue, c.cyan, c.violet), center = center),
                startAngle = -200f, sweepAngle = 360f * progress.value, useCenter = false,
                topLeft = Offset(w * 0.04f, h * 0.40f), size = Size(w * 0.92f, h * 0.34f),
                style = Stroke(w * 0.022f, cap = StrokeCap.Round),
            )
        }
        val path = Path().apply {
            moveTo(w * 0.22f, h * 0.20f); lineTo(w * 0.38f, h * 0.72f); lineTo(w * 0.50f, h * 0.44f)
            lineTo(w * 0.62f, h * 0.72f); lineTo(w * 0.78f, h * 0.20f)
        }
        drawPath(
            path, Brush.linearGradient(listOf(c.cyan, c.blue, c.violet), start = Offset(w * 0.2f, h * 0.2f), end = Offset(w * 0.8f, h * 0.75f)),
            alpha = progress.value, style = Stroke(w * 0.09f, cap = StrokeCap.Round, join = StrokeJoin.Round),
        )
    }
}

/** Stylised earbuds and charging case with a glowing ring. Purely decorative. */
@Composable
fun EarbudsIllustration(modifier: Modifier = Modifier, animated: Boolean = true) {
    val c = Neo.colors
    val t = rememberInfiniteTransition(label = "hero")
    val glow by t.animateFloat(0.55f, 1f, infiniteRepeatable(tween(2400, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "glow")
    val bob by t.animateFloat(-3f, 3f, infiniteRepeatable(tween(3200, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "bob")
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        val a = if (animated) glow else 0.8f
        drawOval(
            Brush.radialGradient(listOf(c.violet.copy(alpha = 0.35f * a), Color.Transparent), center = Offset(w / 2, h * 0.8f), radius = w * 0.55f),
            topLeft = Offset(w * 0.02f, h * 0.60f), size = Size(w * 0.96f, h * 0.40f),
        )
        drawOval(
            Brush.horizontalGradient(listOf(Color.Transparent, c.violet.copy(alpha = a), c.cyan.copy(alpha = a * 0.8f), c.violet.copy(alpha = a), Color.Transparent)),
            topLeft = Offset(w * 0.04f, h * 0.74f), size = Size(w * 0.92f, h * 0.20f), style = Stroke(3.dp.toPx()),
        )
        val left = w * 0.24f
        val top = h * 0.54f
        val cw = w * 0.52f
        val ch = h * 0.30f
        drawRoundRect(
            Brush.verticalGradient(listOf(Color(0xFF2B2870), Color(0xFF0D0C2B))),
            topLeft = Offset(left, top), size = Size(cw, ch), cornerRadius = CornerRadius(ch * 0.3f),
        )
        drawRoundRect(
            Brush.linearGradient(listOf(c.violet.copy(alpha = 0.9f), c.blue.copy(alpha = 0.25f))),
            topLeft = Offset(left, top), size = Size(cw, ch), cornerRadius = CornerRadius(ch * 0.3f), style = Stroke(1.5.dp.toPx()),
        )
        drawLine(c.violet.copy(alpha = 0.6f), Offset(left + cw * 0.05f, top + ch * 0.36f), Offset(left + cw * 0.95f, top + ch * 0.36f), strokeWidth = 2.dp.toPx())
        listOf(0.37f to 1f, 0.63f to -1f).forEach { (fx, dir) ->
            val cx = w * fx
            val cy = h * 0.36f + (if (animated) bob * dir else 0f)
            drawRoundRect(
                Brush.verticalGradient(listOf(Color(0xFF3A3790), Color(0xFF14123A))),
                topLeft = Offset(cx - w * 0.02f, cy), size = Size(w * 0.04f, h * 0.2f), cornerRadius = CornerRadius(w * 0.02f),
            )
            drawCircle(
                Brush.radialGradient(listOf(Color(0xFF6A66D8), Color(0xFF18164A)), center = Offset(cx - w * 0.02f, cy - w * 0.02f), radius = w * 0.11f),
                radius = w * 0.085f, center = Offset(cx, cy),
            )
            drawCircle(c.violet.copy(alpha = 0.7f), radius = w * 0.085f, center = Offset(cx, cy), style = Stroke(1.2.dp.toPx()))
        }
    }
}

private class Particle(val x: Float, val y: Float, val loops: Int, val radius: Float, val phase: Float)

/** Slowly rising, twinkling dots. Loops seamlessly. */
@Composable
fun ParticleField(modifier: Modifier = Modifier, count: Int = 28) {
    val c = Neo.colors
    val particles = remember {
        val r = Random(42)
        List(count) { Particle(r.nextFloat(), r.nextFloat(), 1 + r.nextInt(2), 1f + r.nextFloat() * 2.2f, r.nextFloat()) }
    }
    val transition = rememberInfiniteTransition(label = "particles")
    val time by transition.animateFloat(0f, 1f, infiniteRepeatable(tween(16000, easing = LinearEasing)), label = "time")
    Canvas(modifier) {
        particles.forEach { p ->
            val y = ((p.y - time * p.loops) % 1f + 1f) % 1f
            val twinkle = 0.35f + 0.65f * abs(sin(time * 2f * PI.toFloat() * 3f + p.phase * 2f * PI.toFloat()))
            drawCircle(
                (if (p.phase > 0.5f) c.cyan else c.violet).copy(alpha = 0.6f * twinkle),
                radius = p.radius.dp.toPx(), center = Offset(p.x * size.width, y * size.height),
            )
        }
    }
}

/** Flowing neon waves for the bottom of the sign-in screen. */
@Composable
fun WaveBackdrop(modifier: Modifier = Modifier) {
    val c = Neo.colors
    val transition = rememberInfiniteTransition(label = "waves")
    val phase by transition.animateFloat(0f, 2f * PI.toFloat(), infiniteRepeatable(tween(9000, easing = LinearEasing)), label = "phase")
    Canvas(modifier) {
        listOf(Triple(0.45f, 0.10f, c.violet), Triple(0.60f, 0.07f, c.blue), Triple(0.75f, 0.05f, c.cyan)).forEachIndexed { i, (yFrac, amp, color) ->
            val path = Path()
            val steps = 56
            for (s in 0..steps) {
                val x = size.width * s / steps
                val y = size.height * (yFrac + amp * sin(phase + s * 0.16f + i))
                if (s == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }
            drawPath(path, color.copy(alpha = 0.6f), style = Stroke(2.dp.toPx(), cap = StrokeCap.Round))
        }
    }
}

/** Expanding rings while [active]. The animation only exists while a scan is running. */
@Composable
fun RadarScan(active: Boolean, modifier: Modifier = Modifier) {
    val c = Neo.colors
    Box(modifier.size(160.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            drawCircle(c.violet.copy(alpha = 0.14f), radius = size.minDimension / 2 * 0.34f)
        }
        if (active) RadarRings()
        Icon(Icons.Rounded.Bluetooth, null, tint = c.cyan, modifier = Modifier.size(34.dp))
    }
}

@Composable
private fun RadarRings() {
    val c = Neo.colors
    val transition = rememberInfiniteTransition(label = "radar")
    val phase by transition.animateFloat(0f, 1f, infiniteRepeatable(tween(2600, easing = LinearEasing), RepeatMode.Restart), label = "phase")
    Canvas(Modifier.fillMaxSize()) {
        val maxRadius = size.minDimension / 2
        repeat(3) { i ->
            val p = (phase + i / 3f) % 1f
            drawCircle(
                c.violet.copy(alpha = (1f - p) * 0.7f),
                radius = maxRadius * (0.34f + 0.66f * p), style = Stroke(2.dp.toPx()),
            )
        }
    }
}

/** One vertical equalizer band. Drag or tap anywhere along it. Values snap to 0.5 dB. */
@Composable
fun VerticalBandSlider(value: Float, onValueChange: (Float) -> Unit, modifier: Modifier = Modifier, min: Float = -12f, max: Float = 12f) {
    val c = Neo.colors
    val current by rememberUpdatedState(onValueChange)
    Canvas(
        modifier.pointerInput(min, max) {
            awaitEachGesture {
                fun apply(y: Float) {
                    val f = (y / size.height).coerceIn(0f, 1f)
                    current(((max - f * (max - min)) * 2f).roundToInt() / 2f)
                }
                val down = awaitFirstDown()
                apply(down.position.y)
                do {
                    val event = awaitPointerEvent()
                    val change = event.changes.firstOrNull()
                    if (change != null && change.pressed) {
                        apply(change.position.y)
                        change.consume()
                    }
                } while (event.changes.any { it.pressed })
            }
        },
    ) {
        val cx = size.width / 2
        val pad = 12.dp.toPx()
        val usable = size.height - 2 * pad
        val y = pad + (max - value.coerceIn(min, max)) / (max - min) * usable
        val mid = pad + usable / 2
        drawLine(c.panelStrong, Offset(cx, pad), Offset(cx, size.height - pad), strokeWidth = 3.dp.toPx(), cap = StrokeCap.Round)
        drawLine(
            Brush.verticalGradient(listOf(c.violet, c.blue), startY = minOf(y, mid), endY = maxOf(y, mid)),
            Offset(cx, mid), Offset(cx, y), strokeWidth = 4.dp.toPx(), cap = StrokeCap.Round,
        )
        drawCircle(c.violet.copy(alpha = 0.25f), radius = 14.dp.toPx(), center = Offset(cx, y))
        drawCircle(Color.White, radius = 7.dp.toPx(), center = Offset(cx, y))
        drawCircle(c.violet, radius = 7.dp.toPx(), center = Offset(cx, y), style = Stroke(2.dp.toPx()))
    }
}

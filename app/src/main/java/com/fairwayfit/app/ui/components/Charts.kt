package com.fairwayfit.app.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Circular progress ring that animates from empty to [progress] (0..1) when first shown or changed. */
@Composable
fun ProgressRing(
    progress: Float,
    modifier: Modifier = Modifier,
    strokeWidth: Dp = 12.dp,
    color: Color = MaterialTheme.colorScheme.primary,
    trackColor: Color = MaterialTheme.colorScheme.surfaceContainerHighest,
    content: @Composable BoxScope.() -> Unit = {}
) {
    val animated = remember { Animatable(0f) }
    LaunchedEffect(progress) {
        animated.animateTo(progress.coerceIn(0f, 1f), tween(900, easing = FastOutSlowInEasing))
    }
    Box(modifier, contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val stroke = Stroke(width = strokeWidth.toPx(), cap = StrokeCap.Round)
            val inset = strokeWidth.toPx() / 2f
            val arcSize = Size(size.width - inset * 2, size.height - inset * 2)
            val topLeft = Offset(inset, inset)
            drawArc(trackColor, 0f, 360f, false, topLeft, arcSize, style = stroke)
            if (animated.value > 0.001f) {
                drawArc(color, -90f, 360f * animated.value, false, topLeft, arcSize, style = stroke)
            }
        }
        content()
    }
}

/** Horizontal bar that fills to [fraction] (0..1). */
@Composable
fun ScoreBar(
    fraction: Float,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary,
    trackColor: Color = MaterialTheme.colorScheme.surfaceContainerHighest,
    height: Dp = 8.dp
) {
    val animated = remember { Animatable(0f) }
    LaunchedEffect(fraction) {
        animated.animateTo(fraction.coerceIn(0f, 1f), tween(700, easing = FastOutSlowInEasing))
    }
    Box(
        modifier
            .fillMaxWidth()
            .height(height)
            .clip(CircleShape)
            .background(trackColor)
    ) {
        Box(
            Modifier
                .fillMaxHeight()
                .fillMaxWidth(animated.value)
                .clip(CircleShape)
                .background(color)
        )
    }
}

private fun smoothPath(points: List<Offset>): Path {
    val path = Path()
    path.moveTo(points.first().x, points.first().y)
    for (i in 1 until points.size) {
        val a = points[i - 1]
        val b = points[i]
        val midX = (a.x + b.x) / 2f
        path.cubicTo(midX, a.y, midX, b.y, b.x, b.y)
    }
    return path
}

/** Full chart with faint grid lines, a smooth gradient-filled line and first/last x labels. */
@Composable
fun LineChart(
    values: List<Float>,
    xLabels: List<String>,
    modifier: Modifier = Modifier,
    yMin: Float = 0f,
    yMax: Float = 100f,
    color: Color = MaterialTheme.colorScheme.primary
) {
    require(values.isNotEmpty()) { "LineChart needs at least one value" }
    require(values.size == xLabels.size) { "values and xLabels must have the same size" }
    val reveal = remember { Animatable(0f) }
    LaunchedEffect(values) {
        reveal.snapTo(0f)
        reveal.animateTo(1f, tween(1000, easing = FastOutSlowInEasing))
    }
    val gridColor = MaterialTheme.colorScheme.outlineVariant
    val dotCore = MaterialTheme.colorScheme.background

    Column(modifier) {
        Canvas(
            Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            val padX = 12.dp.toPx()
            val padY = 10.dp.toPx()
            val w = size.width - padX * 2
            val h = size.height - padY * 2
            val range = (yMax - yMin).takeIf { it > 0f } ?: 1f

            val dash = PathEffect.dashPathEffect(floatArrayOf(10f, 10f))
            for (i in 0..2) {
                val y = padY + h * i / 2f
                drawLine(gridColor, Offset(0f, y), Offset(size.width, y), strokeWidth = 1.dp.toPx(), pathEffect = dash)
            }

            val points = values.mapIndexed { i, v ->
                val x = if (values.size == 1) padX + w / 2f else padX + w * i / (values.size - 1)
                val y = padY + h - ((v - yMin) / range).coerceIn(0f, 1f) * h
                Offset(x, y)
            }

            clipRect(right = size.width * reveal.value) {
                if (points.size > 1) {
                    val line = smoothPath(points)
                    val fill = Path().apply {
                        addPath(line)
                        lineTo(points.last().x, size.height)
                        lineTo(points.first().x, size.height)
                        close()
                    }
                    drawPath(fill, Brush.verticalGradient(listOf(color.copy(alpha = 0.32f), Color.Transparent)))
                    drawPath(line, color, style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
                }
                points.forEachIndexed { i, p ->
                    val isLast = i == points.lastIndex
                    drawCircle(color, radius = if (isLast) 7.dp.toPx() else 4.dp.toPx(), center = p)
                    drawCircle(dotCore, radius = if (isLast) 3.dp.toPx() else 1.8.dp.toPx(), center = p)
                }
            }
        }
        Row(
            Modifier
                .fillMaxWidth()
                .height(20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            Text(xLabels.first(), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (xLabels.size > 1) {
                Text(xLabels.last(), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

/** Tiny axis-less trend line, scaled to its own min/max. */
@Composable
fun Sparkline(
    values: List<Float>,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary
) {
    val dotCore = MaterialTheme.colorScheme.surfaceContainerHigh
    Canvas(modifier) {
        if (values.isEmpty()) return@Canvas
        val pad = 4.dp.toPx()
        val w = size.width - pad * 2
        val h = size.height - pad * 2
        val lo = values.min()
        val hi = values.max()
        val range = (hi - lo).takeIf { it > 0f }
        val points = values.mapIndexed { i, v ->
            val x = if (values.size == 1) pad + w / 2f else pad + w * i / (values.size - 1)
            val y = if (range == null) pad + h / 2f else pad + h - ((v - lo) / range) * h
            Offset(x, y)
        }
        if (points.size > 1) {
            drawPath(smoothPath(points), color, style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
        }
        drawCircle(color, radius = 5.dp.toPx(), center = points.last())
        drawCircle(dotCore, radius = 2.2.dp.toPx(), center = points.last())
    }
}

/** Simple rounded bar chart with a label under every bar. The highlighted index uses the accent color. */
@Composable
fun BarChart(
    values: List<Int>,
    labels: List<String>,
    modifier: Modifier = Modifier,
    highlightIndex: Int = values.lastIndex,
    color: Color = MaterialTheme.colorScheme.primary,
    mutedColor: Color = MaterialTheme.colorScheme.surfaceContainerHighest
) {
    require(values.size == labels.size) { "values and labels must have the same size" }
    val reveal = remember { Animatable(0f) }
    LaunchedEffect(values) {
        reveal.snapTo(0f)
        reveal.animateTo(1f, tween(800, easing = FastOutSlowInEasing))
    }
    val max = (values.maxOrNull() ?: 0).coerceAtLeast(1)
    Column(modifier) {
        Canvas(
            Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            val count = values.size
            val gap = 10.dp.toPx()
            val barW = (size.width - gap * (count - 1)) / count
            values.forEachIndexed { i, v ->
                val fullH = size.height * (v.toFloat() / max)
                val barH = maxOf(fullH * reveal.value, 6.dp.toPx())
                val x = i * (barW + gap)
                drawRoundRect(
                    color = if (i == highlightIndex) color else mutedColor,
                    topLeft = Offset(x, size.height - barH),
                    size = Size(barW, barH),
                    cornerRadius = CornerRadius(8.dp.toPx(), 8.dp.toPx())
                )
            }
        }
        Row(Modifier.fillMaxWidth().height(20.dp), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.Bottom) {
            labels.forEach {
                Text(
                    it,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        }
    }
}

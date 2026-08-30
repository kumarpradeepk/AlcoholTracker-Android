package com.mtss.alcoholtracker.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mtss.alcoholtracker.ui.theme.LocalAppColors
import com.mtss.alcoholtracker.ui.theme.LocalReducedMotion
import com.mtss.alcoholtracker.ui.theme.Motion
import com.mtss.alcoholtracker.ui.theme.text

/** Animated progress ring — the intake ring and the dry-days ring. */
@Composable
fun ProgressRing(
    progress: Float,
    diameter: Dp,
    strokeWidth: Dp,
    color: Color,
    modifier: Modifier = Modifier,
    center: (@Composable () -> Unit)? = null
) {
    val c = LocalAppColors.current
    val reduced = LocalReducedMotion.current
    val anim = remember { Animatable(0f) }
    LaunchedEffect(progress) {
        if (reduced) anim.snapTo(progress.coerceIn(0f, 1f))
        else anim.animateTo(progress.coerceIn(0f, 1f), tween(850, easing = Motion.BarGrow))
    }
    Box(modifier.size(diameter), contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(diameter)) {
            val sw = strokeWidth.toPx()
            val inset = sw / 2f
            drawArc(
                color = c.surface2,
                startAngle = 0f, sweepAngle = 360f, useCenter = false,
                topLeft = Offset(inset, inset),
                size = Size(size.width - sw, size.height - sw),
                style = Stroke(sw, cap = StrokeCap.Round)
            )
            if (anim.value > 0f) {
                drawArc(
                    color = color,
                    startAngle = -90f, sweepAngle = 360f * anim.value, useCenter = false,
                    topLeft = Offset(inset, inset),
                    size = Size(size.width - sw, size.height - sw),
                    style = Stroke(sw, cap = StrokeCap.Round)
                )
            }
        }
        if (center != null) center()
    }
}

data class BarDatum(val label: String, val fraction: Float, val color: Color)

/** Staggered animated bar chart used by Units Consumed and Spending Trend. */
@Composable
fun BarChart(
    bars: List<BarDatum>,
    chartHeight: Dp,
    averageFraction: Float? = null,
    modifier: Modifier = Modifier
) {
    val c = LocalAppColors.current
    val reduced = LocalReducedMotion.current
    val grow = remember(bars.size) { Animatable(0f) }
    LaunchedEffect(bars) {
        grow.snapTo(0f)
        if (reduced) grow.snapTo(1f)
        else grow.animateTo(1f, tween(700, easing = Motion.BarGrow))
    }
    Column(modifier.fillMaxWidth()) {
        Box(Modifier.fillMaxWidth().height(chartHeight)) {
            Canvas(Modifier.fillMaxWidth().height(chartHeight)) {
                val gap = 3.dp.toPx()
                val n = bars.size.coerceAtLeast(1)
                val bw = (size.width - gap * (n - 1)) / n
                bars.forEachIndexed { i, bar ->
                    // Stagger: each bar joins the growth slightly later.
                    val delayFrac = if (n > 1) i.toFloat() / n * 0.3f else 0f
                    val local = ((grow.value - delayFrac) / (1f - delayFrac)).coerceIn(0f, 1f)
                    val hFrac = bar.fraction.coerceIn(0.02f, 1f) * local
                    val h = size.height * hFrac
                    drawRoundRect(
                        bar.color,
                        topLeft = Offset(i * (bw + gap), size.height - h),
                        size = Size(bw, h),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(3.dp.toPx())
                    )
                }
                if (averageFraction != null) {
                    val y = size.height * (1f - averageFraction.coerceIn(0f, 0.96f))
                    drawLine(
                        c.faint, Offset(0f, y), Offset(size.width, y),
                        strokeWidth = 1.5.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f))
                    )
                }
            }
        }
        if (bars.any { it.label.isNotEmpty() }) {
            Row(
                Modifier.fillMaxWidth().padding(top = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                bars.forEach { bar ->
                    Text(
                        bar.label,
                        style = text(9.sp),
                        color = c.faint,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

/** BAC trend polyline with a soft area fill, like the mock's 7-day chart. */
@Composable
fun TrendChart(
    values: List<Float>,
    chartHeight: Dp,
    modifier: Modifier = Modifier
) {
    val c = LocalAppColors.current
    Canvas(modifier.fillMaxWidth().height(chartHeight)) {
        if (values.size < 2) return@Canvas
        val maxV = maxOf(0.04f, values.max())
        val stepX = size.width / (values.size - 1)
        val pts = values.mapIndexed { i, v ->
            Offset(i * stepX, size.height - (v / maxV) * size.height * 0.83f - size.height * 0.05f)
        }
        val area = Path().apply {
            moveTo(pts.first().x, size.height)
            pts.forEach { lineTo(it.x, it.y) }
            lineTo(pts.last().x, size.height)
            close()
        }
        drawPath(area, c.accent.copy(alpha = 0.18f))
        val line = Path().apply {
            moveTo(pts.first().x, pts.first().y)
            pts.drop(1).forEach { lineTo(it.x, it.y) }
        }
        drawPath(line, c.accent, style = Stroke(2.5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
}

/** Small horizontal share bar for the drink breakdown rows. */
@Composable
fun ShareBar(fraction: Float, modifier: Modifier = Modifier) {
    val c = LocalAppColors.current
    val reduced = LocalReducedMotion.current
    val anim = remember { Animatable(0f) }
    LaunchedEffect(fraction) {
        if (reduced) anim.snapTo(fraction) else anim.animateTo(fraction, tween(800, easing = Motion.BarGrow))
    }
    Canvas(modifier.fillMaxWidth().height(6.dp)) {
        drawRoundRect(c.surface2, cornerRadius = androidx.compose.ui.geometry.CornerRadius(3.dp.toPx()))
        drawRoundRect(
            c.accent,
            size = Size(size.width * anim.value.coerceIn(0.04f, 1f), size.height),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(3.dp.toPx())
        )
    }
}

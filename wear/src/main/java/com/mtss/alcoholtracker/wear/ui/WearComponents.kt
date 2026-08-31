package com.mtss.alcoholtracker.wear.ui

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

// ── Motion, from the watch canvas ───────────────────────────────────────

/** `cubic-bezier(.32,.72,.33,1)` — the page strip. */
val WSettle: Easing = CubicBezierEasing(0.32f, 0.72f, 0.33f, 1f)

/** `cubic-bezier(.3,.7,.3,1.2)` — the page dots widening. */
val WOvershoot: Easing = CubicBezierEasing(0.3f, 0.7f, 0.3f, 1.2f)

/** `cubic-bezier(.3,.7,.3,1)` — the ring sweep. */
val WSweep: Easing = CubicBezierEasing(0.3f, 0.7f, 0.3f, 1f)

/** `cubic-bezier(.3,.7,.3,1.5)` — the confirmation pop. */
val WPop: Easing = CubicBezierEasing(0.3f, 0.7f, 0.3f, 1.5f)

/**
 * Press feedback: `transition:transform .15s` with `scale(.9…96)`.
 *
 * No ripple — Material's indication is wrong on this black ground and the
 * canvas expresses every press as a scale instead.
 */
fun Modifier.pressable(
    pressedScale: Float = 0.95f,
    enabled: Boolean = true,
    onClick: () -> Unit
): Modifier = composed {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) pressedScale else 1f,
        animationSpec = tween(150),
        label = "press"
    )
    this
        .scale(scale)
        .clickable(
            interactionSource = interaction,
            indication = null,
            enabled = enabled,
            onClick = onClick
        )
}

/** `animation:fadeUp .4s <delay> both`, the watch's 12px variant. */
fun Modifier.wFadeUp(delayMillis: Int = 0): Modifier = composed {
    var shown by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { shown = true }
    val p by animateFloatAsState(
        targetValue = if (shown) 1f else 0f,
        animationSpec = tween(400, delayMillis = delayMillis, easing = WSettle),
        label = "wFadeUp"
    )
    this
        .alpha(p)
        .graphicsLayer { translationY = (1f - p) * 12.dp.toPx() }
}

// ── Atoms ───────────────────────────────────────────────────────────────

/** The app's droplet mark, breathing on the splash. */
@Composable
fun WatchDroplet(size: Dp = 54.dp, color: Color = W.acc, breathing: Boolean = true) {
    val t = rememberInfiniteTransition(label = "breathe")
    val s by t.animateFloat(
        initialValue = 1f,
        targetValue = if (breathing) 1.13f else 1f,
        animationSpec = infiniteRepeatable(tween(800, easing = WSweep), RepeatMode.Reverse),
        label = "breatheScale"
    )
    Box(
        Modifier
            .size(size)
            .scale(s)
            .rotate(45f)
            .clip(RoundedCornerShape(topStart = size / 2, topEnd = size / 2, bottomEnd = size / 2, bottomStart = size / 10))
            .background(color)
    )
}

/**
 * The post-log confirmation: a ripple ring, a popped moss tile, and the tick
 * drawing itself in — the canvas's `ripple` + `pop` + `idraw` together.
 */
@Composable
fun ConfirmCheck() {
    var go by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { go = true }

    val ripple by animateFloatAsState(
        targetValue = if (go) 1f else 0f,
        animationSpec = tween(1000, delayMillis = 100),
        label = "ripple"
    )
    val pop by animateFloatAsState(
        targetValue = if (go) 1f else 0f,
        animationSpec = tween(550, easing = WPop),
        label = "pop"
    )
    val draw by animateFloatAsState(
        targetValue = if (go) 1f else 0f,
        animationSpec = tween(500, delayMillis = 200),
        label = "idraw"
    )

    Box(Modifier.size(104.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val r = size.minDimension / 2f - 20.dp.toPx()
            drawCircle(
                color = W.moss.copy(alpha = (1f - ripple) * 0.65f),
                radius = r * (0.55f + 1.15f * ripple),
                style = Stroke(2.5.dp.toPx())
            )
        }
        Box(
            Modifier
                .size(62.dp)
                .scale(0.82f + 0.26f * pop)
                .clip(RoundedCornerShape(23.dp))
                .background(W.moss),
            contentAlignment = Alignment.Center
        ) {
            Canvas(Modifier.size(26.dp)) {
                // The canvas's check: M5 12.5l4.5 4.5L19 7.5 on a 24 grid.
                val k = size.minDimension / 24f
                val p = Path().apply {
                    moveTo(5f * k, 12.5f * k)
                    lineTo(9.5f * k, 17f * k)
                    lineTo(19f * k, 7.5f * k)
                }
                val m = PathMeasure().apply { setPath(p, false) }
                val seg = Path()
                if (m.getSegment(0f, m.length * draw, seg, true)) {
                    drawPath(
                        seg, Color.White,
                        style = Stroke(2.8f * k, cap = StrokeCap.Round, join = StrokeJoin.Round)
                    )
                }
            }
        }
    }
}

/**
 * The glance ring. Sweeps its arc rather than snapping, matching the canvas's
 * 700 ms `stroke-dashoffset` transition.
 */
@Composable
fun WatchRing(
    progress: Float,
    color: Color,
    diameter: Dp = 104.dp,
    stroke: Dp = 9.dp,
    content: @Composable () -> Unit
) {
    val p by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = tween(700, easing = WSweep),
        label = "ring"
    )
    Box(Modifier.size(diameter), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val w = stroke.toPx()
            val box = Size(size.width - w, size.height - w)
            val at = Offset(w / 2, w / 2)
            drawArc(W.elev, 0f, 360f, false, at, box, style = Stroke(w))
            drawArc(
                color, -90f, 360f * p, false, at, box,
                style = Stroke(w, cap = StrokeCap.Round)
            )
        }
        content()
    }
}

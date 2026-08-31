package com.mtss.alcoholtracker.ui.theme

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp

/**
 * The canvas's motion vocabulary, one-for-one with its `@keyframes` block and
 * its `transition:` declarations.
 *
 * Durations and curves are the design's own numbers rather than Material
 * defaults; the whole feel of this direction is carried by them, so they are
 * kept literal and named after the keyframe they came from.
 */
object Motion {

    // ── Curves ──────────────────────────────────────────────────────────

    /** `cubic-bezier(.32,.72,.33,1)` — sheets, panels, slide-ins. */
    val Settle: Easing = CubicBezierEasing(0.32f, 0.72f, 0.33f, 1f)

    /** `cubic-bezier(.3,.7,.3,1)` — the ring sweep and width growth. */
    val Sweep: Easing = CubicBezierEasing(0.3f, 0.7f, 0.3f, 1f)

    /** `cubic-bezier(.3,.7,.3,1.2)` — a mild overshoot, e.g. the segmented thumb. */
    val Overshoot: Easing = CubicBezierEasing(0.3f, 0.7f, 0.3f, 1.2f)

    /** `cubic-bezier(.3,.7,.3,1.3)` — the toast's livelier arrival. */
    val OvershootToast: Easing = CubicBezierEasing(0.3f, 0.7f, 0.3f, 1.3f)

    /** `cubic-bezier(.3,.7,.3,1.5)` — the strongest pop in the design. */
    val OvershootPop: Easing = CubicBezierEasing(0.3f, 0.7f, 0.3f, 1.5f)

    /** `cubic-bezier(.2,.75,.2,1)` — the odometer digit rise. */
    val Rise: Easing = CubicBezierEasing(0.2f, 0.75f, 0.2f, 1f)

    // ── Durations, in the canvas's milliseconds ─────────────────────────

    const val FADE_UP = 400
    const val FADE_IN = 320
    const val POP = 500
    const val SHEET_UP = 500
    const val SLIDE_IN = 420
    const val TOAST_UP = 350
    const val GROW_BAR = 500
    const val IDRAW = 600
    const val RISE_IN = 550
    const val RIPPLE = 1000
    const val DOT_FLY = 950
    const val OVERLAY = 1250

    /** The ring's `stroke-dashoffset .7s`. */
    const val RING = 700

    /** `transition:transform .15s` — the press response on every control. */
    const val PRESS = 150

    /**
     * The global `transition:background-color .3s,color .3s,border-color .3s,
     * fill .3s,stroke .3s` that carries the light/dark cross-fade.
     */
    const val THEME = 300

    /** Entry stagger between siblings; the canvas steps in 20 ms. */
    const val STAGGER = 20

    fun <T> settle(durationMillis: Int = SHEET_UP): FiniteAnimationSpec<T> =
        tween(durationMillis, easing = Settle)

    fun <T> sweep(durationMillis: Int = RING): FiniteAnimationSpec<T> =
        tween(durationMillis, easing = Sweep)

    fun <T> theme(): FiniteAnimationSpec<T> = tween(THEME, easing = LinearEasing)
}

/** True when the system asks for reduced motion; durations collapse to ~0. */
val LocalReducedMotion = staticCompositionLocalOf { false }

fun duration(reduced: Boolean, millis: Int): Int = if (reduced) 1 else millis

/**
 * `animation:fadeUp .4s <delay> both` — the design's universal entrance:
 * 14dp up and a fade, staggered down the screen.
 */
fun Modifier.fadeUp(
    delayMillis: Int = 0,
    durationMillis: Int = Motion.FADE_UP
): Modifier = composed {
    val reduced = LocalReducedMotion.current
    var shown by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { shown = true }
    val p by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (shown) 1f else 0f,
        animationSpec = tween(
            durationMillis = duration(reduced, durationMillis),
            delayMillis = duration(reduced, delayMillis),
            easing = Motion.Settle
        ),
        label = "fadeUp"
    )
    this
        .alpha(p)
        .graphicsLayer { translationY = (1f - p) * 14.dp.toPx() }
}

/** Convenience for a staggered list: `Modifier.fadeUpIndex(i)`. */
fun Modifier.fadeUpIndex(index: Int, step: Int = Motion.STAGGER): Modifier =
    fadeUp(delayMillis = index * step)

/**
 * `animation:pop .5s both` — scale .82 → 1.08 → 1. Used where something
 * arrives that the user just created.
 */
fun Modifier.pop(startDelay: Int = 0): Modifier = composed {
    val reduced = LocalReducedMotion.current
    var shown by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { shown = true }
    // The canvas's three stops, animated on the scale directly. The parameter
    // is `startDelay` and not `delayMillis` because the keyframes DSL exposes a
    // property of that name, which a same-named parameter would shadow.
    val scale by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (shown) 1f else 0.82f,
        animationSpec = keyframes {
            durationMillis = duration(reduced, Motion.POP)
            delayMillis = duration(reduced, startDelay)
            0.82f at 0
            1.08f at durationMillis * 60 / 100
            1f at durationMillis
        },
        label = "pop"
    )
    graphicsLayer { scaleX = scale; scaleY = scale }
}

/**
 * `animation:breathe` — the splash droplet, scale 1 ⇄ 1.13.
 * Returns the current scale so the caller can apply it where it belongs.
 */
@Composable
fun rememberBreathe(periodMillis: Int = 1600): Float {
    if (LocalReducedMotion.current) return 1f
    val t = rememberInfiniteTransition(label = "breathe")
    val v by t.animateFloat(
        initialValue = 1f,
        targetValue = 1.13f,
        animationSpec = infiniteRepeatable(
            tween(periodMillis / 2, easing = Motion.Sweep), RepeatMode.Reverse
        ),
        label = "breatheScale"
    )
    return v
}

/**
 * `animation:fizzIn` — a bubble rising inside the glass mark. Returns 0→1 on a
 * loop; the caller maps it to offset, scale and opacity.
 */
@Composable
fun rememberFizz(periodMillis: Int, delayMillis: Int = 0): Float {
    if (LocalReducedMotion.current) return 0f
    val t = rememberInfiniteTransition(label = "fizz")
    val v by t.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            tween(periodMillis, delayMillis = delayMillis, easing = LinearEasing),
            RepeatMode.Restart
        ),
        label = "fizzProgress"
    )
    return v
}

/**
 * `animation:idraw .6s <delay> ease both` — the stroke-on that draws the line
 * icons. Returns the fraction of the path to reveal.
 *
 * Every icon in this design is a 24×24 stroked path, and they draw themselves
 * in rather than appearing; this is what makes the set feel like one system.
 */
@Composable
fun rememberIconDraw(delayMillis: Int = 0, durationMillis: Int = Motion.IDRAW): Float {
    val reduced = LocalReducedMotion.current
    var shown by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { shown = true }
    val p by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (shown) 1f else 0f,
        animationSpec = tween(
            durationMillis = duration(reduced, durationMillis),
            delayMillis = duration(reduced, delayMillis)
        ),
        label = "idraw"
    )
    return if (reduced) 1f else p
}

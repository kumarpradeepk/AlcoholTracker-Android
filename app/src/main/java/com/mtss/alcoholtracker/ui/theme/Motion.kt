package com.mtss.alcoholtracker.ui.theme

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.runtime.staticCompositionLocalOf

/**
 * The mock's motion vocabulary. `Settle` is its workhorse
 * cubic-bezier(.32,.72,0,1); `Springy` approximates the overshoot
 * cubic-bezier(.34,1.45,.5,1) used for presses and pops.
 */
object Motion {
    val Settle: Easing = CubicBezierEasing(0.32f, 0.72f, 0f, 1f)
    val Springy: Easing = CubicBezierEasing(0.34f, 1.45f, 0.5f, 1f)
    val SpringyMild: Easing = CubicBezierEasing(0.34f, 1.2f, 0.4f, 1f)
    val BarGrow: Easing = CubicBezierEasing(0.3f, 0.9f, 0.3f, 1f)
}

/** True when the system asks for reduced motion; durations collapse to ~0. */
val LocalReducedMotion = staticCompositionLocalOf { false }

fun duration(reduced: Boolean, millis: Int): Int = if (reduced) 1 else millis

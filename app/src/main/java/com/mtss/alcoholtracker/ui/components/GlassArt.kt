package com.mtss.alcoholtracker.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.mtss.alcoholtracker.ui.theme.LocalAppColors
import com.mtss.alcoholtracker.ui.theme.LocalReducedMotion
import kotlin.math.min

/**
 * The design's signature empty-state artwork: a tall glass whose liquid is
 * two slowly counter-rotating blobs, with tiny bubbles drifting up. All drawn
 * in code, with restraint.
 */
@Composable
fun SwirlingGlass(
    width: Dp,
    height: Dp,
    liquid: Color? = null,
    borderColor: Color? = null,
    breathe: Boolean = true
) {
    val c = LocalAppColors.current
    val reduced = LocalReducedMotion.current
    val liquidColor = liquid ?: c.tide
    val border = borderColor ?: c.ink.copy(alpha = 0.22f)

    val transition = rememberInfiniteTransition(label = "swirl")
    val angle by transition.animateFloat(
        0f, 360f,
        infiniteRepeatable(tween(11000, easing = LinearEasing)),
        label = "a1"
    )
    val angle2 by transition.animateFloat(
        360f, 0f,
        infiniteRepeatable(tween(14000, easing = LinearEasing)),
        label = "a2"
    )
    val drift by transition.animateFloat(
        0f, 1f,
        infiniteRepeatable(tween(5400, easing = LinearEasing)),
        label = "drift"
    )
    val breatheScale = if (breathe && !reduced) 1f + 0.03f * rememberInfiniteBreath(7000) else 1f

    Canvas(Modifier.size(width, height).scale(breatheScale)) {
        val w = size.width
        val h = size.height
        val glassPath = Path().apply {
            addRoundRect(
                RoundRect(
                    0f, 0f, w, h,
                    topLeftCornerRadius = CornerRadius(w * 0.1f),
                    topRightCornerRadius = CornerRadius(w * 0.1f),
                    bottomLeftCornerRadius = CornerRadius(w * 0.27f),
                    bottomRightCornerRadius = CornerRadius(w * 0.27f)
                )
            )
        }
        clipPath(glassPath) {
            if (reduced) {
                // Still water for reduced motion: a simple fill, no churn.
                drawRect(liquidColor.copy(alpha = 0.5f), Offset(0f, h * 0.55f), androidx.compose.ui.geometry.Size(w, h * 0.45f))
            } else {
                val blobR = w * 1.25f
                drawBlob(Offset(w / 2f, h + blobR * 0.42f), blobR, angle, liquidColor.copy(alpha = 0.6f))
                drawBlob(Offset(w / 2f + w * 0.06f, h + blobR * 0.46f), blobR, angle2, liquidColor.copy(alpha = 0.3f))
                // Bubbles: three offsets on the same driver, staggered by phase.
                drawBubble(w * 0.32f, h, drift, 0f, w * 0.052f)
                drawBubble(w * 0.58f, h, (drift + 0.35f) % 1f, 0.35f, w * 0.042f)
                drawBubble(w * 0.45f, h, (drift + 0.68f) % 1f, 0.68f, w * 0.032f)
            }
        }
        drawPath(glassPath, border, style = Stroke(width = 2.5f * density))
    }
}

private fun DrawScope.drawBlob(center: Offset, radius: Float, angleDeg: Float, color: Color) {
    rotate(angleDeg, pivot = center) {
        // A slightly squashed oval reads as liquid once it spins.
        val path = Path().apply {
            addOval(
                androidx.compose.ui.geometry.Rect(
                    center.x - radius, center.y - radius * 0.9f,
                    center.x + radius * 0.94f, center.y + radius * 0.92f
                )
            )
        }
        drawPath(path, color)
    }
}

private fun DrawScope.drawBubble(x: Float, h: Float, t: Float, phase: Float, r: Float) {
    val travel = h * 0.36f
    val y = h - h * 0.11f - travel * t
    val alpha = when {
        t < 0.15f -> t / 0.15f * 0.75f
        t > 0.85f -> (1f - t) / 0.15f * 0.75f
        else -> 0.75f
    }
    drawCircle(Color.White.copy(alpha = alpha), r, Offset(x, y))
}

/**
 * The FAB's cocktail glass, from the mock's inline SVG: clipped bowl with a
 * bobbing liquid wave and fizz, stem and foot below.
 */
@Composable
fun FabGlass(size: Dp = 34.dp, tint: Color = Color.White) {
    val reduced = LocalReducedMotion.current
    val transition = rememberInfiniteTransition(label = "fab")
    val bob by transition.animateFloat(
        0f, 1f,
        infiniteRepeatable(tween(1500, easing = EaseInOutSine), RepeatMode.Reverse),
        label = "bob"
    )
    val fizz by transition.animateFloat(
        0f, 1f,
        infiniteRepeatable(tween(2200, easing = LinearEasing)),
        label = "fizz"
    )
    Canvas(Modifier.size(size)) {
        val s = min(this.size.width, this.size.height) / 34f
        // Bowl outline: M3.2 1 C3.2 9.5, 6 14.8, 13 14.8 C20 14.8, 22.8 9.5, 22.8 1
        val bowl = Path().apply {
            moveTo(3.2f * s, 1f * s)
            cubicTo(3.2f * s, 9.5f * s, 6f * s, 14.8f * s, 13f * s, 14.8f * s)
            cubicTo(20f * s, 14.8f * s, 22.8f * s, 9.5f * s, 22.8f * s, 1f * s)
        }
        val bowlClip = Path().apply {
            moveTo(3.2f * s, 1f * s)
            lineTo(22.8f * s, 1f * s)
            cubicTo(22.8f * s, 9.5f * s, 20f * s, 14.8f * s, 13f * s, 14.8f * s)
            cubicTo(6f * s, 14.8f * s, 3.2f * s, 9.5f * s, 3.2f * s, 1f * s)
            close()
        }
        clipPath(bowlClip) {
            val dy = if (reduced) 1.75f * s else (3.5f - 3.5f * bob) * s
            translate(top = dy) {
                val wave = Path().apply {
                    moveTo(0f, 7.5f * s)
                    cubicTo(4.5f * s, 6f * s, 8.5f * s, 9f * s, 13f * s, 7.8f * s)
                    cubicTo(17.5f * s, 6.7f * s, 21.5f * s, 8.6f * s, 26f * s, 7.5f * s)
                    lineTo(26f * s, 16f * s)
                    lineTo(0f, 16f * s)
                    close()
                }
                drawPath(wave, tint.copy(alpha = 0.9f))
            }
            if (!reduced) {
                val fy = 13f * s - 9f * s * fizz
                val fAlpha = if (fizz < 0.25f) fizz / 0.25f * 0.95f else (1f - fizz) * 0.95f
                drawCircle(tint.copy(alpha = fAlpha), 1.1f * s, Offset(9f * s, fy))
                drawCircle(tint.copy(alpha = fAlpha * 0.85f), 0.9f * s, Offset(13.5f * s, fy + 2f * s))
                drawCircle(tint.copy(alpha = fAlpha * 0.9f), 0.9f * s, Offset(17f * s, fy + 1f * s))
            }
        }
        drawPath(bowl, tint.copy(alpha = 0.95f), style = Stroke(2f * s, cap = StrokeCap.Round))
        // Stem
        drawRoundRect(
            tint.copy(alpha = 0.95f),
            topLeft = Offset(12f * s, 14.5f * s),
            size = androidx.compose.ui.geometry.Size(2f * s, 12.5f * s),
            cornerRadius = CornerRadius(1f * s)
        )
        // Foot
        val foot = Path().apply {
            moveTo(6f * s, 31.5f * s)
            cubicTo(6f * s, 30.2f * s, 9f * s, 29.6f * s, 13f * s, 29.6f * s)
            cubicTo(17f * s, 29.6f * s, 20f * s, 30.2f * s, 20f * s, 31.5f * s)
            cubicTo(20f * s, 32.6f * s, 17f * s, 33f * s, 13f * s, 33f * s)
            cubicTo(9f * s, 33f * s, 6f * s, 32.6f * s, 6f * s, 31.5f * s)
            close()
        }
        drawPath(foot, tint.copy(alpha = 0.95f))
    }
}

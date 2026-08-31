package com.mtss.alcoholtracker.ui.components

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipRect
import com.mtss.alcoholtracker.ui.theme.BarFill
import com.mtss.alcoholtracker.ui.theme.Pattern

/**
 * Surface texture.
 *
 * The mono themes have no hue to separate one state from another, so the
 * canvas gives them a dot grid and diagonal hatching instead — the texture is
 * carrying meaning there, not decoration. The warm themes set [Pattern.None]
 * and nothing is drawn at all.
 */
fun Modifier.pattern(p: Pattern): Modifier = when (p) {
    is Pattern.None -> this
    is Pattern.Dots -> drawBehind { drawDots(p.color, p.spacing) }
    is Pattern.Diagonal -> drawBehind { drawDiagonal(p.color, p.period, p.thickness) }
}

/** As [pattern], but over the content rather than under it. */
fun Modifier.patternOver(p: Pattern): Modifier = when (p) {
    is Pattern.None -> this
    is Pattern.Dots -> drawWithContent { drawContent(); drawDots(p.color, p.spacing) }
    is Pattern.Diagonal -> drawWithContent {
        drawContent(); drawDiagonal(p.color, p.period, p.thickness)
    }
}

/** `radial-gradient(<c> 1px, transparent 1px)` — a 1px dot on a square grid. */
private fun DrawScope.drawDots(color: Color, spacingDp: Float) {
    val step = spacingDp * density
    if (step <= 0f) return
    val r = 1f * density
    var y = step / 2f
    while (y < size.height) {
        var x = step / 2f
        while (x < size.width) {
            drawCircle(color, radius = r, center = Offset(x, y))
            x += step
        }
        y += step
    }
}

/**
 * `repeating-linear-gradient(135deg, …)` — fine hatching.
 *
 * Drawn as strokes rather than a shader because the period is only a few dp
 * and a tiled bitmap shows seams at that size.
 */
private fun DrawScope.drawDiagonal(color: Color, periodDp: Float, thicknessDp: Float) {
    val period = periodDp * density
    if (period <= 0f) return
    val w = thicknessDp * density
    clipRect {
        // 135° in CSS runs top-left to bottom-right on screen.
        var x = -size.height
        while (x < size.width + size.height) {
            drawLine(
                color,
                start = Offset(x, 0f),
                end = Offset(x + size.height, size.height),
                strokeWidth = w,
                cap = StrokeCap.Butt
            )
            x += period
        }
    }
}

/**
 * Paints a chart bar.
 *
 * [BarFill.Striped] is how the mono themes say "over goal" without a red to
 * say it in, so this is a meaning-bearing branch rather than a style flourish.
 */
fun Modifier.barFill(fill: BarFill): Modifier = when (fill) {
    is BarFill.Flat -> drawBehind { drawRect(fill.primary) }
    is BarFill.Striped -> drawBehind {
        drawRect(fill.secondary)
        drawDiagonal(fill.primary, 6f, 3f)
    }
}

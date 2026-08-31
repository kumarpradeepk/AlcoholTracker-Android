package com.mtss.alcoholtracker.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.mtss.alcoholtracker.ui.theme.LocalAppColors
import com.mtss.alcoholtracker.ui.theme.rememberIconDraw

/**
 * One of the canvas's line icons.
 *
 * The set is drawn on a shared 24×24 grid with round caps and joins, so the
 * icons read as one family; [IconSpec.strokeWidth] is the canvas's own weight
 * for that glyph and is scaled with the icon rather than fixed, which is what
 * keeps a 16dp chevron and a 42dp bottle looking like the same pen.
 *
 * @param progress how much of the stroke to reveal, for the `idraw` draw-on.
 *        1f draws the finished icon; [AnimatedAppIcon] animates it.
 */
@Composable
fun AppIcon(
    spec: IconSpec,
    modifier: Modifier = Modifier,
    size: Dp = 18.dp,
    tint: Color? = null,
    strokeWidth: Float? = null,
    progress: Float = 1f,
    contentDescription: String? = null
) {
    val c = LocalAppColors.current
    val color = tint ?: c.ink
    // Parsing is not free and the specs are constant, so a glyph is parsed once
    // per call site rather than on every recomposition or frame of the draw-on.
    val subPaths = remember(spec) { spec.paths.map { PathParser().parsePathString(it).toPath() } }

    Canvas(
        modifier
            .size(size)
            .then(
                if (contentDescription != null)
                    Modifier.semantics { this.contentDescription = contentDescription }
                else Modifier
            )
    ) {
        val k = this.size.minDimension / AppIcons.VIEWPORT
        scale(k, pivot = androidx.compose.ui.geometry.Offset.Zero) {
            if (spec.filled) {
                subPaths.forEach { drawPath(it, color) }
            } else {
                val stroke = Stroke(
                    width = strokeWidth ?: spec.strokeWidth,
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round
                )
                if (progress >= 1f) {
                    subPaths.forEach { drawPath(it, color, style = stroke) }
                } else {
                    drawPartial(subPaths, progress, color, stroke)
                }
            }
        }
    }
}

/**
 * Reveals the glyph as one continuous line rather than growing every sub-path
 * at once: the sub-paths are drawn in order and share a single budget, which is
 * what makes the calendar's frame land before its tick marks.
 */
private fun DrawScope.drawPartial(
    subPaths: List<Path>,
    progress: Float,
    color: Color,
    stroke: Stroke
) {
    val measures = subPaths.map { PathMeasure().apply { setPath(it, false) } }
    val total = measures.sumOf { it.length.toDouble() }.toFloat()
    if (total <= 0f) return
    var budget = total * progress.coerceIn(0f, 1f)
    val out = Path()
    for (m in measures) {
        if (budget <= 0f) break
        val take = minOf(budget, m.length)
        out.rewind()
        if (m.getSegment(0f, take, out, true)) drawPath(out, color, style = stroke)
        budget -= take
    }
}

/**
 * The canvas's `animation:idraw .6s <delay> ease both` — the icon draws itself
 * in. Used where an icon arrives with its screen; a control the user is about
 * to press uses the static [AppIcon] so it is never half-drawn under a finger.
 */
@Composable
fun AnimatedAppIcon(
    spec: IconSpec,
    modifier: Modifier = Modifier,
    size: Dp = 18.dp,
    tint: Color? = null,
    strokeWidth: Float? = null,
    delayMillis: Int = 0,
    contentDescription: String? = null
) {
    AppIcon(
        spec = spec,
        modifier = modifier,
        size = size,
        tint = tint,
        strokeWidth = strokeWidth,
        progress = rememberIconDraw(delayMillis = delayMillis),
        contentDescription = contentDescription
    )
}

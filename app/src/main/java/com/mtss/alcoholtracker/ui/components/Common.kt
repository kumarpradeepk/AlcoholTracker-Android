package com.mtss.alcoholtracker.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.Canvas
import com.mtss.alcoholtracker.ui.theme.LocalAppColors
import com.mtss.alcoholtracker.ui.theme.Radii
import com.mtss.alcoholtracker.ui.theme.LocalReducedMotion
import com.mtss.alcoholtracker.ui.theme.Motion
import com.mtss.alcoholtracker.ui.theme.text

/** Press feedback the whole app uses: scale down with a springy return. */
fun Modifier.pressable(
    pressedScale: Float = 0.96f,
    role: Role = Role.Button,
    enabled: Boolean = true,
    onClick: () -> Unit
): Modifier = composed {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) pressedScale else 1f,
        animationSpec = tween(280, easing = Motion.OvershootPop),
        label = "press"
    )
    this
        .scale(scale)
        .clickable(
            interactionSource = interaction,
            indication = null,
            role = role,
            enabled = enabled,
            onClick = onClick
        )
}

/** The mock's kRise entrance: fade + 12dp rise, optionally staggered. */
fun Modifier.riseIn(delayMillis: Int = 0, durationMillis: Int = 500): Modifier = composed {
    val reduced = LocalReducedMotion.current
    val progress = remember { Animatable(if (reduced) 1f else 0f) }
    LaunchedEffect(Unit) {
        if (!reduced) {
            progress.animateTo(1f, tween(durationMillis, delayMillis, Motion.Settle))
        }
    }
    val dy = with(LocalDensity.current) { (12 * (1f - progress.value)).dp }
    this
        .offset(y = dy)
        .alpha(progress.value)
        .scale(0.985f + 0.015f * progress.value)
}

/** Animated count-up used for the ring value and stat tiles (~850 ms ease-out). */
@Composable
fun animatedValue(target: Double): Double {
    val reduced = LocalReducedMotion.current
    val anim = remember { Animatable(target.toFloat()) }
    LaunchedEffect(target) {
        if (reduced) anim.snapTo(target.toFloat())
        else anim.animateTo(
            target.toFloat(),
            tween(850, easing = CubicBezierEasing(0.215f, 0.61f, 0.355f, 1f))
        )
    }
    return anim.value.toDouble()
}

@Composable
fun AppCard(
    modifier: Modifier = Modifier,
    radius: Dp? = null,
    padding: Dp = 14.dp,
    color: Color? = null,
    bordered: Boolean = true,
    content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit
) {
    val c = LocalAppColors.current
    // A card in this system is a plane with an edge, not a floating slab: the
    // canvas draws every surface with a 1px line and no shadow anywhere.
    val shape = RoundedCornerShape(radius ?: Radii.CARD.dp)
    Column(
        modifier
            .fillMaxWidth()
            .clip(shape)
            .background(color ?: c.card)
            .then(if (bordered) Modifier.border(1.dp, c.line, shape) else Modifier)
            .padding(padding),
        content = content
    )
}

@Composable
fun SectionLabel(label: String, modifier: Modifier = Modifier) {
    val c = LocalAppColors.current
    Text(
        label,
        style = text(12.sp, FontWeight.SemiBold, letterSpacing = 0.6.sp),
        color = c.sub,
        modifier = modifier.padding(start = 4.dp)
    )
}

enum class ChevronDirection { LEFT, RIGHT, UP, DOWN }

@Composable
fun Chevron(
    direction: ChevronDirection,
    color: Color,
    size: Dp = 12.dp,
    strokeWidth: Float = 6f
) {
    Canvas(Modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val path = Path().apply {
            when (direction) {
                ChevronDirection.RIGHT -> { moveTo(w * 0.3f, h * 0.12f); lineTo(w * 0.72f, h * 0.5f); lineTo(w * 0.3f, h * 0.88f) }
                ChevronDirection.LEFT -> { moveTo(w * 0.7f, h * 0.12f); lineTo(w * 0.28f, h * 0.5f); lineTo(w * 0.7f, h * 0.88f) }
                ChevronDirection.DOWN -> { moveTo(w * 0.12f, h * 0.34f); lineTo(w * 0.5f, h * 0.72f); lineTo(w * 0.88f, h * 0.34f) }
                ChevronDirection.UP -> { moveTo(w * 0.12f, h * 0.66f); lineTo(w * 0.5f, h * 0.28f); lineTo(w * 0.88f, h * 0.66f) }
            }
        }
        drawPath(path, color, style = Stroke(strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
}

@Composable
fun CheckMark(color: Color = Color.White, size: Dp = 10.dp, strokeWidth: Float = 5f) {
    Canvas(Modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val path = Path().apply {
            moveTo(w * 0.08f, h * 0.52f)
            lineTo(w * 0.4f, h * 0.85f)
            lineTo(w * 0.92f, h * 0.2f)
        }
        drawPath(path, color, style = Stroke(strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
}

@Composable
fun CloseGlyph(color: Color, size: Dp = 11.dp, strokeWidth: Float = 5f) {
    Canvas(Modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        drawLine(color, androidx.compose.ui.geometry.Offset(0f, 0f), androidx.compose.ui.geometry.Offset(w, h), strokeWidth, StrokeCap.Round)
        drawLine(color, androidx.compose.ui.geometry.Offset(w, 0f), androidx.compose.ui.geometry.Offset(0f, h), strokeWidth, StrokeCap.Round)
    }
}

/** 36dp round icon button on a card surface — day nav, sheet close, back. */
@Composable
fun RoundIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 36.dp,
    enabled: Boolean = true,
    dimmedWhenDisabled: Boolean = true,
    onCard2: Boolean = false,
    contentDescription: String? = null,
    content: @Composable () -> Unit
) {
    val c = LocalAppColors.current
    // Square-ish with the theme's small radius, matching the canvas's day-nav
    // and sheet-close controls. Variants on a sunken well drop the border that
    // would otherwise double against it.
    val iconShape = RoundedCornerShape(Radii.TILE.dp)
    Box(
        modifier
            .size(size)
            .alpha(if (!enabled && dimmedWhenDisabled) 0.3f else 1f)
            .clip(iconShape)
            .background(if (onCard2) c.elev else c.card)
            .then(if (onCard2) Modifier else Modifier.border(1.dp, c.line, iconShape))
            .pressable(pressedScale = 0.88f, enabled = enabled, onClick = onClick)
            // The callers all pass a localized a11y label; it was being dropped
            // on the floor, so every one of these buttons was unlabelled.
            .then(
                if (contentDescription != null) {
                    Modifier.semantics {
                        this.contentDescription = contentDescription
                        role = Role.Button
                    }
                } else Modifier
            ),
        contentAlignment = Alignment.Center
    ) { content() }
}

/** The 54dp tide pill CTA. */
@Composable
fun PrimaryButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    height: Dp = 52.dp,
    color: Color? = null,
    textColor: Color? = null
) {
    val c = LocalAppColors.current
    val shape = RoundedCornerShape(Radii.BANNER.dp)
    Box(
        modifier
            .fillMaxWidth()
            .height(height)
            .alpha(if (enabled) 1f else 0.45f)
            .clip(shape)
            .background(color ?: c.acc)
            .pressable(pressedScale = 0.97f, onClick = { if (enabled) onClick() }),
        contentAlignment = Alignment.Center
    ) {
        // Never hardcode white: Nocturne's accent is ivory and Kiln's dark
        // accent is a mid rust, so the label colour comes from the pair.
        Text(label, style = text(15.sp, FontWeight.Bold), color = textColor ?: c.accInk)
    }
}

/** Soft (tinted) secondary pill. */
@Composable
fun SoftButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    height: Dp = 48.dp,
    container: Color? = null,
    contentColor: Color? = null
) {
    val c = LocalAppColors.current
    Box(
        modifier
            .fillMaxWidth()
            .height(height)
            .clip(RoundedCornerShape(Radii.TILE.dp))
            .background(container ?: c.elev)
            .pressable(pressedScale = 0.97f, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(label, style = text(14.sp, FontWeight.Bold), color = contentColor ?: c.ink)
    }
}

/** iOS-style 50×30 switch matching the mock. */
@Composable
fun AppSwitch(checked: Boolean, onToggle: () -> Unit) {
    val c = LocalAppColors.current
    val knobX by animateFloatAsState(if (checked) 19f else 2f, tween(300, easing = Motion.Overshoot), label = "knob")
    val track by androidx.compose.animation.animateColorAsState(
        if (checked) c.acc else c.elev, tween(300), label = "track"
    )
    val knobColor by androidx.compose.animation.animateColorAsState(
        if (checked) c.accInk else c.card, tween(300), label = "knobColor"
    )
    Box(
        Modifier
            .size(40.dp, 23.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(track)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                role = Role.Switch,
                onClick = onToggle
            )
    ) {
        Box(
            Modifier
                .offset(x = knobX.dp, y = 2.dp)
                .size(19.dp)
                .background(knobColor, CircleShape)
        )
    }
}

/** Segmented control on a card2 track with a sliding/selected thumb. */
@Composable
fun Segmented(
    options: List<String>,
    selectedIndex: Int,
    modifier: Modifier = Modifier,
    itemPadding: Dp = 9.dp,
    fontSize: androidx.compose.ui.unit.TextUnit = 14.5.sp,
    onSelect: (Int) -> Unit
) {
    val c = LocalAppColors.current
        Row(
        modifier
            .clip(RoundedCornerShape(Radii.TILE.dp))
            .background(c.elev)
            .padding(2.dp)
    ) {
        options.forEachIndexed { i, label ->
            val sel = i == selectedIndex
            Box(
                Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(Radii.TILE.dp))
                    .background(if (sel) c.acc else Color.Transparent)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { onSelect(i) }
                    .padding(vertical = itemPadding),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    label,
                    style = text(fontSize, FontWeight.SemiBold),
                    color = if (sel) c.accInk else c.sub
                )
            }
        }
    }
}

/** − value + stepper row used for ABV, serving, guideline targets. */
@Composable
fun StepperRow(
    valueText: @Composable () -> Unit,
    onMinus: () -> Unit,
    onPlus: () -> Unit
) {
    val c = LocalAppColors.current
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(c.elev)
                .pressable(pressedScale = 0.86f, onClick = onMinus),
            contentAlignment = Alignment.Center
        ) { Text("−", style = text(22.sp), color = c.sub) }
        valueText()
        Box(
            Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(c.elev)
                .pressable(pressedScale = 0.86f, onClick = onPlus),
            contentAlignment = Alignment.Center
        ) { Text("+", style = text(22.sp), color = c.sub) }
    }
}

/** The tilted-droplet brand mark drawn exactly like the mock's CSS shape. */
@Composable
fun DropletMark(size: Dp, color: Color? = null, breathing: Boolean = true) {
    val c = LocalAppColors.current
    val reduced = LocalReducedMotion.current
    val scale = if (breathing && !reduced) {
        val t = rememberInfiniteBreath(periodMillis = 5000)
        1f + 0.03f * t
    } else 1f
    Box(
        Modifier
            .size(size)
            .scale(scale)
            .rotate(45f)
            .clip(RoundedCornerShape(topStart = size * 0.13f, topEnd = size / 2, bottomEnd = size / 2, bottomStart = size / 2))
            .background(color ?: c.acc)
    )
}

/** A little glass icon whose fill height encodes strength. */
@Composable
fun GlassIcon(
    fillFraction: Float,
    width: Dp = 30.dp,
    height: Dp = 38.dp,
    fill: Color? = null,
    borderColor: Color? = null
) {
    val c = LocalAppColors.current
    val shape = RoundedCornerShape(
        topStart = 6.dp, topEnd = 6.dp, bottomStart = 12.dp, bottomEnd = 12.dp
    )
    Box(
        Modifier
            .size(width, height)
            .clip(shape)
            .border(2.dp, borderColor ?: c.line, shape)
    ) {
        Box(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(height * fillFraction.coerceIn(0f, 1f))
                .background((fill ?: c.acc).copy(alpha = 0.55f))
        )
    }
}

/** 0→1→0 breathing driver shared by the droplet and the empty-state glass. */
@Composable
fun rememberInfiniteBreath(periodMillis: Int): Float {
    val transition = rememberInfiniteTransition(label = "breath")
    val v by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(periodMillis / 2, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "breathValue"
    )
    return v
}

val EaseInOutSine = CubicBezierEasing(0.37f, 0f, 0.63f, 1f)

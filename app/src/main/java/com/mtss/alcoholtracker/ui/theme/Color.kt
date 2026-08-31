package com.mtss.alcoholtracker.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * The token set, verbatim from the Claude Design canvas (`Alcohol Tracker
 * Phone.html`, the `.at` / `.at.dk` / `.at.mnl` / `.at.dk.mnd` blocks).
 *
 * The concrete palettes are generated into `Palettes.kt`; this file is the
 * shape they fill and the behaviour that hangs off them.
 */
@Immutable
data class AppColors(
    /** The page the app is drawn on. */
    val page: Color,
    /** Behind the page — the frame ground. */
    val outer: Color,
    /** A raised card. In this design a card is a fill, not a border. */
    val card: Color,
    /** A sunken well inside a card: icon tiles, inset rows. */
    val elev: Color,
    /** Primary text. */
    val ink: Color,
    /** Secondary text. The canvas has exactly two ink levels — do not add a third. */
    val sub: Color,
    /** Hairline dividers. */
    val line: Color,

    /** The accent. Fills the intake hero and every primary action. */
    val acc: Color,
    /** Text and icons on top of [acc]. */
    val accInk: Color,
    /** Positive: dry days, under-goal bars. */
    val moss: Color,
    /** Text on top of [moss]. */
    val mossInk: Color,
    /** Caution: approaching the daily goal. */
    val amber: Color,
    /** Over goal, and every destructive action. */
    val danger: Color,
    /** Text on top of [danger]. */
    val dangerInk: Color,
    /** A switch's track when it is on — its own token, not always [acc]. */
    val togOn: Color,

    /** The intake ring in its caution band. */
    val ringWarn: Color,
    /** The intake ring once over goal. */
    val ringOver: Color,
    /** The ring's second over-goal lap, past 200%. */
    val ringOver2: Color,

    /** Dimmer behind sheets and dialogs. */
    val scrim: Color,

    /** Texture behind the page. */
    val patPage: Pattern,
    /** Texture inside the intake hero. */
    val patHero: Pattern,
    /** Texture on a moss surface. */
    val patMoss: Pattern,
    /** Fill for an over-goal bar. */
    val barOver: BarFill,
    /** Fill for a caution bar. */
    val barWarnBg: BarFill,
    /**
     * Opacity of the decorative glass silhouettes. Zero in the warm themes,
     * which do not use them at all.
     */
    val silhouette: Float,

    val isDark: Boolean
) {
    /**
     * The goal-progress ramp. Colour may judge; copy may not.
     *
     * The mono themes have no hue to judge with, so their moss/amber/danger
     * collapse towards greys and the *pattern* carries the meaning instead —
     * see [barOver] and [barWarnBg].
     */
    fun forRatio(ratio: Double): Color = when {
        ratio <= 0.75 -> moss
        ratio <= 1.0 -> amber
        else -> danger
    }

    /**
     * The canvas's `color-mix(in oklab, <c> <pct>%, var(--card))`, used for
     * every soft accent tint — Pro pills, chip tiles, status chips.
     *
     * Mixed in linear space: a plain sRGB blend of a saturated accent into a
     * warm card goes muddy, and this stays far closer to what the canvas draws.
     */
    fun mix(color: Color, percent: Float): Color {
        val t = percent.coerceIn(0f, 1f)
        fun lin(c: Float) = if (c <= 0.04045f) c / 12.92f
        else Math.pow(((c + 0.055f) / 1.055f).toDouble(), 2.4).toFloat()
        fun srgb(c: Float) = if (c <= 0.0031308f) c * 12.92f
        else (1.055f * Math.pow(c.toDouble(), 1 / 2.4).toFloat() - 0.055f)
        fun ch(a: Float, b: Float) = srgb(lin(a) * t + lin(b) * (1 - t))
        return Color(
            ch(color.red, card.red),
            ch(color.green, card.green),
            ch(color.blue, card.blue),
            1f
        )
    }
}

/**
 * Surface texture. The mono themes cannot use hue to separate states, so they
 * reach for a dot grid and diagonal hatching instead; the warm themes set
 * [None] and the drawing is skipped entirely.
 */
@Immutable
sealed interface Pattern {
    @Immutable
    data object None : Pattern

    /** `radial-gradient(<c> 1px, transparent 1px)` — a 1px dot on a grid. */
    @Immutable
    data class Dots(val color: Color, val spacing: Float = 10f) : Pattern

    /** `repeating-linear-gradient(135deg, …)` — fine diagonal hatching. */
    @Immutable
    data class Diagonal(val color: Color, val period: Float = 10f, val thickness: Float = 2f) : Pattern
}

/**
 * A chart bar's fill. Striped is how the mono themes say "over goal" without a
 * red to say it in.
 */
@Immutable
sealed interface BarFill {
    val primary: Color

    @Immutable
    data class Flat(override val primary: Color) : BarFill

    @Immutable
    data class Striped(override val primary: Color, val secondary: Color) : BarFill
}

/**
 * The two theme families. Each is designed in light and dark independently, so
 * this is orthogonal to the scheme choice.
 *
 * [id] is the persisted identity and never changes.
 */
enum class AppTheme(val id: String) {
    WARM("warm"),
    MONO("mono");

    companion object {
        fun from(raw: String?): AppTheme = entries.firstOrNull { it.id == raw } ?: WARM
    }
}

/**
 * Corner radii, read off the canvas. This design is round and the steps carry
 * meaning, so they are named for what they wrap.
 */
object Radii {
    /** Icon buttons and small tiles — 15dp in the canvas. */
    const val TILE = 15
    /** Chips and pills that are not fully round. */
    const val CHIP = 18
    /** Banners and secondary cards. */
    const val BANNER = 20
    /** Standard content card. */
    const val CARD = 24
    /** The intake hero and bottom sheets. */
    const val HERO = 30
}

val LocalAppColors = staticCompositionLocalOf { Palettes.WarmLight }

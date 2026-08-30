package com.mtss.alcoholtracker.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * The token set, verbatim from the Claude Design canvas `Coaster Prototype.dc.html`.
 *
 * The canvas ships **three** themes, each with an independently designed light
 * and dark palette — dark is never an inversion. The names here are the
 * canvas's own (`bg`, `surface`, `accent`, `b1`…`b4`), so a future canvas
 * revision can be diffed against this file without a translation step.
 *
 * The four `b*` tokens are the brief's §5.2 **band scale** — a single ramp that
 * expresses "how far through the daily goal", never a value judgment. See
 * [AppColors.band].
 */
@Immutable
data class AppColors(
    /** Page ground. */
    val bg: Color,
    /** Raised surface: cards, rows, the tab bar. */
    val surface: Color,
    /** Sunken surface: chips, wells, inset groups. */
    val surface2: Color,
    /** Hairline borders. In this system a card is a border, not a shadow. */
    val line: Color,
    /** Primary text. */
    val text: Color,
    /** Secondary text. */
    val muted: Color,
    /** Tertiary text, captions, disabled. */
    val faint: Color,
    /** The one accent: primary actions, links, selection. */
    val accent: Color,
    /** Text/icon that sits on top of [accent]. */
    val onAccent: Color,
    /** The earned/premium accent — gold. Owns Pro and the banked-day mark. */
    val accent2: Color,
    /** Text/icon on top of [accent2]. */
    val onAccent2: Color,
    /** Band 1 — at or under 75% of the daily goal. Also "dry day". */
    val b1: Color,
    /** Band 2 — 75–100%. */
    val b2: Color,
    /** Band 3 — 100–150%. Also the destructive colour. */
    val b3: Color,
    /** Band 4 — over 150%. */
    val b4: Color,
    val isDark: Boolean
) {
    /**
     * The brief's FIXED band scale (§5.2). Colour may judge; copy may not.
     * Thresholds are the canvas's `band(ratio)` exactly.
     */
    fun band(ratio: Double): Color = when {
        ratio <= 0.75 -> b1
        ratio <= 1.0 -> b2
        ratio <= 1.5 -> b3
        else -> b4
    }
}

/**
 * Corner radii. Each theme sets its own — Kiln is tight and papery at 12dp,
 * Nocturne is soft at 18dp — so radius is a theme token, not a constant.
 */
@Immutable
data class AppGeometry(
    /** Cards and grouped rows. */
    val r: Dp,
    /** Chips, small controls, inset wells. */
    val rs: Dp,
    /** Primary buttons. */
    val rl: Dp
)

/**
 * The three themes from the canvas's `THEMES` table.
 *
 * [id] is the persisted identity and never changes; the display name and
 * description are read from resources so they follow the locale.
 */
enum class AppTheme(val id: String) {
    KILN("kiln"),
    NOCTURNE("nocturne"),
    COASTER("coaster");

    companion object {
        fun from(raw: String?): AppTheme = entries.firstOrNull { it.id == raw } ?: KILN
    }
}

// ── Kiln — warm stone and struck brass ───────────────────────────────────

private val KilnLight = AppColors(
    bg = Color(0xFFF6F3EE),
    surface = Color(0xFFFFFFFF),
    surface2 = Color(0xFFF0E6DE),
    line = Color(0xFFE7E1D7),
    text = Color(0xFF1C1A17),
    muted = Color(0xFF6E675D),
    faint = Color(0xFF9A9287),
    accent = Color(0xFFB4623A),
    onAccent = Color(0xFFFFFFFF),
    accent2 = Color(0xFFC9962F),
    onAccent2 = Color(0xFFF6F3EE),
    b1 = Color(0xFF5E7A5B),
    b2 = Color(0xFFB08A3C),
    b3 = Color(0xFF8C4A63),
    b4 = Color(0xFF5A2440),
    isDark = false
)

private val KilnDark = AppColors(
    bg = Color(0xFF16130F),
    surface = Color(0xFF201C17),
    surface2 = Color(0xFF2A241C),
    line = Color(0xFF332C22),
    text = Color(0xFFF2ECE1),
    muted = Color(0xFFA0968A),
    faint = Color(0xFF8C8272),
    accent = Color(0xFFC4703F),
    onAccent = Color(0xFF16130F),
    accent2 = Color(0xFFD9AC48),
    onAccent2 = Color(0xFF16130F),
    b1 = Color(0xFF7E9B79),
    b2 = Color(0xFFD2A24A),
    b3 = Color(0xFFC07694),
    b4 = Color(0xFF9A5678),
    isDark = true
)

// ── Nocturne — graphite, ivory for banked days ───────────────────────────

private val NocturneLight = AppColors(
    bg = Color(0xFFF4F4F2),
    surface = Color(0xFFFFFFFF),
    surface2 = Color(0xFFF0F0ED),
    line = Color(0xFFE3E3DF),
    text = Color(0xFF14161A),
    muted = Color(0xFF6E747C),
    faint = Color(0xFF8B9096),
    accent = Color(0xFF14161A),
    onAccent = Color(0xFFFFFFFF),
    accent2 = Color(0xFF8A6B2F),
    onAccent2 = Color(0xFFF4F4F2),
    b1 = Color(0xFF4F7A56),
    b2 = Color(0xFFA6791F),
    b3 = Color(0xFFC25A2E),
    b4 = Color(0xFFA33227),
    isDark = false
)

private val NocturneDark = AppColors(
    bg = Color(0xFF101114),
    surface = Color(0xFF191B1F),
    surface2 = Color(0xFF23262B),
    line = Color(0xFF2B2F35),
    text = Color(0xFFF1EFEA),
    muted = Color(0xFF969CA4),
    faint = Color(0xFF6E747C),
    accent = Color(0xFFEDE4D3),
    onAccent = Color(0xFF101114),
    accent2 = Color(0xFFE3B341),
    onAccent2 = Color(0xFF101114),
    b1 = Color(0xFF8FB07A),
    b2 = Color(0xFFE3B341),
    b3 = Color(0xFFE07A4B),
    b4 = Color(0xFFB8443A),
    isDark = true
)

// ── Coaster — kraft paper, ink and honey ─────────────────────────────────

private val CoasterLight = AppColors(
    bg = Color(0xFFEFE9DE),
    surface = Color(0xFFFBF7F0),
    surface2 = Color(0xFFEFE4D2),
    line = Color(0xFFDED5C6),
    text = Color(0xFF17140F),
    muted = Color(0xFF6F6759),
    faint = Color(0xFF9C927F),
    accent = Color(0xFFC4872F),
    onAccent = Color(0xFFFFFFFF),
    accent2 = Color(0xFFC4872F),
    onAccent2 = Color(0xFFEFE9DE),
    b1 = Color(0xFF5E7A5B),
    b2 = Color(0xFFC4872F),
    b3 = Color(0xFFA9713C),
    b4 = Color(0xFF17140F),
    isDark = false
)

private val CoasterDark = AppColors(
    bg = Color(0xFF14120F),
    surface = Color(0xFF1D1A16),
    surface2 = Color(0xFF262119),
    line = Color(0xFF332C22),
    text = Color(0xFFF5F0E7),
    muted = Color(0xFFA0968A),
    faint = Color(0xFF8A8172),
    accent = Color(0xFFC4872F),
    onAccent = Color(0xFF14120F),
    accent2 = Color(0xFFD9A441),
    onAccent2 = Color(0xFF14120F),
    b1 = Color(0xFF7E9B79),
    b2 = Color(0xFFD9A441),
    b3 = Color(0xFFC08B54),
    b4 = Color(0xFFE7DFCE),
    isDark = true
)

/** Palette lookup. `onAccent2` follows the canvas rule: it is always `bg`. */
fun colorsFor(theme: AppTheme, dark: Boolean): AppColors = when (theme) {
    AppTheme.KILN -> if (dark) KilnDark else KilnLight
    AppTheme.NOCTURNE -> if (dark) NocturneDark else NocturneLight
    AppTheme.COASTER -> if (dark) CoasterDark else CoasterLight
}

fun geometryFor(theme: AppTheme): AppGeometry = when (theme) {
    AppTheme.KILN -> AppGeometry(r = 12.dp, rs = 9.dp, rl = 14.dp)
    AppTheme.NOCTURNE -> AppGeometry(r = 18.dp, rs = 12.dp, rl = 16.dp)
    AppTheme.COASTER -> AppGeometry(r = 16.dp, rs = 12.dp, rl = 16.dp)
}

/**
 * Per-drink-category tints, from the canvas's `TINTS`. These are *identity*
 * colours for a kind of drink, deliberately outside the band scale — a wine
 * is plum whether or not you are over your goal.
 */
object DrinkTints {
    val Beer = Color(0xFFD9A441)
    val Wine = Color(0xFF8E4257)
    val Spirit = Color(0xFFA9713C)
    val Cocktail = Color(0xFFB4623A)
    val Rtd = Color(0xFF7E8A88)
    val Cider = Color(0xFFC9962F)

    /** The canvas keys tints off a category slug; unknown drinks read as spirit. */
    fun forCategory(category: String?): Color = when (category?.lowercase()) {
        "beer" -> Beer
        "wine" -> Wine
        "spirit", "spirits" -> Spirit
        "cocktail" -> Cocktail
        "rtd", "seltzer" -> Rtd
        "cider" -> Cider
        else -> Spirit
    }

    /**
     * Category guessed from ABV and serving size, for the presets and logs that
     * predate the category field. Wide pours at low strength are beer; small
     * pours at high strength are spirits.
     */
    fun forDrink(abv: Double, ml: Double): Color = when {
        abv >= 30.0 -> Spirit
        abv >= 9.0 && ml <= 220.0 -> Wine
        abv <= 7.5 && ml >= 300.0 -> Beer
        abv <= 7.5 -> Rtd
        else -> Cocktail
    }
}

val LocalAppColors = staticCompositionLocalOf { KilnLight }
val LocalAppGeometry = staticCompositionLocalOf { geometryFor(AppTheme.KILN) }

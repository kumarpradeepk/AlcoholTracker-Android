package com.mtss.alcoholtracker.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import com.mtss.alcoholtracker.R

/** Numbers align in columns everywhere the design uses tabular figures. */
const val TABULAR = "tnum"

// ── Families ─────────────────────────────────────────────────────────────
//
// Each theme pairs a display face with a body face. Where a family has no cut
// at a requested weight the nearest available one is mapped here rather than
// left to the system's synthetic bolding, which smears these designs badly.

/** Kiln's display face. One optical weight; italic exists but is unused. */
private val InstrumentSerif = FontFamily(
    Font(R.font.instrument_serif_regular, FontWeight.Normal),
    Font(R.font.instrument_serif_regular, FontWeight.Medium),
    Font(R.font.instrument_serif_regular, FontWeight.SemiBold),
    Font(R.font.instrument_serif_regular, FontWeight.Bold),
    Font(R.font.instrument_serif_italic, FontWeight.Normal, FontStyle.Italic)
)

/** Body face for Kiln and Coaster. */
private val PublicSans = FontFamily(
    Font(R.font.public_sans_regular, FontWeight.Normal),
    Font(R.font.public_sans_medium, FontWeight.Medium),
    Font(R.font.public_sans_semibold, FontWeight.SemiBold),
    Font(R.font.public_sans_bold, FontWeight.Bold)
)

/** Nocturne's display face. Ships no SemiBold cut, so 600 takes Bold. */
private val SpaceGrotesk = FontFamily(
    Font(R.font.space_grotesk_regular, FontWeight.Normal),
    Font(R.font.space_grotesk_medium, FontWeight.Medium),
    Font(R.font.space_grotesk_bold, FontWeight.SemiBold),
    Font(R.font.space_grotesk_bold, FontWeight.Bold)
)

/** Nocturne's body face. 700 takes the SemiBold cut. */
private val IBMPlexSans = FontFamily(
    Font(R.font.ibm_plex_sans_regular, FontWeight.Normal),
    Font(R.font.ibm_plex_sans_medium, FontWeight.Medium),
    Font(R.font.ibm_plex_sans_semibold, FontWeight.SemiBold),
    Font(R.font.ibm_plex_sans_semibold, FontWeight.Bold)
)

/** Coaster's display face. */
private val Outfit = FontFamily(
    Font(R.font.outfit_regular, FontWeight.Normal),
    Font(R.font.outfit_medium, FontWeight.Medium),
    Font(R.font.outfit_semibold, FontWeight.SemiBold),
    Font(R.font.outfit_bold, FontWeight.Bold)
)

/**
 * The active theme's two faces plus the weight its display face is set at.
 *
 * [displayWeight] matters: Instrument Serif is drawn at 400 because it is a
 * true display serif that breaks up when synthesised heavier, while Space
 * Grotesk and Outfit are set at 600 to hold their counters at small sizes.
 */
@Immutable
data class AppFonts(
    val display: FontFamily,
    val body: FontFamily,
    val displayWeight: FontWeight
)

fun fontsFor(theme: AppTheme): AppFonts = when (theme) {
    AppTheme.KILN -> AppFonts(InstrumentSerif, PublicSans, FontWeight.Normal)
    AppTheme.NOCTURNE -> AppFonts(SpaceGrotesk, IBMPlexSans, FontWeight.SemiBold)
    AppTheme.COASTER -> AppFonts(Outfit, PublicSans, FontWeight.SemiBold)
}

val LocalAppFonts = staticCompositionLocalOf { fontsFor(AppTheme.KILN) }

// ── Styles ───────────────────────────────────────────────────────────────

/**
 * Body text in the active theme's body face.
 *
 * Not a `@Composable` on purpose — it is called from `remember` blocks and
 * from non-composable formatting helpers. Callers that need the themed face
 * use [text] below; this raw form is kept for the few places that style a
 * string outside composition.
 */
fun rawText(
    size: TextUnit,
    weight: FontWeight = FontWeight.Normal,
    tabular: Boolean = false,
    letterSpacing: TextUnit = TextUnit.Unspecified,
    family: FontFamily? = null
): TextStyle = TextStyle(
    fontFamily = family,
    fontSize = size,
    fontWeight = weight,
    letterSpacing = letterSpacing,
    fontFeatureSettings = if (tabular) TABULAR else null,
    platformStyle = PlatformTextStyle(includeFontPadding = false)
)

/** Body copy, chips, rows, labels — everything that is not a headline figure. */
@Composable
fun text(
    size: TextUnit,
    weight: FontWeight = FontWeight.Normal,
    tabular: Boolean = false,
    letterSpacing: TextUnit = TextUnit.Unspecified
): TextStyle = rawText(size, weight, tabular, letterSpacing, LocalAppFonts.current.body)

/**
 * The display face: screen titles, sheet titles, and every headline number
 * (the ring value, stat tiles, steppers, entry amounts).
 *
 * In this design the numerals are the personality of the theme, so a figure
 * that reads as data goes through [display], never [text].
 */
@Composable
fun display(
    size: TextUnit,
    tabular: Boolean = true,
    letterSpacing: TextUnit = TextUnit.Unspecified,
    weight: FontWeight? = null
): TextStyle {
    val fonts = LocalAppFonts.current
    return rawText(
        size = size,
        weight = weight ?: fonts.displayWeight,
        tabular = tabular,
        letterSpacing = letterSpacing,
        family = fonts.display
    )
}

/** Uppercase section captions — the design's 9–10sp tracked labels. */
@Composable
fun caption(size: TextUnit = 10.sp): TextStyle =
    text(size, FontWeight.Bold, letterSpacing = 1.1.sp)

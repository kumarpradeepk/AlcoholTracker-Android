package com.mtss.alcoholtracker.wear.ui

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import com.mtss.alcoholtracker.wear.R

/**
 * The watch token set, verbatim from the canvas's `.wt` block
 * (`Alcohol Tracker Watch.html`).
 *
 * Dark only, and deliberately so: the design draws on a black screen, which on
 * an OLED watch is also the battery budget. There is no light variant to
 * resolve, so unlike the phone this is a plain object rather than a palette
 * pair.
 */
@Immutable
object W {
    val screen = Color(0xFF000000)
    val acc = Color(0xFFE0643C)
    val ink = Color(0xFFF4EFE8)
    val sub = Color(0xFF9D9386)
    /** The raised surface: BAC card, chips, stepper wells. */
    val elev = Color(0xFF1C1916)
    val moss = Color(0xFF618A5E)
    val mossInk = Color(0xFF8FBB8B)
    val amber = Color(0xFFD99A2B)
    val danger = Color(0xFFC23524)
    val onAcc = Color(0xFFFFFFFF)

    /** A hairline over the black ground, for the stepper buttons. */
    val well = Color(0x12FFFFFF)

    /** The goal ramp, matching the phone's `forRatio`. */
    fun forRatio(ratio: Double): Color = when {
        ratio <= 0.75 -> moss
        ratio <= 1.0 -> amber
        else -> danger
    }
}

/** General Sans travels to the watch: one typeface across both surfaces. */
val GeneralSans = FontFamily(
    Font(R.font.general_sans_regular, FontWeight.Normal),
    Font(R.font.general_sans_medium, FontWeight.Medium),
    Font(R.font.general_sans_semibold, FontWeight.SemiBold),
    Font(R.font.general_sans_bold, FontWeight.Bold),
    Font(R.font.general_sans_bold, FontWeight.ExtraBold)
)

fun wtext(
    size: TextUnit,
    weight: FontWeight = FontWeight.SemiBold,
    tabular: Boolean = false
): TextStyle = TextStyle(
    fontFamily = GeneralSans,
    fontSize = size,
    fontWeight = weight,
    fontFeatureSettings = if (tabular) "tnum" else null,
    platformStyle = PlatformTextStyle(includeFontPadding = false)
)

val LocalReducedMotion = staticCompositionLocalOf { false }

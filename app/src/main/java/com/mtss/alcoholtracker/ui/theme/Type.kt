package com.mtss.alcoholtracker.ui.theme

import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import com.mtss.alcoholtracker.R

/** Numbers align in columns everywhere the design sets a figure in a row. */
const val TABULAR = "tnum"

/**
 * General Sans (Fontshare, FFL) — the design's only typeface, replacing the
 * five families the previous direction carried.
 *
 * The canvas asks for weight 800 in a few places; 700 is the heaviest cut
 * published, so those map to Bold rather than being synthesised.
 */
val GeneralSans = FontFamily(
    Font(R.font.general_sans_regular, FontWeight.Normal),
    Font(R.font.general_sans_medium, FontWeight.Medium),
    Font(R.font.general_sans_semibold, FontWeight.SemiBold),
    Font(R.font.general_sans_bold, FontWeight.Bold),
    // 800 and above resolve to the Bold cut, not a synthetic smear.
    Font(R.font.general_sans_bold, FontWeight.ExtraBold),
    Font(R.font.general_sans_bold, FontWeight.Black)
)

/**
 * Any text in the app. One family, so this is the only entry point — there is
 * no separate display face in this direction.
 *
 * [tracking] exists because the design tightens big figures hard
 * (`letter-spacing:-.04em` on the 62sp intake number) and opens up small
 * uppercase captions (`.08em`), and both read wrong at the default.
 */
fun text(
    size: TextUnit,
    weight: FontWeight = FontWeight.Medium,
    tabular: Boolean = false,
    letterSpacing: TextUnit = TextUnit.Unspecified
): TextStyle = TextStyle(
    fontFamily = GeneralSans,
    fontSize = size,
    fontWeight = weight,
    letterSpacing = letterSpacing,
    fontFeatureSettings = if (tabular) TABULAR else null,
    platformStyle = PlatformTextStyle(includeFontPadding = false)
)

/**
 * A headline figure: the intake number, a BAC reading, a stat value.
 *
 * Tracking is negative and scales with size, matching the canvas's
 * `-.02em` at 30px through `-.04em` at 62px.
 */
fun figure(
    size: TextUnit,
    weight: FontWeight = FontWeight.ExtraBold,
    tabular: Boolean = true
): TextStyle = text(
    size = size,
    weight = weight,
    tabular = tabular,
    letterSpacing = (if (size.value >= 48f) -0.04f else -0.025f * 1f).let { (it * size.value).sp }
)

/** The design's uppercase eyebrow: 12sp, bold, wide tracking, [AppColors.sub]. */
fun eyebrow(size: TextUnit = 12.sp): TextStyle =
    text(size, FontWeight.Bold, letterSpacing = (size.value * 0.08f).sp)

/**
 * The canvas's recurring roles, so a screen does not have to re-derive them.
 * Sizes and weights are read straight off the design.
 */
object Type {
    val ScreenTitle = figure(30.sp, FontWeight.ExtraBold)
    val Hero = figure(62.sp, FontWeight.ExtraBold)
    val BacValue = figure(36.sp, FontWeight.ExtraBold)
    val CardTitle = text(15.sp, FontWeight.Bold)
    val RowTitle = text(14.sp, FontWeight.Bold)
    val Body = text(13.5.sp, FontWeight.Medium)
    val Meta = text(12.5.sp, FontWeight.Medium)
    val Caption = text(11.5.sp, FontWeight.SemiBold)
    val Eyebrow = eyebrow()
}

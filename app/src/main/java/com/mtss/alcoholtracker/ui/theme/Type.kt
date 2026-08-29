package com.mtss.alcoholtracker.ui.theme

import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp

/** Numbers align in columns everywhere the mock uses tabular-nums. */
const val TABULAR = "tnum"

fun text(
    size: TextUnit,
    weight: FontWeight = FontWeight.Normal,
    tabular: Boolean = false,
    letterSpacing: TextUnit = TextUnit.Unspecified
): TextStyle = TextStyle(
    fontSize = size,
    fontWeight = weight,
    letterSpacing = letterSpacing,
    fontFeatureSettings = if (tabular) TABULAR else null,
    platformStyle = PlatformTextStyle(includeFontPadding = false)
)

object Type {
    val LargeTitle = text(32.sp, FontWeight.Bold, letterSpacing = (-0.5).sp)
    val Title = text(27.sp, FontWeight.Bold, letterSpacing = (-0.4).sp)
    val CardTitle = text(15.sp, FontWeight.SemiBold)
    val Body = text(15.5.sp)
    val Caption = text(13.sp)
    val SectionLabel = text(12.sp, FontWeight.SemiBold, letterSpacing = 0.6.sp)
}

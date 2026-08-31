package com.mtss.alcoholtracker.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * GENERATED — do not edit by hand. Regenerate with `scratchpad/gen_themes.py`.
 *
 * The canvas's four theme states, resolved from its cascading `.at` /
 * `.at.dk` / `.at.mnl` / `.at.dk.mnd` blocks. Mono-dark layers on dark in the
 * canvas, so it is flattened here rather than left as inheritance.
 */
object Palettes {
    val WarmLight = AppColors(
        page = Color(0xFFEFECE7),
        outer = Color(0xFFE5E1DA),
        card = Color(0xFFFFFFFF),
        elev = Color(0xFFF4F1EC),
        ink = Color(0xFF231C15),
        sub = Color(0xFF8D8478),
        line = Color(0xFFE7E2DA),
        acc = Color(0xFFE0643C),
        accInk = Color(0xFFFFFFFF),
        moss = Color(0xFF618A5E),
        mossInk = Color(0xFFFFFFFF),
        amber = Color(0xFFD99A2B),
        danger = Color(0xFFC23524),
        dangerInk = Color(0xFFFFFFFF),
        togOn = Color(0xFFE0643C),
        ringWarn = Color(0xFFFFE08A),
        ringOver = Color(0x80000000),
        ringOver2 = Color(0xC7000000),
        scrim = Color(0x731E160E),
        patPage = Pattern.None,
        patHero = Pattern.None,
        patMoss = Pattern.None,
        barOver = BarFill.Flat(Color(0x66000000)),
        barWarnBg = BarFill.Flat(Color(0xFFD99A2B)),
        silhouette = 0f,
        isDark = false
    )

    val WarmDark = AppColors(
        page = Color(0xFF161310),
        outer = Color(0xFF0D0B09),
        card = Color(0xFF211D18),
        elev = Color(0xFF2A251F),
        ink = Color(0xFFF2EBE2),
        sub = Color(0xFF9D9386),
        line = Color(0xFF37312A),
        acc = Color(0xFFE0643C),
        accInk = Color(0xFFFFFFFF),
        moss = Color(0xFF618A5E),
        mossInk = Color(0xFFFFFFFF),
        amber = Color(0xFFD99A2B),
        danger = Color(0xFFC23524),
        dangerInk = Color(0xFFFFFFFF),
        togOn = Color(0xFFE0643C),
        ringWarn = Color(0xFFFFE08A),
        ringOver = Color(0x80000000),
        ringOver2 = Color(0xC7000000),
        scrim = Color(0x99000000),
        patPage = Pattern.None,
        patHero = Pattern.None,
        patMoss = Pattern.None,
        barOver = BarFill.Flat(Color(0x66000000)),
        barWarnBg = BarFill.Flat(Color(0xFFD99A2B)),
        silhouette = 0f,
        isDark = true
    )

    val MonoLight = AppColors(
        page = Color(0xFFF3F3F1),
        outer = Color(0xFFE9E9E6),
        card = Color(0xFFFFFFFF),
        elev = Color(0xFFEBEBE8),
        ink = Color(0xFF141413),
        sub = Color(0xFF8B8B86),
        line = Color(0xFFE3E3DF),
        acc = Color(0xFF141413),
        accInk = Color(0xFFFFFFFF),
        moss = Color(0xFF4A4A46),
        mossInk = Color(0xFFFFFFFF),
        amber = Color(0xFF9A9A94),
        danger = Color(0xFF000000),
        dangerInk = Color(0xFFFFFFFF),
        togOn = Color(0xFF141413),
        ringWarn = Color(0xFFD8D8D2),
        ringOver = Color(0x8CFFFFFF),
        ringOver2 = Color(0xD9FFFFFF),
        scrim = Color(0x730A0A0A),
        patPage = Pattern.Dots(Color(0x12141413)),
        patHero = Pattern.Diagonal(Color(0x0AFFFFFF)),
        patMoss = Pattern.Diagonal(Color(0x0D141413)),
        barOver = BarFill.Striped(Color(0xB2FFFFFF), Color(0x38FFFFFF)),
        barWarnBg = BarFill.Striped(Color(0xFF141413), Color(0xFFA9A9A3)),
        silhouette = .09f,
        isDark = false
    )

    val MonoDark = AppColors(
        page = Color(0xFF0F0F0E),
        outer = Color(0xFF080808),
        card = Color(0xFF1A1A19),
        elev = Color(0xFF242422),
        ink = Color(0xFFF4F4F1),
        sub = Color(0xFF90908A),
        line = Color(0xFF2F2F2D),
        acc = Color(0xFFF4F4F1),
        accInk = Color(0xFF141413),
        moss = Color(0xFFCFCFC8),
        mossInk = Color(0xFF141413),
        amber = Color(0xFF8F8F8A),
        danger = Color(0xFFF4F4F1),
        dangerInk = Color(0xFF141413),
        togOn = Color(0xFF77776F),
        ringWarn = Color(0xFF6F6F68),
        ringOver = Color(0x80000000),
        ringOver2 = Color(0xCC000000),
        scrim = Color(0xA6000000),
        patPage = Pattern.Dots(Color(0x0FFFFFFF)),
        patHero = Pattern.Diagonal(Color(0x0D000000)),
        patMoss = Pattern.Diagonal(Color(0x0FF4F4F1)),
        barOver = BarFill.Striped(Color(0x99000000), Color(0x33000000)),
        barWarnBg = BarFill.Striped(Color(0xFFF4F4F1), Color(0xFF55554F)),
        silhouette = .1f,
        isDark = true
    )

    fun of(theme: AppTheme, dark: Boolean): AppColors = when (theme) {
        AppTheme.WARM -> if (dark) WarmDark else WarmLight
        AppTheme.MONO -> if (dark) MonoDark else MonoLight
    }
}

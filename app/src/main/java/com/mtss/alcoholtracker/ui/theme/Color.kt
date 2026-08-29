package com.mtss.alcoholtracker.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * The design's token set, verbatim from the mock's CSS custom properties.
 * `sec`/`ter`/`hair` are ink at fixed alphas so they sit correctly on any
 * card surface in both themes.
 */
@Immutable
data class AppColors(
    val bg: Color,
    val card: Color,
    val card2: Color,
    val ink: Color,
    val sec: Color,
    val ter: Color,
    val hair: Color,
    val tide: Color,
    val tideSoft: Color,
    val moss: Color,
    val mossSoft: Color,
    val amber: Color,
    val amberSoft: Color,
    val danger: Color,
    val glass: Color,
    val isDark: Boolean
)

val LightColors = AppColors(
    bg = Color(0xFFF6F5F2),
    card = Color(0xFFFFFFFF),
    card2 = Color(0xFFECEAE4),
    ink = Color(0xFF1D1C19),
    sec = Color(0xFF1D1C19).copy(alpha = 0.55f),
    ter = Color(0xFF1D1C19).copy(alpha = 0.36f),
    hair = Color(0xFF1D1C19).copy(alpha = 0.09f),
    tide = Color(0xFF2E8FBF),
    tideSoft = Color(0xFFDCEFF9),
    moss = Color(0xFF4BA36A),
    mossSoft = Color(0xFFDEF4E5),
    amber = Color(0xFFD68A28),
    amberSoft = Color(0xFFFAEED4),
    danger = Color(0xFFC9563E),
    glass = Color(0xFFFFFFFF).copy(alpha = 0.72f),
    isDark = false
)

val DarkColors = AppColors(
    bg = Color(0xFF151513),
    card = Color(0xFF1F1F1C),
    card2 = Color(0xFF2A2925),
    ink = Color(0xFFF2F0EB),
    sec = Color(0xFFF2F0EB).copy(alpha = 0.60f),
    ter = Color(0xFFF2F0EB).copy(alpha = 0.38f),
    hair = Color(0xFFF2F0EB).copy(alpha = 0.12f),
    tide = Color(0xFF6BC1E8),
    tideSoft = Color(0xFF1E3A4A),
    moss = Color(0xFF7FD49A),
    mossSoft = Color(0xFF20402C),
    amber = Color(0xFFF0B45E),
    amberSoft = Color(0xFF403319),
    danger = Color(0xFFE88268),
    glass = Color(0xFF1C1C1A).copy(alpha = 0.72f),
    isDark = true
)

val LocalAppColors = staticCompositionLocalOf { LightColors }

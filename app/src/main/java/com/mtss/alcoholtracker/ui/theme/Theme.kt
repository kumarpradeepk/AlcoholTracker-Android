package com.mtss.alcoholtracker.ui.theme

import android.provider.Settings
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.mtss.alcoholtracker.data.DarkChoice

/**
 * Resolves the user's theme and light/dark choice into the token set the whole
 * app reads, then publishes it.
 *
 * Two independent axes, exactly as the canvas has them: **which theme** (Kiln /
 * Nocturne / Coaster) and **which scheme** (System / Light / Dark). Every theme
 * is designed in both schemes, so the two never need to agree.
 */
@Composable
fun AlcoholTrackerTheme(
    theme: AppTheme,
    darkChoice: DarkChoice,
    content: @Composable () -> Unit
) {
    val dark = when (darkChoice) {
        DarkChoice.SYSTEM -> isSystemInDarkTheme()
        DarkChoice.LIGHT -> false
        DarkChoice.DARK -> true
    }
    val target = colorsFor(theme, dark)

    // The ground and the ink cross-fade when either axis changes, so switching
    // theme reads as one deliberate move rather than a flash of a new app.
    val bg by animateColorAsState(target.bg, tween(420), label = "bg")
    val text by animateColorAsState(target.text, tween(420), label = "text")
    val surface by animateColorAsState(target.surface, tween(420), label = "surface")
    val accent by animateColorAsState(target.accent, tween(420), label = "accent")
    val colors = target.copy(bg = bg, text = text, surface = surface, accent = accent)

    val context = LocalContext.current
    val reduced = remember {
        Settings.Global.getFloat(
            context.contentResolver,
            Settings.Global.ANIMATOR_DURATION_SCALE, 1f
        ) == 0f
    }

    val scheme = if (dark) {
        darkColorScheme(
            primary = colors.accent, onPrimary = colors.onAccent,
            background = colors.bg, surface = colors.surface,
            onBackground = colors.text, onSurface = colors.text
        )
    } else {
        lightColorScheme(
            primary = colors.accent, onPrimary = colors.onAccent,
            background = colors.bg, surface = colors.surface,
            onBackground = colors.text, onSurface = colors.text
        )
    }

    CompositionLocalProvider(
        LocalAppColors provides colors,
        LocalAppGeometry provides geometryFor(theme),
        LocalAppFonts provides fontsFor(theme),
        LocalReducedMotion provides reduced
    ) {
        MaterialTheme(colorScheme = scheme, content = content)
    }
}

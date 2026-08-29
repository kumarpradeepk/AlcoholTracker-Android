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

@Composable
fun AlcoholTrackerTheme(
    darkChoice: DarkChoice,
    content: @Composable () -> Unit
) {
    val dark = when (darkChoice) {
        DarkChoice.SYSTEM -> isSystemInDarkTheme()
        DarkChoice.LIGHT -> false
        DarkChoice.DARK -> true
    }
    val target = if (dark) DarkColors else LightColors

    // The mock cross-fades background/ink over 500 ms when the toggle flips.
    val bg by animateColorAsState(target.bg, tween(500), label = "bg")
    val ink by animateColorAsState(target.ink, tween(500), label = "ink")
    val colors = target.copy(bg = bg, ink = ink)

    val context = LocalContext.current
    val reduced = remember {
        Settings.Global.getFloat(
            context.contentResolver,
            Settings.Global.ANIMATOR_DURATION_SCALE, 1f
        ) == 0f
    }

    val scheme = if (dark) {
        darkColorScheme(
            primary = colors.tide, background = colors.bg, surface = colors.card,
            onBackground = colors.ink, onSurface = colors.ink
        )
    } else {
        lightColorScheme(
            primary = colors.tide, background = colors.bg, surface = colors.card,
            onBackground = colors.ink, onSurface = colors.ink
        )
    }

    CompositionLocalProvider(
        LocalAppColors provides colors,
        LocalReducedMotion provides reduced
    ) {
        MaterialTheme(colorScheme = scheme, content = content)
    }
}

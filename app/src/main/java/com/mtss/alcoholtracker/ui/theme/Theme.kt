package com.mtss.alcoholtracker.ui.theme

import android.provider.Settings
import androidx.compose.animation.animateColorAsState
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
 * Resolves the theme family and the light/dark choice into the token set the
 * whole app reads.
 *
 * Two independent axes, exactly as the canvas has them: **which family**
 * (Warm / Mono) and **which scheme** (System / Light / Dark). Every family is
 * designed in both schemes, so the two never need to agree.
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
    val target = Palettes.of(theme, dark)

    // The canvas puts `transition:background-color .3s,color .3s,border-color
    // .3s,fill .3s,stroke .3s` on *every* element, so a change of family or
    // scheme is a cross-fade of the whole surface rather than a hard cut.
    val page by animateColorAsState(target.page, Motion.theme(), label = "page")
    val card by animateColorAsState(target.card, Motion.theme(), label = "card")
    val elev by animateColorAsState(target.elev, Motion.theme(), label = "elev")
    val ink by animateColorAsState(target.ink, Motion.theme(), label = "ink")
    val sub by animateColorAsState(target.sub, Motion.theme(), label = "sub")
    val line by animateColorAsState(target.line, Motion.theme(), label = "line")
    val acc by animateColorAsState(target.acc, Motion.theme(), label = "acc")
    val colors = target.copy(
        page = page, card = card, elev = elev, ink = ink, sub = sub, line = line, acc = acc
    )

    val context = LocalContext.current
    val reduced = remember {
        Settings.Global.getFloat(
            context.contentResolver,
            Settings.Global.ANIMATOR_DURATION_SCALE, 1f
        ) == 0f
    }

    val scheme = if (dark) {
        darkColorScheme(
            primary = colors.acc, onPrimary = colors.accInk,
            background = colors.page, surface = colors.card,
            onBackground = colors.ink, onSurface = colors.ink
        )
    } else {
        lightColorScheme(
            primary = colors.acc, onPrimary = colors.accInk,
            background = colors.page, surface = colors.card,
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

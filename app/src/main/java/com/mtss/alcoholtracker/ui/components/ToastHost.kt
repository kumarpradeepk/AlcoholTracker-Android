package com.mtss.alcoholtracker.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mtss.alcoholtracker.R
import com.mtss.alcoholtracker.ui.ToastData
import com.mtss.alcoholtracker.ui.theme.LocalAppColors
import com.mtss.alcoholtracker.ui.theme.Motion
import com.mtss.alcoholtracker.ui.theme.text

/** The dark pill snackbar with its amber Undo, floating above the tab bar. */
@Composable
fun ToastHost(
    toast: ToastData?,
    onUndo: () -> Unit,
    modifier: Modifier = Modifier
) {
    val c = LocalAppColors.current
    // Hold the last non-null toast so the exit animation still has content.
    var last by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf<ToastData?>(null) }
    if (toast != null) last = toast
    Box(modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        AnimatedVisibility(
            visible = toast != null,
            enter = slideInVertically(tween(400, easing = Motion.Springy)) { it / 2 } + fadeIn(tween(250)),
            exit = slideOutVertically(tween(220)) { it / 3 } + fadeOut(tween(200))
        ) {
            val data = toast ?: last ?: return@AnimatedVisibility
            Row(
                Modifier
                    .widthIn(max = 340.dp)
                    .shadow(14.dp, RoundedCornerShape(22.dp), spotColor = androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.5f))
                    .background(c.ink, RoundedCornerShape(22.dp))
                    .padding(horizontal = 18.dp, vertical = 11.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    data.message,
                    style = text(14.sp, FontWeight.Medium),
                    color = c.bg
                )
                if (data.undoable) {
                    Text(
                        stringResource(R.string.action_undo),
                        style = text(14.sp, FontWeight.Bold),
                        color = c.amber,
                        modifier = Modifier.clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = onUndo
                        )
                    )
                }
            }
        }
    }
}

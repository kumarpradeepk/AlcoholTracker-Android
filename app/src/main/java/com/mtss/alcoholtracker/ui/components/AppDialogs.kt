package com.mtss.alcoholtracker.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.mtss.alcoholtracker.ui.theme.LocalAppColors
import com.mtss.alcoholtracker.ui.theme.figure
import com.mtss.alcoholtracker.ui.theme.text

/**
 * The centered 282dp confirmation card. Destructive confirmations are the one
 * place the design allows a dialog — never on the logging path.
 */
@Composable
fun ConfirmDialog(
    title: String,
    body: String,
    cancelLabel: String,
    confirmLabel: String,
    destructive: Boolean,
    onCancel: () -> Unit,
    onConfirm: () -> Unit
) {
    val c = LocalAppColors.current
    Dialog(onDismissRequest = onCancel, properties = DialogProperties()) {
        Column(
            Modifier
                .width(282.dp)
                .shadow(24.dp, RoundedCornerShape(22.dp))
                .clip(RoundedCornerShape(22.dp))
                .background(c.card)
                .padding(22.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(title, style = figure(17.sp, tabular = false), color = c.ink, textAlign = TextAlign.Center)
            Text(
                body,
                style = text(13.5.sp),
                color = c.sub,
                textAlign = TextAlign.Center,
                lineHeight = 20.sp,
                modifier = Modifier.padding(top = 8.dp)
            )
            Row(
                Modifier.fillMaxWidth().padding(top = 18.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    Modifier
                        .weight(1f)
                        .height(44.dp)
                        .clip(RoundedCornerShape(22.dp))
                        .background(c.elev)
                        .pressable(pressedScale = 0.95f, onClick = onCancel),
                    contentAlignment = Alignment.Center
                ) {
                    Text(cancelLabel, style = text(15.sp, FontWeight.SemiBold), color = c.ink)
                }
                Box(
                    Modifier
                        .weight(1f)
                        .height(44.dp)
                        .clip(RoundedCornerShape(22.dp))
                        .background(if (destructive) c.danger else c.acc)
                        .pressable(pressedScale = 0.95f, onClick = onConfirm),
                    contentAlignment = Alignment.Center
                ) {
                    Text(confirmLabel, style = text(15.sp, FontWeight.SemiBold), color = Color.White)
                }
            }
        }
    }
}

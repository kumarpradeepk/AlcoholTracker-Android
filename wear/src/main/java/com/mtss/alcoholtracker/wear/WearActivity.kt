package com.mtss.alcoholtracker.wear

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material.Text
import com.mtss.alcoholtracker.wear.sync.WearSync
import com.mtss.alcoholtracker.wear.ui.DryDayPage
import com.mtss.alcoholtracker.wear.ui.GlancePage
import com.mtss.alcoholtracker.wear.ui.QuickLogPage
import com.mtss.alcoholtracker.wear.ui.W
import com.mtss.alcoholtracker.wear.ui.WOvershoot
import com.mtss.alcoholtracker.wear.ui.WSettle
import com.mtss.alcoholtracker.wear.ui.WatchDroplet
import com.mtss.alcoholtracker.wear.ui.ConfirmCheck
import com.mtss.alcoholtracker.wear.ui.pressable
import com.mtss.alcoholtracker.wear.ui.wtext
import kotlinx.coroutines.delay

class WearActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { WearRoot() }
    }

    override fun onResume() {
        super.onResume()
        // The listener service may have missed a snapshot while the app was
        // closed; read whatever is currently on the node.
        WearSync.hydrate(this)
    }
}

/**
 * The watch app: three pages side by side, swiped horizontally, with the page
 * dots and the clock pinned above them.
 *
 * The canvas is a single 300%-wide strip translated by a third at a time; the
 * same shape is used here rather than a pager component, because the dots have
 * to animate their width in step with the strip and a pager would own that
 * state separately.
 */
@Composable
private fun WearRoot() {
    val context = LocalContext.current
    val snapshot by WearSync.state.collectAsState()
    var page by remember { mutableIntStateOf(0) }
    var toast by remember { mutableStateOf<String?>(null) }
    var confirming by remember { mutableStateOf(false) }
    var splash by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        delay(900)
        splash = false
    }
    LaunchedEffect(toast) {
        if (toast != null) {
            delay(4000)
            toast = null
        }
    }
    LaunchedEffect(confirming) {
        if (confirming) {
            delay(1250)
            confirming = false
        }
    }

    val widthDp = LocalConfiguration.current.screenWidthDp.dp
    val shift by animateDpAsState(
        targetValue = -widthDp * page,
        animationSpec = tween(450, easing = WSettle),
        label = "page"
    )

    Box(
        Modifier
            .fillMaxSize()
            .background(W.screen)
            .pointerInput(Unit) {
                var dx = 0f
                detectHorizontalDragGestures(
                    onDragEnd = {
                        if (dx < -40) page = (page + 1).coerceAtMost(2)
                        if (dx > 40) page = (page - 1).coerceAtLeast(0)
                        dx = 0f
                    }
                ) { _, amount -> dx += amount }
            }
    ) {
        Row(Modifier.fillMaxSize().offset(x = shift)) {
            Box(Modifier.width(widthDp).fillMaxSize()) {
                GlancePage(
                    s = snapshot,
                    onLog = { page = 1 },
                    onDry = { page = 2 }
                )
            }
            Box(Modifier.width(widthDp).fillMaxSize()) {
                QuickLogPage(
                    s = snapshot,
                    onLog = { name, ml, abv, cost ->
                        WearSync.sendLog(context, name, ml, abv, cost)
                        toast = name
                        confirming = true
                        page = 0
                    }
                )
            }
            Box(Modifier.width(widthDp).fillMaxSize()) {
                DryDayPage(
                    s = snapshot,
                    onToggle = { dry ->
                        WearSync.sendDry(context, dry)
                        if (dry) confirming = true
                    }
                )
            }
        }

        PageDots(page, Modifier.align(Alignment.TopCenter).padding(top = 16.dp)) { page = it }

        // The confirmation the canvas plays after a log: a ripple, a popped
        // tile and the check drawing itself in.
        AnimatedVisibility(
            visible = confirming,
            enter = fadeIn(tween(150)),
            exit = fadeOut(tween(350)),
            modifier = Modifier.align(Alignment.Center)
        ) { ConfirmBurst() }

        AnimatedVisibility(
            visible = toast != null,
            enter = fadeIn(tween(200)),
            exit = fadeOut(tween(200)),
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 18.dp)
        ) {
            UndoToast(
                label = toast.orEmpty(),
                onUndo = {
                    WearSync.sendUndo(context)
                    toast = null
                }
            )
        }

        AnimatedVisibility(
            visible = splash,
            exit = fadeOut(tween(400)),
            modifier = Modifier.fillMaxSize()
        ) { Splash() }
    }
}

@Composable
private fun PageDots(page: Int, modifier: Modifier = Modifier, onTap: (Int) -> Unit) {
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
        repeat(3) { i ->
            val w by animateDpAsState(
                targetValue = if (i == page) 18.dp else 6.dp,
                animationSpec = tween(350, easing = WOvershoot),
                label = "dot"
            )
            Box(
                Modifier
                    .size(width = w, height = 6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(if (i == page) W.acc else W.elev)
            )
        }
    }
}

@Composable
private fun Splash() {
    Box(Modifier.fillMaxSize().background(W.screen), contentAlignment = Alignment.Center) {
        WatchDroplet(size = 54.dp)
    }
}

@Composable
private fun ConfirmBurst() {
    ConfirmCheck()
}

@Composable
private fun UndoToast(label: String, onUndo: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 22.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(W.elev)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            stringResource(R.string.w_logged, label),
            style = wtext(12.sp, FontWeight.SemiBold),
            color = W.ink,
            maxLines = 1
        )
        Text(
            stringResource(R.string.w_undo),
            style = wtext(12.sp, FontWeight.Bold),
            color = W.acc,
            modifier = Modifier
                .padding(start = 10.dp)
                .clip(RoundedCornerShape(10.dp))
                .pressable(onClick = onUndo)
                .padding(horizontal = 6.dp, vertical = 4.dp)
        )
    }
}

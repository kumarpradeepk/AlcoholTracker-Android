package com.mtss.alcoholtracker.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mtss.alcoholtracker.R
import com.mtss.alcoholtracker.domain.AlcoholMath
import com.mtss.alcoholtracker.domain.UnitsConfig
import com.mtss.alcoholtracker.ui.components.ConfirmDialog
import com.mtss.alcoholtracker.ui.components.DropletMark
import com.mtss.alcoholtracker.ui.components.FabGlass
import com.mtss.alcoholtracker.ui.components.ToastHost
import com.mtss.alcoholtracker.ui.components.pressable
import com.mtss.alcoholtracker.ui.screens.BaselineScreen
import com.mtss.alcoholtracker.ui.screens.DiaryScreen
import com.mtss.alcoholtracker.ui.screens.GoalsScreen
import com.mtss.alcoholtracker.ui.screens.PaywallScreen
import com.mtss.alcoholtracker.ui.screens.PushHost
import com.mtss.alcoholtracker.ui.screens.SettingsScreen
import com.mtss.alcoholtracker.ui.screens.SheetHost
import com.mtss.alcoholtracker.ui.screens.StatisticsScreen
import com.mtss.alcoholtracker.ui.screens.WelcomeScreen
import com.mtss.alcoholtracker.ui.theme.AlcoholTrackerTheme
import com.mtss.alcoholtracker.ui.theme.AppTheme
import com.mtss.alcoholtracker.ui.theme.LocalAppColors
import com.mtss.alcoholtracker.ui.theme.Motion
import com.mtss.alcoholtracker.ui.theme.text
import com.mtss.alcoholtracker.util.Formatters

@Composable
fun Root(vm: AppViewModel) {
    val settings by vm.settings.collectAsState()
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    // Rebind the locale-sensitive singletons on every configuration change, so a
    // language or region switch takes effect without a process restart.
    LaunchedEffect(configuration) {
        Formatters.bind(context)
        AlcoholMath.bind(UnitsConfig.countryFor(context))
    }
    AlcoholTrackerTheme(
        theme = AppTheme.from(settings.themeId),
        darkChoice = settings.darkChoice
    ) {
        val c = LocalAppColors.current
        Box(Modifier.fillMaxSize().background(c.bg)) {
            Box(
                Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(WindowInsets.statusBars)
            ) {
                when (vm.phase) {
                    Phase.BOOT -> {}
                    Phase.WELCOME -> WelcomeScreen(vm)
                    Phase.GOALS -> GoalsScreen(vm)
                    Phase.BASELINE -> BaselineScreen(vm)
                    Phase.PAYWALL -> PaywallScreen(vm)
                    Phase.APP -> MainScaffold(vm)
                }
            }

            // Toast overlay (above the tab bar, below dialogs)
            ToastHost(
                toast = vm.toast,
                onUndo = { vm.undoLast() },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 118.dp)
            )

            // Dialogs
            when (vm.dialog) {
                AppDialog.DELETE_ENTRY -> ConfirmDialog(
                    title = stringResource(R.string.dialog_delete_title),
                    body = stringResource(R.string.dialog_delete_body),
                    cancelLabel = stringResource(R.string.dialog_delete_keep),
                    confirmLabel = stringResource(R.string.dialog_delete_remove),
                    destructive = true,
                    onCancel = { vm.closeDialog() }, onConfirm = { vm.confirmDelete() }
                )
                AppDialog.CLEAR_ALL -> ConfirmDialog(
                    title = stringResource(R.string.dialog_clear_title),
                    body = stringResource(R.string.dialog_clear_body),
                    cancelLabel = stringResource(R.string.action_cancel),
                    confirmLabel = stringResource(R.string.dialog_clear_delete),
                    destructive = true,
                    onCancel = { vm.closeDialog() }, onConfirm = { vm.confirmClearAll() }
                )
                AppDialog.HEALTH_CONNECT -> ConfirmDialog(
                    title = stringResource(
                        R.string.dialog_health_title,
                        stringResource(R.string.app_name),
                        AppViewModel.HEALTH_CONNECT
                    ),
                    body = stringResource(R.string.sheet_health_data_sub),
                    cancelLabel = stringResource(R.string.dialog_health_deny),
                    confirmLabel = stringResource(R.string.dialog_health_allow),
                    destructive = false,
                    onCancel = { vm.closeDialog() }, onConfirm = { vm.confirmHealth() }
                )
                null -> {}
            }

            // Sheets
            SheetHost(vm)

            // Boot overlay
            AnimatedVisibility(
                visible = vm.phase == Phase.BOOT,
                exit = fadeOut(tween(600))
            ) {
                Column(
                    Modifier
                        .fillMaxSize()
                        .background(c.bg),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    DropletMark(46.dp)
                    Text(
                        stringResource(R.string.app_name),
                        style = text(15.sp, FontWeight.SemiBold, letterSpacing = 0.4.sp),
                        color = c.muted,
                        modifier = Modifier.padding(top = 18.dp)
                    )
                }
            }

            // App lock veil — nothing is shown until unlocked.
            if (vm.locked) {
                Column(
                    Modifier
                        .fillMaxSize()
                        .background(c.bg)
                        .pressable(pressedScale = 1f) { vm.hostActions?.showBiometricPrompt() },
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    DropletMark(46.dp)
                    Text(
                        stringResource(R.string.lock_tap_to_unlock),
                        style = text(15.sp, FontWeight.SemiBold),
                        color = c.muted,
                        modifier = Modifier.padding(top = 18.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun MainScaffold(vm: AppViewModel) {
    val c = LocalAppColors.current
    Box(Modifier.fillMaxSize()) {
        // Tab content
        Box(Modifier.fillMaxSize()) {
            AnimatedContent(
                targetState = vm.tab,
                transitionSpec = { fadeIn(tween(350)) togetherWith fadeOut(tween(200)) },
                label = "tab"
            ) { tab ->
                when (tab) {
                    Tab.DIARY -> DiaryScreen(vm)
                    Tab.STATS -> StatisticsScreen(vm)
                    Tab.SETTINGS -> SettingsScreen(vm)
                }
            }
            // Push panel slides over the content, tab bar stays.
            PushHost(vm)
        }

        // FAB cluster (Diary only)
        if (vm.tab == Tab.DIARY && vm.push == null) {
            FabCluster(vm, Modifier.align(Alignment.BottomEnd))
        }

        // Tab bar
        TabBar(vm, Modifier.align(Alignment.BottomCenter))
    }
}

@Composable
private fun FabCluster(vm: AppViewModel, modifier: Modifier = Modifier) {
    val c = LocalAppColors.current
    val open = vm.fabOpen
    val actionAlpha by animateFloatAsState(if (open) 1f else 0f, tween(300), label = "fabA")
    val actionTy by animateFloatAsState(if (open) 0f else 16f, tween(380, easing = Motion.SpringyMild), label = "fabTy")
    val rot by animateFloatAsState(if (open) -22f else 0f, tween(400, easing = Motion.Springy), label = "fabRot")
    Column(
        modifier.padding(end = 20.dp, bottom = 112.dp),
        horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        if (actionAlpha > 0.01f) {
            FabAction(stringResource(R.string.action_dry_day), c.b1, actionAlpha, actionTy) {
                vm.fabOpen = false
                vm.markDry(vm.selectedDay())
            }
            FabAction(stringResource(R.string.action_log_drink), c.accent, actionAlpha, actionTy) {
                vm.fabOpen = false
                vm.startLog()
            }
        }
        Box(
            Modifier
                .size(58.dp)
                .shadow(10.dp, CircleShape, spotColor = Color.Black.copy(alpha = 0.4f))
                .clip(CircleShape)
                .background(c.accent)
                .pressable(pressedScale = 0.9f) { vm.fabOpen = !vm.fabOpen },
            contentAlignment = Alignment.Center
        ) {
            Box(Modifier.rotate(rot).scale(if (open) 1.08f else 1f)) {
                FabGlass(size = 32.dp)
            }
        }
    }
}

@Composable
private fun FabAction(label: String, color: Color, alpha: Float, ty: Float, onClick: () -> Unit) {
    val c = LocalAppColors.current
    Box(
        Modifier
            .alpha(alpha)
            .offset(y = ty.dp)
            .height(42.dp)
            .shadow(10.dp, RoundedCornerShape(21.dp), spotColor = Color.Black.copy(alpha = 0.35f))
            .clip(RoundedCornerShape(21.dp))
            .background(c.surface)
            .pressable(pressedScale = 0.95f, onClick = onClick)
            .padding(horizontal = 18.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(label, style = text(14.5.sp, FontWeight.SemiBold), color = color)
    }
}

@Composable
private fun TabBar(vm: AppViewModel, modifier: Modifier = Modifier) {
    val c = LocalAppColors.current
    Row(
        modifier
            .fillMaxWidth()
            .background(c.surface)
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(horizontal = 14.dp)
            .padding(top = 10.dp, bottom = 10.dp)
    ) {
        // B1: the tab bar gets the SHORT caption; the full platform term
        // (Einstellungen, Configuración, Statistiques, …) stays the screen H1 and
        // the accessibility label. Nothing is truncated and no synonym is invented.
        TabItem(
            vm, Tab.DIARY,
            stringResource(R.string.tab_diary_short),
            stringResource(R.string.tab_diary)
        ) { color, pop ->
            Canvas(Modifier.size(22.dp, 19.dp).scale(pop)) {
                drawRoundRect(
                    color,
                    topLeft = Offset(0f, 0f),
                    size = size,
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(4.dp.toPx()),
                    style = Stroke(2.dp.toPx())
                )
                drawLine(color, Offset(size.width / 2, 0f), Offset(size.width / 2, size.height), 2.dp.toPx())
            }
        }
        TabItem(
            vm, Tab.STATS,
            stringResource(R.string.tab_statistics_short),
            stringResource(R.string.tab_statistics)
        ) { color, pop ->
            Canvas(Modifier.size(22.dp, 19.dp).scale(pop)) {
                val w = size.width; val h = size.height
                drawLine(color, Offset(w * 0.18f, h), Offset(w * 0.18f, h * 0.64f), 3.dp.toPx(), StrokeCap.Round)
                drawLine(color, Offset(w * 0.5f, h), Offset(w * 0.5f, h * 0.08f), 3.dp.toPx(), StrokeCap.Round)
                drawLine(color, Offset(w * 0.82f, h), Offset(w * 0.82f, h * 0.46f), 3.dp.toPx(), StrokeCap.Round)
            }
        }
        TabItem(
            vm, Tab.SETTINGS,
            stringResource(R.string.tab_settings_short),
            stringResource(R.string.tab_settings)
        ) { color, pop ->
            Canvas(Modifier.size(22.dp, 19.dp).scale(pop)) {
                val w = size.width; val h = size.height
                drawLine(color, Offset(0f, h * 0.3f), Offset(w, h * 0.3f), 2.dp.toPx(), StrokeCap.Round)
                drawCircle(c.bg, 3.dp.toPx(), Offset(w * 0.36f, h * 0.3f))
                drawCircle(color, 3.dp.toPx(), Offset(w * 0.36f, h * 0.3f), style = Stroke(2.dp.toPx()))
                drawLine(color, Offset(0f, h * 0.7f), Offset(w, h * 0.7f), 2.dp.toPx(), StrokeCap.Round)
                drawCircle(c.bg, 3.dp.toPx(), Offset(w * 0.64f, h * 0.7f))
                drawCircle(color, 3.dp.toPx(), Offset(w * 0.64f, h * 0.7f), style = Stroke(2.dp.toPx()))
            }
        }
    }
}

@Composable
private fun androidx.compose.foundation.layout.RowScope.TabItem(
    vm: AppViewModel,
    tab: Tab,
    label: String,
    accessibilityLabel: String,
    icon: @Composable (Color, Float) -> Unit
) {
    val c = LocalAppColors.current
    val selected = vm.tab == tab
    val color by androidx.compose.animation.animateColorAsState(
        if (selected) c.accent else c.faint, tween(300), label = "tabColor"
    )
    // Pop when this tab becomes selected.
    val pop = remember { Animatable(1f) }
    LaunchedEffect(selected) {
        if (selected) {
            pop.snapTo(0.72f)
            pop.animateTo(1f, tween(500, easing = Motion.Springy))
        }
    }
    Column(
        Modifier
            .weight(1f)
            .pressable(pressedScale = 0.92f) { vm.selectTab(tab) }
            .padding(vertical = 4.dp)
            // The visible caption is the short one; the accessibility label is
            // the full platform term, so a screen reader never hears "Mehr".
            .clearAndSetSemantics { contentDescription = accessibilityLabel },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        icon(color, pop.value)
        Text(label, style = text(10.5.sp, FontWeight.SemiBold), color = color, maxLines = 1)
    }
}

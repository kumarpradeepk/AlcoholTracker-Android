package com.mtss.alcoholtracker.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import androidx.compose.ui.unit.sp
import com.mtss.alcoholtracker.R
import com.mtss.alcoholtracker.data.DrinkLog
import com.mtss.alcoholtracker.data.Tone
import com.mtss.alcoholtracker.domain.AlcoholMath
import com.mtss.alcoholtracker.domain.rememberUnits
import com.mtss.alcoholtracker.domain.unitsPlural
import com.mtss.alcoholtracker.domain.unitsString
import com.mtss.alcoholtracker.ui.AppViewModel
import com.mtss.alcoholtracker.ui.CalMode
import com.mtss.alcoholtracker.ui.PushScreen
import com.mtss.alcoholtracker.ui.Sheet
import com.mtss.alcoholtracker.ui.components.AppCard
import com.mtss.alcoholtracker.ui.components.CheckMark
import com.mtss.alcoholtracker.ui.components.Chevron
import com.mtss.alcoholtracker.ui.components.ChevronDirection
import com.mtss.alcoholtracker.ui.components.DropletMark
import com.mtss.alcoholtracker.ui.components.GlassIcon
import com.mtss.alcoholtracker.ui.components.PrimaryButton
import com.mtss.alcoholtracker.ui.components.ProgressRing
import com.mtss.alcoholtracker.ui.components.RoundIconButton
import com.mtss.alcoholtracker.ui.components.SoftButton
import com.mtss.alcoholtracker.ui.components.SwirlingGlass
import com.mtss.alcoholtracker.ui.components.animatedValue
import com.mtss.alcoholtracker.ui.components.pressable
import com.mtss.alcoholtracker.ui.components.rememberInfiniteBreath
import com.mtss.alcoholtracker.ui.components.riseIn
import com.mtss.alcoholtracker.ui.theme.LocalAppColors
import com.mtss.alcoholtracker.ui.theme.LocalReducedMotion
import com.mtss.alcoholtracker.ui.theme.figure
import com.mtss.alcoholtracker.ui.theme.text
import com.mtss.alcoholtracker.util.Formatters
import kotlin.math.min
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.width
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import com.mtss.alcoholtracker.ui.components.AppIcon
import com.mtss.alcoholtracker.ui.components.AppIcons
import com.mtss.alcoholtracker.ui.theme.Motion
import com.mtss.alcoholtracker.ui.theme.Radii
import com.mtss.alcoholtracker.ui.theme.duration
import com.mtss.alcoholtracker.ui.theme.fadeUp
import kotlin.math.roundToInt

@Composable
fun DiaryScreen(vm: AppViewModel) {
    val c = LocalAppColors.current
    val units = rememberUnits()
    val settings by vm.settings.collectAsState()
    val logsAll by vm.logs.collectAsState()
    val dryDays by vm.dryDays.collectAsState()

    val today = vm.todayKey()
    val day = vm.selectedDay()
    val dayLogs = remember(logsAll, day) { logsAll.filter { it.epochDay == day }.sortedBy { it.atMillis } }
    val dayUnits = dayLogs.sumOf { AlcoholMath.units(it.ml, it.abv) }
    val isDry = dryDays.contains(day) && dayLogs.isEmpty()

    val scroll = rememberScrollState()
    val shrunk = scroll.value > 90

    Column(Modifier.fillMaxSize()) {
        // Header
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp)
                .padding(top = 18.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            Column {
                val fs by animateFloatAsState(if (shrunk) 22f else 32f, tween(300), label = "titleFs")
                Text(
                    Formatters.dayTitle(LocalContext.current, day, today),
                    style = text(fs.sp, FontWeight.Bold, letterSpacing = (-0.5).sp),
                    color = c.ink
                )
                Text(
                    Formatters.daySubtitle(day),
                    style = text(14.sp), color = c.sub,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                RoundIconButton(
                    onClick = { vm.dayPrev() },
                    size = 42.dp,
                    contentDescription = stringResource(R.string.a11y_prev_day)
                ) { AppIcon(AppIcons.ChevronLeft, size = 18.dp, tint = c.sub) }
                RoundIconButton(
                    onClick = { vm.openCalendar(CalMode.SELECT) },
                    size = 42.dp,
                    contentDescription = stringResource(R.string.a11y_open_calendar)
                ) { AppIcon(AppIcons.Calendar, size = 18.dp, tint = c.sub) }
                RoundIconButton(
                    onClick = { vm.dayNext() },
                    size = 42.dp,
                    enabled = day < today,
                    contentDescription = stringResource(R.string.a11y_next_day)
                ) { AppIcon(AppIcons.ChevronRight, size = 18.dp, tint = c.sub) }
            }
        }

        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(scroll)
                .padding(horizontal = 18.dp)
                .padding(bottom = 150.dp)
        ) {
            // Pro banner
            if (!settings.pro) {
                Row(
                    Modifier
                        .padding(top = 16.dp)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(c.elev)
                        .pressable(pressedScale = 0.98f) { vm.openPaywall() }
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    DropletMark(22.dp, breathing = false)
                    Column(Modifier.weight(1f)) {
                        Text(stringResource(R.string.diary_pro_banner_title), style = text(14.5.sp, FontWeight.SemiBold), color = c.ink)
                        Text(
                            stringResource(R.string.diary_pro_banner_sub),
                            style = text(12.5.sp), color = c.sub, modifier = Modifier.padding(top = 1.dp)
                        )
                    }
                    Chevron(ChevronDirection.RIGHT, c.sub, 12.dp)
                }
            }

            // Quick log
            val quick = vm.quickItems()
            if (quick.isNotEmpty()) {
                Text(stringResource(R.string.diary_usual), style = text(13.sp), color = c.sub, modifier = Modifier.padding(top = 16.dp))
                Row(
                    Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(top = 8.dp, bottom = 2.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    quick.forEach { p ->
                        Row(
                            Modifier
                                .height(44.dp)
                                .shadow(if (c.isDark) 0.dp else 6.dp, RoundedCornerShape(22.dp), spotColor = Color.Black.copy(alpha = 0.25f))
                                .clip(RoundedCornerShape(22.dp))
                                .background(c.card)
                                .pressable(pressedScale = 0.94f) { vm.quickLog(p) }
                                .padding(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(Modifier.size(6.dp).clip(CircleShape).background(c.acc))
                            Text(p.name, style = text(14.sp, FontWeight.SemiBold), color = c.ink, maxLines = 1)
                            Text(
                                stringResource(
                                    R.string.units_abbrev_value,
                                    AlcoholMath.units(p.ml, p.abv),
                                    units.noun.abbrev
                                ),
                                style = text(12.5.sp, tabular = true), color = c.sub, maxLines = 1
                            )
                        }
                    }
                }
            }

            // Intake card
            IntakeCard(vm, dayUnits, settings.dailyGoal, settings.weeklyGoal, settings.monthlyGoal, settings.tone, day == today)

            // BAC card
            when {
                !settings.pro -> BacFreeCard(vm)
                vm.bacNow() == null -> BacSetupCard(vm)
                settings.bacOn -> BacLiveCard(vm)
            }

            // Dry-day card
            if (isDry) {
                DryDayCard(
                    heading = stringResource(
                        if (day == today) R.string.diary_dry_marked else R.string.diary_dry_remembered
                    ),
                    onUnmark = { vm.unDry(day) }
                )
            }

            // Entries or empty state
            if (dayLogs.isNotEmpty()) {
                Text(
                    stringResource(
                        if (day == today) R.string.diary_logged_today else R.string.diary_logged_this_day
                    ),
                    style = text(13.sp, FontWeight.SemiBold, letterSpacing = 0.6.sp),
                    color = c.sub,
                    modifier = Modifier.padding(top = 22.dp)
                )
                dayLogs.forEachIndexed { i, log ->
                    EntryRow(log, delay = i * 45) { vm.openSheet(Sheet.Entry(log)) }
                }
            } else if (!isDry) {
                EmptyDiary(vm, isToday = day == today)
            }
        }
    }
}

@Composable
private fun IntakeCard(
    vm: AppViewModel,
    dayUnits: Double,
    dailyGoal: Int,
    weeklyGoal: Int,
    monthlyGoal: Int,
    tone: Tone,
    isToday: Boolean
) {
    val c = LocalAppColors.current
    val units = rememberUnits()
    val unitsInfoLabel = unitsString(R.string.a11y_units_info, units.noun.indefinite)
    val animated = animatedValue(dayUnits)
    val ratio = dayUnits / dailyGoal
    val wkU = vm.weekUnits()

    // The hero is the one filled surface in the design: the accent carries the
    // whole card and everything on it is drawn in onAcc, so none of the page
    // tokens apply inside. It is also the only card with a shadow.
    Column(
        Modifier
            .padding(top = 14.dp)
            .fadeUp(delayMillis = 60)
            .fillMaxWidth()
            .shadow(
                elevation = 22.dp,
                shape = RoundedCornerShape(Radii.HERO.dp),
                spotColor = Color.Black.copy(alpha = 0.35f),
                ambientColor = Color.Transparent
            )
            .clip(RoundedCornerShape(Radii.HERO.dp))
            .background(c.acc)
            .padding(20.dp)
    ) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AppIcon(AppIcons.DropletFill, size = 16.dp, tint = c.accInk)
                Text(
                    stringResource(if (isToday) R.string.diary_intake_today else R.string.diary_intake),
                    style = text(14.sp, FontWeight.Bold), color = c.accInk
                )
            }
            Box(
                Modifier
                    .size(30.dp)
                    .clip(RoundedCornerShape(11.dp))
                    .background(c.accInk.copy(alpha = 0.16f))
                    .pressable(pressedScale = 0.9f) { vm.openSheet(Sheet.UnitsInfo) }
                    .semantics { contentDescription = unitsInfoLabel },
                contentAlignment = Alignment.Center
            ) {
                Text("i", style = text(13.sp, FontWeight.ExtraBold), color = c.accInk)
            }
        }

        // The figure leads and the ring is the counterweight, which is the
        // reverse of the previous direction's centred ring.
        Row(
            Modifier.fillMaxWidth().padding(top = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    Formatters.one(animated),
                    style = figure(62.sp), color = c.accInk, maxLines = 1
                )
                Text(
                    unitsPlural(R.plurals.diary_ring_of_goal, dailyGoal, dailyGoal, units.noun.plural),
                    style = text(13.5.sp, FontWeight.SemiBold),
                    color = c.accInk.copy(alpha = 0.8f),
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
            HeroRing(
                progress = (dayUnits / dailyGoal).toFloat(),
                track = c.accInk.copy(alpha = 0.22f),
                stroke = c.accInk,
                label = stringResource(R.string.diary_ring_percent, (ratio * 100).roundToInt()),
                labelColor = c.accInk
            )
        }

        // Seven days at a glance, growing from the baseline.
        Row(
            Modifier.fillMaxWidth().padding(top = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            WeekBars(vm, dailyGoal, onAcc = c.accInk)
            Text(
                stringResource(R.string.diary_mini_value, wkU, weeklyGoal),
                style = text(12.sp, FontWeight.SemiBold, tabular = true),
                color = c.accInk.copy(alpha = 0.8f)
            )
        }

        Row(
            Modifier.fillMaxWidth().padding(top = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // The pill takes what is left after the action, and truncates: the
            // remaining line is a whole localized sentence in several packs and
            // will happily push "Edit goal" onto a second line otherwise.
            Box(
                Modifier
                    .weight(1f, fill = false)
                    .clip(RoundedCornerShape(999.dp))
                    .background(c.accInk.copy(alpha = 0.16f))
                    .padding(horizontal = 13.dp, vertical = 8.dp)
            ) {
                Text(
                    vm.remainingLine(LocalContext.current, dayUnits, tone, dailyGoal, units.noun),
                    style = text(12.5.sp, FontWeight.Bold), color = c.accInk,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
            }
            Text(
                stringResource(R.string.diary_edit_goal),
                style = text(12.5.sp, FontWeight.Bold),
                color = c.accInk.copy(alpha = 0.85f),
                maxLines = 1,
                softWrap = false,
                modifier = Modifier
                    .padding(start = 8.dp)
                    .pressable { vm.openPush(PushScreen.GUIDE) }
                    .padding(horizontal = 4.dp, vertical = 8.dp)
            )
        }
    }
}

/**
 * The hero's 86dp ring. Sweeps its arc rather than snapping — the canvas
 * animates `stroke-dashoffset` over 700 ms on the same curve.
 */
@Composable
private fun HeroRing(
    progress: Float,
    track: Color,
    stroke: Color,
    label: String,
    labelColor: Color
) {
    val reduced = LocalReducedMotion.current
    val p by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = tween(duration(reduced, Motion.RING), easing = Motion.Sweep),
        label = "heroRing"
    )
    Box(Modifier.size(86.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val w = 7.dp.toPx()
            val inset = w / 2
            // `size` would bind to the named argument below, not the receiver.
            val box = androidx.compose.ui.geometry.Size(size.width - w, size.height - w)
            drawArc(
                color = track, startAngle = 0f, sweepAngle = 360f, useCenter = false,
                topLeft = androidx.compose.ui.geometry.Offset(inset, inset),
                size = box,
                style = Stroke(w)
            )
            drawArc(
                color = stroke, startAngle = -90f, sweepAngle = 360f * p, useCenter = false,
                topLeft = androidx.compose.ui.geometry.Offset(inset, inset),
                size = box,
                style = Stroke(w, cap = StrokeCap.Round)
            )
        }
        Text(label, style = text(16.sp, FontWeight.ExtraBold, tabular = true), color = labelColor)
    }
}

/**
 * The seven-day strip inside the hero. Bars are coloured by how far through the
 * daily goal each day went, and grow from the baseline with the canvas's
 * staggered `growBar`.
 */
@Composable
private fun WeekBars(vm: AppViewModel, dailyGoal: Int, onAcc: Color) {
    val reduced = LocalReducedMotion.current
    val today = vm.todayKey()
    val days = remember(today, dailyGoal, vm.logs.value) {
        ((today - 6)..today).map { vm.dayUnits(it) }
    }
    val peak = maxOf(days.maxOrNull() ?: 0.0, dailyGoal.toDouble(), 0.1)
    Row(
        Modifier.height(34.dp),
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        days.forEachIndexed { i, u ->
            val target = (u / peak).toFloat().coerceIn(0.06f, 1f)
            val h by animateFloatAsState(
                targetValue = target,
                animationSpec = tween(
                    durationMillis = duration(reduced, Motion.GROW_BAR),
                    delayMillis = duration(reduced, i * 40),
                    easing = Motion.Sweep
                ),
                label = "weekBar"
            )
            Box(
                Modifier
                    .width(7.dp)
                    .fillMaxHeight(h)
                    .clip(RoundedCornerShape(4.dp))
                    .background(
                        // Over goal reads at full strength; a quiet day recedes.
                        if (u > dailyGoal) onAcc else onAcc.copy(alpha = 0.45f)
                    )
            )
        }
    }
}


@Composable
private fun MiniProgress(label: String, valueLabel: String, fraction: Float) {
    val c = LocalAppColors.current
    val anim by animateFloatAsState(fraction.coerceIn(0f, 1f), tween(900, easing = com.mtss.alcoholtracker.ui.theme.Motion.Sweep), label = "mini")
    Column {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, style = text(12.5.sp), color = c.sub)
            Text(valueLabel, style = text(12.5.sp, FontWeight.SemiBold, tabular = true), color = c.ink)
        }
        Box(
            Modifier
                .padding(top = 5.dp)
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(c.elev)
        ) {
            Box(
                Modifier
                    .fillMaxWidth(anim)
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(c.acc)
            )
        }
    }
}

@Composable
private fun BacFreeCard(vm: AppViewModel) {
    val c = LocalAppColors.current
    val reduced = LocalReducedMotion.current
    AppCard(Modifier.padding(top = 12.dp).riseIn(delayMillis = 80)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.bac_monitor), style = text(15.sp, FontWeight.SemiBold), color = c.ink)
            ProBadge { vm.openPaywall() }
        }
        Row(Modifier.padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.Bottom) {
            Text("--", style = figure(33.sp, tabular = false), color = c.sub)
            Row(
                Modifier.padding(bottom = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val pulse = if (reduced) 1f else 0.25f + 0.75f * rememberInfiniteBreath(2200)
                Box(Modifier.size(7.dp).clip(CircleShape).background(c.sub.copy(alpha = pulse)))
                Text(stringResource(R.string.bac_status_waiting), style = text(11.sp, FontWeight.Bold, letterSpacing = 1.sp), color = c.sub)
            }
        }
        Text(
            stringResource(R.string.bac_free_body),
            style = text(13.5.sp), color = c.sub, modifier = Modifier.padding(top = 6.dp)
        )
        Row(Modifier.padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(18.dp)) {
            Text(stringResource(R.string.bac_how_estimated), style = text(13.sp), color = c.acc, modifier = Modifier.pressable { vm.openSheet(Sheet.BacInfo) })
            Text(stringResource(R.string.bac_trends), style = text(13.sp), color = c.acc, modifier = Modifier.pressable { vm.openPaywall() })
        }
    }
}

@Composable
private fun BacSetupCard(vm: AppViewModel) {
    val c = LocalAppColors.current
    AppCard(Modifier.padding(top = 12.dp).riseIn(delayMillis = 80)) {
        Text(stringResource(R.string.bac_setup_title), style = text(15.sp, FontWeight.SemiBold), color = c.ink)
        Text(
            stringResource(R.string.bac_setup_body),
            style = text(13.5.sp), color = c.sub, lineHeight = 20.sp,
            modifier = Modifier.padding(top = 6.dp)
        )
        Box(
            Modifier
                .padding(top = 14.dp)
                .height(40.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(c.elev)
                .pressable(pressedScale = 0.95f) { vm.openPush(PushScreen.PROFILE) }
                .padding(horizontal = 20.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(stringResource(R.string.bac_setup_cta), style = text(14.5.sp, FontWeight.SemiBold), color = c.acc)
        }
    }
}

@Composable
private fun BacLiveCard(vm: AppViewModel) {
    val c = LocalAppColors.current
    val settings by vm.settings.collectAsState()
    val b = vm.bacNow() ?: return
    val bv = animatedValue(b.percent)
    val (chipBg, chipCol) = when (b.status) {
        AlcoholMath.BacStatus.RISING -> c.elev to c.amber
        AlcoholMath.BacStatus.SETTLING -> c.elev to c.acc
        AlcoholMath.BacStatus.CLEAR -> c.elev to c.moss
    }
    AppCard(Modifier.padding(top = 12.dp).riseIn(delayMillis = 80)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.bac_monitor), style = text(15.sp, FontWeight.SemiBold), color = c.ink)
            Box(
                Modifier.clip(RoundedCornerShape(9.dp)).background(chipBg).padding(horizontal = 9.dp, vertical = 4.dp)
            ) {
                Text(
                    stringResource(
                        when (b.status) {
                            AlcoholMath.BacStatus.RISING -> R.string.bac_status_rising
                            AlcoholMath.BacStatus.SETTLING -> R.string.bac_status_settling
                            AlcoholMath.BacStatus.CLEAR -> R.string.bac_status_clear
                        }
                    ),
                    style = text(10.5.sp, FontWeight.Bold, letterSpacing = 0.8.sp), color = chipCol
                )
            }
        }
        Text(
            if (settings.bacUnitPercent) stringResource(R.string.bac_value_percent, bv)
            else stringResource(R.string.bac_value_permille, bv * 10),
            style = figure(33.sp, tabular = true), color = c.ink,
            modifier = Modifier.padding(top = 10.dp)
        )
        Text(
            if (b.percent > 0.002)
                stringResource(R.string.bac_sober_in, AlcoholMath.formatHours(b.hoursToZero))
            else stringResource(R.string.bac_all_clear),
            style = text(14.sp), color = c.moss, modifier = Modifier.padding(top = 3.dp)
        )
        Box(Modifier.fillMaxWidth().padding(top = 14.dp).height(0.5.dp).background(c.line))
        Row(
            Modifier
                .fillMaxWidth()
                .pressable(pressedScale = 1f) { vm.openPush(PushScreen.TRENDS) }
                .padding(top = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(stringResource(R.string.bac_trends), style = text(14.sp, FontWeight.SemiBold), color = c.acc)
            Chevron(ChevronDirection.RIGHT, c.sub, 12.dp)
        }
        Text(
            stringResource(R.string.bac_disclaimer_short),
            style = text(11.5.sp), color = c.sub, lineHeight = 16.sp,
            modifier = Modifier.padding(top = 10.dp)
        )
        Text(
            stringResource(R.string.bac_how_estimated), style = text(13.sp), color = c.acc,
            modifier = Modifier.padding(top = 6.dp).pressable { vm.openSheet(Sheet.BacInfo) }
        )
    }
}

@Composable
fun ProBadge(onClick: () -> Unit) {
    val c = LocalAppColors.current
    Box(
        Modifier
            .clip(RoundedCornerShape(9.dp))
            .background(c.elev)
            .pressable(onClick = onClick)
            .padding(horizontal = 9.dp, vertical = 4.dp)
    ) {
        Text(stringResource(R.string.badge_pro), style = text(10.5.sp, FontWeight.Bold, letterSpacing = 0.5.sp), color = c.acc)
    }
}

@Composable
private fun DryDayCard(heading: String, onUnmark: () -> Unit) {
    val c = LocalAppColors.current
    Column(
        Modifier
            .padding(top = 12.dp)
            .riseIn(delayMillis = 100)
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(c.elev)
            .padding(22.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box {
            SwirlingGlass(width = 72.dp, height = 96.dp, liquid = c.moss, borderColor = c.moss.copy(alpha = 0.55f))
            Box(
                Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 0.dp)
                    .size(24.dp)
                    .shadow(4.dp, CircleShape)
                    .clip(CircleShape)
                    .background(c.moss),
                contentAlignment = Alignment.Center
            ) { CheckMark(size = 10.dp) }
        }
        Text(heading, style = figure(17.sp, tabular = false), color = c.moss, modifier = Modifier.padding(top = 12.dp))
        Text(
            stringResource(R.string.diary_dry_body),
            style = text(13.5.sp), color = c.sub, modifier = Modifier.padding(top = 5.dp)
        )
        Text(
            stringResource(R.string.diary_dry_unmark),
            style = text(13.sp).copy(textDecoration = TextDecoration.Underline),
            color = c.sub,
            modifier = Modifier.padding(top = 12.dp).pressable(onClick = onUnmark)
        )
    }
}

@Composable
fun EntryRow(log: DrinkLog, delay: Int, onClick: () -> Unit) {
    val c = LocalAppColors.current
    val mins = remember(log.atMillis) {
        val z = java.time.Instant.ofEpochMilli(log.atMillis).atZone(java.time.ZoneId.systemDefault())
        z.hour * 60 + z.minute
    }
    Row(
        Modifier
            .padding(top = 10.dp)
            .riseIn(delayMillis = delay)
            .fillMaxWidth()
            .shadow(if (c.isDark) 0.dp else 5.dp, RoundedCornerShape(18.dp), spotColor = Color.Black.copy(alpha = 0.2f))
            .clip(RoundedCornerShape(18.dp))
            .background(c.card)
            .pressable(pressedScale = 0.97f, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        GlassIcon(fillFraction = min(0.85f, (log.abv * 2.6f + 18f).toFloat() / 100f))
        Column(Modifier.weight(1f)) {
            Text(log.name, style = text(15.5.sp, FontWeight.SemiBold), color = c.ink)
            Text(
                stringResource(R.string.diary_entry_meta, log.ml.toInt(), AlcoholMath.trimAbv(log.abv)),
                style = text(13.sp), color = c.sub, modifier = Modifier.padding(top = 2.dp)
            )
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(
                stringResource(
                    R.string.units_abbrev_value,
                    AlcoholMath.units(log.ml, log.abv),
                    rememberUnits().noun.abbrev
                ),
                style = text(14.sp, FontWeight.SemiBold, tabular = true), color = c.acc
            )
            Text(Formatters.time(mins), style = text(12.sp), color = c.sub, modifier = Modifier.padding(top = 2.dp))
        }
    }
}

@Composable
private fun EmptyDiary(vm: AppViewModel, isToday: Boolean) {
    val c = LocalAppColors.current
    Column(
        Modifier
            .fillMaxWidth()
            .riseIn(delayMillis = 100, durationMillis = 550)
            .padding(top = 34.dp, bottom = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        SwirlingGlass(width = 96.dp, height = 128.dp)
        Text(
            stringResource(
                if (isToday) R.string.diary_empty_title_today else R.string.diary_empty_title_other
            ),
            style = figure(19.sp, tabular = false), color = c.ink,
            modifier = Modifier.padding(top = 24.dp)
        )
        Text(
            stringResource(
                if (isToday) R.string.diary_empty_body_today else R.string.diary_empty_body_other
            ),
            style = text(14.5.sp), color = c.sub, textAlign = TextAlign.Center, lineHeight = 22.sp,
            modifier = Modifier.padding(top = 8.dp).widthIn(max = 280.dp)
        )
        Column(
            Modifier.fillMaxWidth().padding(top = 22.dp, start = 26.dp, end = 26.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            PrimaryButton(stringResource(R.string.action_log_drink), height = 52.dp, onClick = { vm.startLog() })
            SoftButton(
                stringResource(R.string.action_mark_dry_day),
                container = c.elev, contentColor = c.moss,
                onClick = { vm.markDry(vm.selectedDay()) }
            )
            Text(
                stringResource(R.string.diary_add_previous_dry),
                style = text(14.sp), color = c.acc, textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp)
                    .pressable { vm.openCalendar(CalMode.DRY) }
            )
        }
    }
}

@Composable
private fun CalendarGlyph(color: Color) {
    Box(
        Modifier
            .size(15.dp, 14.dp)
            .border(1.8.dp, color, RoundedCornerShape(4.dp))
    ) {
        Box(Modifier.align(Alignment.TopStart).padding(start = 2.5.dp, top = 2.5.dp).size(3.dp).clip(RoundedCornerShape(1.dp)).background(color))
        Box(Modifier.align(Alignment.TopEnd).padding(end = 2.5.dp, top = 2.5.dp).size(3.dp).clip(RoundedCornerShape(1.dp)).background(color))
    }
}


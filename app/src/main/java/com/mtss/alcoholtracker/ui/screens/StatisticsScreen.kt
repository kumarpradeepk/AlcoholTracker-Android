package com.mtss.alcoholtracker.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mtss.alcoholtracker.R
import com.mtss.alcoholtracker.data.Tone
import com.mtss.alcoholtracker.domain.AlcoholMath
import com.mtss.alcoholtracker.domain.StatsEngine
import com.mtss.alcoholtracker.domain.StatsPeriod
import com.mtss.alcoholtracker.domain.rememberCurrency
import com.mtss.alcoholtracker.domain.rememberUnits
import com.mtss.alcoholtracker.domain.unitsPlural
import com.mtss.alcoholtracker.domain.unitsString
import com.mtss.alcoholtracker.ui.AppViewModel
import com.mtss.alcoholtracker.ui.Sheet
import com.mtss.alcoholtracker.ui.components.AppCard
import com.mtss.alcoholtracker.ui.components.AppSwitch
import com.mtss.alcoholtracker.ui.components.BarChart
import com.mtss.alcoholtracker.ui.components.BarDatum
import com.mtss.alcoholtracker.ui.components.Chevron
import com.mtss.alcoholtracker.ui.components.ChevronDirection
import com.mtss.alcoholtracker.ui.components.ProgressRing
import com.mtss.alcoholtracker.ui.components.RoundIconButton
import com.mtss.alcoholtracker.ui.components.ShareBar
import com.mtss.alcoholtracker.ui.components.animatedValue
import com.mtss.alcoholtracker.ui.components.pressable
import com.mtss.alcoholtracker.ui.components.riseIn
import com.mtss.alcoholtracker.ui.theme.LocalAppColors
import com.mtss.alcoholtracker.ui.theme.Motion
import com.mtss.alcoholtracker.ui.theme.text
import com.mtss.alcoholtracker.util.Formatters
import kotlin.math.roundToInt

@Composable
fun StatisticsScreen(vm: AppViewModel) {
    val c = LocalAppColors.current
    val context = LocalContext.current
    val units = rememberUnits()
    val currency = rememberCurrency()
    val settings by vm.settings.collectAsState()
    val logs by vm.logs.collectAsState()
    val dry by vm.dryDays.collectAsState()

    val range = remember(logs, dry, vm.period, vm.pageBack, vm.customFrom, vm.customTo, settings.dailyGoal, settings.cutoff) {
        StatsEngine.rangeFor(vm.period, vm.pageBack, vm.todayKey(), logs, dry, settings.dailyGoal, vm.customFrom, vm.customTo)
    }
    val (verdict, verdictDown) = StatsEngine.verdict(context, range, settings.tone, units.noun)

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 18.dp)
            .padding(bottom = 150.dp)
    ) {
        // Header
        Row(
            Modifier.fillMaxWidth().padding(top = 18.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            Text(
                stringResource(R.string.tab_statistics),
                style = text(32.sp, FontWeight.Bold, letterSpacing = (-0.5).sp), color = c.ink
            )
            Row(
                Modifier
                    .height(36.dp)
                    .shadow(if (c.isDark) 0.dp else 4.dp, RoundedCornerShape(18.dp), spotColor = Color.Black.copy(alpha = 0.25f))
                    .clip(RoundedCornerShape(18.dp))
                    .background(c.card)
                    .pressable(pressedScale = 0.92f) { vm.openSheet(Sheet.Export) }
                    .padding(horizontal = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ExportGlyph(c.tide)
                Text(stringResource(R.string.action_export), style = text(13.5.sp, FontWeight.SemiBold), color = c.tide)
            }
        }

        // Period segmented control
        BoxWithConstraints(
            Modifier
                .padding(top = 16.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(c.card2)
                .padding(3.dp)
        ) {
            val segW = maxWidth / 5
            val idx = StatsPeriod.entries.indexOf(vm.period)
            val x by animateFloatAsState(idx.toFloat(), tween(380, easing = Motion.SpringyMild), label = "thumb")
            Box(
                Modifier
                    .offset(x = segW * x)
                    .width(segW)
                    .height(32.dp)
                    .shadow(if (c.isDark) 0.dp else 3.dp, RoundedCornerShape(11.dp))
                    .clip(RoundedCornerShape(11.dp))
                    .background(c.card)
            )
            Row {
                StatsPeriod.entries.forEach { p ->
                    val sel = vm.period == p
                    Row(
                        Modifier
                            .width(segW)
                            .height(32.dp)
                            .pressable(pressedScale = 1f) { vm.selectPeriod(p) },
                        horizontalArrangement = Arrangement.spacedBy(3.dp, Alignment.CenterHorizontally),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            stringResource(p.labelRes),
                            style = text(13.sp, FontWeight.SemiBold),
                            color = if (sel) c.ink else c.sec
                        )
                        if (p.locked && !settings.pro) LockGlyph(c.ter)
                    }
                }
            }
        }

        // Period pager
        Row(
            Modifier.fillMaxWidth().padding(top = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            RoundIconButton(
                onClick = { vm.pageBack++ }, size = 34.dp,
                contentDescription = stringResource(R.string.a11y_prev_period)
            ) {
                Chevron(ChevronDirection.LEFT, c.sec, 12.dp)
            }
            Text(range.label, style = text(14.5.sp, FontWeight.SemiBold), color = c.ink)
            RoundIconButton(
                onClick = { if (vm.pageBack > 0) vm.pageBack-- },
                size = 34.dp, enabled = vm.pageBack > 0,
                contentDescription = stringResource(R.string.a11y_next_period)
            ) { Chevron(ChevronDirection.RIGHT, c.sec, 12.dp) }
        }
        if (vm.period == StatsPeriod.CUSTOM) {
            Text(
                stringResource(R.string.stats_edit_range), style = text(13.sp), color = c.tide,
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(top = 8.dp)
                    .pressable { vm.openSheet(Sheet.Range) }
            )
        }
        if (verdict.isNotEmpty()) {
            Text(
                verdict,
                style = text(13.5.sp, FontWeight.SemiBold),
                color = if (verdictDown) c.moss else c.sec,
                modifier = Modifier.align(Alignment.CenterHorizontally).padding(top = 10.dp)
            )
        }

        // Stat tiles
        Row(Modifier.padding(top = 14.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StatTileBig(
                stringResource(R.string.stats_tile_drunk),
                Formatters.grouped(animatedValue(range.totalMl).roundToInt().toLong()),
                stringResource(R.string.stats_unit_ml), 0, Modifier.weight(1f)
            )
            StatTileBig(
                stringResource(R.string.stats_tile_alcohol),
                Formatters.one(animatedValue(range.totalUnits)),
                // The caption sits under the number in a 12-char budget, so it takes
                // the config's SHORT form, not the two-word full noun (punch-list A2b).
                unitsPlural(
                    R.plurals.stats_unit_units,
                    range.totalUnits.roundToInt(),
                    units.noun.short
                ),
                50, Modifier.weight(1f)
            )
        }
        Row(Modifier.padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StatTileBig(
                stringResource(R.string.stats_tile_spending),
                Formatters.money(animatedValue(range.totalSpend)), "", 100, Modifier.weight(1f)
            )
            val burgers = range.totalKcal / AlcoholMath.KCAL_PER_CHEESEBURGER
            StatTileBig(
                stringResource(R.string.stats_tile_calories),
                Formatters.grouped(animatedValue(range.totalKcal.toDouble()).roundToInt().toLong()),
                stringResource(R.string.stats_unit_kcal), 150,
                Modifier.weight(1f),
                footnote = if (settings.tone != Tone.NUMBERS && burgers >= 1)
                    pluralStringResource(R.plurals.stats_kcal_burgers_neutral, burgers, burgers)
                else null
            )
        }

        // Units chart
        AppCard(Modifier.padding(top = 12.dp).riseIn(delayMillis = 100), padding = 16.dp) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(
                    unitsString(R.string.stats_units_chart_title, units.noun.plural),
                    style = text(15.sp, FontWeight.SemiBold), color = c.ink
                )
                Row(horizontalArrangement = Arrangement.spacedBy(7.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(R.string.stats_daily_average), style = text(12.sp), color = c.sec)
                    AppSwitch(vm.avgUnits) { vm.avgUnits = !vm.avgUnits }
                }
            }
            if (range.totalUnits > 0) {
                val maxU = maxOf(0.1, range.buckets.maxOf { it.units })
                BarChart(
                    bars = range.buckets.map {
                        BarDatum(it.label, (it.units / maxU).toFloat(), if (it.overDaily) c.amber else c.tide)
                    },
                    chartHeight = 132.dp,
                    averageFraction = if (vm.avgUnits) ((range.totalUnits / range.days) / maxU).toFloat() else null,
                    modifier = Modifier.padding(top = 14.dp)
                )
                Row(Modifier.fillMaxWidth().padding(top = 10.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(
                        stringResource(R.string.stats_units_total, range.totalUnits, units.noun.plural),
                        style = text(12.5.sp), color = c.sec
                    )
                    Text(
                        stringResource(R.string.stats_units_per_day, range.totalUnits / range.days),
                        style = text(12.5.sp, tabular = true), color = c.sec
                    )
                }
            } else {
                Column(
                    Modifier.fillMaxWidth().height(110.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(stringResource(R.string.stats_units_empty), style = text(13.5.sp), color = c.sec)
                }
            }
        }

        // Spending chart
        AppCard(Modifier.padding(top = 12.dp).riseIn(delayMillis = 140), padding = 16.dp) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.stats_spend_chart_title), style = text(15.sp, FontWeight.SemiBold), color = c.ink)
                Row(horizontalArrangement = Arrangement.spacedBy(7.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(R.string.stats_daily_average), style = text(12.sp), color = c.sec)
                    AppSwitch(vm.avgSpend) { vm.avgSpend = !vm.avgSpend }
                }
            }
            if (range.totalSpend > 0) {
                val maxSp = maxOf(0.1, range.buckets.maxOf { it.spend })
                BarChart(
                    bars = range.buckets.map {
                        BarDatum("", (it.spend / maxSp).toFloat(), c.amber.copy(alpha = 0.7f))
                    },
                    chartHeight = 110.dp,
                    averageFraction = if (vm.avgSpend) ((range.totalSpend / range.days) / maxSp).toFloat() else null,
                    modifier = Modifier.padding(top = 14.dp)
                )
                Row(Modifier.fillMaxWidth().padding(top = 10.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(
                        stringResource(R.string.stats_spend_total, Formatters.money(range.totalSpend)),
                        style = text(12.5.sp, tabular = true), color = c.sec
                    )
                    Text(
                        stringResource(R.string.stats_spend_per_day, Formatters.money(range.totalSpend / range.days)),
                        style = text(12.5.sp, tabular = true), color = c.sec
                    )
                }
            } else {
                Box(Modifier.fillMaxWidth().height(90.dp), contentAlignment = Alignment.Center) {
                    Text(stringResource(R.string.stats_spend_empty), style = text(13.5.sp), color = c.sec)
                }
            }
        }

        // Money saved
        AppCard(Modifier.padding(top = 12.dp), padding = 16.dp) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(stringResource(R.string.stats_money_saved_title), style = text(15.sp, FontWeight.SemiBold), color = c.ink)
                Row(horizontalArrangement = Arrangement.spacedBy(5.dp), verticalAlignment = Alignment.CenterVertically) {
                    // The symbol is config, and it is a suffix in fr/de/pl/da (A3).
                    Text(
                        stringResource(R.string.stats_baseline_label, currency.symbol),
                        style = text(12.5.sp), color = c.sec
                    )
                    AppTextField(
                        value = settings.baseline,
                        onValueChange = { v -> vm.updateSettings { it.copy(baseline = v.filter { ch -> ch.isDigit() || ch == '.' }) } },
                        placeholder = "0",
                        keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal,
                        centered = true,
                        modifier = Modifier.width(64.dp)
                    )
                }
            }
            Text(
                StatsEngine.savedLine(context, settings.baseline.toDoubleOrNull() ?: 0.0, range),
                style = text(13.5.sp), color = c.sec, lineHeight = 20.sp,
                modifier = Modifier.padding(top = 10.dp)
            )
        }

        // Drink breakdown
        when {
            !settings.pro -> {
                AppCard(
                    Modifier
                        .padding(top = 12.dp)
                        .pressable(pressedScale = 0.98f) { vm.openPaywall() },
                    padding = 16.dp
                ) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text(stringResource(R.string.stats_breakdown_title), style = text(15.sp, FontWeight.SemiBold), color = c.ink)
                        ProBadge { vm.openPaywall() }
                    }
                    Text(
                        stringResource(R.string.stats_breakdown_locked),
                        style = text(13.5.sp), color = c.sec, lineHeight = 20.sp,
                        modifier = Modifier.padding(top = 6.dp)
                    )
                }
            }
            range.breakdown.isEmpty() -> {
                AppCard(Modifier.padding(top = 12.dp), padding = 16.dp) {
                    Text(stringResource(R.string.stats_breakdown_title), style = text(15.sp, FontWeight.SemiBold), color = c.ink)
                    Text(
                        stringResource(R.string.stats_breakdown_empty),
                        style = text(13.5.sp), color = c.sec, lineHeight = 20.sp,
                        modifier = Modifier.padding(top = 6.dp)
                    )
                }
            }
            else -> {
                AppCard(Modifier.padding(top = 12.dp), padding = 16.dp) {
                    Text(stringResource(R.string.stats_breakdown_title), style = text(15.sp, FontWeight.SemiBold), color = c.ink)
                    range.breakdown.forEachIndexed { i, row ->
                        Column(Modifier.padding(top = 12.dp).riseIn(delayMillis = i * 40, durationMillis = 450)) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text(
                                        pluralStringResource(
                                            R.plurals.stats_breakdown_row,
                                            row.pours, row.name, row.pours
                                        ),
                                        style = text(13.5.sp, FontWeight.SemiBold), color = c.ink
                                    )
                                }
                                Text(
                                    stringResource(R.string.stats_breakdown_pct, row.pct),
                                    style = text(13.5.sp, FontWeight.SemiBold, tabular = true), color = c.tide
                                )
                            }
                            ShareBar(fraction = row.pct / 100f, modifier = Modifier.padding(top = 5.dp))
                        }
                    }
                    Text(
                        stringResource(
                            R.string.stats_breakdown_leader,
                            range.breakdown.first().name,
                            range.breakdown.first().pct,
                            units.noun.plural
                        ),
                        style = text(13.sp), color = c.sec, modifier = Modifier.padding(top = 12.dp)
                    )
                }
            }
        }

        // Dry days ring
        AppCard(Modifier.padding(top = 12.dp).riseIn(delayMillis = 180), padding = 16.dp) {
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
                ProgressRing(
                    progress = range.dryPct / 100f,
                    diameter = 84.dp,
                    strokeWidth = 8.dp,
                    color = c.moss
                ) {
                    Text(
                        stringResource(
                            R.string.stats_dry_ring_pct,
                            animatedValue(range.dryPct.toDouble()).roundToInt()
                        ),
                        style = text(16.sp, FontWeight.Bold, tabular = true), color = c.ink
                    )
                }
                Column(Modifier.weight(1f)) {
                    // Numbers tone is a pure ledger: no evaluative copy in any state.
                    val numbers = settings.tone == Tone.NUMBERS
                    Text(
                        stringResource(
                            when {
                                numbers -> R.string.stats_dry_title_numbers
                                range.dryCount > 0 -> R.string.stats_dry_title_positive
                                else -> R.string.stats_dry_title_none
                            }
                        ),
                        style = text(15.sp, FontWeight.SemiBold), color = c.ink
                    )
                    Text(
                        when {
                            numbers -> stringResource(
                                R.string.stats_dry_body_numbers,
                                range.dryCount, range.days, range.dryPct
                            )
                            range.dryCount > 0 ->
                                stringResource(R.string.stats_dry_body_positive, range.dryPct)
                            else -> stringResource(R.string.stats_dry_body_none)
                        },
                        style = text(13.sp), color = c.sec, lineHeight = 20.sp,
                        modifier = Modifier.padding(top = 5.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun StatTileBig(
    label: String,
    value: String,
    unit: String,
    delay: Int,
    modifier: Modifier = Modifier,
    footnote: String? = null
) {
    val c = LocalAppColors.current
    Column(
        modifier
            .riseIn(delayMillis = delay, durationMillis = 450)
            .shadow(if (c.isDark) 0.dp else 5.dp, RoundedCornerShape(18.dp), spotColor = Color.Black.copy(alpha = 0.2f))
            .clip(RoundedCornerShape(18.dp))
            .background(c.card)
            .padding(14.dp)
    ) {
        Text(label, style = text(12.sp, FontWeight.SemiBold), color = c.sec)
        Row(Modifier.padding(top = 4.dp), verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(value, style = text(20.sp, FontWeight.Bold, tabular = true), color = c.ink)
            if (unit.isNotEmpty()) {
                Text(unit, style = text(12.sp, FontWeight.Medium), color = c.sec, modifier = Modifier.padding(bottom = 2.dp))
            }
        }
        if (footnote != null) {
            Text(footnote, style = text(11.sp), color = c.ter, modifier = Modifier.padding(top = 2.dp))
        }
    }
}

@Composable
private fun ExportGlyph(color: Color) {
    Canvas(Modifier.size(12.dp, 14.dp)) {
        val w = size.width; val h = size.height
        val p = Path().apply {
            moveTo(w / 2, h * 0.62f); lineTo(w / 2, 0f)
            moveTo(w * 0.2f, h * 0.22f); lineTo(w / 2, 0f); lineTo(w * 0.8f, h * 0.22f)
            moveTo(w * 0.08f, h * 0.55f); lineTo(w * 0.08f, h); lineTo(w * 0.92f, h); lineTo(w * 0.92f, h * 0.55f)
        }
        drawPath(p, color, style = Stroke(1.8.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
}

@Composable
private fun LockGlyph(color: Color) {
    Canvas(Modifier.size(8.dp, 10.dp)) {
        val w = size.width; val h = size.height
        drawRoundRect(
            color,
            topLeft = androidx.compose.ui.geometry.Offset(w * 0.1f, h * 0.4f),
            size = androidx.compose.ui.geometry.Size(w * 0.8f, h * 0.55f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(2.dp.toPx())
        )
        drawArc(
            color,
            startAngle = 180f, sweepAngle = 180f, useCenter = false,
            topLeft = androidx.compose.ui.geometry.Offset(w * 0.22f, h * 0.05f),
            size = androidx.compose.ui.geometry.Size(w * 0.56f, h * 0.6f),
            style = Stroke(1.2.dp.toPx())
        )
    }
}

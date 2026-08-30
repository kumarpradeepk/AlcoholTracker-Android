package com.mtss.alcoholtracker.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mtss.alcoholtracker.R
import com.mtss.alcoholtracker.data.DrinkLog
import com.mtss.alcoholtracker.domain.AlcoholMath
import com.mtss.alcoholtracker.domain.DrinkPresets
import com.mtss.alcoholtracker.domain.StatsEngine
import com.mtss.alcoholtracker.domain.rememberCurrency
import com.mtss.alcoholtracker.domain.rememberUnits
import com.mtss.alcoholtracker.domain.unitsPlural
import com.mtss.alcoholtracker.domain.unitsString
import com.mtss.alcoholtracker.ui.AppViewModel
import com.mtss.alcoholtracker.ui.CalMode
import com.mtss.alcoholtracker.ui.PickedDrink
import com.mtss.alcoholtracker.ui.PushScreen
import com.mtss.alcoholtracker.ui.Sheet
import com.mtss.alcoholtracker.ui.components.Chevron
import com.mtss.alcoholtracker.ui.components.ChevronDirection
import com.mtss.alcoholtracker.ui.components.CloseGlyph
import com.mtss.alcoholtracker.ui.components.DropletMark
import com.mtss.alcoholtracker.ui.components.GlassIcon
import com.mtss.alcoholtracker.ui.components.PrimaryButton
import com.mtss.alcoholtracker.ui.components.Segmented
import com.mtss.alcoholtracker.ui.components.SoftButton
import com.mtss.alcoholtracker.ui.components.StepperRow
import com.mtss.alcoholtracker.ui.components.SwirlingGlass
import com.mtss.alcoholtracker.ui.components.pressable
import com.mtss.alcoholtracker.ui.components.riseIn
import com.mtss.alcoholtracker.ui.theme.LocalAppColors
import com.mtss.alcoholtracker.ui.theme.Motion
import com.mtss.alcoholtracker.ui.theme.display
import com.mtss.alcoholtracker.ui.theme.text
import com.mtss.alcoholtracker.util.Formatters
import com.mtss.alcoholtracker.util.LocaleText
import java.time.LocalDate
import kotlin.math.min
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SheetHost(vm: AppViewModel) {
    val sheet = vm.sheet ?: return
    val c = LocalAppColors.current
    val state = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = { vm.closeSheet() },
        sheetState = state,
        containerColor = c.bg,
        contentColor = c.text,
        shape = RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp),
        dragHandle = {
            Box(Modifier.padding(top = 9.dp, bottom = 2.dp)) {
                Box(Modifier.size(38.dp, 5.dp).clip(RoundedCornerShape(3.dp)).background(c.line))
            }
        }
    ) {
        when (sheet) {
            is Sheet.Log -> LogSheet(vm)
            is Sheet.Cal -> CalendarSheet(vm, sheet.mode)
            is Sheet.Entry -> EntrySheet(vm, sheet.log)
            Sheet.UnitsInfo -> UnitsInfoSheet(vm)
            Sheet.BacInfo -> BacInfoSheet()
            Sheet.Range -> RangeSheet(vm)
            Sheet.NewNotif -> NewNotifSheet(vm)
            Sheet.LivePreview -> LivePreviewSheet(vm)
            Sheet.CustomDrink -> CustomDrinkSheet(vm)
            Sheet.Export -> ExportSheet(vm)
            Sheet.Health -> HealthSheet(vm)
        }
    }
}

// ── Log Drink: three sliding steps ───────────────────────────────────────

@Composable
private fun LogSheet(vm: AppViewModel) {
    val c = LocalAppColors.current
    Column(Modifier.heightIn(max = 700.dp)) {
        // Header
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (vm.logStep > 0) {
                Box(
                    Modifier.size(32.dp).clip(CircleShape).background(c.surface2)
                        .pressable(pressedScale = 0.88f) { vm.logBack() },
                    contentAlignment = Alignment.Center
                ) { Chevron(ChevronDirection.LEFT, c.muted, 12.dp) }
            } else Spacer(Modifier.size(32.dp))
            Text(
                listOf(
                    stringResource(R.string.action_log_drink),
                    stringResource(R.string.log_title_tune),
                    stringResource(R.string.log_title_review)
                )[vm.logStep],
                style = text(16.5.sp, FontWeight.SemiBold), color = c.text,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f)
            )
            Box(
                Modifier.size(32.dp).clip(CircleShape).background(c.surface2)
                    .pressable(pressedScale = 0.88f) { vm.closeSheet() },
                contentAlignment = Alignment.Center
            ) { CloseGlyph(c.muted) }
        }

        // Sliding pages
        BoxWithConstraints(Modifier.weight(1f, fill = false).heightIn(min = 320.dp, max = 560.dp)) {
            val w = maxWidth
            val offset by animateFloatAsState(
                targetValue = -vm.logStep.toFloat(),
                animationSpec = tween(480, easing = Motion.Settle),
                label = "step"
            )
            Row(
                Modifier
                    .requiredWidth(w * 3)
                    .offset(x = w * offset + w)
            ) {
                Box(Modifier.width(w)) { LogStepPick(vm) }
                Box(Modifier.width(w)) { LogStepTune(vm) }
                Box(Modifier.width(w)) { LogStepReview(vm) }
            }
        }

        // CTA
        Box(Modifier.padding(horizontal = 20.dp).padding(top = 10.dp, bottom = 24.dp)) {
            PrimaryButton(
                label = listOf(
                    stringResource(R.string.log_cta_next),
                    stringResource(R.string.log_cta_next),
                    stringResource(R.string.log_cta_save)
                )[vm.logStep],
                enabled = !(vm.logStep == 0 && vm.pick == null),
                height = 52.dp,
                onClick = { vm.logNext() }
            )
        }
    }
}

@Composable
private fun LogStepPick(vm: AppViewModel) {
    val c = LocalAppColors.current
    val saved by vm.savedDrinks.collectAsState()
    val q = vm.query
    Column(
        Modifier
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .padding(top = 4.dp, bottom = 16.dp)
    ) {
        Text(
            stringResource(R.string.log_pick_intro),
            style = text(14.5.sp), color = c.muted, lineHeight = 21.sp
        )
        AppTextField(
            value = q, onValueChange = { vm.query = it },
            placeholder = stringResource(R.string.log_search_placeholder),
            modifier = Modifier.padding(top = 14.dp)
        )
        // Custom drink row
        Row(
            Modifier
                .padding(top = 12.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(c.surface)
                .pressable(pressedScale = 0.97f) { vm.openCustomDrink() }
                .padding(horizontal = 14.dp, vertical = 13.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier.size(34.dp).clip(RoundedCornerShape(11.dp)).background(c.surface2),
                contentAlignment = Alignment.Center
            ) { Text("+", style = text(20.sp), color = c.accent) }
            Column {
                Text(stringResource(R.string.log_custom_title), style = text(15.sp, FontWeight.SemiBold), color = c.text)
                Text(
                    stringResource(R.string.log_custom_sub),
                    style = text(12.5.sp), color = c.muted, modifier = Modifier.padding(top = 1.dp)
                )
            }
        }

        val savedFiltered = saved.filter { it.name.contains(q, ignoreCase = true) }
        if (savedFiltered.isNotEmpty()) {
            Text(
                stringResource(R.string.log_section_your_drinks), style = text(12.sp, FontWeight.SemiBold, letterSpacing = 0.6.sp),
                color = c.faint, modifier = Modifier.padding(top = 16.dp)
            )
            DrinkGrid(
                items = savedFiltered.map { PickedDrink(it.name, it.abv, it.ml, null) },
                picked = vm.pick, fill = c.b1, onPick = { vm.pickDrink(it) }
            )
        }

        Text(
            stringResource(R.string.log_section_popular), style = text(12.sp, FontWeight.SemiBold, letterSpacing = 0.6.sp),
            color = c.faint, modifier = Modifier.padding(top = 16.dp)
        )
        DrinkGrid(
            items = DrinkPresets.ALL
                .filter { it.name.contains(q, ignoreCase = true) }
                .map { PickedDrink(it.name, it.abv, it.ml, it.cost) },
            picked = vm.pick, fill = c.accent, onPick = { vm.pickDrink(it) }
        )
    }
}

@Composable
private fun DrinkGrid(
    items: List<PickedDrink>,
    picked: PickedDrink?,
    fill: Color,
    onPick: (PickedDrink) -> Unit
) {
    val c = LocalAppColors.current
    Column(Modifier.padding(top = 8.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        items.chunked(2).forEachIndexed { rowIdx, row ->
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                row.forEachIndexed { colIdx, p ->
                    val sel = picked?.name == p.name
                    Column(
                        Modifier
                            .weight(1f)
                            .riseIn(delayMillis = (rowIdx * 2 + colIdx) * 28, durationMillis = 450)
                            .clip(RoundedCornerShape(18.dp))
                            .background(if (sel) c.surface2 else c.surface)
                            .border(1.5.dp, if (sel) c.accent else Color.Transparent, RoundedCornerShape(18.dp))
                            .pressable(pressedScale = 0.96f) { onPick(p) }
                            .padding(12.dp)
                    ) {
                        GlassIcon(
                            fillFraction = min(0.85f, (p.abv * 2.6f + 18f).toFloat() / 100f),
                            width = 26.dp, height = 33.dp, fill = fill
                        )
                        Text(p.name, style = text(14.sp, FontWeight.SemiBold), color = c.text, modifier = Modifier.padding(top = 8.dp))
                        Text(
                            stringResource(R.string.log_grid_meta, trimAbv(p.abv), p.ml.toInt()),
                            style = text(12.sp), color = c.muted, modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

private fun trimAbv(v: Double): String =
    if (v == v.toLong().toDouble()) v.toLong().toString() else v.toString()

@Composable
private fun LogStepTune(vm: AppViewModel) {
    val c = LocalAppColors.current
    val units = rememberUnits()
    Column(
        Modifier
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .padding(top = 4.dp, bottom = 16.dp)
    ) {
        Text(
            stringResource(R.string.log_tune_intro),
            style = text(14.5.sp), color = c.muted, lineHeight = 21.sp
        )
        SheetCard(top = 14.dp) {
            Text(stringResource(R.string.log_abv_label), style = text(13.sp, FontWeight.SemiBold), color = c.muted)
            Box(Modifier.padding(top = 10.dp)) {
                StepperRow(
                    valueText = {
                        Text(stringResource(R.string.log_abv_value, vm.dAbv), style = display(30.sp), color = c.text)
                    },
                    onMinus = { vm.dAbv = ((vm.dAbv - 0.5).coerceAtLeast(0.5) * 10).roundToInt() / 10.0 },
                    onPlus = { vm.dAbv = ((vm.dAbv + 0.5).coerceAtMost(96.0) * 10).roundToInt() / 10.0 }
                )
            }
            Row(Modifier.padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(10.0, 14.0, 20.0).forEach { v ->
                    ChipButton(stringResource(R.string.log_abv_value, v), Modifier.weight(1f)) { vm.dAbv = v }
                }
            }
        }
        SheetCard(top = 12.dp) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.log_serving_label), style = text(13.sp, FontWeight.SemiBold), color = c.muted)
                Segmented(
                    options = listOf(
                        stringResource(R.string.log_unit_ml),
                        stringResource(R.string.log_unit_floz)
                    ),
                    selectedIndex = if (vm.servMl) 0 else 1,
                    itemPadding = 4.dp,
                    fontSize = 12.5.sp,
                    modifier = Modifier.width(120.dp),
                    onSelect = { vm.servMl = it == 0 }
                )
            }
            Box(Modifier.padding(top = 10.dp)) {
                StepperRow(
                    valueText = {
                        Text(
                            if (vm.servMl) stringResource(R.string.log_serving_ml_value, vm.dMl.toInt())
                            else stringResource(R.string.log_serving_oz_value, AlcoholMath.mlToOz(vm.dMl)),
                            style = display(30.sp), color = c.text
                        )
                    },
                    onMinus = { vm.dMl = (vm.dMl - 10).coerceAtLeast(10.0) },
                    onPlus = { vm.dMl = (vm.dMl + 10).coerceAtMost(1000.0) }
                )
            }
            Row(Modifier.padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(120.0, 150.0, 240.0, 355.0).forEach { v ->
                    ChipButton(
                        if (vm.servMl) stringResource(R.string.log_serving_ml_value, v.toInt())
                        else stringResource(R.string.log_serving_oz_value, AlcoholMath.mlToOz(v)),
                        Modifier.weight(1f)
                    ) { vm.dMl = v }
                }
            }
        }
        SheetCard(top = 12.dp) {
            Text(stringResource(R.string.log_quantity_label), style = text(13.sp, FontWeight.SemiBold), color = c.muted)
            Row(Modifier.padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(0.5 to "½", 1.0 to "1", 2.0 to "2").forEach { (v, label) ->
                    val sel = vm.qty == v
                    Box(
                        Modifier
                            .weight(1f)
                            .height(36.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .background(if (sel) c.accent else c.surface2)
                            .pressable(pressedScale = 0.93f) { vm.qty = v },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(label, style = text(14.sp, FontWeight.SemiBold), color = if (sel) Color.White else c.muted)
                    }
                }
            }
        }
        val u = AlcoholMath.units(vm.dMl * vm.qty, vm.dAbv)
        val kc = AlcoholMath.kcal(vm.dMl * vm.qty, vm.dAbv)
        Text(
            stringResource(R.string.log_result_line, u, kc, units.noun.plural),
            style = text(14.sp), color = c.muted, textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(top = 14.dp)
        )
        Text(
            when (vm.qty) {
                0.5 -> stringResource(
                    R.string.log_working_line_half,
                    vm.dMl.toInt(), trimAbv(vm.dAbv), u, units.noun.plural
                )
                1.0 -> stringResource(
                    R.string.log_working_line,
                    vm.dMl.toInt(), trimAbv(vm.dAbv), u, units.noun.plural
                )
                else -> stringResource(
                    R.string.log_working_line_qty,
                    vm.qty.toInt(), vm.dMl.toInt(), trimAbv(vm.dAbv), u, units.noun.plural
                )
            },
            style = text(11.5.sp, tabular = true), color = c.faint, textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(top = 5.dp)
        )
    }
}

@Composable
private fun LogStepReview(vm: AppViewModel) {
    val c = LocalAppColors.current
    val context = LocalContext.current
    val units = rememberUnits()
    val currency = rememberCurrency()
    val settings by vm.settings.collectAsState()
    val p = vm.pick
    val u = AlcoholMath.units(vm.dMl * vm.qty, vm.dAbv)
    val kc = AlcoholMath.kcal(vm.dMl * vm.qty, vm.dAbv)
    Column(
        Modifier
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .padding(top = 4.dp, bottom = 16.dp)
    ) {
        SheetCard(top = 0.dp) {
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
                GlassIcon(
                    fillFraction = min(0.85f, (vm.dAbv * 2.6f + 18f).toFloat() / 100f),
                    width = 42.dp, height = 54.dp
                )
                Column(Modifier.weight(1f)) {
                    Text(p?.name ?: "", style = display(18.sp, tabular = false), color = c.text)
                    Text(
                        when (vm.qty) {
                            1.0 -> stringResource(R.string.log_review_meta, vm.dMl.toInt(), trimAbv(vm.dAbv))
                            0.5 -> stringResource(R.string.log_review_meta_half, vm.dMl.toInt(), trimAbv(vm.dAbv))
                            else -> stringResource(
                                R.string.log_review_meta_qty,
                                vm.qty.toInt(), vm.dMl.toInt(), trimAbv(vm.dAbv)
                            )
                        },
                        style = text(13.5.sp), color = c.muted, modifier = Modifier.padding(top = 3.dp)
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(Formatters.one(u), style = display(22.sp), color = c.accent)
                    Text(units.noun.plural, style = text(11.5.sp), color = c.muted)
                }
            }
        }
        SheetCard(top = 12.dp) {
            Text(stringResource(R.string.log_when_label), style = text(13.sp, FontWeight.SemiBold), color = c.muted)
            Row(Modifier.padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(
                    stringResource(R.string.log_when_now),
                    stringResource(R.string.log_when_1h),
                    stringResource(R.string.log_when_2h)
                ).forEachIndexed { i, label ->
                    val sel = vm.dWhen == i
                    Box(
                        Modifier
                            .weight(1f)
                            .height(36.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .background(if (sel) c.accent else c.surface2)
                            .pressable(pressedScale = 0.94f) { vm.dWhen = i },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(label, style = text(13.5.sp, FontWeight.SemiBold), color = if (sel) Color.White else c.muted)
                    }
                }
            }
        }
        if (settings.askCost) {
            SheetCard(top = 12.dp) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(stringResource(R.string.log_cost_label), style = text(15.sp, FontWeight.SemiBold), color = c.text)
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(stringResource(R.string.log_cost_currency, currency.symbol), style = display(18.sp, tabular = false), color = c.muted)
                        AppTextField(
                            value = vm.dCost,
                            onValueChange = { vm.dCost = it.filter { ch -> ch.isDigit() || ch == '.' } },
                            placeholder = stringResource(R.string.log_cost_placeholder),
                            keyboardType = KeyboardType.Decimal,
                            modifier = Modifier.width(80.dp),
                            centered = true
                        )
                    }
                }
            }
        }
        if (settings.showCalories) {
            SheetCard(top = 12.dp) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(stringResource(R.string.log_calories_label), style = text(15.sp, FontWeight.SemiBold), color = c.text)
                    Text(stringResource(R.string.log_calories_kcal, kc), style = text(15.sp, tabular = true), color = c.muted)
                }
            }
        }
        val today = vm.todayKey()
        Text(
            if (vm.selectedDay() == today) stringResource(R.string.log_saved_to_today)
            else stringResource(
                R.string.log_saved_to_day,
                Formatters.dayTitle(context, vm.selectedDay(), today)
            ),
            style = text(12.5.sp), color = c.faint, textAlign = TextAlign.Center, lineHeight = 18.sp,
            modifier = Modifier.fillMaxWidth().padding(top = 14.dp)
        )
    }
}

// ── Calendar ─────────────────────────────────────────────────────────────

@Composable
private fun CalendarSheet(vm: AppViewModel, mode: CalMode) {
    val c = LocalAppColors.current
    val logs by vm.logs.collectAsState()
    val dry by vm.dryDays.collectAsState()
    val today = vm.todayKey()
    val base = remember(vm.calMonthOffset) {
        LocalDate.ofEpochDay(today).withDayOfMonth(1).plusMonths(vm.calMonthOffset.toLong())
    }
    val loggedDays = remember(logs) { logs.map { it.epochDay }.toSet() }

    Column(Modifier.padding(horizontal = 20.dp).padding(top = 6.dp, bottom = 26.dp)) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier.size(34.dp).clip(CircleShape).background(c.surface2)
                    .pressable(pressedScale = 0.88f) { vm.calMonthOffset-- },
                contentAlignment = Alignment.Center
            ) { Chevron(ChevronDirection.LEFT, c.muted, 12.dp) }
            Text(Formatters.monthYear(base), style = text(16.5.sp, FontWeight.SemiBold), color = c.text)
            Box(
                Modifier.size(34.dp).clip(CircleShape).background(c.surface2)
                    .pressable(pressedScale = 0.88f) { if (vm.calMonthOffset < 0) vm.calMonthOffset++ }
                    .padding(0.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(Modifier.alpha2(vm.calMonthOffset < 0)) { Chevron(ChevronDirection.RIGHT, c.muted, 12.dp) }
            }
        }
        // punch-list B4 + A7. Two letters, not one: German Mo/Mi both collapse to
        // "M" and Di/Do both to "D", and Thai Thursday (พฤ) cannot shorten below two
        // at all. The week also starts where the *platform locale* says it does,
        // not on a hardcoded Sunday - es-419 is split internally (AR/CL Monday,
        // MX/CO/PE Sunday), so the language alone cannot answer it.
        val calendarLocale = Formatters.locale()
        val headers = remember(calendarLocale) { LocaleText.weekdayHeaders(calendarLocale) }
        Row(Modifier.fillMaxWidth().padding(top = 14.dp)) {
            headers.forEach {
                Text(
                    it, style = text(11.sp, FontWeight.SemiBold), color = c.faint,
                    textAlign = TextAlign.Center, maxLines = 1, modifier = Modifier.weight(1f)
                )
            }
        }
        val lead = LocaleText.weekIndex(base.dayOfWeek, calendarLocale)
        val daysInMonth = base.lengthOfMonth()
        val cells = lead + daysInMonth
        val rows = (cells + 6) / 7
        Column(Modifier.padding(top = 6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            repeat(rows) { r ->
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    repeat(7) { col ->
                        val idx = r * 7 + col
                        val dayNum = idx - lead + 1
                        if (dayNum in 1..daysInMonth) {
                            val epoch = base.withDayOfMonth(dayNum).toEpochDay()
                            val future = epoch > today
                            val isSel = epoch == vm.selectedDay() && mode == CalMode.SELECT
                            val dotColor = when {
                                loggedDays.contains(epoch) -> c.accent
                                dry.contains(epoch) -> c.b1
                                else -> Color.Transparent
                            }
                            Column(
                                Modifier
                                    .weight(1f)
                                    .height(44.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (isSel) c.surface2 else Color.Transparent)
                                    .border(1.5.dp, if (epoch == today) c.accent else Color.Transparent, RoundedCornerShape(12.dp))
                                    .alpha2(!future)
                                    .pressable(pressedScale = 0.92f, enabled = !future) {
                                        vm.calendarDayTapped(epoch, mode)
                                    },
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text("$dayNum", style = text(14.5.sp, FontWeight.Medium), color = c.text)
                                Box(Modifier.padding(top = 3.dp).size(5.dp).clip(CircleShape).background(dotColor))
                            }
                        } else {
                            Spacer(Modifier.weight(1f).height(44.dp))
                        }
                    }
                }
            }
        }
        Text(
            if (mode == CalMode.DRY)
                stringResource(R.string.sheet_cal_hint_dry)
            else
                stringResource(R.string.sheet_cal_hint_select),
            style = text(13.sp), color = c.muted, textAlign = TextAlign.Center, lineHeight = 19.sp,
            modifier = Modifier.fillMaxWidth().padding(top = 14.dp)
        )
        Box(
            Modifier
                .padding(top = 14.dp)
                .fillMaxWidth()
                .height(48.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(c.surface2)
                .pressable(pressedScale = 0.97f) { vm.closeSheet() },
            contentAlignment = Alignment.Center
        ) { Text(stringResource(R.string.action_done), style = text(16.sp, FontWeight.SemiBold), color = c.text) }
    }
}

private fun Modifier.alpha2(visible: Boolean): Modifier =
    if (visible) this else this.then(Modifier.graphicsLayer { alpha = 0.3f })

// ── Entry detail ─────────────────────────────────────────────────────────

@Composable
private fun EntrySheet(vm: AppViewModel, log: DrinkLog) {
    val c = LocalAppColors.current
    val units = rememberUnits()
    val mins = remember(log.atMillis) {
        val z = java.time.Instant.ofEpochMilli(log.atMillis).atZone(java.time.ZoneId.systemDefault())
        z.hour * 60 + z.minute
    }
    Column(Modifier.padding(horizontal = 22.dp).padding(top = 10.dp, bottom = 28.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
            GlassIcon(
                fillFraction = min(0.85f, (log.abv * 2.6f + 18f).toFloat() / 100f),
                width = 46.dp, height = 58.dp
            )
            Column {
                Text(log.name, style = display(21.sp, tabular = false), color = c.text)
                Text(
                    stringResource(
                        R.string.sheet_entry_meta,
                        log.ml.toInt(), trimAbv(log.abv), Formatters.time(mins)
                    ),
                    style = text(14.sp), color = c.muted, modifier = Modifier.padding(top = 3.dp)
                )
            }
        }
        Row(Modifier.padding(top = 18.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            val entryUnits = AlcoholMath.units(log.ml, log.abv)
            StatTile(
                unitsPlural(R.plurals.stat_label_units, entryUnits.roundToInt(), units.noun.short),
                Formatters.one(entryUnits), c.accent, Modifier.weight(1f)
            )
            StatTile(stringResource(R.string.stat_label_kcal), "${log.kcal}", c.text, Modifier.weight(1f))
            StatTile(stringResource(R.string.stat_label_spent), Formatters.money(log.cost), c.text, Modifier.weight(1f))
        }
        SoftButton(
            stringResource(R.string.sheet_entry_relog),
            modifier = Modifier.padding(top = 18.dp),
            onClick = { vm.relog(log) }
        )
        Box(
            Modifier
                .padding(top = 10.dp)
                .fillMaxWidth()
                .height(50.dp)
                .clip(RoundedCornerShape(25.dp))
                .pressable(pressedScale = 0.97f) { vm.askDelete(log) },
            contentAlignment = Alignment.Center
        ) {
            Text(stringResource(R.string.sheet_entry_remove), style = text(16.sp, FontWeight.SemiBold), color = c.b3)
        }
    }
}

@Composable
private fun StatTile(label: String, value: String, valueColor: Color, modifier: Modifier = Modifier) {
    val c = LocalAppColors.current
    Column(
        modifier
            .clip(RoundedCornerShape(16.dp))
            .background(c.surface)
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(value, style = display(17.sp), color = valueColor)
        Text(label, style = text(11.5.sp), color = c.muted, modifier = Modifier.padding(top = 2.dp))
    }
}

// ── Units info ───────────────────────────────────────────────────────────

@Composable
private fun UnitsInfoSheet(vm: AppViewModel) {
    val c = LocalAppColors.current
    val units = rememberUnits()
    Column(Modifier.padding(horizontal = 22.dp).padding(top = 10.dp, bottom = 28.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
            SwirlingGlass(width = 54.dp, height = 70.dp)
            Column {
                Text(
                    unitsString(R.string.sheet_units_title, units.noun.indefinite),
                    style = display(20.sp, tabular = false, letterSpacing = (-0.2).sp), color = c.text
                )
                Text(
                    stringResource(R.string.sheet_units_body),
                    style = text(13.5.sp), color = c.muted, lineHeight = 20.sp,
                    modifier = Modifier.padding(top = 5.dp)
                )
            }
        }
        // Formula chips
        Row(
            Modifier.padding(top = 18.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            FormulaChip(stringResource(R.string.sheet_units_chip_pour), c.surface2, c.accent, 50)
            Text("×", style = text(15.sp), color = c.faint)
            FormulaChip(stringResource(R.string.sheet_units_chip_abv), c.surface2, c.b2, 150)
        }
        Row(
            Modifier.padding(top = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            FormulaChip(stringResource(R.string.sheet_units_chip_density), c.surface2, c.muted, 250)
            Text("=", style = text(15.sp), color = c.faint)
            FormulaChip(unitsString(R.string.sheet_units_chip_result, units.noun.plural), c.surface2, c.b1, 400)
        }
        Text(stringResource(R.string.sheet_units_density_note), style = text(12.sp), color = c.faint, modifier = Modifier.padding(top = 8.dp))
        Text(
            unitsString(R.string.sheet_units_targets, units.noun.indefinite),
            style = text(14.sp), color = c.muted, lineHeight = 22.sp,
            modifier = Modifier.padding(top = 14.dp)
        )
        Box(
            Modifier
                .padding(top = 16.dp)
                .fillMaxWidth()
                .height(48.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(c.surface2)
                .pressable(pressedScale = 0.97f) {
                    vm.closeSheet(); vm.openPush(PushScreen.GUIDE)
                },
            contentAlignment = Alignment.Center
        ) { Text(stringResource(R.string.diary_adjust_guideline), style = text(15.5.sp, FontWeight.SemiBold), color = c.text) }
    }
}

@Composable
private fun FormulaChip(label: String, bg: Color, fg: Color, delay: Int) {
    Box(
        Modifier
            .riseIn(delayMillis = delay, durationMillis = 400)
            .clip(RoundedCornerShape(12.dp))
            .background(bg)
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Text(label, style = text(13.5.sp, FontWeight.SemiBold, tabular = true), color = fg)
    }
}

// ── BAC info ─────────────────────────────────────────────────────────────

@Composable
private fun BacInfoSheet() {
    val c = LocalAppColors.current
    Column(Modifier.padding(horizontal = 22.dp).padding(top = 10.dp, bottom = 28.dp)) {
        Text(stringResource(R.string.bac_how_estimated), style = display(20.sp, tabular = false), color = c.text)
        Text(
            stringResource(R.string.sheet_bac_widmark),
            style = text(14.5.sp), color = c.muted, lineHeight = 22.sp, modifier = Modifier.padding(top = 10.dp)
        )
        Text(
            stringResource(R.string.sheet_bac_not_measurement),
            style = text(14.5.sp), color = c.muted, lineHeight = 22.sp, modifier = Modifier.padding(top = 10.dp)
        )
        Box(
            Modifier
                .padding(top = 14.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(c.surface2)
                .padding(horizontal = 16.dp, vertical = 14.dp)
        ) {
            Text(
                stringResource(R.string.sheet_bac_never_drive),
                style = text(13.5.sp, FontWeight.Medium), color = c.b2, lineHeight = 20.sp
            )
        }
    }
}

// ── Custom range ─────────────────────────────────────────────────────────

@Composable
private fun RangeSheet(vm: AppViewModel) {
    val c = LocalAppColors.current
    val context = LocalContext.current
    val today = vm.todayKey()
    Column(Modifier.padding(horizontal = 22.dp).padding(top = 8.dp, bottom = 26.dp)) {
        Text(stringResource(R.string.sheet_range_title), style = display(20.sp, tabular = false), color = c.text)
        Row(Modifier.padding(top = 14.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            DateField(stringResource(R.string.sheet_range_from), vm.customFrom ?: (today - 13), Modifier.weight(1f)) { picked ->
                vm.customFrom = picked
            }
            DateField(stringResource(R.string.sheet_range_to), vm.customTo ?: today, Modifier.weight(1f)) { picked ->
                vm.customTo = picked
            }
        }
        Row(Modifier.padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ChipButton(stringResource(R.string.sheet_range_last7), Modifier.weight(1f)) {
                vm.customFrom = today - 6; vm.customTo = today
            }
            ChipButton(stringResource(R.string.sheet_range_last30), Modifier.weight(1f)) {
                vm.customFrom = today - 29; vm.customTo = today
            }
        }
        PrimaryButton(
            stringResource(R.string.action_apply), height = 50.dp,
            modifier = Modifier.padding(top = 16.dp),
            onClick = { vm.applyCustomRange() }
        )
        Box(
            Modifier
                .padding(top = 8.dp)
                .fillMaxWidth()
                .height(46.dp)
                .pressable { vm.closeSheet() },
            contentAlignment = Alignment.Center
        ) { Text(stringResource(R.string.action_cancel), style = text(15.5.sp, FontWeight.SemiBold), color = c.muted) }
    }
}

@Composable
private fun DateField(label: String, epochDay: Long, modifier: Modifier = Modifier, onPicked: (Long) -> Unit) {
    val c = LocalAppColors.current
    val context = LocalContext.current
    Column(
        modifier
            .clip(RoundedCornerShape(16.dp))
            .background(c.surface)
            .pressable(pressedScale = 0.97f) {
                val d = LocalDate.ofEpochDay(epochDay)
                android.app.DatePickerDialog(
                    context,
                    { _, y, m, day -> onPicked(LocalDate.of(y, m + 1, day).toEpochDay()) },
                    d.year, d.monthValue - 1, d.dayOfMonth
                ).show()
            }
            .padding(horizontal = 14.dp, vertical = 12.dp)
    ) {
        Text(label, style = text(12.sp, FontWeight.SemiBold), color = c.muted)
        Text(
            Formatters.shortDate(epochDay),
            style = text(15.sp, FontWeight.SemiBold), color = c.text,
            modifier = Modifier.padding(top = 6.dp)
        )
    }
}

// ── Create notification ──────────────────────────────────────────────────

@Composable
private fun NewNotifSheet(vm: AppViewModel) {
    val c = LocalAppColors.current
    val context = LocalContext.current
    Column(Modifier.padding(horizontal = 22.dp).padding(top = 8.dp, bottom = 26.dp)) {
        Text(stringResource(R.string.notif_create_cta), style = display(20.sp, tabular = false), color = c.text)
        AppTextField(
            value = vm.ndTitle, onValueChange = { vm.ndTitle = it },
            placeholder = stringResource(R.string.sheet_notif_title_placeholder),
            modifier = Modifier.padding(top = 14.dp)
        )
        Row(
            Modifier
                .padding(top = 10.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(c.surface)
                .padding(horizontal = 16.dp, vertical = 13.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(stringResource(R.string.sheet_notif_time_label), style = text(15.sp, FontWeight.SemiBold), color = c.text)
            Box(
                Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(c.surface2)
                    .pressable(pressedScale = 0.95f) {
                        android.app.TimePickerDialog(
                            context,
                            { _, h, m -> vm.ndTime = h * 60 + m },
                            vm.ndTime / 60, vm.ndTime % 60, false
                        ).show()
                    }
                    .padding(horizontal = 14.dp, vertical = 8.dp)
            ) {
                Text(Formatters.time(vm.ndTime), style = text(15.sp, FontWeight.SemiBold, tabular = true), color = c.text)
            }
        }
        AppTextField(
            value = vm.ndMsg, onValueChange = { vm.ndMsg = it },
            placeholder = stringResource(R.string.sheet_notif_message_placeholder),
            modifier = Modifier.padding(top = 10.dp)
        )
        Text(
            stringResource(R.string.sheet_notif_repeat_note),
            style = text(12.5.sp), color = c.faint, modifier = Modifier.padding(top = 10.dp)
        )
        PrimaryButton(
            stringResource(R.string.sheet_notif_cta), height = 50.dp, enabled = vm.ndTitle.isNotBlank(),
            modifier = Modifier.padding(top = 14.dp),
            onClick = { vm.createReminder() }
        )
    }
}

// ── Live preview ─────────────────────────────────────────────────────────

@Composable
private fun LivePreviewSheet(vm: AppViewModel) {
    val c = LocalAppColors.current
    Column(Modifier.padding(horizontal = 22.dp).padding(top = 8.dp, bottom = 28.dp)) {
        Text(stringResource(R.string.sheet_live_title), style = display(20.sp, tabular = false), color = c.text)
        Text(
            stringResource(R.string.sheet_live_body),
            style = text(14.sp), color = c.muted, lineHeight = 21.sp, modifier = Modifier.padding(top = 6.dp)
        )
        Column(
            Modifier
                .padding(top = 16.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(Color(0xFF101014))
                .padding(16.dp)
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                DropletMark(28.dp, color = c.accent, breathing = false)
                Column(Modifier.weight(1f)) {
                    Text(stringResource(R.string.app_name), style = text(13.sp, FontWeight.SemiBold), color = Color.White)
                    Text(stringResource(R.string.sheet_live_mock_status), style = text(11.5.sp), color = Color.White.copy(alpha = 0.55f), modifier = Modifier.padding(top = 1.dp))
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(stringResource(R.string.sheet_live_mock_value), style = display(19.sp), color = Color.White)
                    Text(stringResource(R.string.sheet_live_mock_tozero), style = text(11.sp), color = c.b1, modifier = Modifier.padding(top = 1.dp))
                }
            }
            Box(
                Modifier
                    .padding(top = 14.dp)
                    .fillMaxWidth()
                    .height(5.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(Color.White.copy(alpha = 0.14f))
            ) {
                Box(
                    Modifier
                        .fillMaxWidth(0.46f)
                        .height(5.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(c.accent)
                )
            }
        }
        Text(
            stringResource(R.string.sheet_live_note),
            style = text(12.5.sp), color = c.faint, textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(top = 12.dp)
        )
        Box(
            Modifier
                .padding(top = 14.dp)
                .fillMaxWidth()
                .height(48.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(c.surface2)
                .pressable(pressedScale = 0.97f) { vm.closeSheet() },
            contentAlignment = Alignment.Center
        ) { Text(stringResource(R.string.action_done), style = text(15.5.sp, FontWeight.SemiBold), color = c.text) }
    }
}

// ── Custom drink ─────────────────────────────────────────────────────────

@Composable
private fun CustomDrinkSheet(vm: AppViewModel) {
    val c = LocalAppColors.current
    Column(
        Modifier
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 22.dp)
            .padding(top = 8.dp, bottom = 26.dp)
    ) {
        Text(stringResource(R.string.sheet_custom_title), style = display(20.sp, tabular = false), color = c.text)
        AppTextField(
            value = vm.cuName, onValueChange = { vm.cuName = it },
            placeholder = stringResource(R.string.sheet_custom_name_placeholder),
            modifier = Modifier.padding(top = 14.dp)
        )
        Text(
            stringResource(R.string.sheet_custom_base_caption), style = text(12.sp, FontWeight.SemiBold, letterSpacing = 0.6.sp), color = c.faint,
            modifier = Modifier.padding(top = 12.dp)
        )
        Row(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(
                "Beer" to stringResource(R.string.drink_cat_beer),
                "Wine" to stringResource(R.string.drink_cat_wine),
                "Spirit" to stringResource(R.string.drink_cat_spirit)
            ).forEach { (bt, label) -> BaseChip(vm, bt, label) }
        }
        Row(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(
                "Cocktail" to stringResource(R.string.drink_cat_cocktail),
                "Other" to stringResource(R.string.drink_cat_other)
            ).forEach { (bt, label) -> BaseChip(vm, bt, label) }
        }
        Row(Modifier.padding(top = 14.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Column(
                Modifier.weight(1f).clip(RoundedCornerShape(16.dp)).background(c.surface).padding(horizontal = 14.dp, vertical = 12.dp)
            ) {
                Text(stringResource(R.string.sheet_custom_abv_label), style = text(12.sp, FontWeight.SemiBold), color = c.muted)
                AppTextField(
                    value = vm.cuAbv, onValueChange = { vm.cuAbv = it.filter { ch -> ch.isDigit() || ch == '.' } },
                    placeholder = "5", keyboardType = KeyboardType.Decimal, bare = true,
                    modifier = Modifier.padding(top = 6.dp)
                )
            }
            Column(
                Modifier.weight(1f).clip(RoundedCornerShape(16.dp)).background(c.surface).padding(horizontal = 14.dp, vertical = 12.dp)
            ) {
                Text(stringResource(R.string.sheet_custom_ml_label), style = text(12.sp, FontWeight.SemiBold), color = c.muted)
                AppTextField(
                    value = vm.cuMl, onValueChange = { vm.cuMl = it.filter { ch -> ch.isDigit() } },
                    placeholder = "355", keyboardType = KeyboardType.Number, bare = true,
                    modifier = Modifier.padding(top = 6.dp)
                )
            }
        }
        AppTextField(
            value = vm.cuNotes, onValueChange = { vm.cuNotes = it },
            placeholder = stringResource(R.string.sheet_custom_notes_placeholder),
            singleLine = false, minHeight = 72.dp,
            modifier = Modifier.padding(top = 10.dp)
        )
        PrimaryButton(
            stringResource(R.string.action_save), height = 50.dp, enabled = vm.cuName.isNotBlank(),
            modifier = Modifier.padding(top = 14.dp),
            onClick = { vm.saveCustomDrink() }
        )
        Box(
            Modifier.padding(top = 8.dp).fillMaxWidth().height(44.dp).pressable { vm.closeSheet() },
            contentAlignment = Alignment.Center
        ) { Text(stringResource(R.string.action_cancel), style = text(15.sp, FontWeight.SemiBold), color = c.muted) }
    }
}

@Composable
private fun BaseChip(vm: AppViewModel, base: String, label: String) {
    val c = LocalAppColors.current
    val sel = vm.cuBase == base
    Box(
        Modifier
            .height(34.dp)
            .clip(RoundedCornerShape(17.dp))
            .background(if (sel) c.accent else c.surface2)
            .pressable(pressedScale = 0.93f) { vm.cuBase = base }
            .padding(horizontal = 15.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(label, style = text(13.5.sp, FontWeight.SemiBold), color = if (sel) Color.White else c.muted)
    }
}

// ── Export ───────────────────────────────────────────────────────────────

@Composable
private fun ExportSheet(vm: AppViewModel) {
    val c = LocalAppColors.current
    val logs by vm.logs.collectAsState()
    val dry by vm.dryDays.collectAsState()
    val settings by vm.settings.collectAsState()
    val range = StatsEngine.rangeFor(
        vm.period, vm.pageBack, vm.todayKey(), logs, dry, settings.dailyGoal, vm.customFrom, vm.customTo
    )
    val fileName = stringResource(R.string.sheet_export_filename, range.label)
    Column(Modifier.padding(horizontal = 22.dp).padding(top = 8.dp, bottom = 26.dp)) {
        Text(stringResource(R.string.sheet_export_title), style = display(20.sp, tabular = false), color = c.text)
        Row(
            Modifier
                .padding(top = 14.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(c.surface)
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier.size(42.dp, 52.dp).clip(RoundedCornerShape(8.dp)).background(c.surface2),
                contentAlignment = Alignment.Center
            ) { Text(stringResource(R.string.sheet_export_format), style = text(10.sp, FontWeight.Bold), color = c.accent) }
            Column {
                Text(fileName, style = text(14.5.sp, FontWeight.SemiBold), color = c.text)
                Text(
                    stringResource(R.string.sheet_export_desc),
                    style = text(12.5.sp), color = c.muted, modifier = Modifier.padding(top = 2.dp)
                )
            }
        }
        PrimaryButton(
            stringResource(R.string.sheet_export_cta), height = 50.dp,
            modifier = Modifier.padding(top = 16.dp),
            onClick = { vm.exportCsv(fileName.removeSuffix(".csv"), range.logsInRange) }
        )
        Box(
            Modifier.padding(top = 8.dp).fillMaxWidth().height(46.dp).pressable { vm.closeSheet() },
            contentAlignment = Alignment.Center
        ) { Text(stringResource(R.string.action_cancel), style = text(15.5.sp, FontWeight.SemiBold), color = c.muted) }
    }
}

// ── Health Connect ───────────────────────────────────────────────────────

/**
 * Platform brand name, not copy: the catalogue parameterises it so the iOS pack
 * can say "Apple Health" from the same sentence. No resource exists for the
 * name itself, and a brand is not translated.
 */
private const val HEALTH_PLATFORM = "Health Connect"

/** The system path a reader has to walk. Same reasoning as [HEALTH_PLATFORM]. */
private const val HEALTH_SETTINGS_PATH = "Settings › Apps › Health Connect"

@Composable
private fun HealthSheet(vm: AppViewModel) {
    val c = LocalAppColors.current
    Column(Modifier.padding(horizontal = 22.dp).padding(top = 8.dp, bottom = 26.dp)) {
        Text(stringResource(R.string.sheet_health_title), style = display(20.sp, tabular = false), color = c.text)
        Text(
            stringResource(R.string.sheet_health_body, HEALTH_PLATFORM),
            style = text(14.sp), color = c.muted, lineHeight = 22.sp, modifier = Modifier.padding(top = 8.dp)
        )
        Row(
            Modifier
                .padding(top = 14.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(c.surface)
                .padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier.size(30.dp).clip(RoundedCornerShape(8.dp)).background(c.surface2),
                contentAlignment = Alignment.Center
            ) { Box(Modifier.size(11.dp).clip(CircleShape).background(c.b1)) }
            Column {
                Text(stringResource(R.string.sheet_health_data_title), style = text(14.5.sp, FontWeight.SemiBold), color = c.text)
                Text(
                    stringResource(R.string.sheet_health_data_sub),
                    style = text(12.5.sp), color = c.muted, modifier = Modifier.padding(top = 1.dp)
                )
            }
        }
        PrimaryButton(
            stringResource(R.string.sheet_health_cta, HEALTH_PLATFORM), height = 50.dp,
            modifier = Modifier.padding(top = 16.dp),
            onClick = { vm.connectHealth() }
        )
        Box(
            Modifier.padding(top = 8.dp).fillMaxWidth().height(46.dp).pressable { vm.closeSheet() },
            contentAlignment = Alignment.Center
        ) { Text(stringResource(R.string.action_cancel), style = text(15.5.sp, FontWeight.SemiBold), color = c.muted) }
        Text(
            stringResource(R.string.sheet_health_change_access, HEALTH_SETTINGS_PATH),
            style = text(12.sp), color = c.faint, textAlign = TextAlign.Center, lineHeight = 18.sp,
            modifier = Modifier.fillMaxWidth().padding(top = 10.dp)
        )
    }
}

// ── Shared bits ──────────────────────────────────────────────────────────

@Composable
fun SheetCard(top: androidx.compose.ui.unit.Dp, content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit) {
    val c = LocalAppColors.current
    Column(
        Modifier
            .padding(top = top)
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(c.surface)
            .padding(16.dp),
        content = content
    )
}

@Composable
fun ChipButton(label: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val c = LocalAppColors.current
    Box(
        modifier
            .height(34.dp)
            .clip(RoundedCornerShape(17.dp))
            .background(c.surface2)
            .pressable(pressedScale = 0.92f, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(label, style = text(13.5.sp, FontWeight.SemiBold), color = c.muted, maxLines = 1)
    }
}

@Composable
fun AppTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    keyboardType: KeyboardType = KeyboardType.Text,
    singleLine: Boolean = true,
    centered: Boolean = false,
    bare: Boolean = false,
    minHeight: androidx.compose.ui.unit.Dp = 44.dp
) {
    val c = LocalAppColors.current
    androidx.compose.foundation.text.BasicTextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = singleLine,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        textStyle = text(
            if (bare) 19.sp else 15.5.sp,
            if (bare || centered) FontWeight.SemiBold else FontWeight.Normal,
            tabular = bare || centered
        ).copy(color = c.text, textAlign = if (centered) TextAlign.Center else TextAlign.Start),
        cursorBrush = androidx.compose.ui.graphics.SolidColor(c.accent),
        modifier = modifier.fillMaxWidth(),
        decorationBox = { inner ->
            Box(
                Modifier
                    .fillMaxWidth()
                    .heightIn(min = if (bare) 28.dp else minHeight)
                    .then(
                        if (bare) Modifier
                        else Modifier
                            .clip(RoundedCornerShape(14.dp))
                            .background(c.surface2)
                            .padding(horizontal = 16.dp, vertical = if (singleLine) 0.dp else 12.dp)
                    ),
                contentAlignment = if (centered) Alignment.Center else Alignment.CenterStart
            ) {
                if (value.isEmpty()) {
                    Text(placeholder, style = text(if (bare) 19.sp else 15.5.sp), color = c.faint, maxLines = 1)
                }
                inner()
            }
        }
    )
}

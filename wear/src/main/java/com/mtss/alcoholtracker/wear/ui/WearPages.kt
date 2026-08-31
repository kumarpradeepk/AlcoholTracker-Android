package com.mtss.alcoholtracker.wear.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material.Text
import com.mtss.alcoholtracker.wear.R
import com.mtss.alcoholtracker.wear.sync.QuickDrink
import com.mtss.alcoholtracker.wear.sync.WearSnapshot
import kotlin.math.roundToInt

private fun one(v: Double) = ((v * 10).roundToInt() / 10.0).toString()

// ── 1. Glance ───────────────────────────────────────────────────────────

/**
 * The at-a-glance page: where today stands, and the two actions worth taking
 * from a wrist. Everything here is rendered from the phone's snapshot — the
 * watch never recomputes units itself.
 */
@Composable
fun GlancePage(s: WearSnapshot, onLog: () -> Unit, onDry: () -> Unit) {
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(start = 22.dp, end = 22.dp, top = 44.dp, bottom = 24.dp)
    ) {
        Text(s.dayLabel, style = wtext(13.sp, FontWeight.Bold), color = W.sub)

        Row(
            Modifier.padding(top = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            WatchRing(progress = s.ratio.toFloat(), color = W.forRatio(s.ratio)) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        one(s.dayUnits),
                        style = wtext(23.sp, FontWeight.Bold, tabular = true), color = W.ink
                    )
                    Text(
                        stringResource(R.string.w_of_goal, s.dailyGoal.toString()),
                        style = wtext(11.sp, FontWeight.SemiBold), color = W.sub
                    )
                }
            }
            Column {
                Text(s.remainLine, style = wtext(15.sp, FontWeight.Bold), color = W.ink)
                Text(
                    s.weekLine, style = wtext(12.sp, FontWeight.SemiBold), color = W.sub,
                    modifier = Modifier.padding(top = 3.dp)
                )
            }
        }

        // The BAC card only exists for a Pro user who has it switched on; the
        // watch never shows a locked teaser, there being no room to sell here.
        if (s.pro && s.bacOn && s.bacValue.isNotEmpty()) {
            val band = when (s.bacBand) {
                1 -> W.amber
                2 -> W.acc
                else -> W.moss
            }
            Row(
                Modifier
                    .padding(top = 14.dp)
                    .wFadeUp(80)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(22.dp))
                    .background(W.elev)
                    .padding(horizontal = 16.dp, vertical = 13.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(stringResource(R.string.w_bac), style = wtext(11.sp, FontWeight.Bold), color = W.sub)
                    Text(s.bacValue, style = wtext(17.sp, FontWeight.Bold, tabular = true), color = W.ink)
                }
                Row(
                    Modifier
                        .clip(RoundedCornerShape(99.dp))
                        .background(band.copy(alpha = 0.16f))
                        .padding(horizontal = 10.dp, vertical = 5.dp),
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(Modifier.size(5.dp).clip(RoundedCornerShape(99.dp)).background(band))
                    Text(s.bacStatus, style = wtext(11.sp, FontWeight.Bold), color = band)
                }
            }
        }

        Row(
            Modifier.padding(top = 10.dp).wFadeUp(120).fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            WatchButton(
                label = stringResource(R.string.w_log_drink),
                container = W.acc, content = W.onAcc,
                modifier = Modifier.weight(1.3f), onClick = onLog
            )
            WatchButton(
                label = stringResource(R.string.w_dry_day),
                container = W.moss.copy(alpha = 0.22f), content = W.mossInk,
                modifier = Modifier.weight(1f), onClick = onDry
            )
        }
    }
}

@Composable
private fun WatchButton(
    label: String,
    container: androidx.compose.ui.graphics.Color,
    content: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier
            .height(48.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(container)
            .pressable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(label, style = wtext(14.5.sp, FontWeight.Bold), color = content, maxLines = 1)
    }
}

// ── 2. Quick log ────────────────────────────────────────────────────────

/**
 * "The usual?" on the wrist — the same five tiles the phone derives, plus a
 * custom pour for the drink that is not on the list.
 *
 * The custom entry deliberately captures only ABV and serving: naming a drink
 * on a watch is a worse experience than naming it later, so the phone does
 * that part.
 */
@Composable
fun QuickLogPage(
    s: WearSnapshot,
    onLog: (name: String, ml: Double, abv: Double, cost: Double) -> Unit
) {
    var custom by remember { mutableStateOf(false) }
    var abv by remember { mutableStateOf(5.0) }
    var ml by remember { mutableStateOf(330.0) }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(start = 18.dp, end = 18.dp, top = 44.dp, bottom = 24.dp)
    ) {
        if (custom) {
            Row(
                Modifier.padding(bottom = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    Modifier
                        .size(30.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(W.elev)
                        .pressable(pressedScale = 0.9f) { custom = false },
                    contentAlignment = Alignment.Center
                ) { Text("‹", style = wtext(15.sp, FontWeight.Bold), color = W.ink) }
                Text(
                    stringResource(R.string.w_custom_drink),
                    style = wtext(14.sp, FontWeight.Bold), color = W.ink
                )
            }

            // Previewed with the phone's own grams-per-unit, so the number on
            // the wrist is the number the diary will show.
            val units = ml * (abv / 100.0) * 0.789 / s.gramsPerUnit
            Column(
                Modifier.fillMaxWidth().padding(bottom = 10.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(one(units), style = wtext(30.sp, FontWeight.Bold, tabular = true), color = W.ink)
                Text(
                    stringResource(R.string.w_standard_drinks),
                    style = wtext(11.sp, FontWeight.SemiBold), color = W.sub,
                    modifier = Modifier.padding(top = 3.dp)
                )
            }

            StepperRow(
                value = "${abv.let { if (it % 1.0 == 0.0) it.toInt().toString() else one(it) }}%",
                label = stringResource(R.string.w_abv),
                onMinus = { abv = (abv - 0.5).coerceAtLeast(0.5) },
                onPlus = { abv = (abv + 0.5).coerceAtMost(96.0) }
            )
            Spacer(Modifier.height(7.dp))
            StepperRow(
                value = "${ml.toInt()} ml",
                label = stringResource(R.string.w_serving),
                onMinus = { ml = (ml - 10).coerceAtLeast(10.0) },
                onPlus = { ml = (ml + 10).coerceAtMost(1000.0) }
            )
            Spacer(Modifier.height(10.dp))
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(46.dp)
                    .clip(RoundedCornerShape(23.dp))
                    .background(W.acc)
                    .pressable { onLog("Custom", ml, abv, 0.0) },
                contentAlignment = Alignment.Center
            ) {
                Text(stringResource(R.string.w_log_it), style = wtext(14.5.sp, FontWeight.Bold), color = W.onAcc)
            }
            Text(
                stringResource(R.string.w_name_later),
                style = wtext(10.5.sp, FontWeight.SemiBold), color = W.sub,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
            )
        } else {
            Text(
                stringResource(R.string.w_the_usual),
                style = wtext(13.sp, FontWeight.Bold), color = W.sub,
                modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
            )
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(22.dp))
                    .border(1.5.dp, W.acc.copy(alpha = 0.55f), RoundedCornerShape(22.dp))
                    .pressable(pressedScale = 0.96f) { custom = true }
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(11.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(W.acc.copy(alpha = 0.18f)),
                    contentAlignment = Alignment.Center
                ) { Text("+", style = wtext(15.sp, FontWeight.Bold), color = W.acc) }
                Text(
                    stringResource(R.string.w_custom_drink),
                    style = wtext(14.sp, FontWeight.Bold), color = W.acc
                )
            }

            Spacer(Modifier.height(7.dp))
            if (s.quick.isEmpty()) {
                Text(
                    stringResource(R.string.w_no_history),
                    style = wtext(11.5.sp, FontWeight.SemiBold), color = W.sub,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(top = 12.dp)
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                    s.quick.forEachIndexed { i, d -> QuickChip(d, s, i) { onLog(d.name, d.ml, d.abv, d.cost) } }
                }
                Text(
                    stringResource(R.string.w_tune_on_phone),
                    style = wtext(11.sp, FontWeight.SemiBold), color = W.sub,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(top = 12.dp)
                )
            }
        }
    }
}

@Composable
private fun QuickChip(d: QuickDrink, s: WearSnapshot, index: Int, onTap: () -> Unit) {
    val units = d.ml * (d.abv / 100.0) * 0.789 / s.gramsPerUnit
    Row(
        Modifier
            .wFadeUp(index * 40)
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(W.elev)
            .pressable(pressedScale = 0.96f, onClick = onTap)
            .padding(horizontal = 14.dp, vertical = 11.dp),
        horizontalArrangement = Arrangement.spacedBy(11.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .size(32.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(W.ink.copy(alpha = 0.06f)),
            contentAlignment = Alignment.Center
        ) { Text("+", style = wtext(15.sp, FontWeight.Bold), color = W.acc) }
        Column(Modifier.weight(1f)) {
            Text(d.name, style = wtext(14.sp, FontWeight.Bold), color = W.ink, maxLines = 1)
            Text(
                "${d.ml.toInt()} ml · ${if (d.abv % 1.0 == 0.0) d.abv.toInt() else d.abv}%",
                style = wtext(11.sp, FontWeight.SemiBold), color = W.sub, maxLines = 1
            )
        }
        Text(one(units), style = wtext(13.sp, FontWeight.Bold, tabular = true), color = W.sub)
    }
}

@Composable
private fun StepperRow(value: String, label: String, onMinus: () -> Unit, onPlus: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(W.elev)
            .padding(horizontal = 13.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        StepperKey("−", onMinus)
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(value, style = wtext(15.sp, FontWeight.Bold, tabular = true), color = W.ink)
            Text(label, style = wtext(10.sp, FontWeight.SemiBold), color = W.sub)
        }
        StepperKey("+", onPlus)
    }
}

@Composable
private fun StepperKey(glyph: String, onClick: () -> Unit) {
    Box(
        Modifier
            .size(34.dp)
            .clip(RoundedCornerShape(13.dp))
            .background(W.well)
            .pressable(pressedScale = 0.9f, onClick = onClick),
        contentAlignment = Alignment.Center
    ) { Text(glyph, style = wtext(17.sp, FontWeight.Bold), color = W.ink) }
}

// ── 3. Dry day ──────────────────────────────────────────────────────────

/**
 * Banking a dry day from the wrist.
 *
 * The phone's rule holds here: a day with drinks on it cannot be marked, and
 * the watch says so rather than silently refusing — the button is replaced by
 * an explanation instead of being disabled without a reason.
 */
@Composable
fun DryDayPage(s: WearSnapshot, onToggle: (Boolean) -> Unit) {
    val blocked = s.dayHasDrinks && !s.isDryToday
    Column(
        Modifier
            .fillMaxSize()
            .padding(start = 22.dp, end = 22.dp, top = 44.dp, bottom = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp, Alignment.CenterVertically)
    ) {
        Box(
            Modifier
                .size(76.dp)
                .clip(RoundedCornerShape(28.dp))
                .background(if (s.isDryToday) W.moss else W.elev),
            contentAlignment = Alignment.Center
        ) {
            Text(
                "✓",
                style = wtext(30.sp, FontWeight.Bold),
                color = if (s.isDryToday) W.onAcc else W.sub
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                stringResource(
                    when {
                        s.isDryToday -> R.string.w_dry_marked
                        blocked -> R.string.w_dry_has_drinks
                        else -> R.string.w_dry_unmarked
                    }
                ),
                style = wtext(17.sp, FontWeight.Bold), color = W.ink, textAlign = TextAlign.Center
            )
            Text(
                stringResource(
                    when {
                        s.isDryToday -> R.string.w_dry_marked_sub
                        blocked -> R.string.w_dry_has_drinks_sub
                        else -> R.string.w_dry_unmarked_sub
                    }
                ),
                style = wtext(12.sp, FontWeight.SemiBold), color = W.sub,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 3.dp)
            )
        }
        if (!blocked) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(if (s.isDryToday) W.elev else W.moss)
                    .pressable { onToggle(!s.isDryToday) },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    stringResource(if (s.isDryToday) R.string.w_unmark else R.string.w_mark_dry),
                    style = wtext(15.sp, FontWeight.Bold),
                    color = if (s.isDryToday) W.sub else W.onAcc
                )
            }
        }
    }
}

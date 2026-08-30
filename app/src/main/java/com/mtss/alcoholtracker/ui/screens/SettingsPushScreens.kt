package com.mtss.alcoholtracker.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
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
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.fillMaxHeight
import com.mtss.alcoholtracker.data.DarkChoice
import com.mtss.alcoholtracker.ui.theme.AppTheme
import com.mtss.alcoholtracker.ui.theme.colorsFor
import com.mtss.alcoholtracker.ui.theme.LocalAppGeometry
import com.mtss.alcoholtracker.R
import com.mtss.alcoholtracker.domain.AlcoholMath
import com.mtss.alcoholtracker.domain.rememberUnits
import com.mtss.alcoholtracker.domain.unitsPlural
import com.mtss.alcoholtracker.domain.unitsString
import com.mtss.alcoholtracker.ui.AppViewModel
import com.mtss.alcoholtracker.ui.AppDialog
import com.mtss.alcoholtracker.ui.PushScreen
import com.mtss.alcoholtracker.ui.Sheet
import com.mtss.alcoholtracker.ui.components.AppSwitch
import com.mtss.alcoholtracker.ui.components.CheckMark
import com.mtss.alcoholtracker.ui.components.Chevron
import com.mtss.alcoholtracker.ui.components.ChevronDirection
import com.mtss.alcoholtracker.ui.components.DropletMark
import com.mtss.alcoholtracker.ui.components.PrimaryButton
import com.mtss.alcoholtracker.ui.components.Segmented
import com.mtss.alcoholtracker.ui.components.SoftButton
import com.mtss.alcoholtracker.ui.components.StepperRow
import com.mtss.alcoholtracker.ui.components.SwirlingGlass
import com.mtss.alcoholtracker.ui.components.TrendChart
import com.mtss.alcoholtracker.ui.components.pressable
import com.mtss.alcoholtracker.ui.components.rememberInfiniteBreath
import com.mtss.alcoholtracker.ui.components.riseIn
import com.mtss.alcoholtracker.ui.theme.LocalAppColors
import com.mtss.alcoholtracker.ui.theme.LocalReducedMotion
import com.mtss.alcoholtracker.ui.theme.Motion
import com.mtss.alcoholtracker.ui.theme.display
import com.mtss.alcoholtracker.ui.theme.text
import com.mtss.alcoholtracker.util.Formatters

/** Slide-in push panel over the tab content, like the mock's pushTo(). */
@Composable
fun PushHost(vm: AppViewModel) {
    val c = LocalAppColors.current
    AnimatedVisibility(
        visible = vm.push != null,
        enter = slideInHorizontally(tween(460, easing = Motion.Settle)) { it } + fadeIn(tween(200)),
        exit = slideOutHorizontally(tween(420, easing = Motion.Settle)) { it } + fadeOut(tween(250))
    ) {
        val push = vm.push ?: return@AnimatedVisibility
        Column(
            Modifier
                .fillMaxSize()
                .background(c.bg)
        ) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 18.dp).padding(top = 18.dp, bottom = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    Modifier
                        .size(36.dp)
                        .shadow(if (c.isDark) 0.dp else 4.dp, CircleShape)
                        .clip(CircleShape)
                        .background(c.surface)
                        .pressable(pressedScale = 0.88f) { vm.closePush() },
                    contentAlignment = Alignment.Center
                ) { Chevron(ChevronDirection.LEFT, c.muted, 13.dp) }
                Text(pushTitle(push), style = display(20.sp, tabular = false, letterSpacing = (-0.3).sp), color = c.text)
            }
            Column(
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 18.dp)
                    .padding(top = 8.dp, bottom = 140.dp)
            ) {
                when (push) {
                    PushScreen.PROFILE -> ProfilePush(vm)
                    PushScreen.UNITS -> UnitsPush(vm)
                    PushScreen.NOTIFS -> NotifsPush(vm)
                    PushScreen.BAC -> BacPush(vm)
                    PushScreen.QUICKLOG -> QuickLogPush(vm)
                    PushScreen.BACKUP -> BackupPush(vm)
                    PushScreen.ABOUT -> AboutPush(vm)
                    PushScreen.ICON -> IconPush(vm)
                    PushScreen.THEME -> ThemePush(vm)
                    PushScreen.TRENDS -> TrendsPush(vm)
                    PushScreen.GUIDE -> GuidePush(vm)
                }
            }
        }
    }
}

/**
 * The push-panel headline. [PushScreen] carries an English label for the enum's
 * own sake; the displayed wording comes from the resource catalogue, resolved by
 * enum value here.
 */

/**
 * The guideline stepper's value: one localized string, with the numeral rendered
 * at display weight and everything around it at caption weight.
 *
 * It is a single string on purpose. Polish inflects the unit noun *and* its
 * adjective with the CLDR plural category (jednostka / jednostki / jednostek,
 * drink standardowy / drinki standardowe / drinkow standardowych), so number and
 * noun cannot be laid out as two independent runs - punch-list A2-worst.
 */
@Composable
private fun StepperValue(label: String, value: Int) {
    val c = LocalAppColors.current
    val digits = value.toString()
    val at = label.indexOf(digits)
    val styled = buildAnnotatedString {
        append(label)
        if (at >= 0) {
            addStyle(
                SpanStyle(fontSize = 30.sp, fontWeight = FontWeight.Bold, color = c.text),
                at, at + digits.length
            )
        }
    }
    Text(styled, style = text(14.sp, FontWeight.Medium), color = c.muted)
}

@Composable
private fun pushTitle(push: PushScreen): String = when (push) {
    PushScreen.PROFILE -> stringResource(R.string.set_profile)
    PushScreen.UNITS -> stringResource(R.string.set_units)
    PushScreen.NOTIFS -> stringResource(R.string.set_notifications)
    PushScreen.BAC -> stringResource(R.string.bac_monitor)
    PushScreen.QUICKLOG -> stringResource(R.string.push_title_quick_log)
    PushScreen.BACKUP -> stringResource(R.string.push_title_backup)
    PushScreen.ABOUT -> stringResource(R.string.push_title_about)
    PushScreen.ICON -> stringResource(R.string.set_app_icon)
    PushScreen.THEME -> stringResource(R.string.push_title_theme)
    PushScreen.TRENDS -> stringResource(R.string.push_title_trends)
    PushScreen.GUIDE -> stringResource(R.string.push_title_guideline)
}

// ── Profile ──────────────────────────────────────────────────────────────

@Composable
private fun ProfilePush(vm: AppViewModel) {
    val c = LocalAppColors.current
    val settings by vm.settings.collectAsState()
    Column(Modifier.riseIn(durationMillis = 400)) {
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
            SwirlingGlass(width = 58.dp, height = 76.dp)
            Column {
                Text(stringResource(R.string.profile_headline), style = display(19.sp, tabular = false, letterSpacing = (-0.2).sp), color = c.text)
                Text(
                    stringResource(R.string.profile_body),
                    style = text(13.5.sp), color = c.muted, lineHeight = 20.sp,
                    modifier = Modifier.padding(top = 5.dp)
                )
            }
        }
        Text(
            stringResource(R.string.profile_privacy_note),
            style = text(12.5.sp), color = c.faint, modifier = Modifier.padding(top = 12.dp, start = 2.dp)
        )
        SettingsCard(top = 16.dp) {
            Column(Modifier.padding(16.dp)) {
                Text(stringResource(R.string.profile_sex_label), style = text(13.sp, FontWeight.SemiBold), color = c.muted)
                Segmented(
                    options = listOf(
                        stringResource(R.string.profile_sex_female),
                        stringResource(R.string.profile_sex_male)
                    ),
                    selectedIndex = when (settings.sex) { "Female" -> 0; "Male" -> 1; else -> -1 },
                    modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
                    onSelect = { i -> vm.updateSettings { it.copy(sex = if (i == 0) "Female" else "Male") } }
                )
            }
        }
        SettingsCard(top = 12.dp) {
            Column(Modifier.padding(16.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(R.string.profile_weight_label), style = text(13.sp, FontWeight.SemiBold), color = c.muted)
                    Segmented(
                        options = listOf(
                            stringResource(R.string.profile_weight_kg),
                            stringResource(R.string.profile_weight_lb)
                        ),
                        selectedIndex = if (settings.weightKgUnit) 0 else 1,
                        itemPadding = 4.dp,
                        fontSize = 12.5.sp,
                        modifier = Modifier.width(110.dp),
                        onSelect = { i -> vm.updateSettings { it.copy(weightKgUnit = i == 0) } }
                    )
                }
                AppTextField(
                    value = settings.weight,
                    onValueChange = { v -> vm.updateSettings { it.copy(weight = v.filter { ch -> ch.isDigit() || ch == '.' }) } },
                    placeholder = stringResource(R.string.profile_weight_placeholder),
                    keyboardType = KeyboardType.Decimal,
                    centered = true,
                    minHeight = 48.dp,
                    modifier = Modifier.padding(top = 12.dp)
                )
            }
        }
        PrimaryButton(
            stringResource(R.string.profile_cta), height = 52.dp,
            modifier = Modifier.padding(top = 18.dp),
            onClick = { vm.saveProfile() }
        )
        Text(
            stringResource(R.string.profile_footer),
            style = text(12.5.sp), color = c.faint, textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(top = 10.dp)
        )
    }
}

// ── Units ────────────────────────────────────────────────────────────────

@Composable
private fun UnitsPush(vm: AppViewModel) {
    val c = LocalAppColors.current
    val settings by vm.settings.collectAsState()
    Column(Modifier.riseIn(durationMillis = 400)) {
        SettingsCard(top = 0.dp) {
            Column(Modifier.padding(16.dp)) {
                Text(stringResource(R.string.units_energy_label), style = text(13.sp, FontWeight.SemiBold), color = c.muted)
                Segmented(
                    options = listOf(
                        stringResource(R.string.units_kcal),
                        stringResource(R.string.units_kj)
                    ),
                    selectedIndex = if (settings.energyKcal) 0 else 1,
                    modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
                    onSelect = { i -> vm.updateSettings { it.copy(energyKcal = i == 0) } }
                )
            }
        }
        SettingsCard(top = 12.dp) {
            Column(Modifier.padding(16.dp)) {
                Text(stringResource(R.string.units_serving_label), style = text(13.sp, FontWeight.SemiBold), color = c.muted)
                Segmented(
                    options = listOf(
                        stringResource(R.string.units_millilitres),
                        stringResource(R.string.units_ounces)
                    ),
                    selectedIndex = if (settings.servingMl) 0 else 1,
                    modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
                    onSelect = { i -> vm.updateSettings { it.copy(servingMl = i == 0) } }
                )
            }
        }
        Text(
            stringResource(R.string.units_note),
            style = text(13.sp), color = c.faint, lineHeight = 19.sp,
            modifier = Modifier.padding(top = 14.dp, start = 4.dp)
        )
    }
}

// ── Notifications ────────────────────────────────────────────────────────

@Composable
private fun NotifsPush(vm: AppViewModel) {
    val c = LocalAppColors.current
    val reminders by vm.reminders.collectAsState()
    val reduced = LocalReducedMotion.current
    Column(Modifier.riseIn(durationMillis = 400)) {
        if (reminders.isNotEmpty()) {
            SettingsCard(top = 0.dp) {
                reminders.forEachIndexed { i, n ->
                    if (i > 0) Divider1()
                    Row(
                        Modifier.fillMaxWidth().heightIn(min = 56.dp).padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(n.title, style = text(15.sp, FontWeight.SemiBold), color = c.text)
                            Text(
                                if (n.message.isNotEmpty())
                                    stringResource(R.string.notif_reminder_sub, Formatters.time(n.timeMinutes), n.message)
                                else Formatters.time(n.timeMinutes),
                                style = text(13.sp), color = c.muted, modifier = Modifier.padding(top = 2.dp)
                            )
                        }
                        Box(
                            Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(c.surface2)
                                .pressable(pressedScale = 0.85f) { vm.removeReminder(n) },
                            contentAlignment = Alignment.Center
                        ) { Text("−", style = text(17.sp), color = c.b3) }
                    }
                }
            }
            Text(
                stringResource(R.string.notif_repeat_hint),
                style = text(12.5.sp), color = c.faint, modifier = Modifier.padding(top = 10.dp, start = 4.dp)
            )
        } else {
            Column(
                Modifier.fillMaxWidth().padding(top = 36.dp, bottom = 10.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(contentAlignment = Alignment.Center) {
                    if (!reduced) {
                        val ripple = rememberInfiniteBreath(1800)
                        Box(
                            Modifier
                                .size((56 + 40 * ripple).dp)
                                .border(1.5.dp, c.accent.copy(alpha = 1f - ripple), CircleShape)
                        )
                    }
                    DropletMark(26.dp)
                }
                Text(stringResource(R.string.notif_empty_title), style = display(18.sp, tabular = false), color = c.text, modifier = Modifier.padding(top = 18.dp))
                Text(
                    stringResource(R.string.notif_empty_body),
                    style = text(14.sp), color = c.muted, textAlign = TextAlign.Center, lineHeight = 21.sp,
                    modifier = Modifier.padding(top = 7.dp).width(260.dp)
                )
            }
        }
        PrimaryButton(
            stringResource(R.string.notif_create_cta), height = 52.dp,
            modifier = Modifier.padding(top = 18.dp),
            onClick = { vm.openNewNotif() }
        )
    }
}

// ── BAC Monitor settings ─────────────────────────────────────────────────

@Composable
private fun BacPush(vm: AppViewModel) {
    val c = LocalAppColors.current
    val settings by vm.settings.collectAsState()
    Column(Modifier.riseIn(durationMillis = 400)) {
        SettingsCard(top = 0.dp) {
            Row(
                Modifier.fillMaxWidth().heightIn(min = 56.dp).padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text(stringResource(R.string.bac_monitor), style = text(15.5.sp), color = c.text)
                    Text(
                        stringResource(R.string.set_bac_row_sub),
                        style = text(12.5.sp), color = c.muted, modifier = Modifier.padding(top = 2.dp)
                    )
                }
                AppSwitch(settings.bacOn) { vm.updateSettings { it.copy(bacOn = !it.bacOn) } }
            }
            Divider1()
            Row(
                Modifier.fillMaxWidth().heightIn(min = 52.dp).padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(stringResource(R.string.set_bac_unit_label), style = text(15.5.sp), color = c.text)
                Segmented(
                    options = listOf(
                        stringResource(R.string.set_bac_unit_percent),
                        stringResource(R.string.set_bac_unit_permille)
                    ),
                    selectedIndex = if (settings.bacUnitPercent) 0 else 1,
                    itemPadding = 5.dp,
                    fontSize = 13.sp,
                    modifier = Modifier.width(110.dp),
                    onSelect = { i -> vm.updateSettings { it.copy(bacUnitPercent = i == 0) } }
                )
            }
        }
        Row(
            Modifier
                .padding(top = 12.dp)
                .fillMaxWidth()
                .shadow(if (c.isDark) 0.dp else 5.dp, RoundedCornerShape(20.dp), spotColor = Color.Black.copy(alpha = 0.2f))
                .clip(RoundedCornerShape(20.dp))
                .background(c.surface)
                .pressable(pressedScale = 0.98f) { vm.openSheet(Sheet.LivePreview) }
                .padding(horizontal = 16.dp, vertical = 15.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.set_bac_preview_title), style = text(15.5.sp), color = c.text)
                Text(
                    stringResource(R.string.set_bac_preview_sub),
                    style = text(12.5.sp), color = c.muted, modifier = Modifier.padding(top = 2.dp)
                )
            }
            Chevron(ChevronDirection.RIGHT, c.faint, 11.dp)
        }
        Box(
            Modifier
                .padding(top = 14.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(c.surface2)
                .padding(horizontal = 16.dp, vertical = 13.dp)
        ) {
            Text(
                stringResource(R.string.set_bac_disclaimer),
                style = text(13.sp, FontWeight.Medium), color = c.b2, lineHeight = 19.sp
            )
        }
    }
}

// ── Quick log tiles (the mock's Apple Watch screen, Android-shaped) ──────

@Composable
private fun QuickLogPush(vm: AppViewModel) {
    val c = LocalAppColors.current
    val saved by vm.savedDrinks.collectAsState()
    val selected = saved.count { it.quickAccess }
    Column(Modifier.riseIn(durationMillis = 400)) {
        Box(Modifier.fillMaxWidth().padding(top = 10.dp, bottom = 4.dp), contentAlignment = Alignment.Center) {
            Column(
                Modifier
                    .size(132.dp, 160.dp)
                    .clip(RoundedCornerShape(30.dp))
                    .background(Color(0xFF101012))
                    .border(4.dp, Color(0xFF2E2E33), RoundedCornerShape(30.dp)),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(7.dp, Alignment.CenterVertically)
            ) {
                Text(stringResource(R.string.quicklog_mock_caption), style = text(8.5.sp, FontWeight.SemiBold, letterSpacing = 0.5.sp), color = Color.White.copy(alpha = 0.55f))
                WPill(stringResource(R.string.drink_preset_margarita))
                WPill(stringResource(R.string.drink_preset_gin_tonic), moss = true)
                Text(stringResource(R.string.quicklog_mock_footer), style = text(9.sp), color = Color.White.copy(alpha = 0.4f))
            }
        }
        Row(
            Modifier.fillMaxWidth().padding(top = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(stringResource(R.string.quicklog_section_caption), style = text(12.sp, FontWeight.SemiBold, letterSpacing = 0.6.sp), color = c.faint)
            Text(stringResource(R.string.quicklog_selected_count, selected, 4), style = text(12.5.sp), color = c.muted)
        }
        if (saved.isNotEmpty()) {
            SettingsCard(top = 8.dp) {
                saved.forEachIndexed { i, w ->
                    if (i > 0) Divider1()
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .heightIn(min = 52.dp)
                            .pressable(pressedScale = 1f) { vm.toggleQuickAccess(w) }
                            .padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            Modifier
                                .size(22.dp)
                                .clip(CircleShape)
                                .background(if (w.quickAccess) c.accent else Color.Transparent)
                                .border(1.5.dp, if (w.quickAccess) c.accent else c.line, CircleShape),
                            contentAlignment = Alignment.Center
                        ) { if (w.quickAccess) CheckMark(size = 9.dp) }
                        Text(w.name, style = text(15.sp, FontWeight.Medium), color = c.text, modifier = Modifier.weight(1f))
                        Text(stringResource(R.string.quicklog_drink_meta, w.ml.toInt(), w.abv.toString()), style = text(13.sp), color = c.muted)
                    }
                }
            }
        } else {
            SettingsCard(top = 8.dp) {
                Column(
                    Modifier.fillMaxWidth().padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(stringResource(R.string.quicklog_empty_title), style = text(14.5.sp, FontWeight.SemiBold), color = c.text)
                    Text(
                        stringResource(R.string.quicklog_empty_sub),
                        style = text(13.sp), color = c.muted, textAlign = TextAlign.Center, lineHeight = 20.sp,
                        modifier = Modifier.padding(top = 5.dp)
                    )
                }
            }
        }
        SoftButton(
            stringResource(R.string.log_custom_title),
            modifier = Modifier.padding(top = 12.dp),
            onClick = { vm.openCustomDrink() }
        )
    }
}

@Composable
private fun WPill(label: String, moss: Boolean = false) {
    val c = LocalAppColors.current
    Box(
        Modifier
            .size(92.dp, 26.dp)
            .clip(RoundedCornerShape(13.dp))
            .background(if (moss) c.b1.copy(alpha = 0.22f) else c.accent.copy(alpha = 0.25f)),
        contentAlignment = Alignment.Center
    ) {
        Text(label, style = text(10.sp, FontWeight.SemiBold), color = if (moss) c.b1 else c.accent)
    }
}

// ── Backup & Restore ─────────────────────────────────────────────────────

@Composable
private fun BackupPush(vm: AppViewModel) {
    val c = LocalAppColors.current
    val settings by vm.settings.collectAsState()
    val logs by vm.logs.collectAsState()
    val dry by vm.dryDays.collectAsState()
    Column(Modifier.riseIn(durationMillis = 400)) {
        Row(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(c.surface2)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(Modifier.size(8.dp).clip(CircleShape).background(c.b1))
            Text(
                if (settings.lastBackupAt > 0)
                    stringResource(
                        R.string.backup_status_dated,
                        Formatters.dateTimeShort(settings.lastBackupAt)
                    )
                else stringResource(R.string.backup_status_none_android),
                style = text(13.5.sp, FontWeight.SemiBold), color = c.b1
            )
        }
        SettingsCard(top = 12.dp) {
            Column(Modifier.padding(16.dp)) {
                Text(stringResource(R.string.backup_whats_here), style = text(13.sp, FontWeight.SemiBold), color = c.muted)
                Row(Modifier.padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    CountTile("${logs.size}", pluralStringResource(R.plurals.backup_count_logs, logs.size), c.text, Modifier.weight(1f))
                    CountTile("${dry.size}", pluralStringResource(R.plurals.backup_count_dry, dry.size), c.b1, Modifier.weight(1f))
                    CountTile("1", stringResource(R.string.backup_count_profile), c.text, Modifier.weight(1f))
                }
            }
        }
        SettingsCard(top = 12.dp) {
            ActionRow(stringResource(R.string.backup_export)) { vm.exportDataTapped() }
            Divider1()
            ActionRow(stringResource(R.string.backup_import)) { vm.importDataTapped() }
            Divider1()
            ActionRow(stringResource(R.string.backup_create)) { vm.createLocalBackup() }
            Divider1()
            ActionRow(stringResource(R.string.backup_restore)) { vm.restoreLocalBackup() }
        }
        Text(
            stringResource(R.string.backup_merge_note),
            style = text(12.5.sp), color = c.faint, lineHeight = 19.sp,
            modifier = Modifier.padding(top = 10.dp, start = 4.dp)
        )
        Text(
            stringResource(R.string.backup_danger_zone),
            style = text(12.sp, FontWeight.SemiBold, letterSpacing = 0.6.sp), color = c.b3,
            modifier = Modifier.padding(top = 22.dp, start = 4.dp)
        )
        SettingsCard(top = 8.dp) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .heightIn(min = 52.dp)
                    .pressable(pressedScale = 1f) { vm.openDialog(AppDialog.CLEAR_ALL) }
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(stringResource(R.string.backup_clear_all), style = text(15.5.sp, FontWeight.Medium), color = c.b3)
            }
        }
        Text(
            stringResource(R.string.backup_clear_note),
            style = text(12.5.sp), color = c.faint, lineHeight = 19.sp,
            modifier = Modifier.padding(top = 10.dp, start = 4.dp)
        )
    }
}

@Composable
private fun CountTile(value: String, label: String, valueColor: Color, modifier: Modifier = Modifier) {
    val c = LocalAppColors.current
    Column(
        modifier.clip(RoundedCornerShape(14.dp)).background(c.surface2).padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(value, style = display(19.sp), color = valueColor)
        Text(label, style = text(11.5.sp), color = c.muted, modifier = Modifier.padding(top = 2.dp))
    }
}

@Composable
private fun ActionRow(label: String, onClick: () -> Unit) {
    val c = LocalAppColors.current
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 52.dp)
            .pressable(pressedScale = 1f, onClick = onClick)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = text(15.5.sp, FontWeight.Medium), color = c.accent)
    }
}

// ── About ────────────────────────────────────────────────────────────────

@Composable
private fun AboutPush(vm: AppViewModel) {
    val c = LocalAppColors.current
    Column(Modifier.riseIn(durationMillis = 400)) {
        Column(Modifier.fillMaxWidth().padding(top = 14.dp, bottom = 6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            DropletMark(44.dp)
            Text(stringResource(R.string.app_name), style = display(17.sp, tabular = false), color = c.text, modifier = Modifier.padding(top = 16.dp))
            Text(
                stringResource(R.string.about_version, stringResource(R.string.set_about_version_value), "Still Water"),
                style = text(13.sp), color = c.muted, modifier = Modifier.padding(top = 3.dp)
            )
        }
        SettingsCard(top = 16.dp) {
            NavRow(stringResource(R.string.about_privacy), value = null, icon = {}) { vm.hostActions?.openUrl("https://alcoholtracker.app/privacy") }
            Divider1()
            NavRow(stringResource(R.string.about_terms), value = null, icon = {}) { vm.hostActions?.openUrl("https://alcoholtracker.app/terms") }
        }
        Text(
            stringResource(R.string.about_ack_caption),
            style = text(12.sp, FontWeight.SemiBold, letterSpacing = 0.6.sp), color = c.faint,
            modifier = Modifier.padding(top = 20.dp, start = 4.dp)
        )
        SettingsCard(top = 8.dp) {
            AckRow("Jetpack Compose", stringResource(R.string.about_ack_role_ui_android))
            Divider1()
            AckRow("Room", stringResource(R.string.about_ack_role_db))
            Divider1()
            AckRow("DataStore", stringResource(R.string.about_ack_role_settings_android))
        }
        Text(
            stringResource(R.string.about_footer),
            style = text(12.sp), color = c.faint, textAlign = TextAlign.Center, lineHeight = 19.sp,
            modifier = Modifier.fillMaxWidth().padding(top = 20.dp)
        )
    }
}

@Composable
private fun AckRow(name: String, role: String) {
    val c = LocalAppColors.current
    Row(
        Modifier.fillMaxWidth().heightIn(min = 48.dp).padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(name, style = text(15.sp), color = c.text, modifier = Modifier.weight(1f))
        Text(role, style = text(13.sp), color = c.faint)
    }
}

// ── App icon ─────────────────────────────────────────────────────────────

@Composable
private fun IconPush(vm: AppViewModel) {
    val c = LocalAppColors.current
    val settings by vm.settings.collectAsState()
    val options = listOf(
        Triple(stringResource(R.string.set_icon_default), c.accent, Color.White),
        Triple(stringResource(R.string.set_icon_gift), Color(0xFFF5EBDA), Color(0xFFB97F2E)),
        Triple(stringResource(R.string.set_icon_holiday), Color(0xFF22333D), Color(0xFF8CB694))
    )
    Column(Modifier.riseIn(durationMillis = 400)) {
        Text(
            stringResource(R.string.icon_intro),
            style = text(14.sp), color = c.muted, lineHeight = 21.sp
        )
        Row(
            Modifier.fillMaxWidth().padding(top = 18.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp, Alignment.CenterHorizontally)
        ) {
            options.forEachIndexed { i, (name, tileBg, drop) ->
                val sel = settings.iconIndex == i
                Column(
                    Modifier.pressable(pressedScale = 0.93f) {
                        vm.updateSettings { it.copy(iconIndex = i) }
                    },
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        Modifier
                            .size(84.dp)
                            .shadow(if (c.isDark) 0.dp else 8.dp, RoundedCornerShape(20.dp))
                            .clip(RoundedCornerShape(20.dp))
                            .background(tileBg)
                            .border(2.5.dp, if (sel) c.accent else Color.Transparent, RoundedCornerShape(20.dp)),
                        contentAlignment = Alignment.Center
                    ) { DropletMark(30.dp, color = drop, breathing = false) }
                    Text(name, style = text(13.sp, FontWeight.SemiBold), color = c.muted)
                }
            }
        }
        Text(
            stringResource(R.string.icon_relaunch_note),
            style = text(12.sp), color = c.faint, textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(top = 14.dp)
        )
        PrimaryButton(
            stringResource(R.string.action_done), height = 52.dp,
            modifier = Modifier.padding(top = 20.dp),
            onClick = { vm.applyIconChoice(vm.settings.value.iconIndex) }
        )
    }
}

// ── BAC Trends ───────────────────────────────────────────────────────────

@Composable
private fun TrendsPush(vm: AppViewModel) {
    val c = LocalAppColors.current
    val settings by vm.settings.collectAsState()
    val logs by vm.logs.collectAsState()
    val today = vm.todayKey()
    val weightKg = settings.weightKg()
    val hasProfile = weightKg > 0 && settings.sex != null
    val values = if (hasProfile) {
        (-6..0).map { off ->
            val dayLogs = logs.filter { it.epochDay == today + off }
            val grams = dayLogs.sumOf { AlcoholMath.grams(it.ml, it.abv) }
            val r = if (settings.sex == "Female") 0.55 else 0.68
            (grams / (weightKg * 1000.0 * r) * 100.0).toFloat()
        }
    } else emptyList()
    val empty = !hasProfile || values.all { it == 0f }
    Column(Modifier.riseIn(durationMillis = 400)) {
        if (!empty) {
            SettingsCard(top = 0.dp) {
                Column(Modifier.padding(18.dp)) {
                    Text(stringResource(R.string.trends_title), style = text(15.sp, FontWeight.SemiBold), color = c.text)
                    Text(stringResource(R.string.trends_range), style = text(12.5.sp), color = c.muted, modifier = Modifier.padding(top = 3.dp))
                    TrendChart(values = values, chartHeight = 120.dp, modifier = Modifier.padding(top = 12.dp))
                    Row(Modifier.fillMaxWidth().padding(top = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(stringResource(R.string.trends_axis_start), style = text(10.5.sp), color = c.faint)
                        Text(stringResource(R.string.trends_axis_end), style = text(10.5.sp), color = c.faint)
                    }
                }
            }
            Text(
                stringResource(R.string.trends_note),
                style = text(13.sp), color = c.muted, lineHeight = 20.sp,
                modifier = Modifier.padding(top = 14.dp, start = 4.dp)
            )
        } else {
            Column(
                Modifier.fillMaxWidth().padding(top = 40.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                SwirlingGlass(width = 72.dp, height = 96.dp)
                Text(stringResource(R.string.trends_empty_title), style = display(17.sp, tabular = false), color = c.text, modifier = Modifier.padding(top = 18.dp))
                Text(
                    stringResource(R.string.trends_empty_body),
                    style = text(14.sp), color = c.muted, textAlign = TextAlign.Center, lineHeight = 21.sp,
                    modifier = Modifier.padding(top = 7.dp).width(250.dp)
                )
            }
        }
    }
}

// ── Alcohol guideline ────────────────────────────────────────────────────

@Composable
private fun GuidePush(vm: AppViewModel) {
    val c = LocalAppColors.current
    val settings by vm.settings.collectAsState()
    val units = rememberUnits()
    Column(Modifier.riseIn(durationMillis = 400)) {
        Text(
            unitsString(R.string.guideline_intro, units.noun.plural),
            style = text(14.sp), color = c.muted, lineHeight = 21.sp
        )
        SettingsCard(top = 16.dp) {
            Column(Modifier.padding(16.dp)) {
                Text(stringResource(R.string.guideline_daily_target), style = text(13.sp, FontWeight.SemiBold), color = c.muted)
                Box(Modifier.padding(top = 10.dp)) {
                    StepperRow(
                        valueText = {
                            // One string, not two runs: Polish inflects the noun
                            // *and* its adjective with the CLDR category, so the
                            // number and the noun have to travel together
                            // (punch-list A2-worst). The digits keep the display
                            // weight via a span.
                            StepperValue(
                                unitsPlural(
                                    R.plurals.guideline_target_units,
                                    settings.dailyGoal, settings.dailyGoal, units.noun.plural
                                ),
                                settings.dailyGoal
                            )
                        },
                        onMinus = { vm.updateSettings { it.copy(dailyGoal = (it.dailyGoal - 1).coerceAtLeast(1)) } },
                        onPlus = { vm.updateSettings { it.copy(dailyGoal = (it.dailyGoal + 1).coerceAtMost(6)) } }
                    )
                }
            }
        }
        SettingsCard(top = 12.dp) {
            Column(Modifier.padding(16.dp)) {
                Text(stringResource(R.string.guideline_weekly_target), style = text(13.sp, FontWeight.SemiBold), color = c.muted)
                Box(Modifier.padding(top = 10.dp)) {
                    StepperRow(
                        valueText = {
                            // One string, not two runs: Polish inflects the noun
                            // *and* its adjective with the CLDR category, so the
                            // number and the noun have to travel together
                            // (punch-list A2-worst). The digits keep the display
                            // weight via a span.
                            StepperValue(
                                unitsPlural(
                                    R.plurals.guideline_target_units,
                                    settings.weeklyGoal, settings.weeklyGoal, units.noun.plural
                                ),
                                settings.weeklyGoal
                            )
                        },
                        onMinus = { vm.updateSettings { it.copy(weeklyGoal = (it.weeklyGoal - 1).coerceAtLeast(2)) } },
                        onPlus = { vm.updateSettings { it.copy(weeklyGoal = (it.weeklyGoal + 1).coerceAtMost(30)) } }
                    )
                }
            }
        }
        Text(
            unitsPlural(
                R.plurals.guideline_monthly_note,
                settings.monthlyGoal,
                settings.monthlyGoal,
                units.noun.forCount(settings.monthlyGoal.toDouble())
            ),
            style = text(13.sp), color = c.faint, lineHeight = 20.sp,
            modifier = Modifier.padding(top = 14.dp, start = 4.dp)
        )
    }
}


// ── Theme ────────────────────────────────────────────────────────────────

/**
 * The canvas's theme card list. Each row previews the theme in its *own*
 * colours rather than the active one, so the choice is legible before it is
 * made — that is why the swatches are literals here and not tokens.
 */
private data class ThemeOption(
    val theme: AppTheme,
    @androidx.annotation.StringRes val nameRes: Int,
    @androidx.annotation.StringRes val descRes: Int
)

private val THEME_OPTIONS = listOf(
    ThemeOption(AppTheme.KILN, R.string.theme_kiln, R.string.theme_kiln_desc),
    ThemeOption(AppTheme.NOCTURNE, R.string.theme_nocturne, R.string.theme_nocturne_desc),
    ThemeOption(AppTheme.COASTER, R.string.theme_coaster, R.string.theme_coaster_desc)
)

@Composable
private fun ThemePush(vm: AppViewModel) {
    val c = LocalAppColors.current
    val g = LocalAppGeometry.current
    val settings by vm.settings.collectAsState()
    val active = AppTheme.from(settings.themeId)

    Column(Modifier.riseIn(durationMillis = 400)) {
        Text(
            stringResource(R.string.theme_intro),
            style = text(13.sp), color = c.muted, lineHeight = 20.sp
        )

        Text(
            stringResource(R.string.theme_caption),
            style = text(9.sp, FontWeight.Bold, letterSpacing = 1.1.sp),
            color = c.faint,
            modifier = Modifier.padding(top = 18.dp, bottom = 8.dp)
        )
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            THEME_OPTIONS.forEachIndexed { i, option ->
                val selected = option.theme == active
                // Preview swatches: the theme's own light ground, accent and
                // gold, so each card shows what it is rather than what is on.
                val preview = colorsFor(option.theme, dark = false)
                Row(
                    Modifier
                        .fillMaxWidth()
                        .riseIn(delayMillis = i * 45, durationMillis = 420)
                        .clip(RoundedCornerShape(g.r))
                        .background(c.surface)
                        .border(
                            if (selected) 1.5.dp else 1.dp,
                            if (selected) c.accent else c.line,
                            RoundedCornerShape(g.r)
                        )
                        .pressable(pressedScale = 0.98f) { vm.applyTheme(option.theme.id) }
                        .padding(horizontal = 12.dp, vertical = 11.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        Modifier
                            .size(46.dp)
                            .clip(RoundedCornerShape(10.dp))
                    ) {
                        Box(Modifier.weight(1f).fillMaxHeight().background(preview.bg))
                        Box(Modifier.weight(1f).fillMaxHeight().background(preview.accent))
                        Box(Modifier.weight(1f).fillMaxHeight().background(preview.accent2))
                    }
                    Column(Modifier.weight(1f)) {
                        Text(
                            stringResource(option.nameRes),
                            style = text(14.sp, FontWeight.Bold), color = c.text
                        )
                        Text(
                            stringResource(option.descRes),
                            style = text(11.5.sp), color = c.muted,
                            modifier = Modifier.padding(top = 1.dp)
                        )
                    }
                    Box(
                        Modifier
                            .size(20.dp)
                            .clip(CircleShape)
                            .background(if (selected) c.accent else Color.Transparent)
                            .border(1.5.dp, if (selected) c.accent else c.line, CircleShape),
                        contentAlignment = Alignment.Center
                    ) { if (selected) CheckMark(color = c.onAccent, size = 9.dp) }
                }
            }
        }

        Text(
            stringResource(R.string.theme_scheme_caption),
            style = text(9.sp, FontWeight.Bold, letterSpacing = 1.1.sp),
            color = c.faint,
            modifier = Modifier.padding(top = 22.dp, bottom = 8.dp)
        )
        // Light/dark is a separate axis: every theme is designed in both, so
        // this never disables and never depends on which theme is active.
        Segmented(
            options = listOf(
                stringResource(R.string.theme_scheme_system),
                stringResource(R.string.theme_scheme_light),
                stringResource(R.string.theme_scheme_dark)
            ),
            selectedIndex = when (settings.darkChoice) {
                DarkChoice.SYSTEM -> 0
                DarkChoice.LIGHT -> 1
                DarkChoice.DARK -> 2
            },
            itemPadding = 10.dp,
            fontSize = 13.sp,
            modifier = Modifier.fillMaxWidth(),
            onSelect = { i ->
                vm.updateSettings {
                    it.copy(
                        darkChoice = when (i) {
                            0 -> DarkChoice.SYSTEM
                            1 -> DarkChoice.LIGHT
                            else -> DarkChoice.DARK
                        }
                    )
                }
            }
        )
    }
}

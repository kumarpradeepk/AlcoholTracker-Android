package com.mtss.alcoholtracker.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mtss.alcoholtracker.ui.theme.AppTheme
import com.mtss.alcoholtracker.R
import com.mtss.alcoholtracker.data.DarkChoice
import com.mtss.alcoholtracker.data.DayCutoff
import com.mtss.alcoholtracker.data.Tone
import com.mtss.alcoholtracker.ui.AppViewModel
import com.mtss.alcoholtracker.ui.PushScreen
import com.mtss.alcoholtracker.ui.Sheet
import com.mtss.alcoholtracker.ui.components.AppSwitch
import com.mtss.alcoholtracker.ui.components.CheckMark
import com.mtss.alcoholtracker.ui.components.Chevron
import com.mtss.alcoholtracker.ui.components.ChevronDirection
import com.mtss.alcoholtracker.ui.components.DropletMark
import com.mtss.alcoholtracker.ui.components.Segmented
import com.mtss.alcoholtracker.ui.components.pressable
import com.mtss.alcoholtracker.ui.theme.LocalAppColors
import com.mtss.alcoholtracker.ui.theme.display
import com.mtss.alcoholtracker.ui.theme.text
import com.mtss.alcoholtracker.util.Formatters
import java.text.SimpleDateFormat
import java.util.Date

@Composable
fun SettingsScreen(vm: AppViewModel) {
    val c = LocalAppColors.current
    val settings by vm.settings.collectAsState()
    val reminders by vm.reminders.collectAsState()
    val saved by vm.savedDrinks.collectAsState()
    val logs by vm.logs.collectAsState()
    val dry by vm.dryDays.collectAsState()
    val context = LocalContext.current

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 18.dp)
            .padding(bottom = 150.dp)
    ) {
        Text(
            stringResource(R.string.tab_settings),
            style = display(32.sp, tabular = false, letterSpacing = (-0.5).sp), color = c.text,
            modifier = Modifier.padding(top = 18.dp)
        )

        if (!settings.pro) {
            Row(
                Modifier
                    .padding(top = 14.dp)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(c.surface2)
                    .pressable(pressedScale = 0.98f) { vm.openPaywall() }
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                DropletMark(22.dp, breathing = false)
                Column(Modifier.weight(1f)) {
                    Text(stringResource(R.string.set_pro_banner_title), style = text(14.5.sp, FontWeight.SemiBold), color = c.text)
                    Text(
                        stringResource(R.string.set_pro_banner_sub),
                        style = text(12.5.sp), color = c.muted, modifier = Modifier.padding(top = 1.dp)
                    )
                }
                Chevron(ChevronDirection.RIGHT, c.faint, 12.dp)
            }
        } else {
            Row(
                Modifier
                    .padding(top = 14.dp)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(c.surface2)
                    .padding(horizontal = 16.dp, vertical = 11.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(Modifier.size(8.dp).clip(CircleShape).background(c.b1))
                Text(stringResource(R.string.set_pro_active), style = text(13.5.sp, FontWeight.SemiBold), color = c.b1)
            }
        }

        // YOU
        Section(stringResource(R.string.set_section_you))
        SettingsCard {
            NavRow(stringResource(R.string.set_profile), value = if (settings.weight.isNotBlank())
                stringResource(
                    R.string.set_profile_summary,
                    settings.weight,
                    stringResource(if (settings.weightKgUnit) R.string.profile_weight_kg else R.string.profile_weight_lb),
                    when (settings.sex) {
                        "Female" -> stringResource(R.string.profile_sex_female)
                        "Male" -> stringResource(R.string.profile_sex_male)
                        else -> "—"
                    }
                )
            else stringResource(R.string.set_profile_not_set),
                icon = { IconBox(c.surface2) { Box(Modifier.size(10.dp).clip(CircleShape).background(c.accent)) } }
            ) { vm.openPush(PushScreen.PROFILE) }
            Divider1()
            NavRow(stringResource(R.string.set_units), value = stringResource(
                R.string.set_units_summary,
                stringResource(if (settings.energyKcal) R.string.units_kcal else R.string.units_kj),
                if (settings.servingMl) "ml" else "oz"
            ),
                icon = { IconBox(c.surface2) { Box(Modifier.size(10.dp).clip(RoundedCornerShape(3.dp)).background(c.b1)) } }
            ) { vm.openPush(PushScreen.UNITS) }
            Divider1()
            NavRow(stringResource(R.string.set_guideline), value = stringResource(R.string.set_guideline_value, settings.dailyGoal),
                icon = { IconBox(c.surface2) { DropletMark(10.dp, color = c.b2, breathing = false) } }
            ) { vm.openPush(PushScreen.GUIDE) }
            Divider1()
            ToggleRow(stringResource(R.string.set_ask_cost),
                icon = { IconBox(c.surface2) { Text("$", style = text(13.sp, FontWeight.Bold), color = c.accent) } },
                checked = settings.askCost
            ) { vm.updateSettings { it.copy(askCost = !it.askCost) } }
            Divider1()
            ToggleRow(stringResource(R.string.set_show_calories),
                icon = { IconBox(c.surface2) { RingDot(c.b2) } },
                checked = settings.showCalories
            ) { vm.updateSettings { it.copy(showCalories = !it.showCalories) } }
        }

        // INSIGHTS & SYNC
        Section(stringResource(R.string.set_section_insights))
        SettingsCard {
            Row(
                Modifier.fillMaxWidth().heightIn(min = 52.dp).padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconBox(c.surface2) { CheckMark(color = c.b1, size = 10.dp) }
                Text(stringResource(R.string.set_auto_dry), style = text(15.5.sp), color = c.text, modifier = Modifier.weight(1f))
                if (!settings.pro) SmallProChip()
                AppSwitch(settings.autoDry && settings.pro) {
                    if (settings.pro) vm.updateSettings { it.copy(autoDry = !it.autoDry) }
                    else vm.openPaywall()
                }
            }
            Divider1()
            NavRow(stringResource(R.string.bac_monitor), value = null,
                icon = { IconBox(c.surface2) { Text("%", style = text(12.sp, FontWeight.Bold), color = c.accent) } },
                trailing = { if (!settings.pro) SmallProChip() }
            ) { if (settings.pro) vm.openPush(PushScreen.BAC) else vm.openPaywall() }
            Divider1()
            NavRow(stringResource(R.string.set_notifications),
                value = if (reminders.isNotEmpty())
                    pluralStringResource(R.plurals.set_notif_count, reminders.size, reminders.size)
                else stringResource(R.string.set_notif_none),
                icon = { IconBox(c.surface2) { BellGlyph(c.b2) } }
            ) { vm.openPush(PushScreen.NOTIFS) }
            Divider1()
            NavRow(stringResource(R.string.set_health_sync),
                value = if (settings.healthConnected) stringResource(R.string.set_health_connected)
                else stringResource(R.string.set_health_off),
                icon = { IconBox(c.b3.copy(alpha = 0.14f)) { PlusGlyph(c.b3) } }
            ) { if (settings.pro) vm.openSheet(Sheet.Health) else vm.openPaywall() }
            Divider1()
            NavRow(stringResource(R.string.set_quick_log),
                value = saved.count { it.quickAccess }.let {
                    if (it > 0) pluralStringResource(R.plurals.set_quick_log_count, it, it)
                    else stringResource(R.string.set_quick_log_setup)
                },
                icon = { IconBox(c.surface2) { TileGlyph(c.muted) } }
            ) { if (settings.pro) vm.openPush(PushScreen.QUICKLOG) else vm.openPaywall() }
        }

        // APPEARANCE
        Section(stringResource(R.string.set_section_appearance))
        SettingsCard {
            // Theme and light/dark are one destination now: three complete
             // looks each designed in both schemes, so a single toggle could no
             // longer express the choice.
            NavRow(
                stringResource(R.string.set_theme),
                value = themeSummary(settings.themeId, settings.darkChoice),
                icon = { IconBox(c.surface2) { MoonGlyph(c.muted) } }
            ) { vm.openPush(PushScreen.THEME) }
            Divider1()
            NavRow(stringResource(R.string.set_app_icon), value = listOf(
                stringResource(R.string.set_icon_default),
                stringResource(R.string.set_icon_gift),
                stringResource(R.string.set_icon_holiday)
            ).getOrElse(settings.iconIndex) { stringResource(R.string.set_icon_default) },
                icon = { IconBox(c.surface2) { SquareGlyph(c.accent) } }
            ) { vm.openPush(PushScreen.ICON) }
            Divider1()
            NavRow(stringResource(R.string.set_language), value = stringResource(R.string.set_language_value),
                icon = { IconBox(c.surface2) { GlobeGlyph(c.b1) } }
            ) { vm.hostActions?.openLanguageSettings() }
        }

        // YOUR DATA
        Section(stringResource(R.string.set_section_data))
        Row(
            Modifier
                .padding(top = 8.dp)
                .fillMaxWidth()
                .shadow(if (c.isDark) 0.dp else 5.dp, RoundedCornerShape(20.dp), spotColor = Color.Black.copy(alpha = 0.2f))
                .clip(RoundedCornerShape(20.dp))
                .background(c.surface)
                .padding(horizontal = 16.dp, vertical = 13.dp),
            horizontalArrangement = Arrangement.spacedBy(11.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier.size(24.dp).clip(CircleShape).background(c.surface2),
                contentAlignment = Alignment.Center
            ) { CheckMark(color = c.b1, size = 9.dp) }
            Column {
                Text(
                    if (settings.lastBackupAt > 0) stringResource(R.string.set_backup_title)
                    else stringResource(R.string.set_backup_title_none),
                    style = text(14.5.sp, FontWeight.SemiBold), color = c.text
                )
                Text(
                    if (settings.lastBackupAt > 0)
                        stringResource(
                            R.string.set_backup_detail_dated,
                            Formatters.dateTimeShort(settings.lastBackupAt)
                        )
                    else stringResource(R.string.set_backup_detail_never_android),
                    style = text(12.5.sp), color = c.muted, modifier = Modifier.padding(top = 1.dp)
                )
            }
        }
        SettingsCard(top = 8.dp) {
            NavRow(stringResource(R.string.set_backup_row), value = null,
                icon = { IconBox(c.surface2) { UpGlyph(c.accent) } },
                trailing = { Text(stringResource(R.string.set_backup_free), style = text(12.5.sp, FontWeight.SemiBold), color = c.b1) }
            ) { vm.openPush(PushScreen.BACKUP) }
        }

        // PRIVACY & TONE
        Section(stringResource(R.string.set_section_privacy))
        SettingsCard {
            SubtitledToggleRow(
                stringResource(R.string.set_app_lock), stringResource(R.string.set_app_lock_sub),
                checked = settings.appLock,
                icon = { IconBox(c.surface2) { LockBodyGlyph(c.muted) } }
            ) { vm.updateSettings { it.copy(appLock = !it.appLock) } }
            Divider1()
            SubtitledToggleRow(
                stringResource(R.string.set_discreet), stringResource(R.string.set_discreet_sub),
                checked = settings.discreet,
                icon = { IconBox(c.surface2) { EyeOffGlyph(c.muted) } }
            ) { vm.updateSettings { it.copy(discreet = !it.discreet) } }
            Divider1()
            Row(
                Modifier
                    .fillMaxWidth()
                    .heightIn(min = 56.dp)
                    .pressable(pressedScale = 1f) { vm.updateSettings { it.copy(cutoff = it.cutoff.next()) } }
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconBox(c.surface2) { ClockGlyph(c.muted) }
                Column(Modifier.weight(1f)) {
                    Text(stringResource(R.string.set_day_ends), style = text(15.5.sp), color = c.text)
                    Text(stringResource(R.string.set_day_ends_sub), style = text(12.5.sp), color = c.muted, modifier = Modifier.padding(top = 1.dp))
                }
                Text(cutoffLabel(settings.cutoff), style = text(14.5.sp, FontWeight.SemiBold), color = c.accent)
            }
        }
        SettingsCard(top = 8.dp) {
            Column(Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    IconBox(c.surface2) { Text(stringResource(R.string.set_tone_icon_sample), style = text(11.sp, FontWeight.Bold), color = c.accent) }
                    Text(stringResource(R.string.set_tone), style = text(15.5.sp), color = c.text)
                }
                Segmented(
                    options = Tone.entries.map { toneLabel(it) },
                    selectedIndex = Tone.entries.indexOf(settings.tone),
                    itemPadding = 8.dp,
                    fontSize = 13.sp,
                    modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
                    onSelect = { i -> vm.updateSettings { it.copy(tone = Tone.entries[i]) } }
                )
                Text(
                    when (settings.tone) {
                        Tone.NUMBERS -> stringResource(R.string.set_tone_sub_numbers)
                        Tone.PUSH -> stringResource(R.string.set_tone_sub_push)
                        Tone.NEUTRAL -> stringResource(R.string.set_tone_sub_neutral)
                    },
                    style = text(12.5.sp), color = c.faint, modifier = Modifier.padding(top = 8.dp)
                )
            }
        }

        // SUPPORT
        Section(stringResource(R.string.set_section_support))
        SettingsCard {
            NavRow(stringResource(R.string.set_contact), value = null,
                icon = { IconBox(c.surface2) { MailGlyph(c.accent) } }
            ) { vm.hostActions?.contactSupport(settings.customerId) }
            Divider1()
            NavRow(stringResource(R.string.set_about), value = stringResource(R.string.set_about_version_value),
                icon = { IconBox(c.surface2) { Text("i", style = text(12.sp, FontWeight.SemiBold).copy(fontFamily = androidx.compose.ui.text.font.FontFamily.Serif, fontStyle = androidx.compose.ui.text.font.FontStyle.Italic), color = c.muted) } }
            ) { vm.openPush(PushScreen.ABOUT) }
            Divider1()
            Row(
                Modifier.fillMaxWidth().heightIn(min = 52.dp).padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val customerIdLabel = stringResource(R.string.set_customer_id)
                val copiedMessage = stringResource(R.string.toast_customer_id_copied)
                IconBox(c.surface2) { Text("#", style = text(12.sp, FontWeight.Bold), color = c.muted) }
                Text(customerIdLabel, style = text(15.5.sp), color = c.muted, modifier = Modifier.weight(1f))
                Text(settings.customerId, style = text(13.sp, tabular = true), color = c.faint)
                Text(
                    stringResource(R.string.set_copy), style = text(13.5.sp, FontWeight.SemiBold), color = c.accent,
                    modifier = Modifier
                        .pressable {
                            val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            cm.setPrimaryClip(ClipData.newPlainText(customerIdLabel, settings.customerId))
                            vm.toast(copiedMessage)
                        }
                        .padding(vertical = 6.dp, horizontal = 4.dp)
                )
            }
        }

        Text(
            stringResource(R.string.set_footer),
            style = text(12.sp), color = c.faint, textAlign = TextAlign.Center, lineHeight = 19.sp,
            modifier = Modifier.fillMaxWidth().padding(top = 22.dp)
        )
    }
}

// ── Enum labels ──────────────────────────────────────────────────────────
// The enums in data/SettingsRepository.kt carry English labels; the display
// wording lives in the resource catalogue, resolved by enum value here.

/** "Kiln · System" — the theme name plus which scheme it is being shown in. */
@Composable
private fun themeSummary(themeId: String, darkChoice: DarkChoice): String {
    val theme = stringResource(
        when (AppTheme.from(themeId)) {
            AppTheme.KILN -> R.string.theme_kiln
            AppTheme.NOCTURNE -> R.string.theme_nocturne
            AppTheme.COASTER -> R.string.theme_coaster
        }
    )
    val scheme = stringResource(
        when (darkChoice) {
            DarkChoice.SYSTEM -> R.string.theme_scheme_system
            DarkChoice.LIGHT -> R.string.theme_scheme_light
            DarkChoice.DARK -> R.string.theme_scheme_dark
        }
    )
    return "$theme \u00B7 $scheme"
}

@Composable
private fun toneLabel(tone: Tone): String = when (tone) {
    Tone.NEUTRAL -> stringResource(R.string.set_tone_neutral)
    Tone.PUSH -> stringResource(R.string.set_tone_push)
    Tone.NUMBERS -> stringResource(R.string.set_tone_numbers)
}

@Composable
private fun cutoffLabel(cutoff: DayCutoff): String = when (cutoff) {
    DayCutoff.MIDNIGHT -> stringResource(R.string.set_cutoff_midnight)
    DayCutoff.TWO_AM -> stringResource(R.string.set_cutoff_2am)
    DayCutoff.FOUR_AM -> stringResource(R.string.set_cutoff_4am)
}

// ── Row / card building blocks ───────────────────────────────────────────

@Composable
fun Section(label: String) {
    val c = LocalAppColors.current
    Text(
        label,
        style = text(12.sp, FontWeight.SemiBold, letterSpacing = 0.6.sp), color = c.faint,
        modifier = Modifier.padding(top = 20.dp, start = 4.dp)
    )
}

@Composable
fun SettingsCard(top: androidx.compose.ui.unit.Dp = 8.dp, content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit) {
    val c = LocalAppColors.current
    Column(
        Modifier
            .padding(top = top)
            .fillMaxWidth()
            .shadow(if (c.isDark) 0.dp else 5.dp, RoundedCornerShape(20.dp), spotColor = Color.Black.copy(alpha = 0.2f))
            .clip(RoundedCornerShape(20.dp))
            .background(c.surface),
        content = content
    )
}

@Composable
fun Divider1() {
    val c = LocalAppColors.current
    Box(
        Modifier
            .fillMaxWidth()
            .padding(start = 56.dp)
            .height(0.5.dp)
            .background(c.line)
    )
}

@Composable
fun IconBox(bg: Color, content: @Composable () -> Unit) {
    Box(
        Modifier.size(28.dp).clip(RoundedCornerShape(8.dp)).background(bg),
        contentAlignment = Alignment.Center
    ) { content() }
}

@Composable
fun NavRow(
    title: String,
    value: String?,
    icon: @Composable () -> Unit,
    trailing: (@Composable () -> Unit)? = null,
    onClick: () -> Unit
) {
    val c = LocalAppColors.current
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 52.dp)
            .pressable(pressedScale = 1f, onClick = onClick)
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        icon()
        Text(title, style = text(15.5.sp), color = c.text, modifier = Modifier.weight(1f))
        if (trailing != null) trailing()
        if (value != null) Text(value, style = text(14.sp), color = c.muted)
        Chevron(ChevronDirection.RIGHT, c.faint, 11.dp)
    }
}

@Composable
fun ToggleRow(
    title: String,
    icon: @Composable () -> Unit,
    checked: Boolean,
    onToggle: () -> Unit
) {
    val c = LocalAppColors.current
    Row(
        Modifier.fillMaxWidth().heightIn(min = 52.dp).padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        icon()
        Text(title, style = text(15.5.sp), color = c.text, modifier = Modifier.weight(1f))
        AppSwitch(checked, onToggle)
    }
}

@Composable
fun SubtitledToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    icon: @Composable () -> Unit,
    onToggle: () -> Unit
) {
    val c = LocalAppColors.current
    Row(
        Modifier.fillMaxWidth().heightIn(min = 56.dp).padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        icon()
        Column(Modifier.weight(1f)) {
            Text(title, style = text(15.5.sp), color = c.text)
            Text(subtitle, style = text(12.5.sp), color = c.muted, modifier = Modifier.padding(top = 1.dp))
        }
        AppSwitch(checked, onToggle)
    }
}

@Composable
fun SmallProChip() {
    val c = LocalAppColors.current
    Box(
        Modifier.clip(RoundedCornerShape(7.dp)).background(c.surface2).padding(horizontal = 7.dp, vertical = 3.dp)
    ) {
        Text(stringResource(R.string.badge_pro), style = text(10.sp, FontWeight.Bold, letterSpacing = 0.4.sp), color = c.accent)
    }
}

// ── Tiny glyphs ──────────────────────────────────────────────────────────

@Composable fun RingDot(color: Color) {
    androidx.compose.foundation.Canvas(Modifier.size(11.dp)) {
        drawCircle(
            color,
            radius = size.width / 2 - 1.25.dp.toPx(),
            style = androidx.compose.ui.graphics.drawscope.Stroke(2.5.dp.toPx())
        )
    }
}

@Composable fun BellGlyph(color: Color) {
    androidx.compose.foundation.Canvas(Modifier.size(14.dp)) {
        val w = size.width; val h = size.height
        drawArc(color, 180f, 180f, false,
            topLeft = androidx.compose.ui.geometry.Offset(w * 0.15f, h * 0.08f),
            size = androidx.compose.ui.geometry.Size(w * 0.7f, h * 0.85f),
            style = androidx.compose.ui.graphics.drawscope.Stroke(2.dp.toPx()))
        drawLine(color, androidx.compose.ui.geometry.Offset(0f, h * 0.78f), androidx.compose.ui.geometry.Offset(w, h * 0.78f), 2.dp.toPx(), androidx.compose.ui.graphics.StrokeCap.Round)
    }
}

@Composable fun PlusGlyph(color: Color) {
    androidx.compose.foundation.Canvas(Modifier.size(12.dp)) {
        val w = size.width; val h = size.height
        drawLine(color, androidx.compose.ui.geometry.Offset(0f, h / 2), androidx.compose.ui.geometry.Offset(w, h / 2), 2.5.dp.toPx(), androidx.compose.ui.graphics.StrokeCap.Round)
        drawLine(color, androidx.compose.ui.geometry.Offset(w / 2, 0f), androidx.compose.ui.geometry.Offset(w / 2, h), 2.5.dp.toPx(), androidx.compose.ui.graphics.StrokeCap.Round)
    }
}

@Composable fun TileGlyph(color: Color) {
    androidx.compose.foundation.Canvas(Modifier.size(12.dp, 14.dp)) {
        drawRoundRect(color, style = androidx.compose.ui.graphics.drawscope.Stroke(2.dp.toPx()),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(3.dp.toPx()))
    }
}

@Composable fun MoonGlyph(color: Color) {
    androidx.compose.foundation.Canvas(Modifier.size(13.dp)) {
        val r = size.width / 2
        drawCircle(color, r, style = androidx.compose.ui.graphics.drawscope.Stroke(2.dp.toPx()))
        drawArc(color, -45f, 180f, false,
            topLeft = androidx.compose.ui.geometry.Offset(size.width * 0.2f, size.height * 0.2f),
            size = androidx.compose.ui.geometry.Size(size.width * 0.6f, size.height * 0.6f),
            style = androidx.compose.ui.graphics.drawscope.Stroke(1.5.dp.toPx()))
    }
}

@Composable fun SquareGlyph(color: Color) {
    androidx.compose.foundation.Canvas(Modifier.size(12.dp)) {
        drawRoundRect(color, style = androidx.compose.ui.graphics.drawscope.Stroke(2.dp.toPx()),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(4.dp.toPx()))
    }
}

@Composable fun GlobeGlyph(color: Color) {
    androidx.compose.foundation.Canvas(Modifier.size(12.dp)) {
        drawCircle(color, size.width / 2 - 1.dp.toPx(), style = androidx.compose.ui.graphics.drawscope.Stroke(2.dp.toPx()))
        drawLine(color, androidx.compose.ui.geometry.Offset(size.width / 2, 0f), androidx.compose.ui.geometry.Offset(size.width / 2, size.height), 2.dp.toPx())
    }
}

@Composable fun UpGlyph(color: Color) {
    androidx.compose.foundation.Canvas(Modifier.size(12.dp)) {
        val w = size.width; val h = size.height
        val p = androidx.compose.ui.graphics.Path().apply {
            moveTo(w / 2, h); lineTo(w / 2, h * 0.1f)
            moveTo(w * 0.15f, h * 0.42f); lineTo(w / 2, h * 0.05f); lineTo(w * 0.85f, h * 0.42f)
        }
        drawPath(p, color, style = androidx.compose.ui.graphics.drawscope.Stroke(2.dp.toPx(), cap = androidx.compose.ui.graphics.StrokeCap.Round, join = androidx.compose.ui.graphics.StrokeJoin.Round))
    }
}

@Composable fun LockBodyGlyph(color: Color) {
    androidx.compose.foundation.Canvas(Modifier.size(12.dp, 14.dp)) {
        val w = size.width; val h = size.height
        drawRoundRect(color,
            topLeft = androidx.compose.ui.geometry.Offset(0f, h * 0.4f),
            size = androidx.compose.ui.geometry.Size(w, h * 0.58f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(2.5.dp.toPx()))
        drawArc(color, 180f, 180f, false,
            topLeft = androidx.compose.ui.geometry.Offset(w * 0.18f, 0f),
            size = androidx.compose.ui.geometry.Size(w * 0.64f, h * 0.7f),
            style = androidx.compose.ui.graphics.drawscope.Stroke(2.dp.toPx()))
    }
}

@Composable fun EyeOffGlyph(color: Color) {
    androidx.compose.foundation.Canvas(Modifier.size(13.dp)) {
        val w = size.width; val h = size.height
        drawCircle(color, w * 0.22f, androidx.compose.ui.geometry.Offset(w / 2, h / 2), style = androidx.compose.ui.graphics.drawscope.Stroke(2.dp.toPx()))
        drawLine(color, androidx.compose.ui.geometry.Offset(w * 0.05f, h * 0.9f), androidx.compose.ui.geometry.Offset(w * 0.95f, h * 0.1f), 2.dp.toPx(), androidx.compose.ui.graphics.StrokeCap.Round)
    }
}

@Composable fun ClockGlyph(color: Color) {
    androidx.compose.foundation.Canvas(Modifier.size(13.dp)) {
        val w = size.width; val h = size.height
        drawCircle(color, w / 2 - 1.dp.toPx(), style = androidx.compose.ui.graphics.drawscope.Stroke(2.dp.toPx()))
        drawLine(color, androidx.compose.ui.geometry.Offset(w / 2, h / 2), androidx.compose.ui.geometry.Offset(w / 2, h * 0.22f), 2.dp.toPx(), androidx.compose.ui.graphics.StrokeCap.Round)
    }
}

@Composable fun MailGlyph(color: Color) {
    androidx.compose.foundation.Canvas(Modifier.size(14.dp, 11.dp)) {
        val w = size.width; val h = size.height
        drawRoundRect(color, style = androidx.compose.ui.graphics.drawscope.Stroke(1.8.dp.toPx()),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(2.dp.toPx()))
        val p = androidx.compose.ui.graphics.Path().apply {
            moveTo(w * 0.08f, h * 0.2f); lineTo(w / 2, h * 0.55f); lineTo(w * 0.92f, h * 0.2f)
        }
        drawPath(p, color, style = androidx.compose.ui.graphics.drawscope.Stroke(1.8.dp.toPx(), cap = androidx.compose.ui.graphics.StrokeCap.Round, join = androidx.compose.ui.graphics.StrokeJoin.Round))
    }
}

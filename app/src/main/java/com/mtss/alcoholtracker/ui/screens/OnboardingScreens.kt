package com.mtss.alcoholtracker.ui.screens

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mtss.alcoholtracker.R
import com.mtss.alcoholtracker.ui.AppViewModel
import com.mtss.alcoholtracker.ui.components.CheckMark
import com.mtss.alcoholtracker.ui.components.Chevron
import com.mtss.alcoholtracker.ui.components.ChevronDirection
import com.mtss.alcoholtracker.ui.components.DropletMark
import com.mtss.alcoholtracker.ui.components.PrimaryButton
import com.mtss.alcoholtracker.ui.components.pressable
import com.mtss.alcoholtracker.ui.components.riseIn
import com.mtss.alcoholtracker.ui.theme.LocalAppColors
import com.mtss.alcoholtracker.ui.theme.figure
import com.mtss.alcoholtracker.ui.theme.text

/** Goal rows, in the order the view model indexes them. */
private val GOAL_LABELS = listOf(
    R.string.ob_goal_less,
    R.string.ob_goal_awareness,
    R.string.ob_goal_break,
    R.string.ob_goal_free_days,
    R.string.ob_goal_conscious,
    R.string.ob_goal_reset,
    R.string.ob_goal_social
)

/** Baseline rows, in the order the view model indexes them. */
private val BASELINE_LABELS = listOf(
    R.string.ob_base_0_4,
    R.string.ob_base_5_9,
    R.string.ob_base_10_14,
    R.string.ob_base_15_19,
    R.string.ob_base_20_plus
)

@Composable
fun WelcomeScreen(vm: AppViewModel) {
    val c = LocalAppColors.current
    Column(
        Modifier
            .fillMaxSize()
            .riseIn()
            .padding(horizontal = 28.dp)
            .padding(top = 72.dp, bottom = 40.dp)
    ) {
        Column(
            Modifier.weight(1f).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            DropletMark(54.dp)
            Spacer(Modifier.height(32.dp))
            Text(
                stringResource(R.string.ob_welcome_title),
                style = figure(30.sp, tabular = false),
                color = c.ink,
                textAlign = TextAlign.Center,
                lineHeight = 34.sp
            )
            Text(
                stringResource(R.string.ob_welcome_sub),
                style = text(17.sp), color = c.sub,
                modifier = Modifier.padding(top = 10.dp)
            )
            Text(
                stringResource(R.string.ob_welcome_body),
                style = text(15.sp), color = c.sub, textAlign = TextAlign.Center,
                lineHeight = 22.sp,
                modifier = Modifier.padding(top = 18.dp).widthIn(max = 290.dp)
            )
            // Quick-log preview card
            Row(
                Modifier
                    .padding(top = 34.dp)
                    .riseIn(delayMillis = 250, durationMillis = 600)
                    .shadow(12.dp, RoundedCornerShape(22.dp), spotColor = Color.Black.copy(alpha = 0.25f))
                    .clip(RoundedCornerShape(22.dp))
                    .background(c.card)
                    .padding(14.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    Modifier
                        .size(64.dp, 78.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFF101012))
                        .border(3.dp, Color(0xFF2E2E33), RoundedCornerShape(16.dp)),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(5.dp, Alignment.CenterVertically)
                ) {
                    MiniPill(stringResource(R.string.drink_cat_beer), c.acc, c.acc.copy(alpha = 0.25f))
                    MiniPill(stringResource(R.string.drink_cat_wine), c.moss, c.moss.copy(alpha = 0.22f))
                }
                Column {
                    Text(stringResource(R.string.ob_welcome_quick_caption), style = text(13.sp, FontWeight.SemiBold), color = c.ink)
                    Text(stringResource(R.string.ob_welcome_bac_example), style = text(12.sp), color = c.sub, modifier = Modifier.padding(top = 3.dp))
                    Text(stringResource(R.string.ob_welcome_sober_example), style = text(12.sp), color = c.moss, modifier = Modifier.padding(top = 1.dp))
                }
            }
        }
        Row(
            Modifier.fillMaxWidth().padding(bottom = 18.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally)
        ) {
            Box(Modifier.size(20.dp, 6.dp).clip(RoundedCornerShape(3.dp)).background(c.acc))
            Box(Modifier.size(6.dp).clip(CircleShape).background(c.line))
            Box(Modifier.size(6.dp).clip(CircleShape).background(c.line))
        }
        PrimaryButton(stringResource(R.string.ob_get_started), onClick = { vm.toGoals() })
    }
}

@Composable
private fun MiniPill(label: String, fg: Color, bg: Color) {
    Box(
        Modifier.size(44.dp, 20.dp).clip(RoundedCornerShape(10.dp)).background(bg),
        contentAlignment = Alignment.Center
    ) {
        Text(label, style = text(8.sp, FontWeight.SemiBold), color = fg)
    }
}

@Composable
fun GoalsScreen(vm: AppViewModel) {
    val c = LocalAppColors.current
    val n = vm.obGoals.size
    OnboardingScaffold(
        step = stringResource(R.string.ob_step_indicator, 2),
        title = stringResource(R.string.ob_goals_title),
        subtitle = stringResource(R.string.ob_goals_sub),
        onBack = { vm.backToWelcome() },
        onSkip = { vm.skipOnboarding() },
        cta = if (n > 0) pluralStringResource(R.plurals.ob_goals_cta, n, n)
        else stringResource(R.string.ob_goals_cta_empty),
        ctaEnabled = n > 0,
        onCta = { vm.goalsContinue() }
    ) {
        GOAL_LABELS.forEachIndexed { i, goal ->
            val sel = vm.obGoals.contains(i)
            SelectRow(
                label = stringResource(goal),
                selected = sel,
                delay = i * 38,
                round = false,
                onClick = { vm.toggleGoal(i) }
            )
        }
    }
}

@Composable
fun BaselineScreen(vm: AppViewModel) {
    OnboardingScaffold(
        step = stringResource(R.string.ob_step_indicator, 3),
        title = stringResource(R.string.ob_base_title),
        subtitle = stringResource(R.string.ob_base_sub),
        onBack = { vm.backToGoals() },
        onSkip = { vm.skipOnboarding() },
        cta = stringResource(R.string.ob_continue),
        ctaEnabled = vm.obBase >= 0,
        onCta = { vm.baseContinue() }
    ) {
        BASELINE_LABELS.forEachIndexed { i, label ->
            SelectRow(
                label = stringResource(label),
                selected = vm.obBase == i,
                delay = i * 38,
                round = true,
                onClick = { vm.pickBase(i) }
            )
        }
    }
}

@Composable
private fun OnboardingScaffold(
    step: String,
    title: String,
    subtitle: String,
    onBack: () -> Unit,
    onSkip: () -> Unit,
    cta: String,
    ctaEnabled: Boolean,
    onCta: () -> Unit,
    content: @Composable () -> Unit
) {
    val c = LocalAppColors.current
    Column(
        Modifier
            .fillMaxSize()
            .riseIn(durationMillis = 450)
            .padding(horizontal = 24.dp)
            .padding(top = 48.dp, bottom = 36.dp)
    ) {
        Box(
            Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(c.elev)
                .pressable(pressedScale = 0.9f, onClick = onBack),
            contentAlignment = Alignment.Center
        ) { Chevron(ChevronDirection.LEFT, c.sub, 14.dp) }
        Row(
            Modifier.fillMaxWidth().padding(top = 18.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(step, style = text(12.sp, FontWeight.SemiBold, letterSpacing = 1.sp), color = c.sub)
            Text(
                stringResource(R.string.ob_skip), style = text(14.sp, FontWeight.Medium), color = c.sub,
                modifier = Modifier.pressable(onClick = onSkip).padding(4.dp)
            )
        }
        Text(
            title,
            style = figure(27.sp, tabular = false),
            color = c.ink, lineHeight = 32.sp,
            modifier = Modifier.padding(top = 6.dp)
        )
        Text(
            subtitle, style = text(15.sp), color = c.sub, lineHeight = 21.sp,
            modifier = Modifier.padding(top = 6.dp)
        )
        Column(
            Modifier
                .weight(1f)
                .padding(top = 18.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            content()
            Spacer(Modifier.height(8.dp))
        }
        PrimaryButton(cta, enabled = ctaEnabled, onClick = onCta)
    }
}

@Composable
private fun SelectRow(
    label: String,
    selected: Boolean,
    delay: Int,
    round: Boolean,
    onClick: () -> Unit
) {
    val c = LocalAppColors.current
    Row(
        Modifier
            .fillMaxWidth()
            .riseIn(delayMillis = delay)
            .clip(RoundedCornerShape(18.dp))
            .background(if (selected) c.elev else c.card)
            .border(1.5.dp, if (selected) c.acc else Color.Transparent, RoundedCornerShape(18.dp))
            .pressable(pressedScale = 0.97f, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .size(24.dp)
                .clip(CircleShape)
                .background(if (selected) c.acc else Color.Transparent)
                .border(1.5.dp, if (selected) c.acc else c.line, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            if (selected) {
                if (round) Box(Modifier.size(10.dp).clip(CircleShape).background(Color.White))
                else CheckMark(size = 11.dp)
            }
        }
        Text(label, style = text(16.sp, FontWeight.Medium), color = c.ink)
    }
}

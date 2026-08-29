package com.mtss.alcoholtracker.ui.screens

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.annotation.StringRes
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mtss.alcoholtracker.R
import com.mtss.alcoholtracker.data.Cadence
import com.mtss.alcoholtracker.data.ProPlan
import com.mtss.alcoholtracker.ui.AppViewModel
import com.mtss.alcoholtracker.ui.components.AppCard
import com.mtss.alcoholtracker.ui.components.CheckMark
import com.mtss.alcoholtracker.ui.components.CloseGlyph
import com.mtss.alcoholtracker.ui.components.DropletMark
import com.mtss.alcoholtracker.ui.components.PrimaryButton
import com.mtss.alcoholtracker.ui.components.pressable
import com.mtss.alcoholtracker.ui.components.riseIn
import com.mtss.alcoholtracker.ui.theme.LocalAppColors
import com.mtss.alcoholtracker.ui.theme.text

/** The store whose subscription screen the FAQ points at. Brand name, not copy. */
private const val STORE_NAME = "Play Store"

private val BENEFITS = listOf(
    R.string.pay_benefit_history,
    R.string.pay_benefit_bac,
    R.string.pay_benefit_insights,
    R.string.pay_benefit_streaks,
    R.string.pay_benefit_quicklog,
    R.string.pay_benefit_report
)

/**
 * The free column is deliberately loud. Every row marked free is a promise
 * from the product spec, and seeing it kept is what makes the paid column
 * believable.
 */
private val COMP_ROWS = listOf(
    Triple(R.string.pay_compare_logging, true, true),
    Triple(R.string.pay_compare_export, true, true),
    Triple(R.string.pay_compare_darkmode, true, true),
    Triple(R.string.pay_compare_history, false, true),
    Triple(R.string.pay_compare_bac, false, true),
    Triple(R.string.pay_compare_insights, false, true)
)

@StringRes
private fun Cadence.nameRes(): Int = when (this) {
    Cadence.WEEK -> R.string.pay_plan_weekly
    Cadence.MONTH -> R.string.pay_plan_monthly
    Cadence.YEAR -> R.string.pay_plan_annual
    Cadence.LIFETIME -> R.string.pay_plan_lifetime
}

@StringRes
private fun Cadence.priceLineRes(): Int = when (this) {
    Cadence.WEEK -> R.string.pay_price_line_weekly
    Cadence.MONTH -> R.string.pay_price_line_monthly
    Cadence.YEAR -> R.string.pay_price_line_annual
    Cadence.LIFETIME -> R.string.pay_price_line_lifetime
}

/** Walks up the Compose context to the Activity the store sheet needs. */
private fun Context.findActivity(): Activity? {
    var c: Context? = this
    while (c is ContextWrapper) {
        if (c is Activity) return c
        c = c.baseContext
    }
    return null
}

@Composable
fun PaywallScreen(vm: AppViewModel) {
    val c = LocalAppColors.current
    val store by vm.pro.state.collectAsState()
    val activity = LocalContext.current.findActivity()
    val plans = store.plans
    val selected = plans.getOrNull(vm.planIndex)
    val trialDays = plans.firstOrNull { it.hasTrial }?.freeTrialDays ?: 0

    val faq = buildList {
        if (trialDays > 0) {
            add(
                stringResource(R.string.pay_faq_q_trial) to
                    stringResource(R.string.pay_faq_a_trial, trialDays)
            )
            add(
                stringResource(R.string.pay_faq_q_after) to
                    stringResource(R.string.pay_faq_a_after)
            )
        }
        add(stringResource(R.string.pay_faq_q_logs) to stringResource(R.string.pay_faq_a_logs))
        add(
            stringResource(R.string.pay_faq_q_cancel) to
                stringResource(R.string.pay_faq_a_cancel, STORE_NAME)
        )
    }

    Box(Modifier.fillMaxSize().riseIn()) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 22.dp)
                .padding(top = 56.dp, bottom = 150.dp)
        ) {
            DropletMark(40.dp, breathing = false)
            Spacer(Modifier.height(20.dp))
            Text(
                stringResource(R.string.pay_title),
                style = text(29.sp, FontWeight.Bold, letterSpacing = (-0.5).sp), color = c.ink
            )
            Text(
                stringResource(R.string.pay_sub),
                style = text(15.5.sp), color = c.sec, lineHeight = 22.sp,
                modifier = Modifier.padding(top = 6.dp)
            )
            Row(
                Modifier
                    .padding(top = 12.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(c.mossSoft)
                    .padding(horizontal = 12.dp, vertical = 7.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(Modifier.size(7.dp).clip(CircleShape).background(c.moss))
                Text(
                    stringResource(R.string.pay_no_ads),
                    style = text(12.5.sp, FontWeight.SemiBold), color = c.moss
                )
            }

            Column(
                Modifier.padding(top = 20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                BENEFITS.forEachIndexed { i, b ->
                    Row(
                        Modifier.riseIn(delayMillis = i * 45, durationMillis = 450),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(Modifier.size(7.dp).clip(CircleShape).background(c.tide))
                        Text(stringResource(b), style = text(15.sp), color = c.ink)
                    }
                }
            }

            // FREE / PRO comparison
            AppCard(Modifier.padding(top = 22.dp), radius = 20.dp, padding = 0.dp) {
                Column(Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
                    Row(Modifier.padding(top = 10.dp, bottom = 6.dp)) {
                        Text(
                            stringResource(R.string.pay_compare_caption),
                            style = text(11.sp, FontWeight.SemiBold, letterSpacing = 0.6.sp),
                            color = c.ter, modifier = Modifier.weight(1f)
                        )
                        Text(
                            stringResource(R.string.pay_compare_free),
                            style = text(11.sp, FontWeight.SemiBold, letterSpacing = 0.6.sp),
                            color = c.ter, textAlign = TextAlign.Center,
                            modifier = Modifier.width(44.dp)
                        )
                        Text(
                            stringResource(R.string.pay_compare_pro),
                            style = text(11.sp, FontWeight.SemiBold, letterSpacing = 0.6.sp),
                            color = c.ter, textAlign = TextAlign.Center,
                            modifier = Modifier.width(44.dp)
                        )
                    }
                    COMP_ROWS.forEach { (label, free, pro) ->
                        Column {
                            Box(Modifier.fillMaxWidth().height(0.5.dp).background(c.hair))
                            Row(
                                Modifier.padding(vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    stringResource(label), style = text(14.sp), color = c.ink,
                                    modifier = Modifier.weight(1f)
                                )
                                Box(Modifier.width(44.dp), contentAlignment = Alignment.Center) {
                                    if (free) CheckMark(color = c.moss, size = 12.dp)
                                    else Text("—", color = c.ter)
                                }
                                Box(Modifier.width(44.dp), contentAlignment = Alignment.Center) {
                                    if (pro) CheckMark(color = c.tide, size = 12.dp)
                                    else Text("—", color = c.ter)
                                }
                            }
                        }
                    }
                }
            }

            // Plans, exactly as the store priced them today.
            when {
                store.loading && plans.isEmpty() ->
                    Text(
                        stringResource(R.string.pay_loading),
                        style = text(14.sp), color = c.ter,
                        modifier = Modifier.padding(top = 24.dp)
                    )

                plans.isEmpty() ->
                    Column(Modifier.padding(top = 24.dp)) {
                        Text(
                            stringResource(R.string.pay_unavailable),
                            style = text(14.sp), color = c.sec, lineHeight = 20.sp
                        )
                        Text(
                            stringResource(R.string.pay_retry),
                            style = text(14.sp, FontWeight.SemiBold), color = c.tide,
                            modifier = Modifier.padding(top = 10.dp).pressable { vm.pro.refresh() }
                        )
                    }

                else ->
                    Column(
                        Modifier.padding(top = 20.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        plans.forEachIndexed { i, plan ->
                            PlanRow(plan, selected = vm.planIndex == i) { vm.planIndex = i }
                        }
                    }
            }

            // FAQ
            Text(
                stringResource(R.string.pay_faq_caption),
                style = text(13.sp, FontWeight.SemiBold, letterSpacing = 0.6.sp), color = c.ter,
                modifier = Modifier.padding(top = 22.dp)
            )
            AppCard(Modifier.padding(top = 8.dp), radius = 20.dp, padding = 0.dp) {
                Column(Modifier.padding(horizontal = 16.dp, vertical = 2.dp)) {
                    faq.forEachIndexed { i, (q, a) ->
                        val open = vm.faqOpen == i
                        val rot by animateFloatAsState(if (open) 45f else 0f, tween(350), label = "faqRot")
                        Column(
                            Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp)
                                .animateContentSize(tween(400))
                        ) {
                            if (i > 0) Box(Modifier.fillMaxWidth().height(0.5.dp).background(c.hair))
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .padding(top = if (i > 0) 12.dp else 0.dp)
                                    .pressable(pressedScale = 1f) { vm.faqOpen = if (open) -1 else i },
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    q, style = text(14.5.sp, FontWeight.Medium), color = c.ink,
                                    modifier = Modifier.weight(1f)
                                )
                                Box(Modifier.size(16.dp).rotate(rot), contentAlignment = Alignment.Center) {
                                    Text("+", style = text(18.sp), color = c.ter)
                                }
                            }
                            if (open) {
                                Text(
                                    a, style = text(13.5.sp), color = c.sec, lineHeight = 20.sp,
                                    modifier = Modifier.padding(top = 8.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Close button
        Box(
            Modifier
                .align(Alignment.TopEnd)
                .padding(top = 48.dp, end = 18.dp)
                .size(34.dp)
                .clip(CircleShape)
                .background(c.card2)
                .pressable(pressedScale = 0.9f) { vm.closePaywall() },
            contentAlignment = Alignment.Center
        ) { CloseGlyph(c.sec) }

        // Bottom CTA
        Column(
            Modifier
                .align(Alignment.BottomCenter)
                .background(Brush.verticalGradient(0f to c.bg.copy(alpha = 0f), 0.26f to c.bg))
                .padding(horizontal = 22.dp)
                .padding(top = 10.dp, bottom = 24.dp)
        ) {
            PrimaryButton(
                ctaLabel(selected),
                enabled = selected != null && !store.purchaseInFlight,
                onClick = { vm.buy(activity) }
            )
            reassurance(selected)?.let { line ->
                Text(
                    line, style = text(12.5.sp), color = c.sec, textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(top = 9.dp)
                )
            }
            Row(
                Modifier.fillMaxWidth().padding(top = 11.dp),
                horizontalArrangement = Arrangement.spacedBy(24.dp, Alignment.CenterHorizontally)
            ) {
                Text(
                    stringResource(R.string.pay_restore), style = text(13.sp), color = c.ter,
                    modifier = Modifier.pressable { vm.restorePurchases() }
                )
                Text(
                    stringResource(R.string.pay_terms), style = text(13.sp), color = c.ter,
                    modifier = Modifier.pressable {
                        vm.hostActions?.openUrl("https://alcoholtracker.app/terms")
                    }
                )
                Text(
                    stringResource(R.string.pay_privacy), style = text(13.sp), color = c.ter,
                    modifier = Modifier.pressable {
                        vm.hostActions?.openUrl("https://alcoholtracker.app/privacy")
                    }
                )
            }
        }
    }
}

/**
 * A plan card.
 *
 * At most one badge: a trial beats a saving, because "7 days free" is the
 * thing that actually decides it and two badges on one row read as noise.
 */
@Composable
private fun PlanRow(plan: ProPlan, selected: Boolean, onClick: () -> Unit) {
    val c = LocalAppColors.current
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(if (selected) c.tideSoft else c.card)
            .border(1.5.dp, if (selected) c.tide else Color.Transparent, RoundedCornerShape(20.dp))
            .pressable(pressedScale = 0.98f, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 15.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .size(22.dp)
                .clip(CircleShape)
                .background(if (selected) c.tide else Color.Transparent)
                .border(1.5.dp, if (selected) c.tide else c.hair, CircleShape),
            contentAlignment = Alignment.Center
        ) { if (selected) CheckMark(size = 10.dp) }
        Column(Modifier.weight(1f)) {
            Text(
                stringResource(plan.cadence.nameRes()),
                style = text(16.sp, FontWeight.SemiBold), color = c.ink
            )
            Text(
                stringResource(plan.cadence.priceLineRes(), plan.price),
                style = text(13.sp), color = c.sec, modifier = Modifier.padding(top = 1.dp)
            )
        }
        when {
            plan.hasTrial -> Badge(stringResource(R.string.pay_badge_trial, plan.freeTrialDays), good = true)
            plan.cadence == Cadence.LIFETIME -> Badge(stringResource(R.string.pay_badge_best), good = false)
            plan.savingPercent >= 10 -> Badge(stringResource(R.string.pay_badge_save, plan.savingPercent), good = false)
        }
    }
}

@Composable
private fun Badge(label: String, good: Boolean) {
    val c = LocalAppColors.current
    Box(
        Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (good) c.mossSoft else c.amberSoft)
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(
            label,
            style = text(10.sp, FontWeight.Bold, letterSpacing = 0.5.sp),
            color = if (good) c.moss else c.amber
        )
    }
}

/** The button. Leads with free when free is on the table, price otherwise. */
@Composable
private fun ctaLabel(plan: ProPlan?): String = when {
    plan == null -> stringResource(R.string.pay_cta_default)
    plan.hasTrial -> stringResource(R.string.pay_cta_trial, plan.freeTrialDays)
    plan.cadence == Cadence.LIFETIME -> stringResource(R.string.pay_cta_lifetime)
    else -> stringResource(R.string.pay_cta_plan, plan.price)
}

/** The line under the button: what happens next, in plain words, always. */
@Composable
private fun reassurance(plan: ProPlan?): String? = when {
    plan == null -> null
    plan.hasTrial -> stringResource(R.string.pay_sub_trial, plan.freeTrialDays, plan.price)
    plan.cadence == Cadence.LIFETIME -> stringResource(R.string.pay_sub_lifetime)
    else -> stringResource(R.string.pay_sub_recurring, plan.price)
}

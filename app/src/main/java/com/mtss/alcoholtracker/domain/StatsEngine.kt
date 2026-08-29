package com.mtss.alcoholtracker.domain

import android.content.Context
import androidx.annotation.StringRes
import com.mtss.alcoholtracker.R
import com.mtss.alcoholtracker.data.DrinkLog
import com.mtss.alcoholtracker.data.Tone
import com.mtss.alcoholtracker.util.Formatters
import com.mtss.alcoholtracker.util.LocaleText
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

enum class StatsPeriod(@StringRes val labelRes: Int, val locked: Boolean) {
    D7(R.string.stats_period_7d, false),
    D30(R.string.stats_period_30d, true),
    D90(R.string.stats_period_90d, true),
    Y1(R.string.stats_period_1y, true),
    CUSTOM(R.string.stats_period_custom, true)
}

data class Bucket(val label: String, val units: Double, val spend: Double, val overDaily: Boolean)

data class BreakdownRow(val name: String, val pours: Int, val pct: Int)

data class StatsRange(
    val label: String,
    val fromDay: Long,
    val toDay: Long,
    val days: Int,
    val buckets: List<Bucket>,
    val totalMl: Double,
    val totalUnits: Double,
    val totalSpend: Double,
    val totalKcal: Int,
    val prevUnits: Double,
    val breakdown: List<BreakdownRow>,
    val dryCount: Int,
    val dryPct: Int,
    val logsInRange: List<DrinkLog>
)

/**
 * Everything Statistics shows is derived here from the raw log list, so the
 * roll-ups always equal the rows beneath them.
 */
object StatsEngine {

    private fun monthShort() = DateTimeFormatter.ofPattern("LLL", Formatters.locale())

    fun rangeFor(
        period: StatsPeriod,
        pageBack: Int,
        todayKey: Long,
        logs: List<DrinkLog>,
        dryDays: Set<Long>,
        dailyGoal: Int,
        customFrom: Long?,
        customTo: Long?
    ): StatsRange {
        val (from, to) = when (period) {
            StatsPeriod.D7 -> {
                val t = todayKey - 7L * pageBack
                (t - 6) to t
            }
            StatsPeriod.D30 -> {
                val t = todayKey - 30L * pageBack
                (t - 29) to t
            }
            StatsPeriod.D90 -> {
                val t = todayKey - 90L * pageBack
                (t - 89) to t
            }
            StatsPeriod.Y1 -> {
                val t = todayKey - 365L * pageBack
                (t - 364) to t
            }
            StatsPeriod.CUSTOM -> {
                val f = min(customFrom ?: (todayKey - 13), todayKey)
                val t = min(customTo ?: todayKey, todayKey)
                min(f, t) to max(f, t)
            }
        }

        val byDay = logs.groupBy { it.epochDay }
        fun dayUnits(d: Long) = (byDay[d] ?: emptyList()).sumOf { AlcoholMath.units(it.ml, it.abv) }
        fun daySpend(d: Long) = (byDay[d] ?: emptyList()).sumOf { it.cost }

        val buckets = mutableListOf<Bucket>()
        when (period) {
            StatsPeriod.D7 -> {
                for (d in from..to) {
                    val date = LocalDate.ofEpochDay(d)
                    // Two characters, from the platform locale: German Mo/Mi and
                    // Di/Do both collapse to one letter, and Thai พฤ cannot shorten
                    // below two at all (punch-list B4).
                    val dow = date.dayOfWeek
                        .getDisplayName(TextStyle.SHORT, Formatters.locale())
                        .trimEnd('.')
                        .take(2)
                    buckets += Bucket(dow, dayUnits(d), daySpend(d), dayUnits(d) > dailyGoal)
                }
            }
            StatsPeriod.D30 -> {
                for (d in from..to) {
                    val date = LocalDate.ofEpochDay(d)
                    val l = if (date.dayOfMonth == 1 || date.dayOfMonth % 10 == 0) date.dayOfMonth.toString() else ""
                    buckets += Bucket(l, dayUnits(d), daySpend(d), dayUnits(d) > dailyGoal)
                }
            }
            StatsPeriod.D90 -> {
                for (w in 0 until 13) {
                    val a = from + w * 7L
                    val b = min(to, a + 6)
                    var u = 0.0; var sp = 0.0; var over = false
                    for (d in a..b) { u += dayUnits(d); sp += daySpend(d); if (dayUnits(d) > dailyGoal) over = true }
                    val l = if (w % 4 == 0) LocalDate.ofEpochDay(a).format(monthShort()) else ""
                    buckets += Bucket(l, u, sp, over)
                }
            }
            StatsPeriod.Y1 -> {
                for (m in 0 until 12) {
                    val a = from + m * 30L
                    val b = min(to, a + 29)
                    var u = 0.0; var sp = 0.0; var over = false
                    for (d in a..b) { u += dayUnits(d); sp += daySpend(d); if (dayUnits(d) > dailyGoal) over = true }
                    buckets += Bucket(LocalDate.ofEpochDay(b).format(monthShort()).take(1), u, sp, over)
                }
            }
            StatsPeriod.CUSTOM -> {
                val span = (to - from + 1).toInt()
                if (span <= 31) {
                    for (d in from..to) buckets += Bucket("", dayUnits(d), daySpend(d), dayUnits(d) > dailyGoal)
                } else {
                    val weeks = ceil(span / 7.0).toInt()
                    for (w in 0 until weeks) {
                        val a = from + w * 7L
                        val b = min(to, a + 6)
                        var u = 0.0; var sp = 0.0; var over = false
                        for (d in a..b) { u += dayUnits(d); sp += daySpend(d); if (dayUnits(d) > dailyGoal) over = true }
                        buckets += Bucket("", u, sp, over)
                    }
                }
            }
        }

        val inRange = logs.filter { it.epochDay in from..to }.sortedBy { it.atMillis }
        val totalMl = inRange.sumOf { it.ml }
        val totalUnits = inRange.sumOf { AlcoholMath.units(it.ml, it.abv) }
        val totalSpend = inRange.sumOf { it.cost }
        val totalKcal = inRange.sumOf { it.kcal }

        val span = to - from + 1
        val prevLogs = logs.filter { it.epochDay in (from - span) until from }
        val prevUnits = prevLogs.sumOf { AlcoholMath.units(it.ml, it.abv) }

        val byName = inRange.groupBy { it.name }
        val totU = if (totalUnits == 0.0) 1.0 else totalUnits
        val breakdown = byName.entries
            .map { (n, l) -> Triple(n, l.sumOf { AlcoholMath.units(it.ml, it.abv) }, l.size) }
            .sortedByDescending { it.second }
            .take(5)
            .map { (n, u, c) -> BreakdownRow(n, c, (u / totU * 100).roundToInt()) }

        val dryCount = (from..to).count { dryDays.contains(it) }
        val days = span.toInt()

        val label = Formatters.shortDate(from) + " – " + Formatters.shortDate(to)

        return StatsRange(
            label = label, fromDay = from, toDay = to, days = days, buckets = buckets,
            totalMl = totalMl, totalUnits = totalUnits, totalSpend = totalSpend, totalKcal = totalKcal,
            prevUnits = prevUnits, breakdown = breakdown,
            dryCount = dryCount, dryPct = (dryCount.toDouble() / days * 100).roundToInt(),
            logsInRange = inRange
        )
    }

    /**
     * The plain-language verdict line, tone-aware. Empty string = hidden.
     * The Numbers-tone rows carry the unit noun, which is country config and not
     * a translatable word, so the caller supplies it.
     */
    fun verdict(
        context: Context,
        range: StatsRange,
        tone: Tone,
        noun: UnitNoun
    ): Pair<String, Boolean> {
        // second = "down" (render in moss)
        if (range.prevUnits > 0) {
            val dd = range.totalUnits - range.prevUnits
            val pct = (abs(dd) / range.prevUnits * 100).roundToInt()
            return when {
                tone == Tone.NUMBERS -> context.getString(
                    R.string.stats_verdict_numbers,
                    // da-DK gives the noun its own slot on each side of the
                    // separator, so it is passed twice; the packs that reuse
                    // %3$s simply ignore the fourth argument.
                    range.totalUnits, range.prevUnits, noun.abbrev, noun.abbrev
                ) to false
                pct == 0 -> context.getString(R.string.stats_verdict_level_neutral) to false
                dd < 0 -> context.getString(R.string.stats_verdict_less_neutral, pct) to true
                else -> context.getString(R.string.stats_verdict_more_neutral, pct) to false
            }
        }
        if (range.totalUnits > 0) {
            return if (tone == Tone.NUMBERS)
                context.getString(
                    R.string.stats_verdict_first_numbers, range.totalUnits, noun.abbrev
                ) to false
            else context.getString(R.string.stats_verdict_first_neutral) to false
        }
        return "" to false
    }

    /**
     * Money saved. The amount is rendered by the currency formatter, so the
     * string takes a pre-formatted `%1$s` and never a glued `$` (punch-list A3).
     */
    fun savedLine(context: Context, baselineWeekly: Double, range: StatsRange): String {
        if (baselineWeekly <= 0.0) return context.getString(R.string.stats_saved_unset)
        val saved = baselineWeekly * (range.days / 7.0) - range.totalSpend
        return if (saved >= 0)
            context.getString(R.string.stats_saved_under, Formatters.moneyWhole(saved))
        else
            context.getString(R.string.stats_saved_over, Formatters.moneyWhole(-saved))
    }
}

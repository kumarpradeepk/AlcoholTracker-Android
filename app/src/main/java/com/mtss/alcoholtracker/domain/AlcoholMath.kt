package com.mtss.alcoholtracker.domain

import com.mtss.alcoholtracker.data.DayCutoff
import com.mtss.alcoholtracker.data.DrinkLog
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * All alcohol arithmetic in one place. One formula, documented, recomputed
 * from entries — never cached partial sums that can drift from the rows.
 *
 * grams ethanol = ml × (abv/100) × 0.789   (0.789 g/ml, density of ethanol)
 * 1 standard drink = the *country's* definition, not a constant. It was
 * hardcoded at the WHO 10 g, which under-reports a British reader by ~20%, a
 * Danish reader by ~20% and a Japanese reader by about half. See [UnitsConfig].
 */
object AlcoholMath {

    const val ETHANOL_DENSITY = 0.789
    const val KCAL_PER_GRAM = 7.0
    const val ELIMINATION_PER_HOUR = 0.015   // % BAC cleared per hour
    const val KCAL_PER_CHEESEBURGER = 303

    /** US fluid ounce. */
    const val ML_PER_US_FL_OZ = 29.5735

    /** Imperial fluid ounce — 4% smaller, and the one a UK reader means. */
    const val ML_PER_IMPERIAL_FL_OZ = 28.4131

    /**
     * Grams of ethanol per standard drink for the active country. Bound once at
     * process start and again on a configuration change, so the ordinary
     * [units] call sites do not each have to carry a Context.
     */
    @Volatile
    var gramsPerUnit: Double = UnitCountry.WHO.gramsPerStandardDrink
        private set

    /** True where a fluid ounce means the imperial one (28.4131 ml). */
    @Volatile
    var imperialFluidOunce: Boolean = false
        private set

    fun bind(country: UnitCountry) {
        gramsPerUnit = country.gramsPerStandardDrink
        imperialFluidOunce = country == UnitCountry.GB
    }

    fun grams(ml: Double, abv: Double): Double = ml * (abv / 100.0) * ETHANOL_DENSITY

    fun units(ml: Double, abv: Double): Double = grams(ml, abv) / gramsPerUnit

    fun units(ml: Double, abv: Double, gramsPerStandardDrink: Double): Double =
        grams(ml, abv) / gramsPerStandardDrink

    fun kcal(ml: Double, abv: Double): Int = (grams(ml, abv) * KCAL_PER_GRAM).roundToInt()

    /** Millilitres to fluid ounces, in whichever fluid ounce the market uses. */
    fun mlToOz(ml: Double): Double =
        ml / (if (imperialFluidOunce) ML_PER_IMPERIAL_FL_OZ else ML_PER_US_FL_OZ)

    fun trimAbv(v: Double): String =
        if (v == v.toLong().toDouble()) v.toLong().toString() else v.toString()

    // ── Drinking-day boundaries ──────────────────────────────────────────

    /** The drinking day an instant belongs to, honouring the day cut-off. */
    fun drinkingDay(atMillis: Long, cutoff: DayCutoff, zone: ZoneId = ZoneId.systemDefault()): LocalDate {
        val dt = LocalDateTime.ofInstant(Instant.ofEpochMilli(atMillis), zone)
        return if (dt.hour < cutoff.hour) dt.toLocalDate().minusDays(1) else dt.toLocalDate()
    }

    fun todayKey(cutoff: DayCutoff): Long =
        drinkingDay(System.currentTimeMillis(), cutoff).toEpochDay()

    // ── BAC (Widmark) ────────────────────────────────────────────────────

    enum class BacStatus { CLEAR, RISING, SETTLING }

    data class BacEstimate(val percent: Double, val hoursToZero: Double, val status: BacStatus)

    /**
     * Conservative Widmark estimate over today's logs. Informational only —
     * the UI must always pair this with the do-not-drive disclaimer.
     */
    fun bac(
        todayLogs: List<DrinkLog>,
        weightKg: Double,
        sex: String?,
        nowMillis: Long = System.currentTimeMillis()
    ): BacEstimate? {
        if (weightKg <= 0.0 || sex == null) return null
        if (todayLogs.isEmpty()) return BacEstimate(0.0, 0.0, BacStatus.CLEAR)
        val gramsTotal = todayLogs.sumOf { grams(it.ml, it.abv) }
        val first = todayLogs.minOf { it.atMillis }
        val last = todayLogs.maxOf { it.atMillis }
        val r = if (sex == "Female") 0.55 else 0.68
        val hoursSinceFirst = max(0.0, (nowMillis - first) / 3_600_000.0)
        val raw = gramsTotal / (weightKg * 1000.0 * r) * 100.0 - ELIMINATION_PER_HOUR * hoursSinceFirst
        val v = max(0.0, raw)
        val minutesSinceLast = (nowMillis - last) / 60_000.0
        val status = when {
            v <= 0.002 -> BacStatus.CLEAR
            minutesSinceLast < 40 -> BacStatus.RISING
            else -> BacStatus.SETTLING
        }
        return BacEstimate(v, v / ELIMINATION_PER_HOUR, status)
    }

    fun formatHours(hours: Double): String {
        if (hours <= 0) return ""
        val h = hours.toInt()
        val m = ((hours - h) * 60).roundToInt()
        return if (h > 0) "${h}h ${m}m" else "${m}m"
    }
}

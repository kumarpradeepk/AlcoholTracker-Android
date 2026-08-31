package com.mtss.alcoholtracker.wear.sync

/** One of the phone's "The usual?" tiles, as it crosses the bridge. */
data class QuickDrink(
    val name: String,
    val ml: Double,
    val abv: Double,
    val cost: Double
)

/**
 * Everything the watch needs to draw itself, as sent by the phone.
 *
 * The watch is deliberately a *view* plus a write path: it never recomputes
 * units from its own rules, because the standard-drink definition is the
 * phone's country setting and two sources of that number would eventually
 * disagree. It receives derived figures and sends back raw pours.
 */
data class WearSnapshot(
    val dayUnits: Double = 0.0,
    val dailyGoal: Int = 2,
    val weekUnits: Double = 0.0,
    val weeklyGoal: Int = 10,
    /** Grams of ethanol per standard drink, so a custom pour can be previewed. */
    val gramsPerUnit: Double = 14.0,
    val unitNoun: String = "standard drinks",
    val dayLabel: String = "",
    val remainLine: String = "",
    val weekLine: String = "",
    val isDryToday: Boolean = false,
    val dayHasDrinks: Boolean = false,
    val quick: List<QuickDrink> = emptyList(),
    val pro: Boolean = false,
    val bacOn: Boolean = false,
    val bacValue: String = "",
    val bacStatus: String = "",
    /** 0 clear, 1 rising, 2 settling — kept numeric so the watch owns the colour. */
    val bacBand: Int = 0
) {
    val ratio: Double get() = if (dailyGoal <= 0) 0.0 else dayUnits / dailyGoal
}

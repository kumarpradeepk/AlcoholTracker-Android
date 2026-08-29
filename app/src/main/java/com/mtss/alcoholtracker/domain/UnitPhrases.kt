package com.mtss.alcoholtracker.domain

import com.mtss.alcoholtracker.R

/**
 * GENERATED FILE - do not edit by hand.
 * Regenerate: python Localization/tools/gen_android.py
 *
 * The whole-phrase escape hatch (punch-list A2/A2a/A2c/A2d). Gendered languages,
 * classifier languages and Korean particle alternation all break slot-for-slot
 * substitution of the unit noun, so the packs give a complete sentence per
 * country for the rows that cannot be parameterised. Every entry here resolves a
 * (base resource, country) pair to the country's own string; call sites that pass
 * a noun argument stay correct either way, because an unused format argument is
 * ignored.
 */
object UnitPhrases {

    private val strings: Map<Int, Map<String, Int>> = mapOf(
        R.string.a11y_units_info to mapOf(
            "us" to R.string.a11y_units_info__us, "gb" to R.string.a11y_units_info__gb, "au" to R.string.a11y_units_info__au, "ca" to R.string.a11y_units_info__ca, "jp" to R.string.a11y_units_info__jp, "dk" to R.string.a11y_units_info__dk, "vn" to R.string.a11y_units_info__vn, "who" to R.string.a11y_units_info__who
        ),
        R.string.guideline_intro to mapOf(
            "us" to R.string.guideline_intro__us, "gb" to R.string.guideline_intro__gb, "au" to R.string.guideline_intro__au, "ca" to R.string.guideline_intro__ca, "jp" to R.string.guideline_intro__jp, "dk" to R.string.guideline_intro__dk, "vn" to R.string.guideline_intro__vn, "who" to R.string.guideline_intro__who
        ),
        R.string.sheet_units_chip_result to mapOf(
            "us" to R.string.sheet_units_chip_result__us, "gb" to R.string.sheet_units_chip_result__gb, "au" to R.string.sheet_units_chip_result__au, "ca" to R.string.sheet_units_chip_result__ca, "jp" to R.string.sheet_units_chip_result__jp, "dk" to R.string.sheet_units_chip_result__dk, "vn" to R.string.sheet_units_chip_result__vn, "who" to R.string.sheet_units_chip_result__who
        ),
        R.string.sheet_units_targets to mapOf(
            "us" to R.string.sheet_units_targets__us, "gb" to R.string.sheet_units_targets__gb, "au" to R.string.sheet_units_targets__au, "ca" to R.string.sheet_units_targets__ca, "jp" to R.string.sheet_units_targets__jp, "dk" to R.string.sheet_units_targets__dk, "vn" to R.string.sheet_units_targets__vn, "who" to R.string.sheet_units_targets__who
        ),
        R.string.sheet_units_title to mapOf(
            "us" to R.string.sheet_units_title__us, "gb" to R.string.sheet_units_title__gb, "au" to R.string.sheet_units_title__au, "ca" to R.string.sheet_units_title__ca, "jp" to R.string.sheet_units_title__jp, "dk" to R.string.sheet_units_title__dk, "vn" to R.string.sheet_units_title__vn, "who" to R.string.sheet_units_title__who
        ),
        R.string.stats_units_chart_title to mapOf(
            "us" to R.string.stats_units_chart_title__us, "gb" to R.string.stats_units_chart_title__gb, "au" to R.string.stats_units_chart_title__au, "ca" to R.string.stats_units_chart_title__ca, "jp" to R.string.stats_units_chart_title__jp, "dk" to R.string.stats_units_chart_title__dk, "vn" to R.string.stats_units_chart_title__vn, "who" to R.string.stats_units_chart_title__who
        ),
    )

    private val plurals: Map<Int, Map<String, Int>> = mapOf(
        R.plurals.diary_ring_of_goal to mapOf(
            "us" to R.plurals.diary_ring_of_goal__us, "gb" to R.plurals.diary_ring_of_goal__gb, "au" to R.plurals.diary_ring_of_goal__au, "ca" to R.plurals.diary_ring_of_goal__ca, "jp" to R.plurals.diary_ring_of_goal__jp, "dk" to R.plurals.diary_ring_of_goal__dk, "vn" to R.plurals.diary_ring_of_goal__vn, "who" to R.plurals.diary_ring_of_goal__who
        ),
        R.plurals.guideline_monthly_note to mapOf(
            "us" to R.plurals.guideline_monthly_note__us, "gb" to R.plurals.guideline_monthly_note__gb, "au" to R.plurals.guideline_monthly_note__au, "ca" to R.plurals.guideline_monthly_note__ca, "jp" to R.plurals.guideline_monthly_note__jp, "dk" to R.plurals.guideline_monthly_note__dk, "vn" to R.plurals.guideline_monthly_note__vn, "who" to R.plurals.guideline_monthly_note__who
        ),
        R.plurals.guideline_target_units to mapOf(
            "us" to R.plurals.guideline_target_units__us, "gb" to R.plurals.guideline_target_units__gb, "au" to R.plurals.guideline_target_units__au, "ca" to R.plurals.guideline_target_units__ca, "jp" to R.plurals.guideline_target_units__jp, "dk" to R.plurals.guideline_target_units__dk, "vn" to R.plurals.guideline_target_units__vn, "who" to R.plurals.guideline_target_units__who
        ),
        R.plurals.stat_label_units to mapOf(
            "us" to R.plurals.stat_label_units__us, "gb" to R.plurals.stat_label_units__gb, "au" to R.plurals.stat_label_units__au, "ca" to R.plurals.stat_label_units__ca, "jp" to R.plurals.stat_label_units__jp, "dk" to R.plurals.stat_label_units__dk, "vn" to R.plurals.stat_label_units__vn, "who" to R.plurals.stat_label_units__who
        ),
        R.plurals.stats_unit_units to mapOf(
            "us" to R.plurals.stats_unit_units__us, "gb" to R.plurals.stats_unit_units__gb, "au" to R.plurals.stats_unit_units__au, "ca" to R.plurals.stats_unit_units__ca, "jp" to R.plurals.stats_unit_units__jp, "dk" to R.plurals.stats_unit_units__dk, "vn" to R.plurals.stats_unit_units__vn, "who" to R.plurals.stats_unit_units__who
        ),
    )

    /** The country's own wording for [base], or [base] itself when it has none. */
    fun string(base: Int, country: UnitCountry): Int =
        strings[base]?.get(country.phraseKey) ?: base

    /** The country's own plural container for [base], or [base] itself. */
    fun plural(base: Int, country: UnitCountry): Int =
        plurals[base]?.get(country.phraseKey) ?: base
}

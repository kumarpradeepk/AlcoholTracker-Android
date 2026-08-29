package com.mtss.alcoholtracker.util

import android.content.Context
import android.os.Build
import androidx.annotation.StringRes
import java.time.DayOfWeek
import java.time.format.TextStyle
import java.time.temporal.WeekFields
import java.util.Locale

/**
 * Locale-sensitive text operations the platform will get wrong if you let it.
 */
object LocaleText {

    fun primaryLocale(context: Context): Locale =
        if (Build.VERSION.SDK_INT >= 24) {
            context.resources.configuration.locales[0]
        } else {
            @Suppress("DEPRECATION")
            context.resources.configuration.locale
        }

    /**
     * Uppercasing, done in the locale's own rules.
     *
     * Turkish has a dotted and a dotless i. A default `uppercase()` runs the
     * root locale's rules and turns *İstatistik* into *ISTATISTIK*, which is a
     * misspelling, not a style choice. Azerbaijani has the same pair.
     * (punch-list A5b)
     */
    fun upper(value: String, locale: Locale): String = value.uppercase(locale)

    fun lower(value: String, locale: Locale): String = value.lowercase(locale)

    /**
     * The day the week starts on, from the platform locale — never from the
     * language. es-419 is split internally (AR/CL Monday, MX/CO/PE Sunday), so
     * language alone cannot answer this. (punch-list A7)
     */
    fun firstDayOfWeek(locale: Locale): DayOfWeek =
        WeekFields.of(locale).firstDayOfWeek

    /**
     * Calendar column headers, seven of them, starting at the locale's own first
     * day of the week.
     *
     * Two characters, not one. German single letters collide twice — Montag and
     * Mittwoch are both M, Dienstag and Donnerstag both D — and a German reader
     * cannot disambiguate them. Thai Thursday is พฤ and cannot be shortened
     * below two characters at all. (punch-list B4)
     */
    fun weekdayHeaders(locale: Locale): List<String> {
        val start = firstDayOfWeek(locale)
        return (0..6).map { offset ->
            val day = start.plus(offset.toLong())
            shortenWeekday(day.getDisplayName(TextStyle.SHORT, locale), locale)
        }
    }

    /** The offset of [day] within a week that starts at the locale's first day. */
    fun weekIndex(day: DayOfWeek, locale: Locale): Int {
        val start = firstDayOfWeek(locale).value
        return ((day.value - start) + 7) % 7
    }

    private fun shortenWeekday(raw: String, locale: Locale): String {
        // U+00B7 MIDDLE DOT and U+66DC (the CJK 'day-of-week' marker) are
        // appended to the short weekday name in some locales. Written as escapes
        // so the source stays ASCII and cannot be mis-decoded by a stray charset.
        val cleaned = raw.trim().trimEnd('.', '\u00B7', '\u66DC')
        if (cleaned.isEmpty()) return raw.trim()
        // Scripts whose abbreviation is already one ideograph stay one character;
        // everything else is capped at two so no two days collide.
        return if (cleaned.length <= 2) cleaned else cleaned.substring(0, 2)
    }

    /**
     * Reorders a two-part format string so the qualifier, not the value, is what
     * survives an end-truncation.
     *
     * The ongoing BAC notification title is `Estimated BAC %1$s`. Every pack puts
     * the placeholder last, so "Estimated" / 추정 / ค่าประมาณ leads and survives —
     * but nothing in the code guaranteed that, and a future translation that
     * fronts the number would silently ship a title reading only "0,032…".
     * This makes the guarantee structural: whatever the pack's word order, the
     * literal part of the template is emitted first. (punch-list B5)
     */
    fun qualifierFirst(context: Context, @StringRes template: Int, value: String): String =
        qualifierFirst(context.resources.getText(template).toString(), value)

    fun qualifierFirst(template: String, value: String): String {
        // A private-use sentinel: it cannot occur in any pack.
        val marker = ""
        val laid = template.format(marker)
        val at = laid.indexOf(marker)
        if (at < 0) return laid
        val before = laid.substring(0, at)
        val after = laid.substring(at + marker.length)
        if (before.isNotBlank()) return laid.replace(marker, value)
        // The pack put the value first: move it behind the qualifier instead of
        // letting the qualifier be the part that gets ellipsised.
        return (after.trimStart() + " " + value).trim()
    }
}

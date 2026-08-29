package com.mtss.alcoholtracker.util

import android.content.Context
import android.text.format.DateFormat
import com.mtss.alcoholtracker.R
import com.mtss.alcoholtracker.domain.CurrencyConfig
import com.mtss.alcoholtracker.domain.CurrencySpec
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

/**
 * Number, money and date rendering.
 *
 * Everything here used to be pinned to `Locale.US`: a hardcoded `$`, `%.2f` on
 * every currency, `MMMM d` date patterns and a 12-hour clock. All four are wrong
 * outside the United States, and two of them (the currency symbol and the digit
 * count) were on the punch list as shipping blockers.
 *
 * [bind] is called from the app process and again whenever the configuration
 * changes, so a locale switch takes effect without a restart.
 */
object Formatters {

    @Volatile
    private var locale: Locale = Locale.getDefault()

    @Volatile
    private var currency: CurrencySpec = CurrencyConfig.forLocale(Locale.getDefault())

    @Volatile
    private var use24Hour: Boolean = true

    private var oneDecimal = DecimalFormat("0.0", DecimalFormatSymbols.getInstance(Locale.getDefault()))
    private var twoDecimal = DecimalFormat("0.00", DecimalFormatSymbols.getInstance(Locale.getDefault()))
    private var grouping = DecimalFormat("#,##0", DecimalFormatSymbols.getInstance(Locale.getDefault()))
    private var fullDate = DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL)
    private var shortDate = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)
    private var monthYear = DateTimeFormatter.ofPattern("LLLL yyyy", Locale.getDefault())
    private var timeFormat = DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT)
    private var dateTimeShort =
        DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.SHORT)

    fun bind(context: Context) {
        val l = LocaleText.primaryLocale(context)
        locale = l
        currency = CurrencyConfig.forLocale(l)
        use24Hour = DateFormat.is24HourFormat(context)
        val symbols = DecimalFormatSymbols.getInstance(l)
        oneDecimal = DecimalFormat("0.0", symbols)
        twoDecimal = DecimalFormat("0.00", symbols)
        grouping = DecimalFormat("#,##0", symbols)
        fullDate = DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL).withLocale(l)
        shortDate = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(l)
        monthYear = DateTimeFormatter.ofPattern("LLLL yyyy", l)
        timeFormat = DateTimeFormatter
            .ofPattern(if (use24Hour) "HH:mm" else "h:mm a", l)
        dateTimeShort = DateTimeFormatter
            .ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.SHORT)
            .withLocale(l)
    }

    fun locale(): Locale = locale

    fun currency(): CurrencySpec = currency

    /** One decimal place, in the locale's own separator (`1,5` in de/fr/da/pl). */
    fun one(v: Double): String = oneDecimal.format(v)

    fun two(v: Double): String = twoDecimal.format(v)

    /**
     * Money, with the configured symbol, position and minor-unit count.
     * `¥800`, not `¥800.00`; `12,50 €`, not `€12.50`. (punch-list A3)
     */
    fun money(v: Double): String = currency.format(v, locale)

    fun moneyWhole(v: Double): String = currency.format(Math.round(v).toDouble(), locale, whole = true)

    fun grouped(v: Long): String = grouping.format(v)

    /** Clock time, honouring the device's 12/24-hour setting and the locale. */
    fun time(minutesOfDay: Int): String {
        val t = ((minutesOfDay % 1440) + 1440) % 1440
        return LocalTime.of(t / 60, t % 60).format(timeFormat)
    }

    /** A timestamp for the backup card: locale date pattern, locale clock. */
    fun dateTimeShort(millis: Long): String =
        dateTimeShort.format(
            java.time.Instant.ofEpochMilli(millis).atZone(java.time.ZoneId.systemDefault())
        )

    fun dayTitle(context: Context, epochDay: Long, todayKey: Long): String = when (epochDay) {
        todayKey -> context.getString(R.string.diary_today)
        todayKey - 1 -> context.getString(R.string.diary_yesterday)
        else -> shortDate(epochDay)
    }

    fun daySubtitle(epochDay: Long): String = LocalDate.ofEpochDay(epochDay).format(fullDate)

    fun monthYear(date: LocalDate): String = date.format(monthYear)

    fun shortDate(epochDay: Long): String = LocalDate.ofEpochDay(epochDay).format(shortDate)
}

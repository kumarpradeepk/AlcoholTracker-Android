package com.mtss.alcoholtracker.domain

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import com.mtss.alcoholtracker.util.LocaleText
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Currency
import java.util.Locale

/**
 * Money formatting. The old `Formatters.money` was wrong in three separate ways
 * at once (punch-list A3):
 *
 *  - **Symbol** was hardcoded `$`.
 *  - **Digits** were hardcoded `%.2f`, which prints `¥800.00`. JPY, KRW, VND,
 *    IDR, HUF and CLP have no minor unit at all.
 *  - **Position** was prefix-only. `€` is a *suffix* in French, and so is `₫`
 *    in Vietnamese; a prefix-only formatter is wrong in every French context,
 *    including for a French user configured in USD.
 */
data class CurrencySpec(
    val code: String,
    val symbol: String,
    val suffix: Boolean,
    val digits: Int,
    /** Whether a space separates symbol and amount. Non-breaking when it does. */
    val spaced: Boolean
) {
    fun format(amount: Double, locale: Locale, whole: Boolean = false): String {
        val decimals = if (whole) 0 else digits
        val symbols = DecimalFormatSymbols.getInstance(locale)
        val pattern = if (decimals > 0) "#,##0." + "0".repeat(decimals) else "#,##0"
        val number = DecimalFormat(pattern, symbols).format(amount)
        val gap = if (spaced) NBSP else ""
        return if (suffix) number + gap + symbol else symbol + gap + number
    }

    companion object {
        const val NBSP = " "
    }
}

object CurrencyConfig {

    /** Currencies with no minor unit — two decimals on these is simply wrong. */
    private val ZERO_DECIMAL = setOf("JPY", "KRW", "VND", "IDR", "HUF", "CLP", "ISK")

    /** Currencies written after the amount in the markets in scope. */
    private val SUFFIX_CURRENCIES = setOf("VND", "PLN", "DKK", "SEK", "NOK", "CZK", "HUF")

    /**
     * Locales that write the symbol after the amount whatever the currency.
     * French does this for USD too (`12,50 $`), which is why this is keyed on
     * the locale and not only on the currency.
     */
    private val SUFFIX_LOCALES = setOf("fr", "de", "it", "es", "pt", "pl", "da", "fi", "sv", "cs", "tr", "vi")

    /** Where the platform's own symbol is ambiguous or wrong for the market. */
    private val SYMBOLS = mapOf(
        "USD" to "$", "GBP" to "£", "EUR" to "€", "JPY" to "¥", "KRW" to "₩",
        "VND" to "₫", "IDR" to "Rp", "MYR" to "RM", "PHP" to "₱", "THB" to "฿",
        "HKD" to "HK$", "TRY" to "₺", "PLN" to "zł", "DKK" to "kr", "INR" to "₹",
        "BRL" to "R$", "MXN" to "$", "CLP" to "$", "COP" to "$", "PEN" to "S/",
        "ARS" to "$", "CAD" to "$", "AUD" to "$", "CHF" to "CHF", "HUF" to "Ft",
        "SGD" to "S$", "NZD" to "$"
    )

    fun forLocale(locale: Locale): CurrencySpec {
        val currency = runCatching { Currency.getInstance(locale) }.getOrNull()
        val code = currency?.currencyCode ?: "USD"
        val symbol = SYMBOLS[code] ?: currency?.getSymbol(locale) ?: code
        val digits = when {
            code in ZERO_DECIMAL -> 0
            currency != null && currency.defaultFractionDigits >= 0 -> currency.defaultFractionDigits
            else -> 2
        }
        val language = locale.language.lowercase(Locale.ROOT)
        val suffix = code in SUFFIX_CURRENCIES || language in SUFFIX_LOCALES
        return CurrencySpec(code, symbol, suffix, digits, spaced = suffix)
    }

    fun forContext(context: Context): CurrencySpec = forLocale(LocaleText.primaryLocale(context))
}

@Composable
fun rememberCurrency(): CurrencySpec {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    return remember(configuration) { CurrencyConfig.forContext(context) }
}

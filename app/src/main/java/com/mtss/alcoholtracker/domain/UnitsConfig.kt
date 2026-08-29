package com.mtss.alcoholtracker.domain

import android.content.Context
import androidx.annotation.PluralsRes
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import com.mtss.alcoholtracker.R
import com.mtss.alcoholtracker.util.LocaleText
import java.util.Locale

/**
 * Country configuration for the standard-drink definition and its noun.
 *
 * The app used to hardcode the WHO 10 g unit and the English word "units". Both
 * are wrong for most of the release markets, and in the direction that flatters
 * the user: a British reader was seeing ~20% less than they drank, a Danish
 * reader the same, a Japanese reader about half. For a harm-reduction product
 * that is the worse direction to be wrong in.
 *
 * The noun is not a translation of "unit" — it is a different measure with a
 * different national definition, so it lives here and not in the copy.
 */
enum class NounSense { UNIT, STANDARD, LOCAL }

enum class UnitCountry(
    val code: String,
    /** Grams of pure ethanol in one standard drink, per the national definition. */
    val gramsPerStandardDrink: Double,
    val sense: NounSense,
    /** Suffix used by the whole-phrase resources; see [UnitPhrases]. */
    val phraseKey: String
) {
    US("US", 14.0, NounSense.STANDARD, "us"),
    GB("GB", 8.0, NounSense.UNIT, "gb"),
    AU("AU", 10.0, NounSense.STANDARD, "au"),
    /** CCDUS, *Canada's Guidance on Alcohol and Health* (2023) — 13.45 g, 17.05 ml. */
    CA("CA", 13.45, NounSense.STANDARD, "ca"),
    /** MHLW defines the Japanese 単位 at 20 g; 19.75 g is the figure in the data set. */
    JP("JP", 19.75, NounSense.LOCAL, "jp"),
    DK("DK", 12.0, NounSense.LOCAL, "dk"),
    /** Bộ Y tế — matches the WHO figure, so Vietnam ships correct with nothing invented. */
    VN("VN", 10.0, NounSense.LOCAL, "vn"),
    IN("IN", 10.0, NounSense.STANDARD, "who"),
    /** WHO / EU and every market without its own published definition. */
    WHO("WHO", 10.0, NounSense.STANDARD, "who");

    companion object {
        fun fromCountryCode(raw: String?): UnitCountry {
            val cc = raw?.trim()?.uppercase(Locale.ROOT).orEmpty()
            return entries.firstOrNull { it.code == cc }
                ?: when (cc) {
                    "UK" -> GB
                    else -> WHO
                }
        }
    }
}

/**
 * The four surface forms the punch list asks for, plus two the packs proved were
 * also needed:
 *
 *  - [short] exists because *verres standard*, *bebidas estándar*, *bicchieri
 *    standard* and *Standardgläser* all overflow the 12-character stat-tile
 *    caption, and the abbreviation is too terse to sit under a number.
 *  - [indefinite] exists because in the gendered languages the article agrees
 *    with the noun (*une unité* / *un verre standard*), so a bare singular
 *    cannot be dropped into a sentence that needs one.
 *
 * Chinese's classifier (A2d) is not a sixth field: 個 sits between a numeral and
 * 酒精單位, but 標準杯 already ends in a classifier, so carrying a separate
 * token would put the burden on every call site. It is folded into the forms
 * that follow a numeral instead, and [short] keeps the bare noun for the
 * surfaces that do not - see the zh-Hant-HK rows in the generator.
 */
data class UnitNoun(
    val singular: String,
    val plural: String,
    val short: String,
    val abbrev: String,
    val indefinite: String
) {
    /**
     * The form to use next to a literal count. Only for surfaces that are *not*
     * plural resources — anything genuinely count-driven goes through a
     * `<plurals>` container so the locale's own CLDR rule selects the form.
     */
    fun forCount(count: Double): String = if (count == 1.0) singular else plural
}

object UnitsConfig {

    /**
     * The user's unit country. There is no picker yet, so it follows the device
     * region — which is also what A7 asks for on week start: the platform locale,
     * never the language.
     */
    fun countryFor(locale: Locale): UnitCountry =
        UnitCountry.fromCountryCode(locale.country)

    fun countryFor(context: Context): UnitCountry =
        countryFor(LocaleText.primaryLocale(context))

    fun gramsPerUnit(country: UnitCountry): Double = country.gramsPerStandardDrink

    fun noun(context: Context, country: UnitCountry): UnitNoun {
        val r = context.resources
        return when (country.sense) {
            NounSense.UNIT -> UnitNoun(
                r.getString(R.string.units_noun_unit_sg),
                r.getString(R.string.units_noun_unit_pl),
                r.getString(R.string.units_noun_unit_short),
                r.getString(R.string.units_noun_unit_abbr),
                r.getString(R.string.units_noun_unit_indef)
            )
            NounSense.STANDARD -> UnitNoun(
                r.getString(R.string.units_noun_std_sg),
                r.getString(R.string.units_noun_std_pl),
                r.getString(R.string.units_noun_std_short),
                r.getString(R.string.units_noun_std_abbr),
                r.getString(R.string.units_noun_std_indef)
            )
            NounSense.LOCAL -> UnitNoun(
                r.getString(R.string.units_noun_local_sg),
                r.getString(R.string.units_noun_local_pl),
                r.getString(R.string.units_noun_local_short),
                r.getString(R.string.units_noun_local_abbr),
                r.getString(R.string.units_noun_local_indef)
            )
        }
    }
}

/** Everything a call site needs to render a unit-bearing string. */
data class UnitsContext(
    val country: UnitCountry,
    val noun: UnitNoun,
    val locale: Locale
) {
    val gramsPerUnit: Double get() = country.gramsPerStandardDrink
}

@Composable
fun rememberUnits(): UnitsContext {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    return remember(configuration) {
        val locale = LocaleText.primaryLocale(context)
        val country = UnitsConfig.countryFor(locale)
        UnitsContext(country, UnitsConfig.noun(context, country), locale)
    }
}

/**
 * A unit-bearing string. Resolves the country's own wording first (the
 * whole-phrase escape hatch), then formats. Passing the noun even when the
 * resolved phrase does not use it is deliberate and safe: an unused format
 * argument is ignored, which is what lets one call site serve both the
 * parameterised locales and the ones that had to spell the sentence out.
 */
@Composable
fun unitsString(@StringRes base: Int, vararg args: Any): String {
    val units = rememberUnits()
    val context = LocalContext.current
    return context.getString(UnitPhrases.string(base, units.country), *args)
}

@Composable
fun unitsPlural(@PluralsRes base: Int, count: Int, vararg args: Any): String {
    val units = rememberUnits()
    val context = LocalContext.current
    return context.resources.getQuantityString(
        UnitPhrases.plural(base, units.country), count, *args
    )
}

fun unitsString(context: Context, @StringRes base: Int, vararg args: Any): String {
    val country = UnitsConfig.countryFor(context)
    return context.getString(UnitPhrases.string(base, country), *args)
}

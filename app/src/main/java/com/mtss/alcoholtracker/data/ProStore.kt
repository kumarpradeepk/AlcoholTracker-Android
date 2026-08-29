package com.mtss.alcoholtracker.data

import android.app.Activity
import android.app.Application
import android.content.pm.ApplicationInfo
import android.util.Log
import com.revenuecat.purchases.CustomerInfo
import com.revenuecat.purchases.LogLevel
import com.revenuecat.purchases.Offering
import com.revenuecat.purchases.Package
import com.revenuecat.purchases.PackageType
import com.revenuecat.purchases.PurchaseParams
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.PurchasesConfiguration
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.getCustomerInfoWith
import com.revenuecat.purchases.getOfferingsWith
import com.revenuecat.purchases.interfaces.UpdatedCustomerInfoListener
import com.revenuecat.purchases.models.Period
import com.revenuecat.purchases.purchaseWith
import com.revenuecat.purchases.restorePurchasesWith
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** How often a plan bills. Also the paywall's display order. */
enum class Cadence { WEEK, MONTH, YEAR, LIFETIME }

/**
 * One buyable plan, reduced to what the paywall needs.
 *
 * [price] is a store string, never assembled here — Play has already put the
 * right symbol on the right side with the right number of minor digits for the
 * buyer's country. [savingPercent] is worked out against the dearest week in
 * the offering, so it is the same number a shopper would get with a calculator.
 */
data class ProPlan(
    val id: String,
    val cadence: Cadence,
    val price: String,
    val priceMicros: Long,
    val freeTrialDays: Int,
    val savingPercent: Int,
    val rcPackage: Package
) {
    val hasTrial: Boolean get() = freeTrialDays > 0
}

/**
 * Everything the app knows about buying Pro.
 *
 * Two rules hold this file together:
 *
 * 1. **No price is ever written in the app.** Every number the paywall renders
 *    comes from the store through RevenueCat, already formatted for the
 *    buyer's currency, so the screen is right in all 19 shipped locales
 *    without a single currency literal in the string catalog. Changing what
 *    Pro costs is a dashboard edit, not a release.
 * 2. **The entitlement is the only truth.** `Settings.pro` is whatever
 *    RevenueCat says the `pro` entitlement is, cached to DataStore so a
 *    plane-mode launch still unlocks. Nothing else may set it.
 */
class ProStore(private val app: Application, private val settings: SettingsRepository) {

    data class State(
        val plans: List<ProPlan> = emptyList(),
        val loading: Boolean = true,
        /** True once an offering came back, even an empty one. */
        val loaded: Boolean = false,
        val purchaseInFlight: Boolean = false
    )

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val _state = MutableStateFlow(State())
    val state: StateFlow<State> = _state.asStateFlow()

    // ── Lifecycle ────────────────────────────────────────────────────────

    /**
     * Configures the SDK once, from [com.mtss.alcoholtracker.AlcoholApp].
     *
     * The app user id is the same opaque `customerId` the support screen
     * shows, so a purchase can be traced without an account and without a real
     * identity ever reaching RevenueCat.
     */
    fun configure(appUserId: String?) {
        if (Purchases.isConfigured) return
        Purchases.logLevel = if (debuggable) LogLevel.DEBUG else LogLevel.WARN
        Purchases.configure(
            PurchasesConfiguration.Builder(app, PUBLIC_SDK_KEY)
                .appUserID(appUserId?.takeIf { it.isNotBlank() })
                .build()
        )
        Purchases.sharedInstance.updatedCustomerInfoListener =
            UpdatedCustomerInfoListener { info -> applyEntitlement(info) }
        refresh()
    }

    /** Pulls the offering and the current entitlement. Cheap; safe on resume. */
    fun refresh() {
        if (!Purchases.isConfigured) return
        Purchases.sharedInstance.getOfferingsWith(
            onError = { e -> onOfferingsFailed(e) },
            onSuccess = { offerings -> onOfferings(offerings.current) }
        )
        Purchases.sharedInstance.getCustomerInfoWith(
            onError = { /* offline is not "not pro" — keep the cached entitlement */ },
            onSuccess = { info -> applyEntitlement(info) }
        )
    }

    // ── Buying ───────────────────────────────────────────────────────────

    /**
     * Runs the store's own purchase sheet for [plan].
     *
     * [onResult] gets true only when the `pro` entitlement is actually active
     * afterwards. A cancelled sheet and a declined card both report false, and
     * neither is an error worth shouting about.
     */
    fun purchase(activity: Activity, plan: ProPlan, onResult: (Boolean) -> Unit) {
        if (!Purchases.isConfigured || _state.value.purchaseInFlight) { onResult(false); return }
        _state.value = _state.value.copy(purchaseInFlight = true)
        Purchases.sharedInstance.purchaseWith(
            PurchaseParams.Builder(activity, plan.rcPackage).build(),
            onError = { _, _ ->
                _state.value = _state.value.copy(purchaseInFlight = false)
                onResult(false)
            },
            onSuccess = { _, info ->
                _state.value = _state.value.copy(purchaseInFlight = false)
                applyEntitlement(info)
                onResult(info.hasPro)
            }
        )
    }

    /** Re-reads purchases from the store. Works without an account. */
    fun restore(onResult: (Boolean) -> Unit) {
        if (!Purchases.isConfigured) { onResult(false); return }
        Purchases.sharedInstance.restorePurchasesWith(
            onError = { onResult(false) },
            onSuccess = { info -> applyEntitlement(info); onResult(info.hasPro) }
        )
    }

    // ── Internals ────────────────────────────────────────────────────────

    private fun onOfferings(current: Offering?) {
        val plans = current?.availablePackages.orEmpty().map { it.toPlan() }
        _state.value = _state.value.copy(
            plans = plans.withSavings().sortedBy { it.cadence.ordinal },
            loading = false,
            loaded = true
        )
    }

    private fun onOfferingsFailed(e: PurchasesError) {
        Log.w(TAG, "offerings unavailable: ${e.message}")
        _state.value = _state.value.copy(loading = false, loaded = false)
    }

    private fun applyEntitlement(info: CustomerInfo) {
        val pro = info.hasPro
        scope.launch { settings.update { if (it.pro == pro) it else it.copy(pro = pro) } }
    }

    /** Mirrors `BuildConfig.DEBUG` without asking for the generated class. */
    private val debuggable: Boolean
        get() = (app.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0

    private fun Package.toPlan(): ProPlan {
        val cadence = when (packageType) {
            PackageType.WEEKLY -> Cadence.WEEK
            PackageType.MONTHLY -> Cadence.MONTH
            PackageType.ANNUAL -> Cadence.YEAR
            PackageType.LIFETIME -> Cadence.LIFETIME
            else -> product.period.toCadence()
        }
        return ProPlan(
            id = identifier,
            cadence = cadence,
            price = product.price.formatted,
            priceMicros = product.price.amountMicros,
            freeTrialDays = product.defaultOption?.freePhase?.billingPeriod.dayCount(),
            savingPercent = 0,
            rcPackage = this
        )
    }

    /**
     * Fills in [ProPlan.savingPercent] against the dearest week in the offering.
     *
     * Per-week rather than per-plan is what makes "save 92%" true rather than
     * marketing; lifetime is left out because it has no week.
     */
    private fun List<ProPlan>.withSavings(): List<ProPlan> {
        val perWeek = { p: ProPlan -> p.priceMicros.toDouble() / p.cadence.weeks() }
        val dearest = filter { it.cadence != Cadence.LIFETIME }.maxOfOrNull(perWeek) ?: return this
        if (dearest <= 0.0) return this
        return map { plan ->
            if (plan.cadence == Cadence.LIFETIME) plan
            else plan.copy(
                savingPercent = (100.0 * (1.0 - perWeek(plan) / dearest)).toInt().coerceIn(0, 99)
            )
        }
    }

    companion object {
        /** One store per process; the SDK is a singleton underneath anyway. */
        @Volatile private var instance: ProStore? = null

        fun get(app: Application): ProStore = instance ?: synchronized(this) {
            instance ?: ProStore(app, SettingsRepository.get(app)).also { instance = it }
        }

        private const val TAG = "ProStore"

        /**
         * RevenueCat public SDK key for "Alcohol Tracker (Play Store)", project
         * `4452a7f1`. Public by design: it can read offerings and start a
         * purchase for this app and nothing else. The secret key is never shipped.
         */
        private const val PUBLIC_SDK_KEY = "goog_NDZciDxTaVyVORqGLzhGozWfUvW"

        /** Entitlement identifier, as configured in the RevenueCat dashboard. */
        private const val ENTITLEMENT_PRO = "pro"

        private val CustomerInfo.hasPro: Boolean
            get() = entitlements[ENTITLEMENT_PRO]?.isActive == true

        private fun Cadence.weeks(): Double = when (this) {
            Cadence.WEEK -> 1.0
            Cadence.MONTH -> 52.0 / 12.0
            Cadence.YEAR -> 52.0
            Cadence.LIFETIME -> 520.0 // unused; keeps the maths total
        }

        /** A store period expressed in days, whatever unit the store chose. */
        private fun Period?.dayCount(): Int = when (this?.unit) {
            Period.Unit.DAY -> value
            Period.Unit.WEEK -> value * 7
            Period.Unit.MONTH -> value * 30
            Period.Unit.YEAR -> value * 365
            else -> 0
        }

        private fun Period?.toCadence(): Cadence = when (this?.unit) {
            Period.Unit.WEEK -> Cadence.WEEK
            Period.Unit.YEAR -> Cadence.YEAR
            Period.Unit.MONTH -> if (value >= 12) Cadence.YEAR else Cadence.MONTH
            Period.Unit.DAY -> if (value >= 300) Cadence.YEAR else Cadence.WEEK
            else -> Cadence.LIFETIME
        }
    }
}

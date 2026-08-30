package com.mtss.alcoholtracker.ui

import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.annotation.StringRes
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.mtss.alcoholtracker.data.AppDatabase
import com.mtss.alcoholtracker.data.BackupManager
import com.mtss.alcoholtracker.data.DrinkLog
import com.mtss.alcoholtracker.data.DryDay
import com.mtss.alcoholtracker.data.ReminderItem
import com.mtss.alcoholtracker.data.SavedDrink
import com.mtss.alcoholtracker.data.Settings
import com.mtss.alcoholtracker.data.Cadence
import com.mtss.alcoholtracker.data.ProPlan
import com.mtss.alcoholtracker.data.ProStore
import com.mtss.alcoholtracker.data.SettingsRepository
import com.mtss.alcoholtracker.data.Tone
import com.mtss.alcoholtracker.R
import com.mtss.alcoholtracker.domain.AlcoholMath
import com.mtss.alcoholtracker.domain.DrinkPresets
import com.mtss.alcoholtracker.domain.Preset
import com.mtss.alcoholtracker.domain.StatsPeriod
import com.mtss.alcoholtracker.domain.UnitNoun
import com.mtss.alcoholtracker.domain.UnitsConfig
import com.mtss.alcoholtracker.util.Formatters
import com.mtss.alcoholtracker.notifications.BacStatusNotifier
import com.mtss.alcoholtracker.notifications.ReminderScheduler
import com.mtss.alcoholtracker.util.CsvExport
import com.mtss.alcoholtracker.util.Haptics
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

enum class Phase { BOOT, WELCOME, GOALS, BASELINE, PAYWALL, APP }
enum class Tab { DIARY, STATS, SETTINGS }
enum class PushScreen(@StringRes val titleRes: Int) {
    PROFILE(R.string.set_profile),
    UNITS(R.string.set_units),
    NOTIFS(R.string.set_notifications),
    BAC(R.string.bac_monitor),
    QUICKLOG(R.string.push_title_quick_log),
    BACKUP(R.string.push_title_backup),
    ABOUT(R.string.push_title_about),
    ICON(R.string.set_app_icon),
    THEME(R.string.push_title_theme),
    TRENDS(R.string.push_title_trends),
    GUIDE(R.string.push_title_guideline)
}
enum class CalMode { SELECT, DRY }

sealed interface Sheet {
    data object Log : Sheet
    data class Cal(val mode: CalMode) : Sheet
    data class Entry(val log: DrinkLog) : Sheet
    data object UnitsInfo : Sheet
    data object BacInfo : Sheet
    data object Range : Sheet
    data object NewNotif : Sheet
    data object LivePreview : Sheet
    data object CustomDrink : Sheet
    data object Export : Sheet
    data object Health : Sheet
}

enum class AppDialog { DELETE_ENTRY, CLEAR_ALL, HEALTH_CONNECT }

data class ToastData(val message: String, val undoable: Boolean = false)

/** What the user picked on step 1 of the log sheet. */
data class PickedDrink(val name: String, val abv: Double, val ml: Double, val cost: Double?)

class AppViewModel(app: Application) : AndroidViewModel(app) {

    interface HostActions {
        fun requestNotificationPermission()
        fun launchExportDocument(suggestedName: String)
        fun launchImportDocument()
        fun shareIntent(intent: Intent)
        fun openUrl(url: String)
        fun openLanguageSettings()
        fun contactSupport(customerId: String)
        fun applyIcon(index: Int)
        fun showBiometricPrompt()
    }

    var hostActions: HostActions? = null

    private val db = AppDatabase.get(app)
    private val dao = db.dao()
    val settingsRepo = SettingsRepository.get(app)
    val backup = BackupManager(app, dao)
    val pro = ProStore.get(app)

    val logs = dao.logsFlow().stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val dryDays = dao.dryDaysFlow().map { list -> list.map { it.epochDay }.toSet() }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptySet())
    val savedDrinks = dao.savedDrinksFlow().stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val reminders = dao.remindersFlow().stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val settings = settingsRepo.flow.stateIn(viewModelScope, SharingStarted.Eagerly, Settings())

    // ── Phase / boot ─────────────────────────────────────────────────────

    var phase by mutableStateOf(Phase.BOOT)
        private set
    var bootFading by mutableStateOf(false)
        private set

    /** App lock: content hidden until the prompt succeeds. */
    var locked by mutableStateOf(false)
        private set
    private var lockChecked = false

    init {
        // Bind the locale-sensitive singletons before anything renders.
        Formatters.bind(app)
        AlcoholMath.bind(UnitsConfig.countryFor(app))
        viewModelScope.launch {
            settingsRepo.ensureCustomerId()
            val s = settingsRepo.flow.first()
            // Purchases needs the stable, anonymous customer id, so it waits
            // for DataStore rather than configuring in Application.onCreate().
            pro.configure(s.customerId)
            if (s.appLock) {
                locked = true
                lockChecked = true
            }
            delay(1000)
            bootFading = true
            delay(650)
            phase = if (s.onboardingDone) Phase.APP else Phase.WELCOME
            if (locked) hostActions?.showBiometricPrompt()
        }
        viewModelScope.launch {
            // Keep the settling notification honest as logs/settings change.
            logs.collect { refreshBacNotification() }
        }
    }

    fun onAppForegrounded() {
        if (!lockChecked) return
        refreshBacNotification()
    }

    fun onUnlocked() {
        locked = false
    }

    // ── Onboarding ───────────────────────────────────────────────────────

    var obGoals by mutableStateOf(setOf<Int>())
    var obBase by mutableStateOf(-1)

    fun toGoals() { phase = Phase.GOALS }
    fun backToWelcome() { phase = Phase.WELCOME }
    fun backToGoals() { phase = Phase.GOALS }
    fun toggleGoal(i: Int) {
        obGoals = if (obGoals.contains(i)) obGoals - i else obGoals + i
    }
    fun goalsContinue() { if (obGoals.isNotEmpty()) phase = Phase.BASELINE }
    fun pickBase(i: Int) { obBase = i }
    fun baseContinue() {
        if (obBase < 0) return
        persistOnboarding()
        // P7/§10: no paywall at onboarding — it appears on intent only.
        phase = Phase.APP
    }
    fun skipOnboarding() {
        persistOnboarding()
        phase = Phase.APP
    }
    private fun persistOnboarding() {
        val goals = obGoals; val base = obBase
        viewModelScope.launch {
            settingsRepo.update { it.copy(onboardingDone = true, goals = goals, baselineAnswer = base) }
        }
    }

    // ── Paywall ──────────────────────────────────────────────────────────

    /** Which plan card is selected. Reset to the recommended one on open. */
    var planIndex by mutableStateOf(0)
    var faqOpen by mutableStateOf(-1)

    /** Plans as the store priced them; empty until the offering lands. */
    val plans: List<ProPlan> get() = pro.state.value.plans

    val selectedPlan: ProPlan? get() = plans.getOrNull(planIndex)

    /**
     * The plan that should be selected when the paywall opens: the one with a
     * free trial if the store is offering one, otherwise the yearly.
     *
     * Deliberately not "the dearest" — the trial is what the research says
     * moves conversion, and pre-selecting a plan the buyer did not choose is
     * the kind of trick this app promised not to play.
     */
    private fun recommendedIndex(): Int {
        val list = plans
        val trial = list.indexOfFirst { it.hasTrial }
        if (trial >= 0) return trial
        val yearly = list.indexOfFirst { it.cadence == Cadence.YEAR }
        return if (yearly >= 0) yearly else 0
    }

    fun openPaywall() {
        pro.refresh()
        planIndex = recommendedIndex()
        faqOpen = -1
        phase = Phase.PAYWALL
    }

    fun closePaywall() { phase = Phase.APP }

    /**
     * Hands the selected plan to the store. The entitlement, and therefore
     * `Settings.pro`, is set by [ProStore] from what the purchase actually
     * granted — this function never flips it itself.
     */
    fun buy(activity: Activity?) {
        val plan = selectedPlan
        if (activity == null || plan == null) { toast(R.string.toast_purchase_unavailable); return }
        pro.purchase(activity, plan) { unlocked ->
            if (unlocked) {
                phase = Phase.APP
                toast(R.string.toast_pro_welcome)
            }
        }
    }

    fun restorePurchases() = pro.restore { unlocked ->
        if (unlocked) {
            phase = Phase.APP
            toast(R.string.toast_restore_success)
        } else {
            toast(R.string.toast_restore_none)
        }
    }

    // ── Tabs / diary day ─────────────────────────────────────────────────

    var tab by mutableStateOf(Tab.DIARY)
    var fabOpen by mutableStateOf(false)

    fun selectTab(t: Tab) { tab = t; fabOpen = false }

    fun todayKey(): Long = AlcoholMath.todayKey(settings.value.cutoff)

    var selDay by mutableStateOf<Long?>(null)   // null = today (tracks cutoff live)
    fun selectedDay(): Long = selDay ?: todayKey()
    fun dayPrev() { selDay = selectedDay() - 1; fabOpen = false }
    fun dayNext() { if (selectedDay() < todayKey()) selDay = selectedDay() + 1 }

    fun dayLogs(day: Long): List<DrinkLog> =
        logs.value.filter { it.epochDay == day }.sortedBy { it.atMillis }

    fun dayUnits(day: Long): Double =
        dayLogs(day).sumOf { AlcoholMath.units(it.ml, it.abv) }

    fun weekUnits(): Double {
        val t = todayKey()
        return ((t - 6)..t).sumOf { dayUnits(it) }
    }

    fun monthUnits(): Double {
        val today = java.time.LocalDate.ofEpochDay(todayKey())
        return logs.value
            .filter {
                val d = java.time.LocalDate.ofEpochDay(it.epochDay)
                d.month == today.month && d.year == today.year
            }
            .sumOf { AlcoholMath.units(it.ml, it.abv) }
    }

    fun bacNow(): AlcoholMath.BacEstimate? {
        val s = settings.value
        return AlcoholMath.bac(dayLogs(todayKey()), s.weightKg(), s.sex)
    }

    /** Top-3 most frequent drinks, falling back to the first presets. */
    fun quickItems(): List<PickedDrink> {
        val freq = logs.value.groupBy { it.name }
        val top = freq.entries.sortedByDescending { it.value.size }.take(3)
            .map { (n, l) ->
                val last = l.maxBy { it.atMillis }
                PickedDrink(n, last.abv, last.ml, last.cost.takeIf { c -> c > 0 })
            }
        if (top.isNotEmpty()) return top
        return DrinkPresets.ALL.take(3).map { PickedDrink(it.name, it.abv, it.ml, it.cost) }
    }

    // ── Sheets / push / dialogs / toasts ─────────────────────────────────

    var sheet by mutableStateOf<Sheet?>(null)
        private set
    var push by mutableStateOf<PushScreen?>(null)
        private set
    var dialog by mutableStateOf<AppDialog?>(null)
        private set
    var toast by mutableStateOf<ToastData?>(null)
        private set

    private var toastJob: Job? = null
    private var pendingDelete: DrinkLog? = null

    fun openSheet(s: Sheet) { fabOpen = false; sheet = s }
    fun closeSheet() { sheet = null }
    fun openPush(p: PushScreen) { push = p }
    fun closePush() { push = null }
    fun openDialog(d: AppDialog) { dialog = d }
    fun closeDialog() { dialog = null }

    fun toast(@StringRes message: Int, undoable: Boolean = false) =
        toast(str(message), undoable)

    fun toast(message: String, undoable: Boolean = false) {
        toastJob?.cancel()
        toast = ToastData(message, undoable)
        toastJob = viewModelScope.launch {
            delay(if (undoable) 5000 else 2600)
            toast = null
        }
    }

    // ── Log sheet draft ──────────────────────────────────────────────────

    var logStep by mutableStateOf(0)
    var pick by mutableStateOf<PickedDrink?>(null)
    var query by mutableStateOf("")
    var dAbv by mutableStateOf(5.0)
    var dMl by mutableStateOf(355.0)
    var servMl by mutableStateOf(true)
    var qty by mutableStateOf(1.0)
    var dWhen by mutableStateOf(0)       // 0 now · 1 an hour ago · 2 two hours ago
    var dCost by mutableStateOf("")

    fun startLog() {
        logStep = 0; pick = null; query = ""; qty = 1.0; dWhen = 0
        openSheet(Sheet.Log)
    }

    fun pickDrink(p: PickedDrink) { pick = p }

    fun logNext() {
        val p = pick ?: return
        when (logStep) {
            0 -> { dAbv = p.abv; dMl = p.ml; servMl = true; logStep = 1 }
            1 -> { dCost = ((p.cost ?: (dMl * 0.07)).let { Math.round(it) }).toString(); dWhen = 0; logStep = 2 }
            else -> saveLog()
        }
    }

    fun logBack() { if (logStep > 0) logStep-- }

    private var lastLoggedId: String? = null

    fun saveLog() {
        val p = pick ?: return
        val s = settings.value
        val now = System.currentTimeMillis()
        val at = now - longArrayOf(0, 3_600_000, 7_200_000)[dWhen]
        val ml = dMl * qty
        val target = selectedDay()
        val log = DrinkLog(
            id = UUID.randomUUID().toString(),
            name = p.name,
            ml = ml,
            abv = dAbv,
            atMillis = at,
            // A back-dated diary day keeps its date; a live log follows the cut-off.
            epochDay = if (target == todayKey()) AlcoholMath.drinkingDay(at, s.cutoff).toEpochDay() else target,
            cost = dCost.toDoubleOrNull() ?: 0.0,
            kcal = AlcoholMath.kcal(ml, dAbv)
        )
        viewModelScope.launch {
            dao.insertLog(log)
            dao.deleteDryDay(log.epochDay)
        }
        lastLoggedId = log.id
        Haptics.logged(getApplication())
        closeSheet()
        toast(
            str(R.string.toast_logged, AlcoholMath.units(ml, dAbv), unitNoun().plural),
            undoable = true
        )
        refreshBacNotification()
    }

    fun quickLog(p: PickedDrink) {
        val s = settings.value
        val now = System.currentTimeMillis()
        val log = DrinkLog(
            id = UUID.randomUUID().toString(),
            name = p.name, ml = p.ml, abv = p.abv, atMillis = now,
            epochDay = AlcoholMath.drinkingDay(now, s.cutoff).toEpochDay(),
            cost = p.cost ?: 0.0,
            kcal = AlcoholMath.kcal(p.ml, p.abv)
        )
        viewModelScope.launch {
            dao.insertLog(log)
            dao.deleteDryDay(log.epochDay)
        }
        lastLoggedId = log.id
        selDay = null
        Haptics.logged(getApplication())
        toast(
            str(
                R.string.toast_quick_logged,
                p.name, AlcoholMath.units(p.ml, p.abv), unitNoun().abbrev
            ),
            undoable = true
        )
        refreshBacNotification()
    }

    fun undoLast() {
        val id = lastLoggedId ?: return
        lastLoggedId = null
        toastJob?.cancel(); toast = null
        viewModelScope.launch { dao.deleteLog(id) }
        refreshBacNotification()
    }

    fun relog(entry: DrinkLog) {
        val s = settings.value
        val now = System.currentTimeMillis()
        val log = entry.copy(
            id = UUID.randomUUID().toString(),
            atMillis = now,
            epochDay = AlcoholMath.drinkingDay(now, s.cutoff).toEpochDay()
        )
        viewModelScope.launch {
            dao.insertLog(log)
            dao.deleteDryDay(log.epochDay)
        }
        Haptics.logged(getApplication())
        closeSheet()
        toast(R.string.toast_relogged)
        refreshBacNotification()
    }

    fun askDelete(entry: DrinkLog) {
        pendingDelete = entry
        openDialog(AppDialog.DELETE_ENTRY)
    }

    fun confirmDelete() {
        val e = pendingDelete ?: return
        pendingDelete = null
        viewModelScope.launch { dao.deleteLog(e.id) }
        closeDialog(); closeSheet()
        toast(R.string.toast_entry_removed)
        refreshBacNotification()
    }

    // ── Dry days ─────────────────────────────────────────────────────────

    fun markDry(day: Long) {
        if (dayLogs(day).isNotEmpty()) {
            toast(R.string.toast_day_has_drinks)
            return
        }
        viewModelScope.launch { dao.insertDryDay(DryDay(day)) }
        Haptics.dryDay(getApplication())
        toast(R.string.toast_dry_marked)
    }

    fun unDry(day: Long) {
        viewModelScope.launch { dao.deleteDryDay(day) }
    }

    fun calendarDayTapped(day: Long, mode: CalMode) {
        when (mode) {
            CalMode.SELECT -> { selDay = if (day == todayKey()) null else day; closeSheet() }
            CalMode.DRY -> {
                if (dayLogs(day).isNotEmpty()) { toast(R.string.toast_day_has_drinks_cal); return }
                if (dryDays.value.contains(day)) unDry(day) else {
                    viewModelScope.launch { dao.insertDryDay(DryDay(day)) }
                    Haptics.tick(getApplication())
                }
            }
        }
    }

    // ── Custom drinks / quick tiles ──────────────────────────────────────

    var cuName by mutableStateOf("")
    var cuBase by mutableStateOf("Beer")
    var cuAbv by mutableStateOf("5")
    var cuMl by mutableStateOf("355")
    var cuNotes by mutableStateOf("")

    fun openCustomDrink() {
        cuName = ""; cuBase = "Beer"; cuAbv = "5"; cuMl = "355"; cuNotes = ""
        openSheet(Sheet.CustomDrink)
    }

    fun saveCustomDrink() {
        if (cuName.isBlank()) return
        val drink = SavedDrink(
            name = cuName.trim(),
            base = cuBase,
            abv = cuAbv.toDoubleOrNull() ?: 5.0,
            ml = cuMl.toDoubleOrNull() ?: 355.0,
            notes = cuNotes.trim(),
            createdAt = System.currentTimeMillis()
        )
        viewModelScope.launch { dao.insertSavedDrink(drink) }
        closeSheet()
        toast(R.string.toast_custom_saved)
    }

    fun toggleQuickAccess(drink: SavedDrink) {
        val current = savedDrinks.value.count { it.quickAccess }
        val turningOn = !drink.quickAccess
        if (turningOn && current >= 4) return
        viewModelScope.launch { dao.setQuickAccess(drink.id, turningOn) }
    }

    // ── Reminders ────────────────────────────────────────────────────────

    var ndTitle by mutableStateOf("")
    var ndTime by mutableStateOf(20 * 60 + 30)
    var ndMsg by mutableStateOf("")
    private var notifPermissionAsked = false
    private var pendingReminder: ReminderItem? = null

    fun openNewNotif() {
        ndTitle = ""; ndTime = 20 * 60 + 30; ndMsg = ""
        openSheet(Sheet.NewNotif)
    }

    fun createReminder() {
        if (ndTitle.isBlank()) return
        val item = ReminderItem(title = ndTitle.trim(), timeMinutes = ndTime, message = ndMsg.trim())
        closeSheet()
        if (!notifPermissionAsked) {
            notifPermissionAsked = true
            pendingReminder = item
            hostActions?.requestNotificationPermission()
        } else {
            persistReminder(item, announce = true)
        }
    }

    fun onNotifPermissionResult(granted: Boolean) {
        val item = pendingReminder
        pendingReminder = null
        if (item != null) {
            persistReminder(item, announce = granted)
            if (!granted) toast(str(R.string.toast_reminder_silent, PLATFORM))
        }
    }

    private fun persistReminder(item: ReminderItem, announce: Boolean) {
        viewModelScope.launch { dao.insertReminder(item) }
        ReminderScheduler.schedule(getApplication(), item)
        if (announce) toast(R.string.toast_reminder_scheduled)
    }

    fun removeReminder(item: ReminderItem) {
        viewModelScope.launch { dao.deleteReminder(item.id) }
        ReminderScheduler.cancel(getApplication(), item)
    }

    // ── Statistics state ─────────────────────────────────────────────────

    var period by mutableStateOf(StatsPeriod.D7)
    var pageBack by mutableStateOf(0)
    var avgUnits by mutableStateOf(false)
    var avgSpend by mutableStateOf(false)
    var customFrom by mutableStateOf<Long?>(null)
    var customTo by mutableStateOf<Long?>(null)

    fun selectPeriod(p: StatsPeriod) {
        if (p.locked && !settings.value.pro) { openPaywall(); return }
        if (p == StatsPeriod.CUSTOM) {
            if (customFrom == null) customFrom = todayKey() - 13
            if (customTo == null) customTo = todayKey()
            openSheet(Sheet.Range)
            return
        }
        period = p; pageBack = 0
    }

    fun applyCustomRange() {
        period = StatsPeriod.CUSTOM; pageBack = 0
        closeSheet()
    }

    fun exportCsv(fileLabel: String, logsInRange: List<DrinkLog>) {
        val intent = CsvExport.share(getApplication(), fileLabel, logsInRange)
        closeSheet()
        hostActions?.shareIntent(intent)
        toast(R.string.toast_csv_ready_android)
    }

    // ── Calendar sheet state ─────────────────────────────────────────────

    var calMonthOffset by mutableStateOf(0)

    fun openCalendar(mode: CalMode) {
        calMonthOffset = 0
        openSheet(Sheet.Cal(mode))
    }

    // ── Settings mutations ───────────────────────────────────────────────

    fun updateSettings(transform: (Settings) -> Settings) {
        viewModelScope.launch {
            settingsRepo.update(transform)
            refreshBacNotification()
        }
    }

    fun saveProfile(): Boolean {
        val s = settings.value
        if (s.weight.isBlank() || s.sex == null) {
            toast(R.string.toast_profile_incomplete)
            return false
        }
        closePush()
        toast(R.string.toast_profile_saved)
        return true
    }

    /** Switches the whole token set. Persisted immediately; no confirm step. */
    fun applyTheme(themeId: String) {
        if (settings.value.themeId == themeId) return
        updateSettings { it.copy(themeId = themeId) }
        Haptics.tick(getApplication())
    }

    fun applyIconChoice(index: Int) {
        updateSettings { it.copy(iconIndex = index) }
        hostActions?.applyIcon(index)
        closePush()
        toast(R.string.toast_icon_updated)
    }

    fun connectHealth() {
        openDialog(AppDialog.HEALTH_CONNECT)
    }

    fun confirmHealth() {
        // Health Connect write integration point: register alcohol-consumption
        // records via androidx.health.connect when wiring the real SDK.
        updateSettings { it.copy(healthConnected = true) }
        closeDialog(); closeSheet()
        toast(str(R.string.toast_health_connected, HEALTH_CONNECT))
    }

    // ── Backup / export / clear ──────────────────────────────────────────

    fun exportDataTapped() {
        hostActions?.launchExportDocument("alcohol-tracker-export.json")
    }

    fun importDataTapped() {
        hostActions?.launchImportDocument()
    }

    fun onExportDocumentPicked(uri: Uri) {
        viewModelScope.launch {
            val json = backup.buildJson(
                logs.value,
                dryDays.value.map { DryDay(it) },
                savedDrinks.value,
                reminders.value
            )
            backup.writeTo(uri, json)
            toast(R.string.toast_export_file_android)
        }
    }

    fun onImportDocumentPicked(uri: Uri) {
        viewModelScope.launch {
            val text = backup.readFrom(uri)
            if (text == null) { toast(R.string.toast_file_unreadable); return@launch }
            runCatching { backup.importJson(text) }
                .onSuccess { result -> toast(importResultLine(result)) }
                .onFailure { toast(R.string.toast_import_invalid_android) }
        }
    }

    fun createLocalBackup() {
        viewModelScope.launch {
            backup.createLocalBackup(
                logs.value, dryDays.value.map { DryDay(it) }, savedDrinks.value, reminders.value
            )
            settingsRepo.update { it.copy(lastBackupAt = System.currentTimeMillis()) }
            toast(R.string.toast_backup_created)
        }
    }

    fun restoreLocalBackup() {
        viewModelScope.launch {
            val merged = backup.restoreLocalBackup()
            if (merged == null) toast(R.string.toast_backup_none_android)
            else toast(importResultLine(merged))
        }
    }

    fun confirmClearAll() {
        viewModelScope.launch {
            reminders.value.forEach { ReminderScheduler.cancel(getApplication(), it) }
            dao.clearLogs(); dao.clearDryDays(); dao.clearSavedDrinks(); dao.clearReminders()
        }
        closeDialog(); closePush()
        toast(R.string.toast_cleared)
        refreshBacNotification()
    }

    // ── BAC ongoing notification ─────────────────────────────────────────

    private fun refreshBacNotification() {
        val s = settings.value
        BacStatusNotifier.update(
            getApplication(),
            bacNow(),
            enabled = s.pro && s.bacOn,
            discreet = s.discreet,
            unitPercent = s.bacUnitPercent
        )
    }

    // ── Copy helpers (tone-aware) ────────────────────────────────────────

    /**
     * The status line under the ring. The tone branching is unchanged; only the
     * source of the words moved. The unit noun is country config, so it comes
     * from [UnitNoun] rather than from an English "s".
     */
    fun remainingLine(
        context: Context,
        dayUnits: Double,
        tone: Tone,
        dailyGoal: Int,
        noun: UnitNoun
    ): String {
        val rem = dailyGoal - dayUnits
        return when {
            tone == Tone.NUMBERS -> context.getString(
                R.string.diary_remaining_numbers,
                dayUnits, dailyGoal, noun.forCount(dailyGoal.toDouble())
            )
            dayUnits == 0.0 -> context.getString(R.string.diary_remaining_zero_neutral)
            rem > 0 -> context.getString(
                R.string.diary_remaining_left_neutral, rem, noun.forCount(rem)
            )
            rem == 0.0 -> context.getString(R.string.diary_remaining_at_target_neutral)
            else -> context.getString(
                R.string.diary_remaining_over_neutral, -rem, dailyGoal, noun.forCount(-rem)
            )
        }
    }

    /**
     * Two-dimensional plural (imported x skipped). Android's selector takes one
     * quantity and every pack inflects on the *skipped* count, so that drives the
     * container and the imported count rides along as a plain argument.
     */
    private fun importResultLine(result: BackupManager.ImportResult): String =
        getApplication<Application>().resources.getQuantityString(
            R.plurals.toast_import_result,
            result.skipped, result.imported, result.skipped
        )

    private fun unitNoun(): UnitNoun {
        val app = getApplication<Application>()
        return UnitsConfig.noun(app, UnitsConfig.countryFor(app))
    }

    private fun str(@StringRes id: Int, vararg args: Any): String =
        getApplication<Application>().getString(id, *args)

    companion object {
        /** Product names, deliberately excluded from the inventory as brand identity. */
        const val HEALTH_CONNECT = "Health Connect"
        const val PLATFORM = "Android"

        fun factory(app: Application) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T =
                AppViewModel(app) as T
        }
    }
}

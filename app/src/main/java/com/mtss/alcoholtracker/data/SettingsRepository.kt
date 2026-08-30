package com.mtss.alcoholtracker.data

import android.content.Context
import androidx.annotation.StringRes
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.mtss.alcoholtracker.R
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID

private val Context.dataStore by preferencesDataStore(name = "settings")

/**
 * The stored name is the persisted identity and never changes; [labelRes] is what
 * the user reads. Keeping the two apart is what stops a translated label from
 * being written into DataStore.
 */
enum class Tone(@StringRes val labelRes: Int, @StringRes val subRes: Int) {
    NEUTRAL(R.string.set_tone_neutral, R.string.set_tone_sub_neutral),
    PUSH(R.string.set_tone_push, R.string.set_tone_sub_push),
    NUMBERS(R.string.set_tone_numbers, R.string.set_tone_sub_numbers);

    companion object {
        fun from(raw: String?): Tone = entries.firstOrNull { it.name == raw } ?: NEUTRAL
    }
}

enum class DarkChoice { SYSTEM, LIGHT, DARK }

enum class DayCutoff(@StringRes val labelRes: Int, val hour: Int) {
    MIDNIGHT(R.string.set_cutoff_midnight, 0),
    TWO_AM(R.string.set_cutoff_2am, 2),
    FOUR_AM(R.string.set_cutoff_4am, 4);

    fun next(): DayCutoff = entries[(ordinal + 1) % entries.size]

    companion object {
        fun from(raw: String?): DayCutoff = entries.firstOrNull { it.name == raw } ?: FOUR_AM
    }
}

data class Settings(
    val askCost: Boolean = true,
    val showCalories: Boolean = true,
    val autoDry: Boolean = true,
    val bacOn: Boolean = true,
    val bacUnitPercent: Boolean = true,      // % vs ‰
    val energyKcal: Boolean = true,          // kcal vs kJ
    val servingMl: Boolean = true,           // ml vs oz
    val sex: String? = null,                 // "Female" | "Male"
    val weight: String = "",
    val weightKgUnit: Boolean = true,        // kg vs lb
    val dailyGoal: Int = 2,
    val weeklyGoal: Int = 10,
    val healthConnected: Boolean = false,
    val iconIndex: Int = 0,
    val baseline: String = "",
    val tone: Tone = Tone.NEUTRAL,
    val appLock: Boolean = false,
    val discreet: Boolean = true,
    val cutoff: DayCutoff = DayCutoff.FOUR_AM,
    val pro: Boolean = false,
    val onboardingDone: Boolean = false,
    val darkChoice: DarkChoice = DarkChoice.SYSTEM,
    /** Theme id from `ui.theme.AppTheme`; stored as its stable slug. */
    val themeId: String = "kiln",
    val goals: Set<Int> = emptySet(),
    val baselineAnswer: Int = -1,
    val customerId: String = "",
    val lastBackupAt: Long = 0L
) {
    val monthlyGoal: Int get() = Math.round(weeklyGoal * 4.3).toInt()

    fun weightKg(): Double {
        val w = weight.toDoubleOrNull() ?: return 0.0
        return if (weightKgUnit) w else w * 0.4536
    }
}

class SettingsRepository(private val context: Context) {

    private object K {
        val askCost = booleanPreferencesKey("askCost")
        val showCalories = booleanPreferencesKey("showCalories")
        val autoDry = booleanPreferencesKey("autoDry")
        val bacOn = booleanPreferencesKey("bacOn")
        val bacUnitPercent = booleanPreferencesKey("bacUnitPercent")
        val energyKcal = booleanPreferencesKey("energyKcal")
        val servingMl = booleanPreferencesKey("servingMl")
        val sex = stringPreferencesKey("sex")
        val weight = stringPreferencesKey("weight")
        val weightKgUnit = booleanPreferencesKey("weightKgUnit")
        val dailyGoal = intPreferencesKey("dailyGoal")
        val weeklyGoal = intPreferencesKey("weeklyGoal")
        val healthConnected = booleanPreferencesKey("healthConnected")
        val iconIndex = intPreferencesKey("iconIndex")
        val baseline = stringPreferencesKey("baseline")
        val tone = stringPreferencesKey("tone")
        val appLock = booleanPreferencesKey("appLock")
        val discreet = booleanPreferencesKey("discreet")
        val cutoff = stringPreferencesKey("cutoff")
        val pro = booleanPreferencesKey("pro")
        val onboardingDone = booleanPreferencesKey("onboardingDone")
        val darkChoice = stringPreferencesKey("darkChoice")
        val themeId = stringPreferencesKey("themeId")
        val goals = stringPreferencesKey("goals")
        val baselineAnswer = intPreferencesKey("baselineAnswer")
        val customerId = stringPreferencesKey("customerId")
        val lastBackupAt = longPreferencesKey("lastBackupAt")
    }

    val flow: Flow<Settings> = context.dataStore.data.map { p ->
        Settings(
            askCost = p[K.askCost] ?: true,
            showCalories = p[K.showCalories] ?: true,
            autoDry = p[K.autoDry] ?: true,
            bacOn = p[K.bacOn] ?: true,
            bacUnitPercent = p[K.bacUnitPercent] ?: true,
            energyKcal = p[K.energyKcal] ?: true,
            servingMl = p[K.servingMl] ?: true,
            sex = p[K.sex],
            weight = p[K.weight] ?: "",
            weightKgUnit = p[K.weightKgUnit] ?: true,
            dailyGoal = p[K.dailyGoal] ?: 2,
            weeklyGoal = p[K.weeklyGoal] ?: 10,
            healthConnected = p[K.healthConnected] ?: false,
            iconIndex = p[K.iconIndex] ?: 0,
            baseline = p[K.baseline] ?: "",
            tone = Tone.from(p[K.tone]),
            appLock = p[K.appLock] ?: false,
            discreet = p[K.discreet] ?: true,
            cutoff = DayCutoff.from(p[K.cutoff]),
            pro = p[K.pro] ?: false,
            onboardingDone = p[K.onboardingDone] ?: false,
            darkChoice = p[K.darkChoice]?.let { raw ->
                DarkChoice.entries.firstOrNull { it.name == raw }
            } ?: DarkChoice.SYSTEM,
            themeId = p[K.themeId] ?: "kiln",
            goals = (p[K.goals] ?: "").split(',')
                .mapNotNull { it.toIntOrNull() }.toSet(),
            baselineAnswer = p[K.baselineAnswer] ?: -1,
            customerId = p[K.customerId] ?: "",
            lastBackupAt = p[K.lastBackupAt] ?: 0L
        )
    }

    suspend fun update(transform: (Settings) -> Settings) {
        context.dataStore.edit { p ->
            val current = Settings(
                askCost = p[K.askCost] ?: true,
                showCalories = p[K.showCalories] ?: true,
                autoDry = p[K.autoDry] ?: true,
                bacOn = p[K.bacOn] ?: true,
                bacUnitPercent = p[K.bacUnitPercent] ?: true,
                energyKcal = p[K.energyKcal] ?: true,
                servingMl = p[K.servingMl] ?: true,
                sex = p[K.sex],
                weight = p[K.weight] ?: "",
                weightKgUnit = p[K.weightKgUnit] ?: true,
                dailyGoal = p[K.dailyGoal] ?: 2,
                weeklyGoal = p[K.weeklyGoal] ?: 10,
                healthConnected = p[K.healthConnected] ?: false,
                iconIndex = p[K.iconIndex] ?: 0,
                baseline = p[K.baseline] ?: "",
                tone = Tone.from(p[K.tone]),
                appLock = p[K.appLock] ?: false,
                discreet = p[K.discreet] ?: true,
                cutoff = DayCutoff.from(p[K.cutoff]),
                pro = p[K.pro] ?: false,
                onboardingDone = p[K.onboardingDone] ?: false,
                darkChoice = p[K.darkChoice]?.let { raw ->
                    DarkChoice.entries.firstOrNull { it.name == raw }
                } ?: DarkChoice.SYSTEM,
                themeId = p[K.themeId] ?: "kiln",
                goals = (p[K.goals] ?: "").split(',')
                    .mapNotNull { it.toIntOrNull() }.toSet(),
                baselineAnswer = p[K.baselineAnswer] ?: -1,
                customerId = p[K.customerId] ?: "",
                lastBackupAt = p[K.lastBackupAt] ?: 0L
            )
            val s = transform(current)
            p[K.askCost] = s.askCost
            p[K.showCalories] = s.showCalories
            p[K.autoDry] = s.autoDry
            p[K.bacOn] = s.bacOn
            p[K.bacUnitPercent] = s.bacUnitPercent
            p[K.energyKcal] = s.energyKcal
            p[K.servingMl] = s.servingMl
            if (s.sex != null) p[K.sex] = s.sex else p.remove(K.sex)
            p[K.weight] = s.weight
            p[K.weightKgUnit] = s.weightKgUnit
            p[K.dailyGoal] = s.dailyGoal
            p[K.weeklyGoal] = s.weeklyGoal
            p[K.healthConnected] = s.healthConnected
            p[K.iconIndex] = s.iconIndex
            p[K.baseline] = s.baseline
            p[K.tone] = s.tone.name
            p[K.appLock] = s.appLock
            p[K.discreet] = s.discreet
            p[K.cutoff] = s.cutoff.name
            p[K.pro] = s.pro
            p[K.onboardingDone] = s.onboardingDone
            p[K.darkChoice] = s.darkChoice.name
            p[K.themeId] = s.themeId
            p[K.goals] = s.goals.joinToString(",")
            p[K.baselineAnswer] = s.baselineAnswer
            p[K.customerId] = s.customerId
            p[K.lastBackupAt] = s.lastBackupAt
        }
    }

    /** Stable support id like AT-7F3K-92QD, generated once. */
    suspend fun ensureCustomerId() {
        update { s ->
            if (s.customerId.isNotEmpty()) s
            else {
                val alphabet = "23456789ABCDEFGHJKMNPQRSTUVWXYZ"
                val part = { (1..4).map { alphabet.random() }.joinToString("") }
                s.copy(customerId = "AT-${part()}-${part()}")
            }
        }
    }

    companion object {
        @Volatile private var instance: SettingsRepository? = null
        fun get(context: Context): SettingsRepository =
            instance ?: synchronized(this) {
                instance ?: SettingsRepository(context.applicationContext).also { instance = it }
            }
    }
}

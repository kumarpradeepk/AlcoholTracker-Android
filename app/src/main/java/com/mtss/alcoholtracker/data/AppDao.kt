package com.mtss.alcoholtracker.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface AppDao {

    // Logs
    @Query("SELECT * FROM drink_logs ORDER BY atMillis ASC")
    fun logsFlow(): Flow<List<DrinkLog>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: DrinkLog)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertLogsIgnoring(logs: List<DrinkLog>): List<Long>

    @Query("DELETE FROM drink_logs WHERE id = :id")
    suspend fun deleteLog(id: String)

    @Query("DELETE FROM drink_logs")
    suspend fun clearLogs()

    // Dry days
    @Query("SELECT * FROM dry_days")
    fun dryDaysFlow(): Flow<List<DryDay>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertDryDay(day: DryDay)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertDryDaysIgnoring(days: List<DryDay>): List<Long>

    @Query("DELETE FROM dry_days WHERE epochDay = :epochDay")
    suspend fun deleteDryDay(epochDay: Long)

    @Query("DELETE FROM dry_days")
    suspend fun clearDryDays()

    // Saved drinks
    @Query("SELECT * FROM saved_drinks ORDER BY createdAt ASC")
    fun savedDrinksFlow(): Flow<List<SavedDrink>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSavedDrink(drink: SavedDrink)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertSavedDrinksIgnoring(drinks: List<SavedDrink>): List<Long>

    @Query("UPDATE saved_drinks SET quickAccess = :on WHERE id = :id")
    suspend fun setQuickAccess(id: String, on: Boolean)

    @Query("DELETE FROM saved_drinks")
    suspend fun clearSavedDrinks()

    // Reminders
    @Query("SELECT * FROM reminders ORDER BY timeMinutes ASC")
    fun remindersFlow(): Flow<List<ReminderItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReminder(item: ReminderItem)

    @Query("DELETE FROM reminders WHERE id = :id")
    suspend fun deleteReminder(id: String)

    @Query("SELECT * FROM reminders")
    suspend fun remindersOnce(): List<ReminderItem>

    @Query("DELETE FROM reminders")
    suspend fun clearReminders()
}

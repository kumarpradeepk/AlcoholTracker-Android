package com.mtss.alcoholtracker.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

/**
 * One logged drink. `epochDay` is the *drinking day* the entry belongs to —
 * already adjusted for the user's day cut-off at the moment of logging, so a
 * 1 AM pour with a 4 AM cut-off carries the previous date. `atMillis` is the
 * real instant, which BAC math and ordering use.
 */
@Entity(tableName = "drink_logs")
data class DrinkLog(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val name: String,
    val ml: Double,
    val abv: Double,
    val atMillis: Long,
    val epochDay: Long,
    val cost: Double,
    val kcal: Int
)

/** A banked alcohol-free day. Existence of the row is the mark. */
@Entity(tableName = "dry_days")
data class DryDay(
    @PrimaryKey val epochDay: Long
)

/** A user-created drink, shown under YOUR DRINKS and available as a quick tile. */
@Entity(tableName = "saved_drinks")
data class SavedDrink(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val name: String,
    val base: String,
    val abv: Double,
    val ml: Double,
    val notes: String,
    val quickAccess: Boolean = false,
    val createdAt: Long
)

/** A daily reminder the user created. */
@Entity(tableName = "reminders")
data class ReminderItem(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val title: String,
    val timeMinutes: Int,
    val message: String
)

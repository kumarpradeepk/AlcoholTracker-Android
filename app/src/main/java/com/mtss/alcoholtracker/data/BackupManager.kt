package com.mtss.alcoholtracker.data

import android.content.Context
import android.net.Uri
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/**
 * Hand-written JSON backup: key-stable, human-readable, no serialization
 * library to shrink or break. Imports merge; duplicates (same id / same day)
 * are quietly skipped by the DAO's IGNORE strategy.
 */
class BackupManager(private val context: Context, private val dao: AppDao) {

    fun buildJson(
        logs: List<DrinkLog>,
        dryDays: List<DryDay>,
        saved: List<SavedDrink>,
        reminders: List<ReminderItem>
    ): String {
        val root = JSONObject()
        root.put("app", "com.mtss.alcoholtracker")
        root.put("version", 1)
        root.put("exportedAt", System.currentTimeMillis())
        root.put("logs", JSONArray().apply {
            logs.forEach {
                put(JSONObject().apply {
                    put("id", it.id); put("name", it.name); put("ml", it.ml); put("abv", it.abv)
                    put("atMillis", it.atMillis); put("epochDay", it.epochDay)
                    put("cost", it.cost); put("kcal", it.kcal)
                })
            }
        })
        root.put("dryDays", JSONArray().apply { dryDays.forEach { put(it.epochDay) } })
        root.put("savedDrinks", JSONArray().apply {
            saved.forEach {
                put(JSONObject().apply {
                    put("id", it.id); put("name", it.name); put("base", it.base)
                    put("abv", it.abv); put("ml", it.ml); put("notes", it.notes)
                    put("quickAccess", it.quickAccess); put("createdAt", it.createdAt)
                })
            }
        })
        root.put("reminders", JSONArray().apply {
            reminders.forEach {
                put(JSONObject().apply {
                    put("id", it.id); put("title", it.title)
                    put("timeMinutes", it.timeMinutes); put("message", it.message)
                })
            }
        })
        return root.toString(2)
    }

    /**
     * Rows actually written vs. rows the IGNORE conflict strategy dropped as
     * duplicates. Android used to report neither, so the toast could only say
     * "duplicates were quietly skipped"; the shared string wants both numbers.
     */
    data class ImportResult(val imported: Int, val skipped: Int)

    suspend fun importJson(text: String): ImportResult {
        val root = JSONObject(text)
        var imported = 0
        var skipped = 0

        fun tally(rowIds: List<Long>) {
            // Room returns -1 for a row the conflict strategy ignored.
            imported += rowIds.count { it != -1L }
            skipped += rowIds.count { it == -1L }
        }

        val logsArr = root.optJSONArray("logs") ?: JSONArray()
        val logs = (0 until logsArr.length()).map { i ->
            val o = logsArr.getJSONObject(i)
            DrinkLog(
                id = o.getString("id"), name = o.getString("name"),
                ml = o.getDouble("ml"), abv = o.getDouble("abv"),
                atMillis = o.getLong("atMillis"), epochDay = o.getLong("epochDay"),
                cost = o.optDouble("cost", 0.0), kcal = o.optInt("kcal", 0)
            )
        }
        tally(dao.insertLogsIgnoring(logs))

        val dryArr = root.optJSONArray("dryDays") ?: JSONArray()
        val dry = (0 until dryArr.length()).map { DryDay(dryArr.getLong(it)) }
        tally(dao.insertDryDaysIgnoring(dry))

        val savedArr = root.optJSONArray("savedDrinks") ?: JSONArray()
        val saved = (0 until savedArr.length()).map { i ->
            val o = savedArr.getJSONObject(i)
            SavedDrink(
                id = o.getString("id"), name = o.getString("name"),
                base = o.optString("base", "Beer"), abv = o.getDouble("abv"),
                ml = o.getDouble("ml"), notes = o.optString("notes", ""),
                quickAccess = o.optBoolean("quickAccess", false),
                createdAt = o.optLong("createdAt", System.currentTimeMillis())
            )
        }
        tally(dao.insertSavedDrinksIgnoring(saved))

        return ImportResult(imported, skipped)
    }

    /** Local backup written to app files; the Settings card shows its time. */
    suspend fun createLocalBackup(
        logs: List<DrinkLog>, dryDays: List<DryDay>,
        saved: List<SavedDrink>, reminders: List<ReminderItem>
    ): File {
        val dir = File(context.filesDir, "backups").apply { mkdirs() }
        val file = File(dir, "alcohol-tracker-backup.json")
        file.writeText(buildJson(logs, dryDays, saved, reminders))
        return file
    }

    suspend fun restoreLocalBackup(): ImportResult? {
        val file = File(File(context.filesDir, "backups"), "alcohol-tracker-backup.json")
        if (!file.exists()) return null
        return importJson(file.readText())
    }

    fun writeTo(uri: Uri, json: String) {
        context.contentResolver.openOutputStream(uri)?.use { it.write(json.toByteArray()) }
    }

    fun readFrom(uri: Uri): String? =
        context.contentResolver.openInputStream(uri)?.use { it.readBytes().decodeToString() }
}

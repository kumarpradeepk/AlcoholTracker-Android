package com.mtss.alcoholtracker.wear

import android.content.Context
import android.util.Log
import com.google.android.gms.wearable.DataEvent
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.PutDataMapRequest
import com.google.android.gms.wearable.Wearable
import com.google.android.gms.wearable.WearableListenerService
import org.json.JSONArray
import org.json.JSONObject

/**
 * The phone half of the phone/watch bridge.
 *
 * Two one-way channels rather than a shared store, because they carry
 * different things: the phone publishes a **snapshot** the watch renders, and
 * the watch appends **pours** the phone folds into its own database. Neither
 * side merges the other's state, which keeps the standard-drink maths — the
 * one number that must never disagree — in exactly one place.
 */
object WearBridge {

    const val PATH_SNAPSHOT = "/alcohol/snapshot"
    const val PATH_OUTBOX = "/alcohol/outbox"
    private const val KEY_JSON = "json"
    private const val TAG = "WearBridge"

    /** What the watch draws. Every figure is already derived and formatted. */
    data class Snapshot(
        val dayUnits: Double,
        val dailyGoal: Int,
        val weekUnits: Double,
        val weeklyGoal: Int,
        val gramsPerUnit: Double,
        val unitNoun: String,
        val dayLabel: String,
        val remainLine: String,
        val weekLine: String,
        val isDryToday: Boolean,
        val dayHasDrinks: Boolean,
        val quick: List<Quick>,
        val pro: Boolean,
        val bacOn: Boolean,
        val bacValue: String,
        val bacStatus: String,
        val bacBand: Int
    )

    data class Quick(val name: String, val ml: Double, val abv: Double, val cost: Double)

    fun publish(context: Context, s: Snapshot) {
        val json = JSONObject().apply {
            put("dayUnits", s.dayUnits)
            put("dailyGoal", s.dailyGoal)
            put("weekUnits", s.weekUnits)
            put("weeklyGoal", s.weeklyGoal)
            put("gramsPerUnit", s.gramsPerUnit)
            put("unitNoun", s.unitNoun)
            put("dayLabel", s.dayLabel)
            put("remainLine", s.remainLine)
            put("weekLine", s.weekLine)
            put("isDryToday", s.isDryToday)
            put("dayHasDrinks", s.dayHasDrinks)
            put("pro", s.pro)
            put("bacOn", s.bacOn)
            put("bacValue", s.bacValue)
            put("bacStatus", s.bacStatus)
            put("bacBand", s.bacBand)
            put("quick", JSONArray().apply {
                s.quick.forEach {
                    put(JSONObject().apply {
                        put("name", it.name); put("ml", it.ml)
                        put("abv", it.abv); put("cost", it.cost)
                    })
                }
            })
        }.toString()

        val req = PutDataMapRequest.create(PATH_SNAPSHOT).apply {
            dataMap.putString(KEY_JSON, json)
            // The Data Layer drops an item that has not changed, so a timestamp
            // is what makes "the same numbers, a minute later" still deliver.
            dataMap.putLong("ts", System.currentTimeMillis())
        }
        runCatching {
            Wearable.getDataClient(context)
                .putDataItem(req.asPutDataRequest().setUrgent())
                .addOnFailureListener { Log.w(TAG, "snapshot failed: ${it.message}") }
        }.onFailure { Log.w(TAG, "no wearable service: ${it.message}") }
    }

    /** What the watch sent back. */
    sealed interface Action {
        data class Log(val name: String, val ml: Double, val abv: Double, val cost: Double) : Action
        data object Undo : Action
        data class Dry(val value: Boolean) : Action
    }

    fun parseAction(json: String): Action? {
        val o = JSONObject(json)
        return when (o.optString("kind")) {
            "log" -> Action.Log(
                name = o.optString("name", "Drink"),
                ml = o.optDouble("ml", 0.0),
                abv = o.optDouble("abv", 0.0),
                cost = o.optDouble("cost", 0.0)
            )
            "undo" -> Action.Undo
            "dry" -> Action.Dry(o.optBoolean("value", true))
            else -> null
        }
    }
}

/**
 * Receives the watch's pours while the phone app is closed.
 *
 * The work is handed to [PendingWearActions] rather than done here: this
 * service has no view model and no database scope of its own, and a log has to
 * land through the same path as a phone log so the dry-day un-banking and the
 * BAC notification refresh happen exactly once.
 */
class WearInboxService : WearableListenerService() {
    override fun onDataChanged(events: DataEventBuffer) {
        for (e in events) {
            if (e.type != DataEvent.TYPE_CHANGED) continue
            if (e.dataItem.uri.path != WearBridge.PATH_OUTBOX) continue
            val json = DataMapItem.fromDataItem(e.dataItem).dataMap.getString("json") ?: continue
            runCatching { WearBridge.parseAction(json) }
                .getOrNull()
                ?.let { PendingWearActions.offer(it) }
        }
    }
}

/**
 * A hand-off queue between the wearable service and the view model.
 *
 * Deliberately in-memory and small: if the phone app is not running the watch's
 * action still sits on the Data Layer item, and the next launch replays it
 * through [com.mtss.alcoholtracker.ui.AppViewModel.drainWearActions].
 */
object PendingWearActions {
    private val queue = ArrayDeque<WearBridge.Action>()

    @Synchronized
    fun offer(a: WearBridge.Action) {
        queue.addLast(a)
    }

    @Synchronized
    fun drain(): List<WearBridge.Action> {
        val out = queue.toList()
        queue.clear()
        return out
    }
}

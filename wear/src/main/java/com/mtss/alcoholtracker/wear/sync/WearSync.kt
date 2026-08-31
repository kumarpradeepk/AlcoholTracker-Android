package com.mtss.alcoholtracker.wear.sync

import android.content.Context
import android.util.Log
import com.google.android.gms.wearable.DataClient
import com.google.android.gms.wearable.DataEvent
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.PutDataMapRequest
import com.google.android.gms.wearable.Wearable
import com.google.android.gms.wearable.WearableListenerService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject

/**
 * The phone/watch bridge.
 *
 * Two one-way channels rather than a shared store, because they carry different
 * things: the phone publishes a **snapshot** the watch renders, and the watch
 * appends **pours** the phone folds into its own database. Neither side tries
 * to merge the other's state, which is what keeps the standard-drink maths in
 * exactly one place.
 */
object WearSync {

    /** Phone -> watch: the rendered figures. */
    const val PATH_SNAPSHOT = "/alcohol/snapshot"

    /** Watch -> phone: pours and dry-day marks the watch originated. */
    const val PATH_OUTBOX = "/alcohol/outbox"

    private const val KEY_JSON = "json"
    private const val TAG = "WearSync"

    private val _state = MutableStateFlow(WearSnapshot())
    val state: StateFlow<WearSnapshot> = _state.asStateFlow()

    fun publishLocally(snapshot: WearSnapshot) {
        _state.value = snapshot
    }

    // ── Reading the phone's snapshot ─────────────────────────────────────

    fun parse(json: String): WearSnapshot {
        val o = JSONObject(json)
        val quick = o.optJSONArray("quick") ?: JSONArray()
        return WearSnapshot(
            dayUnits = o.optDouble("dayUnits", 0.0),
            dailyGoal = o.optInt("dailyGoal", 2),
            weekUnits = o.optDouble("weekUnits", 0.0),
            weeklyGoal = o.optInt("weeklyGoal", 10),
            gramsPerUnit = o.optDouble("gramsPerUnit", 14.0),
            unitNoun = o.optString("unitNoun", "standard drinks"),
            dayLabel = o.optString("dayLabel", ""),
            remainLine = o.optString("remainLine", ""),
            weekLine = o.optString("weekLine", ""),
            isDryToday = o.optBoolean("isDryToday", false),
            dayHasDrinks = o.optBoolean("dayHasDrinks", false),
            pro = o.optBoolean("pro", false),
            bacOn = o.optBoolean("bacOn", false),
            bacValue = o.optString("bacValue", ""),
            bacStatus = o.optString("bacStatus", ""),
            bacBand = o.optInt("bacBand", 0),
            quick = (0 until quick.length()).map { i ->
                val q = quick.getJSONObject(i)
                QuickDrink(
                    name = q.optString("name"),
                    ml = q.optDouble("ml", 0.0),
                    abv = q.optDouble("abv", 0.0),
                    cost = q.optDouble("cost", 0.0)
                )
            }
        )
    }

    fun onDataChanged(events: DataEventBuffer) {
        for (e in events) {
            if (e.type != DataEvent.TYPE_CHANGED) continue
            if (e.dataItem.uri.path != PATH_SNAPSHOT) continue
            val json = DataMapItem.fromDataItem(e.dataItem).dataMap.getString(KEY_JSON) ?: continue
            runCatching { _state.value = parse(json) }
                .onFailure { Log.w(TAG, "bad snapshot: ${it.message}") }
        }
    }

    /** Reads whatever snapshot is already on the node, for a cold launch. */
    fun hydrate(context: Context) {
        Wearable.getDataClient(context).dataItems.addOnSuccessListener { buffer ->
            buffer.forEach { item ->
                if (item.uri.path == PATH_SNAPSHOT) {
                    val json = DataMapItem.fromDataItem(item).dataMap.getString(KEY_JSON)
                    if (json != null) runCatching { _state.value = parse(json) }
                }
            }
            buffer.release()
        }
    }

    // ── Sending the watch's own actions ──────────────────────────────────

    /**
     * Appends an action to the outbox.
     *
     * Every entry carries a UUID and the whole outbox is re-sent as one data
     * item, because the Data Layer is last-write-wins and drops items that do
     * not change — an id-per-entry is what stops two identical pours a minute
     * apart from collapsing into one.
     */
    fun send(context: Context, action: JSONObject) {
        val client: DataClient = Wearable.getDataClient(context)
        action.put("id", java.util.UUID.randomUUID().toString())
        action.put("at", System.currentTimeMillis())
        val req = PutDataMapRequest.create(PATH_OUTBOX).apply {
            dataMap.putString(KEY_JSON, action.toString())
            dataMap.putLong("ts", System.currentTimeMillis())
        }
        client.putDataItem(req.asPutDataRequest().setUrgent())
            .addOnFailureListener { Log.w(TAG, "outbox failed: ${it.message}") }
    }

    fun sendLog(context: Context, name: String, ml: Double, abv: Double, cost: Double) =
        send(context, JSONObject().apply {
            put("kind", "log")
            put("name", name)
            put("ml", ml)
            put("abv", abv)
            put("cost", cost)
        })

    fun sendUndo(context: Context) =
        send(context, JSONObject().apply { put("kind", "undo") })

    fun sendDry(context: Context, dry: Boolean) =
        send(context, JSONObject().apply {
            put("kind", "dry")
            put("value", dry)
        })
}

/** Wakes on a snapshot from the phone, even when the watch app is closed. */
class WearSyncService : WearableListenerService() {
    override fun onDataChanged(dataEvents: DataEventBuffer) {
        WearSync.onDataChanged(dataEvents)
    }
}

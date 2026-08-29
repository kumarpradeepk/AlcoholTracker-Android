package com.mtss.alcoholtracker.util

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.mtss.alcoholtracker.data.DrinkLog
import com.mtss.alcoholtracker.domain.AlcoholMath
import java.io.File
import java.time.LocalDate
import java.util.Locale

object CsvExport {

    /** Writes the statistics CSV and hands back a share intent. Export is free, always. */
    fun share(context: Context, fileLabel: String, logs: List<DrinkLog>): Intent {
        val dir = File(context.cacheDir, "exports").apply { mkdirs() }
        val safe = fileLabel.replace(Regex("[\\\\/:*?\"<>|]"), "-")
        val file = File(dir, "$safe.csv")
        file.bufferedWriter().use { w ->
            w.appendLine("date,time,drink,volume_ml,abv_percent,units,kcal,cost")
            logs.forEach { log ->
                val date = LocalDate.ofEpochDay(log.epochDay)
                val mins = java.time.Instant.ofEpochMilli(log.atMillis)
                    .atZone(java.time.ZoneId.systemDefault())
                // CSV is machine-facing: ROOT locale throughout, so a German or
                // French device does not emit comma decimals into a comma-separated
                // file. The column headers stay English for the same reason.
                val time = "%02d:%02d".format(Locale.ROOT, mins.hour, mins.minute)
                val units = "%.2f".format(Locale.ROOT, AlcoholMath.units(log.ml, log.abv))
                val cost = "%.2f".format(Locale.ROOT, log.cost)
                val name = "\"" + log.name.replace("\"", "\"\"") + "\""
                w.appendLine("$date,$time,$name,${log.ml.toInt()},${log.abv},$units,${log.kcal},$cost")
            }
        }
        val uri = FileProvider.getUriForFile(context, context.packageName + ".fileprovider", file)
        return Intent(Intent.ACTION_SEND).apply {
            type = "text/csv"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }
}

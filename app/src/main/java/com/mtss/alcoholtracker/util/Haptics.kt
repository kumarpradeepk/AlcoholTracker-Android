package com.mtss.alcoholtracker.util

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

/**
 * Small, deliberate haptics: a light tick for selection, a firmer thud when a
 * log lands, and a soft double-pulse for the dry-day mark. Never on failure.
 */
object Haptics {

    private fun vibrator(context: Context): Vibrator =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            (context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager).defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }

    fun tick(context: Context) {
        vibrator(context).vibrate(VibrationEffect.createOneShot(12, 60))
    }

    fun logged(context: Context) {
        vibrator(context).vibrate(VibrationEffect.createOneShot(28, 140))
    }

    fun dryDay(context: Context) {
        vibrator(context).vibrate(
            VibrationEffect.createWaveform(longArrayOf(0, 24, 70, 32), intArrayOf(0, 120, 0, 180), -1)
        )
    }
}

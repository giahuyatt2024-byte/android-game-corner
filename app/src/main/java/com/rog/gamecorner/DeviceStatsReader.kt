package com.rog.gamecorner

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.app.ActivityManager
import android.os.BatteryManager
import android.hardware.display.DisplayManager
import android.view.Display
import kotlin.math.roundToInt

/**
 * Reads values exposed by Android's public APIs.
 *
 * RAM is process-visible device memory, temperature is battery temperature,
 * and the FPS value is the physical display refresh rate. Android does not
 * expose another app's internal render FPS or CPU temperature to a normal
 * third-party overlay without privileged/OEM APIs.
 */
data class DeviceStats(
    val ramText: String,
    val temperatureText: String,
    val refreshRateText: String,
)

class DeviceStatsReader(context: Context) {
    private val appContext = context.applicationContext
    private val activityManager =
        appContext.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
    private val displayManager =
        appContext.getSystemService(Context.DISPLAY_SERVICE) as DisplayManager

    fun read(): DeviceStats {
        val memoryInfo = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memoryInfo)
        val totalMb = memoryInfo.totalMem / MB
        val availableMb = memoryInfo.availMem / MB
        val usedMb = (totalMb - availableMb).coerceAtLeast(0L)
        val ramText = if (totalMb > 0) {
            "${usedMb.toInt()}/${totalMb.toInt()}M"
        } else {
            "--"
        }

        val batteryIntent = appContext.registerReceiver(
            null,
            IntentFilter(Intent.ACTION_BATTERY_CHANGED),
        )
        val temperatureTenths = batteryIntent?.getIntExtra(
            BatteryManager.EXTRA_TEMPERATURE,
            Int.MIN_VALUE,
        ) ?: Int.MIN_VALUE
        val temperatureText = if (temperatureTenths != Int.MIN_VALUE) {
            "${(temperatureTenths / 10f).roundToInt()}°C"
        } else {
            "--"
        }

        val display = displayManager.getDisplay(Display.DEFAULT_DISPLAY)
        val refreshRate = display?.refreshRate?.roundToInt() ?: 0
        val refreshRateText = if (refreshRate > 0) "${refreshRate}Hz" else "--"

        return DeviceStats(
            ramText = ramText,
            temperatureText = temperatureText,
            refreshRateText = refreshRateText,
        )
    }

    companion object {
        private const val MB = 1024L * 1024L
    }
}
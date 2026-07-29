package com.example.retroachivementscompanion

import android.app.AppOpsManager
import android.app.usage.UsageStatsManager
import android.content.Context
import android.os.Process
import android.util.Log

class ForegroundAppWatcher(
    private val context: Context,
    private val onChange: (Boolean) -> Unit
) {
    private var running = true
    private val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
    private var lastState: Boolean = false

    fun start() {
        Thread {
            Log.d("AppWatcher", "Watcher thread started")
            while (running) {
                if (hasUsageStatsPermission()) {
                    checkForeground()
                } else {
                    if (lastState) {
                        Log.d("AppWatcher", "Permission lost, disabling HUD")
                        onChange(false)
                        lastState = false
                    }
                }
                Thread.sleep(1500)
            }
        }.start()
    }

    fun stop() {
        running = false
    }

    fun hasUsageStatsPermission(): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = appOps.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), context.packageName)
        return mode == AppOpsManager.MODE_ALLOWED
    }

    private fun getTrackedPackages(): Set<String> {
        val prefs = context.getSharedPreferences("companion_prefs", Context.MODE_PRIVATE)
        val json = prefs.getString("tracked_packages", "[]") ?: "[]"
        return json.trim()
            .removeSurrounding("[", "]")
            .split(",")
            .map { it.trim().removeSurrounding("\"") }
            .filter { it.isNotEmpty() }
            .toSet()
    }

    private fun checkForeground() {
        val now = System.currentTimeMillis()
        // Query daily stats for the last 30 seconds to find the top app
        val stats = usageStatsManager.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, now - 30000, now)
        if (stats == null || stats.isEmpty()) return

        val currentTop = stats.maxByOrNull { it.lastTimeUsed }?.packageName ?: return
        
        val tracked = getTrackedPackages()
        val isActive = tracked.contains(currentTop)

        if (isActive != lastState) {
            Log.d("AppWatcher", "Local App State changed: $isActive (Current Top: $currentTop)")
            onChange(isActive)
            lastState = isActive
        }
    }
}

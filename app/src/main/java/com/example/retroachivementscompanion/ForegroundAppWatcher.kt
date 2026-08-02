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
    private var shizukuSupport: ShizukuSupport? = null

    fun getShizukuSupport(): ShizukuSupport? = shizukuSupport

    fun start() {
        shizukuSupport = ShizukuSupport(context, {
            // State changed (detected or permission granted)
            onChange(lastState) // Trigger a UI refresh via the existing callback
        }) { packageName ->
            checkTrackedPackage(packageName)
        }
        shizukuSupport?.init()

        Thread {
            Log.d("AppWatcher", "Watcher thread started")
            while (running) {
                // Perform foreground check using reliable UsageStats.
                // We always do this for now to ensure the HUD trigger (Rich Presence) works perfectly,
                // while Shizuku is used for the heavy telemetry work.
                if (hasUsageStatsPermission()) {
                    checkForeground()
                } else if (lastState) {
                    Log.d("AppWatcher", "Permission lost, disabling HUD")
                    onChange(false)
                    lastState = false
                }
                
                // Adaptive Sleep: 1.5s when active, 3s when idle.
                val sleepTime = if (lastState) 1500L else 3000L
                Thread.sleep(sleepTime)
            }
        }.start()
    }

    private fun checkTrackedPackage(packageName: String?) {
        val tracked = getTrackedPackages()
        val isActive = tracked.contains(packageName)
        Log.d("AppWatcher", "Checking package: $packageName | Tracked: ${tracked.joinToString(",")} | Active: $isActive")
        if (isActive != lastState) {
            Log.d("AppWatcher", "Local App State changed: $isActive (Current Top: $packageName)")
            onChange(isActive)
            lastState = isActive
        }
    }

    fun stop() {
        running = false
        shizukuSupport?.stop()
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
        val stats = usageStatsManager.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, now - 30000, now)
        if (stats == null || stats.isEmpty()) return

        val currentTop = stats.maxByOrNull { it.lastTimeUsed }?.packageName ?: return
        checkTrackedPackage(currentTop)
    }
}

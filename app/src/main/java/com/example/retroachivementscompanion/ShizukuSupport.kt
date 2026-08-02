package com.example.retroachivementscompanion

import android.app.IProcessObserver
import android.content.Context
import android.content.pm.PackageManager
import android.os.IBinder
import android.util.Log
import rikka.shizuku.Shizuku
import rikka.shizuku.ShizukuBinderWrapper
import rikka.shizuku.SystemServiceHelper

class ShizukuSupport(
    private val context: Context,
    private val onStateChanged: () -> Unit = {},
    private val onForegroundChanged: (String?) -> Unit
) {
    private var isObserverRegistered = false
    
    private val processObserver = object : IProcessObserver.Stub() {
        override fun onForegroundActivitiesChanged(pid: Int, uid: Int, foregroundActivities: Boolean) {
            if (foregroundActivities) {
                val packageName = getPackageNameForUid(uid)
                onForegroundChanged(packageName)
            }
        }
        override fun onForegroundServicesChanged(pid: Int, uid: Int, serviceTypes: Int) {}
        override fun onProcessDied(pid: Int, uid: Int) {}
    }

    private val permissionListener = Shizuku.OnRequestPermissionResultListener { _, grantResult ->
        if (grantResult == PackageManager.PERMISSION_GRANTED) {
            onStateChanged()
        }
    }

    private val binderReceivedListener = Shizuku.OnBinderReceivedListener {
        Log.d("ShizukuSupport", "Binder received via listener")
        onStateChanged()
    }

    fun init() {
        Log.d("ShizukuSupport", "Initializing ShizukuSupport...")
        Shizuku.addBinderReceivedListener(binderReceivedListener)
        Shizuku.addRequestPermissionResultListener(permissionListener)
        
        if (isShizukuAvailable()) {
            if (!hasPermission()) {
                try {
                    Log.d("ShizukuSupport", "Requesting Shizuku permission...")
                    Shizuku.requestPermission(0)
                } catch (e: Exception) {
                    Log.e("ShizukuSupport", "Failed to request permission", e)
                }
            }
        }
        onStateChanged()
    }

    fun isShizukuAvailable(): Boolean {
        return try {
            Shizuku.pingBinder()
        } catch (e: Exception) {
            false
        }
    }

    fun hasPermission(): Boolean {
        return try {
            if (Shizuku.isPreV11()) false
            else Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
        } catch (e: Exception) {
            false
        }
    }

    fun stop() {
        Shizuku.removeRequestPermissionResultListener(permissionListener)
        Shizuku.removeBinderReceivedListener(binderReceivedListener)
    }

    private fun getPackageNameForUid(uid: Int): String? {
        return try {
            context.packageManager.getPackagesForUid(uid)?.firstOrNull()
        } catch (e: Exception) {
            null
        }
    }

    fun runShell(command: String): String {
        return try {
            val process = Shizuku.newProcess(arrayOf("sh", "-c", command), null, null)
            val output = process.inputStream.bufferedReader().use { it.readText() }
            process.waitFor()
            output
        } catch (e: Exception) {
            Log.e("ShizukuSupport", "Failed to run shell: $command", e)
            ""
        }
    }
}

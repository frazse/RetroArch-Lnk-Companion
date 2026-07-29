package com.example.retroachivementscompanion

import android.content.Context
import android.os.BatteryManager
import java.io.File

data class TelemetrySnapshot(
    val cpu_util: Int? = null,
    val gpu_util: Int? = null,
    val temp_cpu: Int? = null,
    val temp_gpu: Int? = null,
    val battery: Int? = null,
    val temp_battery: Int? = null,
    val fps: Int? = null,
    val frametime: Double? = null
)

class TelemetryReader(private val context: Context) {
    private val reader = DefaultPrivilegedSysfsReader()
    private val batteryManager = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
    
    private var lastCpuStats: Map<Int, Pair<Long, Long>>? = null
    private var cpuTempZone: String? = null
    private var gpuTempZone: String? = null
    private var gpuRoot: String? = null

    private var lastVsyncCount: Long? = null
    private var lastVsyncTime: Long = 0

    init {
        resolveZones()
        resolveGpuRoot()
    }

    private fun safeRead(path: String, privileged: Boolean = false): String? {
        return try {
            val f = File(path)
            if (f.exists() && f.canRead()) f.readText().trim()
            else if (privileged) reader.readText(path)
            else null
        } catch (e: Exception) {
            if (privileged) reader.readText(path) else null
        }
    }

    private fun resolveZones() {
        for (i in 0..90) {
            val path = "/sys/class/thermal/thermal_zone$i"
            val type = safeRead("$path/type", true)?.lowercase() ?: continue
            if (cpuTempZone == null && type.contains("cpu")) cpuTempZone = path
            if (gpuTempZone == null && (type.contains("gpu") || type.contains("kgsl") || type.contains("gfx"))) gpuTempZone = path
            if (cpuTempZone != null && gpuTempZone != null) break
        }
    }

    private fun resolveGpuRoot() {
        val paths = listOf("/sys/class/kgsl/kgsl-3d0", "/sys/devices/platform/soc@0/3d00000.gpu/kgsl/kgsl-3d0")
        for (p in paths) {
            if (safeRead("$p/max_pwrlevel", true) != null) {
                gpuRoot = p
                break
            }
        }
    }

    fun poll(): TelemetrySnapshot {
        val cpu = pollCpu()
        val gpu = pollGpu()
        val cpuTemp = pollTemp(cpuTempZone)
        val gpuTemp = pollTemp(gpuTempZone)
        val batt = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
        val battTempStr = safeRead("/sys/class/power_supply/battery/temp") ?: safeRead("/sys/class/power_supply/battery/batt_temp")
        val battTemp = battTempStr?.toIntOrNull()?.let { if (it > 1000) it / 1000 else it }

        val (fps, ft) = pollFps()

        return TelemetrySnapshot(
            cpu_util = cpu,
            gpu_util = gpu,
            temp_cpu = cpuTemp,
            temp_gpu = gpuTemp,
            battery = if (batt > 0) batt else null,
            temp_battery = battTemp,
            fps = fps,
            frametime = ft
        )
    }

    private fun pollCpu(): Int? {
        val lines = try {
            val f = File("/proc/stat")
            if (f.exists() && f.canRead()) f.readLines()
            else reader.readText("/proc/stat")?.lines() ?: emptyList()
        } catch (e: Exception) {
            reader.readText("/proc/stat")?.lines() ?: emptyList()
        }

        val currentStats = mutableMapOf<Int, Pair<Long, Long>>()
        val cpuRegex = Regex("^cpu(\\d+)\\s+(.*)$")
        
        lines.forEach { line ->
            val match = cpuRegex.find(line)
            if (match != null) {
                val coreIdx = match.groupValues[1].toInt()
                val parts = match.groupValues[2].trim().split(Regex("\\s+")).map { it.toLong() }
                if (parts.size >= 7) {
                    val user = parts[0]
                    val nice = parts[1]
                    val system = parts[2]
                    val idle = parts[3]
                    val iowait = parts[4]
                    val irq = parts[5]
                    val softirq = parts[6]
                    val steal = if (parts.size > 7) parts[7] else 0L
                    
                    val total = user + nice + system + idle + iowait + irq + softirq + steal
                    val busy = total - (idle + iowait)
                    currentStats[coreIdx] = Pair(busy, total)
                }
            }
        }

        val last = lastCpuStats
        lastCpuStats = currentStats
        if (last == null || currentStats.isEmpty()) return null

        var totalBusyFraction = 0.0
        var count = 0
        currentStats.forEach { (idx, current) ->
            val prev = last[idx] ?: return@forEach
            val deltaTotal = current.second - prev.second
            val deltaBusy = current.first - prev.first
            if (deltaTotal > 0) {
                totalBusyFraction += deltaBusy.toDouble() / deltaTotal.toDouble()
                count++
            }
        }

        return if (count > 0) ( (totalBusyFraction / count) * 100 ).toInt().coerceIn(0, 100) else null
    }

    private fun pollGpu(): Int? {
        val root = gpuRoot ?: return null
        val busyStr = safeRead("$root/gpu_busy_percentage", true) ?: return null
        return Regex("^(\\d+)").find(busyStr)?.groupValues?.get(1)?.toIntOrNull()
    }

    private fun pollTemp(zone: String?): Int? {
        if (zone == null) return null
        val tempStr = safeRead("$zone/temp", true) ?: return null
        val raw = tempStr.toIntOrNull() ?: return null
        val temp = if (raw > 1000) raw / 1000 else raw
        return if (temp in 1..150) temp else null
    }

    private fun pollFps(): Pair<Int?, Double?> {
        val out = RootSupport.runRootCommand("dumpsys SurfaceFlinger") ?: return Pair(null, null)
        val vsyncLine = out.lines().find { it.contains("VSyncState") && it.contains("count=") } ?: return Pair(null, null)
        val currentCount = Regex("count=(\\d+)").find(vsyncLine)?.groupValues?.get(1)?.toLongOrNull() ?: return Pair(null, null)
        val currentTime = System.currentTimeMillis()

        val lastCount = lastVsyncCount
        val lastTime = lastVsyncTime
        lastVsyncCount = currentCount
        lastVsyncTime = currentTime

        if (lastCount == null || lastTime == 0L) return Pair(null, null)

        val deltaCount = currentCount - lastCount
        val deltaTime = currentTime - lastTime

        if (deltaTime <= 0 || deltaCount < 0) return Pair(null, null)

        val fps = (deltaCount.toDouble() / (deltaTime.toDouble() / 1000.0)).toInt().coerceIn(0, 240)
        val ft = if (fps > 0) (1000.0 / fps) else 0.0
        
        return Pair(fps, Math.round(ft * 10.0) / 10.0)
    }
}

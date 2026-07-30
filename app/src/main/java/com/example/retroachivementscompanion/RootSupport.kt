package com.example.retroachivementscompanion

import java.io.File
import java.util.concurrent.locks.ReentrantLock
import android.content.Context
import android.util.Log

object RootSupport {
    private val lock = ReentrantLock()

    fun runRootCommand(command: String): String? {
        lock.lock()
        return try {
            RootExec.executeAsRoot(command).getOrNull()
        } finally {
            lock.unlock()
        }
    }

    fun runGeneratedScript(context: Context, scriptName: String, scriptContents: String): String? {
        context.filesDir.setExecutable(true, false)
        val scriptsDir = File(context.filesDir, "root-scripts")
        if (!scriptsDir.exists()) scriptsDir.mkdirs()
        scriptsDir.setExecutable(true, false)
        
        val scriptFile = File(scriptsDir, scriptName)
        try {
            scriptFile.writeText(scriptContents)
            scriptFile.setReadable(true, false)
            scriptFile.setExecutable(true, false)
            return runRootCommand("sh ${scriptFile.absolutePath}")
        } catch (e: Exception) {
            return null
        }
    }
}

fun interface PrivilegedSysfsReader {
    fun readText(path: String): String?
}

class DefaultPrivilegedSysfsReader : PrivilegedSysfsReader {
    override fun readText(path: String): String? {
        val escapedPath = path.replace("'", "'\\''")
        return RootSupport.runRootCommand("cat '$escapedPath' 2>/dev/null")
    }
}

package com.example.retroachivementscompanion

import java.util.concurrent.locks.ReentrantLock

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

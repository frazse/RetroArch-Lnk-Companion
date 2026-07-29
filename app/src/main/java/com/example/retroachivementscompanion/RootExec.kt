package com.example.retroachivementscompanion

import android.os.IBinder
import android.os.Parcel
import android.util.Log

object RootExec {
    private const val TAG = "RootExec"
    private const val PSERVER_INTERFACE = "PServerBinder"
    private const val TRANSACTION_EXECUTE = IBinder.FIRST_CALL_TRANSACTION

    fun executeAsRoot(cmd: String): Result<String?> {
        return try {
            val serviceManagerClass = Class.forName("android.os.ServiceManager")
            val getServiceMethod = serviceManagerClass.getMethod("getService", String::class.java)
            val binder = getServiceMethod.invoke(null, PSERVER_INTERFACE) as? IBinder
                ?: return Result.failure(Exception("PServerBinder not found"))

            val data = Parcel.obtain()
            val reply = Parcel.obtain()
            try {
                data.writeInterfaceToken(PSERVER_INTERFACE)
                // Pulse uses writeStringArray(arrayOf(cmd, "1"))
                data.writeStringArray(arrayOf(cmd, "1"))
                
                binder.transact(TRANSACTION_EXECUTE, data, reply, 0)
                reply.readException()
                
                val resultBytes = reply.createByteArray()
                if (resultBytes == null) {
                    Result.success(null)
                } else {
                    val out = String(resultBytes).trim()
                    if (out == "null") Result.success(null)
                    else Result.success(out)
                }
            } finally {
                data.recycle()
                reply.recycle()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error executing root command: ${e.message}")
            Result.failure(e)
        }
    }
}

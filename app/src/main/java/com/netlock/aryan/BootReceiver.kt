package com.netlock.aryan

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.content.ContextCompat

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        val prefs = context.getSharedPreferences(Prefs.FILE, Context.MODE_PRIVATE)
        if (prefs.getBoolean(Prefs.START_ON_BOOT, true) &&
            prefs.getBoolean(Prefs.MONITORING_ENABLED, true)
        ) {
            try {
                ContextCompat.startForegroundService(context, Intent(context, NetLockService::class.java))
            } catch (e: Exception) {
                Log.d("NetLock", "Boot start failed: ${e.message}")
            }
        }
    }
}
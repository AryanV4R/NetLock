package com.netlock.aryan

import android.app.PendingIntent
import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Intent
import android.graphics.drawable.Icon
import android.os.Build
import android.provider.Settings
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import android.util.Log
import android.telephony.SubscriptionManager
import android.telephony.TelephonyCallback
import android.telephony.TelephonyDisplayInfo
import android.telephony.TelephonyManager
import android.Manifest
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.IconCompat
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface

class NetworkTypeTileService : TileService() {
    
    private var telephonyManager: TelephonyManager? = null
    private var telephonyCallback: TelephonyCallback? = null

    override fun onStartListening() {
        super.onStartListening()
        qsTile?.apply {
            label = "Network Type"
            icon = generateTileIcon("··")
            state = Tile.STATE_INACTIVE
            updateTile()
        }
        startTrackingNetworkType()
    }

    override fun onStopListening() {
        super.onStopListening()
        stopTrackingNetworkType()
    }

    override fun onClick() {
        super.onClick()
        openPreferredNetworkTypeSettings()
    }
        private fun resolveSim1SubscriptionId(): Int {
        return try {
            getSystemService(SubscriptionManager::class.java)
                ?.activeSubscriptionInfoList
                ?.firstOrNull { it.simSlotIndex == 0 }
                ?.subscriptionId
                ?: SubscriptionManager.INVALID_SUBSCRIPTION_ID
        } catch (e: SecurityException) {
            SubscriptionManager.INVALID_SUBSCRIPTION_ID
        }
    }
    
    private fun startTrackingNetworkType() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE)
            != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        val manager = getSystemService(TELEPHONY_SERVICE) as TelephonyManager
        telephonyManager = manager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val callback = object : TelephonyCallback(), TelephonyCallback.DisplayInfoListener {
                override fun onDisplayInfoChanged(displayInfo: TelephonyDisplayInfo) {
                    updateTileLabel(shortNetworkLabel(displayInfo))
                }
            }
            telephonyCallback = callback
            try {
                manager.registerTelephonyCallback(mainExecutor, callback)
            } catch (e: SecurityException) {
                Log.d("NetLock", "Tile: unable to register telephony callback: ${e.message}")
            }
        } else {
            try {
                @Suppress("DEPRECATION")
                updateTileLabel(legacyShortLabel(manager.dataNetworkType))
            } catch (e: SecurityException) {
                Log.d("NetLock", "Tile: unable to read network type: ${e.message}")
            }
        }
    }

    private fun stopTrackingNetworkType() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            telephonyCallback?.let { telephonyManager?.unregisterTelephonyCallback(it) }
        }
        telephonyCallback = null
        telephonyManager = null
    }

    private fun updateTileLabel(shortLabel: String) {
        qsTile?.apply {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                subtitle = shortLabel
            } else {
                label = "Network: $shortLabel"
            }
            icon = generateTileIcon(shortLabel)
            state = if (shortLabel == "5G") Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
            updateTile()
        }
    }

    private fun shortNetworkLabel(info: TelephonyDisplayInfo): String {
        return when (info.overrideNetworkType) {
            TelephonyDisplayInfo.OVERRIDE_NETWORK_TYPE_NR_ADVANCED,
            TelephonyDisplayInfo.OVERRIDE_NETWORK_TYPE_NR_NSA -> "5G"
            else -> if (info.networkType == TelephonyManager.NETWORK_TYPE_NR) {
                "5G"
            } else {
                legacyShortLabel(info.networkType)
            }
        }
    }

    private fun legacyShortLabel(type: Int): String {
        return when (type) {
            TelephonyManager.NETWORK_TYPE_NR -> "5G"
            TelephonyManager.NETWORK_TYPE_LTE -> "4G"
            TelephonyManager.NETWORK_TYPE_HSPAP,
            TelephonyManager.NETWORK_TYPE_HSDPA,
            TelephonyManager.NETWORK_TYPE_HSUPA,
            TelephonyManager.NETWORK_TYPE_HSPA,
            TelephonyManager.NETWORK_TYPE_UMTS,
            TelephonyManager.NETWORK_TYPE_EVDO_0,
            TelephonyManager.NETWORK_TYPE_EVDO_A,
            TelephonyManager.NETWORK_TYPE_EVDO_B -> "3G"
            TelephonyManager.NETWORK_TYPE_GPRS,
            TelephonyManager.NETWORK_TYPE_EDGE,
            TelephonyManager.NETWORK_TYPE_CDMA,
            TelephonyManager.NETWORK_TYPE_1xRTT,
            TelephonyManager.NETWORK_TYPE_IDEN -> "2G"
            else -> "—"
        }
    }
    private fun openPreferredNetworkTypeSettings() {
        val subId = resolveSim1SubscriptionId()
        val hasSubId = subId != SubscriptionManager.INVALID_SUBSCRIPTION_ID

        // Best-effort: jump straight into the "Preferred network type"
        // fragment for SIM 1. Relies on internal Settings class names that
        // vary by MIUI/ROM version — may not resolve on every build, hence
        // wrapped in resolveActivity() checks with a safe fallback below.
        val deepLinkIntents = if (hasSubId) {
            listOf(
                // Confirmed via Activity Finder
                Intent().setComponent(
                    ComponentName("com.android.phone", "com.android.phone.settings.PreferredNetworkTypeListPreference")
                ).apply {
                    putExtra(Settings.EXTRA_SUB_ID, subId)
                },
                Intent().setComponent(
                    ComponentName("com.android.settings", "com.android.settings.Settings\$NetworkSelectSettingsActivity")
                ).apply {
                    putExtra(Settings.EXTRA_SUB_ID, subId)
                },
                Intent().setComponent(
                    ComponentName("com.android.settings", "com.android.settings.Settings\$MobileNetworkActivity")
                ).apply {
                    putExtra(Settings.EXTRA_SUB_ID, subId)
                }
            )
        } else {
            emptyList()
        }

        for (intent in deepLinkIntents) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            try {
                if (packageManager.resolveActivity(intent, 0)?.activityInfo?.exported == true) {
                    launchAndCollapse(intent)
                    return
                }
            } catch (e: Exception) {
                // try the next candidate
            }
        }

        // Reliable fallback: standard public API + EXTRA_SUB_ID — this
        // skips the SIM-picker page entirely and opens SIM 1's own network
        // settings page directly (one tap short of "Preferred network type").
        val fallback = Intent(Settings.ACTION_NETWORK_OPERATOR_SETTINGS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            if (hasSubId) {
                putExtra(Settings.EXTRA_SUB_ID, subId)
            }
        }
        try {
            launchAndCollapse(fallback)
        } catch (e: ActivityNotFoundException) {
            Log.d("NetLock", "Network operator settings not available: ${e.message}")
        }
    }

    @Suppress("DEPRECATION")
    private fun launchAndCollapse(intent: Intent) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            val pendingIntent = PendingIntent.getActivity(
                this, 0, intent, PendingIntent.FLAG_IMMUTABLE
            )
            startActivityAndCollapse(pendingIntent)
        } else {
            startActivityAndCollapse(intent)
        }
    }

    private fun generateTileIcon(text: String): Icon {
        val size = 96
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.WHITE
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create(Typeface.DEFAULT_BOLD, Typeface.BOLD)
            textSize = size * 1.0f
        }
        val maxWidth = size * 1.0f
        val measuredWidth = paint.measureText(text)
        if (measuredWidth > maxWidth) {
            paint.textSize *= maxWidth / measuredWidth
        }
        val yPos = (canvas.height / 2f) - ((paint.descent() + paint.ascent()) / 2f)
        canvas.drawText(text, canvas.width / 2f, yPos, paint)
        return Icon.createWithBitmap(bitmap)
    }
}
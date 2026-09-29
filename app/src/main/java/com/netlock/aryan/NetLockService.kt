package com.netlock.aryan

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import android.os.Build
import android.os.IBinder
import androidx.core.content.edit
import androidx.core.graphics.createBitmap
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.os.VibrationAttributes
import android.media.AudioAttributes
import android.provider.Settings
import android.telephony.CellInfo
import android.telephony.CellSignalStrengthLte
import android.telephony.CellSignalStrengthNr
import android.telephony.ServiceState
import android.telephony.SignalStrength
import android.telephony.SubscriptionManager
import android.telephony.TelephonyCallback
import android.telephony.TelephonyDisplayInfo
import android.telephony.TelephonyManager
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.IconCompat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

class NetLockService : Service() {

    private val networkType = NetLockState.networkType
    private val lastSwitchTime = NetLockState.lastSwitchTime
    private val vibrateEnabled = NetLockState.vibrateEnabled
    private val soundEnabled = NetLockState.soundEnabled
    private val selectedSimSlot = NetLockState.selectedSimSlot
    private val sim1Available = NetLockState.sim1Available
    private val sim2Available = NetLockState.sim2Available
    private val trackingLabel = NetLockState.trackingLabel
    private val isAirplaneModeOn = NetLockState.isAirplaneModeOn
    private val alertOn3G2G = NetLockState.alertOn3G2G
    private val eventLog = NetLockState.eventLog
    private val totalSwitchCount = NetLockState.totalSwitchCount
    private val carrierOperatorName = NetLockState.carrierOperatorName
    private val carrierMccMnc = NetLockState.carrierMccMnc
    private val rsrpValue = NetLockState.rsrpValue
    private val rsrqValue = NetLockState.rsrqValue
    private val switchCountLog = NetLockState.switchCountLog
    private val bypassDnd = NetLockState.bypassDnd
    private lateinit var telephonyManager: TelephonyManager
    private lateinit var prefs: SharedPreferences
    private var monitoredTelephonyManager: TelephonyManager? = null
    private var telephonyCallback: TelephonyCallback? = null
    private var previousNetworkType: String? = null
    private var lastDisplayInfo: TelephonyDisplayInfo? = null
    private var baselineNextReading = true
    private var monitoredSubId: Int = SubscriptionManager.INVALID_SUBSCRIPTION_ID

    private var confirmedCountType: String? = null
    private var pendingSwitchType: String? = null
    private var pendingSwitchFrom: String? = null
    private var pendingSwitchTimestamp: Long = 0L
    private val switchDebounceHandler = android.os.Handler(android.os.Looper.getMainLooper())
    private var switchDebounceRunnable: Runnable? = null

    private val flightModeResyncHandler = android.os.Handler(android.os.Looper.getMainLooper())
    private var flightModeResyncRunnable: Runnable? = null

    private var resumeAfterGap = false
    private val heartbeatHandler = android.os.Handler(android.os.Looper.getMainLooper())
    private val heartbeatRunnable = object : Runnable {
        override fun run() {
            prefs.edit { putLong(KEY_LAST_ALIVE, System.currentTimeMillis()) }
            heartbeatHandler.postDelayed(this, HEARTBEAT_MILLIS)
        }
    }

    private val subscriptionsChangedListener = object : SubscriptionManager.OnSubscriptionsChangedListener() {
        override fun onSubscriptionsChanged() {
            refreshSimSlotAvailability()
            if (selectedSimSlot.value == "auto" ||
                resolveTargetSubscriptionId() != monitoredSubId
            ) {
                previousNetworkType = null
                baselineNextReading = true
                confirmedCountType = null
                cancelPendingSwitch()
                startNetworkMonitoring()
            }
        }
    }

    private val airplaneModeReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            refreshAirplaneModeStatus()
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_PHONE_PERMISSION_GRANTED -> onPhonePermissionGranted()
            ACTION_RESTART_SIM -> restartMonitoringForSimChange()
            ACTION_SIMULATE -> intent.getStringExtra(EXTRA_TYPE)?.let { simulateNetwork(it) }
            ACTION_TEST_VIBRATE -> testVibrate()
        }
        return START_STICKY
    }

    override fun onCreate() {
        super.onCreate()
        prefs = getSharedPreferences(Prefs.FILE, MODE_PRIVATE)
        vibrateEnabled.value = prefs.getBoolean(KEY_VIBRATE_ENABLED, true)
        soundEnabled.value = prefs.getBoolean(KEY_SOUND_ENABLED, true)
        selectedSimSlot.value = prefs.getString(KEY_SIM_SLOT, "auto") ?: "auto"
        lastSwitchTime.value = prefs.getString(KEY_LAST_SWITCH, null)
        alertOn3G2G.value = prefs.getBoolean(KEY_ALERT_3G_2G, false)
        bypassDnd.value = prefs.getBoolean(KEY_BYPASS_DND, true)
        eventLog.value = loadEventLog()
        val lastAlive = prefs.getLong(KEY_LAST_ALIVE, 0L)
        if (lastAlive != 0L && System.currentTimeMillis() - lastAlive > GAP_THRESHOLD_MILLIS) {
            logNetworkEvent(GAP_MARKER, timestamp = lastAlive)
            resumeAfterGap = true
        }
        heartbeatRunnable.run()
        totalSwitchCount.value = prefs.getLong(KEY_TOTAL_SWITCHES, 0L)
        switchCountLog.value = loadSwitchCountLog()

        createNotificationChannels()
        val initial = NotificationCompat.Builder(this, CHANNEL_ID_STATUS)
            .setSmallIcon(generateStatusIcon("··"))
            .setContentTitle("NetLock")
            .setContentText(getString(R.string.status_monitoring))
            .setOngoing(true)
            .setSilent(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setContentIntent(openAppIntent())
            .build()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(NOTIFICATION_ID_STATUS, initial, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(NOTIFICATION_ID_STATUS, initial)
        }

        telephonyManager = getSystemService(TELEPHONY_SERVICE) as TelephonyManager
        ContextCompat.registerReceiver(
            this, airplaneModeReceiver,
            IntentFilter(Intent.ACTION_AIRPLANE_MODE_CHANGED),
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
        isAirplaneModeOn.value = Settings.Global.getInt(
            contentResolver,
            Settings.Global.AIRPLANE_MODE_ON,
            0
        ) != 0
        refreshAirplaneModeStatus()

        try {
            getSystemService(SubscriptionManager::class.java)
                ?.addOnSubscriptionsChangedListener(mainExecutor, subscriptionsChangedListener)
        } catch (e: SecurityException) {
            Log.d("NetLock", "Unable to register subscription listener: ${e.message}")
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE)
            == PackageManager.PERMISSION_GRANTED
        ) {
            refreshSimSlotAvailability()
            startNetworkMonitoring()
        } else {
            startNetworkMonitoring()
        }
    }

    fun restartMonitoringForSimChange() {
        cancelPendingSwitch()
        confirmedCountType = null
        previousNetworkType = null
        baselineNextReading = true
        startNetworkMonitoring()
    }
    fun onPhonePermissionGranted() {
        refreshSimSlotAvailability()
        startNetworkMonitoring()
    }
    fun simulateNetwork(type: String) = updateNetworkType(type)
    fun testVibrate() = vibrateOnce()

    private fun startNetworkMonitoring() {
        stopNetworkMonitoring()

        val targetSubId = resolveTargetSubscriptionId()
        monitoredSubId = targetSubId

        if (targetSubId == SubscriptionManager.INVALID_SUBSCRIPTION_ID &&
            selectedSimSlot.value != "auto" &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE)
                == PackageManager.PERMISSION_GRANTED
        ) {
            monitoredTelephonyManager = null
            trackingLabel.value = buildTrackingLabel(targetSubId)
            carrierOperatorName.value = "—"
            carrierMccMnc.value = "—"
            rsrpValue.value = "—"
            rsrqValue.value = "—"
            networkType.value = "No Service"
            refreshStatusNotificationIcon()
            return
        }

        val target = if (targetSubId != SubscriptionManager.INVALID_SUBSCRIPTION_ID) {
            telephonyManager.createForSubscriptionId(targetSubId)
        } else {
            telephonyManager
        }
        monitoredTelephonyManager = target
        trackingLabel.value = buildTrackingLabel(targetSubId)
        updateCarrierInfo(target)

        val callback = object : TelephonyCallback(),
            TelephonyCallback.DisplayInfoListener,
            TelephonyCallback.ServiceStateListener,
            TelephonyCallback.SignalStrengthsListener,
            TelephonyCallback.ActiveDataSubscriptionIdListener {
            override fun onDisplayInfoChanged(displayInfo: TelephonyDisplayInfo) {
                lastDisplayInfo = displayInfo
                updateNetworkType(mapDetailedNetworkType(displayInfo))
            }
            override fun onServiceStateChanged(serviceState: ServiceState) {
                if (serviceState.state == ServiceState.STATE_OUT_OF_SERVICE ||
                    serviceState.state == ServiceState.STATE_POWER_OFF ||
                    serviceState.state == ServiceState.STATE_EMERGENCY_ONLY
                ) {
                    updateNetworkType("No Service")
                } else if (serviceState.state == ServiceState.STATE_IN_SERVICE) {
                    resyncAfterServiceRestored(target)
                }
            }
            override fun onSignalStrengthsChanged(signalStrength: SignalStrength) {
                updateSignalQuality(signalStrength)
            }
            override fun onActiveDataSubscriptionIdChanged(subId: Int) {
                if (selectedSimSlot.value == "auto" &&
                    resolveTargetSubscriptionId() != monitoredSubId
                ) {
                    mainExecutor.execute { restartMonitoringForSimChange() }
                }
            }
        }
        telephonyCallback = callback
        try {
            target.registerTelephonyCallback(mainExecutor, callback)
        } catch (_: SecurityException) {
            networkType.value = "Permission denied"
        }
    }

    private fun stopNetworkMonitoring() {
        telephonyCallback?.let { monitoredTelephonyManager?.unregisterTelephonyCallback(it) }
        telephonyCallback = null
        lastDisplayInfo = null
    }

    private fun updateCarrierInfo(manager: TelephonyManager) {
        val opName = try { manager.networkOperatorName } catch (_: Exception) { null }
        carrierOperatorName.value = if (!opName.isNullOrBlank()) opName else "—"

        val op = try { manager.networkOperator } catch (_: Exception) { null }
        carrierMccMnc.value = if (!op.isNullOrBlank() && op.length >= 5) {
            "${op.substring(0, 3)} · ${op.substring(3)}"
        } else {
            "—"
        }
    }

    private fun resyncAfterServiceRestored(manager: TelephonyManager) {
        updateCarrierInfo(manager)
        trackingLabel.value = buildTrackingLabel(resolveTargetSubscriptionId())
        if (networkTier(networkType.value) > 0) return
        val restored = lastDisplayInfo?.let { mapDetailedNetworkType(it) } ?: return
        if (networkTier(restored) != -1) updateNetworkType(restored)
    }

    private fun updateSignalQuality(signalStrength: SignalStrength) {
        val nr = signalStrength.getCellSignalStrengths(CellSignalStrengthNr::class.java).firstOrNull()
        val lte = signalStrength.getCellSignalStrengths(CellSignalStrengthLte::class.java).firstOrNull()
        when {
            nr != null && nr.ssRsrp != CellInfo.UNAVAILABLE -> {
                rsrpValue.value = "${nr.ssRsrp} dBm"
                rsrqValue.value = "${nr.ssRsrq} dB"
            }
            lte != null && lte.rsrp != CellInfo.UNAVAILABLE -> {
                rsrpValue.value = "${lte.rsrp} dBm"
                rsrqValue.value = "${lte.rsrq} dB"
            }
            else -> {
                rsrpValue.value = "—"
                rsrqValue.value = "—"
            }
        }
    }

    private fun resolveTargetSubscriptionId(): Int {
        return when (selectedSimSlot.value) {
            "SIM 1" -> getSubscriptionIdForSlot(0)
            "SIM 2" -> getSubscriptionIdForSlot(1)
            else -> SubscriptionManager.getDefaultDataSubscriptionId()
        }
    }

    private fun getSubscriptionIdForSlot(slotIndex: Int): Int {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE)
            != PackageManager.PERMISSION_GRANTED
        ) {
            return SubscriptionManager.INVALID_SUBSCRIPTION_ID
        }
        return try {
            getSystemService(SubscriptionManager::class.java)
                ?.activeSubscriptionInfoList
                ?.firstOrNull { it.simSlotIndex == slotIndex }
                ?.subscriptionId
                ?: SubscriptionManager.INVALID_SUBSCRIPTION_ID
        } catch (_: SecurityException) {
            SubscriptionManager.INVALID_SUBSCRIPTION_ID
        }
    }

    private fun refreshSimSlotAvailability() {
        sim1Available.value = isSimPresentInSlot(0)
        sim2Available.value = isSimPresentInSlot(1)
    }

    private fun isSimPresentInSlot(slotIndex: Int): Boolean {
        return try {
            val state = telephonyManager.getSimState(slotIndex)
            state != TelephonyManager.SIM_STATE_ABSENT && state != TelephonyManager.SIM_STATE_UNKNOWN
        } catch (_: Exception) {
            false
        }
    }
    private fun refreshAirplaneModeStatus() {
        val wasOn = isAirplaneModeOn.value
        val nowOn = Settings.Global.getInt(
            contentResolver,
            Settings.Global.AIRPLANE_MODE_ON,
            0
        ) != 0
        isAirplaneModeOn.value = nowOn
        if (nowOn != wasOn) {
            logNetworkEvent(if (nowOn) "Flight Mode" else "Flight Mode Off")
            if (!nowOn) {
                startNetworkMonitoring()
                scheduleFlightModeStatusResync()
                return
            }
        }
        refreshStatusNotificationIcon()
    }

    private fun scheduleFlightModeStatusResync() {
        flightModeResyncRunnable?.let { flightModeResyncHandler.removeCallbacks(it) }
        val runnable = Runnable { refreshStatusNotificationIcon(forceRepost = true) }
        flightModeResyncRunnable = runnable
        flightModeResyncHandler.postDelayed(runnable, 1500L)
    }

    private fun buildTrackingLabel(subId: Int): String {
        if (subId == SubscriptionManager.INVALID_SUBSCRIPTION_ID) {
            return "SIM unavailable"
        }
        val slotLabel = when (selectedSimSlot.value) {
            "SIM 1" -> "SIM 1"
            "SIM 2" -> "SIM 2"
            else -> {
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE)
                    == PackageManager.PERMISSION_GRANTED
                ) {
                    val info = try {
                        getSystemService(SubscriptionManager::class.java)
                            ?.activeSubscriptionInfoList
                            ?.firstOrNull { it.subscriptionId == subId }
                    } catch (_: SecurityException) {
                        null
                    }
                    when (info?.simSlotIndex) {
                        0 -> "SIM 1"
                        1 -> "SIM 2"
                        else -> "SIM"
                    }
                } else {
                    "SIM"
                }
            }
        }
        val operatorName = try {
            telephonyManager.createForSubscriptionId(subId).networkOperatorName
        } catch (_: Exception) {
            ""
        }
        return if (operatorName.isNotBlank()) "$slotLabel • $operatorName" else slotLabel
    }

    private fun mapDetailedNetworkType(info: TelephonyDisplayInfo): String {
        return when (info.overrideNetworkType) {
            TelephonyDisplayInfo.OVERRIDE_NETWORK_TYPE_NR_ADVANCED -> "5G NSA+"
            TelephonyDisplayInfo.OVERRIDE_NETWORK_TYPE_NR_NSA -> "5G NSA"
            TelephonyDisplayInfo.OVERRIDE_NETWORK_TYPE_LTE_CA,
            TelephonyDisplayInfo.OVERRIDE_NETWORK_TYPE_LTE_ADVANCED_PRO -> "4G+"
            else -> {
                if (info.networkType == TelephonyManager.NETWORK_TYPE_NR) {
                    "5G SA"
                } else {
                    mapLegacyType(info.networkType)
                }
            }
        }
    }

    @Suppress("DEPRECATION")
    private fun mapLegacyType(type: Int): String {
        return when (type) {
            TelephonyManager.NETWORK_TYPE_NR -> "5G"
            TelephonyManager.NETWORK_TYPE_LTE -> "4G LTE"
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
            else -> "Unknown"
        }
    }

    private fun updateNetworkType(newType: String) {
        val lastLoggedType = eventLog.value.lastOrNull { networkTier(it.type) != -1 }?.type
        val previous = if (baselineNextReading) null else (previousNetworkType ?: lastLoggedType)
        val previousTier = previous?.let { networkTier(it) } ?: -1
        val newTier = networkTier(newType)

        if (previous != null && previous != newType && previousTier != -1 && newTier != -1) {
            val touchesNoService = previousTier == 0 || newTier == 0
            val touches3GOr2G = previousTier in 1..2 || newTier in 1..2

            val shouldAlert = when {
                touchesNoService -> true
                touches3GOr2G -> alertOn3G2G.value
                else -> true
            }

            if (newTier != previousTier) updateLastSwitchTime()

            if (shouldAlert) {
                if (newTier > previousTier) {
                    vibrateTwice()
                    showNetworkChangeNotification(
                        CHANNEL_ID_UPGRADE,
                        "Network Upgraded",
                        "Switched from $previous to $newType"
                    )
                } else if (newTier < previousTier) {
                    vibrateOnce()
                    showNetworkChangeNotification(
                        CHANNEL_ID_DOWNGRADE,
                        "Network Downgraded",
                        "Switched from $previous to $newType"
                    )
                }
            }
        }
        if (newTier != -1) {
            val lastLogged = lastLoggedType
            if (previous == null) {
                if (lastLogged == null || lastLogged != newType || resumeAfterGap) {
                    logNetworkEvent(newType, countAsSwitch = false)
                }
            } else if (previous != newType && lastLogged != newType) {
                logNetworkEvent(newType)
            }
            previousNetworkType = newType
            baselineNextReading = false
            resumeAfterGap = false

            if (confirmedCountType == null) {
                confirmedCountType = newType
            } else if (confirmedCountType != newType) {
                scheduleDebouncedSwitch(from = confirmedCountType!!, to = newType)
            } else {
                cancelPendingSwitch()
            }
        }
        networkType.value = newType
        refreshStatusNotificationIcon()
    }

    private fun networkTier(type: String): Int {
        return when {
            type.startsWith("5G") -> 4
            type.startsWith("4G") -> 3
            type == "3G" -> 2
            type == "2G" -> 1
            type == "No Service" -> 0
            else -> -1
        }
    }

    private fun updateLastSwitchTime() {
        val formatter = SimpleDateFormat("dd MMM, HH:mm", Locale.getDefault())
        val formatted = formatter.format(Date())
        lastSwitchTime.value = formatted
        prefs.edit { putString(KEY_LAST_SWITCH, formatted) }
    }

    
    @Suppress("UNUSED_PARAMETER")
    private fun logNetworkEvent(type: String, countAsSwitch: Boolean = true, timestamp: Long = System.currentTimeMillis()) {
        val cutoff = System.currentTimeMillis() - SEVEN_DAYS_MILLIS
        val updated = (eventLog.value + NetworkEvent(type, timestamp))
            .filter { it.timestampMillis >= cutoff }
            .takeLast(MAX_EVENTS)
        eventLog.value = updated
        saveEventLog(updated)
    }
    
    private fun scheduleDebouncedSwitch(from: String, to: String) {
        if (pendingSwitchType == to) return

        switchDebounceRunnable?.let { switchDebounceHandler.removeCallbacks(it) }
        pendingSwitchType = to
        pendingSwitchFrom = from
        pendingSwitchTimestamp = System.currentTimeMillis()

        val runnable = Runnable { confirmDebouncedSwitch(from, to, pendingSwitchTimestamp) }
        switchDebounceRunnable = runnable
        switchDebounceHandler.postDelayed(runnable, SWITCH_DEBOUNCE_MILLIS)
    }

    private fun cancelPendingSwitch() {
        switchDebounceRunnable?.let { switchDebounceHandler.removeCallbacks(it) }
        switchDebounceRunnable = null
        pendingSwitchType = null
        pendingSwitchFrom = null
    }

    private fun confirmDebouncedSwitch(from: String, to: String, transitionTimestamp: Long) {
        if (pendingSwitchType != to || confirmedCountType != from) {
            return
        }
        confirmedCountType = to
        totalSwitchCount.value += 1
        prefs.edit { putLong(KEY_TOTAL_SWITCHES, totalSwitchCount.value) }
        val updated = (switchCountLog.value + transitionTimestamp)
            .filter { it >= System.currentTimeMillis() - SEVEN_DAYS_MILLIS }
        switchCountLog.value = updated
        saveSwitchCountLog(updated)
        pendingSwitchType = null
        pendingSwitchFrom = null
        switchDebounceRunnable = null
    }

    private fun saveSwitchCountLog(timestamps: List<Long>) {
        val serialized = timestamps.joinToString("|")
        prefs.edit { putString(KEY_SWITCH_COUNT_LOG, serialized) }
    }

    private fun loadSwitchCountLog(): List<Long> {
        val raw = prefs.getString(KEY_SWITCH_COUNT_LOG, null) ?: return emptyList()
        val cutoff = System.currentTimeMillis() - SEVEN_DAYS_MILLIS
        return raw.split("|").mapNotNull { it.toLongOrNull() }.filter { it >= cutoff }
    }

    private fun saveEventLog(events: List<NetworkEvent>) {
        val arr = JSONArray()
        events.forEach { arr.put(JSONObject().put("t", it.timestampMillis).put("y", it.type)) }
        prefs.edit { putString(KEY_EVENT_LOG, arr.toString()) }
    }

    private fun loadEventLog(): List<NetworkEvent> {
        val raw = prefs.getString(KEY_EVENT_LOG, null) ?: return emptyList()
        val cutoff = System.currentTimeMillis() - SEVEN_DAYS_MILLIS
        val parsed: List<NetworkEvent> = if (raw.startsWith("[")) {
            try {
                val arr = JSONArray(raw)
                (0 until arr.length()).mapNotNull { i ->
                    val o = arr.optJSONObject(i) ?: return@mapNotNull null
                    val type = o.optString("y")
                    if (type.isEmpty()) null else NetworkEvent(type, o.optLong("t"))
                }
            } catch (_: JSONException) {
                emptyList()
            }
        } else {
            raw.split("|").mapNotNull { entry ->
                val parts = entry.split(":", limit = 2)
                val ts = parts.getOrNull(0)?.toLongOrNull()
                if (parts.size == 2 && ts != null) NetworkEvent(parts[1], ts) else null
            }
        }
        return parsed.filter { it.timestampMillis >= cutoff }.takeLast(MAX_EVENTS)
    }
    private fun getVibrator(): Vibrator {
        val manager = getSystemService(VIBRATOR_MANAGER_SERVICE) as VibratorManager
        return manager.defaultVibrator
    }

    private fun playVibration(vibrator: Vibrator, effect: VibrationEffect) {
        val bypass = bypassDnd.value
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val usage = if (bypass) VibrationAttributes.USAGE_ALARM
                        else VibrationAttributes.USAGE_NOTIFICATION
            vibrator.vibrate(effect, VibrationAttributes.createForUsage(usage))
        } else {
            val usage = if (bypass) AudioAttributes.USAGE_ALARM
                        else AudioAttributes.USAGE_NOTIFICATION
            @Suppress("DEPRECATION")
            vibrator.vibrate(
                effect,
                AudioAttributes.Builder()
                    .setUsage(usage)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
        }
    }

    private fun vibrateOnce() {
        val vibrator = getVibrator()
        Log.d("NetLock", "vibrateOnce() called, enabled=${vibrateEnabled.value}, hasVibrator=${vibrator.hasVibrator()}")
        if (!vibrateEnabled.value) return
        playVibration(vibrator, VibrationEffect.createOneShot(200, VibrationEffect.DEFAULT_AMPLITUDE))
    }

    private fun vibrateTwice() {
        val vibrator = getVibrator()
        Log.d("NetLock", "vibrateTwice() called, enabled=${vibrateEnabled.value}, hasVibrator=${vibrator.hasVibrator()}")
        if (!vibrateEnabled.value) return
        val pattern = longArrayOf(0, 200, 400, 200)
        playVibration(vibrator, VibrationEffect.createWaveform(pattern, -1))
    }

    private fun createNotificationChannels() {
        val manager = getSystemService(NotificationManager::class.java)

        val upgradeChannel = NotificationChannel(
            CHANNEL_ID_UPGRADE,
            "Network Upgrade Alerts",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Alerts when network upgrades from 4G to 5G"
            enableVibration(false)
        }

        val downgradeChannel = NotificationChannel(
            CHANNEL_ID_DOWNGRADE,
            "Network Downgrade Alerts",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Alerts when network downgrades from 5G to 4G"
            enableVibration(false)
        }

        val statusChannel = NotificationChannel(
            CHANNEL_ID_STATUS,
            "Network Status",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Ongoing status bar icon showing the current network type"
            setShowBadge(false)
        }

        manager.createNotificationChannel(upgradeChannel)
        manager.createNotificationChannel(downgradeChannel)
        manager.createNotificationChannel(statusChannel)
    }

    private fun openAppIntent(): PendingIntent {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        return PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
    }

    private fun showNetworkChangeNotification(channelId: String, title: String, message: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            Log.d("NetLock", "Skipping notification — POST_NOTIFICATIONS not granted")
            return
        }
        val notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setSilent(!soundEnabled.value)
            .setContentIntent(openAppIntent())
            .build()
        try {
            NotificationManagerCompat.from(this).notify(NOTIFICATION_ID, notification)
        } catch (e: SecurityException) {
            Log.d("NetLock", "Notification blocked: ${e.message}")
        }
    }
    
    private fun refreshStatusNotificationIcon(forceRepost: Boolean = false) {
        val shortLabel = if (isAirplaneModeOn.value) {
            "✈"
        } else {
            val type = networkType.value
            when {
                type.startsWith("5G") -> "5G"
                type.startsWith("4G") -> "4G"
                type == "3G" -> "3G"
                type == "2G" -> "2G"
                type == "No Service" -> "SOS"
                else -> "··"
            }
        }
        showStatusNotification(shortLabel, forceRepost)
    }

    private fun showStatusNotification(shortLabel: String, forceRepost: Boolean = false) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        val statusText = when (shortLabel) {
            "✈" -> getString(R.string.status_flight_mode)
            "SOS" -> getString(R.string.status_no_service)
            "··" -> getString(R.string.status_detecting)
            else -> getString(R.string.status_connected_via, shortLabel)
        }
        val statusIcon = if (shortLabel == "✈") {
            IconCompat.createWithResource(this, R.drawable.ic_flight)
        } else {
            generateStatusIcon(shortLabel)
        }
        val notification = NotificationCompat.Builder(this, CHANNEL_ID_STATUS)
            .setSmallIcon(statusIcon)
            .setContentTitle("NetLock")
            .setContentText(statusText)
            .setOngoing(true)
            .setSilent(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .setContentIntent(openAppIntent())
            .build()
        try {
            if (forceRepost) {
                NotificationManagerCompat.from(this).cancel(NOTIFICATION_ID_STATUS)
            }
            NotificationManagerCompat.from(this).notify(NOTIFICATION_ID_STATUS, notification)
        } catch (e: SecurityException) {
            Log.d("NetLock", "Status notification blocked: ${e.message}")
        }
    }

    private fun generateStatusIcon(text: String): IconCompat {
        val size = 96
        val bitmap = createBitmap(size, size, Bitmap.Config.ARGB_8888)
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
        return IconCompat.createWithBitmap(bitmap)
    }
    override fun onDestroy() {
        super.onDestroy()
        heartbeatHandler.removeCallbacks(heartbeatRunnable)
        switchDebounceHandler.removeCallbacksAndMessages(null)
        flightModeResyncHandler.removeCallbacksAndMessages(null)
        prefs.edit { putLong(KEY_LAST_ALIVE, System.currentTimeMillis()) }
        stopNetworkMonitoring()
        getSystemService(SubscriptionManager::class.java)
            ?.removeOnSubscriptionsChangedListener(subscriptionsChangedListener)
        unregisterReceiver(airplaneModeReceiver)
    }

    companion object {
        const val ACTION_PHONE_PERMISSION_GRANTED = "com.netlock.aryan.action.PHONE_PERMISSION_GRANTED"
        const val ACTION_RESTART_SIM = "com.netlock.aryan.action.RESTART_SIM"
        const val ACTION_SIMULATE = "com.netlock.aryan.action.SIMULATE"
        const val ACTION_TEST_VIBRATE = "com.netlock.aryan.action.TEST_VIBRATE"
        const val EXTRA_TYPE = "type"

        private const val CHANNEL_ID_UPGRADE = "network_upgrade"
        private const val CHANNEL_ID_DOWNGRADE = "network_downgrade"
        private const val CHANNEL_ID_STATUS = "network_status"
        private const val NOTIFICATION_ID = 1001
        private const val NOTIFICATION_ID_STATUS = 1002
        private const val KEY_VIBRATE_ENABLED = Prefs.VIBRATE_ENABLED
        private const val KEY_SOUND_ENABLED = Prefs.SOUND_ENABLED
        private const val KEY_SIM_SLOT = Prefs.SIM_SLOT
        private const val KEY_LAST_SWITCH = Prefs.LAST_SWITCH
        private const val KEY_ALERT_3G_2G = Prefs.ALERT_3G_2G
        private const val KEY_EVENT_LOG = Prefs.EVENT_LOG
        private const val KEY_TOTAL_SWITCHES = Prefs.TOTAL_SWITCHES
        private const val KEY_SWITCH_COUNT_LOG = Prefs.SWITCH_COUNT_LOG
        private const val KEY_LAST_ALIVE = Prefs.LAST_ALIVE
        private const val KEY_BYPASS_DND = Prefs.BYPASS_DND
        private const val SEVEN_DAYS_MILLIS = 7L * 24 * 60 * 60 * 1000
        private const val SWITCH_DEBOUNCE_MILLIS = 60_000L
        private const val HEARTBEAT_MILLIS = 5 * 60 * 1000L
        private const val GAP_THRESHOLD_MILLIS = 6 * 60 * 1000L
        private const val GAP_MARKER = "Not Monitored"
        private const val MAX_EVENTS = 1000
    }
}
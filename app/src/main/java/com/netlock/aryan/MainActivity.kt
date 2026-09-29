package com.netlock.aryan

import android.Manifest
import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.ui.draw.clip
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Settings as SettingsIcon
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Calendar
import java.util.Locale
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.core.net.toUri
import androidx.compose.material3.TooltipAnchorPosition
import kotlin.time.Duration.Companion.seconds
import android.util.Log
import com.netlock.aryan.ui.theme.NetLockTheme
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.AirplanemodeActive
import androidx.compose.material.icons.outlined.SignalCellularOff
import androidx.compose.material.icons.outlined.Lock
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.ScaffoldDefaults
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import kotlinx.coroutines.delay
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.systemBarsPadding
data class NetworkEvent(val type: String, val timestampMillis: Long)

private val BgBlack = Color(0xFF0F0F0F)
private val CardDark = Color(0xFF121212)
private val AccentGreen = Color(0xFF00E676)
private val AccentOrange = Color(0xFFFF9500)
private val AccentRed = Color(0xFFFF3B30)
private val AccentCyan = Color(0xFF29B6F6)
private val LabelGray = Color(0xFF8E8E93)
private val DisplayFont = FontFamily(
    Font(R.font.montserrat_regular, FontWeight.Normal),
    Font(R.font.montserrat_medium, FontWeight.Medium),
    Font(R.font.montserrat_bold, FontWeight.Bold)
)

private const val THEME_ANIM_MS = 350

@Composable
private fun themed(dark: Boolean, darkColor: Color, lightColor: Color): Color {
    val animated by animateColorAsState(
        targetValue = if (dark) darkColor else lightColor,
        animationSpec = tween(durationMillis = THEME_ANIM_MS, easing = FastOutSlowInEasing),
        label = "themeColor"
    )
    return animated
}

@Composable private fun accentGreen(dark: Boolean) = themed(dark, AccentGreen, Color(0xFF00873D))
@Composable private fun accentOrange(dark: Boolean) = themed(dark, AccentOrange, Color(0xFFB35C00))
@Composable private fun accentRed(dark: Boolean) = themed(dark, AccentRed, Color(0xFFD32F2F))
@Composable private fun accentCyan(dark: Boolean) = themed(dark, AccentCyan, Color(0xFF0277BD))
@Composable private fun dividerColor(dark: Boolean) = themed(dark, Color(0xFF2A2A2A), Color(0xFFE0E0E0))
@Composable private fun cardColor(dark: Boolean) = themed(dark, CardDark, Color(0xFFF2F2F7))
@Composable private fun screenBgColor(dark: Boolean) = themed(dark, BgBlack, Color.White)
@Composable private fun screenContentColor(dark: Boolean) = themed(dark, Color.White, Color.Black)
@Composable private fun navIndicatorColor(dark: Boolean) = themed(dark, Color(0xFF1E3A2B), Color(0xFFDDF5E6))

private fun to12Hour(raw: String): String {
    if (raw.contains("AM", ignoreCase = true) || raw.contains("PM", ignoreCase = true)) return raw
    return Regex("""(\d{1,2}):(\d{2})(:\d{2})?""").replace(raw) { m ->
        val h24 = m.groupValues[1].toInt()
        val suffix = if (h24 < 12) "AM" else "PM"
        val h12 = if (h24 % 12 == 0) 12 else h24 % 12
        "$h12:${m.groupValues[2]}${m.groupValues[3]} $suffix"
    }
}

class MainActivity : ComponentActivity() {

    private val networkType = NetLockState.networkType

    private val isDarkTheme = mutableStateOf(true)
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
    private val startOnBoot = NetLockState.startOnBoot
    private val monitoringEnabled = NetLockState.monitoringEnabled
    private val bypassDnd = NetLockState.bypassDnd
    private val showOnboarding = mutableStateOf(false)
    private val onboardingPage = mutableIntStateOf(0)

    private lateinit var prefs: SharedPreferences
    private var trackingStartMillis: Long = 0L



    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (showOnboarding.value) {
            if (!granted) networkType.value = "Permission denied"
            advanceOnboarding()
        } else if (granted) {
            sendServiceAction(NetLockService.ACTION_PHONE_PERMISSION_GRANTED)
        } else {
            networkType.value = "Permission denied"
        }
    }

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        Log.d("NetLock", "POST_NOTIFICATIONS granted=$granted")
        if (showOnboarding.value) advanceOnboarding()
    }

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        prefs = getSharedPreferences(Prefs.FILE, MODE_PRIVATE)
        isDarkTheme.value = prefs.getBoolean(KEY_DARK_THEME, true)
        vibrateEnabled.value = prefs.getBoolean(KEY_VIBRATE_ENABLED, true)
        soundEnabled.value = prefs.getBoolean(KEY_SOUND_ENABLED, true)
        selectedSimSlot.value = prefs.getString(KEY_SIM_SLOT, "auto") ?: "auto"
        lastSwitchTime.value = prefs.getString(KEY_LAST_SWITCH, null)
        alertOn3G2G.value = prefs.getBoolean(KEY_ALERT_3G_2G, false)
        startOnBoot.value = prefs.getBoolean(KEY_START_ON_BOOT, true)
        monitoringEnabled.value = prefs.getBoolean(KEY_MONITORING_ENABLED, true)
        bypassDnd.value = prefs.getBoolean(KEY_BYPASS_DND, true)
        totalSwitchCount.value = prefs.getLong(KEY_TOTAL_SWITCHES, 0L)
        val savedTrackingStart = prefs.getLong(KEY_TRACKING_START, 0L)
        trackingStartMillis = if (savedTrackingStart != 0L) {
            savedTrackingStart
        } else {
            val earliestExisting = eventLog.value.minOfOrNull { it.timestampMillis }
            val start = earliestExisting ?: System.currentTimeMillis()
            prefs.edit { putLong(KEY_TRACKING_START, start) }
            start
        }

        onboardingPage.intValue = savedInstanceState?.getInt(STATE_ONBOARDING_PAGE) ?: 0
        val phoneAlreadyGranted = ContextCompat.checkSelfPermission(
            this, Manifest.permission.READ_PHONE_STATE
        ) == PackageManager.PERMISSION_GRANTED
        if (savedInstanceState == null &&
            !prefs.getBoolean(KEY_ONBOARDING_DONE, false) &&
            phoneAlreadyGranted
        ) {
            prefs.edit { putBoolean(KEY_ONBOARDING_DONE, true) }
        }
        showOnboarding.value = !prefs.getBoolean(KEY_ONBOARDING_DONE, false)

        if (!showOnboarding.value) {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }

        if (monitoringEnabled.value) {
            ContextCompat.startForegroundService(this, Intent(this, NetLockService::class.java))
        } else {
            networkType.value = "Paused"
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE)
            != PackageManager.PERMISSION_GRANTED
        ) {
            permissionLauncher.launch(Manifest.permission.READ_PHONE_STATE)
        }
        }

        setContent {
            val selectedTab = rememberSaveable { mutableIntStateOf(0) }
                        val nowTick = remember { mutableLongStateOf(System.currentTimeMillis()) }
            LaunchedEffect(selectedTab.intValue) {
                if (selectedTab.intValue == 1) {
                    while (true) {
                        nowTick.longValue = System.currentTimeMillis()
                        delay(10.seconds)
                    }
                }
            }
            NetLockTheme(darkTheme = isDarkTheme.value) {
                val isDark = isDarkTheme.value
                DisposableEffect(isDark) {
                    val barStyle = if (isDark) {
                        SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
                    } else {
                        SystemBarStyle.light(
                            android.graphics.Color.TRANSPARENT,
                            android.graphics.Color.TRANSPARENT
                        )
                    }
                    enableEdgeToEdge(statusBarStyle = barStyle, navigationBarStyle = barStyle)
                    onDispose { }
                }
                val screenBg = screenBgColor(isDarkTheme.value)
                val screenContent = screenContentColor(isDarkTheme.value)
                CompositionLocalProvider(
    LocalContentColor provides screenContent,
    LocalTextStyle provides LocalTextStyle.current.copy(fontFamily = DisplayFont)
) {
                if (showOnboarding.value) {
                    BackHandler(enabled = onboardingPage.intValue > 0) {
                        onboardingPage.intValue -= 1
                    }
                    OnboardingScreen(
                        currentPage = onboardingPage.intValue,
                        isDarkTheme = isDarkTheme.value,
                        onButtonClick = { onOnboardingAction() }
                    )
                } else {
                val density = LocalDensity.current
                val containerSize = LocalWindowInfo.current.containerSize
                val widthDp = with(density) { containerSize.width.toDp() }
                val heightDp = with(density) { containerSize.height.toDp() }
                val useRail = widthDp >= 600.dp || widthDp > heightDp
                val navItems = listOf(
                    Icons.Outlined.Home to "Home",
                    Icons.Outlined.BarChart to "Stats",
                    Icons.Outlined.SettingsIcon to "Settings"
                )
                BackHandler(enabled = selectedTab.intValue != 0) { selectedTab.intValue = 0 }
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    containerColor = screenBg,
                    contentWindowInsets = ScaffoldDefaults.contentWindowInsets.union(WindowInsets.displayCutout),
                    bottomBar = {
                        if (!useRail) {
                            NavigationBar(containerColor = screenBg) {
                                navItems.forEachIndexed { index, (icon, label) ->
                                    NavTooltipItem(
                                        selected = selectedTab.intValue == index,
                                        onClick = { selectedTab.intValue = index },
                                        icon = icon,
                                        label = label,
                                        isDarkTheme = isDarkTheme.value
                                    )
                                }
                            }
                        }
                    }
                ) { innerPadding ->
                    Row(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
                        if (useRail) {
                            NavigationRail(
                                containerColor = screenBg,
                                windowInsets = WindowInsets(0.dp, 0.dp, 0.dp, 0.dp)
                            ) {
                                navItems.forEachIndexed { index, (icon, label) ->
                                    NavTooltipRailItem(
                                        selected = selectedTab.intValue == index,
                                        onClick = { selectedTab.intValue = index },
                                        icon = icon,
                                        label = label,
                                        isDarkTheme = isDarkTheme.value
                                    )
                                }
                            }
                        }
                        Box(
                            modifier = Modifier.weight(1f).fillMaxHeight(),
                            contentAlignment = Alignment.TopCenter
                        ) {
                        Box(modifier = Modifier.widthIn(max = 640.dp).fillMaxSize()) {
                    when (selectedTab.intValue) {
                        0 -> HomeScreen(
                            appName = "NetLock",
                            networkType = networkType.value,
                            trackingLabel = trackingLabel.value,
                            lastSwitchTime = lastSwitchTime.value,
                            isAirplaneModeOn = isAirplaneModeOn.value,
                            isDarkTheme = isDarkTheme.value,
                            recentEvents = last24hEvents(),
                            onToggleTheme = { toggleTheme() }
                        )
                        1 -> StatsScreen(
                            appName = "NetLock",
                            isDarkTheme = isDarkTheme.value,
                            onToggleTheme = { toggleTheme() },
                            totalCount = totalSwitchCount.value,
                            todayCount = todaySwitchCount(),
                            sevenDayCount = sevenDaySwitchCount(),
                            distribution = distributionDurations(nowTick.longValue),
                            distributionWindowMillis = distributionWindowMillis(nowTick.longValue),
                            operatorName = carrierOperatorName.value,
                            mccMnc = carrierMccMnc.value,
                            radioType = networkType.value,
                            rsrpValue = rsrpValue.value,
                            rsrqValue = rsrqValue.value,
                        )
                        else -> SettingsScreen(
                            appName = "NetLock",
                            isDarkTheme = isDarkTheme.value,
                            onToggleTheme = { toggleTheme() },
                            vibrateEnabled = vibrateEnabled.value,
                            onVibrateToggle = { setVibrateEnabled(it) },
                            soundEnabled = soundEnabled.value,
                            onSoundToggle = { setSoundEnabled(it) },
                            alertOn3G2G = alertOn3G2G.value,
                            onAlertOn3G2GToggle = { setAlertOn3G2G(it) },
                            selectedSimSlot = selectedSimSlot.value,
                            sim1Available = sim1Available.value,
                            sim2Available = sim2Available.value,
                            onSimSlotSelected = { setSelectedSimSlot(it) },
                            onBatteryOptimizationClick = { openBatteryOptimizationSettings() },
                            onAutoStartClick = { openAutoStartSettings() },
                            startOnBoot = startOnBoot.value,
                            onStartOnBootToggle = { setStartOnBoot(it) },
                                                        monitoringEnabled = monitoringEnabled.value,
                            onMonitoringToggle = { setMonitoringEnabled(it) },
                            bypassDnd = bypassDnd.value,
                            onBypassDndToggle = { setBypassDnd(it) },
                        )
                    }
                        }
                        }
                    }
                }
                }
                }
            }
        }
    }


    
    
    private fun toggleTheme() {
        isDarkTheme.value = !isDarkTheme.value
        prefs.edit { putBoolean(KEY_DARK_THEME, isDarkTheme.value) }
    }

    private fun setVibrateEnabled(enabled: Boolean) {
        vibrateEnabled.value = enabled
        prefs.edit { putBoolean(KEY_VIBRATE_ENABLED, enabled) }
    }

    private fun setSoundEnabled(enabled: Boolean) {
        soundEnabled.value = enabled
        prefs.edit { putBoolean(KEY_SOUND_ENABLED, enabled) }
    }

    private fun setSelectedSimSlot(slot: String) {
        selectedSimSlot.value = slot
        prefs.edit { putString(KEY_SIM_SLOT, slot) }
        sendServiceAction(NetLockService.ACTION_RESTART_SIM)
    }

    private fun setAlertOn3G2G(enabled: Boolean) {
        alertOn3G2G.value = enabled
        prefs.edit { putBoolean(KEY_ALERT_3G_2G, enabled) }
    }
    private fun setStartOnBoot(enabled: Boolean) {
        startOnBoot.value = enabled
        prefs.edit { putBoolean(KEY_START_ON_BOOT, enabled) }
    }

    private fun setBypassDnd(enabled: Boolean) {
        bypassDnd.value = enabled
        prefs.edit { putBoolean(KEY_BYPASS_DND, enabled) }
    }

    private fun setMonitoringEnabled(enabled: Boolean) {
        monitoringEnabled.value = enabled
        prefs.edit { putBoolean(KEY_MONITORING_ENABLED, enabled) }
        val serviceIntent = Intent(this, NetLockService::class.java)
        if (enabled) {
            networkType.value = "Detecting..."
            ContextCompat.startForegroundService(this, serviceIntent)
        } else {
            stopService(serviceIntent)
            networkType.value = "Paused"
        }
    }

    private fun sendServiceAction(action: String, type: String? = null) {
        if (!monitoringEnabled.value) return
        val intent = Intent(this, NetLockService::class.java).setAction(action)
        if (type != null) intent.putExtra(NetLockService.EXTRA_TYPE, type)
        ContextCompat.startForegroundService(this, intent)
    }

    private fun last24hEvents(): List<NetworkEvent> {
        val cutoff = System.currentTimeMillis() - 24 * 60 * 60 * 1000L
        return eventLog.value
            .filter { it.timestampMillis >= cutoff }
            .sortedByDescending { it.timestampMillis }
    }

    private fun startOfTodayMillis(): Long {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }

    private fun todaySwitchCount(): Int {
        val cutoff = startOfTodayMillis()
        return switchCountLog.value.count { it >= cutoff }
    }

    private fun sevenDaySwitchCount(): Int {
        val cutoff = System.currentTimeMillis() - SEVEN_DAYS_MILLIS
        return switchCountLog.value.count { it >= cutoff }
    }

    private fun distributionBucket(type: String): String? {
        return when (type) {
            "5G SA", "5G" -> "5G SA"
            "5G NSA", "5G NSA+" -> "5G NSA"
            "4G", "4G LTE", "4G+" -> "4G"
            "3G" -> "3G"
            "2G" -> "2G"
            else -> null
        }
    }

    private fun distributionDurations(now: Long = System.currentTimeMillis()): Map<String, Long> {
        val cutoff = now - SEVEN_DAYS_MILLIS
        val durations = linkedMapOf("5G SA" to 0L, "5G NSA" to 0L, "4G" to 0L, "3G" to 0L, "2G" to 0L)
        val events = eventLog.value
            .filter { it.timestampMillis >= cutoff }
            .sortedBy { it.timestampMillis }
        for (i in events.indices) {
            val start = events[i].timestampMillis
            val end = if (i + 1 < events.size) events[i + 1].timestampMillis else now
            val bucket = distributionBucket(events[i].type)
            if (bucket != null) {
                durations[bucket] = (durations[bucket] ?: 0L) + (end - start).coerceAtLeast(0L)
            }
        }
        return durations
    }

    private fun distributionWindowMillis(now: Long = System.currentTimeMillis()): Long {
        val cutoff = now - SEVEN_DAYS_MILLIS
        val firstEvent = eventLog.value
            .filter { it.timestampMillis >= cutoff }
            .minOfOrNull { it.timestampMillis }
            ?: return 0L
        return (now - firstEvent).coerceIn(0L, SEVEN_DAYS_MILLIS)
    }



    @SuppressLint("BatteryLife")
    private fun openBatteryOptimizationSettings() {
        val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
            data = "package:$packageName".toUri()
        }
        try {
            startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            Log.d("NetLock", "Battery optimization settings not available: ${e.message}")
        }
    }

    private fun openAutoStartSettings() {
        val candidateIntents = listOf(
            Intent().setComponent(
                ComponentName("com.miui.securitycenter", "com.miui.permcenter.autostart.AutoStartManagementActivity")
            ),
            Intent().setComponent(
                ComponentName("com.coloros.safecenter", "com.coloros.safecenter.permission.startup.StartupAppListActivity")
            ),
            Intent().setComponent(
                ComponentName("com.coloros.safecenter", "com.coloros.safecenter.startupapp.StartupAppListActivity")
            ),
            Intent().setComponent(
                ComponentName("com.oppo.safe", "com.oppo.safe.permission.startup.StartupAppListActivity")
            ),
            Intent().setComponent(
                ComponentName("com.iqoo.secure", "com.iqoo.secure.ui.phoneoptimize.AddWhiteListActivity")
            ),
            Intent().setComponent(
                ComponentName("com.iqoo.secure", "com.iqoo.secure.ui.phoneoptimize.BgStartUpManagerActivity")
            ),
            Intent().setComponent(
                ComponentName("com.vivo.permissionmanager", "com.vivo.permissionmanager.activity.BgStartUpManagerActivity")
            ),
            Intent().setComponent(
                ComponentName("com.huawei.systemmanager", "com.huawei.systemmanager.startupmgr.ui.StartupNormalAppListActivity")
            ),
            Intent().setComponent(
                ComponentName("com.letv.android.letvsafe", "com.letv.android.letvsafe.AutobootManageActivity")
            ),
            Intent().setComponent(
                ComponentName("com.asus.mobilemanager", "com.asus.mobilemanager.autostart.AutoStartActivity")
            )
        )

        for (intent in candidateIntents) {
            try {
                if (packageManager.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY) != null) {
                    startActivity(intent)
                    return
                }
            } catch (e: Exception) {
                Log.d("NetLock", "Autostart intent failed, trying next: ${e.message}")
            }
        }

        try {
            val fallback = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = "package:$packageName".toUri()
            }
            startActivity(fallback)
        } catch (e: ActivityNotFoundException) {
            Log.d("NetLock", "Autostart settings not available: ${e.message}")
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt(STATE_ONBOARDING_PAGE, onboardingPage.intValue)
    }

    private fun onOnboardingAction() {
        when (OnboardingSteps.getOrNull(onboardingPage.intValue)) {
            OnboardingStep.NOTIFICATION -> {
                val needsAsk = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                    ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) !=
                    PackageManager.PERMISSION_GRANTED
                if (needsAsk) {
                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                } else {
                    advanceOnboarding()
                }
            }
            OnboardingStep.PHONE_STATE -> {
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE)
                    != PackageManager.PERMISSION_GRANTED
                ) {
                    permissionLauncher.launch(Manifest.permission.READ_PHONE_STATE)
                } else {
                    advanceOnboarding()
                }
            }
            OnboardingStep.BATTERY -> {
                openBatteryOptimizationSettings()
                advanceOnboarding()
            }
            OnboardingStep.ALL_SET -> finishOnboarding()
            else -> advanceOnboarding()
        }
    }

    private fun advanceOnboarding() {
        if (onboardingPage.intValue < OnboardingSteps.lastIndex) onboardingPage.intValue += 1
    }

    private fun finishOnboarding() {
        prefs.edit { putBoolean(KEY_ONBOARDING_DONE, true) }
        showOnboarding.value = false
        if (monitoringEnabled.value) {
            ContextCompat.startForegroundService(this, Intent(this, NetLockService::class.java))
        } else {
            networkType.value = "Paused"
        }
    }
    companion object {
        private const val KEY_DARK_THEME = Prefs.DARK_THEME
        private const val KEY_VIBRATE_ENABLED = Prefs.VIBRATE_ENABLED
        private const val KEY_SOUND_ENABLED = Prefs.SOUND_ENABLED
        private const val KEY_SIM_SLOT = Prefs.SIM_SLOT
        private const val KEY_LAST_SWITCH = Prefs.LAST_SWITCH
        private const val KEY_ALERT_3G_2G = Prefs.ALERT_3G_2G
        private const val KEY_TOTAL_SWITCHES = Prefs.TOTAL_SWITCHES
        private const val KEY_TRACKING_START = Prefs.TRACKING_START
        private const val KEY_START_ON_BOOT = Prefs.START_ON_BOOT
        private const val KEY_MONITORING_ENABLED = Prefs.MONITORING_ENABLED
        private const val KEY_BYPASS_DND = Prefs.BYPASS_DND
        private const val SEVEN_DAYS_MILLIS = 7L * 24 * 60 * 60 * 1000
        private const val KEY_ONBOARDING_DONE = Prefs.ONBOARDING_DONE
        private const val STATE_ONBOARDING_PAGE = "onboarding_page"
    }
}

@Composable
fun HomeScreen(
    appName: String,
    networkType: String,
    trackingLabel: String,
    lastSwitchTime: String?,
    isAirplaneModeOn: Boolean,
    isDarkTheme: Boolean,
    modifier: Modifier = Modifier,
    recentEvents: List<NetworkEvent> = emptyList(),
    onToggleTheme: () -> Unit
) {
    val density = LocalDensity.current
    val containerSize = LocalWindowInfo.current.containerSize
    val screenWidthDp = with(density) { containerSize.width.toDp().value }
    val screenHeightDp = with(density) { containerSize.height.toDp().value }
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(screenBgColor(isDarkTheme))
            .verticalScroll(rememberScrollState())
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 24.dp, end = 11.dp, top = 8.dp, bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = appName.uppercase(),
                fontSize = 14.sp,
                letterSpacing = 4.sp,
                fontWeight = FontWeight.Medium,
                fontFamily = DisplayFont,
                color = LabelGray
            )
ThemeToggleIcon(isDarkTheme, onToggleTheme)
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = (screenHeightDp * 0.07f).coerceIn(16f, 72f).dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .background(color = accentGreen(isDarkTheme), shape = CircleShape)
            )

            Spacer(modifier = Modifier.height(24.dp))

            val specialStateIcon = when {
                isAirplaneModeOn -> Icons.Outlined.AirplanemodeActive
                networkType == "No Service" -> Icons.Outlined.SignalCellularOff
                networkType == "Permission denied" -> Icons.Outlined.Lock
                else -> null
            }
            if (specialStateIcon != null) {
                Icon(
                    imageVector = specialStateIcon,
                    contentDescription = if (isAirplaneModeOn) "Flight Mode" else networkType,
                    modifier = Modifier.size(64.dp)
                )
            } else {
                Text(
                    text = networkType,
                    fontSize = (screenWidthDp * 0.16f).coerceIn(40f, 64f).sp,
                    maxLines = 1,
                    softWrap = false,
                    fontWeight = FontWeight.Bold,
                    fontStyle = FontStyle.Italic,
                    fontFamily = DisplayFont
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "TRACKING • $trackingLabel",
                fontSize = 12.sp,
                letterSpacing = 1.sp,
                fontStyle = FontStyle.Italic,
                fontFamily = DisplayFont,
                fontWeight = FontWeight.Medium,
                color = LabelGray
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "LAST SWITCH  ${lastSwitchTime?.let { to12Hour(it) } ?: "—"}",
                fontSize = 13.sp,
                letterSpacing = 2.sp,
                fontStyle = FontStyle.Italic,
                fontFamily = DisplayFont,
                fontWeight = FontWeight.Medium,
                color = LabelGray
            )

            Spacer(modifier = Modifier.height(40.dp))

            Text(
                text = "RECENT",
                fontSize = 13.sp,
                letterSpacing = 3.sp,
                fontFamily = DisplayFont,
                fontWeight = FontWeight.Medium,
                color = LabelGray,
                modifier = Modifier
                    .align(Alignment.Start)
                    .padding(horizontal = 24.dp)
            )
            HorizontalDivider(
                modifier = Modifier.padding(top = 12.dp),
                color = dividerColor(isDarkTheme)
            )

            Spacer(modifier = Modifier.height(16.dp))

            LazyRow(
                modifier = Modifier
                    .align(Alignment.Start)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                items(recentEvents) { event ->
                    RecentEventItem(event, isDarkTheme)
                }
            }
        }
    }
}

@Composable
private fun RecentEventItem(event: NetworkEvent, isDarkTheme: Boolean) {
    val color = when {
        event.type.startsWith("5G") -> accentGreen(isDarkTheme)
        event.type.startsWith("4G") -> accentOrange(isDarkTheme)
        event.type.startsWith("3G") -> accentOrange(isDarkTheme)
        event.type.startsWith("2G") -> accentRed(isDarkTheme)
        else -> accentRed(isDarkTheme)
    }
    val timeFormatter = remember { SimpleDateFormat("h:mm:ss a", Locale.ENGLISH) }
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .background(color = color, shape = CircleShape)
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = event.type,
            fontSize = 13.sp,
            color = color,
            fontWeight = FontWeight.Medium,
            fontFamily = DisplayFont
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = timeFormatter.format(Date(event.timestampMillis)),
            fontSize = 11.sp,
            fontFamily = DisplayFont
        )
    }
}

@Preview(showBackground = true)
@Composable
fun HomeScreenPreview() {
    NetLockTheme {
        HomeScreen(
            appName = "NetLock",
            networkType = "5G",
            trackingLabel = "SIM 1 • Jio",
            lastSwitchTime = "26 Sep, 19:15",
            isAirplaneModeOn = false,
            isDarkTheme = true,
            onToggleTheme = {}
        )
    }
}

@Composable
fun StatsScreen(
    appName: String,
    isDarkTheme: Boolean,
    onToggleTheme: () -> Unit,
    modifier: Modifier = Modifier,
    totalCount: Long = 0L,
    todayCount: Int = 0,
    sevenDayCount: Int = 0,
    distribution: Map<String, Long> = emptyMap(),
    distributionWindowMillis: Long = 7L * 24 * 60 * 60 * 1000L,
    operatorName: String = "—",
    mccMnc: String = "—",
    radioType: String = "—",
    rsrpValue: String = "—",
    rsrqValue: String = "—"
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(screenBgColor(isDarkTheme))
            .verticalScroll(rememberScrollState())
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 24.dp, end = 11.dp, top = 8.dp, bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = appName.uppercase(),
                fontSize = 14.sp,
                letterSpacing = 4.sp,
                fontWeight = FontWeight.Medium,
                color = LabelGray
            )
ThemeToggleIcon(isDarkTheme, onToggleTheme)
        }

        Text(
            text = "STATS",
            fontSize = 13.sp,
            letterSpacing = 3.sp,
            color = LabelGray,
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
        )
        HorizontalDivider(color = dividerColor(isDarkTheme))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            StatCard(value = totalCount.toString(), label = "TOTAL", isDarkTheme = isDarkTheme, modifier = Modifier.weight(1f))
            StatCard(value = todayCount.toString(), label = "TODAY", isDarkTheme = isDarkTheme, modifier = Modifier.weight(1f))
            StatCard(value = sevenDayCount.toString(), label = "7 DAYS", isDarkTheme = isDarkTheme, modifier = Modifier.weight(1f))
        }

        Text(
            text = "DISTRIBUTION",
            fontSize = 13.sp,
            letterSpacing = 3.sp,
            color = LabelGray,
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
        )
        HorizontalDivider(color = dividerColor(isDarkTheme))

        Column(modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp)) {
            val totalWindowMillis = distributionWindowMillis.coerceAtLeast(1L)
            DistributionRow("5G SA", accentGreen(isDarkTheme), distribution["5G SA"] ?: 0L, totalWindowMillis, isDarkTheme)
            DistributionRow("5G NSA", accentCyan(isDarkTheme), distribution["5G NSA"] ?: 0L, totalWindowMillis, isDarkTheme)
            DistributionRow("4G", accentOrange(isDarkTheme), distribution["4G"] ?: 0L, totalWindowMillis, isDarkTheme)
            DistributionRow("3G", accentOrange(isDarkTheme), distribution["3G"] ?: 0L, totalWindowMillis, isDarkTheme)
            DistributionRow("2G", accentRed(isDarkTheme), distribution["2G"] ?: 0L, totalWindowMillis, isDarkTheme)
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "LIVE SIGNAL",
                fontSize = 13.sp,
                letterSpacing = 3.sp,
                color = LabelGray
            )
        }
        HorizontalDivider(color = dividerColor(isDarkTheme))

        SignalGroupLabel("CARRIER & IDENTITY")
        SignalMetricRow(
            listOf(
                "operator" to operatorName,
                "mcc · mnc" to mccMnc,
                "radio type" to radioType
            )
        )

        SignalGroupLabel("SIGNAL QUALITY")
        SignalMetricRow(
            listOf(
                "rsrp / ss-rsrp" to rsrpValue,
                "rsrq / ss-rsrq" to rsrqValue
            )
        )

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun StatCard(value: String, label: String, isDarkTheme: Boolean, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .background(color = cardColor(isDarkTheme), shape = RoundedCornerShape(12.dp))
            .padding(vertical = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
    text = value,
    fontSize = 36.sp,
    fontWeight = FontWeight.Bold,
    fontStyle = FontStyle.Italic,
    fontFamily = DisplayFont,
    color = screenContentColor(isDarkTheme)
)
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = label,
            fontSize = 11.sp,
            letterSpacing = 2.sp,
            fontFamily = DisplayFont,
            fontWeight = FontWeight.Medium,
            color = LabelGray
        )
    }
}

@Composable
private fun DistributionRow(label: String, color: Color, durationMillis: Long, totalWindowMillis: Long, isDarkTheme: Boolean) {
    val fraction = if (totalWindowMillis > 0) (durationMillis.toFloat() / totalWindowMillis.toFloat()).coerceIn(0f, 1f) else 0f
    val percentText = if (durationMillis > 0) "${(fraction * 100).toInt()}%" else "—"
    val hoursText = if (durationMillis > 0) "${durationMillis / 3_600_000L}h" else "—"
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = 14.sp,
            color = color,
            modifier = Modifier.width(70.dp)
        )
        Box(
            modifier = Modifier
                .weight(1f)
                .height(4.dp)
                .background(color = dividerColor(isDarkTheme), shape = RoundedCornerShape(2.dp))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(fraction)
                    .fillMaxHeight()
                    .background(color = color, shape = RoundedCornerShape(2.dp))
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = percentText,
            fontSize = 13.sp,
            color = LabelGray,
            modifier = Modifier.width(40.dp)
        )
        Text(
            text = hoursText,
            fontSize = 13.sp,
            color = LabelGray,
            modifier = Modifier.width(32.dp)
        )
    }
}

@Composable
private fun SignalGroupLabel(text: String) {
    Text(
        text = text,
        fontSize = 12.sp,
        letterSpacing = 1.sp,
        fontStyle = FontStyle.Italic,
        color = LabelGray,
        modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 16.dp, bottom = 4.dp)
    )
}

@Composable
private fun SignalMetricRow(items: List<Pair<String, String>>) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 10.dp)
    ) {
        items.forEach { (label, value) ->
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = label,
                    fontSize = 13.sp,
                    fontStyle = FontStyle.Italic,
                    color = LabelGray
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = value, fontSize = 15.sp)
            }
        }
    }
}

@Composable
fun SettingsScreen(
    appName: String,
    isDarkTheme: Boolean,
    onToggleTheme: () -> Unit,
    vibrateEnabled: Boolean,
    onVibrateToggle: (Boolean) -> Unit,
    soundEnabled: Boolean,
    onSoundToggle: (Boolean) -> Unit,
    alertOn3G2G: Boolean,
    onAlertOn3G2GToggle: (Boolean) -> Unit,
    selectedSimSlot: String,
    sim1Available: Boolean,
    sim2Available: Boolean,
    onSimSlotSelected: (String) -> Unit,
    onBatteryOptimizationClick: () -> Unit,
    onAutoStartClick: () -> Unit,
    startOnBoot: Boolean,
    onStartOnBootToggle: (Boolean) -> Unit,
    monitoringEnabled: Boolean,
    onMonitoringToggle: (Boolean) -> Unit,
    bypassDnd: Boolean,
    onBypassDndToggle: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(screenBgColor(isDarkTheme))
            .verticalScroll(rememberScrollState())
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 24.dp, end = 11.dp, top = 8.dp, bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = appName.uppercase(),
                fontSize = 14.sp,
                letterSpacing = 4.sp,
                fontWeight = FontWeight.Medium,
                color = LabelGray
            )
            ThemeToggleIcon(isDarkTheme, onToggleTheme)
        }

        SettingsSectionHeader(stringResource(R.string.section_monitoring))

        SettingsSwitchRow(
            title = stringResource(R.string.setting_monitoring),
            subtitle = stringResource(R.string.setting_monitoring_sub),
            checked = monitoringEnabled,
            onCheckedChange = onMonitoringToggle
        )
        HorizontalDivider()
        SettingsSectionHeader(stringResource(R.string.section_alerts))

        SettingsSwitchRow(
            title = stringResource(R.string.setting_vibrate),
            checked = vibrateEnabled,
            onCheckedChange = onVibrateToggle
        )
        HorizontalDivider()

        SettingsSwitchRow(
            title = stringResource(R.string.setting_sound),
            checked = soundEnabled,
            onCheckedChange = onSoundToggle
        )
        HorizontalDivider()

        SettingsSwitchRow(
            title = stringResource(R.string.setting_alert_3g_2g),
            checked = alertOn3G2G,
            onCheckedChange = onAlertOn3G2GToggle
        )
        HorizontalDivider()

        
        SettingsSwitchRow(
            title = stringResource(R.string.setting_bypass_dnd),
            subtitle = stringResource(R.string.setting_bypass_dnd_sub),
            checked = bypassDnd,
            onCheckedChange = onBypassDndToggle
        )
        HorizontalDivider()

        SettingsSectionHeader(stringResource(R.string.section_sim_slot))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 24.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            SimSlotOption(
                label = stringResource(R.string.sim_auto),
                selected = selectedSimSlot == "auto",
                onClick = { onSimSlotSelected("auto") }
            )
            SimSlotOption(
                label = "SIM 1",
                selected = selectedSimSlot == "SIM 1",
                onClick = { onSimSlotSelected("SIM 1") },
                enabled = sim1Available
            )
            SimSlotOption(
                label = "SIM 2",
                selected = selectedSimSlot == "SIM 2",
                onClick = { onSimSlotSelected("SIM 2") },
                enabled = sim2Available
            )
        }
        HorizontalDivider()

        SettingsSectionHeader(stringResource(R.string.section_device))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 20.dp)
                .clickable { onBatteryOptimizationClick() },
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = stringResource(R.string.setting_battery_opt), fontSize = 16.sp)
            Icon(
    imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
    contentDescription = null,
    tint = LabelGray,
    modifier = Modifier.size(20.dp)
)
        }
        HorizontalDivider()

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 20.dp)
                .clickable { onAutoStartClick() },
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = stringResource(R.string.setting_autostart), fontSize = 16.sp)
            Icon(
    imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
    contentDescription = null,
    tint = LabelGray,
    modifier = Modifier.size(20.dp)
)
        }
        HorizontalDivider()
                SettingsSwitchRow(
            title = stringResource(R.string.setting_start_on_boot),
            checked = startOnBoot,
            onCheckedChange = onStartOnBootToggle
        )
        HorizontalDivider()

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun ThemeToggleIcon(isDarkTheme: Boolean, onToggleTheme: () -> Unit) {
    Icon(
        imageVector = if (isDarkTheme) Icons.Outlined.DarkMode else Icons.Outlined.LightMode,
        contentDescription = stringResource(R.string.toggle_theme),
        tint = screenContentColor(isDarkTheme),
        modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .clickable(role = Role.Button) { onToggleTheme() }
            .padding(13.dp)
    )
}

enum class OnboardingStep { WELCOME, NOTIFICATION, PHONE_STATE, BATTERY, ALL_SET }

private val OnboardingSteps = listOf(
    OnboardingStep.WELCOME,
    OnboardingStep.NOTIFICATION,
    OnboardingStep.PHONE_STATE,
    OnboardingStep.BATTERY,
    OnboardingStep.ALL_SET
)

@Composable
private fun OnboardingScreen(
    currentPage: Int,
    isDarkTheme: Boolean,
    onButtonClick: () -> Unit
) {
    val step = OnboardingSteps[currentPage.coerceIn(0, OnboardingSteps.lastIndex)]
    val (title, body, button) = when (step) {
        OnboardingStep.WELCOME -> Triple(
            stringResource(R.string.onb_welcome_title),
            stringResource(R.string.onb_welcome_body),
            stringResource(R.string.onb_welcome_btn)
        )
        OnboardingStep.NOTIFICATION -> Triple(
            stringResource(R.string.onb_notif_title),
            stringResource(R.string.onb_notif_body),
            stringResource(R.string.onb_notif_btn)
        )
        OnboardingStep.PHONE_STATE -> Triple(
            stringResource(R.string.onb_phone_title),
            stringResource(R.string.onb_phone_body),
            stringResource(R.string.onb_phone_btn)
        )
        OnboardingStep.BATTERY -> Triple(
            stringResource(R.string.onb_battery_title),
            stringResource(R.string.onb_battery_body),
            stringResource(R.string.onb_battery_btn)
        )
        OnboardingStep.ALL_SET -> Triple(
            stringResource(R.string.onb_done_title),
            stringResource(R.string.onb_done_body),
            stringResource(R.string.onb_done_btn)
        )
    }

    val titleColor = themed(isDarkTheme, Color(0xFFEDEDED), Color(0xFF111111))
    val activeDot = themed(isDarkTheme, Color(0xFFD0D0D0), Color(0xFF222222))
    val inactiveDot = themed(isDarkTheme, Color(0xFF4A4A4A), Color(0xFFBDBDBD))
    val buttonShape = RoundedCornerShape(16.dp)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(screenBgColor(isDarkTheme))
            .systemBarsPadding()
            .padding(horizontal = 32.dp)
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 4.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = title,
                fontSize = 34.sp,
                lineHeight = 44.sp,
                fontStyle = FontStyle.Italic,
                fontFamily = DisplayFont,
                fontWeight = FontWeight.Normal,
                color = titleColor
            )
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = body,
                fontSize = 17.sp,
                lineHeight = 26.sp,
                fontStyle = FontStyle.Italic,
                fontFamily = DisplayFont,
                color = LabelGray
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            OnboardingSteps.indices.forEach { i ->
                Box(
                    modifier = Modifier
                        .padding(horizontal = 5.dp)
                        .size(8.dp)
                        .then(
                            if (i == currentPage) Modifier.background(activeDot, CircleShape)
                            else Modifier.border(1.5.dp, inactiveDot, CircleShape)
                        )
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .clip(buttonShape)
                .border(1.dp, dividerColor(isDarkTheme), buttonShape)
                .clickable(role = Role.Button) { onButtonClick() },
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = button.uppercase(),
                fontSize = 15.sp,
                letterSpacing = 3.sp,
                fontStyle = FontStyle.Italic,
                fontFamily = DisplayFont,
                fontWeight = FontWeight.Medium,
                color = titleColor
            )
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}
@Composable
private fun SettingsSectionHeader(title: String) {
    Text(
        text = title,
        fontSize = 13.sp,
        letterSpacing = 3.sp,
        fontFamily = DisplayFont,
        fontWeight = FontWeight.Medium,
        color = LabelGray,
        modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp)
    )
}

@Composable
private fun SettingsSwitchRow(
    title: String,
    subtitle: String? = null,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    enabled: Boolean = true
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 20.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(text = title, fontSize = 16.sp)
            if (subtitle != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = subtitle, fontSize = 13.sp)
            }
        }
        MinimalToggle(checked = checked, onCheckedChange = onCheckedChange, enabled = enabled)
    }
}

@Composable
private fun MinimalToggle(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    enabled: Boolean = true
) {
    val trackColor by animateColorAsState(
        targetValue = if (checked) AccentGreen else Color(0xFF3A3A3C),
        label = "trackColor"
    )
    Box(
        modifier = Modifier
            .size(width = 44.dp, height = 26.dp)
            .clip(RoundedCornerShape(50))
            .background(trackColor)
            .clickable(enabled = enabled) { onCheckedChange(!checked) }
            .padding(3.dp),
        contentAlignment = if (checked) Alignment.CenterEnd else Alignment.CenterStart
    ) {
        Box(
            modifier = Modifier
                .size(20.dp)
                .clip(CircleShape)
                .background(Color.White)
        )
    }
}

@Composable
private fun SimSlotOption(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    enabled: Boolean = true
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = if (enabled) Modifier.clickable { onClick() } else Modifier
    ) {
        RadioButton(
            selected = selected && enabled,
            onClick = onClick,
            enabled = enabled,
            colors = RadioButtonDefaults.colors(
                selectedColor = LocalContentColor.current,
                unselectedColor = LocalContentColor.current.copy(alpha = 0.6f)
            )
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = label,
            fontSize = 16.sp,
            color = if (enabled) Color.Unspecified else Color.Gray
        )
    }
}

@Preview(showBackground = true)
@Composable
fun SettingsScreenPreview() {
    NetLockTheme {
        SettingsScreen(
            appName = "NetLock",
            isDarkTheme = true,
            onToggleTheme = {},
            vibrateEnabled = true,
            onVibrateToggle = {},
            soundEnabled = true,
            onSoundToggle = {},
            alertOn3G2G = false,
            onAlertOn3G2GToggle = {},
            selectedSimSlot = "auto",
            sim1Available = true,
            sim2Available = false,
            onSimSlotSelected = {},
            onBatteryOptimizationClick = {},
            onAutoStartClick = {},
            startOnBoot = false,
            onStartOnBootToggle = {},
            monitoringEnabled = true,
            onMonitoringToggle = {},
            bypassDnd = true,
            onBypassDndToggle = {}
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RowScope.NavTooltipItem(
    selected: Boolean,
    onClick: () -> Unit,
    icon: ImageVector,
    label: String,
    isDarkTheme: Boolean
) {
    NavigationBarItem(
        selected = selected,
        onClick = onClick,
        icon = {
            TooltipBox(
                positionProvider = TooltipDefaults.rememberTooltipPositionProvider(TooltipAnchorPosition.Above),
                tooltip = { PlainTooltip { Text(label) } },
                state = rememberTooltipState()
            ) {
                Icon(imageVector = icon, contentDescription = label)
            }
        },
        label = null,
        alwaysShowLabel = false,
        colors = NavigationBarItemDefaults.colors(
            selectedIconColor = accentGreen(isDarkTheme),
            unselectedIconColor = screenContentColor(isDarkTheme),
            indicatorColor = navIndicatorColor(isDarkTheme)
        )
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NavTooltipRailItem(
    selected: Boolean,
    onClick: () -> Unit,
    icon: ImageVector,
    label: String,
    isDarkTheme: Boolean
) {
    NavigationRailItem(
        selected = selected,
        onClick = onClick,
        icon = {
            TooltipBox(
                positionProvider = TooltipDefaults.rememberTooltipPositionProvider(TooltipAnchorPosition.Above),
                tooltip = { PlainTooltip { Text(label) } },
                state = rememberTooltipState()
            ) {
                Icon(imageVector = icon, contentDescription = label)
            }
        },
        label = null,
        alwaysShowLabel = false,
        colors = NavigationRailItemDefaults.colors(
            selectedIconColor = accentGreen(isDarkTheme),
            unselectedIconColor = screenContentColor(isDarkTheme),
            indicatorColor = navIndicatorColor(isDarkTheme)
        )
    )
}
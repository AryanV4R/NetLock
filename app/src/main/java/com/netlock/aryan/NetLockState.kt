package com.netlock.aryan

import androidx.compose.runtime.mutableStateOf

object NetLockState {
    val networkType = mutableStateOf("Detecting...")
    val lastSwitchTime = mutableStateOf<String?>(null)
    val vibrateEnabled = mutableStateOf(true)
    val soundEnabled = mutableStateOf(true)
    val selectedSimSlot = mutableStateOf("auto")
    val sim1Available = mutableStateOf(false)
    val sim2Available = mutableStateOf(false)
    val trackingLabel = mutableStateOf("")
    val isAirplaneModeOn = mutableStateOf(false)
    val alertOn3G2G = mutableStateOf(false)
    val eventLog = mutableStateOf<List<NetworkEvent>>(emptyList())
    val totalSwitchCount = mutableStateOf(0L)
    val carrierOperatorName = mutableStateOf("\u2014")
    val carrierMccMnc = mutableStateOf("\u2014")
    val rsrpValue = mutableStateOf("\u2014")
    val rsrqValue = mutableStateOf("\u2014")
    val sinrValue = mutableStateOf("\u2014")
    val switchCountLog = mutableStateOf<List<Long>>(emptyList())
    val startOnBoot = mutableStateOf(true)
    val monitoringEnabled = mutableStateOf(true)
    val bypassDnd = mutableStateOf(true)
}
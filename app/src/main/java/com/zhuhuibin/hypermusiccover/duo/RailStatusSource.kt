package com.zhuhuibin.hypermusiccover.duo

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.database.ContentObserver
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.wifi.WifiInfo
import android.net.wifi.WifiManager
import android.os.BatteryManager
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.telephony.ServiceState
import android.telephony.SignalStrength
import android.telephony.SubscriptionManager
import android.telephony.TelephonyCallback
import android.telephony.TelephonyDisplayInfo
import android.telephony.TelephonyManager

internal data class RailStatusSnapshot(
    val battery: Int = -1, val charging: Boolean = false, val wifi: Int = -1,
    val signal: Int = -1, val secondSignal: Int = -1, val dualSim: Boolean = false,
    val network: String = "", val airplane: Boolean = false
)

/** Read platform status inside SystemUI; no companion process, location data or wake lock. */
internal class RailStatusSource(private val context: Context, private val changed: (RailStatusSnapshot) -> Unit) {
    private val main = Handler(Looper.getMainLooper())
    private val connectivity = context.getSystemService(ConnectivityManager::class.java)
    private val wifi = context.getSystemService(WifiManager::class.java)
    private val subscriptions = context.getSystemService(SubscriptionManager::class.java)
    private val telephony = context.getSystemService(TelephonyManager::class.java)
    private var running = false
    private var receiverRegistered = false
    private var networkRegistered = false
    private var subscriptionsRegistered = false
    private var activeDataRegistered = false
    private var settingsRegistered = false
    private var battery = -1
    private var charging = false
    private var airplane = false
    private var activeData = SubscriptionManager.INVALID_SUBSCRIPTION_ID
    private val wifiLevels = mutableMapOf<Network, Int>()
    private val sims = linkedMapOf<Int, Sim>()
    private var last: RailStatusSnapshot? = null
    private val errors = linkedSetOf<String>()

    private inner class Sim(val id: Int, val manager: TelephonyManager) {
        var level = -1
        var inService = true
        var label = ""
        val callback: TelephonyCallback = object : TelephonyCallback(), TelephonyCallback.SignalStrengthsListener,
            TelephonyCallback.DisplayInfoListener, TelephonyCallback.ServiceStateListener {
            override fun onSignalStrengthsChanged(signalStrength: SignalStrength) {
                if (!running || sims[id] !== this@Sim) return
                level = signalStrength.level; publish()
            }
            override fun onDisplayInfoChanged(telephonyDisplayInfo: TelephonyDisplayInfo) {
                if (!running || sims[id] !== this@Sim) return
                label = networkName(telephonyDisplayInfo); publish()
            }
            override fun onServiceStateChanged(serviceState: ServiceState) {
                if (!running || sims[id] !== this@Sim) return
                inService = serviceState.state == ServiceState.STATE_IN_SERVICE; publish()
            }
        }
    }

    private val activeCallback = object : TelephonyCallback(), TelephonyCallback.ActiveDataSubscriptionIdListener {
        override fun onActiveDataSubscriptionIdChanged(subId: Int) {
            if (!running) return
            activeData = subId; publish()
        }
    }
    private val subscriptionCallback = object : SubscriptionManager.OnSubscriptionsChangedListener() {
        override fun onSubscriptionsChanged() { if (running) refreshSims() }
    }
    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onCapabilitiesChanged(network: Network, capabilities: NetworkCapabilities) {
            if (!running) return
            attempt("wifi update") { readWifi(network, capabilities); publish() }
        }
        override fun onLost(network: Network) {
            if (!running) return
            wifiLevels.remove(network); publish()
        }
    }
    private val settings = object : ContentObserver(main) {
        override fun onChange(selfChange: Boolean) { if (running) { readAirplane(); tickNow() } }
    }
    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (!running) return
            when (intent.action) {
                Intent.ACTION_BATTERY_CHANGED -> readBattery(intent)
                Intent.ACTION_AIRPLANE_MODE_CHANGED -> readAirplane()
                WifiManager.RSSI_CHANGED_ACTION, WifiManager.WIFI_STATE_CHANGED_ACTION,
                WifiManager.NETWORK_STATE_CHANGED_ACTION -> refreshWifi()
            }
            tickNow()
        }
    }
    private val minute = Runnable { if (running) tickNow() }

    fun start() {
        if (running) return
        running = true
        attempt("battery/time") {
            val filter = IntentFilter().apply {
                addAction(Intent.ACTION_BATTERY_CHANGED)
                addAction(Intent.ACTION_TIME_TICK); addAction(Intent.ACTION_TIME_CHANGED)
                addAction(Intent.ACTION_TIMEZONE_CHANGED); addAction(Intent.ACTION_AIRPLANE_MODE_CHANGED)
                addAction(WifiManager.RSSI_CHANGED_ACTION); addAction(WifiManager.WIFI_STATE_CHANGED_ACTION)
                addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION)
            }
            val sticky = context.registerReceiver(receiver, filter, null, main, Context.RECEIVER_EXPORTED)
            receiverRegistered = true
            sticky?.let(::readBattery)
        }
        readAirplane()
        attempt("clock format") {
            context.contentResolver.registerContentObserver(Settings.System.getUriFor(Settings.System.TIME_12_24),
                false, settings)
            settingsRegistered = true
        }
        attempt("wifi callback") {
            connectivity?.registerNetworkCallback(NetworkRequest.Builder()
                .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET).build(), networkCallback, main)
            networkRegistered = connectivity != null
        }
        refreshWifi()
        attempt("subscriptions") {
            activeData = SubscriptionManager.getDefaultDataSubscriptionId()
            subscriptions?.addOnSubscriptionsChangedListener(context.mainExecutor, subscriptionCallback)
            subscriptionsRegistered = subscriptions != null
        }
        attempt("active SIM") {
            telephony?.registerTelephonyCallback(context.mainExecutor, activeCallback)
            activeDataRegistered = telephony != null
        }
        refreshSims()
        tickNow()
    }

    fun stop() {
        if (!running) return
        running = false
        main.removeCallbacks(minute)
        if (receiverRegistered) runCatching { context.unregisterReceiver(receiver) }
        if (networkRegistered) runCatching { connectivity?.unregisterNetworkCallback(networkCallback) }
        if (subscriptionsRegistered) runCatching { subscriptions?.removeOnSubscriptionsChangedListener(subscriptionCallback) }
        if (activeDataRegistered) runCatching { telephony?.unregisterTelephonyCallback(activeCallback) }
        if (settingsRegistered) runCatching { context.contentResolver.unregisterContentObserver(settings) }
        for (sim in sims.values) runCatching { sim.manager.unregisterTelephonyCallback(sim.callback) }
        sims.clear(); wifiLevels.clear(); last = null
        receiverRegistered = false; networkRegistered = false; subscriptionsRegistered = false
        activeDataRegistered = false; settingsRegistered = false
    }

    private fun readBattery(intent: Intent) {
        battery = RailStatusValues.batteryPercent(intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1),
            intent.getIntExtra(BatteryManager.EXTRA_SCALE, 0))
        val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, BatteryManager.BATTERY_STATUS_UNKNOWN)
        charging = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0) != 0 &&
            (status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL)
    }
    private fun readAirplane() = attempt("airplane") {
        airplane = Settings.Global.getInt(context.contentResolver, Settings.Global.AIRPLANE_MODE_ON, 0) != 0
    }

    @Suppress("DEPRECATION")
    private fun readWifi(network: Network, capabilities: NetworkCapabilities) {
        if (!capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
            !capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)) {
            wifiLevels.remove(network); return
        }
        // Only RSSI is used; SSIDs, BSSIDs and other network identifiers are never retained.
        val rssi = (capabilities.transportInfo as? WifiInfo)?.rssi ?: wifi?.connectionInfo?.rssi ?: -127
        wifiLevels[network] = RailStatusValues.wifiLevel(true, rssi)
    }
    @Suppress("DEPRECATION")
    private fun refreshWifi() = attempt("wifi state") {
        wifiLevels.clear()
        if (wifi?.isWifiEnabled == true) for (network in connectivity?.allNetworks.orEmpty()) {
            connectivity?.getNetworkCapabilities(network)?.let { readWifi(network, it) }
        }
    }

    private fun refreshSims() = attempt("SIM state") {
        val ids = subscriptions?.activeSubscriptionInfoList.orEmpty()
            .filter { it.simSlotIndex >= 0 }.sortedBy { it.simSlotIndex }.take(2).map { it.subscriptionId }
        for (id in sims.keys.filter { it !in ids }) {
            sims.remove(id)?.let { runCatching { it.manager.unregisterTelephonyCallback(it.callback) } }
        }
        for (id in ids) if (!sims.containsKey(id) && telephony != null) {
            val sim = Sim(id, telephony.createForSubscriptionId(id))
            sims[id] = sim
            attempt("SIM signal") {
                sim.level = sim.manager.signalStrength?.level ?: -1
                sim.inService = sim.manager.serviceState?.state?.let { it == ServiceState.STATE_IN_SERVICE } ?: true
            }
            attempt("SIM callback") {
                sim.manager.registerTelephonyCallback(context.mainExecutor, sim.callback)
            }
        }
        // Stable physical slot order, including after an eSIM is enabled or removed.
        val ordered = ids.mapNotNull { id -> sims[id]?.let { id to it } }
        sims.clear(); sims.putAll(ordered)
        publish()
    }

    private fun tickNow() {
        if (!running) return
        publish(force = true)
        main.removeCallbacks(minute)
        // Handler deadlines do not wake a sleeping CPU. Broadcasts/wake redraw catch up.
        main.postDelayed(minute, 60_000L - System.currentTimeMillis().mod(60_000L) + 20L)
    }
    private fun publish(force: Boolean = false) {
        if (!running) return
        val rows = sims.values.toList()
        fun level(index: Int) = rows.getOrNull(index)?.let {
            RailStatusValues.signalLevel(it.level, it.inService && !airplane)
        } ?: -1
        val data = sims[activeData] ?: rows.firstOrNull()
        val value = RailStatusSnapshot(battery, charging, wifiLevels.values.maxOrNull() ?: -1,
            level(0), level(1), rows.size > 1, if (!airplane && data?.inService == true) data.label else "", airplane)
        if (force || value != last) { last = value; attempt("draw") { changed(value) } }
    }
    private inline fun attempt(name: String, block: () -> Unit) {
        runCatching(block).onFailure {
            val tag = "$name:${it.javaClass.simpleName}"
            if (errors.add(tag)) Xp.w("DuoStatus: $tag")
        }
    }
    fun describe() = "listening=$running sims=${sims.size} wifi=${wifiLevels.isNotEmpty()} errors=${errors.joinToString()}"

    private fun networkName(info: TelephonyDisplayInfo): String =
        RailStatusValues.networkLabel(info.overrideNetworkType, info.networkType)
}

package com.zhuhuibin.hypermusiccover.duo

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONObject

/** Exact filters verified against the installed HyperOS 17.03.260226.r SystemUI. */
internal object LockscreenNotificationRuntime {
    private const val SHOW_ALL = "showAllLockscreenNotifications"
    private const val RETAIN = "retainLockscreenNotifications"
    @Volatile private var policy = LockscreenNotificationPolicy()
    @Volatile private var preferences: SharedPreferences? = null
    private var showHook = false
    private var retainHook = false
    private var retentionReadFailed = false
    private var retentionRestoreFailed = false
    private val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        if (key == null || key == "config") {
            load()
            Main.main().post { LockIslands.notificationPolicyChanged() }
        }
    }

    private fun load() {
        val config = runCatching { JSONObject(preferences?.getString("config", null).orEmpty()) }
            .getOrDefault(JSONObject())
        policy = LockscreenNotificationPolicy(config.optBoolean(SHOW_ALL, false), config.optBoolean(RETAIN, false))
    }

    /** SystemUI's application context can arrive after hook installation. */
    private fun current(): LockscreenNotificationPolicy {
        if (preferences == null) Main.sAppCtx?.let { context ->
            runCatching {
                preferences = context.getSharedPreferences("hmc_mini", Context.MODE_PRIVATE)
                preferences?.registerOnSharedPreferenceChangeListener(listener)
                load()
            }
        }
        return policy
    }

    fun install(loader: ClassLoader) {
        current()
        runCatching {
            val expanded = Xp.findClass("com.android.systemui.statusbar.notification.ExpandedNotification", loader)
            val method = expanded.getDeclaredMethod("canShowOnKeyguard")
            check(method.returnType == Boolean::class.javaPrimitiveType)
            Xp.hook(method) { chain ->
                val original = chain.proceed()
                if (original is Boolean) current().canShowOnKeyguard(original) else original
            }
            showHook = true
        }.onFailure { Xp.w("MCNotif: show-all filter unavailable; native visibility retained: $it") }

        runCatching {
            val entryClass = Xp.findClass("com.android.systemui.statusbar.notification.collection.NotificationEntry", loader)
            val expanded = Xp.findClass("com.android.systemui.statusbar.notification.ExpandedNotification", loader)
            val filter = Xp.findClass("com.android.systemui.statusbar.notification.policy.NotificationFilterController", loader)
            val sbnField = entryClass.getDeclaredField("mSbn").also { it.isAccessible = true }
            val seenField = expanded.getDeclaredField("mHasShownAfterUnlock").also { it.isAccessible = true }
            check(seenField.type == Boolean::class.javaPrimitiveType && expanded.isAssignableFrom(sbnField.type))
            val method = filter.getDeclaredMethod("forceHideOnKeyguard", entryClass)
            check(method.returnType == Boolean::class.javaPrimitiveType)
            Xp.hook(method) { chain ->
                val entry = chain.args.firstOrNull()
                if (!current().retain || entry == null) return@hook chain.proceed()
                val sbn = runCatching { sbnField.get(entry) }.getOrNull()
                    ?: return@hook chain.proceed()
                synchronized(sbn) {
                    val original = runCatching { seenField.getBoolean(sbn) }.getOrElse {
                        if (!retentionReadFailed) Xp.w("MCNotif: retention marker unreadable; native filter retained: $it")
                        retentionReadFailed = true
                        return@hook chain.proceed()
                    }
                    val masked = current().shownAfterUnlock(original)
                    if (masked == original) return@hook chain.proceed()
                    runCatching { seenField.setBoolean(sbn, masked) }.getOrElse {
                        if (!retentionReadFailed) Xp.w("MCNotif: retention marker not writable; native filter retained: $it")
                        retentionReadFailed = true
                        return@hook chain.proceed()
                    }
                    try {
                        // Continue the complete OEM filter. Global privacy, secret notifications,
                        // user/profile restrictions and redaction remain the provider's decisions.
                        chain.proceed()
                    } finally {
                        runCatching { seenField.setBoolean(sbn, original) }.onFailure {
                            if (!retentionRestoreFailed) Xp.w("MCNotif: retention marker restore failed: $it")
                            retentionRestoreFailed = true
                        }
                    }
                }
            }
            retainHook = true
        }.onFailure { Xp.w("MCNotif: retention filter unavailable; native visibility retained: $it") }
    }

    fun describe(): String = current().let {
        "notificationFilters showAll=${it.showAll}/$showHook retain=${it.retain}/$retainHook" +
            " readFailed=$retentionReadFailed restoreFailed=$retentionRestoreFailed"
    }
}

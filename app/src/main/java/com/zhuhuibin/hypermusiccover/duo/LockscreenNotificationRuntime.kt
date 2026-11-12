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
    private var providerHook = false
    private var markerClears = 0
    private var providerRef: java.lang.ref.WeakReference<Any>? = null
    private var notifyProvider: java.lang.reflect.Method? = null
    private val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        if (key == null || key == "config") {
            load()
            Main.main().post {
                // The native provider invalidates all its consumers, even when islands are off.
                providerRef?.get()?.let { provider ->
                    runCatching { notifyProvider?.invoke(null, provider, "Duo notification settings") }
                }
                LockIslands.notificationPolicyChanged()
            }
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
                if (current().canShowOnKeyguard(false)) true else chain.proceed()
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
            fun retainEntry(entry: Any?) {
                val policy = current()
                if (!policy.retain || entry == null) return
                runCatching {
                    val sbn = sbnField.get(entry) ?: return
                    val original = seenField.getBoolean(sbn)
                    val retained = policy.shownAfterUnlock(original)
                    if (retained != original) {
                        // Match HyperLight's entry boundary: this is the shared native marker,
                        // not a temporary mask restored before grouping/counting consumers run.
                        seenField.setBoolean(sbn, retained)
                        markerClears++
                    }
                }.onFailure {
                    if (!retentionReadFailed) Xp.w("MCNotif: retention marker unavailable: $it")
                    retentionReadFailed = true
                }
            }
            runCatching {
                val provider = Xp.findClass("com.android.systemui.statusbar.notification.interruption." +
                    "KeyguardNotificationVisibilityProviderImpl", loader)
                notifyProvider = runCatching {
                    provider.getDeclaredMethod("access\$notifyStateChanged", provider, String::class.java)
                        .also { it.isAccessible = true }
                }.getOrNull()
                // Both the one-argument wrapper and the direct boolean overload are used.
                provider.declaredMethods.filter { it.name == "shouldHideNotification" &&
                    it.parameterTypes.firstOrNull() == entryClass }.forEach { method ->
                    Xp.hook(method) { chain ->
                        providerRef = java.lang.ref.WeakReference(chain.thisObject)
                        retainEntry(chain.args.firstOrNull())
                        chain.proceed()
                    }
                    providerHook = true
                }
            }.onFailure { Xp.w("MCNotif: early retention boundary unavailable: $it") }
            val method = filter.getDeclaredMethod("forceHideOnKeyguard", entryClass)
            check(method.returnType == Boolean::class.javaPrimitiveType)
            Xp.hook(method) { chain ->
                // Direct stock callers outside the visibility provider read the same marker.
                retainEntry(chain.args.firstOrNull())
                chain.proceed()
            }
            retainHook = true
        }.onFailure { Xp.w("MCNotif: retention filter unavailable; native visibility retained: $it") }
    }

    fun describe(): String = current().let {
        "notificationFilters showAll=${it.showAll}/$showHook retain=${it.retain}/$retainHook" +
            " provider=$providerHook cleared=$markerClears readFailed=$retentionReadFailed"
    }
}

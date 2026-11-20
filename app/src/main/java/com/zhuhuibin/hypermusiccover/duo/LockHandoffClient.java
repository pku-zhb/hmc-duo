package com.zhuhuibin.hypermusiccover.duo;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.ResultReceiver;
import android.view.Display;
import android.view.View;
import java.lang.ref.WeakReference;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * SystemUI adapter for the v1 lockscreen handoff protocol between HMC Duo, MiDuo (0x7e484d01),
 * and Notification Optimizer / xmz (0x7e484d02).
 *
 * Keeps all protocol logic in {@link LockHandoffClientState} and executes revocation cleanup
 * synchronously on the SystemUI main thread inside local {@link ResultReceiver}(null) callbacks
 * and shade detach hooks.
 */
final class LockHandoffClient {
    private static final String TAG = "MCLockHandoff: ";
    private static final Handler MAIN = new Handler(Looper.getMainLooper());

    private static volatile Context appContext;
    private static volatile boolean installed;
    private static volatile boolean receiverRegistered;
    private static WeakReference<View> currentShadeRef = new WeakReference<>(null);
    @SuppressWarnings({"unchecked", "rawtypes"})
    private static final Function<Bundle, Bundle>[] endpoints =
            new Function[LockHandoffClientState.PROVIDER_COUNT];

    private static final LockHandoffClientState STATE = new LockHandoffClientState(
            new LockHandoffClientState.Host() {
                @Override
                public boolean isPackageInstalled(String packageName) {
                    return queryPackageInstalled(packageName);
                }

                @Override
                public LockHandoffClientState.ProbeResult probeEndpoint(int provider) {
                    return resolveTagEndpoint(provider);
                }

                @Override
                public LockHandoffClientState.Response callEndpoint(
                        int provider, LockHandoffClientState.Request request, int callbackToken) {
                    return invokeEndpoint(provider, request, callbackToken);
                }

                @Override
                public void schedulePostAttachRetry(Object windowToken) {
                    if (!(windowToken instanceof View)) return;
                    final View shade = (View) windowToken;
                    shade.post(() -> {
                        if (Looper.myLooper() == Looper.getMainLooper()) {
                            STATE.onPostAttachRetry(shade);
                        }
                    });
                }

                @Override
                public void scheduleReconcile(Object windowToken) {
                    MAIN.post(() -> STATE.onPostedReconcile(windowToken));
                }

                @Override
                public void onCapabilitiesRevokedSync(int revokedMask, int effectiveMask) {
                    performSyncRevokeCleanup(revokedMask);
                }

                @Override
                public void onCapabilitiesGained(int gainedMask, int effectiveMask) {
                    refreshForGainedCapabilities(gainedMask);
                    Xp.log(TAG + describe());
                }

                @Override
                public void logWarn(String message) {
                    Xp.w(TAG + message);
                }
            });

    private static final View.OnAttachStateChangeListener SHADE_LIFECYCLE =
            new View.OnAttachStateChangeListener() {
                @Override public void onViewAttachedToWindow(View view) {
                    onShadeAttached(view);
                }

                @Override public void onViewDetachedFromWindow(View view) {
                    onShadeDetached(view);
                }
            };

    private LockHandoffClient() {}

    static void install(ClassLoader loader) {
        if (installed) return;
        try {
            Class<?> shadeCls = Xp.findClass(
                    "com.android.systemui.shade.NotificationShadeWindowView", loader);
            Xp.hookAll(shadeCls, "onAttachedToWindow", chain -> {
                Object result = chain.proceed();
                Object self = chain.getThisObject();
                if (self instanceof View shade && shade.isAttachedToWindow()) {
                    shade.removeOnAttachStateChangeListener(SHADE_LIFECYCLE);
                    shade.addOnAttachStateChangeListener(SHADE_LIFECYCLE);
                    if (currentShadeRef.get() != shade) onShadeAttached(shade);
                }
                return result;
            });
            installed = true;
            Xp.log(TAG + "installed on NotificationShadeWindowView");
        } catch (Throwable error) {
            Xp.w(TAG + "install failed: " + error);
        }
    }

    static boolean allowsShortcuts() {
        return STATE.allowsShortcuts();
    }

    static boolean allowsStatusBar() {
        return STATE.allowsStatusBar();
    }

    static boolean allowsTrio() {
        return STATE.allowsTrio();
    }

    static boolean allowsNotifications() {
        return STATE.allowsNotifications();
    }

    static int effectiveMask() {
        return STATE.effectiveMask();
    }

    static void sync() {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            syncOnMain();
        } else {
            MAIN.post(LockHandoffClient::syncOnMain);
        }
    }

    static String describe() {
        return "effective=0x" + Integer.toHexString(STATE.effectiveMask())
                + " desired=0x" + Integer.toHexString(STATE.desiredMask())
                + " attached=" + STATE.isWindowAttached()
                + " miduo(inst=" + STATE.isProviderInstalled(LockHandoffClientState.PROVIDER_MIDUO)
                + ",ready=" + STATE.isProviderEndpointReady(LockHandoffClientState.PROVIDER_MIDUO)
                + ",grant=0x" + Integer.toHexString(STATE.providerGrantedMask(LockHandoffClientState.PROVIDER_MIDUO))
                + ",pend=0x" + Integer.toHexString(STATE.providerPendingMask(LockHandoffClientState.PROVIDER_MIDUO)) + ")"
                + " xmz(inst=" + STATE.isProviderInstalled(LockHandoffClientState.PROVIDER_XMZ)
                + ",ready=" + STATE.isProviderEndpointReady(LockHandoffClientState.PROVIDER_XMZ)
                + ",grant=0x" + Integer.toHexString(STATE.providerGrantedMask(LockHandoffClientState.PROVIDER_XMZ))
                + ",pend=0x" + Integer.toHexString(STATE.providerPendingMask(LockHandoffClientState.PROVIDER_XMZ)) + ")";
    }

    private static void syncOnMain() {
        if (appContext == null && Main.sAppCtx != null) {
            appContext = Main.sAppCtx;
            ensurePackageReceiver(appContext);
            STATE.refreshInstalledPackages();
        }
        STATE.syncDesired(computeDesiredMask());
    }

    private static int computeDesiredMask() {
        int mask = 0;
        if (Main.sRightRail) {
            mask |= LockHandoffClientState.BIT_SHORTCUTS
                    | LockHandoffClientState.BIT_TRIO
                    | LockHandoffClientState.BIT_STATUSBAR;
        }
        if (Main.sHideLockStatusBar) {
            mask |= LockHandoffClientState.BIT_STATUSBAR;
        }
        if ((Main.sRightRail && Main.sReflowLockContent) || MiniPlayerRuntime.isEnabled()) {
            mask |= LockHandoffClientState.BIT_NOTIFICATIONS;
        }
        return mask;
    }

    private static void onShadeAttached(View shade) {
        if (shade == null || Looper.myLooper() != Looper.getMainLooper()) return;
        Display display = shade.getDisplay();
        if (display != null && display.getDisplayId() != Display.DEFAULT_DISPLAY) return;

        Context ctx = shade.getContext();
        Context app = ctx == null ? null : ctx.getApplicationContext();
        if (app != null) {
            appContext = app;
        } else if (ctx != null) {
            appContext = ctx;
        }
        ensurePackageReceiver(appContext);

        if (currentShadeRef.get() != shade) {
            currentShadeRef = new WeakReference<>(shade);
            endpoints[LockHandoffClientState.PROVIDER_MIDUO] = null;
            endpoints[LockHandoffClientState.PROVIDER_XMZ] = null;
        }

        STATE.onWindowAttached(shade, computeDesiredMask());
    }

    private static void onShadeDetached(View shade) {
        if (shade == null || Looper.myLooper() != Looper.getMainLooper()) return;
        View current = currentShadeRef.get();
        if (current != shade) return;
        endpoints[LockHandoffClientState.PROVIDER_MIDUO] = null;
        endpoints[LockHandoffClientState.PROVIDER_XMZ] = null;
        currentShadeRef = new WeakReference<>(null);
        STATE.onWindowDetached(shade);
    }

    private static void ensurePackageReceiver(Context ctx) {
        if (receiverRegistered || ctx == null) return;
        try {
            IntentFilter filter = new IntentFilter();
            filter.addAction(Intent.ACTION_PACKAGE_ADDED);
            filter.addAction(Intent.ACTION_PACKAGE_REMOVED);
            filter.addAction(Intent.ACTION_PACKAGE_REPLACED);
            filter.addAction(Intent.ACTION_PACKAGE_CHANGED);
            filter.addDataScheme("package");
            ctx.registerReceiver(new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    if (intent == null || intent.getData() == null) return;
                    final String pkg = intent.getData().getSchemeSpecificPart();
                    if (!LockHandoffClientState.PKG_MIDUO.equals(pkg)
                            && !LockHandoffClientState.PKG_XMZ.equals(pkg)) {
                        return;
                    }
                    final String action = intent.getAction();
                    final boolean replacing = intent.getBooleanExtra(Intent.EXTRA_REPLACING, false);
                    // The registration below delivers on MAIN. During an APK replacement
                    // keep the dependency blocked until the replacement has completed.
                    if (Intent.ACTION_PACKAGE_REMOVED.equals(action)) {
                        if (replacing) STATE.onPackageReplacementStarted(pkg);
                        else STATE.onPackageChanged(pkg, false);
                    } else if (Intent.ACTION_PACKAGE_REPLACED.equals(action)
                            || (Intent.ACTION_PACKAGE_ADDED.equals(action) && !replacing)) {
                        STATE.onPackageChanged(pkg, true);
                    } else if (Intent.ACTION_PACKAGE_CHANGED.equals(action)) {
                        STATE.refreshInstalledPackages();
                    }
                }
            }, filter, null, MAIN);
            receiverRegistered = true;
        } catch (Throwable error) {
            Xp.w(TAG + "package receiver registration failed: " + error);
        }
    }

    private static boolean queryPackageInstalled(String packageName) {
        Context ctx = appContext;
        if (ctx == null) {
            View shade = currentShadeRef.get();
            ctx = shade == null ? null : shade.getContext();
        }
        if (ctx == null) ctx = Main.sAppCtx;
        if (ctx == null) return true;
        try {
            ctx.getPackageManager().getPackageInfo(packageName, 0);
            return true;
        } catch (PackageManager.NameNotFoundException missing) {
            return false;
        }
    }

    @SuppressWarnings("unchecked")
    private static LockHandoffClientState.ProbeResult resolveTagEndpoint(int provider) {
        View shade = currentShadeRef.get();
        if (shade == null || !shade.isAttachedToWindow()) {
            return LockHandoffClientState.ProbeResult.ABSENT;
        }
        int tagKey = LockHandoffClientState.tagKeyForProvider(provider);
        String protocol = LockHandoffClientState.protocolForProvider(provider);
        Object raw = shade.getTag(tagKey);
        if (raw == null) {
            return LockHandoffClientState.ProbeResult.ABSENT;
        }
        if (!(raw instanceof Map<?, ?>)) {
            return LockHandoffClientState.ProbeResult.INVALID_TAG;
        }
        Object fn = ((Map<?, ?>) raw).get(protocol);
        if (!(fn instanceof Function<?, ?>)) {
            return LockHandoffClientState.ProbeResult.INVALID_TAG;
        }
        Function<Bundle, Bundle> previous = endpoints[provider];
        endpoints[provider] = (Function<Bundle, Bundle>) fn;
        return previous != null && previous != fn
                ? LockHandoffClientState.ProbeResult.REPLACED : LockHandoffClientState.ProbeResult.READY;
    }

    private static LockHandoffClientState.Response invokeEndpoint(
            int provider, LockHandoffClientState.Request request, final int callbackToken) {
        Function<Bundle, Bundle> fn = endpoints[provider];
        if (fn == null) return null;
        Bundle req = new Bundle();
        req.putString("method", request.method);
        if (request.hasRequestId) {
            req.putLong("requestId", request.requestId);
        }
        if (request.capabilities != null) {
            req.putStringArray("capabilities", request.capabilities.clone());
        }
        if (request.generation != null) {
            req.putString("generation", request.generation);
        }
        if (request.sessionId != null) {
            req.putString("sessionId", request.sessionId);
        }
        if (request.includeCallback) {
            // Contract: local synchronous ResultReceiver(null) on SystemUI main thread; never pass a Handler.
            ResultReceiver receiver = new ResultReceiver(null) {
                @Override
                protected void onReceiveResult(int resultCode, Bundle resultData) {
                    if (resultCode == 0 && Looper.myLooper() == Looper.getMainLooper()) {
                        STATE.onEndpointStatePush(provider, callbackToken, parseResponse(resultData));
                    }
                }
            };
            req.putParcelable("onState", receiver);
        }
        Bundle rawResp = fn.apply(req);
        return parseResponse(rawResp);
    }

    private static LockHandoffClientState.Response parseResponse(Bundle bundle) {
        if (bundle == null) return null;
        try {
            String protocol = bundle.getString("protocol");
            String state = bundle.getString("state");
            String generation = bundle.getString("generation");
            String sessionId = bundle.getString("sessionId");
            boolean hasRequestId = bundle.containsKey("requestId");
            Object rawId = bundle.get("requestId");
            if (hasRequestId && !(rawId instanceof Long)) return null;
            long requestId = hasRequestId ? (Long) rawId : 0L;
            String[] granted = bundle.getStringArray("granted");
            Bundle deniedBundle = bundle.getBundle("denied");
            Map<String, String> denied;
            if (deniedBundle == null || deniedBundle.isEmpty()) {
                denied = Collections.emptyMap();
            } else {
                denied = new LinkedHashMap<>();
                for (String key : deniedBundle.keySet()) {
                    String reason = deniedBundle.getString(key);
                    if (reason != null) {
                        denied.put(key, reason);
                    }
                }
            }
            String error = bundle.getString("error");
            return new LockHandoffClientState.Response(
                    protocol, state, generation, sessionId,
                    hasRequestId, requestId, granted, denied, error);
        } catch (Throwable error) {
            Xp.w(TAG + "parseResponse failed: " + error);
            return null;
        }
    }

    /**
     * Hard requirement: every cleanup here MUST run synchronously on the SystemUI main thread
     * before onState/detach returns. Never defer via post().
     */
    private static void performSyncRevokeCleanup(int revokedMask) {
        if ((revokedMask & LockHandoffClientState.BIT_SHORTCUTS) != 0) {
            try { MiniPlayerRuntime.revokeShortcutsForHandoff(); } catch (Throwable t) { Xp.w(TAG + "mini shortcut revoke: " + t); }
            try { RailShortcutRuntime.revokeForHandoff(); } catch (Throwable t) { Xp.w(TAG + "shortcuts revoke: " + t); }
            try { ShortcutGestureRuntime.revokeForHandoff(); } catch (Throwable t) { Xp.w(TAG + "shortcut gesture revoke: " + t); }
        }
        if ((revokedMask & LockHandoffClientState.BIT_STATUSBAR) != 0) {
            try { LockscreenStatusBar.revokeForHandoff(); } catch (Throwable t) { Xp.w(TAG + "statusbar revoke: " + t); }
            try { RailStatusRuntime.detach(); } catch (Throwable t) { Xp.w(TAG + "rail status revoke: " + t); }
        }
        if ((revokedMask & LockHandoffClientState.BIT_TRIO) != 0) {
            try { RailStatusRuntime.revokeTrioTouchForHandoff(); } catch (Throwable t) { Xp.w(TAG + "status touch revoke: " + t); }
            try { LockScreenRailRuntime.revokeForHandoff(); } catch (Throwable t) { Xp.w(TAG + "rail runtime revoke: " + t); }
            try { MiniPlayerRuntime.revokeTrioForHandoff(); } catch (Throwable t) { Xp.w(TAG + "mini trio revoke: " + t); }
        }
        if ((revokedMask & LockHandoffClientState.BIT_NOTIFICATIONS) != 0) {
            try { ShortcutGestureRuntime.revokeContentForHandoff(); } catch (Throwable t) { Xp.w(TAG + "content gesture revoke: " + t); }
            try { MiniPlayerRuntime.revokeNotificationsForHandoff(); } catch (Throwable t) { Xp.w(TAG + "mini notifications revoke: " + t); }
            try { RailStackBounds.release(); } catch (Throwable t) { Xp.w(TAG + "stack bounds revoke: " + t); }
            try { NumState.revokeForHandoff(); } catch (Throwable t) { Xp.w(TAG + "numstate revoke: " + t); }
            try { LockIslands.revokeForHandoff(); } catch (Throwable t) { Xp.w(TAG + "lock islands revoke: " + t); }
        }
    }

    private static void refreshForGainedCapabilities(int gainedMask) {
        if ((gainedMask & LockHandoffClientState.BIT_SHORTCUTS) != 0) {
            RailShortcutRuntime.refreshForHandoff();
            MiniPlayerRuntime.refresh();
        }
        if ((gainedMask & LockHandoffClientState.BIT_STATUSBAR) != 0) {
            LockscreenStatusBar.refresh();
        }
        if ((gainedMask & (LockHandoffClientState.BIT_STATUSBAR
                | LockHandoffClientState.BIT_TRIO
                | LockHandoffClientState.BIT_NOTIFICATIONS)) != 0) {
            LockScreenRailRuntime.requestHandoffLayout();
        }
        if ((gainedMask & (LockHandoffClientState.BIT_TRIO
                | LockHandoffClientState.BIT_NOTIFICATIONS)) != 0) {
            MiniPlayerRuntime.refresh();
        }
    }
}

package com.zhuhuibin.hypermusiccover.duo;

import android.content.Context;
import android.content.ContentResolver;
import android.database.ContentObserver;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/** Extends the native current-display gate without owning view visibility or power state. */
final class InnerScreenAod {
    private static Method currentScreen, getImpl, innerDisplay;
    private static Field master, context;
    private static Class<?> hostInterface;
    private static String error = "none";
    private static long extendedCalls;
    private static final String SHARED_OPT_IN = "hmc_duo_inner_full_screen_aod";
    private static boolean aodInstalled;
    private static final java.util.Set<Class<?>> aodTypes = java.util.Collections.newSetFromMap(
            new java.util.WeakHashMap<Class<?>, Boolean>());
    private static volatile boolean aodOptIn;
    private static ContentResolver aodResolver;
    private static ContentObserver aodObserver;
    private static long aodExtensions;

    private InnerScreenAod() {}

    /** The AOD plugin has its own copy of the per-display eligibility gate. */
    static void installAod(ClassLoader loader) {
        Class<?> target = null;
        try {
            Class<?> utils = Xp.findClass("com.miui.aod.Utils", loader);
            synchronized (aodTypes) { if (!aodTypes.add(utils)) return; }
            target = utils;
            Method gate = Xp.findMethodExact(utils, "isCurrentScreenSupportFsAod", Context.class);
            Method fold = Xp.findMethodExact(utils, "isFoldDevice");
            if (gate.getReturnType() != boolean.class || fold.getReturnType() != boolean.class)
                throw new NoSuchMethodException("AOD display eligibility contract");
            Xp.hook(gate, chain -> {
                Object nativeAnswer = chain.proceed();
                if (!Boolean.FALSE.equals(nativeAnswer)) return nativeAnswer;
                try {
                    observeOptIn((Context) chain.getArg(0));
                    // A negative native answer already proves the current large display.
                    // Masters, clock style, scheduling and brightness stay with native callers.
                    if (aodOptIn && Boolean.TRUE.equals(fold.invoke(null))) {
                        aodExtensions++;
                        return true;
                    }
                } catch (Throwable failure) { error = failure.getClass().getSimpleName(); }
                return nativeAnswer;
            });
            aodInstalled = true;
            Xp.log("MCInnerAod: native AOD plugin display extension installed");
        } catch (Throwable failure) {
            if (target != null) synchronized (aodTypes) { aodTypes.remove(target); }
            error = failure.getClass().getSimpleName();
            Xp.w("MCInnerAod: AOD plugin contract unavailable: " + failure);
        }
    }

    /** One explicit cross-process choice, watched by the plugin without a frame-time query. */
    private static synchronized void observeOptIn(Context context) {
        if (aodResolver != null || context == null) return;
        ContentResolver resolver = context.getContentResolver();
        ContentObserver observer = new ContentObserver(new Handler(Looper.getMainLooper())) {
            @Override public void onChange(boolean selfChange) { readOptIn(resolver); }
        };
        resolver.registerContentObserver(Settings.Global.getUriFor(SHARED_OPT_IN), false, observer);
        aodObserver = observer;
        aodResolver = resolver;
        readOptIn(resolver);
    }

    private static void readOptIn(ContentResolver resolver) {
        try { aodOptIn = Settings.Global.getInt(resolver, SHARED_OPT_IN, 0) == 1; }
        catch (Throwable failure) { aodOptIn = false; error = failure.getClass().getSimpleName(); }
    }

    static void publishOptIn() {
        Context context = Main.sAppCtx;
        if (context == null) return;
        try {
            ContentResolver resolver = context.getContentResolver();
            int desired = Main.sInnerFullScreenAod ? 1 : 0;
            if (Settings.Global.getInt(resolver, SHARED_OPT_IN, 0) != desired
                    && !Settings.Global.putInt(resolver, SHARED_OPT_IN, desired))
                throw new IllegalStateException("AOD choice publication refused");
        } catch (Throwable failure) {
            error = failure.getClass().getSimpleName();
            Xp.w("MCInnerAod: choice publication unavailable: " + failure);
        }
    }

    static void install(ClassLoader loader) {
        if (currentScreen != null) return;
        try {
            Class<?> host = Xp.findClass("com.android.keyguard.injector.DozeServiceHostInjector", loader);
            Method gate = Xp.findMethodExact(host, "isCurrentScreenFullAodEnabled");
            if (gate.getReturnType() != boolean.class) throw new NoSuchMethodException("current display gate");
            Field nativeMaster = host.getDeclaredField("mFullAodEnable");
            Field nativeContext = host.getDeclaredField("mContext");
            if (nativeMaster.getType() != boolean.class || !Context.class.isAssignableFrom(nativeContext.getType()))
                throw new NoSuchFieldException("native AOD contract");
            nativeMaster.setAccessible(true);
            nativeContext.setAccessible(true);
            Method isInner = Xp.findMethodExact(Xp.findClass("com.miui.utils.configs.MiuiConfigs", loader),
                    "isFoldLargeScreenAndNotPad", Context.class);
            if (isInner.getReturnType() != boolean.class) throw new NoSuchMethodException("inner display gate");
            Class<?> iface = Xp.findClass("com.miui.sysuiinterfaces.IDozeServiceHost", loader);
            Method lookup = Xp.findMethodExact(Xp.findClass(
                    "com.miui.systemui.interfacesmanager.InterfacesImplManager", loader), "getImpl", Class.class);
            Xp.hook(gate, chain -> {
                Object result = chain.proceed();
                if (!Boolean.FALSE.equals(result) || !Main.sInnerFullScreenAod) return result;
                try {
                    Object instance = chain.getThisObject();
                    boolean inner = Boolean.TRUE.equals(isInner.invoke(null, nativeContext.get(instance)));
                    boolean enable = InnerScreenAodPolicy.enabled(false, true,
                            nativeMaster.getBoolean(instance), inner);
                    if (enable) extendedCalls++;
                    return enable;
                } catch (Throwable failure) {
                    // An OS change must keep its own answer, never force an unknown display on.
                    error = failure.getClass().getSimpleName();
                    return result;
                }
            });
            master = nativeMaster;
            context = nativeContext;
            innerDisplay = isInner;
            hostInterface = iface;
            getImpl = lookup;
            currentScreen = gate;
            installPluginLifecycle(loader);
            Xp.log("MCInnerAod: native current-display extension installed");
        } catch (Throwable failure) {
            error = failure.getClass().getSimpleName();
            Xp.w("MCInnerAod: native current-display contract unavailable: " + failure);
        }
    }

    /** The live plugin can use a loader distinct from the AOD settings application's loader. */
    private static void installPluginLifecycle(ClassLoader loader) {
        try {
            Class<?> owner = Xp.findClass("com.android.keyguard.doze.MiuiDozeService", loader);
            for (Method method : owner.getDeclaredMethods()) {
                if (!method.getName().equals("onPluginConnected") || method.getParameterCount() != 2
                        || method.getParameterTypes()[1] != Context.class || method.getReturnType() != void.class) continue;
                Xp.hook(method, chain -> {
                    Object plugin = chain.getArg(0);
                    if (plugin != null) installAod(plugin.getClass().getClassLoader());
                    return chain.proceed();
                });
            }
        } catch (Throwable failure) {
            Xp.w("MCInnerAod: live plugin lifecycle unavailable: " + failure);
        }
    }

    /** Null only when this ROM has no known per-display contract; callers retain legacy fallback. */
    static Boolean currentScreenEnabled() {
        if (currentScreen == null) return null;
        try {
            Object host = getImpl.invoke(null, hostInterface);
            return host == null ? Boolean.FALSE : (Boolean) currentScreen.invoke(host);
        } catch (Throwable failure) {
            error = failure.getClass().getSimpleName();
            return Boolean.FALSE;
        }
    }

    static String describe() {
        String state = "installed=" + (currentScreen != null) + " optIn=" + Main.sInnerFullScreenAod;
        try {
            Object host = getImpl == null ? null : getImpl.invoke(null, hostInterface);
            if (host != null) state += " master=" + master.getBoolean(host)
                    + " inner=" + innerDisplay.invoke(null, context.get(host))
                    + " current=" + currentScreen.invoke(host);
        } catch (Throwable failure) { error = failure.getClass().getSimpleName(); }
        return state + " extensions=" + extendedCalls + " plugin=" + aodInstalled
                + " pluginOptIn=" + aodOptIn + " pluginExtensions=" + aodExtensions + " error=" + error;
    }
}

package com.zhuhuibin.hypermusiccover.duo;

import android.content.Context;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/** Extends the native current-display gate without owning view visibility or power state. */
final class InnerScreenAod {
    private static Method currentScreen, getImpl, innerDisplay;
    private static Field master, context;
    private static Class<?> hostInterface;
    private static String error = "none";
    private static long extendedCalls;

    private InnerScreenAod() {}

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
            Xp.log("MCInnerAod: native current-display extension installed");
        } catch (Throwable failure) {
            error = failure.getClass().getSimpleName();
            Xp.w("MCInnerAod: native current-display contract unavailable: " + failure);
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
        return state + " extensions=" + extendedCalls + " error=" + error;
    }
}

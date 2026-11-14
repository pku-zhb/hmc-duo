package com.zhuhuibin.hypermusiccover.duo;

/** Only the OEM notification player background is a source, never an island or module overlay. */
final class NativeMaterialSource {
    private NativeMaterialSource() {}

    static boolean ownedTarget(Object target) {
        return target instanceof NativeMaterialTarget;
    }

    static boolean accepts(String resourceName, Iterable<String> lineage, boolean registeredTarget) {
        if (registeredTarget || !"media_bg".equals(resourceName)) return false;
        boolean media = false;
        for (String name : lineage) {
            String lower = name.toLowerCase(java.util.Locale.ROOT);
            if (name.startsWith("com.zhuhuibin.") || name.startsWith("btm.m.") ||
                    lower.contains("mediaisland") || lower.contains("dynamicisland") ||
                    lower.contains("playerisland")) return false;
            if (name.startsWith("com.android.systemui.statusbar.notification.mediacontrol.")) media = true;
        }
        return media;
    }
}

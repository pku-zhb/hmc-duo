package com.zhuhuibin.hypermusiccover.duo;

import android.content.res.Resources;
import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.function.ToIntBiFunction;

/** Positive SystemUI IDs are stable within a Resources owner; a missing late asset is retried. */
final class ResourceIds {
    private static final Cache<Resources> IDS = new Cache<>();
    static int id(Resources resources, String name) {
        return IDS.get(resources, name, (r, n) -> r.getIdentifier(n, "id", "com.android.systemui"));
    }
    static final class Cache<R> {
        private final Map<R, Map<String, Integer>> owners = new WeakHashMap<>();
        synchronized int get(R owner, String name, ToIntBiFunction<R, String> resolve) {
            Map<String, Integer> ids = owners.get(owner);
            Integer found = ids == null ? null : ids.get(name);
            if (found != null) return found;
            int value = resolve.applyAsInt(owner, name);
            if (value != 0) owners.computeIfAbsent(owner, ignored -> new HashMap<>()).put(name, value);
            return value;
        }
    }
    private ResourceIds() {}
}

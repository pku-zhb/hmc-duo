package com.zhuhuibin.hypermusiccover.duo;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;
import java.util.WeakHashMap;

/** Coalesces legacy clear/add calls without retaining detached render targets. */
final class NativePaletteBatch<T> {
    private final Map<T, Integer> depth = new WeakHashMap<>();
    private final Map<T, Boolean> dirty = new WeakHashMap<>();

    synchronized void begin(T target) { depth.put(target, depth.getOrDefault(target, 0) + 1); }

    /** True means no complete recipe owns the write, so a frame callback must flush it. */
    synchronized boolean mark(T target) {
        dirty.put(target, Boolean.TRUE);
        return !depth.containsKey(target);
    }

    /** True means the outermost complete recipe has one final palette to restore. */
    synchronized boolean end(T target) {
        Integer count = depth.get(target);
        if (count == null) return false;
        if (count > 1) { depth.put(target, count - 1); return false; }
        depth.remove(target);
        return dirty.remove(target) != null;
    }

    /** An explicit complete palette supersedes any preceding legacy writes. */
    synchronized void completed(T target) { dirty.remove(target); }
    synchronized void remove(T target) { dirty.remove(target); depth.remove(target); }

    synchronized ArrayList<T> drain() {
        ArrayList<T> result = new ArrayList<>();
        Iterator<T> pending = dirty.keySet().iterator();
        while (pending.hasNext()) {
            T target = pending.next();
            if (depth.containsKey(target)) continue;
            result.add(target);
            pending.remove();
        }
        return result;
    }
}

package com.zhuhuibin.hypermusiccover.duo;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.WeakHashMap;

/** Exact weak pairs; interface lineage is never registration. */
final class NativeOwnedMaterialRegistry<T> {
    static final int GLASS = 1, BLUR = 2;
    private static final class Pair<T> {
        final WeakReference<T> element, container;
        boolean claimed;
        Pair(T element, T container) {
            this.element = new WeakReference<>(element);
            this.container = new WeakReference<>(container);
        }
    }
    private final WeakHashMap<T, Pair<T>> pairs = new WeakHashMap<>();

    synchronized boolean register(T element, T container) {
        if (element == null || container == null || element == container) return false;
        Pair<T> current = pair(element);
        if (current != null && current.container.get() == container) return true;
        // Each carrier owns one material element. A sibling cannot steal its claim or
        // become an unclaimed second route from the player into that carrier.
        if (current != null || pair(container) != null) return false;
        Pair<T> next = new Pair<>(element, container);
        pairs.put(element, next); pairs.put(container, next);
        return true;
    }

    synchronized void remove(T target) {
        Pair<T> pair = pair(target);
        if (pair == null) { pairs.remove(target); return; }
        pairs.remove(pair.element.get()); pairs.remove(pair.container.get());
    }

    private Pair<T> pair(T target) {
        if (target == null) return null;
        Pair<T> pair = pairs.get(target);
        if (pair == null) return null;
        T element = pair.element.get(), container = pair.container.get();
        return element != null && container != null && (target == element || target == container)
                && pairs.get(element) == pair && pairs.get(container) == pair ? pair : null;
    }

    synchronized T owner(T target) {
        Pair<T> pair = pair(target);
        return pair == null ? null : pair.element.get();
    }
    synchronized T container(T target) {
        Pair<T> pair = pair(target);
        return pair == null ? null : pair.container.get();
    }
    synchronized int role(T target) {
        Pair<T> pair = pair(target);
        return pair == null ? 0 : pair.element.get() == target ? GLASS : BLUR;
    }
    synchronized boolean claim(T target, int version) {
        Pair<T> pair = pair(target);
        if (version != 1 || pair == null) return false;
        pair.claimed = true;
        return true;
    }
    synchronized boolean claimed(T target) {
        Pair<T> pair = pair(target);
        return pair != null && pair.claimed;
    }
    synchronized ArrayList<T> owners() {
        ArrayList<T> result = new ArrayList<>();
        for (T key : new ArrayList<>(pairs.keySet())) if (role(key) == GLASS) result.add(key);
        return result;
    }
}

package com.zhuhuibin.hypermusiccover.duo;

import android.content.res.AssetManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.function.Function;

/** Cache resource names, never live dimensions, Views, or native animation state. */
final class LayoutResourceIds {
    private static final Cache<Resources, Configuration, Assets> IDS = new Cache<>();
    private static final Method APK_ASSETS = apkAssetsMethod();
    private static final ThreadLocal<Scope> CURRENT = new ThreadLocal<>();

    /** A synchronous layout operation resolves each Resources asset table at most once. */
    static Scope scope() { return new Scope(); }
    static final class Scope implements AutoCloseable {
        final Scope previous = CURRENT.get();
        final Map<Resources, Assets> assets = new IdentityHashMap<>();
        Scope() { CURRENT.set(this); }
        Assets assets(Resources resources) {
            if (!assets.containsKey(resources)) assets.put(resources, readAssets(resources));
            return assets.get(resources);
        }
        @Override public void close() {
            if (CURRENT.get() != this) throw new IllegalStateException("unbalanced layout resource scope");
            assets.clear();
            if (previous == null) CURRENT.remove(); else CURRENT.set(previous);
        }
    }

    static int get(Resources resources, String type, String name) {
        Scope scope = CURRENT.get();
        Assets assets = scope == null ? readAssets(resources) : scope.assets(resources);
        // Without an observable asset-table boundary, preserve native late-resource behavior.
        if (assets == null) return resources.getIdentifier(name, type, "com.android.systemui");
        return IDS.get(resources, resources.getConfiguration(), assets, type, name,
                Configuration::new, (owner, kind, entry) -> owner.getIdentifier(entry, kind, "com.android.systemui"));
    }

    private static Method apkAssetsMethod() {
        try {
            Method method = AssetManager.class.getDeclaredMethod("getApkAssets");
            method.setAccessible(true);
            return method;
        } catch (Throwable unavailable) { return null; }
    }
    private static Assets readAssets(Resources resources) {
        if (APK_ASSETS == null) return null;
        try {
            AssetManager manager = resources.getAssets();
            Object value = APK_ASSETS.invoke(manager);
            return value instanceof Object[] ? new Assets(manager, (Object[]) value) : null;
        } catch (Throwable unavailable) { return null; }
    }

    /** A manager can replace its ApkAssets in place without changing its own identity. */
    static final class Assets {
        private final Object manager;
        private final Object[] entries;
        Assets(Object manager, Object[] entries) { this.manager = manager; this.entries = entries.clone(); }
        @Override public boolean equals(Object value) {
            if (!(value instanceof Assets)) return false;
            Assets other = (Assets) value;
            if (manager != other.manager || entries.length != other.entries.length) return false;
            for (int i = 0; i < entries.length; ++i) if (entries[i] != other.entries[i]) return false;
            return true;
        }
        @Override public int hashCode() { return System.identityHashCode(manager); }
    }

    interface Resolver<R> { int resolve(R owner, String type, String name); }

    static final class Cache<R, C, A> {
        private static final class Epoch<C, A> {
            final C configuration;
            final A assets;
            final Map<String, Map<String, Integer>> identifiers = new HashMap<>();
            Epoch(C configuration, A assets) { this.configuration = configuration; this.assets = assets; }
        }
        private final Map<R, Epoch<C, A>> owners = new WeakHashMap<>();

        synchronized int get(R owner, C configuration, A assets, String type, String name,
                             Function<C, C> snapshot, Resolver<R> resolve) {
            Epoch<C, A> epoch = owners.get(owner);
            if (epoch == null || !epoch.assets.equals(assets) || !epoch.configuration.equals(configuration)) {
                epoch = new Epoch<>(snapshot.apply(configuration), assets);
                owners.put(owner, epoch);
            }
            Map<String, Integer> entries = epoch.identifiers.computeIfAbsent(type, ignored -> new HashMap<>());
            Integer found = entries.get(name);
            if (found != null) return found;
            int value = resolve.resolve(owner, type, name);
            // Missing resource names are stable only within this assets/configuration epoch.
            // A positive ID still performs findViewById at its caller on every native frame.
            entries.put(name, value);
            return value;
        }
    }

    private LayoutResourceIds() {}
}

package com.zhuhuibin.hypermusiccover.duo;

import org.junit.Test;
import static org.junit.Assert.*;

public class LayoutResourceIdsTest {
    private static final class Config {
        int orientation, night, assetsSequence;
        float fontScale = 1f;
        String locale = "en";
        Config() {}
        Config(Config other) {
            orientation = other.orientation; night = other.night; assetsSequence = other.assetsSequence;
            fontScale = other.fontScale; locale = other.locale;
        }
        @Override public boolean equals(Object value) {
            if (!(value instanceof Config)) return false;
            Config other = (Config) value;
            return orientation == other.orientation && night == other.night
                    && assetsSequence == other.assetsSequence && fontScale == other.fontScale && locale.equals(other.locale);
        }
        @Override public int hashCode() { return orientation; }
    }

    @Test public void repeatedFramesBoundSuccessfulAndMissingNameResolution() {
        LayoutResourceIds.Cache<Object, Config, Object> cache = new LayoutResourceIds.Cache<>();
        Object resources = new Object(), assets = new Object();
        Config config = new Config(); int[] resolutions = {0}, snapshots = {0};
        for (int i = 0; i < 1000; ++i) {
            assertEquals(7, cache.get(resources, config, assets, "id", "player",
                    c -> { snapshots[0]++; return new Config(c); }, (r, t, n) -> { resolutions[0]++; return 7; }));
            assertEquals(0, cache.get(resources, config, assets, "id", "optional_label",
                    Config::new, (r, t, n) -> { resolutions[0]++; return 0; }));
        }
        assertEquals(2, resolutions[0]);
        assertEquals(1, snapshots[0]);
        assertEquals(19, cache.get(resources, config, assets, "dimen", "player", Config::new,
                (r, t, n) -> { assertEquals("dimen", t); return 19; }));
    }

    @Test public void inPlaceConfigurationChangesInvalidateTheCopiedEpoch() {
        LayoutResourceIds.Cache<Object, Config, Object> cache = new LayoutResourceIds.Cache<>();
        Object resources = new Object(), assets = new Object(); Config config = new Config();
        int[] calls = {0};
        LayoutResourceIds.Resolver<Object> resolve = (r, t, n) -> ++calls[0];
        assertEquals(1, cache.get(resources, config, assets, "dimen", "height", Config::new, resolve));
        config.orientation++;
        assertEquals(2, cache.get(resources, config, assets, "dimen", "height", Config::new, resolve));
        config.night++;
        assertEquals(3, cache.get(resources, config, assets, "dimen", "height", Config::new, resolve));
        config.fontScale = 1.2f;
        assertEquals(4, cache.get(resources, config, assets, "dimen", "height", Config::new, resolve));
        config.locale = "zh";
        assertEquals(5, cache.get(resources, config, assets, "dimen", "height", Config::new, resolve));
        config.assetsSequence++;
        assertEquals(6, cache.get(resources, config, assets, "dimen", "height", Config::new, resolve));
        assertEquals(6, cache.get(resources, config, assets, "dimen", "height", Config::new, resolve));
    }

    @Test public void missingNamesRetryAfterAssetsReplacementAndNeverCrossResources() {
        LayoutResourceIds.Cache<Object, Config, Object> cache = new LayoutResourceIds.Cache<>();
        Object first = new Object(), second = new Object(), oldAssets = new Object(), newAssets = new Object();
        Config config = new Config();
        assertEquals(0, cache.get(first, config, oldAssets, "id", "late_resource", Config::new, (r, t, n) -> 0));
        assertEquals(0, cache.get(first, config, oldAssets, "id", "late_resource", Config::new,
                (r, t, n) -> { throw new AssertionError("missing name was resolved again in one epoch"); }));
        assertEquals(42, cache.get(first, config, newAssets, "id", "late_resource", Config::new, (r, t, n) -> 42));
        assertEquals(81, cache.get(second, config, newAssets, "id", "late_resource", Config::new, (r, t, n) -> 81));
        config.assetsSequence++;
        assertEquals(43, cache.get(first, config, newAssets, "id", "late_resource", Config::new, (r, t, n) -> 43));
    }

    @Test public void failedResolutionDoesNotBecomeAPermanentMissingResource() {
        LayoutResourceIds.Cache<Object, Config, Object> cache = new LayoutResourceIds.Cache<>();
        Object resources = new Object(), assets = new Object(); Config config = new Config();
        try {
            cache.get(resources, config, assets, "id", "player", Config::new,
                    (r, t, n) -> { throw new IllegalStateException("resources not ready"); });
            fail("Expected native resource failure");
        } catch (IllegalStateException expected) { }
        assertEquals(7, cache.get(resources, config, assets, "id", "player", Config::new, (r, t, n) -> 7));
    }

    @Test public void sameManagerReplacingApkAssetsRetriesPreviouslyMissingNames() {
        LayoutResourceIds.Cache<Object, Config, LayoutResourceIds.Assets> cache = new LayoutResourceIds.Cache<>();
        Object resources = new Object(), manager = new Object(), firstApk = new Object(), nextApk = new Object();
        Config config = new Config(); Object[] mutableTable = {firstApk};
        LayoutResourceIds.Assets old = new LayoutResourceIds.Assets(manager, mutableTable);
        assertEquals(0, cache.get(resources, config, old, "id", "late_resource", Config::new, (r, t, n) -> 0));
        assertEquals(0, cache.get(resources, config, new LayoutResourceIds.Assets(manager, new Object[]{firstApk}),
                "id", "late_resource", Config::new,
                (r, t, n) -> { throw new AssertionError("equivalent ApkAssets copied array changed the epoch"); }));
        mutableTable[0] = nextApk;
        LayoutResourceIds.Assets replaced = new LayoutResourceIds.Assets(manager, mutableTable);
        assertNotEquals(old, replaced);
        assertEquals(42, cache.get(resources, config, replaced, "id", "late_resource", Config::new, (r, t, n) -> 42));
    }

    @Test public void nestedFailureRestoresOuterScopeAndReleasesThreadLocalOnReturn() {
        try (LayoutResourceIds.Scope outer = LayoutResourceIds.scope()) {
            assertNull(outer.previous);
            try (LayoutResourceIds.Scope nested = LayoutResourceIds.scope()) {
                assertSame(outer, nested.previous);
                throw new IllegalStateException("native layout failed");
            } catch (IllegalStateException expected) { }
            try (LayoutResourceIds.Scope afterFailure = LayoutResourceIds.scope()) {
                assertSame(outer, afterFailure.previous);
            }
        }
        try (LayoutResourceIds.Scope nextLayout = LayoutResourceIds.scope()) {
            assertNull(nextLayout.previous);
        }
    }

    @Test public void synchronousLookupScopeNeverCrossesThreads() throws Exception {
        java.util.concurrent.atomic.AtomicReference<Throwable> failure = new java.util.concurrent.atomic.AtomicReference<>();
        try (LayoutResourceIds.Scope outer = LayoutResourceIds.scope()) {
            Thread thread = new Thread(() -> {
                try (LayoutResourceIds.Scope separate = LayoutResourceIds.scope()) { assertNull(separate.previous); }
                catch (Throwable error) { failure.set(error); }
            });
            thread.start(); thread.join();
            if (failure.get() != null) throw new AssertionError(failure.get());
            try (LayoutResourceIds.Scope sameThread = LayoutResourceIds.scope()) { assertSame(outer, sameThread.previous); }
        }
    }
}

package com.zhuhuibin.hypermusiccover.duo;

import org.junit.Test;
import static org.junit.Assert.*;

public class NativeOwnedMaterialScopeTest {
    @Test public void repeatedProviderQueriesAndNestedSettersResolveSceneOnce() {
        NativeOwnedMaterialScope<Object> scope = new NativeOwnedMaterialScope<>();
        Object owner = new Object(), container = new Object(), unrelated = new Object();
        int[] resolutions = {0};
        scope.begin(owner, () -> { resolutions[0]++; return new NativeOwnedMaterialScope.Snapshot<>(owner, container, 8); });
        for (int i = 0; i < 1000; ++i) {
            assertEquals(8, scope.snapshot(owner).state);
            assertSame(owner, scope.snapshot(container).owner);
            assertNull(scope.snapshot(unrelated));
        }
        scope.begin(container, () -> { throw new AssertionError("nested pair resolved again"); });
        scope.end(container);
        assertEquals(1, resolutions[0]);
        scope.end(owner);
        assertNull(scope.snapshot(owner));
    }
    @Test public void nextRecipeObservesSceneAndContainerChangesWithoutTimers() {
        NativeOwnedMaterialScope<Object> scope = new NativeOwnedMaterialScope<>();
        Object owner = new Object(), first = new Object(), next = new Object();
        scope.begin(owner, () -> new NativeOwnedMaterialScope.Snapshot<>(owner, first, 8));
        scope.end(owner);
        scope.begin(owner, () -> new NativeOwnedMaterialScope.Snapshot<>(owner, next, 16));
        assertNull(scope.snapshot(first));
        assertEquals(16, scope.snapshot(next).state);
        scope.end(owner);
    }
    @Test public void independentNestedRecipesRestoreOuterSnapshot() {
        NativeOwnedMaterialScope<Object> scope = new NativeOwnedMaterialScope<>();
        Object a = new Object(), b = new Object(), c = new Object(), d = new Object();
        scope.begin(a, () -> new NativeOwnedMaterialScope.Snapshot<>(a, b, 8));
        try {
            scope.begin(c, () -> new NativeOwnedMaterialScope.Snapshot<>(c, d, 16));
            try { assertEquals(16, scope.snapshot(d).state); }
            finally { scope.end(c); }
            assertEquals(8, scope.snapshot(b).state);
        } finally { scope.end(a); }
        assertNull(scope.snapshot(a));
        assertNull(scope.snapshot(c));
    }
}

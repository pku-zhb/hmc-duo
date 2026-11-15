package com.zhuhuibin.hypermusiccover.duo;

import org.junit.Test;
import static org.junit.Assert.*;

public class NativeMaterialNodeCacheTest {
    private static float[] recipe(float value) {
        float[] result = new float[42];
        result[34] = value;
        return result;
    }

    @Test public void aFirstUnchangedOrUnknownWriteCannotInventAnAcceptedRecipe() {
        NativeMaterialNodeCache<Object> cache = new NativeMaterialNodeCache<>();
        Object node = new Object();
        assertNull(cache.glass(node, recipe(.6f), true));
        cache.track(node);
        assertNull(cache.glass(node, recipe(.6f), false));
        assertNull(cache.blur(node, 8, 24, false));
    }

    @Test public void sceneProfileLossCanRecoverAnUnchangedPreviouslyCommittedNode() {
        NativeMaterialNodeCache<Object> cache = new NativeMaterialNodeCache<>();
        Object node = new Object();
        cache.track(node);
        assertArrayEquals(recipe(.6f), cache.glass(node, recipe(.6f), true), 0f);
        // Scene/profile ownership is separate; entering keyguard can repeat the same write.
        assertArrayEquals(recipe(.6f), cache.glass(node, recipe(.6f), false), 0f);
        assertArrayEquals(new int[]{8, 24}, cache.blur(node, 8, 24, true));
        assertArrayEquals(new int[]{8, 24}, cache.blur(node, 8, 24, false));
    }

    @Test public void newerRealWritesInvalidateStaleReplayWithoutAcceptingRejectedValues() {
        NativeMaterialNodeCache<Object> cache = new NativeMaterialNodeCache<>();
        Object node = new Object();
        cache.track(node);
        cache.glass(node, recipe(.6f), true);
        cache.glass(node, recipe(.3f), true);
        assertNull(cache.glass(node, recipe(.6f), false));
        assertNull(cache.glass(node, recipe(.9f), false));
        assertArrayEquals(recipe(.3f), cache.glass(node, recipe(.3f), false), 0f);
        cache.blur(node, 8, 24, true);
        cache.blur(node, 4, 12, true);
        assertNull(cache.blur(node, 8, 24, false));
        assertArrayEquals(new int[]{4, 12}, cache.blur(node, 4, 12, false));
    }

    @Test public void aNewHolderCannotBorrowTheOldRenderNodesCommit() {
        NativeMaterialNodeCache<Object> cache = new NativeMaterialNodeCache<>();
        Object old = new Object(), replacement = new Object();
        cache.track(old);
        cache.glass(old, recipe(.6f), true);
        cache.track(replacement);
        assertNull(cache.glass(replacement, recipe(.6f), false));
        assertFalse(cache.tracked(new Object()));
        assertFalse(cache.tracked(null));
    }

    @Test public void invalidWritesAndMutableCallerArraysCannotPoisonAcceptedEvidence() {
        NativeMaterialNodeCache<Object> cache = new NativeMaterialNodeCache<>();
        Object node = new Object();
        cache.track(node);
        float[] input = recipe(.6f);
        float[] output = cache.glass(node, input, true);
        input[34] = .9f;
        output[34] = .2f;
        assertArrayEquals(recipe(.6f), cache.glass(node, recipe(.6f), false), 0f);
        float[] invalid = recipe(Float.NaN);
        assertNull(cache.glass(node, invalid, true));
        assertNull(cache.glass(node, recipe(.6f), false));
        cache.blur(node, 8, 24, true);
        assertNull(cache.blur(node, -1, 24, true));
        assertNull(cache.blur(node, 8, 24, false));
    }

    @Test public void completeFrameReadbackUsesOnlyAcceptedImmutableNodeState() {
        NativeMaterialNodeCache<Object> cache = new NativeMaterialNodeCache<>();
        Object node = new Object();
        assertNull(cache.currentGlass(node));
        assertNull(cache.currentBlur(node));
        cache.track(node);
        cache.glass(node, recipe(.6f), true);
        cache.blur(node, 8, 24, true);
        // A rejected setter and an outside caller cannot replace the accepted snapshot.
        cache.glass(node, recipe(.9f), false);
        cache.blur(node, 2, 6, false);
        float[] glass = cache.currentGlass(node);
        int[] blur = cache.currentBlur(node);
        glass[34] = .1f; blur[0] = 0;
        assertArrayEquals(recipe(.6f), cache.currentGlass(node), 0f);
        assertArrayEquals(new int[]{8, 24}, cache.currentBlur(node));
        cache.glass(node, null, true);
        assertNull(cache.currentGlass(node));
    }

    @Test public void aThrowAfterAnUncertainNativeWriteCannotPromoteOldGlassOnAPaletteOnlyFrame() {
        NativeMaterialNodeCache<Object> cache = new NativeMaterialNodeCache<>();
        NativeMaterialBatch<Object> batches = new NativeMaterialBatch<>();
        Object node = new Object(), source = new Object();
        cache.track(node);
        cache.glass(node, recipe(.6f), true);
        cache.blur(node, 8, 24, true);
        // Native may already have accepted B before a downstream hook throws.
        // Keep the original exception path, but retire only that uncertain channel.
        cache.uncertainGlass(node);
        assertArrayEquals(new int[]{8, 24}, cache.currentBlur(node));
        NativeMaterialBatch.Frame<Object> palette = batches.begin(source);
        batches.defer(source);
        assertTrue(batches.end(palette, true));
        assertNull(cache.currentGlass(node));
        assertNull(cache.glass(node, recipe(.6f), false));
        cache.glass(node, recipe(.9f), true);
        cache.uncertainBlur(node);
        assertArrayEquals(recipe(.9f), cache.currentGlass(node), 0f);
        assertNull(cache.currentBlur(node));
    }
}

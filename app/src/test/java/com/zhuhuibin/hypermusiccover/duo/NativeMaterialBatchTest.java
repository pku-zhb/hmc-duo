package com.zhuhuibin.hypermusiccover.duo;

import org.junit.Test;
import java.util.ArrayList;
import java.util.List;
import static org.junit.Assert.*;

public class NativeMaterialBatchTest {
    @Test public void clearAndEachAddCannotPublishAnIncompleteSourcePalette() {
        NativeMaterialBatch<Object> batches = new NativeMaterialBatch<>();
        Object source = new Object();
        List<Integer> nativePalette = new ArrayList<>(List.of(10, 20, 30));
        List<Integer> followerPalette = new ArrayList<>(nativePalette);
        NativeMaterialBatch.Frame<Object> frame = batches.begin(source);
        nativePalette.clear();
        assertTrue(batches.defer(source));
        assertEquals(List.of(10, 20, 30), followerPalette);
        for (int color : new int[]{40, 50, 60}) {
            nativePalette.add(color);
            assertTrue(batches.defer(source));
            // A synchronous follower callback sees the last complete recipe until return.
            assertEquals(List.of(10, 20, 30), followerPalette);
        }
        if (batches.end(frame, true)) followerPalette = new ArrayList<>(nativePalette);
        assertEquals(List.of(40, 50, 60), followerPalette);
        assertFalse(batches.active(source));
    }

    @Test public void nestedBlurInitializationPublishesOnlyAtTheOuterGlassEffectBoundary() {
        NativeMaterialBatch<Object> batches = new NativeMaterialBatch<>();
        Object source = new Object();
        NativeMaterialBatch.Frame<Object> glass = batches.begin(source);
        NativeMaterialBatch.Frame<Object> blur = batches.begin(source);
        assertTrue(batches.defer(source));
        assertFalse(batches.end(blur, true));
        assertTrue(batches.active(source));
        assertTrue(batches.defer(source));
        assertTrue(batches.end(glass, true));
        assertFalse(batches.active(source));
    }

    @Test public void firstWakePaletteCanUseTheActualUnchangedNativeGlassWithoutASecondSetter() {
        NativeMaterialBatch<Object> batches = new NativeMaterialBatch<>();
        NativeMaterialNodeCache<Object> nodes = new NativeMaterialNodeCache<>();
        Object source = new Object(), node = new Object();
        nodes.track(node);
        float[] accepted = new float[42]; accepted[0] = .75f;
        nodes.glass(node, accepted, true);
        GlassProfile sleeping = GlassProfile.capture(accepted, 32, false);
        assertNull(sleeping.forScene(32, true));
        NativeMaterialBatch.Frame<Object> wake = batches.begin(source);
        // The native frame changes its palette but deliberately leaves glass untouched.
        batches.defer(source);
        assertTrue(batches.end(wake, true));
        GlassProfile current = GlassProfile.capture(nodes.currentGlass(node), 32, true);
        assertArrayEquals(accepted, current.forScene(32, true), 0f);
    }

    @Test public void aThrowingNativeEffectDoesNotPublishItsPartialRecipe() {
        NativeMaterialBatch<Object> batches = new NativeMaterialBatch<>();
        Object source = new Object();
        NativeMaterialBatch.Frame<Object> outer = batches.begin(source);
        NativeMaterialBatch.Frame<Object> inner = batches.begin(source);
        batches.defer(source);
        assertFalse(batches.end(inner, false));
        // Even if an outer OEM method catches the failure, this was a partial recipe.
        assertFalse(batches.end(outer, true));
        assertFalse(batches.active(source));
        NativeMaterialBatch.Frame<Object> retry = batches.begin(source);
        batches.defer(source);
        assertTrue(batches.end(retry, true));
    }

    @Test public void aCaughtInnerFailureKeepsBothThePreviousRelayAndRecordedRecipe() {
        NativeMaterialBatch<Object> batches = new NativeMaterialBatch<>();
        Object source = new Object();
        List<String> previous = List.of("complete blend", "complete glass");
        List<String> recorded = new ArrayList<>();
        NativeMaterialBatch.Frame<Object> outer = batches.begin(source);
        NativeMaterialBatch.Frame<Object> blur = batches.begin(source);
        recorded.add("partial blur clear");
        batches.defer(source);
        assertFalse(batches.end(blur, false));
        // The outer method catches the inner failure and returns normally. Both the
        // snapshot publisher and the independent legacy recorder must share the verdict.
        boolean publishRelay = batches.end(outer, true);
        List<String> relay = publishRelay ? recorded : previous;
        List<String> recipe = batches.failed(outer) ? previous : recorded;
        assertSame(previous, relay);
        assertSame(previous, recipe);
        assertTrue(batches.failed(outer));
        NativeMaterialBatch.Frame<Object> next = batches.begin(source);
        assertFalse(batches.failed(next));
        batches.defer(source);
        assertTrue(batches.end(next, true));
    }

    @Test public void anotherSourceCannotFinishOrDirtyTheCurrentSourcesTransaction() {
        NativeMaterialBatch<Object> batches = new NativeMaterialBatch<>();
        Object first = new Object(), second = new Object();
        NativeMaterialBatch.Frame<Object> one = batches.begin(first);
        assertFalse(batches.defer(second));
        NativeMaterialBatch.Frame<Object> two = batches.begin(second);
        batches.defer(second);
        assertTrue(batches.end(two, true));
        assertTrue(batches.active(first));
        assertFalse(batches.active(second));
        assertFalse(batches.end(one, true));
    }

    @Test public void samplingStaysDeferredUntilEverySetterInTheOuterRecipeHasReturned() {
        NativeMaterialBatch<Object> batches = new NativeMaterialBatch<>();
        Object source = new Object();
        NativeMaterialBatch.Frame<Object> effect = batches.begin(source);
        for (int i = 0; i < 12; i++) {
            assertTrue(batches.active(source));
            batches.defer(source);
        }
        assertTrue(batches.end(effect, true));
        assertFalse(batches.active(source));
        assertFalse(batches.defer(source));
    }
}

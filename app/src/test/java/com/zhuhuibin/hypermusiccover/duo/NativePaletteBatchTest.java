package com.zhuhuibin.hypermusiccover.duo;

import org.junit.Test;
import java.util.List;
import static org.junit.Assert.*;

public class NativePaletteBatchTest {
    @Test public void twoClearsAndThreeAddsRestoreOnceAtTheCompleteRecipeBoundary() {
        NativePaletteBatch<Object> batch = new NativePaletteBatch<>();
        Object view = new Object();
        batch.begin(view);
        for (int i = 0; i < 5; ++i) assertFalse(batch.mark(view));
        assertTrue(batch.drain().isEmpty());
        assertTrue(batch.end(view));
        assertFalse(batch.end(view));
        assertTrue(batch.drain().isEmpty());
    }

    @Test public void nestedRecipesDoNotExposeAnIntermediatePalette() {
        NativePaletteBatch<Object> batch = new NativePaletteBatch<>();
        Object view = new Object();
        batch.begin(view);
        batch.mark(view);
        batch.begin(view);
        batch.mark(view);
        assertFalse(batch.end(view));
        assertTrue(batch.drain().isEmpty());
        assertTrue(batch.end(view));
    }

    @Test public void unwrappedLegacyCallsCoalesceOncePerTargetPerFrame() {
        NativePaletteBatch<Object> batch = new NativePaletteBatch<>();
        Object first = new Object(), second = new Object();
        for (int i = 0; i < 20; ++i) assertTrue(batch.mark(first));
        batch.mark(second);
        List<Object> frame = batch.drain();
        assertEquals(2, frame.size());
        assertTrue(frame.contains(first));
        assertTrue(frame.contains(second));
        assertTrue(batch.drain().isEmpty());
        batch.mark(first);
        assertEquals(1, batch.drain().size());
    }

    @Test public void completeBulkPaletteCancelsPriorLegacyReplayWithoutClosingTheRecipe() {
        NativePaletteBatch<Object> batch = new NativePaletteBatch<>();
        Object view = new Object();
        batch.begin(view);
        batch.mark(view);
        batch.completed(view);
        assertFalse(batch.end(view));
        assertTrue(batch.drain().isEmpty());
        batch.mark(view);
        batch.completed(view);
        assertTrue(batch.drain().isEmpty());
    }

    @Test public void frameFlushDoesNotConsumeAnIndependentInProgressTransaction() {
        NativePaletteBatch<Object> batch = new NativePaletteBatch<>();
        Object ongoing = new Object(), ready = new Object();
        batch.begin(ongoing);
        batch.mark(ongoing);
        batch.mark(ready);
        List<Object> frame = batch.drain();
        assertEquals(1, frame.size());
        assertSame(ready, frame.get(0));
        assertTrue(batch.end(ongoing));
    }

    @Test public void failedRecipeFinallyRestoresOnceAndUnregisteredTargetsAreDropped() {
        NativePaletteBatch<Object> batch = new NativePaletteBatch<>();
        Object view = new Object();
        batch.begin(view);
        try {
            batch.mark(view);
            throw new IllegalStateException("later add failed");
        } catch (IllegalStateException expected) {
            assertEquals("later add failed", expected.getMessage());
        } finally { assertTrue(batch.end(view)); }
        assertTrue(batch.drain().isEmpty());
        batch.begin(view);
        batch.mark(view);
        batch.remove(view);
        assertFalse(batch.end(view));
        assertTrue(batch.drain().isEmpty());
    }
}

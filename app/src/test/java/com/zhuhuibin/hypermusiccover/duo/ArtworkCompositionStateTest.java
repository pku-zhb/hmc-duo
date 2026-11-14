package com.zhuhuibin.hypermusiccover.duo;

import org.junit.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.Assert.*;

public class ArtworkCompositionStateTest {
    private final Object window = new Object();
    private final ArtworkCompositionState state = new ArtworkCompositionState();

    private ArtworkCompositionKey key(Object window, long revision, int width, int mode,
                                      boolean card, long source) {
        return new ArtworkCompositionKey(window, revision, width, 1600, mode, card, source, 24, .5f);
    }

    private ArtworkCompositionKey key() { return key(window, 3, 1000, 1, true, 8); }

    @Test public void anIdenticalPendingOrCommittedKeyDoesNotScheduleMoreWork() {
        ArtworkCompositionKey a = key();
        assertTrue(state.request(a));
        assertFalse(state.request(key()));
        assertSame(a, state.pending());
        assertTrue(state.commit(a));
        assertNull(state.pending());
        assertFalse(state.request(key()));
        assertSame(a, state.committed());
    }

    @Test public void pausedTrackGeometryChangesStillRequestARebuild() {
        ArtworkCompositionKey a = key();
        state.request(a); state.commit(a);
        ArtworkCompositionKey resized = key(window, 4, 1600, 1, true, 8);
        assertTrue(state.request(resized));
        assertSame(a, state.committed());
        assertFalse(state.accepts(a));
        assertTrue(state.accepts(resized));
    }

    @Test public void switchingModeOrCardCompositionInvalidatesUnfinishedWork() {
        ArtworkCompositionKey a = key();
        ArtworkCompositionKey mode = key(window, 3, 1000, 2, true, 8);
        ArtworkCompositionKey card = key(window, 3, 1000, 2, false, 8);
        assertTrue(state.request(a));
        assertTrue(state.request(mode));
        assertFalse(state.commit(a));
        assertTrue(state.request(card));
        assertFalse(state.commit(mode));
        assertTrue(state.commit(card));
    }

    @Test public void reverseCompletionAcrossWindowAToBToADoesNotRestoreAnOldRevision() {
        Object bWindow = new Object();
        ArtworkCompositionKey firstA = key(window, 3, 1000, 1, true, 8);
        ArtworkCompositionKey b = key(bWindow, 4, 1600, 1, true, 8);
        ArtworkCompositionKey finalA = key(window, 5, 1000, 1, true, 8);
        state.request(firstA); state.request(b); state.request(finalA);
        assertTrue(state.commit(finalA));
        assertFalse(state.commit(b));
        assertFalse(state.commit(firstA));
        assertSame(finalA, state.committed());
    }

    @Test public void anOldSourceCannotCommitAfterTheTrackChanges() {
        ArtworkCompositionKey previous = key();
        ArtworkCompositionKey next = key(window, 3, 1000, 1, true, 9);
        state.request(previous); state.request(next);
        assertFalse(state.accepts(previous));
        assertFalse(state.commit(previous));
        assertTrue(state.commit(next));
    }

    @Test public void returningToTheCommittedImageCancelsAnUnneededDifferentRequest() {
        ArtworkCompositionKey a = key();
        ArtworkCompositionKey b = key(window, 4, 1600, 1, true, 8);
        state.request(a); state.commit(a); state.request(b);
        assertFalse(state.request(key()));
        assertNull(state.pending());
        assertFalse(state.commit(b));
        assertSame(a, state.committed());
    }

    @Test public void aCancelledLatestRequestCanBeResentWithoutAPlaybackEvent() {
        ArtworkCompositionKey a = key();
        state.request(a);
        assertTrue(state.cancel(a));
        assertSame(a, state.desired());
        assertNull(state.pending());
        assertTrue(state.request(key()));
        assertTrue(state.commit(a));
    }

    @Test public void aLateCancellationDoesNotClearTheNewerPendingComposition() {
        ArtworkCompositionKey a = key(), b = key(window, 4, 1600, 1, true, 8);
        state.request(a); state.request(b);
        assertFalse(state.cancel(a));
        assertSame(b, state.pending());
        assertTrue(state.commit(b));
    }

    @Test public void invalidGeometryBlocksOldCompletionButKeepsTheOutgoingImage() {
        ArtworkCompositionKey a = key();
        state.request(a); state.commit(a);
        ArtworkCompositionKey invalid = key(window, 4, 0, 1, true, 8);
        assertFalse(state.request(invalid));
        assertNull(state.pending());
        assertFalse(state.accepts(a));
        assertFalse(state.commit(invalid));
        assertSame(a, state.committed());
    }

    @Test public void clearingReleasesEveryKeyAndRejectsOldWorkers() {
        ArtworkCompositionKey a = key();
        state.request(a); state.commit(a);
        state.clear();
        assertNull(state.desired());
        assertNull(state.pending());
        assertNull(state.committed());
        assertFalse(state.accepts(a));
        assertFalse(state.commit(a));
    }

    @Test public void workerCompletionUsesTheLatestMainThreadRequest() throws Exception {
        ArtworkCompositionKey a = key(), b = key(window, 4, 1600, 1, true, 8);
        state.request(a);
        CountDownLatch started = new CountDownLatch(1), finish = new CountDownLatch(1);
        AtomicBoolean accepted = new AtomicBoolean(true);
        Thread worker = new Thread(() -> {
            started.countDown();
            try {
                if (finish.await(2, TimeUnit.SECONDS)) accepted.set(state.commit(a));
            } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        });
        worker.start();
        assertTrue(started.await(2, TimeUnit.SECONDS));
        state.request(b);
        finish.countDown();
        worker.join(2000);
        assertFalse(worker.isAlive());
        assertFalse(accepted.get());
        assertTrue(state.commit(b));
    }
}

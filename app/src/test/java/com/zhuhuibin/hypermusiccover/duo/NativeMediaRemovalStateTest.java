package com.zhuhuibin.hypermusiccover.duo;

import org.junit.Test;
import static org.junit.Assert.*;

public class NativeMediaRemovalStateTest {
    private final Object notification = new Object(), card = new Object(), owner = new Object(), data = new Object();
    private final NativeMediaRemovalState removal = new NativeMediaRemovalState();

    private void bind() { removal.commit(notification, card, owner, data); }

    @Test public void nullTopRetiresTheRetainedHiddenHolderWithoutSessionDestruction() {
        bind();
        MusicSessionLifecycle<String> sessions = new MusicSessionLifecycle<>(4);
        assertTrue(removal.removeTop(notification, null, true, card, owner, data));
        assertTrue(sessions.alive("paused"));
        assertFalse(removal.accepts(notification, card, owner, data, null));
        assertFalse(removal.removeTop(notification, null, true, card, owner, data));
        assertFalse(removal.removeDetached(false, false));
    }

    @Test public void nullTopAlsoRetiresAHolderWhoseNativeDataWasAlreadyCleared() {
        bind();
        assertTrue(removal.removeTop(notification, null, false, card, null, null));
        assertFalse(removal.accepts(notification, card, owner, data, null));
    }

    @Test public void detachWithoutATopEventPublishesOnlyOneRemoval() {
        bind();
        assertFalse(removal.removeDetached(true, false));
        assertTrue(removal.removeDetached(false, false));
        assertFalse(removal.removeDetached(false, false));
    }

    @Test public void unrelatedOwnerCannotRemoveTheCommittedSelection() {
        bind();
        assertFalse(removal.removeTop(new Object(), null, false, null, null, null));
        assertTrue(removal.accepts(notification, card, owner, data, null));
    }

    @Test public void coherentReplacementWinsOverAnOldNullTopOrDetach() {
        bind();
        Object nextCard = new Object(), nextOwner = new Object(), nextData = new Object();
        assertFalse(removal.removeTop(notification, null, true, nextCard, nextOwner, nextData));
        assertFalse(removal.removeDetached(false, true));
        removal.commit(notification, nextCard, nextOwner, nextData);
        assertFalse(removal.removeTop(new Object(), null, false, null, null, null));
    }

    @Test public void reinflationDuringRemovalGraceCanCommitAndCancelPendingRemoval() {
        bind();
        NativeMediaSelectionState<String> selection = new NativeMediaSelectionState<>();
        selection.publish(card, owner, data, "paused", "player|song|artist");
        assertTrue(removal.removeDetached(false, false));
        selection.publish(null, owner, null, null, "");
        long removalGeneration = selection.snapshot().generation;
        Object replacementCard = new Object();
        assertTrue(removal.accepts(notification, replacementCard, owner, data, null));
        removal.commit(notification, replacementCard, owner, data);
        assertTrue(selection.publish(replacementCard, owner, data, "paused", "player|song|artist"));
        assertTrue(selection.snapshot().generation > removalGeneration);
        assertEquals("paused", selection.token());
        assertFalse(removal.removeDetached(true, false));
    }

    @Test public void temporaryBindingLossWithNonNullTopDoesNotRemoveTheLiveHolder() {
        bind();
        assertFalse(removal.removeTop(notification, data, false, card, null, null));
        assertFalse(removal.removeDetached(true, false));
    }

    @Test public void aNewDataCommitOnTheSameHolderWinsOverOldPipelineRemoval() {
        bind();
        Object nextData = new Object();
        assertFalse(removal.removeTop(notification, null, true, card, owner, nextData));
    }

    @Test public void aNewNonNullPipelineCommitCanReuseTheSameHolderAndData() {
        bind();
        assertTrue(removal.removeTop(notification, null, true, card, owner, data));
        assertTrue(removal.accepts(notification, card, owner, data, data));
        removal.commit(notification, card, owner, data);
        assertTrue(removal.accepts(notification, card, owner, data, null));
    }
}

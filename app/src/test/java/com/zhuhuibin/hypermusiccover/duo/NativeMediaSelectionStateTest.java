package com.zhuhuibin.hypermusiccover.duo;

import org.junit.Test;
import static org.junit.Assert.*;

public class NativeMediaSelectionStateTest {
    private final Object card = new Object(), owner = new Object();
    private final NativeMediaSelectionState<String> selection = new NativeMediaSelectionState<>();
    private final MusicSessionLifecycle<String> sessions = new MusicSessionLifecycle<>(4);
    private static final String BILI = "tv.danmaku.bili|previous video|uploader";
    private static final String NCM = "com.netease.cloudmusic|天下|张杰";

    @Test public void nativeMusicBindingReplacesThePausedVideoWhileBothSessionsRemainLive() {
        selection.publish(card, owner, new Object(), "bili", BILI);
        NativeMediaSelectionState.Snapshot<String> previous = selection.snapshot();
        selection.publish(card, owner, new Object(), "ncm", NCM);
        NativeMediaSelectionState.Snapshot<String> visible = selection.snapshot();
        assertTrue(sessions.alive("bili"));
        assertTrue(sessions.alive("ncm"));
        assertEquals("ncm", visible.token);
        assertEquals(NCM, visible.key);
        assertFalse(selection.allowsSessionFallback());
        assertEquals("bili", previous.token);
        assertEquals(BILI, previous.key);
        assertTrue(visible.generation > previous.generation);
    }

    @Test public void nativeVideoBindingCanReplaceTheMusicWithoutAnySessionListChange() {
        selection.publish(card, owner, new Object(), "ncm", NCM);
        selection.publish(card, owner, new Object(), "bili", BILI);
        assertTrue(sessions.alive("ncm"));
        assertTrue(sessions.alive("bili"));
        assertEquals("bili", selection.snapshot().token);
        assertEquals(BILI, selection.snapshot().key);
    }

    @Test public void switchingBackToVideoKeepsTheFirstVideoCallbacksRetired() {
        selection.publish(card, owner, new Object(), "bili", BILI);
        long firstVideo = sessions.nextWatchGeneration();
        selection.publish(card, owner, new Object(), "ncm", NCM);
        long music = sessions.nextWatchGeneration();
        selection.publish(card, owner, new Object(), "bili", BILI);
        long currentVideo = sessions.nextWatchGeneration();
        assertTrue(sessions.alive("bili"));
        assertTrue(sessions.alive("ncm"));
        assertFalse(sessions.accepts("bili", selection.token(), firstVideo));
        assertFalse(sessions.accepts("ncm", selection.token(), music));
        assertTrue(sessions.accepts("bili", selection.token(), currentVideo));
        assertEquals(BILI, selection.key());
    }

    @Test public void aTrackChangeOnTheSameNativeDataObjectStillCommitsItsNewIdentity() {
        Object data = new Object();
        selection.publish(card, owner, data, "ncm", NCM);
        long generation = selection.snapshot().generation;
        assertTrue(selection.publish(card, owner, data, "ncm", "com.netease.cloudmusic|next|张杰"));
        assertEquals("com.netease.cloudmusic|next|张杰", selection.key());
        assertEquals(generation + 1, selection.snapshot().generation);
    }

    @Test public void repeatedLayoutEventsDoNotRebindTheSameNativeSelection() {
        Object data = new Object();
        selection.publish(card, owner, data, "ncm", NCM);
        NativeMediaSelectionState.Snapshot<String> original = selection.snapshot();
        for (int i = 0; i < 100; ++i) assertFalse(selection.publish(card, owner, data, "ncm", NCM));
        assertSame(original, selection.snapshot());
    }

    @Test public void aReplacementHolderAndUpdatedArtworkDataPublishEvenOnTheSameTrack() {
        selection.publish(card, owner, new Object(), "ncm", NCM);
        NativeMediaSelectionState.Snapshot<String> previous = selection.snapshot();
        Object nextCard = new Object(), nextOwner = new Object(), nextData = new Object();
        assertTrue(selection.publish(nextCard, nextOwner, nextData, "ncm", NCM));
        assertSame(nextCard, selection.snapshot().card);
        assertSame(nextOwner, selection.snapshot().owner);
        assertSame(nextData, selection.snapshot().data);
        assertTrue(selection.snapshot().generation > previous.generation);
    }

    @Test public void aNativeCardWithoutALiveTokenNeverBorrowsTheOtherPausedSession() {
        assertTrue(selection.allowsSessionFallback());
        selection.publish(card, owner, new Object(), null, "");
        assertTrue(sessions.alive("bili"));
        assertFalse(selection.allowsSessionFallback());
        assertNull(selection.token());
        selection.clear();
        assertFalse(selection.allowsSessionFallback());
        assertNull(selection.token());
        assertEquals("", selection.key());
    }
}

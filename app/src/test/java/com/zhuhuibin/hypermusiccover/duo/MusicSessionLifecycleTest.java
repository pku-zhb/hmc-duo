package com.zhuhuibin.hypermusiccover.duo;

import org.junit.Test;
import static org.junit.Assert.*;

public class MusicSessionLifecycleTest {
    @Test public void aDestroyedTokenCannotBeRevivedByItsRetainedCard() {
        MusicSessionLifecycle<String> state = new MusicSessionLifecycle<>(4);
        assertTrue(state.alive("paused-old"));
        state.destroy("paused-old");
        assertFalse(state.alive("paused-old"));
        assertFalse(state.accepts("paused-old", "paused-old"));
        assertTrue(state.alive("replacement"));
    }
    @Test public void aQueuedOldSessionCallbackCannotChangeTheNewSession() {
        MusicSessionLifecycle<String> state = new MusicSessionLifecycle<>(4);
        assertFalse(state.accepts("old", "new"));
        assertTrue(state.accepts("new", "new"));
        state.destroy("old");
        assertTrue(state.accepts("new", "new"));
    }
    @Test public void aLivePausedSessionDoesNotExpireWithBackgroundAge() {
        MusicSessionLifecycle<String> state = new MusicSessionLifecycle<>(4);
        for (int i = 0; i < 1000; i++) assertTrue(state.accepts("paused", "paused"));
        assertFalse(state.alive(null));
    }
    @Test public void tombstonesAreBoundedAndRepeatedDestructionKeepsTheNewest() {
        MusicSessionLifecycle<String> state = new MusicSessionLifecycle<>(2);
        state.destroy("one"); state.destroy("two"); state.destroy("one"); state.destroy("three");
        assertFalse(state.alive("one"));
        assertTrue(state.alive("two"));
        assertFalse(state.alive("three"));
    }
}

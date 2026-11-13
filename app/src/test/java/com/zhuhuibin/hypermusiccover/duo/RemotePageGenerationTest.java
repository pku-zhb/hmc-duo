package com.zhuhuibin.hypermusiccover.duo;

import org.junit.Test;
import static org.junit.Assert.*;

public class RemotePageGenerationTest {
    @Test public void aLateSurfaceReplyCannotAttachToTheNextPreparedPage() {
        RemotePageGeneration session = new RemotePageGeneration();
        long first = session.begin();
        assertTrue(session.accepts(first));
        session.retire();
        assertFalse(session.accepts(first));
        long second = session.begin();
        assertFalse(session.accepts(first));
        assertTrue(session.accepts(second));
        session.retire();
        assertFalse(session.accepts(second));
    }

    @Test public void navigationUsesTheSamePreferenceWithoutRequiringMusicArtwork() {
        assertTrue(ArtworkClockDestination.sceneRequested(false, true, true, true));
        assertFalse(ArtworkClockDestination.sceneRequested(false, true, true, false));
        assertTrue(ArtworkClockDestination.sceneRequested(true, false, false, false));
        assertFalse(ArtworkClockDestination.sceneRequested(true, true, false, false));
    }
}

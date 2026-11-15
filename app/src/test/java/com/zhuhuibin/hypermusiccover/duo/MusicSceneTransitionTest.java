package com.zhuhuibin.hypermusiccover.duo;

import org.junit.Test;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import static org.junit.Assert.*;

public class MusicSceneTransitionTest {
    private static final class Cards implements MusicSceneTransition.Target {
        final List<String> events = new ArrayList<>();
        boolean notifications = true, liveCard = true, nativePlayer = true, artwork;
        @Override public void collapseNonMusic() {
            notifications = false;
            liveCard = false;
            events.add("collapse");
        }
        @Override public void commit(boolean scene) {
            if (scene) {
                assertFalse(notifications);
                assertFalse(liveCard);
            }
            artwork = scene;
            events.add(scene ? "music" : "cards");
        }
        @Override public void pushArtwork(boolean scene) { assertEquals(scene, artwork); }
    }

    @Test public void everyArtworkAndLyricPageCollapsesOtherCardsBeforeItsBackgroundCanCommit() {
        for (int page : new int[]{RailMusicPagePolicy.ARTWORK, RailMusicPagePolicy.LYRICS,
                RailMusicPagePolicy.ARTWORK_AND_LYRICS}) {
            MusicSceneTransition scenes = new MusicSceneTransition();
            Cards cards = new Cards();
            scenes.apply(scenes.request(RailMusicPagePolicy.ownsScene(page)), cards);
            assertEquals(Arrays.asList("collapse", "music"), cards.events);
            assertTrue(cards.artwork);
            assertTrue(cards.nativePlayer);
        }
    }

    @Test public void openingACardCancelsAnEarlierQueuedSceneEvenBeforeCoverModeBecameTrue() {
        MusicSceneTransition scenes = new MusicSceneTransition();
        Cards cards = new Cards();
        long music = scenes.request(true);
        long notification = scenes.request(false);
        scenes.apply(music, cards);
        assertTrue(cards.events.isEmpty());
        scenes.apply(notification, cards);
        assertEquals(Arrays.asList("cards"), cards.events);
        assertTrue(cards.notifications);
        assertTrue(cards.liveCard);
        assertFalse(cards.artwork);
        assertFalse(scenes.wanted());
    }

    @Test public void aNewMusicSelectionSupersedesAnOlderQueuedCardExit() {
        MusicSceneTransition scenes = new MusicSceneTransition();
        Cards cards = new Cards();
        long first = scenes.request(true);
        long exit = scenes.request(false);
        long latest = scenes.request(true);
        scenes.apply(first, cards);
        scenes.apply(exit, cards);
        assertTrue(cards.events.isEmpty());
        scenes.apply(latest, cards);
        assertEquals(Arrays.asList("collapse", "music"), cards.events);
    }

    @Test public void aNewCardActionDuringNativeCollapsePreventsTheOldMusicCommit() {
        MusicSceneTransition scenes = new MusicSceneTransition();
        Cards cards = new Cards();
        long[] cardRequest = {-1L};
        scenes.apply(scenes.request(true), new MusicSceneTransition.Target() {
            @Override public void collapseNonMusic() {
                cards.collapseNonMusic();
                cardRequest[0] = scenes.request(false);
            }
            @Override public void commit(boolean scene) { fail("superseded scene committed"); }
            @Override public void pushArtwork(boolean scene) { fail("superseded artwork dispatched"); }
        });
        assertEquals(Arrays.asList("collapse"), cards.events);
        scenes.apply(cardRequest[0], cards);
        assertEquals(Arrays.asList("collapse", "cards"), cards.events);
        assertFalse(cards.artwork);
    }

    @Test public void closingArtworkReleasesItsBackgroundWithoutRecollapsingOtherCards() {
        MusicSceneTransition scenes = new MusicSceneTransition();
        Cards cards = new Cards();
        scenes.apply(scenes.request(true), cards);
        cards.events.clear();
        cards.notifications = true;
        scenes.apply(scenes.request(false), cards);
        assertEquals(Arrays.asList("cards"), cards.events);
        assertFalse(cards.artwork);
        assertTrue(cards.notifications);
    }

    private static final class ArtworkScene implements MusicSceneTransition.Target {
        static final String CARD = "com.netease.cloudmusic|天下|张杰";
        static final String SESSION = CARD + "|明天过后";
        final List<Runnable> worker = new ArrayList<>();
        final List<Boolean> dispatched = new ArrayList<>();
        String track = "";
        boolean mode, published, immediateWorker;
        int artworkGeneration;
        Runnable duringCommit;
        @Override public void collapseNonMusic() { }
        @Override public void commit(boolean scene) {
            track = scene ? ArtworkTrackIdentity.entryKey(CARD, SESSION) : "";
            mode = scene;
            Runnable action = duringCommit;
            duringCommit = null;
            if (action != null) action.run();
        }
        @Override public void pushArtwork(boolean scene) {
            // The worker can start as soon as dispatch happens, before the main call returns.
            assertEquals(scene, mode);
            if (scene) assertEquals(SESSION, track);
            dispatched.add(scene);
            int generation = ++artworkGeneration;
            String requestedTrack = track;
            Runnable job = () -> {
                boolean confirmedTrack = !scene
                        || !requestedTrack.isEmpty() && requestedTrack.equals(track);
                if (!ArtworkPredictionPolicy.canPublish(generation, artworkGeneration,
                        confirmedTrack, false, false)) return;
                if (scene) {
                    assertTrue(mode);
                    assertEquals(requestedTrack, track);
                    assertFalse(track.isEmpty());
                }
                published = scene;
            };
            if (immediateWorker) job.run();
            else worker.add(job);
        }
    }

    @Test public void firstExplicitArtworkAfterOffPublishesIdentityAndModeBeforeAnImmediateWorker() {
        MusicSceneTransition scenes = new MusicSceneTransition();
        ArtworkScene target = new ArtworkScene();
        target.immediateWorker = true;
        scenes.apply(scenes.request(false), target);
        assertEquals("", target.track);
        scenes.apply(scenes.request(true), target);
        assertEquals(ArtworkScene.SESSION, target.track);
        assertTrue(target.mode);
        assertTrue(target.published);
        assertEquals(Arrays.asList(false, true), target.dispatched);
    }

    @Test public void aNewExitDuringModeCommitPreventsTheOlderArtworkDispatch() {
        MusicSceneTransition scenes = new MusicSceneTransition();
        ArtworkScene target = new ArtworkScene();
        long[] exit = {-1L};
        target.duringCommit = () -> exit[0] = scenes.request(false);
        scenes.apply(scenes.request(true), target);
        assertTrue(target.dispatched.isEmpty());
        scenes.apply(exit[0], target);
        assertEquals(Arrays.asList(false), target.dispatched);
        assertFalse(target.mode);
    }

    @Test public void aReentrantEntryCancelsTheOuterExitBeforeItCanEnqueueArtworkRemoval() {
        MusicSceneTransition scenes = new MusicSceneTransition();
        ArtworkScene target = new ArtworkScene();
        target.duringCommit = () -> scenes.apply(scenes.request(true), target);
        scenes.apply(scenes.request(false), target);
        assertEquals(Arrays.asList(true), target.dispatched);
        assertTrue(target.mode);
        assertEquals(ArtworkScene.SESSION, target.track);
    }

    @Test public void queuedOffThenOnKeepsTheLatestArtworkAndItsIdentity() {
        MusicSceneTransition scenes = new MusicSceneTransition();
        ArtworkScene target = new ArtworkScene();
        scenes.apply(scenes.request(false), target);
        scenes.apply(scenes.request(true), target);
        for (Runnable work : target.worker) work.run();
        assertTrue(target.published);
        assertTrue(target.mode);
        assertEquals(ArtworkScene.SESSION, target.track);
    }

    @Test public void queuedOnThenOffCannotPublishArtworkIntoTheClosedScene() {
        MusicSceneTransition scenes = new MusicSceneTransition();
        ArtworkScene target = new ArtworkScene();
        scenes.apply(scenes.request(true), target);
        scenes.apply(scenes.request(false), target);
        for (Runnable work : target.worker) work.run();
        assertFalse(target.published);
        assertFalse(target.mode);
        assertEquals("", target.track);
    }
}

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
}

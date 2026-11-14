package com.zhuhuibin.hypermusiccover.duo;

import org.junit.Test;

import static org.junit.Assert.*;

public class NativeMediaCardBindingStateTest {
    private final Object window = new Object();
    private final Object card = new Object(), art = new Object(), title = new Object();
    private final Object artist = new Object(), observer = new Object();
    private final NativeMediaCardBindingState<Object> state = new NativeMediaCardBindingState<>();

    private void bind() { state.bind(window, 7, card, art, title, artist, observer); }

    @Test public void anEmptyBindingDoesNotMatchOrAdvanceOnClear() {
        assertNull(state.snapshot());
        assertFalse(state.matches(window, 7, card, art, title, artist, observer));
        assertFalse(state.clearIfCard(card));
        state.clear();
        assertEquals(0, state.generation());
    }

    @Test public void unchangedCardAndChildrenDoNotPublishAnotherGeneration() {
        bind();
        NativeMediaCardBindingState.Snapshot<Object> original = state.snapshot();
        assertFalse(state.bind(window, 7, card, art, title, artist, observer));
        assertSame(original, state.snapshot());
        assertEquals(1, state.generation());
    }

    @Test public void foldReplacementDoesNotRequireAMetadataChange() {
        bind();
        Object nextCard = new Object(), nextArt = new Object(), nextTitle = new Object();
        Object nextArtist = new Object(), nextObserver = new Object();
        assertTrue(NativeMediaCardBindingState.shouldReplace(false, true, true, true, true));
        assertTrue(state.bind(window, 8, nextCard, nextArt, nextTitle, nextArtist, nextObserver));
        assertTrue(state.matches(window, 8, nextCard, nextArt, nextTitle, nextArtist, nextObserver));
        assertSame(nextCard, state.snapshot().card);
        assertEquals(2, state.generation());
    }

    @Test public void reinflatedArtworkInTheSameCardIsANewBinding() {
        bind();
        Object nextArt = new Object();
        assertFalse(state.matches(window, 7, card, nextArt, title, artist, observer));
        assertTrue(state.bind(window, 7, card, nextArt, title, artist, observer));
        assertSame(nextArt, state.snapshot().art);
        assertSame(card, state.snapshot().card);
        assertEquals(2, state.generation());
    }

    @Test public void reinflatedTextChildrenAreDetectedWithoutReplacingTheCard() {
        bind();
        Object nextTitle = new Object(), nextArtist = new Object();
        assertFalse(state.matches(window, 7, card, art, nextTitle, artist, observer));
        assertFalse(state.matches(window, 7, card, art, title, nextArtist, observer));
        state.bind(window, 7, card, art, nextTitle, nextArtist, observer);
        assertSame(nextTitle, state.snapshot().title);
        assertSame(nextArtist, state.snapshot().artist);
    }

    @Test public void replacingTheObserverOnTheSameCardRequiresRebinding() {
        bind();
        Object nextObserver = new Object();
        assertFalse(state.matches(window, 7, card, art, title, artist, nextObserver));
        assertTrue(state.bind(window, 7, card, art, title, artist, nextObserver));
        assertSame(nextObserver, state.snapshot().observer);
    }

    @Test public void changingTheWindowOrItsLayoutRevisionInvalidatesTheBinding() {
        bind();
        assertFalse(state.matches(new Object(), 7, card, art, title, artist, observer));
        assertFalse(state.matches(window, 8, card, art, title, artist, observer));
        assertTrue(state.bind(window, 8, card, art, title, artist, observer));
        assertEquals(8, state.snapshot().revision);
    }

    @Test public void lateDetachFromTheOldCardDoesNotClearItsReplacement() {
        bind();
        Object nextCard = new Object();
        state.bind(window, 8, nextCard, new Object(), new Object(), new Object(), new Object());
        NativeMediaCardBindingState.Snapshot<Object> replacement = state.snapshot();
        assertFalse(state.clearIfCard(card));
        assertSame(replacement, state.snapshot());
        assertEquals(2, state.generation());
        assertTrue(state.clearIfCard(nextCard));
        assertNull(state.snapshot());
        assertEquals(3, state.generation());
    }

    @Test public void anOldSnapshotRemainsCoherentAfterReplacement() {
        bind();
        NativeMediaCardBindingState.Snapshot<Object> original = state.snapshot();
        state.bind(window, 8, new Object(), new Object(), new Object(), new Object(), new Object());
        assertSame(card, original.card);
        assertSame(art, original.art);
        assertSame(title, original.title);
        assertSame(artist, original.artist);
        assertSame(observer, original.observer);
        assertEquals(7, original.revision);
        assertEquals(1, original.generation);
    }

    @Test public void equalObjectsDoNotStandInForTheSameNativeView() {
        Object a = new String("same"), b = new String("same");
        assertEquals(a, b);
        state.bind(window, 7, a, art, title, artist, observer);
        assertFalse(state.matches(window, 7, b, art, title, artist, observer));
        assertFalse(state.clearIfCard(b));
        assertSame(a, state.snapshot().card);
    }

    @Test public void missingOptionalChildrenCanLaterBeRebound() {
        state.bind(window, 7, card, null, title, null, observer);
        assertTrue(state.matches(window, 7, card, null, title, null, observer));
        assertTrue(state.bind(window, 7, card, art, title, artist, observer));
        assertSame(art, state.snapshot().art);
        assertSame(artist, state.snapshot().artist);
    }

    @Test public void aForeignWindowOrIslandCandidateCannotReplaceEvenADetachedCard() {
        assertFalse(NativeMediaCardBindingState.shouldReplace(false, false, true, true, true));
        assertFalse(NativeMediaCardBindingState.shouldReplace(false, true, true, true, false));
        assertFalse(NativeMediaCardBindingState.shouldReplace(true, false, true, true, true));
        assertFalse(NativeMediaCardBindingState.shouldReplace(true, true, true, true, false));
    }

    @Test public void aHiddenAttachedCurrentCardSurvivesAnIneligibleReplacement() {
        assertFalse(NativeMediaCardBindingState.shouldReplace(true, true, false, true, true));
        assertFalse(NativeMediaCardBindingState.shouldReplace(true, true, true, false, true));
        assertTrue(NativeMediaCardBindingState.shouldReplace(true, true, true, true, true));
    }

    @Test public void aDetachedCurrentCardCanBindTheAttachedNativeCardBeforeItBecomesVisible() {
        assertTrue(NativeMediaCardBindingState.shouldReplace(false, true, true, false, true));
        assertFalse(NativeMediaCardBindingState.shouldReplace(false, true, false, false, true));
    }

    @Test public void explicitClearReleasesOnceAndAFutureBindStartsAnotherGeneration() {
        bind();
        state.clear();
        state.clear();
        assertNull(state.snapshot());
        assertEquals(2, state.generation());
        bind();
        assertEquals(3, state.generation());
    }
}

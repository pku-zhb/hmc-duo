package com.zhuhuibin.hypermusiccover.duo;

import org.junit.Test;
import static org.junit.Assert.*;

public class ArtworkBackdropStateTest {
    @Test public void anOpaqueRendererDoesNotAcknowledgeANewCompositionWithZeroContribution() {
        assertTrue(Float.isNaN(ArtworkBackdropState.compositionEvidence(0f, 1f)));
        assertTrue(Float.isNaN(ArtworkBackdropState.compositionEvidence(Float.NaN, 1f)));
        assertEquals(.4f, ArtworkBackdropState.compositionEvidence(.4f, 1f), 0f);
        assertEquals(.2f, ArtworkBackdropState.compositionEvidence(.4f, .2f), 0f);
    }

    @Test public void actualRendererInvisibilityWithdrawsEvenWhenTheCompositionProofIsUnknown() {
        assertEquals(0f, ArtworkBackdropState.compositionEvidence(1f, 0f), 0f);
        assertEquals(0f, ArtworkBackdropState.compositionEvidence(Float.NaN, 0f), 0f);
        assertEquals(0f, ArtworkBackdropState.compositionEvidence(1f, Float.NaN), 0f);
    }

    @Test public void anOutgoingGenerationMayWithdrawButCannotAcknowledgeTheNewComposition() {
        assertTrue(ArtworkBackdropState.acceptsEvidence(12, 11, 0f));
        assertTrue(ArtworkBackdropState.acceptsEvidence(12, -1, 0f));
        assertFalse(ArtworkBackdropState.acceptsEvidence(12, 11, 1f));
        assertFalse(ArtworkBackdropState.acceptsEvidence(12, -1, 1f));
        assertFalse(ArtworkBackdropState.acceptsEvidence(-1, -1, 1f));
        assertFalse(ArtworkBackdropState.acceptsEvidence(12, 12, Float.NaN));
        assertTrue(ArtworkBackdropState.acceptsEvidence(12, 12, 1f));
    }

    @Test public void sameSourceGeometryRebuildKeepsTheOutgoingFramesHonestOriginalKey() {
        ArtworkBackdropState.Handoff handoff = new ArtworkBackdropState.Handoff();
        Object window = new Object();
        handoff.select(1L, ArtworkBackdropState.MESH, window, 41L, 7L, 11);
        handoff.presented(1L, ArtworkBackdropState.MESH, window, 41L, 7L, 11, 1f);
        handoff.select(2L, ArtworkBackdropState.MESH, window, 41L, 8L, 12);
        assertTrue(handoff.local());
        assertEquals(7L, handoff.ownerGeometry());
        assertEquals(11, handoff.generation());
        assertEquals(1L, handoff.ownerEpoch());
        assertEquals(41L, handoff.ownerSource());
        assertTrue(handoff.matches(2L, window, 41L, 8L, 12));
        assertTrue(handoff.needsPresentation(ArtworkBackdropState.MESH, 1f));
        // Drawing the retained old composition cannot claim that it was made for revision 8.
        assertFalse(handoff.presented(2L, ArtworkBackdropState.MESH, window, 41L, 7L, 11, 1f));
        assertEquals(7L, handoff.ownerGeometry());
        assertFalse(handoff.presented(2L, ArtworkBackdropState.MESH, window, 41L, 8L, 12, .5f));
        assertEquals(7L, handoff.ownerGeometry());
        assertTrue(handoff.presented(2L, ArtworkBackdropState.MESH, window, 41L, 8L, 12, 1f));
        assertEquals(8L, handoff.ownerGeometry());
        assertEquals(12, handoff.generation());
        assertEquals(2L, handoff.ownerEpoch());
    }

    @Test public void modeOrBlurRecompositionNeedsANewDrawEvenInTheSameRendererAndGeometry() {
        ArtworkBackdropState.Handoff handoff = new ArtworkBackdropState.Handoff();
        Object window = new Object();
        handoff.select(1L, ArtworkBackdropState.MESH, window, 41L, 7L, 11);
        handoff.presented(1L, ArtworkBackdropState.MESH, window, 41L, 7L, 11, 1f);
        handoff.select(2L, ArtworkBackdropState.MESH, window, 41L, 7L, 12);
        assertTrue(handoff.local());
        assertEquals(11, handoff.generation());
        assertTrue(handoff.needsPresentation(ArtworkBackdropState.MESH, 1f));
        assertFalse(handoff.presented(1L, ArtworkBackdropState.MESH, window, 41L, 7L, 11, 1f));
        handoff.presented(2L, ArtworkBackdropState.MESH, window, 41L, 7L, 12, 1f);
        assertEquals(12, handoff.generation());
        assertFalse(handoff.needsPresentation(ArtworkBackdropState.MESH, 1f));
    }

    @Test public void aNewSourceRetainsTheActualPresenterWithoutBorrowingItsCompositionProof() {
        ArtworkBackdropState.Handoff handoff = new ArtworkBackdropState.Handoff();
        Object window = new Object();
        handoff.select(1L, ArtworkBackdropState.MESH, window, 41L, 7L, 11);
        handoff.presented(1L, ArtworkBackdropState.MESH, window, 41L, 7L, 11, 1f);
        handoff.select(2L, ArtworkBackdropState.MESH, window, 42L, 7L, 11);
        assertTrue(handoff.local());
        assertEquals(11, handoff.generation());
        assertEquals(41L, handoff.ownerSource());
        assertEquals(1L, handoff.ownerEpoch());
        assertFalse(handoff.coveredBy(ArtworkBackdropState.MESH));
        assertTrue(handoff.needsPresentation(ArtworkBackdropState.MESH, 1f));
        assertEquals(1L, handoff.sourceRetentions());
        assertFalse(handoff.presented(2L, ArtworkBackdropState.MESH, window, 41L, 7L, 11, 1f));
        assertFalse(handoff.invisible(1L, ArtworkBackdropState.MESH, window, 41L));
        handoff.presented(2L, ArtworkBackdropState.MESH, window, 42L, 7L, 11, 1f);
        assertEquals(42L, handoff.ownerSource());
    }

    @Test public void sourcePreparationAndCrossfadeNeverRestoreTheNativeSampler() {
        ArtworkBackdropState.Handoff handoff = new ArtworkBackdropState.Handoff();
        NativeSamplingLease sampler = new NativeSamplingLease();
        Object window = new Object();
        sampler.bind(2, true, 0);
        handoff.select(1L, ArtworkBackdropState.MESH, window, 41L, 7L, 11);
        handoff.presented(1L, ArtworkBackdropState.MESH, window, 41L, 7L, 11, 1f);
        sampler.committed(handoff.local());
        // publish() changes source identity before its async composition has replaced current.
        handoff.select(2L, ArtworkBackdropState.MESH, window, 42L, 7L, 11);
        assertFalse(sampler.needsApply(handoff.local()));
        assertFalse(handoff.presented(1L, ArtworkBackdropState.MESH, window, 41L, 7L, 11, 1f));
        // adoptPending() starts a real previous/current crossfade in the same presenter.
        handoff.select(3L, ArtworkBackdropState.MESH, window, 42L, 7L, 12);
        for (float coverage : new float[]{.01f, .25f, .75f, .99f}) {
            assertFalse(handoff.presented(3L, ArtworkBackdropState.MESH, window, 42L, 7L, 12, coverage));
            assertEquals(41L, handoff.ownerSource());
            assertFalse(handoff.coveredBy(ArtworkBackdropState.MESH));
            assertFalse(sampler.needsApply(handoff.local()));
        }
        assertTrue(handoff.presented(3L, ArtworkBackdropState.MESH, window, 42L, 7L, 12, 1f));
        assertEquals(42L, handoff.ownerSource());
        assertTrue(handoff.coveredBy(ArtworkBackdropState.MESH));
        assertFalse(sampler.needsApply(handoff.local()));
        // A distinct same-track quality source follows exactly the same contract.
        handoff.select(4L, ArtworkBackdropState.MESH, window, 43L, 7L, 12);
        assertEquals(42L, handoff.ownerSource());
        assertFalse(sampler.needsApply(handoff.local()));
        assertEquals(2L, handoff.sourceRetentions());
    }

    @Test public void sourceAndRendererChangesKeepOnlyTheRealOutgoingPresenter() {
        ArtworkBackdropState.Handoff handoff = new ArtworkBackdropState.Handoff();
        Object window = new Object();
        handoff.select(1L, ArtworkBackdropState.MESH, window, 41L, 7L, 11);
        handoff.presented(1L, ArtworkBackdropState.MESH, window, 41L, 7L, 11, 1f);
        handoff.select(2L, ArtworkBackdropState.STILL, window, 42L, 8L, 12);
        assertTrue(handoff.retains(ArtworkBackdropState.MESH));
        assertEquals(41L, handoff.ownerSource());
        handoff.presented(2L, ArtworkBackdropState.STILL, window, 42L, 8L, 12, .5f);
        assertTrue(handoff.retains(ArtworkBackdropState.MESH));
        assertFalse(handoff.coveredBy(ArtworkBackdropState.STILL));
        handoff.presented(2L, ArtworkBackdropState.STILL, window, 42L, 8L, 12, 1f);
        assertFalse(handoff.retains(ArtworkBackdropState.MESH));
        assertTrue(handoff.coveredBy(ArtworkBackdropState.STILL));
        assertEquals(42L, handoff.ownerSource());
    }

    @Test public void aPreparedCandidateCannotReplaceAWithdrawnOutgoingSource() {
        ArtworkBackdropState.Handoff handoff = new ArtworkBackdropState.Handoff();
        Object window = new Object();
        handoff.select(1L, ArtworkBackdropState.STILL, window, 41L, 7L, 11);
        handoff.presented(1L, ArtworkBackdropState.STILL, window, 41L, 7L, 11, 1f);
        // This lower renderer was valid for source 41, not the next requested source.
        handoff.presented(1L, ArtworkBackdropState.MESH, window, 41L, 7L, 11, 1f);
        handoff.select(2L, ArtworkBackdropState.MESH, window, 42L, 7L, 12);
        handoff.withdraw(ArtworkBackdropState.STILL);
        assertFalse(handoff.local());
        assertFalse(handoff.coveredBy(ArtworkBackdropState.MESH));
        assertTrue(handoff.presented(2L, ArtworkBackdropState.MESH, window, 42L, 7L, 12, .1f));
        assertTrue(handoff.local());
        assertEquals(42L, handoff.ownerSource());
    }

    @Test public void disappearingOutgoingSourceRevokesTheSamplerBeforeIncomingProof() {
        ArtworkBackdropState.Handoff handoff = new ArtworkBackdropState.Handoff();
        NativeSamplingLease sampler = new NativeSamplingLease();
        Object window = new Object();
        sampler.bind(2, true, 0);
        handoff.select(1L, ArtworkBackdropState.MESH, window, 41L, 7L, 11);
        handoff.presented(1L, ArtworkBackdropState.MESH, window, 41L, 7L, 11, 1f);
        sampler.committed(handoff.local());
        handoff.select(2L, ArtworkBackdropState.MESH, window, 42L, 7L, 11);
        assertFalse(handoff.invisible(1L, ArtworkBackdropState.MESH, window, 41L));
        assertTrue(handoff.local());
        assertTrue(handoff.invisible(2L, ArtworkBackdropState.MESH, window, 42L));
        assertTrue(sampler.needsApply(handoff.local()));
        sampler.committed(handoff.local());
        assertFalse(sampler.local());
        // Returning to the same source cannot revive a hidden presenter's lease.
        handoff.select(3L, ArtworkBackdropState.MESH, window, 41L, 7L, 11);
        assertFalse(handoff.local());
        assertFalse(handoff.presented(1L, ArtworkBackdropState.MESH, window, 41L, 7L, 11, 1f));
    }

    @Test public void aNewWindowAndANativeSceneBothWithdrawTheRetainedSourceImmediately() {
        ArtworkBackdropState.Handoff handoff = new ArtworkBackdropState.Handoff();
        Object first = new Object(), second = new Object();
        handoff.select(1L, ArtworkBackdropState.STILL, first, 41L, 7L, 11);
        handoff.presented(1L, ArtworkBackdropState.STILL, first, 41L, 7L, 11, 1f);
        handoff.select(2L, ArtworkBackdropState.STILL, second, 41L, 8L, 12);
        assertFalse(handoff.local());
        assertFalse(handoff.presented(2L, ArtworkBackdropState.STILL, first, 41L, 8L, 12, 1f));
        handoff.presented(2L, ArtworkBackdropState.STILL, second, 41L, 8L, 12, 1f);
        handoff.select(3L, ArtworkBackdropState.NATIVE, second, 41L, 8L, 12);
        assertFalse(handoff.local());
        handoff.select(4L, ArtworkBackdropState.STILL, second, 41L, 8L, 12);
        assertFalse(handoff.local());
        assertFalse(handoff.presented(2L, ArtworkBackdropState.STILL, second, 41L, 8L, 12, 1f));
    }

    @Test public void aWithdrawnOutgoingRendererCannotFallBackToAnOldGeometryCandidate() {
        ArtworkBackdropState.Handoff handoff = new ArtworkBackdropState.Handoff();
        Object window = new Object();
        handoff.select(1L, ArtworkBackdropState.STILL, window, 41L, 7L, 11);
        handoff.presented(1L, ArtworkBackdropState.STILL, window, 41L, 7L, 11, 1f);
        handoff.presented(1L, ArtworkBackdropState.MESH, window, 41L, 7L, 11, 1f);
        handoff.select(2L, ArtworkBackdropState.MESH, window, 41L, 8L, 12);
        assertTrue(handoff.local());
        handoff.withdraw(ArtworkBackdropState.STILL);
        assertFalse(handoff.local());
        assertEquals(ArtworkBackdropState.NATIVE, handoff.owner());
    }

    @Test public void aZeroAlphaOutgoingDrawRevokesPixelsWithoutMasqueradingAsTheNewGeometry() {
        ArtworkBackdropState.Handoff handoff = new ArtworkBackdropState.Handoff();
        Object window = new Object();
        handoff.select(1L, ArtworkBackdropState.MESH, window, 41L, 7L, 11);
        handoff.presented(1L, ArtworkBackdropState.MESH, window, 41L, 7L, 11, 1f);
        handoff.select(2L, ArtworkBackdropState.STILL, window, 41L, 8L, 12);
        assertFalse(handoff.invisible(1L, ArtworkBackdropState.MESH, window, 41L));
        assertTrue(handoff.local());
        assertTrue(handoff.invisible(2L, ArtworkBackdropState.MESH, window, 41L));
        assertFalse(handoff.local());
    }

    @Test public void geometryAndAodRendererCanChangeTogetherWithoutRelabellingTheOldFrame() {
        ArtworkBackdropState.Handoff handoff = new ArtworkBackdropState.Handoff();
        Object window = new Object();
        handoff.select(1L, ArtworkBackdropState.MESH, window, 41L, 7L, 11);
        handoff.presented(1L, ArtworkBackdropState.MESH, window, 41L, 7L, 11, 1f);
        handoff.select(2L, ArtworkBackdropState.STILL, window, 41L, 8L, 12);
        assertTrue(handoff.retains(ArtworkBackdropState.MESH));
        handoff.presented(2L, ArtworkBackdropState.STILL, window, 41L, 8L, 12, .5f);
        assertEquals(ArtworkBackdropState.MESH, handoff.owner());
        assertEquals(7L, handoff.ownerGeometry());
        handoff.presented(2L, ArtworkBackdropState.STILL, window, 41L, 8L, 12, 1f);
        assertEquals(ArtworkBackdropState.STILL, handoff.owner());
        assertEquals(8L, handoff.ownerGeometry());
        assertEquals(41L, handoff.ownerSource());
    }

    @Test public void unavailableCompactLyricsDoNotOwnAnArtworkBackground() {
        // Instrumental/no-provider result: the requested page is LYRICS, but compact=true
        // and no lyric/BPM content can render. A prepared artwork bitmap does not change it.
        assertFalse(ArtworkBackdropState.foregroundReady(false, true, false));
        assertTrue(ArtworkBackdropState.foregroundReady(false, true, true));
    }

    @Test public void realArtworkCombinedAndExplicitPlayerPagesKeepTheirBackgroundChoice() {
        assertTrue(ArtworkBackdropState.foregroundReady(false, false, false)); // ARTWORK
        assertTrue(ArtworkBackdropState.foregroundReady(false, false, true)); // combined
        assertTrue(ArtworkBackdropState.foregroundReady(true, true, false)); // explicit PLAYER
        assertTrue(ArtworkBackdropState.foregroundReady(false, true, true)); // lyrics or BPM
    }

    @Test public void anEmptyLyricSelectionRemainsNativeAfterTheNotificationOverrideClears() {
        ArtworkBackdropState.Handoff handoff = new ArtworkBackdropState.Handoff();
        Object window = new Object();
        handoff.select(1L, ArtworkBackdropState.MESH, window, 4L, 3);
        handoff.presented(1L, ArtworkBackdropState.MESH, window, 4L, 3, 1f);
        // Opening LIST closes the artwork foreground.
        handoff.select(2L, ArtworkBackdropState.NATIVE, window, 4L, 3);
        assertFalse(handoff.local());
        // Explicit lyrics selection clears LIST's compact override, but an unavailable
        // lyric page is still compact and empty. Neither awake nor AOD may revive its fill.
        boolean ready = ArtworkBackdropState.foregroundReady(false, true, false);
        handoff.select(3L, ready ? ArtworkBackdropState.MESH : ArtworkBackdropState.NATIVE,
                window, 4L, 3);
        assertFalse(handoff.presented(3L, ArtworkBackdropState.MESH, window, 4L, 3, 1f));
        handoff.select(4L, ready ? ArtworkBackdropState.STILL : ArtworkBackdropState.NATIVE,
                window, 4L, 3);
        assertFalse(handoff.presented(1L, ArtworkBackdropState.MESH, window, 4L, 3, 1f));
        assertFalse(handoff.presented(4L, ArtworkBackdropState.STILL, window, 4L, 3, 1f));
        assertFalse(handoff.local());
    }

    @Test public void losingContentWithdrawsTheBackgroundAndRealContentCanReopenItWithoutATimer() {
        ArtworkBackdropState.Handoff handoff = new ArtworkBackdropState.Handoff();
        Object window = new Object();
        handoff.select(1L, ArtworkBackdropState.STILL, window, 0L, 8);
        handoff.presented(1L, ArtworkBackdropState.STILL, window, 0L, 8, 1f);
        assertTrue(handoff.local());
        boolean ready = ArtworkBackdropState.foregroundReady(false, true, false);
        handoff.select(2L, ready ? ArtworkBackdropState.STILL : ArtworkBackdropState.NATIVE,
                window, 0L, 8);
        assertFalse(handoff.local());
        assertFalse(handoff.retains(ArtworkBackdropState.STILL));
        // Actual lyric/BPM content becomes available while the selected page stays put.
        ready = ArtworkBackdropState.foregroundReady(false, true, true);
        handoff.select(3L, ready ? ArtworkBackdropState.STILL : ArtworkBackdropState.NATIVE,
                window, 0L, 8);
        assertFalse(handoff.local());
        assertFalse(handoff.presented(1L, ArtworkBackdropState.STILL, window, 0L, 8, 1f));
        assertTrue(handoff.presented(3L, ArtworkBackdropState.STILL, window, 0L, 8, 1f));
        assertTrue(handoff.local());
    }

    @Test public void reopeningArtworkAfterAnInstrumentalMissStillRequiresItsOwnDraw() {
        ArtworkBackdropState.Handoff handoff = new ArtworkBackdropState.Handoff();
        Object window = new Object();
        handoff.select(1L, ArtworkBackdropState.NATIVE, window, 0L, 5);
        boolean ready = ArtworkBackdropState.foregroundReady(false, false, false);
        handoff.select(2L, ready ? ArtworkBackdropState.MESH : ArtworkBackdropState.NATIVE,
                window, 0L, 5);
        assertFalse(handoff.local());
        assertFalse(handoff.presented(1L, ArtworkBackdropState.MESH, window, 0L, 5, 1f));
        assertTrue(handoff.presented(2L, ArtworkBackdropState.MESH, window, 0L, 5, 1f));
        assertTrue(handoff.local());
    }

    @Test public void fullScreenAodBackgroundDoesNotRequireAClockHoldOrVisibleClockContainer() {
        // A native full-AOD scene is sufficient even when the foreground clock never
        // acquired a custom pose or its own container is hidden by native notifications.
        assertTrue(ArtworkBackdropState.eligibleAod(true, false, true, true, false, true));
    }

    @Test public void aodBackgroundCannotLeakIntoNativeOnlyAodOrAnUnlockedWindow() {
        assertFalse(ArtworkBackdropState.eligibleAod(true, false, false, true, false, true));
        assertFalse(ArtworkBackdropState.eligibleAod(true, true, true, true, false, true));
        assertFalse(ArtworkBackdropState.eligibleAod(true, false, true, false, false, true));
        assertFalse(ArtworkBackdropState.eligibleAod(true, false, true, true, true, true));
        assertFalse(ArtworkBackdropState.eligibleAod(true, false, true, true, false, false));
        assertFalse(ArtworkBackdropState.eligibleAod(false, false, true, true, false, true));
    }

    @Test public void aodSamplingRequiresCurrentVisiblePixelsRatherThanAnOldVisibleWash() {
        assertTrue(ArtworkBackdropState.presentedFrame(4, 4, 64f / 255f));
        assertFalse(ArtworkBackdropState.presentedFrame(5, 4, 64f / 255f));
        assertFalse(ArtworkBackdropState.presentedFrame(-1, -1, 1f));
        assertFalse(ArtworkBackdropState.presentedFrame(4, 4, 0f));
        assertFalse(ArtworkBackdropState.presentedFrame(4, 4, Float.NaN));
        assertFalse(ArtworkBackdropState.presentedFrame(4, 4, Float.POSITIVE_INFINITY));
    }

    @Test public void preparingASourceDoesNotRevealItBeforeAnActualDraw() {
        ArtworkBackdropState state = new ArtworkBackdropState();
        state.select(4);
        assertFalse(state.reveal(state.epoch(), 4));
        state.drawn(4);
        assertTrue(state.reveal(state.epoch(), 4));
    }

    @Test public void aNewGenerationCannotBeRevealedByThePreviousFramesCallback() {
        ArtworkBackdropState state = new ArtworkBackdropState();
        state.select(4);
        state.drawn(4);
        long callback = state.epoch();
        state.select(5);
        assertFalse(state.reveal(callback, 4));
        assertFalse(state.reveal(callback, 5));
        state.drawn(5);
        assertTrue(state.reveal(state.epoch(), 5));
    }

    @Test public void unlockingOrDozingCancelsAQueuedFirstFrameFade() {
        ArtworkBackdropState state = new ArtworkBackdropState();
        state.select(7);
        state.drawn(7);
        long oldScene = state.epoch();
        state.hide();
        assertFalse(state.reveal(oldScene, 7));
        state.select(7);
        state.drawn(7);
        assertFalse(state.reveal(oldScene, 7));
        assertTrue(state.reveal(state.epoch(), 7));
    }

    @Test public void allUnlockAndNativeBackgroundGatesTakePriorityOverCachedArtwork() {
        assertTrue(ArtworkBackdropState.eligible(true, true, true, true, false, true));
        assertFalse(ArtworkBackdropState.eligible(false, true, true, true, false, true));
        assertFalse(ArtworkBackdropState.eligible(true, false, true, true, false, true));
        assertFalse(ArtworkBackdropState.eligible(true, true, false, true, false, true));
        assertFalse(ArtworkBackdropState.eligible(true, true, true, false, false, true));
        assertFalse(ArtworkBackdropState.eligible(true, true, true, true, true, true));
        assertFalse(ArtworkBackdropState.eligible(true, true, true, true, false, false));
    }

    @Test public void preparingTheReplacementKeepsOnlyTheActuallyDrawnOutgoingRenderer() {
        ArtworkBackdropState.Handoff handoff = new ArtworkBackdropState.Handoff();
        Object window = new Object();
        handoff.select(1L, ArtworkBackdropState.MESH, window, 7L, 4);
        assertFalse(handoff.local());
        handoff.presented(1L, ArtworkBackdropState.MESH, window, 7L, 4, 1f);
        handoff.select(2L, ArtworkBackdropState.STILL, window, 7L, 4);
        assertTrue(handoff.local());
        assertTrue(handoff.retains(ArtworkBackdropState.MESH));
        handoff.presented(2L, ArtworkBackdropState.STILL, window, 7L, 4, .25f);
        assertEquals(ArtworkBackdropState.MESH, handoff.owner());
        handoff.presented(2L, ArtworkBackdropState.STILL, window, 7L, 4, 1f);
        assertEquals(ArtworkBackdropState.STILL, handoff.owner());
        assertFalse(handoff.retains(ArtworkBackdropState.MESH));
    }

    @Test public void wakingKeepsTheStaticPixelsUntilTheMeshHasActuallyCoveredThem() {
        ArtworkBackdropState.Handoff handoff = new ArtworkBackdropState.Handoff();
        Object window = new Object();
        handoff.select(8L, ArtworkBackdropState.STILL, window, 2L, 6);
        handoff.presented(8L, ArtworkBackdropState.STILL, window, 2L, 6, 1f);
        handoff.select(9L, ArtworkBackdropState.MESH, window, 2L, 6);
        assertTrue(handoff.retains(ArtworkBackdropState.STILL));
        assertTrue(handoff.local());
        handoff.presented(9L, ArtworkBackdropState.MESH, window, 2L, 6, .9f);
        assertEquals(ArtworkBackdropState.STILL, handoff.owner());
        handoff.presented(9L, ArtworkBackdropState.MESH, window, 2L, 6, 1f);
        assertEquals(ArtworkBackdropState.MESH, handoff.owner());
    }

    @Test public void aNativeSceneRevokesOwnershipAndRejectsLateFramesEvenForTheSameBitmap() {
        ArtworkBackdropState.Handoff handoff = new ArtworkBackdropState.Handoff();
        Object window = new Object();
        handoff.select(1L, ArtworkBackdropState.MESH, window, 0L, 4);
        handoff.presented(1L, ArtworkBackdropState.MESH, window, 0L, 4, 1f);
        handoff.select(2L, ArtworkBackdropState.NATIVE, window, 0L, 4);
        assertFalse(handoff.local());
        handoff.select(3L, ArtworkBackdropState.MESH, window, 0L, 4);
        assertFalse(handoff.presented(1L, ArtworkBackdropState.MESH, window, 0L, 4, 1f));
        assertFalse(handoff.local());
        assertTrue(handoff.presented(3L, ArtworkBackdropState.MESH, window, 0L, 4, 1f));
        assertTrue(handoff.local());
    }

    @Test public void windowGeometryAndSourceChangesCannotRetainAStaleDisplayList() {
        ArtworkBackdropState.Handoff handoff = new ArtworkBackdropState.Handoff();
        Object first = new Object(), second = new Object();
        handoff.select(1L, ArtworkBackdropState.MESH, first, 0L, 4);
        handoff.presented(1L, ArtworkBackdropState.MESH, first, 0L, 4, 1f);
        handoff.select(2L, ArtworkBackdropState.STILL, second, 0L, 4);
        assertFalse(handoff.local());
        assertFalse(handoff.presented(2L, ArtworkBackdropState.STILL, first, 0L, 4, 1f));
        handoff.presented(2L, ArtworkBackdropState.STILL, second, 0L, 4, 1f);
        handoff.select(3L, ArtworkBackdropState.MESH, second, 1L, 4);
        assertFalse(handoff.local());
        handoff.presented(3L, ArtworkBackdropState.MESH, second, 1L, 4, 1f);
        handoff.select(4L, ArtworkBackdropState.STILL, second, 1L, 5);
        assertFalse(handoff.local());
        assertFalse(handoff.presented(4L, ArtworkBackdropState.STILL, second, 1L, 4, 1f));
    }

    @Test public void aSameBitmapPageChangeKeepsValidPixelsButRejectsOldCallbacks() {
        ArtworkBackdropState.Handoff handoff = new ArtworkBackdropState.Handoff();
        Object window = new Object();
        handoff.select(1L, ArtworkBackdropState.MESH, window, 0L, 4);
        handoff.presented(1L, ArtworkBackdropState.MESH, window, 0L, 4, 1f);
        handoff.select(2L, ArtworkBackdropState.MESH, window, 0L, 4);
        assertTrue(handoff.local());
        assertFalse(handoff.presented(1L, ArtworkBackdropState.STILL, window, 0L, 4, 1f));
        assertEquals(ArtworkBackdropState.MESH, handoff.owner());
    }

    @Test public void aHiddenOrZeroAlphaOwnerDoesNotAuthorizeLocalSampling() {
        ArtworkBackdropState.Handoff handoff = new ArtworkBackdropState.Handoff();
        Object window = new Object();
        handoff.select(1L, ArtworkBackdropState.MESH, window, 0L, 4);
        handoff.presented(1L, ArtworkBackdropState.MESH, window, 0L, 4, 1f);
        handoff.presented(1L, ArtworkBackdropState.MESH, window, 0L, 4, 0f);
        assertFalse(handoff.local());
        handoff.presented(1L, ArtworkBackdropState.MESH, window, 0L, 4, 1f);
        handoff.withdraw(ArtworkBackdropState.MESH);
        assertFalse(handoff.local());
    }

    @Test public void anUnrequestedRendererCannotStealTheSamplingOwner() {
        ArtworkBackdropState.Handoff handoff = new ArtworkBackdropState.Handoff();
        Object window = new Object();
        handoff.select(1L, ArtworkBackdropState.STILL, window, 0L, 4);
        handoff.presented(1L, ArtworkBackdropState.MESH, window, 0L, 4, 1f);
        assertFalse(handoff.local());
        handoff.presented(1L, ArtworkBackdropState.STILL, window, 0L, 4, .1f);
        assertEquals(ArtworkBackdropState.STILL, handoff.owner());
    }
    @Test public void fullWakeCannotRetireTheOldArtworkOnBackgroundCoverageAlone() {
        ArtworkBackdropState.Handoff handoff = new ArtworkBackdropState.Handoff();
        Object window = new Object();
        handoff.select(1L, ArtworkBackdropState.STILL, window, 7L, 3L, 6);
        handoff.presented(1L, ArtworkBackdropState.STILL, window, 7L, 3L, 6, 1f);
        handoff.select(2L, ArtworkBackdropState.MESH, window, 7L, 3L, 6);
        for (int step = 0; step < 100; step++) {
            float band = step / 100f;
            float evidence = ArtworkBackdropState.compositionEvidence(1f, 1f, band);
            if (Float.isFinite(evidence))
                handoff.presented(2L, ArtworkBackdropState.MESH, window, 7L, 3L, 6, evidence);
            assertTrue(handoff.retains(ArtworkBackdropState.STILL));
        }
        handoff.presented(2L, ArtworkBackdropState.MESH, window, 7L, 3L, 6,
                ArtworkBackdropState.compositionEvidence(1f, 1f, 1f));
        assertEquals(ArtworkBackdropState.MESH, handoff.owner());
    }

    @Test public void reversingBeforeTheNewBandCoversCannotRevokeItsOriginalPresenter() {
        ArtworkBackdropState.Handoff handoff = new ArtworkBackdropState.Handoff();
        Object window = new Object();
        handoff.select(1L, ArtworkBackdropState.STILL, window, 7L, 3L, 6);
        handoff.presented(1L, ArtworkBackdropState.STILL, window, 7L, 3L, 6, 1f);
        handoff.select(2L, ArtworkBackdropState.MESH, window, 7L, 3L, 6);
        handoff.presented(2L, ArtworkBackdropState.MESH, window, 7L, 3L, 6,
                ArtworkBackdropState.compositionEvidence(.4f, .4f, 1f));
        handoff.select(3L, ArtworkBackdropState.STILL, window, 7L, 3L, 6);
        assertEquals(ArtworkBackdropState.STILL, handoff.owner());
        assertTrue(handoff.local());
        assertFalse(handoff.presented(2L, ArtworkBackdropState.MESH, window, 7L, 3L, 6, 1f));
    }

    @Test public void wakeRetiresTheUpperStillCompositionOnlyAfterCompleteLowerCoverage() {
        ArtworkBackdropState.Handoff handoff = new ArtworkBackdropState.Handoff();
        Object window = new Object();
        handoff.select(1L, ArtworkBackdropState.STILL, window, 2L, 1L, 5);
        handoff.presented(1L, ArtworkBackdropState.STILL, window, 2L, 1L, 5, 1f);
        handoff.select(2L, ArtworkBackdropState.MESH, window, 2L, 1L, 5);
        // Merely selecting the awake renderer cannot remove the upper still display list.
        assertFalse(handoff.coveredBy(ArtworkBackdropState.MESH));
        assertTrue(handoff.retains(ArtworkBackdropState.STILL));
        for (float coverage : new float[]{.1f, .5f, .99f}) {
            handoff.presented(2L, ArtworkBackdropState.MESH, window, 2L, 1L, 5, coverage);
            assertFalse(handoff.coveredBy(ArtworkBackdropState.MESH));
            assertTrue(handoff.retains(ArtworkBackdropState.STILL));
        }
        handoff.presented(2L, ArtworkBackdropState.MESH, window, 2L, 1L, 5, 1f);
        assertTrue(handoff.coveredBy(ArtworkBackdropState.MESH));
        assertFalse(handoff.retains(ArtworkBackdropState.STILL));
        assertFalse(handoff.coveredBy(ArtworkBackdropState.STILL));
    }

    @Test public void oldCoverageCannotRetireTheUpperRendererForAnotherComposition() {
        ArtworkBackdropState.Handoff handoff = new ArtworkBackdropState.Handoff();
        Object window = new Object();
        handoff.select(1L, ArtworkBackdropState.MESH, window, 2L, 1L, 5);
        // First presentation can own a renderer while still partially visible.
        handoff.presented(1L, ArtworkBackdropState.MESH, window, 2L, 1L, 5, .5f);
        assertFalse(handoff.coveredBy(ArtworkBackdropState.MESH));
        handoff.presented(1L, ArtworkBackdropState.MESH, window, 2L, 1L, 5, 1f);
        assertTrue(handoff.coveredBy(ArtworkBackdropState.MESH));
        handoff.select(2L, ArtworkBackdropState.MESH, window, 2L, 2L, 6);
        assertFalse(handoff.coveredBy(ArtworkBackdropState.MESH));
        assertFalse(handoff.presented(1L, ArtworkBackdropState.MESH, window, 2L, 1L, 5, 1f));
        assertFalse(handoff.coveredBy(ArtworkBackdropState.MESH));
        handoff.presented(2L, ArtworkBackdropState.MESH, window, 2L, 2L, 6, 1f);
        assertTrue(handoff.coveredBy(ArtworkBackdropState.MESH));
    }

}

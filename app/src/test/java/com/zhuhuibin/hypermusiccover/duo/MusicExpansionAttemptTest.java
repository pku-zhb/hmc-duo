package com.zhuhuibin.hypermusiccover.duo;

import org.junit.Test;
import static org.junit.Assert.*;

public class MusicExpansionAttemptTest {
    private MusicExpansionAttempt<String> attempt() { return new MusicExpansionAttempt<>("paused", 7, 100L); }

    @Test public void aPausedSessionWithItsRealHeaderOpensImmediately() {
        assertEquals(MusicExpansionAttempt.Step.OPEN,
                attempt().evaluate("paused", 7, true, true, true, true, 100));
    }
    @Test public void detachedHeaderDoesNotCommitSelectionWhileItsReplacementArrives() {
        MusicExpansionAttempt<String> a = attempt();
        assertEquals(MusicExpansionAttempt.Step.WAIT, a.evaluate("paused", 7, true, true, true, false, 100));
        assertEquals(MusicExpansionAttempt.Step.WAIT, a.evaluate("paused", 7, true, true, true, false, 700));
        assertEquals(MusicExpansionAttempt.Step.OPEN, a.evaluate("paused", 7, true, true, true, true, 900));
    }
    @Test public void aMissingHeaderExpiresWithinOneSecond() {
        MusicExpansionAttempt<String> a = attempt();
        assertEquals(MusicExpansionAttempt.Step.WAIT, a.evaluate("paused", 7, true, true, true, false, 1099));
        assertEquals(1L, a.retryAfter(1099));
        assertEquals(MusicExpansionAttempt.Step.EXPIRE, a.evaluate("paused", 7, true, true, true, false, 1100));
    }
    @Test public void oldTimeoutCannotDiscardANewCardForTheSameSession() {
        assertEquals(MusicExpansionAttempt.Step.CANCEL,
                attempt().evaluate("paused", 8, true, true, true, false, 1500));
        assertEquals(MusicExpansionAttempt.Step.CANCEL,
                attempt().evaluate("paused", 8, true, true, true, true, 700));
    }
    @Test public void oldTimeoutCannotDiscardAReplacementSession() {
        assertEquals(MusicExpansionAttempt.Step.CANCEL,
                attempt().evaluate("replacement", 7, true, true, true, false, 1500));
    }
    @Test public void unlockingOrLosingTheSessionCancelsWithoutOpeningOrExpiring() {
        assertEquals(MusicExpansionAttempt.Step.CANCEL,
                attempt().evaluate("paused", 7, false, true, true, false, 1500));
        assertEquals(MusicExpansionAttempt.Step.CANCEL,
                attempt().evaluate("paused", 7, true, false, true, false, 1500));
        assertEquals(MusicExpansionAttempt.Step.CANCEL,
                attempt().evaluate("paused", 7, true, true, false, false, 1500));
    }
    @Test public void readyHeaderWinsAtTheDeadlineWithoutAnUnnecessaryDiscard() {
        assertEquals(MusicExpansionAttempt.Step.OPEN,
                attempt().evaluate("paused", 7, true, true, true, true, 1100));
    }
    @Test public void nativeResumeCardsNeverBorrowAnotherPausedController() {
        assertTrue(MusicExpansionAttempt.nativeOnly(true, false, false));
        assertTrue(MusicExpansionAttempt.nativeOnly(true, true, true));
        assertTrue(MusicExpansionAttempt.nativeOnly(false, true, false));
        assertFalse(MusicExpansionAttempt.nativeOnly(false, true, true));
        // Unknown token metadata is not fabricated into proof that a session died.
        assertFalse(MusicExpansionAttempt.nativeOnly(false, false, false));
    }
}

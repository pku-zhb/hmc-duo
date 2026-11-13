package com.zhuhuibin.hypermusiccover.duo;

import org.junit.Test;
import static org.junit.Assert.*;

public class LyricPageRecoveryTest {
    @Test public void relockingOnTheSameTrackRestoresWordsHiddenByAnotherPage() {
        LyricPageRecovery recovery = new LyricPageRecovery();
        recovery.lookupStarted("song", 1000L);
        assertEquals(LyricPageRecovery.RESTORE,
                recovery.onPageSelected("song", 1100L, false, false, true));
        assertEquals(LyricPageRecovery.NONE,
                recovery.onPageSelected("song", 1200L, false, true, true));
    }

    @Test public void cachedWordsNeedNotWaitForAnOptionalProviderUpgrade() {
        LyricPageRecovery recovery = new LyricPageRecovery();
        recovery.lookupStarted("song", 1000L);
        assertEquals(LyricPageRecovery.RESTORE,
                recovery.onPageSelected("song", 1200L, true, false, true));
    }

    @Test public void aFailedSameTrackLookupCanBeRetriedOnALaterExplicitEntry() {
        LyricPageRecovery recovery = new LyricPageRecovery();
        recovery.lookupStarted("song", 1000L);
        recovery.lookupFinished();
        assertEquals(LyricPageRecovery.NONE,
                recovery.onPageSelected("song", 1100L, false, false, false));
        assertEquals(LyricPageRecovery.RETRY,
                recovery.onPageSelected("song", 16000L, false, false, false));
        recovery.lookupStarted("song", 16000L);
        assertEquals(LyricPageRecovery.NONE,
                recovery.onPageSelected("song", 16100L, false, false, false));
    }

    @Test public void anotherEntryDoesNotDuplicateAnInFlightLookupEvenAfterTheCooldown() {
        LyricPageRecovery recovery = new LyricPageRecovery();
        recovery.lookupStarted("song", 1000L);
        assertEquals(LyricPageRecovery.NONE,
                recovery.onPageSelected("song", 31000L, true, false, false));
        assertEquals(LyricPageRecovery.RETRY,
                recovery.onPageSelected("song", 31000L, false, false, false));
    }

    @Test public void noTrackRemainsUnavailableWithoutStartingARequest() {
        LyricPageRecovery recovery = new LyricPageRecovery();
        assertEquals(LyricPageRecovery.NONE,
                recovery.onPageSelected("", 1000L, false, false, false));
        assertEquals(LyricPageRecovery.NONE,
                recovery.onPageSelected(null, 1000L, false, false, false));
        assertEquals(LyricPageRecovery.RETRY,
                recovery.onPageSelected("another", 1000L, false, false, false));
    }

    @Test public void anEarlyEmptyResultDoesNotDuplicateTheStillRunningFallback() {
        LyricPageRecovery recovery = new LyricPageRecovery();
        recovery.lookupStarted("song", 1000L);
        assertEquals(LyricPageRecovery.NONE,
                recovery.onPageSelected("song", 17000L, false, false, false));
        recovery.lookupFinished();
        assertEquals(LyricPageRecovery.RETRY,
                recovery.onPageSelected("song", 17000L, false, false, false));
    }
}

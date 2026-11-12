package com.zhuhuibin.hypermusiccover.duo;

import org.junit.Test;
import static org.junit.Assert.*;

public class WallpaperHandoffHistoryTest {
    @Test public void laterSleepCannotEraseUnlockDimEvidence() {
        WallpaperHandoffHistory history = new WallpaperHandoffHistory();
        history.add(100, "black", "value=0.55");
        assertTrue(history.add(200, "unlock", "away=true aod=true"));
        history.add(250, "black", "value=0.3");
        for (int i = 0; i < 200; i++) history.add(3000 + i, "doze", "later-sleep");
        String result = history.describe();
        assertTrue(result.contains("100 black value=0.55"));
        assertTrue(result.contains("250 black value=0.3"));
        assertFalse(result.contains("later-sleep"));
    }

    @Test public void repeatedExitWithinWindowKeepsTheOriginalAodPrelude() {
        WallpaperHandoffHistory history = new WallpaperHandoffHistory();
        assertTrue(history.add(100, "unlock", "away=true aod=true"));
        history.add(200, "unlock", "away=false");
        assertFalse(history.add(220, "unlock", "away=true aod=false"));
        assertFalse(history.describe().contains("unlock@220"));
        assertTrue(history.describe().contains("aod=true"));
    }

    @Test public void onlyThreeMostRecentUnlockWindowsAreRetained() {
        WallpaperHandoffHistory history = new WallpaperHandoffHistory();
        for (int i = 1; i <= 4; i++) history.add(i * 2000L, "unlock", "away=true");
        assertFalse(history.describe().contains("unlock@2000"));
        assertTrue(history.describe().contains("unlock@4000"));
        assertTrue(history.describe().contains("unlock@8000"));
    }

    @Test public void lateDrawSummaryStillBelongsToItsOwnWindow() {
        WallpaperHandoffHistory history = new WallpaperHandoffHistory();
        history.add(100, "unlock", "away=true");
        history.finish(100, 2200, "pre=80 draw=0");
        assertTrue(history.describe().contains("2200 frames pre=80 draw=0"));
    }
}

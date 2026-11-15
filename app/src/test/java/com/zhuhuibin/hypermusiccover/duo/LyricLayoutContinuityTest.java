package com.zhuhuibin.hypermusiccover.duo;

import java.util.Arrays;
import java.util.Collections;
import org.junit.Test;
import static org.junit.Assert.*;

public class LyricLayoutContinuityTest {
    private LyricLine line(String text, String translation, int start) {
        return new LyricLine(text, translation, start, start + 1000, false, null, null, null);
    }

    @Test public void widthOrTranslationReflowKeepsTheExistingSongState() {
        assertTrue(LyricLayoutContinuity.sameTimeline(
                Arrays.asList(line("first", null, 0), line("second", null, 2000)),
                Arrays.asList(line("first", "translated", 0), line("second", "translated", 2000))));
    }

    @Test public void anotherSongOrChangedTimingCannotBorrowPlaybackState() {
        assertFalse(LyricLayoutContinuity.sameTimeline(Collections.emptyList(), Collections.emptyList()));
        assertFalse(LyricLayoutContinuity.sameTimeline(
                Collections.singletonList(line("first", null, 0)),
                Collections.singletonList(line("second", null, 0))));
        assertFalse(LyricLayoutContinuity.sameTimeline(
                Collections.singletonList(line("first", null, 0)),
                Collections.singletonList(line("first", null, 10))));
    }

    @Test public void reflowPreservesEveryDrawnRowBeforeTheExistingSpringCarriesItHome() {
        float[] oldBase = {0, 100, 200}, newBase = {0, 150, 250};
        float[] scroll = {80, 90, 100}, aim = {100, 100, 100}, next = {100, 100, 100};
        float[] drawn = {oldBase[0] - scroll[0], oldBase[1] - scroll[1], oldBase[2] - scroll[2]};
        LyricLayoutContinuity.rebase(oldBase, newBase, scroll, aim, next, 150);
        for (int i = 0; i < 3; i++) {
            assertEquals(drawn[i], newBase[i] - scroll[i], 0f);
            assertEquals(150f, next[i], 0f);
        }
        assertArrayEquals(new float[]{100, 150, 150}, aim, 0f);
    }
}

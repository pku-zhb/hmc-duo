package com.zhuhuibin.hypermusiccover.duo;

import org.junit.Test;
import static org.junit.Assert.*;

public class CompactPlayerFrameHistoryTest {
    @Test public void evidenceIsBoundedAndKeepsTheLatestFramesInChronologicalOrder() {
        CompactPlayerFrameHistory history = new CompactPlayerFrameHistory();
        int[] row = history.sample();
        row[CompactPlayerFrameHistory.EVENT] = CompactPlayerFrameHistory.HEIGHT;
        for (int i = 0; i < CompactPlayerFrameHistory.CAPACITY + 3; i++) {
            row[CompactPlayerFrameHistory.BACKGROUND_HEIGHT] = i;
            history.append(i);
        }
        assertEquals(CompactPlayerFrameHistory.CAPACITY, history.size());
        String description = history.describe();
        assertTrue(description.contains("[3,height,"));
        assertFalse(description.contains("[0,height,"));
        assertTrue(description.contains(";258,height,"));
    }

    @Test public void repeatedPreDrawAndHeightCallbacksDoNotHideTheEarlierTransition() {
        CompactPlayerFrameHistory history = new CompactPlayerFrameHistory();
        int[] row = history.sample();
        row[CompactPlayerFrameHistory.EVENT] = CompactPlayerFrameHistory.HEIGHT;
        row[CompactPlayerFrameHistory.ACTUAL_HEIGHT] = 312;
        history.append(1);
        for (int i = 0; i < 1000; i++) {
            row[CompactPlayerFrameHistory.EVENT] = i % 2 == 0 ? CompactPlayerFrameHistory.LAYOUT : CompactPlayerFrameHistory.HEIGHT;
            history.append(i + 2);
        }
        assertEquals(1, history.size());
        row[CompactPlayerFrameHistory.EVENT] = CompactPlayerFrameHistory.FULL_AOD;
        history.append(1002);
        assertEquals(2, history.size());
        assertTrue(history.describe().contains(";1002,fullAod,"));
    }

    @Test public void diagnosticFloatsStayExactUntilTheDescriptionIsRequested() {
        CompactPlayerFrameHistory history = new CompactPlayerFrameHistory();
        int[] row = history.sample();
        row[CompactPlayerFrameHistory.EVENT] = CompactPlayerFrameHistory.TARGET;
        row[CompactPlayerFrameHistory.SEEK_ALPHA] = Float.floatToIntBits(.25f);
        row[CompactPlayerFrameHistory.BACKGROUND_BIAS] = Float.floatToIntBits(.5f);
        history.append(1234);
        assertTrue(history.describe().contains(",0.25,0.5,"));
    }
}

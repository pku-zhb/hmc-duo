package com.zhuhuibin.hypermusiccover.duo;

import org.junit.Test;
import static org.junit.Assert.*;

public class RailContentWidthTest {
    @Test public void repeatedPixelWriteBackDoesNotContractAnAlreadyContractedCard() {
        RailContentWidth width = new RailContentWidth(1000, 1000);
        int requested = 1000;
        for (int i = 0; i < 100; i++) {
            width.observe(requested);
            int next = width.target(1000, .8f);
            assertEquals(800, next);
            width.wrote(next);
            requested = next + 1; // OEM rounds the measured child back into LayoutParams.
        }
        assertEquals(1000, width.restore(800));
    }

    @Test public void matchParentUsesTheParentEvenAfterNativePixelWriteBack() {
        RailContentWidth width = new RailContentWidth(-1, 1000);
        width.wrote(width.target(1000, .8f));
        width.observe(801);
        assertEquals(800, width.target(1000, .8f));
        assertEquals(960, width.target(1200, .8f));
        width.wrote(960);
        assertEquals(-1, width.restore(960));
    }

    @Test public void repeatedLockSessionsRestoreTheUncontractedSizingIntent() {
        for (int nativeWidth : new int[]{1000, -1}) {
            int layoutWidth = nativeWidth;
            for (int session = 0; session < 20; session++) {
                RailContentWidth width = new RailContentWidth(layoutWidth, 1000);
                int contracted = width.target(1000, .8f);
                assertEquals(800, contracted);
                width.wrote(contracted);
                width.observe(contracted + 1);
                width.wrote(width.target(1000, .8f));
                layoutWidth = width.restore(800);
                assertEquals(nativeWidth, layoutWidth);
            }
        }
    }

    @Test public void geometryReplacementCapturesANewNativeBaseline() {
        RailContentWidth portrait = new RailContentWidth(-1, 1000);
        portrait.wrote(portrait.target(1000, .8f));
        int restored = portrait.restore(800);
        RailContentWidth landscape = new RailContentWidth(restored, 1800);
        assertEquals(1440, landscape.target(1800, .8f));
        RailContentWidth fixed = new RailContentWidth(1500, 1500);
        assertEquals(1200, fixed.target(1800, .8f));
    }

    @Test public void intrinsicSizingAndExplicitSizingModeChangesRemainNative() {
        RailContentWidth width = new RailContentWidth(-2, 500);
        assertEquals(-2, width.target(1000, .8f));
        width.observe(600);
        assertEquals(480, width.target(1000, .8f));
        width.wrote(480);
        width.observe(-1);
        assertEquals(800, width.target(1000, .8f));
        width.wrote(800);
        width.observe(-2);
        width.wrote(width.target(1000, .8f));
        assertEquals(-2, width.restore(-2));
    }

    @Test public void releaseDoesNotOverwriteAnotherOwnersLastWrite() {
        RailContentWidth width = new RailContentWidth(-1, 1000);
        width.wrote(width.target(1000, .8f));
        assertEquals(920, width.restore(920));
        assertEquals(-2, width.restore(-2));
        assertEquals(-1, width.restore(800));
    }
}

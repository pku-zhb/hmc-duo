package com.zhuhuibin.hypermusiccover.duo;

import org.junit.Test;
import static org.junit.Assert.*;

public class GeometryLeaseTest {
    @Test public void disablingReflowRestoresMatchParentAndItsOriginalOffset() {
        GeometryLease l = new GeometryLease(-1, 4);
        l.observe(-1, 4); l.wrote(316, -42);
        assertEquals(-1, l.restoreWidth(316));
        assertEquals(4, l.restoreX(-42), 0);
    }
    @Test public void repeatedFramesDoNotTreatOurOwnGeometryAsTheNativeBaseline() {
        GeometryLease l = new GeometryLease(400, 0);
        for (int i = 0; i < 100; i++) {
            l.observe(i == 0 ? 400 : 316, i == 0 ? 0 : -42);
            l.wrote(316, -42);
        }
        assertEquals(400, l.restoreWidth(316));
        assertEquals(0, l.restoreX(-42), 0);
    }
    @Test public void nativeGeometryChangesAreNeverRolledBackToAnOlderLayout() {
        GeometryLease l = new GeometryLease(400, 0);
        l.wrote(316, -42);
        // A rotation/other owner changes the view before our next frame.
        assertEquals(500, l.restoreWidth(500));
        assertEquals(12, l.restoreX(12), 0);
        l.observe(500, 12); l.wrote(395, -60);
        assertEquals(500, l.restoreWidth(395));
        assertEquals(12, l.restoreX(-60), 0);
    }
}

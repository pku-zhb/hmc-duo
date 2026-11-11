package com.zhuhuibin.hypermusiccover.duo;

import org.junit.Test;
import static org.junit.Assert.*;

public class RailAodInsetTest {
    @Test public void keepsThirtyPercentOfTheNativeInset() {
        assertEquals(1053.8f, RailAodInset.target(1061, 1037), .001f);
        assertEquals(1061, RailAodInset.target(1061, 1061), 0);
    }
    @Test public void compensatesInParentCoordinatesAndRestoresNativeOwnership() {
        GeometryLease lease = new GeometryLease(132, 10);
        float x = RailAodInset.translation(10, 1037, 1061, .95f);
        assertEquals(1053.8f, 1037 + (x - 10) * .95f, .001f);
        lease.wrote(132, x);
        assertEquals(10, lease.restoreX(x), 0);
        lease.observe(132, 15);
        assertEquals(15, lease.restoreX(15), 0);
    }
}

package com.zhuhuibin.hypermusiccover.duo

import org.junit.Assert.*
import org.junit.Test
import kotlin.math.abs

class RailContentMotionTest {
    @Test fun aLongPullOnlyMovesContentByEightDp() {
        for (d in listOf(1f, 1.5f, 2.75f, 3f)) {
            for (distance in listOf(-100000f, -200f, 0f, 200f, 100000f)) {
                assertTrue(abs(RailContentMotion.pull(distance * d, d)) <= 8f * d)
            }
            assertEquals(0f, RailContentMotion.pull(0f, d), 0f)
        }
        assertEquals(0f, RailContentMotion.pull(Float.NaN, 2f), 0f)
    }

    @Test fun replacementOccursOnlyAfterTheOutgoingContentHasFaded() {
        val outgoing = RailContentMotion.frame(.499f, true, 2.75f)
        val incoming = RailContentMotion.frame(.5f, true, 2.75f)
        assertFalse(outgoing.incoming)
        assertTrue(outgoing.alpha < .001f)
        assertTrue(incoming.incoming)
        assertEquals(0f, incoming.alpha, 0f)
        assertTrue(outgoing.y < 0f)
        assertTrue(incoming.y > 0f)
    }

    @Test fun previousAndNextTravelInOppositeDirectionsAndLandAtRest() {
        for (p in listOf(0f, .2f, .49f, .5f, .8f, 1f)) {
            val next = RailContentMotion.frame(p, true, 2f)
            val previous = RailContentMotion.frame(p, false, 2f)
            assertEquals(next.y, -previous.y, .001f)
            assertTrue(abs(next.y) <= 56f)
            assertTrue(next.alpha in 0f..1f)
        }
        val landed = RailContentMotion.frame(1f, true, 2f)
        assertEquals(0f, landed.y, 0f)
        assertEquals(1f, landed.alpha, 0f)
    }

    @Test fun contentStartsWhereTheFingerLeftItWithoutMovingTheShell() {
        val start = RailContentMotion.frame(0f, true, 2.75f, -18f)
        assertEquals(-18f, start.y, 0f)
        assertEquals(1f, start.alpha, 0f)
    }
}

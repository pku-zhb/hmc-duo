package com.zhuhuibin.hypermusiccover.duo

import org.junit.Assert.*
import org.junit.Test

class RailFusionDragTest {
    @Test fun aStationaryFingerHoldsTheEntireExchangeAtOneFrame() {
        val span = RailFusionDrag.span(221f, 1f)
        val held = RailFusionDrag.progress(0f, -36f, true, span)
        assertTrue(held > 0f && held < .5f)
        repeat(100) { assertEquals(held, RailFusionDrag.progress(0f, -36f, true, span), 0f) }
        assertEquals(.5f, RailFusionDrag.progress(0f, -10000f, true, span), 0f)
    }
    @Test fun pullingBackFollowsTheSameFramesInReverse() {
        val span = RailFusionDrag.span(221f, 1f)
        val out = (0..80).map { RailFusionDrag.progress(0f, -it.toFloat(), true, span) }
        val back = (80 downTo 0).map { RailFusionDrag.progress(0f, -it.toFloat(), true, span) }
        assertEquals(out, back.reversed())
        assertEquals(0f, back.last(), 0f)
        assertFalse(RailFusionDrag.commit(.1f, -10f, 0f, true, 1f, false))
        assertFalse(RailFusionDrag.commit(.5f, -80f, 0f, true, 1f, true))
    }
    @Test fun catchingASettlingExchangeKeepsItsExactPoseWithoutFinishingIt() {
        for (start in listOf(.16f, .5f, .76f)) {
            assertEquals(start, RailFusionDrag.progress(start, 0f, true, 100f), 0f)
            assertTrue(RailFusionDrag.progress(start, 20f, true, 100f) < start)
        }
        assertEquals(.76f, RailFusionDrag.progress(.76f, -100f, true, 100f), 0f)
    }
    @Test fun releaseHonorsDirectionAndCancellation() {
        assertTrue(RailFusionDrag.commit(.4f, -70f, 0f, true, 1f, false))
        assertFalse(RailFusionDrag.commit(.4f, -70f, 700f, true, 1f, false))
        assertTrue(RailFusionDrag.commit(.1f, 20f, 1500f, false, 1f, false))
        assertFalse(RailFusionDrag.commit(.1f, 20f, 1500f, false, 1f, true))
        assertTrue(RailFusionDrag.settleDuration(.48f, 1f) < RailFusionDrag.settleDuration(.1f, 1f))
    }

    @Test fun elasticPullSoftensTowardExactContactWithoutStretchingPastIt() {
        val distance = RailFusionDrag.contactSpan(58f, 1f)
        val poses = (0..100).map { RailFusionDrag.progress(0f, -distance * it / 100f,
            true, distance, elastic = true) }
        assertEquals(0f, poses.first(), 0f)
        assertEquals(.5f, poses.last(), .000001f)
        assertTrue(poses.zipWithNext().all { (a, b) -> a < b })
        assertTrue(poses[10] - poses[0] > poses[100] - poses[90])
        assertEquals(.5f, RailFusionDrag.progress(0f, -distance * 2f, true, distance, true), 0f)
        assertEquals(.5f, RailFusionDrag.progress(0f, distance, false, distance, true), .000001f)
        assertEquals(distance * 3f, RailFusionDrag.contactSpan(58f * 3f, 3f), .001f)
    }

    @Test fun elasticPullCanReverseOrCatchAnExistingPoseWithoutAJump() {
        val distance = RailFusionDrag.contactSpan(58f, 1f)
        for (start in listOf(.2f, .5f, .76f)) {
            assertEquals(start, RailFusionDrag.progress(start, 0f, true, distance, true), 0f)
            assertTrue(RailFusionDrag.progress(start, 20f, true, distance, true) < start)
            assertEquals(0f, RailFusionDrag.progress(start, 10000f, true, distance, true), 0f)
        }
        assertEquals(.5f, RailFusionDrag.progress(0f, -Float.MAX_VALUE, true,
            Float.MIN_VALUE, true), 0f)
        assertEquals(0f, RailFusionDrag.progress(0f, Float.NaN, true, distance, true), 0f)
        // Opting into the elastic HyperOS path leaves the ColorOS waist mapping unchanged.
        assertEquals(.25f, RailFusionDrag.progress(0f, -distance / 2f, true, distance), 0f)
    }

    @Test fun recoilDisappearsContinuouslyAtThePictureOwnershipHandoff() {
        assertEquals(0f, RailFusionDrag.recoilWeight(.5f), 0f)
        assertEquals(1f, RailFusionDrag.recoilWeight(.42f), .000001f)
        assertEquals(1f, RailFusionDrag.recoilWeight(.58f), .000001f)
        for (t in listOf(.1f, .2f, .3f, 0f, 1f)) assertEquals(1f, RailFusionDrag.recoilWeight(t), 0f)
        val approach = (0..100).map { RailFusionDrag.recoilWeight(.42f + .08f * it / 100f) }
        assertTrue(approach.zipWithNext().all { (a, b) -> a >= b })
        assertEquals(RailFusionDrag.recoilWeight(.46f), RailFusionDrag.recoilWeight(.54f), .000001f)
        assertTrue(100f * RailFusionDrag.recoilWeight(.49999f) < .001f)
        assertTrue(100f * RailFusionDrag.recoilWeight(.50001f) < .001f)
        assertEquals(0f, RailFusionDrag.recoilWeight(Float.NaN), 0f)
        assertEquals(0f, RailFusionDrag.recoilWeight(Float.POSITIVE_INFINITY), 0f)
    }

    @Test fun pausingAtContactStillReleasesStoredTensionWithoutAnInitialJump() {
        for (up in listOf(true, false)) {
            val velocity = RailFusionDrag.releaseVelocity(.5f, 0f, up, 1f, true)
            assertEquals(if (up) 250f else -250f, velocity, 0f)
            val spring = CoverMorphMotion()
            spring.value = 0f
            spring.target = 0f
            spring.velocity = velocity / 100f
            assertEquals(0f, spring.value, 0f)
            var peak = 0f
            repeat(240) {
                spring.step(1f / 120f, .32f)
                peak = maxOf(peak, kotlin.math.abs(spring.value * 100f))
            }
            assertTrue("The stored tension should produce a small visible recoil", peak in 3f..8f)
            assertTrue(spring.atRest())
        }
        assertTrue(RailFusionDrag.releaseVelocity(.1f, 0f, true, 1f, true) <
            RailFusionDrag.releaseVelocity(.3f, 0f, true, 1f, true))
        assertEquals(750f, RailFusionDrag.releaseVelocity(.5f, 0f, true, 3f, true), 0f)
    }

    @Test fun reversalAndCancellationDoNotAddSyntheticReleaseTension() {
        assertEquals(0f, RailFusionDrag.releaseVelocity(.5f, 0f, true, 1f, false), 0f)
        assertEquals(-350f, RailFusionDrag.releaseVelocity(.5f, -1000f, true, 1f, false), 0f)
        assertEquals(0f, RailFusionDrag.releaseVelocity(.5f, 1500f, true, 1f, true, true), 0f)
    }

    @Test fun releaseSpeedRemainsFiniteAndBoundedForMalformedInputsAndFastFlings() {
        for (vy in listOf(-Float.MAX_VALUE, -10000f, 10000f, Float.MAX_VALUE)) {
            val speed = RailFusionDrag.releaseVelocity(.5f, vy, true, 3f, true)
            assertTrue(speed.isFinite() && kotlin.math.abs(speed) <= 3000f)
        }
        assertTrue(RailFusionDrag.releaseVelocity(Float.MAX_VALUE, Float.MAX_VALUE,
            true, Float.MAX_VALUE, true).isFinite())
        assertEquals(0f, RailFusionDrag.releaseVelocity(Float.NaN, 0f, true, 1f, true), 0f)
        assertEquals(0f, RailFusionDrag.releaseVelocity(.5f, Float.NaN, true, 1f, true), 0f)
        assertEquals(0f, RailFusionDrag.releaseVelocity(.5f, 0f, true, 0f, true), 0f)
    }
}

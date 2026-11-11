package com.zhuhuibin.hypermusiccover.duo

import org.junit.Assert.*
import org.junit.Test

class NotificationReturnProgressTest {
    @Test fun anInstantNativeFoldCannotSkipBubbleBirth() {
        val motion = NotificationReturnProgress(0f)
        assertEquals(0f, motion.update(0, 1f, 1f, false, true), 0f)
        assertFalse(motion.completed)
        assertEquals(.5f, motion.update(120, 1f, 1f, false, true), .001f)
        assertFalse(motion.completed)
        assertEquals(1f, motion.update(240, 1f, 1f, false, true), 0f)
        assertTrue(motion.completed)
    }

    @Test fun staleHiddenAlphaUsesScrollAndNeverCompletesAnOpenStack() {
        val motion = NotificationReturnProgress(1f)
        assertEquals(0f, motion.update(100, 1f, 0f, true, false), 0f)
        assertEquals(.4f, motion.update(300, 1f, .4f, true, false), 0f)
        assertFalse(motion.completed)
        assertEquals(.4f, motion.update(320, .1f, .2f, true, false), 0f)
        assertFalse(motion.completed)
    }

    @Test fun slowNativeMotionStillOwnsTheWholeReturn() {
        val motion = NotificationReturnProgress(.2f)
        assertEquals(.5f, motion.update(500, .6f, .25f, true, false), .001f)
        assertFalse(motion.completed)
        assertEquals(1f, motion.update(900, 1f, 1f, false, true), 0f)
        assertTrue(motion.completed)
    }

    @Test fun missingCarriersFinishOnlyAfterTheNativeStackFolds() {
        val motion = NotificationReturnProgress(null)
        assertEquals(0f, motion.update(400, null, Float.NaN, false, false), 0f)
        assertFalse(motion.completed)
        assertEquals(1f, motion.update(500, null, null, false, true), 0f)
        assertTrue(motion.completed)
    }
}

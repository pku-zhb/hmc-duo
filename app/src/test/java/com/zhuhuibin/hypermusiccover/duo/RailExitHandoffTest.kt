package com.zhuhuibin.hypermusiccover.duo

import org.junit.Assert.*
import org.junit.Test

class RailExitHandoffTest {
    private class Card : RailExitHandoff.Target {
        var visible = true
        var top = 1800
        var transitionAlpha = 0f
        var mini = false
        val steps = mutableListOf<String>()
        var safeRestore = false
        override fun hideNative() { visible = false; steps += "hide" }
        override fun selectMini() { mini = true; steps += "mini" }
        override fun restorePose() {
            top = 1100
            transitionAlpha = 1f
            safeRestore = !visible
            steps += "restore"
        }
    }
    @Test fun successfulReturnHidesTheCardBeforeRestoringItsOnScreenSlotAndAlpha() {
        val card = Card()
        RailExitHandoff.complete(true, card)
        assertTrue(card.safeRestore)
        assertTrue(card.mini)
        assertFalse(card.visible)
        assertEquals(1100, card.top)
        assertEquals(1f, card.transitionAlpha)
        assertEquals(listOf("hide", "mini", "restore"), card.steps)
    }
    @Test fun cancellationRestoresTheCurrentPageWithoutForcingItIntoThePill() {
        val card = Card()
        RailExitHandoff.complete(false, card)
        assertTrue(card.visible)
        assertFalse(card.mini)
        assertEquals(listOf("restore"), card.steps)
    }
}

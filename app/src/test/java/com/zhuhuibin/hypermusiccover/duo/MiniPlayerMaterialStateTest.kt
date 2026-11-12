package com.zhuhuibin.hypermusiccover.duo

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MiniPlayerMaterialStateTest {
    @Test fun coldShortcutWaitsForItsBlurContainerWithoutConsumingTheRecipe() {
        val gate = MiniPlayerMaterialState.LayoutGate()
        // The element is shaped on pre-draw, before its newly inserted parent is laid out.
        assertFalse(gate.needsApply(0, true, 0, 0, 48, 48))
        assertTrue(gate.needsApply(0, true, 72, 72, 48, 48))
        gate.completed(0, true, 0)
        assertFalse(gate.needsApply(0, true, 72, 72, 48, 48))
    }

    @Test fun glassWaitsForBothAttachmentAndElementBounds() {
        val gate = MiniPlayerMaterialState.LayoutGate()
        assertFalse(gate.needsApply(4, false, 72, 72, 48, 48))
        assertFalse(gate.needsApply(4, true, 72, 72, 0, 0))
        assertTrue(gate.needsApply(4, true, 72, 72, 48, 48))
    }

    @Test fun movingGlassToItsNativeLayerReappliesTheSameRecipe() {
        val gate = MiniPlayerMaterialState.LayoutGate()
        gate.completed(4, true, 0)
        gate.detached()
        assertFalse(gate.needsApply(4, false, 72, 72, 48, 48))
        assertTrue(gate.needsApply(4, true, 72, 72, 48, 48))
    }

    @Test fun aNewRecipeRedressesButOrdinarySqueezeFramesDoNot() {
        val gate = MiniPlayerMaterialState.LayoutGate()
        gate.completed(4, true, 0)
        assertFalse(gate.needsApply(4, true, 72, 72, 46, 50))
        assertTrue(gate.needsApply(5, true, 72, 72, 46, 50))
    }

    @Test fun failedMaterialRetriesAfterBackoffWithoutClaimingNativeSuccess() {
        val gate = MiniPlayerMaterialState.LayoutGate()
        gate.completed(4, false, 1000)
        assertEquals(null, gate.appliedGeneration)
        assertEquals("fallback-retry", gate.status(4))
        assertFalse(gate.needsApply(4, true, 72, 72, 48, 48, 1149))
        assertEquals(1L, gate.retryDelay(4, 1149))
        assertTrue(gate.needsApply(4, true, 72, 72, 48, 48, 1150))
        gate.completed(4, true, 1150)
        assertEquals(4, gate.appliedGeneration)
        assertEquals("native", gate.status(4))
        assertEquals(null, gate.retryDelay(4, 1150))
        assertFalse(gate.needsApply(4, true, 72, 72, 48, 48, 5000))
    }

    @Test fun persistentFailureStopsAfterThreeAttemptsUntilRecipeOrAttachmentChanges() {
        val gate = MiniPlayerMaterialState.LayoutGate()
        for (now in listOf(1000L, 1150L, 1300L)) {
            assertTrue(gate.needsApply(4, true, 72, 72, 48, 48, now))
            gate.completed(4, false, now)
        }
        assertEquals(3, gate.attempts)
        assertEquals("fallback-final", gate.status(4))
        assertEquals(null, gate.retryDelay(4, 1300))
        assertFalse(gate.needsApply(4, true, 72, 72, 48, 48, 5000))
        assertTrue(gate.needsApply(5, true, 72, 72, 48, 48, 1300))
        gate.completed(5, false, 1300)
        assertEquals(1, gate.attempts)
        gate.detached()
        assertEquals("pending", gate.status(5))
        assertTrue(gate.needsApply(5, true, 72, 72, 48, 48, 1301))
    }

    @Test fun equalNativeArrayValuesHaveTheSameSignature() {
        val a = arrayOf<Any>(intArrayOf(1, 2), floatArrayOf(0.2f, 0.8f))
        val b = arrayOf<Any>(intArrayOf(1, 2), floatArrayOf(0.2f, 0.8f))
        assertEquals(MiniPlayerMaterialState.snapshot(a), MiniPlayerMaterialState.snapshot(b))
    }

    @Test fun nativeArrayReuseCannotMutateThePreviousSignature() {
        val values = floatArrayOf(0.2f, 0.8f)
        val previous = MiniPlayerMaterialState.snapshot(values)
        values[0] = 0.5f
        assertNotEquals(previous, MiniPlayerMaterialState.snapshot(values))
        assertEquals(listOf(0.2f, 0.8f), previous)
    }

    @Test fun systemStyleChangesKeepTheCustomBlurLayer() {
        val frosted = MiniPlayerMaterialState.styleKey("MediaViewBlurEffect", true, 1)
        val soft = MiniPlayerMaterialState.styleKey("MediaViewGlassEffect", true, 2)
        assertNotEquals(frosted, soft)
        assertFalse(MiniPlayerMaterialState.replacesLayer(frosted, soft))
        assertFalse(MiniPlayerMaterialState.replacesLayer(soft, frosted))
    }

    @Test fun disablingCustomBlurRestoresTheNativeMaterialFamily() {
        val custom = MiniPlayerMaterialState.styleKey("MediaViewGlassEffect", true, 1)
        val native = MiniPlayerMaterialState.styleKey("MediaViewGlassEffect", false, 2)
        assertTrue(MiniPlayerMaterialState.replacesLayer(custom, native))
        assertTrue(MiniPlayerMaterialState.replacesLayer(native, custom))
        assertTrue(MiniPlayerMaterialState.replacesLayer(native,
            MiniPlayerMaterialState.styleKey("MediaViewBlurEffect", false, 3)))
    }

    @Test fun onlyAnotherEffectReplacesTheLayer() {
        assertFalse(MiniPlayerMaterialState.replacesLayer(null, "Blur#1"))
        assertFalse(MiniPlayerMaterialState.replacesLayer("Blur#1", "Blur#2"))
        assertTrue(MiniPlayerMaterialState.replacesLayer("Blur#2", "Glass#3"))
    }
}

package com.zhuhuibin.hypermusiccover.duo

import org.junit.Assert.*
import org.junit.Test

class CommittedMaterialSignatureTest {
    @Test fun aDownstreamGlassReplacementChangesTheSignatureWithoutChangingTheOemRecipe() {
        val original = FloatArray(42).apply { this[34] = .1f }
        val final = original.copyOf().apply { this[34] = .7f }
        val args = arrayOf<Any?>(Any(), original)
        val signature = MiniPlayerMaterialState.committedSignatureArgs("setMiGlassCompat", args, 0, final, null)
        final[34] = .9f
        assertEquals(.7f, (signature[1] as List<*>)[34])
        assertEquals(.1f, original[34], 0f)
        assertSame(original, args[1])
        assertNull(signature[0])
    }

    @Test fun independentModeKeepsTheOemValuesAndUnknownCallsKeepTheirGeometry() {
        val original = FloatArray(42)
        val args = arrayOf<Any?>(Any(), original)
        assertEquals(original.toList(), MiniPlayerMaterialState.committedSignatureArgs(
            "setMiGlassCompat", args, 0, null, null)[1])
        assertEquals(listOf(null, 46f, 110f), MiniPlayerMaterialState.committedSignatureArgs(
            "setMiGlassSdfMaxSizeCompat", arrayOf(Any(), 46f, 110f), 0, FloatArray(42), intArrayOf(5, 8)))
    }

    @Test fun equalCommittedProfilesIgnoreDifferentPreAdjustmentArrays() {
        val a = FloatArray(42).apply { this[34] = .1f }
        val b = FloatArray(42).apply { this[34] = .2f }
        val final = FloatArray(42).apply { this[34] = .7f }
        assertEquals(
            MiniPlayerMaterialState.committedSignatureArgs("setMiGlassCompat", arrayOf(Any(), a), 0, final, null),
            MiniPlayerMaterialState.committedSignatureArgs("setMiGlassCompat", arrayOf(Any(), b), 0, final, null))
    }

    @Test fun blurSignatureUsesTheTwoCommittedRadiiButDoesNotGuessAnUnknownOverload() {
        assertEquals(listOf(null, 0, 90), MiniPlayerMaterialState.committedSignatureArgs(
            "setMiGlassBlurRadius", arrayOf(Any(), 10, 20), 0, null, intArrayOf(0, 90)))
        assertEquals(listOf(null, 10, 20, 30), MiniPlayerMaterialState.committedSignatureArgs(
            "setMiGlassBlurRadius", arrayOf(Any(), 10, 20, 30), 0, null, intArrayOf(0, 90)))
    }

    @Test fun backgroundSignaturesUseReadbackWhileLeavingTheirOemBaselineUnmodified() {
        val state = GlassProfile.Background.capture(48, .8f, intArrayOf(0x55667788), intArrayOf(101), 32, true)
        val colors = intArrayOf(0x11223344, 101)
        val args = arrayOf<Any?>(null, Any(), false, colors, false)
        val signature = MiniPlayerMaterialState.committedSignatureArgs("applyElementViewBlend", args, 1,
            null, null, state)
        assertEquals(listOf(0x55667788, 101), signature[3])
        assertArrayEquals(intArrayOf(0x11223344, 101), colors)
        assertEquals(listOf(null, 48), MiniPlayerMaterialState.committedSignatureArgs(
            "setMiBackgroundBlurRadiusCompat", arrayOf(Any(), 10), 0, null, null, state))
        assertEquals(listOf(null, .8f), MiniPlayerMaterialState.committedSignatureArgs(
            "setMiBackgroundBlurScaleRatioCompat", arrayOf(Any(), .4f), 0, null, null, state))
    }
}

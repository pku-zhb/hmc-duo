package com.zhuhuibin.hypermusiccover.duo

import org.junit.Assert.*
import org.junit.Test

class ColorOsPillAxisTest {
    private val screen = CoverMorphMotion.Box(1000f, 600f, 126.5f, 607.75f)
    private fun assertBox(a: CoverMorphMotion.Box, b: CoverMorphMotion.Box) {
        assertEquals(a.x, b.x, .001f); assertEquals(a.y, b.y, .001f)
        assertEquals(a.w, b.w, .001f); assertEquals(a.h, b.h, .001f)
    }

    @Test fun verticalSwitchStartsAndEndsInTheOriginalUprightCapsule() {
        val axis = ColorOsPillAxis(true, screen)
        for (next in listOf(false, true)) {
            val from = ColorOsPill.frame(axis.logical, 0f, next, 26f, 24.75f, 6.6f)
            val to = ColorOsPill.frame(axis.logical, 1f, next, 26f, 24.75f, 6.6f)
            assertBox(screen, axis.toScreen(from.old))
            assertBox(screen, axis.toScreen(to.new))
        }
    }

    @Test fun everyOriginalFramePreservesTheNarrowRailAndMovesAlongItsLongAxis() {
        val axis = ColorOsPillAxis(true, screen)
        for (next in listOf(false, true)) for (i in 0..1000) {
            val f = ColorOsPill.frame(axis.logical, i / 1000f, next, 26f, 24.75f, 6.6f)
            for (box in listOf(f.old, f.new)) {
                val drawn = axis.toScreen(box)
                assertEquals(box.h, drawn.w, .001f)
                assertEquals(box.w, drawn.h, .001f)
                assertEquals(screen.cx(), drawn.cx(), .001f)
                assertTrue(drawn.w <= screen.w + .001f)
                assertTrue(drawn.y >= screen.y - 25f)
                assertTrue(drawn.y + drawn.h <= screen.y + screen.h + 25f)
            }
        }
    }

    @Test fun upAndDownRemainMirrorImagesAtEveryProgress() {
        val axis = ColorOsPillAxis(true, screen)
        for (i in 0..100) {
            val up = axis.toScreen(ColorOsPill.frame(axis.logical, i / 100f, true, 26f, 24.75f, 6.6f).new)
            val down = axis.toScreen(ColorOsPill.frame(axis.logical, i / 100f, false, 26f, 24.75f, 6.6f).new)
            assertEquals(2f * screen.cy() - up.y - up.h, down.y, .001f)
            assertEquals(up.h, down.h, .001f)
        }
    }

    @Test fun horizontalColorOsGeometryIsUnchanged() {
        val horizontal = CoverMorphMotion.Box(200f, 1200f, 600f, 120f)
        val axis = ColorOsPillAxis(false, horizontal)
        assertBox(horizontal, axis.logical)
        for (i in 0..100) {
            val f = ColorOsPill.frame(horizontal, i / 100f, true, 26f, 25f, 7f)
            assertBox(f.new, axis.toScreen(f.new)); assertBox(f.old, axis.toScreen(f.old))
        }
    }
}

package com.zhuhuibin.hypermusiccover.duo

import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

class MiniPlayerExpansionPageTest {
    private fun config(raw: String) = JSONObject(MiniPlayerConfig.normalizedJson(raw))

    @Test fun everyReopenSharesTheSavedPortraitPageAcrossOuterAndInnerPortrait() {
        for (page in MiniPlayerConfig.VIEW_PLAYER..MiniPlayerConfig.VIEW_LYRICS) {
            val value = config("""{"startupMusicView":$page,"collapsedCategories":7,"landscapeMusicView":3,"landscapePageSchema":1}""")
            for (size in listOf(1168f to 1712f, 1672f to 2364f)) {
                val landscape = FoldArtworkLayout.innerLandscape(size.first, size.second, 2.75f)
                assertFalse(landscape)
                repeat(3) { assertEquals(page, MiniPlayerConfig.expandedMusicView(value, landscape)) }
            }
            assertEquals(MiniPlayerConfig.VIEW_ARTWORK_AND_LYRICS, MiniPlayerConfig.expandedMusicView(value, true))
        }
    }

    @Test fun landscapeRetainsAllFourIndependentPages() {
        for (page in MiniPlayerConfig.VIEW_PLAYER..MiniPlayerConfig.VIEW_ARTWORK_AND_LYRICS) {
            val value = config("""{"startupMusicView":1,"landscapeMusicView":$page,"landscapePageSchema":1}""")
            assertEquals(MiniPlayerConfig.VIEW_ARTWORK, MiniPlayerConfig.expandedMusicView(value, false))
            assertEquals(page, MiniPlayerConfig.expandedMusicView(value, true))
        }
    }

    @Test fun existingAndLegacyInitialChoicesRemainTheDefaultRatherThanBeingReset() {
        assertEquals(MiniPlayerConfig.VIEW_ARTWORK, MiniPlayerConfig.expandedMusicView(config("""{"startupMusicView":1}"""), false))
        assertEquals(MiniPlayerConfig.VIEW_PLAYER, MiniPlayerConfig.expandedMusicView(config("""{"startupMode":1}"""), false))
        assertEquals(MiniPlayerConfig.VIEW_LYRICS, MiniPlayerConfig.expandedMusicView(config("{}"), false))
    }

    @Test fun rememberedLockEntryDoesNotDisableTheExplicitManualReopenChoice() {
        val value = config("""{"startupMusicView":1,"keepLastLockscreen":true,"collapsedCategories":7}""")
        assertTrue(MiniPlayerConfig.keepLastLockscreen(value))
        assertEquals(MiniPlayerConfig.COLLAPSE_ALL, MiniPlayerConfig.collapsedCategories(value))
        assertEquals(MiniPlayerConfig.VIEW_ARTWORK, MiniPlayerConfig.expandedMusicView(value, false))
    }
}

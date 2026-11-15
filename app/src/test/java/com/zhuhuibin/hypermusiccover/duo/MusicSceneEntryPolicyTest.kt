package com.zhuhuibin.hypermusiccover.duo

import org.junit.Assert.*
import org.junit.Test

class MusicSceneEntryPolicyTest {
    @Test fun allFourPagesAndEightCategoryMasksResolveToOneCoherentEntry() {
        for (page in 0..3) for (collapsed in 0..7) {
            val ownsScene = page != MiniPlayerConfig.VIEW_PLAYER &&
                collapsed and MiniPlayerConfig.COLLAPSE_MUSIC == 0
            assertEquals("$page/$collapsed", ownsScene, MusicSceneEntryPolicy.ownsScene(collapsed, page))
            assertEquals("$page/$collapsed", if (ownsScene) collapsed or 6 else collapsed,
                MusicSceneEntryPolicy.collapsedCategories(collapsed, page))
        }
    }

    @Test fun rememberedArtworkLyricsAndCombinedPagesCannotRestoreAnExpandedLiveCardOrList() {
        for (page in 1..3) {
            val old = LockscreenSnapshot(0, page, setOf("timer", "delivery"), setOf("timer"), "delivery")
            val entry = LockscreenSnapshot.decode(old.encode())!!.exclusiveMusicScene()
            assertEquals(6, entry.collapsed)
            assertTrue(entry.collapseFocus("timer"))
            assertTrue(entry.collapseFocus("delivery"))
            assertTrue(entry.collapseFocus("late-calendar"))
            assertTrue(entry.focusExpanded.isEmpty())
            assertEquals(entry, entry.exclusiveMusicScene())
            assertEquals(entry, LockscreenSnapshot.decode(entry.encode())!!.exclusiveMusicScene())
        }
    }

    @Test fun nativePlayerAndCollapsedMusicPreserveIndependentNotificationChoices() {
        for (page in 0..3) for (mask in 0..7) {
            if (page != 0 && mask and 1 == 0) continue
            val selected = LockscreenSnapshot(mask, page, setOf("timer"), setOf("timer"))
            assertEquals(selected, selected.exclusiveMusicScene())
            assertFalse(selected.exclusiveMusicScene().collapseFocus("timer"))
        }
    }

    @Test fun anExplicitCardChoiceSupersedesInitialCategoriesWhoseViewsHaveNotArrived() {
        val entry = LockscreenEntryState()
        assertFalse(entry.takeMusic(false))
        entry.finishPresentation(false, listOf("timer"))
        assertFalse(entry.takeMusic(true))
        assertFalse(entry.takeNotifications(true))
        assertFalse(entry.takeFocus("timer"))
        assertFalse(entry.takeSelection(true, false))
        assertTrue(entry.takeFocus("late-calendar"))
        assertTrue(entry.takeMusic(true, true))
        entry.arm()
        assertTrue(entry.takeMusic(true))
        assertTrue(entry.takeNotifications(true))
        assertTrue(entry.takeFocus("timer"))
    }

    @Test fun aSelectedMusicSceneFoldsLateCategoriesRegardlessOfTheOlderStartupDefaults() {
        for (page in 0..3) for (mask in 0..7) {
            assertTrue(MusicSceneEntryPolicy.ownsScene(mask, page, true))
            assertEquals(6, MusicSceneEntryPolicy.collapsedCategories(mask, page, true))
        }
    }
}

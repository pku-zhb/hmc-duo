package com.zhuhuibin.hypermusiccover.duo

/** Saved and configured scene requests obey the same exclusivity as a native page button. */
internal object MusicSceneEntryPolicy {
    fun ownsScene(collapsed: Int, musicView: Int, selectedMusicScene: Boolean = false): Boolean =
        selectedMusicScene ||
            collapsed and MiniPlayerConfig.COLLAPSE_MUSIC == 0 && RailMusicPagePolicy.ownsScene(musicView)

    fun collapsedCategories(collapsed: Int, musicView: Int, selectedMusicScene: Boolean = false): Int =
        if (ownsScene(collapsed, musicView, selectedMusicScene))
            (collapsed and MiniPlayerConfig.COLLAPSE_MUSIC.inv()) or
                MiniPlayerConfig.COLLAPSE_LIVE or MiniPlayerConfig.COLLAPSE_NOTIFICATIONS
        else collapsed
}

package com.zhuhuibin.hypermusiccover.duo

/** Saved and configured scene requests obey the same exclusivity as a native page button. */
internal object MusicSceneEntryPolicy {
    fun ownsScene(collapsed: Int, musicView: Int): Boolean =
        collapsed and MiniPlayerConfig.COLLAPSE_MUSIC == 0 && RailMusicPagePolicy.ownsScene(musicView)

    fun collapsedCategories(collapsed: Int, musicView: Int): Int =
        if (ownsScene(collapsed, musicView))
            collapsed or MiniPlayerConfig.COLLAPSE_LIVE or MiniPlayerConfig.COLLAPSE_NOTIFICATIONS
        else collapsed
}

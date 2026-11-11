package com.zhuhuibin.hypermusiccover.duo

import org.json.JSONArray
import org.json.JSONObject

/** Presentation only; never stores notification text, artwork or playback position. */
internal data class LockscreenSnapshot(
    val collapsed: Int,
    val musicView: Int,
    val focusKnown: Set<String> = emptySet(),
    val focusExpanded: Set<String> = emptySet(),
    val selected: String? = null,
) {
    fun collapseFocus(key: String): Boolean = if (key in focusKnown) key !in focusExpanded
        else collapsed and MiniPlayerConfig.COLLAPSE_LIVE != 0

    fun encode(): String = JSONObject().put("collapsed", collapsed).put("musicView", musicView)
        .put("focusKnown", JSONArray(focusKnown.sorted())).put("focusExpanded", JSONArray(focusExpanded.sorted()))
        .put("selected", selected ?: JSONObject.NULL).toString()

    companion object {
        fun preference(landscape: Boolean) = if (landscape) "lastLockLandscape" else "lastLockPortrait"
        fun decode(raw: String?): LockscreenSnapshot? = runCatching {
            val json = JSONObject(raw ?: return null)
            fun keys(name: String): Set<String> = json.optJSONArray(name)?.let { a ->
                (0 until minOf(a.length(), 100)).mapNotNull { a.optString(it).takeIf(String::isNotEmpty) }.toSet()
            } ?: emptySet()
            val known = keys("focusKnown")
            LockscreenSnapshot(json.getInt("collapsed").coerceIn(0, MiniPlayerConfig.COLLAPSE_ALL),
                json.getInt("musicView").coerceIn(MiniPlayerConfig.VIEW_PLAYER, MiniPlayerConfig.VIEW_LYRICS),
                known, keys("focusExpanded").intersect(known),
                if (json.isNull("selected")) null else json.optString("selected").takeIf(String::isNotEmpty))
        }.getOrNull()
    }
}

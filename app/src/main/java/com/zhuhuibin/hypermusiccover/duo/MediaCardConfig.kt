package com.zhuhuibin.hypermusiccover.duo

import org.json.JSONObject

/** Two independent card profiles; zero-valued appearance choices retain the native style. */
object MediaCardConfig {
    /**
     * The expanded island is HyperOS's own, the default, or pure black. Every saved config holds this
     * key, so 0 has to be HyperOS's for it to reach them; dark (2) always meant black, and the 1 left
     * by the removed light theme and by Soft Glass, as this choice was once called, means HyperOS's.
     */
    const val ISLAND_THEME_DEFAULT = 0
    const val ISLAND_THEME_OLED = 2

    val scopes = listOf("notification", "island")
    val defaults = linkedMapOf("cover" to 0, "scrollText" to 0, "hideSource" to 0, "hideDevice" to 0,
        "background" to 0, "theme" to 0, "flow" to 0, "pauseRestore" to 1,
        "animate" to 1, "blur" to 8, "invert" to 1, "tone" to 1)

    fun limit(name: String, value: Int): Int = when (name) {
        "cover" -> value.coerceIn(0, 3)
        "background" -> value.coerceIn(0, 5)
        "theme" -> value.coerceIn(0, 2)
        "flow" -> value.coerceIn(0, 4)
        "blur" -> value.coerceIn(1, 20)
        else -> value.coerceIn(0, 1)
    }


    fun limitKey(key: String, value: Int): Int {
        val name = key.substringAfter('.')
        if (key.startsWith("island.")) return defaults.getValue(name)
        return limit(name, value)
    }

    fun parse(json: String?): Map<String, Int> {
        val obj = runCatching { JSONObject(json ?: "{}") }.getOrNull()
        return buildMap {
            for (scope in scopes) for ((name, default) in defaults) {
                val key = "$scope.$name"
                put(key, limitKey(key, obj?.optInt(key, default) ?: default))
            }
        }
    }

    fun encode(values: Map<String, Int>): String = JSONObject().also { out ->
        for (scope in scopes) for ((name, default) in defaults) {
            val key = "$scope.$name"
            out.put(key, limitKey(key, values[key] ?: default))
        }
    }.toString()

    fun normalized(json: String?): String = encode(parse(json))
    /** Native means no material ownership, not a request to apply any glass recipe. */
    fun followsNativeMaterial(values: Map<String, Int>, scope: String = "notification"): Boolean =
        listOf("background", "flow", "theme").all { values["$scope.$it"] == 0 }

    fun customBackground(style: Int, flow: Int): Boolean = style > 0 || flow in 1..3
    fun animate(playing: Boolean, pauseRestore: Boolean, shown: Boolean, screenOn: Boolean): Boolean =
        shown && screenOn && (playing || !pauseRestore)
}

internal object MediaCardLog {
    fun i(tag: String, message: String) = Xp.d("$tag $message")
    fun w(tag: String, message: String) = Xp.w("$tag $message")
}

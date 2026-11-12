// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 btm_m
package com.zhuhuibin.hypermusiccover.duo

import android.content.SharedPreferences
import org.json.JSONObject
import kotlin.math.roundToInt

/** Portable settings for the HyperChanger lockscreen mini player. */
object MiniPlayerConfig {
    const val ENABLED = "enabled"
    const val MEDIA_COLLAPSED_DEFAULT = "mediaCollapsedDefault"
    const val STARTUP_MODE = "startupMode"
    const val STARTUP_MUSIC_VIEW = "startupMusicView"
    const val LANDSCAPE_STARTUP_MODE = "landscapeStartupMode"
    const val LANDSCAPE_MUSIC_VIEW = "landscapeMusicView"
    const val COLLAPSED_CATEGORIES = "collapsedCategories"
    const val LANDSCAPE_COLLAPSED_CATEGORIES = "landscapeCollapsedCategories"
    const val KEEP_LAST_LOCKSCREEN = "keepLastLockscreen"
    const val SHOW_ALL_LOCKSCREEN_NOTIFICATIONS = "showAllLockscreenNotifications"
    const val RETAIN_LOCKSCREEN_NOTIFICATIONS = "retainLockscreenNotifications"
    const val COLLAPSE_MUSIC = 1
    const val COLLAPSE_LIVE = 2
    const val COLLAPSE_NOTIFICATIONS = 4
    const val COLLAPSE_ALL = 7
    const val START_COLLAPSED = 0
    const val START_EXPANDED = 1
    const val START_MUSIC_ONLY = 2
    const val VIEW_PLAYER = 0
    const val VIEW_ARTWORK = 1
    const val VIEW_LYRICS = 2

    @JvmStatic fun startupMode(config: JSONObject): Int = config.optInt(STARTUP_MODE, START_MUSIC_ONLY)
        .coerceIn(START_COLLAPSED, START_MUSIC_ONLY)
    @JvmStatic fun startupMusicView(config: JSONObject): Int = config.optInt(STARTUP_MUSIC_VIEW, VIEW_LYRICS)
        .coerceIn(VIEW_PLAYER, VIEW_LYRICS)
    @JvmStatic fun startupMode(config: JSONObject, innerLandscape: Boolean): Int =
        if (innerLandscape) config.optInt(LANDSCAPE_STARTUP_MODE, START_EXPANDED).coerceIn(START_COLLAPSED, START_MUSIC_ONLY)
        else startupMode(config)
    @JvmStatic fun startupMusicView(config: JSONObject, innerLandscape: Boolean): Int =
        if (innerLandscape) config.optInt(LANDSCAPE_MUSIC_VIEW, VIEW_LYRICS).coerceIn(VIEW_PLAYER, VIEW_LYRICS)
        else startupMusicView(config)
    @JvmStatic fun collapsedCategories(config: JSONObject, innerLandscape: Boolean = false): Int {
        val key = if (innerLandscape) LANDSCAPE_COLLAPSED_CATEGORIES else COLLAPSED_CATEGORIES
        val legacy = when (startupMode(config, innerLandscape)) {
            START_COLLAPSED -> COLLAPSE_ALL
            START_EXPANDED -> 0
            else -> COLLAPSE_LIVE or COLLAPSE_NOTIFICATIONS
        }
        return config.optInt(key, legacy).coerceIn(0, COLLAPSE_ALL)
    }
    @JvmStatic fun keepLastLockscreen(config: JSONObject): Boolean = config.optBoolean(KEEP_LAST_LOCKSCREEN, false)
    @JvmStatic fun automaticCoverAllowed(raw: String?): Boolean = automaticCoverAllowed(raw, false)
    @JvmStatic fun automaticCoverAllowed(raw: String?, innerLandscape: Boolean): Boolean {
        val config = JSONObject(normalizedJson(raw))
        return !config.getBoolean(ENABLED) || !keepLastLockscreen(config) &&
            collapsedCategories(config, innerLandscape) and COLLAPSE_MUSIC == 0 &&
            startupMusicView(config, innerLandscape) != VIEW_PLAYER
    }
    const val WIDTH = "widthDp"
    const val HEIGHT_RADIUS = "heightRadiusDp"
    const val ART_RADIUS = "artRadiusDp"
    const val BACKGROUND_BLUR = "backgroundBlur"
    const val BACKGROUND_BLUR_RADIUS = "backgroundBlurRadiusDp"
    const val BACKGROUND_BLUR_BRIGHTNESS = "backgroundBlurBrightness"

    @JvmStatic fun blurBrightness(value: Double): Float =
        if (value.isFinite()) value.toFloat().coerceIn(0f, 100f) else 80f

    /** Fade the dark tint into a light tint at high brightness without hiding the sampled blur. */
    @JvmStatic fun blurTint(value: Double): Int {
        val brightness = blurBrightness(value)
        val alpha = ((60f - brightness.coerceAtMost(80f) * 0.5f) * 2.55f).roundToInt()
        val light = ((brightness - 70f) / 30f).coerceIn(0f, 1f)
        val red = (31f + 217f * light).roundToInt()
        val green = (35f + 215f * light).roundToInt()
        val blue = (36f + 216f * light).roundToInt()
        return (alpha shl 24) or (red shl 16) or (green shl 8) or blue
    }

    @JvmStatic fun blurRadius(value: Double): Float =
        if (value.isFinite()) value.toFloat().coerceIn(0f, 80f) else 30f

    /** The row takes the room a switched-off torch or camera leaves (MiniPlayerRuntime.pillRest). */
    const val ADAPTIVE_WIDTH = "adaptiveWidth"

    /** Move lasting lock screen status beside the date while islands are enabled. */
    const val STATUS_AT_DATE = "statusAtDate"

    /**
     * The row's look: [STYLE_COLOROS], ColorOS 16/17's capsule - one island at a time, swiped
     * between with a liquid switch (ColorOsPill) - or [STYLE_HYPEROS], the super island's big
     * island with the next one as a small circle beside it.
     */
    const val PILL_STYLE = "pillStyle"
    const val STYLE_COLOROS = 0
    const val STYLE_HYPEROS = 1
    @JvmStatic fun colorOs(config: JSONObject): Boolean =
        config.optInt(PILL_STYLE, STYLE_COLOROS) == STYLE_COLOROS

    /** A notification island pulled up opens the list without the media player in it. */
    const val LIST_WITHOUT_PLAYER = "listWithoutPlayer"

    /** A notification island's long title and line scroll in the pill; off, they end in "…". */
    const val NOTE_TEXT_SCROLL = "noteTextScroll"

    /** ColorOS's torch, camera and capsule at their full size; the capsule gets shorter to fit. */
    const val COLOROS_FULL_SIZE = "colorOsFullSize"

    const val COMPACT_NOTIFICATIONS = "compactNotifications"
    const val NOTIFICATION_SIZE = "notificationSizePercent"
    const val NOTIFICATION_DROP = "notificationDropDp"

    @JvmStatic fun notificationSize(value: Double): Float =
        if (value.isFinite()) value.toFloat().coerceIn(70f, 100f) else 80f

    @JvmStatic fun notificationDrop(value: Double): Float =
        if (value.isFinite()) value.toFloat().coerceIn(0f, 32f) else 16f

    private val defaults = linkedMapOf<String, Any>(
        ENABLED to false,
        MEDIA_COLLAPSED_DEFAULT to false,
        STARTUP_MODE to START_MUSIC_ONLY,
        STARTUP_MUSIC_VIEW to VIEW_LYRICS,
        LANDSCAPE_STARTUP_MODE to START_EXPANDED,
        LANDSCAPE_MUSIC_VIEW to VIEW_LYRICS,
        COLLAPSED_CATEGORIES to (COLLAPSE_LIVE or COLLAPSE_NOTIFICATIONS),
        LANDSCAPE_COLLAPSED_CATEGORIES to 0,
        KEEP_LAST_LOCKSCREEN to false,
        SHOW_ALL_LOCKSCREEN_NOTIFICATIONS to false,
        RETAIN_LOCKSCREEN_NOTIFICATIONS to false,
        WIDTH to 221f,
        HEIGHT_RADIUS to 27f,
        ART_RADIUS to 12f,
        ADAPTIVE_WIDTH to false,
        STATUS_AT_DATE to false,
        PILL_STYLE to STYLE_COLOROS,
        LIST_WITHOUT_PLAYER to false,
        NOTE_TEXT_SCROLL to false,
        COLOROS_FULL_SIZE to false,
        COMPACT_NOTIFICATIONS to false,
        NOTIFICATION_SIZE to 80f,
        NOTIFICATION_DROP to 16f,
        BACKGROUND_BLUR to false,
        BACKGROUND_BLUR_RADIUS to 30f,
        BACKGROUND_BLUR_BRIGHTNESS to 80f,
    )

    /** Automatic entry requires an island to return to; explicit scene restoration bypasses it. */
    @JvmStatic fun mediaCollapsedByDefault(raw: String?): Boolean {
        val config = JSONObject(normalizedJson(raw))
        return config.getBoolean(ENABLED) && config.getBoolean(MEDIA_COLLAPSED_DEFAULT)
    }

    @JvmStatic fun defaultJson(): String = normalizedJson(null)

    // Keep retired keys readable by legacy internals, but never restore their active state.
    private val retiredSwitches = setOf(ADAPTIVE_WIDTH, LIST_WITHOUT_PLAYER,
        COMPACT_NOTIFICATIONS, BACKGROUND_BLUR, NOTE_TEXT_SCROLL, COLOROS_FULL_SIZE, STATUS_AT_DATE)

    /** Normalize current settings and make old imports safe for the dedicated Fold layout. */
    @JvmStatic fun normalizedJson(raw: String?): String {
        val input = runCatching { JSONObject(raw.orEmpty()) }.getOrDefault(JSONObject())
        val out = JSONObject()
        defaults.forEach { (key, fallback) ->
            out.put(key, if (key in retiredSwitches) {
                false
            } else if (key == ENABLED || key == MEDIA_COLLAPSED_DEFAULT ||
                key == KEEP_LAST_LOCKSCREEN || key == COLOROS_FULL_SIZE ||
                key == SHOW_ALL_LOCKSCREEN_NOTIFICATIONS || key == RETAIN_LOCKSCREEN_NOTIFICATIONS) {
                runCatching { input.getBoolean(key) }.getOrDefault(fallback)
            } else if (key == COLLAPSED_CATEGORIES || key == LANDSCAPE_COLLAPSED_CATEGORIES) {
                val landscape = key == LANDSCAPE_COLLAPSED_CATEGORIES
                val migrated = JSONObject(input.toString())
                if (!landscape && !input.has(STARTUP_MODE) && input.optBoolean(MEDIA_COLLAPSED_DEFAULT))
                    migrated.put(STARTUP_MODE, START_COLLAPSED)
                collapsedCategories(migrated, landscape)
            } else if (key == STARTUP_MODE) {
                if (input.has(STARTUP_MODE)) input.optInt(STARTUP_MODE, START_MUSIC_ONLY).coerceIn(START_COLLAPSED, START_MUSIC_ONLY)
                else if (input.optBoolean(MEDIA_COLLAPSED_DEFAULT, false)) START_COLLAPSED else START_MUSIC_ONLY
            } else if (key == LANDSCAPE_STARTUP_MODE) {
                input.optInt(key, START_EXPANDED).coerceIn(START_COLLAPSED, START_MUSIC_ONLY)
            } else if (key == LANDSCAPE_MUSIC_VIEW) {
                input.optInt(key, VIEW_LYRICS).coerceIn(VIEW_PLAYER, VIEW_LYRICS)
            } else if (key == STARTUP_MUSIC_VIEW) {
                if (!input.has(COLLAPSED_CATEGORIES) && startupMode(input) == START_EXPANDED) VIEW_PLAYER
                else input.optInt(STARTUP_MUSIC_VIEW, VIEW_LYRICS).coerceIn(VIEW_PLAYER, VIEW_LYRICS)
            } else if (key == PILL_STYLE) {
                input.optInt(key, STYLE_COLOROS).coerceIn(STYLE_COLOROS, STYLE_HYPEROS)
            } else if (key == NOTIFICATION_SIZE) {
                notificationSize(input.optDouble(key, 80.0))
            } else if (key == NOTIFICATION_DROP) {
                notificationDrop(input.optDouble(key, 16.0))
            } else if (key == BACKGROUND_BLUR_RADIUS) {
                blurRadius(input.optDouble(key, 30.0))
            } else if (key == BACKGROUND_BLUR_BRIGHTNESS) {
                blurBrightness(input.optDouble(key, 80.0))
            } else {
                fallback
            })
        }
        // The old key remains a migration stub; it cannot contradict the new entry selector.
        out.put(MEDIA_COLLAPSED_DEFAULT, collapsedCategories(out) and COLLAPSE_MUSIC != 0)
        return out.toString()
    }

    @JvmStatic fun fromPreferences(prefs: SharedPreferences): String =
        normalizedJson(prefs.getString("config", null))

    @JvmStatic fun apply(prefs: SharedPreferences, raw: String?): String {
        val normalized = normalizedJson(raw)
        prefs.edit().putString("config", normalized).apply()
        return normalized
    }

    /** The original height setting is a shortcut radius, so the visible pill uses its diameter. */
    @JvmStatic fun visibleHeightDp(raw: String?): Float =
        MiniPlayerGeometry.heightDp(JSONObject(normalizedJson(raw))
            .getDouble(HEIGHT_RADIUS).toFloat())
}

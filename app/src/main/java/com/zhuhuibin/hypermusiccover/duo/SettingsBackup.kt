package com.zhuhuibin.hypermusiccover.duo

import android.content.Context
import org.json.JSONObject

/**
 * Export and import of everything the user has set, which lives in two places.
 *
 * [AppSettings] owns how the app looks. The module's own parameters - where the cover sits, what
 * it does to the card and to the fingerprint, which of the lyric options are on - belong to the
 * hook inside SystemUI, and
 * [AppSettings] deliberately keeps no copy of them so there is only ever one truth. That is why
 * exporting has to ask the module for them and importing has to hand them back, rather than
 * reading and writing a local mirror.
 *
 * A file written before the module's values were included still imports: the keys are simply
 * absent and nothing is pushed. One written after the settings were trimmed imports too, minus
 * everything that is no longer settable - see the note in export.
 */
object SettingsBackup {

    private const val KEY_BACKDROP_SCHEMA = "backdropSchema"
    private const val KEY_HIDE_PLAYER_BACKGROUND = "hidePlayerBackground"
    private const val KEY_BIAS = "coverBias"
    private const val KEY_FULL_BLUR = "coverFullBlur"
    private const val KEY_COVER_STYLE = "coverStyle"
    /** The size as a fraction of the style's full clock; the same key the trimmed builds dropped. */
    private const val KEY_CLOCK_SIZE = "clockSizeFraction"
    private const val KEY_CARD_TITLE_TAP = "cardTitleTap"
    private const val KEY_COVER_FLIP = "coverFlip"
    private const val KEY_GLOW_DRIFT = "glowDrift"
    private const val KEY_MESH_BACKDROP = "meshBackdrop"
    private const val KEY_DEPTH_OPEN = "depthLimitRemoved"
    private const val KEY_AMAP_MAP = "amapNavigationMap"
    private const val KEY_RAIL_STATUS_Y = "railStatusOffsetYDp"
    private const val KEY_FORCE_COLON = "forceClockColon"
    private const val KEY_MEDIA_BAR_GLOW = "mediaBarGlow"
    private const val KEY_LYRICS_KEEP_ON = "lyricsKeepOn"
    private const val KEY_LYRICS_HDR = "lyricsHdr"
    private const val KEY_LYRICS_TRANS = "lyricsTranslation"
    private const val KEY_LYRICS_TRANSLATE_MODE = "lyricsTranslateMode"
    private const val KEY_LYRICS_TRANSLATE_PROVIDER = "lyricsTranslateProvider"
    private const val KEY_LYRICS_TRANSLATE_ENDPOINT = "lyricsTranslateEndpoint"
    private const val KEY_LYRICS_TRANSLATE_SOURCE = "lyricsTranslateSource"
    private const val KEY_LYRICS_TRANSLATE_TARGET = "lyricsTranslateTarget"
    private const val KEY_LYRICS_ALIVE_FX = "lyricsAliveEffects"
    private const val KEY_LYRICS_DEPTH_BLUR = "lyricsDepthBlur"
    private const val KEY_LYRICS_ROMA = "lyricsRomanisation"
    private const val KEY_LYRICS_GROUPS = "lyricsGentleFlow"
    private const val KEY_LYRICS_SEARCH = "lyricsSearchMode"
    private const val KEY_SPICY_LYRICS = "spicyLyricsEnabled"
    private val LYRIC_PROVIDER_KEYS = linkedMapOf(
        "lyricsProviderQq" to "qq",
        "lyricsProviderNetease" to "netease",
        "lyricsProviderKuwo" to "kuwo",
        "lyricsProviderKugou" to "kugou",
        "lyricsProviderLrcLib" to "lrclib",
        "lyricsProviderVariants" to "variants",
    )

    /** Export preferences only; provider and translation credentials stay on the device. */
    internal fun writeLyricProviders(json: JSONObject, module: ModuleBridge.State) {
        val values = listOf(module.providerQq, module.providerNetease, module.providerKuwo,
            module.providerKugou, module.providerLrcLib, module.providerVariants)
        LYRIC_PROVIDER_KEYS.keys.forEachIndexed { index, key -> json.put(key, values[index]) }
        json.put(KEY_SPICY_LYRICS, module.spicyLyricsEnabled)
    }

    /** Missing or malformed fields must not reset preferences when restoring older backups. */
    internal fun readLyricProviders(json: JSONObject): Map<String, Boolean> = buildMap {
        for ((key, extra) in LYRIC_PROVIDER_KEYS) {
            if (!json.has(key)) continue
            val value = runCatching { json.getBoolean(key) }.getOrNull() ?: continue
            put(extra, value)
        }
    }
    /** A missing module section must not reset a device's selected clock destination. */
    internal fun readArtworkClockDestination(json: JSONObject): Int? = when {
        json.has("artworkClockDestination") -> runCatching {
            ArtworkClockDestination.normalize(json.getInt("artworkClockDestination"))
        }.getOrNull()
        json.has("artworkClockRow") -> runCatching {
            ArtworkClockDestination.fromLegacy(json.getBoolean("artworkClockRow"))
        }.getOrNull()
        else -> null
    }

    internal fun readAmapNavigationMap(json: JSONObject): Boolean? = json.opt(KEY_AMAP_MAP) as? Boolean

    internal fun writeAmapNavigationMap(json: JSONObject, module: ModuleBridge.State) {
        json.put(KEY_AMAP_MAP, module.amapNavigationMap)
    }

    /** A partial/older backup cannot reset the current status position; reject malformed inputs. */
    internal fun readRailStatusOffsetY(json: JSONObject): Float? {
        val number = json.opt(KEY_RAIL_STATUS_Y) as? Number ?: return null
        val value = number.toDouble()
        return value.takeIf { it.isFinite() && it >= RailStatusLayout.OFFSET_Y_MIN_DP &&
            it <= RailStatusLayout.OFFSET_Y_MAX_DP }?.toFloat()
    }

    internal fun writeRailStatusOffsetY(json: JSONObject, module: ModuleBridge.State) {
        json.put(KEY_RAIL_STATUS_Y, RailStatusLayout.normalizeOffsetY(module.railStatusOffsetYDp).toDouble())
    }

    /** Older Fold backups stored inactive backdrop switches; importing them must stay native. */
    internal fun readBackdropSettings(json: JSONObject): Map<String, Boolean> {
        val keys = listOf(KEY_HIDE_PLAYER_BACKGROUND, KEY_GLOW_DRIFT, KEY_MESH_BACKDROP)
        if (keys.none(json::has)) return emptyMap()
        if (json.optInt(KEY_BACKDROP_SCHEMA, 0) < 1) return linkedMapOf(
            KEY_HIDE_PLAYER_BACKGROUND to true,
            KEY_GLOW_DRIFT to false,
            KEY_MESH_BACKDROP to false,
        )
        return buildMap {
            for (key in keys) {
                if (!json.has(key)) continue
                val value = runCatching { json.getBoolean(key) }.getOrNull() ?: continue
                put(key, value)
            }
        }
    }

    private const val KEY_LYRICS_ALIGN = "lyricsAlignment"
    private const val KEY_MINI = "lockscreenMiniPlayer"

    suspend fun export(context: Context): String {
        val json = JSONObject(AppSettings.load(context).toJson())
        // With the module not answering there are no real values to write down, and writing the
        // defaults would quietly turn an export into a reset for whoever imports it.
        val module = ModuleBridge.query(context)
        if (module.alive) {
            // The settings that are no longer settings are not written, and not read back on
            // import either: the square's size and place, the clock's offset, the glass
            // end, the spring, the two media card switches, the small AOD clock, the lyrics
            // switch and the five values the lyric band and its type were. Each is fixed where it
            // is used now, so a backup that carries one is a backup of something that can no
            // longer be set - see Main and LyricStyle. The keys are gone from this file rather
            // than kept for compatibility: importing a value nothing can hold is the one outcome
            // worse than not importing it.
            json.put(KEY_BIAS, module.bias.toDouble())
            json.put(KEY_FULL_BLUR, module.fullBlur)
            json.put(KEY_COVER_STYLE, module.coverStyle)
            json.put(KEY_BACKDROP_SCHEMA, 1)
            json.put(KEY_HIDE_PLAYER_BACKGROUND, module.hidePlayerBackground)
            json.put("foldMusicLayout", module.foldMusicLayout)
            json.put("rightRail", module.rightRail)
            writeRailStatusOffsetY(json, module)
            json.put("reflowLockContent", module.reflowLockContent)
            json.put("followNativeMaterial", module.followNativeMaterial)
            json.put("hideChargingHint", module.hideChargingHint)
            json.put("forceNotificationFloor", module.forceNotificationFloor)
            json.put("hideLockStatusBar", module.hideLockStatusBar)
            json.put("artworkClockRow", module.artworkClockDestination == ArtworkClockDestination.DATE_ROW)
            json.put("artworkClockDestination", module.artworkClockDestination)
            json.put("artworkClockBeforeDate", module.artworkClockBeforeDate)
            json.put("wallpaperDim", module.wallpaperDim)
            if (module.clockSize > 0f) json.put(KEY_CLOCK_SIZE, module.clockSize.toDouble())
            json.put(KEY_CARD_TITLE_TAP, module.mcTitleTap)
            json.put(KEY_COVER_FLIP, module.coverFlip)
            json.put(KEY_GLOW_DRIFT, module.glowDrift)
            json.put(KEY_MESH_BACKDROP, module.meshBackdrop)
            json.put(KEY_FORCE_COLON, module.forceColon)
            json.put(KEY_DEPTH_OPEN, module.depthOpen)
            writeAmapNavigationMap(json, module)
            json.put(KEY_MEDIA_BAR_GLOW, module.mediaBarGlow)
            json.put(KEY_LYRICS_KEEP_ON, module.lyricsKeepOn)
            json.put(KEY_LYRICS_HDR, module.lyricsHdr)
            json.put(KEY_LYRICS_TRANS, module.lyricsTrans)
            json.put(KEY_LYRICS_TRANSLATE_MODE, module.lyricOnlineMode)
            json.put(KEY_LYRICS_TRANSLATE_PROVIDER, module.lyricTranslateProvider)
            // Credentials deliberately stay on-device, never in exported JSON.
            json.put(KEY_LYRICS_TRANSLATE_ENDPOINT, module.lyricTranslateEndpoint)
            json.put(KEY_LYRICS_TRANSLATE_SOURCE, module.lyricTranslateSource)
            json.put(KEY_LYRICS_TRANSLATE_TARGET, module.lyricTranslateTarget)
            json.put(KEY_LYRICS_ALIVE_FX, module.lyricsAliveFx)
            json.put(KEY_LYRICS_DEPTH_BLUR, module.lyricsDepthBlur)
            json.put(KEY_LYRICS_ROMA, module.lyricsRoma)
            json.put(KEY_LYRICS_GROUPS, module.lyricsGroups)
            json.put(KEY_LYRICS_SEARCH, module.lyricsSearchMode)
            writeLyricProviders(json, module)
            json.put(KEY_LYRICS_ALIGN, module.lyricsAlign)
            json.put("mediaCardCustomization", JSONObject(MediaCardConfig.normalized(module.mediaCardConfig)))
            json.put(KEY_MINI, JSONObject(MiniPlayerConfig.normalizedJson(module.miniConfig)))
        }
        return json.toString(2)
    }

    /** Applies both halves. Returns the app settings so the caller can restart the UI with them. */
    suspend fun import(context: Context, json: String): AppSettings {
        val settings = AppSettings.importFromJson(context, json)
        try {
            val obj = JSONObject(json)
            if (obj.has(KEY_BIAS)) ModuleBridge.setBias(context, obj.getDouble(KEY_BIAS).toFloat())
            if (obj.has(KEY_FULL_BLUR)) ModuleBridge.setFullBlur(context, obj.getInt(KEY_FULL_BLUR))
            // Only the mode. The square's size, place and corners are fixed, so a file that still
            // carries coverCardFill, coverCardPos, coverCardCorner or the dp coverCardSizeDp from
            // before them is not read for any of it - the module holds them where they are.
            // Only when the file has one: a file without the module's half (exported while the
            // module did not answer, or from before it was included) used to reset it to FULL.
            if (obj.has("wallpaperDim")) ModuleBridge.setWallpaperDim(context, obj.getInt("wallpaperDim"))
            val backdrop = readBackdropSettings(obj)
            backdrop[KEY_HIDE_PLAYER_BACKGROUND]?.let { ModuleBridge.setHidePlayerBackground(context, it) }
            backdrop[KEY_GLOW_DRIFT]?.let { ModuleBridge.setGlowDrift(context, it) }
            backdrop[KEY_MESH_BACKDROP]?.let { ModuleBridge.setMeshBackdrop(context, it) }
            if (obj.has("foldMusicLayout")) {
                ModuleBridge.setFoldMusicLayout(context, obj.getBoolean("foldMusicLayout"))
            }
            for (key in listOf("rightRail", "reflowLockContent", "followNativeMaterial", "hideChargingHint",
                "forceNotificationFloor", "hideLockStatusBar", "artworkClockBeforeDate")) {
                if (obj.has(key)) ModuleBridge.setRailSetting(context, key, obj.getBoolean(key))
            }
            readRailStatusOffsetY(obj)?.let { ModuleBridge.setRailStatusOffsetY(context, it) }
            readArtworkClockDestination(obj)?.let { ModuleBridge.setArtworkClockDestination(context, it) }
            if (obj.has(KEY_COVER_STYLE)) {
                ModuleBridge.setCoverStyle(context, "mode",
                    if (obj.optInt("backdropSchema", 0) >= 1) obj.getInt(KEY_COVER_STYLE).toFloat() else 1f)
            }
            if (obj.has(KEY_CLOCK_SIZE)) {
                ModuleBridge.setClockSize(context, obj.getDouble(KEY_CLOCK_SIZE).toFloat())
            }
            if (obj.has(KEY_CARD_TITLE_TAP)) {
                ModuleBridge.setCardTitleTap(context, obj.getBoolean(KEY_CARD_TITLE_TAP))
            }
            if (obj.has(KEY_COVER_FLIP)) {
                ModuleBridge.setCoverFlip(context, obj.getBoolean(KEY_COVER_FLIP))
            }
            if (obj.has(KEY_FORCE_COLON)) {
                ModuleBridge.setForceColon(context, obj.getBoolean(KEY_FORCE_COLON))
            }
            if (obj.has(KEY_DEPTH_OPEN)) {
                ModuleBridge.setDepthOpen(context, obj.getBoolean(KEY_DEPTH_OPEN))
            }
            readAmapNavigationMap(obj)?.let { ModuleBridge.setAmapNavigationMap(context, it) }
            if (obj.has(KEY_MEDIA_BAR_GLOW)) {
                ModuleBridge.setMediaBarGlow(context, obj.getBoolean(KEY_MEDIA_BAR_GLOW))
            }
            if (obj.has(KEY_LYRICS_HDR)) {
                ModuleBridge.setLyricsHdr(context, obj.getBoolean(KEY_LYRICS_HDR))
            }
            if (obj.has(KEY_LYRICS_KEEP_ON)) {
                ModuleBridge.setLyricsKeepOn(context, obj.getBoolean(KEY_LYRICS_KEEP_ON))
            }
            if (obj.has(KEY_LYRICS_TRANS)) {
                ModuleBridge.setLyricsTrans(context, obj.getBoolean(KEY_LYRICS_TRANS))
            }
            if (obj.has(KEY_LYRICS_TRANSLATE_MODE)
                || obj.has(KEY_LYRICS_TRANSLATE_PROVIDER)
                || obj.has(KEY_LYRICS_TRANSLATE_ENDPOINT)
                || obj.has(KEY_LYRICS_TRANSLATE_SOURCE)
                || obj.has(KEY_LYRICS_TRANSLATE_TARGET)
            ) {
                val now = ModuleBridge.query(context)
                val provider = TranslationProvider.normalize(
                    obj.optString(KEY_LYRICS_TRANSLATE_PROVIDER,
                        if (obj.has(KEY_LYRICS_TRANSLATE_ENDPOINT)) TranslationProvider.CUSTOM
                        else now.lyricTranslateProvider)
                )
                val endpoint = obj.optString(KEY_LYRICS_TRANSLATE_ENDPOINT, now.lyricTranslateEndpoint)
                // Never hand an existing credential to a different backend or custom host.
                val keepKey = provider == now.lyricTranslateProvider &&
                    (provider != TranslationProvider.CUSTOM || endpoint == now.lyricTranslateEndpoint)
                ModuleBridge.setLyricTranslateConfig(
                    context,
                    endpoint,
                    if (keepKey) now.lyricTranslateApiKey else "",
                    obj.optString(KEY_LYRICS_TRANSLATE_SOURCE, now.lyricTranslateSource),
                    obj.optString(KEY_LYRICS_TRANSLATE_TARGET, now.lyricTranslateTarget),
                    obj.optInt(KEY_LYRICS_TRANSLATE_MODE, now.lyricOnlineMode),
                    provider,
                )
            }
            if (obj.has(KEY_LYRICS_ROMA)) {
                ModuleBridge.setLyricsRoma(context, obj.getBoolean(KEY_LYRICS_ROMA))
            }
            if (obj.has(KEY_LYRICS_GROUPS)) {
                ModuleBridge.setLyricsGroups(context, obj.getBoolean(KEY_LYRICS_GROUPS))
            }
            val providers = readLyricProviders(obj)
            if (providers.isNotEmpty()) ModuleBridge.setLyricProviders(context, providers)
            if (obj.has(KEY_SPICY_LYRICS)) {
                runCatching { obj.getBoolean(KEY_SPICY_LYRICS) }.getOrNull()?.let {
                    ModuleBridge.setSpicyLyrics(context, it)
                }
            }
            if (obj.has(KEY_LYRICS_SEARCH)) {
                ModuleBridge.setLyricsSearchMode(context, obj.optInt(KEY_LYRICS_SEARCH, 0))
            }
            if (obj.has(KEY_LYRICS_ALIGN)) {
                ModuleBridge.setLyricsAlign(context, obj.getInt(KEY_LYRICS_ALIGN))
            }
            if (obj.has(KEY_LYRICS_DEPTH_BLUR)) {
                ModuleBridge.setLyricsDepthBlur(context, obj.getInt(KEY_LYRICS_DEPTH_BLUR))
            }
            if (obj.has(KEY_LYRICS_ALIVE_FX)) {
                ModuleBridge.setLyricsAliveFx(context, obj.getInt(KEY_LYRICS_ALIVE_FX))
            }
            if (obj.has("mediaCardCustomization")) {
                ModuleBridge.setMediaCardConfig(context, MediaCardConfig.normalized(obj.getJSONObject("mediaCardCustomization").toString()))
            }
            if (obj.has(KEY_MINI)) {
                ModuleBridge.setMiniConfig(context, MiniPlayerConfig.normalizedJson(obj.getJSONObject(KEY_MINI).toString()))
            }
        } catch (_: Exception) {
            // The app half is already applied; a malformed module half is not worth losing it over.
        }
        return settings
    }
}

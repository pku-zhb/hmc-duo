// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 btm_m
package com.zhuhuibin.hypermusiccover.duo

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.zhuhuibin.hypermusiccover.duo.ui.theme.AppTheme
import com.zhuhuibin.hypermusiccover.duo.ui.util.SettingsNote
import com.zhuhuibin.hypermusiccover.duo.ui.util.PageScaffold
import org.json.JSONObject
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.preference.CheckboxPreference
import top.yukonga.miuix.kmp.preference.SwitchPreference
import top.yukonga.miuix.kmp.preference.WindowDropdownPreference
import top.yukonga.miuix.kmp.theme.ColorSchemeMode

class MiniPlayerActivity : ComponentActivity() {
    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LocaleHelper.wrapContext(newBase, LocaleHelper.getSavedLanguage(newBase)))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val settings = AppSettings.load(this)
        val theme = runCatching { ColorSchemeMode.valueOf(settings.themeMode) }
            .getOrDefault(ColorSchemeMode.System)
        setContent {
            AppTheme(themeMode = theme) { MiniPlayerPage(settings.isBlurEnabled, resumes, ::finish) }
        }
    }

    // See CoverActivity: asked again each time the screen comes back to the front.
    private var resumes by mutableIntStateOf(0)
    private var resumedOnce = false

    override fun onResume() {
        super.onResume()
        if (resumedOnce) resumes++ else resumedOnce = true
    }
}

@Composable
private fun MiniPlayerPage(blur: Boolean, refreshKey: Int, onBack: () -> Unit) {
    val context = LocalContext.current
    var configText by remember { mutableStateOf(MiniPlayerConfig.defaultJson()) }
    var alive by remember { mutableStateOf(false) }
    var followNativeMaterial by remember { mutableStateOf(false) }
    val config = remember(configText) { JSONObject(configText) }
    // A setting the module did not take means it went away: grey the page until it answers again.
    val lost by ModuleBridge.lost.collectAsState()
    LaunchedEffect(lost) { if (lost > 0) alive = false }
    LaunchedEffect(refreshKey, lost) {
        val reply = ModuleBridge.queryAlive(context)
        alive = reply.alive
        followNativeMaterial = reply.followNativeMaterial
        if (reply.alive) configText = reply.miniConfig
    }
    fun push(key: String, value: Any) {
        configText = MiniPlayerConfig.normalizedJson(JSONObject(configText).put(key, value).toString())
        ModuleBridge.setMiniConfig(context, configText)
    }

    val enabled = config.optBoolean(MiniPlayerConfig.ENABLED)
    PageScaffold(title = stringResource(R.string.mini_page_title), isBlurEnabled = blur,
        onBack = onBack) {
        item {
            Column {
                Card(Modifier.padding(horizontal = 12.dp, vertical = 12.dp)) {
                    SwitchPreference(title = stringResource(R.string.mini_enabled),
                        summary = if (alive) null else stringResource(R.string.mini_enabled_waiting),
                        checked = enabled, enabled = alive,
                        onCheckedChange = { push(MiniPlayerConfig.ENABLED, it) })
                }
                if (enabled) {
                    Card(Modifier.padding(horizontal = 12.dp).padding(bottom = 12.dp)) {
                        IslandDemo(MiniPlayerConfig.colorOs(config))
                    }
                    SmallTitle(text = stringResource(R.string.mini_general_section))
                    Card(Modifier.padding(horizontal = 12.dp)) {
                        WindowDropdownPreference(title = stringResource(R.string.mini_style),
                            summary = stringResource(R.string.mini_style_summary),
                            items = listOf(stringResource(R.string.mini_style_coloros),
                                stringResource(R.string.mini_style_hyperos)),
                            selectedIndex = config.optInt(MiniPlayerConfig.PILL_STYLE,
                                MiniPlayerConfig.STYLE_COLOROS).coerceIn(0, 1),
                            enabled = alive,
                            onSelectedIndexChange = { push(MiniPlayerConfig.PILL_STYLE, it) })
                    }
                    SettingsNote(stringResource(R.string.duo_vertical_text_summary),
                        Modifier.padding(horizontal = 12.dp).padding(bottom = 4.dp))

                    LockScreenEntryControls(config, alive, ::push)

                    SmallTitle(text = stringResource(R.string.duo_notification_section))
                    Card(Modifier.padding(horizontal = 12.dp)) {
                        SwitchPreference(title = stringResource(R.string.duo_notification_show_all),
                            summary = stringResource(R.string.duo_notification_show_all_summary),
                            checked = config.optBoolean(MiniPlayerConfig.SHOW_ALL_LOCKSCREEN_NOTIFICATIONS),
                            enabled = alive,
                            onCheckedChange = { push(MiniPlayerConfig.SHOW_ALL_LOCKSCREEN_NOTIFICATIONS, it) })
                        SwitchPreference(title = stringResource(R.string.duo_notification_retain),
                            summary = stringResource(R.string.duo_notification_retain_summary),
                            checked = config.optBoolean(MiniPlayerConfig.RETAIN_LOCKSCREEN_NOTIFICATIONS),
                            enabled = alive,
                            onCheckedChange = { push(MiniPlayerConfig.RETAIN_LOCKSCREEN_NOTIFICATIONS, it) })
                    }
                    SettingsNote(stringResource(R.string.duo_notification_other_module),
                        Modifier.padding(horizontal = 12.dp).padding(bottom = 4.dp))
                    SmallTitle(text = stringResource(R.string.fold_material_section))
                    Card(Modifier.padding(horizontal = 12.dp).padding(bottom = 12.dp)) {
                        WindowDropdownPreference(title = stringResource(R.string.fold_material),
                            summary = stringResource(R.string.fold_material_summary),
                            items = listOf(stringResource(R.string.fold_material_native),
                                stringResource(R.string.fold_material_follow)),
                            selectedIndex = if (followNativeMaterial) 1 else 0,
                            enabled = alive,
                            onSelectedIndexChange = {
                                followNativeMaterial = it == 1
                                ModuleBridge.setRailSetting(context, "followNativeMaterial", it == 1)
                            })
                    }
                }
            }
        }
    }
}

@Composable
private fun LockScreenEntryControls(config: JSONObject, enabled: Boolean, push: (String, Any) -> Unit) {
    val keepLast = MiniPlayerConfig.keepLastLockscreen(config)
    SmallTitle(text = stringResource(R.string.fold_startup_mode))
    Card(Modifier.padding(horizontal = 12.dp).padding(bottom = 12.dp)) {
        SwitchPreference(title = stringResource(R.string.fold_keep_last),
            summary = stringResource(R.string.fold_keep_last_summary),
            checked = keepLast, enabled = enabled,
            onCheckedChange = { push(MiniPlayerConfig.KEEP_LAST_LOCKSCREEN, it) })
    }
    if (!keepLast) for (landscape in listOf(false, true)) {
        val mask = MiniPlayerConfig.collapsedCategories(config, landscape)
        SmallTitle(text = stringResource(if (landscape) R.string.fold_landscape_collapsed
            else R.string.fold_collapsed_categories))
        Card(Modifier.padding(horizontal = 12.dp).padding(bottom = 12.dp)) {
            SettingsNote(stringResource(R.string.fold_collapsed_categories_summary))
            val categories = listOf(MiniPlayerConfig.COLLAPSE_MUSIC to R.string.fold_music_player,
                MiniPlayerConfig.COLLAPSE_LIVE to R.string.fold_live_cards,
                MiniPlayerConfig.COLLAPSE_NOTIFICATIONS to R.string.fold_notifications)
            for ((bit, label) in categories) CheckboxPreference(title = stringResource(label),
                checked = mask and bit != 0, enabled = enabled,
                onCheckedChange = { checked ->
                    push(if (landscape) MiniPlayerConfig.LANDSCAPE_COLLAPSED_CATEGORIES else MiniPlayerConfig.COLLAPSED_CATEGORIES,
                        if (checked) mask or bit else mask and bit.inv())
                })
            if (mask and MiniPlayerConfig.COLLAPSE_MUSIC == 0) WindowDropdownPreference(
                title = stringResource(if (landscape) R.string.fold_landscape_music_view else R.string.fold_startup_music_view),
                summary = stringResource(R.string.fold_startup_music_view_summary),
                items = listOf(stringResource(R.string.fold_music_player), stringResource(R.string.fold_music_artwork),
                    stringResource(R.string.fold_music_lyrics)) +
                    if (landscape) listOf(stringResource(R.string.fold_music_artwork_and_lyrics)) else emptyList(),
                selectedIndex = MiniPlayerConfig.startupMusicView(config, landscape), enabled = enabled,
                onSelectedIndexChange = { push(if (landscape) MiniPlayerConfig.LANDSCAPE_MUSIC_VIEW else MiniPlayerConfig.STARTUP_MUSIC_VIEW, it) })
        }
    }
}

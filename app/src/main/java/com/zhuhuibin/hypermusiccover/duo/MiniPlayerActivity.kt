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
import com.zhuhuibin.hypermusiccover.duo.ui.util.PageScaffold
import org.json.JSONObject
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.SmallTitle
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

    PageScaffold(title = stringResource(R.string.mini_page_title), isBlurEnabled = blur,
        onBack = onBack) {
        item {
            // What the row does, drawn with the module's own island geometry, before the switches.
            Card(Modifier.padding(horizontal = 12.dp).padding(top = 12.dp)) {
                IslandDemo(MiniPlayerConfig.colorOs(config))
            }
        }
        item {
            // Only controls supported by the dedicated Fold layout are exposed.
            Column {
                SmallTitle(text = stringResource(R.string.mini_general_section))
                Card(Modifier.padding(horizontal = 12.dp).padding(bottom = 12.dp)) {
                    // The one line the switch still carries, and only while the module is away:
                    // a switch that cannot be moved and does not say why reads as the switch
                    // being broken. Nothing here describes the feature itself any more.
                    SwitchPreference(title = stringResource(R.string.mini_enabled),
                        summary = if (alive) null
                                  else stringResource(R.string.mini_enabled_waiting),
                        checked = config.optBoolean(MiniPlayerConfig.ENABLED), enabled = alive,
                        onCheckedChange = { push(MiniPlayerConfig.ENABLED, it) })
                    // ColorOS first: it is the default, and the order is the stored value.
                    WindowDropdownPreference(title = stringResource(R.string.mini_style),
                        summary = stringResource(R.string.mini_style_summary),
                        items = listOf(stringResource(R.string.mini_style_coloros),
                            stringResource(R.string.mini_style_hyperos)),
                        selectedIndex = config.optInt(MiniPlayerConfig.PILL_STYLE,
                            MiniPlayerConfig.STYLE_COLOROS).coerceIn(0, 1),
                        enabled = alive && config.optBoolean(MiniPlayerConfig.ENABLED),
                        onSelectedIndexChange = { push(MiniPlayerConfig.PILL_STYLE, it) })
                    top.yukonga.miuix.kmp.basic.BasicComponent(
                        title = stringResource(R.string.duo_vertical_text),
                        summary = stringResource(R.string.duo_vertical_text_summary))

                }
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

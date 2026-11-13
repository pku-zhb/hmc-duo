package com.zhuhuibin.hypermusiccover.duo.ui.util

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

/** Non-interactive guidance must not resemble a preference row. */
@Composable
internal fun SettingsNote(text: String, modifier: Modifier = Modifier) {
    Text(text = text, fontSize = 13.sp, color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
        modifier = modifier.padding(horizontal = 16.dp, vertical = 8.dp))
}

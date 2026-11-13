package com.zhuhuibin.hypermusiccover.duo.ui.screen.features

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.zhuhuibin.hypermusiccover.duo.ModuleBridge
import com.zhuhuibin.hypermusiccover.duo.R
import top.yukonga.miuix.kmp.basic.BasicComponent
import top.yukonga.miuix.kmp.basic.Slider
import top.yukonga.miuix.kmp.basic.SliderDefaults
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.window.WindowDialog
import kotlin.math.roundToInt

/** Fine alignment stays near zero; numeric input preserves the full supported import range. */
@Composable
internal fun RailStatusOffsetSetting(enabled: Boolean, value: Float, onChange: (Float) -> Unit) {
    var showInput by rememberSaveable { mutableStateOf(false) }
    var input by rememberSaveable { mutableStateOf("") }
    var invalid by remember { mutableStateOf(false) }
    val label = value.toString().removeSuffix(".0")
    Column {
        BasicComponent(
            title = stringResource(R.string.rail_status_offset_y),
            summary = stringResource(R.string.rail_status_offset_y_summary),
            enabled = enabled,
            endActions = {
                TextButton(text = "$label dp", enabled = enabled, minWidth = 44.dp, minHeight = 44.dp,
                    onClick = { input = label; invalid = false; showInput = true })
            },
        )
        Slider(
            value = value.coerceIn(-24f, 24f), valueRange = -24f..24f, enabled = enabled,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            onValueChange = { onChange(it.roundToInt().toFloat()) },
            hapticEffect = SliderDefaults.SliderHapticEffect.Step,
            keyPoints = listOf(0f), showKeyPoints = true, magnetThreshold = 0f,
        )
        TextButton(text = stringResource(R.string.rail_status_offset_y_reset), enabled = enabled,
            modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, bottom = 12.dp),
            minHeight = 44.dp, onClick = { onChange(0f) })
    }
    WindowDialog(show = showInput, title = stringResource(R.string.rail_status_offset_y),
        summary = stringResource(R.string.rail_status_offset_y_range), onDismissRequest = { showInput = false }) {
        Column {
            TextField(value = input, onValueChange = { input = it; invalid = false },
                label = stringResource(R.string.rail_status_offset_y_input), singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text))
            if (invalid) Text(stringResource(R.string.rail_status_offset_y_invalid))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                TextButton(stringResource(R.string.lyrics_online_cancel), { showInput = false },
                    Modifier.weight(1f), minHeight = 44.dp)
                TextButton(stringResource(R.string.lyrics_online_save), {
                    val next = input.toFloatOrNull()
                    if (next == null || !next.isFinite() || next < ModuleBridge.railStatusOffsetYMinDp ||
                        next > ModuleBridge.railStatusOffsetYMaxDp) invalid = true
                    else { onChange(next); showInput = false }
                }, Modifier.weight(1f), enabled = enabled, minHeight = 44.dp)
            }
        }
    }
}

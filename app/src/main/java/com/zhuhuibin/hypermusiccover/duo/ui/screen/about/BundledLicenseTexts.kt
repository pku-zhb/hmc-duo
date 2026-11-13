package com.zhuhuibin.hypermusiccover.duo.ui.screen.about

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.zhuhuibin.hypermusiccover.duo.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.preference.WindowDropdownPreference

/** Original notices and licenses are readable offline without leaving the application. */
@Composable
internal fun BundledLicenseTexts(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var selected by rememberSaveable { mutableIntStateOf(0) }
    val files = listOf("NOTICE", "AGPL-3.0.txt", "GPL-3.0.txt", "Apache-2.0.txt")
    val labels = listOf(stringResource(R.string.license_notices), "AGPL-3.0", "GPL-3.0", "Apache-2.0")
    var text by remember { mutableStateOf<String?>(null) }
    var failed by remember { mutableStateOf(false) }
    LaunchedEffect(context, selected) {
        text = null; failed = false
        val result = withContext(Dispatchers.IO) {
            runCatching { context.assets.open("licenses/${files[selected]}").bufferedReader().use { it.readText() } }
        }
        text = result.getOrNull(); failed = result.isFailure
    }
    Card(modifier) {
        Column {
            WindowDropdownPreference(title = stringResource(R.string.license_bundled_title),
                items = labels, selectedIndex = selected, onSelectedIndexChange = { selected = it })
            Text(text ?: stringResource(if (failed) R.string.license_load_failed else R.string.license_loading), Modifier.padding(16.dp))
        }
    }
}

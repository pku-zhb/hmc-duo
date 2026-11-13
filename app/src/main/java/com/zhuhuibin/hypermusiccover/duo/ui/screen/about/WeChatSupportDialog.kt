package com.zhuhuibin.hypermusiccover.duo.ui.screen.about

import android.content.ContentValues
import android.content.Context
import android.graphics.BitmapFactory
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.zhuhuibin.hypermusiccover.duo.R
import java.io.IOException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.window.WindowDialog

/** The supplied asset is displayed proportionally and exported without bitmap re-encoding. */
@Composable
internal fun WeChatSupportDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val resources = LocalResources.current
    val scope = rememberCoroutineScope()
    var saving by remember { mutableStateOf(false) }
    val image = remember(resources) {
        resources.openRawResource(R.raw.wechat_support).use { input ->
            requireNotNull(BitmapFactory.decodeStream(input)).asImageBitmap()
        }
    }
    val windowInfo = LocalWindowInfo.current
    val density = LocalDensity.current
    val maxImageHeight = with(density) {
        (windowInfo.containerSize.height.toDp() * .42f).coerceIn(120.dp, 360.dp)
    }
    WindowDialog(show = true, title = stringResource(R.string.support_wechat_title),
        summary = stringResource(R.string.support_wechat_hint), onDismissRequest = onDismiss) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Image(bitmap = image, contentDescription = stringResource(R.string.support_wechat_title),
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxWidth().heightIn(max = maxImageHeight))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                TextButton(stringResource(R.string.update_close), onDismiss,
                    Modifier.weight(1f), minHeight = 44.dp)
                TextButton(stringResource(if (saving) R.string.support_image_saving else R.string.support_save_image),
                    onClick = {
                        saving = true
                        scope.launch {
                            val success = try {
                                withContext(Dispatchers.IO) { saveSupportImage(context.applicationContext) }
                                true
                            } catch (cancelled: CancellationException) {
                                throw cancelled
                            } catch (_: Exception) { false }
                            saving = false
                            Toast.makeText(context, if (success) R.string.support_image_saved
                                else R.string.support_image_save_failed, Toast.LENGTH_SHORT).show()
                        }
                    }, modifier = Modifier.weight(1f), enabled = !saving, minHeight = 44.dp)
            }
        }
    }
}

/** Android 10+ permits an App to create its own gallery image without broad storage access. */
private fun saveSupportImage(context: Context) {
    val resolver = context.contentResolver
    val values = ContentValues().apply {
        put(MediaStore.Images.Media.DISPLAY_NAME, "hmc-duo-wechat-support.png")
        put(MediaStore.Images.Media.MIME_TYPE, "image/png")
        put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/hmc-duo")
        put(MediaStore.Images.Media.IS_PENDING, 1)
    }
    val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
        ?: throw IOException("Unable to create support image")
    try {
        val output = resolver.openOutputStream(uri) ?: throw IOException("Unable to open support image")
        output.use { out -> context.resources.openRawResource(R.raw.wechat_support).use { it.copyTo(out) } }
        if (resolver.update(uri, ContentValues().apply { put(MediaStore.Images.Media.IS_PENDING, 0) }, null, null) != 1)
            throw IOException("Unable to publish support image")
    } catch (error: Exception) {
        runCatching { resolver.delete(uri, null, null) }
        throw error
    }
}

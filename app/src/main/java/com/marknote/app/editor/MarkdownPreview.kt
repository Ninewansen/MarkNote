package com.marknote.app.editor

import android.text.method.LinkMovementMethod
import android.text.SpannableString
import android.text.Spanned
import android.view.View
import android.widget.TextView
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.io.File

/**
 * 实时 Markdown 预览（native Spannable 渲染，无 WebView）。
 * 渲染放到后台线程，内容变化后旧内容保持显示，避免闪烁。
 */
@Composable
fun MarkdownPreview(
    markdown: String,
    dark: Boolean,
    baseDir: File?,
    modifier: Modifier = Modifier,
    onViewCreated: (View) -> Unit = {},
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val markwon = remember(dark) { MarkdownRenderer.get(context.applicationContext, dark) }

    var rendered by remember(markdown, dark, baseDir) { mutableStateOf<Spanned?>(null) }

    LaunchedEffect(markdown, dark, baseDir) {
        // 输入防抖：快速打字时取消未开始的渲染
        delay(120)
        rendered = withContext(Dispatchers.Default) {
            MarkdownRenderer.render(markwon, markdown, baseDir)
        }
    }

    val colors = MaterialTheme.colorScheme
    val horizontalPadding = with(density) { 16.dp.toPx() }
    val bottomPadding = with(density) { 64.dp.toPx() }

    Column(
        modifier = modifier.verticalScroll(rememberScrollState()),
    ) {
        AndroidView(
            modifier = Modifier.fillMaxWidth(),
            factory = { ctx ->
                TextView(ctx).apply {
                    movementMethod = LinkMovementMethod.getInstance()
                    setPadding(
                        horizontalPadding.toInt(),
                        (12 * density.density).toInt(),
                        horizontalPadding.toInt(),
                        bottomPadding.toInt(),
                    )
                    textSize = 15f
                    setLineSpacing(0f, 1.45f)
                    setTextColor(colors.onSurface.toArgb())
                    setLinkTextColor(colors.primary.toArgb())
                    onViewCreated(this)
                }
            },
            update = { textView ->
                textView.setTextColor(colors.onSurface.toArgb())
                textView.setLinkTextColor(colors.primary.toArgb())
                // 使用 Markwon 官方入口 setParsedMarkdown：内部会调用插件的
                // beforeSetText/afterSetText 钩子，自动调度 AsyncDrawable 图片加载。
                markwon.setParsedMarkdown(textView, rendered ?: SpannableString(""))
            },
        )
    }
}

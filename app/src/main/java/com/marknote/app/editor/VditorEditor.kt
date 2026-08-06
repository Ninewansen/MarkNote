package com.marknote.app.editor

import android.annotation.SuppressLint
import android.content.Context
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.ViewGroup
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.marknote.app.R
import com.marknote.app.data.FileRepository
import com.marknote.app.data.IMAGES_DIR_NAME
import com.marknote.app.util.AppPreferences
import java.io.File
import java.io.FileInputStream
import java.util.Locale

/**
 * Notion/Typora 风格“实时渲染编辑”面板：基于 Vditor 的 WYSIWYG 模式。
 * 输入内容即时渲染成富文本，同时通过 JS 桥把 Markdown 回传给 ViewModel。
 */
@Composable
fun VditorPane(
    content: String,
    dark: Boolean,
    visible: Boolean,
    onReady: () -> Unit,
    onContentChanged: (String) -> Unit,
    onControllerCreated: (LiveEditorController) -> Unit,
    onPickImage: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var webView by remember { mutableStateOf<WebView?>(null) }
    var ready by remember { mutableStateOf(false) }
    var lastSynced by remember { mutableStateOf<String?>(null) }

    Box(modifier = modifier) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { context ->
                val controller = LiveEditorController(
                    VditorPreloader.take() ?: createVditorWebView(context)
                )
                val wv = controller.webView
                val reused = controller.webView === VditorPreloader.lastTaken
                if (reused) {
                    // 复用预热 WebView：把回调接到预加载器的转发桥
                    VditorPreloader.attach(
                        ready = {
                            controller.markReady()
                            ready = true
                            onReady()
                        },
                        content = { md ->
                            lastSynced = md
                            onContentChanged(md)
                        },
                        pickImage = onPickImage,
                    )
                    if (VditorPreloader.isReady()) {
                        controller.markReady()
                        ready = true
                        onReady()
                    }
                } else {
                    val bridge = VditorBridge(
                        language = currentBridgeLanguage(context),
                        readyCallback = {
                            controller.markReady()
                            ready = true
                            onReady()
                        },
                        contentCallback = { md ->
                            lastSynced = md
                            onContentChanged(md)
                        },
                        pickImageCallback = onPickImage,
                    )
                    wv.addJavascriptInterface(bridge, "Android")
                    wv.loadUrl("file:///android_asset/vditor/editor.html")
                }
                webView = wv
                onControllerCreated(controller)
                wv
            },
            update = { wv ->
                if (webView !== wv) {
                    webView = wv
                }
                // 非实时模式（分屏/仅预览/仅编辑）时真正隐藏 WebView，
                // 避免其残留画面叠在原生预览上。
                wv.visibility = if (visible) View.VISIBLE else View.GONE
            },
        )

        // 编辑器内核（Vditor + Lute）初始化需要一点时间，未就绪时显示加载提示，
        // 避免看起来像白屏/卡死。
        if (!ready) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator()
                    Spacer(Modifier.height(10.dp))
                    Text(
                        stringResource(R.string.editor_loading),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.outline,
                    )
                }
            }
        }
    }

    // 页面加载完成后把当前内容推入编辑器（切换模式时同步）
    LaunchedEffect(content, ready) {
        val wv = webView ?: return@LaunchedEffect
        if (ready && content != lastSynced) {
            wv.evaluateJavascript("window.setMarkNoteContent(${jsString(content)})", null)
            lastSynced = content
        }
    }

    // 深色模式同步
    LaunchedEffect(dark, ready) {
        if (ready) {
            webView?.evaluateJavascript("window.setMarkNoteDark($dark)", null)
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            VditorPreloader.detach()
            val wv = webView
            if (wv != null) {
                (wv.parent as? ViewGroup)?.removeView(wv)
                wv.removeJavascriptInterface("Android")
                wv.stopLoading()
                wv.loadUrl("about:blank")
                wv.destroy()
            }
        }
    }
}

/** 供 Compose 工具栏调用的实时编辑器命令 */
class LiveEditorController internal constructor(
    internal val webView: WebView,
) {
    @Volatile
    private var jsReady = false

    internal fun markReady() {
        jsReady = true
    }

    fun execCommand(name: String) {
        if (!jsReady) return
        webView.evaluateJavascript("window.markNoteExec(${jsString(name)})", null)
    }

    fun heading(level: Int) {
        if (!jsReady) return
        webView.evaluateJavascript("window.markNoteHeading($level)", null)
    }

    fun insertMarkdown(md: String) {
        if (!jsReady) return
        webView.evaluateJavascript("window.markNoteInsert(${jsString(md)})", null)
    }

    fun undo() = execCommand("undo")

    fun redo() = execCommand("redo")

    fun focus() {
        if (!jsReady) return
        webView.requestFocus()
        webView.evaluateJavascript("window.focusMarkNote()", null)
    }

    /** 切换回实时模式时重建页面布局高度（防止 WebView 初始 0 高度导致的空白） */
    fun resize() {
        if (!jsReady) return
        webView.evaluateJavascript("window.__markNoteFixHeight()", null)
    }
}

/** 根据应用设置/系统语言，返回 editor.html 使用的语言标记 */
internal fun currentBridgeLanguage(context: Context): String {
    val appLang = AppPreferences.language(context)
    return when {
        appLang == AppPreferences.LANG_ZH -> "zh"
        appLang == AppPreferences.LANG_EN -> "en"
        appLang == AppPreferences.LANG_FR -> "fr"
        appLang == AppPreferences.LANG_DE -> "de"
        appLang == AppPreferences.LANG_JA -> "ja"
        appLang == AppPreferences.LANG_ES -> "es"
        else -> {
            when (Locale.getDefault().language) {
                "zh" -> "zh"
                "fr" -> "fr"
                "de" -> "de"
                "ja" -> "ja"
                "es" -> "es"
                else -> "en"
            }
        }
    }
}

private fun createVditorWebView(context: Context): WebView {
    // 允许通过 chrome://inspect 远程调试编辑器页面
    WebView.setWebContentsDebuggingEnabled(true)

    @SuppressLint("SetJavaScriptEnabled")
    val wv = WebView(context)
    wv.settings.apply {
        javaScriptEnabled = true
        domStorageEnabled = true
        allowFileAccess = true
        allowContentAccess = true
        databaseEnabled = true
        loadsImagesAutomatically = true
        mediaPlaybackRequiresUserGesture = false
        textZoom = 100
    }
    val imagesDir = FileRepository(context.applicationContext).imagesDir
    wv.webViewClient = object : WebViewClient() {
        override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
            // 阻止内容中的链接直接接管 WebView 导航（编辑页内不应跳转）
            return true
        }

        // 把 md 里的相对图片路径（Images/xxx.png）映射到真实图片文件，让实时编辑器能显示本地图片
        override fun shouldInterceptRequest(view: WebView?, url: String?): WebResourceResponse? {
            val u = url ?: return null
            val prefix = "file:///android_asset/vditor/$IMAGES_DIR_NAME/"
            if (u.startsWith(prefix)) {
                val name = Uri.decode(u.removePrefix(prefix))
                if (name.isNotBlank() && !name.contains('/')) {
                    val img = File(imagesDir, name)
                    if (img.isFile) {
                        val mime = when (img.extension.lowercase(Locale.ROOT)) {
                            "png" -> "image/png"
                            "jpg", "jpeg" -> "image/jpeg"
                            "gif" -> "image/gif"
                            "webp" -> "image/webp"
                            "svg" -> "image/svg+xml"
                            else -> "application/octet-stream"
                        }
                        return try {
                            WebResourceResponse(mime, null, FileInputStream(img))
                        } catch (_: Exception) {
                            null
                        }
                    }
                }
            }
            return null
        }
    }
    wv.webChromeClient = WebChromeClient()
    wv.setBackgroundColor(0x00000000)
    return wv
}

/**
 * Android -> JS 桥（被 editor.html 调用）
 */
private class VditorBridge(
    private val language: String,
    private val readyCallback: () -> Unit,
    private val contentCallback: (String) -> Unit,
    private val pickImageCallback: () -> Unit = {},
) {
    private val main = Handler(Looper.getMainLooper())

    @JavascriptInterface
    fun onReady() {
        main.post { readyCallback() }
    }

    @JavascriptInterface
    fun onContentChanged(md: String) {
        main.post { contentCallback(md) }
    }

    @JavascriptInterface
    fun getLanguage(): String {
        return language
    }

    /** 用户在实时编辑器里选“图片”命令时，唤起系统图片选择器 */
    @JavascriptInterface
    fun pickImage() {
        main.post { pickImageCallback() }
    }
}

/**
 * 编辑器内核预热：在文件列表页停留时后台加载 Vditor + Lute（最耗时的 4MB+ JS），
 * 用户打开文件时直接复用，避免“切换语言/打开文件卡几秒”。
 */
object VditorPreloader {
    @Volatile
    private var webView: WebView? = null

    @Volatile
    private var language: String? = null

    @Volatile
    private var jsReady = false

    @Volatile
    private var readyListener: (() -> Unit)? = null

    @Volatile
    private var contentListener: ((String) -> Unit)? = null

    @Volatile
    private var pickImageListener: (() -> Unit)? = null

    /** 最近一次 take 出去的 WebView（供调用方判断是否复用） */
    @Volatile
    internal var lastTaken: WebView? = null
        private set

    fun ensure(context: Context, language: String) {
        if (webView != null && this.language == language && jsReady) return
        if (webView != null) {
            destroy()
        }
        this.language = language
        val bridge = VditorBridge(
            language = language,
            readyCallback = {
                jsReady = true
                readyListener?.invoke()
            },
            contentCallback = { md -> contentListener?.invoke(md) },
            pickImageCallback = { pickImageListener?.invoke() },
        )
        val wv = createVditorWebView(context)
        wv.addJavascriptInterface(bridge, "Android")
        webView = wv
        wv.loadUrl("file:///android_asset/vditor/editor.html")
    }

    /** 取出已预热完成的 WebView；未就绪则返回 null（调用方新建） */
    fun take(): WebView? {
        val wv = webView
        if (wv == null || !jsReady) return null
        webView = null
        lastTaken = wv
        return wv
    }

    fun isReady(): Boolean = jsReady

    fun attach(
        ready: () -> Unit,
        content: (String) -> Unit,
        pickImage: () -> Unit,
    ) {
        readyListener = ready
        contentListener = content
        pickImageListener = pickImage
        if (jsReady) ready()
    }

    fun detach() {
        readyListener = null
        contentListener = null
        pickImageListener = null
    }

    fun destroy() {
        webView?.let { wv ->
            (wv.parent as? ViewGroup)?.removeView(wv)
            wv.removeJavascriptInterface("Android")
            wv.stopLoading()
            wv.loadUrl("about:blank")
            wv.destroy()
        }
        webView = null
        lastTaken = null
        jsReady = false
        language = null
        detach()
    }
}

/** 转成 JS 字符串字面量（JSON 字符串安全） */
private fun jsString(value: String): String {
    val sb = StringBuilder(value.length + 2)
    sb.append('"')
    value.forEach { c ->
        when (c) {
            '"' -> sb.append("\\\"")
            '\\' -> sb.append("\\\\")
            '\n' -> sb.append("\\n")
            '\r' -> sb.append("\\r")
            '\t' -> sb.append("\\t")
            '\b' -> sb.append("\\b")
            '\u000C' -> sb.append("\\f")
            else -> {
                if (c.code < 0x20) {
                    sb.append("\\u")
                    sb.append(c.code.toString(16).padStart(4, '0'))
                } else {
                    sb.append(c)
                }
            }
        }
    }
    sb.append('"')
    return sb.toString()
}

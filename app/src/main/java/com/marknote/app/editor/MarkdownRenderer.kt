package com.marknote.app.editor

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.net.Uri
import android.text.Spanned
import android.text.style.ReplacementSpan
import io.noties.markwon.AbstractMarkwonPlugin
import io.noties.markwon.Markwon
import io.noties.markwon.MarkwonSpansFactory
import io.noties.markwon.SpanFactory
import io.noties.markwon.core.MarkwonTheme
import io.noties.markwon.ext.strikethrough.StrikethroughPlugin
import io.noties.markwon.ext.tables.TablePlugin
import io.noties.markwon.ext.tasklist.TaskListPlugin
import io.noties.markwon.image.ImageProps
import io.noties.markwon.image.ImagesPlugin
import io.noties.markwon.image.file.FileSchemeHandler
import io.noties.markwon.linkify.LinkifyPlugin
import io.noties.markwon.syntax.Prism4jThemeDarkula
import io.noties.markwon.syntax.Prism4jThemeDefault
import io.noties.markwon.syntax.SyntaxHighlightPlugin
import io.noties.prism4j.Prism4j
import org.commonmark.node.Image
import java.io.File
import kotlin.math.min

/**
 * Markdown 预览渲染器。
 *
 * 基于 GitHub 上维护良好的 Markwon 项目（noties/Markwon）：
 * 不经过 WebView/HTML，直接把 Markdown 渲染成 Android 原生 Spannable，
 * 实时预览稳定、流畅、无桥接空白页问题。
 */
object MarkdownRenderer {

    @Volatile
    private var lightInstance: Markwon? = null

    @Volatile
    private var darkInstance: Markwon? = null

    fun get(context: Context, dark: Boolean): Markwon {
        return if (dark) {
            darkInstance ?: synchronized(this) {
                darkInstance ?: build(context.applicationContext, dark = true).also { darkInstance = it }
            }
        } else {
            lightInstance ?: synchronized(this) {
                lightInstance ?: build(context.applicationContext, dark = false).also { lightInstance = it }
            }
        }
    }

    /** 在后台线程调用：把 Markdown 源码渲染成可显示的 Spanned */
    fun render(markwon: Markwon, markdown: String, baseDir: File?): Spanned {
        val resolved = resolveImagePaths(markdown, baseDir)
        return markwon.toMarkdown(normalizeBlockImages(resolved))
    }

    private fun build(context: Context, dark: Boolean): Markwon {
        val prism4j = Prism4j(MarkdownGrammarLocator())
        val syntaxTheme = if (dark) {
            Prism4jThemeDarkula.create(0xFF1F252D.toInt())
        } else {
            Prism4jThemeDefault.create()
        }

        val colors = PreviewColors.of(dark)

        return Markwon.builder(context)
            .usePlugin(StrikethroughPlugin.create())
            .usePlugin(TablePlugin.create(context))
            .usePlugin(TaskListPlugin.create(context))
            .usePlugin(LinkifyPlugin.create())
            // 必须先于 ImagesPlugin 注册：本地图片走同步解码 Span，
            // 非本地图片（http/data）返回 null，由 ImagesPlugin 继续处理。
            .usePlugin(object : AbstractMarkwonPlugin() {
                override fun configureSpansFactory(builder: MarkwonSpansFactory.Builder) {
                    builder.setFactory(Image::class.java, SpanFactory { _, props ->
                        val destination = props.get(ImageProps.DESTINATION)
                        if (destination != null && destination.startsWith("file:")) {
                            decodeLocalImage(context, destination)
                        } else {
                            null
                        }
                    })
                }
            })
            .usePlugin(
                ImagesPlugin.create()
                    // 支持本地 file:// 图片（md 里 Images/xxx.png 相对路径）
                    .addSchemeHandler(FileSchemeHandler.create()),
            )
            .usePlugin(SyntaxHighlightPlugin.create(prism4j, syntaxTheme, "text"))
            .usePlugin(object : AbstractMarkwonPlugin() {
                override fun configureTheme(builder: MarkwonTheme.Builder) {
                    builder
                        .linkColor(colors.link)
                        .isLinkUnderlined(true)
                        .blockQuoteColor(colors.quoteBar)
                        .blockQuoteWidth(4)
                        .listItemColor(colors.bullet)
                        .codeTextColor(colors.codeText)
                        .codeBackgroundColor(colors.codeBackground)
                        .codeBlockTextColor(colors.codeBlockText)
                        .codeBlockBackgroundColor(colors.codeBlockBackground)
                        .codeBlockMargin(8)
                        .headingBreakColor(colors.divider)
                        .headingBreakHeight(1)
                        .thematicBreakColor(colors.divider)
                        .thematicBreakHeight(1)
                }
            })
            .build()
    }

    // 把相对路径图片（如 ![a](images/1.png)）解析为绝对 file:// URI，
    // 让预览能直接显示 Markdown 所在目录的本地图片。
    private val imageDestinationRegex = Regex("""(!\[[^\]]*]\()([^)\s]+)(\))""")

    /** 完整的图片语法（含可选 title）：![alt](url "title") */
    private val imageSyntaxRegex = Regex("""(!\[[^\]]*]\([^)\s]+(?:\s+"[^"]*")?\))""")

    /**
     * 预览排版优化：把与其它文字同行的图片拆成独立段落，
     * 避免大图把句子切成“图片左/右各挤一段文字”的混乱布局。
     * 图片链接 [![a](b)](c) 除外（保持原样）。
     */
    private fun normalizeBlockImages(markdown: String): String {
        val sb = StringBuilder(markdown.length + 32)
        markdown.split("\n").forEach { rawLine ->
            val line = rawLine.trimEnd()
            if (line.isBlank()) {
                sb.append(rawLine).append('\n')
                return@forEach
            }
            val matches = imageSyntaxRegex.findAll(line).toList()
            if (matches.isEmpty()) {
                sb.append(rawLine).append('\n')
                return@forEach
            }
            val withoutImages = line.replace(imageSyntaxRegex, "")
            if (withoutImages.isBlank()) {
                sb.append(line).append('\n')
                return@forEach
            }
            // 图片链接（后面紧跟 ]）不拆，保持行内
            if (matches.any { line.getOrNull(it.range.last + 1) == ']' }) {
                sb.append(rawLine).append('\n')
                return@forEach
            }
            var last = 0
            for (m in matches) {
                sb.append(line.substring(last, m.range.first).trim()).append("\n\n")
                sb.append(m.value.trim()).append("\n\n")
                last = m.range.last + 1
            }
            sb.append(line.substring(last).trim()).append('\n')
        }
        return sb.toString()
    }

    private fun resolveImagePaths(markdown: String, baseDir: File?): String {
        if (baseDir == null) return markdown
        return imageDestinationRegex.replace(markdown) { match ->
            val destination = match.groupValues[2]
            val isAbsolute =
                destination.startsWith("/") ||
                    destination.startsWith("file:") ||
                    destination.startsWith("http://") ||
                    destination.startsWith("https://") ||
                    destination.startsWith("data:") ||
                    destination.contains("://")
            if (isAbsolute) {
                match.value
            } else {
                val resolved = File(baseDir, destination).canonicalFile
                match.groupValues[1] + resolved.toURI().toString() + match.groupValues[3]
            }
        }
    }

    /** 在后台渲染线程同步加载本地图片，按屏幕宽度降采样，返回固定尺寸的 Span */
    private fun decodeLocalImage(context: Context, fileUri: String): Drawable? {
        val path = Uri.parse(fileUri).path ?: return null
        val file = File(path)
        if (!file.isFile) return null

        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(path, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

        val metrics = context.resources.displayMetrics
        val maxWidth = (metrics.widthPixels * 0.92f).toInt().coerceAtLeast(1)
        val maxHeight = (metrics.heightPixels * 0.55f).toInt().coerceAtLeast(1)

        var sample = 1
        while (bounds.outWidth / (sample * 2) >= maxWidth ||
            bounds.outHeight / (sample * 2) >= maxHeight
        ) {
            sample *= 2
        }

        val decode = BitmapFactory.Options().apply { inSampleSize = sample }
        val bitmap = BitmapFactory.decodeFile(path, decode) ?: return null
        val scale = min(
            1f,
            min(maxWidth.toFloat() / bitmap.width, maxHeight.toFloat() / bitmap.height),
        )
        val width = (bitmap.width * scale).toInt().coerceAtLeast(1)
        val height = (bitmap.height * scale).toInt().coerceAtLeast(1)
        val scaled = if (scale < 1f) {
            Bitmap.createScaledBitmap(bitmap, width, height, true)
        } else {
            bitmap
        }
        return BitmapDrawable(context.resources, scaled).apply {
            setBounds(0, 0, width, height)
        }
    }
}

/** 同步图片 Span：尺寸恒定，不会像异步 Span 那样先按旧尺寸排版再跳变 */
private class PreviewImageSpan(private val drawable: Drawable) : ReplacementSpan() {

    override fun getSize(
        paint: Paint,
        text: CharSequence,
        start: Int,
        end: Int,
        fm: Paint.FontMetricsInt?,
    ): Int {
        if (fm != null) {
            fm.ascent = -drawable.bounds.height()
            fm.descent = 0
            fm.top = fm.ascent
            fm.bottom = 0
        }
        return drawable.bounds.width()
    }

    override fun draw(
        canvas: Canvas,
        text: CharSequence,
        start: Int,
        end: Int,
        x: Float,
        top: Int,
        baseline: Int,
        bottom: Int,
        paint: Paint,
    ) {
        canvas.save()
        canvas.translate(x, (baseline - drawable.bounds.bottom).toFloat())
        drawable.draw(canvas)
        canvas.restore()
    }
}

/** 与 Compose Material 主题保持一致的预览配色 */
private data class PreviewColors(
    val link: Int,
    val quoteBar: Int,
    val bullet: Int,
    val codeText: Int,
    val codeBackground: Int,
    val codeBlockText: Int,
    val codeBlockBackground: Int,
    val divider: Int,
) {
    companion object {
        fun of(dark: Boolean): PreviewColors = if (dark) {
            PreviewColors(
                link = 0xFF7EA6FF.toInt(),
                quoteBar = 0xFF8B949E.toInt(),
                bullet = 0xFF8B949E.toInt(),
                codeText = 0xFFE6EDF3.toInt(),
                codeBackground = 0xFF161B22.toInt(),
                codeBlockText = 0xFFE6EDF3.toInt(),
                codeBlockBackground = 0xFF161B22.toInt(),
                divider = 0xFF30363D.toInt(),
            )
        } else {
            PreviewColors(
                link = 0xFF1B5FD1.toInt(),
                quoteBar = 0xFF57606A.toInt(),
                bullet = 0xFF57606A.toInt(),
                codeText = 0xFF24292F.toInt(),
                codeBackground = 0xFFF6F8FA.toInt(),
                codeBlockText = 0xFF24292F.toInt(),
                codeBlockBackground = 0xFFF6F8FA.toInt(),
                divider = 0xFFD0D7DE.toInt(),
            )
        }
    }
}

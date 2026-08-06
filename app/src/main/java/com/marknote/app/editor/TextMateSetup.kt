package com.marknote.app.editor

import android.content.Context
import android.util.TypedValue
import io.github.rosemoe.sora.langs.textmate.TextMateColorScheme
import io.github.rosemoe.sora.langs.textmate.TextMateLanguage
import io.github.rosemoe.sora.langs.textmate.registry.FileProviderRegistry
import io.github.rosemoe.sora.langs.textmate.registry.GrammarRegistry
import io.github.rosemoe.sora.langs.textmate.registry.ThemeRegistry
import io.github.rosemoe.sora.langs.textmate.registry.model.ThemeModel
import io.github.rosemoe.sora.langs.textmate.registry.provider.AssetsFileResolver
import io.github.rosemoe.sora.widget.CodeEditor
import org.eclipse.tm4e.core.registry.IThemeSource

/**
 * sora-editor TextMate 环境初始化（主题 + 语法，只做一次）。
 */
object TextMateSetup {

    @Volatile
    private var initialized = false

    fun init(context: Context) {
        if (initialized) return
        synchronized(this) {
            if (initialized) return

            // 1. 注册 assets 文件提供器（textmate 语法/主题文件都放在 assets/textmate/ 下）
            FileProviderRegistry.getInstance()
                .addFileProvider(AssetsFileResolver(context.applicationContext.assets))

            // 2. 加载主题（demo 同款：darcula/ayu-dark/quietlight/solarized_dark）
            val themeRegistry = ThemeRegistry.getInstance()
            val themes = arrayOf("darcula", "ayu-dark", "quietlight", "solarized_dark")
            for (name in themes) {
                val path = "textmate/$name.json"
                themeRegistry.loadTheme(
                    ThemeModel(
                        IThemeSource.fromInputStream(
                            FileProviderRegistry.getInstance().tryGetInputStream(path),
                            path,
                            null
                        ),
                        name
                    ).apply {
                        if (name != "quietlight") isDark = true
                    }
                )
            }
            themeRegistry.setTheme("quietlight")

            // 3. 加载语法（languages.json 中已含 markdown 及其内嵌语言）
            GrammarRegistry.getInstance().loadGrammars("textmate/languages.json")

            initialized = true
        }
    }

    /** 按系统深色模式切换编辑器主题 */
    fun applyTheme(dark: Boolean) {
        ThemeRegistry.getInstance().setTheme(if (dark) "darcula" else "quietlight")
    }

    /** 创建配置好的 Markdown 编辑器实例 */
    fun createMarkdownEditor(context: Context): CodeEditor {
        val editor = CodeEditor(context)
        editor.colorScheme = TextMateColorScheme.create(ThemeRegistry.getInstance())
        editor.setEditorLanguage(TextMateLanguage.create("text.html.markdown", true))
        editor.isLineNumberEnabled = true
        editor.isWordwrap = true
        editor.tabWidth = 4
        editor.textSizePx = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_SP,
            15f,
            context.resources.displayMetrics,
        )
        editor.isHighlightCurrentLine = true
        editor.isHighlightBracketPair = true
        editor.isCursorAnimationEnabled = true
        return editor
    }
}

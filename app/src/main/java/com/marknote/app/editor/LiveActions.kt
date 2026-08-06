package com.marknote.app.editor

/**
 * 实时渲染编辑（Vditor WYSIWYG）下的格式命令映射。
 * 复用 / 快捷菜单与底部面板的命令 ID，路由到 Vditor 内置工具栏命令。
 */
object LiveActions {

    fun apply(command: SlashCommand, live: LiveEditorController) {
        when (command.id) {
            "h1" -> live.heading(1)
            "h2" -> live.heading(2)
            "h3" -> live.heading(3)
            "bold" -> live.execCommand("bold")
            "italic" -> live.execCommand("italic")
            "strikethrough" -> live.execCommand("strike")
            "code" -> live.execCommand("inline-code")
            "codeblock" -> live.execCommand("code")
            "quote" -> live.execCommand("quote")
            "bullet" -> live.execCommand("list")
            "ordered" -> live.execCommand("ordered-list")
            "task" -> live.execCommand("check")
            "link" -> live.execCommand("link")
            "table" -> live.execCommand("table")
            "hr" -> live.execCommand("line")
            "image" -> live.insertMarkdown("![](https://)")
        }
    }
}

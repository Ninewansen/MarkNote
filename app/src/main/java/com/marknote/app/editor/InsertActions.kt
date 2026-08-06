package com.marknote.app.editor

import io.github.rosemoe.sora.text.Content
import io.github.rosemoe.sora.widget.CodeEditor

/**
 * 工具栏插入动作：在光标处/选中区插入 Markdown 语法模板。
 */
object InsertActions {

    /** 标题 # ~ ##### */
    fun heading(editor: CodeEditor, level: Int) {
        insertLinePrefix(editor, "#".repeat(level) + " ")
    }

    fun bold(editor: CodeEditor) = wrapSelection(editor, "**", "**")

    fun italic(editor: CodeEditor) = wrapSelection(editor, "*", "*")

    fun strikethrough(editor: CodeEditor) = wrapSelection(editor, "~~", "~~")

    fun quote(editor: CodeEditor) = insertLinePrefix(editor, "> ")

    fun bulletList(editor: CodeEditor) = insertLinePrefix(editor, "- ")

    fun orderedList(editor: CodeEditor) = insertLinePrefix(editor, "1. ")

    fun taskList(editor: CodeEditor) = insertLinePrefix(editor, "- [ ] ")

    fun inlineCode(editor: CodeEditor) = wrapSelection(editor, "`", "`")

    fun codeBlock(editor: CodeEditor) = wrapSelection(editor, "```\n", "\n```")

    fun link(editor: CodeEditor) = wrapSelection(editor, "[", "](https://)")

    fun image(editor: CodeEditor) = wrapSelection(editor, "![", "](https://)")

    /** 插入本地图片：md 里写相对路径（如 Images/xxx.png），预览/实时编辑都能显示 */
    fun imageFile(editor: CodeEditor, relativePath: String) {
        val name = relativePath.substringAfterLast('/')
        insertAtCursor(editor, "![$name]($relativePath)")
    }

    fun horizontalRule(editor: CodeEditor) = insertLinePrefix(editor, "---\n")

    fun table(editor: CodeEditor) {
        val template = """
            | 列1 | 列2 | 列3 |
            | --- | --- | --- |
            | 内容 | 内容 | 内容 |
        """.trimIndent()
        insertAtCursor(editor, "\n$template\n")
    }

    fun quoteBlock(editor: CodeEditor) = wrapSelection(editor, "> ", "\n")

    // ---------- 基础操作 ----------

    /** 在选区两侧包裹前后缀，光标定位到选区中间 */
    private fun wrapSelection(editor: CodeEditor, prefix: String, suffix: String) {
        val content: Content = editor.text
        val left = editor.cursor.leftLine to editor.cursor.leftColumn
        val right = editor.cursor.rightLine to editor.cursor.rightColumn
        val start = content.getCharIndex(left.first, left.second)
        val end = content.getCharIndex(right.first, right.second)
        val selected = content.subSequence(start, end)

        content.replace(start, end, prefix + selected + suffix)

        // 光标移到内容中间
        val target = content.indexer.getCharPosition(start + prefix.length)
        editor.setSelection(target.line, target.column)
    }

    /** 在光标所在行首插入前缀（多行选区则每行插入） */
    private fun insertLinePrefix(editor: CodeEditor, prefix: String) {
        val content: Content = editor.text
        val startLine = editor.cursor.leftLine
        val endLine = editor.cursor.rightLine

        // 从后往前逐行插入，避免行号偏移
        for (line in endLine downTo startLine) {
            val lineStart = content.getCharIndex(line, 0)
            val lineText = content.getLine(line).toString()
            val indent = lineText.takeWhile { it == ' ' || it == '\t' }
            content.replace(lineStart, lineStart + indent.length, indent + prefix)
        }

        // 光标移到本行内容之后
        val pos = content.indexer.getCharPosition(content.getCharIndex(endLine, 0) + prefix.length)
        editor.setSelection(pos.line, pos.column)
    }

    /** 在光标处插入文本 */
    private fun insertAtCursor(editor: CodeEditor, text: String) {
        val content: Content = editor.text
        val pos = editor.cursor.leftLine to editor.cursor.leftColumn
        val index = content.getCharIndex(pos.first, pos.second)
        content.replace(index, index, text)
        val target = content.indexer.getCharPosition(index + text.length)
        editor.setSelection(target.line, target.column)
    }
}

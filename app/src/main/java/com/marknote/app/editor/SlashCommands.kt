package com.marknote.app.editor

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.FormatListBulleted
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.DataObject
import androidx.compose.material.icons.filled.FormatBold
import androidx.compose.material.icons.filled.FormatItalic
import androidx.compose.material.icons.filled.FormatListNumbered
import androidx.compose.material.icons.filled.FormatQuote
import androidx.compose.material.icons.filled.FormatStrikethrough
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.HorizontalRule
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Title
import androidx.compose.ui.graphics.vector.ImageVector
import com.marknote.app.R
import io.github.rosemoe.sora.widget.CodeEditor

/** Notion 风格斜杠菜单状态：/ 所在行、列与当前输入的关键词 */
data class SlashState(
    val query: String,
    val line: Int,
    val column: Int,
    val slashColumn: Int,
)

/** 斜杠菜单命令 */
data class SlashCommand(
    val id: String,
    val labelRes: Int,
    val icon: ImageVector,
    val keywords: List<String>,
    val action: (CodeEditor) -> Unit,
) {
    fun matches(query: String): Boolean {
        if (query.isBlank()) return true
        val q = query.lowercase()
        return keywords.any { it.contains(q, ignoreCase = true) } || id.contains(q, ignoreCase = true)
    }
}

object SlashCommands {

    private val SLASH_REGEX = Regex("""^(\s*)/([\p{L}\p{N}]*)$""")

    val all: List<SlashCommand> = listOf(
        SlashCommand("h1", R.string.menu_heading_1, Icons.Filled.Title, listOf("h1", "heading", "标题")) {
            InsertActions.heading(it, 1)
        },
        SlashCommand("h2", R.string.menu_heading_2, Icons.Filled.Title, listOf("h2", "标题")) {
            InsertActions.heading(it, 2)
        },
        SlashCommand("h3", R.string.menu_heading_3, Icons.Filled.Title, listOf("h3", "标题")) {
            InsertActions.heading(it, 3)
        },
        SlashCommand("bold", R.string.toolbar_bold, Icons.Filled.FormatBold, listOf("bold", "加粗", "b")) {
            InsertActions.bold(it)
        },
        SlashCommand("italic", R.string.toolbar_italic, Icons.Filled.FormatItalic, listOf("italic", "斜体", "i")) {
            InsertActions.italic(it)
        },
        SlashCommand("strikethrough", R.string.toolbar_strikethrough, Icons.Filled.FormatStrikethrough, listOf("strikethrough", "删除线", "del")) {
            InsertActions.strikethrough(it)
        },
        SlashCommand("code", R.string.toolbar_inline_code, Icons.Filled.Code, listOf("code", "行内代码", "代码")) {
            InsertActions.inlineCode(it)
        },
        SlashCommand("codeblock", R.string.toolbar_code_block, Icons.Filled.DataObject, listOf("codeblock", "代码块", "fence")) {
            InsertActions.codeBlock(it)
        },
        SlashCommand("quote", R.string.toolbar_quote, Icons.Filled.FormatQuote, listOf("quote", "引用")) {
            InsertActions.quote(it)
        },
        SlashCommand("bullet", R.string.toolbar_bullet_list, Icons.AutoMirrored.Filled.FormatListBulleted, listOf("bullet", "list", "无序", "列表")) {
            InsertActions.bulletList(it)
        },
        SlashCommand("ordered", R.string.toolbar_ordered_list, Icons.Filled.FormatListNumbered, listOf("ordered", "numbered", "有序")) {
            InsertActions.orderedList(it)
        },
        SlashCommand("task", R.string.toolbar_task_list, Icons.Filled.Checklist, listOf("task", "todo", "任务")) {
            InsertActions.taskList(it)
        },
        SlashCommand("link", R.string.toolbar_link, Icons.Filled.Link, listOf("link", "链接")) {
            InsertActions.link(it)
        },
        SlashCommand("image", R.string.toolbar_image, Icons.Filled.Image, listOf("image", "图片", "img")) {
            InsertActions.image(it)
        },
        SlashCommand("table", R.string.toolbar_table, Icons.Filled.GridOn, listOf("table", "表格")) {
            InsertActions.table(it)
        },
        SlashCommand("hr", R.string.toolbar_horizontal_rule, Icons.Filled.HorizontalRule, listOf("hr", "rule", "分割线")) {
            InsertActions.horizontalRule(it)
        },
    )

    /** 根据当前光标位置判断斜杠菜单是否激活 */
    fun computeState(editor: CodeEditor): SlashState? {
        val cursor = editor.cursor
        if (cursor.isSelected) return null
        val line = cursor.leftLine
        val column = cursor.leftColumn
        if (column > 64) return null
        val lineText = editor.text.getLine(line).toString()
        if (column > lineText.length) return null
        val before = lineText.substring(0, column)
        val match = SLASH_REGEX.matchEntire(before) ?: return null
        val slashColumn = match.groupValues[1].length
        return SlashState(
            query = match.groupValues[2],
            line = line,
            column = column,
            slashColumn = slashColumn,
        )
    }

    /** 移除 /query 并把光标移到原位置，然后执行命令 */
    fun apply(editor: CodeEditor, state: SlashState, action: (CodeEditor) -> Unit) {
        val content = editor.text
        val start = content.getCharIndex(state.line, state.slashColumn)
        val end = content.getCharIndex(state.line, state.column)
        content.replace(start, end, "")
        val position = content.indexer.getCharPosition(start)
        editor.setSelection(position.line, position.column)
        action(editor)
    }
}

package com.marknote.app.data

import android.content.Context
import android.net.Uri
import com.marknote.app.R
import java.io.File
import java.io.FileOutputStream
import java.util.Locale

/** 支持编辑/预览的文本文件扩展名 */
val SUPPORTED_TEXT_EXTENSIONS = setOf("md", "markdown", "txt", "text")

/** 图片等媒体文件的相对子目录（相对 Documents/ 根目录） */
const val IMAGES_DIR_NAME = "Images"

/** 文件列表条目 */
data class MarkdownFile(
    val name: String,
    val size: Long,
    val modifiedAt: Long,
    val excerpt: String = "",
) {
    fun toFile(parent: File) = File(parent, name)
}

/**
 * Markdown 文件仓库。
 * 文档存放于应用专属外部目录 Documents/（Android 11+ 无需任何存储权限），
 * 卸载应用时一并清除；内部存储兜底。
 */
class FileRepository(private val context: Context) {

    val rootDir: File
        get() = context.getExternalFilesDir("Documents") ?: context.filesDir

    /** 图片等媒体文件目录：与文档同在一个文件夹体系，md 中用相对路径 Images/xxx 引用 */
    val imagesDir: File
        get() = File(rootDir, IMAGES_DIR_NAME)

    fun ensureRoot() {
        rootDir.mkdirs()
        imagesDir.mkdirs()
    }

    /** 把外部图片复制到 Images/ 目录，返回 Markdown 相对路径（Images/xxx.png）；失败返回 null */
    fun importImage(uri: Uri): String? {
        ensureRoot()
        return try {
            val resolver = context.contentResolver
            val displayName = resolver.getType(uri)
                ?.substringAfterLast('/', "png")
                ?.takeIf { it.isNotBlank() }
                ?: "png"
            val safeExt = displayName.lowercase(Locale.ROOT).replace(Regex("""[^a-z0-9]"""), "")
                .takeIf { it.isNotBlank() } ?: "png"
            val name = "${System.currentTimeMillis()}.$safeExt"
            val target = File(imagesDir, name)
            resolver.openInputStream(uri)?.use { input ->
                FileOutputStream(target).use { output ->
                    input.copyTo(output)
                }
            } ?: return null
            "$IMAGES_DIR_NAME/$name"
        } catch (_: Exception) {
            null
        }
    }

    fun listFiles(): List<MarkdownFile> {
        ensureRoot()
        return rootDir.listFiles { f -> f.isFile && f.extension.lowercase() in SUPPORTED_TEXT_EXTENSIONS }
            ?.sortedByDescending { it.lastModified() }
            ?.map { file ->
                MarkdownFile(
                    name = file.name,
                    size = file.length(),
                    modifiedAt = file.lastModified(),
                    excerpt = readExcerpt(file),
                )
            }
            ?: emptyList()
    }

    /** 新建文件；name 不含扩展名时自动补 .md；已存在则返回 null */
    fun create(name: String): File? {
        ensureRoot()
        val fileName = sanitizeFileName(name)
        if (fileName.isBlank()) return null
        val file = File(rootDir, fileName)
        if (file.exists()) return null
        return if (file.createNewFile()) file else null
    }

    /** 重命名；目标已存在返回 false */
    fun rename(oldName: String, newName: String): Boolean {
        val old = File(rootDir, oldName)
        val new = File(rootDir, sanitizeFileName(newName))
        if (!old.exists() || new.exists() || newName.isBlank()) return false
        return old.renameTo(new)
    }

    fun delete(name: String): Boolean {
        val file = File(rootDir, name)
        return file.exists() && file.delete()
    }

    /** 从外部导入内容为新的文本文件；重名时自动加序号，不覆盖已有文件 */
    fun import(name: String, content: String): File? {
        ensureRoot()
        val base = sanitizeFileName(name)
        val stem = base.substringBeforeLast('.')
        val ext = base.substringAfterLast('.', "md")
        var candidate = File(rootDir, base)
        var index = 1
        while (candidate.exists()) {
            candidate = File(rootDir, "$stem ($index).$ext")
            index++
        }
        candidate.writeText(content)
        return candidate
    }

    fun read(file: File): String = file.readText()

    fun write(file: File, content: String) {
        file.writeText(content)
    }

    /** 列表预览：取第一段非空文字，去掉 Markdown 记号，截断到 80 字 */
    private fun readExcerpt(file: File, maxLength: Int = 80): String {
        return try {
            file.useLines { lines ->
                lines
                    .map { it.trim() }
                    .firstOrNull { it.isNotEmpty() }
                    ?.let { stripMarkdown(it) }
                    ?.take(maxLength)
                    ?: ""
            }
        } catch (_: Exception) {
            ""
        }
    }

    private fun stripMarkdown(line: String): String {
        return line
            .trimStart('#', '>', '-', '*', '+', ' ', '\t')
            .trim()
    }

    private fun sanitizeFileName(name: String): String {
        var n = name.trim().replace(Regex("""[\\/:*?"<>|]"""), "_")
        if (n.isBlank()) n = context.getString(R.string.default_file_name)
        val ext = n.substringAfterLast('.', "").lowercase()
        if (ext !in SUPPORTED_TEXT_EXTENSIONS) n += ".md"
        return n
    }
}

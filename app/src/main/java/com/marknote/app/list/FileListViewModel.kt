package com.marknote.app.list

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.marknote.app.R
import com.marknote.app.data.FileRepository
import com.marknote.app.data.MarkdownFile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

enum class FileSort { RECENT, NAME }

class FileListViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = FileRepository(app)

    val rootDir: File
        get() = repo.rootDir

    private val _files = MutableStateFlow<List<MarkdownFile>>(emptyList())
    val files: StateFlow<List<MarkdownFile>> = _files

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query

    private val _filtered = MutableStateFlow<List<MarkdownFile>>(emptyList())
    /** 搜索结果（按修改时间倒序） */
    val filtered: StateFlow<List<MarkdownFile>> = _filtered

    private val _events = MutableStateFlow<String?>(null)
    val events: StateFlow<String?> = _events

    private val _sort = MutableStateFlow(FileSort.RECENT)
    val sort: StateFlow<FileSort> = _sort

    fun consumeEvent() {
        _events.value = null
    }

    private fun notify(message: String) {
        _events.value = message
    }

    private fun recompute() {
        val q = _query.value.trim()
        val sorted = when (_sort.value) {
            FileSort.RECENT -> _files.value.sortedByDescending { it.modifiedAt }
            FileSort.NAME -> _files.value.sortedBy { it.name.lowercase() }
        }
        _filtered.value = if (q.isBlank()) {
            sorted
        } else {
            sorted.filter { it.name.contains(q, ignoreCase = true) }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            var list = withContext(Dispatchers.IO) { repo.listFiles() }
            // 刚安装/升级后外部存储可能尚未就绪，短暂重试一次，避免误显示“空列表”
            if (list.isEmpty()) {
                delay(800)
                list = withContext(Dispatchers.IO) { repo.listFiles() }
            }
            _files.value = list
            recompute()
        }
    }

    fun toFile(md: MarkdownFile): File = File(rootDir, md.name)

    fun setQuery(q: String) {
        _query.value = q
        recompute()
    }

    fun setSort(sort: FileSort) {
        _sort.value = sort
        recompute()
    }

    /** 新建文件，成功返回 File 供直接打开 */
    fun create(name: String): File? = repo.create(name)

    /** 通过 SAF 导入外部文本/Markdown 文件到应用文档目录 */
    fun importUri(uri: Uri) {
        viewModelScope.launch {
            try {
                val resolver = getApplication<Application>().contentResolver
                val displayName = resolver.query(
                    uri,
                    arrayOf(android.provider.OpenableColumns.DISPLAY_NAME),
                    null,
                    null,
                    null,
                )?.use { cursor ->
                    if (cursor.moveToFirst()) cursor.getString(0) else null
                }
                val name = displayName?.takeIf { it.isNotBlank() } ?: "导入文件.md"
                val imported = withContext(Dispatchers.IO) {
                    val content = resolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() } ?: ""
                    repo.import(name, content)
                }
                if (imported != null) {
                    refresh()
                    notify(getApplication<Application>().getString(R.string.import_success, imported.name))
                } else {
                    notify(getApplication<Application>().getString(R.string.import_failed))
                }
            } catch (e: Exception) {
                notify(
                    getApplication<Application>().getString(
                        R.string.import_failed_detail,
                        e.message ?: "?",
                    ),
                )
            }
        }
    }

    /** 通过 SAF 把文件导出到用户选择的位置（保证 .md 扩展名） */
    fun exportTo(fileName: String, uri: Uri) {
        viewModelScope.launch {
            try {
                val file = File(rootDir, fileName)
                val resolver = getApplication<Application>().contentResolver
                withContext(Dispatchers.IO) {
                    resolver.openOutputStream(uri)?.use { output ->
                        output.write(repo.read(file).toByteArray())
                    } ?: throw IllegalStateException("无法打开目标文件")
                }
                notify(getApplication<Application>().getString(R.string.export_success, fileName))
            } catch (e: Exception) {
                notify(
                    getApplication<Application>().getString(
                        R.string.export_failed,
                        e.message ?: "?",
                    ),
                )
            }
        }
    }

    fun rename(oldName: String, newName: String): Boolean =
        repo.rename(oldName, newName).also { if (it) refresh() }

    fun delete(name: String) {
        repo.delete(name)
        refresh()
    }
}

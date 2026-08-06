package com.marknote.app.editor

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.marknote.app.R
import com.marknote.app.data.FileRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/** 保存状态 */
enum class SaveState { LOADING, SAVED, DIRTY, SAVING }

class EditorViewModel(
    app: Application,
    private val file: File,
) : AndroidViewModel(app) {

    private val repo = FileRepository(app)

    private val _content = MutableStateFlow("")
    val content: StateFlow<String> = _content

    private val _saveState = MutableStateFlow(SaveState.SAVED)
    val saveState: StateFlow<SaveState> = _saveState

    private val _loadError = MutableStateFlow<String?>(null)
    val loadError: StateFlow<String?> = _loadError

    fun clearError() {
        _loadError.value = null
    }

    private var saveJob: Job? = null

    init {
        load()
    }

    private fun load() {
        _saveState.value = SaveState.LOADING
        viewModelScope.launch {
            try {
                val text = withContext(Dispatchers.IO) { repo.read(file) }
                _content.value = text
                _saveState.value = SaveState.SAVED
            } catch (e: Exception) {
                _saveState.value = SaveState.DIRTY
                _loadError.value = getApplication<Application>().getString(
                    R.string.error_read_file,
                    e.message ?: "?",
                )
            }
        }
    }

    /** 编辑器内容变化（用户输入） */
    fun onContentChanged(newContent: String) {
        if (_content.value == newContent) return
        _content.value = newContent
        _saveState.value = SaveState.DIRTY
        scheduleAutoSave()
    }

    /** 防抖自动保存 */
    private fun scheduleAutoSave() {
        saveJob?.cancel()
        saveJob = viewModelScope.launch {
            delay(1200)
            save()
        }
    }

    /** 立即保存（后台写盘，不阻塞 UI） */
    fun save() {
        if (_saveState.value == SaveState.SAVED) return
        saveJob?.cancel()
        saveJob = viewModelScope.launch {
            _saveState.value = SaveState.SAVING
            val snapshot = _content.value
            try {
                withContext(Dispatchers.IO) { repo.write(file, snapshot) }
                _saveState.value =
                    if (_content.value == snapshot) SaveState.SAVED else SaveState.DIRTY
            } catch (e: Exception) {
                _saveState.value = SaveState.DIRTY
                _loadError.value = getApplication<Application>().getString(
                    R.string.error_save_file,
                    e.message ?: "?",
                )
            }
        }
    }

    /** 销毁兜底：尽量把内存中的内容写回（小文件，主线程可接受） */
    override fun onCleared() {
        super.onCleared()
        if (_content.value.isEmpty() && _saveState.value != SaveState.DIRTY) return
        try {
            if (_saveState.value == SaveState.DIRTY || _saveState.value == SaveState.SAVING) {
                repo.write(file, _content.value)
            }
        } catch (_: Exception) {
            // 兜底失败不抛崩溃；正常路径的自动保存已覆盖绝大多数场景
        }
    }
}

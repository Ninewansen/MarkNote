package com.marknote.app.sync

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.marknote.app.R
import com.marknote.app.data.FileRepository
import com.marknote.app.data.SyncEngine
import com.marknote.app.data.SyncResult
import com.marknote.app.data.WebDavConfig
import com.marknote.app.util.AppPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed interface SyncUiState {
    data object Idle : SyncUiState
    data class Progress(val message: String) : SyncUiState
    data class Done(val result: SyncResult) : SyncUiState
    data class Failed(val message: String) : SyncUiState
    data class Notice(val message: String) : SyncUiState
}

class SyncViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = FileRepository(app)

    private val _config = MutableStateFlow(AppPreferences.webDavConfig(app))
    val config: StateFlow<WebDavConfig> = _config

    private val _state = MutableStateFlow<SyncUiState>(SyncUiState.Idle)
    val state: StateFlow<SyncUiState> = _state

    private val _lastSyncTime = MutableStateFlow(AppPreferences.lastSyncTime(app))
    val lastSyncTime: StateFlow<Long> = _lastSyncTime

    private var autoSyncStarted = false

    fun updateServerUrl(value: String) {
        _state.value = SyncUiState.Idle
        _config.value = _config.value.copy(serverUrl = value)
    }

    fun updateUsername(value: String) {
        _state.value = SyncUiState.Idle
        _config.value = _config.value.copy(username = value)
    }

    fun updatePassword(value: String) {
        _state.value = SyncUiState.Idle
        _config.value = _config.value.copy(password = value)
    }

    fun updateRemotePath(value: String) {
        _state.value = SyncUiState.Idle
        _config.value = _config.value.copy(remotePath = value)
    }

    fun updateAutoSync(value: Boolean) {
        _config.value = _config.value.copy(autoSync = value)
    }

    /** 打开同步对话框时清掉上一次的失败/成功提示，避免误以为当前配置又报错 */
    fun resetState() {
        _state.value = SyncUiState.Idle
    }

    fun saveConfig() {
        AppPreferences.saveWebDavConfig(getApplication(), _config.value)
    }

    fun setTemporaryStatus(message: String) {
        _state.value = SyncUiState.Notice(message)
    }

    fun runSync() {
        val config = _config.value
        if (config.serverUrl.isBlank()) {
            _state.value = SyncUiState.Failed(getApplication<Application>().getString(R.string.sync_no_server))
            return
        }
        // 常见误填：把邮箱/密码和地址粘在一起（含空格或换行），OkHttp 会报
        // “Invalid URL host”，这里提前给出友好提示。
        if (config.serverUrl.any { it.isWhitespace() }) {
            _state.value = SyncUiState.Failed(
                getApplication<Application>().getString(R.string.sync_invalid_url),
            )
            return
        }
        val trimmed = config.serverUrl.trim()
        if (!trimmed.startsWith("http://") && !trimmed.startsWith("https://")) {
            _state.value = SyncUiState.Failed(
                getApplication<Application>().getString(R.string.sync_invalid_url),
            )
            return
        }
        saveConfig()
        viewModelScope.launch {
            _state.value = SyncUiState.Progress(getApplication<Application>().getString(R.string.sync_connecting))
            try {
                val result = SyncEngine.sync(getApplication(), config, repo.rootDir) { message ->
                    _state.value = SyncUiState.Progress(message)
                }
                AppPreferences.setLastSyncTime(getApplication(), result.finishedAt)
                _lastSyncTime.value = result.finishedAt
                _state.value = SyncUiState.Done(result)
            } catch (e: Exception) {
                Log.e("MarkNoteSync", "sync failed", e)
                _state.value = SyncUiState.Failed(
                    getApplication<Application>().getString(R.string.sync_failed, e.message ?: "?"),
                )
            }
        }
    }

    /** 启动时自动同步：每个进程只触发一次，避免反复进入列表页重复同步 */
    fun maybeAutoSync() {
        val config = _config.value
        if (autoSyncStarted || !config.autoSync || config.serverUrl.isBlank()) return
        autoSyncStarted = true
        runSync()
    }
}

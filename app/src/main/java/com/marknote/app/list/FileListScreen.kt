package com.marknote.app.list

import android.app.Activity
import android.content.ClipData
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Article
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DriveFileRenameOutline
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SaveAlt
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.marknote.app.R
import com.marknote.app.data.MarkdownFile
import com.marknote.app.data.WebDavConfig
import com.marknote.app.editor.VditorPreloader
import com.marknote.app.editor.currentBridgeLanguage
import com.marknote.app.sync.SyncUiState
import com.marknote.app.sync.SyncViewModel
import com.marknote.app.util.AppPreferences
import com.marknote.app.util.LocaleManager
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FileListScreen(
    onOpen: (File) -> Unit,
    viewModel: FileListViewModel = viewModel(),
    syncViewModel: SyncViewModel = viewModel(),
) {
    val filtered by viewModel.filtered.collectAsStateWithLifecycle()
    val query by viewModel.query.collectAsStateWithLifecycle()
    val sort by viewModel.sort.collectAsStateWithLifecycle()
    val event by viewModel.events.collectAsStateWithLifecycle()

    val syncState by syncViewModel.state.collectAsStateWithLifecycle()
    val syncConfig by syncViewModel.config.collectAsStateWithLifecycle()
    val lastSyncTime by syncViewModel.lastSyncTime.collectAsStateWithLifecycle()

    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    // 预热实时编辑器内核（Vditor + Lute），打开文件时不再卡顿
    LaunchedEffect(Unit) {
        val appContext = context.applicationContext
        VditorPreloader.ensure(appContext, currentBridgeLanguage(appContext))
    }

    var showCreateDialog by remember { mutableStateOf(false) }
    var renameTarget by remember { mutableStateOf<MarkdownFile?>(null) }
    var deleteTarget by remember { mutableStateOf<MarkdownFile?>(null) }
    var menuTarget by remember { mutableStateOf<MarkdownFile?>(null) }
    var showTopMenu by remember { mutableStateOf(false) }
    var showLanguageDialog by remember { mutableStateOf(false) }
    var showSyncDialog by remember { mutableStateOf(false) }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri ->
        uri?.let(viewModel::importUri)
    }

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/markdown"),
    ) { uri ->
        val target = menuTarget
        menuTarget = null
        if (uri != null && target != null) {
            viewModel.exportTo(target.name, uri)
        }
    }

    LaunchedEffect(Unit) {
        viewModel.refresh()
        syncViewModel.maybeAutoSync()
    }

    LaunchedEffect(event) {
        event?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.consumeEvent()
        }
    }

    // 同步完成后刷新列表（新下载/新上传的文件立即可见）
    LaunchedEffect(syncState) {
        if (syncState is SyncUiState.Done) {
            viewModel.refresh()
        }
    }

    fun shareFile(file: MarkdownFile) {
        val target = viewModel.toFile(file)
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", target)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/markdown"
            putExtra(Intent.EXTRA_STREAM, uri)
            clipData = ClipData.newUri(context.contentResolver, file.name, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, context.getString(R.string.share_title, file.name)))
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.app_name)) },
                actions = {
                    Box {
                        IconButton(onClick = { showTopMenu = true }) {
                            Icon(Icons.Filled.MoreVert, contentDescription = stringResource(R.string.list_more))
                        }
                        DropdownMenu(
                            expanded = showTopMenu,
                            onDismissRequest = { showTopMenu = false },
                        ) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.list_new_file)) },
                                leadingIcon = { Icon(Icons.Filled.Add, contentDescription = null) },
                                onClick = {
                                    showTopMenu = false
                                    showCreateDialog = true
                                },
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.list_import_file)) },
                                leadingIcon = { Icon(Icons.Filled.FileOpen, contentDescription = null) },
                                onClick = {
                                    showTopMenu = false
                                    importLauncher.launch(
                                        arrayOf(
                                            "text/plain",
                                            "text/markdown",
                                            "application/markdown",
                                            "application/octet-stream",
                                        ),
                                    )
                                },
                            )
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        stringResource(
                                            if (sort == FileSort.RECENT) R.string.list_sort_recent
                                            else R.string.list_sort_name,
                                        ),
                                    )
                                },
                                leadingIcon = { Icon(Icons.AutoMirrored.Filled.Article, contentDescription = null) },
                                onClick = {
                                    showTopMenu = false
                                    viewModel.setSort(
                                        if (sort == FileSort.RECENT) FileSort.NAME else FileSort.RECENT,
                                    )
                                },
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.menu_language)) },
                                leadingIcon = { Icon(Icons.Filled.Language, contentDescription = null) },
                                onClick = {
                                    showTopMenu = false
                                    showLanguageDialog = true
                                },
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.menu_sync)) },
                                leadingIcon = { Icon(Icons.Filled.CloudSync, contentDescription = null) },
                                onClick = {
                                    showTopMenu = false
                                    showSyncDialog = true
                                },
                            )
                        }
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showCreateDialog = true }) {
                Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.list_new_file))
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            OutlinedTextField(
                value = query,
                onValueChange = viewModel::setQuery,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                placeholder = { Text(stringResource(R.string.list_search_hint)) },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                singleLine = true,
            )

            if (filtered.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.AutoMirrored.Filled.Article,
                            contentDescription = null,
                            modifier = Modifier.size(56.dp),
                            tint = MaterialTheme.colorScheme.outlineVariant,
                        )
                        Text(
                            if (query.isBlank()) {
                                stringResource(R.string.list_empty_no_files)
                            } else {
                                stringResource(R.string.list_empty_no_match)
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.outline,
                        )
                        if (query.isBlank()) {
                            TextButton(onClick = { showCreateDialog = true }) {
                                Text(stringResource(R.string.list_create_first))
                            }
                        }
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 4.dp, bottom = 88.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    items(filtered, key = { it.name }) { file ->
                        FileItem(
                            file = file,
                            onClick = { onOpen(viewModel.toFile(file)) },
                            onLongClick = { menuTarget = file },
                            onMenuClick = { menuTarget = file },
                        )
                    }
                }
            }
        }
    }

    // 新建对话框
    if (showCreateDialog) {
        NameDialog(
            title = stringResource(R.string.dialog_new_title),
            initial = context.getString(R.string.default_file_name) + ".md",
            confirmLabel = stringResource(R.string.dialog_create),
            onConfirm = { name ->
                val file = viewModel.create(name)
                if (file != null) {
                    viewModel.refresh()
                    onOpen(file)
                }
                showCreateDialog = false
            },
            onDismiss = { showCreateDialog = false },
        )
    }

    // 重命名对话框
    renameTarget?.let { target ->
        NameDialog(
            title = stringResource(R.string.dialog_rename_title),
            initial = target.name,
            confirmLabel = stringResource(R.string.dialog_ok),
            onConfirm = { newName ->
                viewModel.rename(target.name, newName)
                renameTarget = null
            },
            onDismiss = { renameTarget = null },
        )
    }

    // 删除确认
    deleteTarget?.let { target ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text(stringResource(R.string.dialog_delete_title)) },
            text = { Text(stringResource(R.string.dialog_delete_message, target.name)) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.delete(target.name)
                    deleteTarget = null
                }) {
                    Text(
                        stringResource(R.string.dialog_delete),
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteTarget = null }) {
                    Text(stringResource(R.string.dialog_cancel))
                }
            },
        )
    }

    // 长按 / 更多菜单
    menuTarget?.let { target ->
        DropdownMenu(expanded = true, onDismissRequest = { menuTarget = null }) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.action_open)) },
                leadingIcon = { Icon(Icons.AutoMirrored.Filled.Article, contentDescription = null) },
                onClick = {
                    onOpen(viewModel.toFile(target))
                    menuTarget = null
                },
            )
            DropdownMenuItem(
                text = { Text(stringResource(R.string.action_rename)) },
                leadingIcon = { Icon(Icons.Filled.DriveFileRenameOutline, contentDescription = null) },
                onClick = {
                    renameTarget = target
                    menuTarget = null
                },
            )
            DropdownMenuItem(
                text = { Text(stringResource(R.string.action_share)) },
                leadingIcon = { Icon(Icons.Filled.Share, contentDescription = null) },
                onClick = {
                    shareFile(target)
                    menuTarget = null
                },
            )
            DropdownMenuItem(
                text = { Text(stringResource(R.string.action_export)) },
                leadingIcon = { Icon(Icons.Filled.SaveAlt, contentDescription = null) },
                onClick = {
                    exportLauncher.launch(target.name.toExportName())
                },
            )
            DropdownMenuItem(
                text = { Text(stringResource(R.string.action_delete)) },
                leadingIcon = { Icon(Icons.Filled.Delete, contentDescription = null) },
                onClick = {
                    deleteTarget = target
                    menuTarget = null
                },
            )
        }
    }

    // 语言设置
    if (showLanguageDialog) {
        LanguageDialog(
            current = AppPreferences.language(context),
            onSelect = { code ->
                AppPreferences.setLanguage(context, code)
                (context as? Activity)?.recreate()
            },
            onDismiss = { showLanguageDialog = false },
        )
    }

    // WebDAV 同步
    LaunchedEffect(showSyncDialog) {
        if (showSyncDialog) syncViewModel.resetState()
    }
    if (showSyncDialog) {
        SyncDialog(
            config = syncConfig,
            state = syncState,
            lastSyncTime = lastSyncTime,
            onServerUrl = syncViewModel::updateServerUrl,
            onUsername = syncViewModel::updateUsername,
            onPassword = syncViewModel::updatePassword,
            onRemotePath = syncViewModel::updateRemotePath,
            onAutoSync = syncViewModel::updateAutoSync,
            onSave = {
                syncViewModel.saveConfig()
                syncViewModel.setTemporaryStatus(
                    context.getString(R.string.sync_saved),
                )
            },
            onSync = syncViewModel::runSync,
            onFinish = {
                syncViewModel.saveConfig()
                showSyncDialog = false
            },
            onDismiss = { showSyncDialog = false },
        )
    }
}

@Composable
private fun FileItem(
    file: MarkdownFile,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onMenuClick: () -> Unit,
) {
    val context = LocalContext.current
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onClick, onLongClick = onLongClick),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 14.dp, top = 10.dp, bottom = 10.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Icons.AutoMirrored.Filled.Article,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 12.dp),
            ) {
                Text(
                    file.name,
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (file.excerpt.isNotBlank()) {
                    Text(
                        file.excerpt,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Text(
                    "${formatRelativeTime(context, file.modifiedAt)} · ${formatSize(file.size)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline,
                )
            }
            IconButton(onClick = onMenuClick) {
                Icon(
                    Icons.Filled.MoreVert,
                    contentDescription = stringResource(R.string.list_more),
                    tint = MaterialTheme.colorScheme.outline,
                )
            }
        }
    }
}

@Composable
private fun NameDialog(
    title: String,
    initial: String,
    confirmLabel: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var name by remember { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                singleLine = true,
                label = { Text(stringResource(R.string.dialog_filename_hint)) },
            )
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(name) }) { Text(confirmLabel) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.dialog_cancel)) }
        },
    )
}

@Composable
private fun LanguageDialog(
    current: String,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val options = listOf(
        Triple(AppPreferences.LANG_SYSTEM, stringResource(R.string.language_system), Locale.getDefault().displayName),
        Triple(AppPreferences.LANG_ZH, stringResource(R.string.language_chinese), "简体中文"),
        Triple(AppPreferences.LANG_EN, stringResource(R.string.language_english), "English"),
        Triple(AppPreferences.LANG_FR, stringResource(R.string.language_french), "Français"),
        Triple(AppPreferences.LANG_DE, stringResource(R.string.language_german), "Deutsch"),
        Triple(AppPreferences.LANG_JA, stringResource(R.string.language_japanese), "日本語"),
        Triple(AppPreferences.LANG_ES, stringResource(R.string.language_spanish), "Español"),
    )
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.language_title)) },
        text = {
            Column {
                options.forEach { (code, label, hint) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(code) }
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(selected = current == code, onClick = { onSelect(code) })
                        Column {
                            Text(label)
                            if (hint.isNotBlank()) {
                                Text(
                                    hint,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.outline,
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.dialog_ok)) }
        },
    )
}

@Composable
private fun SyncDialog(
    config: WebDavConfig,
    state: SyncUiState,
    lastSyncTime: Long,
    onServerUrl: (String) -> Unit,
    onUsername: (String) -> Unit,
    onPassword: (String) -> Unit,
    onRemotePath: (String) -> Unit,
    onAutoSync: (Boolean) -> Unit,
    onSave: () -> Unit,
    onSync: () -> Unit,
    onFinish: () -> Unit,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    var showPassword by remember { mutableStateOf(false) }
    val status = when (state) {
        SyncUiState.Idle -> {
            if (lastSyncTime > 0) {
                context.getString(R.string.sync_last_time, formatSyncTime(lastSyncTime))
            } else {
                context.getString(R.string.sync_idle)
            }
        }
        is SyncUiState.Progress -> state.message
        is SyncUiState.Done -> {
            val base = "✓ " + context.getString(
                R.string.sync_done,
                state.result.uploaded,
                state.result.downloaded,
            )
            if (state.result.errors.isNotEmpty()) {
                base + "\n" + state.result.errors.take(3).joinToString("\n")
            } else {
                base
            }
        }
        is SyncUiState.Failed -> state.message
        is SyncUiState.Notice -> state.message
    }
    val statusColor = when (state) {
        is SyncUiState.Failed -> MaterialTheme.colorScheme.error
        is SyncUiState.Done -> if (state.result.hasErrors) {
            MaterialTheme.colorScheme.error
        } else {
            MaterialTheme.colorScheme.primary
        }
        is SyncUiState.Notice -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    val syncing = state is SyncUiState.Progress

    val confirmButton: @Composable () -> Unit = when (state) {
        is SyncUiState.Done -> {
            {
                TextButton(onClick = onFinish) {
                    Text(stringResource(R.string.sync_finish))
                }
            }
        }
        is SyncUiState.Failed -> {
            {
                TextButton(onClick = onSync) {
                    Text(stringResource(R.string.sync_retry))
                }
            }
        }
        is SyncUiState.Progress -> {
            {
                TextButton(onClick = {}, enabled = false) {
                    Text(stringResource(R.string.sync_syncing))
                }
            }
        }
        else -> {
            {
                TextButton(onClick = onSync) {
                    Text(stringResource(R.string.sync_now))
                }
            }
        }
    }

    val dismissButton: @Composable () -> Unit = when (state) {
        is SyncUiState.Done -> {
            {
                TextButton(onClick = onSync) {
                    Text(stringResource(R.string.sync_again))
                }
            }
        }
        is SyncUiState.Progress -> {
            {
                TextButton(onClick = {}, enabled = false) {
                    Text(stringResource(R.string.dialog_cancel))
                }
            }
        }
        else -> {
            {
                TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.dialog_cancel))
                }
            }
        }
    }

    AlertDialog(
        onDismissRequest = {
            if (!syncing) onDismiss()
        },
        title = { Text(stringResource(R.string.sync_title)) },
        text = {
            Column(
                Modifier.verticalScroll(rememberScrollState()),
            ) {
                OutlinedTextField(
                    value = config.serverUrl,
                    onValueChange = onServerUrl,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.sync_server_url)) },
                    placeholder = { Text(stringResource(R.string.sync_server_url_hint)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                )
                OutlinedTextField(
                    value = config.username,
                    onValueChange = onUsername,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    label = { Text(stringResource(R.string.sync_username)) },
                    singleLine = true,
                )
                OutlinedTextField(
                    value = config.password,
                    onValueChange = onPassword,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    label = { Text(stringResource(R.string.sync_password)) },
                    singleLine = true,
                    visualTransformation = if (showPassword) {
                        VisualTransformation.None
                    } else {
                        PasswordVisualTransformation()
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    trailingIcon = {
                        IconButton(onClick = { showPassword = !showPassword }) {
                            Icon(
                                imageVector = if (showPassword) {
                                    Icons.Filled.VisibilityOff
                                } else {
                                    Icons.Filled.Visibility
                                },
                                contentDescription = stringResource(
                                    if (showPassword) R.string.sync_password_hide
                                    else R.string.sync_password_show,
                                ),
                            )
                        }
                    },
                )
                OutlinedTextField(
                    value = config.remotePath,
                    onValueChange = onRemotePath,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    label = { Text(stringResource(R.string.sync_remote_path)) },
                    placeholder = { Text(stringResource(R.string.sync_remote_path_hint)) },
                    singleLine = true,
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        stringResource(R.string.sync_auto),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f),
                    )
                    Switch(
                        checked = config.autoSync,
                        onCheckedChange = onAutoSync,
                    )
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 2.dp),
                    horizontalArrangement = Arrangement.End,
                ) {
                    TextButton(onClick = onSave, enabled = !syncing) {
                        Text(stringResource(R.string.sync_save))
                    }
                }
                Text(
                    stringResource(R.string.sync_https_hint),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.padding(top = 6.dp),
                )
                Text(
                    status,
                    style = MaterialTheme.typography.bodySmall,
                    color = statusColor,
                    modifier = Modifier.padding(top = 10.dp),
                )
            }
        },
        confirmButton = confirmButton,
        dismissButton = dismissButton,
    )
}

/** 导出时保证扩展名为 .md */
private fun String.toExportName(): String {
    val cleaned = trim()
    return if (cleaned.endsWith(".md", ignoreCase = true)) {
        cleaned
    } else {
        cleaned.substringBeforeLast('.', cleaned) + ".md"
    }
}

private fun formatRelativeTime(context: android.content.Context, timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - timestamp
    return when {
        diff < 60_000 -> context.getString(R.string.time_just_now)
        diff < 3_600_000 -> context.getString(R.string.time_minutes_ago, diff / 60_000)
        diff < 86_400_000 -> context.getString(R.string.time_hours_ago, diff / 3_600_000)
        diff < 172_800_000 -> context.getString(R.string.time_yesterday)
        else -> SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(timestamp))
    }
}

private fun formatSyncTime(timestamp: Long): String =
    SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(timestamp))

private fun formatSize(bytes: Long): String = when {
    bytes >= 1024 * 1024 -> "%.1f MB".format(bytes / 1024.0 / 1024.0)
    bytes >= 1024 -> "%.1f KB".format(bytes / 1024.0)
    else -> "$bytes B"
}

package com.marknote.app.editor

import android.view.MotionEvent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FormatBold
import androidx.compose.material.icons.filled.FormatItalic
import androidx.compose.material.icons.filled.FormatListNumbered
import androidx.compose.material.icons.filled.Title
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.automirrored.filled.FormatListBulleted
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.lifecycle.ViewModelProvider
import com.marknote.app.R
import com.marknote.app.data.FileRepository
import io.github.rosemoe.sora.event.ContentChangeEvent
import io.github.rosemoe.sora.event.SelectionChangeEvent
import io.github.rosemoe.sora.widget.CodeEditor
import java.io.File
import kotlinx.coroutines.launch

/** 显示模式：实时渲染编辑（Notion 式）/ 仅编辑源码 / 分屏 / 仅预览 */
enum class EditorMode { LIVE, EDIT, SPLIT, PREVIEW }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorScreen(
    file: File,
    onBack: () -> Unit,
) {
    // 每次进入编辑器都创建新的 ViewModel 会话：返回列表再打开时重新从磁盘加载，
    // 避免复用上一次会话的旧内容（例如文件被 WebDAV 同步更新后）。
    val sessionId = remember { java.util.UUID.randomUUID().toString() }
    val viewModel: EditorViewModel = viewModel(
        key = "$sessionId|${file.absolutePath}",
        factory = viewModelFactory {
            initializer {
                EditorViewModel(
                    app = requireNotNull(this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY]),
                    file = file,
                )
            }
        },
    )
    val content by viewModel.content.collectAsStateWithLifecycle()
    val saveState by viewModel.saveState.collectAsStateWithLifecycle()
    val loadError by viewModel.loadError.collectAsStateWithLifecycle()

    // 每次打开文档都默认进入实时渲染编辑；不跨会话恢复源码/预览模式，
    // 保证“以实时预览为主、源码一键查看”的交互。
    var mode by remember { mutableStateOf(EditorMode.LIVE) }
    var editor by remember { mutableStateOf<CodeEditor?>(null) }
    var liveEditor by remember { mutableStateOf<LiveEditorController?>(null) }
    var slashMenu by remember { mutableStateOf<SlashState?>(null) }
    var showModeMenu by remember { mutableStateOf(false) }
    var showFormatSheet by remember { mutableStateOf(false) }
    val dark = isSystemInDarkTheme()
    val keyboard = LocalSoftwareKeyboardController.current
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    val repo = remember { FileRepository(context.applicationContext) }
    val scope = rememberCoroutineScope()

    // 图片选择器：选择后复制到 Documents/Images/，再以相对路径插入文档
    val imagePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia(),
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        val relative = repo.importImage(uri)
        if (relative == null) {
            scope.launch {
                snackbarHostState.showSnackbar(context.getString(R.string.image_import_failed))
            }
            return@rememberLauncherForActivityResult
        }
        val markdown = "![${relative.substringAfterLast('/')}]($relative)"
        val live = liveEditor
        if (live != null) {
            live.insertMarkdown(markdown)
        } else {
            editor?.let { InsertActions.imageFile(it, relative) }
        }
        scope.launch {
            snackbarHostState.showSnackbar(context.getString(R.string.image_imported))
        }
    }
    val launchImagePicker: () -> Unit = {
        keyboard?.hide()
        imagePicker.launch(
            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
        )
    }

    // 读/写错误提示
    LaunchedEffect(loadError) {
        loadError?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    // 返回：先关斜杠菜单，再保存并返回
    BackHandler {
        if (slashMenu != null) {
            slashMenu = null
        } else {
            viewModel.save()
            onBack()
        }
    }
    DisposableEffect(Unit) {
        onDispose { viewModel.save() }
    }

    // 深色模式同步编辑器主题
    LaunchedEffect(dark) {
        TextMateSetup.applyTheme(dark)
        editor?.invalidate()
    }

    // 模式切换时的焦点/键盘行为
    LaunchedEffect(mode) {
        when (mode) {
            EditorMode.LIVE -> {
                // 从源码/预览切回实时模式时重建 WebView 布局高度（防白屏）
                liveEditor?.resize()
                liveEditor?.focus()
            }
            EditorMode.EDIT -> {
                editor?.requestFocus()
                keyboard?.show()
            }
            EditorMode.PREVIEW -> keyboard?.hide()
            EditorMode.SPLIT -> Unit
        }
        if (mode == EditorMode.PREVIEW) {
            slashMenu = null
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(file.name, maxLines = 1)
                        Text(
                            when (saveState) {
                                SaveState.LOADING -> stringResource(R.string.save_loading)
                                SaveState.SAVED -> stringResource(R.string.save_saved)
                                SaveState.DIRTY -> stringResource(R.string.save_dirty)
                                SaveState.SAVING -> stringResource(R.string.save_saving)
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = if (saveState == SaveState.SAVED) {
                                MaterialTheme.colorScheme.outline
                            } else {
                                MaterialTheme.colorScheme.primary
                            },
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = {
                        viewModel.save()
                        onBack()
                    }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.editor_back),
                        )
                    }
                },
                actions = {
                    // 一个按键切换：实时编辑 <-> 显示源码；分屏 <-> 只看预览
                    when (mode) {
                        EditorMode.LIVE -> IconButton(onClick = { mode = EditorMode.EDIT }) {
                            Icon(
                                Icons.Filled.Code,
                                contentDescription = stringResource(R.string.editor_show_source),
                            )
                        }
                        EditorMode.EDIT -> IconButton(onClick = { mode = EditorMode.LIVE }) {
                            Icon(
                                Icons.Filled.Edit,
                                contentDescription = stringResource(R.string.editor_live_preview),
                            )
                        }
                        EditorMode.SPLIT -> IconButton(onClick = { mode = EditorMode.PREVIEW }) {
                            Icon(
                                Icons.Filled.Visibility,
                                contentDescription = stringResource(R.string.editor_show_preview),
                            )
                        }
                        EditorMode.PREVIEW -> IconButton(onClick = { mode = EditorMode.LIVE }) {
                            Icon(
                                Icons.Filled.Edit,
                                contentDescription = stringResource(R.string.editor_live_preview),
                            )
                        }
                    }
                    IconButton(onClick = { viewModel.save() }) {
                        Icon(Icons.Filled.Save, contentDescription = stringResource(R.string.editor_save))
                    }
                    Box {
                        IconButton(onClick = { showModeMenu = true }) {
                            Icon(Icons.Filled.MoreVert, contentDescription = stringResource(R.string.list_more))
                        }
                        DropdownMenu(
                            expanded = showModeMenu,
                            onDismissRequest = { showModeMenu = false },
                        ) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.editor_live_preview)) },
                                onClick = {
                                    showModeMenu = false
                                    mode = EditorMode.LIVE
                                },
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.editor_edit_only)) },
                                onClick = {
                                    showModeMenu = false
                                    mode = EditorMode.EDIT
                                },
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.editor_split)) },
                                onClick = {
                                    showModeMenu = false
                                    mode = EditorMode.SPLIT
                                },
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.editor_preview_only)) },
                                onClick = {
                                    showModeMenu = false
                                    mode = EditorMode.PREVIEW
                                },
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.editor_save)) },
                                onClick = {
                                    showModeMenu = false
                                    viewModel.save()
                                },
                            )
                        }
                    }
                },
            )
        },
        bottomBar = {
            if (mode == EditorMode.LIVE) {
                LiveFormatToolbar(
                    live = liveEditor,
                    onMoreClick = {
                        keyboard?.hide()
                        showFormatSheet = true
                    },
                )
            } else if (mode != EditorMode.PREVIEW) {
                FormatToolbar(
                    editor = editor,
                    onMoreClick = {
                        keyboard?.hide()
                        showFormatSheet = true
                    },
                )
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
            Column(
                Modifier
                    .fillMaxSize()
                    .padding(padding),
            ) {
                // Notion/Typora 式实时渲染编辑：面板常驻组合树，切换模式只改显隐，
                // 不销毁重建 WebView，避免真机上“切回实时预览”白屏/内容丢失。
                VditorPane(
                    content = content,
                    dark = dark,
                    visible = mode == EditorMode.LIVE,
                    onReady = { },
                    onContentChanged = viewModel::onContentChanged,
                    onControllerCreated = { liveEditor = it },
                    onPickImage = launchImagePicker,
                    modifier = if (mode == EditorMode.LIVE) {
                        Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    } else {
                        Modifier
                            .fillMaxWidth()
                            .height(0.dp)
                    },
                )
                if (mode != EditorMode.LIVE) {
                    // 源码编辑与预览面板始终保留在组合树中：切换模式不丢光标与撤销栈。
                    EditorPane(
                        content = content,
                        onEditorCreated = { editor = it },
                        onContentChanged = viewModel::onContentChanged,
                        onSlashChanged = { slashMenu = it },
                        modifier = if (mode == EditorMode.SPLIT || mode == EditorMode.EDIT) {
                            Modifier
                                .fillMaxWidth()
                                .weight(1f)
                        } else {
                            Modifier
                                .fillMaxWidth()
                                .height(0.dp)
                        },
                    )
                    if (mode == EditorMode.SPLIT) {
                        HorizontalDivider()
                    }
                    MarkdownPreview(
                        markdown = content,
                        dark = dark,
                        baseDir = file.parentFile,
                        modifier = if (mode == EditorMode.SPLIT || mode == EditorMode.PREVIEW) {
                            Modifier
                                .fillMaxWidth()
                                .weight(1f)
                        } else {
                            Modifier
                                .fillMaxWidth()
                                .height(0.dp)
                        },
                    )
                    if (mode == EditorMode.EDIT || mode == EditorMode.SPLIT) {
                        slashMenu?.let { state ->
                            SlashCommandPanel(
                                query = state.query,
                                onSelect = { command ->
                                    if (command.id == "image") {
                                        // 源码模式下选“图片”：先移除 /query，再打开系统图片选择器
                                        editor?.let { SlashCommands.apply(it, state) {} }
                                        slashMenu = null
                                        launchImagePicker()
                                    } else {
                                        editor?.let { editor ->
                                            SlashCommands.apply(editor, state, command.action)
                                            slashMenu = null
                                        }
                                    }
                                },
                            )
                        }
                    }
                }
            }
        }

    // 更多格式：底部面板（与 / 快捷菜单同一套命令）
    if (showFormatSheet) {
        FormatSheet(
            editor = editor,
            live = liveEditor,
            onImageClick = launchImagePicker,
            onDismiss = { showFormatSheet = false },
        )
    }
}

// ---------- 编辑区 ----------

@Composable
private fun EditorPane(
    content: String,
    onEditorCreated: (CodeEditor) -> Unit,
    onContentChanged: (String) -> Unit,
    onSlashChanged: (SlashState?) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                val editor = TextMateSetup.createMarkdownEditor(ctx)
                editor.setText(content)
                // Compose AndroidView 下触摸不会自动聚焦，DOWN 时手动请求焦点
                editor.setOnTouchListener { v, e ->
                    if (e.actionMasked == MotionEvent.ACTION_DOWN) {
                        v.requestFocus()
                    }
                    false
                }
                editor.subscribeEvent(ContentChangeEvent::class.java) { _, _ ->
                    onContentChanged(editor.text.toString())
                    onSlashChanged(SlashCommands.computeState(editor))
                }
                editor.subscribeEvent(SelectionChangeEvent::class.java) { _, _ ->
                    onSlashChanged(SlashCommands.computeState(editor))
                }
                onEditorCreated(editor)
                editor
            },
            update = { editor ->
                if (editor.text.toString() != content) {
                    editor.setText(content)
                }
            },
        )
    }
}

// ---------- 斜杠快捷菜单（Notion 风格） ----------

@Composable
private fun SlashCommandPanel(
    query: String,
    onSelect: (SlashCommand) -> Unit,
) {
    val commands = remember(query) { SlashCommands.all.filter { it.matches(query) } }
    Surface(
        tonalElevation = 6.dp,
        shadowElevation = 4.dp,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 300.dp),
    ) {
        if (commands.isEmpty()) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
            ) {
                Text(
                    stringResource(R.string.slash_no_match),
                    color = MaterialTheme.colorScheme.outline,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxWidth()) {
                items(commands, key = { it.id }) { command ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(command) }
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            command.icon,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                        )
                        Text(
                            stringResource(command.labelRes),
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.padding(start = 12.dp),
                        )
                    }
                }
            }
        }
    }
}

// ---------- 格式工具栏（简版：固定宽度，不横滑；其余格式进底部面板） ----------

@Composable
private fun LiveFormatToolbar(
    live: LiveEditorController?,
    onMoreClick: () -> Unit,
) {
    // imePadding：输入法弹起时工具栏跟随键盘上移，而不是被键盘遮挡
    Surface(
        tonalElevation = 3.dp,
        modifier = Modifier.imePadding(),
    ) {
        val enabled = live != null
        var showHeading by remember { mutableStateOf(false) }
        var showList by remember { mutableStateOf(false) }
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = { live?.undo() }, enabled = enabled) {
                Icon(
                    Icons.AutoMirrored.Filled.Undo,
                    contentDescription = stringResource(R.string.toolbar_undo),
                )
            }
            IconButton(onClick = { live?.redo() }, enabled = enabled) {
                Icon(
                    Icons.AutoMirrored.Filled.Redo,
                    contentDescription = stringResource(R.string.toolbar_redo),
                )
            }
            ToolbarDivider()
            IconButton(onClick = { live?.execCommand("bold") }, enabled = enabled) {
                Icon(
                    Icons.Filled.FormatBold,
                    contentDescription = stringResource(R.string.toolbar_bold),
                )
            }
            IconButton(onClick = { live?.execCommand("italic") }, enabled = enabled) {
                Icon(
                    Icons.Filled.FormatItalic,
                    contentDescription = stringResource(R.string.toolbar_italic),
                )
            }
            Box {
                IconButton(onClick = { showHeading = true }, enabled = enabled) {
                    Icon(
                        Icons.Filled.Title,
                        contentDescription = stringResource(R.string.toolbar_heading),
                    )
                }
                DropdownMenu(
                    expanded = showHeading,
                    onDismissRequest = { showHeading = false },
                ) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.menu_heading_1)) },
                        onClick = {
                            showHeading = false
                            live?.heading(1)
                        },
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.menu_heading_2)) },
                        onClick = {
                            showHeading = false
                            live?.heading(2)
                        },
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.menu_heading_3)) },
                        onClick = {
                            showHeading = false
                            live?.heading(3)
                        },
                    )
                }
            }
            Box {
                IconButton(onClick = { showList = true }, enabled = enabled) {
                    Icon(
                        Icons.AutoMirrored.Filled.FormatListBulleted,
                        contentDescription = stringResource(R.string.toolbar_bullet_list),
                    )
                }
                DropdownMenu(
                    expanded = showList,
                    onDismissRequest = { showList = false },
                ) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.toolbar_bullet_list)) },
                        leadingIcon = {
                            Icon(Icons.AutoMirrored.Filled.FormatListBulleted, contentDescription = null)
                        },
                        onClick = {
                            showList = false
                            live?.execCommand("list")
                        },
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.toolbar_ordered_list)) },
                        leadingIcon = {
                            Icon(Icons.Filled.FormatListNumbered, contentDescription = null)
                        },
                        onClick = {
                            showList = false
                            live?.execCommand("ordered-list")
                        },
                    )
                }
            }
            Spacer(Modifier.weight(1f))
            IconButton(onClick = onMoreClick, enabled = enabled) {
                Icon(
                    Icons.Filled.AddCircle,
                    contentDescription = stringResource(R.string.toolbar_more),
                    modifier = Modifier.size(32.dp),
                )
            }
        }
    }
}

@Composable
private fun FormatToolbar(
    editor: CodeEditor?,
    onMoreClick: () -> Unit,
) {
    Surface(
        tonalElevation = 3.dp,
        modifier = Modifier.imePadding(),
    ) {
        val enabled = editor != null
        var showHeading by remember { mutableStateOf(false) }
        var showList by remember { mutableStateOf(false) }
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = { editor?.undo() }, enabled = enabled) {
                Icon(
                    Icons.AutoMirrored.Filled.Undo,
                    contentDescription = stringResource(R.string.toolbar_undo),
                )
            }
            IconButton(onClick = { editor?.redo() }, enabled = enabled) {
                Icon(
                    Icons.AutoMirrored.Filled.Redo,
                    contentDescription = stringResource(R.string.toolbar_redo),
                )
            }
            ToolbarDivider()
            IconButton(onClick = { editor?.let(InsertActions::bold) }, enabled = enabled) {
                Icon(
                    Icons.Filled.FormatBold,
                    contentDescription = stringResource(R.string.toolbar_bold),
                )
            }
            IconButton(onClick = { editor?.let(InsertActions::italic) }, enabled = enabled) {
                Icon(
                    Icons.Filled.FormatItalic,
                    contentDescription = stringResource(R.string.toolbar_italic),
                )
            }
            Box {
                IconButton(onClick = { showHeading = true }, enabled = enabled) {
                    Icon(
                        Icons.Filled.Title,
                        contentDescription = stringResource(R.string.toolbar_heading),
                    )
                }
                DropdownMenu(
                    expanded = showHeading,
                    onDismissRequest = { showHeading = false },
                ) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.menu_heading_1)) },
                        onClick = {
                            showHeading = false
                            editor?.let { InsertActions.heading(it, 1) }
                        },
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.menu_heading_2)) },
                        onClick = {
                            showHeading = false
                            editor?.let { InsertActions.heading(it, 2) }
                        },
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.menu_heading_3)) },
                        onClick = {
                            showHeading = false
                            editor?.let { InsertActions.heading(it, 3) }
                        },
                    )
                }
            }
            Box {
                IconButton(onClick = { showList = true }, enabled = enabled) {
                    Icon(
                        Icons.AutoMirrored.Filled.FormatListBulleted,
                        contentDescription = stringResource(R.string.toolbar_bullet_list),
                    )
                }
                DropdownMenu(
                    expanded = showList,
                    onDismissRequest = { showList = false },
                ) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.toolbar_bullet_list)) },
                        leadingIcon = {
                            Icon(Icons.AutoMirrored.Filled.FormatListBulleted, contentDescription = null)
                        },
                        onClick = {
                            showList = false
                            editor?.let(InsertActions::bulletList)
                        },
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.toolbar_ordered_list)) },
                        leadingIcon = {
                            Icon(Icons.Filled.FormatListNumbered, contentDescription = null)
                        },
                        onClick = {
                            showList = false
                            editor?.let(InsertActions::orderedList)
                        },
                    )
                }
            }
            Spacer(Modifier.weight(1f))
            IconButton(onClick = onMoreClick, enabled = enabled) {
                Icon(
                    Icons.Filled.AddCircle,
                    contentDescription = stringResource(R.string.toolbar_more),
                    modifier = Modifier.size(32.dp),
                )
            }
        }
    }
}

// ---------- 更多格式面板（底部弹出，与 / 快捷菜单同一套命令） ----------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FormatSheet(
    editor: CodeEditor?,
    live: LiveEditorController?,
    onImageClick: () -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 460.dp),
        ) {
            items(SlashCommands.all, key = { it.id }) { command ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            if (command.id == "image") {
                                onImageClick()
                            } else if (live != null) {
                                LiveActions.apply(command, live)
                            } else {
                                editor?.let(command.action)
                            }
                            onDismiss()
                        }
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        command.icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                    )
                    Text(
                        stringResource(command.labelRes),
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(start = 16.dp),
                    )
                }
            }
            item {
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun ToolbarDivider() {
    Box(
        Modifier
            .padding(horizontal = 2.dp)
            .size(width = 1.dp, height = 24.dp)
            .background(MaterialTheme.colorScheme.outlineVariant),
    )
}

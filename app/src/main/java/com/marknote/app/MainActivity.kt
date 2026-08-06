package com.marknote.app

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.marknote.app.editor.EditorScreen
import com.marknote.app.list.FileListScreen
import com.marknote.app.ui.theme.MarkNoteTheme
import com.marknote.app.util.LocaleManager
import java.io.File

class MainActivity : ComponentActivity() {

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LocaleManager.wrap(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MarkNoteTheme {
                AppNav()
            }
        }
    }
}

@Composable
private fun AppNav() {
    // 当前打开的文档（null = 列表页）
    var openedPath by rememberSaveable { mutableStateOf<String?>(null) }
    val openedFile = openedPath?.let { File(it) }

    if (openedFile == null) {
        FileListScreen(
            onOpen = { openedPath = it.absolutePath },
        )
    } else {
        EditorScreen(
            file = openedFile,
            onBack = { openedPath = null },
        )
    }
}

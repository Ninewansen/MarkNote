package com.marknote.app

import android.app.Application
import com.marknote.app.editor.TextMateSetup

class MarkNoteApp : Application() {
    override fun onCreate() {
        super.onCreate()
        TextMateSetup.init(this)
    }
}

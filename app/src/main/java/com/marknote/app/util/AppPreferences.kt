package com.marknote.app.util

import android.content.Context
import android.content.SharedPreferences
import com.marknote.app.data.WebDavConfig

/** 轻量本地设置存储（语言、WebDAV 配置、上次同步时间） */
object AppPreferences {

    private const val NAME = "marknote_prefs"

    const val LANG_SYSTEM = "system"
    const val LANG_ZH = "zh"
    const val LANG_EN = "en"
    const val LANG_FR = "fr"
    const val LANG_DE = "de"
    const val LANG_JA = "ja"
    const val LANG_ES = "es"

    private fun prefs(context: Context): SharedPreferences =
        context.applicationContext.getSharedPreferences(NAME, Context.MODE_PRIVATE)

    // ---------- 语言 ----------

    fun language(context: Context): String =
        prefs(context).getString("language", LANG_SYSTEM) ?: LANG_SYSTEM

    fun setLanguage(context: Context, language: String) {
        prefs(context).edit().putString("language", language).apply()
    }

    // ---------- WebDAV ----------

    fun webDavConfig(context: Context): WebDavConfig = with(prefs(context)) {
        WebDavConfig(
            serverUrl = getString("webdav_server", "") ?: "",
            username = getString("webdav_username", "") ?: "",
            password = getString("webdav_password", "") ?: "",
            remotePath = getString("webdav_path", "MarkNote") ?: "MarkNote",
            autoSync = getBoolean("webdav_auto_sync", false),
        )
    }

    fun saveWebDavConfig(context: Context, config: WebDavConfig) {
        prefs(context).edit()
            .putString("webdav_server", config.serverUrl)
            .putString("webdav_username", config.username)
            .putString("webdav_password", config.password)
            .putString("webdav_path", config.remotePath)
            .putBoolean("webdav_auto_sync", config.autoSync)
            .apply()
    }

    fun lastSyncTime(context: Context): Long =
        prefs(context).getLong("webdav_last_sync", 0L)

    fun setLastSyncTime(context: Context, time: Long) {
        prefs(context).edit().putLong("webdav_last_sync", time).apply()
    }
}

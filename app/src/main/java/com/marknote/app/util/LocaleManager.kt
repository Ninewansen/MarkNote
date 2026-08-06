package com.marknote.app.util

import android.content.Context
import android.content.res.Configuration
import java.util.Locale

/** 应用内语言切换：把选中的 Locale 应用到 Activity 的 base context */
object LocaleManager {

    fun wrap(base: Context): Context {
        val locale = when (AppPreferences.language(base)) {
            AppPreferences.LANG_ZH -> Locale.SIMPLIFIED_CHINESE
            AppPreferences.LANG_EN -> Locale.ENGLISH
            AppPreferences.LANG_FR -> Locale.FRENCH
            AppPreferences.LANG_DE -> Locale.GERMAN
            AppPreferences.LANG_JA -> Locale.JAPANESE
            AppPreferences.LANG_ES -> Locale("es")
            else -> Locale.getDefault()
        }
        val configuration = Configuration(base.resources.configuration)
        configuration.setLocale(locale)
        return base.createConfigurationContext(configuration)
    }
}

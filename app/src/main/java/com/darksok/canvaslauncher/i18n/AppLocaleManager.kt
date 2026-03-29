package com.darksok.canvaslauncher.i18n

import android.content.Context
import android.content.SharedPreferences
import android.content.res.Configuration
import android.content.res.Resources
import android.os.Build
import android.os.LocaleList
import java.util.Locale

object AppLocaleManager {

    fun wrapContext(base: Context): Context {
        val language = readPreferredLanguage(base)
        val localeTag = language.localeTag ?: return base
        return applyLocale(base, localeTag)
    }

    fun readPreferredLanguage(context: Context): AppLanguage {
        return AppLanguage.fromStoredValue(
            preferences(context).getString(KEY_LANGUAGE, AppLanguage.SYSTEM.storageValue),
        )
    }

    fun updatePreferredLanguage(
        context: Context,
        language: AppLanguage,
    ): Boolean {
        val prefs = preferences(context)
        val current = AppLanguage.fromStoredValue(
            prefs.getString(KEY_LANGUAGE, AppLanguage.SYSTEM.storageValue),
        )
        if (current == language) return false
        prefs.edit()
            .putString(KEY_LANGUAGE, language.storageValue)
            .apply()
        return true
    }

    fun shouldRecreateForPreferredLanguage(context: Context): Boolean {
        val preferred = readPreferredLanguage(context)
        val currentLanguage = currentLocale(context).language
        val expectedLanguage = when (preferred) {
            AppLanguage.SYSTEM -> systemLocale().language
            else -> Locale.forLanguageTag(preferred.localeTag.orEmpty()).language
        }
        return currentLanguage != expectedLanguage
    }

    private fun preferences(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
    }

    private fun applyLocale(
        context: Context,
        localeTag: String,
    ): Context {
        val locale = Locale.forLanguageTag(localeTag)
        Locale.setDefault(locale)
        val configuration = Configuration(context.resources.configuration)
        configuration.setLocale(locale)
        configuration.setLayoutDirection(locale)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            configuration.setLocales(LocaleList(locale))
        }
        return context.createConfigurationContext(configuration)
    }

    private fun currentLocale(context: Context): Locale {
        val configuration = context.resources.configuration
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            configuration.locales.get(0)
        } else {
            @Suppress("DEPRECATION")
            configuration.locale
        }
    }

    private fun systemLocale(): Locale {
        val configuration = Resources.getSystem().configuration
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            configuration.locales.get(0)
        } else {
            @Suppress("DEPRECATION")
            configuration.locale
        }
    }

    private const val PREFERENCES_NAME = "canvas_launcher_locale"
    private const val KEY_LANGUAGE = "language"
}

package com.darksok.canvaslauncher.i18n

import android.app.Application
import android.content.Context
import android.content.res.Configuration
import android.os.Build
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import java.util.Locale
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class AppLocaleManagerTest {

    private val app: Application = ApplicationProvider.getApplicationContext()

    @Before
    fun resetPreference() {
        preferences(app).edit().clear().commit()
        Locale.setDefault(Locale.ENGLISH)
    }

    @Test
    fun `update preferred language stores value and avoids duplicate writes`() {
        assertThat(AppLocaleManager.readPreferredLanguage(app)).isEqualTo(AppLanguage.SYSTEM)

        val changedToRussian = AppLocaleManager.updatePreferredLanguage(app, AppLanguage.RUSSIAN)
        val changedAgain = AppLocaleManager.updatePreferredLanguage(app, AppLanguage.RUSSIAN)

        assertThat(changedToRussian).isTrue()
        assertThat(changedAgain).isFalse()
        assertThat(AppLocaleManager.readPreferredLanguage(app)).isEqualTo(AppLanguage.RUSSIAN)
    }

    @Test
    fun `wrap context applies stored locale while system keeps original context`() {
        AppLocaleManager.updatePreferredLanguage(app, AppLanguage.SYSTEM)
        val systemWrapped = AppLocaleManager.wrapContext(app)
        assertThat(systemWrapped).isSameInstanceAs(app)

        AppLocaleManager.updatePreferredLanguage(app, AppLanguage.GERMAN)
        val wrapped = AppLocaleManager.wrapContext(app)
        assertThat(localeOf(wrapped).language).isEqualTo(Locale.GERMAN.language)
    }

    @Test
    fun `should recreate compares current and preferred languages`() {
        AppLocaleManager.updatePreferredLanguage(app, AppLanguage.RUSSIAN)
        val russianContext = localizedContext(app, Locale("ru"))
        val englishContext = localizedContext(app, Locale.ENGLISH)

        assertThat(AppLocaleManager.shouldRecreateForPreferredLanguage(russianContext)).isFalse()
        assertThat(AppLocaleManager.shouldRecreateForPreferredLanguage(englishContext)).isTrue()
    }

    private fun localeOf(context: Context): Locale {
        val configuration = context.resources.configuration
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            configuration.locales[0]
        } else {
            @Suppress("DEPRECATION")
            configuration.locale
        }
    }

    private fun localizedContext(base: Context, locale: Locale): Context {
        val configuration = Configuration(base.resources.configuration)
        configuration.setLocale(locale)
        return base.createConfigurationContext(configuration)
    }

    private fun preferences(context: Context) =
        context.getSharedPreferences("canvas_launcher_locale", Context.MODE_PRIVATE)
}

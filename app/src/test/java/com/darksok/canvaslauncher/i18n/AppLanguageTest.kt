package com.darksok.canvaslauncher.i18n

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class AppLanguageTest {

    @Test
    fun `null stored value falls back to system`() {
        assertThat(AppLanguage.fromStoredValue(null)).isEqualTo(AppLanguage.SYSTEM)
    }

    @Test
    fun `unknown stored value falls back to system`() {
        assertThat(AppLanguage.fromStoredValue("unknown")).isEqualTo(AppLanguage.SYSTEM)
    }

    @Test
    fun `empty stored value falls back to system`() {
        assertThat(AppLanguage.fromStoredValue("")).isEqualTo(AppLanguage.SYSTEM)
    }

    @Test
    fun `system storage value resolves to system`() {
        assertThat(AppLanguage.fromStoredValue("system")).isEqualTo(AppLanguage.SYSTEM)
    }

    @Test
    fun `english storage value resolves to english`() {
        assertThat(AppLanguage.fromStoredValue("en")).isEqualTo(AppLanguage.ENGLISH)
    }

    @Test
    fun `russian storage value resolves to russian`() {
        assertThat(AppLanguage.fromStoredValue("ru")).isEqualTo(AppLanguage.RUSSIAN)
    }

    @Test
    fun `spanish storage value resolves to spanish`() {
        assertThat(AppLanguage.fromStoredValue("es")).isEqualTo(AppLanguage.SPANISH)
    }

    @Test
    fun `german storage value resolves to german`() {
        assertThat(AppLanguage.fromStoredValue("de")).isEqualTo(AppLanguage.GERMAN)
    }

    @Test
    fun `french storage value resolves to french`() {
        assertThat(AppLanguage.fromStoredValue("fr")).isEqualTo(AppLanguage.FRENCH)
    }

    @Test
    fun `portuguese brazil storage value resolves to portuguese brazil`() {
        assertThat(AppLanguage.fromStoredValue("pt-BR")).isEqualTo(AppLanguage.PORTUGUESE_BRAZIL)
    }

    @Test
    fun `storage values are unique`() {
        assertThat(AppLanguage.entries.map { it.storageValue }.distinct()).hasSize(AppLanguage.entries.size)
    }

    @Test
    fun `non system languages expose locale tag`() {
        assertThat(AppLanguage.entries.filter { it != AppLanguage.SYSTEM }.all { it.localeTag != null }).isTrue()
    }

    @Test
    fun `system language exposes null locale tag`() {
        assertThat(AppLanguage.SYSTEM.localeTag).isNull()
    }

    @Test
    fun `stored value lookup is case sensitive`() {
        assertThat(AppLanguage.fromStoredValue("EN")).isEqualTo(AppLanguage.SYSTEM)
    }
}

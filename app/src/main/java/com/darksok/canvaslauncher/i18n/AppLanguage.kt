package com.darksok.canvaslauncher.i18n

enum class AppLanguage(
    val storageValue: String,
    val localeTag: String?,
) {
    SYSTEM(
        storageValue = "system",
        localeTag = null,
    ),
    ENGLISH(
        storageValue = "en",
        localeTag = "en",
    ),
    RUSSIAN(
        storageValue = "ru",
        localeTag = "ru",
    ),
    SPANISH(
        storageValue = "es",
        localeTag = "es",
    ),
    GERMAN(
        storageValue = "de",
        localeTag = "de",
    ),
    FRENCH(
        storageValue = "fr",
        localeTag = "fr",
    ),
    PORTUGUESE_BRAZIL(
        storageValue = "pt-BR",
        localeTag = "pt-BR",
    ),
    ;

    companion object {
        fun fromStoredValue(value: String?): AppLanguage {
            return entries.firstOrNull { language -> language.storageValue == value } ?: SYSTEM
        }
    }
}

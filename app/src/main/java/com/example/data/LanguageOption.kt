package com.example.data

import com.google.mlkit.nl.translate.TranslateLanguage
import java.util.Locale

data class LanguageOption(
    val name: String,
    val flag: String,
    val mlKitCode: String
)

val englishOption = LanguageOption("English", "\uD83C\uDDFA\uD83C\uDDF8", TranslateLanguage.ENGLISH)

val supportedLanguages = listOf(
    LanguageOption("Spanish", "\uD83C\uDDEA\uD83C\uDDF8", TranslateLanguage.SPANISH),
    LanguageOption("French", "\uD83C\uDDEB\uD83C\uDDF7", TranslateLanguage.FRENCH),
    LanguageOption("German", "\uD83C\uDDE9\uD83C\uDDEA", TranslateLanguage.GERMAN),
    LanguageOption("Italian", "\uD83C\uDDEE\uD83C\uDDF9", TranslateLanguage.ITALIAN),
    LanguageOption("Chinese", "\uD83C\uDDE8\uD83C\uDDF3", TranslateLanguage.CHINESE),
    LanguageOption("Japanese", "\uD83C\uDDEF\uD83C\uDDF5", TranslateLanguage.JAPANESE),
    LanguageOption("Arabic", "\uD83C\uDDF8\uD83C\uDDE6", TranslateLanguage.ARABIC),
    LanguageOption("Hindi", "\uD83C\uDDEE\uD83C\uDDF3", TranslateLanguage.HINDI),
    LanguageOption("Korean", "\uD83C\uDDF0\uD83C\uDDF7", TranslateLanguage.KOREAN),
    LanguageOption("Russian", "\uD83C\uDDF7\uD83C\uDDFA", TranslateLanguage.RUSSIAN),
    LanguageOption("Portuguese", "\uD83C\uDDF5\uD83C\uDDF9", TranslateLanguage.PORTUGUESE),
    LanguageOption("Dutch", "\uD83C\uDDF3\uD83C\uDDF1", TranslateLanguage.DUTCH)
)

fun getLanguageByCode(code: String): LanguageOption? {
    if (code.equals(TranslateLanguage.ENGLISH, ignoreCase = true)) return englishOption
    return supportedLanguages.firstOrNull { it.mlKitCode.equals(code, ignoreCase = true) }
}

fun getLocaleForLanguageCode(code: String): Locale {
    return when (code.lowercase()) {
        "es" -> Locale("es")
        "fr" -> Locale.FRENCH
        "de" -> Locale.GERMAN
        "it" -> Locale.ITALIAN
        "zh" -> Locale.CHINESE
        "ja" -> Locale.JAPANESE
        "ar" -> Locale("ar")
        "hi" -> Locale("hi", "IN")
        "ko" -> Locale.KOREAN
        "ru" -> Locale("ru")
        "pt" -> Locale("pt")
        "nl" -> Locale("nl")
        "en" -> Locale.ENGLISH
        else -> Locale(code)
    }
}

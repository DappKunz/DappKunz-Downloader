package com.downloader.app.core.util

import android.content.Context
import android.content.res.Configuration
import com.downloader.app.core.storage.PrefsRepository
import java.util.Locale

object LocaleHelper {

    const val LANG_INDONESIAN = "in"
    const val LANG_ENGLISH = "en"

    @Volatile
    private var cachedLang: String? = null

    @Volatile
    private var cachedContext: Context? = null

    fun normalize(tag: String?): String =
        if (tag?.lowercase(Locale.ROOT) == LANG_ENGLISH) LANG_ENGLISH else LANG_INDONESIAN

    fun currentTag(): String = normalize(PrefsRepository.language.value)

    fun localeFor(tag: String): Locale = Locale(tag)

    fun wrap(context: Context): Context {
        val lang = currentTag()
        cachedContext?.let { if (cachedLang == lang) return it }
        val locale = localeFor(lang)
        Locale.setDefault(locale)
        val config = Configuration(context.resources.configuration)
        config.setLocale(locale)
        config.setLayoutDirection(locale)
        val wrapped = context.createConfigurationContext(config)
        cachedLang = lang
        cachedContext = wrapped
        return wrapped
    }

    fun invalidate() {
        cachedLang = null
        cachedContext = null
    }
}

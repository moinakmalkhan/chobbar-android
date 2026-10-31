package shop.chobbar.app.i18n

import android.app.LocaleManager
import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.os.LocaleList
import shop.chobbar.app.data.local.LanguageStore
import java.util.Locale

/**
 * Per-app language without AppCompat:
 *  - Android 13+: the system LocaleManager (also shows in system Settings › App languages),
 *  - older: wrap the Activity context in attachBaseContext.
 * First launch asks the API (/locale/) which uses the visitor's country (CDN IP header) → ur for Pakistan, ar for Arab countries.
 */
object AppLocale {
    val SUPPORTED = listOf("en", "ur", "ar")

    fun current(store: LanguageStore): String = store.language ?: deviceDefault()

    fun deviceDefault(): String {
        val code = Locale.getDefault().language
        return if (code in SUPPORTED) code else "en"
    }

    fun isRtl(code: String) = code == "ur" || code == "ar"

    fun wrap(base: Context, store: LanguageStore): Context {
        if (Build.VERSION.SDK_INT >= 33) return base
        val locale = Locale(current(store))
        Locale.setDefault(locale)
        val config = Configuration(base.resources.configuration)
        config.setLocale(locale)
        config.setLayoutDirection(locale)
        return base.createConfigurationContext(config)
    }

    /** Applies and persists a language. Returns true when the Activity must be recreated (pre-13). */
    fun apply(context: Context, store: LanguageStore, code: String): Boolean {
        store.language = code
        store.confirmed = true
        return if (Build.VERSION.SDK_INT >= 33) {
            context.getSystemService(LocaleManager::class.java).applicationLocales = LocaleList.forLanguageTags(code)
            false
        } else {
            true
        }
    }

    /** On 13+ the system may hold a choice made in Settings; mirror it into our store. */
    fun syncFromSystem(context: Context, store: LanguageStore) {
        if (Build.VERSION.SDK_INT < 33) return
        val tags = context.getSystemService(LocaleManager::class.java).applicationLocales
        if (!tags.isEmpty) {
            val code = tags[0].language
            if (code in SUPPORTED) store.language = code
        } else {
            store.language?.let { context.getSystemService(LocaleManager::class.java).applicationLocales = LocaleList.forLanguageTags(it) }
        }
    }
}

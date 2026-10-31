package shop.chobbar.app

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import shop.chobbar.app.i18n.AppLocale
import shop.chobbar.app.ui.nav.ChobbarRoot
import shop.chobbar.app.ui.theme.ChobbarTheme
import shop.chobbar.app.ui.theme.LocalAppContainer
import shop.chobbar.app.ui.theme.LocalLowEnd

class MainActivity : ComponentActivity() {
    private val app get() = application as ChobbarApp
    private var ready = false
    private var deepLink: String? = null

    override fun attachBaseContext(newBase: Context) {
        val store = shop.chobbar.app.data.local.LanguageStore(newBase)
        super.attachBaseContext(AppLocale.wrap(newBase, store))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        val splash = installSplashScreen()
        super.onCreate(savedInstanceState)
        splash.setKeepOnScreenCondition { !ready }
        enableEdgeToEdge(statusBarStyle = SystemBarStyle.auto(0, 0), navigationBarStyle = SystemBarStyle.auto(0, 0))
        val c = app.container
        AppLocale.syncFromSystem(this, c.languageStore)
        deepLink = intent?.data?.toString()

        lifecycleScope.launch {
            // First launch: open in the visitor's language (country from IP via the API), max 1.5 s.
            if (c.languageStore.language == null) {
                val detected = withTimeoutOrNull(1500) { runCatching { c.api.locale().language }.getOrNull() }
                val code = detected?.takeIf { it in AppLocale.SUPPORTED } ?: AppLocale.deviceDefault()
                val mustRecreate = AppLocale.apply(this@MainActivity, c.languageStore, code)
                c.languageStore.confirmed = false // show the "we picked your language" card once
                if (mustRecreate && code != AppLocale.deviceDefault()) {
                    recreate()
                    return@launch
                }
            }
            withTimeoutOrNull(1200) { c.auth.restore() }
            ready = true
            launch { c.cart.refresh() }
            launch { c.wishlist.refresh() }
        }

        setContent {
            val theme by c.local.theme.collectAsState(initial = "system")
            val dark = when (theme) { "dark" -> true; "light" -> false; else -> isSystemInDarkTheme() }
            CompositionLocalProvider(LocalAppContainer provides c, LocalLowEnd provides app.isLowEnd) {
                ChobbarTheme(dark = dark) {
                    ChobbarRoot(initialDeepLink = deepLink, onLanguageChange = { code ->
                        if (AppLocale.apply(this, c.languageStore, code)) recreate()
                        lifecycleScope.launch { if (c.auth.isSignedIn) runCatching { c.api.updateMe(shop.chobbar.app.data.model.ProfilePatch(language = code)) } }
                    })
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        intent.data?.toString()?.let { shop.chobbar.app.ui.nav.DeepLinks.emit(it) }
    }
}

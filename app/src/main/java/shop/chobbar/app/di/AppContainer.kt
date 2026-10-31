package shop.chobbar.app.di

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import shop.chobbar.app.data.api.ApiClient
import shop.chobbar.app.data.local.LanguageStore
import shop.chobbar.app.data.local.LocalStore
import shop.chobbar.app.data.local.SessionStore
import shop.chobbar.app.data.repo.AuthRepository
import shop.chobbar.app.data.repo.CartRepository
import shop.chobbar.app.data.repo.RealtimeRepository
import shop.chobbar.app.data.repo.WishlistRepository
import shop.chobbar.app.i18n.AppLocale

/**
 * Hand-rolled dependency container: no annotation processing or reflection, which keeps
 * the APK small and cold start fast on low-end phones.
 */
class AppContainer(context: Context) {
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    val languageStore = LanguageStore(context)
    val session = SessionStore(context)
    val local = LocalStore(context)
    val client = ApiClient(context, session, local) { AppLocale.current(languageStore) }
    val api get() = client.api
    val cart = CartRepository(client, local)
    val auth = AuthRepository(client, session, local, cart, scope)
    val wishlist = WishlistRepository(client, local) { auth.isSignedIn }
    val realtime = RealtimeRepository(client, scope)

    init {
        client.onSessionExpired = { auth.expired() }
    }
}

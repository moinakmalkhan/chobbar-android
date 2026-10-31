package shop.chobbar.app.data.local

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import shop.chobbar.app.data.api.AppJson
import shop.chobbar.app.data.model.ProductCard
import java.security.KeyStore
import java.util.UUID
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

private val Context.sessionStore by preferencesDataStore("session")
private val Context.localStore by preferencesDataStore("local")

/** AES-GCM with a non-exportable Android Keystore key: tokens at rest are unreadable off-device. */
private object TokenCipher {
    private const val ALIAS = "chobbar_session"
    private fun key(): SecretKey {
        val ks = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (ks.getEntry(ALIAS, null) as? KeyStore.SecretKeyEntry)?.let { return it.secretKey }
        val gen = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        gen.init(
            KeyGenParameterSpec.Builder(ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .build(),
        )
        return gen.generateKey()
    }

    fun encrypt(plain: String): String {
        val c = Cipher.getInstance("AES/GCM/NoPadding").apply { init(Cipher.ENCRYPT_MODE, key()) }
        val out = c.iv + c.doFinal(plain.toByteArray())
        return Base64.encodeToString(out, Base64.NO_WRAP)
    }

    fun decrypt(enc: String): String? = runCatching {
        val bytes = Base64.decode(enc, Base64.NO_WRAP)
        val c = Cipher.getInstance("AES/GCM/NoPadding").apply { init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(128, bytes, 0, 12)) }
        String(c.doFinal(bytes, 12, bytes.size - 12))
    }.getOrNull()
}

class SessionStore(private val context: Context) {
    private val accessKey = stringPreferencesKey("access")
    private val refreshKey = stringPreferencesKey("refresh")
    @Volatile private var cachedAccess: String? = null
    @Volatile private var cachedRefresh: String? = null
    @Volatile private var loaded = false

    val isSignedIn: Flow<Boolean> = context.sessionStore.data.map { it[refreshKey] != null }

    private suspend fun load() {
        if (loaded) return
        val p = context.sessionStore.data.first()
        cachedAccess = p[accessKey]?.let(TokenCipher::decrypt)
        cachedRefresh = p[refreshKey]?.let(TokenCipher::decrypt)
        loaded = true
    }

    suspend fun access(): String? { load(); return cachedAccess }
    suspend fun refresh(): String? { load(); return cachedRefresh }

    suspend fun save(access: String, refresh: String) {
        cachedAccess = access; cachedRefresh = refresh; loaded = true
        context.sessionStore.edit { it[accessKey] = TokenCipher.encrypt(access); it[refreshKey] = TokenCipher.encrypt(refresh) }
    }

    suspend fun clear() {
        cachedAccess = null; cachedRefresh = null; loaded = true
        context.sessionStore.edit { it.clear() }
    }
}

@Serializable
data class CheckoutContact(val email: String = "", val phone: String = "", val fullName: String = "", val city: String = "", val province: String = "")

/**
 * Everything that belongs on the device: guest bag token, guest wishlist, recently viewed,
 * recent searches, last checkout contact and theme. Small JSON values in DataStore.
 */
class LocalStore(private val context: Context) {
    private val cartTokenKey = stringPreferencesKey("cart_token")
    private val sessionKeyKey = stringPreferencesKey("session_key")
    private val wishlistKey = stringPreferencesKey("wishlist")
    private val viewedKey = stringPreferencesKey("recently_viewed")
    private val searchesKey = stringPreferencesKey("recent_searches")
    private val contactKey = stringPreferencesKey("contact")
    private val themeKey = stringPreferencesKey("theme")
    @Volatile private var cartTokenCache: String? = null
    @Volatile private var cartTokenLoaded = false
    @Volatile private var sessionKeyCache: String? = null

    private val strings = ListSerializer(String.serializer())
    private val cards = ListSerializer(ProductCard.serializer())
    private val data get() = context.localStore.data

    suspend fun cartToken(): String? {
        if (!cartTokenLoaded) { cartTokenCache = data.first()[cartTokenKey]; cartTokenLoaded = true }
        return cartTokenCache
    }

    suspend fun setCartToken(token: String?) {
        if (token == cartTokenCache && cartTokenLoaded) return
        cartTokenCache = token; cartTokenLoaded = true
        context.localStore.edit { if (token == null) it.remove(cartTokenKey) else it[cartTokenKey] = token }
    }

    suspend fun sessionKey(): String {
        sessionKeyCache?.let { return it }
        val existing = data.first()[sessionKeyKey]
        val key = existing ?: UUID.randomUUID().toString().also { k -> context.localStore.edit { it[sessionKeyKey] = k } }
        sessionKeyCache = key
        return key
    }

    private fun <T> decode(p: Preferences, key: Preferences.Key<String>, s: kotlinx.serialization.KSerializer<List<T>>): List<T> =
        p[key]?.let { runCatching { AppJson.decodeFromString(s, it) }.getOrNull() } ?: emptyList()

    val wishlist: Flow<List<String>> = data.map { decode(it, wishlistKey, strings) }
    val recentlyViewed: Flow<List<ProductCard>> = data.map { decode(it, viewedKey, cards) }
    val recentSearches: Flow<List<String>> = data.map { decode(it, searchesKey, strings) }
    val theme: Flow<String> = data.map { it[themeKey] ?: "system" }
    val contact: Flow<CheckoutContact?> = data.map { p -> p[contactKey]?.let { runCatching { AppJson.decodeFromString(CheckoutContact.serializer(), it) }.getOrNull() } }

    suspend fun toggleWishlist(slug: String): Boolean {
        var added = false
        context.localStore.edit { p ->
            val list = decode(p, wishlistKey, strings).toMutableList()
            added = !list.remove(slug)
            if (added) list.add(0, slug)
            p[wishlistKey] = AppJson.encodeToString(strings, list.take(100))
        }
        return added
    }

    suspend fun clearWishlist() = context.localStore.edit { it.remove(wishlistKey) }

    suspend fun pushViewed(card: ProductCard) = context.localStore.edit { p ->
        val list = listOf(card) + decode(p, viewedKey, cards).filter { it.slug != card.slug }
        p[viewedKey] = AppJson.encodeToString(cards, list.take(12))
    }

    suspend fun pushSearch(q: String) {
        val v = q.trim()
        if (v.length < 2) return
        context.localStore.edit { p ->
            val list = listOf(v) + decode(p, searchesKey, strings).filter { !it.equals(v, ignoreCase = true) }
            p[searchesKey] = AppJson.encodeToString(strings, list.take(8))
        }
    }

    suspend fun clearSearches() = context.localStore.edit { it.remove(searchesKey) }
    suspend fun setTheme(value: String) = context.localStore.edit { it[themeKey] = value }
    suspend fun saveContact(c: CheckoutContact) = context.localStore.edit { it[contactKey] = AppJson.encodeToString(CheckoutContact.serializer(), c) }
}

/** Language choice must be readable synchronously before the first frame (attachBaseContext), so it uses SharedPreferences. */
class LanguageStore(context: Context) {
    private val prefs = context.getSharedPreferences("language", Context.MODE_PRIVATE)
    var language: String?
        get() = prefs.getString("code", null)
        set(value) = prefs.edit().putString("code", value).apply()
    var confirmed: Boolean
        get() = prefs.getBoolean("confirmed", false)
        set(value) = prefs.edit().putBoolean("confirmed", value).apply()
}

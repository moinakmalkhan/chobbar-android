package shop.chobbar.app.data.repo

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import shop.chobbar.app.BuildConfig
import shop.chobbar.app.data.api.ApiClient
import shop.chobbar.app.data.api.AppJson
import shop.chobbar.app.data.local.LocalStore
import shop.chobbar.app.data.local.SessionStore
import shop.chobbar.app.data.model.*
import java.util.UUID

/** Signed-in user, shared app-wide. */
class AuthRepository(
    private val client: ApiClient,
    private val session: SessionStore,
    private val local: LocalStore,
    private val cart: CartRepository,
    private val scope: CoroutineScope,
) {
    private val api get() = client.api
    private val _user = MutableStateFlow<User?>(null)
    val user: StateFlow<User?> = _user.asStateFlow()
    val isSignedIn get() = _user.value != null

    suspend fun restore() {
        if (session.refresh() == null) return
        runCatching { _user.value = api.me() }
    }

    suspend fun login(email: String, password: String) = complete(api.login(LoginBody(email.trim(), password)))
    suspend fun google(idToken: String) = complete(api.google(GoogleBody(idToken)))
    suspend fun register(body: RegisterBody) = complete(api.register(body))

    /** After any sign-in: save tokens, push the guest wishlist up, reload the (server-merged) bag. */
    private suspend fun complete(res: AuthResponse): User {
        session.save(res.access, res.refresh)
        _user.value = res.user
        scope.launch {
            val guest = local.wishlist.first()
            if (guest.isNotEmpty()) {
                val have = runCatching { api.wishlist().results.map { it.product.slug }.toSet() }.getOrDefault(emptySet())
                guest.filter { it !in have }.forEach { runCatching { api.toggleWishlist(WishlistBody(it)) } }
                local.clearWishlist()
            }
            cart.refresh()
        }
        return res.user
    }

    fun setUser(u: User) { _user.value = u }

    suspend fun logout() {
        session.refresh()?.let { r -> runCatching { api.logout(RefreshBody(r)) } }
        session.clear()
        _user.value = null
        local.setCartToken(null)
        cart.refresh()
    }

    fun expired() { _user.value = null }
}

/** The bag: one source of truth for the badge, drawer, bag screen and checkout. */
class CartRepository(private val client: ApiClient, private val local: LocalStore) {
    private val api get() = client.api
    private val _cart = MutableStateFlow<Cart?>(null)
    val cart: StateFlow<Cart?> = _cart.asStateFlow()

    private suspend fun keep(c: Cart): Cart {
        local.setCartToken(c.guestToken)
        _cart.value = c
        return c
    }

    suspend fun refresh(): Cart? = runCatching { keep(api.cart()) }.getOrNull()
    suspend fun add(variant: String, quantity: Int) = keep(api.addToCart(AddItemBody(variant, quantity)))
    suspend fun setQuantity(itemId: String, quantity: Int) = keep(api.setQuantity(itemId, QuantityBody(quantity)))
    suspend fun remove(itemId: String) = keep(api.removeItem(itemId))
    suspend fun applyCoupon(code: String) = keep(api.applyCoupon(CouponBody(code)))
    suspend fun removeCoupon() = keep(api.removeCoupon())
    fun clearLocal() { _cart.value = _cart.value?.copy(items = emptyList(), quantity = 0, itemCount = 0, subtotal = 0.0, discount = 0.0) }
}

/** Wishlist: server-backed when signed in, device-local for guests. */
class WishlistRepository(private val client: ApiClient, private val local: LocalStore, private val auth: () -> Boolean) {
    private val _server = MutableStateFlow<List<String>>(emptyList())
    val server: StateFlow<List<String>> = _server.asStateFlow()

    suspend fun refresh() {
        if (auth()) _server.value = runCatching { client.api.wishlist().results.map { it.product.slug } }.getOrDefault(_server.value)
    }

    suspend fun toggle(slug: String): Boolean = if (auth()) {
        val added = client.api.toggleWishlist(WishlistBody(slug)).wishlisted
        _server.value = if (added) listOf(slug) + _server.value else _server.value - slug
        added
    } else {
        local.toggleWishlist(slug)
    }
}

/** Realtime events (notifications + chat) over OkHttp WebSockets with JWT in the query string. */
class RealtimeRepository(private val client: ApiClient, private val scope: CoroutineScope) {
    private val _events = MutableSharedFlow<Notification>(extraBufferCapacity = 8)
    val notifications = _events.asSharedFlow()
    private var socket: WebSocket? = null
    private var attempts = 0

    fun connect() {
        if (socket != null) return
        scope.launch {
            val token = client.accessTokenForSocket() ?: return@launch
            val req = Request.Builder().url("${BuildConfig.WS_URL}notifications/?token=$token").build()
            socket = client.http.newWebSocket(req, object : WebSocketListener() {
                override fun onOpen(webSocket: WebSocket, response: Response) { attempts = 0 }
                override fun onMessage(webSocket: WebSocket, text: String) {
                    if (text == "pong") return
                    runCatching {
                        val o = AppJson.parseToJsonElement(text).jsonObject
                        if (o["event"]?.jsonPrimitive?.content != "notification") return
                        _events.tryEmit(
                            Notification(
                                id = o["id"]?.jsonPrimitive?.content ?: "",
                                kind = o["kind"]?.jsonPrimitive?.content ?: "system",
                                level = o["level"]?.jsonPrimitive?.content ?: "info",
                                title = o["title"]?.jsonPrimitive?.content ?: "",
                                body = o["body"]?.jsonPrimitive?.content ?: "",
                                url = o["url"]?.jsonPrimitive?.content ?: "",
                            ),
                        )
                    }
                }
                override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) = retry()
                override fun onClosed(webSocket: WebSocket, code: Int, reason: String) { if (code != 4401) retry() }
            })
        }
    }

    private fun retry() {
        socket = null
        val delayMs = (1000L shl attempts.coerceAtMost(5)).coerceAtMost(30_000)
        attempts++
        scope.launch { kotlinx.coroutines.delay(delayMs); connect() }
    }

    fun disconnect() { socket?.close(1000, null); socket = null }

    /** One live chat thread. */
    fun chat(conversationId: String, onMessage: (ChatMessage) -> Unit, onTyping: (String?) -> Unit): ChatConnection {
        val conn = ChatConnection()
        scope.launch {
            val token = client.accessTokenForSocket() ?: return@launch
            val req = Request.Builder().url("${BuildConfig.WS_URL}chat/$conversationId/?token=$token").build()
            conn.ws = client.http.newWebSocket(req, object : WebSocketListener() {
                override fun onMessage(webSocket: WebSocket, text: String) {
                    runCatching {
                        val o = AppJson.parseToJsonElement(text).jsonObject
                        when (o["event"]?.jsonPrimitive?.content) {
                            "message", "ack" -> o["message"]?.let { onMessage(AppJson.decodeFromJsonElement(ChatMessage.serializer(), it)) }
                            "typing" -> {
                                val typing = o["is_typing"]?.jsonPrimitive?.content == "true"
                                onTyping(if (typing) o["user"]?.jsonObject?.get("name")?.jsonPrimitive?.content else null)
                            }
                        }
                    }
                }
            })
        }
        return conn
    }
}

class ChatConnection {
    var ws: WebSocket? = null
    fun send(body: String): String {
        val id = UUID.randomUUID().toString()
        ws?.send("""{"type":"message","body":${AppJson.encodeToString(String.serializer(), body)},"client_id":"$id"}""")
        return id
    }
    fun typing(isTyping: Boolean) { ws?.send("""{"type":"typing","is_typing":$isTyping}""") }
    fun read() { ws?.send("""{"type":"read"}""") }
    fun close() { ws?.close(1000, null) }
}

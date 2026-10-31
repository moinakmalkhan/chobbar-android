package shop.chobbar.app.data.api

import android.content.Context
import android.util.Base64
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.Cache
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import retrofit2.HttpException
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import shop.chobbar.app.BuildConfig
import shop.chobbar.app.data.local.LocalStore
import shop.chobbar.app.data.local.SessionStore
import shop.chobbar.app.data.model.ApiErrorBody
import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit

val AppJson = Json {
    ignoreUnknownKeys = true
    explicitNulls = false
    coerceInputValues = true
    isLenient = true
}

/** Seconds left before a JWT's `exp` (decoded locally, no network). */
fun jwtSecondsLeft(token: String?): Long {
    if (token.isNullOrBlank()) return -1
    return try {
        val payload = token.split(".")[1]
        val json = String(Base64.decode(payload, Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING))
        JSONObject(json).getLong("exp") - System.currentTimeMillis() / 1000
    } catch (_: Exception) {
        -1
    }
}

/**
 * OkHttp + Retrofit with:
 *  - access token refreshed *before* a request when < 45 s remain (single-flight via Mutex),
 *  - one refresh + retry on 401,
 *  - X-Language, X-Cart-Token and X-Session-Key headers on every call,
 *  - a 10 MB HTTP cache so catalog pages open instantly on slow networks.
 */
class ApiClient(context: Context, private val session: SessionStore, private val local: LocalStore, private val language: () -> String) {
    private val refreshLock = Mutex()
    @Volatile var onSessionExpired: (() -> Unit)? = null

    private val bare = OkHttpClient.Builder().connectTimeout(15, TimeUnit.SECONDS).readTimeout(20, TimeUnit.SECONDS).build()

    private suspend fun refresh(): String? = refreshLock.withLock {
        val current = session.access()
        if (jwtSecondsLeft(current) > 45) return current // another caller already refreshed
        val refresh = session.refresh() ?: return null
        if (jwtSecondsLeft(refresh) <= 0) {
            session.clear(); onSessionExpired?.invoke(); return null
        }
        val req = Request.Builder()
            .url(BuildConfig.API_URL + "auth/refresh/")
            .post("""{"refresh":"$refresh"}""".toRequestBody(JSON))
            .build()
        return try {
            bare.newCall(req).execute().use { res ->
                if (!res.isSuccessful) {
                    if (res.code == 401 || res.code == 400) { session.clear(); onSessionExpired?.invoke() }
                    return null
                }
                val obj = JSONObject(res.body!!.string())
                val access = obj.getString("access")
                session.save(access, obj.optString("refresh").ifBlank { refresh })
                access
            }
        } catch (_: IOException) {
            current
        }
    }

    private val headers = Interceptor { chain ->
        var token = runBlocking { session.access() }
        if (token != null && jwtSecondsLeft(token) <= 45) token = runBlocking { refresh() }
        val b = chain.request().newBuilder()
            .header("Accept", "application/json")
            .header("X-Language", language())
            .header("X-Session-Key", runBlocking { local.sessionKey() })
        runBlocking { local.cartToken() }?.let { b.header("X-Cart-Token", it) }
        token?.let { b.header("Authorization", "Bearer $it") }
        val response = chain.proceed(b.build())
        if (response.code == 401 && token != null) {
            val fresh = runBlocking { refresh() }
            if (fresh != null && fresh != token) {
                response.close()
                return@Interceptor chain.proceed(b.header("Authorization", "Bearer $fresh").build())
            }
        }
        response
    }

    val http: OkHttpClient = OkHttpClient.Builder()
        .cache(Cache(File(context.cacheDir, "http"), 10L * 1024 * 1024))
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(25, TimeUnit.SECONDS)
        .addInterceptor(headers)
        .apply {
            if (BuildConfig.DEBUG) {
                addInterceptor(okhttp3.logging.HttpLoggingInterceptor().apply { level = okhttp3.logging.HttpLoggingInterceptor.Level.BASIC })
            }
        }
        .build()

    val api: ChobbarApi = Retrofit.Builder()
        .baseUrl(BuildConfig.API_URL)
        .client(http)
        .addConverterFactory(AppJson.asConverterFactory(JSON))
        .build()
        .create(ChobbarApi::class.java)

    /** A fresh access token for WebSocket connections. */
    suspend fun accessTokenForSocket(): String? {
        val t = session.access() ?: return null
        return if (jwtSecondsLeft(t) <= 45) refresh() else t
    }

    companion object {
        val JSON = "application/json".toMediaType()
    }
}

/** API error → one user-facing message plus field errors (the backend's {detail, code, errors} envelope). */
data class AppError(val message: String, val code: String = "", val fields: Map<String, String> = emptyMap()) : Exception(message)

fun Throwable.toAppError(offline: String, generic: String): AppError = when (this) {
    is AppError -> this
    is HttpException -> {
        val body = try { response()?.errorBody()?.string() } catch (_: Exception) { null }
        val parsed = body?.let { runCatching { AppJson.decodeFromString(ApiErrorBody.serializer(), it) }.getOrNull() }
        val fields = parsed?.errors?.mapValues { (_, v) ->
            runCatching { v.jsonArray.first().jsonPrimitive.content }.getOrElse { v.toString() }
        } ?: emptyMap()
        AppError(parsed?.detail?.takeIf { it.isNotBlank() } ?: generic, parsed?.code ?: code().toString(), fields)
    }
    is IOException -> AppError(offline, "network")
    else -> AppError(message ?: generic)
}

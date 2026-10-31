package shop.chobbar.app.ui.nav

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import shop.chobbar.app.R
import shop.chobbar.app.data.api.AppError
import shop.chobbar.app.data.api.toAppError

/** App-wide snackbar messages (the Android equivalent of the website's toasts). */
object UiMessages {
    data class Msg(val text: String, val action: String? = null, val onAction: (() -> Unit)? = null)
    private val _flow = MutableSharedFlow<Msg>(extraBufferCapacity = 4)
    val flow = _flow.asSharedFlow()
    fun show(text: String, action: String? = null, onAction: (() -> Unit)? = null) { _flow.tryEmit(Msg(text, action, onAction)) }
}

/** Links from emails/website (https://chobbar.shop/...) delivered while the app is open. */
object DeepLinks {
    private val _flow = MutableSharedFlow<String>(extraBufferCapacity = 2)
    val flow = _flow.asSharedFlow()
    fun emit(url: String) { _flow.tryEmit(url) }

    /** Maps a website URL to an in-app route, or null. */
    fun route(url: String): String? {
        val path = url.substringAfter("chobbar.shop", "").substringBefore("?").trim('/').split('/').filter { it.isNotEmpty() }
        val parts = if (path.firstOrNull() in listOf("en", "ur", "ar")) path.drop(1) else path
        return when {
            parts.size >= 2 && parts[0] == "products" -> "product/${parts[1]}"
            parts.size >= 2 && parts[0] == "categories" -> "list/category/${parts[1]}"
            parts.size >= 2 && parts[0] == "collections" -> "list/collection/${parts[1]}"
            parts.size >= 3 && parts[0] == "account" && parts[1] == "orders" -> "order/${parts[2]}"
            parts.size >= 3 && parts[0] == "account" && parts[1] == "messages" -> "chat/${parts[2]}"
            parts.firstOrNull() == "track" -> "track"
            parts.firstOrNull() == "cart" -> "cart"
            else -> null
        }
    }
}

fun Throwable.userMessage(context: Context): String =
    toAppError(context.getString(R.string.errors_network), context.getString(R.string.errors_generic)).message

fun Throwable.fieldErrors(): Map<String, String> = (this as? AppError)?.fields ?: emptyMap()

sealed interface Load<out T> {
    data object Loading : Load<Nothing>
    data class Ok<T>(val data: T) : Load<T>
    data class Failed(val error: Throwable) : Load<Nothing>
}

/** Survives rotation; re-runs only when asked. Keeps screens free of boilerplate ViewModels. */
class ResourceVM : ViewModel() {
    val state = mutableStateOf<Load<Any?>>(Load.Loading)
    private var job: Job? = null
    private var started = false
    fun start(block: suspend () -> Any?, force: Boolean = false) {
        if (started && !force) return
        started = true
        job?.cancel()
        if (force && state.value is Load.Ok) Unit else state.value = Load.Loading
        job = viewModelScope.launch {
            state.value = try { Load.Ok(block()) } catch (e: Exception) { if (e is kotlinx.coroutines.CancellationException) throw e; Load.Failed(e) }
        }
    }
}

class Resource<T>(private val vm: ResourceVM, private val block: suspend () -> T) {
    @Suppress("UNCHECKED_CAST")
    val state: State<Load<T>> get() = vm.state as State<Load<T>>
    val data: T? get() = (state.value as? Load.Ok<T>)?.data
    fun reload() = vm.start(block, force = true)
    fun set(value: T) { vm.state.value = Load.Ok(value) }
}

@Composable
fun <T> rememberResource(key: String, block: suspend () -> T): Resource<T> {
    val vm: ResourceVM = viewModel(key = key)
    vm.start(block)
    return Resource(vm, block)
}

@Composable
fun str(id: Int, vararg args: Any): String = LocalContext.current.getString(id, *args)

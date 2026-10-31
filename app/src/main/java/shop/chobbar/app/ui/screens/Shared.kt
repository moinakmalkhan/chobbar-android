package shop.chobbar.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import kotlinx.coroutines.launch
import shop.chobbar.app.R
import shop.chobbar.app.data.model.ProductCard
import shop.chobbar.app.ui.components.ProductCardView
import shop.chobbar.app.ui.components.Shimmer
import shop.chobbar.app.ui.nav.UiMessages
import shop.chobbar.app.ui.nav.userMessage
import shop.chobbar.app.ui.theme.LocalAppContainer

/** Saved slugs (server when signed in, device for guests) and a toggle that shows feedback. */
@Composable
fun rememberWishlist(): Pair<Set<String>, (ProductCard) -> Unit> {
    val c = LocalAppContainer.current
    val user by c.auth.user.collectAsState()
    val server by c.wishlist.server.collectAsState()
    val local by c.local.wishlist.collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()
    val ctx = LocalContext.current
    val saved = (if (user != null) server else local).toSet()
    val toggle: (ProductCard) -> Unit = { p ->
        scope.launch {
            runCatching { c.wishlist.toggle(p.slug) }
                .onSuccess { added -> UiMessages.show(ctx.getString(if (added) R.string.product_saved_to_wishlist else R.string.product_removed_from_wishlist)) }
                .onFailure { UiMessages.show(it.userMessage(ctx)) }
        }
    }
    return saved to toggle
}

@Composable
fun ProductRail(products: List<ProductCard>?, nav: NavHostController) {
    val (saved, toggle) = rememberWishlist()
    LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        if (products == null) {
            items(4) { Shimmer(Modifier.width(158.dp).height(260.dp)) }
        } else {
            items(products, key = { it.id }) { p ->
                ProductCardView(p, p.slug in saved, onClick = { nav.navigate("product/${p.slug}") }, onToggleWishlist = { toggle(p) }, modifier = Modifier.width(158.dp))
            }
        }
    }
}

@Composable
fun BackTopBar(title: String, nav: NavHostController, actions: @Composable RowScope.() -> Unit = {}) {
    TopAppBar(
        title = { Text(title, style = MaterialTheme.typography.titleLarge) },
        navigationIcon = { IconButton(onClick = { nav.popBackStack() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.common_back)) } },
        actions = actions,
        colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
        windowInsets = WindowInsets.statusBars,
    )
}

@Composable
fun ErrorRetry(message: String, onRetry: () -> Unit) {
    Column(Modifier.fillMaxWidth().padding(32.dp), horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(message, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        OutlinedButton(onClick = onRetry) { Text(stringResource(R.string.common_retry)) }
    }
}

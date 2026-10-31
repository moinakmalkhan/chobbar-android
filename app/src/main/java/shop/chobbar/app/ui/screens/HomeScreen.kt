package shop.chobbar.app.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import shop.chobbar.app.R
import shop.chobbar.app.data.model.*
import shop.chobbar.app.ui.components.*
import shop.chobbar.app.ui.nav.DeepLinks
import shop.chobbar.app.ui.nav.Load
import shop.chobbar.app.ui.nav.rememberResource
import shop.chobbar.app.ui.nav.userMessage
import shop.chobbar.app.ui.theme.Brand
import shop.chobbar.app.ui.theme.LocalAppContainer
import shop.chobbar.app.ui.theme.LocalLowEnd

data class HomeData(
    val storefront: Storefront,
    val categories: List<CategoryNode>,
    val fresh: List<ProductCard>,
    val best: List<ProductCard>,
    val sale: List<ProductCard>,
    val collections: List<Collection>,
)

@Composable
fun HomeScreen(nav: NavHostController) {
    val c = LocalAppContainer.current
    val api = c.api
    val home = rememberResource("home") {
        kotlinx.coroutines.coroutineScope {
            val s = async { api.storefront() }
            val cats = async { api.categories() }
            val fresh = async { api.feed("new-arrivals") }
            val best = async { api.feed("best-sellers") }
            val sale = async { api.products(mapOf("on_sale" to "true", "page_size" to "12", "ordering" to "-sales_count")).results }
            val cols = async { api.collections() }
            HomeData(s.await(), cats.await(), fresh.await(), best.await(), sale.await(), cols.await())
        }
    }
    val viewed by c.local.recentlyViewed.collectAsState(initial = emptyList())
    val user by c.auth.user.collectAsState()
    var refreshing by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val ctx = androidx.compose.ui.platform.LocalContext.current

    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Row(Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 16.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Wordmark(height = 26.dp)
            Spacer(Modifier.weight(1f))
            if (user != null) IconButton(onClick = { nav.navigate("notifications") }) { Icon(Icons.Outlined.Notifications, stringResource(R.string.nav_notifications)) }
            IconButton(onClick = { nav.navigate("wishlist") }) { Icon(Icons.Outlined.FavoriteBorder, stringResource(R.string.nav_wishlist)) }
        }
        PullToRefreshBox(
            isRefreshing = refreshing,
            onRefresh = { scope.launch { refreshing = true; home.reload(); delay(600); refreshing = false } },
            modifier = Modifier.fillMaxSize(),
        ) {
            when (val s = home.state.value) {
                is Load.Failed -> ErrorRetry(s.error.userMessage(ctx)) { home.reload() }
                else -> {
                    val d = (s as? Load.Ok)?.data
                    LazyColumn(contentPadding = PaddingValues(bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(22.dp)) {
                        item { HeroPager(d?.storefront?.banners?.filter { it.placement == "hero" }, nav) }
                        item { Perks() }
                        item {
                            SectionHeader(stringResource(R.string.home_shop_by_category))
                            CategoryRow(d?.categories?.filter { it.showInMenu }, nav)
                        }
                        item {
                            SectionHeader(stringResource(R.string.home_new_arrivals), stringResource(R.string.home_new_arrivals_sub), stringResource(R.string.common_view_all)) { nav.navigate("list/collection/new-arrivals") }
                            ProductRail(d?.fresh, nav)
                        }
                        d?.storefront?.banners?.firstOrNull { it.placement == "promo" }?.let { b -> item { PromoCard(b, nav) } }
                        item {
                            SectionHeader(stringResource(R.string.home_best_sellers), stringResource(R.string.home_best_sellers_sub), stringResource(R.string.common_view_all)) { nav.navigate("list/collection/best-sellers") }
                            ProductRail(d?.best, nav)
                        }
                        if (d == null || d.sale.isNotEmpty()) item { SaleBlock(d?.sale, nav) }
                        if (!d?.collections.isNullOrEmpty()) item {
                            SectionHeader(stringResource(R.string.nav_collections))
                            LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                items(d!!.collections.filter { it.productCount > 0 }, key = { it.slug }) { col ->
                                    Box(Modifier.width(220.dp).height(140.dp).clip(RoundedCornerShape(20.dp)).clickable { nav.navigate("list/collection/${col.slug}") }) {
                                        RemoteImage(col.image, col.name, Modifier.fillMaxSize())
                                        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.65f)))))
                                        Column(Modifier.align(Alignment.BottomStart).padding(14.dp)) {
                                            Text(col.name, color = Color.White, style = MaterialTheme.typography.titleLarge)
                                            if (col.tagline.isNotBlank()) Text(col.tagline, color = Color.White.copy(alpha = 0.8f), style = MaterialTheme.typography.bodySmall, maxLines = 1)
                                        }
                                    }
                                }
                            }
                        }
                        if (viewed.size >= 2) item {
                            SectionHeader(stringResource(R.string.home_recently_viewed))
                            ProductRail(viewed, nav)
                        }
                        item { StoryCard() }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun HeroPager(banners: List<Banner>?, nav: NavHostController) {
    val shape = RoundedCornerShape(24.dp)
    val mod = Modifier.padding(horizontal = 16.dp).fillMaxWidth().aspectRatio(0.82f).clip(shape)
    if (banners == null) { Shimmer(mod); return }
    if (banners.isEmpty()) return
    val pager = rememberPagerState { banners.size }
    val lowEnd = LocalLowEnd.current
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    LaunchedEffect(pager.currentPage, banners.size) {
        if (banners.size < 2) return@LaunchedEffect
        delay(6000)
        val next = (pager.currentPage + 1) % banners.size
        if (lowEnd) pager.scrollToPage(next) else pager.animateScrollToPage(next)
    }
    Box(mod) {
        HorizontalPager(pager, Modifier.fillMaxSize()) { i ->
            val b = banners[i]
            Box(Modifier.fillMaxSize().clickable { DeepLinks.route("https://chobbar.shop" + b.ctaUrl)?.let { nav.navigate(it) } }) {
                // Artwork is drawn for LTR (copy on the left); mirror it in Urdu/Arabic.
                RemoteImage(b.mobileImage ?: b.image, b.title, Modifier.fillMaxSize().graphicsLayer { if (rtl) scaleX = -1f })
                Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.45f), Color.Transparent, Color.Black.copy(alpha = 0.25f)))))
                Column(Modifier.padding(22.dp).align(Alignment.TopStart), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (b.eyebrow.isNotBlank()) Badge(b.eyebrow.uppercase(), Color.White.copy(alpha = 0.18f), Color.White)
                    Text(b.title, style = MaterialTheme.typography.displaySmall, color = Color.White)
                    if (b.subtitle.isNotBlank()) Text(b.subtitle, style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.85f), maxLines = 3)
                    Spacer(Modifier.height(4.dp))
                    Button(
                        onClick = { DeepLinks.route("https://chobbar.shop" + b.ctaUrl)?.let { nav.navigate(it) } },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Brand.Ink),
                        shape = CircleShape,
                    ) { Text(b.ctaLabel.ifBlank { stringResource(R.string.common_shop_now) }, fontWeight = FontWeight.Bold) }
                }
            }
        }
        if (banners.size > 1) Row(Modifier.align(Alignment.BottomCenter).padding(14.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            repeat(banners.size) { i ->
                Box(Modifier.height(5.dp).width(if (i == pager.currentPage) 22.dp else 8.dp).clip(CircleShape).background(Color.White.copy(alpha = if (i == pager.currentPage) 1f else 0.45f)))
            }
        }
    }
}

@Composable
private fun Perks() {
    val perks = listOf(R.string.home_perk_delivery to AppIcons.Truck, R.string.home_perk_cod to AppIcons.Bag, R.string.home_perk_returns to AppIcons.Return)
    LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        items(perks) { (label, icon) ->
            Row(Modifier.clip(CircleShape).background(MaterialTheme.colorScheme.surfaceVariant).padding(horizontal = 14.dp, vertical = 9.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(8.dp))
                Text(stringResource(label), style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}

@Composable
fun CategoryRow(cats: List<CategoryNode>?, nav: NavHostController) {
    LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        if (cats == null) items(4) { Shimmer(Modifier.size(120.dp, 150.dp).clip(RoundedCornerShape(20.dp))) }
        else items(cats, key = { it.slug }) { cat ->
            Box(Modifier.size(120.dp, 150.dp).clip(RoundedCornerShape(20.dp)).clickable { nav.navigate("list/category/${cat.slug}") }) {
                RemoteImage(cat.image, cat.name, Modifier.fillMaxSize())
                Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.6f)))))
                Text(cat.name, color = Color.White, style = MaterialTheme.typography.titleMedium, modifier = Modifier.align(Alignment.BottomStart).padding(12.dp))
            }
        }
    }
}

@Composable
private fun PromoCard(b: Banner, nav: NavHostController) {
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    Box(Modifier.padding(horizontal = 16.dp).fillMaxWidth().aspectRatio(1.8f).clip(RoundedCornerShape(22.dp)).clickable { DeepLinks.route("https://chobbar.shop" + b.ctaUrl)?.let { nav.navigate(it) } }) {
        RemoteImage(b.image, b.title, Modifier.fillMaxSize().graphicsLayer { if (rtl) scaleX = -1f })
        Box(Modifier.fillMaxSize().background(Brush.horizontalGradient(listOf(Color.Black.copy(alpha = 0.7f), Color.Transparent))))
        Column(Modifier.align(Alignment.CenterStart).padding(20.dp).fillMaxWidth(0.62f)) {
            Text(b.eyebrow.uppercase(), color = Brand.RoseLight, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
            Text(b.title, color = Color.White, style = MaterialTheme.typography.headlineSmall)
            Text(b.subtitle, color = Color.White.copy(alpha = 0.8f), style = MaterialTheme.typography.bodySmall, maxLines = 2)
        }
    }
}

@Composable
private fun SaleBlock(products: List<ProductCard>?, nav: NavHostController) {
    Column(Modifier.padding(horizontal = 16.dp).clip(RoundedCornerShape(24.dp)).background(Brand.Rose).padding(vertical = 18.dp)) {
        Row(Modifier.padding(horizontal = 18.dp), verticalAlignment = Alignment.Bottom) {
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.home_sale_title), color = Color.White, style = MaterialTheme.typography.headlineMedium)
                Text(stringResource(R.string.home_sale_sub), color = Color.White.copy(alpha = 0.85f), style = MaterialTheme.typography.bodySmall)
            }
            TextButton(onClick = { nav.navigate("list/collection/sale") }) { Text(stringResource(R.string.common_view_all), color = Color.White) }
        }
        Spacer(Modifier.height(12.dp))
        CompositionLocalProvider(LocalContentColor provides Color.White) {
            Surface(color = Color.Transparent, contentColor = Color.White) { ProductRail(products, nav) }
        }
    }
}

@Composable
private fun StoryCard() {
    Column(Modifier.padding(horizontal = 16.dp).fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(Brand.Ink950).padding(22.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(stringResource(R.string.home_story_eyebrow).uppercase(), color = Brand.RoseLight, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
        Text(stringResource(R.string.home_story_title), color = Color.White, style = MaterialTheme.typography.headlineSmall)
        Text(stringResource(R.string.home_story_text), color = Color.White.copy(alpha = 0.7f), style = MaterialTheme.typography.bodyMedium)
    }
}

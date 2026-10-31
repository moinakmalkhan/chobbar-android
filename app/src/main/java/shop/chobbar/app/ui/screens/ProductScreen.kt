package shop.chobbar.app.ui.screens

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import kotlinx.coroutines.launch
import shop.chobbar.app.R
import shop.chobbar.app.data.model.*
import shop.chobbar.app.ui.components.*
import shop.chobbar.app.ui.nav.Load
import shop.chobbar.app.ui.nav.UiMessages
import shop.chobbar.app.ui.nav.rememberResource
import shop.chobbar.app.ui.nav.userMessage
import shop.chobbar.app.ui.theme.Brand
import shop.chobbar.app.ui.theme.LocalAppContainer

@Composable
fun ProductScreen(nav: NavHostController, slug: String) {
    val c = LocalAppContainer.current
    val api = c.api
    val ctx = LocalContext.current
    val product = rememberResource("product-$slug") { api.product(slug) }
    val related = rememberResource("related-$slug") { api.related(slug) }
    val (saved, toggle) = rememberWishlist()

    LaunchedEffect(product.data?.id) { product.data?.let { c.local.pushViewed(it.toCard()) } }

    Column(Modifier.fillMaxSize()) {
        BackTopBar("", nav, actions = {
            product.data?.let { p ->
                IconButton(onClick = {
                    val send = android.content.Intent(android.content.Intent.ACTION_SEND).apply { type = "text/plain"; putExtra(android.content.Intent.EXTRA_TEXT, "${p.name} — https://chobbar.shop/products/${p.slug}") }
                    ctx.startActivity(android.content.Intent.createChooser(send, null))
                }) { Icon(Icons.Filled.Share, stringResource(R.string.product_share)) }
                HeartButton(p.slug in saved, { toggle(p.toCard()) }, Modifier.padding(end = 8.dp))
            }
        })
        when (val s = product.state.value) {
            Load.Loading -> Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Shimmer(Modifier.fillMaxWidth().aspectRatio(0.8f).clip(RoundedCornerShape(24.dp))); Shimmer(Modifier.fillMaxWidth(0.7f).height(24.dp)); Shimmer(Modifier.fillMaxWidth(0.3f).height(20.dp))
            }
            is Load.Failed -> ErrorRetry(s.error.userMessage(ctx)) { product.reload() }
            is Load.Ok -> ProductBody(s.data, related.data, nav)
        }
    }
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalLayoutApi::class)
@Composable
private fun ColumnScope.ProductBody(p: ProductDetail, related: List<ProductCard>?, nav: NavHostController) {
    val c = LocalAppContainer.current
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val colors = remember(p) { p.variants.mapNotNull { it.color }.distinctBy { it.slug } }
    val sizes = remember(p) { p.sizeGroup?.sizes?.map { it.label } ?: p.variants.mapNotNull { it.size }.distinct() }
    var color by rememberSaveable(p.slug) { mutableStateOf(colors.firstOrNull { col -> p.variants.any { it.color?.slug == col.slug && it.inStock } }?.slug ?: colors.firstOrNull()?.slug) }
    var size by rememberSaveable(p.slug) { mutableStateOf(if (sizes.size == 1) sizes[0] else null) }
    var qty by rememberSaveable(p.slug) { mutableIntStateOf(1) }
    var adding by remember { mutableStateOf(false) }
    var sizeError by remember { mutableStateOf(false) }
    var guideOpen by remember { mutableStateOf(false) }

    fun variantFor(col: String?, sz: String?) = p.variants.firstOrNull { it.color?.slug == col && it.size == sz }
    val variant = variantFor(color, size) ?: if (sizes.isEmpty()) p.variants.firstOrNull { it.color?.slug == color } else null
    val images = remember(p, color) { p.images.filter { it.color == color }.ifEmpty { p.images } }
    val maxQty = (variant?.stock ?: 10).coerceIn(1, 10)
    val soldOut = variant != null && !variant.inStock

    fun add() {
        if (size == null && sizes.isNotEmpty()) { sizeError = true; UiMessages.show(ctx.getString(R.string.product_select_size)); return }
        val v = variant ?: return
        adding = true
        scope.launch {
            runCatching { c.cart.add(v.id, qty.coerceAtMost(maxQty)) }
                .onSuccess { UiMessages.show(ctx.getString(R.string.product_added_to_bag), ctx.getString(R.string.product_view_bag)) { nav.navigate("cart") } }
                .onFailure { UiMessages.show(it.userMessage(ctx)) }
            adding = false
        }
    }

    LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(bottom = 24.dp)) {
        item {
            val pager = rememberPagerState { images.size.coerceAtLeast(1) }
            LaunchedEffect(color) { pager.scrollToPage(0) }
            Box(Modifier.padding(horizontal = 16.dp).fillMaxWidth().aspectRatio(0.8f).clip(RoundedCornerShape(24.dp))) {
                HorizontalPager(pager) { i -> RemoteImage(images.getOrNull(i)?.url, images.getOrNull(i)?.alt ?: p.name, Modifier.fillMaxSize()) }
                if (images.size > 1) Row(Modifier.align(Alignment.BottomCenter).padding(12.dp), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    repeat(images.size) { i -> Box(Modifier.height(5.dp).width(if (i == pager.currentPage) 18.dp else 6.dp).clip(CircleShape).background(if (i == pager.currentPage) Brand.Ink else Brand.Ink.copy(alpha = 0.3f))) }
                }
                if (p.discountPercent > 0) Box(Modifier.padding(12.dp)) { Badge("−${p.discountPercent}%", Brand.Rose, Color.White) }
            }
        }
        item {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(p.categoryName, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(p.name, style = MaterialTheme.typography.headlineMedium)
                if (p.ratingCount > 0) RatingStars(p.ratingAvg, count = p.ratingCount)
                PriceText(variant?.price ?: p.price, p.compareAtPrice, large = true)
                if (p.compareAtPrice != null && p.compareAtPrice > p.price) Text(stringResource(R.string.product_save_amount, money(p.compareAtPrice - p.price)), color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelMedium)
                if (p.shortDescription.isNotBlank()) Text(p.shortDescription, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        if (colors.isNotEmpty()) item {
            Column(Modifier.padding(horizontal = 16.dp)) {
                Text(stringResource(R.string.product_color) + ": " + (colors.firstOrNull { it.slug == color }?.name ?: ""), style = MaterialTheme.typography.titleSmall)
                Spacer(Modifier.height(10.dp))
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    colors.forEach { col ->
                        val inStock = p.variants.any { it.color?.slug == col.slug && it.inStock }
                        val on = col.slug == color
                        Box(
                            Modifier.size(44.dp).clip(CircleShape).background(parseHex(col.hex))
                                .border(if (on) 3.dp else 1.dp, if (on) MaterialTheme.colorScheme.onBackground else Color.Black.copy(alpha = 0.15f), CircleShape)
                                .clickable { color = col.slug },
                            contentAlignment = Alignment.Center,
                        ) {
                            if (on) Icon(Icons.Filled.Check, col.name, tint = if (col.hex.lowercase() in listOf("#f5f5f5", "#ffffff", "#d6c3a5", "#9a9a9a")) Color.Black else Color.White)
                            else if (!inStock) Box(Modifier.fillMaxSize().background(Color.White.copy(alpha = 0.55f)))
                        }
                    }
                }
            }
        }
        if (sizes.isNotEmpty()) item {
            Column(Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(if (sizeError && size == null) R.string.product_select_size else R.string.product_size), style = MaterialTheme.typography.titleSmall, color = if (sizeError && size == null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onBackground, modifier = Modifier.weight(1f))
                    if (!p.sizeGroup?.guide.isNullOrEmpty()) TextButton(onClick = { guideOpen = true }) { Icon(AppIcons.Ruler, null, Modifier.size(16.dp)); Spacer(Modifier.width(4.dp)); Text(stringResource(R.string.product_size_guide)) }
                }
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    sizes.forEach { s ->
                        val v = variantFor(color, s)
                        val out = v == null || !v.inStock
                        val on = s == size
                        Box(
                            Modifier.widthIn(min = 56.dp).height(46.dp).clip(RoundedCornerShape(14.dp))
                                .background(if (on) MaterialTheme.colorScheme.onBackground else Color.Transparent)
                                .border(1.dp, if (on) MaterialTheme.colorScheme.onBackground else if (sizeError && size == null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.outline, RoundedCornerShape(14.dp))
                                .clickable { size = s; sizeError = false }.padding(horizontal = 14.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(s, fontWeight = FontWeight.SemiBold, color = if (on) MaterialTheme.colorScheme.background else if (out) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onBackground, textDecoration = if (out && !on) TextDecoration.LineThrough else null)
                        }
                    }
                }
                if (variant?.lowStock == true) Text(stringResource(R.string.product_low_stock, num(variant.stock)), color = Brand.Warning, style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(top = 8.dp))
            }
        }
        if (soldOut && variant != null) item { NotifyMe(variant.id) }
        item { Details(p) }
        if (p.ratingCount > 0) item { ReviewsBlock(p.slug) }
        if (!related.isNullOrEmpty()) item { SectionHeader(stringResource(R.string.product_you_may_like)); ProductRail(related, nav) }
    }

    // Sticky add-to-bag bar
    Surface(tonalElevation = 3.dp, shadowElevation = 8.dp) {
        Row(Modifier.fillMaxWidth().navigationBarsPadding().padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            QuantityStepper(qty.coerceAtMost(maxQty), { qty = it }, max = maxQty)
            LoadingButton(stringResource(if (soldOut) R.string.common_sold_out else R.string.product_add_to_bag), adding, ::add, Modifier.weight(1f), enabled = !soldOut)
        }
    }

    if (guideOpen) ModalBottomSheet(onDismissRequest = { guideOpen = false }) {
        Column(Modifier.padding(20.dp).navigationBarsPadding()) {
            Text(stringResource(R.string.product_size_guide), style = MaterialTheme.typography.headlineSmall)
            Text(stringResource(R.string.product_guide_units), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(12.dp))
            val guide = p.sizeGroup!!.guide
            val cols = guide.first().keys.toList()
            Row(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceVariant).padding(10.dp)) { cols.forEach { Text(guideLabel(it), Modifier.weight(1f), fontWeight = FontWeight.Bold) } }
            guide.forEach { row -> Row(Modifier.fillMaxWidth().padding(10.dp)) { cols.forEach { k -> Text(row[k].orEmpty(), Modifier.weight(1f)) } } }
            Text(stringResource(R.string.product_guide_tip), style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 12.dp))
        }
    }
}

@Composable
private fun guideLabel(k: String) = stringResource(
    when (k) { "chest" -> R.string.product_guide_chest; "length" -> R.string.product_guide_length; "waist" -> R.string.product_guide_waist; "inseam" -> R.string.product_guide_inseam; "bust" -> R.string.product_guide_bust; else -> R.string.product_guide_size },
)

@Composable
private fun NotifyMe(variantId: String) {
    val c = LocalAppContainer.current
    val ctx = LocalContext.current
    val user by c.auth.user.collectAsState()
    val scope = rememberCoroutineScope()
    var email by remember { mutableStateOf(user?.email ?: "") }
    var done by remember(variantId) { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (done) Text(stringResource(R.string.product_notify_done), color = Brand.Success, fontWeight = FontWeight.SemiBold)
        else {
            if (user == null) OutlinedTextField(email, { email = it }, Modifier.fillMaxWidth(), label = { Text(stringResource(R.string.product_notify_email)) }, singleLine = true, shape = RoundedCornerShape(14.dp))
            OutlinedButton(
                onClick = {
                    busy = true
                    scope.launch {
                        runCatching { c.api.notifyMe(StockAlertBody(variantId, email.takeIf { user == null })) }
                            .onSuccess { done = true; UiMessages.show(ctx.getString(R.string.product_notify_done)) }
                            .onFailure { UiMessages.show(it.userMessage(ctx)) }
                        busy = false
                    }
                },
                enabled = !busy && (user != null || email.contains("@")), modifier = Modifier.fillMaxWidth().height(50.dp), shape = CircleShape,
            ) { Icon(Icons.Outlined.Notifications, null); Spacer(Modifier.width(8.dp)); Text(stringResource(R.string.product_notify_me)) }
        }
    }
}

@Composable
private fun Details(p: ProductDetail) {
    val sections = buildList {
        add(stringResource(R.string.product_details) to (p.description + if (p.specifications.isNotEmpty()) "\n\n" + p.specifications.entries.joinToString("\n") { "${it.key}: ${it.value}" } else ""))
        if (p.material.isNotBlank() || p.careInstructions.isNotBlank()) add(stringResource(R.string.product_material_care) to listOf(p.material, p.careInstructions).filter { it.isNotBlank() }.joinToString("\n\n"))
        add(stringResource(R.string.product_delivery_returns) to stringResource(R.string.product_delivery_text) + "\n\n" + stringResource(R.string.product_returns_text))
    }
    Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
        sections.forEachIndexed { i, (title, body) ->
            var open by rememberSaveable { mutableStateOf(i == 0) }
            Column(Modifier.animateContentSize()) {
                Row(Modifier.fillMaxWidth().clickable { open = !open }.padding(vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                    Icon(Icons.Filled.KeyboardArrowDown, null, Modifier.rotate(if (open) 180f else 0f))
                }
                if (open) Text(body, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(bottom = 14.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            }
        }
    }
}

@Composable
private fun ReviewsBlock(slug: String) {
    val api = LocalAppContainer.current.api
    val summary = rememberResource("rsum-$slug") { api.reviewSummary(slug) }
    val list = rememberResource("rlist-$slug") { api.reviews(slug).results }
    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(stringResource(R.string.reviews_title), style = MaterialTheme.typography.headlineSmall)
        summary.data?.let { s ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(String.format(java.util.Locale.ROOT, "%.1f", s.average), style = MaterialTheme.typography.displaySmall)
                Spacer(Modifier.width(10.dp))
                Column { RatingStars(s.average, 16.dp); Text(stringResource(R.string.reviews_based_on, num(s.total)), style = MaterialTheme.typography.bodySmall) }
            }
        }
        list.data?.take(5)?.forEach { r ->
            Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(MaterialTheme.colorScheme.surfaceVariant).padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                RatingStars(r.rating.toDouble(), 13.dp)
                if (r.title.isNotBlank()) Text(r.title, fontWeight = FontWeight.SemiBold)
                if (r.body.isNotBlank()) Text(r.body, style = MaterialTheme.typography.bodyMedium)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(r.author.name, style = MaterialTheme.typography.labelMedium)
                    if (r.isVerifiedPurchase) Text("  · " + stringResource(R.string.reviews_verified), color = Brand.Success, style = MaterialTheme.typography.labelSmall)
                }
                if (r.staffReply.isNotBlank()) Text(stringResource(R.string.reviews_reply) + ": " + r.staffReply, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

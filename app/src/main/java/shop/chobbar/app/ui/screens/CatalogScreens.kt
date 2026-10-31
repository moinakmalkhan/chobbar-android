package shop.chobbar.app.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import shop.chobbar.app.R
import shop.chobbar.app.data.model.*
import shop.chobbar.app.ui.components.*
import shop.chobbar.app.ui.nav.Load
import shop.chobbar.app.ui.nav.rememberResource
import shop.chobbar.app.ui.nav.userMessage
import shop.chobbar.app.ui.theme.LocalAppContainer

/* ---------------- Shop tab: category tree ---------------- */

@Composable
fun ShopScreen(nav: NavHostController) {
    val api = LocalAppContainer.current.api
    val cats = rememberResource("categories") { api.categories() }
    val ctx = LocalContext.current
    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        Text(stringResource(R.string.nav_shop), style = MaterialTheme.typography.headlineMedium, modifier = Modifier.padding(16.dp))
        when (val s = cats.state.value) {
            Load.Loading -> Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) { repeat(5) { Shimmer(Modifier.fillMaxWidth().height(72.dp).clip(RoundedCornerShape(18.dp))) } }
            is Load.Failed -> ErrorRetry(s.error.userMessage(ctx)) { cats.reload() }
            is Load.Ok -> LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        QuickTile(stringResource(R.string.nav_new_in), MaterialTheme.colorScheme.secondary, MaterialTheme.colorScheme.onSecondary, Modifier.weight(1f)) { nav.navigate("list/collection/new-arrivals") }
                        QuickTile(stringResource(R.string.nav_sale), MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.onPrimary, Modifier.weight(1f)) { nav.navigate("list/collection/sale") }
                    }
                }
                items(s.data, key = { it.slug }) { cat -> CategoryExpandable(cat, nav) }
                item {
                    OutlinedButton(onClick = { nav.navigate("list/all/all") }, modifier = Modifier.fillMaxWidth().height(50.dp), shape = CircleShape) { Text(stringResource(R.string.nav_all_products)) }
                }
            }
        }
    }
}

@Composable
private fun QuickTile(label: String, bg: Color, fg: Color, modifier: Modifier, onClick: () -> Unit) {
    Box(modifier.height(64.dp).clip(RoundedCornerShape(18.dp)).background(bg).clickable(onClick = onClick).padding(16.dp), contentAlignment = Alignment.CenterStart) {
        Text(label, color = fg, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun CategoryExpandable(cat: CategoryNode, nav: NavHostController) {
    var open by rememberSaveable(cat.slug) { mutableStateOf(false) }
    Column(Modifier.clip(RoundedCornerShape(18.dp)).background(MaterialTheme.colorScheme.surface).border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(18.dp)).animateContentSize()) {
        Row(Modifier.fillMaxWidth().clickable { if (cat.children.isEmpty()) nav.navigate("list/category/${cat.slug}") else open = !open }.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
            RemoteImage(cat.image, cat.name, Modifier.size(52.dp).clip(RoundedCornerShape(14.dp)))
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(cat.name, style = MaterialTheme.typography.titleMedium)
                Text(stringResource(R.string.common_results, num(cat.productCount)), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(Icons.Filled.KeyboardArrowDown, null, Modifier.rotate(if (open) 180f else 0f))
        }
        AnimatedVisibility(open) {
            Column(Modifier.padding(start = 12.dp, end = 12.dp, bottom = 10.dp)) {
                SubRow(stringResource(R.string.app_all_in, cat.name), null) { nav.navigate("list/category/${cat.slug}") }
                cat.children.forEach { child -> SubRow(child.name, child.productCount) { nav.navigate("list/category/${child.slug}") } }
            }
        }
    }
}

@Composable
private fun SubRow(label: String, count: Int?, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).clickable(onClick = onClick).padding(horizontal = 12.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
        count?.let { Text(num(it), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/* ---------------- Product list with filters (category / collection / search / all) ---------------- */

data class Filters(
    val sizes: Set<String> = emptySet(),
    val colors: Set<String> = emptySet(),
    val genders: Set<String> = emptySet(),
    val minPrice: String = "",
    val maxPrice: String = "",
    val inStock: Boolean = false,
    val onSale: Boolean = false,
) {
    val count get() = sizes.size + colors.size + genders.size + (if (minPrice.isNotBlank()) 1 else 0) + (if (maxPrice.isNotBlank()) 1 else 0) + (if (inStock) 1 else 0) + (if (onSale) 1 else 0)
    fun params(): Map<String, String> = buildMap {
        if (sizes.isNotEmpty()) put("size", sizes.joinToString(","))
        if (colors.isNotEmpty()) put("color", colors.joinToString(","))
        if (genders.isNotEmpty()) put("gender", genders.joinToString(","))
        if (minPrice.isNotBlank()) put("min_price", minPrice)
        if (maxPrice.isNotBlank()) put("max_price", maxPrice)
        if (inStock) put("in_stock", "true")
        if (onSale) put("on_sale", "true")
    }
}

private val SORTS = listOf("" to R.string.listing_sort_recommended, "-created_at" to R.string.listing_sort_newest, "-sales_count" to R.string.listing_sort_best, "price" to R.string.listing_sort_price_asc, "-price" to R.string.listing_sort_price_desc, "-rating_avg" to R.string.listing_sort_rating)

@Composable
fun ProductListScreen(nav: NavHostController, kind: String, slug: String, query: String? = null) {
    val c = LocalAppContainer.current
    val api = c.api
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var filters by rememberSaveable(stateSaver = androidx.compose.runtime.saveable.Saver<Filters, String>({ shop.chobbar.app.data.api.AppJson.encodeToString(FiltersSurrogate.serializer(), FiltersSurrogate.from(it)) }, { shop.chobbar.app.data.api.AppJson.decodeFromString(FiltersSurrogate.serializer(), it).to() })) { mutableStateOf(Filters()) }
    var ordering by rememberSaveable { mutableStateOf("") }
    var showFilters by remember { mutableStateOf(false) }
    val base: Map<String, String> = when (kind) {
        "category" -> mapOf("category" to slug)
        "collection" -> mapOf("collection" to slug)
        else -> emptyMap()
    } + (query?.let { mapOf("q" to it) } ?: emptyMap())
    val title = rememberResource("title-$kind-$slug") {
        when (kind) { "category" -> api.category(slug).name; "collection" -> api.collection(slug).name; else -> "" }
    }
    val facets = rememberResource("facets-$kind-$slug-${query.orEmpty()}") { api.facets(base) }

    // Manual infinite scroll: simple, small, cancels on filter change.
    val items = remember { mutableStateListOf<ProductCard>() }
    var page by remember { mutableIntStateOf(0) }
    var total by remember { mutableIntStateOf(-1) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var hasMore by remember { mutableStateOf(true) }
    val key = base + filters.params() + mapOf("ordering" to ordering)

    suspend fun loadNext(reset: Boolean) {
        if (loading) return
        loading = true; error = null
        val next = if (reset) 1 else page + 1
        try {
            val params = key.filterValues { it.isNotBlank() } + mapOf("page" to next.toString(), "page_size" to "24")
            val res = if (query != null) api.search(params) else api.products(params)
            if (reset) items.clear()
            items.addAll(res.results.filter { r -> items.none { it.id == r.id } })
            page = res.page; total = res.count; hasMore = res.next != null
        } catch (e: Exception) {
            error = e.userMessage(ctx)
        } finally { loading = false }
    }
    LaunchedEffect(key) { loadNext(reset = true) }
    val grid = rememberLazyGridState()
    val nearEnd by remember { derivedStateOf { grid.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0 >= items.size - 6 } }
    LaunchedEffect(nearEnd, items.size) { if (nearEnd && hasMore && !loading && items.isNotEmpty()) loadNext(reset = false) }
    val (saved, toggle) = rememberWishlist()

    Column(Modifier.fillMaxSize()) {
        if (query == null) BackTopBar(title.data?.ifBlank { null } ?: stringResource(R.string.nav_all_products), nav)
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(selected = filters.count > 0, onClick = { showFilters = true }, label = { Text(stringResource(R.string.listing_filters) + if (filters.count > 0) " · ${filters.count}" else "") }, leadingIcon = { Icon(AppIcons.Filter, null, Modifier.size(18.dp)) })
            var sortOpen by remember { mutableStateOf(false) }
            Box {
                FilterChip(selected = ordering.isNotBlank(), onClick = { sortOpen = true }, label = { Text(stringResource(SORTS.first { it.first == ordering }.second)) }, trailingIcon = { Icon(Icons.Filled.KeyboardArrowDown, null, Modifier.size(18.dp)) })
                DropdownMenu(sortOpen, onDismissRequest = { sortOpen = false }) {
                    SORTS.forEach { (v, label) -> DropdownMenuItem(text = { Text(stringResource(label)) }, onClick = { ordering = v; sortOpen = false }, trailingIcon = { if (v == ordering) Icon(Icons.Filled.Check, null) }) }
                }
            }
            Spacer(Modifier.weight(1f))
            if (total >= 0) Text(stringResource(R.string.common_results, num(total)), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        LazyVerticalGrid(
            columns = GridCells.Adaptive(158.dp), state = grid,
            contentPadding = PaddingValues(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(20.dp),
            modifier = Modifier.weight(1f),
        ) {
            items(items, key = { it.id }) { p ->
                ProductCardView(p, p.slug in saved, onClick = { nav.navigate("product/${p.slug}") }, onToggleWishlist = { toggle(p) })
            }
            if (loading && items.isEmpty()) items(6) { Shimmer(Modifier.fillMaxWidth().aspectRatio(0.62f).clip(RoundedCornerShape(18.dp))) }
            if (loading && items.isNotEmpty()) item(span = { GridItemSpan(maxLineSpan) }) { Box(Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp) } }
            error?.let { msg -> item(span = { GridItemSpan(maxLineSpan) }) { ErrorRetry(msg) { scope.launch { loadNext(items.isEmpty()) } } } }
            if (!loading && error == null && items.isEmpty() && total == 0) item(span = { GridItemSpan(maxLineSpan) }) {
                EmptyState(stringResource(R.string.listing_no_results), stringResource(R.string.listing_no_results_hint), if (filters.count > 0) stringResource(R.string.listing_clear_all) else null, { filters = Filters() })
            }
        }
    }

    if (showFilters) {
        ModalBottomSheet(onDismissRequest = { showFilters = false }, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
            FiltersSheet(facets.data, filters, total, onChange = { filters = it }, onDone = { showFilters = false })
        }
    }
}

@kotlinx.serialization.Serializable
private data class FiltersSurrogate(val s: List<String>, val c: List<String>, val g: List<String>, val mn: String, val mx: String, val st: Boolean, val sa: Boolean) {
    fun to() = Filters(s.toSet(), c.toSet(), g.toSet(), mn, mx, st, sa)
    companion object { fun from(f: Filters) = FiltersSurrogate(f.sizes.toList(), f.colors.toList(), f.genders.toList(), f.minPrice, f.maxPrice, f.inStock, f.onSale) }
}

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
private fun FiltersSheet(facets: Facets?, value: Filters, total: Int, onChange: (Filters) -> Unit, onDone: () -> Unit) {
    // Price text is debounced so typing doesn't refetch on every keystroke.
    var minP by remember { mutableStateOf(value.minPrice) }
    var maxP by remember { mutableStateOf(value.maxPrice) }
    LaunchedEffect(minP, maxP) { kotlinx.coroutines.delay(500); if (minP != value.minPrice || maxP != value.maxPrice) onChange(value.copy(minPrice = minP, maxPrice = maxP)) }
    Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp).navigationBarsPadding(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.listing_filters), style = MaterialTheme.typography.headlineSmall, modifier = Modifier.weight(1f))
            if (value.count > 0) TextButton(onClick = { minP = ""; maxP = ""; onChange(Filters()) }) { Text(stringResource(R.string.listing_clear_all)) }
        }
        Row(verticalAlignment = Alignment.CenterVertically) { Text(stringResource(R.string.listing_in_stock_only), Modifier.weight(1f)); Switch(value.inStock, { onChange(value.copy(inStock = it)) }) }
        Row(verticalAlignment = Alignment.CenterVertically) { Text(stringResource(R.string.listing_on_sale_only), Modifier.weight(1f)); Switch(value.onSale, { onChange(value.copy(onSale = it)) }) }
        if ((facets?.genders?.size ?: 0) > 1) {
            Text(stringResource(R.string.listing_gender), style = MaterialTheme.typography.titleMedium)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                facets!!.genders.forEach { g ->
                    val label = stringResource(when (g.value) { "men" -> R.string.listing_genders_men; "women" -> R.string.listing_genders_women; "kids" -> R.string.listing_genders_kids; else -> R.string.listing_genders_unisex })
                    FilterChip(g.value in value.genders, { onChange(value.copy(genders = value.genders.toggle(g.value))) }, label = { Text("$label ${num(g.count)}") })
                }
            }
        }
        if (!facets?.sizes.isNullOrEmpty()) {
            Text(stringResource(R.string.listing_size), style = MaterialTheme.typography.titleMedium)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                facets!!.sizes.forEach { s -> FilterChip(s.label in value.sizes, { onChange(value.copy(sizes = value.sizes.toggle(s.label))) }, label = { Text(s.label) }) }
            }
        }
        if (!facets?.colors.isNullOrEmpty()) {
            Text(stringResource(R.string.listing_color), style = MaterialTheme.typography.titleMedium)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                facets!!.colors.forEach { col ->
                    val on = col.slug in value.colors
                    Box(
                        Modifier.size(38.dp).clip(CircleShape).background(parseHex(col.hex))
                            .border(if (on) 3.dp else 1.dp, if (on) MaterialTheme.colorScheme.onBackground else Color.Black.copy(alpha = 0.15f), CircleShape)
                            .clickable { onChange(value.copy(colors = value.colors.toggle(col.slug))) },
                        contentAlignment = Alignment.Center,
                    ) { if (on) Icon(Icons.Filled.Check, col.name, tint = if (col.hex.lowercase() in listOf("#f5f5f5", "#ffffff", "#d6c3a5")) Color.Black else Color.White) }
                }
            }
        }
        Text(stringResource(R.string.listing_price), style = MaterialTheme.typography.titleMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedTextField(minP, { minP = it.filter(Char::isDigit) }, Modifier.weight(1f), label = { Text(stringResource(R.string.listing_min)) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true, shape = RoundedCornerShape(14.dp))
            OutlinedTextField(maxP, { maxP = it.filter(Char::isDigit) }, Modifier.weight(1f), label = { Text(stringResource(R.string.listing_max)) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true, shape = RoundedCornerShape(14.dp))
        }
        LoadingButton(stringResource(R.string.listing_show_results, num(maxOf(total, 0))), false, onDone, Modifier.fillMaxWidth())
        Spacer(Modifier.height(8.dp))
    }
}

private fun Set<String>.toggle(v: String) = if (v in this) this - v else this + v

/* ---------------- Search with debounced suggestions ---------------- */

@OptIn(FlowPreview::class)
@Composable
fun SearchScreen(nav: NavHostController) {
    val c = LocalAppContainer.current
    val api = c.api
    val scope = rememberCoroutineScope()
    var text by rememberSaveable { mutableStateOf("") }
    var submitted by rememberSaveable { mutableStateOf<String?>(null) }
    var suggest by remember { mutableStateOf<SuggestResponse?>(null) }
    var busy by remember { mutableStateOf(false) }
    val recent by c.local.recentSearches.collectAsState(initial = emptyList())
    val trending = rememberResource("trending") { api.trending().queries }
    val focus = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current

    LaunchedEffect(Unit) {
        snapshotFlow { text.trim() }.distinctUntilChanged().debounce(300).filter { submitted == null }.map { q ->
            if (q.length < 2) null else { busy = true; runCatching { api.suggest(q) }.getOrNull().also { busy = false } }
        }.collect { suggest = it }
    }
    LaunchedEffect(Unit) { if (submitted == null) focus.requestFocus() }
    fun submit(q: String) {
        val v = q.trim(); if (v.isEmpty()) return
        text = v; submitted = v; keyboard?.hide()
        scope.launch { c.local.pushSearch(v) }
    }

    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        OutlinedTextField(
            value = text,
            onValueChange = { text = it; submitted = null },
            modifier = Modifier.fillMaxWidth().padding(16.dp).focusRequester(focus),
            placeholder = { Text(stringResource(R.string.nav_search_placeholder)) },
            leadingIcon = { if (busy) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp) else Icon(Icons.Outlined.Search, null) },
            trailingIcon = { if (text.isNotEmpty()) IconButton(onClick = { text = ""; submitted = null; suggest = null }) { Icon(Icons.Filled.Clear, stringResource(R.string.common_close)) } },
            singleLine = true, shape = CircleShape,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search), keyboardActions = KeyboardActions(onSearch = { submit(text) }),
        )
        val q = submitted
        when {
            q != null -> ProductListScreen(nav, "all", "all", query = q)
            text.trim().length >= 2 -> LazyColumn(contentPadding = PaddingValues(bottom = 24.dp)) {
                item { ListItem(headlineContent = { Text(stringResource(R.string.search_see_all_results, text.trim()), fontWeight = FontWeight.SemiBold) }, leadingContent = { Icon(Icons.Outlined.Search, null, tint = MaterialTheme.colorScheme.primary) }, modifier = Modifier.clickable { submit(text) }) }
                suggest?.let { s ->
                    items(s.products, key = { it.id }) { p ->
                        ListItem(
                            headlineContent = { Text(p.name, maxLines = 1) }, supportingContent = { Text(p.categoryName) },
                            leadingContent = { RemoteImage(p.image?.url, p.name, Modifier.size(48.dp, 60.dp).clip(RoundedCornerShape(10.dp))) },
                            trailingContent = { PriceText(p.price, p.compareAtPrice) },
                            modifier = Modifier.clickable { scope.launch { c.local.pushSearch(text) }; nav.navigate("product/${p.slug}") },
                        )
                    }
                    items(s.categories, key = { "c" + it.slug }) { cat -> ListItem(headlineContent = { Text(cat.name) }, supportingContent = { Text(stringResource(R.string.common_results, num(cat.productCount))) }, modifier = Modifier.clickable { nav.navigate("list/category/${cat.slug}") }) }
                    items(s.collections, key = { "k" + it.slug }) { col -> ListItem(headlineContent = { Text(col.name) }, modifier = Modifier.clickable { nav.navigate("list/collection/${col.slug}") }) }
                    if (s.products.isEmpty() && s.categories.isEmpty()) item { Text(stringResource(R.string.search_no_matches), Modifier.padding(24.dp), color = MaterialTheme.colorScheme.onSurfaceVariant) }
                }
            }
            else -> LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                if (recent.isNotEmpty()) item {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(stringResource(R.string.search_recent), style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                        TextButton(onClick = { scope.launch { c.local.clearSearches() } }) { Text(stringResource(R.string.search_clear)) }
                    }
                    ChipFlow(recent) { submit(it) }
                }
                trending.data?.takeIf { it.isNotEmpty() }?.let { t -> item { Text(stringResource(R.string.search_trending), style = MaterialTheme.typography.titleMedium); Spacer(Modifier.height(8.dp)); ChipFlow(t) { submit(it) } } }
                if (recent.isEmpty() && trending.data.isNullOrEmpty()) item { EmptyState(stringResource(R.string.search_empty), stringResource(R.string.search_empty_hint)) }
            }
        }
    }
}

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
private fun ChipFlow(values: List<String>, onClick: (String) -> Unit) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) { values.forEach { v -> AssistChip(onClick = { onClick(v) }, label = { Text(v) }) } }
}

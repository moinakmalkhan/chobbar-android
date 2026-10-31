package shop.chobbar.app.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import shop.chobbar.app.BuildConfig
import shop.chobbar.app.R
import shop.chobbar.app.data.model.*
import shop.chobbar.app.data.repo.ChatConnection
import shop.chobbar.app.i18n.AppLocale
import shop.chobbar.app.ui.components.*
import shop.chobbar.app.ui.nav.*
import shop.chobbar.app.ui.theme.Brand
import shop.chobbar.app.ui.theme.LocalAppContainer
import shop.chobbar.app.util.Pk
import shop.chobbar.app.util.currentLocale
import shop.chobbar.app.util.formatDate
import shop.chobbar.app.util.formatDateTime

private val CardShape = RoundedCornerShape(18.dp)

@Composable
private fun Modifier.card() = this.fillMaxWidth().clip(CardShape).background(MaterialTheme.colorScheme.surface).border(1.dp, MaterialTheme.colorScheme.outlineVariant, CardShape)

/* ---------------- Account tab ---------------- */

@Composable
fun AccountScreen(nav: NavHostController, onLanguageChange: (String) -> Unit) {
    val c = LocalAppContainer.current
    val user by c.auth.user.collectAsState()
    val unread = rememberResource("unread-${user?.id}") { if (user != null) c.api.unreadCount().count else 0 }
    val summary = rememberResource("summary-${user?.id}") { if (user != null) c.api.ordersSummary() else null }
    val ctx = LocalContext.current
    LazyColumn(Modifier.fillMaxSize().statusBarsPadding(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            if (user == null) {
                Column(Modifier.card().padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    LogoMark(Modifier.width(44.dp))
                    Text(stringResource(R.string.app_welcome_title), style = MaterialTheme.typography.headlineSmall)
                    Text(stringResource(R.string.app_guest_hint), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Button(onClick = { nav.navigate("login") }, shape = CircleShape) { Text(stringResource(R.string.nav_sign_in)) }
                        OutlinedButton(onClick = { nav.navigate("register") }, shape = CircleShape) { Text(stringResource(R.string.nav_sign_up)) }
                    }
                }
            } else {
                val u = user!!
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(56.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer), contentAlignment = Alignment.Center) {
                        if (u.avatarUrl.isNotBlank()) RemoteImage(u.avatarUrl, null, Modifier.fillMaxSize())
                        else Text(u.fullName.split(" ").mapNotNull { it.firstOrNull()?.toString() }.take(2).joinToString("").ifBlank { u.email.take(1) }.uppercase(), color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.width(14.dp))
                    Column {
                        Text(stringResource(R.string.account_hi, u.firstName.ifBlank { u.email.substringBefore('@') }), style = MaterialTheme.typography.headlineSmall)
                        Text(u.email, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                summary.data?.let { s ->
                    Spacer(Modifier.height(14.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Stat(stringResource(R.string.account_open_orders), s.open, Modifier.weight(1f))
                        Stat(stringResource(R.string.account_total_orders), s.total, Modifier.weight(1f))
                        Stat(stringResource(R.string.account_active_returns), s.returns, Modifier.weight(1f))
                    }
                }
            }
        }
        item {
            Column(Modifier.card()) {
                if (user != null) {
                    MenuRow(AppIcons.Package, stringResource(R.string.account_orders)) { nav.navigate("orders") }
                    MenuRow(Icons.Outlined.Notifications, stringResource(R.string.account_notifications), badge = unread.data ?: 0) { nav.navigate("notifications") }
                    MenuRow(AppIcons.Chat, stringResource(R.string.account_messages)) { nav.navigate("chats") }
                    MenuRow(Icons.Outlined.LocationOn, stringResource(R.string.account_addresses)) { nav.navigate("addresses") }
                }
                MenuRow(Icons.Outlined.FavoriteBorder, stringResource(R.string.account_wishlist)) { nav.navigate("wishlist") }
                MenuRow(AppIcons.Truck, stringResource(R.string.nav_track_order)) { nav.navigate("track") }
                MenuRow(Icons.Outlined.Settings, stringResource(R.string.account_settings)) { nav.navigate("settings") }
            }
        }
        item {
            Column(Modifier.card()) {
                MenuRow(AppIcons.Help, stringResource(R.string.footer_faq)) { nav.navigate("faq") }
                MenuRow(AppIcons.Chat, stringResource(R.string.chat_launcher)) { nav.requireAuth(user != null, "chats") }
                MenuRow(Icons.Outlined.Phone, stringResource(R.string.app_whatsapp)) { ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/923000000000"))) }
                MenuRow(Icons.Outlined.Email, stringResource(R.string.app_email_us)) { ctx.startActivity(Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:support@chobbar.shop"))) }
                MenuRow(Icons.Outlined.Info, stringResource(R.string.app_policies)) { nav.navigate("page/shipping-delivery") }
            }
        }
        item { LanguageCard(onLanguageChange) }
        item { Text(stringResource(R.string.app_version, BuildConfig.VERSION_NAME), Modifier.fillMaxWidth(), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = androidx.compose.ui.text.style.TextAlign.Center) }
    }
}

@Composable
private fun Stat(label: String, value: Int, modifier: Modifier) {
    Column(modifier.clip(CardShape).background(MaterialTheme.colorScheme.surfaceVariant).padding(12.dp)) {
        Text(num(value), style = MaterialTheme.typography.headlineMedium)
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
    }
}

@Composable
private fun MenuRow(icon: ImageVector, label: String, badge: Int = 0, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 15.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, Modifier.size(21.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.width(14.dp))
        Text(label, Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
        if (badge > 0) Badge(num(badge), Brand.Rose, Color.White)
        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun LanguageCard(onLanguageChange: (String) -> Unit) {
    val c = LocalAppContainer.current
    val current = AppLocale.current(c.languageStore)
    var confirmed by remember { mutableStateOf(c.languageStore.confirmed) }
    Column(Modifier.card().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) { Icon(AppIcons.Globe, null); Spacer(Modifier.width(10.dp)); Text(stringResource(R.string.app_choose_language), style = MaterialTheme.typography.titleMedium) }
        if (!confirmed) Text(stringResource(R.string.app_detected_language), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("en" to "English", "ur" to "اردو", "ar" to "العربية").forEach { (code, name) ->
                FilterChip(code == current, { c.languageStore.confirmed = true; confirmed = true; if (code != current) onLanguageChange(code) }, label = { Text(name) })
            }
        }
    }
}

/* ---------------- Orders ---------------- */

@Composable
fun OrdersScreen(nav: NavHostController) {
    val api = LocalAppContainer.current.api
    var open by rememberSaveable { mutableStateOf<Boolean?>(null) }
    val orders = rememberResource("orders-$open") { api.orders(1, open).results }
    val ctx = LocalContext.current
    Column(Modifier.fillMaxSize()) {
        BackTopBar(stringResource(R.string.account_orders), nav)
        Row(Modifier.padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(open == null, { open = null }, label = { Text(stringResource(R.string.account_all_orders)) })
            FilterChip(open == true, { open = true }, label = { Text(stringResource(R.string.account_open_orders)) })
            FilterChip(open == false, { open = false }, label = { Text(stringResource(R.string.account_past_orders)) })
        }
        when (val s = orders.state.value) {
            Load.Loading -> Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) { repeat(3) { Shimmer(Modifier.fillMaxWidth().height(96.dp).clip(CardShape)) } }
            is Load.Failed -> ErrorRetry(s.error.userMessage(ctx)) { orders.reload() }
            is Load.Ok -> if (s.data.isEmpty()) EmptyState(stringResource(R.string.account_no_orders), stringResource(R.string.account_no_orders_hint), stringResource(R.string.common_shop_now), { nav.navigate("home") })
            else LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(s.data, key = { it.id }) { o ->
                    Row(Modifier.card().clickable { nav.navigate("order/${o.id}") }.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        RemoteImage(o.thumbnails.firstOrNull(), null, Modifier.size(56.dp, 70.dp).clip(RoundedCornerShape(12.dp)))
                        Column(Modifier.weight(1f).padding(horizontal = 12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(o.number, fontWeight = FontWeight.SemiBold)
                            StatusChip(o.status)
                            Text(stringResource(R.string.account_placed_on, formatDate(o.createdAt, currentLocale())) + " · " + money(o.grandTotal), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null)
                    }
                }
            }
        }
    }
}

private val STEPS = listOf("pending", "confirmed", "processing", "shipped", "out_for_delivery", "delivered")

@Composable
fun OrderProgress(status: String) {
    if (status == "cancelled" || status == "returned") { StatusChip(status); return }
    val current = STEPS.indexOf(if (status == "completed") "delivered" else status).coerceAtLeast(0)
    Column(verticalArrangement = Arrangement.spacedBy(0.dp)) {
        STEPS.forEachIndexed { i, s ->
            val done = i <= current
            Row(verticalAlignment = Alignment.Top) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(Modifier.size(18.dp).clip(CircleShape).background(if (done) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline))
                    if (i < STEPS.lastIndex) Box(Modifier.width(2.dp).height(22.dp).background(if (i < current) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline))
                }
                Spacer(Modifier.width(12.dp))
                Text(statusLabel(s), style = MaterialTheme.typography.bodyMedium, fontWeight = if (i == current) FontWeight.Bold else FontWeight.Normal, color = if (done) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
fun OrderDetailScreen(nav: NavHostController, id: String) {
    val c = LocalAppContainer.current
    val api = c.api
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val order = rememberResource("order-$id") { api.order(id) }
    var cancelOpen by remember { mutableStateOf(false) }
    var returnOpen by remember { mutableStateOf(false) }
    var reviewFor by remember { mutableStateOf<OrderItem?>(null) }
    Column(Modifier.fillMaxSize()) {
        BackTopBar(order.data?.number ?: "", nav)
        when (val s = order.state.value) {
            Load.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            is Load.Failed -> ErrorRetry(s.error.userMessage(ctx)) { order.reload() }
            is Load.Ok -> {
                val o = s.data
                LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    item {
                        Column(Modifier.card().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) { StatusChip(o.status); Spacer(Modifier.width(8.dp)); StatusChip(o.paymentStatus) }
                            OrderProgress(o.status)
                            o.estimatedDelivery?.let { Text(stringResource(R.string.track_estimated) + ": " + formatDate(it, currentLocale()), fontWeight = FontWeight.SemiBold) }
                            if (o.trackingNumber.isNotBlank()) Text("${o.carrier} · ${o.trackingNumber}")
                            if (o.trackingUrl.isNotBlank()) OutlinedButton(onClick = { ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(o.trackingUrl))) }, shape = CircleShape) { Text(stringResource(R.string.track_track_with_courier)) }
                        }
                    }
                    item {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                            OutlinedButton(onClick = {
                                scope.launch { runCatching { api.reorder(o.id) }.onSuccess { c.cart.refresh(); UiMessages.show(it.detail, ctx.getString(R.string.product_view_bag)) { nav.navigate("cart") } }.onFailure { UiMessages.show(it.userMessage(ctx)) } }
                            }, shape = CircleShape) { Text(stringResource(R.string.account_reorder)) }
                            if (o.canCancel) OutlinedButton(onClick = { cancelOpen = true }, shape = CircleShape) { Text(stringResource(R.string.account_cancel_order), color = MaterialTheme.colorScheme.error) }
                            if (o.canReturn) OutlinedButton(onClick = { returnOpen = true }, shape = CircleShape) { Text(stringResource(R.string.account_request_return)) }
                        }
                    }
                    items(o.items, key = { it.id }) { item ->
                        Row(Modifier.card().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            RemoteImage(item.imageUrl, item.productName, Modifier.size(60.dp, 75.dp).clip(RoundedCornerShape(12.dp)).clickable { item.productSlug?.let { nav.navigate("product/$it") } })
                            Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                                Text(item.productName, fontWeight = FontWeight.SemiBold, maxLines = 2)
                                Text("${item.variantLabel} × ${item.quantity}", style = MaterialTheme.typography.bodySmall)
                                if (item.canReview && item.productSlug != null) TextButton(onClick = { reviewFor = item }, contentPadding = PaddingValues(0.dp)) { Icon(Icons.Filled.Star, null, Modifier.size(16.dp), tint = Brand.Amber); Text(" " + stringResource(R.string.account_write_review)) }
                            }
                            Text(money(item.lineTotal), fontWeight = FontWeight.SemiBold)
                        }
                    }
                    item { Column(Modifier.card().padding(16.dp)) { Totals(o.subtotal, o.discountTotal, o.shippingTotal, o.codFee, o.grandTotal) } }
                    item {
                        val a = o.shippingAddress
                        Column(Modifier.card().padding(16.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(stringResource(R.string.account_delivery_to), style = MaterialTheme.typography.titleMedium)
                            Text(a.fullName, fontWeight = FontWeight.SemiBold)
                            Text(listOf(a.line1, a.line2, a.city, a.province).filter { it.isNotBlank() }.joinToString(", "))
                            Text(a.phone)
                        }
                    }
                    if (o.history.isNotEmpty()) item {
                        Column(Modifier.card().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text(stringResource(R.string.account_timeline), style = MaterialTheme.typography.titleMedium)
                            o.history.reversed().forEach { h ->
                                Column { Text(statusLabel(h.toStatus), fontWeight = FontWeight.SemiBold); if (h.note.isNotBlank()) Text(h.note, style = MaterialTheme.typography.bodySmall); Text(formatDateTime(h.createdAt, currentLocale()), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                            }
                        }
                    }
                    item {
                        OutlinedButton(onClick = {
                            scope.launch {
                                runCatching { api.startConversation(StartChatBody(body = ctx.getString(R.string.account_chat_about_order) + " " + o.number, subject = o.number, topic = "order", order = o.id)) }
                                    .onSuccess { nav.navigate("chat/${it.id}") }.onFailure { UiMessages.show(it.userMessage(ctx)) }
                            }
                        }, Modifier.fillMaxWidth(), shape = CircleShape) { Icon(AppIcons.Chat, null); Spacer(Modifier.width(8.dp)); Text(stringResource(R.string.account_chat_about_order)) }
                    }
                }
                if (cancelOpen) CancelDialog(o, { cancelOpen = false }) { order.set(it) }
                if (returnOpen) ReturnSheet(o, { returnOpen = false }) { order.reload() }
                reviewFor?.let { item -> ReviewDialog(item, { reviewFor = null }) { order.reload() } }
            }
        }
    }
}

@Composable
private fun CancelDialog(o: Order, onDismiss: () -> Unit, onDone: (Order) -> Unit) {
    val api = LocalAppContainer.current.api
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var reason by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.account_cancel_confirm)) },
        text = { Column { Text(stringResource(R.string.account_cancel_hint)); Spacer(Modifier.height(10.dp)); OutlinedTextField(reason, { reason = it.take(300) }, label = { Text(stringResource(R.string.account_cancel_reason)) }) } },
        confirmButton = {
            TextButton(enabled = !busy, onClick = {
                busy = true
                scope.launch { runCatching { api.cancel(o.id, CancelBody(reason)) }.onSuccess { onDone(it); UiMessages.show(ctx.getString(R.string.account_cancelled)); onDismiss() }.onFailure { UiMessages.show(it.userMessage(ctx)) }; busy = false }
            }) { Text(stringResource(R.string.account_cancel_order), color = MaterialTheme.colorScheme.error) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.account_keep_order)) } },
    )
}

private val REASONS = listOf("size" to R.string.account_reasons_size, "defect" to R.string.account_reasons_defect, "not_as_described" to R.string.account_reasons_not_as_described, "changed_mind" to R.string.account_reasons_changed_mind, "wrong_item" to R.string.account_reasons_wrong_item, "other" to R.string.account_reasons_other)

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ReturnSheet(o: Order, onDismiss: () -> Unit, onDone: () -> Unit) {
    val api = LocalAppContainer.current.api
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var kind by remember { mutableStateOf("exchange") }
    var reason by remember { mutableStateOf("size") }
    var details by remember { mutableStateOf("") }
    val picked = remember { mutableStateMapOf<String, Int>() }
    var busy by remember { mutableStateOf(false) }
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        LazyColumn(contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item { Text(stringResource(R.string.account_return_title), style = MaterialTheme.typography.headlineSmall) }
            item { Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { FilterChip(kind == "exchange", { kind = "exchange" }, label = { Text(stringResource(R.string.account_exchange)) }); FilterChip(kind == "refund", { kind = "refund" }, label = { Text(stringResource(R.string.account_refund)) }) } }
            items(o.items.filter { it.quantity > it.returnedQuantity }, key = { it.id }) { item ->
                val max = item.quantity - item.returnedQuantity
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox((picked[item.id] ?: 0) > 0, { picked[item.id] = if (it) 1 else 0 })
                    Text("${item.productName} · ${item.variantLabel}", Modifier.weight(1f), maxLines = 2)
                    if ((picked[item.id] ?: 0) > 0 && max > 1) QuantityStepper(picked[item.id]!!, { picked[item.id] = it }, max = max, small = true)
                }
            }
            item {
                Text(stringResource(R.string.account_return_reason), style = MaterialTheme.typography.titleSmall)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) { REASONS.forEach { (k, l) -> FilterChip(reason == k, { reason = k }, label = { Text(stringResource(l)) }) } }
            }
            item { OutlinedTextField(details, { details = it }, Modifier.fillMaxWidth(), label = { Text(stringResource(R.string.account_return_details)) }, minLines = 3) }
            item {
                LoadingButton(stringResource(R.string.account_return_submit), busy, {
                    busy = true
                    scope.launch {
                        runCatching { api.requestReturn(o.id, ReturnBody(kind, reason, details, picked.filterValues { it > 0 }.map { ReturnItemBody(it.key, it.value) })) }
                            .onSuccess { UiMessages.show(ctx.getString(R.string.account_return_done)); onDone(); onDismiss() }.onFailure { UiMessages.show(it.userMessage(ctx)) }
                        busy = false
                    }
                }, Modifier.fillMaxWidth().navigationBarsPadding(), enabled = picked.values.any { it > 0 })
            }
        }
    }
}

@Composable
private fun ReviewDialog(item: OrderItem, onDismiss: () -> Unit, onDone: () -> Unit) {
    val api = LocalAppContainer.current.api
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var rating by remember { mutableIntStateOf(0) }
    var title by remember { mutableStateOf("") }
    var body by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.reviews_write)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(item.productName, style = MaterialTheme.typography.bodySmall)
                Row { (1..5).forEach { i -> IconButton(onClick = { rating = i }) { Icon(Icons.Filled.Star, "$i", tint = if (i <= rating) Brand.Amber else MaterialTheme.colorScheme.outline, modifier = Modifier.size(32.dp)) } } }
                OutlinedTextField(title, { title = it.take(120) }, label = { Text(stringResource(R.string.reviews_review_title)) }, singleLine = true)
                OutlinedTextField(body, { body = it.take(3000) }, label = { Text(stringResource(R.string.reviews_review_body)) }, minLines = 3)
            }
        },
        confirmButton = {
            TextButton(enabled = rating > 0, onClick = {
                scope.launch { runCatching { api.createReview(ReviewBody(item.productSlug!!, rating, title, body)) }.onSuccess { UiMessages.show(ctx.getString(R.string.reviews_thanks)); onDone(); onDismiss() }.onFailure { UiMessages.show(it.userMessage(ctx)) } }
            }) { Text(stringResource(R.string.reviews_submit)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.common_cancel)) } },
    )
}

/* ---------------- Wishlist ---------------- */

@Composable
fun WishlistScreen(nav: NavHostController) {
    val c = LocalAppContainer.current
    val user by c.auth.user.collectAsState()
    val (saved, toggle) = rememberWishlist()
    val key = saved.sorted().joinToString(",")
    val products = rememberResource("wishlist-$key") {
        if (user != null) c.api.wishlist().results.map { it.product }
        else if (saved.isEmpty()) emptyList() else c.api.products(mapOf("slugs" to key, "page_size" to "100")).results
    }
    Column(Modifier.fillMaxSize()) {
        BackTopBar(stringResource(R.string.nav_wishlist), nav)
        val list = products.data?.filter { it.slug in saved }
        when {
            list == null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            list.isEmpty() -> EmptyState(stringResource(R.string.account_empty_wishlist), stringResource(R.string.account_empty_wishlist_hint), stringResource(R.string.common_shop_now), { nav.navigate("home") })
            else -> LazyVerticalGrid(GridCells.Adaptive(158.dp), contentPadding = PaddingValues(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
                if (user == null) item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(maxLineSpan) }) { Text(stringResource(R.string.account_wishlist_guest), style = MaterialTheme.typography.bodySmall) }
                items(list, key = { it.id }) { p -> ProductCardView(p, true, { nav.navigate("product/${p.slug}") }, { toggle(p) }) }
            }
        }
    }
}

/* ---------------- Addresses ---------------- */

@Composable
fun AddressesScreen(nav: NavHostController) {
    val api = LocalAppContainer.current.api
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val list = rememberResource("addresses") { api.addresses() }
    var editing by remember { mutableStateOf<Address?>(null) }
    Column(Modifier.fillMaxSize()) {
        BackTopBar(stringResource(R.string.account_addresses), nav, actions = { TextButton(onClick = { editing = Address() }) { Text(stringResource(R.string.account_add_address)) } })
        val data = list.data
        if (data == null) Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        else if (data.isEmpty()) EmptyState(stringResource(R.string.account_no_addresses), null, stringResource(R.string.account_add_address), { editing = Address() })
        else LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(data, key = { it.id }) { a ->
                Column(Modifier.card().padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(a.fullName, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                        if (a.isDefault) Badge(stringResource(R.string.common_default), MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.primary)
                    }
                    Text(listOf(a.line1, a.line2, a.city, a.province).filter { it.isNotBlank() }.joinToString(", "), style = MaterialTheme.typography.bodyMedium)
                    Text(a.phone, style = MaterialTheme.typography.bodySmall)
                    Row {
                        TextButton(onClick = { editing = a }) { Text(stringResource(R.string.common_edit)) }
                        if (!a.isDefault) TextButton(onClick = { scope.launch { runCatching { api.updateAddress(a.id, a.copy(isDefault = true)) }; list.reload() } }) { Text(stringResource(R.string.account_make_default)) }
                        TextButton(onClick = { scope.launch { runCatching { api.deleteAddress(a.id) }.onSuccess { UiMessages.show(ctx.getString(R.string.account_address_removed)) }; list.reload() } }) { Text(stringResource(R.string.common_delete), color = MaterialTheme.colorScheme.error) }
                    }
                }
            }
        }
    }
    editing?.let { a -> AddressSheet(a, { editing = null }) { list.reload() } }
}

@Composable
private fun AddressSheet(initial: Address, onDismiss: () -> Unit, onSaved: () -> Unit) {
    val api = LocalAppContainer.current.api
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var a by remember { mutableStateOf(initial) }
    var busy by remember { mutableStateOf(false) }
    var errors by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        LazyColumn(contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.imePadding()) {
            item { Text(stringResource(if (initial.id.isBlank()) R.string.account_add_address else R.string.account_edit_address), style = MaterialTheme.typography.headlineSmall) }
            item { OutlinedTextField(a.fullName, { a = a.copy(fullName = it) }, Modifier.fillMaxWidth(), label = { Text(stringResource(R.string.checkout_full_name)) }, isError = errors["full_name"] != null, singleLine = true) }
            item { OutlinedTextField(a.phone, { a = a.copy(phone = it) }, Modifier.fillMaxWidth(), label = { Text(stringResource(R.string.checkout_phone)) }, isError = errors["phone"] != null, supportingText = errors["phone"]?.let { { Text(it) } }, singleLine = true) }
            item { OutlinedTextField(a.line1, { a = a.copy(line1 = it) }, Modifier.fillMaxWidth(), label = { Text(stringResource(R.string.checkout_line1)) }, isError = errors["line1"] != null, singleLine = true) }
            item { OutlinedTextField(a.line2, { a = a.copy(line2 = it) }, Modifier.fillMaxWidth(), label = { Text(stringResource(R.string.checkout_line2)) }, singleLine = true) }
            item { OutlinedTextField(a.city, { a = a.copy(city = it, province = Pk.provinceFor(it) ?: a.province) }, Modifier.fillMaxWidth(), label = { Text(stringResource(R.string.checkout_city)) }, isError = errors["city"] != null, singleLine = true) }
            item { OutlinedTextField(a.province, { a = a.copy(province = it) }, Modifier.fillMaxWidth(), label = { Text(stringResource(R.string.checkout_province)) }, singleLine = true) }
            item { Row(verticalAlignment = Alignment.CenterVertically) { Checkbox(a.isDefault, { a = a.copy(isDefault = it) }); Text(stringResource(R.string.account_make_default)) } }
            item {
                LoadingButton(stringResource(R.string.common_save), busy, {
                    val e = mutableMapOf<String, String>()
                    if (a.fullName.isBlank()) e["full_name"] = ctx.getString(R.string.common_required)
                    if (!Pk.validPhone(a.phone)) e["phone"] = ctx.getString(R.string.checkout_invalid_phone)
                    if (a.line1.isBlank()) e["line1"] = ctx.getString(R.string.common_required)
                    if (a.city.isBlank()) e["city"] = ctx.getString(R.string.common_required)
                    errors = e
                    if (e.isNotEmpty()) return@LoadingButton
                    busy = true
                    scope.launch {
                        runCatching { if (a.id.isBlank()) api.createAddress(a) else api.updateAddress(a.id, a) }
                            .onSuccess { UiMessages.show(ctx.getString(R.string.account_address_saved)); onSaved(); onDismiss() }
                            .onFailure { errors = it.fieldErrors(); UiMessages.show(it.userMessage(ctx)) }
                        busy = false
                    }
                }, Modifier.fillMaxWidth().navigationBarsPadding())
            }
        }
    }
}

/* ---------------- Notifications ---------------- */

@Composable
fun NotificationsScreen(nav: NavHostController) {
    val api = LocalAppContainer.current.api
    val scope = rememberCoroutineScope()
    val items = remember { mutableStateListOf<Notification>() }
    var next by remember { mutableStateOf<String?>(BuildConfig.API_URL + "notifications/") }
    var loading by remember { mutableStateOf(false) }
    suspend fun more() {
        val url = next ?: return
        if (loading) return
        loading = true
        runCatching { api.notifications(url) }.onSuccess { page -> items.addAll(page.results.filter { n -> items.none { it.id == n.id } }); next = page.next }
        loading = false
    }
    LaunchedEffect(Unit) { more() }
    val state = rememberLazyListState()
    val atEnd by remember { derivedStateOf { (state.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0) >= items.size - 3 } }
    LaunchedEffect(atEnd) { if (atEnd) more() }
    Column(Modifier.fillMaxSize()) {
        BackTopBar(stringResource(R.string.account_notifications), nav, actions = {
            if (items.any { !it.isRead }) TextButton(onClick = { scope.launch { runCatching { api.readAll() }; val copy = items.map { it.copy(isRead = true) }; items.clear(); items.addAll(copy) } }) { Text(stringResource(R.string.account_mark_all_read)) }
        })
        if (items.isEmpty() && !loading) EmptyState(stringResource(R.string.account_all_caught_up))
        LazyColumn(state = state, contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(items, key = { it.id }) { n ->
                Row(Modifier.card().background(if (!n.isRead) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f) else Color.Transparent).clickable {
                    scope.launch { runCatching { api.markRead(n.id) } }
                    val i = items.indexOfFirst { it.id == n.id }; if (i >= 0) items[i] = n.copy(isRead = true)
                    DeepLinks.route("https://chobbar.shop" + n.url)?.let { nav.navigate(it) }
                }.padding(14.dp)) {
                    Column(Modifier.weight(1f)) {
                        Text(n.title, fontWeight = if (n.isRead) FontWeight.Medium else FontWeight.Bold)
                        if (n.body.isNotBlank()) Text(n.body, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(formatDateTime(n.createdAt, currentLocale()), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    if (!n.isRead) Box(Modifier.padding(top = 6.dp).size(8.dp).clip(CircleShape).background(Brand.Rose))
                }
            }
            if (loading) item { Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { CircularProgressIndicator(Modifier.size(24.dp)) } }
        }
    }
}

/* ---------------- Settings ---------------- */

@Composable
fun SettingsScreen(nav: NavHostController, onLanguageChange: (String) -> Unit) {
    val c = LocalAppContainer.current
    val api = c.api
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val user by c.auth.user.collectAsState()
    val theme by c.local.theme.collectAsState(initial = "system")
    var first by remember(user) { mutableStateOf(user?.firstName.orEmpty()) }
    var last by remember(user) { mutableStateOf(user?.lastName.orEmpty()) }
    var phone by remember(user) { mutableStateOf(user?.phone.orEmpty()) }
    var current by remember { mutableStateOf("") }
    var newPass by remember { mutableStateOf("") }
    var confirmDelete by remember { mutableStateOf(false) }
    fun patch(p: ProfilePatch, msg: Int = R.string.account_profile_saved) = scope.launch {
        runCatching { api.updateMe(p) }.onSuccess { c.auth.setUser(it); UiMessages.show(ctx.getString(msg)) }.onFailure { UiMessages.show(it.userMessage(ctx)) }
    }
    Column(Modifier.fillMaxSize()) {
        BackTopBar(stringResource(R.string.account_settings), nav)
        LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            item { LanguageCard(onLanguageChange) }
            item {
                Column(Modifier.card().padding(16.dp)) {
                    Text(stringResource(R.string.app_theme), style = MaterialTheme.typography.titleMedium)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("system" to R.string.app_theme_system, "light" to R.string.app_theme_light, "dark" to R.string.app_theme_dark).forEach { (v, l) ->
                            FilterChip(theme == v, { scope.launch { c.local.setTheme(v) } }, label = { Text(stringResource(l)) })
                        }
                    }
                }
            }
            val u = user
            if (u != null) {
                item {
                    Column(Modifier.card().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(stringResource(R.string.account_profile), style = MaterialTheme.typography.titleMedium)
                        OutlinedTextField(first, { first = it }, Modifier.fillMaxWidth(), label = { Text(stringResource(R.string.auth_first_name)) }, singleLine = true)
                        OutlinedTextField(last, { last = it }, Modifier.fillMaxWidth(), label = { Text(stringResource(R.string.auth_last_name)) }, singleLine = true)
                        OutlinedTextField(phone, { phone = it }, Modifier.fillMaxWidth(), label = { Text(stringResource(R.string.auth_phone)) }, singleLine = true)
                        Button(onClick = { patch(ProfilePatch(firstName = first, lastName = last, phone = phone)) }, shape = CircleShape) { Text(stringResource(R.string.common_save_changes)) }
                    }
                }
                item {
                    Column(Modifier.card().padding(16.dp)) {
                        Text(stringResource(R.string.account_preferences), style = MaterialTheme.typography.titleMedium)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) { Text(stringResource(R.string.account_pref_orders)); Text(stringResource(R.string.account_pref_orders_hint), style = MaterialTheme.typography.bodySmall) }
                            Switch(u.notificationPreferences["orders"] != false, { patch(ProfilePatch(notificationPreferences = u.notificationPreferences + ("orders" to it))) })
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(stringResource(R.string.account_pref_promos), Modifier.weight(1f))
                            Switch(u.marketingOptIn, { patch(ProfilePatch(marketingOptIn = it)) })
                        }
                    }
                }
                item {
                    Column(Modifier.card().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(stringResource(if (u.hasPassword) R.string.account_change_password else R.string.account_set_password), style = MaterialTheme.typography.titleMedium)
                        if (u.hasPassword) PasswordField(current, { current = it }, stringResource(R.string.account_current_password))
                        PasswordField(newPass, { newPass = it }, stringResource(R.string.auth_new_password), hint = stringResource(R.string.auth_password_hint))
                        Button(enabled = newPass.length >= 10, onClick = {
                            scope.launch { runCatching { api.changePassword(ChangePasswordBody(current, newPass)) }.onSuccess { current = ""; newPass = ""; UiMessages.show(ctx.getString(R.string.account_password_changed)) }.onFailure { UiMessages.show(it.userMessage(ctx)) } }
                        }, shape = CircleShape) { Text(stringResource(R.string.common_save)) }
                    }
                }
                item {
                    OutlinedButton(onClick = { scope.launch { c.auth.logout(); UiMessages.show(ctx.getString(R.string.auth_signed_out)); nav.navigate("home") { popUpTo("home") { inclusive = true } } } }, Modifier.fillMaxWidth().height(50.dp), shape = CircleShape) {
                        Icon(Icons.AutoMirrored.Filled.ExitToApp, null); Spacer(Modifier.width(8.dp)); Text(stringResource(R.string.nav_sign_out))
                    }
                }
                item { TextButton(onClick = { confirmDelete = true }, Modifier.fillMaxWidth()) { Text(stringResource(R.string.account_delete_confirm), color = MaterialTheme.colorScheme.error) } }
            }
        }
    }
    if (confirmDelete) AlertDialog(
        onDismissRequest = { confirmDelete = false },
        title = { Text(stringResource(R.string.account_delete_confirm)) },
        text = { Text(stringResource(R.string.account_delete_hint)) },
        confirmButton = { TextButton(onClick = { scope.launch { runCatching { api.deleteMe() }.onSuccess { c.auth.logout(); UiMessages.show(ctx.getString(R.string.account_deleted)); nav.navigate("home") { popUpTo("home") { inclusive = true } } }.onFailure { UiMessages.show(it.userMessage(ctx)) } } }) { Text(stringResource(R.string.common_delete), color = MaterialTheme.colorScheme.error) } },
        dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text(stringResource(R.string.common_cancel)) } },
    )
}

/* ---------------- Track, FAQ, pages ---------------- */

@Composable
fun TrackScreen(nav: NavHostController) {
    val c = LocalAppContainer.current
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val saved by c.local.contact.collectAsState(initial = null)
    var number by rememberSaveable { mutableStateOf("") }
    var contact by rememberSaveable { mutableStateOf("") }
    var info by remember { mutableStateOf<TrackingInfo?>(null) }
    var busy by remember { mutableStateOf(false) }
    LaunchedEffect(saved) { if (contact.isBlank()) contact = saved?.email.orEmpty() }
    Column(Modifier.fillMaxSize()) {
        BackTopBar(stringResource(R.string.track_title), nav)
        LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item { Text(stringResource(R.string.track_subtitle), color = MaterialTheme.colorScheme.onSurfaceVariant) }
            item { OutlinedTextField(number, { number = it.uppercase() }, Modifier.fillMaxWidth(), label = { Text(stringResource(R.string.track_order_number)) }, singleLine = true) }
            item { OutlinedTextField(contact, { contact = it }, Modifier.fillMaxWidth(), label = { Text(stringResource(R.string.track_contact)) }, singleLine = true) }
            item {
                LoadingButton(stringResource(R.string.track_submit), busy, {
                    busy = true
                    scope.launch { runCatching { c.api.track(number.trim(), contact.trim()) }.onSuccess { info = it }.onFailure { info = null; UiMessages.show(it.userMessage(ctx)) }; busy = false }
                }, Modifier.fillMaxWidth(), enabled = number.isNotBlank() && contact.isNotBlank())
            }
            info?.let { t ->
                item {
                    Column(Modifier.card().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(t.number, style = MaterialTheme.typography.titleLarge)
                        OrderProgress(t.status)
                        t.estimatedDelivery?.let { Text(stringResource(R.string.track_estimated) + ": " + formatDate(it, currentLocale())) }
                        if (t.carrier.isNotBlank()) Text(stringResource(R.string.track_courier) + ": ${t.carrier} ${t.trackingNumber}")
                        t.items.forEach { Text("• ${it.name} (${it.variant}) × ${it.quantity}", style = MaterialTheme.typography.bodySmall) }
                        Text(stringResource(R.string.cart_total) + ": " + money(t.grandTotal), fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun FaqScreen(nav: NavHostController) {
    val api = LocalAppContainer.current.api
    val faqs = rememberResource("faq") { api.faq() }
    Column(Modifier.fillMaxSize()) {
        BackTopBar(stringResource(R.string.footer_faq), nav)
        LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            item { Text(stringResource(R.string.faq_subtitle), color = MaterialTheme.colorScheme.onSurfaceVariant) }
            items(faqs.data ?: emptyList(), key = { it.id }) { f ->
                var open by rememberSaveable { mutableStateOf(false) }
                Column(Modifier.card().clickable { open = !open }.padding(16.dp).animateContentSize()) {
                    Text(f.question, fontWeight = FontWeight.SemiBold)
                    if (open) Text(f.answer, Modifier.padding(top = 8.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            item { OutlinedButton(onClick = { nav.navigate("chats") }, Modifier.fillMaxWidth(), shape = CircleShape) { Text(stringResource(R.string.faq_still_need_help)) } }
        }
    }
}


@Composable
fun ContentPageScreen(nav: NavHostController, slug: String) {
    val api = LocalAppContainer.current.api
    val page = rememberResource("page-$slug") { api.page(slug) }
    Column(Modifier.fillMaxSize()) {
        BackTopBar(page.data?.title ?: "", nav)
        LazyColumn(contentPadding = PaddingValues(16.dp)) {
            item {
                // Markdown from the admin rendered as plain paragraphs (headings bold, list markers kept).
                page.data?.body?.split("\n")?.forEach { line ->
                    val t = line.trim()
                    when {
                        t.startsWith("#") -> Text(t.trimStart('#', ' '), style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(top = 14.dp, bottom = 6.dp))
                        t.isNotEmpty() -> Text(t.replace("**", ""), style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(bottom = 6.dp))
                    }
                }
            }
        }
    }
}

/* ---------------- Support chat ---------------- */

@Composable
fun ChatListScreen(nav: NavHostController) {
    val api = LocalAppContainer.current.api
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val list = rememberResource("chats") { api.conversations().results }
    var body by remember { mutableStateOf("") }
    Column(Modifier.fillMaxSize()) {
        BackTopBar(stringResource(R.string.account_messages), nav)
        LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            val data = list.data
            if (data != null && data.isEmpty()) item { EmptyState(stringResource(R.string.account_no_conversations), stringResource(R.string.account_start_chat)) }
            items(data ?: emptyList(), key = { it.id }) { conv ->
                Row(Modifier.card().clickable { nav.navigate("chat/${conv.id}") }.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(conv.subject, fontWeight = FontWeight.SemiBold, maxLines = 1)
                        Text(conv.lastMessagePreview, style = MaterialTheme.typography.bodySmall, maxLines = 1, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    if (conv.unreadCount > 0) Badge(num(conv.unreadCount), Brand.Rose, Color.White)
                }
            }
        }
        Row(Modifier.fillMaxWidth().navigationBarsPadding().imePadding().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(body, { body = it }, Modifier.weight(1f), placeholder = { Text(stringResource(R.string.account_new_conversation)) }, shape = CircleShape, maxLines = 3)
            IconButton(enabled = body.isNotBlank(), onClick = {
                val text = body; body = ""
                scope.launch { runCatching { api.startConversation(StartChatBody(body = text, topic = "other")) }.onSuccess { nav.navigate("chat/${it.id}") }.onFailure { UiMessages.show(it.userMessage(ctx)) } }
            }) { Icon(Icons.AutoMirrored.Filled.Send, stringResource(R.string.chat_send), tint = MaterialTheme.colorScheme.primary) }
        }
    }
}

@Composable
fun ChatScreen(nav: NavHostController, id: String) {
    val c = LocalAppContainer.current
    val user by c.auth.user.collectAsState()
    val messages = remember { mutableStateListOf<ChatMessage>() }
    var typing by remember { mutableStateOf<String?>(null) }
    var text by remember { mutableStateOf("") }
    var conn by remember { mutableStateOf<ChatConnection?>(null) }
    val list = rememberLazyListState()

    fun upsert(m: ChatMessage) {
        val i = messages.indexOfFirst { it.id == m.id || (m.clientId.isNotBlank() && it.clientId == m.clientId) }
        if (i >= 0) messages[i] = m.copy(pending = false) else messages.add(m)
    }
    LaunchedEffect(id) {
        runCatching { c.api.messages(BuildConfig.API_URL + "chat/conversations/$id/messages/") }.onSuccess { page -> messages.clear(); messages.addAll(page.results.reversed()) }
        conn = c.realtime.chat(id, onMessage = { m -> upsert(m) }, onTyping = { name -> typing = name })
    }
    DisposableEffect(id) { onDispose { conn?.close() } }
    LaunchedEffect(messages.size) { if (messages.isNotEmpty()) { list.animateScrollToItem(messages.lastIndex); conn?.read() } }
    // Typing indicator: on while typing, off 1.5 s after the last keystroke.
    LaunchedEffect(text) { if (text.isNotEmpty()) { conn?.typing(true); delay(1500); conn?.typing(false) } }

    Column(Modifier.fillMaxSize()) {
        BackTopBar(stringResource(R.string.chat_title), nav)
        LazyColumn(Modifier.weight(1f), state = list, contentPadding = PaddingValues(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            items(messages, key = { it.id }) { m ->
                val mine = m.sender?.id == user?.id
                if (m.kind == "system") Text(m.body, Modifier.fillMaxWidth(), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                else Row(Modifier.fillMaxWidth(), horizontalArrangement = if (mine) Arrangement.End else Arrangement.Start) {
                    Column(
                        Modifier.widthIn(max = 290.dp).clip(RoundedCornerShape(18.dp)).background(if (mine) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant).padding(horizontal = 14.dp, vertical = 9.dp),
                    ) {
                        if (!mine && m.sender?.isStaff == true) Text(m.sender.name, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                        Text(m.body, color = if (mine) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface)
                        Text((if (m.pending) "… " else "") + formatDateTime(m.createdAt, currentLocale()), style = MaterialTheme.typography.labelSmall, color = (if (mine) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant).copy(alpha = 0.7f))
                    }
                }
            }
            typing?.let { name -> item { Text(stringResource(R.string.chat_typing, name), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant) } }
        }
        Row(Modifier.fillMaxWidth().navigationBarsPadding().imePadding().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(text, { text = it }, Modifier.weight(1f), placeholder = { Text(stringResource(R.string.chat_placeholder)) }, shape = CircleShape, maxLines = 4)
            IconButton(enabled = text.isNotBlank(), onClick = {
                val body = text.trim(); text = ""
                val clientId = conn?.send(body) ?: ""
                upsert(ChatMessage(id = clientId.ifBlank { System.nanoTime().toString() }, body = body, clientId = clientId, sender = user?.let { ChatSender(it.id, it.fullName) }, pending = true))
            }) { Icon(Icons.AutoMirrored.Filled.Send, stringResource(R.string.chat_send), tint = MaterialTheme.colorScheme.primary) }
        }
    }
}

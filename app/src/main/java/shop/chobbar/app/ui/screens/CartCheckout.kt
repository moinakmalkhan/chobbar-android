package shop.chobbar.app.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import shop.chobbar.app.R
import shop.chobbar.app.data.local.CheckoutContact
import shop.chobbar.app.data.model.*
import shop.chobbar.app.ui.components.*
import shop.chobbar.app.ui.nav.Load
import shop.chobbar.app.ui.nav.UiMessages
import shop.chobbar.app.ui.nav.fieldErrors
import shop.chobbar.app.ui.nav.rememberResource
import shop.chobbar.app.ui.nav.userMessage
import shop.chobbar.app.ui.theme.Brand
import shop.chobbar.app.ui.theme.LocalAppContainer
import shop.chobbar.app.util.Pk

/* ---------------- Bag ---------------- */

@Composable
fun CartScreen(nav: NavHostController) {
    val c = LocalAppContainer.current
    val cart by c.cart.cart.collectAsState()
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var loaded by remember { mutableStateOf(cart != null) }
    LaunchedEffect(Unit) { c.cart.refresh(); loaded = true }

    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        Text(stringResource(R.string.cart_title) + (cart?.quantity?.takeIf { it > 0 }?.let { " (${it})" } ?: ""), style = MaterialTheme.typography.headlineMedium, modifier = Modifier.padding(16.dp))
        val current = cart
        when {
            !loaded && current == null -> Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) { repeat(3) { Shimmer(Modifier.fillMaxWidth().height(110.dp).clip(RoundedCornerShape(18.dp))) } }
            current == null || current.items.isEmpty() -> EmptyState(stringResource(R.string.cart_empty), stringResource(R.string.cart_empty_hint), stringResource(R.string.common_shop_now), { nav.navigate("list/collection/new-arrivals") })
            else -> {
                LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    item { FreeDeliveryBar(current) }
                    if (current.issues.isNotEmpty()) item { Text(current.issues.joinToString("\n"), color = Brand.Warning, style = MaterialTheme.typography.bodySmall) }
                    items(current.items, key = { it.id }) { item -> CartLine(item, nav) }
                    item { CouponBox(current) }
                    item { Totals(current.subtotal, current.discount, null, 0.0, current.subtotal - current.discount, current.freeShipping) }
                }
                Surface(shadowElevation = 8.dp) {
                    Column(Modifier.fillMaxWidth().padding(16.dp)) {
                        LoadingButton(stringResource(R.string.cart_checkout) + " · " + money(current.subtotal - current.discount), false, { nav.navigate("checkout") }, Modifier.fillMaxWidth())
                        Text(stringResource(R.string.cart_secure), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.align(Alignment.CenterHorizontally).padding(top = 6.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun CartLine(item: CartItem, nav: NavHostController) {
    val c = LocalAppContainer.current
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    // Steppers update instantly; the API is called once the taps stop (350 ms debounce).
    var local by remember(item.id, item.quantity) { mutableIntStateOf(item.quantity) }
    LaunchedEffect(local) {
        if (local == item.quantity) return@LaunchedEffect
        delay(350)
        runCatching { c.cart.setQuantity(item.id, local) }.onFailure { UiMessages.show(it.userMessage(ctx)); local = item.quantity }
    }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        RemoteImage(item.product.image?.url, item.product.name, Modifier.size(88.dp, 110.dp).clip(RoundedCornerShape(14.dp)).clickable { nav.navigate("product/${item.product.slug}") })
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Row {
                Text(item.product.name, Modifier.weight(1f), style = MaterialTheme.typography.titleSmall, maxLines = 2)
                IconButton(onClick = { scope.launch { runCatching { c.cart.remove(item.id) }.onSuccess { UiMessages.show(ctx.getString(R.string.cart_removed)) }.onFailure { UiMessages.show(it.userMessage(ctx)) } } }, Modifier.size(32.dp)) {
                    Icon(Icons.Filled.Delete, stringResource(R.string.cart_remove), Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Text(item.variant.label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (item.issue.isNotBlank()) Text(item.issue, style = MaterialTheme.typography.labelSmall, color = Brand.Warning)
            Spacer(Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                QuantityStepper(local, { local = it }, max = item.available.coerceIn(1, 10).coerceAtLeast(item.quantity), small = true)
                Spacer(Modifier.weight(1f))
                Text(money(item.unitPrice * local), fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun FreeDeliveryBar(cart: Cart) {
    val c = LocalAppContainer.current
    val sf = rememberResource("storefront") { c.api.storefront() }
    val contact by c.local.contact.collectAsState(initial = null)
    val zones = sf.data?.shipping?.zones ?: return
    val zone = contact?.city?.let { city -> zones.firstOrNull { z -> z.cities.any { it.equals(city, true) } } ?: zones.firstOrNull { z -> z.provinces.any { it.equals(contact?.province, true) } } }
    val threshold = zone?.freeOver ?: sf.data?.shipping?.freeOverMin ?: return
    val spent = cart.subtotal - cart.discount
    val done = cart.freeShipping || spent >= threshold
    val pct by animateFloatAsState(if (done) 1f else (spent / threshold).toFloat().coerceIn(0.04f, 1f), tween(700), label = "free")
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(MaterialTheme.colorScheme.surfaceVariant).padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(AppIcons.Truck, null, Modifier.size(18.dp), tint = if (done) Brand.Success else MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.width(8.dp))
            Text(if (done) stringResource(R.string.cart_free_delivery_unlocked) else stringResource(R.string.cart_free_delivery_progress, money(threshold - spent)), style = MaterialTheme.typography.bodyMedium, color = if (done) Brand.Success else MaterialTheme.colorScheme.onBackground)
        }
        LinearProgressIndicator(progress = { pct }, Modifier.fillMaxWidth().height(6.dp).clip(CircleShape), color = if (done) Brand.Success else MaterialTheme.colorScheme.primary, trackColor = MaterialTheme.colorScheme.outline)
    }
}

@Composable
private fun CouponBox(cart: Cart) {
    val c = LocalAppContainer.current
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var code by rememberSaveable { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    if (cart.couponCode.isNotBlank()) {
        Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).border(1.dp, Brand.Success.copy(alpha = 0.5f), RoundedCornerShape(16.dp)).padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) { Text(cart.couponCode, fontWeight = FontWeight.Bold); Text(cart.couponMessage, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            TextButton(onClick = { scope.launch { runCatching { c.cart.removeCoupon() }.onSuccess { UiMessages.show(ctx.getString(R.string.cart_coupon_removed)) } } }) { Text(stringResource(R.string.common_remove)) }
        }
        return
    }
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(code, { code = it.uppercase().trim() }, Modifier.weight(1f), placeholder = { Text(stringResource(R.string.cart_coupon_placeholder)) }, singleLine = true, shape = CircleShape,
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters))
        Button(onClick = {
            busy = true
            scope.launch {
                runCatching { c.cart.applyCoupon(code) }
                    .onSuccess { r -> if (r.couponCode.isNotBlank()) { UiMessages.show(ctx.getString(R.string.cart_coupon_applied)); code = "" } else UiMessages.show(r.couponMessage) }
                    .onFailure { UiMessages.show(it.userMessage(ctx)) }
                busy = false
            }
        }, enabled = code.isNotBlank() && !busy, shape = CircleShape, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)) { Text(stringResource(R.string.cart_apply)) }
    }
}

@Composable
fun Totals(subtotal: Double, discount: Double, shipping: Double?, codFee: Double, total: Double, freeShipping: Boolean = false) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        TotalRow(stringResource(R.string.cart_subtotal), money(subtotal))
        if (discount > 0) TotalRow(stringResource(R.string.cart_discount), "−" + money(discount), Brand.Success)
        TotalRow(stringResource(R.string.cart_delivery), when { shipping == null -> if (freeShipping) stringResource(R.string.common_free) else stringResource(R.string.cart_delivery_calculated); shipping == 0.0 -> stringResource(R.string.common_free); else -> money(shipping) })
        if (codFee > 0) TotalRow(stringResource(R.string.checkout_cod_fee), money(codFee))
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        Row { Text(stringResource(R.string.cart_total), Modifier.weight(1f), style = MaterialTheme.typography.titleLarge); Text(money(total), style = MaterialTheme.typography.titleLarge) }
    }
}

@Composable
private fun TotalRow(label: String, value: String, color: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onBackground) {
    Row { Text(label, Modifier.weight(1f), color = MaterialTheme.colorScheme.onSurfaceVariant); Text(value, color = color) }
}

/* ---------------- Checkout ---------------- */

@OptIn(FlowPreview::class)
@Composable
fun CheckoutScreen(nav: NavHostController) {
    val c = LocalAppContainer.current
    val api = c.api
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val cart by c.cart.cart.collectAsState()
    val user by c.auth.user.collectAsState()
    val addresses = rememberResource("addresses-${user?.id}") { if (user != null) api.addresses() else emptyList() }

    var email by rememberSaveable { mutableStateOf("") }
    var phone by rememberSaveable { mutableStateOf("") }
    var name by rememberSaveable { mutableStateOf("") }
    var line1 by rememberSaveable { mutableStateOf("") }
    var line2 by rememberSaveable { mutableStateOf("") }
    var city by rememberSaveable { mutableStateOf("") }
    var province by rememberSaveable { mutableStateOf("") }
    var payment by rememberSaveable { mutableStateOf("cod") }
    var notes by rememberSaveable { mutableStateOf("") }
    var gift by rememberSaveable { mutableStateOf(false) }
    var saveAddress by rememberSaveable { mutableStateOf(true) }
    var selected by rememberSaveable { mutableStateOf<String?>(null) }
    var errors by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    var placing by remember { mutableStateOf(false) }
    var quote by remember { mutableStateOf<ShippingQuote?>(null) }

    // Prefill: profile, then what this device remembers from the last checkout.
    LaunchedEffect(user) {
        val saved = c.local.contact.first()
        if (email.isBlank()) email = user?.email ?: saved?.email.orEmpty()
        if (phone.isBlank()) phone = user?.phone?.ifBlank { null } ?: saved?.phone.orEmpty()
        if (name.isBlank()) name = user?.fullName?.ifBlank { null } ?: saved?.fullName.orEmpty()
        if (city.isBlank()) city = saved?.city.orEmpty()
        if (province.isBlank()) province = saved?.province.orEmpty()
    }
    fun apply(a: Address) { selected = a.id; name = a.fullName; phone = a.phone; line1 = a.line1; line2 = a.line2; city = a.city; province = a.province; saveAddress = false }
    LaunchedEffect(addresses.data) { if (selected == null) addresses.data?.let { list -> (list.firstOrNull { it.isDefault } ?: list.firstOrNull())?.let(::apply) } }
    // Delivery charge follows the city/payment method, debounced 400 ms.
    LaunchedEffect(Unit) {
        snapshotFlow { Triple(city.trim(), province, payment) }.distinctUntilChanged().debounce(400).collectLatest { (ct, pr, pm) ->
            quote = if (ct.length < 2) null else runCatching { api.shippingQuote(QuoteBody(ct, pr, pm)) }.getOrNull()
        }
    }

    val current = cart
    if (current == null || current.items.isEmpty()) {
        Column(Modifier.fillMaxSize()) { BackTopBar(stringResource(R.string.checkout_title), nav); EmptyState(stringResource(R.string.checkout_empty_bag), null, stringResource(R.string.common_shop_now), { nav.navigate("home") }) }
        return
    }

    fun validate(): Boolean {
        val e = mutableMapOf<String, String>()
        if (!Pk.validEmail(email)) e["email"] = ctx.getString(R.string.auth_invalid_email)
        if (!Pk.validPhone(phone)) e["phone"] = ctx.getString(R.string.checkout_invalid_phone)
        if (name.trim().length < 2) e["full_name"] = ctx.getString(R.string.common_required)
        if (line1.trim().length < 4) e["line1"] = ctx.getString(R.string.common_required)
        if (city.trim().length < 2) e["city"] = ctx.getString(R.string.common_required)
        errors = e
        return e.isEmpty()
    }

    fun place() {
        if (!validate()) { UiMessages.show(ctx.getString(R.string.errors_check_form)); return }
        placing = true
        scope.launch {
            try {
                val order = api.checkout(
                    CheckoutBody(
                        email = email.trim(), paymentMethod = payment, notes = notes, isGift = gift,
                        saveAddress = user != null && selected == null && saveAddress,
                        address = CheckoutAddress(name.trim(), phone.trim(), line1.trim(), line2.trim(), city.trim(), province),
                    ),
                )
                c.local.saveContact(CheckoutContact(email.trim(), phone.trim(), name.trim(), city.trim(), province))
                c.cart.clearLocal()
                launch { c.cart.refresh() }
                SuccessCache.order = order
                nav.navigate("success/${order.id}") { popUpTo("cart") }
            } catch (e: Exception) {
                errors = e.fieldErrors()
                UiMessages.show(e.userMessage(ctx))
                c.cart.refresh()
            } finally { placing = false }
        }
    }

    val shipping = quote?.rate
    val codFee = if (payment == "cod") quote?.codFee ?: 0.0 else 0.0
    val total = quote?.total ?: (current.subtotal - current.discount)
    val tf = Modifier.fillMaxWidth()
    val shape = RoundedCornerShape(14.dp)

    Column(Modifier.fillMaxSize()) {
        BackTopBar(stringResource(R.string.checkout_title), nav)
        LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            item { Text(stringResource(R.string.checkout_contact), style = MaterialTheme.typography.titleLarge) }
            if (user == null) item { TextButton(onClick = { nav.navigate("login") }) { Text(stringResource(R.string.checkout_sign_in_prompt) + " " + stringResource(R.string.nav_sign_in)) } }
            item { Field(email, { email = it }, R.string.checkout_email, errors["email"], KeyboardType.Email) }
            item { Field(phone, { phone = it }, R.string.checkout_phone, errors["phone"] ?: errors["address.phone"], KeyboardType.Phone, hint = stringResource(R.string.checkout_phone_hint)) }
            item { Text(stringResource(R.string.checkout_delivery), style = MaterialTheme.typography.titleLarge) }
            addresses.data?.takeIf { it.isNotEmpty() }?.let { list ->
                items(list, key = { it.id }) { a ->
                    val on = selected == a.id
                    Column(Modifier.fillMaxWidth().clip(shape).border(if (on) 2.dp else 1.dp, if (on) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.outline, shape).clickable { apply(a) }.padding(14.dp)) {
                        Text(a.fullName, fontWeight = FontWeight.SemiBold)
                        Text(listOf(a.line1, a.line2, a.city).filter { it.isNotBlank() }.joinToString(", "), style = MaterialTheme.typography.bodySmall)
                        Text(a.phone, style = MaterialTheme.typography.bodySmall)
                    }
                }
                item { OutlinedButton(onClick = { selected = null; line1 = ""; line2 = ""; saveAddress = true }, shape = CircleShape) { Text(stringResource(R.string.checkout_new_address)) } }
            }
            if (selected == null) {
                item { Field(name, { name = it }, R.string.checkout_full_name, errors["full_name"]) }
                item { Field(line1, { line1 = it }, R.string.checkout_line1, errors["line1"]) }
                item { Field(line2, { line2 = it }, R.string.checkout_line2, null) }
                item {
                    Field(city, { city = it; Pk.provinceFor(it)?.let { p -> province = p } }, R.string.checkout_city, errors["city"])
                    val matches = Pk.CITIES.keys.filter { city.length >= 2 && it.startsWith(city.trim(), true) && !it.equals(city.trim(), true) }.take(4)
                    if (matches.isNotEmpty()) Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(top = 6.dp)) { matches.forEach { m -> AssistChip(onClick = { city = m; province = Pk.CITIES[m].orEmpty() }, label = { Text(m) }) } }
                }
                item {
                    var open by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(open, { open = it }) {
                        OutlinedTextField(province, {}, tf.menuAnchor(MenuAnchorType.PrimaryNotEditable), readOnly = true, label = { Text(stringResource(R.string.checkout_province)) }, shape = shape)
                        ExposedDropdownMenu(open, { open = false }) { Pk.PROVINCES.forEach { p -> DropdownMenuItem(text = { Text(p) }, onClick = { province = p; open = false }) } }
                    }
                }
                if (user != null) item { Row(verticalAlignment = Alignment.CenterVertically) { Checkbox(saveAddress, { saveAddress = it }); Text(stringResource(R.string.checkout_save_address)) } }
            }
            item {
                Row(Modifier.fillMaxWidth().clip(shape).background(MaterialTheme.colorScheme.surfaceVariant).padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(AppIcons.Truck, null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(10.dp))
                    val q = quote
                    if (q == null) Text(stringResource(R.string.checkout_enter_city), style = MaterialTheme.typography.bodyMedium)
                    else {
                        Column(Modifier.weight(1f)) { Text(q.zone.orEmpty(), fontWeight = FontWeight.SemiBold); Text(stringResource(R.string.checkout_eta, num(q.minDays), num(q.maxDays)), style = MaterialTheme.typography.bodySmall) }
                        Text(if (q.rate == 0.0) stringResource(R.string.common_free) else money(q.rate), fontWeight = FontWeight.Bold)
                    }
                }
            }
            item { Text(stringResource(R.string.checkout_payment), style = MaterialTheme.typography.titleLarge) }
            item {
                listOf(Triple("cod", R.string.checkout_cod, R.string.checkout_cod_hint), Triple("bank_transfer", R.string.checkout_bank, R.string.checkout_bank_hint)).forEach { (code, title, hint) ->
                    val on = payment == code
                    Row(Modifier.fillMaxWidth().padding(bottom = 10.dp).clip(shape).border(if (on) 2.dp else 1.dp, if (on) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline, shape).clickable { payment = code }.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(on, { payment = code })
                        Column { Text(stringResource(title), fontWeight = FontWeight.SemiBold); Text(stringResource(hint), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                    }
                }
            }
            item { OutlinedTextField(notes, { notes = it.take(500) }, tf, label = { Text(stringResource(R.string.checkout_notes)) }, placeholder = { Text(stringResource(R.string.checkout_notes_placeholder)) }, shape = shape, minLines = 2) }
            item { Row(verticalAlignment = Alignment.CenterVertically) { Checkbox(gift, { gift = it }); Text(stringResource(R.string.checkout_gift)) } }
            item { Text(stringResource(R.string.checkout_summary), style = MaterialTheme.typography.titleLarge) }
            items(current.items, key = { "s" + it.id }) { i ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RemoteImage(i.product.image?.url, null, Modifier.size(48.dp, 60.dp).clip(RoundedCornerShape(10.dp)))
                    Column(Modifier.weight(1f).padding(horizontal = 10.dp)) { Text(i.product.name, maxLines = 1, style = MaterialTheme.typography.bodyMedium); Text("${i.variant.label} × ${i.quantity}", style = MaterialTheme.typography.bodySmall) }
                    Text(money(i.lineTotal))
                }
            }
            item { Totals(current.subtotal, current.discount, shipping, codFee, total, current.freeShipping) }
            item { Text(stringResource(R.string.checkout_agree), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
        Surface(shadowElevation = 8.dp) {
            LoadingButton(stringResource(R.string.checkout_place_order, money(total)), placing, ::place, Modifier.fillMaxWidth().navigationBarsPadding().padding(16.dp))
        }
    }
}

@Composable
private fun Field(value: String, onChange: (String) -> Unit, label: Int, error: String?, type: KeyboardType = KeyboardType.Text, hint: String? = null) {
    OutlinedTextField(
        value, onChange, Modifier.fillMaxWidth(), label = { Text(stringResource(label)) }, isError = error != null,
        supportingText = (error ?: hint)?.let { { Text(it) } }, singleLine = true, shape = RoundedCornerShape(14.dp),
        keyboardOptions = KeyboardOptions(keyboardType = type, capitalization = if (type == KeyboardType.Text) KeyboardCapitalization.Words else KeyboardCapitalization.None),
    )
}

/** Hands the freshly placed order to the success screen (guests can't refetch it). */
object SuccessCache { var order: Order? = null }

/* ---------------- Order placed ---------------- */

@Composable
fun OrderSuccessScreen(nav: NavHostController, id: String) {
    val c = LocalAppContainer.current
    val api = c.api
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val clipboard = LocalClipboardManager.current
    val order = SuccessCache.order?.takeIf { it.id == id }
    val remote = rememberResource("order-$id") { order ?: api.order(id) }
    val o = remote.data ?: order
    val banks = rememberResource("banks") { api.bankAccounts() }
    var reference by rememberSaveable { mutableStateOf("") }
    var proof by remember { mutableStateOf<Uri?>(null) }
    var sending by remember { mutableStateOf(false) }
    var sent by rememberSaveable { mutableStateOf(false) }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { proof = it }

    if (o == null) { Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }; return }
    LazyColumn(Modifier.fillMaxSize().statusBarsPadding(), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item {
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                var shown by remember { mutableStateOf(false) }
                LaunchedEffect(Unit) { shown = true }
                val scale by animateFloatAsState(if (shown) 1f else 0.4f, androidx.compose.animation.core.spring(dampingRatio = 0.45f), label = "ok")
                Icon(Icons.Filled.CheckCircle, null, Modifier.size(84.dp).graphicsLayerScale(scale), tint = Brand.Success)
                Text(stringResource(R.string.success_title, o.shippingAddress.fullName.substringBefore(' ')), style = MaterialTheme.typography.headlineMedium)
                Text(stringResource(R.string.success_subtitle, o.number, o.email), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        item {
            Text(
                if (o.paymentMethod == "bank_transfer") stringResource(R.string.success_bank_next, money(o.grandTotal), o.number) else stringResource(R.string.success_cod_next, money(o.grandTotal)),
                Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(MaterialTheme.colorScheme.surfaceVariant).padding(16.dp),
            )
        }
        if (o.paymentMethod == "bank_transfer") {
            items(banks.data ?: emptyList()) { b ->
                Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(18.dp)).padding(16.dp)) {
                    Text(b.bankName, fontWeight = FontWeight.Bold); Text(b.accountName, style = MaterialTheme.typography.bodySmall)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(b.accountNumber, Modifier.weight(1f), fontWeight = FontWeight.SemiBold)
                        TextButton(onClick = { clipboard.setText(AnnotatedString(b.accountNumber.filter { !it.isWhitespace() })); UiMessages.show(ctx.getString(R.string.success_copied)) }) { Text(stringResource(R.string.success_copy)) }
                    }
                    if (b.iban.isNotBlank()) Text(b.iban, style = MaterialTheme.typography.bodySmall)
                }
            }
            item {
                if (sent) Text(stringResource(R.string.success_receipt_done), color = Brand.Success, fontWeight = FontWeight.SemiBold)
                else Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(stringResource(R.string.success_upload_receipt), style = MaterialTheme.typography.titleLarge)
                    OutlinedTextField(reference, { reference = it }, Modifier.fillMaxWidth(), label = { Text(stringResource(R.string.success_reference)) }, singleLine = true, shape = RoundedCornerShape(14.dp))
                    OutlinedButton(onClick = { picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }, shape = CircleShape) {
                        Text(if (proof != null) stringResource(R.string.app_photo_selected) else stringResource(R.string.app_choose_photo))
                    }
                    LoadingButton(stringResource(R.string.success_submit_receipt), sending, {
                        sending = true
                        scope.launch {
                            runCatching {
                                val text = "text/plain".toMediaType()
                                val part = proof?.let { uri ->
                                    val bytes = ctx.contentResolver.openInputStream(uri)!!.use { it.readBytes() }
                                    MultipartBody.Part.createFormData("proof", "receipt.jpg", bytes.toRequestBody("image/jpeg".toMediaType()))
                                }
                                api.submitProof(o.id, reference.toRequestBody(text), o.email.toRequestBody(text), part)
                            }.onSuccess { sent = true; UiMessages.show(ctx.getString(R.string.success_receipt_done)) }.onFailure { UiMessages.show(it.userMessage(ctx)) }
                            sending = false
                        }
                    }, Modifier.fillMaxWidth(), enabled = reference.isNotBlank())
                }
            }
        }
        item { Totals(o.subtotal, o.discountTotal, o.shippingTotal, o.codFee, o.grandTotal) }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                LoadingButton(stringResource(R.string.success_track_order), false, { if (c.auth.isSignedIn) nav.navigate("order/${o.id}") else nav.navigate("track") }, Modifier.fillMaxWidth())
                OutlinedButton(onClick = { nav.navigate("home") { popUpTo("home") { inclusive = true } } }, Modifier.fillMaxWidth().height(52.dp), shape = CircleShape) { Text(stringResource(R.string.success_keep_shopping)) }
                if (!c.auth.isSignedIn) TextButton(onClick = { nav.navigate("register") }, Modifier.fillMaxWidth()) { Text(stringResource(R.string.success_create_account)) }
            }
        }
    }
}

private fun Modifier.graphicsLayerScale(s: Float) = this.graphicsLayer { scaleX = s; scaleY = s }

package shop.chobbar.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.SubcomposeAsyncImage
import shop.chobbar.app.R
import shop.chobbar.app.data.model.ProductCard
import shop.chobbar.app.ui.theme.Brand
import shop.chobbar.app.ui.theme.LocalLowEnd
import shop.chobbar.app.util.currentLocale
import shop.chobbar.app.util.formatMoney
import shop.chobbar.app.util.formatNumber

/* ---------- Logo (same geometry as the web/SVG brand assets) ---------- */

private val BagBody = PathParser().parsePathString("M30 44 Q30 40 34 40 L88 40 Q92 40 92.6 44 L100 100 Q100.6 106 94 106 L24 106 Q18 106 18.6 100 Z").toPath()
private val BagHandle = PathParser().parsePathString("M45 43 V31 A15 15 0 0 1 75 31 V43").toPath()
private val LetterC = PathParser().parsePathString("M69.71 59.11 A16 16 0 1 0 69.71 82.89").toPath()
private val Swoosh = PathParser().parsePathString("M12 104 C1 97 26 86 58 78 C82 71.5 104 65.5 119 61.5 C121.6 61 122.2 63 119.7 64.3 C102 71.5 80 80 56 88 C38 94 21 100.5 12 104 Z").toPath()
private val Sparks = PathParser().parsePathString("M101 21 L104 11 M108 31 L116 24").toPath()

@Composable
fun LogoMark(modifier: Modifier = Modifier.size(36.dp), ink: Color = MaterialTheme.colorScheme.onBackground, cut: Color = MaterialTheme.colorScheme.background, accent: Color = MaterialTheme.colorScheme.primary) {
    Canvas(modifier.aspectRatio(122f / 110f)) {
        val s = size.width / 122f
        scale(s, s, pivot = Offset.Zero) {
            translate(-4f, -2f) {
                rotate(-7f, pivot = Offset(60f, 72f)) {
                    drawPath(BagBody, ink)
                    drawPath(BagHandle, ink, style = Stroke(8f, cap = StrokeCap.Round))
                }
                drawPath(LetterC, cut, style = Stroke(10.5f, cap = StrokeCap.Round))
                drawPath(Swoosh, accent)
                drawPath(Sparks, accent, style = Stroke(4.5f, cap = StrokeCap.Round))
            }
        }
    }
}

@Composable
fun Wordmark(modifier: Modifier = Modifier, height: Dp = 28.dp) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        LogoMark(Modifier.height(height))
        Spacer(Modifier.width(6.dp))
        Text(
            buildString { append("Ch") },
            fontWeight = FontWeight.Black, fontSize = (height.value * 0.78f).sp, color = MaterialTheme.colorScheme.onBackground,
        )
        Text("o", fontWeight = FontWeight.Black, fontSize = (height.value * 0.78f).sp, color = MaterialTheme.colorScheme.primary)
        Text("bbar", fontWeight = FontWeight.Black, fontSize = (height.value * 0.78f).sp, color = MaterialTheme.colorScheme.onBackground)
    }
}

/* ---------- Images ---------- */

@Composable
fun Shimmer(modifier: Modifier) {
    val lowEnd = LocalLowEnd.current
    val base = MaterialTheme.colorScheme.surfaceVariant
    if (lowEnd) {
        Box(modifier.background(base))
        return
    }
    val t = rememberInfiniteTransition(label = "shimmer")
    val x by t.animateFloat(-400f, 1200f, infiniteRepeatable(tween(1300), RepeatMode.Restart), label = "x")
    val highlight = MaterialTheme.colorScheme.surface.copy(alpha = 0.55f)
    Box(modifier.background(Brush.linearGradient(listOf(base, highlight, base), start = Offset(x, 0f), end = Offset(x + 400f, 400f))))
}

@Composable
fun RemoteImage(url: String?, contentDescription: String?, modifier: Modifier = Modifier, contentScale: ContentScale = ContentScale.Crop) {
    SubcomposeAsyncImage(
        model = url,
        contentDescription = contentDescription,
        contentScale = contentScale,
        modifier = modifier,
        loading = { Shimmer(Modifier.fillMaxSize()) },
        error = {
            Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surfaceVariant), contentAlignment = Alignment.Center) {
                LogoMark(Modifier.fillMaxWidth(0.3f), ink = MaterialTheme.colorScheme.outline, cut = MaterialTheme.colorScheme.surfaceVariant, accent = MaterialTheme.colorScheme.outline)
            }
        },
    )
}

/* ---------- Money, rating, stepper ---------- */

@Composable
fun money(value: Double): String = formatMoney(value, currentLocale(), stringResource(R.string.common_currency))

@Composable
fun num(value: Number): String = formatNumber(value, currentLocale())

@Composable
fun PriceText(price: Double, compare: Double?, modifier: Modifier = Modifier, large: Boolean = false) {
    val onSale = compare != null && compare > price
    Row(modifier, verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            money(price),
            style = if (large) MaterialTheme.typography.headlineSmall else MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = if (onSale) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground,
        )
        if (onSale) {
            Text(money(compare!!), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, textDecoration = TextDecoration.LineThrough)
        }
    }
}

@Composable
fun RatingStars(value: Double, size: Dp = 14.dp, count: Int? = null) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        repeat(5) { i ->
            Icon(Icons.Filled.Star, null, Modifier.size(size), tint = if (i < Math.round(value)) Brand.Amber else MaterialTheme.colorScheme.outline)
        }
        if (count != null) Text(" (${num(count)})", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun QuantityStepper(value: Int, onChange: (Int) -> Unit, min: Int = 1, max: Int = 10, small: Boolean = false) {
    val size = if (small) 30.dp else 40.dp
    Row(
        Modifier.border(1.dp, MaterialTheme.colorScheme.outline, CircleShape).padding(2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        StepButton("−", enabled = value > min, size = size) { onChange(value - 1) }
        Text(num(value), Modifier.widthIn(min = 28.dp), style = MaterialTheme.typography.labelLarge, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        StepButton("+", enabled = value < max, size = size) { onChange(value + 1) }
    }
}

@Composable
private fun StepButton(label: String, enabled: Boolean, size: Dp, onClick: () -> Unit) {
    Box(
        Modifier.size(size).clip(CircleShape).clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { Text(label, style = MaterialTheme.typography.titleMedium, color = if (enabled) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.outline) }
}

/* ---------- Product card ---------- */

@Composable
fun ProductCardView(
    product: ProductCard,
    saved: Boolean,
    onClick: () -> Unit,
    onToggleWishlist: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier.clip(RoundedCornerShape(18.dp)).clickable(onClick = onClick)) {
        Box(Modifier.fillMaxWidth().aspectRatio(0.8f).clip(RoundedCornerShape(18.dp))) {
            RemoteImage(product.image?.url, product.name, Modifier.fillMaxSize())
            Column(Modifier.align(Alignment.TopStart).padding(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                when {
                    !product.inStock -> Badge(stringResource(R.string.common_sold_out), Brand.Ink.copy(alpha = 0.85f), Color.White)
                    product.discountPercent > 0 -> Badge("−${product.discountPercent}%", Brand.Rose, Color.White)
                }
                if (product.isNew && product.inStock) Badge(stringResource(R.string.common_new), Color.White, Brand.Ink)
            }
            HeartButton(saved, onToggleWishlist, Modifier.align(Alignment.TopEnd).padding(6.dp))
        }
        Spacer(Modifier.height(8.dp))
        if (product.colors.isNotEmpty()) {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.padding(horizontal = 2.dp)) {
                product.colors.take(5).forEach { c ->
                    Box(Modifier.size(11.dp).clip(CircleShape).background(parseHex(c.hex)).border(0.5.dp, Color.Black.copy(alpha = 0.15f), CircleShape))
                }
            }
            Spacer(Modifier.height(4.dp))
        }
        Text(product.name, style = MaterialTheme.typography.bodyMedium, maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(horizontal = 2.dp))
        Text(product.categoryName, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(horizontal = 2.dp))
        PriceText(product.price, product.compareAtPrice, Modifier.padding(horizontal = 2.dp, vertical = 2.dp))
    }
}

@Composable
fun HeartButton(saved: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    var bump by remember { mutableIntStateOf(0) }
    val scale = remember { androidx.compose.animation.core.Animatable(1f) }
    LaunchedEffect(bump) {
        if (bump > 0) {
            scale.animateTo(1.3f, tween(120))
            scale.animateTo(1f, spring(dampingRatio = 0.4f))
        }
    }
    val tint by animateColorAsState(if (saved) Brand.Rose else Brand.Ink, label = "heart")
    Box(
        modifier.size(36.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.92f)).clickable { bump++; onClick() },
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            if (saved) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
            contentDescription = stringResource(if (saved) R.string.product_wishlist_remove else R.string.product_wishlist_add),
            tint = tint,
            modifier = Modifier.size(19.dp).scale(scale.value),
        )
    }
}

@Composable
fun Badge(text: String, bg: Color, fg: Color) {
    Text(
        text, color = fg, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold,
        modifier = Modifier.clip(CircleShape).background(bg).padding(horizontal = 8.dp, vertical = 3.dp),
    )
}

fun parseHex(hex: String): Color = runCatching { Color(android.graphics.Color.parseColor(hex)) }.getOrDefault(Color.Gray)

/* ---------- Layout helpers ---------- */

@Composable
fun SectionHeader(title: String, subtitle: String? = null, action: String? = null, onAction: (() -> Unit)? = null) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.Bottom) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.headlineSmall)
            subtitle?.let { Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
        if (action != null && onAction != null) TextButton(onClick = onAction) { Text(action) }
    }
}

@Composable
fun EmptyState(title: String, message: String? = null, actionLabel: String? = null, onAction: (() -> Unit)? = null, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Box(Modifier.size(72.dp).clip(RoundedCornerShape(22.dp)).background(MaterialTheme.colorScheme.primaryContainer), contentAlignment = Alignment.Center) {
            LogoMark(Modifier.width(38.dp), ink = MaterialTheme.colorScheme.primary, cut = MaterialTheme.colorScheme.primaryContainer, accent = MaterialTheme.colorScheme.onBackground)
        }
        Text(title, style = MaterialTheme.typography.titleLarge, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        message?.let { Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = androidx.compose.ui.text.style.TextAlign.Center) }
        if (actionLabel != null && onAction != null) Button(onClick = onAction, shape = CircleShape) { Text(actionLabel) }
    }
}

@Composable
fun LoadingButton(text: String, loading: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true, secondary: Boolean = false) {
    Button(
        onClick = onClick,
        enabled = enabled && !loading,
        modifier = modifier.heightIn(min = 52.dp),
        shape = CircleShape,
        colors = if (secondary) ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary, contentColor = MaterialTheme.colorScheme.onSecondary) else ButtonDefaults.buttonColors(),
    ) {
        if (loading) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp, color = LocalContentColor.current)
        else Text(text, style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
fun StatusChip(status: String) {
    val label = statusLabel(status)
    val (bg, fg) = when (status) {
        "delivered", "completed", "paid", "approved", "open" -> Brand.Success.copy(alpha = 0.12f) to Brand.Success
        "cancelled", "rejected", "failed" -> Color(0xFFDC2626).copy(alpha = 0.12f) to Color(0xFFDC2626)
        "pending", "unpaid", "requested" -> Brand.Warning.copy(alpha = 0.14f) to Brand.Warning
        else -> MaterialTheme.colorScheme.primaryContainer to MaterialTheme.colorScheme.primary
    }
    Text(label, color = fg, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, modifier = Modifier.clip(CircleShape).background(bg).padding(horizontal = 10.dp, vertical = 4.dp))
}

@Composable
fun statusLabel(status: String): String = stringResource(
    when (status) {
        "pending" -> R.string.status_pending; "confirmed" -> R.string.status_confirmed; "processing" -> R.string.status_processing
        "shipped" -> R.string.status_shipped; "out_for_delivery" -> R.string.status_out_for_delivery; "delivered" -> R.string.status_delivered
        "completed" -> R.string.status_completed; "cancelled" -> R.string.status_cancelled; "returned" -> R.string.status_returned
        "unpaid" -> R.string.status_unpaid; "paid" -> R.string.status_paid; "cod_pending" -> R.string.status_cod_pending
        "refunded" -> R.string.status_refunded; "requested" -> R.string.status_requested; "approved" -> R.string.status_approved
        "received" -> R.string.status_received; "rejected" -> R.string.status_rejected; "closed" -> R.string.status_closed
        else -> R.string.status_open
    },
)

/** Icons outside the small material-icons-core set, drawn from path data (no icons-extended dependency). */
object AppIcons {
    private fun icon(name: String, d: String, stroke: Boolean = true) = ImageVector.Builder(name, 24.dp, 24.dp, 24f, 24f).apply {
        addPath(
            PathParser().parsePathString(d).toNodes(),
            fill = if (stroke) null else androidx.compose.ui.graphics.SolidColor(Color.Black),
            stroke = if (stroke) androidx.compose.ui.graphics.SolidColor(Color.Black) else null,
            strokeLineWidth = if (stroke) 1.8f else 0f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = androidx.compose.ui.graphics.StrokeJoin.Round,
        )
    }.build()

    val Bag = icon("bag", "M6 7h12l1 13H5L6 7z M9 7V6a3 3 0 0 1 6 0v1")
    val Filter = icon("filter", "M4 6h16 M7 12h10 M10 18h4")
    val Chat = icon("chat", "M4 5h16v11H9l-5 4V5z")
    val Truck = icon("truck", "M3 7h11v9H3z M14 10h4l3 3v3h-7z M7 19a1.5 1.5 0 1 0 0-3 1.5 1.5 0 0 0 0 3z M17 19a1.5 1.5 0 1 0 0-3 1.5 1.5 0 0 0 0 3z")
    val Grid = icon("grid", "M4 4h7v7H4z M13 4h7v7h-7z M4 13h7v7H4z M13 13h7v7h-7z")
    val Globe = icon("globe", "M12 3a9 9 0 1 0 0 18 9 9 0 0 0 0-18z M3 12h18 M12 3c2.5 2.5 3.5 5.5 3.5 9s-1 6.5-3.5 9c-2.5-2.5-3.5-5.5-3.5-9s1-6.5 3.5-9z")
    val Package = icon("package", "M3 7l9-4 9 4-9 4-9-4z M3 7v10l9 4 9-4V7 M12 11v10")
    val Return = icon("return", "M9 14L4 9l5-5 M4 9h10a6 6 0 0 1 0 12h-3")
    val Ruler = icon("ruler", "M3 17L17 3l4 4L7 21z M7 13l2 2 M10 10l2 2 M13 7l2 2")
    val Moon = icon("moon", "M20 14A8 8 0 1 1 10 4a7 7 0 0 0 10 10z")
    val Help = icon("help", "M12 21a9 9 0 1 0 0-18 9 9 0 0 0 0 18z M9.5 9a2.5 2.5 0 1 1 3.5 2.3c-.6.3-1 .9-1 1.7 M12 17h.01")
}

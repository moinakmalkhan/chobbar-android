package shop.chobbar.app.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

/* API models — mirror the Django serializers (chobbar-backend) and chobbar-web/src/types/api.ts. */

@Serializable
data class Paginated<T>(
    val count: Int = 0,
    val page: Int = 1,
    @SerialName("page_size") val pageSize: Int = 24,
    @SerialName("total_pages") val totalPages: Int = 1,
    val next: String? = null,
    val previous: String? = null,
    val results: List<T> = emptyList(),
)

@Serializable
data class CursorPage<T>(val next: String? = null, val previous: String? = null, val results: List<T> = emptyList())

@Serializable
data class ApiErrorBody(val detail: String = "", val code: String = "", val errors: JsonObject? = null)

@Serializable
data class LocaleInfo(val language: String = "en", val source: String = "", val country: String = "", val rtl: Boolean = false)

@Serializable
data class User(
    val id: String,
    val email: String,
    @SerialName("first_name") val firstName: String = "",
    @SerialName("last_name") val lastName: String = "",
    @SerialName("full_name") val fullName: String = "",
    val phone: String = "",
    @SerialName("avatar_url") val avatarUrl: String = "",
    @SerialName("date_of_birth") val dateOfBirth: String? = null,
    @SerialName("shop_for") val shopFor: String = "",
    val language: String = "en",
    @SerialName("is_staff") val isStaff: Boolean = false,
    @SerialName("is_email_verified") val isEmailVerified: Boolean = false,
    @SerialName("auth_provider") val authProvider: String = "email",
    @SerialName("marketing_opt_in") val marketingOptIn: Boolean = true,
    @SerialName("notification_preferences") val notificationPreferences: Map<String, Boolean> = emptyMap(),
    @SerialName("has_password") val hasPassword: Boolean = true,
    @SerialName("order_count") val orderCount: Int = 0,
)

@Serializable
data class AuthResponse(val access: String, val refresh: String, val user: User)

@Serializable
data class TokenPair(val access: String, val refresh: String? = null)

@Serializable
data class Detail(val detail: String = "")

@Serializable
data class Address(
    val id: String = "",
    val label: String = "home",
    @SerialName("full_name") val fullName: String = "",
    val phone: String = "",
    val line1: String = "",
    val line2: String = "",
    val city: String = "",
    val province: String = "",
    @SerialName("postal_code") val postalCode: String = "",
    val country: String = "PK",
    @SerialName("is_default") val isDefault: Boolean = false,
)

@Serializable
data class CategoryNode(
    val id: String,
    val name: String,
    val slug: String,
    val icon: String = "",
    val image: String? = null,
    val description: String = "",
    @SerialName("product_count") val productCount: Int = 0,
    val depth: Int = 0,
    @SerialName("show_in_menu") val showInMenu: Boolean = true,
    val children: List<CategoryNode> = emptyList(),
)

@Serializable
data class Crumb(val name: String, val slug: String)

@Serializable
data class CategoryDetail(
    val id: String,
    val name: String,
    val slug: String,
    val description: String = "",
    val image: String? = null,
    val breadcrumbs: List<Crumb> = emptyList(),
    val children: List<CategoryNode> = emptyList(),
    @SerialName("product_count") val productCount: Int = 0,
)

@Serializable
data class Collection(
    val id: String,
    val name: String,
    val slug: String,
    val tagline: String = "",
    val description: String = "",
    val image: String? = null,
    @SerialName("is_featured") val isFeatured: Boolean = false,
    @SerialName("product_count") val productCount: Int = 0,
)

@Serializable
data class ColorRef(val name: String, val slug: String, val hex: String)

@Serializable
data class ImageRef(val url: String, val alt: String = "", val width: Int? = null, val height: Int? = null)

@Serializable
data class ProductCard(
    val id: String,
    val name: String,
    val slug: String,
    val sku: String = "",
    val gender: String = "",
    val category: String? = null,
    @SerialName("category_name") val categoryName: String = "",
    @SerialName("short_description") val shortDescription: String = "",
    val image: ImageRef? = null,
    @SerialName("hover_image") val hoverImage: ImageRef? = null,
    val price: Double = 0.0,
    @SerialName("compare_at_price") val compareAtPrice: Double? = null,
    @SerialName("discount_percent") val discountPercent: Int = 0,
    @SerialName("is_on_sale") val isOnSale: Boolean = false,
    @SerialName("in_stock") val inStock: Boolean = true,
    @SerialName("rating_avg") val ratingAvg: Double = 0.0,
    @SerialName("rating_count") val ratingCount: Int = 0,
    val colors: List<ColorRef> = emptyList(),
    val sizes: List<String> = emptyList(),
    @SerialName("is_new") val isNew: Boolean = false,
)

@Serializable
data class Variant(
    val id: String,
    val sku: String = "",
    val color: ColorRef? = null,
    val size: String? = null,
    val price: Double = 0.0,
    val stock: Int = 0,
    @SerialName("in_stock") val inStock: Boolean = false,
    @SerialName("low_stock") val lowStock: Boolean = false,
    val label: String = "",
)

@Serializable
data class ProductImage(
    val id: Int,
    val url: String,
    val alt: String = "",
    val color: String? = null,
    @SerialName("is_primary") val isPrimary: Boolean = false,
)

@Serializable
data class SizeOption(val id: Int, val label: String, @SerialName("sort_order") val sortOrder: Int = 0)

@Serializable
data class SizeGroup(val id: Int, val name: String, val slug: String, val sizes: List<SizeOption> = emptyList(), val guide: List<Map<String, String>> = emptyList())

@Serializable
data class ProductDetail(
    val id: String,
    val name: String,
    val slug: String,
    val sku: String = "",
    @SerialName("category_name") val categoryName: String = "",
    @SerialName("short_description") val shortDescription: String = "",
    val description: String = "",
    val material: String = "",
    @SerialName("care_instructions") val careInstructions: String = "",
    val specifications: Map<String, String> = emptyMap(),
    val image: ImageRef? = null,
    val images: List<ProductImage> = emptyList(),
    val variants: List<Variant> = emptyList(),
    @SerialName("size_group") val sizeGroup: SizeGroup? = null,
    val price: Double = 0.0,
    @SerialName("compare_at_price") val compareAtPrice: Double? = null,
    @SerialName("discount_percent") val discountPercent: Int = 0,
    @SerialName("in_stock") val inStock: Boolean = true,
    @SerialName("rating_avg") val ratingAvg: Double = 0.0,
    @SerialName("rating_count") val ratingCount: Int = 0,
    val colors: List<ColorRef> = emptyList(),
    val sizes: List<String> = emptyList(),
    @SerialName("is_new") val isNew: Boolean = false,
    val breadcrumbs: List<Crumb> = emptyList(),
) {
    fun toCard() = ProductCard(
        id = id, name = name, slug = slug, sku = sku, categoryName = categoryName, shortDescription = shortDescription,
        image = image, price = price, compareAtPrice = compareAtPrice, discountPercent = discountPercent, inStock = inStock,
        ratingAvg = ratingAvg, ratingCount = ratingCount, colors = colors, sizes = sizes, isNew = isNew,
    )
}

@Serializable
data class FacetSize(val label: String, val count: Int)

@Serializable
data class FacetColor(val name: String, val slug: String, val hex: String, val count: Int)

@Serializable
data class FacetGender(val value: String, val count: Int)

@Serializable
data class PriceRange(val min: Double = 0.0, val max: Double = 0.0)

@Serializable
data class Facets(
    val sizes: List<FacetSize> = emptyList(),
    val colors: List<FacetColor> = emptyList(),
    val price: PriceRange = PriceRange(),
    val genders: List<FacetGender> = emptyList(),
)

@Serializable
data class CartItem(
    val id: String,
    val product: ProductCard,
    val variant: Variant,
    val quantity: Int,
    @SerialName("unit_price") val unitPrice: Double,
    @SerialName("line_total") val lineTotal: Double,
    val available: Int = 0,
    val issue: String = "",
)

@Serializable
data class Cart(
    val id: String = "",
    @SerialName("guest_token") val guestToken: String? = null,
    val items: List<CartItem> = emptyList(),
    @SerialName("item_count") val itemCount: Int = 0,
    val quantity: Int = 0,
    val subtotal: Double = 0.0,
    val discount: Double = 0.0,
    @SerialName("coupon_code") val couponCode: String = "",
    @SerialName("coupon_message") val couponMessage: String = "",
    @SerialName("free_shipping") val freeShipping: Boolean = false,
    val issues: List<String> = emptyList(),
)

@Serializable
data class WishlistEntry(val product: ProductCard, val variant: Variant? = null)

@Serializable
data class WishlistResponse(val count: Int = 0, val results: List<WishlistEntry> = emptyList())

@Serializable
data class WishlistToggle(val wishlisted: Boolean, val detail: String = "")

@Serializable
data class ShippingQuote(
    val zone: String? = null,
    val rate: Double = 0.0,
    @SerialName("cod_fee") val codFee: Double = 0.0,
    @SerialName("is_free") val isFree: Boolean = false,
    @SerialName("free_over") val freeOver: Double? = null,
    @SerialName("min_days") val minDays: Int = 0,
    @SerialName("max_days") val maxDays: Int = 0,
    val subtotal: Double = 0.0,
    val discount: Double = 0.0,
    val total: Double = 0.0,
)

@Serializable
data class OrderItem(
    val id: String,
    @SerialName("product_slug") val productSlug: String? = null,
    @SerialName("product_name") val productName: String,
    @SerialName("variant_label") val variantLabel: String = "",
    val sku: String = "",
    @SerialName("image_url") val imageUrl: String = "",
    val quantity: Int,
    @SerialName("unit_price") val unitPrice: Double,
    @SerialName("line_total") val lineTotal: Double,
    @SerialName("returned_quantity") val returnedQuantity: Int = 0,
    @SerialName("can_review") val canReview: Boolean = false,
)

@Serializable
data class OrderSummary(
    val id: String,
    val number: String,
    val status: String,
    @SerialName("payment_status") val paymentStatus: String = "",
    @SerialName("payment_method") val paymentMethod: String = "cod",
    @SerialName("grand_total") val grandTotal: Double = 0.0,
    @SerialName("total_quantity") val totalQuantity: Int = 0,
    val thumbnails: List<String> = emptyList(),
    @SerialName("estimated_delivery") val estimatedDelivery: String? = null,
    @SerialName("created_at") val createdAt: String = "",
)

@Serializable
data class AddressSnapshot(
    @SerialName("full_name") val fullName: String = "",
    val phone: String = "",
    val line1: String = "",
    val line2: String = "",
    val city: String = "",
    val province: String = "",
    @SerialName("postal_code") val postalCode: String = "",
)

@Serializable
data class HistoryEntry(@SerialName("to_status") val toStatus: String, val note: String = "", @SerialName("created_at") val createdAt: String)

@Serializable
data class ReturnItemInfo(@SerialName("product_name") val productName: String, @SerialName("variant_label") val variantLabel: String = "", val quantity: Int)

@Serializable
data class ReturnRequest(
    val id: String,
    val number: String,
    @SerialName("order_number") val orderNumber: String = "",
    val kind: String,
    val status: String,
    val reason: String = "",
    val items: List<ReturnItemInfo> = emptyList(),
    @SerialName("refund_amount") val refundAmount: Double? = null,
    @SerialName("staff_notes") val staffNotes: String = "",
    @SerialName("created_at") val createdAt: String = "",
)

@Serializable
data class Order(
    val id: String,
    val number: String,
    val status: String,
    @SerialName("payment_status") val paymentStatus: String = "",
    @SerialName("payment_method") val paymentMethod: String = "cod",
    val email: String = "",
    val items: List<OrderItem> = emptyList(),
    val history: List<HistoryEntry> = emptyList(),
    val subtotal: Double = 0.0,
    @SerialName("discount_total") val discountTotal: Double = 0.0,
    @SerialName("shipping_total") val shippingTotal: Double = 0.0,
    @SerialName("cod_fee") val codFee: Double = 0.0,
    @SerialName("grand_total") val grandTotal: Double = 0.0,
    @SerialName("total_quantity") val totalQuantity: Int = 0,
    @SerialName("shipping_address") val shippingAddress: AddressSnapshot = AddressSnapshot(),
    val carrier: String = "",
    @SerialName("tracking_number") val trackingNumber: String = "",
    @SerialName("tracking_url") val trackingUrl: String = "",
    @SerialName("estimated_delivery") val estimatedDelivery: String? = null,
    val returns: List<ReturnRequest> = emptyList(),
    @SerialName("can_cancel") val canCancel: Boolean = false,
    @SerialName("can_return") val canReturn: Boolean = false,
    @SerialName("created_at") val createdAt: String = "",
)

@Serializable
data class OrdersSummaryCounts(val total: Int = 0, val open: Int = 0, val delivered: Int = 0, val returns: Int = 0)

@Serializable
data class TrackingStep(val status: String, val reached: Boolean)

@Serializable
data class TrackingEvent(val status: String, val note: String = "", val at: String)

@Serializable
data class TrackingItem(val name: String, val variant: String = "", val quantity: Int, @SerialName("image_url") val imageUrl: String = "")

@Serializable
data class TrackingInfo(
    val number: String,
    val status: String,
    val steps: List<TrackingStep> = emptyList(),
    val timeline: List<TrackingEvent> = emptyList(),
    val carrier: String = "",
    @SerialName("tracking_number") val trackingNumber: String = "",
    @SerialName("tracking_url") val trackingUrl: String = "",
    @SerialName("estimated_delivery") val estimatedDelivery: String? = null,
    val items: List<TrackingItem> = emptyList(),
    @SerialName("grand_total") val grandTotal: Double = 0.0,
)

@Serializable
data class BankAccount(
    val label: String,
    @SerialName("bank_name") val bankName: String,
    @SerialName("account_name") val accountName: String,
    @SerialName("account_number") val accountNumber: String,
    val iban: String = "",
    val instructions: String = "",
)

@Serializable
data class Review(
    val id: String,
    val author: ReviewAuthor,
    val rating: Int,
    val title: String = "",
    val body: String = "",
    @SerialName("size_purchased") val sizePurchased: String = "",
    @SerialName("is_verified_purchase") val isVerifiedPurchase: Boolean = false,
    @SerialName("helpful_count") val helpfulCount: Int = 0,
    @SerialName("staff_reply") val staffReply: String = "",
    @SerialName("created_at") val createdAt: String = "",
)

@Serializable
data class ReviewAuthor(val name: String, @SerialName("avatar_url") val avatarUrl: String = "")

@Serializable
data class ReviewSummary(val average: Double = 0.0, val total: Int = 0, val distribution: Map<String, Int> = emptyMap())

@Serializable
data class Banner(
    val id: Int,
    val title: String,
    val subtitle: String = "",
    val eyebrow: String = "",
    val image: String? = null,
    @SerialName("mobile_image") val mobileImage: String? = null,
    @SerialName("cta_label") val ctaLabel: String = "",
    @SerialName("cta_url") val ctaUrl: String = "",
    val placement: String = "hero",
)

@Serializable
data class Announcement(val id: Int, val text: String, val url: String = "")

@Serializable
data class ShippingZoneInfo(
    val name: String,
    val cities: List<String> = emptyList(),
    val provinces: List<String> = emptyList(),
    @SerialName("is_default") val isDefault: Boolean = false,
    val rate: Double = 0.0,
    @SerialName("free_over") val freeOver: Double? = null,
)

@Serializable
data class ShippingSummary(@SerialName("free_over_min") val freeOverMin: Double? = null, val zones: List<ShippingZoneInfo> = emptyList())

@Serializable
data class PageLink(val title: String, val slug: String)

@Serializable
data class Storefront(
    val banners: List<Banner> = emptyList(),
    val announcements: List<Announcement> = emptyList(),
    val shipping: ShippingSummary = ShippingSummary(),
    val pages: List<PageLink> = emptyList(),
)

@Serializable
data class ContentPage(val title: String, val slug: String, val body: String = "")

@Serializable
data class Faq(val id: Int, val question: String, val answer: String, val category: String = "")

@Serializable
data class Notification(
    val id: String,
    val kind: String = "system",
    val level: String = "info",
    val title: String,
    val body: String = "",
    val url: String = "",
    @SerialName("is_read") val isRead: Boolean = false,
    @SerialName("created_at") val createdAt: String = "",
)

@Serializable
data class SuggestCategory(val name: String, val slug: String, @SerialName("product_count") val productCount: Int = 0)

@Serializable
data class SuggestCollection(val name: String, val slug: String)

@Serializable
data class SuggestResponse(
    val query: String = "",
    val products: List<ProductCard> = emptyList(),
    val categories: List<SuggestCategory> = emptyList(),
    val collections: List<SuggestCollection> = emptyList(),
)

@Serializable
data class Trending(val queries: List<String> = emptyList())

@Serializable
data class ChatSender(val id: String, val name: String = "", @SerialName("is_staff") val isStaff: Boolean = false)

@Serializable
data class ChatMessage(
    val id: String,
    val kind: String = "text",
    val body: String = "",
    val attachment: String? = null,
    @SerialName("attachment_name") val attachmentName: String = "",
    @SerialName("client_id") val clientId: String = "",
    val sender: ChatSender? = null,
    @SerialName("created_at") val createdAt: String = "",
    val pending: Boolean = false,
)

@Serializable
data class OrderRef(val id: String, val number: String)

@Serializable
data class Conversation(
    val id: String,
    val subject: String = "",
    val topic: String = "",
    val status: String = "open",
    val order: OrderRef? = null,
    @SerialName("last_message_at") val lastMessageAt: String? = null,
    @SerialName("last_message_preview") val lastMessagePreview: String = "",
    @SerialName("unread_count") val unreadCount: Int = 0,
    @SerialName("created_at") val createdAt: String = "",
)

@Serializable
data class Count(val count: Int = 0)

/* Request bodies */

@Serializable data class LoginBody(val email: String, val password: String)
@Serializable data class GoogleBody(@SerialName("id_token") val idToken: String)
@Serializable data class RefreshBody(val refresh: String)
@Serializable data class EmailBody(val email: String)
@Serializable data class RegisterBody(
    val email: String,
    val password: String,
    @SerialName("first_name") val firstName: String,
    @SerialName("last_name") val lastName: String = "",
    val phone: String = "",
    @SerialName("marketing_opt_in") val marketingOptIn: Boolean = true,
)
@Serializable data class ChangePasswordBody(@SerialName("current_password") val currentPassword: String, @SerialName("new_password") val newPassword: String)
@Serializable data class AddItemBody(val variant: String, val quantity: Int)
@Serializable data class QuantityBody(val quantity: Int)
@Serializable data class CouponBody(val code: String)
@Serializable data class WishlistBody(val product: String)
@Serializable data class StockAlertBody(val variant: String, val email: String? = null)
@Serializable data class QuoteBody(val city: String, val province: String = "", @SerialName("payment_method") val paymentMethod: String = "cod")
@Serializable data class CheckoutAddress(
    @SerialName("full_name") val fullName: String,
    val phone: String,
    val line1: String,
    val line2: String = "",
    val city: String,
    val province: String = "",
    @SerialName("postal_code") val postalCode: String = "",
    val country: String = "PK",
)
@Serializable data class CheckoutBody(
    val email: String,
    val address: CheckoutAddress,
    @SerialName("payment_method") val paymentMethod: String,
    val notes: String = "",
    @SerialName("save_address") val saveAddress: Boolean = false,
    @SerialName("is_gift") val isGift: Boolean = false,
)
@Serializable data class CancelBody(val reason: String = "")
@Serializable data class ReorderResult(val added: Int = 0, val detail: String = "")
@Serializable data class ReturnItemBody(@SerialName("order_item") val orderItem: String, val quantity: Int)
@Serializable data class ReturnBody(val kind: String, val reason: String, val details: String = "", val items: List<ReturnItemBody>)
@Serializable data class ReviewBody(val product: String, val rating: Int, val title: String = "", val body: String = "")
@Serializable data class StartChatBody(val body: String, val subject: String = "", val topic: String = "", val order: String? = null)
@Serializable data class ProfilePatch(
    @SerialName("first_name") val firstName: String? = null,
    @SerialName("last_name") val lastName: String? = null,
    val phone: String? = null,
    val language: String? = null,
    @SerialName("marketing_opt_in") val marketingOptIn: Boolean? = null,
    @SerialName("notification_preferences") val notificationPreferences: Map<String, Boolean>? = null,
)

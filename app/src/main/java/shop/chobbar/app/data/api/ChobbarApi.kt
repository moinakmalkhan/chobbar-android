package shop.chobbar.app.data.api

import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.Query
import retrofit2.http.QueryMap
import retrofit2.http.Url
import shop.chobbar.app.data.model.*

interface ChobbarApi {
    @GET("locale/") suspend fun locale(): LocaleInfo

    // Auth & account
    @POST("auth/login/") suspend fun login(@Body body: LoginBody): AuthResponse
    @POST("auth/login/google/") suspend fun google(@Body body: GoogleBody): AuthResponse
    @POST("auth/register/") suspend fun register(@Body body: RegisterBody): AuthResponse
    @POST("auth/logout/") suspend fun logout(@Body body: RefreshBody)
    @POST("auth/password/reset/") suspend fun requestReset(@Body body: EmailBody): Detail
    @POST("auth/password/change/") suspend fun changePassword(@Body body: ChangePasswordBody): Detail
    @POST("auth/verify-email/resend/") suspend fun resendVerification(): Detail
    @GET("accounts/me/") suspend fun me(): User
    @PATCH("accounts/me/") suspend fun updateMe(@Body body: ProfilePatch): User
    @DELETE("accounts/me/") suspend fun deleteMe()
    @GET("accounts/addresses/") suspend fun addresses(): List<Address>
    @POST("accounts/addresses/") suspend fun createAddress(@Body body: Address): Address
    @PATCH("accounts/addresses/{id}/") suspend fun updateAddress(@Path("id") id: String, @Body body: Address): Address
    @DELETE("accounts/addresses/{id}/") suspend fun deleteAddress(@Path("id") id: String)
    @POST("accounts/newsletter/") suspend fun subscribe(@Body body: EmailBody): Detail

    // Catalog & content
    @GET("content/storefront/") suspend fun storefront(): Storefront
    @GET("content/faq/") suspend fun faq(): List<Faq>
    @GET("content/pages/{slug}/") suspend fun page(@Path("slug") slug: String): ContentPage
    @GET("catalog/categories/") suspend fun categories(): List<CategoryNode>
    @GET("catalog/categories/{slug}/") suspend fun category(@Path("slug") slug: String): CategoryDetail
    @GET("catalog/collections/") suspend fun collections(): List<Collection>
    @GET("catalog/collections/{slug}/") suspend fun collection(@Path("slug") slug: String): Collection
    @GET("catalog/products/") suspend fun products(@QueryMap params: Map<String, String>): Paginated<ProductCard>
    @GET("catalog/products/facets/") suspend fun facets(@QueryMap params: Map<String, String>): Facets
    @GET("catalog/products/{slug}/") suspend fun product(@Path("slug") slug: String): ProductDetail
    @GET("catalog/products/{slug}/related/") suspend fun related(@Path("slug") slug: String): List<ProductCard>
    @GET("catalog/products/{feed}/") suspend fun feed(@Path("feed") feed: String): List<ProductCard>
    @POST("catalog/stock-alerts/") suspend fun notifyMe(@Body body: StockAlertBody): Detail
    @GET("search/") suspend fun search(@QueryMap params: Map<String, String>): Paginated<ProductCard>
    @GET("search/suggest/") suspend fun suggest(@Query("q") q: String): SuggestResponse
    @GET("search/trending/") suspend fun trending(): Trending
    @GET("search/recommendations/") suspend fun recommendations(@Query("product") product: String? = null): List<ProductCard>

    // Cart & wishlist
    @GET("cart/") suspend fun cart(): Cart
    @POST("cart/") suspend fun addToCart(@Body body: AddItemBody): Cart
    @PATCH("cart/items/{id}/") suspend fun setQuantity(@Path("id") id: String, @Body body: QuantityBody): Cart
    @DELETE("cart/items/{id}/") suspend fun removeItem(@Path("id") id: String): Cart
    @POST("cart/coupon/") suspend fun applyCoupon(@Body body: CouponBody): Cart
    @DELETE("cart/coupon/") suspend fun removeCoupon(): Cart
    @GET("cart/wishlist/") suspend fun wishlist(): WishlistResponse
    @POST("cart/wishlist/") suspend fun toggleWishlist(@Body body: WishlistBody): WishlistToggle

    // Orders & payments
    @POST("orders/shipping-quote/") suspend fun shippingQuote(@Body body: QuoteBody): ShippingQuote
    @POST("orders/checkout/") suspend fun checkout(@Body body: CheckoutBody): Order
    @GET("orders/") suspend fun orders(@Query("page") page: Int, @Query("open") open: Boolean? = null): Paginated<OrderSummary>
    @GET("orders/summary/") suspend fun ordersSummary(): OrdersSummaryCounts
    @GET("orders/{id}/") suspend fun order(@Path("id") id: String): Order
    @POST("orders/{id}/cancel/") suspend fun cancel(@Path("id") id: String, @Body body: CancelBody): Order
    @POST("orders/{id}/reorder/") suspend fun reorder(@Path("id") id: String): ReorderResult
    @POST("orders/{id}/returns/") suspend fun requestReturn(@Path("id") id: String, @Body body: ReturnBody): ReturnRequest
    @GET("orders/track/") suspend fun track(@Query("number") number: String, @Query("contact") contact: String): TrackingInfo
    @GET("payments/bank-accounts/") suspend fun bankAccounts(): List<BankAccount>
    @Multipart
    @POST("payments/orders/{id}/")
    suspend fun submitProof(
        @Path("id") orderId: String,
        @Part("reference") reference: RequestBody,
        @Part("email") email: RequestBody,
        @Part proof: MultipartBody.Part?,
    ): Detail

    // Reviews
    @GET("reviews/") suspend fun reviews(@Query("product") product: String, @Query("page") page: Int = 1): Paginated<Review>
    @GET("reviews/summary/") suspend fun reviewSummary(@Query("product") product: String): ReviewSummary
    @POST("reviews/") suspend fun createReview(@Body body: ReviewBody): Review

    // Notifications & chat
    @GET suspend fun notifications(@Url url: String): CursorPage<Notification>
    @GET("notifications/unread-count/") suspend fun unreadCount(): Count
    @POST("notifications/read-all/") suspend fun readAll()
    @POST("notifications/{id}/read/") suspend fun markRead(@Path("id") id: String)
    @GET("chat/conversations/") suspend fun conversations(): Paginated<Conversation>
    @POST("chat/conversations/") suspend fun startConversation(@Body body: StartChatBody): Conversation
    @GET suspend fun messages(@Url url: String): CursorPage<ChatMessage>
}

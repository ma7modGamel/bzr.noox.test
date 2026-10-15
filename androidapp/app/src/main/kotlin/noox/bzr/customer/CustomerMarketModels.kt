package noox.bzr.customer

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// DEC-061, 9أ stage 3 — offers, the provider profile and price proposals (C06–C08, C26, C30/C31), read from
// the recorded server answers in src/test/resources/api/customer (MobileCustomerMarketResponsesTest,
// MobileCustomerOrderResponsesTest). Money and ratings are decimal on the server, so they arrive as strings.

@Serializable
data class OfferProvider(
    val id: Int,
    val name: String,
    @SerialName("is_verified") val isVerified: Boolean = false,
    @SerialName("available_now") val availableNow: Boolean = false,
    @SerialName("rating_avg") val ratingAvg: String? = null,
    @SerialName("completed_orders") val completedOrders: Int? = null,
    @SerialName("ratings_count") val ratingsCount: Int? = null,
)

@Serializable
data class CustomerOffer(
    val id: Int,
    val price: String,
    @SerialName("eta_minutes") val etaMinutes: Int? = null,
    @SerialName("inspection_fee_deductible") val inspectionFeeDeductible: Boolean = false,
    @SerialName("includes_text") val includesText: String? = null,
    val note: String? = null,
    val provider: OfferProvider,
)

@Serializable
data class RatingBreakdown(val quality: String? = null, val punctuality: String? = null, val conduct: String? = null)

@Serializable
data class ProviderReview(
    val photos: List<CustomerPhoto> = emptyList(),
    val area: String? = null,
    val tags: List<String> = emptyList(),
    @SerialName("customer_name") val customerName: String,
    val quality: Int,
    val punctuality: Int,
    val conduct: Int,
    val comment: String? = null,
    @SerialName("created_at") val createdAt: String? = null,
)

/** `GET providers/{id}?order_id=` (C07): only what the customer may see — no phone, no payout details. */
@Serializable
data class ReviewTagStatistic(val code: String, val label: String, val count: Int)

@Serializable
data class ProviderContext(
    @SerialName("tag_statistics") val tagStatistics: List<ReviewTagStatistic> = emptyList(),
    val id: Int,
    val name: String,
    @SerialName("avatar_url") val avatarUrl: String? = null,
    val bio: String? = null,
    @SerialName("is_verified") val isVerified: Boolean = false,
    @SerialName("available_now") val availableNow: Boolean = false,
    @SerialName("experience_years") val experienceYears: Int? = null,
    @SerialName("rating_avg") val ratingAvg: String? = null,
    @SerialName("completed_orders") val completedOrders: Int? = null,
    @SerialName("rating_breakdown") val ratingBreakdown: RatingBreakdown = RatingBreakdown(),
    @SerialName("ratings_count") val ratingsCount: Int? = null,
    val specialties: List<IdName> = emptyList(),
    val portfolio: List<PortfolioItem> = emptyList(),
    val reviews: List<ProviderReview> = emptyList(),
    @SerialName("available_actions") val availableActions: List<String> = emptyList(),
)

@Serializable
data class CustomerProposal(
    val id: Int,
    val type: String,
    @SerialName("type_label") val typeLabel: String,
    val amount: String,
    @SerialName("projected_total") val projectedTotal: String,
    val reason: String,
    @SerialName("photo_path") val photoPath: String? = null,
    @SerialName("photo_url") val photoUrl: String? = null,
    val status: String,
    @SerialName("expires_at") val expiresAt: String? = null,
    @SerialName("server_time") val serverTime: String? = null,
    @SerialName("expiry_message") val expiryMessage: String? = null,
    @SerialName("current_total") val currentTotal: String? = null,
) {
    fun remainingSeconds(deviceNow: java.time.OffsetDateTime = java.time.OffsetDateTime.now()): Int =
        expiresAt?.let { expiry -> runCatching {
            java.time.Duration.between(serverTime?.let(java.time.OffsetDateTime::parse) ?: deviceNow, java.time.OffsetDateTime.parse(expiry)).seconds.toInt().coerceAtLeast(0)
        }.getOrDefault(0) } ?: 0
}

/** A provider's portfolio entry; image_url is authenticated and scoped to the customer's order. */
@Serializable
data class PortfolioItem(
    val id: Int,
    @SerialName("image_path") val imagePath: String? = null,
    @SerialName("image_url") val imageUrl: String? = null,
    val caption: String? = null,
)

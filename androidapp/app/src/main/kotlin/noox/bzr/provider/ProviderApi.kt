package noox.bzr.provider

import noox.bzr.customer.CustomerMediaUpload
import noox.bzr.customer.IdName
import noox.bzr.customer.NotificationItem
import noox.bzr.customer.NotificationsReadBody
import noox.bzr.customer.SendMessageBody
import noox.bzr.network.ApiException
import noox.bzr.network.BremoHttp
import noox.bzr.network.bremoExecute
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.Call

data class ProviderApplicationPayload(
    val status: String,
    val displayStatus: String,
    val reason: String?,
    val experienceYears: Int?,
    val bio: String,
    val categoryIds: List<Int>,
    val specialtyIds: List<Int>,
    val areaIds: List<Int>,
    val payoutMethod: String,
    val payoutDetails: String,
    val profilePhotoUploaded: Boolean,
    val idFrontUploaded: Boolean,
    val idBackUploaded: Boolean,
    val availableActions: List<String>,
)

data class ProviderChoice(val id: Int, val label: String)
data class ProviderCategory(val id: Int, val label: String, val specialties: List<ProviderChoice>)
data class ProviderPayoutOption(val code: String, val label: String, val fieldLabel: String)
data class ProviderHomeOrder(
    val id: Int,
    val number: String,
    val category: String,
    val problemType: String,
    val area: String,
    val timingType: String,
    val slotStart: String?,
    val displayStatus: String,
)
data class ProviderHomePayload(
    val operatingMode: String,
    val availableNow: Boolean,
    val activeOrder: ProviderHomeOrder?,
    val availableActions: List<String>,
    val unreadNotifications: Int = 0,
)
data class ProviderOfferContext(
    val minimumAmount: String,
    val commissionRate: String,
    val etaCodes: List<String>,
    val etaLabels: List<String>,
    val deductibleCodes: List<String>,
    val deductibleLabels: List<String>,
    val defaultIncludes: String,
)
data class ProviderMarketRequestPayload(
    val order: ProviderOrderPayload,
    val mediaLabels: List<String>,
    val ownOfferId: Int?,
    val ownOfferSummary: List<String>,
    val context: ProviderOfferContext,
)
data class ProviderOfferPayload(
    val id: Int,
    val orderId: Int,
    val orderVersion: Int,
    val title: String,
    val detail: String,
    val status: String,
    val displayStatus: String,
    val availableActions: List<String>,
)
@kotlinx.serialization.Serializable
data class ProviderReason(val code: String, val label: String)
data class ProviderPortfolioItem(val id: Int, val caption: String)
data class ProviderProfilePayload(
    val name: String,
    val rating: String,
    val completedOrders: Int,
    val averageResponseMinutes: Int?,
    val hasAvatar: Boolean,
    val experienceYears: Int,
    val bio: String,
    val categories: List<ProviderCategory>,
    val specialtyIds: List<Int>,
    val areas: List<ProviderChoice>,
    val areaIds: List<Int>,
    val portfolio: List<ProviderPortfolioItem>,
    val availableActions: List<String>,
)
data class ProviderEarningsPayload(
    val operatingMode: String,
    val summary: List<String>,
    val transactionTitles: List<String>,
    val transactionDetails: List<String>,
    val transactionStates: List<String>,
    val duesBlocked: Boolean = false,
    val duesDebt: String = "0.00",
    val duesLimit: String? = null,
    val filterOptions: List<ProviderReason> = emptyList(),
    val currentPage: Int = 1,
    val lastPage: Int = 1,
)
data class ProviderConversationSummary(val id: Int, val orderId: Int, val customerName: String)
data class ProviderConversationPayload(
    val status: String,
    val customerName: String,
    val orderSummary: String,
    val bodies: List<String>,
    val kinds: List<String>,
    val times: List<String>,
    val imageGroups: List<String> = emptyList(),
)
data class ProviderOrderPayload(
    val id: Int,
    val number: String,
    val version: Int,
    val status: String,
    val displayStatus: String,
    val category: String,
    val problemType: String,
    val address: String,
    val area: String,
    val latitude: Double?,
    val longitude: Double?,
    val timingType: String,
    val slotStart: String?,
    val customerName: String,
    val customerPhone: String?,
    val customerRating: String,
    val stepStates: List<String>,
    val availableActions: List<String>,
    val laborTotal: String,
    val materialsTotal: String,
    val finalAmount: String,
    val paymentMethod: String?,
    val paymentMethodLabel: String,
    val priceGuideMinimum: String?,
    val priceGuideMaximum: String?,
    val priceGuideReviewRequired: Boolean,
    val conversationId: Int?,
    val description: String,
    val pricingMode: String,
    val pricingModeLabel: String,
    val materialsResponsibility: String,
    val materialsResponsibilityLabel: String,
    val budgetAmount: String?,
    val offersCloseAt: String?,
    val absenceReportPending: Boolean = false,
    val warrantyCoverage: String = "",
    val completionEstimateAt: String = "",
    val arrivalCodeRequired: Boolean = false,
    val arrivalExceptionPending: Boolean = false,
    val proposalTypeOptions: List<ProviderReason> = emptyList(),
    val customerRatingTagOptions: List<ProviderReason> = emptyList(),
    val beforePhotoCount: Int = 0,
    val afterPhotoCount: Int = 0,
)

interface ProviderApi {
    fun notifications(token: String): List<NotificationItem>
    fun markNotificationsRead(token: String, ids: List<String>)
    fun application(token: String): ProviderApplicationPayload
    fun submit(token: String, body: ApplicationBody): ProviderApplicationPayload
    fun catalog(): List<ProviderCategory>
    fun cities(): List<ProviderChoice>
    fun areas(cityId: Int): List<ProviderChoice>
    fun payoutOptions(): List<ProviderPayoutOption>
    fun attachWorkPhoto(token: String, order: ProviderOrderPayload, mediaId: Int, purpose: String): ProviderOrderPayload = throw UnsupportedOperationException()
    fun image(token: String, url: String): ByteArray = throw UnsupportedOperationException()
    fun uploadImage(token: String, upload: CustomerMediaUpload): Int
    fun home(token: String): ProviderHomePayload
    fun setAvailability(token: String, availableNow: Boolean): ProviderHomePayload
    fun availableRequests(token: String): List<ProviderOrderPayload>
    fun availableRequest(token: String, orderId: Int): ProviderMarketRequestPayload
    fun submitOffer(token: String, orderId: Int, body: OfferBody): ProviderOfferPayload
    fun offers(token: String): List<ProviderOfferPayload>
    fun withdrawOffer(token: String, offerId: Int): ProviderOfferPayload
    fun currentOrders(token: String): List<ProviderOrderPayload>
    fun pastOrders(token: String): List<ProviderOrderPayload>
    fun providerReasons(): List<ProviderReason>
    fun proposalTypes(): List<ProviderReason>
    fun performOrderAction(token: String, order: ProviderOrderPayload, path: String, input: OrderActionInput = OrderActionInput()): ProviderOrderPayload
    fun submitProposal(token: String, order: ProviderOrderPayload, body: ProposalBody)
    fun rateCustomer(token: String, orderId: Int, stars: Int)
    fun rateCustomerWithFeedback(token: String, orderId: Int, stars: Int, tags: List<String>, comment: String?) {
        if (tags.isNotEmpty() || !comment.isNullOrBlank()) throw UnsupportedOperationException()
        rateCustomer(token, orderId, stars)
    }
    fun earnings(token: String): ProviderEarningsPayload
    fun filteredEarnings(token: String, page: Int, type: String?, dateFrom: String?, dateTo: String?): ProviderEarningsPayload {
        if (page != 1 || type != null || dateFrom != null || dateTo != null) throw UnsupportedOperationException()
        return earnings(token)
    }
    fun exportEarnings(token: String, type: String?, dateFrom: String?, dateTo: String?): ByteArray = throw UnsupportedOperationException()

    fun profile(token: String): ProviderProfilePayload
    fun updateProfile(token: String, body: ProfileUpdateBody): ProviderProfilePayload
    fun addPortfolio(token: String, mediaId: Int, caption: String): ProviderPortfolioItem
    fun deletePortfolio(token: String, itemId: Int)
    fun updateAvatar(token: String, mediaId: Int)
    fun conversations(token: String): List<ProviderConversationSummary>
    fun conversation(token: String, conversationId: Int): ProviderConversationPayload
    fun sendMessage(token: String, conversationId: Int, body: String, mediaIds: List<Int>)
}

/** DEC-061 — every provider call is a typed Retrofit function; a refused one becomes [ProviderApiException]. */
class UrlConnectionProviderApi(private val baseUrl: String) : ProviderApi {
    private val service = BremoHttp.typed(baseUrl, ProviderService::class.java, appMode = "PROVIDER")

    // C32 بوضع الفني: الخادم يصفّي بـX-App-Mode (DEC-058).
    override fun notifications(token: String): List<NotificationItem> = typed { service.notifications(bearer(token)) }.data

    override fun markNotificationsRead(token: String, ids: List<String>) {
        typed { service.markNotificationsRead(bearer(token), NotificationsReadBody(ids)) }
    }

    override fun application(token: String): ProviderApplicationPayload = typed { service.application(bearer(token)) }.data.toPayload()

    override fun submit(token: String, body: ApplicationBody): ProviderApplicationPayload =
        typed { service.submitApplication(bearer(token), body) }.data.toPayload()

    override fun catalog(): List<ProviderCategory> = typed { service.catalog() }.data.map { category ->
        ProviderCategory(category.id, category.name, category.problemTypes.map { ProviderChoice(it.id, it.name) })
    }

    override fun cities(): List<ProviderChoice> = typed { service.cities() }.data.mapNotNull(IdName::choice)

    override fun areas(cityId: Int): List<ProviderChoice> = typed { service.areas(cityId) }.data.mapNotNull(IdName::choice)

    override fun payoutOptions(): List<ProviderPayoutOption> = options("provider_payout_methods").map {
        ProviderPayoutOption(it.code, it.label, it.fieldLabel.orEmpty())
    }

    override fun attachWorkPhoto(token: String, order: ProviderOrderPayload, mediaId: Int, purpose: String): ProviderOrderPayload =
        typed { service.attachWorkPhoto(bearer(token), order.id, WorkPhotoBody(mediaId, purpose, order.version)) }.data.toPayload()

    override fun image(token: String, url: String): ByteArray {
        val expected = java.net.URI(baseUrl)
        val actual = java.net.URI(url)
        require(actual.scheme == expected.scheme && actual.host == expected.host && actual.port == expected.port)
        return typed { service.privateImage(bearer(token), url) }.use { it.bytes() }
    }

    override fun uploadImage(token: String, upload: CustomerMediaUpload): Int = typed {
        val file = upload.bytes.toRequestBody(upload.mimeType.toMediaType())
        service.uploadMedia(bearer(token), MultipartBody.Part.createFormData("file", upload.fileName, file))
    }.media.id

    override fun home(token: String): ProviderHomePayload = typed { service.home(bearer(token)) }.data.toPayload()

    override fun setAvailability(token: String, availableNow: Boolean): ProviderHomePayload =
        typed { service.setAvailability(bearer(token), AvailabilityBody(availableNow)) }.data.toPayload()

    override fun availableRequests(token: String): List<ProviderOrderPayload> =
        typed { service.availableRequests(bearer(token)) }.data.map(ProviderOrder::toPayload)

    override fun availableRequest(token: String, orderId: Int): ProviderMarketRequestPayload =
        typed { service.availableRequest(bearer(token), orderId) }.data.toPayload()

    override fun submitOffer(token: String, orderId: Int, body: OfferBody): ProviderOfferPayload =
        typed { service.submitOffer(bearer(token), orderId, body) }.data.toPayload()

    override fun offers(token: String): List<ProviderOfferPayload> = typed { service.offers(bearer(token)) }.data.map(ProviderOffer::toPayload)

    override fun withdrawOffer(token: String, offerId: Int): ProviderOfferPayload =
        typed { service.withdrawOffer(bearer(token), offerId) }.data.toPayload()

    override fun currentOrders(token: String): List<ProviderOrderPayload> =
        typed { service.orders(bearer(token), "current") }.data.map(ProviderOrder::toPayload)

    override fun pastOrders(token: String): List<ProviderOrderPayload> =
        typed { service.orders(bearer(token), "past") }.data.map(ProviderOrder::toPayload)

    override fun providerReasons(): List<ProviderReason> = options("provider_cancellation_reasons").map { ProviderReason(it.code, it.label) }

    override fun proposalTypes(): List<ProviderReason> = options("proposal_types").map { ProviderReason(it.code, it.label) }

    override fun performOrderAction(token: String, order: ProviderOrderPayload, path: String, input: OrderActionInput): ProviderOrderPayload =
        typed {
            service.orderAction(
                bearer(token), order.id, path,
                OrderActionBody(order.version, input.lat, input.lng, input.amount, input.reasonCode, input.note, input.minutes, input.arrivalCode),
            )
        }.data.toPayload()

    override fun submitProposal(token: String, order: ProviderOrderPayload, body: ProposalBody) {
        typed { service.submitProposal(bearer(token), order.id, body) }
    }

    override fun rateCustomer(token: String, orderId: Int, stars: Int) {
        typed { service.rateCustomer(bearer(token), orderId, CustomerRatingBody(stars)) }
    }

    override fun rateCustomerWithFeedback(token: String, orderId: Int, stars: Int, tags: List<String>, comment: String?) {
        typed { service.rateCustomer(bearer(token), orderId, CustomerRatingBody(stars, tags, comment)) }
    }

    override fun earnings(token: String): ProviderEarningsPayload = typed { service.earnings(bearer(token)) }.data.toPayload()
    override fun filteredEarnings(token: String, page: Int, type: String?, dateFrom: String?, dateTo: String?) = typed { service.earnings(bearer(token), page, type, dateFrom, dateTo) }.data.toPayload()
    override fun exportEarnings(token: String, type: String?, dateFrom: String?, dateTo: String?): ByteArray = typed { service.exportEarnings(bearer(token), type, dateFrom, dateTo) }.use { it.bytes() }


    override fun profile(token: String): ProviderProfilePayload = typed { service.profile(bearer(token)) }.data.toPayload()

    override fun updateProfile(token: String, body: ProfileUpdateBody): ProviderProfilePayload =
        typed { service.updateProfile(bearer(token), body) }.data.toPayload()

    override fun addPortfolio(token: String, mediaId: Int, caption: String): ProviderPortfolioItem =
        typed { service.addPortfolio(bearer(token), PortfolioBody(mediaId, caption.ifBlank { null })) }.data.toPayload()

    override fun deletePortfolio(token: String, itemId: Int) {
        typed { service.deletePortfolio(bearer(token), itemId) }
    }

    override fun updateAvatar(token: String, mediaId: Int) {
        typed { service.updateAvatar(bearer(token), AvatarBody(mediaId)) }
    }

    override fun conversations(token: String): List<ProviderConversationSummary> = typed { service.conversations(bearer(token)) }.data.map {
        ProviderConversationSummary(it.id, it.order.id, it.customer?.name.orEmpty())
    }

    override fun conversation(token: String, conversationId: Int): ProviderConversationPayload {
        val page = typed { service.conversationMessages(bearer(token), conversationId) }
        val order = page.conversation.order
        return ProviderConversationPayload(
            status = page.conversation.status,
            customerName = page.conversation.customer?.name.orEmpty(),
            orderSummary = listOf("#${order.number?.toString().orEmpty()}", order.category.orEmpty(), order.statusLabel.orEmpty())
                .filter(String::isNotBlank).joinToString(" · "),
            imageGroups = page.data.map { message -> message.media.mapNotNull { it.contentUrl }.joinToString("\n") },
            bodies = page.data.map { it.body.orEmpty() },
            kinds = page.data.map { message ->
                val side = if (message.isMine) "sent" else "received"
                val kind = when {
                    message.wasMasked -> "blocked"
                    message.media.isNotEmpty() -> "image"
                    else -> "text"
                }
                "${side}_$kind"
            },
            times = page.data.map { it.createdAt.orEmpty() },
        )
    }

    override fun sendMessage(token: String, conversationId: Int, body: String, mediaIds: List<Int>) {
        typed { service.sendMessage(bearer(token), conversationId, SendMessageBody(body.ifBlank { null }, mediaIds)) }
    }

    private fun options(key: String): List<ProviderOption> = typed { service.config() }.optionLists[key].orEmpty()

    private fun bearer(token: String) = "Bearer $token"

    private fun <T> typed(call: () -> Call<T>): T = try {
        bremoExecute(call())
    } catch (exception: ApiException) {
        throw ProviderApiException(exception.status, exception.code, exception.error?.fields.orEmpty(), exception.error?.message)
    }
}

class ProviderApiException(val status: Int, val code: String, val fields: Map<String, List<String>>, val serverMessage: String? = null) : RuntimeException(serverMessage ?: code)

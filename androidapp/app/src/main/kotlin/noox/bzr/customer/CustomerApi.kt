package noox.bzr.customer

import noox.bzr.auth.AccountUser
import noox.bzr.network.ApiError
import noox.bzr.network.ApiException
import noox.bzr.network.BremoHttp
import noox.bzr.network.bremoExecute
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.Call

data class CustomerHomePayload(
    val operatingMode: String,
    val orderCount: Int,
    val firstOrderId: Int?,
    val addressId: Int?,
    val categories: List<CustomerCategory>,
    val optionLists: Map<String, List<CustomerOption>>,
    val optionDefaults: Map<String, String>,
    val serviceHoursFrom: String,
    val serviceHoursTo: String,
    val customerName: String = "",
    val addressLabel: String = "",
    /** The default address's city: C16 slots and the category check need it without opening C14 (6ب). */
    val addressCityId: Int? = null,
    val orders: List<CustomerOrder> = emptyList(),
    val instapay: InstapayDetails? = null,
)

@kotlinx.serialization.Serializable
data class CustomerOption(val code: String, val label: String)

data class CustomerProblemType(val id: Int, val name: String, val isOther: Boolean, val customerPriceGuide: CustomerPriceGuide? = null)

data class CustomerCategory(
    val id: Int,
    val name: String,
    val iconKey: String?,
    val problemTypes: List<CustomerProblemType>,
)

data class CustomerTrackingPayload(
    val latitude: Double?,
    val longitude: Double?,
    val etaMinutes: Int?,
    val etaApproximate: Boolean,
)

interface CustomerApi {
    fun submitStructuredReview(token: String, orderId: Int, quality: Int, punctuality: Int, conduct: Int, comment: String?, mediaIds: List<Int>, tags: List<String>) {
        require(tags.isEmpty()) { "Structured review tags are not supported by this API implementation." }
        submitReviewWithPhotos(token, orderId, quality, punctuality, conduct, comment, mediaIds)
    }
    fun arrivalCode(token: String, orderId: Int, refresh: Boolean = false): ArrivalCode = throw UnsupportedOperationException()
    fun profileImage(token: String, url: String): ByteArray = throw UnsupportedOperationException()
    fun shareVisit(token: String, orderId: Int): VisitShareLink = throw UnsupportedOperationException()
    fun warrantyClaims(token: String, orderId: Int): List<CustomerDispute> = throw UnsupportedOperationException()
    fun requestWarranty(token: String, orderId: Int, description: String): CustomerDispute = throw UnsupportedOperationException()
    fun requestAvailability(token: String, addressId: Int, categoryId: Int): Boolean = throw UnsupportedOperationException()

    fun submitInstapay(token: String, orderId: Int, reference: String, receiptMediaId: Int, expectedVersion: Int): PaymentAttempt = throw UnsupportedOperationException()
    fun submitReviewWithPhotos(token: String, orderId: Int, quality: Int, punctuality: Int, conduct: Int, comment: String?, mediaIds: List<Int>) {
        if (mediaIds.isNotEmpty()) throw UnsupportedOperationException()
        submitReview(token, orderId, quality, punctuality, conduct, comment)
    }

    fun home(token: String): CustomerHomePayload
    fun catalogCategoryIds(cityId: Int): Set<Int>
    fun order(token: String, orderId: Int): CustomerOrder
    fun tracking(token: String, orderId: Int): CustomerTrackingPayload
    fun orders(token: String, scope: String, page: Int = 1): OrdersPage
    fun conversations(token: String, page: Int = 1): ConversationsPage
    fun conversationMessages(token: String, conversationId: Int, page: Int = 1): ConversationMessagesPage
    fun sendMessage(token: String, conversationId: Int, body: String, mediaIds: List<Int> = emptyList()): CustomerMessage
    fun offers(token: String, orderId: Int, sort: String = "rating"): List<CustomerOffer>
    fun provider(token: String, providerId: Int, orderId: Int): ProviderContext
    fun publish(token: String, body: PublishOrderBody, idempotencyKey: String): CustomerOrder
    fun updateOrder(token: String, orderId: Int, body: PublishOrderBody): CustomerOrder
    fun acceptOffer(token: String, orderId: Int, offerId: Int, expectedVersion: Int, paymentMethod: String): CustomerOrder
    fun cancelOrder(token: String, orderId: Int, reasonCode: String, note: String?, expectedVersion: Int): CustomerOrder
    fun proposals(token: String, orderId: Int): List<CustomerProposal>
    fun decideProposal(token: String, orderId: Int, proposalId: Int, approve: Boolean, expectedVersion: Int): CustomerOrder
    fun changePaymentMethod(token: String, orderId: Int, method: String, expectedVersion: Int): CustomerOrder
    fun createPayment(token: String, orderId: Int, channel: String, expectedVersion: Int): PaymentAttempt
    fun confirmCompletion(token: String, orderId: Int, expectedVersion: Int): CustomerOrder
    fun submitReview(token: String, orderId: Int, quality: Int, punctuality: Int, conduct: Int, comment: String?)
    fun addresses(token: String): List<CustomerAddress>
    fun cities(): List<IdName>
    fun areas(cityId: Int): List<IdName>
    fun saveAddress(token: String, addressId: Int?, body: AddressBody): CustomerAddress
    fun makeAddressDefault(token: String, addressId: Int): AddressBookUpdate
    fun deleteAddress(token: String, addressId: Int): AddressDeletion
    fun slots(cityId: Int, date: String): List<SlotRange>
    fun uploadMedia(token: String, upload: CustomerMediaUpload): UploadedMedia
    fun deleteMedia(token: String, mediaId: Int)
    fun disputes(token: String, orderId: Int): List<CustomerDispute>
    fun openDispute(token: String, orderId: Int, reasonCode: String, description: String, mediaIds: List<Int>): CustomerDispute
    fun reportProvider(token: String, providerId: Int, orderId: Int, reasonCode: String, description: String?)
    fun faqs(): List<FaqItem>
    fun sendSupportMessage(token: String, subject: String, message: String)
    fun unreadNotificationCount(token: String): Int = notifications(token).count { it.readAt == null }
    fun updateAccountDetails(token: String, name: String, email: String, phone: String, currentPassword: String?): AccountUser = throw UnsupportedOperationException()
    fun updateAvatar(token: String, mediaId: Int): AccountUser = throw UnsupportedOperationException()
    fun notifications(token: String): List<NotificationItem>
    fun markNotificationsRead(token: String, ids: List<String>)
    fun registerDevice(token: String, deviceToken: String)
    fun unregisterDevice(token: String, deviceToken: String)
    fun logout(token: String)
    fun updateRatingReminders(token: String, enabled: Boolean): AccountUser
    fun account(token: String): AccountUser
    fun updateAccount(token: String, name: String, phone: String): AccountUser
    fun updateLocale(token: String, locale: String)
    fun changePassword(token: String, currentPassword: String, password: String, confirmation: String)
    fun deleteAccount(token: String)
    fun terms(): TermsPage
    fun republish(token: String, orderId: Int): CustomerOrder
}

data class CustomerMediaUpload(val fileName: String, val mimeType: String, val bytes: ByteArray)

class UrlConnectionCustomerApi(private val baseUrl: String) : CustomerApi {
    private val service = BremoHttp.typed(baseUrl, CustomerService::class.java, appMode = "CUSTOMER")

    override fun warrantyClaims(token: String, orderId: Int): List<CustomerDispute> = typed { service.warrantyClaims(bearer(token), orderId) }.data
    override fun requestWarranty(token: String, orderId: Int, description: String): CustomerDispute = typed { service.requestWarranty(bearer(token), orderId, WarrantyBody(description)) }.data

    override fun requestAvailability(token: String, addressId: Int, categoryId: Int): Boolean =
        typed { service.requestAvailability(bearer(token), addressId, categoryId) }.nowAvailable

    override fun catalogCategoryIds(cityId: Int): Set<Int> = typed { service.catalog(cityId) }.data.map(CatalogCategory::id).toSet()

    override fun home(token: String): CustomerHomePayload {
        val bearer = bearer(token)
        val config = typed { service.config() }
        val orders = typed { service.orders(bearer, "current", 1) }.data
        val addresses = typed { service.addresses(bearer) }.data
        val address = addresses.firstOrNull(CustomerAddress::isDefault) ?: addresses.firstOrNull()
        val cityId = address?.city?.id?.takeIf { it > 0 }
        val categories = typed { service.catalog(cityId) }.data
        val user = typed { service.me(bearer) }.user
        return CustomerHomePayload(
            // The app vocabulary is marketplace/staff; the server sends MARKETPLACE/EMPLOYEE (39).
            if (config.operatingMode == "MARKETPLACE") "marketplace" else "staff",
            orders.size,
            orders.firstOrNull()?.id,
            address?.id,
            categories.map { category ->
                CustomerCategory(
                    id = category.id,
                    name = category.name,
                    iconKey = category.iconPath?.takeIf(String::isNotBlank)?.substringAfterLast('/')?.substringBeforeLast('.'),
                    problemTypes = category.problemTypes.map { CustomerProblemType(it.id, it.name, it.isOther, it.customerPriceGuide) },
                )
            },
            config.optionLists.mapValues { (_, items) -> items.map { CustomerOption(it.code, it.label) } },
            config.optionDefaults,
            config.serviceHours.from,
            config.serviceHours.to,
            customerName = user.name,
            addressLabel = address?.let(::addressLabel).orEmpty(),
            addressCityId = cityId,
            orders = orders,
            instapay = config.instapay,
        )
    }

    override fun order(token: String, orderId: Int): CustomerOrder = typed { service.order(bearer(token), orderId) }.data

    override fun shareVisit(token: String, orderId: Int): VisitShareLink = typed { service.shareVisit(bearer(token), orderId) }.data

    override fun arrivalCode(token: String, orderId: Int, refresh: Boolean): ArrivalCode = typed { service.arrivalCode(bearer(token), orderId, ArrivalCodeRequest(refresh)) }.data

    override fun profileImage(token: String, url: String): ByteArray {
        val expected = java.net.URI(baseUrl)
        val actual = java.net.URI(url)
        require(actual.scheme == expected.scheme && actual.host == expected.host && actual.port == expected.port)
        return typed { service.profileImage(bearer(token), url) }.use { it.bytes() }
    }

    override fun tracking(token: String, orderId: Int): CustomerTrackingPayload {
        val payload = typed { service.tracking(bearer(token), orderId) }
        return CustomerTrackingPayload(
            latitude = payload.lastLocation?.lat?.toDoubleOrNull(),
            longitude = payload.lastLocation?.lng?.toDoubleOrNull(),
            etaMinutes = payload.etaMinutes,
            etaApproximate = payload.etaApproximate,
        )
    }

    override fun orders(token: String, scope: String, page: Int): OrdersPage = typed { service.orders(bearer(token), scope, page) }

    override fun conversations(token: String, page: Int): ConversationsPage = typed { service.conversations(bearer(token), page) }

    override fun conversationMessages(token: String, conversationId: Int, page: Int): ConversationMessagesPage =
        typed { service.conversationMessages(bearer(token), conversationId, page) }

    override fun sendMessage(token: String, conversationId: Int, body: String, mediaIds: List<Int>): CustomerMessage =
        typed { service.sendMessage(bearer(token), conversationId, SendMessageBody(body.ifBlank { null }, mediaIds)) }.data

    override fun offers(token: String, orderId: Int, sort: String): List<CustomerOffer> =
        typed { service.offers(bearer(token), orderId, sort) }.data

    override fun provider(token: String, providerId: Int, orderId: Int): ProviderContext =
        typed { service.provider(bearer(token), providerId, orderId) }.data

    override fun publish(token: String, body: PublishOrderBody, idempotencyKey: String): CustomerOrder =
        typed { service.publish(bearer(token), idempotencyKey, body) }.data

    override fun updateOrder(token: String, orderId: Int, body: PublishOrderBody): CustomerOrder =
        typed { service.updateOrder(bearer(token), orderId, body) }.data

    override fun acceptOffer(token: String, orderId: Int, offerId: Int, expectedVersion: Int, paymentMethod: String): CustomerOrder =
        typed { service.acceptOffer(bearer(token), orderId, offerId, AcceptOfferRequest(expectedVersion, paymentMethod)) }.data

    override fun cancelOrder(token: String, orderId: Int, reasonCode: String, note: String?, expectedVersion: Int): CustomerOrder =
        typed { service.cancelOrder(bearer(token), orderId, CancelOrderRequest(reasonCode, note, expectedVersion)) }.data

    override fun proposals(token: String, orderId: Int): List<CustomerProposal> =
        typed { service.proposals(bearer(token), orderId) }.data

    override fun decideProposal(token: String, orderId: Int, proposalId: Int, approve: Boolean, expectedVersion: Int): CustomerOrder =
        typed { service.decideProposal(bearer(token), orderId, proposalId, DecideProposalRequest(approve, expectedVersion)) }.data

    override fun changePaymentMethod(token: String, orderId: Int, method: String, expectedVersion: Int): CustomerOrder =
        typed { service.changePaymentMethod(bearer(token), orderId, PaymentMethodRequest(method, expectedVersion)) }.data

    override fun submitInstapay(token: String, orderId: Int, reference: String, receiptMediaId: Int, expectedVersion: Int): PaymentAttempt =
        typed { service.submitInstapay(bearer(token), orderId, InstapayTransferBody(reference, receiptMediaId, expectedVersion)) }.data

    override fun submitStructuredReview(token: String, orderId: Int, quality: Int, punctuality: Int, conduct: Int, comment: String?, mediaIds: List<Int>, tags: List<String>) {
        typed { service.submitReview(bearer(token), orderId, ReviewBody(quality, punctuality, conduct, comment, mediaIds, tags)) }
    }

    override fun submitReviewWithPhotos(token: String, orderId: Int, quality: Int, punctuality: Int, conduct: Int, comment: String?, mediaIds: List<Int>) {
        typed { service.submitReview(bearer(token), orderId, ReviewBody(quality, punctuality, conduct, comment, mediaIds)) }
    }

    override fun createPayment(token: String, orderId: Int, channel: String, expectedVersion: Int): PaymentAttempt =
        typed { service.createPayment(bearer(token), orderId, CreatePaymentBody(channel, expectedVersion)) }.data

    override fun confirmCompletion(token: String, orderId: Int, expectedVersion: Int): CustomerOrder =
        typed { service.confirmCompletion(bearer(token), orderId, ExpectedVersionRequest(expectedVersion)) }.data

    override fun submitReview(token: String, orderId: Int, quality: Int, punctuality: Int, conduct: Int, comment: String?) {
        typed { service.submitReview(bearer(token), orderId, ReviewBody(quality, punctuality, conduct, comment)) }
    }

    override fun addresses(token: String): List<CustomerAddress> = typed { service.addresses(bearer(token)) }.data

    override fun cities(): List<IdName> = typed { service.cities() }.data

    override fun areas(cityId: Int): List<IdName> = typed { service.areas(cityId) }.data

    override fun saveAddress(token: String, addressId: Int?, body: AddressBody): CustomerAddress = typed {
        if (addressId == null) service.createAddress(bearer(token), body) else service.updateAddress(bearer(token), addressId, body)
    }.data

    override fun makeAddressDefault(token: String, addressId: Int): AddressBookUpdate =
        typed { service.makeAddressDefault(bearer(token), addressId) }

    override fun deleteAddress(token: String, addressId: Int): AddressDeletion =
        typed { service.deleteAddress(bearer(token), addressId) }.data

    override fun slots(cityId: Int, date: String): List<SlotRange> = typed { service.slots(cityId, date) }.data

    override fun uploadMedia(token: String, upload: CustomerMediaUpload): UploadedMedia = typed {
        val file = upload.bytes.toRequestBody(upload.mimeType.toMediaType())
        service.uploadMedia(bearer(token), MultipartBody.Part.createFormData("file", upload.fileName, file))
    }.media

    override fun deleteMedia(token: String, mediaId: Int) {
        typed { service.deleteMedia(bearer(token), mediaId) }
    }

    override fun disputes(token: String, orderId: Int): List<CustomerDispute> = typed { service.disputes(bearer(token), orderId) }.data

    override fun openDispute(token: String, orderId: Int, reasonCode: String, description: String, mediaIds: List<Int>): CustomerDispute =
        typed { service.openDispute(bearer(token), orderId, OpenDisputeBody(reasonCode, description, mediaIds)) }.data

    override fun reportProvider(token: String, providerId: Int, orderId: Int, reasonCode: String, description: String?) {
        typed { service.reportProvider(bearer(token), providerId, ProviderReportBody(orderId, reasonCode, description)) }
    }

    override fun faqs(): List<FaqItem> = typed { service.faqs() }.data

    override fun sendSupportMessage(token: String, subject: String, message: String) {
        typed { service.sendSupportMessage(bearer(token), SupportMessageBody(subject, message)) }
    }

    override fun unreadNotificationCount(token: String): Int = typed { service.notifications(bearer(token)) }.let { response -> response.meta?.unreadCount ?: response.data.count { it.readAt == null } }
    override fun updateAccountDetails(token: String, name: String, email: String, phone: String, currentPassword: String?): AccountUser =
        typed { service.updateAccount(bearer(token), AccountUpdateBody(name, phone, email, currentPassword)) }.user
    override fun updateAvatar(token: String, mediaId: Int): AccountUser =
        typed { service.updateAccount(bearer(token), AccountUpdateBody(avatarMediaId = mediaId)) }.user

    override fun notifications(token: String): List<NotificationItem> = typed { service.notifications(bearer(token)) }.data

    override fun markNotificationsRead(token: String, ids: List<String>) {
        typed { service.markNotificationsRead(bearer(token), NotificationsReadBody(ids)) }
    }

    // DEC-058 — رمز FCM للجهاز؛ Push يصل لكل رموز الحساب أيًّا كان الوضع.
    override fun registerDevice(token: String, deviceToken: String) {
        typed { service.registerDevice(bearer(token), RegisterDeviceBody(deviceToken, "ANDROID")) }
    }

    override fun unregisterDevice(token: String, deviceToken: String) {
        typed { service.unregisterDevice(bearer(token), DeviceTokenBody(deviceToken)) }
    }

    override fun logout(token: String) {
        typed { service.logout(bearer(token)) }
    }

    override fun updateRatingReminders(token: String, enabled: Boolean): AccountUser =
        typed { service.updateRatingReminders(bearer(token), RatingRemindersBody(enabled)) }.user

    override fun account(token: String): AccountUser = typed { service.me(bearer(token)) }.user

    override fun updateLocale(token: String, locale: String) {
        typed { service.updateLocale(bearer(token), LocaleUpdateBody(locale)) }
    }

    override fun updateAccount(token: String, name: String, phone: String): AccountUser =
        typed { service.updateAccount(bearer(token), AccountUpdateBody(name, phone)) }.user

    override fun changePassword(token: String, currentPassword: String, password: String, confirmation: String) {
        typed { service.changePassword(bearer(token), PasswordChangeBody(currentPassword, password, confirmation)) }
    }

    override fun deleteAccount(token: String) {
        typed { service.deleteAccount(bearer(token)) }
    }

    override fun terms(): TermsPage = typed { service.terms() }.data

    override fun republish(token: String, orderId: Int): CustomerOrder = typed { service.republish(bearer(token), orderId) }.data


    private fun bearer(token: String) = "Bearer $token"

    /** A typed call (DEC-061); a refused request keeps the [CustomerApiException] the screens already handle. */
    private fun <T> typed(call: () -> Call<T>): T = try {
        bremoExecute(call())
    } catch (exception: ApiException) {
        throw CustomerApiException(exception.status, exception.message.orEmpty(), exception.error)
    }

}

class CustomerApiException(val status: Int, message: String, val error: ApiError? = null) : RuntimeException(message)

/** C04 address row: the saved label and area, as the C14 row shows them. */
internal fun addressLabel(address: CustomerAddress): String =
    listOf(address.label, address.area.name.orEmpty()).filter(String::isNotBlank).joinToString(" · ")

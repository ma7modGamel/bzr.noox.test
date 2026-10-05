package noox.bzr.customer

import noox.bzr.network.ApiError
import noox.bzr.network.ApiException
import noox.bzr.network.BremoApiClient
import noox.bzr.network.BremoHttp
import noox.bzr.network.bremoExecute
import org.json.JSONArray
import org.json.JSONObject
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
)

data class CustomerOption(val code: String, val label: String)

data class CustomerProblemType(val id: Int, val name: String, val isOther: Boolean)

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
    fun home(token: String): CustomerHomePayload
    fun catalogCategoryIds(cityId: Int): Set<Int>
    fun order(token: String, orderId: Int): CustomerOrder
    fun tracking(token: String, orderId: Int): CustomerTrackingPayload
    fun orders(token: String, scope: String, page: Int = 1): OrdersPage
    fun conversations(token: String, page: Int = 1): JSONObject
    fun conversationMessages(token: String, conversationId: Int, page: Int = 1): JSONObject
    fun sendMessage(token: String, conversationId: Int, body: String, mediaIds: List<Int> = emptyList()): JSONObject
    fun offers(token: String, orderId: Int, sort: String = "rating"): JSONArray
    fun provider(token: String, providerId: Int, orderId: Int): JSONObject
    fun publish(token: String, body: JSONObject, idempotencyKey: String): CustomerOrder
    fun updateOrder(token: String, orderId: Int, body: JSONObject): CustomerOrder
    fun acceptOffer(token: String, orderId: Int, offerId: Int, expectedVersion: Int, paymentMethod: String): CustomerOrder
    fun cancelOrder(token: String, orderId: Int, reasonCode: String, note: String?, expectedVersion: Int): CustomerOrder
    fun proposals(token: String, orderId: Int): JSONArray
    fun decideProposal(token: String, orderId: Int, proposalId: Int, approve: Boolean, expectedVersion: Int): CustomerOrder
    fun changePaymentMethod(token: String, orderId: Int, method: String, expectedVersion: Int): CustomerOrder
    fun createPayment(token: String, orderId: Int, channel: String, expectedVersion: Int): JSONObject
    fun confirmCompletion(token: String, orderId: Int, expectedVersion: Int): CustomerOrder
    fun submitReview(token: String, orderId: Int, quality: Int, punctuality: Int, conduct: Int, comment: String?): JSONObject
    fun addresses(token: String): JSONArray
    fun cities(): JSONArray
    fun areas(cityId: Int): JSONArray
    fun saveAddress(token: String, addressId: Int?, body: JSONObject): JSONObject
    fun deleteAddress(token: String, addressId: Int)
    fun slots(cityId: Int, date: String): JSONArray
    fun uploadMedia(token: String, upload: CustomerMediaUpload): JSONObject
    fun deleteMedia(token: String, mediaId: Int)
    fun disputes(token: String, orderId: Int): JSONArray
    fun openDispute(token: String, orderId: Int, reasonCode: String, description: String, mediaIds: List<Int>): JSONObject
    fun reportProvider(token: String, providerId: Int, orderId: Int, reasonCode: String, description: String?): JSONObject
    fun faqs(): JSONArray
    fun sendSupportMessage(token: String, subject: String, message: String)
    fun notifications(token: String): JSONObject
    fun markNotificationsRead(token: String, ids: List<String>)
    fun registerDevice(token: String, deviceToken: String)
    fun unregisterDevice(token: String, deviceToken: String)
    fun logout(token: String)
    fun updateRatingReminders(token: String, enabled: Boolean): JSONObject
    fun account(token: String): JSONObject
    fun updateAccount(token: String, name: String, phone: String): JSONObject
    fun changePassword(token: String, currentPassword: String, password: String, confirmation: String)
    fun deleteAccount(token: String)
    fun terms(): JSONObject
    fun republish(token: String, orderId: Int): CustomerOrder
}

data class CustomerMediaUpload(val fileName: String, val mimeType: String, val bytes: ByteArray)

class UrlConnectionCustomerApi(private val baseUrl: String) : CustomerApi {
    private val service = BremoHttp.typed(baseUrl, CustomerService::class.java, appMode = "CUSTOMER")

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
                    problemTypes = category.problemTypes.map { CustomerProblemType(it.id, it.name, it.isOther) },
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
        )
    }

    override fun order(token: String, orderId: Int): CustomerOrder = typed { service.order(bearer(token), orderId) }.data

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

    override fun conversations(token: String, page: Int): JSONObject =
        request("conversations?page=$page", token = token, method = "GET")

    override fun conversationMessages(token: String, conversationId: Int, page: Int): JSONObject =
        request("conversations/$conversationId/messages?page=$page", token = token, method = "GET")

    override fun sendMessage(token: String, conversationId: Int, body: String, mediaIds: List<Int>): JSONObject =
        request(
            "conversations/$conversationId/messages",
            JSONObject().put("body", body.ifBlank { JSONObject.NULL }).put("media_ids", JSONArray(mediaIds)),
            token,
        ).getJSONObject("data")

    override fun offers(token: String, orderId: Int, sort: String): JSONArray =
        request("orders/$orderId/offers?sort=$sort", token = token, method = "GET").getJSONArray("data")

    override fun provider(token: String, providerId: Int, orderId: Int): JSONObject =
        request("providers/$providerId?order_id=$orderId", token = token, method = "GET").getJSONObject("data")

    override fun publish(token: String, body: JSONObject, idempotencyKey: String): CustomerOrder =
        typed { service.publish(bearer(token), idempotencyKey, body.toJsonElement()) }.data

    override fun updateOrder(token: String, orderId: Int, body: JSONObject): CustomerOrder =
        typed { service.updateOrder(bearer(token), orderId, body.toJsonElement()) }.data

    override fun acceptOffer(token: String, orderId: Int, offerId: Int, expectedVersion: Int, paymentMethod: String): CustomerOrder =
        typed { service.acceptOffer(bearer(token), orderId, offerId, AcceptOfferRequest(expectedVersion, paymentMethod)) }.data

    override fun cancelOrder(token: String, orderId: Int, reasonCode: String, note: String?, expectedVersion: Int): CustomerOrder =
        typed { service.cancelOrder(bearer(token), orderId, CancelOrderRequest(reasonCode, note, expectedVersion)) }.data

    override fun proposals(token: String, orderId: Int): JSONArray =
        request("orders/$orderId/proposals", token = token, method = "GET").getJSONArray("data")

    override fun decideProposal(token: String, orderId: Int, proposalId: Int, approve: Boolean, expectedVersion: Int): CustomerOrder =
        typed { service.decideProposal(bearer(token), orderId, proposalId, DecideProposalRequest(approve, expectedVersion)) }.data

    override fun changePaymentMethod(token: String, orderId: Int, method: String, expectedVersion: Int): CustomerOrder =
        typed { service.changePaymentMethod(bearer(token), orderId, PaymentMethodRequest(method, expectedVersion)) }.data

    override fun createPayment(token: String, orderId: Int, channel: String, expectedVersion: Int): JSONObject =
        request(
            "orders/$orderId/payments",
            JSONObject().put("channel", channel).put("expected_version", expectedVersion),
            token,
        ).getJSONObject("data")

    override fun confirmCompletion(token: String, orderId: Int, expectedVersion: Int): CustomerOrder =
        typed { service.confirmCompletion(bearer(token), orderId, ExpectedVersionRequest(expectedVersion)) }.data

    override fun submitReview(
        token: String,
        orderId: Int,
        quality: Int,
        punctuality: Int,
        conduct: Int,
        comment: String?,
    ): JSONObject = request(
        "orders/$orderId/review",
        JSONObject().put("quality", quality).put("punctuality", punctuality).put("conduct", conduct).put("comment", comment),
        token,
    ).getJSONObject("data")

    override fun addresses(token: String): JSONArray =
        request("addresses", token = token, method = "GET").getJSONArray("data")

    override fun cities(): JSONArray = request("cities", method = "GET").getJSONArray("data")

    override fun areas(cityId: Int): JSONArray = request("cities/$cityId/areas", method = "GET").getJSONArray("data")

    override fun saveAddress(token: String, addressId: Int?, body: JSONObject): JSONObject = request(
        if (addressId == null) "addresses" else "addresses/$addressId",
        body,
        token,
        method = if (addressId == null) "POST" else "PATCH",
    ).getJSONObject("data")

    override fun deleteAddress(token: String, addressId: Int) {
        request("addresses/$addressId", token = token, method = "DELETE")
    }

    override fun slots(cityId: Int, date: String): JSONArray =
        request("slots?city_id=$cityId&date=$date", method = "GET").getJSONArray("data")

    override fun uploadMedia(token: String, upload: CustomerMediaUpload): JSONObject =
        multipart("media", token, upload).getJSONObject("media")

    override fun deleteMedia(token: String, mediaId: Int) {
        request("media/$mediaId", token = token, method = "DELETE")
    }

    override fun disputes(token: String, orderId: Int): JSONArray =
        request("orders/$orderId/disputes", token = token, method = "GET").getJSONArray("data")

    override fun openDispute(
        token: String,
        orderId: Int,
        reasonCode: String,
        description: String,
        mediaIds: List<Int>,
    ): JSONObject = request(
        "orders/$orderId/disputes",
        JSONObject().put("reason_code", reasonCode).put("description", description).put("media_ids", JSONArray(mediaIds)),
        token,
    ).getJSONObject("data")

    override fun reportProvider(
        token: String,
        providerId: Int,
        orderId: Int,
        reasonCode: String,
        description: String?,
    ): JSONObject = request(
        "providers/$providerId/reports",
        JSONObject().put("order_id", orderId).put("reason_code", reasonCode).put("description", description),
        token,
    ).getJSONObject("data")

    override fun faqs(): JSONArray = request("support/faqs", method = "GET").getJSONArray("data")

    override fun sendSupportMessage(token: String, subject: String, message: String) {
        request("support/messages", JSONObject().put("subject", subject).put("message", message), token)
    }

    override fun notifications(token: String): JSONObject = request("notifications", token = token, method = "GET")

    override fun markNotificationsRead(token: String, ids: List<String>) {
        request("notifications/read", JSONObject().put("notification_ids", JSONArray(ids)), token)
    }

    // DEC-058 — رمز FCM للجهاز؛ Push يصل لكل رموز الحساب أيًّا كان الوضع.
    override fun registerDevice(token: String, deviceToken: String) {
        request("me/devices", JSONObject().put("token", deviceToken).put("platform", "ANDROID"), token)
    }

    override fun unregisterDevice(token: String, deviceToken: String) {
        request("me/devices", JSONObject().put("token", deviceToken), token, method = "DELETE")
    }

    override fun logout(token: String) {
        request("auth/logout", JSONObject(), token)
    }

    override fun updateRatingReminders(token: String, enabled: Boolean): JSONObject = request(
        "me", JSONObject().put("rating_reminders_enabled", enabled), token, method = "PATCH",
    ).getJSONObject("user")

    override fun account(token: String): JSONObject = request("me", token = token, method = "GET").getJSONObject("user")

    override fun updateAccount(token: String, name: String, phone: String): JSONObject = request(
        "me", JSONObject().put("name", name).put("phone", phone), token, method = "PATCH",
    ).getJSONObject("user")

    override fun changePassword(token: String, currentPassword: String, password: String, confirmation: String) {
        request(
            "me/password",
            JSONObject().put("current_password", currentPassword).put("password", password)
                .put("password_confirmation", confirmation),
            token,
        )
    }

    override fun deleteAccount(token: String) {
        request("me", token = token, method = "DELETE")
    }

    override fun terms(): JSONObject = request("terms/current", method = "GET").getJSONObject("data")

    override fun republish(token: String, orderId: Int): CustomerOrder = typed { service.republish(bearer(token), orderId) }.data

    private val client = BremoApiClient(baseUrl, appMode = "CUSTOMER")

    private fun bearer(token: String) = "Bearer $token"

    /** A typed call (DEC-061); a refused request keeps the [CustomerApiException] the screens already handle. */
    private fun <T> typed(call: () -> Call<T>): T = try {
        bremoExecute(call())
    } catch (exception: ApiException) {
        throw CustomerApiException(exception.status, exception.message.orEmpty(), exception.error)
    }

    /** Stage 3 types the C03–C05 request body; until then it is passed through as built. */
    private fun JSONObject.toJsonElement() = BremoHttp.json.parseToJsonElement(toString())

    private fun request(
        path: String,
        body: JSONObject? = null,
        token: String? = null,
        method: String = "POST",
        idempotencyKey: String? = null,
    ): JSONObject {
        val response = client.send(path, method, body, token, idempotencyKey)
        if (!response.isSuccessful) throw CustomerApiException(response.status, response.text)
        return response.json()
    }

    private fun multipart(path: String, token: String, upload: CustomerMediaUpload): JSONObject {
        val response = client.upload(path, token, upload.fileName, upload.mimeType, upload.bytes)
        if (!response.isSuccessful) throw CustomerApiException(response.status, response.text)
        return JSONObject(response.text)
    }
}

/** [error] is the decoded server error of a typed call; legacy calls keep the raw body in the message. */
class CustomerApiException(val status: Int, message: String, val error: ApiError? = null) : RuntimeException(message)

/** C04 address row: the saved label and area, as the C14 row shows them. */
internal fun addressLabel(address: JSONObject): String =
    listOf(address.optString("label"), address.optJSONObject("area")?.optString("name").orEmpty())
        .filter(String::isNotBlank).joinToString(" · ")

internal fun addressLabel(address: CustomerAddress): String =
    listOf(address.label, address.area.name.orEmpty()).filter(String::isNotBlank).joinToString(" · ")

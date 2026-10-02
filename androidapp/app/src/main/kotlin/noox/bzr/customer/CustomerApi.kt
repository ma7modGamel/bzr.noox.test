package noox.bzr.customer

import java.net.HttpURLConnection
import java.net.URL
import java.io.ByteArrayOutputStream
import org.json.JSONArray
import org.json.JSONObject

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
    val orders: List<JSONObject> = emptyList(),
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
    fun order(token: String, orderId: Int): JSONObject
    fun tracking(token: String, orderId: Int): CustomerTrackingPayload
    fun orders(token: String, scope: String, page: Int = 1): JSONObject
    fun conversations(token: String, page: Int = 1): JSONObject
    fun conversationMessages(token: String, conversationId: Int, page: Int = 1): JSONObject
    fun sendMessage(token: String, conversationId: Int, body: String, mediaIds: List<Int> = emptyList()): JSONObject
    fun offers(token: String, orderId: Int, sort: String = "rating"): JSONArray
    fun provider(token: String, providerId: Int, orderId: Int): JSONObject
    fun publish(token: String, body: JSONObject, idempotencyKey: String): JSONObject
    fun updateOrder(token: String, orderId: Int, body: JSONObject): JSONObject
    fun acceptOffer(token: String, orderId: Int, offerId: Int, expectedVersion: Int, paymentMethod: String): JSONObject
    fun cancelOrder(token: String, orderId: Int, reasonCode: String, note: String?, expectedVersion: Int): JSONObject
    fun proposals(token: String, orderId: Int): JSONArray
    fun decideProposal(token: String, orderId: Int, proposalId: Int, approve: Boolean, expectedVersion: Int): JSONObject
    fun changePaymentMethod(token: String, orderId: Int, method: String, expectedVersion: Int): JSONObject
    fun createPayment(token: String, orderId: Int, channel: String, expectedVersion: Int): JSONObject
    fun confirmCompletion(token: String, orderId: Int, expectedVersion: Int): JSONObject
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
    fun republish(token: String, orderId: Int): JSONObject
}

data class CustomerMediaUpload(val fileName: String, val mimeType: String, val bytes: ByteArray)

class UrlConnectionCustomerApi(private val baseUrl: String) : CustomerApi {
    override fun catalogCategoryIds(cityId: Int): Set<Int> {
        val categories = request("catalog?city_id=$cityId", method = "GET").getJSONArray("data")
        return (0 until categories.length()).map { categories.getJSONObject(it).getInt("id") }.toSet()
    }

    override fun home(token: String): CustomerHomePayload {
        val config = request("config", method = "GET")
        val orders = request("orders?scope=current", token = token, method = "GET").getJSONArray("data")
        val addresses = request("addresses", token = token, method = "GET").getJSONArray("data")
        val address = (0 until addresses.length()).map(addresses::getJSONObject)
            .let { list -> list.firstOrNull { it.optBoolean("is_default") } ?: list.firstOrNull() }
        val cityId = address?.optJSONObject("city")?.optInt("id")?.takeIf { it > 0 }
        val categories = request("catalog${cityId?.let { "?city_id=$it" }.orEmpty()}", method = "GET").getJSONArray("data")
        val user = request("me", token = token, method = "GET").getJSONObject("user")
        val categoryItems = (0 until categories.length()).map(categories::getJSONObject).map { category ->
            CustomerCategory(
                id = category.getInt("id"),
                name = category.getString("name"),
                iconKey = category.optString("icon_path").takeIf { it.isNotBlank() }
                    ?.substringAfterLast('/')?.substringBeforeLast('.'),
                problemTypes = category.getJSONArray("problem_types").let { problems ->
                    (0 until problems.length()).map(problems::getJSONObject)
                }.map { problem ->
                    CustomerProblemType(
                        id = problem.getInt("id"),
                        name = problem.getString("name"),
                        isOther = problem.optBoolean("is_other"),
                    )
                },
            )
        }
        val optionPayload = config.getJSONObject("option_lists")
        val optionLists = optionPayload.keys().asSequence().associateWith { key ->
            val items = optionPayload.getJSONArray(key)
            (0 until items.length()).map { index ->
                val item = items.getJSONObject(index)
                CustomerOption(item.getString("code"), item.getString("label"))
            }
        }
        return CustomerHomePayload(
            // The app vocabulary is marketplace/staff; the server sends MARKETPLACE/EMPLOYEE (39).
            if (config.getString("operating_mode") == "MARKETPLACE") "marketplace" else "staff",
            orders.length(),
            orders.optJSONObject(0)?.optInt("id")?.takeIf { it > 0 },
            addresses.firstId(preferDefault = true),
            categoryItems,
            optionLists,
            config.getJSONObject("option_defaults").keys().asSequence().associateWith {
                config.getJSONObject("option_defaults").getString(it)
            },
            config.getJSONObject("service_hours").getString("from"),
            config.getJSONObject("service_hours").getString("to"),
            customerName = user.optString("name"),
            addressLabel = address?.let(::addressLabel).orEmpty(),
            addressCityId = cityId,
            orders = (0 until orders.length()).map(orders::getJSONObject),
        )
    }

    override fun order(token: String, orderId: Int): JSONObject = request("orders/$orderId", token = token, method = "GET").getJSONObject("data")

    override fun tracking(token: String, orderId: Int): CustomerTrackingPayload {
        val payload = request("orders/$orderId/tracking", token = token, method = "GET")
        val location = payload.optJSONObject("last_location")

        return CustomerTrackingPayload(
            latitude = location?.optString("lat")?.toDoubleOrNull(),
            longitude = location?.optString("lng")?.toDoubleOrNull(),
            etaMinutes = payload.optInt("eta_minutes").takeIf { !payload.isNull("eta_minutes") },
            etaApproximate = payload.optBoolean("eta_approximate"),
        )
    }

    override fun orders(token: String, scope: String, page: Int): JSONObject =
        request("orders?scope=$scope&page=$page", token = token, method = "GET")

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

    override fun publish(token: String, body: JSONObject, idempotencyKey: String): JSONObject =
        request("orders", body, token, idempotencyKey = idempotencyKey).getJSONObject("data")

    override fun updateOrder(token: String, orderId: Int, body: JSONObject): JSONObject =
        request("orders/$orderId", body, token, method = "PATCH").getJSONObject("data")

    override fun acceptOffer(token: String, orderId: Int, offerId: Int, expectedVersion: Int, paymentMethod: String): JSONObject =
        request(
            "orders/$orderId/offers/$offerId/accept",
            JSONObject().put("expected_version", expectedVersion).put("payment_method", paymentMethod),
            token,
        ).getJSONObject("data")

    override fun cancelOrder(token: String, orderId: Int, reasonCode: String, note: String?, expectedVersion: Int): JSONObject =
        request(
            "orders/$orderId/cancel",
            JSONObject().put("reason_code", reasonCode).put("note", note).put("expected_version", expectedVersion), token,
        ).getJSONObject("data")

    override fun proposals(token: String, orderId: Int): JSONArray =
        request("orders/$orderId/proposals", token = token, method = "GET").getJSONArray("data")

    override fun decideProposal(token: String, orderId: Int, proposalId: Int, approve: Boolean, expectedVersion: Int): JSONObject =
        request(
            "orders/$orderId/proposals/$proposalId/decide",
            JSONObject().put("approve", approve).put("expected_version", expectedVersion), token,
        ).getJSONObject("data")

    override fun changePaymentMethod(token: String, orderId: Int, method: String, expectedVersion: Int): JSONObject =
        request(
            "orders/$orderId/payment-method",
            JSONObject().put("payment_method", method).put("expected_version", expectedVersion),
            token,
            method = "PATCH",
        ).getJSONObject("data")

    override fun createPayment(token: String, orderId: Int, channel: String, expectedVersion: Int): JSONObject =
        request(
            "orders/$orderId/payments",
            JSONObject().put("channel", channel).put("expected_version", expectedVersion),
            token,
        ).getJSONObject("data")

    override fun confirmCompletion(token: String, orderId: Int, expectedVersion: Int): JSONObject =
        request(
            "orders/$orderId/confirm-completion",
            JSONObject().put("expected_version", expectedVersion),
            token,
        ).getJSONObject("data")

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

    override fun republish(token: String, orderId: Int): JSONObject =
        request("orders/$orderId/republish", JSONObject(), token).getJSONObject("data")

    private fun request(
        path: String,
        body: JSONObject? = null,
        token: String? = null,
        method: String = "POST",
        idempotencyKey: String? = null,
    ): JSONObject {
        val connection = URL(baseUrl.trimEnd('/') + "/" + path).openConnection() as HttpURLConnection
        connection.requestMethod = method
        connection.setRequestProperty("Accept", "application/json")
        connection.setRequestProperty("Content-Type", "application/json")
        connection.setRequestProperty("X-App-Mode", "CUSTOMER")
        token?.let { connection.setRequestProperty("Authorization", "Bearer $it") }
        idempotencyKey?.let { connection.setRequestProperty("Idempotency-Key", it) }
        if (method != "GET" && body != null) {
            connection.doOutput = true
            connection.outputStream.use { it.write(body.toString().toByteArray()) }
        }
        val status = connection.responseCode
        val stream = if (status in 200..299) connection.inputStream else connection.errorStream
        val text = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
        if (status !in 200..299) throw CustomerApiException(status, text)
        return if (text.isBlank()) JSONObject() else JSONObject(text)
    }

    private fun multipart(path: String, token: String, upload: CustomerMediaUpload): JSONObject {
        val boundary = "BzrBoundary${java.util.UUID.randomUUID()}"
        val connection = URL(baseUrl.trimEnd('/') + "/" + path).openConnection() as HttpURLConnection
        connection.requestMethod = "POST"
        connection.doOutput = true
        connection.setRequestProperty("Accept", "application/json")
        connection.setRequestProperty("Authorization", "Bearer $token")
        connection.setRequestProperty("X-App-Mode", "CUSTOMER")
        connection.setRequestProperty("Content-Type", "multipart/form-data; boundary=$boundary")
        val body = ByteArrayOutputStream().apply {
            write("--$boundary\r\n".toByteArray())
            write("Content-Disposition: form-data; name=\"file\"; filename=\"${upload.fileName}\"\r\n".toByteArray())
            write("Content-Type: ${upload.mimeType}\r\n\r\n".toByteArray())
            write(upload.bytes)
            write("\r\n--$boundary--\r\n".toByteArray())
        }.toByteArray()
        connection.outputStream.use { it.write(body) }
        val status = connection.responseCode
        val stream = if (status in 200..299) connection.inputStream else connection.errorStream
        val text = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
        if (status !in 200..299) throw CustomerApiException(status, text)
        return JSONObject(text)
    }
}

class CustomerApiException(val status: Int, message: String) : RuntimeException(message)

/** C04 address row: the saved label and area, as the C14 row shows them. */
internal fun addressLabel(address: JSONObject): String =
    listOf(address.optString("label"), address.optJSONObject("area")?.optString("name").orEmpty())
        .filter(String::isNotBlank).joinToString(" · ")

private fun JSONArray.firstId(preferDefault: Boolean): Int? {
    if (length() == 0) return null
    if (preferDefault) {
        (0 until length()).firstOrNull { optJSONObject(it)?.optBoolean("is_default") == true }
            ?.let { return getJSONObject(it).getInt("id") }
    }
    return getJSONObject(0).getInt("id")
}

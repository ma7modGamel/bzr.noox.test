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
    val categoryId: Int?,
    val problemTypeId: Int?,
    val optionLists: Map<String, List<CustomerOption>>,
    val optionDefaults: Map<String, String>,
)

data class CustomerOption(val code: String, val label: String)

interface CustomerApi {
    fun home(token: String): CustomerHomePayload
    fun order(token: String, orderId: Int): JSONObject
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
    fun account(token: String): JSONObject
    fun updateAccount(token: String, name: String, phone: String): JSONObject
    fun changePassword(token: String, currentPassword: String, password: String, confirmation: String)
    fun deleteAccount(token: String)
    fun terms(): JSONObject
    fun republish(token: String, orderId: Int): JSONObject
}

data class CustomerMediaUpload(val fileName: String, val mimeType: String, val bytes: ByteArray)

class UrlConnectionCustomerApi(private val baseUrl: String) : CustomerApi {
    override fun home(token: String): CustomerHomePayload {
        val config = request("config", method = "GET")
        val orders = request("orders?scope=current", token = token, method = "GET").getJSONArray("data")
        val addresses = request("addresses", token = token, method = "GET").getJSONArray("data")
        val categories = request("catalog", method = "GET").getJSONArray("data")
        val category = categories.optJSONObject(0)
        val optionPayload = config.getJSONObject("option_lists")
        val optionLists = optionPayload.keys().asSequence().associateWith { key ->
            val items = optionPayload.getJSONArray(key)
            (0 until items.length()).map { index ->
                val item = items.getJSONObject(index)
                CustomerOption(item.getString("code"), item.getString("label"))
            }
        }
        return CustomerHomePayload(
            config.getString("operating_mode").lowercase(),
            orders.length(),
            orders.optJSONObject(0)?.optInt("id")?.takeIf { it > 0 },
            addresses.firstId(preferDefault = true),
            category?.optInt("id")?.takeIf { it > 0 },
            category?.optJSONArray("problem_types")?.optJSONObject(0)?.optInt("id")?.takeIf { it > 0 },
            optionLists,
            config.getJSONObject("option_defaults").keys().asSequence().associateWith {
                config.getJSONObject("option_defaults").getString(it)
            },
        )
    }

    override fun order(token: String, orderId: Int): JSONObject = request("orders/$orderId", token = token, method = "GET").getJSONObject("data")

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

private fun JSONArray.firstId(preferDefault: Boolean): Int? {
    if (length() == 0) return null
    if (preferDefault) {
        (0 until length()).firstOrNull { optJSONObject(it)?.optBoolean("is_default") == true }
            ?.let { return getJSONObject(it).getInt("id") }
    }
    return getJSONObject(0).getInt("id")
}

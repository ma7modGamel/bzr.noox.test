package noox.bzr.provider

import noox.bzr.customer.CustomerMediaUpload
import noox.bzr.network.ApiResponse
import noox.bzr.network.BremoApiClient
import org.json.JSONArray
import org.json.JSONObject

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
)
data class ProviderConversationSummary(val id: Int, val orderId: Int, val customerName: String)
data class ProviderConversationPayload(
    val status: String,
    val customerName: String,
    val orderSummary: String,
    val bodies: List<String>,
    val kinds: List<String>,
    val times: List<String>,
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
)

interface ProviderApi {
    fun notifications(token: String): JSONObject
    fun markNotificationsRead(token: String, ids: List<String>)
    fun application(token: String): ProviderApplicationPayload
    fun submit(token: String, body: JSONObject): ProviderApplicationPayload
    fun catalog(): List<ProviderCategory>
    fun cities(): List<ProviderChoice>
    fun areas(cityId: Int): List<ProviderChoice>
    fun payoutOptions(): List<ProviderPayoutOption>
    fun uploadImage(token: String, upload: CustomerMediaUpload): Int
    fun home(token: String): ProviderHomePayload
    fun setAvailability(token: String, availableNow: Boolean): ProviderHomePayload
    fun availableRequests(token: String): List<ProviderOrderPayload>
    fun availableRequest(token: String, orderId: Int): ProviderMarketRequestPayload
    fun submitOffer(token: String, orderId: Int, body: JSONObject): ProviderOfferPayload
    fun offers(token: String): List<ProviderOfferPayload>
    fun withdrawOffer(token: String, offerId: Int): ProviderOfferPayload
    fun currentOrders(token: String): List<ProviderOrderPayload>
    fun pastOrders(token: String): List<ProviderOrderPayload>
    fun providerReasons(): List<ProviderReason>
    fun proposalTypes(): List<ProviderReason>
    fun performOrderAction(token: String, order: ProviderOrderPayload, path: String, body: JSONObject = JSONObject()): ProviderOrderPayload
    fun submitProposal(token: String, order: ProviderOrderPayload, body: JSONObject)
    fun rateCustomer(token: String, orderId: Int, stars: Int)
    fun earnings(token: String): ProviderEarningsPayload
    fun profile(token: String): ProviderProfilePayload
    fun updateProfile(token: String, body: JSONObject): ProviderProfilePayload
    fun addPortfolio(token: String, mediaId: Int, caption: String): ProviderPortfolioItem
    fun deletePortfolio(token: String, itemId: Int)
    fun updateAvatar(token: String, mediaId: Int)
    fun conversations(token: String): List<ProviderConversationSummary>
    fun conversation(token: String, conversationId: Int): ProviderConversationPayload
    fun sendMessage(token: String, conversationId: Int, body: String, mediaIds: List<Int>)
}

class UrlConnectionProviderApi(private val baseUrl: String) : ProviderApi {
    // C32 بوضع الفني: الخادم يصفّي بـX-App-Mode (DEC-058).
    override fun notifications(token: String): JSONObject = request("notifications", token = token, method = "GET")

    override fun markNotificationsRead(token: String, ids: List<String>) {
        request("notifications/read", JSONObject().put("notification_ids", JSONArray(ids)), token)
    }

    override fun application(token: String): ProviderApplicationPayload =
        request("provider/application", token = token, method = "GET").getJSONObject("data").application()

    override fun submit(token: String, body: JSONObject): ProviderApplicationPayload =
        request("provider/application", body, token).getJSONObject("data").application()

    override fun catalog(): List<ProviderCategory> = request("catalog", method = "GET").getJSONArray("data").objects().map { category ->
        ProviderCategory(
            category.getInt("id"),
            category.getString("name"),
            category.optJSONArray("problem_types")?.objects().orEmpty().map { ProviderChoice(it.getInt("id"), it.getString("name")) },
        )
    }

    override fun cities(): List<ProviderChoice> = request("cities", method = "GET").getJSONArray("data").choices()

    override fun areas(cityId: Int): List<ProviderChoice> =
        request("cities/$cityId/areas", method = "GET").getJSONArray("data").choices()

    override fun payoutOptions(): List<ProviderPayoutOption> {
        val options = request("config", method = "GET").getJSONObject("option_lists").getJSONArray("provider_payout_methods")
        return options.objects().map {
            ProviderPayoutOption(it.getString("code"), it.getString("label"), it.getString("field_label"))
        }
    }

    override fun uploadImage(token: String, upload: CustomerMediaUpload): Int =
        multipart("media", token, upload).getJSONObject("media").getInt("id")

    override fun home(token: String): ProviderHomePayload =
        request("provider/home", token = token, method = "GET").getJSONObject("data").home()

    override fun setAvailability(token: String, availableNow: Boolean): ProviderHomePayload =
        request(
            "provider/availability",
            JSONObject().put("available_now", availableNow),
            token,
            method = "PUT",
        ).getJSONObject("data").home()

    override fun availableRequests(token: String): List<ProviderOrderPayload> =
        request("provider/requests", token = token, method = "GET")
            .getJSONArray("data").objects().map(JSONObject::providerOrder)

    override fun availableRequest(token: String, orderId: Int): ProviderMarketRequestPayload =
        request("provider/requests/$orderId", token = token, method = "GET")
            .getJSONObject("data").marketRequest()

    override fun submitOffer(token: String, orderId: Int, body: JSONObject): ProviderOfferPayload =
        request("provider/requests/$orderId/offers", body, token).getJSONObject("data").providerOffer()

    override fun offers(token: String): List<ProviderOfferPayload> =
        request("provider/offers", token = token, method = "GET")
            .getJSONArray("data").objects().map(JSONObject::providerOffer)

    override fun withdrawOffer(token: String, offerId: Int): ProviderOfferPayload =
        request("provider/offers/$offerId/withdraw", JSONObject(), token).getJSONObject("data").providerOffer()

    override fun currentOrders(token: String): List<ProviderOrderPayload> =
        request("provider/orders?scope=current", token = token, method = "GET")
            .getJSONArray("data").objects().map(JSONObject::providerOrder)

    override fun pastOrders(token: String): List<ProviderOrderPayload> =
        request("provider/orders?scope=past", token = token, method = "GET")
            .getJSONArray("data").objects().map(JSONObject::providerOrder)

    override fun providerReasons(): List<ProviderReason> =
        request("config", method = "GET").getJSONObject("option_lists")
            .getJSONArray("provider_cancellation_reasons").objects()
            .map { ProviderReason(it.getString("code"), it.getString("label")) }

    override fun proposalTypes(): List<ProviderReason> =
        request("config", method = "GET").getJSONObject("option_lists")
            .getJSONArray("proposal_types").objects()
            .map { ProviderReason(it.getString("code"), it.getString("label")) }

    override fun performOrderAction(
        token: String,
        order: ProviderOrderPayload,
        path: String,
        body: JSONObject,
    ): ProviderOrderPayload = request(
        "provider/orders/${order.id}/$path",
        body.put("expected_version", order.version),
        token,
    ).getJSONObject("data").providerOrder()

    override fun submitProposal(token: String, order: ProviderOrderPayload, body: JSONObject) {
        request(
            "provider/orders/${order.id}/proposals",
            body.put("expected_version", order.version),
            token,
        )
    }

    override fun rateCustomer(token: String, orderId: Int, stars: Int) {
        request("provider/orders/$orderId/customer-rating", JSONObject().put("stars", stars), token)
    }

    override fun earnings(token: String): ProviderEarningsPayload =
        request("provider/earnings", token = token, method = "GET").getJSONObject("data").earnings()

    override fun profile(token: String): ProviderProfilePayload =
        request("provider/profile", token = token, method = "GET").getJSONObject("data").providerProfile()

    override fun updateProfile(token: String, body: JSONObject): ProviderProfilePayload =
        request("provider/profile", body, token, method = "PATCH").getJSONObject("data").providerProfile()

    override fun addPortfolio(token: String, mediaId: Int, caption: String): ProviderPortfolioItem =
        request(
            "provider/portfolio",
            JSONObject().put("media_id", mediaId).put("caption", caption.ifBlank { JSONObject.NULL }),
            token,
        ).getJSONObject("data").portfolioItem()

    override fun deletePortfolio(token: String, itemId: Int) {
        request("provider/portfolio/$itemId", token = token, method = "DELETE")
    }

    override fun updateAvatar(token: String, mediaId: Int) {
        request("me", JSONObject().put("avatar_media_id", mediaId), token, method = "PATCH")
    }

    override fun conversations(token: String): List<ProviderConversationSummary> =
        request("conversations", token = token, method = "GET").getJSONArray("data").objects().map {
            ProviderConversationSummary(
                it.getInt("id"), it.getJSONObject("order").getInt("id"),
                it.optJSONObject("customer")?.optString("name").orEmpty(),
            )
        }

    override fun conversation(token: String, conversationId: Int): ProviderConversationPayload =
        request("conversations/$conversationId/messages", token = token, method = "GET").conversation()

    override fun sendMessage(token: String, conversationId: Int, body: String, mediaIds: List<Int>) {
        request(
            "conversations/$conversationId/messages",
            JSONObject().put("body", body.ifBlank { JSONObject.NULL }).put("media_ids", JSONArray(mediaIds)),
            token,
        )
    }

    private val client = BremoApiClient(baseUrl, appMode = "PROVIDER")

    private fun request(path: String, body: JSONObject? = null, token: String? = null, method: String = "POST"): JSONObject =
        client.send(path, method, body, token).checked()

    private fun multipart(path: String, token: String, upload: CustomerMediaUpload): JSONObject =
        client.upload(path, token, upload.fileName, upload.mimeType, upload.bytes).checked()
}

class ProviderApiException(val status: Int, val code: String, val fields: Map<String, List<String>>) : RuntimeException(code)

private fun ApiResponse.checked(): JSONObject {
    val payload = if (text.isBlank()) JSONObject() else JSONObject(text)
    if (!isSuccessful) {
        val error = payload.optJSONObject("error")
        val fields = error?.optJSONObject("fields")?.let { objectValue ->
            objectValue.keys().asSequence().associateWith { key -> objectValue.getJSONArray(key).strings() }
        }.orEmpty()
        throw ProviderApiException(status, error?.optString("code").orEmpty().ifBlank { "HTTP_$status" }, fields)
    }
    return payload
}

private fun JSONObject.application(): ProviderApplicationPayload {
    val documents = optJSONObject("documents")
    return ProviderApplicationPayload(
        status = getString("status"),
        displayStatus = optString("display_status"),
        reason = optString("rejection_reason").ifBlank { optString("suspension_reason") }.ifBlank { null },
        experienceYears = optInt("experience_years").takeIf { has("experience_years") && !isNull("experience_years") },
        bio = optString("bio"),
        categoryIds = optJSONArray("category_ids")?.ints().orEmpty(),
        specialtyIds = optJSONArray("specialty_ids")?.ints().orEmpty(),
        areaIds = optJSONArray("area_ids")?.ints().orEmpty(),
        payoutMethod = optString("payout_method"),
        payoutDetails = optString("payout_details"),
        profilePhotoUploaded = optBoolean("profile_photo_uploaded"),
        idFrontUploaded = documents?.optBoolean("id_front") == true,
        idBackUploaded = documents?.optBoolean("id_back") == true,
        availableActions = getJSONArray("available_actions").strings(),
    )
}

private fun JSONObject.home(): ProviderHomePayload {
    val order = optJSONObject("active_order")
    return ProviderHomePayload(
        operatingMode = getString("operating_mode"),
        availableNow = getBoolean("available_now"),
        activeOrder = order?.let {
            val timing = it.optJSONObject("timing")
            ProviderHomeOrder(
                id = it.getInt("id"),
                number = it.get("number").toString(),
                category = it.optJSONObject("category")?.optString("name").orEmpty(),
                problemType = it.optJSONObject("problem_type")?.optString("name").orEmpty(),
                area = it.optJSONObject("location")?.optString("area").orEmpty(),
                timingType = timing?.optString("type").orEmpty(),
                slotStart = timing?.optString("slot_start")?.ifBlank { null },
                displayStatus = it.optString("display_status"),
            )
        },
        availableActions = getJSONArray("available_actions").strings(),
        unreadNotifications = optInt("unread_notifications"),
    )
}

private fun JSONObject.providerOrder(): ProviderOrderPayload {
    val location = optJSONObject("location") ?: JSONObject()
    val timing = optJSONObject("timing") ?: JSONObject()
    val customer = optJSONObject("customer") ?: JSONObject()
    val amounts = optJSONObject("amounts") ?: JSONObject()
    val priceGuide = optJSONObject("execution_price_guide")
    return ProviderOrderPayload(
        id = getInt("id"),
        number = get("number").toString(),
        version = getInt("version"),
        status = getString("status"),
        displayStatus = optString("display_status"),
        category = optJSONObject("category")?.optString("name").orEmpty(),
        problemType = optJSONObject("problem_type")?.optString("name").orEmpty(),
        address = listOf(
            location.optString("address_text"), location.optString("building"),
            location.optString("floor"), location.optString("apartment"), location.optString("landmark"),
        ).filter(String::isNotBlank).joinToString(", "),
        area = location.optString("area"),
        latitude = location.optDouble("lat").takeUnless(Double::isNaN),
        longitude = location.optDouble("lng").takeUnless(Double::isNaN),
        timingType = timing.optString("type"),
        slotStart = timing.optString("slot_start").ifBlank { null },
        customerName = customer.optString("name"),
        customerPhone = customer.optString("phone").ifBlank { null },
        customerRating = customer.optString("rating_avg"),
        stepStates = optJSONArray("stepper")?.objects().orEmpty().map { it.optString("state") },
        availableActions = optJSONArray("available_actions")?.strings().orEmpty(),
        laborTotal = amounts.optString("labor_total", "0.00"),
        materialsTotal = amounts.optString("materials_total", "0.00"),
        finalAmount = amounts.optString("final_amount", "0.00"),
        paymentMethod = amounts.optString("payment_method").ifBlank { null },
        paymentMethodLabel = amounts.optString("payment_method_label"),
        priceGuideMinimum = priceGuide?.optString("minimum")?.ifBlank { null },
        priceGuideMaximum = priceGuide?.optString("maximum")?.ifBlank { null },
        priceGuideReviewRequired = optBoolean("price_guide_review_required"),
        conversationId = optInt("conversation_id").takeIf { has("conversation_id") && !isNull("conversation_id") },
        description = optString("description"),
        pricingMode = optString("pricing_mode"),
        pricingModeLabel = optString("pricing_mode_label"),
        materialsResponsibility = optString("materials_responsibility"),
        materialsResponsibilityLabel = optString("materials_responsibility_label"),
        budgetAmount = optString("budget_amount").ifBlank { null },
        offersCloseAt = optJSONObject("deadlines")?.optString("offers_close_at")?.ifBlank { null },
    )
}

private fun JSONObject.marketRequest(): ProviderMarketRequestPayload {
    val order = getJSONObject("order").providerOrder()
    val own = optJSONObject("own_offer")
    val context = getJSONObject("offer_context")
    val eta = context.getJSONArray("eta_options").strings()
    val deductible = context.getJSONArray("deductible_options").objects()
    return ProviderMarketRequestPayload(
        order = order,
        mediaLabels = getJSONArray("media").objects().mapIndexed { index, media ->
            "${media.getString("type")}:${index + 1}"
        },
        ownOfferId = own?.optInt("id")?.takeIf { it > 0 },
        ownOfferSummary = own?.let {
            listOf(
                it.getString("price"),
                it.getString("net_amount"),
                it.getString("display_status"),
            )
        }.orEmpty(),
        context = ProviderOfferContext(
            minimumAmount = context.getString("min_amount"),
            commissionRate = context.getString("commission_rate"),
            etaCodes = eta,
            etaLabels = eta,
            deductibleCodes = deductible.map { it.getString("code") },
            deductibleLabels = deductible.map { it.getString("label") },
            defaultIncludes = context.getString("default_includes_text"),
        ),
    )
}

private fun JSONObject.providerOffer(): ProviderOfferPayload {
    val order = getJSONObject("order")
    val net = getString("net_amount")
    val price = getString("price")
    return ProviderOfferPayload(
        id = getInt("id"),
        orderId = order.getInt("id"),
        orderVersion = order.getInt("version"),
        title = "${order.get("number")}|${order.optString("category")}",
        detail = "${order.optString("area")}|$price|$net",
        status = getString("status"),
        displayStatus = getString("display_status"),
        availableActions = getJSONArray("available_actions").strings(),
    )
}

private fun JSONObject.earnings(): ProviderEarningsPayload {
    val summary = getJSONObject("summary")
    val transactions = getJSONArray("transactions").objects()
    return ProviderEarningsPayload(
        getString("operating_mode"),
        listOf(summary.getString("balance"), summary.getString("available"), summary.getString("pending")),
        transactions.map { it.getString("title") },
        transactions.map { it.getString("detail") },
        transactions.map { it.getString("type") },
    )
}

private fun JSONObject.providerProfile(): ProviderProfilePayload {
    val categories = getJSONArray("categories").objects().map { category ->
        ProviderCategory(
            category.getInt("id"), category.getString("name"),
            category.getJSONArray("specialties").objects().map { ProviderChoice(it.getInt("id"), it.getString("name")) },
        )
    }
    return ProviderProfilePayload(
        name = getString("name"), rating = optString("rating_avg"),
        completedOrders = getInt("completed_orders"),
        averageResponseMinutes = optInt("avg_response_minutes").takeIf { !isNull("avg_response_minutes") },
        hasAvatar = optString("avatar_path").isNotBlank(),
        experienceYears = getInt("experience_years"),
        bio = optString("bio"), categories = categories, specialtyIds = getJSONArray("specialty_ids").ints(),
        areas = getJSONArray("areas").objects().map { ProviderChoice(it.getInt("id"), it.getString("name")) },
        areaIds = getJSONArray("area_ids").ints(),
        portfolio = getJSONArray("portfolio").objects().map(JSONObject::portfolioItem),
        availableActions = getJSONArray("available_actions").strings(),
    )
}

private fun JSONObject.portfolioItem() = ProviderPortfolioItem(getInt("id"), optString("caption"))

private fun JSONObject.conversation(): ProviderConversationPayload {
    val conversation = getJSONObject("conversation")
    val order = conversation.getJSONObject("order")
    val messages = getJSONArray("data").objects()
    return ProviderConversationPayload(
        status = conversation.getString("status"),
        customerName = conversation.optJSONObject("customer")?.optString("name").orEmpty(),
        orderSummary = listOf("#${order.get("number")}", order.optString("category"), order.optString("status_label"))
            .filter(String::isNotBlank).joinToString(" · "),
        bodies = messages.map { it.optString("body") },
        kinds = messages.map { message ->
            val side = if (message.getBoolean("is_mine")) "sent" else "received"
            val kind = when {
                message.getBoolean("was_masked") -> "blocked"
                (message.optJSONArray("media")?.length() ?: 0) > 0 -> "image"
                else -> "text"
            }
            "${side}_$kind"
        },
        times = messages.map { it.optString("created_at") },
    )
}

private fun JSONArray.objects(): List<JSONObject> = (0 until length()).map(::getJSONObject)
private fun JSONArray.strings(): List<String> = (0 until length()).map(::getString)
private fun JSONArray.ints(): List<Int> = (0 until length()).map(::getInt)
private fun JSONArray.choices(): List<ProviderChoice> = objects().map { ProviderChoice(it.getInt("id"), it.getString("name")) }

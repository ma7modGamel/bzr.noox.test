package noox.bzr.customer

import android.os.Handler
import android.os.Looper
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import noox.bzr.auth.AuthSessionStore
import org.json.JSONArray
import org.json.JSONObject
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter

class CustomerViewModel(
    private val api: CustomerApi,
    private val session: AuthSessionStore,
    private val currencyLabel: String,
) : ViewModel() {
    var state by mutableStateOf(CustomerUiState("SCR-C01", CustomerPhase.Loading))
        private set
    var requiresAuthentication by mutableStateOf(false)
        private set
    private var currentOrderId: Int? = null
    private var currentOrderVersion: Int = 0
    private var currentProposalId: Int? = null
    private var currentOrderPayload: JSONObject? = null
    private var selectedPaymentChannel = -1
    private var ratingValues = mutableListOf(0, 0, 0)
    private var ratingComment = ""
    private var cancellationReasonIndex: Int = -1
    private var cancellationNote: String = ""
    private var optionLists: Map<String, List<CustomerOption>> = emptyMap()
    private var optionDefaults: Map<String, String> = emptyMap()
    private var currentProviderId: Int? = null
    private var currentProviderActions: List<String> = emptyList()
    private var supportReasonIndex: Int = -1
    private var supportDescription: String = ""
    private val disputeMedia = mutableListOf<MediaDraft>()
    private var addressId: Int? = null
    private var categoryId: Int? = null
    private var problemTypeId: Int? = null
    private var cityId: Int? = null
    private var addressIds: List<Int> = emptyList()
    private var addressPayloads: List<JSONObject> = emptyList()
    private var areaIds: List<Int> = emptyList()
    private var selectedAddressIndex: Int = -1
    private var addressSelectionMode: Boolean = true
    private var dayDates: List<String> = emptyList()
    private var slotStarts: List<String> = emptyList()
    private var selectedSlotIndex: Int = -1
    private var timingType: String = ""
    private var slotStart: String? = null
    private val media = mutableListOf<MediaDraft>()
    private var addressDraft = AddressDraft()
    private val mainHandler = Handler(Looper.getMainLooper())
    private var chatPoll: Runnable? = null
    private var conversationIds: List<Int> = emptyList()
    private var currentConversationId: Int? = null
    private var currentConversationPayload: JSONObject? = null
    private var chatMessages: List<JSONObject> = emptyList()
    private var chatDraft = ""
    private var orderIds: List<Int> = emptyList()
    private var orderStatuses: List<String> = emptyList()
    private var ordersTab = "current"
    private var faqQuestions: List<String> = emptyList()
    private var faqAnswers: List<String> = emptyList()
    private var expandedFaqIndex = -1
    private var helpSubject = ""
    private var helpMessage = ""
    private var notificationIds: List<String> = emptyList()
    private var notificationLinks: List<String> = emptyList()
    private var accountValues: MutableList<String> = mutableListOf("", "", "")
    private var accountActions: List<String> = emptyList()
    private var accountMode = "overview"
    private var editingOrderId: Int? = null

    fun loadHome() = request("SCR-C01") {
        val home = api.home(token())
        currentOrderId = home.firstOrderId
        addressId = home.addressId
        categoryId = home.categoryId
        problemTypeId = home.problemTypeId
        optionLists = home.optionLists
        optionDefaults = home.optionDefaults
        timingType = optionDefaults["timing_type"].orEmpty()
        mapOf("event" to "loaded", "operating_mode" to home.operatingMode, "has_orders" to (home.orderCount > 0))
    }

    fun show(screen: String, input: Map<String, Any?>) {
        if (screen != "SCR-C19") stopChatPolling()
        state = CustomerLogic.reduce(screen, input)
    }

    fun logout() {
        session.clear()
        requiresAuthentication = true
    }

    fun loadHelp() = request("SCR-C29") {
        val faqs = api.faqs().objects()
        faqQuestions = faqs.map { it.getString("title") }
        faqAnswers = faqs.map { it.getString("body") }
        helpInput("loaded")
    }

    fun toggleFaq(index: Int) {
        expandedFaqIndex = if (expandedFaqIndex == index) -1 else index
        state = CustomerLogic.reduce("SCR-C29", helpInput("loaded"))
    }

    fun updateHelpField(index: Int, value: String) {
        if (index == 0) helpSubject = value.take(120) else helpMessage = value.take(2000)
        state = CustomerLogic.reduce("SCR-C29", helpInput("loaded"))
    }

    fun submitHelpMessage() {
        if (!state.canContinue) return
        state = CustomerLogic.reduce("SCR-C29", helpInput("submitting"))
        Thread {
            val success = runCatching { api.sendSupportMessage(token(), helpSubject.trim(), helpMessage.trim()) }.isSuccess
            mainHandler.post { state = CustomerLogic.reduce("SCR-C29", helpInput(if (success) "success" else "error")) }
        }.start()
    }

    fun loadNotifications() = request("SCR-C32") {
        val values = api.notifications(token()).getJSONArray("data").objects()
        notificationIds = values.map { it.getString("id") }
        notificationLinks = values.map { it.optNullableString("deep_link") }
        notificationInput(values, "loaded")
    }

    fun openNotification(index: Int) {
        val id = notificationIds.getOrNull(index) ?: return
        state = state.copy(isBusy = true)
        Thread {
            runCatching { api.markNotificationsRead(token(), listOf(id)) }
            val orderId = Regex("orders/(\\d+)").find(notificationLinks.getOrNull(index).orEmpty())
                ?.groupValues?.getOrNull(1)?.toIntOrNull()
            mainHandler.post { if (orderId != null) loadOrder(orderId) else loadNotifications() }
        }.start()
    }

    fun loadAccount() = request("SCR-C33") {
        val user = api.account(token())
        accountValues = mutableListOf(user.optString("name"), user.optString("email"), user.optString("phone"))
        accountActions = user.optJSONArray("available_actions")?.strings().orEmpty()
        accountMode = "overview"
        accountInput("loaded")
    }

    fun accountAction(action: String) {
        when (action) {
            "update_account" -> if (accountMode == "edit") submitAccountUpdate() else {
                accountMode = "edit"
                state = CustomerLogic.reduce("SCR-C33", accountInput("loaded"))
            }
            "change_password" -> if (accountMode == "password") submitPasswordChange() else {
                accountMode = "password"
                accountValues = mutableListOf("", "", "")
                state = CustomerLogic.reduce("SCR-C33", accountInput("loaded"))
            }
            "delete_account" -> state = CustomerLogic.reduce("SCR-C33", accountInput("confirm_delete", "delete"))
            "dismiss_delete" -> state = CustomerLogic.reduce("SCR-C33", accountInput("loaded", "overview"))
            "confirm_delete_account" -> submitDeleteAccount()
            "open_terms" -> loadTerms()
            "logout" -> logout()
        }
    }

    fun updateAccountField(index: Int, value: String) {
        if (index !in accountValues.indices) return
        accountValues[index] = value
        state = CustomerLogic.reduce("SCR-C33", accountInput("loaded"))
    }

    fun loadTerms() = request("SCR-C34") {
        val terms = api.terms()
        mapOf(
            "event" to "loaded", "body" to terms.optString("body"),
            "version" to terms.optString("version"), "effective_at" to terms.optString("effective_at").displayDateTime(),
        )
    }

    fun openNoOffers(actions: List<String>) {
        state = CustomerLogic.reduce("SCR-C35", mapOf("event" to "loaded", "available_actions" to actions))
    }

    fun noOffersAction(action: String) {
        when (action) {
            "edit_request" -> {
                val order = currentOrderPayload ?: return
                editingOrderId = currentOrderId
                addressId = order.optInt("customer_address_id").takeIf { it > 0 }
                categoryId = order.optJSONObject("category")?.optInt("id")
                problemTypeId = order.optJSONObject("problem_type")?.optInt("id")
                timingType = order.optJSONObject("timing")?.optString("type").orEmpty()
                slotStart = order.optJSONObject("timing")?.optNullableString("slot_start")
                show(
                    "SCR-C03",
                    mapOf(
                        "event" to "validate", "problem_type_id" to problemTypeId?.toString().orEmpty(),
                        "description" to order.optNullableString("description"),
                    ),
                )
            }
            "republish" -> {
                val orderId = currentOrderId ?: return
                state = CustomerLogic.reduce("SCR-C35", mapOf("event" to "submitting", "available_actions" to state.visibleActions))
                Thread {
                    val order = runCatching { api.republish(token(), orderId) }.getOrNull()
                    mainHandler.post {
                        if (order == null) state = CustomerLogic.reduce("SCR-C35", mapOf("event" to "error"))
                        else {
                            currentOrderId = order.getInt("id")
                            currentOrderPayload = order
                            loadOrder(currentOrderId!!)
                        }
                    }
                }.start()
            }
        }
    }

    fun loadConversations(page: Int = 1) = request("SCR-C18") {
        val response = api.conversations(token(), page)
        val conversations = response.getJSONArray("data").objects()
        conversationIds = conversations.map { it.getInt("id") }
        mapOf(
            "event" to "loaded",
            "provider_names" to conversations.map { it.getJSONObject("provider").optString("name") },
            "order_summaries" to conversations.map {
                val order = it.getJSONObject("order")
                "${order.optString("category")} #${order.optString("number")} · ${order.optString("status_label")}".trim()
            },
            "conversation_statuses" to conversations.map { it.getString("status") },
            "last_messages" to conversations.map { conversation ->
                conversation.optJSONObject("last_message")?.optString("body").orEmpty()
            },
            "last_message_times" to conversations.map { conversation ->
                conversation.optJSONObject("last_message")?.optString("created_at")?.displayDateTime().orEmpty()
            },
            "provider_verified" to conversations.map {
                if (it.getJSONObject("provider").optBoolean("is_verified")) "verified" else ""
            },
        )
    }

    fun openConversation(index: Int) {
        val id = conversationIds.getOrNull(index) ?: return
        currentConversationId = id
        chatDraft = ""
        loadConversation(id, showLoading = true)
    }

    fun updateChatDraft(value: String) {
        chatDraft = value.take(1000)
        state = CustomerLogic.reduce("SCR-C19", chatInput("loaded"))
    }

    fun sendChatMessage() {
        val id = currentConversationId ?: return
        val body = chatDraft.trim()
        if (body.isBlank() || !state.canContinue) return
        state = CustomerLogic.reduce("SCR-C19", chatInput("sending"))
        Thread {
            val success = runCatching { api.sendMessage(token(), id, body) }.isSuccess
            mainHandler.post {
                if (success) {
                    chatDraft = ""
                    loadConversation(id, showLoading = false)
                } else {
                    state = CustomerLogic.reduce("SCR-C19", mapOf("event" to "error"))
                }
            }
        }.start()
    }

    fun sendChatImage(upload: CustomerMediaUpload) {
        val id = currentConversationId ?: return
        state = CustomerLogic.reduce("SCR-C19", chatInput("sending"))
        Thread {
            val success = runCatching {
                val mediaId = api.uploadMedia(token(), upload).getInt("id")
                api.sendMessage(token(), id, "", listOf(mediaId))
            }.isSuccess
            mainHandler.post {
                if (success) loadConversation(id, showLoading = false)
                else state = CustomerLogic.reduce("SCR-C19", mapOf("event" to "error"))
            }
        }.start()
    }

    fun loadOrders(tabIndex: Int = 0, page: Int = 1) = request("SCR-C25") {
        ordersTab = if (tabIndex == 1) "past" else "current"
        val response = api.orders(token(), ordersTab, page)
        val orders = response.getJSONArray("data").objects()
        orderIds = orders.map { it.getInt("id") }
        orderStatuses = orders.map { it.getString("status") }
        mapOf(
            "event" to "loaded", "tab" to ordersTab,
            "order_titles" to orders.map {
                "${it.getJSONObject("category").optString("name")} — ${it.getJSONObject("problem_type").optString("name")}" 
            },
            "order_subtitles" to orders.map {
                "#${it.optString("number")} · ${it.getJSONObject("location").optString("area")} · ${it.optString("created_at").displayDateTime()}"
            },
            "order_statuses" to orders.map { it.getString("status_label") },
        )
    }

    fun selectOrder(index: Int) {
        val id = orderIds.getOrNull(index) ?: return
        if (orderStatuses.getOrNull(index) in setOf("CLOSED", "CANCELLED", "EXPIRED")) loadOrderHistory(id) else loadOrder(id)
    }

    fun loadOrderHistory(orderId: Int) = request("SCR-C26") {
        val order = api.order(token(), orderId)
        val proposals = api.proposals(token(), orderId).objects().filter { it.optString("status") == "APPROVED" }
        currentOrderId = orderId
        currentOrderPayload = order
        currentOrderVersion = order.optInt("version")
        orderHistoryInput(order, proposals)
    }

    fun loadAddresses(selectionMode: Boolean = true) = request("SCR-C14") {
        addressSelectionMode = selectionMode
        val addresses = api.addresses(token())
        addressPayloads = addresses.objects()
        addressIds = addressPayloads.map { it.getInt("id") }
        selectedAddressIndex = addressPayloads.indexOfFirst { it.optBoolean("is_default") }
        addressPayloads.firstOrNull { it.optBoolean("is_default") }?.let {
            cityId = it.getJSONObject("city").getInt("id")
            addressId = it.getInt("id")
        }
        mapOf(
            "event" to "loaded", "selection_mode" to selectionMode,
            "address_labels" to addressPayloads.map { it.getString("label") },
            "address_details" to addressPayloads.map {
                "${it.getJSONObject("area").getString("name")} · ${it.getString("address_text")}"
            },
            "selected_index" to selectedAddressIndex,
        )
    }

    fun selectAddress(index: Int) {
        selectedAddressIndex = index
        addressId = addressIds.getOrNull(index)
        state = state.copy(selectedIndex = index, canContinue = addressId != null)
    }

    fun confirmAddress(index: Int) {
        selectAddress(index)
        state = CustomerLogic.reduce(
            "SCR-C04",
            mapOf(
                "event" to "validate", "operating_mode" to "marketplace",
                "timing_type" to timingType.lowercase(), "now_available" to true,
                "timing_labels" to options("timing_types").map(CustomerOption::label),
                "selected_timing_index" to options("timing_types").indexOfFirst { it.code == timingType },
                "address_id" to (addressId?.toString() ?: ""),
                "slot_id" to if (slotStart == null) "" else "selected",
            ),
        )
    }

    fun openNewAddress() = request("SCR-C15") {
        val cities = api.cities()
        val selectedCity = cities.optJSONObject(0) ?: error("No served cities")
        cityId = selectedCity.getInt("id")
        val areas = api.areas(selectedCity.getInt("id"))
        areaIds = areas.objects().map { it.getInt("id") }
        addressDraft = AddressDraft(cityId = selectedCity.getInt("id"))
        addressInput("validate", areas.objects().map { it.getString("name") })
    }

    fun openEditAddress(index: Int) = request("SCR-C15") {
        val address = addressPayloads.getOrNull(index) ?: error("Address not found")
        val selectedCityId = address.getJSONObject("city").getInt("id")
        val selectedAreaId = address.getJSONObject("area").getInt("id")
        val areas = api.areas(selectedCityId).objects()
        cityId = selectedCityId
        areaIds = areas.map { it.getInt("id") }
        addressDraft = AddressDraft(
            id = address.getInt("id"), cityId = selectedCityId, areaId = selectedAreaId,
            selectedAreaIndex = areaIds.indexOf(selectedAreaId), label = address.getString("label"),
            addressText = address.getString("address_text"), building = address.optNullableString("building"),
            floor = address.optNullableString("floor"), apartment = address.optNullableString("apartment"),
            landmark = address.optNullableString("landmark"), latitude = address.getString("lat").toDouble(),
            longitude = address.getString("lng").toDouble(), isDefault = address.optBoolean("is_default"),
        )
        addressInput("validate", areas.map { it.getString("name") })
    }

    fun updateAddressField(field: String, value: String) {
        addressDraft = when (field) {
            "label" -> addressDraft.copy(label = value)
            "address_text" -> addressDraft.copy(addressText = value)
            "building" -> addressDraft.copy(building = value)
            "floor" -> addressDraft.copy(floor = value)
            "apartment" -> addressDraft.copy(apartment = value)
            "landmark" -> addressDraft.copy(landmark = value)
            else -> addressDraft
        }
        state = CustomerLogic.reduce("SCR-C15", addressInput("validate", state.options))
    }

    fun selectArea(index: Int) {
        addressDraft = addressDraft.copy(areaId = areaIds.getOrNull(index), selectedAreaIndex = index)
        state = CustomerLogic.reduce("SCR-C15", addressInput("validate", state.options))
    }

    fun updateAddressLocation(latitude: Double, longitude: Double) {
        addressDraft = addressDraft.copy(latitude = latitude, longitude = longitude)
        state = CustomerLogic.reduce("SCR-C15", addressInput("validate", state.options))
    }

    fun toggleAddressDefault() {
        addressDraft = addressDraft.copy(isDefault = !addressDraft.isDefault)
        state = CustomerLogic.reduce("SCR-C15", addressInput("validate", state.options))
    }

    fun saveAddress() {
        if (!state.canContinue) return
        state = CustomerLogic.reduce("SCR-C15", addressInput("saving", state.options))
        Thread {
            val draft = addressDraft
            val saved = runCatching {
                api.saveAddress(
                    token(), draft.id,
                    JSONObject()
                        .put("label", draft.label).put("city_id", draft.cityId).put("area_id", draft.areaId)
                        .put("address_text", draft.addressText).put("building", draft.building)
                        .put("floor", draft.floor).put("apartment", draft.apartment).put("landmark", draft.landmark)
                        .put("lat", draft.latitude).put("lng", draft.longitude).put("is_default", draft.isDefault),
                )
            }.isSuccess
            Handler(Looper.getMainLooper()).post {
                if (saved) loadAddresses(addressSelectionMode) else state = CustomerLogic.reduce("SCR-C15", mapOf("event" to "error"))
            }
        }.start()
    }

    fun requestDeleteAddress(index: Int) {
        if (addressIds.getOrNull(index) == null) return
        state = CustomerLogic.reduce("SCR-C14", addressStateInput("confirm_delete", index))
    }

    fun cancelDeleteAddress() {
        state = CustomerLogic.reduce("SCR-C14", addressStateInput("loaded"))
    }

    fun confirmDeleteAddress() {
        val index = state.pendingIndex
        val id = addressIds.getOrNull(index) ?: return
        state = CustomerLogic.reduce("SCR-C14", addressStateInput("deleting", index))
        Thread {
            val next = runCatching {
                api.deleteAddress(token(), id)
                val addresses = api.addresses(token())
                addressPayloads = addresses.objects()
                addressIds = addressPayloads.map { it.getInt("id") }
                mapOf(
                    "event" to "loaded", "selection_mode" to addressSelectionMode,
                    "address_labels" to addressPayloads.map { it.getString("label") },
                    "address_details" to addressPayloads.map { "${it.getJSONObject("area").getString("name")} · ${it.getString("address_text")}" },
                    "selected_index" to addressPayloads.indexOfFirst { it.optBoolean("is_default") },
                )
            }.getOrElse { mapOf("event" to "error") }
            Handler(Looper.getMainLooper()).post { state = CustomerLogic.reduce("SCR-C14", next) }
        }.start()
    }

    private fun addressStateInput(event: String, pendingIndex: Int = -1): Map<String, Any?> = mapOf(
        "event" to event, "selection_mode" to addressSelectionMode,
        "address_labels" to addressPayloads.map { it.getString("label") },
        "address_details" to addressPayloads.map {
            "${it.getJSONObject("area").getString("name")} · ${it.getString("address_text")}"
        },
        "selected_index" to selectedAddressIndex, "pending_delete_index" to pendingIndex,
    )

    fun loadSlots(dayLabels: List<String>, morning: String, evening: String) {
        val activeCity = cityId ?: return show("SCR-C16", mapOf("event" to "error"))
        dayDates = (0..7).map { java.time.LocalDate.now().plusDays(it.toLong()).toString() }
        loadSlotsForDay(activeCity, 0, dayLabels, morning, evening)
    }

    fun selectDay(index: Int, dayLabels: List<String>, morning: String, evening: String) {
        cityId?.let { loadSlotsForDay(it, index, dayLabels, morning, evening) }
    }

    fun selectSlot(index: Int) {
        selectedSlotIndex = index
        state = state.copy(selectedIndex = index, canContinue = slotStarts.getOrNull(index) != null)
    }

    fun confirmSlot() {
        val selected = slotStarts.getOrNull(selectedSlotIndex) ?: return
        timingType = options("timing_types").getOrNull(1)?.code ?: return
        slotStart = selected
        state = CustomerLogic.reduce(
            "SCR-C04",
            mapOf(
                "event" to "validate", "operating_mode" to "marketplace",
                "timing_type" to "scheduled", "address_id" to (addressId?.toString() ?: ""),
                "timing_labels" to options("timing_types").map(CustomerOption::label),
                "selected_timing_index" to options("timing_types").indexOfFirst { it.code == timingType },
                "slot_id" to "selected",
            ),
        )
    }

    fun openMedia() {
        state = CustomerLogic.reduce("SCR-C17", mediaInput())
    }

    fun completeMediaSelection() {
        state = CustomerLogic.reduce(
            "SCR-C03",
            mapOf(
                "event" to "validate", "problem_type_id" to "selected",
                "description" to "", "media_count" to media.count { it.id != null },
            ),
        )
    }

    fun uploadMedia(upload: CustomerMediaUpload, kind: String) {
        media += MediaDraft(null, kind, "uploading")
        state = CustomerLogic.reduce("SCR-C17", mediaInput())
        Thread {
            val next = runCatching {
                val payload = api.uploadMedia(token(), upload)
                media[media.lastIndex] = MediaDraft(payload.getInt("id"), kind, "uploaded")
                mediaInput()
            }.getOrElse {
                media[media.lastIndex] = MediaDraft(null, kind, "failed")
                mediaInput()
            }
            Handler(Looper.getMainLooper()).post { state = CustomerLogic.reduce("SCR-C17", next) }
        }.start()
    }

    fun deleteMedia(index: Int) {
        val item = media.getOrNull(index) ?: return
        media.removeAt(index)
        state = CustomerLogic.reduce("SCR-C17", mediaInput())
        item.id?.let { id -> Thread { runCatching { api.deleteMedia(token(), id) } }.start() }
    }

    fun loadOrder(orderId: Int) = request("SCR-C09") {
        val order = api.order(token(), orderId)
        currentOrderPayload = order
        currentOrderId = orderId
        currentOrderVersion = order.getInt("version")
        mapOf(
            "event" to "loaded",
            "status" to order.getString("status"),
            "display_status" to order.getString("display_status"),
            "stepper" to !order.isNull("stepper"),
            "eta_approximate" to order.optBoolean("eta_approximate"),
            "available_actions" to order.getJSONArray("available_actions").strings(),
            "step_states" to order.optJSONArray("stepper")?.objects()?.map { it.getString("state") }.orEmpty(),
        )
    }

    fun loadPaymentSummary() {
        val orderId = currentOrderId ?: return
        request("SCR-C21") {
            val order = api.order(token(), orderId)
            currentOrderPayload = order
            currentOrderVersion = order.getInt("version")
            paymentSummaryInput(order, "loaded")
        }
    }

    fun selectPaymentMethod(index: Int) {
        val orderId = currentOrderId ?: return
        val method = options("payment_methods").getOrNull(index)?.code ?: return
        val current = currentOrderPayload ?: return
        state = CustomerLogic.reduce("SCR-C21", paymentSummaryInput(current, "switching"))
        Thread {
            val next = runCatching {
                val order = api.changePaymentMethod(token(), orderId, method, currentOrderVersion)
                currentOrderPayload = order
                currentOrderVersion = order.getInt("version")
                paymentSummaryInput(order, "loaded")
            }.getOrElse { mapOf("event" to "error") }
            Handler(Looper.getMainLooper()).post { state = CustomerLogic.reduce("SCR-C21", next) }
        }.start()
    }

    fun openElectronicPayment() {
        val order = currentOrderPayload ?: return
        selectedPaymentChannel = -1
        state = CustomerLogic.reduce("SCR-C22", paymentInput(order, null, "loaded"))
    }

    fun selectPaymentChannel(index: Int) {
        selectedPaymentChannel = index
        val order = currentOrderPayload ?: return
        state = CustomerLogic.reduce("SCR-C22", paymentInput(order, null, "loaded"))
    }

    fun createElectronicPayment() {
        val orderId = currentOrderId ?: return
        val order = currentOrderPayload ?: return
        val channel = options("payment_channels").getOrNull(selectedPaymentChannel)?.code ?: return
        state = CustomerLogic.reduce("SCR-C22", paymentInput(order, null, "creating"))
        Thread {
            val next = runCatching {
                val payment = api.createPayment(token(), orderId, channel, currentOrderVersion)
                paymentInput(order, payment, "loaded")
            }.getOrElse { mapOf("event" to "error") }
            Handler(Looper.getMainLooper()).post { state = CustomerLogic.reduce("SCR-C22", next) }
        }.start()
    }

    fun loadCompletionSummary() {
        val orderId = currentOrderId ?: return
        request("SCR-C23") {
            val order = api.order(token(), orderId)
            currentOrderPayload = order
            currentOrderVersion = order.getInt("version")
            completionInput(order, "loaded")
        }
    }

    fun confirmCompletion() {
        val orderId = currentOrderId ?: return
        val order = currentOrderPayload ?: return
        state = CustomerLogic.reduce("SCR-C23", completionInput(order, "submitting"))
        Thread {
            val result = runCatching { api.confirmCompletion(token(), orderId, currentOrderVersion) }
            Handler(Looper.getMainLooper()).post {
                result.onSuccess {
                    currentOrderPayload = it
                    currentOrderVersion = it.getInt("version")
                    openRating()
                }.onFailure { state = CustomerLogic.reduce("SCR-C23", mapOf("event" to "error")) }
            }
        }.start()
    }

    fun openRating() {
        ratingValues = mutableListOf(0, 0, 0)
        ratingComment = ""
        state = CustomerLogic.reduce("SCR-C24", ratingInput("loaded"))
    }

    fun updateRating(dimension: Int, value: Int) {
        if (dimension !in ratingValues.indices || value !in 1..5) return
        ratingValues[dimension] = value
        state = CustomerLogic.reduce("SCR-C24", ratingInput("loaded"))
    }

    fun updateRatingComment(value: String) {
        ratingComment = value
        state = CustomerLogic.reduce("SCR-C24", ratingInput("loaded"))
    }

    fun submitRating() {
        val orderId = currentOrderId ?: return
        if (!state.canContinue) return
        state = CustomerLogic.reduce("SCR-C24", ratingInput("submitting"))
        Thread {
            val success = runCatching {
                api.submitReview(
                    token(), orderId, ratingValues[0], ratingValues[1], ratingValues[2],
                    ratingComment.trim().ifBlank { null },
                )
            }.isSuccess
            Handler(Looper.getMainLooper()).post {
                state = CustomerLogic.reduce("SCR-C24", ratingInput(if (success) "success" else "error"))
            }
        }.start()
    }

    fun openCancellation() {
        cancellationReasonIndex = -1
        cancellationNote = ""
        state = CustomerLogic.reduce("SCR-C20", cancellationInput("loaded"))
    }

    fun selectCancellationReason(index: Int) {
        cancellationReasonIndex = index
        state = CustomerLogic.reduce("SCR-C20", cancellationInput("loaded"))
    }

    fun updateCancellationNote(value: String) {
        cancellationNote = value
        state = CustomerLogic.reduce("SCR-C20", cancellationInput("loaded"))
    }

    fun submitCancellation() {
        val orderId = currentOrderId ?: return
        val reason = options("customer_cancellation_reasons").getOrNull(cancellationReasonIndex)?.code ?: return
        state = CustomerLogic.reduce("SCR-C20", cancellationInput("submitting"))
        Thread {
            val next = runCatching {
                val order = api.cancelOrder(token(), orderId, reason, cancellationNote.ifBlank { null }, currentOrderVersion)
                currentOrderVersion = order.getInt("version")
                trackingInput(order)
            }.getOrElse { mapOf("event" to "error") }
            Handler(Looper.getMainLooper()).post { state = CustomerLogic.reduce(if (next["event"] == "error") "SCR-C20" else "SCR-C09", next) }
        }.start()
    }

    fun loadPendingProposal() {
        val orderId = currentOrderId ?: return
        state = CustomerLogic.reduce("SCR-C30", mapOf("event" to "loading"))
        Thread {
            val result = runCatching {
                val order = api.order(token(), orderId)
                currentOrderVersion = order.getInt("version")
                val proposal = api.proposals(token(), orderId).objects().last { it.getString("status") == "PENDING" }
                currentProposalId = proposal.getInt("id")
                val screen = if (proposal.getString("type") == "EXECUTION_QUOTE") "SCR-C30" else "SCR-C31"
                screen to proposalInput(order, proposal, "loaded")
            }.getOrElse { "SCR-C30" to mapOf("event" to "error") }
            Handler(Looper.getMainLooper()).post { state = CustomerLogic.reduce(result.first, result.second) }
        }.start()
    }

    fun decideProposal(action: String) {
        val orderId = currentOrderId ?: return
        val proposalId = currentProposalId ?: return
        val screen = state.screen
        val input = mapOf(
            "event" to "submitting", "type_label" to state.items.getOrElse(0) { "" },
            "amount" to state.items.getOrElse(1) { "" }, "reason" to state.items.getOrElse(2) { "" },
            "total" to state.items.getOrElse(3) { "" }, "countdown_seconds" to state.countdownSeconds,
            "has_photo" to state.hasPhoto, "available_actions" to state.visibleActions,
        )
        state = CustomerLogic.reduce(screen, input)
        Thread {
            val next = runCatching {
                val order = api.decideProposal(token(), orderId, proposalId, action == "approve_proposal", currentOrderVersion)
                currentOrderVersion = order.getInt("version")
                trackingInput(order)
            }.getOrElse { mapOf("event" to "error") }
            Handler(Looper.getMainLooper()).post { state = CustomerLogic.reduce(if (next["event"] == "error") screen else "SCR-C09", next) }
        }.start()
    }

    private fun cancellationInput(event: String): Map<String, Any?> = mapOf(
        "event" to event, "reason_labels" to options("customer_cancellation_reasons").map(CustomerOption::label),
        "selected_reason_index" to cancellationReasonIndex,
        "note" to cancellationNote, "available_actions" to listOf("cancel"),
    )

    private fun loadConversation(id: Int, showLoading: Boolean) {
        stopChatPolling()
        if (showLoading) state = CustomerLogic.reduce("SCR-C19", mapOf("event" to "loading"))
        Thread {
            val next = runCatching {
                val response = api.conversationMessages(token(), id)
                currentConversationPayload = response.getJSONObject("conversation")
                chatMessages = response.getJSONArray("data").objects()
                chatInput("loaded")
            }.getOrElse { mapOf("event" to "error") }
            mainHandler.post {
                state = CustomerLogic.reduce("SCR-C19", next)
                if (next["event"] != "error") scheduleChatPoll(id)
            }
        }.start()
    }

    private fun scheduleChatPoll(id: Int) {
        val poll = Runnable { if (currentConversationId == id && state.screen == "SCR-C19") loadConversation(id, showLoading = false) }
        chatPoll = poll
        mainHandler.postDelayed(poll, 5_000)
    }

    private fun stopChatPolling() {
        chatPoll?.let(mainHandler::removeCallbacks)
        chatPoll = null
    }

    private fun chatInput(event: String): Map<String, Any?> {
        val conversation = currentConversationPayload
        val order = conversation?.optJSONObject("order")
        return mapOf(
            "event" to event,
            "conversation_status" to conversation?.optString("status").orEmpty(),
            "provider_name" to conversation?.optJSONObject("provider")?.optString("name").orEmpty(),
            "order_summary" to "${order?.optString("category").orEmpty()} #${order?.optString("number").orEmpty()} · ${order?.optString("status_label").orEmpty()}",
            "message_bodies" to chatMessages.map { message ->
                if (message.optJSONArray("media")?.length() ?: 0 > 0 && message.optString("body").isBlank()) "" else message.optString("body")
            },
            "message_kinds" to chatMessages.map { message ->
                val direction = if (message.optBoolean("is_mine")) "sent" else "received"
                val kind = when {
                    message.optBoolean("was_masked") -> "blocked"
                    (message.optJSONArray("media")?.length() ?: 0) > 0 -> "image"
                    else -> "text"
                }
                "${direction}_$kind"
            },
            "message_times" to chatMessages.map { it.optString("created_at").displayDateTime() },
            "draft" to chatDraft,
        )
    }

    private fun orderHistoryInput(order: JSONObject, proposals: List<JSONObject>): Map<String, Any?> {
        val amounts = order.getJSONObject("amounts")
        val timing = order.getJSONObject("timing")
        val summary = mutableListOf(
            "#${order.optString("number")}", order.getJSONObject("category").optString("name"),
            order.getJSONObject("problem_type").optString("name"), order.getJSONObject("location").optString("area"),
            (timing.optString("slot_start").ifBlank { order.optString("created_at") }).displayDateTime(),
        )
        if (order.optString("status") == "CLOSED") {
            summary += listOf(
                formatAmount(amounts.optString("labor_total", "0.00")),
                formatAmount(amounts.optString("materials_total", "0.00")),
                formatAmount(amounts.optString("final_amount", "0.00")),
                amounts.optString("payment_method"), amounts.optString("payment_status"),
            )
        }
        return mapOf(
            "event" to "loaded", "status" to order.optString("status"),
            "display_status" to order.optString("status_label"), "summary" to summary,
            "provider_name" to order.optJSONObject("provider")?.optString("name").orEmpty(),
            "termination_reason" to order.optJSONObject("termination")?.let {
                it.optString("note").ifBlank { it.optString("reason_label") }
            }.orEmpty(),
            "proposal_summaries" to proposals.map { "${it.optString("type_label")} · ${formatAmount(it.optString("amount"))}" },
            "available_actions" to order.getJSONArray("available_actions").strings(),
        )
    }

    private fun trackingInput(order: JSONObject): Map<String, Any?> = mapOf(
        "event" to "loaded", "status" to order.getString("status"),
        "display_status" to order.getString("display_status"), "stepper" to !order.isNull("stepper"),
        "eta_approximate" to order.optBoolean("eta_approximate"),
        "available_actions" to order.getJSONArray("available_actions").strings(),
        "step_states" to order.optJSONArray("stepper")?.objects()?.map { it.getString("state") }.orEmpty(),
    )

    private fun paymentSummaryInput(order: JSONObject, event: String): Map<String, Any?> {
        val amounts = order.getJSONObject("amounts")
        return mapOf(
            "event" to event,
            "amounts" to listOf(
                formatAmount(amounts.optString("labor_total", "0.00")),
                formatAmount(amounts.optString("materials_total", "0.00")),
                formatAmount(amounts.optString("final_amount", "0.00")),
            ),
            "method_labels" to options("payment_methods").map(CustomerOption::label),
            "selected_method_index" to options("payment_methods").indexOfFirst { it.code == amounts.optString("payment_method") },
            "payment_method" to amounts.optString("payment_method"),
            "available_actions" to order.getJSONArray("available_actions").strings(),
        )
    }

    private fun paymentInput(order: JSONObject, payment: JSONObject?, event: String): Map<String, Any?> {
        val expiresAt = payment?.optString("expires_at")?.takeIf(String::isNotBlank)
        val seconds = expiresAt?.let {
            runCatching { java.time.Duration.between(OffsetDateTime.now(), OffsetDateTime.parse(it)).seconds.toInt().coerceAtLeast(0) }.getOrDefault(0)
        } ?: 0
        return mapOf(
            "event" to event,
            "total" to formatAmount(order.getJSONObject("amounts").optString("final_amount", "0.00")),
            "channel_labels" to options("payment_channels").map(CustomerOption::label),
            "selected_channel_index" to selectedPaymentChannel,
            "payment_status" to (payment?.optString("status") ?: ""),
            "reference_number" to (payment?.optString("reference_number") ?: ""),
            "checkout_url" to (payment?.optString("checkout_url") ?: ""),
            "countdown_seconds" to seconds,
            "available_actions" to order.getJSONArray("available_actions").strings(),
        )
    }

    private fun completionInput(order: JSONObject, event: String): Map<String, Any?> {
        val amounts = order.getJSONObject("amounts")
        return mapOf(
            "event" to event,
            "summary" to listOf(
                order.optJSONObject("provider")?.optString("name").orEmpty(),
                formatAmount(amounts.optString("labor_total", "0.00")),
                formatAmount(amounts.optString("materials_total", "0.00")),
                formatAmount(amounts.optString("final_amount", "0.00")),
                amounts.optString("payment_method"),
            ),
            "available_actions" to order.getJSONArray("available_actions").strings(),
        )
    }

    private fun ratingInput(event: String): Map<String, Any?> = mapOf(
        "event" to event,
        "ratings" to ratingValues.map(Int::toString),
        "comment" to ratingComment,
        "comment_length" to ratingComment.length,
        "available_actions" to if (event == "success") emptyList<String>() else listOf("rate"),
    )

    private fun proposalInput(order: JSONObject, proposal: JSONObject, event: String): Map<String, Any?> {
        val expires = proposal.optString("expires_at").takeIf { it.isNotBlank() }
        val seconds = expires?.let { runCatching { java.time.Duration.between(OffsetDateTime.now(), OffsetDateTime.parse(it)).seconds.toInt().coerceAtLeast(0) }.getOrDefault(0) } ?: 0
        return mapOf(
            "event" to event, "type_label" to proposal.getString("type_label"),
            "amount" to formatAmount(proposal.getString("amount")),
            "reason" to proposal.getString("reason"),
            "total" to formatAmount(proposal.getString("projected_total")),
            "countdown_seconds" to seconds, "expired" to (seconds == 0),
            "has_photo" to !proposal.isNull("photo_path"),
            "available_actions" to order.getJSONArray("available_actions").strings(),
        )
    }

    private fun formatAmount(value: String): String = "${value.substringBefore('.')} $currencyLabel"

    fun openOrder() {
        currentOrderId?.let(::loadOrder) ?: show("SCR-C01", mapOf("event" to "loaded", "operating_mode" to "marketplace", "has_orders" to false))
    }

    fun publishDraft() {
        val address = addressId
        val category = categoryId
        val problem = problemTypeId
        if (address == null || category == null || problem == null) {
            state = CustomerLogic.reduce("SCR-C05", mapOf("event" to "error"))
            return
        }
        request("SCR-C06") {
            val body = JSONObject()
                    .put("customer_address_id", address)
                    .put("category_id", category)
                    .put("problem_type_id", problem)
                    .put("timing_type", timingType)
                    .put("materials_responsibility", optionDefaults["materials_responsibility"])
                    .put("terms_accepted", true)
                    .put("description", currentOrderPayload?.optNullableString("description"))
                    .put("media_ids", JSONArray(media.mapNotNull { it.id }))
                    .apply { slotStart?.let { put("slot_start", it) } }
            val order = editingOrderId?.let { api.updateOrder(token(), it, body) }
                ?: api.publish(token(), body, java.util.UUID.randomUUID().toString())
            editingOrderId = null
            currentOrderId = order.getInt("id")
            mapOf("event" to "loaded", "offer_count" to 0, "available_actions" to order.getJSONArray("available_actions").strings())
        }
    }

    fun loadOffers(orderId: Int) = request("SCR-C06") {
        val order = api.order(token(), orderId)
        val offers = api.offers(token(), orderId)
        mapOf(
            "event" to "loaded", "offer_count" to offers.length(),
            "pricing_mode" to order.getString("pricing_mode"),
            "timing_type" to order.getJSONObject("timing").getString("type").lowercase(),
            "available_actions" to order.getJSONArray("available_actions").strings(),
        )
    }

    fun loadProvider(providerId: Int, orderId: Int) = request("SCR-C07") {
        val provider = api.provider(token(), providerId, orderId)
        currentProviderId = providerId
        currentOrderId = orderId
        currentProviderActions = provider.getJSONArray("available_actions").strings()
        mapOf(
            "event" to "loaded",
            "provider_available" to provider.getBoolean("available_now"),
            "available_actions" to provider.getJSONArray("available_actions").strings(),
        )
    }

    fun openDispute() {
        val orderId = currentOrderId ?: return
        supportReasonIndex = -1
        supportDescription = ""
        disputeMedia.clear()
        request("SCR-C27") {
            val existing = api.disputes(token(), orderId).objects().firstOrNull()
            if (existing == null) disputeInput("loaded") else disputeInput("loaded", existing)
        }
    }

    fun selectSupportReason(index: Int) {
        supportReasonIndex = index
        state = CustomerLogic.reduce(state.screen, if (state.screen == "SCR-C28") providerReportInput("loaded") else disputeInput("loaded"))
    }

    fun updateSupportDescription(value: String) {
        supportDescription = value.take(1001)
        state = CustomerLogic.reduce(state.screen, if (state.screen == "SCR-C28") providerReportInput("loaded") else disputeInput("loaded"))
    }

    fun uploadDisputePhoto(upload: CustomerMediaUpload) {
        disputeMedia += MediaDraft(null, "photo", "uploading")
        state = CustomerLogic.reduce("SCR-C27", disputeInput("loaded"))
        Thread {
            val next = runCatching {
                val item = api.uploadMedia(token(), upload)
                disputeMedia[disputeMedia.lastIndex] = MediaDraft(item.getInt("id"), "photo", "uploaded")
                disputeInput("loaded")
            }.getOrElse {
                disputeMedia[disputeMedia.lastIndex] = MediaDraft(null, "photo", "failed")
                disputeInput("loaded")
            }
            mainHandler.post { state = CustomerLogic.reduce("SCR-C27", next) }
        }.start()
    }

    fun removeDisputePhoto(index: Int) {
        val item = disputeMedia.getOrNull(index) ?: return
        disputeMedia.removeAt(index)
        state = CustomerLogic.reduce("SCR-C27", disputeInput("loaded"))
        item.id?.let { id -> Thread { runCatching { api.deleteMedia(token(), id) } }.start() }
    }

    fun submitDispute() {
        val orderId = currentOrderId ?: return
        val reason = options("dispute_reasons").getOrNull(supportReasonIndex)?.code ?: return
        if (!state.canContinue) return
        state = CustomerLogic.reduce("SCR-C27", disputeInput("submitting"))
        Thread {
            val next = runCatching {
                val dispute = api.openDispute(
                    token(), orderId, reason, supportDescription.trim(), disputeMedia.mapNotNull(MediaDraft::id),
                )
                disputeInput("success", dispute)
            }.getOrElse { mapOf("event" to "error") }
            mainHandler.post { state = CustomerLogic.reduce("SCR-C27", next) }
        }.start()
    }

    fun openProviderReport() {
        supportReasonIndex = -1
        supportDescription = ""
        state = CustomerLogic.reduce("SCR-C28", providerReportInput("loaded"))
    }

    fun submitProviderReport() {
        val providerId = currentProviderId ?: return
        val orderId = currentOrderId ?: return
        val reason = options("provider_report_reasons").getOrNull(supportReasonIndex)?.code ?: return
        if (!state.canContinue) return
        state = CustomerLogic.reduce("SCR-C28", providerReportInput("submitting"))
        Thread {
            val success = runCatching {
                api.reportProvider(token(), providerId, orderId, reason, supportDescription.trim().ifBlank { null })
            }.isSuccess
            mainHandler.post {
                state = CustomerLogic.reduce("SCR-C28", providerReportInput(if (success) "success" else "error"))
            }
        }.start()
    }

    fun acceptOffer(orderId: Int, offerId: Int, expectedVersion: Int) = request("SCR-C09") {
        val paymentMethod = optionDefaults["payment_method"] ?: return@request mapOf("event" to "error")
        val order = api.acceptOffer(token(), orderId, offerId, expectedVersion, paymentMethod)
        mapOf(
            "event" to "loaded", "status" to order.getString("status"),
            "display_status" to order.getString("display_status"), "stepper" to !order.isNull("stepper"),
            "available_actions" to order.getJSONArray("available_actions").strings(),
        )
    }

    private fun loadSlotsForDay(
        activeCity: Int,
        index: Int,
        dayLabels: List<String>,
        morning: String,
        evening: String,
    ) = request("SCR-C16", loadingEvent = "changing_day") {
        val date = dayDates.getOrElse(index) { dayDates.first() }
        val slots = api.slots(activeCity, date).objects()
        slotStarts = slots.map { it.getString("start") }
        mapOf(
            "event" to "loaded", "day_labels" to dayLabels,
            "slot_labels" to slots.map { "${it.getString("start").timeLabel(morning, evening)}–${it.getString("end").timeLabel(morning, evening)}" },
            "selected_day_index" to index, "selected_slot_index" to -1,
        )
    }

    private fun addressInput(event: String, areaLabels: List<String>): Map<String, Any?> = mapOf(
        "event" to event, "label" to addressDraft.label, "area_id" to (addressDraft.areaId?.toString() ?: ""),
        "area_served" to (addressDraft.areaId != null), "address_text" to addressDraft.addressText,
        "building" to addressDraft.building, "floor" to addressDraft.floor, "apartment" to addressDraft.apartment,
        "landmark" to addressDraft.landmark, "lat" to (addressDraft.latitude?.toString() ?: ""),
        "lng" to (addressDraft.longitude?.toString() ?: ""), "is_default" to addressDraft.isDefault,
        "editing" to (addressDraft.id != null),
        "area_labels" to areaLabels, "selected_area_index" to addressDraft.selectedAreaIndex,
    )

    private fun mediaInput(): Map<String, Any?> = mapOf(
        "event" to "validate", "media_kinds" to media.map { it.kind }, "media_states" to media.map { it.state },
        "photo_count" to media.count { it.kind == "photo" }, "video_count" to media.count { it.kind == "video" },
        "audio_count" to media.count { it.kind == "audio" },
    )

    private fun disputeInput(event: String, dispute: JSONObject? = null): Map<String, Any?> = mapOf(
        "event" to event,
        "existing" to (dispute != null),
        "reason_labels" to options("dispute_reasons").map(CustomerOption::label),
        "selected_reason_index" to supportReasonIndex,
        "description" to (dispute?.optString("description") ?: supportDescription),
        "photo_count" to if (dispute == null) disputeMedia.size else dispute.optJSONArray("attachments")?.length() ?: 0,
        "details" to if (dispute == null) emptyList<String>() else listOf(
            dispute.optString("reason_label"), dispute.optString("description"),
        ),
        "status_details" to if (dispute == null) emptyList<String>() else listOf(
            dispute.optString("status_label", dispute.optString("status")), dispute.optString("resolution_note"),
        ),
        "available_actions" to if (dispute == null) currentOrderPayload
            ?.optJSONArray("available_actions")?.strings().orEmpty() else emptyList<String>(),
    )

    private fun providerReportInput(event: String): Map<String, Any?> = mapOf(
        "event" to event,
        "reason_labels" to options("provider_report_reasons").map(CustomerOption::label),
        "selected_reason_index" to supportReasonIndex,
        "description" to supportDescription,
        "available_actions" to if (event == "success") emptyList<String>() else currentProviderActions,
    )

    private fun helpInput(event: String): Map<String, Any?> = mapOf(
        "event" to event, "faq_questions" to faqQuestions, "faq_answers" to faqAnswers,
        "expanded_index" to expandedFaqIndex, "subject" to helpSubject, "message" to helpMessage,
    )

    private fun notificationInput(values: List<JSONObject>, event: String): Map<String, Any?> = mapOf(
        "event" to event, "notification_titles" to values.map { it.optString("title") },
        "notification_bodies" to values.map { it.optString("body") },
        "notification_states" to values.map { if (it.isNull("read_at")) "unread" else "read" },
        "deep_links" to notificationLinks,
        "notification_times" to values.map { it.optString("created_at").displayDateTime() },
    )

    private fun accountInput(event: String, mode: String = accountMode): Map<String, Any?> = mapOf(
        "event" to event, "mode" to mode, "field_values" to accountValues, "available_actions" to accountActions,
    )

    private fun submitAccountUpdate() {
        if (!state.canContinue) return
        state = CustomerLogic.reduce("SCR-C33", accountInput("submitting"))
        Thread {
            val user = runCatching { api.updateAccount(token(), accountValues[0].trim(), accountValues[2].trim()) }.getOrNull()
            mainHandler.post {
                if (user == null) state = CustomerLogic.reduce("SCR-C33", mapOf("event" to "error"))
                else {
                    accountValues = mutableListOf(user.optString("name"), user.optString("email"), user.optString("phone"))
                    accountMode = "overview"
                    state = CustomerLogic.reduce("SCR-C33", accountInput("success"))
                }
            }
        }.start()
    }

    private fun submitPasswordChange() {
        if (!state.canContinue) return
        state = CustomerLogic.reduce("SCR-C33", accountInput("submitting"))
        Thread {
            val success = runCatching { api.changePassword(token(), accountValues[0], accountValues[1], accountValues[2]) }.isSuccess
            mainHandler.post {
                accountMode = "overview"
                accountValues = mutableListOf("", session.email.orEmpty(), "")
                state = CustomerLogic.reduce("SCR-C33", if (success) accountInput("success") else mapOf("event" to "error"))
            }
        }.start()
    }

    private fun submitDeleteAccount() {
        state = CustomerLogic.reduce("SCR-C33", accountInput("submitting", "delete"))
        Thread {
            val result = runCatching { api.deleteAccount(token()) }
            mainHandler.post {
                if (result.isSuccess) logout()
                else if ((result.exceptionOrNull() as? CustomerApiException)?.status == 409) {
                    accountMode = "overview"
                    state = CustomerLogic.reduce("SCR-C33", accountInput("active_order"))
                } else state = CustomerLogic.reduce("SCR-C33", mapOf("event" to "error"))
            }
        }.start()
    }

    private fun options(key: String): List<CustomerOption> = optionLists[key].orEmpty()

    private fun request(screen: String, loadingEvent: String = "loading", block: () -> Map<String, Any?>) {
        if (screen != "SCR-C19") stopChatPolling()
        state = CustomerLogic.reduce(screen, mapOf("event" to loadingEvent))
        Thread {
            val next = runCatching(block).getOrElse { mapOf("event" to "error") }
            Handler(Looper.getMainLooper()).post { state = CustomerLogic.reduce(screen, next) }
        }.start()
    }

    private fun token(): String = requireNotNull(session.token)
    private fun JSONArray.strings(): List<String> = (0 until length()).map(::getString)
    private fun JSONArray.objects(): List<JSONObject> = (0 until length()).map(::getJSONObject)
    private fun JSONObject.optNullableString(key: String): String = if (isNull(key)) "" else optString(key)

    private fun String.timeLabel(morning: String, evening: String): String = runCatching {
        OffsetDateTime.parse(this).format(DateTimeFormatter.ofPattern("h:mm a"))
            .replace("AM", morning).replace("PM", evening)
    }.getOrDefault(this)

    private fun String.displayDateTime(): String = ifBlank { "" }.let { value ->
        runCatching { OffsetDateTime.parse(value).format(DateTimeFormatter.ofPattern("yyyy-MM-dd · h:mm a")) }.getOrDefault(value)
    }

    override fun onCleared() {
        stopChatPolling()
        super.onCleared()
    }
}

private data class AddressDraft(
    val id: Int? = null,
    val cityId: Int? = null,
    val areaId: Int? = null,
    val selectedAreaIndex: Int = -1,
    val label: String = "",
    val addressText: String = "",
    val building: String = "",
    val floor: String = "",
    val apartment: String = "",
    val landmark: String = "",
    val latitude: Double? = null,
    val longitude: Double? = null,
    val isDefault: Boolean = false,
)

private data class MediaDraft(val id: Int?, val kind: String, val state: String)

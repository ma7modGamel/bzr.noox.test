package noox.bzr.provider

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import noox.bzr.auth.AuthSessionStore
import noox.bzr.customer.CustomerLogic
import noox.bzr.customer.CustomerMediaUpload
import noox.bzr.customer.CustomerUiState
import noox.bzr.links.DeepLinkTarget
import org.json.JSONArray
import org.json.JSONObject
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

class ProviderViewModel(
    private val api: ProviderApi,
    private val session: AuthSessionStore,
    private val currencyLabel: String,
) : ViewModel() {
    private val mutableState = MutableStateFlow(ProviderUiState("SCR-P01", ProviderPhase.Loading))
    val stateFlow: StateFlow<ProviderUiState> = mutableState.asStateFlow()
    private val mutableFlowActive = MutableStateFlow(false)
    val flowActive: StateFlow<Boolean> = mutableFlowActive.asStateFlow()

    private var application: ProviderApplicationPayload? = null
    private var experienceYears = ""
    private var bio = ""
    private var profilePhotoMediaId: Int? = null
    private var idFrontMediaId: Int? = null
    private var idBackMediaId: Int? = null
    private var profilePhotoUploaded = false
    private var idFrontUploaded = false
    private var idBackUploaded = false
    private var categories: List<ProviderCategory> = emptyList()
    private val categoryIds = linkedSetOf<Int>()
    private val specialtyIds = linkedSetOf<Int>()
    private var cities: List<ProviderChoice> = emptyList()
    private var areas: List<ProviderChoice> = emptyList()
    private val areaIds = linkedSetOf<Int>()
    private var selectedCityIndex = -1
    private var payoutOptions: List<ProviderPayoutOption> = emptyList()
    private var selectedPayoutIndex = -1
    private var payoutDetails = ""
    private var home: ProviderHomePayload? = null
    private var activeOrder: ProviderOrderPayload? = null
    private var providerReasons: List<ProviderReason> = emptyList()
    private var proposalTypes: List<ProviderReason> = emptyList()
    private var selectedProposalType = -1
    private var proposalAmount = ""
    private var proposalReason = ""
    private var outsidePriceGuideReason = ""
    private var proposalPhotoMediaId: Int? = null
    private var selectedUnableReason = -1
    private var unableNote = ""
    private var requestedUnableAction = ""
    private var ratingStars = 0
    private var workspaceProfile: ProviderProfilePayload? = null
    private var workspaceSpecialtyIds = linkedSetOf<Int>()
    private var workspaceAreaIds = linkedSetOf<Int>()
    private var workspaceAreas: List<ProviderChoice> = emptyList()
    private var workspacePortfolio: List<ProviderPortfolioItem> = emptyList()
    private var portfolioCaption = ""
    private var activeConversationId: Int? = null
    private var chatPayload: ProviderConversationPayload? = null
    private var chatDraft = ""
    private var availableRequests: List<ProviderOrderPayload> = emptyList()
    private var marketRequest: ProviderMarketRequestPayload? = null
    private var marketOffers: List<ProviderOfferPayload> = emptyList()
    private var offerPrice = ""
    private var offerIncludes = ""
    private var offerNote = ""
    private var selectedOfferEta = -1
    private var selectedOfferDeductible = -1
    private var selectedMarketOffer = -1
    private var notificationIds: List<String> = emptyList()
    private var notificationLinks: List<String> = emptyList()

    /** C32 in provider mode is the customer screen fed by provider-mode data, as C19 is (DEC-058). */
    var notificationsState: CustomerUiState = CustomerLogic.reduce("SCR-C32", mapOf("event" to "loading"))
        private set

    /** Rows open through the activity, which routes across modes (17 §الروابط العميقة). */
    var onDeepLink: (String) -> Unit = {}

    fun openApplication() {
        mutableFlowActive.value = true
        loadApplication()
    }

    fun closeFlow() {
        mutableFlowActive.value = false
    }

    fun goBack() {
        when (mutableState.value.screen) {
            "SCR-P02" -> mutableState.value = ProviderLogic.reduce(
                "SCR-P01",
                mapOf("event" to "loaded", "status" to "NOT_STARTED", "available_actions" to application?.availableActions.orEmpty()),
            )
            "SCR-P03" -> showProfile()
            "SCR-P04" -> mutableState.value = identityState("validate")
            "SCR-P05" -> mutableState.value = catalogState()
            "SCR-P06" -> mutableState.value = areasState()
            "SCR-P10" -> marketRequest?.let { mutableState.value = marketRequestState(it) } ?: loadHome()
            "SCR-P09", "SCR-P11", "SCR-C32" -> loadHome()
            "SCR-P20", "SCR-P21", "SCR-C19" -> activeOrder?.let { mutableState.value = activeOrderState(it) } ?: loadHome()
            "SCR-P12", "SCR-P13", "SCR-P14", "SCR-P15", "SCR-P16", "SCR-P17", "SCR-P18", "SCR-P19" -> loadHome()
            else -> closeFlow()
        }
    }

    fun loadApplication() = launch("SCR-P01") {
        val loaded = api.application(token())
        application = loaded
        hydrate(loaded)
        mutableState.value = if (loaded.status == "NOT_STARTED") {
            ProviderLogic.reduce("SCR-P01", mapOf("event" to "loaded", "status" to loaded.status, "available_actions" to loaded.availableActions))
        } else {
            statusState(loaded)
        }
    }

    fun handleAction(action: String) {
        if (action !in mutableState.value.visibleActions) return
        when (action) {
            "start_provider_application", "resubmit_provider_application" -> showProfile()
            "submit_provider_application" -> if (mutableState.value.screen == "SCR-P06") submit() else showProfile()
            "open_provider_home" -> loadHome()
            "open_assigned_order" -> loadActiveOrder()
            "open_earnings" -> loadEarnings()
            "open_provider_profile" -> loadProviderProfile()
            "open_available_requests" -> availableRequests.firstOrNull()?.let { openMarketRequest(0) }
            "open_my_offers" -> loadMarketOffers()
            "open_messages", "chat" -> openChat()
            "open_notifications" -> loadNotifications()
        }
    }

    fun loadHome() = launch("SCR-P08") {
        val loaded = api.home(token())
        home = loaded
        availableRequests = if ("open_available_requests" in loaded.availableActions) api.availableRequests(token()) else emptyList()
        mutableState.value = homeState(loaded, "loaded")
    }

    /** ACTIVE, PENDING (any submitted application), or NONE — the input DeepLinkRouter needs. */
    fun resolveProviderStatus(onResult: (String) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val status = runCatching { api.application(token()).status }.getOrDefault("NOT_STARTED")
            val mapped = when (status) {
                "ACTIVE" -> "ACTIVE"
                "NOT_STARTED" -> "NONE"
                else -> "PENDING"
            }
            launch(Dispatchers.Main) { onResult(mapped) }
        }
    }

    fun loadNotifications(showDenied: Boolean = false) {
        notificationsState = CustomerLogic.reduce("SCR-C32", mapOf("event" to "loading"))
        mutableFlowActive.value = true
        mutableState.value = ProviderUiState("SCR-C32", ProviderPhase.Loading)
        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                val values = api.notifications(token()).getJSONArray("data").let { array -> List(array.length()) { array.getJSONObject(it) } }
                notificationIds = values.map { it.getString("id") }
                notificationLinks = values.map { if (it.isNull("deep_link")) "" else it.getString("deep_link") }
                CustomerLogic.reduce(
                    "SCR-C32",
                    mapOf(
                        "event" to "loaded",
                        "notification_titles" to values.map { it.optString("title") },
                        "notification_bodies" to values.map { it.optString("body") },
                        "notification_states" to values.map { if (it.isNull("read_at")) "unread" else "read" },
                        "deep_links" to notificationLinks,
                        "notification_times" to values.map { it.optString("created_at").displayDateTime() },
                        "notifications_denied" to showDenied,
                    ),
                )
            }.onSuccess { notificationsState = it }
                .onFailure { notificationsState = CustomerLogic.reduce("SCR-C32", mapOf("event" to "error")) }
            // A fresh state object so the fragment re-renders even when the phase did not change.
            mutableState.value = ProviderUiState("SCR-C32", ProviderPhase.Content, canContinue = notificationsState.canContinue)
        }
    }

    fun openNotification(index: Int) {
        val id = notificationIds.getOrNull(index) ?: return
        val link = notificationLinks.getOrNull(index).orEmpty()
        viewModelScope.launch(Dispatchers.IO) {
            runCatching { api.markNotificationsRead(token(), listOf(id)) }
            viewModelScope.launch(Dispatchers.Main) { if (link.isBlank()) loadNotifications() else onDeepLink(link) }
        }
    }

    /** 17 §الروابط العميقة — provider targets; the router has already checked the provider status. */
    fun openDeepLink(target: DeepLinkTarget, notificationsDenied: Boolean) {
        mutableFlowActive.value = true
        when (target.target) {
            "SCR-P09" -> launch("SCR-P09") {
                marketRequest = api.availableRequest(token(), target.orderId)
                mutableState.value = marketRequestState(checkNotNull(marketRequest))
            }
            "SCR-P11" -> loadMarketOffers()
            "SCR-P18" -> loadEarnings()
            "SCR-P07" -> loadApplication()
            "PROVIDER_ORDER", "SCR-C19", "SCR-P17" -> openOrderLink(target)
            else -> loadNotifications(notificationsDenied)
        }
    }

    private fun openOrderLink(target: DeepLinkTarget) = launch("SCR-P12") {
        val order = api.currentOrders(token()).firstOrNull { it.id == target.orderId }
            ?: api.pastOrders(token()).firstOrNull { it.id == target.orderId }
        if (order == null) {
            viewModelScope.launch(Dispatchers.Main) { loadHome() }
            return@launch
        }
        activeOrder = order
        when {
            target.target == "SCR-C19" -> viewModelScope.launch(Dispatchers.Main) { openChat() }
            target.target == "SCR-P17" && "rate_customer" in order.availableActions -> {
                ratingStars = 0
                mutableState.value = ratingState("loaded")
            }
            order.status in listOf("CANCELLED", "EXPIRED", "OPEN") -> viewModelScope.launch(Dispatchers.Main) { loadHome() }
            else -> mutableState.value = activeOrderState(order)
        }
    }

    fun openMarketRequest(index: Int) {
        val order = availableRequests.getOrNull(index) ?: return
        launch("SCR-P09") {
            marketRequest = api.availableRequest(token(), order.id)
            mutableState.value = marketRequestState(checkNotNull(marketRequest))
        }
    }

    fun handleMarketRequestAction(action: String) {
        val request = marketRequest ?: return
        if (action !in request.order.availableActions) return
        when (action) {
            "submit_offer" -> openOfferForm(request)
            "withdraw_offer" -> loadMarketOffers(request.ownOfferId)
            "chat" -> openChat()
        }
    }

    private fun openOfferForm(request: ProviderMarketRequestPayload) {
        offerPrice = ""
        offerIncludes = request.context.defaultIncludes
        offerNote = ""
        selectedOfferEta = -1
        selectedOfferDeductible = -1
        mutableState.value = offerState("loaded", request)
    }

    fun selectOfferEta(index: Int) {
        if (marketRequest?.context?.etaCodes?.getOrNull(index) == null) return
        selectedOfferEta = index
        mutableState.value = offerState("validate")
    }

    fun selectOfferDeductible(index: Int) {
        if (marketRequest?.context?.deductibleCodes?.getOrNull(index) == null) return
        selectedOfferDeductible = index
        mutableState.value = offerState("validate")
    }

    fun updateOfferPrice(value: String) {
        offerPrice = value.filter { it.isDigit() || it == '.' }.take(12)
        mutableState.value = offerState("validate")
    }

    fun updateOfferIncludes(value: String) {
        offerIncludes = value.take(150)
        mutableState.value = offerState("validate")
    }

    fun updateOfferNote(value: String) {
        offerNote = value.take(300)
        mutableState.value = offerState("validate")
    }

    fun submitMarketOffer() {
        val request = marketRequest ?: return
        val checked = offerState("validate", request)
        if (!checked.canContinue) {
            mutableState.value = checked
            return
        }
        mutableState.value = offerState("submitting", request)
        viewModelScope.launch(Dispatchers.IO) {
            val body = JSONObject()
                .put("price", offerPrice)
                .put("includes_text", offerIncludes)
                .put("note", offerNote.ifBlank { JSONObject.NULL })
            if (request.order.timingType == "NOW") {
                body.put("eta_minutes", request.context.etaCodes[selectedOfferEta].toInt())
            }
            if (request.order.pricingMode == "INSPECTION") {
                body.put("inspection_fee_deductible", request.context.deductibleCodes[selectedOfferDeductible] == "YES")
            }
            runCatching { api.submitOffer(token(), request.order.id, body) }
                .onSuccess { mutableState.value = offerState("success", request, emptyList()) }
                .onFailure { mutableState.value = offerState("unavailable", request, emptyList()) }
        }
    }

    fun loadMarketOffers(selectOfferId: Int? = null) = launch("SCR-P11") {
        marketOffers = api.offers(token())
        selectedMarketOffer = selectOfferId?.let { id -> marketOffers.indexOfFirst { it.id == id } } ?: -1
        mutableState.value = offersState(if (selectedMarketOffer >= 0) "confirm_withdraw" else "loaded")
    }

    fun handleOfferListAction(index: Int, action: String) {
        val offer = marketOffers.getOrNull(index) ?: return
        if (action !in offer.availableActions) return
        when (action) {
            "withdraw_offer" -> {
                selectedMarketOffer = index
                mutableState.value = offersState("confirm_withdraw")
            }
            "open_available_request" -> launch("SCR-P09") {
                marketRequest = api.availableRequest(token(), offer.orderId)
                mutableState.value = marketRequestState(checkNotNull(marketRequest))
            }
            "open_assigned_order" -> loadActiveOrder()
        }
    }

    fun confirmOfferWithdrawal() {
        val offer = marketOffers.getOrNull(selectedMarketOffer) ?: return
        mutableState.value = offersState("withdrawing")
        viewModelScope.launch(Dispatchers.IO) {
            runCatching { api.withdrawOffer(token(), offer.id) }
                .onSuccess { updated ->
                    marketOffers = marketOffers.toMutableList().also { it[selectedMarketOffer] = updated }
                    selectedMarketOffer = -1
                    mutableState.value = offersState("withdrawn")
                }
                .onFailure {
                    marketOffers = runCatching { api.offers(token()) }.getOrDefault(marketOffers)
                    selectedMarketOffer = -1
                    mutableState.value = offersState("changed")
                }
        }
    }

    fun cancelOfferWithdrawal() {
        selectedMarketOffer = -1
        mutableState.value = offersState("loaded")
    }

    fun toggleAvailability(availableNow: Boolean) {
        val current = home ?: return
        if ("set_availability" !in current.availableActions || mutableState.value.isBusy) return
        mutableState.value = homeState(current.copy(availableNow = availableNow), "saving")
        viewModelScope.launch(Dispatchers.IO) {
            runCatching { api.setAvailability(token(), availableNow) }
                .onSuccess {
                    home = it
                    mutableState.value = homeState(it, "availability_saved")
                }
                .onFailure {
                    mutableState.value = ProviderLogic.reduce("SCR-P08", mapOf("event" to "error"))
                }
        }
    }

    fun loadActiveOrder() = launch("SCR-P12") {
        val loaded = api.currentOrders(token()).firstOrNull()
        if (loaded == null) {
            loadHome()
        } else {
            activeOrder = loaded
            mutableState.value = activeOrderState(loaded)
        }
    }

    fun handleOrderAction(action: String) {
        val order = activeOrder ?: return
        if (action !in order.availableActions || mutableState.value.isBusy) return
        when (action) {
            "submit_execution_quote", "submit_proposal" -> openProposal(action)
            "back_out", "report_unable", "report_no_show" -> openUnable(action)
            "start_work" -> performSimpleAction(action, "start-work")
            "complete_inspection_only" -> performSimpleAction(action, "complete-inspection-only")
            "complete_work" -> performSimpleAction(action, "complete")
            "confirm_cash" -> performSimpleAction(
                action,
                "cash-received",
                JSONObject().put("amount", order.finalAmount),
            )
            "chat" -> openChat()
        }
    }

    fun selectCustomerRating(index: Int) {
        ratingStars = index + 1
        mutableState.value = ratingState("loaded")
    }

    fun submitCustomerRating() {
        val order = activeOrder ?: return
        val checked = ratingState("validate")
        if (!checked.canContinue) {
            mutableState.value = checked
            return
        }
        mutableState.value = ratingState("submitting")
        viewModelScope.launch(Dispatchers.IO) {
            runCatching { api.rateCustomer(token(), order.id, ratingStars) }
                .onSuccess { mutableState.value = ratingState("success", emptyList()) }
                .onFailure { mutableState.value = ProviderLogic.reduce("SCR-P17", mapOf("event" to "error")) }
        }
    }

    fun loadEarnings() = launch("SCR-P18") {
        val loaded = api.earnings(token())
        mutableState.value = ProviderLogic.reduce(
            "SCR-P18",
            mapOf(
                "event" to "loaded", "operating_mode" to loaded.operatingMode,
                "summary" to loaded.summary.map(::formatAmount),
                "transaction_titles" to loaded.transactionTitles,
                "transaction_details" to loaded.transactionDetails,
                "transaction_states" to loaded.transactionStates,
            ),
        )
    }

    fun loadProviderProfile() = launch("SCR-P19") {
        val loaded = api.profile(token())
        workspaceProfile = loaded
        experienceYears = loaded.experienceYears.toString()
        bio = loaded.bio
        categories = loaded.categories
        workspaceSpecialtyIds = loaded.specialtyIds.toCollection(linkedSetOf())
        workspaceAreaIds = loaded.areaIds.toCollection(linkedSetOf())
        workspaceAreas = api.cities().flatMap { api.areas(it.id) }.distinctBy(ProviderChoice::id)
        workspacePortfolio = loaded.portfolio
        profilePhotoUploaded = loaded.hasAvatar
        mutableState.value = workspaceProfileState("loaded")
    }

    fun toggleWorkspaceSpecialty(index: Int) {
        val specialty = categories.flatMap(ProviderCategory::specialties).getOrNull(index) ?: return
        if (!workspaceSpecialtyIds.add(specialty.id)) workspaceSpecialtyIds.remove(specialty.id)
        mutableState.value = workspaceProfileState("validate")
    }

    fun toggleWorkspaceArea(index: Int) {
        val area = workspaceAreas.getOrNull(index) ?: return
        if (!workspaceAreaIds.add(area.id)) workspaceAreaIds.remove(area.id)
        mutableState.value = workspaceProfileState("validate")
    }

    fun updateWorkspaceProfile(experience: String? = null, about: String? = null, caption: String? = null) {
        experience?.let { experienceYears = it.filter(Char::isDigit).take(2) }
        about?.let { bio = it }
        caption?.let { portfolioCaption = it.take(150) }
        mutableState.value = workspaceProfileState("validate")
    }

    fun saveWorkspaceProfile() {
        val checked = workspaceProfileState("validate")
        if (!checked.canContinue) {
            mutableState.value = checked
            return
        }
        mutableState.value = workspaceProfileState("saving")
        viewModelScope.launch(Dispatchers.IO) {
            val body = JSONObject()
                .put("experience_years", experienceYears.toInt())
                .put("bio", bio.ifBlank { JSONObject.NULL })
                .put("specialty_ids", JSONArray(workspaceSpecialtyIds.toList()))
                .put("area_ids", JSONArray(workspaceAreaIds.toList()))
            runCatching { api.updateProfile(token(), body) }
                .onSuccess {
                    workspaceProfile = it
                    mutableState.value = workspaceProfileState("saved")
                }
                .onFailure { mutableState.value = ProviderLogic.reduce("SCR-P19", mapOf("event" to "error")) }
        }
    }

    fun uploadWorkspaceAvatar(upload: CustomerMediaUpload) {
        mutableState.value = workspaceProfileState("uploading")
        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                val id = api.uploadImage(token(), upload)
                api.updateAvatar(token(), id)
            }.onSuccess {
                profilePhotoUploaded = true
                mutableState.value = workspaceProfileState("saved")
            }.onFailure { mutableState.value = ProviderLogic.reduce("SCR-P19", mapOf("event" to "error")) }
        }
    }

    fun addWorkspacePortfolio(upload: CustomerMediaUpload) {
        if (workspacePortfolio.size >= 20) return
        mutableState.value = workspaceProfileState("uploading")
        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                val mediaId = api.uploadImage(token(), upload)
                api.addPortfolio(token(), mediaId, portfolioCaption)
            }.onSuccess {
                workspacePortfolio = workspacePortfolio + it
                portfolioCaption = ""
                mutableState.value = workspaceProfileState("saved")
            }.onFailure { mutableState.value = ProviderLogic.reduce("SCR-P19", mapOf("event" to "error")) }
        }
    }

    fun deleteWorkspacePortfolio(index: Int) {
        val item = workspacePortfolio.getOrNull(index) ?: return
        mutableState.value = workspaceProfileState("saving")
        viewModelScope.launch(Dispatchers.IO) {
            runCatching { api.deletePortfolio(token(), item.id) }
                .onSuccess {
                    workspacePortfolio = workspacePortfolio.filterNot { value -> value.id == item.id }
                    mutableState.value = workspaceProfileState("saved")
                }
                .onFailure { mutableState.value = ProviderLogic.reduce("SCR-P19", mapOf("event" to "error")) }
        }
    }

    fun openChat() {
        val order = activeOrder
        launch("SCR-C19") {
            val conversationId = order?.conversationId
                ?: api.conversations(token()).firstOrNull { order == null || it.orderId == order.id }?.id
                ?: error("Conversation is unavailable")
            activeConversationId = conversationId
            chatPayload = api.conversation(token(), conversationId)
            chatDraft = ""
            mutableState.value = chatState("loaded")
        }
    }

    fun updateChatDraft(value: String) {
        chatDraft = value.take(1000)
        mutableState.value = chatState("loaded")
    }

    fun sendChatMessage() {
        val id = activeConversationId ?: return
        if (!chatState("loaded").canContinue) return
        mutableState.value = chatState("sending")
        viewModelScope.launch(Dispatchers.IO) {
            runCatching { api.sendMessage(token(), id, chatDraft, emptyList()) }
                .onSuccess {
                    chatDraft = ""
                    chatPayload = api.conversation(token(), id)
                    mutableState.value = chatState("loaded")
                }
                .onFailure { mutableState.value = ProviderLogic.reduce("SCR-C19", mapOf("event" to "error")) }
        }
    }

    fun sendChatPhoto(upload: CustomerMediaUpload) {
        val id = activeConversationId ?: return
        mutableState.value = chatState("sending")
        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                val mediaId = api.uploadImage(token(), upload)
                api.sendMessage(token(), id, "", listOf(mediaId))
                api.conversation(token(), id)
            }.onSuccess {
                chatPayload = it
                mutableState.value = chatState("loaded")
            }.onFailure { mutableState.value = ProviderLogic.reduce("SCR-C19", mapOf("event" to "error")) }
        }
    }

    fun performLocationAction(action: String, latitude: Double, longitude: Double) {
        val path = when (action) {
            "start_trip" -> "start-trip"
            "mark_arrived" -> "arrived"
            else -> return
        }
        performSimpleAction(action, path, JSONObject().put("lat", latitude).put("lng", longitude))
    }

    fun activeDestination(): Pair<Double, Double>? {
        val order = activeOrder ?: return null
        return order.latitude?.let { latitude -> order.longitude?.let { longitude -> latitude to longitude } }
    }

    fun activeCustomerPhone(): String? = activeOrder?.customerPhone

    fun selectProposalType(index: Int) {
        if (proposalTypes.getOrNull(index) == null) return
        selectedProposalType = index
        mutableState.value = proposalState("loaded")
    }

    fun updateProposalAmount(value: String) {
        proposalAmount = value.filter { it.isDigit() || it == '.' }
        mutableState.value = proposalState("loaded")
    }

    fun updateProposalReason(value: String) {
        proposalReason = value
        mutableState.value = proposalState("loaded")
    }

    fun updateOutsidePriceGuideReason(value: String) {
        outsidePriceGuideReason = value
        mutableState.value = proposalState("loaded")
    }

    fun uploadProposalPhoto(upload: CustomerMediaUpload) {
        mutableState.value = proposalState("submitting")
        viewModelScope.launch(Dispatchers.IO) {
            runCatching { api.uploadImage(token(), upload) }
                .onSuccess {
                    proposalPhotoMediaId = it
                    mutableState.value = proposalState("loaded")
                }
                .onFailure { mutableState.value = proposalState("media_error") }
        }
    }

    fun deleteProposalPhoto() {
        proposalPhotoMediaId = null
        mutableState.value = proposalState("loaded")
    }

    fun submitProposal() {
        val order = activeOrder ?: return
        val state = proposalState("loaded")
        if (!state.canContinue) {
            mutableState.value = state
            return
        }
        val type = proposalTypes.getOrNull(selectedProposalType) ?: return
        mutableState.value = proposalState("submitting")
        viewModelScope.launch(Dispatchers.IO) {
            val body = JSONObject()
                .put("type", type.code)
                .put("amount", proposalAmount)
                .put("reason", proposalReason)
                .putNullable("photo_media_id", proposalPhotoMediaId)
            if (outsidePriceGuideReason.isNotBlank()) body.put("outside_price_guide_reason", outsidePriceGuideReason)
            runCatching { api.submitProposal(token(), order, body) }
                .onSuccess {
                    mutableState.value = proposalState("success")
                }
                .onFailure { mutableState.value = ProviderLogic.reduce("SCR-P20", mapOf("event" to "error")) }
        }
    }

    fun selectUnableReason(index: Int) {
        if (providerReasons.getOrNull(index) == null) return
        selectedUnableReason = index
        mutableState.value = unableState("loaded")
    }

    fun updateUnableNote(value: String) {
        unableNote = value
        mutableState.value = unableState("loaded")
    }

    fun confirmUnable() {
        val order = activeOrder ?: return
        val state = unableState("loaded")
        if (!state.canContinue) {
            mutableState.value = state
            return
        }
        mutableState.value = unableState("submitting")
        viewModelScope.launch(Dispatchers.IO) {
            val path = when (requestedUnableAction) {
                "back_out" -> "back-out"
                "report_unable" -> "unable"
                "report_no_show" -> "customer-no-show"
                else -> return@launch
            }
            val body = JSONObject()
            if (requestedUnableAction != "report_no_show") {
                body.put("reason_code", providerReasons[selectedUnableReason].code)
            }
            if (requestedUnableAction == "report_unable") body.put("note", unableNote)
            runCatching { api.performOrderAction(token(), order, path, body) }
                .onSuccess {
                    activeOrder = it
                    if (it.status == "OPEN" || it.status == "CANCELLED") loadHome() else mutableState.value = activeOrderState(it)
                }
                .onFailure { mutableState.value = ProviderLogic.reduce("SCR-P21", mapOf("event" to "error")) }
        }
    }

    fun showProfile() {
        mutableState.value = profileState("validate")
    }

    fun updateProfile(experience: String? = null, about: String? = null) {
        experience?.let { experienceYears = it.filter(Char::isDigit).take(2) }
        about?.let { bio = it }
        mutableState.value = profileState("validate")
    }

    fun uploadProfilePhoto(upload: CustomerMediaUpload) = uploadImage("SCR-P02", upload) { mediaId ->
        profilePhotoMediaId = mediaId
        profilePhotoUploaded = true
        profileState("validate")
    }

    fun openIdentity() {
        if (profileState("validate").canContinue) mutableState.value = identityState("validate")
    }

    fun uploadIdentity(front: Boolean, upload: CustomerMediaUpload) = uploadImage("SCR-P03", upload) { mediaId ->
        if (front) {
            idFrontMediaId = mediaId
            idFrontUploaded = true
        } else {
            idBackMediaId = mediaId
            idBackUploaded = true
        }
        identityState("validate")
    }

    fun loadCatalog() {
        if (!identityState("validate").canContinue) return
        launch("SCR-P04") {
            categories = api.catalog()
            mutableState.value = catalogState()
        }
    }

    fun toggleCategory(index: Int) {
        val category = categories.getOrNull(index) ?: return
        if (!categoryIds.add(category.id)) {
            categoryIds.remove(category.id)
            specialtyIds.removeAll(category.specialties.map(ProviderChoice::id).toSet())
        }
        mutableState.value = catalogState()
    }

    fun toggleSpecialty(index: Int) {
        val specialty = selectedSpecialties().getOrNull(index) ?: return
        if (!specialtyIds.add(specialty.id)) specialtyIds.remove(specialty.id)
        mutableState.value = catalogState()
    }

    fun loadCities() {
        if (!catalogState().canContinue) return
        launch("SCR-P05") {
            cities = api.cities()
            selectedCityIndex = if (cities.isEmpty()) -1 else 0
            areas = cities.firstOrNull()?.let { api.areas(it.id) }.orEmpty()
            mutableState.value = areasState()
        }
    }

    fun selectCity(index: Int) {
        val city = cities.getOrNull(index) ?: return
        launch("SCR-P05") {
            selectedCityIndex = index
            areas = api.areas(city.id)
            areaIds.retainAll(areas.map(ProviderChoice::id).toSet())
            mutableState.value = areasState()
        }
    }

    fun toggleArea(index: Int) {
        val area = areas.getOrNull(index) ?: return
        if (!areaIds.add(area.id)) areaIds.remove(area.id)
        mutableState.value = areasState()
    }

    fun loadPayout() {
        if (!areasState().canContinue) return
        launch("SCR-P06") {
            payoutOptions = api.payoutOptions()
            selectedPayoutIndex = payoutOptions.indexOfFirst { it.code == application?.payoutMethod }
            mutableState.value = payoutState("validate")
        }
    }

    fun selectPayout(index: Int) {
        if (payoutOptions.getOrNull(index) == null) return
        selectedPayoutIndex = index
        mutableState.value = payoutState("validate")
    }

    fun updatePayoutDetails(value: String) {
        payoutDetails = value
        mutableState.value = payoutState("validate")
    }

    fun submit() {
        val checked = payoutState("validate")
        if (!checked.canContinue) {
            mutableState.value = checked
            return
        }
        launch("SCR-P06", "submitting") {
            val method = payoutOptions[selectedPayoutIndex]
            val body = JSONObject()
                .put("experience_years", experienceYears.toInt())
                .put("bio", bio.ifBlank { JSONObject.NULL })
                .put("category_ids", JSONArray(categoryIds.toList()))
                .put("specialty_ids", JSONArray(specialtyIds.toList()))
                .put("area_ids", JSONArray(areaIds.toList()))
                .put("payout_method", method.code)
                .put("payout_details", payoutDetails)
                .putNullable("profile_photo_media_id", profilePhotoMediaId)
                .putNullable("id_front_media_id", idFrontMediaId)
                .putNullable("id_back_media_id", idBackMediaId)
            val submitted = api.submit(token(), body)
            application = submitted
            mutableState.value = statusState(submitted)
        }
    }

    private fun profileState(event: String) = ProviderLogic.reduce(
        "SCR-P02",
        mapOf(
            "event" to event, "experience_years" to experienceYears, "bio" to bio,
            "profile_photo_uploaded" to profilePhotoUploaded,
        ),
    )

    private fun identityState(event: String) = ProviderLogic.reduce(
        "SCR-P03",
        mapOf("event" to event, "id_front_uploaded" to idFrontUploaded, "id_back_uploaded" to idBackUploaded),
    )

    private fun catalogState() = ProviderLogic.reduce(
        "SCR-P04",
        mapOf(
            "event" to "validate", "category_labels" to categories.map(ProviderCategory::label),
            "specialty_labels" to selectedSpecialties().map(ProviderChoice::label),
            "category_ids" to categoryIds.map(Int::toString), "specialty_ids" to specialtyIds.map(Int::toString),
        ),
    ).copy(
        selectedPrimaryIndices = categories.indices.filter { categories[it].id in categoryIds },
        selectedSecondaryIndices = selectedSpecialties().indices.filter { selectedSpecialties()[it].id in specialtyIds },
    )

    private fun areasState() = ProviderLogic.reduce(
        "SCR-P05",
        mapOf(
            "event" to "validate", "city_labels" to cities.map(ProviderChoice::label),
            "area_labels" to areas.map(ProviderChoice::label), "area_ids" to areaIds.map(Int::toString),
            "selected_city_index" to selectedCityIndex,
        ),
    ).copy(
        selectedPrimaryIndices = listOf(selectedCityIndex).filter { it >= 0 },
        selectedSecondaryIndices = areas.indices.filter { areas[it].id in areaIds },
    )

    private fun payoutState(event: String) = ProviderLogic.reduce(
        "SCR-P06",
        mapOf(
            "event" to event, "payout_labels" to payoutOptions.map(ProviderPayoutOption::label),
            "payout_field_labels" to payoutOptions.map(ProviderPayoutOption::fieldLabel),
            "payout_code" to payoutOptions.getOrNull(selectedPayoutIndex)?.code.orEmpty(), "payout_details" to payoutDetails,
            "available_actions" to application?.availableActions.orEmpty(),
            "summary" to listOf(categoryIds.size, specialtyIds.size, areaIds.size, 3).map(Int::toString),
        ),
    ).copy(selectedPrimaryIndices = listOf(selectedPayoutIndex).filter { it >= 0 })

    private fun statusState(payload: ProviderApplicationPayload) = ProviderLogic.reduce(
        "SCR-P07",
        mapOf(
            "event" to "loaded", "status" to payload.status,
            "display_status" to statusKey(payload.status), "reason" to payload.reason.orEmpty(),
            "available_actions" to payload.availableActions,
        ),
    )

    private fun homeState(payload: ProviderHomePayload, event: String): ProviderUiState {
        val order = payload.activeOrder
        val summary = order?.let {
            listOf(
                it.number,
                listOf(it.category, it.problemType).filter(String::isNotBlank).joinToString(" — "),
                it.area,
                timingLabel(it),
            )
        }.orEmpty()
        return ProviderLogic.reduce(
            "SCR-P08",
            mapOf(
                "event" to event,
                "available_now" to payload.availableNow,
                "active_order_id" to order?.id,
                "order_summary" to summary,
                "display_status" to order?.displayStatus.orEmpty(),
                "available_actions" to payload.availableActions,
                "unread_notifications" to payload.unreadNotifications,
                "request_titles" to availableRequests.map { "#${it.number} — ${it.category}" },
                "request_details" to availableRequests.map {
                    listOf(it.problemType, it.area, timingLabel(it)).filter(String::isNotBlank).joinToString(" · ")
                },
                "request_ids" to availableRequests.map { it.id.toString() },
            ),
        )
    }

    private fun marketRequestState(payload: ProviderMarketRequestPayload, event: String = "loaded"): ProviderUiState {
        val order = payload.order
        return ProviderLogic.reduce(
            "SCR-P09",
            mapOf(
                "event" to event,
                "customer_name" to order.customerName,
                "customer_rating" to order.customerRating,
                "display_status" to order.displayStatus,
                "order_summary" to listOf(
                    "#${order.number}",
                    listOf(order.category, order.problemType).filter(String::isNotBlank).joinToString(" — "),
                    order.description,
                    order.area,
                    timingLabel(order),
                    order.pricingModeLabel,
                    order.budgetAmount?.let(::formatAmount).orEmpty(),
                    order.materialsResponsibilityLabel,
                ),
                "media_labels" to payload.mediaLabels,
                "offer_summary" to payload.ownOfferSummary,
                "available_actions" to order.availableActions,
            ),
        )
    }

    private fun offerState(
        event: String,
        payload: ProviderMarketRequestPayload? = marketRequest,
        actions: List<String> = payload?.order?.availableActions.orEmpty(),
    ): ProviderUiState {
        val request = payload ?: return ProviderLogic.reduce("SCR-P10", mapOf("event" to "error"))
        return ProviderLogic.reduce(
            "SCR-P10",
            mapOf(
                "event" to event,
                "timing_type" to request.order.timingType,
                "pricing_mode" to request.order.pricingMode,
                "min_amount" to request.context.minimumAmount,
                "commission_rate" to request.context.commissionRate,
                "price" to offerPrice,
                "includes_text" to offerIncludes,
                "note" to offerNote,
                "eta_codes" to request.context.etaCodes,
                "eta_labels" to request.context.etaLabels,
                "selected_eta_index" to selectedOfferEta,
                "deductible_codes" to request.context.deductibleCodes,
                "deductible_labels" to request.context.deductibleLabels,
                "selected_deductible_index" to selectedOfferDeductible,
                "order_summary" to listOf("#${request.order.number}", request.order.area, timingLabel(request.order)),
                "available_actions" to actions,
            ),
        )
    }

    private fun offersState(event: String): ProviderUiState = ProviderLogic.reduce(
        "SCR-P11",
        mapOf(
            "event" to event,
            "offer_titles" to marketOffers.map(ProviderOfferPayload::title),
            "offer_details" to marketOffers.map(ProviderOfferPayload::detail),
            "offer_statuses" to marketOffers.map(ProviderOfferPayload::displayStatus),
            "offer_actions" to marketOffers.map { it.availableActions.joinToString(",") },
            "selected_offer_index" to selectedMarketOffer,
        ),
    )

    private fun openProposal(action: String) {
        val order = activeOrder ?: return
        launch("SCR-P20") {
            proposalTypes = api.proposalTypes().filter {
                if (action == "submit_execution_quote") it.code == "EXECUTION_QUOTE" else it.code != "EXECUTION_QUOTE"
            }
            selectedProposalType = if (proposalTypes.isEmpty()) -1 else 0
            proposalAmount = ""
            proposalReason = ""
            outsidePriceGuideReason = ""
            proposalPhotoMediaId = null
            mutableState.value = proposalState("loaded", order)
        }
    }

    private fun openUnable(action: String) {
        val order = activeOrder ?: return
        launch("SCR-P21") {
            providerReasons = if (action == "report_no_show") emptyList() else api.providerReasons()
            selectedUnableReason = -1
            unableNote = ""
            requestedUnableAction = action
            mutableState.value = unableState("loaded", order)
        }
    }

    private fun performSimpleAction(action: String, path: String, body: JSONObject = JSONObject()) {
        val order = activeOrder ?: return
        if (action !in order.availableActions || mutableState.value.isBusy) return
        mutableState.value = activeOrderState(order, "submitting", action)
        viewModelScope.launch(Dispatchers.IO) {
            runCatching { api.performOrderAction(token(), order, path, body) }
                .onSuccess {
                    activeOrder = it
                    if (it.status == "CLOSED" && "rate_customer" in it.availableActions) {
                        ratingStars = 0
                        mutableState.value = ratingState("loaded")
                    } else if (it.status in listOf("CLOSED", "CANCELLED", "EXPIRED", "OPEN")) {
                        loadHome()
                    } else {
                        mutableState.value = activeOrderState(it)
                    }
                }
                .onFailure { mutableState.value = ProviderLogic.reduce(screenFor(order), mapOf("event" to "error")) }
        }
    }

    private fun activeOrderState(
        order: ProviderOrderPayload,
        event: String = "loaded",
        currentAction: String = "",
    ): ProviderUiState {
        val screen = screenFor(order)
        val message = when {
            order.priceGuideReviewRequired && screen == "SCR-P14" -> "provider.price_guide.other_review"
            screen == "SCR-P15" && "submit_proposal" !in order.availableActions && "complete_work" !in order.availableActions ->
                "provider.proposal.pending"
            screen == "SCR-P16" && "confirm_cash" !in order.availableActions -> "provider.payment.waiting"
            else -> ""
        }
        return ProviderLogic.reduce(
            screen,
            mapOf(
                "event" to event,
                "current_action" to currentAction,
                "display_status" to order.displayStatus,
                "customer_name" to order.customerName,
                "customer_rating" to order.customerRating,
                "order_summary" to listOf(
                    "#${order.number}",
                    listOf(order.category, order.problemType).filter(String::isNotBlank).joinToString(" — "),
                    order.address.ifBlank { order.area },
                    timingLabel(order),
                ),
                "amount_summary" to if (screen == "SCR-P16") listOf(
                    formatAmount(order.laborTotal),
                    formatAmount(order.materialsTotal),
                    formatAmount(order.finalAmount),
                    order.paymentMethodLabel,
                ) else emptyList<String>(),
                "step_states" to order.stepStates,
                "available_actions" to order.availableActions,
                "price_guide_min" to order.priceGuideMinimum.orEmpty().substringBefore('.'),
                "price_guide_max" to order.priceGuideMaximum.orEmpty().substringBefore('.'),
                "price_review_required" to order.priceGuideReviewRequired,
                "message_key" to message,
            ),
        )
    }

    private fun proposalState(event: String, order: ProviderOrderPayload? = activeOrder): ProviderUiState {
        val value = order ?: return ProviderLogic.reduce("SCR-P20", mapOf("event" to "error"))
        return ProviderLogic.reduce(
            "SCR-P20",
            mapOf(
                "event" to event,
                "proposal_type_codes" to proposalTypes.map(ProviderReason::code),
                "proposal_type_labels" to proposalTypes.map(ProviderReason::label),
                "selected_type_index" to selectedProposalType,
                "amount" to proposalAmount,
                "reason" to proposalReason,
                "outside_reason" to outsidePriceGuideReason,
                "photo_uploaded" to (proposalPhotoMediaId != null),
                "price_guide_min" to value.priceGuideMinimum.orEmpty(),
                "price_guide_max" to value.priceGuideMaximum.orEmpty(),
                "price_review_required" to value.priceGuideReviewRequired,
                "available_actions" to value.availableActions,
            ),
        )
    }

    private fun unableState(event: String, order: ProviderOrderPayload? = activeOrder): ProviderUiState {
        val value = order ?: return ProviderLogic.reduce("SCR-P21", mapOf("event" to "error"))
        return ProviderLogic.reduce(
            "SCR-P21",
            mapOf(
                "event" to event,
                "requested_action" to requestedUnableAction,
                "reason_codes" to providerReasons.map(ProviderReason::code),
                "reason_labels" to providerReasons.map(ProviderReason::label),
                "selected_reason_index" to selectedUnableReason,
                "note" to unableNote,
                "available_actions" to value.availableActions,
            ),
        )
    }

    private fun ratingState(event: String, actions: List<String> = activeOrder?.availableActions.orEmpty()): ProviderUiState {
        val order = activeOrder
        return ProviderLogic.reduce(
            "SCR-P17",
            mapOf(
                "event" to event,
                "stars" to ratingStars,
                "order_summary" to listOf(
                    "#${order?.number.orEmpty()}", order?.customerName.orEmpty(),
                    listOf(order?.category.orEmpty(), order?.problemType.orEmpty()).filter(String::isNotBlank).joinToString(" — "),
                ),
                "available_actions" to actions,
            ),
        )
    }

    private fun workspaceProfileState(event: String): ProviderUiState {
        val profile = workspaceProfile ?: return ProviderLogic.reduce("SCR-P19", mapOf("event" to "error"))
        val specialties = categories.flatMap(ProviderCategory::specialties)
        return ProviderLogic.reduce(
            "SCR-P19",
            mapOf(
                "event" to event, "name" to profile.name, "rating" to profile.rating,
                "profile_stats" to listOf(
                    profile.rating,
                    profile.completedOrders.toString(),
                    profile.averageResponseMinutes?.toString() ?: "—",
                ),
                "profile_photo_uploaded" to profilePhotoUploaded,
                "experience_years" to experienceYears, "bio" to bio,
                "category_labels" to categories.map(ProviderCategory::label),
                "specialty_labels" to specialties.map(ProviderChoice::label),
                "specialty_ids" to workspaceSpecialtyIds.map(Int::toString),
                "area_labels" to workspaceAreas.map(ProviderChoice::label),
                "area_ids" to workspaceAreaIds.map(Int::toString),
                "portfolio_ids" to workspacePortfolio.map { it.id.toString() },
                "portfolio_labels" to workspacePortfolio.map { it.caption.ifBlank { "#${it.id}" } },
                "portfolio_caption" to portfolioCaption,
                "available_actions" to profile.availableActions,
            ),
        ).copy(
            selectedPrimaryIndices = specialties.indices.filter { specialties[it].id in workspaceSpecialtyIds },
            selectedSecondaryIndices = workspaceAreas.indices.filter { workspaceAreas[it].id in workspaceAreaIds },
        )
    }

    private fun chatState(event: String): ProviderUiState {
        val chat = chatPayload ?: return ProviderLogic.reduce("SCR-C19", mapOf("event" to "error"))
        return ProviderLogic.reduce(
            "SCR-C19",
            mapOf(
                "event" to event, "viewer_role" to "PROVIDER", "counterpart_name" to chat.customerName,
                "conversation_status" to chat.status, "order_summary" to chat.orderSummary,
                "message_bodies" to chat.bodies, "message_kinds" to chat.kinds,
                "message_times" to chat.times, "draft" to chatDraft,
            ),
        )
    }

    private fun screenFor(order: ProviderOrderPayload): String = when (order.status) {
        "CONFIRMED" -> "SCR-P12"
        "ON_THE_WAY" -> "SCR-P13"
        "ARRIVED", "AWAITING_QUOTE_APPROVAL" -> "SCR-P14"
        "IN_PROGRESS" -> "SCR-P15"
        "AWAITING_PAYMENT", "AWAITING_CONFIRMATION" -> "SCR-P16"
        "CLOSED" -> "SCR-P17"
        "DISPUTED" -> when (order.stepStates.indexOf("on_hold")) {
            2 -> "SCR-P14"
            3 -> "SCR-P15"
            in 4..5 -> "SCR-P16"
            else -> "SCR-P12"
        }
        else -> "SCR-P12"
    }

    private fun timingLabel(order: ProviderOrderPayload): String {
        if (order.timingType == "NOW") return "provider.home.timing.now"
        val value = order.slotStart ?: return ""
        return runCatching {
            OffsetDateTime.parse(value).atZoneSameInstant(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("d MMM, h:mm a", Locale.getDefault()))
        }.getOrDefault(value)
    }

    private fun formatAmount(value: String): String = "${value.substringBefore('.')} $currencyLabel"

    private fun timingLabel(order: ProviderHomeOrder): String {
        if (order.timingType == "NOW") return "provider.home.timing.now"
        val value = order.slotStart ?: return ""
        return runCatching {
            OffsetDateTime.parse(value).atZoneSameInstant(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("d MMM, h:mm a", Locale.getDefault()))
        }.getOrDefault(value)
    }

    private fun String.displayDateTime(): String = ifBlank { "" }.let { value ->
        runCatching { OffsetDateTime.parse(value).atZoneSameInstant(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("yyyy-MM-dd · h:mm a")) }.getOrDefault(value)
    }

    private fun selectedSpecialties(): List<ProviderChoice> = categories.filter { it.id in categoryIds }.flatMap(ProviderCategory::specialties)

    private fun hydrate(payload: ProviderApplicationPayload) {
        experienceYears = payload.experienceYears?.toString().orEmpty()
        bio = payload.bio
        categoryIds.apply { clear(); addAll(payload.categoryIds) }
        specialtyIds.apply { clear(); addAll(payload.specialtyIds) }
        areaIds.apply { clear(); addAll(payload.areaIds) }
        payoutDetails = payload.payoutDetails
        profilePhotoUploaded = payload.profilePhotoUploaded
        idFrontUploaded = payload.idFrontUploaded
        idBackUploaded = payload.idBackUploaded
    }

    private fun uploadImage(screen: String, upload: CustomerMediaUpload, onUploaded: (Int) -> ProviderUiState) {
        launch(screen, "uploading") {
            mutableState.value = onUploaded(api.uploadImage(token(), upload))
        }
    }

    private fun launch(screen: String, loadingEvent: String = "loading", block: () -> Unit) {
        mutableState.value = when (screen) {
            "SCR-P02" -> profileState(loadingEvent)
            "SCR-P03" -> identityState(loadingEvent)
            "SCR-P06" -> payoutState(loadingEvent)
            else -> ProviderLogic.reduce(screen, mapOf("event" to "loading"))
        }
        viewModelScope.launch(Dispatchers.IO) {
            runCatching(block).onFailure { error ->
                val event = if (error is ProviderApiException && error.code == "APPLICATION_NOT_EDITABLE") "not_editable" else "error"
                mutableState.value = ProviderLogic.reduce(screen, mapOf("event" to event))
            }
        }
    }

    private fun token(): String = checkNotNull(session.token)

    private fun statusKey(status: String): String = when (status) {
        "PENDING_REVIEW" -> "provider.application.pending"
        "REJECTED" -> "provider.application.rejected"
        "ACTIVE" -> "provider.application.active"
        "SUSPENDED" -> "provider.application.suspended"
        else -> "provider.application.pending"
    }
}

private fun JSONObject.putNullable(key: String, value: Int?): JSONObject = if (value == null) this else put(key, value)

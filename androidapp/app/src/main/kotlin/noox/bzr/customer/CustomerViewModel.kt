package noox.bzr.customer

import android.os.Handler
import android.os.Looper
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import noox.bzr.auth.AuthSessionStore
import noox.bzr.links.DeepLinkTarget
import noox.bzr.links.PushDevice
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import noox.bzr.network.logFailure

class CustomerViewModel(
    private val api: CustomerApi,
    private val session: AuthSessionStore,
    private var currencyLabel: String,
    private val push: PushDevice = PushDevice.None,
) : ViewModel() {
    // DEC-047: only the state holder changed (Compose mutableStateOf → StateFlow); fragments collect [stateFlow].
    private val mutableState = MutableStateFlow(CustomerUiState("SCR-C01", CustomerPhase.Loading))
    val stateFlow: StateFlow<CustomerUiState> = mutableState.asStateFlow()
    var hasStarted: Boolean = false
        private set
    var state: CustomerUiState
        get() = mutableState.value
        private set(value) {
            hasStarted = true
            // Loading is transient; an error screen is shown but not returned to.
            if (value.phase != CustomerPhase.Loading) history.show(value, restorable = value.phase != CustomerPhase.Error)
            mutableState.value = value
        }

    /** The back stack for the top-bar and system back (6ب, `design/fixtures/navigation/history.json`). */
    private val history = ScreenHistory<CustomerUiState>("SCR-C01") { it.screen }

    /** Back: the recorded screen as it was shown, home when nothing is recorded; false on home (the system may leave). */
    fun goBack(): Boolean {
        val target = history.back { CustomerUiState("SCR-C01", CustomerPhase.Loading) } ?: return false
        when (target.screen) {
            "SCR-C04" -> openTiming()
            "SCR-C01" -> loadHome()
            "SCR-C18" -> loadConversations()
            "SCR-C19" -> currentConversationId?.let { loadConversation(it, showLoading = true) } ?: loadConversations()
            else -> {
                stopChatPolling()
                mutableState.value = target
            }
        }
        return true
    }
    private val mutableRequiresAuthentication = MutableStateFlow(false)
    val requiresAuthenticationFlow: StateFlow<Boolean> = mutableRequiresAuthentication.asStateFlow()
    var requiresAuthentication: Boolean
        get() = mutableRequiresAuthentication.value
        private set(value) {
            mutableRequiresAuthentication.value = value
        }
    private var currentOrderId: Int? = null
    private var sharingVisit = false

    fun shareVisit(onReady: (String) -> Unit) {
        val orderId = currentOrderId ?: return
        if (sharingVisit || "share_visit" !in state.visibleActions) return
        sharingVisit = true
        state = state.copy(isBusy = true, messageKey = null)
        Thread {
            val result = runCatching { api.shareVisit(token(), orderId) }
            mainHandler.post {
                sharingVisit = false
                if (currentOrderId != orderId || state.screen != "SCR-C09") return@post
                state = state.copy(isBusy = false, messageKey = if (result.isFailure) "share.error" else null)
                result.getOrNull()?.let { onReady(it.url) }
            }
        }.start()
    }
    private var currentOrderVersion: Int = 0
    private var currentProposalId: Int? = null
    private var currentOrderPayload: CustomerOrder? = null
    private var instapay: InstapayDetails? = null
    private var transferReference = ""
    private var receiptMediaId: Int? = null
    private val ratingMediaIds = mutableListOf<Int>()
    private var selectedPaymentChannel = -1
    private var ratingValues = mutableListOf(0, 0, 0)
    private var ratingComment = ""
    /** DEC-076 (Figma 3002:4475) selected review tags by index → stable server code; stored separately from the comment. */
    private val ratingTags = sortedMapOf<Int, String>()
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
    private var addressPayloads: List<CustomerAddress> = emptyList()
    private var areaIds: List<Int> = emptyList()
    private var selectedAddressIndex: Int = -1
    private var addressSelectionMode: Boolean = true
    private var dayDates: List<String> = emptyList()
    private var slotStarts: List<String> = emptyList()
    private var selectedSlotIndex: Int = -1
    private var timingType: String = ""
    private var slotStart: String? = null
    private val media = mutableListOf<MediaDraft>()
    private var mediaRecording = false
    private var mediaSelectionError = ""
    private var addressDraft = AddressDraft()
    private val mainHandler = Handler(Looper.getMainLooper())
    private var chatPoll: Runnable? = null
    private var conversationIds: List<Int> = emptyList()
    private var currentConversationId: Int? = null
    private var currentConversationPayload: CustomerConversation? = null
    private var chatMessages: List<CustomerMessage> = emptyList()
    private var chatDraft = ""
    private var arrivalCodePayload: ArrivalCode? = null
    private var arrivalCodeFailed = false
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
    private var ratingRemindersEnabled = true
    /** From `GET /config` through home: marketplace or staff (39). Never assumed. */
    private var operatingMode = "staff"
    private var addressLabel = ""
    private var catalog: List<CustomerCategory> = emptyList()
    private var categoryAvailable = true
    private var requestDescription = ""
    /** C03 search (DEC-071): filters the catalog locally by category and problem-type names. */
    private var serviceQuery = ""
    private var currentOffers: List<CustomerOffer> = emptyList()
    private var currentOfferId: Int? = null
    private var currentOfferProviderId: Int? = null
    /** C06 sort chip: 0 rating, 1 price, 2 eta, 3 backend sort options, with best match as the configured default. */
    private var offersSortIndex = 0
    private var selectedMaterialIndex = -1
    private var selectedPricingIndex = -1
    private var requestBudget = ""
    private var serviceHoursFrom = "00:00"
    private var serviceHoursTo = "23:59"
    private var selectedSlotLabel = ""

    /** C32 rows open through the activity, which knows the provider status and can switch modes (DEC-058). */
    var onDeepLink: (String) -> Unit = {}
    private var editingOrderId: Int? = null

    private var language = noox.bzr.network.BremoHttp.language()

    /** Re-fetch display labels while keeping the customer's service draft and stable selections. */
    fun refreshLanguage(currency: String) {
        val nextLanguage = noox.bzr.network.BremoHttp.language()
        if (language == nextLanguage) return
        language = nextLanguage
        currencyLabel = currency
        stopChatPolling()
        history.clear()
        if (session.token == null) return
        val screen = state.screen
        val materialCode = options("materials_responsibilities").getOrNull(selectedMaterialIndex)?.code
        val pricingCode = options("pricing_modes").getOrNull(selectedPricingIndex)?.code
        requestRouted(screen) {
            val home = api.home(token())
            catalog = home.categories
            optionLists = home.optionLists
            optionDefaults = home.optionDefaults
            selectedSlotLabel = slotStart?.displayDateTime().orEmpty()
            selectedMaterialIndex = options("materials_responsibilities").indexOfFirst { it.code == materialCode }
            selectedPricingIndex = options("pricing_modes").indexOfFirst { it.code == pricingCode }
            when (screen) {
                "SCR-C03" -> screen to serviceInput()
                "SCR-C37" -> screen to problemInput()
                "SCR-C38" -> screen to budgetInput()
                "SCR-C04" -> screen to timingInput()
                "SCR-C05" -> screen to reviewInput(false)
                else -> {
                    mainHandler.post {
                        if (nextLanguage == noox.bzr.network.BremoHttp.language()) {
                            when (screen) {
                                "SCR-C32" -> loadNotifications()
                                "SCR-C34" -> loadTerms()
                                "SCR-C29" -> loadHelp()
                                "SCR-C33" -> loadAccount()
                                "SCR-C02" -> loadAccountSummary()
                                "SCR-C18" -> loadConversations()
                                "SCR-C19" -> currentConversationId?.let { loadConversation(it, showLoading = true) } ?: loadConversations()
                                "SCR-C25" -> loadOrders()
                                "SCR-C14" -> loadAddresses(addressSelectionMode)
                                "SCR-C15" -> state = reduce(screen, addressStateInput("validate"))
                                "SCR-C16", "SCR-C17" -> openTiming()
                                else -> currentOrderId?.let(::loadOrder) ?: loadHome()
                            }
                        }
                    }
                    "" to emptyMap()
                }
            }
        }
    }

    fun loadHome() = request("SCR-C01") {
        val home = api.home(token())
        // Offers only exist for open orders; a failed side call leaves the card without figures, never blocks home.
        val offerFigures = home.orders.mapIndexed { index, order ->
            if (index > 0 || order.status != "OPEN") return@mapIndexed 0 to ""
            val offers = runCatching { api.offers(token(), order.id) }.getOrDefault(emptyList())
            offers.size to (offers.mapNotNull { it.price.toBigDecimalOrNull() }.minOrNull()?.stripTrailingZeros()?.toPlainString().orEmpty())
        }
        val unread = runCatching { api.notifications(token()).count { it.readAt == null } }.getOrDefault(0)
        currentOrderId = home.firstOrderId
        orderIds = home.orders.map(CustomerOrder::id)
        orderStatuses = home.orders.map(CustomerOrder::status)
        addressId = home.addressId
        categoryId = null
        problemTypeId = null
        catalog = home.categories
        optionLists = home.optionLists
        optionDefaults = home.optionDefaults
        timingType = optionDefaults["timing_type"].orEmpty()
        selectedMaterialIndex = options("materials_responsibilities").indexOfFirst {
            it.code == optionDefaults["materials_responsibility"]
        }
        selectedPricingIndex = -1
        requestBudget = ""
        serviceHoursFrom = home.serviceHoursFrom
        serviceHoursTo = home.serviceHoursTo
        operatingMode = home.operatingMode
        instapay = home.instapay
        addressLabel = home.addressLabel
        home.addressCityId?.let { cityId = it }
        mapOf(
            "event" to "loaded", "operating_mode" to home.operatingMode, "has_orders" to (home.orderCount > 0),
            "customer_name" to home.customerName, "address_label" to home.addressLabel,
            "category_labels" to catalog.map(CustomerCategory::name),
            "category_icons" to catalog.map { it.iconKey.orEmpty() },
            "selected_category_index" to -1,
            // DEC-073 (Figma home): the current-request card titles the problem, says how long ago it was published
            // and, while open, how many offers arrived and the lowest — all from the API, nothing invented.
            "order_titles" to home.orders.map { it.problemType.name.orEmpty().ifBlank { orderSummary(it)[0] } },
            "order_subtitles" to home.orders.map { orderSummary(it)[1] },
            "order_statuses" to home.orders.map { orderSummary(it)[2] },
            "order_status_codes" to home.orders.map(CustomerOrder::status),
            "order_icons" to home.orders.map { order -> catalog.firstOrNull { it.id == order.category.id }?.iconKey.orEmpty() },
            "order_age_minutes" to home.orders.map { minutesSince(it.createdAt).toString() },
            "order_offer_counts" to offerFigures.map { it.first.toString() },
            "order_lowest_offers" to offerFigures.map { it.second },
            "unread_notifications" to unread,
        )
    }

    /** C03, step 1 of 5 (DEC-071): the service. */
    fun openProblem() {
        state = reduce("SCR-C03", serviceInput())
    }

    /** C37, step 2 of 5: problem type, description and media. */
    fun openProblemDetails() {
        if (categoryId == null) return
        state = reduce("SCR-C37", problemInput())
    }

    fun searchServices(query: String) {
        serviceQuery = query.take(60)
        state = reduce("SCR-C03", serviceInput())
    }

    /** The catalog as C03 shows it: every category whose name or problem types contain the query. */
    private fun visibleCategories(): List<CustomerCategory> {
        val query = serviceQuery.trim()
        if (query.isEmpty()) return catalog
        return catalog.filter { category ->
            category.name.contains(query, ignoreCase = true) ||
                category.problemTypes.any { it.name.contains(query, ignoreCase = true) }
        }
    }

    private fun serviceInput(): Map<String, Any?> {
        val visible = visibleCategories()
        return mapOf(
            "event" to "validate",
            "category_labels" to visible.map(CustomerCategory::name),
            "category_icons" to visible.map { it.iconKey.orEmpty() },
            // DEC-071: the line under each service is its first three problem types from /catalog, one per line;
            // the view joins them with the localized list separator.
            "category_details" to visible.map { category ->
                category.problemTypes.filterNot(CustomerProblemType::isOther).take(3).joinToString("\n", transform = CustomerProblemType::name)
            },
            "selected_category_index" to visible.indexOfFirst { it.id == categoryId },
            "search_query" to serviceQuery,
        )
    }

    /** C01 urgent card (DEC-063): the same request flow, with "now" chosen for the timing step. */
    fun openUrgentRequest() {
        if (options("timing_types").any { it.code == URGENT_TIMING }) timingType = URGENT_TIMING
        openProblem()
    }

    /** [index] is a position in the list C03 (or C01, unfiltered) is showing. */
    fun selectCategory(index: Int) {
        val visible = if (state.screen == "SCR-C03") visibleCategories() else catalog.also { serviceQuery = "" }
        val choice = CustomerCatalogSelection.category(
            categoryIds = visible.map(CustomerCategory::id),
            problemIdsByCategory = visible.map { category -> category.problemTypes.map(CustomerProblemType::id) },
            selectedIndex = index,
            currentProblemId = problemTypeId,
        ) ?: return
        categoryId = choice.categoryId
        problemTypeId = choice.problemTypeId
        state = reduce("SCR-C03", serviceInput())
    }

    fun selectProblem(index: Int) {
        val choice = CustomerCatalogSelection.problem(
            categoryId = categoryId,
            problemIds = selectedCategory()?.problemTypes.orEmpty().map(CustomerProblemType::id),
            selectedIndex = index,
        ) ?: return
        categoryId = choice.categoryId
        problemTypeId = choice.problemTypeId
        state = reduce("SCR-C37", problemInput())
    }

    fun updateProblemDescription(value: String) {
        requestDescription = value.take(1001)
        state = reduce("SCR-C37", problemInput())
    }

    private fun selectedCategory(): CustomerCategory? = catalog.firstOrNull { it.id == categoryId }

    private fun selectedProblem(): CustomerProblemType? =
        selectedCategory()?.problemTypes?.firstOrNull { it.id == problemTypeId }

    private fun problemInput(): Map<String, Any?> {
        val problems = selectedCategory()?.problemTypes.orEmpty()
        return mapOf(
            "event" to "validate",
            "service_label" to selectedCategory()?.name.orEmpty(),
            "service_icon" to selectedCategory()?.iconKey.orEmpty(),
            "problem_labels" to problems.map(CustomerProblemType::name),
            "problem_other" to problems.map { it.isOther.toString() },
            "selected_problem_index" to problems.indexOfFirst { it.id == problemTypeId },
            "description" to requestDescription,
            "media_count" to media.count { it.id != null },
            "media_kinds" to media.filter { it.id != null }.map(MediaDraft::kind),
        )
    }

    /** C01 current-order card: the same title, subtitle and status as the C25 row. */
    private fun orderSummary(order: CustomerOrder): List<String> = listOf(
        "${order.category.name.orEmpty()} — ${order.problemType.name.orEmpty()}",
        "#${order.number} · ${order.location.area.orEmpty()} · ${order.createdAt.orEmpty().displayDateTime()}",
        order.statusLabel.ifBlank { order.displayStatus },
    )

    /** C02 header from `GET /me`, not sample text. */
    fun loadAccountSummary() = request("SCR-C02") {
        val user = api.account(token())
        mapOf(
            "event" to "loaded", "is_verified" to user.isVerified,
            "name" to user.name, "phone" to user.phone.orEmpty(),
            "completed_orders" to user.orderStatistics?.completed?.toString().orEmpty(), "active_orders" to user.orderStatistics?.active?.toString().orEmpty(), "address_count" to user.addressesCount?.toString().orEmpty(),
        )
    }

    fun show(screen: String, input: Map<String, Any?>) {
        if (screen != "SCR-C19") stopChatPolling()
        state = reduce(screen, input)
    }

    /** A new session after logout or expiry: the customer screens may show again. */
    fun signedIn() {
        requiresAuthentication = false
    }

    /** DEC-065: the token is already refused, so only the local session goes; nothing is sent to the server. */
    fun sessionExpired() {
        stopChatPolling()
        session.clear()
        requiresAuthentication = true
    }

    /** AC-NTF-07: the device token goes first, then the Sanctum token; the session clears at once either way. */
    fun logout() {
        val auth = session.token
        val device = push.token
        session.clear()
        requiresAuthentication = true
        if (auth == null) return
        Thread {
            device?.let { runCatching { api.unregisterDevice(auth, it) } }
            runCatching { api.logout(auth) }
        }.start()
    }

    /** DEC-058: after sign-in and whenever Firebase rotates the token. Failures retry on the next launch. */
    fun registerPushDevice(deviceToken: String? = push.token) {
        val auth = session.token ?: return
        val device = deviceToken ?: return
        Thread { runCatching { api.registerDevice(auth, device) } }.start()
    }

    fun persistLanguage() {
        val auth = session.token ?: return
        val language = noox.bzr.network.BremoHttp.language()
        Thread { runCatching { api.updateLocale(auth, language) } }.start()
        registerPushDevice()
    }

    /** 17 §الروابط العميقة — customer targets only; the activity routes provider ones. */
    fun openDeepLink(target: DeepLinkTarget) {
        when (target.target) {
            "ORDER" -> loadOrder(target.orderId)
            "SCR-C06" -> loadOffers(target.orderId)
            "SCR-C19" -> openChatForOrder(target.orderId)
            "SCR-C24" -> openRatingFor(target.orderId)
            "SCR-C27" -> openDisputeFor(target.orderId)
            else -> loadNotifications()
        }
    }

    /** DEC-076: C09's chat button opens the order's conversation (or reloads the order when none exists yet). */
    fun openCurrentOrderChat() {
        currentOrderId?.let(::openChatForOrder)
    }

    /** Direct phone contact is handled by administration; customer/provider communication uses messages. */
    fun currentProviderPhone(): String? = null

    private fun openChatForOrder(orderId: Int) = Thread {
        val conversation = runCatching {
            api.conversations(token(), 1).data.firstOrNull { it.order.id == orderId }
        }.getOrNull()
        mainHandler.post {
            if (conversation == null) {
                loadOrder(orderId)
            } else {
                currentConversationId = conversation.id
                chatDraft = ""
                loadConversation(conversation.id, showLoading = true)
            }
        }
    }.start()

    private fun openRatingFor(orderId: Int) {
        currentOrderId = orderId
        openRating()
    }

    private fun openDisputeFor(orderId: Int) {
        currentOrderId = orderId
        openDispute()
    }

    fun loadHelp() = request("SCR-C29") {
        val faqs = api.faqs()
        faqQuestions = faqs.map(FaqItem::title)
        faqAnswers = faqs.map(FaqItem::body)
        helpInput("loaded")
    }

    fun toggleFaq(index: Int) {
        expandedFaqIndex = if (expandedFaqIndex == index) -1 else index
        state = reduce("SCR-C29", helpInput("loaded"))
    }

    fun updateHelpField(index: Int, value: String) {
        if (index == 0) helpSubject = value.take(120) else helpMessage = value.take(2000)
        state = reduce("SCR-C29", helpInput("loaded"))
    }

    fun submitHelpMessage() {
        if (!state.canContinue) return
        state = reduce("SCR-C29", helpInput("submitting"))
        Thread {
            val success = runCatching { api.sendSupportMessage(token(), helpSubject.trim(), helpMessage.trim()) }.isSuccess
            mainHandler.post { state = reduce("SCR-C29", helpInput(if (success) "success" else "error")) }
        }.start()
    }

    fun loadNotifications() = request("SCR-C32") {
        val values = api.notifications(token())
        notificationIds = values.map(NotificationItem::id)
        notificationLinks = values.map { it.deepLink.orEmpty() }
        notificationInput(values, "loaded")
    }

    fun openNotification(index: Int) {
        val id = notificationIds.getOrNull(index) ?: return
        state = state.copy(isBusy = true)
        Thread {
            runCatching { api.markNotificationsRead(token(), listOf(id)) }
            val link = notificationLinks.getOrNull(index).orEmpty()
            mainHandler.post { if (link.isBlank()) loadNotifications() else onDeepLink(link) }
        }.start()
    }

    fun loadAccount() = request("SCR-C33") {
        val user = api.account(token())
        accountValues = mutableListOf(user.name, user.email, user.phone.orEmpty())
        accountActions = user.availableActions
        ratingRemindersEnabled = user.ratingRemindersEnabled
        accountMode = "overview"
        accountInput("loaded")
    }

    /** NTF-18 only; saved at once, and reverted when the server refuses (C33). */
    fun setRatingReminders(enabled: Boolean) {
        val previous = ratingRemindersEnabled
        ratingRemindersEnabled = enabled
        state = reduce("SCR-C33", accountInput("loaded"))
        Thread {
            val saved = runCatching { api.updateRatingReminders(token(), enabled).ratingRemindersEnabled }
            mainHandler.post {
                ratingRemindersEnabled = saved.getOrDefault(previous)
                if (state.screen == "SCR-C33") state = reduce("SCR-C33", accountInput("loaded"))
            }
        }.start()
    }

    fun accountAction(action: String) {
        when (action) {
            "update_account" -> if (accountMode == "edit") submitAccountUpdate() else {
                accountMode = "edit"
                state = reduce("SCR-C33", accountInput("loaded"))
            }
            "change_password" -> if (accountMode == "password") submitPasswordChange() else {
                accountMode = "password"
                accountValues = mutableListOf("", "", "")
                state = reduce("SCR-C33", accountInput("loaded"))
            }
            "delete_account" -> state = reduce("SCR-C33", accountInput("confirm_delete", "delete"))
            "dismiss_delete" -> state = reduce("SCR-C33", accountInput("loaded", "overview"))
            "confirm_delete_account" -> submitDeleteAccount()
            "open_terms" -> loadTerms()
            "logout" -> logout()
        }
    }

    fun updateAccountField(index: Int, value: String) {
        if (index !in accountValues.indices) return
        accountValues[index] = value
        state = reduce("SCR-C33", accountInput("loaded"))
    }

    fun loadTerms() = request("SCR-C34") {
        val terms = api.terms()
        mapOf(
            "event" to "loaded", "body" to terms.displayBody,
            "version" to terms.version.toString(), "effective_at" to terms.effectiveAt.orEmpty().displayDateTime(),
        )
    }

    fun openNoOffers(actions: List<String>) {
        state = reduce("SCR-C35", mapOf("event" to "loaded", "available_actions" to actions))
    }

    fun noOffersAction(action: String) {
        when (action) {
            "edit_request" -> {
                val order = currentOrderPayload ?: return
                editingOrderId = currentOrderId
                addressId = order.customerAddressId?.takeIf { it > 0 }
                categoryId = order.category.id
                problemTypeId = order.problemType.id
                requestDescription = order.description.orEmpty()
                timingType = order.timing.type
                slotStart = order.timing.slotStart
                selectedMaterialIndex = options("materials_responsibilities").indexOfFirst {
                    it.code == order.materialsResponsibility
                }
                selectedPricingIndex = options("pricing_modes").indexOfFirst {
                    it.code == order.pricingMode
                }
                requestBudget = order.budgetAmount.orEmpty()
                state = reduce("SCR-C03", serviceInput())
            }
            "republish" -> {
                val orderId = currentOrderId ?: return
                state = reduce("SCR-C35", mapOf("event" to "submitting", "available_actions" to state.visibleActions))
                Thread {
                    val order = runCatching { api.republish(token(), orderId) }.getOrNull()
                    mainHandler.post {
                        if (order == null) state = reduce("SCR-C35", mapOf("event" to "error"))
                        else {
                            currentOrderId = order.id
                            currentOrderPayload = order
                            loadOrder(currentOrderId!!)
                        }
                    }
                }.start()
            }
        }
    }

    fun loadConversations(page: Int = 1) = request("SCR-C18") {
        val conversations = api.conversations(token(), page).data
        conversationIds = conversations.map(CustomerConversation::id)
        mapOf(
            "event" to "loaded",
            "provider_names" to conversations.map { it.provider.name.orEmpty() },
            // DEC-076 (Figma 3002:2924): "category • status", without the order number.
            "order_summaries" to conversations.map { it.order.summary() },
            "last_message_mine" to conversations.map { if (it.lastMessage?.isMine == true) "mine" else "" },
            "conversation_statuses" to conversations.map(CustomerConversation::status),
            "last_messages" to conversations.map { it.lastMessage?.body.orEmpty() },
            "last_message_times" to conversations.map { it.lastMessage?.createdAt.orEmpty().displayDateTime() },
            "provider_verified" to conversations.map { if (it.provider.isVerified) "verified" else "" },
            "unread_counts" to conversations.map { it.unreadCount.toString() },
            "providers_available" to conversations.map { it.provider.availableNow.toString() },
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
        state = reduce("SCR-C19", chatInput("loaded"))
    }

    fun sendChatMessage() {
        val id = currentConversationId ?: return
        val body = chatDraft.trim()
        if (body.isBlank() || !state.canContinue) return
        state = reduce("SCR-C19", chatInput("sending"))
        Thread {
            val success = runCatching { api.sendMessage(token(), id, body) }.isSuccess
            mainHandler.post {
                if (success) {
                    chatDraft = ""
                    loadConversation(id, showLoading = false)
                } else {
                    state = reduce("SCR-C19", mapOf("event" to "error"))
                }
            }
        }.start()
    }

    fun sendChatImage(upload: CustomerMediaUpload) {
        val id = currentConversationId ?: return
        state = reduce("SCR-C19", chatInput("sending"))
        Thread {
            val success = runCatching {
                val mediaId = api.uploadMedia(token(), upload).id
                api.sendMessage(token(), id, "", listOf(mediaId))
            }.isSuccess
            mainHandler.post {
                if (success) loadConversation(id, showLoading = false)
                else state = reduce("SCR-C19", mapOf("event" to "error"))
            }
        }.start()
    }

    fun loadOrders(tabIndex: Int = 0, page: Int = 1) = request("SCR-C25") {
        // DEC-076 (Figma 3002:3889): «الملغاة» is the past scope narrowed to cancelled and expired orders, «السابقة» the rest.
        ordersTab = when (tabIndex) { 1 -> "past"; 2 -> "cancelled"; else -> "current" }
        val ended = setOf("CANCELLED", "EXPIRED")
        val orders = api.orders(token(), if (ordersTab == "current") "current" else "past", page).data.filter {
            when (ordersTab) { "past" -> it.status !in ended; "cancelled" -> it.status in ended; else -> true }
        }
        orderIds = orders.map(CustomerOrder::id)
        orderStatuses = orders.map(CustomerOrder::status)
        // Offers only exist for open orders; a failed side call leaves the card without figures.
        val offerFigures = orders.map { order ->
            if (order.status != "OPEN") return@map 0 to ""
            val offers = runCatching { api.offers(token(), order.id) }.getOrDefault(emptyList())
            offers.size to (offers.mapNotNull { it.price.toBigDecimalOrNull() }.minOrNull()?.stripTrailingZeros()?.toPlainString().orEmpty())
        }
        mapOf(
            "event" to "loaded", "tab" to ordersTab,
            "order_titles" to orders.map { it.problemType.name.orEmpty().ifBlank { it.category.name.orEmpty() } },
            "order_subtitles" to orders.map {
                "#${it.number} · ${it.location.area.orEmpty()} · ${it.createdAt.orEmpty().displayDateTime()}"
            },
            "order_statuses" to orders.map(CustomerOrder::statusLabel),
            "order_status_codes" to orders.map(CustomerOrder::status),
            "order_icons" to orders.map { order -> catalog.firstOrNull { it.id == order.category.id }?.iconKey.orEmpty() },
            "order_rows" to orders.map { order ->
                listOf(
                    order.number.toString(),
                    listOf(order.location.area.orEmpty(), order.location.city.orEmpty()).filter(String::isNotBlank).joinToString(" — "),
                    order.provider?.name.orEmpty(), if (order.provider?.isVerified == true) "verified" else "",
                    order.provider?.ratingAvg.orEmpty(),
                    plainAmount(order.amounts.finalAmount ?: order.amounts.laborTotal).takeIf { order.provider != null }.orEmpty(),
                    // The conversation opens when a technician is assigned (B-21: none before acceptance).
                    if (order.provider != null) "chat" else "",
                    "",
                ).joinToString("|") { it.replace('|', ' ') }
            },
            "order_offer_counts" to offerFigures.map { it.first.toString() },
            "order_lowest_offers" to offerFigures.map { it.second },
        )
    }

    /** C25 card chat button (Figma 3002:3889): the conversation of that order, else the order itself. */
    fun openOrderChat(index: Int) {
        orderIds.getOrNull(index)?.let(::openChatForOrder)
    }

    fun selectOrder(index: Int) {
        val id = orderIds.getOrNull(index) ?: return
        if (orderStatuses.getOrNull(index) in setOf("CLOSED", "CANCELLED", "EXPIRED")) loadOrderHistory(id) else loadOrder(id)
    }

    fun loadOrderHistory(orderId: Int) = request("SCR-C26") {
        val order = api.order(token(), orderId)
        val proposals = api.proposals(token(), orderId).filter { it.status == "APPROVED" }
        currentOrderId = orderId
        currentOrderPayload = order
        currentOrderVersion = order.version
        orderHistoryInput(order, proposals)
    }

    fun loadAddresses(selectionMode: Boolean = true) = request("SCR-C14") {
        addressSelectionMode = selectionMode
        addressPayloads = api.addresses(token())
        addressIds = addressPayloads.map(CustomerAddress::id)
        selectedAddressIndex = addressPayloads.indexOfFirst(CustomerAddress::isDefault)
        addressPayloads.firstOrNull(CustomerAddress::isDefault)?.let {
            cityId = it.city.id
            addressId = it.id
        }
        mapOf(
            "event" to "loaded", "selection_mode" to selectionMode,
            "address_labels" to addressPayloads.map(CustomerAddress::label),
            "address_details" to addressPayloads.map { it.detail() },
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
        val address = addressPayloads.getOrNull(index)
        address?.let { addressLabel = addressLabel(it) }
        val city = address?.city?.id?.takeIf { it > 0 }
        val category = categoryId
        if (city == null || category == null) {
            categoryAvailable = true
            openTiming()
            return
        }
        // BR-011: the chosen service must be served in the new address's city, before publish is refused.
        Thread {
            val available = runCatching { category in api.catalogCategoryIds(city) }.getOrDefault(true)
            mainHandler.post {
                categoryAvailable = available
                openTiming()
            }
        }.start()
    }

    /** C04 from the real draft: mode, address and timing (07). */
    private var serverNowAvailable = false
    private var availabilityLoading = false
    private var availabilityFailed = false
    private var availabilityGeneration = 0

    fun openTiming() {
        val generation = ++availabilityGeneration
        val address = addressId
        val category = categoryId
        serverNowAvailable = false
        availabilityFailed = false
        availabilityLoading = address != null && category != null
        state = reduce("SCR-C04", timingInput())
        if (address == null || category == null) return
        Thread {
            val result = runCatching { api.requestAvailability(token(), address, category) }
            mainHandler.post {
                if (generation != availabilityGeneration || addressId != address || categoryId != category) return@post
                availabilityLoading = false
                availabilityFailed = result.isFailure
                serverNowAvailable = result.getOrDefault(false)
                if (state.screen == "SCR-C04") state = reduce("SCR-C04", timingInput())
            }
        }.start()
    }

    fun selectTiming(index: Int) {
        val selected = options("timing_types").getOrNull(index) ?: return
        if (selected.code == "NOW" && !serverNowAvailable) return
        timingType = selected.code
        if (index == 0) {
            slotStart = null
            selectedSlotLabel = ""
            state = reduce("SCR-C04", timingInput())
        }
    }

    /** C38, step 4 of 5 (DEC-071): materials, and in marketplace the pricing mode and budget. */
    fun openBudget() {
        if (operatingMode == "staff") { openTiming(); return }
        state = reduce("SCR-C38", budgetInput())
    }

    fun selectMaterial(index: Int) {
        if (options("materials_responsibilities").getOrNull(index) == null) return
        selectedMaterialIndex = index
        state = if (operatingMode == "staff") reduce("SCR-C04", timingInput()) else reduce("SCR-C38", budgetInput())
    }

    fun selectPricing(index: Int) {
        if (options("pricing_modes").getOrNull(index) == null) return
        selectedPricingIndex = index
        state = reduce("SCR-C38", budgetInput())
    }

    fun updateBudget(value: String) {
        requestBudget = value.take(12)
        state = reduce("SCR-C38", budgetInput())
    }

    private fun budgetInput(): Map<String, Any?> = mapOf(
        "event" to "validate", "operating_mode" to operatingMode,
        "material_labels" to options("materials_responsibilities").map(CustomerOption::label),
        "material_codes" to options("materials_responsibilities").map(CustomerOption::code),
        "selected_material_index" to selectedMaterialIndex,
        "pricing_labels" to options("pricing_modes").map(CustomerOption::label),
        "pricing_codes" to options("pricing_modes").map(CustomerOption::code),
        "selected_pricing_index" to selectedPricingIndex, "budget" to requestBudget,
        "budget_amounts" to selectedProblem()?.customerPriceGuide?.suggestedAmounts.orEmpty(),
    )

    /** C05 before the terms are accepted; the mode decides pricing copy. */
    fun openReview() {
        state = reduce("SCR-C05", reviewInput(false))
    }

    fun setTermsAccepted(accepted: Boolean) {
        state = reduce("SCR-C05", reviewInput(accepted))
    }

    private fun reviewInput(termsAccepted: Boolean): Map<String, Any?> {
        val category = selectedCategory()
        val problem = selectedProblem()
        val serviceRows = listOfNotNull(
            if (category != null && problem != null) "${category.name} — ${problem.name}" else null,
            requestDescription.takeIf(String::isNotBlank),
        )
        val visitRows = listOfNotNull(
            addressLabel.takeIf(String::isNotBlank),
            selectedSlotLabel.takeIf(String::isNotBlank)
                ?: options("timing_types").firstOrNull { it.code == timingType }?.label,
            options("materials_responsibilities").getOrNull(selectedMaterialIndex)?.label,
        )
        val pricingRows = if (operatingMode == "marketplace") listOfNotNull(
            options("pricing_modes").getOrNull(selectedPricingIndex)?.label,
            requestBudget.takeIf(String::isNotBlank),
        ) else emptyList()
        return mapOf(
            "event" to "validate", "operating_mode" to operatingMode,
            "terms_accepted" to termsAccepted, "service_rows" to serviceRows,
            "visit_rows" to visitRows, "pricing_rows" to pricingRows,
        )
    }

    private fun timingInput(): Map<String, Any?> = mapOf(
        "event" to "validate", "operating_mode" to operatingMode,
        "timing_type" to timingType.lowercase(), "now_available" to serverNowAvailable,
        "availability_loading" to availabilityLoading, "availability_failed" to availabilityFailed,
        "material_labels" to options("materials_responsibilities").map(CustomerOption::label),
        "material_codes" to options("materials_responsibilities").map(CustomerOption::code),
        "selected_material_index" to selectedMaterialIndex,
        "timing_labels" to options("timing_types").map(CustomerOption::label),
        "selected_timing_index" to options("timing_types").indexOfFirst { it.code == timingType },
        "address_id" to (addressId?.toString() ?: ""),
        "address_label" to addressLabel,
        "timing_codes" to options("timing_types").map(CustomerOption::code),
        "slot_id" to if (slotStart == null) "" else "selected", "slot_label" to selectedSlotLabel,
        "category_available" to categoryAvailable,
    )


    fun openNewAddress() = request("SCR-C15") {
        val selectedCity = api.cities().firstOrNull()?.id ?: error("No served cities")
        cityId = selectedCity
        val areas = api.areas(selectedCity)
        areaIds = areas.mapNotNull(IdName::id)
        addressDraft = AddressDraft(cityId = selectedCity)
        addressInput("validate", areas.map { it.name.orEmpty() })
    }

    fun openEditAddress(index: Int) = request("SCR-C15") {
        val address = addressPayloads.getOrNull(index) ?: error("Address not found")
        val selectedCityId = checkNotNull(address.city.id)
        val selectedAreaId = checkNotNull(address.area.id)
        val areas = api.areas(selectedCityId)
        cityId = selectedCityId
        areaIds = areas.mapNotNull(IdName::id)
        addressDraft = AddressDraft(
            id = address.id, cityId = selectedCityId, areaId = selectedAreaId,
            selectedAreaIndex = areaIds.indexOf(selectedAreaId), label = address.label,
            addressText = address.addressText, building = address.building.orEmpty(),
            floor = address.floor.orEmpty(), apartment = address.apartment.orEmpty(),
            landmark = address.landmark.orEmpty(), latitude = address.lat?.toDoubleOrNull(),
            longitude = address.lng?.toDoubleOrNull(), isDefault = address.isDefault,
        )
        addressInput("validate", areas.map { it.name.orEmpty() })
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
        state = reduce("SCR-C15", addressInput("validate", state.options))
    }

    fun selectArea(index: Int) {
        addressDraft = addressDraft.copy(areaId = areaIds.getOrNull(index), selectedAreaIndex = index)
        state = reduce("SCR-C15", addressInput("validate", state.options))
    }

    fun updateAddressLocation(latitude: Double, longitude: Double) {
        addressDraft = addressDraft.copy(latitude = latitude, longitude = longitude)
        state = reduce("SCR-C15", addressInput("validate", state.options))
    }

    fun toggleAddressDefault() {
        addressDraft = addressDraft.copy(isDefault = !addressDraft.isDefault)
        state = reduce("SCR-C15", addressInput("validate", state.options))
    }

    fun saveAddress() {
        if (!state.canContinue) return
        state = reduce("SCR-C15", addressInput("saving", state.options))
        Thread {
            val draft = addressDraft
            val saved = runCatching {
                api.saveAddress(
                    token(), draft.id,
                    AddressBody(
                        label = draft.label, cityId = draft.cityId, areaId = draft.areaId, addressText = draft.addressText,
                        building = draft.building, floor = draft.floor, apartment = draft.apartment, landmark = draft.landmark,
                        lat = draft.latitude, lng = draft.longitude, isDefault = draft.isDefault,
                    ),
                )
            }.isSuccess
            Handler(Looper.getMainLooper()).post {
                if (saved) loadAddresses(addressSelectionMode) else state = reduce("SCR-C15", mapOf("event" to "error"))
            }
        }.start()
    }

    fun requestDeleteAddress(index: Int) {
        if (addressIds.getOrNull(index) == null) return
        state = reduce("SCR-C14", addressStateInput("confirm_delete", index))
    }

    fun cancelDeleteAddress() {
        state = reduce("SCR-C14", addressStateInput("loaded"))
    }

    fun confirmDeleteAddress() {
        val index = state.pendingIndex
        val id = addressIds.getOrNull(index) ?: return
        state = reduce("SCR-C14", addressStateInput("deleting", index))
        Thread {
            val next = runCatching {
                api.deleteAddress(token(), id)
                addressPayloads = api.addresses(token())
                addressIds = addressPayloads.map(CustomerAddress::id)
                mapOf(
                    "event" to "loaded", "selection_mode" to addressSelectionMode,
                    "address_labels" to addressPayloads.map(CustomerAddress::label),
                    "address_details" to addressPayloads.map { it.detail() },
                    "selected_index" to addressPayloads.indexOfFirst(CustomerAddress::isDefault),
                )
            }.getOrElse { mapOf("event" to "error") }
            Handler(Looper.getMainLooper()).post { state = reduce("SCR-C14", next) }
        }.start()
    }

    private fun addressStateInput(event: String, pendingIndex: Int = -1): Map<String, Any?> = mapOf(
        "event" to event, "selection_mode" to addressSelectionMode,
        "address_labels" to addressPayloads.map(CustomerAddress::label),
        "address_details" to addressPayloads.map { it.detail() },
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
        // The chosen day and period, e.g. «غدًا · 1:00 م–3:00 م» (6ب).
        selectedSlotLabel = listOf(state.options.getOrElse(state.selectedOptionIndex) { "" }, state.items.getOrElse(selectedSlotIndex) { "" })
            .filter(String::isNotBlank).joinToString(" · ")
        openTiming()
    }

    fun openMedia() {
        state = reduce("SCR-C17", mediaInput())
    }

    fun canAddMedia(kind: String): Boolean = when (kind) {
        "photo" -> media.count { it.kind == kind } < 5
        "video", "audio" -> media.none { it.kind == kind }
        else -> false
    }

    fun rejectMedia(reason: String) {
        if (state.screen != "SCR-C17") return
        mediaSelectionError = reason
        state = reduce("SCR-C17", mediaInput())
    }

    fun completeMediaSelection() {
        mediaSelectionError = ""
        state = reduce("SCR-C37", problemInput())
    }

    fun uploadMedia(upload: CustomerMediaUpload, kind: String) {
        mediaSelectionError = ""
        media += MediaDraft(null, kind, "uploading", upload)
        uploadMediaAt(media.lastIndex)
    }

    fun retryMedia(index: Int) {
        if (media.getOrNull(index)?.state != "failed" || media[index].upload == null) return
        media[index] = media[index].copy(state = "uploading")
        uploadMediaAt(index)
    }

    fun setMediaRecording(recording: Boolean) {
        mediaRecording = recording
        state = reduce("SCR-C17", mediaInput())
    }

    private fun uploadMediaAt(index: Int) {
        val draft = media.getOrNull(index) ?: return
        val upload = draft.upload ?: return
        state = reduce("SCR-C17", mediaInput())
        Thread {
            val next = runCatching {
                val payload = api.uploadMedia(token(), upload)
                val currentIndex = media.indexOfFirst { it.upload === upload }
                if (currentIndex >= 0) {
                    media[currentIndex] = draft.copy(id = payload.id, state = "uploaded", upload = null)
                }
                mediaInput()
            }.getOrElse {
                val currentIndex = media.indexOfFirst { it.upload === upload }
                if (currentIndex >= 0) {
                    media[currentIndex] = draft.copy(id = null, state = "failed")
                }
                mediaInput()
            }
            Handler(Looper.getMainLooper()).post { state = reduce("SCR-C17", next) }
        }.start()
    }

    fun deleteMedia(index: Int) {
        val item = media.getOrNull(index) ?: return
        if (item.state == "uploading") return
        media.removeAt(index)
        state = reduce("SCR-C17", mediaInput())
        item.id?.let { id -> Thread { runCatching { api.deleteMedia(token(), id) } }.start() }
    }

    /** An OPEN order in employee mode waits for assignment on C36; every other order opens C09 (SCR-C36). */
    fun loadOrder(orderId: Int) = requestRouted("SCR-C09") {
        val order = api.order(token(), orderId)
        arrivalCodePayload = null
        arrivalCodeFailed = false
        if (order.arrivalCodeRequired) {
            arrivalCodePayload = runCatching { api.arrivalCode(token(), orderId) }.onFailure { arrivalCodeFailed = true }.getOrNull()
        }
        currentOrderPayload = order
        currentOrderId = orderId
        currentOrderVersion = order.version
        if ((operatingMode == "staff" || !order.warrantyCoverage.isNullOrBlank()) && order.status == "OPEN") {
            return@requestRouted "SCR-C36" to assignmentInput(order)
        }
        "SCR-C09" to trackingInput(order) + ("summary" to listOf("#${order.number}", order.location.area.orEmpty()))
    }

    fun refreshArrivalCode() = requestRouted("SCR-C09") {
        val order = currentOrderPayload ?: error("Order required")
        arrivalCodePayload = runCatching { api.arrivalCode(token(), order.id, true) }.onFailure { arrivalCodeFailed = true }.getOrNull()
        if (arrivalCodePayload != null) arrivalCodeFailed = false
        "SCR-C09" to trackingInput(order)
    }

    fun refreshTracking(onResult: (CustomerTrackingPayload, Pair<Double, Double>?) -> Unit) {
        val orderId = currentOrderId ?: return
        val destination = currentOrderPayload?.location?.let { location ->
            val latitude = location.lat?.toDoubleOrNull()
            val longitude = location.lng?.toDoubleOrNull()
            if (latitude == null || longitude == null) null else latitude to longitude
        }

        Thread {
            runCatching { api.tracking(token(), orderId) }
                .onSuccess { payload ->
                    Handler(Looper.getMainLooper()).post { onResult(payload, destination) }
                }
        }.start()
    }

    fun loadPaymentSummary() {
        val orderId = currentOrderId ?: return
        request("SCR-C21") {
            val order = api.order(token(), orderId)
            currentOrderPayload = order
            currentOrderVersion = order.version
            paymentSummaryInput(order, "loaded")
        }
    }

    fun selectPaymentMethod(index: Int) {
        val orderId = currentOrderId ?: return
        val method = options("payment_methods").getOrNull(index)?.code ?: return
        val current = currentOrderPayload ?: return
        state = reduce("SCR-C21", paymentSummaryInput(current, "switching"))
        Thread {
            val next = runCatching {
                val order = api.changePaymentMethod(token(), orderId, method, currentOrderVersion)
                currentOrderPayload = order
                currentOrderVersion = order.version
                paymentSummaryInput(order, "loaded")
            }.getOrElse { mapOf("event" to "error") }
            Handler(Looper.getMainLooper()).post { state = reduce("SCR-C21", next) }
        }.start()
    }

    fun openElectronicPayment() {
        val order = currentOrderPayload ?: return
        selectedPaymentChannel = options("electronic_payment_gateways").indexOfFirst { it.code == "INSTAPAY_MANUAL" }
        transferReference = ""
        receiptMediaId = null
        state = reduce("SCR-C22", paymentInput(order, order.latestPayment, "loaded"))
    }

    fun selectPaymentChannel(index: Int) {
        selectedPaymentChannel = index
        val order = currentOrderPayload ?: return
        state = reduce("SCR-C22", paymentInput(order, null, "loaded"))
    }

    fun createElectronicPayment() {
        val orderId = currentOrderId ?: return
        val order = currentOrderPayload ?: return
        val channel = options("electronic_payment_gateways").getOrNull(selectedPaymentChannel)?.code ?: return
        if (!state.canContinue) return
        history.commit()
        state = reduce("SCR-C22", paymentInput(order, null, "creating"))
        Thread {
            val next = runCatching {
                val payment = if (channel == "INSTAPAY_MANUAL") api.submitInstapay(token(), orderId, transferReference.trim(), receiptMediaId ?: error("receipt required"), currentOrderVersion)
                    else api.createPayment(token(), orderId, options("payment_channels").firstOrNull()?.code ?: error("channel required"), currentOrderVersion)
                val fresh = api.order(token(), orderId)
                currentOrderPayload = fresh
                currentOrderVersion = fresh.version
                paymentInput(fresh, payment, "loaded")
            }.getOrElse { mapOf("event" to "error") }
            Handler(Looper.getMainLooper()).post { state = reduce("SCR-C22", next) }
        }.start()
    }

    fun updateTransferReference(value: String) {
        transferReference = value
        currentOrderPayload?.let { state = reduce("SCR-C22", paymentInput(it, it.latestPayment, "loaded")) }
    }

    fun refreshPayment() {
        val id = currentOrderId ?: return
        request("SCR-C22") {
            val fresh = api.order(token(), id)
            currentOrderPayload = fresh
            currentOrderVersion = fresh.version
            paymentInput(fresh, fresh.latestPayment, "loaded")
        }
    }

    fun uploadPaymentReceipt(upload: CustomerMediaUpload) {
        val order = currentOrderPayload ?: return
        if (state.isBusy) return
        state = reduce("SCR-C22", paymentInput(order, order.latestPayment, "uploading"))
        Thread {
            val result = runCatching { api.uploadMedia(token(), upload).id }
            Handler(Looper.getMainLooper()).post {
                result.onSuccess { receiptMediaId = it }
                state = reduce("SCR-C22", paymentInput(order, order.latestPayment, if (result.isSuccess) "loaded" else "upload_failed"))
            }
        }.start()
    }

    fun uploadRatingPhoto(upload: CustomerMediaUpload) {
        if (state.isBusy || ratingMediaIds.size >= 5) return
        state = reduce("SCR-C24", ratingInput("uploading"))
        Thread {
            val result = runCatching { api.uploadMedia(token(), upload).id }
            Handler(Looper.getMainLooper()).post {
                result.onSuccess { ratingMediaIds += it }
                state = reduce("SCR-C24", ratingInput(if (result.isSuccess) "loaded" else "upload_failed"))
            }
        }.start()
    }

    fun removeRatingPhoto() {
        if (!state.isBusy && ratingMediaIds.isNotEmpty()) { ratingMediaIds.removeAt(ratingMediaIds.lastIndex); state = reduce("SCR-C24", ratingInput("loaded")) }
    }

    fun loadCompletionSummary() {
        val orderId = currentOrderId ?: return
        request("SCR-C23") {
            val order = api.order(token(), orderId)
            currentOrderPayload = order
            currentOrderVersion = order.version
            completionInput(order, "loaded")
        }
    }

    fun confirmCompletion() {
        val orderId = currentOrderId ?: return
        val order = currentOrderPayload ?: return
        state = reduce("SCR-C23", completionInput(order, "submitting"))
        history.commit()
        Thread {
            val result = runCatching { api.confirmCompletion(token(), orderId, currentOrderVersion) }
            Handler(Looper.getMainLooper()).post {
                result.onSuccess {
                    currentOrderPayload = it
                    currentOrderVersion = it.version
                    openRating()
                }.onFailure { state = reduce("SCR-C23", mapOf("event" to "error")) }
            }
        }.start()
    }

    fun openRating() {
        ratingValues = mutableListOf(0, 0, 0)
        ratingComment = ""
        ratingTags.clear()
        ratingMediaIds.clear()
        state = reduce("SCR-C24", ratingInput("loaded"))
    }

    fun updateRating(dimension: Int, value: Int) {
        if (dimension !in ratingValues.indices || value !in 1..5) return
        ratingValues[dimension] = value
        state = reduce("SCR-C24", ratingInput("loaded"))
    }

    /** Figma's overall row: one tap sets the three API dimensions, which the detail rows can then adjust. */
    fun updateOverallRating(value: Int) {
        if (value !in 1..5) return
        ratingValues = MutableList(3) { value }
        state = reduce("SCR-C24", ratingInput("loaded"))
    }

    fun toggleRatingTag(index: Int, label: String) {
        val code = reviewTagOptions().getOrNull(index)?.code ?: return
        if (ratingTags.remove(index) == null) ratingTags[index] = code
        state = reduce("SCR-C24", ratingInput("loaded"))
    }

    private fun ratingCommentToSend(): String = ratingComment.trim()

    fun updateRatingComment(value: String) {
        ratingComment = value
        state = reduce("SCR-C24", ratingInput("loaded"))
    }

    fun submitRating() {
        val orderId = currentOrderId ?: return
        if (!state.canContinue) return
        state = reduce("SCR-C24", ratingInput("submitting"))
        history.commit()
        Thread {
            val success = runCatching {
                api.submitStructuredReview(
                    token(), orderId, ratingValues[0], ratingValues[1], ratingValues[2],
                    ratingCommentToSend().ifBlank { null }, ratingMediaIds.toList(), ratingTags.values.toList(),
                )
            }.isSuccess
            Handler(Looper.getMainLooper()).post {
                state = reduce("SCR-C24", ratingInput(if (success) "success" else "error"))
            }
        }.start()
    }

    fun openCancellation() {
        cancellationReasonIndex = -1
        cancellationNote = ""
        state = reduce("SCR-C20", cancellationInput("loaded"))
    }

    fun selectCancellationReason(index: Int) {
        cancellationReasonIndex = index
        state = reduce("SCR-C20", cancellationInput("loaded"))
    }

    fun updateCancellationNote(value: String) {
        cancellationNote = value
        state = reduce("SCR-C20", cancellationInput("loaded"))
    }

    fun submitCancellation() {
        val orderId = currentOrderId ?: return
        val reason = options("customer_cancellation_reasons").getOrNull(cancellationReasonIndex)?.code ?: return
        state = reduce("SCR-C20", cancellationInput("submitting"))
        history.commit()
        Thread {
            val next = runCatching {
                val order = api.cancelOrder(token(), orderId, reason, cancellationNote.ifBlank { null }, currentOrderVersion)
                currentOrderVersion = order.version
                trackingInput(order)
            }.getOrElse { mapOf("event" to "error") }
            Handler(Looper.getMainLooper()).post { state = reduce(if (next["event"] == "error") "SCR-C20" else "SCR-C09", next) }
        }.start()
    }

    fun loadPendingProposal() {
        val orderId = currentOrderId ?: return
        state = reduce("SCR-C30", mapOf("event" to "loading"))
        Thread {
            val result = runCatching {
                val order = api.order(token(), orderId)
                currentOrderVersion = order.version
                val proposal = api.proposals(token(), orderId).last { it.status == "PENDING" }
                currentProposalId = proposal.id
                val screen = if (proposal.type == "EXECUTION_QUOTE") "SCR-C30" else "SCR-C31"
                screen to proposalInput(order, proposal, "loaded")
            }.getOrElse { "SCR-C30" to mapOf("event" to "error") }
            Handler(Looper.getMainLooper()).post { state = reduce(result.first, result.second) }
        }.start()
    }

    fun decideProposal(action: String) {
        val orderId = currentOrderId ?: return
        val proposalId = currentProposalId ?: return
        val screen = state.screen
        history.commit()
        val input = mapOf(
            "event" to "submitting", "type_label" to state.items.getOrElse(0) { "" },
            "amount" to state.items.getOrElse(1) { "" }, "reason" to state.items.getOrElse(2) { "" },
            "total" to state.items.getOrElse(3) { "" }, "countdown_seconds" to state.countdownSeconds,
            "proposal_deadline" to state.proposalDeadline, "proposal_expiry_message" to state.proposalExpiryMessage,
            "proposal_loaded_uptime" to state.proposalLoadedUptime,
            "has_photo" to state.hasPhoto, "available_actions" to state.visibleActions,
            "provider_name" to state.fieldValues.getOrElse(0) { "" }, "provider_rating" to state.fieldValues.getOrElse(1) { "" },
            "provider_category" to state.fieldValues.getOrElse(2) { "" }, "original_amount" to state.fieldValues.getOrElse(3) { "" },
            "provider_verified" to state.providerVerified,
        )
        state = reduce(screen, input)
        Thread {
            val next = runCatching {
                val order = api.decideProposal(token(), orderId, proposalId, action == "approve_proposal", currentOrderVersion)
                currentOrderVersion = order.version
                trackingInput(order)
            }.getOrElse { mapOf("event" to "error") }
            Handler(Looper.getMainLooper()).post { state = reduce(if (next["event"] == "error") screen else "SCR-C09", next) }
        }.start()
    }

    private fun cancellationInput(event: String): Map<String, Any?> = mapOf(
        "event" to event, "reason_labels" to options("customer_cancellation_reasons").map(CustomerOption::label),
        "selected_reason_index" to cancellationReasonIndex,
        "note" to cancellationNote, "available_actions" to listOf("cancel"),
    )

    private fun loadConversation(id: Int, showLoading: Boolean) {
        stopChatPolling()
        if (showLoading) state = reduce("SCR-C19", mapOf("event" to "loading"))
        Thread {
            val next = runCatching {
                val response = api.conversationMessages(token(), id)
                currentConversationPayload = response.conversation
                chatMessages = response.data
                chatInput("loaded")
            }.getOrElse { mapOf("event" to "error") }
            mainHandler.post {
                state = reduce("SCR-C19", next)
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
        val order = conversation?.order
        return mapOf(
            "event" to event,
            "conversation_status" to conversation?.status.orEmpty(),
            "provider_name" to conversation?.provider?.name.orEmpty(),
            "provider_available" to (conversation?.provider?.availableNow == true),
            "provider_verified" to (conversation?.provider?.isVerified == true),
            "order_summary" to "${order?.category.orEmpty()} #${order?.number?.toString().orEmpty()} · ${order?.statusLabel.orEmpty()}",
            "image_groups" to chatMessages.map { message -> message.media.mapNotNull { it.contentUrl }.joinToString("\n") },
            "message_bodies" to chatMessages.map { message -> message.body.orEmpty() },
            "message_kinds" to chatMessages.map { message ->
                val direction = if (message.isMine) "sent" else "received"
                val kind = when {
                    message.wasMasked -> "blocked"
                    message.media.isNotEmpty() -> "image"
                    else -> "text"
                }
                "${direction}_$kind"
            },
            "message_times" to chatMessages.map { it.createdAt.orEmpty().displayDateTime() },
            "draft" to chatDraft,
        )
    }

    private fun orderHistoryInput(order: CustomerOrder, proposals: List<CustomerProposal>): Map<String, Any?> {
        val amounts = order.amounts
        val summary = mutableListOf(
            "#${order.number}", order.category.name.orEmpty(), order.problemType.name.orEmpty(), order.location.area.orEmpty(),
            (order.timing.slotStart ?: order.createdAt).orEmpty().displayDateTime(),
        )
        if (order.status == "CLOSED") {
            summary += listOf(
                formatAmount(amounts.laborTotal ?: ZERO_AMOUNT),
                formatAmount(amounts.materialsTotal ?: ZERO_AMOUNT),
                formatAmount(amounts.finalAmount ?: ZERO_AMOUNT),
                amounts.paymentMethod.orEmpty(), amounts.paymentStatus,
            )
        }
        return mapOf(
            "work_before_urls" to order.workPhotos.filter { it.purpose == "REPAIR_BEFORE" }.mapNotNull { it.contentUrl },
            "work_after_urls" to order.workPhotos.filter { it.purpose == "REPAIR_AFTER" }.mapNotNull { it.contentUrl },            "event" to "loaded", "status" to order.status,
            "display_status" to order.statusLabel, "summary" to summary,
            "provider_name" to order.provider?.name.orEmpty(),
            "termination_reason" to (order.termination.note?.takeIf(String::isNotBlank) ?: order.termination.reasonLabel.orEmpty()),
            "proposal_summaries" to proposals.map { "${it.typeLabel} · ${formatAmount(it.amount)}" },
            "available_actions" to order.availableActions,
        )
    }

    private fun trackingInput(order: CustomerOrder): Map<String, Any?> = mapOf(
        "arrival_code_required" to order.arrivalCodeRequired, "arrival_exception_pending" to order.arrivalExceptionPending,
        "arrival_code" to arrivalCodePayload?.code.orEmpty(), "arrival_code_until" to arrivalCodePayload?.expiresAt?.displayDateTime().orEmpty(), "arrival_code_failed" to arrivalCodeFailed,
        "execution_labels" to order.executionSteps.map(CustomerExecutionStep::label),
        "execution_times" to order.executionSteps.map { it.occurredAt?.displayDateTime().orEmpty() },
        "execution_states" to order.executionSteps.map(CustomerExecutionStep::state),
        "completion_estimate_at" to order.completionEstimate?.estimatedAt?.displayDateTime().orEmpty(),
        "completion_estimate_overdue" to (order.completionEstimate?.overdue == true),
        "event" to "loaded", "status" to order.status,
        "display_status" to order.displayStatus, "stepper" to (order.stepper != null),
        // The order carries no ETA; C09 reads it from the tracking call.
        "eta_approximate" to false,
        "available_actions" to order.availableActions,
        "step_states" to order.stepper.orEmpty().map(StepperStep::state),
        // DEC-076: the Figma tracking cards — provider, the agreed job and its money, all from the order.
        "order_number" to order.number.toString(),
        "address" to listOf(order.location.area.orEmpty(), order.location.city.orEmpty()).filter(String::isNotBlank).joinToString(" · "),
        "order_title" to order.problemType.name.orEmpty(),
        "agreed_price" to plainAmount(order.amounts.laborTotal),
        "materials_label" to options("materials_responsibilities").firstOrNull { it.code == order.materialsResponsibility }?.label.orEmpty(),
        "provider_name" to order.provider?.name.orEmpty(),
        "provider_rating" to order.provider?.ratingAvg.orEmpty(),
        "provider_completed" to (order.provider?.completedOrders?.toString().orEmpty()),
        "provider_verified" to (order.provider?.isVerified == true),
        "provider_category" to order.category.name.orEmpty(),
        "final_amount" to plainAmount(order.amounts.finalAmount),
        "timing_type" to order.timing.type,
        "slot_label" to order.timing.slotStart.orEmpty().displayDateTime(),
    )

    private fun plainAmount(value: String?): String =
        value?.toBigDecimalOrNull()?.stripTrailingZeros()?.toPlainString().orEmpty()

    private fun paymentSummaryInput(order: CustomerOrder, event: String): Map<String, Any?> {
        val amounts = order.amounts
        return mapOf(
            "work_before_urls" to order.workPhotos.filter { it.purpose == "REPAIR_BEFORE" }.mapNotNull { it.contentUrl },
            "work_after_urls" to order.workPhotos.filter { it.purpose == "REPAIR_AFTER" }.mapNotNull { it.contentUrl },            "event" to event,
            "amounts" to listOf(
                plainAmount(amounts.laborTotal ?: ZERO_AMOUNT),
                plainAmount(amounts.materialsTotal ?: ZERO_AMOUNT),
                plainAmount(amounts.finalAmount ?: ZERO_AMOUNT),
            ),
            "method_labels" to options("payment_methods").map(CustomerOption::label),
            "method_codes" to options("payment_methods").map(CustomerOption::code),
            "provider_name" to order.provider?.name.orEmpty(),
            "provider_verified" to (order.provider?.isVerified == true),
            "order_title" to order.problemType.name.orEmpty(),
            "address" to listOf(order.location.area.orEmpty(), order.location.city.orEmpty()).filter(String::isNotBlank).joinToString(" · "),
            "selected_method_index" to options("payment_methods").indexOfFirst { it.code == amounts.paymentMethod },
            "payment_method" to amounts.paymentMethod.orEmpty(),
            "available_actions" to order.availableActions,
        )
    }

    private fun paymentInput(order: CustomerOrder, payment: PaymentAttempt?, event: String): Map<String, Any?> {
        val expiresAt = payment?.expiresAt?.takeIf(String::isNotBlank)
        val seconds = expiresAt?.let {
            runCatching { java.time.Duration.between(OffsetDateTime.now(), OffsetDateTime.parse(it)).seconds.toInt().coerceAtLeast(0) }.getOrDefault(0)
        } ?: 0
        return mapOf(
            "event" to event,
            "total" to formatAmount(order.amounts.finalAmount ?: ZERO_AMOUNT),
            "channel_labels" to options("electronic_payment_gateways").map(CustomerOption::label),
            "is_instapay" to (options("electronic_payment_gateways").getOrNull(selectedPaymentChannel)?.code == "INSTAPAY_MANUAL"),
            "receipt_uploaded" to (receiptMediaId != null), "transfer_reference" to transferReference,
            "instapay_address" to instapay?.address.orEmpty(), "instapay_name" to instapay?.displayName.orEmpty(),
            "order_reference" to order.paymentReference.orEmpty(), "failure_reason_label" to payment?.failureReasonLabel.orEmpty(),
            "selected_channel_index" to selectedPaymentChannel,
            "payment_status" to payment?.status.orEmpty(),
            "reference_number" to payment?.referenceNumber.orEmpty(),
            "checkout_url" to payment?.checkoutUrl.orEmpty(),
            "countdown_seconds" to seconds,
            "available_actions" to order.availableActions,
        )
    }

    private fun completionInput(order: CustomerOrder, event: String): Map<String, Any?> {
        val amounts = order.amounts
        return mapOf(
            "work_before_urls" to order.workPhotos.filter { it.purpose == "REPAIR_BEFORE" }.mapNotNull { it.contentUrl },
            "work_after_urls" to order.workPhotos.filter { it.purpose == "REPAIR_AFTER" }.mapNotNull { it.contentUrl },            "event" to event,
            "summary" to listOf(
                order.provider?.name.orEmpty(),
                plainAmount(amounts.laborTotal ?: ZERO_AMOUNT),
                plainAmount(amounts.materialsTotal ?: ZERO_AMOUNT),
                plainAmount(amounts.finalAmount ?: ZERO_AMOUNT),
                amounts.paymentMethod.orEmpty(),
            ),
            "available_actions" to order.availableActions,
            "provider_name" to order.provider?.name.orEmpty(),
            "provider_verified" to (order.provider?.isVerified == true),
            "order_title" to order.problemType.name.orEmpty(),
            "address" to listOf(order.location.area.orEmpty(), order.location.city.orEmpty()).filter(String::isNotBlank).joinToString(" · "),
            "method_label" to amounts.paymentMethodLabel.orEmpty(),
        )
    }

    private fun reviewTagOptions(): List<CustomerOption> = currentOrderPayload?.reviewTagOptions?.map { CustomerOption(it.code, it.label) } ?: options("service_review_tags")

    private fun ratingInput(event: String): Map<String, Any?> = mapOf(
        "event" to event,
        "ratings" to ratingValues.map(Int::toString),
        "comment" to ratingComment,
        "photo_count" to ratingMediaIds.size,
        "comment_length" to ratingCommentToSend().length,
        "available_actions" to if (event == "success") emptyList<String>() else listOf("rate"),
        "tags" to ratingTags.values.toList(),
        "tag_labels" to reviewTagOptions().map(CustomerOption::label),
        "tag_codes" to reviewTagOptions().map(CustomerOption::code),
        "provider_name" to currentOrderPayload?.provider?.name.orEmpty(),
        "provider_verified" to (currentOrderPayload?.provider?.isVerified == true),
        "order_title" to currentOrderPayload?.problemType?.name.orEmpty(),
    )

    private fun proposalInput(order: CustomerOrder, proposal: CustomerProposal, event: String): Map<String, Any?> {
        val expires = proposal.expiresAt?.takeIf { it.isNotBlank() }
        val seconds = proposal.remainingSeconds()
        return mapOf(
            "event" to event, "type_label" to proposal.typeLabel,
            "amount" to plainAmount(proposal.amount),
            "reason" to proposal.reason,
            "total" to plainAmount(proposal.projectedTotal),
            "provider_name" to order.provider?.name.orEmpty(),
            "provider_rating" to order.provider?.ratingAvg.orEmpty(),
            "provider_category" to order.category.name.orEmpty(),
            "provider_verified" to (order.provider?.isVerified == true),
            "original_amount" to proposal.currentTotal?.let(::plainAmount).orEmpty(),
            "proposal_deadline" to expires?.displayDateTime().orEmpty(),
            "proposal_expiry_message" to proposal.expiryMessage.orEmpty(),
            "proposal_loaded_uptime" to (android.os.SystemClock.elapsedRealtime() / 1000).toInt(),
            "countdown_seconds" to seconds, "expired" to (seconds == 0),
            "has_photo" to (proposal.photoUrl != null),
            "proposal_photo_url" to proposal.photoUrl.orEmpty(),
            "available_actions" to order.availableActions,
        )
    }

    private fun formatAmount(value: String): String = noox.bzr.design.BzrFormat.money(value, currencyLabel)

    fun openOrder() {
        currentOrderId?.let(::loadOrder) ?: show("SCR-C01", mapOf("event" to "loaded", "operating_mode" to operatingMode, "has_orders" to false))
    }

    fun publishDraft() {
        val address = addressId
        val category = categoryId
        val problem = problemTypeId
        if (address == null || category == null || problem == null) {
            state = reduce("SCR-C05", mapOf("event" to "error"))
            return
        }
        val target = if (operatingMode == "staff") "SCR-C36" else "SCR-C06"
        // A refused publish stays on C05 with the server's reason (SCR-C05 §الأخطاء), never on the target screen.
        state = reduce("SCR-C05", reviewInput(true) + ("event" to "submitting"))
        history.commit()
        Thread {
            val result = runCatching { publishRequest(target, address, category, problem) }
            mainHandler.post {
                state = result.fold(
                    onSuccess = { reduce(target, it) },
                    onFailure = { reduce("SCR-C05", reviewInput(true) + ("error_message" to serverMessage(it))) },
                )
            }
        }.start()
    }

    /** The server's own message for a refused request (BR-011, BR-019…), or the shared network error. */
    private fun serverMessage(error: Throwable): String = (error as? CustomerApiException)?.error?.message.orEmpty()

    private fun publishRequest(target: String, address: Int, category: Int, problem: Int): Map<String, Any?> {
        return run {
            val marketplace = operatingMode == "marketplace"
            val body = PublishOrderBody(
                customerAddressId = address,
                categoryId = category,
                problemTypeId = problem,
                timingType = timingType,
                termsAccepted = true,
                mediaIds = media.mapNotNull { it.id },
                materialsResponsibility = options("materials_responsibilities").getOrNull(selectedMaterialIndex)?.code,
                description = requestDescription.ifBlank { null },
                // BR-008 — pricing and budget belong to the marketplace only
                pricingMode = if (marketplace) options("pricing_modes").getOrNull(selectedPricingIndex)?.code else null,
                budgetAmount = if (marketplace) requestBudget.toBigDecimalOrNull()?.toPlainString() else null,
                slotStart = slotStart,
            )
            val order = editingOrderId?.let { api.updateOrder(token(), it, body) }
                ?: api.publish(token(), body, java.util.UUID.randomUUID().toString())
            editingOrderId = null
            currentOrderId = order.id
            currentOrderPayload = order
            currentOrderVersion = order.version
            if (target == "SCR-C36") {
                assignmentInput(order)
            } else {
                currentOffers = emptyList()
                offersInput(order)
            }
        }
    }

    fun loadOffers(orderId: Int) = request("SCR-C06") {
        val order = api.order(token(), orderId)
        currentOrderPayload = order
        currentOrderId = orderId
        currentOrderVersion = order.version
        currentOffers = api.offers(token(), orderId, offerSortCode())
        offersInput(order)
    }

    fun selectOffersSort(index: Int) {
        if (index !in 0..2 || (index == 2 && state.showEta.not())) return
        offersSortIndex = index
        currentOffers = when (index) {
            1 -> currentOffers.sortedBy { it.price.toBigDecimalOrNull() }
            2 -> currentOffers.sortedBy { it.etaMinutes ?: Int.MAX_VALUE }
            else -> currentOffers.sortedByDescending { it.provider.ratingAvg?.toBigDecimalOrNull() }
        }
        currentOrderPayload?.let { state = reduce("SCR-C06", offersInput(it)) }
    }

    fun openOffer(index: Int) {
        val offer = currentOffers.getOrNull(index) ?: return
        currentOfferId = offer.id
        currentOfferProviderId = offer.provider.id
        selectedPaymentChannel = options("payment_methods").indexOfFirst { it.code == optionDefaults["payment_method"] }
            .takeIf { it >= 0 } ?: 0
        state = reduce("SCR-C08", offerDetailsInput(offer, "loaded"))
    }

    fun openOfferProvider(index: Int) {
        val offer = currentOffers.getOrNull(index) ?: return
        val providerId = offer.provider.id.takeIf { it > 0 } ?: return
        currentOfferId = offer.id
        currentOfferProviderId = providerId
        val orderId = currentOrderId ?: return
        loadProvider(providerId, orderId)
    }

    fun openCurrentOffer() {
        val offer = currentOffers.firstOrNull { it.id == currentOfferId } ?: return
        selectedPaymentChannel = options("payment_methods").indexOfFirst { it.code == optionDefaults["payment_method"] }
            .takeIf { it >= 0 } ?: 0
        state = reduce("SCR-C08", offerDetailsInput(offer, "loaded"))
    }

    fun selectOfferPayment(index: Int) {
        if (options("payment_methods").getOrNull(index) == null) return
        selectedPaymentChannel = index
        val offer = currentOffers.firstOrNull { it.id == currentOfferId } ?: return
        state = reduce("SCR-C08", offerDetailsInput(offer, "loaded"))
    }

    fun confirmSelectedOffer() {
        val orderId = currentOrderId ?: return
        val offerId = currentOfferId ?: return
        val payment = options("payment_methods").getOrNull(selectedPaymentChannel)?.code ?: return
        val offer = currentOffers.firstOrNull { it.id == offerId } ?: return
        state = reduce("SCR-C08", offerDetailsInput(offer, "submitting"))
        Thread {
            val result = runCatching { api.acceptOffer(token(), orderId, offerId, currentOrderVersion, payment) }
            mainHandler.post {
                result.onSuccess { order ->
                    currentOrderPayload = order
                    currentOrderVersion = order.version
                    loadOrder(orderId)
                }.onFailure {
                    state = reduce("SCR-C08", mapOf("event" to "error"))
                }
            }
        }.start()
    }

    private fun offerSortOptions(): List<CustomerOption> = options("offer_sorts").filter { it.code != "eta" || currentOrderPayload?.timing?.type == "NOW" }
    private fun offerSortCode(): String = offerSortOptions().getOrNull(offersSortIndex)?.code ?: "rating"

    private fun offersInput(order: CustomerOrder): Map<String, Any?> = mapOf(
        "waiting_info" to waitingInfo(order),
        "event" to "loaded",
        "order_title" to "${order.category.name.orEmpty()} — ${order.problemType.name.orEmpty()} · #${order.number}",
        "display_status" to order.displayStatus,
        "countdown_seconds" to deadlineSeconds(order.deadlines),
        "sort_index" to offersSortIndex,
        "sort_labels" to offerSortOptions().map(CustomerOption::label),
        "sort_codes" to offerSortOptions().map(CustomerOption::code),
        "offer_rows" to currentOffers.map { offerRow(it, order) },
        "pricing_mode" to order.pricingMode.lowercase(),
        "timing_type" to order.timing.type.lowercase(),
        "available_actions" to order.availableActions,
        "summary_rows" to listOf(
            order.problemType.name.orEmpty(), order.location.area.orEmpty(),
            order.timing.slotStart?.displayDateTime().orEmpty(), order.budgetAmount?.toBigDecimalOrNull()?.stripTrailingZeros()?.toPlainString().orEmpty(),
        ),
        "order_icon" to catalog.firstOrNull { it.id == order.category.id }?.iconKey.orEmpty(),
    )

    /**
     * C06 card row (DEC-073, Figma 3002:446):
     * id|name|rating|reviews|amount|includes|eta minutes or slot|materials|note|verified|provider id|subtitle.
     */
    private fun offerRow(offer: CustomerOffer, order: CustomerOrder): String {
        val provider = offer.provider
        val timing = order.timing
        val detail = offer.etaMinutes?.takeIf { timing.type == "NOW" && it > 0 }
            ?.let { "$it" }
            ?: timing.slotStart.orEmpty().displayDateTime()
        val materials = options("materials_responsibilities").firstOrNull { it.code == order.materialsResponsibility }?.label.orEmpty()
        return listOf(
            offer.id, provider.name, provider.ratingAvg.orEmpty(), provider.ratingsCount?.toString().orEmpty(),
            offer.price.toBigDecimalOrNull()?.stripTrailingZeros()?.toPlainString() ?: offer.price,
            offer.includesText.orEmpty(), detail, materials, offer.note.orEmpty().replace('|', '/').replace('\n', ' '),
            provider.isVerified, provider.id, order.category.name.orEmpty(), provider.availableNow,
        ).joinToString("|")
    }

    private fun offerDetailsInput(offer: CustomerOffer, event: String): Map<String, Any?> {
        val order = currentOrderPayload ?: return mapOf("event" to "error")
        val provider = offer.provider
        val timing = order.timing
        val pricingMode = order.pricingMode.lowercase()
        val eta = offer.etaMinutes?.takeIf { it > 0 }?.toString().orEmpty()
        return mapOf(
            "event" to event,
            "provider_name" to provider.name,
            "provider_rating" to provider.ratingAvg.orEmpty(),
            "provider_services" to provider.completedOrders?.toString().orEmpty(),
            "provider_verified" to provider.isVerified,
            "order_rows" to listOf(
                order.problemType.name.orEmpty(),
                listOf(order.location.area.orEmpty(), order.location.city.orEmpty()).filter(String::isNotBlank).joinToString(" · "),
                if (timing.type == "NOW") "" else timing.slotStart.orEmpty().displayDateTime(),
            ),
            "provider_category" to order.category.name.orEmpty(),
            "materials_label" to options("materials_responsibilities").firstOrNull { it.code == order.materialsResponsibility }?.label.orEmpty(),
            "payment_labels" to options("payment_methods").map(CustomerOption::label),
            "selected_payment_index" to selectedPaymentChannel,
            "available_actions" to order.availableActions.filter { it == "accept_offer" },
            "pricing_mode" to pricingMode,
            "timing_type" to timing.type.lowercase(),
            "eta_minutes" to eta,
            "offer_rows" to listOf(
                offer.price.toBigDecimalOrNull()?.stripTrailingZeros()?.toPlainString() ?: offer.price,
                if (eta.isNotBlank()) eta else timing.slotStart.orEmpty().displayDateTime(),
                offer.includesText.orEmpty().ifBlank {
                    if (offer.inspectionFeeDeductible) "deductible" else offer.note.orEmpty()
                },
            ),
        )
    }

    private fun deadlineSeconds(deadlines: OrderDeadlines): Int {
        val value = deadlines.offersCloseAt ?: deadlines.selectionDeadlineAt ?: return 0
        return runCatching {
            java.time.Duration.between(OffsetDateTime.now(), OffsetDateTime.parse(value)).seconds.toInt().coerceAtLeast(0)
        }.getOrDefault(0)
    }

    fun loadProvider(providerId: Int, orderId: Int) = request("SCR-C07") {
        val provider = api.provider(token(), providerId, orderId)
        currentProviderId = providerId
        currentOrderId = orderId
        currentProviderActions = provider.availableActions
        val ratings = provider.ratingBreakdown
        mapOf(
            "provider_avatar_url" to provider.avatarUrl.orEmpty(),
            "portfolio_urls" to provider.portfolio.map { it.imageUrl.orEmpty() },
            "event" to "loaded",
            "provider_available" to provider.availableNow,
            "available_actions" to provider.availableActions,
            "provider_name" to provider.name,
            "provider_rating" to provider.ratingAvg.orEmpty(),
            "provider_services" to provider.completedOrders?.toString().orEmpty(),
            "provider_experience" to provider.experienceYears?.toString().orEmpty(),
            "provider_about" to provider.bio.orEmpty(),
            "offer_price" to currentOffers.firstOrNull { it.provider.id == providerId }?.price
                ?.let { it.toBigDecimalOrNull()?.stripTrailingZeros()?.toPlainString() ?: it }.orEmpty(),
            "ratings_count" to provider.ratingsCount?.toString().orEmpty(),
            "provider_category" to currentOrderPayload?.category?.name.orEmpty(),
            "portfolio_captions" to provider.portfolio.map { it.caption.orEmpty() },
            "provider_verified" to provider.isVerified,
            "specialties" to provider.specialties.map { it.name.orEmpty() },
            "rating_values" to listOf(ratings.quality.orEmpty(), ratings.punctuality.orEmpty(), ratings.conduct.orEmpty()),
            "review_tag_summary" to provider.tagStatistics.map { "${it.label} · ${it.count}" },
            "image_groups" to provider.reviews.map { review -> review.photos.mapNotNull { it.contentUrl }.joinToString("\n") },
            "review_rows" to provider.reviews.map { review ->
                listOf(
                    review.customerName,
                    listOf(review.quality, review.punctuality, review.conduct).average().toInt(),
                    review.comment.orEmpty(), listOf(review.createdAt.orEmpty().displayDateTime(), review.area.orEmpty()).filter(String::isNotBlank).joinToString(" · "),
                ).joinToString("|")
            },
        )
    }

    private var warrantyMode = false

    fun loadProfileImage(url: String, onReady: (android.graphics.Bitmap?) -> Unit) {
        val requestToken = token()
        Thread {
            val bitmap = runCatching {
                val bytes = api.profileImage(requestToken, url)
                val bounds = android.graphics.BitmapFactory.Options().apply { inJustDecodeBounds = true }
                android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
                val options = android.graphics.BitmapFactory.Options().apply {
                    while (bounds.outWidth / inSampleSize > 1024 || bounds.outHeight / inSampleSize > 1024) inSampleSize *= 2
                }
                android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
            }.getOrNull()
            mainHandler.post { if (session.token == requestToken) onReady(bitmap) }
        }.start()
    }

    fun openWarrantyVisit() {
        state.warrantyVisitId?.let { if (state.warrantyVisitFinal) loadOrderHistory(it) else loadOrder(it) }
    }

    fun openWarranty() {
        val id = currentOrderId ?: return
        warrantyMode = true
        supportDescription = ""
        supportReasonIndex = -1
        request("SCR-C27") { disputeInput("loaded", api.warrantyClaims(token(), id).firstOrNull()) }
    }

    fun openDispute() {
        warrantyMode = false
        val orderId = currentOrderId ?: return
        supportReasonIndex = -1
        supportDescription = ""
        disputeMedia.clear()
        request("SCR-C27") {
            val existing = api.disputes(token(), orderId).firstOrNull()
            if (existing == null) disputeInput("loaded") else disputeInput("loaded", existing)
        }
    }

    fun selectSupportReason(index: Int) {
        supportReasonIndex = index
        state = reduce(state.screen, if (state.screen == "SCR-C28") providerReportInput("loaded") else disputeInput("loaded"))
    }

    fun updateSupportDescription(value: String) {
        supportDescription = value.take(1001)
        state = reduce(state.screen, if (state.screen == "SCR-C28") providerReportInput("loaded") else disputeInput("loaded"))
    }

    fun uploadDisputePhoto(upload: CustomerMediaUpload) {
        disputeMedia += MediaDraft(null, "photo", "uploading")
        state = reduce("SCR-C27", disputeInput("loaded"))
        Thread {
            val next = runCatching {
                val item = api.uploadMedia(token(), upload)
                disputeMedia[disputeMedia.lastIndex] = MediaDraft(item.id, "photo", "uploaded")
                disputeInput("loaded")
            }.getOrElse {
                disputeMedia[disputeMedia.lastIndex] = MediaDraft(null, "photo", "failed")
                disputeInput("loaded")
            }
            mainHandler.post { state = reduce("SCR-C27", next) }
        }.start()
    }

    fun removeDisputePhoto(index: Int) {
        val item = disputeMedia.getOrNull(index) ?: return
        disputeMedia.removeAt(index)
        state = reduce("SCR-C27", disputeInput("loaded"))
        item.id?.let { id -> Thread { runCatching { api.deleteMedia(token(), id) } }.start() }
    }

    fun submitDispute() {
        val orderId = currentOrderId ?: return
        val reason = if (warrantyMode) "" else options("dispute_reasons").getOrNull(supportReasonIndex)?.code ?: return
        if (!state.canContinue) return
        history.commit()
        state = reduce("SCR-C27", disputeInput("submitting"))
        Thread {
            val next = runCatching {
                val dispute = if (warrantyMode) api.requestWarranty(token(), orderId, supportDescription.trim()) else api.openDispute(
                    token(), orderId, reason, supportDescription.trim(), disputeMedia.mapNotNull(MediaDraft::id),
                )
                disputeInput("success", dispute)
            }.getOrElse { mapOf("event" to "error") }
            mainHandler.post { state = reduce("SCR-C27", next) }
        }.start()
    }

    fun openProviderReport() {
        supportReasonIndex = -1
        supportDescription = ""
        state = reduce("SCR-C28", providerReportInput("loaded"))
    }

    fun submitProviderReport() {
        val providerId = currentProviderId ?: return
        val orderId = currentOrderId ?: return
        val reason = options("provider_report_reasons").getOrNull(supportReasonIndex)?.code ?: return
        if (!state.canContinue) return
        history.commit()
        state = reduce("SCR-C28", providerReportInput("submitting"))
        Thread {
            val success = runCatching {
                api.reportProvider(token(), providerId, orderId, reason, supportDescription.trim().ifBlank { null })
            }.isSuccess
            mainHandler.post {
                state = reduce("SCR-C28", providerReportInput(if (success) "success" else "error"))
            }
        }.start()
    }

    fun acceptOffer(orderId: Int, offerId: Int, expectedVersion: Int) {
        history.commit()
        acceptOfferRequest(orderId, offerId, expectedVersion)
    }

    private fun acceptOfferRequest(orderId: Int, offerId: Int, expectedVersion: Int) = request("SCR-C09") {
        val paymentMethod = optionDefaults["payment_method"] ?: return@request mapOf("event" to "error")
        val order = api.acceptOffer(token(), orderId, offerId, expectedVersion, paymentMethod)
        mapOf(
            "event" to "loaded", "status" to order.status,
            "display_status" to order.displayStatus, "stepper" to (order.stepper != null),
            "available_actions" to order.availableActions,
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
        val slots = api.slots(activeCity, date)
        slotStarts = slots.map(SlotRange::start)
        mapOf(
            "event" to "loaded", "day_labels" to CustomerLogic.slotDayLabels(dayDates, dayLabels),
            "slot_labels" to slots.map { "${it.start.timeLabel(morning, evening)}–${it.end.timeLabel(morning, evening)}" },
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
        "audio_count" to media.count { it.kind == "audio" }, "recording" to mediaRecording, "media_error" to mediaSelectionError,
    )

    private fun disputeInput(event: String, dispute: CustomerDispute? = null): Map<String, Any?> = mapOf(
        "event" to event,
        "existing" to (dispute != null),
        "warranty" to warrantyMode,
        "warranty_visit_id" to (dispute?.visitOrderId ?: 0),
        "warranty_visit_final" to (dispute?.status in listOf("COMPLETED", "CANCELLED")),
        "reason_labels" to options("dispute_reasons").map(CustomerOption::label),
        "selected_reason_index" to supportReasonIndex,
        "description" to (dispute?.description ?: supportDescription),
        "photo_count" to (dispute?.attachments?.size ?: disputeMedia.size),
        "details" to if (dispute == null) emptyList<String>() else listOf(dispute.reasonLabel, dispute.description),
        "status_details" to if (dispute == null) emptyList<String>() else listOf(
            dispute.statusLabel ?: dispute.status, dispute.resolutionNote.orEmpty(),
        ),
        "available_actions" to if (dispute == null) currentOrderPayload?.availableActions.orEmpty() else emptyList<String>(),
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

    private fun notificationInput(values: List<NotificationItem>, event: String): Map<String, Any?> = mapOf(
        "event" to event, "notification_titles" to values.map(NotificationItem::title),
        "notification_bodies" to values.map(NotificationItem::body),
        "notification_states" to values.map { if (it.readAt == null) "unread" else "read" },
        "deep_links" to notificationLinks,
        "notification_times" to values.map { it.createdAt.orEmpty().displayDateTime() },
        "notifications_denied" to !push.notificationsAllowed(),
        // DEC-076 (Figma 3002:3608): type code for the icon, «اليوم» / «سابق» grouping and the relative time.
        "notification_codes" to values.map(NotificationItem::code),
        "notification_days" to values.map { if (isToday(it.createdAt)) "today" else "earlier" },
        "notification_ages" to values.map { minutesSince(it.createdAt).toString() },
    )

    private fun isToday(timestamp: String?): Boolean = runCatching {
        OffsetDateTime.parse(timestamp).atZoneSameInstant(ZoneId.systemDefault()).toLocalDate() == java.time.LocalDate.now()
    }.getOrDefault(false)

    private fun accountInput(event: String, mode: String = accountMode): Map<String, Any?> = mapOf(
        "event" to event, "mode" to mode, "field_values" to accountValues, "available_actions" to accountActions,
        "rating_reminders_enabled" to ratingRemindersEnabled,
    )

    private fun submitAccountUpdate() {
        if (!state.canContinue) return
        state = reduce("SCR-C33", accountInput("submitting"))
        Thread {
            val user = runCatching { api.updateAccount(token(), accountValues[0].trim(), accountValues[2].trim()) }.getOrNull()
            mainHandler.post {
                if (user == null) state = reduce("SCR-C33", mapOf("event" to "error"))
                else {
                    accountValues = mutableListOf(user.name, user.email, user.phone.orEmpty())
                    accountMode = "overview"
                    state = reduce("SCR-C33", accountInput("success"))
                }
            }
        }.start()
    }

    private fun submitPasswordChange() {
        if (!state.canContinue) return
        state = reduce("SCR-C33", accountInput("submitting"))
        Thread {
            val success = runCatching { api.changePassword(token(), accountValues[0], accountValues[1], accountValues[2]) }.isSuccess
            mainHandler.post {
                accountMode = "overview"
                accountValues = mutableListOf("", session.email.orEmpty(), "")
                state = reduce("SCR-C33", if (success) accountInput("success") else mapOf("event" to "error"))
            }
        }.start()
    }

    private fun submitDeleteAccount() {
        state = reduce("SCR-C33", accountInput("submitting", "delete"))
        Thread {
            val result = runCatching { api.deleteAccount(token()) }
            mainHandler.post {
                if (result.isSuccess) logout()
                else if ((result.exceptionOrNull() as? CustomerApiException)?.status == 409) {
                    accountMode = "overview"
                    state = reduce("SCR-C33", accountInput("active_order"))
                } else state = reduce("SCR-C33", mapOf("event" to "error"))
            }
        }.start()
    }

    private fun reduce(screen: String, input: Map<String, Any?>): CustomerUiState =
        CustomerLogic.reduce(screen, input + ("operating_mode" to operatingMode) + ("warranty_coverage" to currentOrderPayload?.warrantyCoverage.orEmpty()) + ("waiting_info" to (input["waiting_info"] ?: waitingInfo(currentOrderPayload))))

    private fun waitingInfo(order: CustomerOrder?): String {
        val waiting = order?.waiting ?: return ""
        return listOfNotNull(waiting.message, waiting.deadlineAt?.let { "${waiting.deadlineLabel.orEmpty()}: ${it.displayDateTime()}" }).joinToString("\n")
    }

    private fun options(key: String): List<CustomerOption> = optionLists[key].orEmpty()

    /** C36: number, area, and the timing (now, or the chosen slot) of an order waiting for assignment. */
    private fun assignmentInput(order: CustomerOrder): Map<String, Any?> {
        val slot = order.timing.slotStart.orEmpty()
        val timingLabel = if (slot.isNotBlank()) {
            slot.displayDateTime()
        } else {
            options("timing_types").firstOrNull { it.code == order.timing.type }?.label.orEmpty()
        }
        return mapOf(
            "waiting_info" to waitingInfo(order),
            "event" to "loaded",
            "order_title" to "${order.category.name.orEmpty()} — ${order.problemType.name.orEmpty()}",
            "order_rows" to listOf("#${order.number}", order.location.area.orEmpty(), timingLabel).filter(String::isNotBlank),
            "available_actions" to order.availableActions,
        )
    }

    /** Like [request], but the loaded data decides which screen shows it. */
    private fun requestRouted(loadingScreen: String, block: () -> Pair<String, Map<String, Any?>>) {
        val requestLanguage = noox.bzr.network.BremoHttp.language()
        stopChatPolling()
        state = reduce(loadingScreen, mapOf("event" to "loading"))
        Thread {
            val (screen, next) = runCatching(block).getOrElse { loadingScreen to mapOf("event" to "error") }
            Handler(Looper.getMainLooper()).post {
                if (screen.isNotEmpty() && requestLanguage == noox.bzr.network.BremoHttp.language()) state = reduce(screen, next)
            }
        }.start()
    }

    private fun request(screen: String, loadingEvent: String = "loading", block: () -> Map<String, Any?>) {
        val requestLanguage = noox.bzr.network.BremoHttp.language()
        if (screen != "SCR-C19") stopChatPolling()
        state = reduce(screen, mapOf("event" to loadingEvent))
        Thread {
            val next = runCatching(block).getOrElse { error ->
                logFailure(screen, error)
                mapOf("event" to "error")
            }
            Handler(Looper.getMainLooper()).post {
                if (requestLanguage == noox.bzr.network.BremoHttp.language()) state = reduce(screen, next)
            }
        }.start()
    }

    private fun token(): String = requireNotNull(session.token)

    private fun String.timeLabel(morning: String, evening: String): String = runCatching {
        OffsetDateTime.parse(this).atZoneSameInstant(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("h:mm a", java.util.Locale.ENGLISH))
            .replace("AM", morning).replace("PM", evening)
    }.getOrDefault(this)

    /** Whole minutes since an ISO-8601 timestamp; -1 when it is missing or unreadable. */
    private fun minutesSince(timestamp: String?): Long = runCatching {
        java.time.Duration.between(java.time.OffsetDateTime.parse(timestamp).toInstant(), java.time.Instant.now()).toMinutes().coerceAtLeast(0)
    }.getOrDefault(-1)

    private fun String.displayDateTime(): String = ifBlank { "" }.let { value ->
        runCatching { OffsetDateTime.parse(value).atZoneSameInstant(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("yyyy-MM-dd · h:mm a", java.util.Locale.forLanguageTag(noox.bzr.network.BremoHttp.language()))) }.getOrDefault(value)
    }

    override fun onCleared() {
        stopChatPolling()
        super.onCleared()
    }

    private companion object {
        /** An amount the server has not set yet (null before pricing) shows as zero. */
        const val ZERO_AMOUNT = "0.00"

        /** The timing code the urgent card preselects. */
        const val URGENT_TIMING = "NOW"
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

private data class MediaDraft(
    val id: Int?,
    val kind: String,
    val state: String,
    val upload: CustomerMediaUpload? = null,
)

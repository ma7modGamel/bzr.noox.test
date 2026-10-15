package noox.bzr.customer

enum class CustomerPhase { Empty, Loading, Content, Error, Offline }

data class CustomerCatalogChoice(val categoryId: Int, val problemTypeId: Int?)

/** Keeps catalog identity selection outside the view so C01/C03 never fall back to list position zero. */
object CustomerCatalogSelection {
    fun category(
        categoryIds: List<Int>,
        problemIdsByCategory: List<List<Int>>,
        selectedIndex: Int,
        currentProblemId: Int?,
    ): CustomerCatalogChoice? {
        val categoryId = categoryIds.getOrNull(selectedIndex) ?: return null
        val problemIds = problemIdsByCategory.getOrNull(selectedIndex).orEmpty()
        return CustomerCatalogChoice(
            categoryId = categoryId,
            problemTypeId = currentProblemId?.takeIf(problemIds::contains),
        )
    }

    fun problem(categoryId: Int?, problemIds: List<Int>, selectedIndex: Int): CustomerCatalogChoice? {
        val resolvedCategoryId = categoryId ?: return null
        val problemTypeId = problemIds.getOrNull(selectedIndex) ?: return null
        return CustomerCatalogChoice(resolvedCategoryId, problemTypeId)
    }
}

data class CustomerUiState(
    val screen: String,
    val phase: CustomerPhase,
    val reviewTagSummary: List<String> = emptyList(),
    val imageGroups: List<List<String>> = emptyList(),
    val beforePhotoUrls: List<String> = emptyList(),
    val afterPhotoUrls: List<String> = emptyList(),
    val proposalPhotoUrl: String = "",
    val arrivalCode: String = "",
    val arrivalCodeUntil: String = "",
    val arrivalCodeRequired: Boolean = false,
    val arrivalExceptionPending: Boolean = false,
    val arrivalCodeFailed: Boolean = false,
    val canContinue: Boolean = false,
    val messageKey: String? = null,
    val descriptionError: String? = null,
    val termsError: String? = null,
    val showPricing: Boolean = false,
    val showMap: Boolean = false,
    val showEta: Boolean = false,
    val showWaiting: Boolean = false,
    val showStepper: Boolean = false,
    val showStatusCard: Boolean = false,
    val etaApproximate: Boolean = false,
    val visibleActions: List<String> = emptyList(),
    val displayStatus: String = "",
    val items: List<String> = emptyList(),
    val itemDetails: List<String> = emptyList(),
    val itemStates: List<String> = emptyList(),
    val options: List<String> = emptyList(),
    val selectedIndex: Int = -1,
    val selectedOptionIndex: Int = -1,
    val fieldValues: List<String> = emptyList(),
    val fieldErrors: List<String> = emptyList(),
    val isBusy: Boolean = false,
    val selectionMode: Boolean = false,
    val isEditing: Boolean = false,
    val isDefault: Boolean = false,
    val showConfirmation: Boolean = false,
    val pendingIndex: Int = -1,
    val countdownSeconds: Int = 0,
    val proposalDeadline: String = "",
    val proposalExpiryMessage: String = "",
    val proposalLoadedUptime: Int = 0,
    val hasPhoto: Boolean = false,
    val stepStates: List<String> = emptyList(),
    val ratings: List<Int> = emptyList(),
    val mapLatitude: Double? = null,
    val mapLongitude: Double? = null,
    val notificationsDenied: Boolean = false,
    val ratingRemindersEnabled: Boolean = true,
    /** DEC-059: catalog icon keys beside `options` (C01, C03). */
    val optionIcons: List<String> = emptyList(),
    val materialOptions: List<String> = emptyList(),
    val selectedMaterialIndex: Int = -1,
    val pricingOptions: List<String> = emptyList(),
    val selectedPricingIndex: Int = -1,
    val noBudget: Boolean = false,
    val providerVerified: Boolean = false,
    /** DEC-071: server codes beside `options` / `materialOptions` / `pricingOptions`, so views pick each card's copy and icon. */
    val optionCodes: List<String> = emptyList(),
    val materialCodes: List<String> = emptyList(),
    val pricingCodes: List<String> = emptyList(),
    /** C01 current-request card (DEC-073): per order, beside `items`. Offer figures are blank when not loaded. */
    val orderIcons: List<String> = emptyList(),
    val orderAgeMinutes: List<Int> = emptyList(),
    val orderOfferCounts: List<Int> = emptyList(),
    val orderLowestOffers: List<String> = emptyList(),
    val unreadNotifications: Int = 0,
    val requestSteps: Int = 5,
    val nowAvailable: Boolean = false,
    val showMaterials: Boolean = false,
    val isInstapay: Boolean = false,
    val isWarranty: Boolean = false,
    val warrantyCoverage: String = "",
    val waitingInfo: String = "",
    val providerAvatarUrl: String = "",
    val portfolioUrls: List<String> = emptyList(),
    val unreadCounts: List<Int> = emptyList(),
    val providersAvailable: List<Boolean> = emptyList(),
    val providerAvailable: Boolean = false,
    val warrantyVisitId: Int? = null,
    val warrantyVisitFinal: Boolean = false,
    val photoCount: Int = 0,
)

object CustomerLogic {
    /**
     * C16 day chips from the real dates (6ب, `design/fixtures/navigation/slot-days.json`): today, tomorrow, then the
     * weekday name. `names` is today, tomorrow, Monday…Sunday.
     */
    fun slotDayLabels(isoDates: List<String>, names: List<String>): List<String> = isoDates.mapIndexed { index, date ->
        when (index) {
            0 -> names[0]
            1 -> names[1]
            else -> names[1 + java.time.LocalDate.parse(date).dayOfWeek.value]
        }
    }

    fun reduce(screen: String, input: Map<String, Any?>): CustomerUiState =
        reduceScreen(screen, input + ("available_actions" to input.strings("available_actions").filter { it != "call" }))
            .copy(reviewTagSummary = input.strings("review_tag_summary"), imageGroups = input.strings("image_groups").map { it.split("\n").filter(String::isNotBlank) }, beforePhotoUrls = input.strings("work_before_urls"), afterPhotoUrls = input.strings("work_after_urls"), proposalPhotoUrl = input.text("proposal_photo_url"), arrivalCode = input.text("arrival_code"), arrivalCodeUntil = input.text("arrival_code_until"), arrivalCodeRequired = input.boolean("arrival_code_required"), arrivalExceptionPending = input.boolean("arrival_exception_pending"), arrivalCodeFailed = input.boolean("arrival_code_failed"))
            .copy(warrantyCoverage = input.text("warranty_coverage"), warrantyVisitId = input.integer("warranty_visit_id").takeIf { it > 0 }, warrantyVisitFinal = input.boolean("warranty_visit_final"))
            .copy(requestSteps = if (input.text("operating_mode") == "staff") 4 else 5, waitingInfo = input.text("waiting_info"))
            .copy(providerAvatarUrl = input.text("provider_avatar_url"), portfolioUrls = input.strings("portfolio_urls"), unreadCounts = input.strings("unread_counts").map { it.toIntOrNull() ?: 0 }, providersAvailable = input.strings("providers_available").map { it == "true" }, providerAvailable = input.boolean("provider_available"))

    private fun reduceScreen(screen: String, input: Map<String, Any?>): CustomerUiState {
        val event = input.text("event")
        if (event == "loading") return CustomerUiState(screen, CustomerPhase.Loading)
        if (event == "error" && (screen != "SCR-C24" || input.strings("ratings").isEmpty())) return CustomerUiState(screen, CustomerPhase.Error, messageKey = "error.body")

        return when (screen) {
            "SCR-C01" -> home(input)
            "SCR-C02" -> account(input)
            "SCR-C03" -> service(input)
            "SCR-C37" -> problem(input)
            "SCR-C04" -> timing(input)
            "SCR-C38" -> budget(input)
            "SCR-C05" -> review(input)
            "SCR-C06" -> offers(input)
            "SCR-C07" -> provider(input)
            "SCR-C08" -> offer(input)
            "SCR-C09" -> tracking(input)
            "SCR-C14" -> addresses(input)
            "SCR-C15" -> addressForm(input)
            "SCR-C16" -> slots(input)
            "SCR-C17" -> media(input)
            "SCR-C18" -> conversations(input)
            "SCR-C19" -> chat(input)
            "SCR-C20" -> cancellation(input)
            "SCR-C21" -> paymentSummary(input)
            "SCR-C22" -> electronicPayment(input)
            "SCR-C23" -> completion(input)
            "SCR-C24" -> rating(input)
            "SCR-C25" -> orders(input)
            "SCR-C26" -> orderHistory(input)
            "SCR-C27" -> dispute(input)
            "SCR-C28" -> providerReport(input)
            "SCR-C29" -> help(input)
            "SCR-C30" -> proposal(input, "SCR-C30")
            "SCR-C31" -> proposal(input, "SCR-C31")
            "SCR-C32" -> notifications(input)
            "SCR-C33" -> accountSettings(input)
            "SCR-C34" -> terms(input)
            "SCR-C35" -> noOffers(input)
            "SCR-C36" -> assignment(input)
            else -> error("Unknown customer screen: $screen")
        }
    }

    private fun home(input: Map<String, Any?>): CustomerUiState {
        val titles = input.strings("order_titles")
        val hasOrders = if (input.containsKey("has_orders")) input.boolean("has_orders") else titles.isNotEmpty()
        return CustomerUiState(
            screen = "SCR-C01",
            phase = if (hasOrders) CustomerPhase.Content else CustomerPhase.Empty,
            canContinue = true,
            showPricing = input.text("operating_mode") != "staff",
            options = input.strings("category_labels"),
            optionIcons = input.strings("category_icons"),
            selectedIndex = input.integer("selected_category_index", -1),
            items = titles,
            itemDetails = input.strings("order_subtitles"),
            itemStates = input.strings("order_statuses"),
            // The default address labels the home header's location row (DEC-063).
            fieldValues = listOf(input.text("customer_name"), input.text("address_label")),
            optionCodes = input.strings("order_status_codes"),
            orderIcons = input.strings("order_icons"),
            orderAgeMinutes = input.strings("order_age_minutes").map { it.toIntOrNull() ?: -1 },
            orderOfferCounts = input.strings("order_offer_counts").map { it.toIntOrNull() ?: 0 },
            orderLowestOffers = input.strings("order_lowest_offers"),
            unreadNotifications = input.integer("unread_notifications"),
        )
    }

    private fun account(input: Map<String, Any?>): CustomerUiState = CustomerUiState(
        screen = "SCR-C02",
        phase = CustomerPhase.Content,
        canContinue = true,
        messageKey = if (input.boolean("is_verified")) "account.verified" else "account.unverified",
        // DEC-076 (Figma 3002:4654): name, phone, completed orders, saved addresses ("" when not loaded).
        fieldValues = listOf(input.text("name"), input.text("phone"), input.text("completed_orders"), input.text("address_count"), input.text("active_orders")),
    )

    /** C03 (DEC-059): catalog categories and the chosen category's problem types; `is_other` needs 10 characters (BR-013). */
    /** C03 (DEC-071, step 1 of 5): the service only. The view model filters by `search_query` before reducing. */
    private fun service(input: Map<String, Any?>): CustomerUiState {
        val labels = input.strings("category_labels")
        val selected = input.integer("selected_category_index", -1)
        val noResults = labels.isEmpty() && input.text("search_query").isNotBlank()
        return CustomerUiState(
            "SCR-C03",
            if (selected < 0) CustomerPhase.Empty else CustomerPhase.Content,
            canContinue = selected >= 0,
            messageKey = if (noResults) "request.search.no_results" else null,
            options = labels,
            optionIcons = input.strings("category_icons"),
            itemDetails = input.strings("category_details"),
            selectedIndex = selected,
            fieldValues = listOf(input.text("search_query")),
        )
    }

    /** C37 (DEC-071, step 2 of 5): problem type, description, media — the former C03 rules. */
    private fun problem(input: Map<String, Any?>): CustomerUiState {
        val selected = input.integer("selected_problem_index", -1)
        val isOther = input.strings("problem_other").getOrNull(selected) == "true"
        val description = input.text("description")
        val error = when {
            selected < 0 -> "request.problem.required"
            isOther && description.trim().length < 10 -> "request.description.other_min"
            description.length > 1000 -> "request.description.max"
            else -> null
        }
        return CustomerUiState(
            "SCR-C37",
            if (selected < 0) CustomerPhase.Empty else CustomerPhase.Content,
            canContinue = error == null,
            descriptionError = error,
            items = listOf(input.text("service_label")),
            itemDetails = input.strings("problem_labels"),
            itemStates = input.strings("media_kinds"),
            optionIcons = listOf(input.text("service_icon")),
            selectedOptionIndex = selected,
            fieldValues = listOf(description, input.integer("media_count").toString()),
        )
    }

    /** C04 (DEC-071, step 3 of 5): address and timing. */
    private fun timing(input: Map<String, Any?>): CustomerUiState {
        val timingType = input.text("timing_type")
        val hasAddress = input.text("address_id").isNotBlank()
        val message = when {
            !hasAddress -> "request.address.required"
            input.containsKey("category_available") && !input.boolean("category_available") -> "request.category.unavailable"
            input.boolean("availability_loading") && timingType != "scheduled" -> "request.timing.checking"
            input.boolean("availability_failed") && timingType != "scheduled" -> "request.timing.check_failed"
            !input.boolean("now_available") && timingType != "scheduled" -> "request.timing.now_unavailable"
            timingType == "scheduled" && input.text("slot_id").isBlank() -> "request.slot.required"
            else -> null
        }
        return CustomerUiState(
            "SCR-C04",
            CustomerPhase.Content,
            canContinue = hasAddress && timingType.isNotBlank() && message == null &&
                (input.text("operating_mode") != "staff" || input.integer("selected_material_index", -1) >= 0),
            nowAvailable = input.boolean("now_available"),
            showMaterials = input.text("operating_mode") == "staff",
            materialOptions = input.strings("material_labels"),
            materialCodes = input.strings("material_codes"),
            selectedMaterialIndex = input.integer("selected_material_index", -1),
            messageKey = message,
            options = input.strings("timing_labels"),
            optionCodes = input.strings("timing_codes"),
            selectedOptionIndex = input.integer("selected_timing_index", -1),
            fieldValues = listOf(input.text("address_label"), "", input.text("slot_label")),
            items = input.strings("address_labels"),
            selectedIndex = input.integer("selected_address_index", -1),
        )
    }

    /** C38 (DEC-071, step 4 of 5): materials; marketplace adds pricing mode and an optional budget (07, BR-015). */
    private fun budget(input: Map<String, Any?>): CustomerUiState {
        val marketplace = input.text("operating_mode") != "staff"
        val budget = input.text("budget").trim()
        val budgetValid = budget.isEmpty() || (budget.toBigDecimalOrNull()?.signum() == 1)
        val materialSelected = input.integer("selected_material_index", -1) >= 0
        val pricingSelected = !marketplace || input.integer("selected_pricing_index", -1) >= 0
        return CustomerUiState(
            "SCR-C38",
            CustomerPhase.Content,
            canContinue = materialSelected && pricingSelected && (!marketplace || budgetValid),
            showPricing = marketplace,
            fieldValues = listOf("", budget),
            fieldErrors = if (marketplace && !budgetValid) listOf("request.budget.invalid") else emptyList(),
            materialOptions = input.strings("material_labels"),
            materialCodes = input.strings("material_codes"),
            selectedMaterialIndex = input.integer("selected_material_index", -1),
            pricingOptions = if (marketplace) input.strings("pricing_labels") else emptyList(),
            pricingCodes = if (marketplace) input.strings("pricing_codes") else emptyList(),
            selectedPricingIndex = input.integer("selected_pricing_index", -1),
            noBudget = input.boolean("no_budget"),
            itemDetails = if (marketplace) input.strings("budget_amounts") else emptyList(),
        )
    }

    /** C05: the real draft (DEC-059) — service, visit, and in marketplace the pricing rows. */
    private fun review(input: Map<String, Any?>): CustomerUiState {
        val accepted = input.boolean("terms_accepted")
        val marketplace = input.text("operating_mode") != "staff"
        val submitting = input.text("event") == "submitting"
        val error = input.text("error_message")
        return CustomerUiState(
            "SCR-C05",
            CustomerPhase.Content,
            canContinue = accepted && !submitting,
            // A refused publish stays here with the server's reason; an empty reason means the shared network error.
            messageKey = if (input.containsKey("error_message")) "request.publish.error" else null,
            fieldValues = if (input.containsKey("error_message")) listOf(error) else emptyList(),
            isBusy = submitting,
            termsError = if (accepted) null else "request.terms.required",
            showPricing = marketplace,
            items = input.strings("service_rows"),
            itemDetails = input.strings("visit_rows"),
            itemStates = if (marketplace) input.strings("pricing_rows") else emptyList(),
        )
    }

    /** C06: offers from `GET /orders/{id}/offers`, one pipe-joined row each: id|name|rating|services|price|detail|badge|verified. */
    private fun offers(input: Map<String, Any?>): CustomerUiState {
        val rows = input.strings("offer_rows")
        val count = if (input.containsKey("offer_rows")) rows.size else input.integer("offer_count")
        return CustomerUiState(
            "SCR-C06",
            if (count == 0) CustomerPhase.Empty else CustomerPhase.Content,
            canContinue = true,
            showPricing = count > 0,
            showEta = input.text("timing_type") == "now",
            visibleActions = input.strings("available_actions"),
            displayStatus = input.text("display_status"),
            items = rows,
            selectedIndex = input.integer("sort_index", 0),
            options = input.strings("sort_labels"), optionCodes = input.strings("sort_codes"),
            countdownSeconds = input.integer("countdown_seconds"),
            fieldValues = listOf(input.text("order_title")),
            pricingOptions = listOf(input.text("pricing_mode")),
            // DEC-073: the request summary C35 shows while no offer has arrived (problem, address, time, budget).
            itemDetails = input.strings("summary_rows"),
            optionIcons = listOf(input.text("order_icon")),
        )
    }

    /** C07 from `GET /providers/{id}?order_id=`: header, stats, specialties, rating breakdown, reviews (name|stars|comment|date). */
    private fun provider(input: Map<String, Any?>): CustomerUiState {
        val available = input.boolean("provider_available")
        return CustomerUiState(
            "SCR-C07",
            if (available) CustomerPhase.Content else CustomerPhase.Empty,
            canContinue = available && "accept_offer" in input.strings("available_actions"),
            visibleActions = input.strings("available_actions"),
            // DEC-073 (Figma 3002:873): [6] review total, [7] the order's category for «فني … موثّق».
            fieldValues = listOf(
                input.text("provider_name"), input.text("provider_rating"), input.text("provider_services"),
                input.text("provider_experience"), input.text("provider_about"), input.text("offer_price"),
                input.text("ratings_count"), input.text("provider_category"),
            ),
            itemStates = input.strings("portfolio_captions"),
            options = input.strings("specialties"),
            itemDetails = input.strings("rating_values"),
            items = input.strings("review_rows"),
            providerVerified = input.boolean("provider_verified"),
        )
    }

    /** C08: the chosen offer, the order, and the payment method (cash or electronic) sent with O-03. */
    private fun offer(input: Map<String, Any?>): CustomerUiState {
        val payment = input.integer("selected_payment_index", -1)
        val actions = input.strings("available_actions")
        return CustomerUiState(
            "SCR-C08",
            CustomerPhase.Content,
            canContinue = "accept_offer" in actions && payment >= 0 && input.text("event") != "submitting",
            showPricing = true,
            showEta = input.text("timing_type") == "now",
            visibleActions = actions,
            isBusy = input.text("event") == "submitting",
            // DEC-073 (Figma 3002:1757): [4] the order's category, [5] who supplies materials.
            fieldValues = listOf(
                input.text("provider_name"), input.text("provider_rating"), input.text("provider_services"), input.text("eta_minutes"),
                input.text("provider_category"), input.text("materials_label"),
            ),
            items = input.strings("offer_rows"),
            itemDetails = input.strings("order_rows"),
            options = input.strings("payment_labels"),
            selectedOptionIndex = payment,
            providerVerified = input.boolean("provider_verified"),
            pricingOptions = listOf(input.text("pricing_mode")),
        )
    }

    /** C36 (employee mode): the published order waiting for assignment, actions from the server. */
    private fun assignment(input: Map<String, Any?>): CustomerUiState = CustomerUiState(
        "SCR-C36",
        CustomerPhase.Content,
        canContinue = true,
        visibleActions = input.strings("available_actions"),
        items = input.strings("order_rows"),
        fieldValues = listOf(input.text("order_title")),
    )

    private fun tracking(input: Map<String, Any?>): CustomerUiState {
        val status = input.text("status")
        val terminal = status in setOf("OPEN", "CANCELLED", "EXPIRED")
        return CustomerUiState(
            "SCR-C09",
            if (input.text("event") == "offline") CustomerPhase.Offline else CustomerPhase.Content,
            canContinue = true,
            messageKey = when (status) {
                "DISPUTED" -> "status.reviewing"
                "CANCELLED" -> "tracking.status.cancelled"
                "EXPIRED" -> "tracking.status.expired"
                "OPEN" -> "tracking.status.open"
                else -> null
            },
            showMap = status == "ON_THE_WAY",
            showEta = status == "ON_THE_WAY",
            showWaiting = status == "CONFIRMED",
            showStepper = !terminal && input.boolean("stepper"),
            showStatusCard = terminal,
            etaApproximate = input.boolean("eta_approximate"),
            visibleActions = input.strings("available_actions"),
            displayStatus = input.text("display_status"),
            items = input.strings("summary"),
            stepStates = input.strings("step_states"),
            itemDetails = input.strings("execution_labels"), options = input.strings("execution_times"), itemStates = input.strings("execution_states"),
            // DEC-076 (Figma 3002:1631/1903/2055/2540): number, address, problem, agreed price, materials, provider name,
            // rating, completed jobs, category, approved total, slot.
            fieldValues = listOf(
                input.text("order_number"), input.text("address"), input.text("order_title"), input.text("agreed_price"),
                input.text("materials_label"), input.text("provider_name"), input.text("provider_rating"),
                input.text("provider_completed"), input.text("provider_category"), input.text("final_amount"), input.text("slot_label"),
                input.text("completion_estimate_at"), if (input.boolean("completion_estimate_overdue")) "overdue" else "",
            ),
            providerVerified = input.boolean("provider_verified"),
            optionCodes = listOf(input.text("status"), input.text("timing_type")),
        )
    }

    private fun addresses(input: Map<String, Any?>): CustomerUiState {
        val labels = input.strings("address_labels")
        val event = input.text("event")
        val busy = event == "deleting"
        val selected = input.integer("selected_index", -1)
        return CustomerUiState(
            screen = "SCR-C14",
            phase = if (labels.isEmpty()) CustomerPhase.Empty else CustomerPhase.Content,
            canContinue = !busy && (!input.boolean("selection_mode") || selected >= 0),
            items = labels,
            itemDetails = input.strings("address_details"),
            selectedIndex = selected,
            isBusy = busy,
            selectionMode = input.boolean("selection_mode"),
            showConfirmation = event == "confirm_delete" || busy,
            pendingIndex = input.integer("pending_delete_index", -1),
        )
    }

    private fun addressForm(input: Map<String, Any?>): CustomerUiState {
        val label = input.text("label")
        val area = input.text("area_id")
        val details = input.text("address_text")
        val latitude = input.text("lat").toDoubleOrNull()
        val longitude = input.text("lng").toDoubleOrNull()
        val errors = buildList {
            if (label.isBlank() || label.length > 50) add("address.validation.label_required")
            if (area.isBlank()) add("address.validation.area_required")
            if (details.isBlank() || details.length > 255) add("address.validation.details_required")
            if (latitude == null || latitude !in -90.0..90.0 || longitude == null || longitude !in -180.0..180.0) {
                add("address.validation.location_required")
            }
            if (area.isNotBlank() && !input.boolean("area_served")) add("address.validation.unserved_area")
        }
        val busy = input.text("event") == "saving"
        return CustomerUiState(
            screen = "SCR-C15",
            phase = if (label.isBlank() && details.isBlank()) CustomerPhase.Empty else CustomerPhase.Content,
            canContinue = errors.isEmpty() && !busy,
            messageKey = errors.firstOrNull { it == "address.validation.unserved_area" },
            showMap = true,
            options = input.strings("area_labels"),
            selectedOptionIndex = input.integer("selected_area_index", -1),
            fieldValues = listOf(
                label, details, input.text("building"), input.text("floor"),
                input.text("apartment"), input.text("landmark"),
            ),
            fieldErrors = errors,
            isBusy = busy,
            isEditing = input.boolean("editing"),
            isDefault = input.boolean("is_default"),
            mapLatitude = latitude,
            mapLongitude = longitude,
        )
    }

    private fun slots(input: Map<String, Any?>): CustomerUiState {
        val periods = input.strings("slot_labels")
        val changingDay = input.text("event") == "changing_day"
        val selected = input.integer("selected_slot_index", -1)
        return CustomerUiState(
            screen = "SCR-C16",
            phase = when {
                changingDay -> CustomerPhase.Loading
                periods.isEmpty() -> CustomerPhase.Empty
                else -> CustomerPhase.Content
            },
            canContinue = !changingDay && selected >= 0,
            items = periods,
            options = input.strings("day_labels"),
            selectedIndex = selected,
            selectedOptionIndex = input.integer("selected_day_index", 0),
            isBusy = changingDay,
        )
    }

    private fun media(input: Map<String, Any?>): CustomerUiState {
        val kinds = input.strings("media_kinds")
        val states = input.strings("media_states")
        val errors = buildList {
            if (input.integer("photo_count") > 5) add("media.validation.photo_limit")
            if (input.integer("video_count") > 1) add("media.validation.video_limit")
            if (input.integer("audio_count") > 1) add("media.validation.audio_limit")
            if ("failed" in states) add("media.validation.rejected")
        }
        val selectionError = input.text("media_error").takeIf { it == "media.validation.video_duration" || it == "media.validation.video_unreadable" }
        val busy = input.boolean("recording") || "uploading" in states
        return CustomerUiState(
            screen = "SCR-C17",
            phase = if (kinds.isEmpty()) CustomerPhase.Empty else CustomerPhase.Content,
            canContinue = errors.none { it != "media.validation.rejected" } && !busy,
            messageKey = when {
                input.boolean("recording") -> "media.recording"
                errors.isNotEmpty() -> errors.first()
                else -> null
            },
            items = kinds,
            itemStates = states,
            fieldErrors = listOfNotNull(selectionError) + errors,
            isBusy = busy,
        )
    }

    private fun cancellation(input: Map<String, Any?>): CustomerUiState {
        val selected = input.integer("selected_reason_index", -1)
        val busy = input.text("event") == "submitting"
        val actions = input.strings("available_actions")
        return CustomerUiState(
            screen = "SCR-C20", phase = CustomerPhase.Content,
            canContinue = selected >= 0 && "cancel" in actions && !busy,
            visibleActions = actions, options = input.strings("reason_labels"),
            selectedOptionIndex = selected, fieldValues = listOf(input.text("note")), isBusy = busy,
        )
    }

    private fun dispute(input: Map<String, Any?>): CustomerUiState {
        val event = input.text("event")
        val warranty = input.boolean("warranty")
        val selected = input.integer("selected_reason_index", -1)
        val description = input.text("description")
        val existing = input.boolean("existing")
        val busy = event == "submitting"
        return CustomerUiState(
            screen = "SCR-C27",
            phase = CustomerPhase.Content,
            canContinue = !existing && description.length <= 1000 && !busy &&
                if (warranty) description.trim().length >= 10 && "request_warranty" in input.strings("available_actions")
                else selected >= 0 && description.isNotBlank() && "open_dispute" in input.strings("available_actions"),
            isWarranty = warranty,
            messageKey = if (event == "success") (if (warranty) "warranty.success" else "dispute.success") else null,
            descriptionError = if (!existing && description.length > 1000) "request.description.max" else null,
            visibleActions = input.strings("available_actions"),
            items = input.strings("details"),
            itemStates = input.strings("status_details"),
            options = input.strings("reason_labels"),
            selectedOptionIndex = selected,
            fieldValues = listOf(description),
            isBusy = busy,
            hasPhoto = input.integer("photo_count") > 0,
        )
    }

    private fun providerReport(input: Map<String, Any?>): CustomerUiState {
        val event = input.text("event")
        val selected = input.integer("selected_reason_index", -1)
        val description = input.text("description")
        val busy = event == "submitting"
        return CustomerUiState(
            screen = "SCR-C28",
            phase = CustomerPhase.Content,
            canContinue = selected >= 0 && description.length <= 1000 &&
                "report_provider" in input.strings("available_actions") && !busy,
            messageKey = if (event == "success") "provider.report.success" else null,
            descriptionError = if (description.length > 1000) "request.description.max" else null,
            visibleActions = input.strings("available_actions"),
            options = input.strings("reason_labels"),
            selectedOptionIndex = selected,
            fieldValues = listOf(description),
            isBusy = busy,
        )
    }

    private fun help(input: Map<String, Any?>): CustomerUiState {
        val subject = input.text("subject")
        val message = input.text("message")
        val busy = input.text("event") in listOf("submitting", "uploading")
        val errors = buildList {
            if (subject.isNotEmpty() && subject.length !in 5..120) add("help.validation.subject")
            if (message.isNotEmpty() && message.length !in 10..2000) add("help.validation.message")
        }
        return CustomerUiState(
            screen = "SCR-C29",
            phase = CustomerPhase.Content,
            canContinue = subject.length in 5..120 && message.length in 10..2000 && !busy,
            messageKey = if (input.text("event") == "success") "help.success" else null,
            items = input.strings("faq_questions"),
            itemDetails = input.strings("faq_answers"),
            selectedIndex = input.integer("expanded_index", -1),
            fieldValues = listOf(subject, message),
            fieldErrors = errors,
            isBusy = busy,
        )
    }

    private fun notifications(input: Map<String, Any?>): CustomerUiState {
        val titles = input.strings("notification_titles")
        return CustomerUiState(
            screen = "SCR-C32",
            phase = if (titles.isEmpty()) CustomerPhase.Empty else CustomerPhase.Content,
            canContinue = titles.isNotEmpty(),
            items = titles,
            itemDetails = input.strings("notification_bodies"),
            itemStates = input.strings("notification_states"),
            options = input.strings("deep_links"),
            fieldValues = input.strings("notification_times"),
            isBusy = input.text("event") == "marking_read",
            notificationsDenied = input.boolean("notifications_denied"),
            // DEC-076 (Figma 3002:3608): NTF code per row, "today" / "earlier", and the age in minutes.
            optionCodes = input.strings("notification_codes"), stepStates = input.strings("notification_days"),
            orderAgeMinutes = input.strings("notification_ages").map { it.toIntOrNull() ?: -1 },
        )
    }

    private fun accountSettings(input: Map<String, Any?>): CustomerUiState {
        val mode = input.text("mode").ifBlank { "overview" }
        val values = input.strings("field_values")
        val errors = buildList {
            if (mode == "edit" && values.getOrNull(0).orEmpty().trim().length < 2) add("account.validation.name")
            if (mode == "password" && values.getOrNull(0).orEmpty().isBlank()) add("account.validation.current_password")
            if (mode == "password" && values.getOrNull(1).orEmpty().length < 8) add("account.validation.new_password")
            if (mode == "password" && values.getOrNull(1) != values.getOrNull(2)) add("account.validation.password_confirmation")
        }
        val busy = input.text("event") == "submitting"
        return CustomerUiState(
            screen = "SCR-C33",
            phase = CustomerPhase.Content,
            canContinue = !busy && (mode == "overview" || errors.isEmpty()),
            messageKey = when (input.text("event")) {
                "success" -> "account.settings.success"
                "active_order" -> "account.delete.active_order"
                else -> null
            },
            visibleActions = input.strings("available_actions"),
            items = listOf(mode),
            fieldValues = values,
            fieldErrors = errors,
            isBusy = busy,
            isEditing = mode != "overview",
            showConfirmation = input.text("event") == "confirm_delete" || (busy && mode == "delete"),
            ratingRemindersEnabled = input["rating_reminders_enabled"] as? Boolean ?: true,
        )
    }

    private fun terms(input: Map<String, Any?>): CustomerUiState = CustomerUiState(
        screen = "SCR-C34",
        phase = CustomerPhase.Content,
        canContinue = input.text("body").isNotBlank(),
        items = listOf(input.text("body")),
        itemDetails = listOf(input.text("version"), input.text("effective_at")),
    )

    private fun noOffers(input: Map<String, Any?>): CustomerUiState {
        val actions = input.strings("available_actions")
        return CustomerUiState(
            screen = "SCR-C35",
            phase = CustomerPhase.Empty,
            canContinue = actions.isNotEmpty() && input.text("event") != "submitting",
            visibleActions = actions,
            isBusy = input.text("event") == "submitting",
            showEta = input.text("timing_type") == "now",
            itemDetails = input.strings("summary_rows"),
            optionIcons = listOf(input.text("order_icon")),
        )
    }

    private fun conversations(input: Map<String, Any?>): CustomerUiState {
        val providers = input.strings("provider_names")
        return CustomerUiState(
            screen = "SCR-C18",
            phase = if (providers.isEmpty()) CustomerPhase.Empty else CustomerPhase.Content,
            canContinue = providers.isNotEmpty(),
            items = providers,
            itemDetails = input.strings("order_summaries"),
            itemStates = input.strings("conversation_statuses"),
            options = input.strings("last_messages"),
            fieldValues = input.strings("last_message_times"),
            fieldErrors = input.strings("provider_verified"),
            // DEC-076 (Figma 3002:2924): "mine" when the last message is the customer's own (sent tick).
            optionCodes = input.strings("last_message_mine"),
        )
    }

    private fun chat(input: Map<String, Any?>): CustomerUiState {
        val messages = input.strings("message_bodies")
        val status = input.text("conversation_status")
        val event = input.text("event")
        val busy = event == "sending"
        val draft = input.text("draft")
        return CustomerUiState(
            screen = "SCR-C19",
            phase = if (messages.isEmpty()) CustomerPhase.Empty else CustomerPhase.Content,
            canContinue = status == "OPEN" && draft.isNotBlank() && draft.length <= 1000 && !busy,
            messageKey = when {
                status == "READ_ONLY" -> "chat.read_only"
                input.strings("message_kinds").any { it.endsWith("_blocked") } -> "chat.masking.notice"
                else -> null
            },
            items = messages,
            itemDetails = input.strings("message_times"),
            itemStates = input.strings("message_kinds"),
            options = listOf(
                input.text("counterpart_name").ifBlank { input.text("provider_name") },
                input.text("order_summary"),
                status,
                input.text("viewer_role"),
            ),
            fieldValues = listOf(draft),
            providerVerified = input.boolean("provider_verified"),
            isBusy = busy,
        )
    }

    private fun orders(input: Map<String, Any?>): CustomerUiState {
        val titles = input.strings("order_titles")
        val tab = input.text("tab")
        val busy = input.text("event") == "loading_more"
        return CustomerUiState(
            screen = "SCR-C25",
            phase = if (titles.isEmpty()) CustomerPhase.Empty else CustomerPhase.Content,
            canContinue = titles.isNotEmpty() && !busy,
            messageKey = if (titles.isEmpty()) "orders.$tab.empty.title" else null,
            items = titles,
            itemDetails = input.strings("order_subtitles"),
            itemStates = input.strings("order_statuses"),
            selectedOptionIndex = when (tab) { "past" -> 1; "cancelled" -> 2; else -> 0 },
            isBusy = busy,
            // DEC-076 (Figma 3002:3889): per order, status code, category icon, the card row
            // "number|place|provider|verified|rating|amount|chat|phone" and, while open, the offer figures.
            optionCodes = input.strings("order_status_codes"), orderIcons = input.strings("order_icons"),
            options = input.strings("order_rows"),
            orderOfferCounts = input.strings("order_offer_counts").map { it.toIntOrNull() ?: 0 },
            orderLowestOffers = input.strings("order_lowest_offers"),
        )
    }

    private fun orderHistory(input: Map<String, Any?>): CustomerUiState {
        val actions = input.strings("available_actions")
        val status = input.text("status")
        return CustomerUiState(
            screen = "SCR-C26",
            phase = CustomerPhase.Content,
            canContinue = actions.isNotEmpty(),
            messageKey = when {
                status == "CANCELLED" -> "order.cancelled.reason"
                status == "EXPIRED" -> "order.expired.reason"
                else -> null
            },
            displayStatus = input.text("display_status"),
            items = input.strings("summary"),
            itemDetails = input.strings("proposal_summaries"),
            options = listOf(input.text("provider_name"), input.text("termination_reason")),
            visibleActions = actions,
        )
    }

    private fun paymentSummary(input: Map<String, Any?>): CustomerUiState {
        val method = input.text("payment_method")
        val actions = input.strings("available_actions")
        val busy = input.text("event") == "switching"
        return CustomerUiState(
            screen = "SCR-C21", phase = CustomerPhase.Content,
            canContinue = method == "ELECTRONIC" && ("pay_electronic" in actions || "submit_instapay_transfer" in actions) && !busy,
            items = input.strings("amounts"), options = input.strings("method_labels"),
            selectedOptionIndex = input.integer("selected_method_index", -1),
            visibleActions = actions, isBusy = busy,
            // DEC-076 (Figma 3002:4134): provider name, problem, address.
            fieldValues = listOf(input.text("provider_name"), input.text("order_title"), input.text("address")),
            providerVerified = input.boolean("provider_verified"),
            optionCodes = input.strings("method_codes"),
        )
    }

    private fun electronicPayment(input: Map<String, Any?>): CustomerUiState {
        val event = input.text("event")
        val status = input.text("payment_status")
        val selected = input.integer("selected_channel_index", -1)
        val busy = event in listOf("creating", "uploading")
        val instapay = input.boolean("is_instapay")
        val actions = input.strings("available_actions")
        val message = when {
            event == "upload_failed" -> "media.upload_error"
            status == "PENDING_VERIFICATION" -> "payment.instapay.pending"
            busy -> "payment.creating"
            status == "SUCCEEDED" -> "payment.success"
            status == "FAILED" -> "payment.failed"
            status == "EXPIRED" -> "payment.expired"
            status == "PENDING" && input.text("reference_number").isNotBlank() -> "payment.kiosk.ready"
            status == "PENDING" -> "payment.checkout.ready"
            else -> null
        }
        return CustomerUiState(
            screen = "SCR-C22", phase = CustomerPhase.Content,
            canContinue = !busy && status !in listOf("PENDING", "PENDING_VERIFICATION", "SUCCEEDED") && selected >= 0 &&
                if (instapay) "submit_instapay_transfer" in actions && input.boolean("receipt_uploaded") && input.text("transfer_reference").trim().matches(Regex("[A-Za-z0-9_ -]{4,64}"))
                else "pay_electronic" in actions,
            isInstapay = instapay, hasPhoto = input.boolean("receipt_uploaded"),
            fieldValues = listOf(input.text("transfer_reference"), input.text("instapay_address"), input.text("instapay_name"), input.text("order_reference"), input.text("failure_reason_label")),
            messageKey = message, visibleActions = actions,
            items = listOf(input.text("total"), input.text("reference_number"), input.text("checkout_url")),
            itemStates = listOf(status), options = input.strings("channel_labels"),
            selectedOptionIndex = selected, isBusy = busy,
            countdownSeconds = input.integer("countdown_seconds"),
        )
    }

    private fun completion(input: Map<String, Any?>): CustomerUiState {
        val actions = input.strings("available_actions")
        val busy = input.text("event") == "submitting"
        return CustomerUiState(
            screen = "SCR-C23", phase = CustomerPhase.Content,
            canContinue = "confirm_completion" in actions && !busy,
            items = input.strings("summary"), visibleActions = actions, isBusy = busy,
            // DEC-076 (Figma 3002:4134): provider name, problem, address, payment method label.
            fieldValues = listOf(input.text("provider_name"), input.text("order_title"), input.text("address"), input.text("method_label")),
            providerVerified = input.boolean("provider_verified"),
        )
    }

    private fun rating(input: Map<String, Any?>): CustomerUiState {
        val ratings = input.strings("ratings").mapNotNull(String::toIntOrNull)
        val complete = ratings.size == 3 && ratings.all { it in 1..5 }
        val commentLength = input.integer("comment_length", input.text("comment").length)
        val busy = input.text("event") in listOf("submitting", "uploading")
        val errors = buildList {
            if (!complete) add("rating.validation.required")
            if (commentLength > 500) add("rating.validation.comment_max")
        }
        val message = when (input.text("event")) {
            "upload_failed" -> "media.upload_error"
            "error" -> "error.body"
            "window_closed" -> "rating.window_closed"
            "already_exists" -> "rating.already_exists"
            "success" -> "rating.success"
            else -> null
        }
        return CustomerUiState(
            screen = "SCR-C24",
            phase = if (input.text("event") == "loaded" && ratings.all { it == 0 }) CustomerPhase.Empty else CustomerPhase.Content,
            canContinue = complete && errors.isEmpty() && !busy && "rate" in input.strings("available_actions"),
            messageKey = message, visibleActions = input.strings("available_actions"),
            // DEC-076 (Figma 3002:4475): comment, provider name, job title; `items` are the chosen quick tags (B-04).
            fieldValues = listOf(input.text("comment"), input.text("provider_name"), input.text("order_title")), fieldErrors = errors,
            isBusy = busy, ratings = ratings, items = input.strings("tags"), providerVerified = input.boolean("provider_verified"),
            photoCount = input.integer("photo_count"),
            options = input.strings("tag_labels"), optionCodes = input.strings("tag_codes"),
        )
    }

    private fun proposal(input: Map<String, Any?>, screen: String): CustomerUiState {
        val actions = input.strings("available_actions")
        val expired = input.boolean("expired")
        val busy = input.text("event") == "submitting"
        return CustomerUiState(
            screen = screen, phase = CustomerPhase.Content,
            canContinue = !expired && !busy && actions.any { it == "approve_proposal" || it == "reject_proposal" },
            messageKey = when { expired -> "proposal.expired"; busy -> "proposal.deciding"; else -> null },
            visibleActions = actions,
            items = listOf(input.text("type_label"), input.text("amount"), input.text("reason"), input.text("total")),
            isBusy = busy, countdownSeconds = input.integer("countdown_seconds"), hasPhoto = input.boolean("has_photo"),
            // DEC-076 (Figma 3002:2264 / 3002:4029): provider name, rating, category, the agreed price before this proposal.
            fieldValues = listOf(input.text("provider_name"), input.text("provider_rating"), input.text("provider_category"), input.text("original_amount")),
            providerVerified = input.boolean("provider_verified"),
            proposalDeadline = input.text("proposal_deadline"),
            proposalExpiryMessage = input.text("proposal_expiry_message"),
            proposalLoadedUptime = input.integer("proposal_loaded_uptime"),
        )
    }

    fun proposalRemaining(state: CustomerUiState, uptimeSeconds: Int): Int =
        (state.countdownSeconds - if (state.proposalLoadedUptime > 0) (uptimeSeconds - state.proposalLoadedUptime).coerceAtLeast(0) else 0).coerceAtLeast(0)

    private fun Map<String, Any?>.text(key: String): String = this[key] as? String ?: ""
    private fun Map<String, Any?>.boolean(key: String): Boolean = this[key] as? Boolean ?: false
    private fun Map<String, Any?>.integer(key: String, default: Int = 0): Int = (this[key] as? Number)?.toInt() ?: default
    private fun Map<String, Any?>.strings(key: String): List<String> = (this[key] as? List<*>)?.filterIsInstance<String>().orEmpty()
}

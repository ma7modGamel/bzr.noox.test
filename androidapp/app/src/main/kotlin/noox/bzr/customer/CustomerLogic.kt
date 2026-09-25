package noox.bzr.customer

enum class CustomerPhase { Empty, Loading, Content, Error, Offline }

data class CustomerUiState(
    val screen: String,
    val phase: CustomerPhase,
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
    val hasPhoto: Boolean = false,
    val stepStates: List<String> = emptyList(),
    val ratings: List<Int> = emptyList(),
)

object CustomerLogic {
    fun reduce(screen: String, input: Map<String, Any?>): CustomerUiState {
        val event = input.text("event")
        if (event == "loading") return CustomerUiState(screen, CustomerPhase.Loading)
        if (event == "error") return CustomerUiState(screen, CustomerPhase.Error, messageKey = "error.body")

        return when (screen) {
            "SCR-C01" -> home(input)
            "SCR-C02" -> account(input)
            "SCR-C03" -> problem(input)
            "SCR-C04" -> timing(input)
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
            else -> error("Unknown customer screen: $screen")
        }
    }

    private fun home(input: Map<String, Any?>): CustomerUiState = CustomerUiState(
        screen = "SCR-C01",
        phase = if (input.boolean("has_orders")) CustomerPhase.Content else CustomerPhase.Empty,
        canContinue = true,
        showPricing = input.text("operating_mode") != "staff",
    )

    private fun account(input: Map<String, Any?>): CustomerUiState = CustomerUiState(
        screen = "SCR-C02",
        phase = CustomerPhase.Content,
        canContinue = true,
        messageKey = if (input.boolean("is_verified")) "account.verified" else "account.unverified",
    )

    private fun problem(input: Map<String, Any?>): CustomerUiState {
        val problemType = input.text("problem_type_id")
        val description = input.text("description")
        val error = when {
            problemType.isBlank() -> "request.problem.required"
            problemType == "other" && description.length < 10 -> "request.description.other_min"
            description.length > 1000 -> "request.description.max"
            else -> null
        }
        return CustomerUiState(
            "SCR-C03",
            if (problemType.isBlank()) CustomerPhase.Empty else CustomerPhase.Content,
            canContinue = error == null,
            descriptionError = error,
        )
    }

    private fun timing(input: Map<String, Any?>): CustomerUiState {
        val timingType = input.text("timing_type")
        val hasAddress = input.text("address_id").isNotBlank()
        val message = when {
            !hasAddress -> "request.address.required"
            timingType == "now" && !input.boolean("now_available") -> "request.timing.now_unavailable"
            timingType == "scheduled" && input.text("slot_id").isBlank() -> "request.slot.required"
            else -> null
        }
        return CustomerUiState(
            "SCR-C04",
            CustomerPhase.Content,
            canContinue = hasAddress && timingType.isNotBlank() && message == null,
            messageKey = message,
            showPricing = input.text("operating_mode") != "staff",
            options = input.strings("timing_labels"),
            selectedOptionIndex = input.integer("selected_timing_index", -1),
        )
    }

    private fun review(input: Map<String, Any?>): CustomerUiState {
        val accepted = input.boolean("terms_accepted")
        return CustomerUiState(
            "SCR-C05",
            CustomerPhase.Content,
            canContinue = accepted,
            termsError = if (accepted) null else "request.terms.required",
            showPricing = input.text("operating_mode") != "staff",
        )
    }

    private fun offers(input: Map<String, Any?>): CustomerUiState = CustomerUiState(
        "SCR-C06",
        if (input.integer("offer_count") == 0) CustomerPhase.Empty else CustomerPhase.Content,
        canContinue = true,
        showPricing = input.integer("offer_count") > 0,
        showEta = input.text("timing_type") == "now",
        visibleActions = input.strings("available_actions"),
    )

    private fun provider(input: Map<String, Any?>): CustomerUiState {
        val available = input.boolean("provider_available")
        return CustomerUiState(
            "SCR-C07",
            if (available) CustomerPhase.Content else CustomerPhase.Empty,
            canContinue = available && "accept_offer" in input.strings("available_actions"),
            visibleActions = input.strings("available_actions"),
        )
    }

    private fun offer(input: Map<String, Any?>): CustomerUiState = CustomerUiState(
        "SCR-C08",
        CustomerPhase.Content,
        canContinue = "accept_offer" in input.strings("available_actions"),
        showPricing = true,
        showEta = input.text("timing_type") == "now",
        visibleActions = input.strings("available_actions"),
    )

    private fun tracking(input: Map<String, Any?>): CustomerUiState {
        val status = input.text("status")
        val terminal = status in setOf("OPEN", "CANCELLED", "EXPIRED")
        return CustomerUiState(
            "SCR-C09",
            if (input.text("event") == "offline") CustomerPhase.Offline else CustomerPhase.Content,
            canContinue = true,
            messageKey = if (status == "DISPUTED") "status.reviewing" else null,
            showMap = status == "ON_THE_WAY",
            showEta = status == "ON_THE_WAY",
            showWaiting = status == "CONFIRMED",
            showStepper = !terminal && input.boolean("stepper"),
            showStatusCard = terminal,
            etaApproximate = input.boolean("eta_approximate"),
            visibleActions = input.strings("available_actions"),
            displayStatus = input.text("display_status"),
            stepStates = input.strings("step_states"),
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
            fieldErrors = errors,
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
        val selected = input.integer("selected_reason_index", -1)
        val description = input.text("description")
        val existing = input.boolean("existing")
        val busy = event == "submitting"
        return CustomerUiState(
            screen = "SCR-C27",
            phase = CustomerPhase.Content,
            canContinue = !existing && selected >= 0 && description.isNotBlank() && description.length <= 1000 &&
                "open_dispute" in input.strings("available_actions") && !busy,
            messageKey = if (event == "success") "dispute.success" else null,
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
        val busy = input.text("event") == "submitting"
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
            options = listOf(input.text("provider_name"), input.text("order_summary"), status),
            fieldValues = listOf(draft),
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
            selectedOptionIndex = if (tab == "past") 1 else 0,
            isBusy = busy,
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
            canContinue = method == "ELECTRONIC" && "pay_electronic" in actions && !busy,
            items = input.strings("amounts"), options = input.strings("method_labels"),
            selectedOptionIndex = input.integer("selected_method_index", -1),
            visibleActions = actions, isBusy = busy,
        )
    }

    private fun electronicPayment(input: Map<String, Any?>): CustomerUiState {
        val event = input.text("event")
        val status = input.text("payment_status")
        val selected = input.integer("selected_channel_index", -1)
        val busy = event == "creating"
        val actions = input.strings("available_actions")
        val message = when {
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
            canContinue = !busy && status != "PENDING" && status != "SUCCEEDED" && selected >= 0 && "pay_electronic" in actions,
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
        )
    }

    private fun rating(input: Map<String, Any?>): CustomerUiState {
        val ratings = input.strings("ratings").mapNotNull(String::toIntOrNull)
        val complete = ratings.size == 3 && ratings.all { it in 1..5 }
        val commentLength = input.integer("comment_length", input.text("comment").length)
        val busy = input.text("event") == "submitting"
        val errors = buildList {
            if (!complete) add("rating.validation.required")
            if (commentLength > 500) add("rating.validation.comment_max")
        }
        val message = when (input.text("event")) {
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
            fieldValues = listOf(input.text("comment")), fieldErrors = errors,
            isBusy = busy, ratings = ratings,
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
        )
    }

    private fun Map<String, Any?>.text(key: String): String = this[key] as? String ?: ""
    private fun Map<String, Any?>.boolean(key: String): Boolean = this[key] as? Boolean ?: false
    private fun Map<String, Any?>.integer(key: String, default: Int = 0): Int = (this[key] as? Number)?.toInt() ?: default
    private fun Map<String, Any?>.strings(key: String): List<String> = (this[key] as? List<*>)?.filterIsInstance<String>().orEmpty()
}

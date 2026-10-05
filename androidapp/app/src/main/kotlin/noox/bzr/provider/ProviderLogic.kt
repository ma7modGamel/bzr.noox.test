package noox.bzr.provider

import java.math.BigDecimal

enum class ProviderPhase { Empty, Loading, Content, Error }

data class ProviderUiState(
    val screen: String,
    val phase: ProviderPhase,
    val canContinue: Boolean = false,
    val messageKey: String? = null,
    val fieldErrors: List<String> = emptyList(),
    val visibleActions: List<String> = emptyList(),
    val items: List<String> = emptyList(),
    val options: List<String> = emptyList(),
    val secondaryOptions: List<String> = emptyList(),
    val selectedCount: Int = 0,
    val selectedPrimaryIndices: List<Int> = emptyList(),
    val selectedSecondaryIndices: List<Int> = emptyList(),
    val isBusy: Boolean = false,
    val hasProfilePhoto: Boolean = false,
    val hasIdFront: Boolean = false,
    val hasIdBack: Boolean = false,
    val fieldValues: List<String> = emptyList(),
    val availableNow: Boolean = false,
    val activeOrderId: Int? = null,
    val displayStatus: String = "",
    val unreadCount: Int = 0,
    val stepStates: List<String> = emptyList(),
    val selectedOptionIndex: Int = -1,
    val optionCodes: List<String> = emptyList(),
    val showPriceGuide: Boolean = false,
    val showOutsideReason: Boolean = false,
    val hasPhoto: Boolean = false,
    val currentAction: String = "",
    val priceGuideMinimum: String = "",
    val priceGuideMaximum: String = "",
    val customerName: String = "",
    val customerRating: String = "",
    val itemDetails: List<String> = emptyList(),
    val itemStates: List<String> = emptyList(),
    val areaOptions: List<String> = emptyList(),
)

/** Pure provider state and validation. Network and Android UI code never own business decisions. */
object ProviderLogic {
    fun reduce(screen: String, input: Map<String, Any?>): ProviderUiState {
        return when (input.text("event")) {
            "loading" -> ProviderUiState(screen, ProviderPhase.Loading)
            "error" -> ProviderUiState(screen, ProviderPhase.Error, messageKey = "error.body")
            else -> when (screen) {
                "SCR-P01" -> intro(input)
                "SCR-P02" -> profile(input)
                "SCR-P03" -> identity(input)
                "SCR-P04" -> catalog(input)
                "SCR-P05" -> areas(input)
                "SCR-P06" -> payout(input)
                "SCR-P07" -> status(input)
                "SCR-P08" -> home(input)
                "SCR-P09" -> availableRequest(input)
                "SCR-P10" -> offerForm(input)
                "SCR-P11" -> offers(input)
                "SCR-P12", "SCR-P13", "SCR-P14", "SCR-P15", "SCR-P16" -> activeOrder(screen, input)
                "SCR-P17" -> rating(input)
                "SCR-P18" -> earnings(input)
                "SCR-P19" -> providerProfile(input)
                "SCR-P20" -> proposal(input)
                "SCR-P21" -> unable(input)
                "SCR-C19" -> chat(input)
                else -> error("Unknown provider screen: $screen")
            }
        }
    }

    private fun intro(input: Map<String, Any?>): ProviderUiState {
        val status = input.text("status")
        return ProviderUiState(
            screen = "SCR-P01",
            phase = ProviderPhase.Content,
            canContinue = true,
            messageKey = statusKey(status),
            visibleActions = input.strings("available_actions"),
        )
    }

    private fun profile(input: Map<String, Any?>): ProviderUiState {
        val hasPhoto = input.boolean("profile_photo_uploaded")
        val experience = input.text("experience_years").toIntOrNull()
        val bioLength = input.integer("bio_length", input.text("bio").length)
        val errors = buildList {
            if (!hasPhoto) add("provider.onboarding.profile_photo.required")
            if (experience == null || experience !in 0..50) add("provider.onboarding.experience.invalid")
            if (bioLength > 300) add("provider.onboarding.bio.max")
        }
        return ProviderUiState(
            screen = "SCR-P02",
            phase = if (input.text("experience_years").isBlank() && input.text("bio").isBlank()) ProviderPhase.Empty else ProviderPhase.Content,
            canContinue = errors.isEmpty() && input.text("event") != "uploading",
            messageKey = if (input.text("event") == "media_error") "media.error.invalid" else null,
            fieldErrors = errors,
            isBusy = input.text("event") == "uploading",
            hasProfilePhoto = hasPhoto,
            fieldValues = listOf(input.text("experience_years"), input.text("bio")),
        )
    }

    private fun identity(input: Map<String, Any?>): ProviderUiState {
        val hasFront = input.boolean("id_front_uploaded")
        val hasBack = input.boolean("id_back_uploaded")
        val errors = if (hasFront && hasBack) emptyList() else listOf("provider.onboarding.identity.required")
        return ProviderUiState(
            screen = "SCR-P03",
            phase = if (!hasFront && !hasBack && input.text("event") != "media_error") ProviderPhase.Empty else ProviderPhase.Content,
            canContinue = errors.isEmpty() && input.text("event") != "uploading",
            messageKey = if (input.text("event") == "media_error") "media.error.invalid" else null,
            fieldErrors = errors,
            isBusy = input.text("event") == "uploading",
            hasIdFront = hasFront,
            hasIdBack = hasBack,
        )
    }

    private fun catalog(input: Map<String, Any?>): ProviderUiState {
        val categories = input.strings("category_ids")
        val specialties = input.strings("specialty_ids")
        val errors = buildList {
            if (categories.isEmpty()) add("provider.onboarding.categories.required")
            if (specialties.isEmpty() || !input.boolean("specialty_relation_valid", true)) {
                add("provider.onboarding.specialties.required")
            }
        }
        val categoryLabels = input.strings("category_labels")
        val specialtyLabels = input.strings("specialty_labels")
        return ProviderUiState(
            screen = "SCR-P04",
            phase = if (categoryLabels.isEmpty()) ProviderPhase.Empty else ProviderPhase.Content,
            canContinue = errors.isEmpty(),
            fieldErrors = errors,
            options = categoryLabels,
            secondaryOptions = specialtyLabels,
            selectedCount = categories.size + specialties.size,
            selectedPrimaryIndices = categoryLabels.indices.take(categories.size),
            selectedSecondaryIndices = specialtyLabels.indices.take(specialties.size),
        )
    }

    private fun areas(input: Map<String, Any?>): ProviderUiState {
        val selected = input.strings("area_ids")
        val areas = input.strings("area_labels")
        val valid = selected.isNotEmpty() && input.boolean("area_selection_valid", true)
        return ProviderUiState(
            screen = "SCR-P05",
            phase = if (areas.isEmpty()) ProviderPhase.Empty else ProviderPhase.Content,
            canContinue = valid,
            fieldErrors = if (valid) emptyList() else listOf("provider.onboarding.areas.required"),
            options = input.strings("city_labels"),
            secondaryOptions = areas,
            selectedCount = selected.size,
            selectedPrimaryIndices = if (input.strings("city_labels").isEmpty()) emptyList() else listOf(0),
            selectedSecondaryIndices = areas.indices.take(selected.size),
        )
    }

    private fun payout(input: Map<String, Any?>): ProviderUiState {
        val method = input.text("payout_code")
        val details = input.text("payout_details")
        val errors = buildList {
            if (method.isBlank()) add("provider.onboarding.payout.method.required")
            if (details.isBlank()) add("provider.onboarding.payout.details.required")
        }
        val busy = input.text("event") == "submitting"
        return ProviderUiState(
            screen = "SCR-P06",
            phase = ProviderPhase.Content,
            canContinue = errors.isEmpty() && !busy && "submit_provider_application" in input.strings("available_actions"),
            messageKey = if (input.text("event") == "not_editable") "provider.application.not_editable" else null,
            fieldErrors = errors,
            visibleActions = input.strings("available_actions"),
            items = input.strings("summary"),
            options = input.strings("payout_labels"),
            secondaryOptions = input.strings("payout_field_labels"),
            selectedPrimaryIndices = if (method.isBlank()) emptyList() else listOf(0),
            isBusy = busy,
            fieldValues = listOf(details),
        )
    }

    private fun status(input: Map<String, Any?>): ProviderUiState {
        val reason = input.text("reason")
        return ProviderUiState(
            screen = "SCR-P07",
            phase = ProviderPhase.Content,
            canContinue = true,
            messageKey = input.text("display_status").ifBlank { statusKey(input.text("status")) },
            visibleActions = input.strings("available_actions"),
            items = reason.takeIf(String::isNotBlank)?.let(::listOf).orEmpty(),
        )
    }

    private fun home(input: Map<String, Any?>): ProviderUiState {
        val busy = input.text("event") == "saving"
        return ProviderUiState(
            screen = "SCR-P08",
            phase = ProviderPhase.Content,
            canContinue = !busy && "set_availability" in input.strings("available_actions"),
            messageKey = if (input.text("event") == "availability_saved") {
                "provider.home.availability.saved"
            } else {
                null
            },
            visibleActions = input.strings("available_actions"),
            items = input.strings("order_summary"),
            options = input.strings("request_titles"),
            secondaryOptions = input.strings("request_details"),
            optionCodes = input.strings("request_ids"),
            isBusy = busy,
            availableNow = input.boolean("available_now"),
            activeOrderId = input.integer("active_order_id").takeIf { it > 0 },
            displayStatus = input.text("display_status"),
            unreadCount = input.integer("unread_notifications"),
        )
    }

    private fun availableRequest(input: Map<String, Any?>): ProviderUiState {
        val busy = input.text("event") == "withdrawing"
        return ProviderUiState(
            screen = "SCR-P09",
            phase = ProviderPhase.Content,
            canContinue = !busy && input.strings("available_actions").isNotEmpty(),
            messageKey = when (input.text("event")) {
                "unavailable" -> "provider.market.request.unavailable"
                "withdrawn" -> "provider.offers.withdraw.success"
                else -> null
            },
            visibleActions = input.strings("available_actions"),
            items = input.strings("order_summary"),
            options = input.strings("media_labels"),
            secondaryOptions = input.strings("offer_summary"),
            isBusy = busy,
            customerName = input.text("customer_name"),
            customerRating = input.text("customer_rating"),
            displayStatus = input.text("display_status"),
        )
    }

    private fun offerForm(input: Map<String, Any?>): ProviderUiState {
        val price = input.text("price").toBigDecimalOrNull()
        val minimum = input.text("min_amount").toBigDecimalOrNull() ?: BigDecimal.ZERO
        val commission = input.text("commission_rate").toBigDecimalOrNull() ?: BigDecimal.ZERO
        val selectedEta = input.integer("selected_eta_index", -1)
        val selectedDeductible = input.integer("selected_deductible_index", -1)
        val now = input.text("timing_type") == "NOW"
        val inspection = input.text("pricing_mode") == "INSPECTION"
        val includes = input.text("includes_text")
        val note = input.text("note")
        val errors = buildList {
            if (price == null || price < minimum) add("provider.offer.price.invalid")
            if (now && selectedEta !in input.strings("eta_codes").indices) add("provider.offer.eta.required")
            if (inspection && selectedDeductible !in input.strings("deductible_codes").indices) {
                add("provider.offer.deductible.required")
            }
            if (includes.isBlank() || includes.length > 150) add("provider.offer.includes.invalid")
            if (note.length > 300) add("provider.offer.note.invalid")
        }
        val busy = input.text("event") == "submitting"
        val success = input.text("event") == "success"
        val net = price?.multiply(BigDecimal.ONE.subtract(commission))?.setScale(2)?.stripTrailingZeros()?.toPlainString().orEmpty()
        return ProviderUiState(
            screen = "SCR-P10",
            phase = ProviderPhase.Content,
            canContinue = !busy && !success && errors.isEmpty() && "submit_offer" in input.strings("available_actions"),
            messageKey = when {
                success -> "provider.offer.success"
                input.text("event") == "unavailable" -> "provider.market.request.unavailable"
                else -> null
            },
            fieldErrors = errors,
            visibleActions = input.strings("available_actions"),
            items = input.strings("order_summary"),
            options = input.strings("eta_labels"),
            secondaryOptions = listOf(net),
            selectedOptionIndex = selectedEta,
            optionCodes = input.strings("eta_codes"),
            selectedSecondaryIndices = if (selectedDeductible < 0) emptyList() else listOf(selectedDeductible),
            isBusy = busy,
            fieldValues = listOf(input.text("price"), includes, note),
            itemDetails = input.strings("deductible_labels"),
            itemStates = input.strings("deductible_codes"),
            currentAction = "submit_offer",
            showPriceGuide = now,
            showOutsideReason = inspection,
        )
    }

    private fun offers(input: Map<String, Any?>): ProviderUiState {
        val titles = input.strings("offer_titles")
        val busy = input.text("event") == "withdrawing"
        return ProviderUiState(
            screen = "SCR-P11",
            phase = if (titles.isEmpty()) ProviderPhase.Empty else ProviderPhase.Content,
            canContinue = titles.isNotEmpty() && !busy,
            messageKey = when (input.text("event")) {
                "withdrawn" -> "provider.offers.withdraw.success"
                "changed" -> "provider.offers.withdraw.changed"
                else -> null
            },
            items = titles,
            itemDetails = input.strings("offer_details"),
            itemStates = input.strings("offer_statuses"),
            secondaryOptions = input.strings("offer_actions"),
            selectedOptionIndex = input.integer("selected_offer_index", -1),
            currentAction = if (input.text("event") in setOf("confirm_withdraw", "withdrawing")) "withdraw_offer" else "",
            isBusy = busy,
        )
    }

    private fun activeOrder(screen: String, input: Map<String, Any?>): ProviderUiState {
        val busy = input.text("event") == "submitting"
        val reviewRequired = input.boolean("price_review_required")
        return ProviderUiState(
            screen = screen,
            phase = ProviderPhase.Content,
            canContinue = !busy && input.strings("available_actions").isNotEmpty(),
            messageKey = input.text("message_key").takeIf(String::isNotBlank)
                ?: if (reviewRequired) "provider.price_guide.other_review" else null,
            visibleActions = input.strings("available_actions"),
            items = input.strings("order_summary"),
            secondaryOptions = input.strings("amount_summary"),
            isBusy = busy,
            displayStatus = input.text("display_status"),
            stepStates = input.strings("step_states"),
            showPriceGuide = input.text("price_guide_min").isNotBlank() && input.text("price_guide_max").isNotBlank(),
            currentAction = input.text("current_action"),
            priceGuideMinimum = input.text("price_guide_min"),
            priceGuideMaximum = input.text("price_guide_max"),
            customerName = input.text("customer_name"),
            customerRating = input.text("customer_rating"),
        )
    }

    private fun proposal(input: Map<String, Any?>): ProviderUiState {
        val selected = input.integer("selected_type_index", -1)
        val codes = input.strings("proposal_type_codes")
        val type = codes.getOrNull(selected).orEmpty()
        val amount = input.text("amount").toBigDecimalOrNull()
        val reason = input.text("reason").trim()
        val outsideReason = input.text("outside_reason").trim()
        val minimum = input.text("price_guide_min").toBigDecimalOrNull()
        val maximum = input.text("price_guide_max").toBigDecimalOrNull()
        val hasGuide = type == "EXECUTION_QUOTE" && minimum != null && maximum != null
        val outside = hasGuide && amount != null && (amount < minimum || amount > maximum)
        val reviewRequired = input.boolean("price_review_required")
        val errors = buildList {
            if (amount == null || amount <= BigDecimal.ZERO) add("provider.proposal.amount.invalid")
            if (reason.length !in 5..300) add("provider.proposal.reason.invalid")
            if (outside && outsideReason.length !in 10..300) add("provider.proposal.outside_reason.invalid")
        }
        val action = if (type == "EXECUTION_QUOTE") "submit_execution_quote" else "submit_proposal"
        val busy = input.text("event") == "submitting"
        val success = input.text("event") == "success"
        return ProviderUiState(
            screen = "SCR-P20",
            phase = ProviderPhase.Content,
            canContinue = !busy && !success && selected >= 0 && errors.isEmpty() && action in input.strings("available_actions"),
            messageKey = when {
                success -> "provider.proposal.success"
                reviewRequired -> "provider.price_guide.other_review"
                else -> null
            },
            fieldErrors = errors,
            visibleActions = input.strings("available_actions"),
            options = input.strings("proposal_type_labels"),
            secondaryOptions = listOf(input.text("amount"), input.text("reason"), input.text("outside_reason")),
            selectedOptionIndex = selected,
            optionCodes = codes,
            showPriceGuide = hasGuide,
            showOutsideReason = outside,
            hasPhoto = input.boolean("photo_uploaded"),
            isBusy = busy,
            currentAction = action,
            priceGuideMinimum = input.text("price_guide_min"),
            priceGuideMaximum = input.text("price_guide_max"),
        )
    }

    private fun unable(input: Map<String, Any?>): ProviderUiState {
        val action = input.text("requested_action")
        val selected = input.integer("selected_reason_index", -1)
        val note = input.text("note").trim()
        val noShow = action == "report_no_show"
        val errors = buildList {
            if (!noShow && selected !in input.strings("reason_codes").indices) {
                add("provider.unable.reason.required")
            }
            if (action == "report_unable" && note.length !in 5..500) {
                add("provider.unable.note.invalid")
            }
        }
        val busy = input.text("event") == "submitting"
        return ProviderUiState(
            screen = "SCR-P21",
            phase = ProviderPhase.Content,
            canContinue = !busy && errors.isEmpty() && action in input.strings("available_actions"),
            messageKey = input.text("message_key").takeIf(String::isNotBlank)
                ?: if (noShow) "provider.no_show.confirm" else null,
            fieldErrors = errors,
            visibleActions = input.strings("available_actions"),
            options = input.strings("reason_labels"),
            optionCodes = input.strings("reason_codes"),
            selectedOptionIndex = selected,
            isBusy = busy,
            currentAction = action,
            fieldValues = listOf(note),
        )
    }

    private fun rating(input: Map<String, Any?>): ProviderUiState {
        val stars = input.integer("stars")
        val busy = input.text("event") == "submitting"
        val success = input.text("event") == "success"
        val valid = stars in 1..5
        return ProviderUiState(
            screen = "SCR-P17",
            phase = ProviderPhase.Content,
            canContinue = valid && !busy && !success && "rate_customer" in input.strings("available_actions"),
            messageKey = when {
                success -> "provider.rating.success"
                input.text("message_key").isNotBlank() -> input.text("message_key")
                else -> null
            },
            fieldErrors = if (!valid && input.text("event") == "validate") listOf("provider.rating.required") else emptyList(),
            visibleActions = input.strings("available_actions"),
            items = input.strings("order_summary"),
            selectedOptionIndex = stars - 1,
            isBusy = busy,
        )
    }

    private fun earnings(input: Map<String, Any?>): ProviderUiState {
        val titles = input.strings("transaction_titles")
        val busy = input.text("event") == "loading_more"
        val empty = titles.isEmpty()
        return ProviderUiState(
            screen = "SCR-P18",
            phase = if (empty) ProviderPhase.Empty else ProviderPhase.Content,
            canContinue = !empty && !busy,
            messageKey = if (empty) {
                "provider.earnings.empty"
            } else if (input.text("operating_mode") == "EMPLOYEE") {
                "provider.earnings.employee.info"
            } else {
                null
            },
            items = titles,
            itemDetails = input.strings("transaction_details"),
            itemStates = input.strings("transaction_states"),
            secondaryOptions = input.strings("summary"),
            isBusy = busy,
        )
    }

    private fun providerProfile(input: Map<String, Any?>): ProviderUiState {
        val experience = input.text("experience_years").toIntOrNull()
        val specialties = input.strings("specialty_ids")
        val areas = input.strings("area_ids")
        val busy = input.text("event") in listOf("uploading", "saving")
        val errors = buildList {
            if (experience == null || experience !in 0..50) add("provider.onboarding.experience.invalid")
            if (input.text("bio").length > 300) add("provider.onboarding.bio.max")
            if (specialties.isEmpty()) add("provider.onboarding.specialties.required")
            if (areas.isEmpty()) add("provider.onboarding.areas.required")
        }
        val portfolio = input.strings("portfolio_labels")
        return ProviderUiState(
            screen = "SCR-P19",
            phase = ProviderPhase.Content,
            canContinue = !busy && errors.isEmpty() && "update_provider_profile" in input.strings("available_actions"),
            messageKey = when {
                input.text("event") == "saved" -> "provider.profile.saved"
                portfolio.size >= 20 -> "provider.profile.portfolio.full"
                else -> null
            },
            fieldErrors = errors,
            visibleActions = input.strings("available_actions"),
            items = portfolio,
            itemDetails = input.strings("portfolio_ids"),
            itemStates = input.strings("profile_stats"),
            options = input.strings("category_labels"),
            secondaryOptions = input.strings("specialty_labels"),
            areaOptions = input.strings("area_labels"),
            selectedCount = specialties.size + areas.size,
            selectedPrimaryIndices = input.strings("specialty_labels").indices.take(specialties.size),
            selectedSecondaryIndices = input.strings("area_labels").indices.take(areas.size),
            isBusy = busy,
            hasProfilePhoto = input.boolean("profile_photo_uploaded"),
            fieldValues = listOf(input.text("experience_years"), input.text("bio"), input.text("portfolio_caption")),
            customerName = input.text("name"),
            customerRating = input.text("rating"),
        )
    }

    private fun chat(input: Map<String, Any?>): ProviderUiState {
        val messages = input.strings("message_bodies")
        val status = input.text("conversation_status")
        val busy = input.text("event") == "sending"
        val draft = input.text("draft")
        return ProviderUiState(
            screen = "SCR-C19",
            phase = if (messages.isEmpty()) ProviderPhase.Empty else ProviderPhase.Content,
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
            isBusy = busy,
        )
    }

    private fun statusKey(status: String): String? = when (status) {
        "PENDING_REVIEW" -> "provider.application.pending"
        "REJECTED" -> "provider.application.rejected"
        "ACTIVE" -> "provider.application.active"
        "SUSPENDED" -> "provider.application.suspended"
        else -> null
    }
}

private fun Map<String, Any?>.text(key: String): String = this[key] as? String ?: ""
private fun Map<String, Any?>.strings(key: String): List<String> = (this[key] as? List<*>)?.filterIsInstance<String>().orEmpty()
private fun Map<String, Any?>.boolean(key: String, default: Boolean = false): Boolean = this[key] as? Boolean ?: default
private fun Map<String, Any?>.integer(key: String, default: Int = 0): Int = (this[key] as? Number)?.toInt() ?: default

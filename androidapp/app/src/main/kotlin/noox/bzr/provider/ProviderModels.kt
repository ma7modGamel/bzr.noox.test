package noox.bzr.provider

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import noox.bzr.customer.IdName

// DEC-061, 9أ stage 4 — the provider surface (P01–P21, C19/C32 in provider mode), read from the recorded
// server answers in src/test/resources/api/provider (tests/Feature/Api/MobileProviderResponsesTest.php).
// Each model maps onto the payload the screens already use, so the view model does not change. Money and
// coordinates are decimal on the server and arrive as strings; order numbers arrive as integers.

@Serializable
data class ApplicationDocuments(@SerialName("id_front") val idFront: Boolean = false, @SerialName("id_back") val idBack: Boolean = false)

@Serializable
data class ProviderApplication(
    val status: String,
    @SerialName("display_status") val displayStatus: String? = null,
    @SerialName("rejection_reason") val rejectionReason: String? = null,
    @SerialName("suspension_reason") val suspensionReason: String? = null,
    @SerialName("experience_years") val experienceYears: Int? = null,
    val bio: String? = null,
    @SerialName("category_ids") val categoryIds: List<Int> = emptyList(),
    @SerialName("specialty_ids") val specialtyIds: List<Int> = emptyList(),
    @SerialName("area_ids") val areaIds: List<Int> = emptyList(),
    @SerialName("payout_method") val payoutMethod: String? = null,
    @SerialName("payout_details") val payoutDetails: String? = null,
    @SerialName("profile_photo_uploaded") val profilePhotoUploaded: Boolean = false,
    val documents: ApplicationDocuments = ApplicationDocuments(),
    @SerialName("available_actions") val availableActions: List<String> = emptyList(),
) {
    fun toPayload() = ProviderApplicationPayload(
        status = status,
        displayStatus = displayStatus.orEmpty(),
        reason = rejectionReason?.ifBlank { null } ?: suspensionReason?.ifBlank { null },
        experienceYears = experienceYears,
        bio = bio.orEmpty(),
        categoryIds = categoryIds,
        specialtyIds = specialtyIds,
        areaIds = areaIds,
        payoutMethod = payoutMethod.orEmpty(),
        payoutDetails = payoutDetails.orEmpty(),
        profilePhotoUploaded = profilePhotoUploaded,
        idFrontUploaded = documents.idFront,
        idBackUploaded = documents.idBack,
        availableActions = availableActions,
    )
}

@Serializable
data class ProviderPriceGuide(val minimum: String? = null, val maximum: String? = null)

@Serializable
data class ProviderTiming(val type: String? = null, @SerialName("slot_start") val slotStart: String? = null)

@Serializable
data class ProviderLocation(
    val area: String? = null,
    @SerialName("address_text") val addressText: String? = null,
    val building: String? = null,
    val floor: String? = null,
    val apartment: String? = null,
    val landmark: String? = null,
    val lat: String? = null,
    val lng: String? = null,
)

@Serializable
data class ProviderDeadlines(@SerialName("offers_close_at") val offersCloseAt: String? = null)

@Serializable
data class ProviderStep(val state: String)

@Serializable
data class ProviderAmounts(
    @SerialName("labor_total") val laborTotal: String? = null,
    @SerialName("materials_total") val materialsTotal: String? = null,
    @SerialName("final_amount") val finalAmount: String? = null,
    @SerialName("payment_method") val paymentMethod: String? = null,
    @SerialName("payment_method_label") val paymentMethodLabel: String? = null,
)

/** Customer details; phone remains nullable for compatibility and is hidden from providers. */
@Serializable
data class ProviderCustomer(val name: String? = null, val phone: String? = null, @SerialName("rating_avg") val ratingAvg: String? = null)

/** OrderResource as the provider reads it (P08–P21). */
@Serializable
data class WorkPhoto(val id: Int, val purpose: String)

@Serializable
data class ProviderOrder(
    val id: Int,
    val number: Long,
    val version: Int,
    val status: String,
    @SerialName("display_status") val displayStatus: String = "",
    val category: IdName? = null,
    @SerialName("problem_type") val problemType: IdName? = null,
    val description: String? = null,
    @SerialName("pricing_mode") val pricingMode: String? = null,
    @SerialName("pricing_mode_label") val pricingModeLabel: String? = null,
    @SerialName("materials_responsibility") val materialsResponsibility: String? = null,
    @SerialName("materials_responsibility_label") val materialsResponsibilityLabel: String? = null,
    @SerialName("budget_amount") val budgetAmount: String? = null,
    @SerialName("execution_price_guide") val executionPriceGuide: ProviderPriceGuide? = null,
    @SerialName("price_guide_review_required") val priceGuideReviewRequired: Boolean = false,
    val timing: ProviderTiming = ProviderTiming(),
    val location: ProviderLocation = ProviderLocation(),
    val deadlines: ProviderDeadlines = ProviderDeadlines(),
    val stepper: List<ProviderStep>? = null,
    @SerialName("conversation_id") val conversationId: Int? = null,
    val amounts: ProviderAmounts = ProviderAmounts(),
    val customer: ProviderCustomer = ProviderCustomer(),
    @SerialName("absence_report_pending") val absenceReportPending: Boolean = false,
    @SerialName("arrival_code_required") val arrivalCodeRequired: Boolean = false,
    @SerialName("arrival_exception_pending") val arrivalExceptionPending: Boolean = false,
    @SerialName("completion_estimate") val completionEstimate: noox.bzr.customer.CustomerCompletionEstimate? = null,
    @SerialName("work_photos") val workPhotos: List<WorkPhoto> = emptyList(),
    @SerialName("warranty_coverage_text") val warrantyCoverage: String? = null,
    @SerialName("proposal_type_options") val proposalTypeOptions: List<ProviderReason> = emptyList(),
    @SerialName("customer_rating_tag_options") val customerRatingTagOptions: List<ProviderReason> = emptyList(),
    @SerialName("available_actions") val availableActions: List<String> = emptyList(),
) {
    fun toPayload() = ProviderOrderPayload(
        id = id,
        number = number.toString(),
        version = version,
        status = status,
        displayStatus = displayStatus,
        category = category?.name.orEmpty(),
        problemType = problemType?.name.orEmpty(),
        address = listOf(location.addressText, location.building, location.floor, location.apartment, location.landmark)
            .mapNotNull { it?.takeIf(String::isNotBlank) }.joinToString(", "),
        area = location.area.orEmpty(),
        latitude = location.lat?.toDoubleOrNull(),
        longitude = location.lng?.toDoubleOrNull(),
        timingType = timing.type.orEmpty(),
        slotStart = timing.slotStart?.ifBlank { null },
        customerName = customer.name.orEmpty(),
        customerPhone = customer.phone?.ifBlank { null },
        customerRating = customer.ratingAvg.orEmpty(),
        stepStates = stepper.orEmpty().map(ProviderStep::state),
        availableActions = availableActions,
        absenceReportPending = absenceReportPending,
        warrantyCoverage = warrantyCoverage.orEmpty(),
        completionEstimateAt = completionEstimate?.estimatedAt.orEmpty(),
        arrivalCodeRequired = arrivalCodeRequired, arrivalExceptionPending = arrivalExceptionPending,
        proposalTypeOptions = proposalTypeOptions,
        customerRatingTagOptions = customerRatingTagOptions,
        beforePhotoCount = workPhotos.count { it.purpose == "REPAIR_BEFORE" },
        afterPhotoCount = workPhotos.count { it.purpose == "REPAIR_AFTER" },
        laborTotal = amounts.laborTotal ?: ZERO,
        materialsTotal = amounts.materialsTotal ?: ZERO,
        finalAmount = amounts.finalAmount ?: ZERO,
        paymentMethod = amounts.paymentMethod?.ifBlank { null },
        paymentMethodLabel = amounts.paymentMethodLabel.orEmpty(),
        priceGuideMinimum = executionPriceGuide?.minimum?.ifBlank { null },
        priceGuideMaximum = executionPriceGuide?.maximum?.ifBlank { null },
        priceGuideReviewRequired = priceGuideReviewRequired,
        conversationId = conversationId,
        description = description.orEmpty(),
        pricingMode = pricingMode.orEmpty(),
        pricingModeLabel = pricingModeLabel.orEmpty(),
        materialsResponsibility = materialsResponsibility.orEmpty(),
        materialsResponsibilityLabel = materialsResponsibilityLabel.orEmpty(),
        budgetAmount = budgetAmount?.ifBlank { null },
        offersCloseAt = deadlines.offersCloseAt?.ifBlank { null },
    )

    fun toHomeOrder() = ProviderHomeOrder(
        id = id,
        number = number.toString(),
        category = category?.name.orEmpty(),
        problemType = problemType?.name.orEmpty(),
        area = location.area.orEmpty(),
        timingType = timing.type.orEmpty(),
        slotStart = timing.slotStart?.ifBlank { null },
        displayStatus = displayStatus,
    )
}

/** An amount the server has not set yet (before pricing). */
private const val ZERO = "0.00"

@Serializable
data class ProviderHome(
    @SerialName("operating_mode") val operatingMode: String,
    @SerialName("available_now") val availableNow: Boolean,
    @SerialName("active_order") val activeOrder: ProviderOrder? = null,
    @SerialName("available_actions") val availableActions: List<String> = emptyList(),
    @SerialName("unread_notifications") val unreadNotifications: Int = 0,
) {
    fun toPayload() = ProviderHomePayload(operatingMode, availableNow, activeOrder?.toHomeOrder(), availableActions, unreadNotifications)
}

@Serializable
data class OfferOrderSummary(
    val id: Int,
    val number: Long,
    val version: Int,
    val category: String? = null,
    val area: String? = null,
)

/** One of the provider's offers (P11), with the net after commission. */
@Serializable
data class ProviderOffer(
    val id: Int,
    val status: String,
    @SerialName("display_status") val displayStatus: String,
    val price: String,
    @SerialName("net_amount") val netAmount: String,
    @SerialName("available_actions") val availableActions: List<String> = emptyList(),
    val order: OfferOrderSummary,
) {
    fun toPayload() = ProviderOfferPayload(
        id = id,
        orderId = order.id,
        orderVersion = order.version,
        title = "${order.number}|${order.category.orEmpty()}",
        detail = "${order.area.orEmpty()}|$price|$netAmount",
        status = status,
        displayStatus = displayStatus,
        availableActions = availableActions,
    )
}

@Serializable
data class OwnOffer(val id: Int, val price: String, @SerialName("net_amount") val netAmount: String, @SerialName("display_status") val displayStatus: String)

@Serializable
data class DeductibleOption(val code: String, val label: String)

@Serializable
data class OfferContext(
    @SerialName("min_amount") val minAmount: String,
    @SerialName("commission_rate") val commissionRate: String,
    @SerialName("eta_options") val etaOptions: List<Int> = emptyList(),
    @SerialName("deductible_options") val deductibleOptions: List<DeductibleOption> = emptyList(),
    @SerialName("default_includes_text") val defaultIncludesText: String = "",
)

@Serializable
data class RequestMedia(val type: String)

/** An available marketplace request (P09–P10): the order without the customer's private data, and offer terms. */
@Serializable
data class MarketRequest(
    val order: ProviderOrder,
    val media: List<RequestMedia> = emptyList(),
    @SerialName("own_offer") val ownOffer: OwnOffer? = null,
    @SerialName("offer_context") val offerContext: OfferContext,
) {
    fun toPayload(): ProviderMarketRequestPayload {
        val eta = offerContext.etaOptions.map(Int::toString)
        return ProviderMarketRequestPayload(
            order = order.toPayload(),
            mediaLabels = media.mapIndexed { index, item -> "${item.type}:${index + 1}" },
            ownOfferId = ownOffer?.id?.takeIf { it > 0 },
            ownOfferSummary = ownOffer?.let { listOf(it.price, it.netAmount, it.displayStatus) }.orEmpty(),
            context = ProviderOfferContext(
                minimumAmount = offerContext.minAmount,
                commissionRate = offerContext.commissionRate,
                etaCodes = eta,
                etaLabels = eta,
                deductibleCodes = offerContext.deductibleOptions.map(DeductibleOption::code),
                deductibleLabels = offerContext.deductibleOptions.map(DeductibleOption::label),
                defaultIncludes = offerContext.defaultIncludesText,
            ),
        )
    }
}

@Serializable
data class ProfileCategory(val id: Int, val name: String, val specialties: List<IdName> = emptyList())

@Serializable
data class PortfolioEntry(val id: Int, val caption: String? = null) {
    fun toPayload() = ProviderPortfolioItem(id, caption.orEmpty())
}

@Serializable
data class ProviderWorkspaceProfile(
    val name: String,
    @SerialName("avatar_path") val avatarPath: String? = null,
    val bio: String? = null,
    @SerialName("experience_years") val experienceYears: Int = 0,
    @SerialName("rating_avg") val ratingAvg: String? = null,
    @SerialName("completed_orders") val completedOrders: Int = 0,
    @SerialName("avg_response_minutes") val avgResponseMinutes: Int? = null,
    val categories: List<ProfileCategory> = emptyList(),
    @SerialName("specialty_ids") val specialtyIds: List<Int> = emptyList(),
    val areas: List<IdName> = emptyList(),
    @SerialName("area_ids") val areaIds: List<Int> = emptyList(),
    val portfolio: List<PortfolioEntry> = emptyList(),
    @SerialName("available_actions") val availableActions: List<String> = emptyList(),
) {
    fun toPayload() = ProviderProfilePayload(
        name = name,
        rating = ratingAvg.orEmpty(),
        completedOrders = completedOrders,
        averageResponseMinutes = avgResponseMinutes,
        hasAvatar = !avatarPath.isNullOrBlank(),
        experienceYears = experienceYears,
        bio = bio.orEmpty(),
        categories = categories.map { category ->
            ProviderCategory(category.id, category.name, category.specialties.mapNotNull(IdName::choice))
        },
        specialtyIds = specialtyIds,
        areas = areas.mapNotNull(IdName::choice),
        areaIds = areaIds,
        portfolio = portfolio.map(PortfolioEntry::toPayload),
        availableActions = availableActions,
    )
}

internal fun IdName.choice(): ProviderChoice? = id?.let { ProviderChoice(it, name.orEmpty()) }

// Request bodies. Optional fields that the JSONObject code left out when empty default to null: the shared
// Json does not encode defaults, so they stay out of the body. Fields it sent as JSON null have no default.

@Serializable
data class ApplicationBody(
    @SerialName("experience_years") val experienceYears: Int,
    val bio: String?,
    @SerialName("category_ids") val categoryIds: List<Int>,
    @SerialName("specialty_ids") val specialtyIds: List<Int>,
    @SerialName("area_ids") val areaIds: List<Int>,
    @SerialName("payout_method") val payoutMethod: String,
    @SerialName("payout_details") val payoutDetails: String,
    @SerialName("profile_photo_media_id") val profilePhotoMediaId: Int? = null,
    @SerialName("id_front_media_id") val idFrontMediaId: Int? = null,
    @SerialName("id_back_media_id") val idBackMediaId: Int? = null,
)

@Serializable
data class OfferBody(
    val price: String,
    @SerialName("includes_text") val includesText: String,
    val note: String?,
    @SerialName("eta_minutes") val etaMinutes: Int? = null,
    @SerialName("inspection_fee_deductible") val inspectionFeeDeductible: Boolean? = null,
)

@Serializable
data class WorkPhotoBody(@SerialName("media_id") val mediaId: Int, val purpose: String, @SerialName("expected_version") val expectedVersion: Int)

@Serializable
data class AvailabilityBody(@SerialName("available_now") val availableNow: Boolean)

/** What an order action carries besides `expected_version` (BR-111 location, cash amount, reasons). */
data class OrderActionInput(
    val lat: Double? = null,
    val lng: Double? = null,
    val amount: String? = null,
    val reasonCode: String? = null,
    val note: String? = null,
    val minutes: Int? = null,
    @SerialName("arrival_code") val arrivalCode: String? = null,
)

@Serializable
data class OrderActionBody(
    @SerialName("expected_version") val expectedVersion: Int,
    val lat: Double? = null,
    val lng: Double? = null,
    val amount: String? = null,
    @SerialName("reason_code") val reasonCode: String? = null,
    val note: String? = null,
    val minutes: Int? = null,
    @SerialName("arrival_code") val arrivalCode: String? = null,
)

@Serializable
data class ProposalBody(
    val type: String,
    val amount: String,
    val reason: String,
    @SerialName("expected_version") val expectedVersion: Int,
    @SerialName("photo_media_id") val photoMediaId: Int? = null,
    @SerialName("outside_price_guide_reason") val outsidePriceGuideReason: String? = null,
)

@Serializable
data class CustomerRatingBody(val stars: Int, val tags: List<String> = emptyList(), val comment: String? = null)

@Serializable
data class ProfileUpdateBody(
    @SerialName("experience_years") val experienceYears: Int,
    val bio: String?,
    @SerialName("specialty_ids") val specialtyIds: List<Int>,
    @SerialName("area_ids") val areaIds: List<Int>,
)

@Serializable
data class PortfolioBody(@SerialName("media_id") val mediaId: Int, val caption: String?)

@Serializable
data class AvatarBody(@SerialName("avatar_media_id") val avatarMediaId: Int)

package noox.bzr.customer

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// DEC-061, 9أ stage 3 — electronic payment (C22, `PaymentAttempt` from stage 2), rating (C24), disputes and provider reports (C27/C28),
// help (C29), notifications (C32), account (C02/C33) and terms (C34), read from the recorded server answers in
// src/test/resources/api/customer (MobileCustomerAccountResponsesTest). Request bodies carry no defaults:
// the shared Json does not encode defaults, so every field is sent (nulls as JSON null).

@Serializable
data class DisputeAttachment(@SerialName("media_id") val mediaId: Int? = null)

@Serializable
data class CustomerDispute(
    val id: Int,
    @SerialName("reason_code") val reasonCode: String,
    @SerialName("reason_label") val reasonLabel: String,
    val description: String,
    val status: String,
    @SerialName("status_label") val statusLabel: String? = null,
    @SerialName("resolution_note") val resolutionNote: String? = null,
    val attachments: List<DisputeAttachment> = emptyList(),
    @SerialName("visit_order_id") val visitOrderId: Int? = null,
)

@Serializable
data class FaqItem(val code: String, val title: String, val body: String)

@Serializable
data class NotificationItem(
    val id: String,
    /** NTF-xx (NotificationCode); picks the Figma icon on C32 (DEC-076). */
    val code: String = "",
    val title: String,
    val body: String,
    @SerialName("deep_link") val deepLink: String? = null,
    @SerialName("read_at") val readAt: String? = null,
    @SerialName("created_at") val createdAt: String? = null,
)

@Serializable
data class TermsPage(val version: Int, val body: String, @SerialName("effective_at") val effectiveAt: String? = null,
    @SerialName("cancellation_policy") val cancellationPolicy: RelatedLegalPage? = null,
    @SerialName("privacy_policy") val privacyPolicy: RelatedLegalPage? = null,
) {
    val displayBody: String get() = body + listOfNotNull(cancellationPolicy, privacyPolicy).joinToString("") { page ->
        "<h2>${page.title.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")}</h2>${page.bodyHtml}"
    }
}

@Serializable
data class RelatedLegalPage(val title: String, @SerialName("body_html") val bodyHtml: String)

@Serializable
data class CreatePaymentBody(val channel: String, @SerialName("expected_version") val expectedVersion: Int)

@Serializable
data class WarrantyBody(val description: String)

@Serializable
data class ReviewBody(val quality: Int, val punctuality: Int, val conduct: Int, val comment: String?, @SerialName("media_ids") val mediaIds: List<Int> = emptyList(), val tags: List<String> = emptyList())

@Serializable
data class InstapayTransferBody(@SerialName("transfer_reference") val transferReference: String, @SerialName("receipt_media_id") val receiptMediaId: Int, @SerialName("expected_version") val expectedVersion: Int)

@Serializable
data class OpenDisputeBody(
    @SerialName("reason_code") val reasonCode: String,
    val description: String,
    @SerialName("media_ids") val mediaIds: List<Int>,
)

@Serializable
data class ProviderReportBody(
    @SerialName("order_id") val orderId: Int,
    @SerialName("reason_code") val reasonCode: String,
    val description: String?,
)

@Serializable
data class SupportMessageBody(val subject: String, val message: String)

@Serializable
data class NotificationsReadBody(@SerialName("notification_ids") val notificationIds: List<String>)

@Serializable
data class RegisterDeviceBody(val token: String, val platform: String)

@Serializable
data class DeviceTokenBody(val token: String)

@Serializable
data class RatingRemindersBody(@SerialName("rating_reminders_enabled") val ratingRemindersEnabled: Boolean)

@Serializable
data class AccountUpdateBody(val name: String, val phone: String)

@Serializable
data class LocaleUpdateBody(val locale: String)

@Serializable
data class PasswordChangeBody(
    @SerialName("current_password") val currentPassword: String,
    val password: String,
    @SerialName("password_confirmation") val passwordConfirmation: String,
)

@Serializable
class EmptyBody

/**
 * `POST orders` / `PATCH orders/{id}` (C03–C05). Optional fields default to null and the shared Json does not
 * encode defaults, so a missing value is left out of the body — as the JSONObject code did (e.g. no
 * `pricing_mode` in employee mode, BR-008). `media_ids` and the required fields are always sent.
 */
@Serializable
data class PublishOrderBody(
    @SerialName("customer_address_id") val customerAddressId: Int,
    @SerialName("category_id") val categoryId: Int,
    @SerialName("problem_type_id") val problemTypeId: Int,
    @SerialName("timing_type") val timingType: String,
    @SerialName("terms_accepted") val termsAccepted: Boolean,
    @SerialName("media_ids") val mediaIds: List<Int>,
    @SerialName("materials_responsibility") val materialsResponsibility: String? = null,
    val description: String? = null,
    @SerialName("pricing_mode") val pricingMode: String? = null,
    @SerialName("budget_amount") val budgetAmount: String? = null,
    @SerialName("slot_start") val slotStart: String? = null,
)

package noox.bzr.customer

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import noox.bzr.auth.AccountResponse
import okhttp3.MultipartBody
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.HTTP
import retrofit2.http.Header
import retrofit2.http.Multipart
import retrofit2.http.Part
import retrofit2.http.POST
import retrofit2.http.PATCH
import retrofit2.http.Path
import retrofit2.http.Query

// DEC-061, 9أ stage 2 — customer home and orders, read from the recorded server answers in
// src/test/resources/api/customer (tests/Feature/Api/MobileCustomerOrderResponsesTest.php).
// Money and coordinates are decimal on the server, so they arrive as strings ("450.00", "30.0600000").

@Serializable
data class IdName(val id: Int? = null, val name: String? = null)

@Serializable
data class CodeLabel(val code: String, val label: String)

@Serializable
data class ServiceHours(val from: String, val to: String)

/** `GET config` (DEC-041): what the customer screens need; the rest of the payload is ignored. */
@Serializable
data class InstapayDetails(val address: String, @SerialName("display_name") val displayName: String? = null, val link: String? = null)

@Serializable
data class AppConfig(
    @SerialName("operating_mode") val operatingMode: String,
    val instapay: InstapayDetails? = null,
    @SerialName("offers_enabled") val offersEnabled: Boolean = false,
    @SerialName("service_hours") val serviceHours: ServiceHours,
    @SerialName("option_lists") val optionLists: Map<String, List<CodeLabel>> = emptyMap(),
    @SerialName("option_defaults") val optionDefaults: Map<String, String> = emptyMap(),
)

@Serializable
data class CatalogProblemType(val id: Int, val name: String, @SerialName("is_other") val isOther: Boolean = false, @SerialName("customer_price_guide") val customerPriceGuide: CustomerPriceGuide? = null)

@Serializable
data class CustomerPriceGuide(val minimum: String, val maximum: String, @SerialName("suggested_amounts") val suggestedAmounts: List<String> = emptyList())

@Serializable
data class CatalogCategory(
    val id: Int,
    val name: String,
    @SerialName("icon_path") val iconPath: String? = null,
    @SerialName("problem_types") val problemTypes: List<CatalogProblemType> = emptyList(),
)

@Serializable
data class DataList<T>(val data: List<T>)

@Serializable
data class DataItem<T>(val data: T)

@Serializable
data class CustomerAddress(
    val id: Int,
    val label: String,
    val city: IdName,
    val area: IdName,
    @SerialName("address_text") val addressText: String,
    val building: String? = null,
    val floor: String? = null,
    val apartment: String? = null,
    val landmark: String? = null,
    val lat: String? = null,
    val lng: String? = null,
    @SerialName("is_default") val isDefault: Boolean = false,
    val actions: List<CustomerOption> = emptyList(),
    @SerialName("display_address") val displayAddress: String? = null,
)

@Serializable
data class AddressBookUpdate(val data: CustomerAddress, val addresses: List<CustomerAddress>)

@Serializable
data class AddressDeletion(
    @SerialName("deleted_id") val deletedId: Int,
    @SerialName("default_address") val defaultAddress: CustomerAddress? = null,
)

@Serializable
data class OrderTiming(
    val type: String,
    @SerialName("slot_start") val slotStart: String? = null,
    @SerialName("slot_end") val slotEnd: String? = null,
)

/** Only the area before confirmation (BR-024); the server drops empty keys. */
@Serializable
data class OrderLocation(
    val area: String? = null,
    val city: String? = null,
    @SerialName("address_text") val addressText: String? = null,
    val building: String? = null,
    val floor: String? = null,
    val apartment: String? = null,
    val landmark: String? = null,
    val lat: String? = null,
    val lng: String? = null,
)

@Serializable
data class OrderDeadlines(
    @SerialName("offers_close_at") val offersCloseAt: String? = null,
    @SerialName("selection_deadline_at") val selectionDeadlineAt: String? = null,
    @SerialName("proposal_expires_at") val proposalExpiresAt: String? = null,
    @SerialName("no_show_allowed_at") val noShowAllowedAt: String? = null,
    @SerialName("auto_close_at") val autoCloseAt: String? = null,
)

@Serializable
data class OrderWaiting(
    val message: String,
    @SerialName("deadline_label") val deadlineLabel: String? = null,
    @SerialName("deadline_at") val deadlineAt: String? = null,
)

@Serializable
data class VisitShareLink(val id: Int, val url: String, @SerialName("expires_at") val expiresAt: String)

@Serializable
data class StepperStep(val key: String, val state: String)

@Serializable
data class OrderAmounts(
    @SerialName("labor_total") val laborTotal: String? = null,
    @SerialName("materials_total") val materialsTotal: String? = null,
    @SerialName("final_amount") val finalAmount: String? = null,
    @SerialName("payment_method") val paymentMethod: String? = null,
    @SerialName("payment_method_label") val paymentMethodLabel: String? = null,
    @SerialName("payment_status") val paymentStatus: String,
)

@Serializable
data class OrderTermination(
    @SerialName("reason_code") val reasonCode: String? = null,
    @SerialName("reason_label") val reasonLabel: String? = null,
    val note: String? = null,
    @SerialName("cancelled_at") val cancelledAt: String? = null,
    @SerialName("expired_at") val expiredAt: String? = null,
    @SerialName("closed_at") val closedAt: String? = null,
)

/** `latest_payment` (Payment::mobilePayload). */
@Serializable
data class PaymentAttempt(
    val id: Int,
    val status: String,
    val amount: String? = null,
    val gateway: String? = null,
    val channel: String? = null,
    @SerialName("transfer_reference") val transferReference: String? = null,
    @SerialName("submitted_at") val submittedAt: String? = null,
    @SerialName("failure_reason") val failureReason: String? = null,
    @SerialName("failure_reason_label") val failureReasonLabel: String? = null,
    @SerialName("reference_number") val referenceNumber: String? = null,
    @SerialName("checkout_url") val checkoutUrl: String? = null,
    @SerialName("expires_at") val expiresAt: String? = null,
)

@Serializable
data class OrderProvider(
    val id: Int,
    val name: String,
    /** Visible to both sides from CONFIRMED (BR-101). */
    val phone: String? = null,
    @SerialName("rating_avg") val ratingAvg: String? = null,
    @SerialName("completed_orders") val completedOrders: Int = 0,
    @SerialName("is_verified") val isVerified: Boolean = false,
    @SerialName("available_now") val availableNow: Boolean = false,
)

@Serializable
data class CustomerExecutionStep(val key: String, val label: String, val state: String, @SerialName("occurred_at") val occurredAt: String? = null)

@Serializable
data class CustomerCompletionEstimate(@SerialName("estimated_at") val estimatedAt: String, @SerialName("updated_at") val updatedAt: String? = null, val overdue: Boolean = false)

/** OrderResource as the customer app sees it. */
@Serializable
data class CustomerPhoto(val id: Int, val purpose: String? = null, @SerialName("content_url") val contentUrl: String? = null)

@Serializable
data class ArrivalCode(val code: String, @SerialName("expires_at") val expiresAt: String, @SerialName("server_time") val serverTime: String)

@Serializable
data class ArrivalCodeRequest(val refresh: Boolean = false)

@Serializable
data class CustomerOrder(
    val id: Int,
    val number: Long,
    @SerialName("operating_mode") val operatingMode: String,
    val status: String,
    @SerialName("status_label") val statusLabel: String = "",
    @SerialName("work_photos") val workPhotos: List<CustomerPhoto> = emptyList(),
    @SerialName("arrival_code_required") val arrivalCodeRequired: Boolean = false,
    @SerialName("arrival_exception_pending") val arrivalExceptionPending: Boolean = false,
    val version: Int,
    val category: IdName,
    @SerialName("problem_type") val problemType: IdName,
    @SerialName("customer_address_id") val customerAddressId: Int? = null,
    val description: String? = null,
    @SerialName("pricing_mode") val pricingMode: String,
    @SerialName("materials_responsibility") val materialsResponsibility: String? = null,
    @SerialName("budget_amount") val budgetAmount: String? = null,
    val timing: OrderTiming,
    val location: OrderLocation = OrderLocation(),
    val deadlines: OrderDeadlines = OrderDeadlines(),
    val waiting: OrderWaiting? = null,
    @SerialName("execution_steps") val executionSteps: List<CustomerExecutionStep> = emptyList(),
    @SerialName("completion_estimate") val completionEstimate: CustomerCompletionEstimate? = null,
    @SerialName("review_tag_options") val reviewTagOptions: List<CodeLabel>? = null,
    @SerialName("display_status") val displayStatus: String = "",
    val stepper: List<StepperStep>? = null,
    @SerialName("conversation_id") val conversationId: Int? = null,
    @SerialName("payment_reference") val paymentReference: String? = null,
    val amounts: OrderAmounts,
    val termination: OrderTermination = OrderTermination(),
    @SerialName("latest_payment") val latestPayment: PaymentAttempt? = null,
    @SerialName("warranty_coverage_text") val warrantyCoverage: String? = null,
    val provider: OrderProvider? = null,
    @SerialName("available_actions") val availableActions: List<String> = emptyList(),
    @SerialName("created_at") val createdAt: String? = null,
)

@Serializable
data class PageMeta(
    @SerialName("current_page") val currentPage: Int,
    @SerialName("last_page") val lastPage: Int,
    @SerialName("per_page") val perPage: Int,
    val total: Int,
)

/** `GET orders` (C25): one page of orders with Laravel's pagination meta. */
@Serializable
data class OrdersPage(val data: List<CustomerOrder>, val meta: PageMeta)

@Serializable
data class TrackingLocation(val lat: String, val lng: String, @SerialName("recorded_at") val recordedAt: String? = null)

@Serializable
data class TrackingResponse(
    @SerialName("last_location") val lastLocation: TrackingLocation? = null,
    @SerialName("eta_minutes") val etaMinutes: Int? = null,
    @SerialName("eta_approximate") val etaApproximate: Boolean = false,
)

@Serializable
data class ExpectedVersionRequest(@SerialName("expected_version") val expectedVersion: Int)

@Serializable
data class AcceptOfferRequest(
    @SerialName("expected_version") val expectedVersion: Int,
    @SerialName("payment_method") val paymentMethod: String,
)

@Serializable
data class CancelOrderRequest(
    @SerialName("reason_code") val reasonCode: String,
    val note: String?,
    @SerialName("expected_version") val expectedVersion: Int,
)

@Serializable
data class DecideProposalRequest(val approve: Boolean, @SerialName("expected_version") val expectedVersion: Int)

@Serializable
data class PaymentMethodRequest(
    @SerialName("payment_method") val paymentMethod: String,
    @SerialName("expected_version") val expectedVersion: Int,
)

/**
 * One Retrofit function per endpoint (DEC-061). Calls are blocking because the customer view model still runs
 * its requests on worker threads; they move to suspend functions with the view model split.
 */
@Serializable
data class RequestAvailabilityPayload(@SerialName("now_available") val nowAvailable: Boolean)

interface CustomerService {
    @POST("orders/{order}/arrival-code")
    fun arrivalCode(@Header("Authorization") bearer: String, @Path("order") orderId: Int, @Body body: ArrivalCodeRequest): Call<DataItem<ArrivalCode>>

    @GET
    @retrofit2.http.Streaming
    fun profileImage(@Header("Authorization") bearer: String, @retrofit2.http.Url url: String): Call<okhttp3.ResponseBody>
    @POST("orders/{order}/share-links")
    fun shareVisit(@Header("Authorization") bearer: String, @Path("order") orderId: Int): Call<DataItem<VisitShareLink>>

    @GET("orders/availability")
    fun requestAvailability(@Header("Authorization") bearer: String, @Query("customer_address_id") addressId: Int, @Query("category_id") categoryId: Int): Call<RequestAvailabilityPayload>

    @GET("config")
    fun config(): Call<AppConfig>

    @GET("catalog")
    fun catalog(@Query("city_id") cityId: Int? = null): Call<DataList<CatalogCategory>>

    @GET("addresses")
    fun addresses(@Header("Authorization") bearer: String): Call<DataList<CustomerAddress>>

    @GET("me")
    fun me(@Header("Authorization") bearer: String): Call<AccountResponse>

    @GET("orders")
    fun orders(@Header("Authorization") bearer: String, @Query("scope") scope: String, @Query("page") page: Int): Call<OrdersPage>

    @GET("orders/{order}")
    fun order(@Header("Authorization") bearer: String, @Path("order") orderId: Int): Call<DataItem<CustomerOrder>>

    @POST("orders/{order}/payments")
    fun createPayment(@Header("Authorization") bearer: String, @Path("order") orderId: Int, @Body body: CreatePaymentBody): Call<DataItem<PaymentAttempt>>

    @POST("orders/{order}/instapay-transfers")
    fun submitInstapay(@Header("Authorization") bearer: String, @Path("order") orderId: Int, @Body body: InstapayTransferBody): Call<DataItem<PaymentAttempt>>

    @POST("orders/{order}/review")
    fun submitReview(@Header("Authorization") bearer: String, @Path("order") orderId: Int, @Body body: ReviewBody): Call<Unit>

    @GET("orders/{order}/warranty-claims")
    fun warrantyClaims(@Header("Authorization") bearer: String, @Path("order") orderId: Int): Call<DataList<CustomerDispute>>
    @POST("orders/{order}/warranty-claims")
    fun requestWarranty(@Header("Authorization") bearer: String, @Path("order") orderId: Int, @Body body: WarrantyBody): Call<DataItem<CustomerDispute>>

    @GET("orders/{order}/disputes")
    fun disputes(@Header("Authorization") bearer: String, @Path("order") orderId: Int): Call<DataList<CustomerDispute>>

    @POST("orders/{order}/disputes")
    fun openDispute(@Header("Authorization") bearer: String, @Path("order") orderId: Int, @Body body: OpenDisputeBody): Call<DataItem<CustomerDispute>>

    @POST("providers/{provider}/reports")
    fun reportProvider(@Header("Authorization") bearer: String, @Path("provider") providerId: Int, @Body body: ProviderReportBody): Call<Unit>

    @GET("support/faqs")
    fun faqs(): Call<DataList<FaqItem>>

    @POST("support/messages")
    fun sendSupportMessage(@Header("Authorization") bearer: String, @Body body: SupportMessageBody): Call<Unit>

    @GET("notifications")
    fun notifications(@Header("Authorization") bearer: String): Call<DataList<NotificationItem>>

    @POST("notifications/read")
    fun markNotificationsRead(@Header("Authorization") bearer: String, @Body body: NotificationsReadBody): Call<Unit>

    @POST("me/devices")
    fun registerDevice(@Header("Authorization") bearer: String, @Body body: RegisterDeviceBody): Call<Unit>

    @HTTP(method = "DELETE", path = "me/devices", hasBody = true)
    fun unregisterDevice(@Header("Authorization") bearer: String, @Body body: DeviceTokenBody): Call<Unit>

    @POST("auth/logout")
    fun logout(@Header("Authorization") bearer: String, @Body body: EmptyBody = EmptyBody()): Call<Unit>

    @PATCH("me")
    fun updateRatingReminders(@Header("Authorization") bearer: String, @Body body: RatingRemindersBody): Call<AccountResponse>

    @PATCH("me")
    fun updateAccount(@Header("Authorization") bearer: String, @Body body: AccountUpdateBody): Call<AccountResponse>

    @PATCH("me")
    fun updateLocale(@Header("Authorization") bearer: String, @Body body: LocaleUpdateBody): Call<AccountResponse>

    @POST("me/password")
    fun changePassword(@Header("Authorization") bearer: String, @Body body: PasswordChangeBody): Call<Unit>

    @DELETE("me")
    fun deleteAccount(@Header("Authorization") bearer: String): Call<Unit>

    @POST("addresses/{address}/default")
    fun makeAddressDefault(@Header("Authorization") bearer: String, @Path("address") addressId: Int, @Body body: EmptyBody = EmptyBody()): Call<AddressBookUpdate>

    @DELETE("addresses/{address}")
    fun deleteAddress(@Header("Authorization") bearer: String, @Path("address") addressId: Int): Call<DataItem<AddressDeletion>>

    @DELETE("media/{media}")
    fun deleteMedia(@Header("Authorization") bearer: String, @Path("media") mediaId: Int): Call<Unit>

    @GET("terms/current")
    fun terms(): Call<DataItem<TermsPage>>

    @GET("cities")
    fun cities(): Call<DataList<IdName>>

    @GET("cities/{city}/areas")
    fun areas(@Path("city") cityId: Int): Call<DataList<IdName>>

    @POST("addresses")
    fun createAddress(@Header("Authorization") bearer: String, @Body body: AddressBody): Call<DataItem<CustomerAddress>>

    @PATCH("addresses/{address}")
    fun updateAddress(
        @Header("Authorization") bearer: String,
        @Path("address") addressId: Int,
        @Body body: AddressBody,
    ): Call<DataItem<CustomerAddress>>

    @GET("slots")
    fun slots(@Query("city_id") cityId: Int, @Query("date") date: String): Call<DataList<SlotRange>>

    @GET("conversations")
    fun conversations(@Header("Authorization") bearer: String, @Query("page") page: Int): Call<ConversationsPage>

    @GET("conversations/{conversation}/messages")
    fun conversationMessages(
        @Header("Authorization") bearer: String,
        @Path("conversation") conversationId: Int,
        @Query("page") page: Int,
    ): Call<ConversationMessagesPage>

    @POST("conversations/{conversation}/messages")
    fun sendMessage(
        @Header("Authorization") bearer: String,
        @Path("conversation") conversationId: Int,
        @Body body: SendMessageBody,
    ): Call<DataItem<CustomerMessage>>

    @Multipart
    @POST("media")
    fun uploadMedia(@Header("Authorization") bearer: String, @Part file: MultipartBody.Part): Call<UploadedMediaResponse>

    @GET("orders/{order}/offers")
    fun offers(@Header("Authorization") bearer: String, @Path("order") orderId: Int, @Query("sort") sort: String): Call<DataList<CustomerOffer>>

    @GET("providers/{provider}")
    fun provider(
        @Header("Authorization") bearer: String,
        @Path("provider") providerId: Int,
        @Query("order_id") orderId: Int,
    ): Call<DataItem<ProviderContext>>

    @GET("orders/{order}/proposals")
    fun proposals(@Header("Authorization") bearer: String, @Path("order") orderId: Int): Call<DataList<CustomerProposal>>

    @GET("orders/{order}/tracking")
    fun tracking(@Header("Authorization") bearer: String, @Path("order") orderId: Int): Call<TrackingResponse>

    @POST("orders")
    fun publish(
        @Header("Authorization") bearer: String,
        @Header("Idempotency-Key") idempotencyKey: String,
        @Body body: PublishOrderBody,
    ): Call<DataItem<CustomerOrder>>

    @PATCH("orders/{order}")
    fun updateOrder(@Header("Authorization") bearer: String, @Path("order") orderId: Int, @Body body: PublishOrderBody): Call<DataItem<CustomerOrder>>

    @POST("orders/{order}/offers/{offer}/accept")
    fun acceptOffer(
        @Header("Authorization") bearer: String,
        @Path("order") orderId: Int,
        @Path("offer") offerId: Int,
        @Body body: AcceptOfferRequest,
    ): Call<DataItem<CustomerOrder>>

    @POST("orders/{order}/cancel")
    fun cancelOrder(@Header("Authorization") bearer: String, @Path("order") orderId: Int, @Body body: CancelOrderRequest): Call<DataItem<CustomerOrder>>

    @POST("orders/{order}/proposals/{proposal}/decide")
    fun decideProposal(
        @Header("Authorization") bearer: String,
        @Path("order") orderId: Int,
        @Path("proposal") proposalId: Int,
        @Body body: DecideProposalRequest,
    ): Call<DataItem<CustomerOrder>>

    @HTTP(method = "PATCH", path = "orders/{order}/payment-method", hasBody = true)
    fun changePaymentMethod(@Header("Authorization") bearer: String, @Path("order") orderId: Int, @Body body: PaymentMethodRequest): Call<DataItem<CustomerOrder>>

    @POST("orders/{order}/confirm-completion")
    fun confirmCompletion(@Header("Authorization") bearer: String, @Path("order") orderId: Int, @Body body: ExpectedVersionRequest): Call<DataItem<CustomerOrder>>

    @POST("orders/{order}/republish")
    fun republish(@Header("Authorization") bearer: String, @Path("order") orderId: Int, @Body body: Map<String, String> = emptyMap()): Call<DataItem<CustomerOrder>>
}

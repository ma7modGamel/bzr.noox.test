package noox.bzr.provider

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import noox.bzr.customer.CatalogCategory
import noox.bzr.customer.ConversationMessagesPage
import noox.bzr.customer.ConversationsPage
import noox.bzr.customer.DataItem
import noox.bzr.customer.DataList
import noox.bzr.customer.EmptyBody
import noox.bzr.customer.IdName
import noox.bzr.customer.NotificationItem
import noox.bzr.customer.NotificationsReadBody
import noox.bzr.customer.SendMessageBody
import noox.bzr.customer.UploadedMediaResponse
import okhttp3.ResponseBody
import okhttp3.MultipartBody
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Multipart
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.Query

// DEC-061 (9أ stage 4, first endpoint) and DEC-068 — `GET provider/earnings`, read from the recorded server
// answers in src/test/resources/api/provider (tests/Feature/Api/MobileProviderEarningsResponsesTest.php).
// Money arrives as decimal strings ("-650.00").

@Serializable
data class EarningsSummary(val balance: String, val available: String, val pending: String)

/** BR-063: [limit] is null in employee mode or before OD-02 sets CFG-061. */
@Serializable
data class EarningsDues(val blocked: Boolean = false, val debt: String = "0.00", val limit: String? = null)

@Serializable
data class EarningsTransaction(
    val id: String,
    val type: String,
    val title: String,
    val detail: String,
    val amount: String,
    @SerialName("occurred_at") val occurredAt: String? = null,
    val available: Boolean = false,
)

@Serializable
data class EarningsMeta(@SerialName("current_page") val currentPage: Int, @SerialName("last_page") val lastPage: Int, val total: Int = 0)

@Serializable
data class ProviderEarnings(
    @SerialName("operating_mode") val operatingMode: String,
    val summary: EarningsSummary,
    val dues: EarningsDues = EarningsDues(),
    val transactions: List<EarningsTransaction> = emptyList(),
    val meta: EarningsMeta? = null,
    @SerialName("filter_options") val filterOptions: List<ProviderOption> = emptyList(),
)

@Serializable
data class ProviderEarningsResponse(val data: ProviderEarnings)

/** An option with the payout field label (P06); other option lists ignore it. */
@Serializable
data class ProviderOption(val code: String, val label: String, @SerialName("field_label") val fieldLabel: String? = null)

/** `GET config`, as far as the provider screens read it. */
@Serializable
data class ProviderConfig(@SerialName("option_lists") val optionLists: Map<String, List<ProviderOption>> = emptyMap())

/** One Retrofit function per endpoint (DEC-061, 9أ stage 4). */
interface ProviderService {
    @retrofit2.http.GET
    fun privateImage(@retrofit2.http.Header("Authorization") bearer: String, @retrofit2.http.Url url: String): retrofit2.Call<okhttp3.ResponseBody>

    @GET("provider/earnings")
    fun earnings(@Header("Authorization") bearer: String, @Query("page") page: Int = 1, @Query("type") type: String? = null, @Query("date_from") dateFrom: String? = null, @Query("date_to") dateTo: String? = null): Call<ProviderEarningsResponse>

    @GET("provider/earnings/export")
    fun exportEarnings(@Header("Authorization") bearer: String, @Query("type") type: String?, @Query("date_from") dateFrom: String?, @Query("date_to") dateTo: String?): Call<ResponseBody>

    @GET("notifications")
    fun notifications(@Header("Authorization") bearer: String): Call<DataList<NotificationItem>>

    @POST("notifications/read")
    fun markNotificationsRead(@Header("Authorization") bearer: String, @Body body: NotificationsReadBody): Call<Unit>

    @GET("provider/application")
    fun application(@Header("Authorization") bearer: String): Call<DataItem<ProviderApplication>>

    @POST("provider/application")
    fun submitApplication(@Header("Authorization") bearer: String, @Body body: ApplicationBody): Call<DataItem<ProviderApplication>>

    @GET("catalog")
    fun catalog(): Call<DataList<CatalogCategory>>

    @GET("cities")
    fun cities(): Call<DataList<IdName>>

    @GET("cities/{city}/areas")
    fun areas(@Path("city") cityId: Int): Call<DataList<IdName>>

    @GET("config")
    fun config(): Call<ProviderConfig>

    @Multipart
    @POST("media")
    fun uploadMedia(@Header("Authorization") bearer: String, @Part file: MultipartBody.Part): Call<UploadedMediaResponse>

    @GET("provider/home")
    fun home(@Header("Authorization") bearer: String): Call<DataItem<ProviderHome>>

    @PUT("provider/availability")
    fun setAvailability(@Header("Authorization") bearer: String, @Body body: AvailabilityBody): Call<DataItem<ProviderHome>>

    @GET("provider/requests")
    fun availableRequests(@Header("Authorization") bearer: String): Call<DataList<ProviderOrder>>

    @GET("provider/requests/{order}")
    fun availableRequest(@Header("Authorization") bearer: String, @Path("order") orderId: Int): Call<DataItem<MarketRequest>>

    @POST("provider/requests/{order}/offers")
    fun submitOffer(@Header("Authorization") bearer: String, @Path("order") orderId: Int, @Body body: OfferBody): Call<DataItem<ProviderOffer>>

    @GET("provider/offers")
    fun offers(@Header("Authorization") bearer: String): Call<DataList<ProviderOffer>>

    @POST("provider/offers/{offer}/withdraw")
    fun withdrawOffer(@Header("Authorization") bearer: String, @Path("offer") offerId: Int, @Body body: EmptyBody = EmptyBody()): Call<DataItem<ProviderOffer>>

    @GET("provider/orders")
    fun orders(@Header("Authorization") bearer: String, @Query("scope") scope: String): Call<DataList<ProviderOrder>>

    @POST("provider/orders/{order}/work-photos")
    fun attachWorkPhoto(@Header("Authorization") bearer: String, @Path("order") orderId: Int, @Body body: WorkPhotoBody): Call<DataItem<ProviderOrder>>

    @POST("provider/orders/{order}/{action}")
    fun orderAction(
        @Header("Authorization") bearer: String,
        @Path("order") orderId: Int,
        @Path("action") action: String,
        @Body body: OrderActionBody,
    ): Call<DataItem<ProviderOrder>>

    @POST("provider/orders/{order}/proposals")
    fun submitProposal(@Header("Authorization") bearer: String, @Path("order") orderId: Int, @Body body: ProposalBody): Call<Unit>

    @POST("provider/orders/{order}/customer-rating")
    fun rateCustomer(@Header("Authorization") bearer: String, @Path("order") orderId: Int, @Body body: CustomerRatingBody): Call<Unit>

    @GET("provider/profile")
    fun profile(@Header("Authorization") bearer: String): Call<DataItem<ProviderWorkspaceProfile>>

    @PATCH("provider/profile")
    fun updateProfile(@Header("Authorization") bearer: String, @Body body: ProfileUpdateBody): Call<DataItem<ProviderWorkspaceProfile>>

    @POST("provider/portfolio")
    fun addPortfolio(@Header("Authorization") bearer: String, @Body body: PortfolioBody): Call<DataItem<PortfolioEntry>>

    @DELETE("provider/portfolio/{item}")
    fun deletePortfolio(@Header("Authorization") bearer: String, @Path("item") itemId: Int): Call<Unit>

    @PATCH("me")
    fun updateAvatar(@Header("Authorization") bearer: String, @Body body: AvatarBody): Call<Unit>

    @GET("conversations")
    fun conversations(@Header("Authorization") bearer: String): Call<ConversationsPage>

    @GET("conversations/{conversation}/messages")
    fun conversationMessages(@Header("Authorization") bearer: String, @Path("conversation") conversationId: Int): Call<ConversationMessagesPage>

    @POST("conversations/{conversation}/messages")
    fun sendMessage(@Header("Authorization") bearer: String, @Path("conversation") conversationId: Int, @Body body: SendMessageBody): Call<Unit>
}

internal fun ProviderEarnings.toPayload() = ProviderEarningsPayload(
    operatingMode = operatingMode,
    summary = listOf(summary.balance, summary.available, summary.pending),
    transactionTitles = transactions.map(EarningsTransaction::title),
    transactionDetails = transactions.map(EarningsTransaction::detail),
    transactionStates = transactions.map(EarningsTransaction::type),
    filterOptions = filterOptions.map { ProviderReason(it.code, it.label) },
    currentPage = meta?.currentPage ?: 1, lastPage = meta?.lastPage ?: 1,
    duesBlocked = dues.blocked,
    duesDebt = dues.debt,
    duesLimit = dues.limit,
)

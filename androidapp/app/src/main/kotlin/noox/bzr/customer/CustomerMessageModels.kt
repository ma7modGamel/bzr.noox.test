package noox.bzr.customer

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// DEC-061, 9أ stage 3 — conversations and messages (C18/C19) and media uploads (C17, C19, C27), read from the
// recorded server answers in src/test/resources/api/customer (MobileCustomerMessagesResponsesTest).

/** Order numbers are integers on the wire; [number] keeps them as sent. */
@Serializable
data class ConversationOrder(
    val id: Int,
    val number: Long? = null,
    val status: String? = null,
    @SerialName("status_label") val statusLabel: String? = null,
    val category: String? = null,
    @SerialName("problem_type") val problemType: String? = null,
    val area: String? = null,
)

@Serializable
data class ConversationProvider(val id: Int, val name: String? = null, @SerialName("is_verified") val isVerified: Boolean = false, @SerialName("available_now") val availableNow: Boolean = false)

@Serializable
data class MessageMedia(val id: Int, val type: String, @SerialName("content_url") val contentUrl: String? = null)

@Serializable
data class CustomerMessage(
    val id: Int,
    @SerialName("is_mine") val isMine: Boolean = false,
    val body: String? = null,
    @SerialName("was_masked") val wasMasked: Boolean = false,
    val media: List<MessageMedia> = emptyList(),
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("read_at") val readAt: String? = null,
)

/** The other side for a provider (C19 in provider mode). */
@Serializable
data class ConversationCustomer(val id: Int? = null, val name: String? = null)

@Serializable
data class CustomerConversation(
    val id: Int,
    val order: ConversationOrder,
    val provider: ConversationProvider,
    val customer: ConversationCustomer? = null,
    val status: String,
    @SerialName("last_message") val lastMessage: CustomerMessage? = null,
    @SerialName("unread_count") val unreadCount: Int = 0,
)

@Serializable
data class SimplePageMeta(@SerialName("current_page") val currentPage: Int = 1, @SerialName("last_page") val lastPage: Int = 1)

@Serializable
data class ConversationsPage(val data: List<CustomerConversation>, val meta: SimplePageMeta = SimplePageMeta())

@Serializable
data class ConversationMessagesPage(
    val conversation: CustomerConversation,
    val data: List<CustomerMessage>,
    val meta: SimplePageMeta = SimplePageMeta(),
)

@Serializable
data class SendMessageBody(val body: String?, @SerialName("media_ids") val mediaIds: List<Int>)

/** A temporary upload (BR-014): attached to a request, message or dispute by its id. */
@Serializable
data class UploadedMedia(
    val id: Int,
    val type: String,
    @SerialName("size_bytes") val sizeBytes: Long? = null,
    @SerialName("duration_sec") val durationSec: Int? = null,
    @SerialName("expires_at") val expiresAt: String? = null,
)

@Serializable
data class UploadedMediaResponse(val media: UploadedMedia)

/** "Category • status" — the conversation line on C18 (DEC-076, Figma 3002:2924). */
internal fun ConversationOrder.summary(): String = listOfNotNull(category, statusLabel).filter(String::isNotBlank).joinToString(" • ")

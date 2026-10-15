package noox.bzr.customer

import kotlinx.serialization.KSerializer
import noox.bzr.customer.CustomerOrderModelsDecodeTest.Companion.recorded
import noox.bzr.network.BremoHttp
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * DEC-061 / 9أ stage 3, groups B and E: conversations, messages and media decode the real server answers
 * (MobileCustomerMessagesResponsesTest), and the calls send the same requests as the JSONObject code did.
 */
class CustomerMessageModelsDecodeTest {
    @Test
    fun conversationsDecode() {
        val conversation = decode("conversations", ConversationsPage.serializer()).data.single()
        assertEquals(1L, conversation.order.number)
        assertEquals("سباكة • تم التأكيد", conversation.order.summary())
        assertEquals("كريم السباك", conversation.provider.name)
        assertTrue(conversation.lastMessage!!.isMine)
    }

    @Test
    fun messagesAndMediaDecode() {
        val page = decode("conversation_messages", ConversationMessagesPage.serializer())
        assertEquals(3, page.data.size)
        assertEquals(listOf(false, true, true), page.data.map(CustomerMessage::isMine))
        assertEquals("IMAGE", page.data.last().media.single().type)
        assertEquals("OPEN", page.conversation.status)
        assertTrue(decode("message_sent", DataItem.serializer(CustomerMessage.serializer())).data.isMine)
        assertEquals("IMAGE", decode("media_uploaded", UploadedMediaResponse.serializer()).media.type)
    }

    @Test
    fun callsSendTheSameRequests() {
        val server = MockWebServer()
        server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse = when {
                request.path!!.endsWith("/media") -> MockResponse().setResponseCode(201).setBody(recorded("media_uploaded"))
                request.method == "POST" -> MockResponse().setResponseCode(201).setBody(recorded("message_sent"))
                request.path!!.contains("/messages") -> MockResponse().setBody(recorded("conversation_messages"))
                else -> MockResponse().setBody(recorded("conversations"))
            }
        }
        server.start()
        try {
            val api = UrlConnectionCustomerApi(server.url("/api/v1/").toString())
            api.conversations("T", 2)
            api.conversationMessages("T", 9)
            api.sendMessage("T", 9, "", listOf(4))
            val media = api.uploadMedia("T", CustomerMediaUpload("leak.jpg", "image/jpeg", byteArrayOf(1, 2, 3)))

            val requests = List(4) { server.takeRequest() }
            assertEquals(
                listOf("conversations?page=2", "conversations/9/messages?page=1", "conversations/9/messages", "media"),
                requests.map { it.path!!.substringAfter("/api/v1/") },
            )
            val sent = JSONObject(requests[2].body.readUtf8())
            assertTrue(sent.isNull("body"))
            assertEquals(4, sent.getJSONArray("media_ids").getInt(0))
            assertTrue(requests[3].getHeader("Content-Type")!!.startsWith("multipart/form-data"))
            assertTrue(requests[3].body.readUtf8().contains("name=\"file\"; filename=\"leak.jpg\""))
            assertEquals("Bearer T", requests[3].getHeader("Authorization"))
            assertEquals("CUSTOMER", requests[3].getHeader("X-App-Mode"))
            assertEquals("IMAGE", media.type)
        } finally {
            server.shutdown()
        }
    }

    private fun <T> decode(name: String, serializer: KSerializer<T>): T = BremoHttp.json.decodeFromString(serializer, recorded(name))
}

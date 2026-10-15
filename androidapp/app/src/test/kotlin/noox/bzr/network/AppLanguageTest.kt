package noox.bzr.network

import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.Assert.assertEquals
import org.junit.Test

class AppLanguageTest {
    @Test
    fun negotiatesSupportedRegionalTagsAndDefaultsToArabic() {
        assertEquals("en", AppLanguage.resolve(listOf("fr-FR", "en-GB", "ar")))
        assertEquals("ar", AppLanguage.resolve(listOf("ar_EG", "en")))
        assertEquals("ar", AppLanguage.resolve(listOf("fr")))
        assertEquals("ar", AppLanguage.resolve(emptyList()))
    }

    @Test
    fun cachedServiceUsesCurrentLanguageForEveryRequest() {
        val previous = BremoHttp.language
        val server = MockWebServer().apply { start() }
        try {
            val service = BremoHttp.typed(server.url("/api/v1/").toString(), ProbeService::class.java)
            for (language in listOf("en", "ar", "en")) {
                BremoHttp.language = { language }
                server.enqueue(MockResponse().setBody("""{"id":1}"""))
                bremoExecute(service.get(null))
                assertEquals(language, server.takeRequest().getHeader("Accept-Language"))
            }
        } finally {
            BremoHttp.language = previous
            server.shutdown()
        }
    }
}

package noox.bzr.auth

import noox.bzr.network.BremoApiClient
import org.json.JSONObject

data class AuthSession(val token: String, val email: String, val isVerified: Boolean)

interface AuthApi {
    fun login(email: String, password: String): AuthSession
    fun register(name: String, email: String, phone: String, password: String): AuthSession
    fun me(token: String): Boolean
    fun resendVerification(token: String)
    fun forgotPassword(email: String)
    fun logout(token: String)
}

class AuthApiException(val code: String, val fields: Map<String, List<String>> = emptyMap()) : RuntimeException(code)

class UrlConnectionAuthApi(private val baseUrl: String) : AuthApi {
    override fun login(email: String, password: String): AuthSession {
        val payload = request("auth/login", JSONObject().put("email", email).put("password", password))
        return session(payload)
    }

    override fun register(name: String, email: String, phone: String, password: String): AuthSession {
        val body = JSONObject().put("name", name).put("email", email).put("phone", phone).put("password", password)
        return session(request("auth/register", body))
    }

    override fun me(token: String): Boolean = request("me", token = token, method = "GET").getJSONObject("user").getBoolean("is_verified")

    override fun resendVerification(token: String) {
        request("auth/email/resend", token = token)
    }

    override fun forgotPassword(email: String) {
        request("auth/password/forgot", JSONObject().put("email", email))
    }

    override fun logout(token: String) {
        request("auth/logout", token = token)
    }

    private fun session(json: JSONObject): AuthSession {
        val user = json.getJSONObject("user")
        return AuthSession(json.getString("token"), user.getString("email"), user.getBoolean("is_verified"))
    }

    private val client = BremoApiClient(baseUrl)

    private fun request(path: String, body: JSONObject? = JSONObject(), token: String? = null, method: String = "POST"): JSONObject {
        val response = client.send(path, method, body, token)
        if (!response.isSuccessful) {
            val error = response.text.takeIf { it.isNotBlank() }?.let { runCatching { JSONObject(it) }.getOrNull() }?.optJSONObject("error")
            throw AuthApiException(error?.optString("code").orEmpty().ifEmpty { "HTTP_${response.status}" })
        }
        return response.json()
    }
}

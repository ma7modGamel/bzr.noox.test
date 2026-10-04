package noox.bzr.auth

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import noox.bzr.network.ApiException
import noox.bzr.network.BremoHttp
import noox.bzr.network.bremoCall
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST

data class AuthSession(val token: String, val email: String, val isVerified: Boolean)

interface AuthApi {
    suspend fun login(email: String, password: String): AuthSession
    suspend fun register(name: String, email: String, phone: String, password: String): AuthSession
    suspend fun me(token: String): Boolean
    suspend fun resendVerification(token: String)
    suspend fun forgotPassword(email: String)
    suspend fun logout(token: String)
}

class AuthApiException(val code: String, val fields: Map<String, List<String>> = emptyMap()) : RuntimeException(code)

// DEC-061 — typed models for the identity endpoints, read from the recorded server answers in
// src/test/resources/api/auth (tests/Feature/Api/MobileAuthResponsesTest.php).

@Serializable
data class LoginRequest(val email: String, val password: String)

@Serializable
data class RegisterRequest(val name: String, val email: String, val phone: String, val password: String)

@Serializable
data class ForgotPasswordRequest(val email: String)

/** `user.provider` on login and register; null for a customer without a provider profile. */
@Serializable
data class AuthProviderProfile(
    val id: Long,
    val status: String,
    @SerialName("available_now") val availableNow: Boolean,
    @SerialName("is_verified") val isVerified: Boolean,
)

/** The user in the login and register answers (AuthController::userPayload). */
@Serializable
data class AuthUser(
    val id: Long,
    val name: String,
    val email: String,
    val phone: String? = null,
    @SerialName("is_verified") val isVerified: Boolean,
    /** decimal:2 on the server, so a string ("4.50"). */
    @SerialName("rating_avg") val ratingAvg: String? = null,
    val provider: AuthProviderProfile? = null,
)

@Serializable
data class AuthResponse(
    val user: AuthUser,
    val token: String,
    @SerialName("email_verification_required") val emailVerificationRequired: Boolean = false,
) {
    fun session() = AuthSession(token, user.email, user.isVerified)
}

@Serializable
data class AccountTerms(
    @SerialName("current_version") val currentVersion: Int? = null,
    @SerialName("accepted_version") val acceptedVersion: Int? = null,
    @SerialName("acceptance_required") val acceptanceRequired: Boolean = false,
)

/** The user in `GET me` (AccountController::payload). */
@Serializable
data class AccountUser(
    val id: Long,
    val name: String,
    val email: String,
    val phone: String? = null,
    @SerialName("avatar_url") val avatarUrl: String? = null,
    @SerialName("is_verified") val isVerified: Boolean,
    @SerialName("rating_reminders_enabled") val ratingRemindersEnabled: Boolean = true,
    val terms: AccountTerms? = null,
    @SerialName("available_actions") val availableActions: List<String> = emptyList(),
)

@Serializable
data class AccountResponse(val user: AccountUser)

/** One Retrofit function per identity endpoint (DEC-061). */
interface AuthService {
    @POST("auth/login")
    suspend fun login(@Body body: LoginRequest): Response<AuthResponse>

    @POST("auth/register")
    suspend fun register(@Body body: RegisterRequest): Response<AuthResponse>

    @GET("me")
    suspend fun me(@Header("Authorization") bearer: String): Response<AccountResponse>

    @POST("auth/email/resend")
    suspend fun resendVerification(@Header("Authorization") bearer: String): Response<Unit>

    @POST("auth/password/forgot")
    suspend fun forgotPassword(@Body body: ForgotPasswordRequest): Response<Unit>

    @POST("auth/logout")
    suspend fun logout(@Header("Authorization") bearer: String): Response<Unit>
}

class RetrofitAuthApi(baseUrl: String) : AuthApi {
    private val service = BremoHttp.typed(baseUrl, AuthService::class.java)

    override suspend fun login(email: String, password: String): AuthSession =
        call("POST", "auth/login") { service.login(LoginRequest(email, password)) }.session()

    override suspend fun register(name: String, email: String, phone: String, password: String): AuthSession =
        call("POST", "auth/register") { service.register(RegisterRequest(name, email, phone, password)) }.session()

    override suspend fun me(token: String): Boolean = call("GET", "me") { service.me(bearer(token)) }.user.isVerified

    override suspend fun resendVerification(token: String) {
        call("POST", "auth/email/resend") { service.resendVerification(bearer(token)) }
    }

    override suspend fun forgotPassword(email: String) {
        call("POST", "auth/password/forgot") { service.forgotPassword(ForgotPasswordRequest(email)) }
    }

    override suspend fun logout(token: String) {
        call("POST", "auth/logout") { service.logout(bearer(token)) }
    }

    private fun bearer(token: String) = "Bearer $token"

    /** Keeps the screens' error contract: only the code reaches AuthLogic, as before DEC-061. */
    private suspend fun <T> call(method: String, path: String, block: suspend () -> Response<T>): T = try {
        bremoCall(method, path, block)
    } catch (exception: ApiException) {
        throw AuthApiException(exception.code)
    }
}

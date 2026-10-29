package Coffee.Auth.Services

import Coffee.Auth.Configuration.SupabaseAuthProperties
import Coffee.Auth.DTO.LoginRequest
import Coffee.Auth.DTO.RegisterRequest
import Coffee.Auth.Model.AppSession
import Coffee.Auth.Model.Profile
import Coffee.Auth.Repository.ProfileRepository
import com.fasterxml.jackson.annotation.JsonProperty
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import tools.jackson.databind.ObjectMapper
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration
import java.util.UUID

@Service
class AuthService(
    private val supabaseAuthProperties: SupabaseAuthProperties,
    private val sessionService: SessionService,
    private val profileRepository: ProfileRepository,
    private val objectMapper: ObjectMapper
) {

    private val httpClient = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(10))
        .build()

    @Transactional
    fun login(request: LoginRequest, userAgent: String? = null, ipAddress: String? = null): AuthSessionResult {
        val supabaseResponse = validateCredentialsWithSupabase(request.email, request.password)
        val supabaseUserId = UUID.fromString(supabaseResponse.user.id)

        val profile = profileRepository.findByUserId(supabaseUserId)
            ?: createProfile(supabaseUserId, request.email)

        val token = sessionService.create(profile, userAgent, ipAddress)

        return AuthSessionResult(
            token = token,
            profile = profile
        )
    }

    @Transactional
    fun register(request: RegisterRequest, userAgent: String? = null, ipAddress: String? = null): AuthSessionResult {
        val supabaseResponse = signupWithSupabase(request.email, request.password)
        val supabaseUserId = UUID.fromString(supabaseResponse.user.id)

        val profile = Profile(
            userId = supabaseUserId,
            email = request.email,
            fullName = request.fullName,
            role = Coffee.Auth.Model.UserRole.CLIENTE
        )
        val savedProfile = profileRepository.save(profile)

        val token = sessionService.create(savedProfile, userAgent, ipAddress)

        return AuthSessionResult(
            token = token,
            profile = savedProfile
        )
    }

    fun resolveSession(token: String): AppSession? {
        return sessionService.resolve(token)
    }

    @Transactional
    fun revokeSession(session: AppSession) {
        sessionService.revoke(session)
    }

    @Transactional
    fun revokeAllSessions(profileId: UUID) {
        sessionService.revokeAll(profileId)
    }

    private fun validateCredentialsWithSupabase(email: String, password: String): SupabaseAuthResponse {
        val body = objectMapper.writeValueAsString(mapOf("email" to email, "password" to password))
        val request = HttpRequest.newBuilder()
            .uri(URI.create("${supabaseAuthProperties.url}/auth/v1/token?grant_type=password"))
            .header("apikey", supabaseAuthProperties.anonKey)
            .header("Authorization", "Bearer ${supabaseAuthProperties.anonKey}")
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(body))
            .timeout(Duration.ofSeconds(10))
            .build()

        val response = httpClient.send(request, HttpResponse.BodyHandlers.ofString())
        if (response.statusCode() == 400 || response.statusCode() == 401) {
            throw InvalidCredentialsException()
        }
        if (response.statusCode() >= 400) {
            throw SupabaseAuthException("Error al autenticar")
        }
        return objectMapper.readValue(response.body(), SupabaseAuthResponse::class.java)
    }

    private fun signupWithSupabase(email: String, password: String): SupabaseAuthResponse {
        val body = objectMapper.writeValueAsString(mapOf("email" to email, "password" to password))
        val request = HttpRequest.newBuilder()
            .uri(URI.create("${supabaseAuthProperties.url}/auth/v1/signup"))
            .header("apikey", supabaseAuthProperties.anonKey)
            .header("Authorization", "Bearer ${supabaseAuthProperties.anonKey}")
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(body))
            .timeout(Duration.ofSeconds(10))
            .build()

        val response = httpClient.send(request, HttpResponse.BodyHandlers.ofString())
        if (response.statusCode() >= 400) {
            throw SupabaseAuthException("Error al registrar usuario")
        }
        return objectMapper.readValue(response.body(), SupabaseAuthResponse::class.java)
    }

    private fun createProfile(userId: UUID, email: String): Profile {
        val profile = Profile(
            userId = userId,
            email = email,
            fullName = null,
            role = Coffee.Auth.Model.UserRole.CLIENTE
        )
        return profileRepository.save(profile)
    }
}

data class AuthSessionResult(
    val token: String,
    val profile: Profile
)

data class SupabaseAuthResponse(
    @JsonProperty("access_token")
    val accessToken: String?,
    @JsonProperty("refresh_token")
    val refreshToken: String?,
    val user: SupabaseAuthUser
)

data class SupabaseAuthUser(
    val id: String,
    val email: String?,
    @JsonProperty("email_confirmed_at")
    val emailConfirmedAt: String?,
    @JsonProperty("created_at")
    val createdAt: String?,
    @JsonProperty("updated_at")
    val updatedAt: String?
)

class InvalidCredentialsException : RuntimeException("Invalid credentials")
class SupabaseAuthException(message: String) : RuntimeException(message)

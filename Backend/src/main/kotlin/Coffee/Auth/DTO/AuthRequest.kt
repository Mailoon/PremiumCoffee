package Coffee.Auth.DTO

import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class LoginRequest(
    @field:Email(message = "El email no es válido")
    @field:NotBlank(message = "El email es obligatorio")
    val email: String,

    @field:NotBlank(message = "La contraseña es obligatoria")
    @field:Size(min = 6, max = 128, message = "La contraseña debe tener entre 6 y 128 caracteres")
    val password: String
)

data class RegisterRequest(
    @field:Email(message = "El email no es válido")
    @field:NotBlank(message = "El email es obligatorio")
    val email: String,

    @field:NotBlank(message = "La contraseña es obligatoria")
    @field:Size(min = 6, max = 128, message = "La contraseña debe tener entre 6 y 128 caracteres")
    val password: String,

    @field:Size(max = 200, message = "El nombre no puede exceder 200 caracteres")
    val fullName: String? = null
)

data class ForgotPasswordRequest(
    @field:Email(message = "El email no es válido")
    @field:NotBlank(message = "El email es obligatorio")
    val email: String
)

data class ResetPasswordRequest(
    @field:NotBlank(message = "El token es obligatorio")
    val token: String,

    @field:NotBlank(message = "La contraseña es obligatoria")
    @field:Size(min = 6, max = 128, message = "La contraseña debe tener entre 6 y 128 caracteres")
    val password: String
)

data class AuthResponse(
    val userId: String,
    val email: String,
    val role: String,
    val sessionId: String
)

data class RefreshRequest(
    @field:NotBlank(message = "El refresh token es obligatorio")
    val refreshToken: String
)


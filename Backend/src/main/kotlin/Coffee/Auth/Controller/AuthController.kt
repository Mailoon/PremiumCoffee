package Coffee.Auth.Controller

import Coffee.Auth.DTO.AuthResponse
import Coffee.Auth.DTO.ForgotPasswordRequest
import Coffee.Auth.DTO.LoginRequest
import Coffee.Auth.DTO.RegisterRequest
import Coffee.Auth.DTO.ResetPasswordRequest
import Coffee.Auth.Services.AuthService
import Coffee.Auth.Services.SessionCookieService
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1/auth")
class AuthController(
    private val authService: AuthService,
    private val sessionCookieService: SessionCookieService
) {

    @PostMapping("/login")
    fun login(
        @RequestBody loginRequest: LoginRequest,
        httpRequest: HttpServletRequest,
        response: HttpServletResponse
    ): ResponseEntity<AuthResponse> {
        val result = authService.login(
            loginRequest,
            userAgent = httpRequest.getHeader("User-Agent"),
            ipAddress = httpRequest.remoteAddr
        )

        sessionCookieService.addSessionCookie(response, result.token)

        return ResponseEntity.ok(
            AuthResponse(
                userId = result.profile.userId.toString(),
                email = result.profile.email,
                role = result.profile.role.name,
                sessionId = result.token
            )
        )
    }

    @PostMapping("/register")
    fun register(
        @RequestBody registerRequest: RegisterRequest,
        httpRequest: HttpServletRequest,
        response: HttpServletResponse
    ): ResponseEntity<AuthResponse> {
        val result = authService.register(
            registerRequest,
            userAgent = httpRequest.getHeader("User-Agent"),
            ipAddress = httpRequest.remoteAddr
        )

        sessionCookieService.addSessionCookie(response, result.token)

        return ResponseEntity.status(HttpStatus.CREATED).body(
            AuthResponse(
                userId = result.profile.userId.toString(),
                email = result.profile.email,
                role = result.profile.role.name,
                sessionId = result.token
            )
        )
    }

    @PostMapping("/logout")
    fun logout(
        request: HttpServletRequest,
        response: HttpServletResponse
    ): ResponseEntity<Void> {
        val cookie = request.getCookies()?.find { it.name == sessionCookieService.getCookieName() }
        if (cookie != null) {
            val session = authService.resolveSession(cookie.value)
            if (session != null) {
                authService.revokeSession(session)
            }
        }

        sessionCookieService.clearSessionCookie(response)

        return ResponseEntity.noContent().build()
    }

    @GetMapping("/me")
    fun me(
        request: HttpServletRequest
    ): ResponseEntity<AuthResponse> {
        val cookie = request.getCookies()?.find { it.name == sessionCookieService.getCookieName() }
        if (cookie == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()
        }

        val session = authService.resolveSession(cookie.value)
        if (session == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()
        }

        return ResponseEntity.ok(
            AuthResponse(
                userId = session.profile.userId.toString(),
                email = session.profile.email,
                role = session.profile.role.name,
                sessionId = cookie.value
            )
        )
    }

    @PostMapping("/forgot-password")
    fun forgotPassword(
        @RequestBody request: ForgotPasswordRequest
    ): ResponseEntity<Void> {
        // TODO: Implementar con Supabase Auth /auth/v1/otp?email=...
        // Por ahora devolvemos 200 siempre para no revelar si el email existe
        return ResponseEntity.ok().build()
    }

    @PostMapping("/reset-password")
    fun resetPassword(
        @RequestBody request: ResetPasswordRequest
    ): ResponseEntity<Void> {
        // TODO: Implementar con Supabase Auth /auth/v1/otp?token=...&type=recovery
        // Por ahora devolvemos 200 siempre para no revelar si el token es válido
        return ResponseEntity.ok().build()
    }

    @PostMapping("/refresh")
    fun refresh(): ResponseEntity<Void> {
        // TODO: Implementar con Supabase Auth /auth/v1/token?grant_type=refresh_token
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build()
    }
}


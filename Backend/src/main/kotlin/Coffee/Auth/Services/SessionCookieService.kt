package Coffee.Auth.Services

import Coffee.Configuration.SessionProperties
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.ResponseCookie
import org.springframework.stereotype.Service
import java.time.Duration

/**
 * Servicio para gestionar cookies de sesión.
 * 
 * Centraliza la creación y eliminación de la cookie HttpOnly que contiene
 * el token de sesión del lado del servidor.
 */
@Service
class SessionCookieService(
    private val sessionProperties: SessionProperties
) {

    fun addSessionCookie(response: HttpServletResponse, token: String) {
        val cookie = ResponseCookie.from(sessionProperties.cookieName, token)
            .httpOnly(true)
            .secure(sessionProperties.secureCookie)
            .sameSite(sessionProperties.sameSite)
            .path("/")
            .maxAge(sessionProperties.idleTimeout)
            .build()

        response.addHeader("Set-Cookie", cookie.toString())
    }

    fun clearSessionCookie(response: HttpServletResponse) {
        val cookie = ResponseCookie.from(sessionProperties.cookieName, "")
            .httpOnly(true)
            .secure(sessionProperties.secureCookie)
            .sameSite(sessionProperties.sameSite)
            .path("/")
            .maxAge(Duration.ZERO)
            .build()

        response.addHeader("Set-Cookie", cookie.toString())
    }

    fun getCookieName(): String = sessionProperties.cookieName
}

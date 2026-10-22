package Coffee.Auth.Security

import Coffee.Auth.Model.UserRole
import jakarta.servlet.http.HttpServletRequest

object SecurityRules {

    private val PUBLIC_READ_PATHS = listOf(
        "/api/v1/catalog",
        "/api/v1/categories"
    )

    private val PUBLIC_AUTH_PATHS = listOf(
        "/api/v1/auth"
    )

    private val WRITE_METHODS = setOf("POST", "PUT", "PATCH", "DELETE")

    fun isPublic(request: HttpServletRequest): Boolean {
        val path = request.requestURI

        if (path == "/actuator/health" || path.startsWith("/actuator/health/")) return true
        if (PUBLIC_AUTH_PATHS.any { path == it || path.startsWith("$it/") }) return true
        if (request.method != "GET") return false

        return PUBLIC_READ_PATHS.any { path == it || path.startsWith("$it/") }
    }

    /** A write needs the admin role. Reading needs any session. */
    fun requiresAdmin(request: HttpServletRequest): Boolean =
        request.method in WRITE_METHODS

    fun isAllowed(role: UserRole?, request: HttpServletRequest): Boolean = when {
        role == null -> false
        requiresAdmin(request) -> role == UserRole.ADMIN
        else -> true
    }
}

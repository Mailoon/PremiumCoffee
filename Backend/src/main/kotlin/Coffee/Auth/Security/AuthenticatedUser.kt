package Coffee.Auth.Security

import Coffee.Auth.Model.UserRole
import jakarta.servlet.http.HttpServletRequest
import java.util.UUID

data class AuthenticatedUser(
    val userId: UUID,
    val email: String,
    val role: UserRole
) {
    companion object {
        const val ATTRIBUTE = "coffee.auth.currentUser"

        fun read(request: HttpServletRequest): AuthenticatedUser? =
            request.getAttribute(ATTRIBUTE) as? AuthenticatedUser
    }
}

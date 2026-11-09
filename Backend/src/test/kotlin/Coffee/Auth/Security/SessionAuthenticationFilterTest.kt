package Coffee.Auth.Security

import Coffee.Auth.Model.AppSession
import Coffee.Auth.Model.Profile
import Coffee.Auth.Model.UserRole
import Coffee.Auth.Services.SessionService
import Coffee.Configuration.SessionProperties
import jakarta.servlet.http.Cookie
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.springframework.mock.web.MockFilterChain
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import tools.jackson.databind.ObjectMapper
import java.time.Instant
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertNull

class SessionAuthenticationFilterTest {

    private val sessionService = mock<SessionService>()
    private val properties = SessionProperties()
    private val filter = SessionAuthenticationFilter(sessionService, properties, ObjectMapper())

    @Test
    fun `a valid session cookie publishes the user`() {
        whenever(sessionService.resolve("a-token")).thenReturn(sessionFor(UserRole.ADMIN))

        val request = requestWithCookie("a-token")
        filter.doFilter(request, MockHttpServletResponse(), MockFilterChain())

        val user = AuthenticatedUser.read(request)
        assertEquals(UserRole.ADMIN, user?.role)
        assertEquals("user@example.com", user?.email)
    }

    @Test
    fun `a request without cookie stays anonymous`() {
        val request = MockHttpServletRequest("GET", "/api/v1/products")
        filter.doFilter(request, MockHttpServletResponse(), MockFilterChain())

        assertNull(AuthenticatedUser.read(request))
    }

    @Test
    fun `a session that does not resolve stays anonymous`() {
        whenever(sessionService.resolve("stale")).thenReturn(null)

        val request = requestWithCookie("stale")
        filter.doFilter(request, MockHttpServletResponse(), MockFilterChain())

        assertNull(AuthenticatedUser.read(request))
    }

    @Test
    fun `a protected resource without a session is unauthorized`() {
        val response = MockHttpServletResponse()

        filter.doFilter(
            MockHttpServletRequest("GET", "/api/v1/products"),
            response,
            MockFilterChain()
        )

        assertEquals(401, response.status)
    }

    @Test
    fun `a write with a client session is forbidden`() {
        whenever(sessionService.resolve("a-token")).thenReturn(sessionFor(UserRole.CLIENTE))
        val response = MockHttpServletResponse()

        filter.doFilter(
            requestWithCookie("a-token", "POST", "/api/v1/categories"),
            response,
            MockFilterChain()
        )

        assertEquals(403, response.status)
    }

    @Test
    fun `a write with an admin session is allowed`() {
        whenever(sessionService.resolve("a-token")).thenReturn(sessionFor(UserRole.ADMIN))
        val response = MockHttpServletResponse()

        filter.doFilter(
            requestWithCookie("a-token", "POST", "/api/v1/categories"),
            response,
            MockFilterChain()
        )

        assertEquals(200, response.status)
    }

    @Test
    fun `the catalog is public`() {
        val response = MockHttpServletResponse()

        filter.doFilter(
            MockHttpServletRequest("GET", "/api/v1/catalog"),
            response,
            MockFilterChain()
        )

        assertEquals(200, response.status)
    }

    private fun requestWithCookie(
        token: String,
        method: String = "GET",
        uri: String = "/api/v1/products"
    ): MockHttpServletRequest {
        val request = MockHttpServletRequest(method, uri)
        request.setCookies(*arrayOf(Cookie(properties.cookieName, token)))
        return request
    }

    private fun sessionFor(role: UserRole): AppSession = AppSession(
        tokenHash = "hash",
        profile = Profile(
            userId = UUID.randomUUID(),
            email = "user@example.com",
            role = role
        ),
        expiresAt = Instant.now().plusSeconds(3600)
    )
}

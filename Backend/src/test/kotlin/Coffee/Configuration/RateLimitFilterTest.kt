package Coffee.Configuration

import org.junit.jupiter.api.Test
import org.springframework.mock.web.MockFilterChain
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import tools.jackson.databind.ObjectMapper
import java.time.Duration
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RateLimitFilterTest {

    private val objectMapper = ObjectMapper()

    private fun filter(
        enabled: Boolean = true,
        trustForwardedFor: Boolean = true,
        authAttempts: Int = 5,
        authWindow: Duration = Duration.ofMinutes(1),
        writeAttempts: Int = 30
    ) = RateLimitFilter(
        RateLimitProperties(
            enabled = enabled,
            trustForwardedFor = trustForwardedFor,
            authAttempts = authAttempts,
            authWindow = authWindow,
            writeAttempts = writeAttempts
        ),
        objectMapper
    )

    private fun request(
        method: String = "POST",
        uri: String = "/api/v1/auth/login",
        ip: String = "203.0.113.10",
        forwardedFor: String? = null
    ): MockHttpServletRequest {
        val request = MockHttpServletRequest(method, uri)
        request.remoteAddr = ip
        forwardedFor?.let { request.addHeader("X-Forwarded-For", it) }
        return request
    }

    @Test
    fun `allows the requests under the limit`() {
        val filter = filter(authAttempts = 3)

        repeat(3) { index ->
            val response = MockHttpServletResponse()
            filter.doFilter(request(), response, MockFilterChain())
            assertEquals(200, response.status, "request $index should pass")
        }
    }

    @Test
    fun `rejects the request over the limit with 429 and a problem detail`() {
        val filter = filter(authAttempts = 2)

        repeat(2) {
            filter.doFilter(request(), MockHttpServletResponse(), MockFilterChain())
        }

        val response = MockHttpServletResponse()
        filter.doFilter(request(), response, MockFilterChain())

        assertEquals(429, response.status)
        // El contenedor anade el charset al content type al usar el writer.
        assertTrue(response.contentType!!.startsWith("application/problem+json"))
        assertEquals("60", response.getHeader("Retry-After"))

        val body = response.contentAsString
        assertContains(body, "\"status\":429")
        assertContains(body, "Too Many Requests")
    }

    @Test
    fun `gives every client its own budget`() {
        val filter = filter(authAttempts = 1)

        filter.doFilter(request(ip = "203.0.113.1"), MockHttpServletResponse(), MockFilterChain())

        val response = MockHttpServletResponse()
        filter.doFilter(request(ip = "203.0.113.2"), response, MockFilterChain())

        assertEquals(200, response.status)
    }

    @Test
    fun `does not limit GET requests`() {
        val filter = filter(authAttempts = 1)

        repeat(20) {
            val response = MockHttpServletResponse()
            filter.doFilter(request(method = "GET", uri = "/api/v1/catalog"), response, MockFilterChain())
            assertEquals(200, response.status)
        }
    }

    @Test
    fun `applies the tighter limit to the authentication paths only`() {
        val filter = filter(authAttempts = 1, writeAttempts = 5)

        val login = MockHttpServletResponse()
        filter.doFilter(request(uri = "/api/v1/auth/login"), login, MockFilterChain())
        filter.doFilter(request(uri = "/api/v1/auth/login"), login, MockFilterChain())
        assertEquals(429, login.status)

        val product = MockHttpServletResponse()
        filter.doFilter(request(uri = "/api/v1/products"), product, MockFilterChain())
        filter.doFilter(request(uri = "/api/v1/products"), product, MockFilterChain())
        assertEquals(200, product.status)
    }

    @Test
    fun `does not limit anything when disabled`() {
        val filter = filter(enabled = true, authAttempts = 1)
        val disabled = filter(enabled = false, authAttempts = 1)

        val response = MockHttpServletResponse()
        repeat(5) {
            disabled.doFilter(request(), response, MockFilterChain())
        }

        assertEquals(200, response.status)
        assertTrue(response.contentAsString.isEmpty())
    }

    @Test
    fun `reads the client ip from the forwarded header`() {
        val filter = filter(authAttempts = 1, trustForwardedFor = true)

        // Misma IP reenviada, distinto socket: si el filtro se apoya en la
        // cabecera, las dos peticiones comparten cubo.
        filter.doFilter(
            request(ip = "10.0.0.1", forwardedFor = "198.51.100.7"),
            MockHttpServletResponse(),
            MockFilterChain()
        )

        val response = MockHttpServletResponse()
        filter.doFilter(
            request(ip = "10.0.0.2", forwardedFor = "198.51.100.7"),
            response,
            MockFilterChain()
        )

        assertEquals(429, response.status)
    }

    @Test
    fun `ignores the forwarded header when it is not trusted`() {
        val filter = filter(authAttempts = 1, trustForwardedFor = false)

        filter.doFilter(
            request(ip = "10.0.0.1", forwardedFor = "198.51.100.7"),
            MockHttpServletResponse(),
            MockFilterChain()
        )

        // Cada socket tiene su propio cubo: la cabecera se ignora.
        val response = MockHttpServletResponse()
        filter.doFilter(
            request(ip = "10.0.0.2", forwardedFor = "198.51.100.7"),
            response,
            MockFilterChain()
        )

        assertEquals(200, response.status)
    }
}

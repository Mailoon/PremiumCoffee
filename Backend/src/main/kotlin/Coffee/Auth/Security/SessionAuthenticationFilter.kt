package Coffee.Auth.Security

import Coffee.Auth.Services.SessionService
import Coffee.Configuration.SessionProperties
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ProblemDetail
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter
import tools.jackson.databind.ObjectMapper

@Component
class SessionAuthenticationFilter(
    private val sessionService: SessionService,
    private val properties: SessionProperties,
    private val objectMapper: ObjectMapper
) : OncePerRequestFilter() {

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        val user = resolveUser(request)

        if (user != null) {
            request.setAttribute(AuthenticatedUser.ATTRIBUTE, user)
        }

        if (SecurityRules.isPublic(request)) {
            filterChain.doFilter(request, response)
            return
        }

        if (user == null) {
            writeProblem(response, HttpStatus.UNAUTHORIZED, "Authentication required")
            return
        }

        if (!SecurityRules.isAllowed(user.role, request)) {
            writeProblem(response, HttpStatus.FORBIDDEN, "Insufficient permissions")
            return
        }

        filterChain.doFilter(request, response)
    }

    private fun resolveUser(request: HttpServletRequest): AuthenticatedUser? {
        val token = request.cookies
            ?.firstOrNull { it.name == properties.cookieName }
            ?.value
            ?: return null

        if (token.isBlank()) return null

        val session = sessionService.resolve(token) ?: return null
        val profile = session.profile

        return AuthenticatedUser(
            userId = profile.userId,
            email = profile.email,
            role = profile.role
        )
    }

    private fun writeProblem(response: HttpServletResponse, status: HttpStatus, detail: String) {
        val problem = ProblemDetail.forStatusAndDetail(status, detail)
        problem.title = status.reasonPhrase

        response.status = status.value()
        response.characterEncoding = "UTF-8"
        response.contentType = MediaType.APPLICATION_PROBLEM_JSON_VALUE
        response.writer.write(objectMapper.writeValueAsString(problem))
        response.writer.flush()
    }
}

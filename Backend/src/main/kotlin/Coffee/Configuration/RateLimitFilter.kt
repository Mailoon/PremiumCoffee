package Coffee.Configuration

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ProblemDetail
import org.springframework.web.filter.OncePerRequestFilter
import tools.jackson.databind.ObjectMapper
import java.time.Duration
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean

class RateLimitFilter(
    private val properties: RateLimitProperties,
    private val objectMapper: ObjectMapper
) : OncePerRequestFilter() {

    private val buckets = ConcurrentHashMap<String, Bucket>()

    override fun shouldNotFilter(request: HttpServletRequest): Boolean =
        !properties.enabled || ruleFor(request) == null

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        val rule = ruleFor(request) ?: run {
            filterChain.doFilter(request, response)
            return
        }

        val key = "${clientIp(request)}|${rule.name}"

        if (!tryConsume(key, rule.limit, rule.window)) {
            writeTooManyRequests(response, rule.window)
            return
        }

        filterChain.doFilter(request, response)
    }

    private fun ruleFor(request: HttpServletRequest): Rule? {
        if (request.requestURI in properties.authPaths) {
            return Rule("auth", properties.authAttempts, properties.authWindow)
        }
        if (request.method in WRITE_METHODS) {
            return Rule("write", properties.writeAttempts, properties.writeWindow)
        }
        return null
    }

    private fun clientIp(request: HttpServletRequest): String {
        if (properties.trustForwardedFor) {
            val forwarded = request.getHeader("X-Forwarded-For")
            val first = forwarded?.substringBefore(',')?.trim()
            if (!first.isNullOrEmpty()) return first
        }
        return request.remoteAddr ?: UNKNOWN_CLIENT
    }

    private fun tryConsume(key: String, limit: Int, window: Duration): Boolean {
        val now = System.currentTimeMillis()
        val windowMs = window.toMillis()
        val allowed = AtomicBoolean(false)

        buckets.compute(key) { _, existing ->
            when {
                existing == null || now - existing.windowStart >= windowMs -> {
                    allowed.set(limit > 0)
                    Bucket(tokens = maxOf(0, limit - 1), windowStart = now)
                }

                existing.tokens > 0 -> {
                    allowed.set(true)
                    existing.copy(tokens = existing.tokens - 1)
                }

                else -> {
                    allowed.set(false)
                    existing
                }
            }
        }

        if (buckets.size > properties.maxTrackedKeys) {
            evictExpired(now, windowMs)
        }

        return allowed.get()
    }

    private fun evictExpired(now: Long, windowMs: Long) {
        val cutoff = now - windowMs
        buckets.entries.removeIf { it.value.windowStart < cutoff }
    }

    private fun writeTooManyRequests(response: HttpServletResponse, window: Duration) {
        val retryAfter = window.seconds.coerceAtLeast(1)
        val problem = ProblemDetail.forStatusAndDetail(
            HttpStatus.TOO_MANY_REQUESTS,
            "Too many requests. Try again in $retryAfter seconds."
        )
        problem.title = "Too Many Requests"

        response.status = HttpStatus.TOO_MANY_REQUESTS.value()
        response.setHeader("Retry-After", retryAfter.toString())
        response.characterEncoding = "UTF-8"
        response.contentType = MediaType.APPLICATION_PROBLEM_JSON_VALUE
        response.writer.write(objectMapper.writeValueAsString(problem))
        response.writer.flush()
    }

    private data class Rule(val name: String, val limit: Int, val window: Duration)

    private data class Bucket(val tokens: Int, val windowStart: Long)

    private companion object {
        private val WRITE_METHODS = setOf("POST", "PUT", "PATCH", "DELETE")
        private const val UNKNOWN_CLIENT = "unknown"
    }
}

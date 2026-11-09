package Coffee.Configuration

import Coffee.Auth.Security.SessionAuthenticationFilter
import Coffee.Auth.Services.SessionService
import jakarta.servlet.http.HttpServletResponse
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ProblemDetail
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.access.intercept.AuthorizationFilter
import tools.jackson.databind.ObjectMapper

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
class SecurityConfig {

    @Bean
    fun sessionAuthenticationFilter(
        sessionService: SessionService,
        sessionProperties: SessionProperties,
        objectMapper: ObjectMapper
    ): SessionAuthenticationFilter =
        SessionAuthenticationFilter(sessionService, sessionProperties, objectMapper)

    @Bean
    fun securityFilterChain(
        http: HttpSecurity,
        sessionAuthenticationFilter: SessionAuthenticationFilter
    ): SecurityFilterChain {
        http
            .csrf { csrf -> csrf.csrfTokenRepository(csrfTokenRepository()) }

            // Before the authorization filter, and after the CSRF check: a
            // request without a valid CSRF token has to fail as a CSRF
            // problem, not as a permissions problem.
            .addFilterBefore(sessionAuthenticationFilter, AuthorizationFilter::class.java)

            .sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
            .formLogin { it.disable() }
            .httpBasic { it.disable() }
            .logout { it.disable() }
            .authorizeHttpRequests { requests ->
                // The access rules are applied by the filter above. Here the
                // chain must not deny anything on its own, or the filter
                // would never get the chance to answer with 401 or 403.
                requests.anyRequest().permitAll()
            }

        return http.build()
    }

    private fun csrfTokenRepository() =
        org.springframework.security.web.csrf.CookieCsrfTokenRepository().apply {
            setCookieCustomizer { builder ->
                builder.path("/").httpOnly(false).sameSite("Lax")
            }
        }
}

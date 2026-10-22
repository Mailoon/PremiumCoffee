package Coffee.Configuration

import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.web.servlet.FilterRegistrationBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.Ordered
import tools.jackson.databind.ObjectMapper

@Configuration
@EnableConfigurationProperties(RateLimitProperties::class)
class RateLimitConfiguration {

    /**
     * The filter is registered on the API path only, and before the
     * controller dispatcher, so a rejected request never reaches the
     * business logic.
     *
     * It runs before the Spring Security chain (which is at
     * `Ordered.HIGHEST_PRECEDENCE + 100` by default) on purpose: the
     * authentication endpoints are the ones that must be cheap to reject.
     */
    @Bean
    fun rateLimitFilter(
        properties: RateLimitProperties,
        objectMapper: ObjectMapper
    ): FilterRegistrationBean<RateLimitFilter> {
        val registration = FilterRegistrationBean(RateLimitFilter(properties, objectMapper))
        registration.addUrlPatterns("/api/*")
        registration.order = Ordered.HIGHEST_PRECEDENCE + 10
        return registration
    }
}

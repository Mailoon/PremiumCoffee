package Coffee.Configuration

import org.springframework.boot.context.properties.ConfigurationProperties
import java.time.Duration

@ConfigurationProperties(prefix = "auth.rate-limit")
data class RateLimitProperties(
    val enabled: Boolean = true,

    val trustForwardedFor: Boolean = true,

    val authPaths: List<String> = listOf(
        "/api/v1/auth/login",
        "/api/v1/auth/register",
        "/api/v1/auth/forgot-password",
        "/api/v1/auth/reset-password"
    ),

    val authAttempts: Int = 5,
    val authWindow: Duration = Duration.ofMinutes(1),

    val writeAttempts: Int = 30,
    val writeWindow: Duration = Duration.ofMinutes(1),

    val maxTrackedKeys: Int = 10_000
)

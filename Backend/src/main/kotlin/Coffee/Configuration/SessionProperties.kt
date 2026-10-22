package Coffee.Configuration

import org.springframework.boot.context.properties.ConfigurationProperties
import java.time.Duration

@ConfigurationProperties(prefix = "auth.session")
data class SessionProperties(
    val cookieName: String = "pcs_session",

    val idleTimeout: Duration = Duration.ofHours(12),

    val absoluteTimeout: Duration = Duration.ofDays(7),

    val renewInterval: Duration = Duration.ofMinutes(15),

    val secureCookie: Boolean = false,

    val sameSite: String = "Lax"
)

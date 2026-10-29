package Coffee.Auth.Configuration

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "supabase.auth")
data class SupabaseAuthProperties(
    val url: String = "",
    val anonKey: String = ""
)

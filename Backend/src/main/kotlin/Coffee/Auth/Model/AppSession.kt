package Coffee.Auth.Model

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "app_sessions")
class AppSession(
    @Id
    val id: UUID = UUID.randomUUID(),

    @Column(name = "token_hash", nullable = false, unique = true, length = 64, updatable = false)
    val tokenHash: String,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "profile_id", nullable = false, updatable = false)
    val profile: Profile,

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: Instant = Instant.now(),

    @Column(name = "last_seen_at", nullable = false)
    var lastSeenAt: Instant = Instant.now(),

    /** Absolute cap: the session dies here no matter how much it is used. */
    @Column(name = "expires_at", nullable = false)
    var expiresAt: Instant,

    @Column(name = "revoked_at")
    var revokedAt: Instant? = null,

    @Column(name = "user_agent", length = 400)
    var userAgent: String? = null,

    @Column(name = "ip_address", length = 64)
    var ipAddress: String? = null
) {
    fun isRevoked(): Boolean = revokedAt != null

    fun isExpired(now: Instant): Boolean = expiresAt.isBefore(now)
}

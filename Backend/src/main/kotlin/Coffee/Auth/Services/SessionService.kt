package Coffee.Auth.Services

import Coffee.Auth.Model.AppSession
import Coffee.Auth.Model.Profile
import Coffee.Auth.Repository.AppSessionRepository
import Coffee.Configuration.SessionProperties
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.security.MessageDigest
import java.security.SecureRandom
import java.time.Instant
import java.util.Base64
import java.util.UUID

@Service
class SessionService(
    private val sessionRepository: AppSessionRepository,
    private val properties: SessionProperties
) {

    /** Creates a session and returns the raw token, which goes in the cookie. */
    @Transactional
    fun create(
        profile: Profile,
        userAgent: String? = null,
        ipAddress: String? = null
    ): String {
        val token = generateToken()
        val now = Instant.now()

        sessionRepository.save(
            AppSession(
                tokenHash = hash(token),
                profile = profile,
                createdAt = now,
                lastSeenAt = now,
                expiresAt = now.plus(properties.absoluteTimeout),
                userAgent = userAgent?.take(MAX_USER_AGENT),
                ipAddress = ipAddress?.take(MAX_IP_ADDRESS)
            )
        )

        return token
    }

    /**
     * Resolves a token into its session, or null when it is not usable.
     *
     * A session stops working when it is revoked, when it passes the absolute
     * cap, or when it sits unused for longer than the idle timeout.
     */
    @Transactional
    fun resolve(token: String): AppSession? {
        val now = Instant.now()
        val session = sessionRepository.findActiveByTokenHash(hash(token), now) ?: return null

        if (session.lastSeenAt.plus(properties.idleTimeout).isBefore(now)) {
            session.revokedAt = now
            return null
        }

        if (session.lastSeenAt.plus(properties.renewInterval).isBefore(now)) {
            session.lastSeenAt = now
        }

        return session
    }

    /** Invalidates the session on the server. Clearing the cookie is not enough. */
    @Transactional
    fun revoke(session: AppSession) {
        if (!session.isRevoked()) {
            session.revokedAt = Instant.now()
        }
    }

    /** Invalidates every session of a user, e.g. when the account is suspended. */
    @Transactional
    fun revokeAll(profileId: UUID) {
        val now = Instant.now()
        sessionRepository.findActiveByProfileId(profileId).forEach { session ->
            session.revokedAt = now
        }
    }

    private fun generateToken(): String {
        val bytes = ByteArray(TOKEN_BYTES)
        secureRandom.nextBytes(bytes)
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
    }

    private fun hash(token: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(token.toByteArray(Charsets.UTF_8))
        return digest.joinToString("") { "%02x".format(it) }
    }1

    private companion object {
        private val secureRandom = SecureRandom()
        private const val TOKEN_BYTES = 32
        private const val MAX_USER_AGENT = 400
        private const val MAX_IP_ADDRESS = 64
    }
}

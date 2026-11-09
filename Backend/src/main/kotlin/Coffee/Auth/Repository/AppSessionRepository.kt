package Coffee.Auth.Repository

import Coffee.Auth.Model.AppSession
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.Instant
import java.util.UUID

interface AppSessionRepository : JpaRepository<AppSession, UUID> {

    @Query(
        """
        select s from AppSession s
        join fetch s.profile
        where s.tokenHash = :tokenHash
          and s.revokedAt is null
          and s.expiresAt > :now
        """
    )
    fun findActiveByTokenHash(
        @Param("tokenHash") tokenHash: String,
        @Param("now") now: Instant
    ): AppSession?

    fun deleteByProfileId(profileId: UUID)

    /** Live sessions of a user, used to invalidate them all at once. */
    @Query(
        """
        select s from AppSession s
        where s.profile.id = :profileId
          and s.revokedAt is null
        """
    )
    fun findActiveByProfileId(@Param("profileId") profileId: UUID): List<AppSession>
}

package Coffee.Auth.Repository

import Coffee.Auth.Model.Profile
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface ProfileRepository : JpaRepository<Profile, UUID> {

    fun findByUserId(userId: UUID): Profile?
}

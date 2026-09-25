package Coffee.Category.DTO

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import java.util.UUID

data class CategoryRequest(
    @field:NotBlank
    @field:Size(max = 100)
    val name: String,

    @field:NotBlank
    @field:Size(max = 120)
    val slug: String,

    val parentId: UUID? = null,
    val active: Boolean = true
)

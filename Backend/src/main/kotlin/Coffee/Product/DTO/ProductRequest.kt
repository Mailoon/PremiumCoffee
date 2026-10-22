package Coffee.Product.DTO

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size
import java.util.UUID

data class ProductRequest(
    @field:NotNull
    val categoryId: UUID,

    @field:NotBlank
    @field:Size(max = 150)
    val name: String,

    @field:NotBlank
    @field:Size(max = 180)
    val slug: String,

    val description: String? = null,
    val active: Boolean = true
)

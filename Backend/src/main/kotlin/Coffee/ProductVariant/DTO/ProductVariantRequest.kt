package Coffee.ProductVariant.DTO

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Positive
import jakarta.validation.constraints.Size
import java.util.UUID

data class ProductVariantRequest(
    @field:NotNull
    val productId: UUID,

    @field:NotBlank
    @field:Size(max = 150)
    val name: String,

    @field:NotBlank
    @field:Size(max = 100)
    val sku: String,

    @field:Positive
    val volumeMl: Int? = null,

    val active: Boolean = true
)

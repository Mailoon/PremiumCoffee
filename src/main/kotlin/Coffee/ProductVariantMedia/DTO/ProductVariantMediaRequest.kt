package Coffee.ProductVariantMedia.DTO

import Coffee.ProductVariantMedia.Model.VariantMediaRole
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.PositiveOrZero
import java.util.UUID

data class ProductVariantMediaRequest(
    @field:NotNull
    val variantId: UUID,

    @field:NotNull
    val mediaAssetId: UUID,

    @field:NotNull
    val role: VariantMediaRole,

    @field:PositiveOrZero
    val sortOrder: Int = 0,

    val isPrimary: Boolean = false
)

package Coffee.Variant3DComponent.DTO

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size
import java.util.UUID

data class Variant3DComponentRequest(
    @field:NotNull
    val variantId: UUID,

    @field:NotBlank
    @field:Size(max = 100)
    val name: String,

    @field:NotBlank
    @field:Size(max = 100)
    val code: String,

    @field:NotNull
    val mediaAssetId: UUID,

    val required: Boolean = false,
    val defaultEnabled: Boolean = true
)

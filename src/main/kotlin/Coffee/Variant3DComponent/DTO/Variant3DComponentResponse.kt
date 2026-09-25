package Coffee.Variant3DComponent.DTO

import Coffee.Variant3DComponent.Model.Variant3DComponent
import java.time.Instant
import java.util.UUID

data class Variant3DComponentResponse(
    val id: UUID,
    val variantId: UUID,
    val name: String,
    val code: String,
    val mediaAssetId: UUID,
    val required: Boolean,
    val defaultEnabled: Boolean,
    val createdAt: Instant
) {
    companion object {
        fun from(component: Variant3DComponent) = Variant3DComponentResponse(
            id = component.id,
            variantId = component.variant.id,
            name = component.name,
            code = component.code,
            mediaAssetId = component.mediaAsset.id,
            required = component.required,
            defaultEnabled = component.defaultEnabled,
            createdAt = component.createdAt
        )
    }
}

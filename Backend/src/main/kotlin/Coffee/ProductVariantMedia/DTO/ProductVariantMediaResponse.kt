package Coffee.ProductVariantMedia.DTO

import Coffee.ProductVariantMedia.Model.ProductVariantMedia
import Coffee.ProductVariantMedia.Model.VariantMediaRole
import com.fasterxml.jackson.annotation.JsonProperty
import java.time.Instant
import java.util.UUID

data class ProductVariantMediaResponse(
    val id: UUID,
    val variantId: UUID,
    val mediaAssetId: UUID,
    val role: VariantMediaRole,
    val sortOrder: Int,
    @get:JsonProperty("isPrimary")
    val isPrimary: Boolean,
    val createdAt: Instant
) {
    companion object {
        fun from(media: ProductVariantMedia) = ProductVariantMediaResponse(
            id = media.id,
            variantId = media.variant.id,
            mediaAssetId = media.mediaAsset.id,
            role = media.role,
            sortOrder = media.sortOrder,
            isPrimary = media.isPrimary,
            createdAt = media.createdAt
        )
    }
}

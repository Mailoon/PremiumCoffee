package Coffee.ProductVariant.DTO

import Coffee.ProductVariant.Model.ProductVariant
import java.time.Instant
import java.util.UUID

data class ProductVariantResponse(
    val id: UUID,
    val productId: UUID,
    val name: String,
    val sku: String,
    val volumeMl: Int?,
    val active: Boolean,
    val createdAt: Instant,
    val updatedAt: Instant
) {
    companion object {
        fun from(variant: ProductVariant) = ProductVariantResponse(
            id = variant.id,
            productId = variant.product.id,
            name = variant.name,
            sku = variant.sku,
            volumeMl = variant.volumeMl,
            active = variant.active,
            createdAt = variant.createdAt,
            updatedAt = variant.updatedAt
        )
    }
}

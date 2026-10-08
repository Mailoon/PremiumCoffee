package Coffee.Product.DTO

import Coffee.Product.Model.Product
import java.time.Instant
import java.util.UUID

data class ProductResponse(
    val id: UUID,
    val categoryId: UUID,
    val name: String,
    val slug: String,
    val description: String?,
    val active: Boolean,
    val createdAt: Instant,
    val updatedAt: Instant
) {
    companion object {
        fun from(product: Product) = ProductResponse(
            id = product.id,
            categoryId = product.category.id,
            name = product.name,
            slug = product.slug,
            description = product.description,
            active = product.active,
            createdAt = product.createdAt,
            updatedAt = product.updatedAt
        )
    }
}

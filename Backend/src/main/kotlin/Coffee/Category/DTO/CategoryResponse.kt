package Coffee.Category.DTO

import Coffee.Category.Model.Category
import java.time.Instant
import java.util.UUID

data class CategoryResponse(
    val id: UUID,
    val parentId: UUID?,
    val name: String,
    val slug: String,
    val active: Boolean,
    val createdAt: Instant,
    val updatedAt: Instant
) {
    companion object {
        fun from(category: Category) = CategoryResponse(
            id = category.id,
            parentId = category.parent?.id,
            name = category.name,
            slug = category.slug,
            active = category.active,
            createdAt = category.createdAt,
            updatedAt = category.updatedAt
        )
    }
}

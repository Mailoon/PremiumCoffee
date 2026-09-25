package Coffee.Category.Services

import Coffee.Category.DTO.CategoryRequest
import Coffee.Category.Model.Category
import Coffee.Category.Repository.CategoryRepository
import Coffee.Common.Services.AbstractCrudService
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.web.server.ResponseStatusException
import java.util.UUID

@Service
class CategoryService(
    private val repository: CategoryRepository
) : AbstractCrudService<Category, UUID, CategoryRequest>(repository) {

    override val resourceName: String = "Category"

    override fun buildEntity(request: CategoryRequest): Category =
        Category(
            name = request.name,
            slug = request.slug,
            parent = request.parentId?.let { findParent(it) },
            active = request.active
        )

    override fun applyUpdate(entity: Category, request: CategoryRequest): Category {
        entity.name = request.name
        entity.slug = request.slug
        entity.active = request.active
        entity.parent = request.parentId?.let { findParent(it) }
        return entity
    }

    private fun findParent(id: UUID): Category =
        repository.findById(id).orElseThrow {
            ResponseStatusException(HttpStatus.BAD_REQUEST, "Parent category $id does not exist")
        }
}

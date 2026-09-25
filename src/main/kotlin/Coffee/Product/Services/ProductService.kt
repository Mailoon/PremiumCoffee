package Coffee.Product.Services

import Coffee.Category.Model.Category
import Coffee.Category.Repository.CategoryRepository
import Coffee.Common.Services.AbstractCrudService
import Coffee.Product.DTO.ProductRequest
import Coffee.Product.Model.Product
import Coffee.Product.Repository.ProductRepository
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.web.server.ResponseStatusException
import java.util.UUID

@Service
class ProductService(
    private val repository: ProductRepository,
    private val categoryRepository: CategoryRepository
) : AbstractCrudService<Product, UUID, ProductRequest>(repository) {

    override val resourceName: String = "Product"

    override fun buildEntity(request: ProductRequest): Product =
        Product(
            category = findByCategory(request.categoryId),
            name = request.name,
            slug = request.slug,
            description = request.description,
            active = request.active
        )

    override fun applyUpdate(entity: Product, request: ProductRequest): Product {
        entity.category = findByCategory(request.categoryId)
        entity.name = request.name
        entity.slug = request.slug
        entity.description = request.description
        entity.active = request.active
        return entity
    }

    private fun findByCategory(id: UUID): Category =
        categoryRepository.findById(id).orElseThrow {
            ResponseStatusException(HttpStatus.BAD_REQUEST, "Category $id does not exist")
        }
}


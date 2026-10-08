package Coffee.ProductVariant.Services

import Coffee.Common.Services.AbstractCrudService
import Coffee.Product.Model.Product
import Coffee.Product.Repository.ProductRepository
import Coffee.ProductVariant.DTO.ProductVariantRequest
import Coffee.ProductVariant.Model.ProductVariant
import Coffee.ProductVariant.Repository.ProductVariantRepository
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.web.server.ResponseStatusException
import java.util.UUID

@Service
class ProductVariantService(
    repository: ProductVariantRepository,
    private val productRepository: ProductRepository
) : AbstractCrudService<ProductVariant, UUID, ProductVariantRequest>(repository) {

    override val resourceName: String = "ProductVariant"

    override fun buildEntity(request: ProductVariantRequest): ProductVariant =
        ProductVariant(
            product = findByProduct(request.productId),
            name = request.name,
            sku = request.sku,
            volumeMl = request.volumeMl,
            active = request.active
        )

    override fun applyUpdate(entity: ProductVariant, request: ProductVariantRequest): ProductVariant {
        entity.product = findByProduct(request.productId)
        entity.name = request.name
        entity.sku = request.sku
        entity.volumeMl = request.volumeMl
        entity.active = request.active
        return entity
    }

    private fun findByProduct(id: UUID): Product =
        productRepository.findById(id).orElseThrow {
            ResponseStatusException(HttpStatus.BAD_REQUEST, "Product $id does not exist")
        }
}


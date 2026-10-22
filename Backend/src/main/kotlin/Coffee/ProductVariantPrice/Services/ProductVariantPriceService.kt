package Coffee.ProductVariantPrice.Services

import Coffee.Common.Services.AbstractCrudService
import Coffee.ProductVariant.Model.ProductVariant
import Coffee.ProductVariant.Repository.ProductVariantRepository
import Coffee.ProductVariantPrice.DTO.ProductVariantPriceRequest
import Coffee.ProductVariantPrice.Model.ProductVariantPrice
import Coffee.ProductVariantPrice.Repository.ProductVariantPriceRepository
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.web.server.ResponseStatusException
import java.util.UUID

@Service
class ProductVariantPriceService(
    repository: ProductVariantPriceRepository,
    private val variantRepository: ProductVariantRepository
) : AbstractCrudService<ProductVariantPrice, UUID, ProductVariantPriceRequest>(repository) {

    override val resourceName: String = "ProductVariantPrice"

    override fun buildEntity(request: ProductVariantPriceRequest): ProductVariantPrice =
        ProductVariantPrice(
            variant = findByVariant(request.variantId),
            price = request.price,
            currency = request.currency,
            validFrom = request.validFrom,
            validUntil = request.validUntil
        )

    override fun applyUpdate(entity: ProductVariantPrice, request: ProductVariantPriceRequest): ProductVariantPrice {
        entity.variant = findByVariant(request.variantId)
        entity.price = request.price
        entity.currency = request.currency
        entity.validFrom = request.validFrom
        entity.validUntil = request.validUntil
        return entity
    }

    private fun findByVariant(id: UUID): ProductVariant =
        variantRepository.findById(id).orElseThrow {
            ResponseStatusException(HttpStatus.BAD_REQUEST, "Variant $id does not exist")
        }
}

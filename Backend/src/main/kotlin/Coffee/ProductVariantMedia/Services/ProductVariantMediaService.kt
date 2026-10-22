package Coffee.ProductVariantMedia.Services

import Coffee.Common.Services.AbstractCrudService
import Coffee.MediaAsset.Model.MediaAsset
import Coffee.MediaAsset.Repository.MediaAssetRepository
import Coffee.ProductVariant.Model.ProductVariant
import Coffee.ProductVariant.Repository.ProductVariantRepository
import Coffee.ProductVariantMedia.DTO.ProductVariantMediaRequest
import Coffee.ProductVariantMedia.Model.ProductVariantMedia
import Coffee.ProductVariantMedia.Repository.ProductVariantMediaRepository
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.web.server.ResponseStatusException
import java.util.UUID

@Service
class ProductVariantMediaService(
    repository: ProductVariantMediaRepository,
    private val variantRepository: ProductVariantRepository,
    private val mediaAssetRepository: MediaAssetRepository
) : AbstractCrudService<ProductVariantMedia, UUID, ProductVariantMediaRequest>(repository) {

    override val resourceName: String = "ProductVariantMedia"

    override fun buildEntity(request: ProductVariantMediaRequest): ProductVariantMedia =
        ProductVariantMedia(
            variant = findByVariant(request.variantId),
            mediaAsset = findByMediaAsset(request.mediaAssetId),
            role = request.role,
            sortOrder = request.sortOrder,
            isPrimary = request.isPrimary
        )

    override fun applyUpdate(entity: ProductVariantMedia, request: ProductVariantMediaRequest): ProductVariantMedia {
        entity.variant = findByVariant(request.variantId)
        entity.mediaAsset = findByMediaAsset(request.mediaAssetId)
        entity.role = request.role
        entity.sortOrder = request.sortOrder
        entity.isPrimary = request.isPrimary
        return entity
    }

    private fun findByVariant(id: UUID): ProductVariant =
        variantRepository.findById(id).orElseThrow {
            ResponseStatusException(HttpStatus.BAD_REQUEST, "Variant $id does not exist")
        }

    private fun findByMediaAsset(id: UUID): MediaAsset =
        mediaAssetRepository.findById(id).orElseThrow {
            ResponseStatusException(HttpStatus.BAD_REQUEST, "Media asset $id does not exist")
        }
}

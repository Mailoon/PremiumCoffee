package Coffee.Variant3DComponent.Services

import Coffee.Common.Services.AbstractCrudService
import Coffee.MediaAsset.Model.MediaAsset
import Coffee.MediaAsset.Repository.MediaAssetRepository
import Coffee.ProductVariant.Model.ProductVariant
import Coffee.ProductVariant.Repository.ProductVariantRepository
import Coffee.Variant3DComponent.DTO.Variant3DComponentRequest
import Coffee.Variant3DComponent.Model.Variant3DComponent
import Coffee.Variant3DComponent.Repository.Variant3DComponentRepository
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.web.server.ResponseStatusException
import java.util.UUID

@Service
class Variant3DComponentService(
    private val repository: Variant3DComponentRepository,
    private val variantRepository: ProductVariantRepository,
    private val mediaAssetRepository: MediaAssetRepository
) : AbstractCrudService<Variant3DComponent, UUID, Variant3DComponentRequest>(repository) {

    override val resourceName: String = "Variant3DComponent"

    override fun buildEntity(request: Variant3DComponentRequest): Variant3DComponent =
        Variant3DComponent(
            variant = findByVariant(request.variantId),
            name = request.name,
            code = request.code,
            mediaAsset = findByMediaAsset(request.mediaAssetId),
            required = request.required,
            defaultEnabled = request.defaultEnabled
        )

    override fun applyUpdate(entity: Variant3DComponent, request: Variant3DComponentRequest): Variant3DComponent {
        entity.variant = findByVariant(request.variantId)
        entity.name = request.name
        entity.code = request.code
        entity.mediaAsset = findByMediaAsset(request.mediaAssetId)
        entity.required = request.required
        entity.defaultEnabled = request.defaultEnabled
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

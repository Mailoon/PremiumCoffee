package Coffee.MediaAsset.Services

import Coffee.Common.Services.AbstractCrudService
import Coffee.MediaAsset.DTO.MediaAssetRequest
import Coffee.MediaAsset.Model.MediaAsset
import Coffee.MediaAsset.Repository.MediaAssetRepository
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class MediaAssetService(
    private val repository: MediaAssetRepository
) : AbstractCrudService<MediaAsset, UUID, MediaAssetRequest>(repository) {

    override val resourceName: String = "MediaAsset"

    override fun buildEntity(request: MediaAssetRequest): MediaAsset =
        MediaAsset(
            assetType = request.assetType,
            provider = request.provider,
            storageKey = request.storageKey,
            publicId = request.publicId,
            url = request.url,
            mimeType = request.mimeType,
            fileName = request.fileName,
            fileSizeBytes = request.fileSizeBytes,
            width = request.width,
            height = request.height
        )

    override fun applyUpdate(entity: MediaAsset, request: MediaAssetRequest): MediaAsset {
        entity.assetType = request.assetType
        entity.provider = request.provider
        entity.storageKey = request.storageKey
        entity.publicId = request.publicId
        entity.url = request.url
        entity.mimeType = request.mimeType
        entity.fileName = request.fileName
        entity.fileSizeBytes = request.fileSizeBytes
        entity.width = request.width
        entity.height = request.height
        return entity
    }
}

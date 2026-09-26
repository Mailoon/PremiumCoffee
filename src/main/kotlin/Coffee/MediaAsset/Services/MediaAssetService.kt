package Coffee.MediaAsset.Services

import Coffee.Common.Exceptions.ResourceNotFoundException
import Coffee.Common.Services.AbstractCrudService
import Coffee.MediaAsset.DTO.MediaAssetRequest
import Coffee.MediaAsset.DTO.MediaAssetUrlResponse
import Coffee.MediaAsset.Model.MediaAsset
import Coffee.MediaAsset.Repository.MediaAssetRepository
import Coffee.Tools.Media.Content.DetectedFormat
import Coffee.Tools.Media.Content.MediaContentInspector
import Coffee.Tools.Media.MediaStorage
import Coffee.Tools.Media.MediaUpload
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.multipart.MultipartFile
import org.springframework.web.server.ResponseStatusException
import java.util.UUID

@Service
class MediaAssetService(
    repository: MediaAssetRepository,
    private val mediaStorage: MediaStorage,
    private val inspector: MediaContentInspector = MediaContentInspector()
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

    fun upload(file: MultipartFile): MediaAsset {
        if (file.isEmpty) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "The uploaded file is empty")
        }

        val detected = detectContent(file) ?: throw ResponseStatusException(
            HttpStatus.BAD_REQUEST,
            "The uploaded file is not a supported image or 3D model"
        )

        val fileName = sanitizeFileName(file.originalFilename)
        val stored = mediaStorage.upload(
            MediaUpload(
                fileName = fileName ?: DEFAULT_FILE_NAME,
                contentType = file.contentType,
                content = file.bytes
            )
        )

        return repository.save(
            MediaAsset(
                assetType = detected.assetType,
                provider = mediaStorage.provider,
                storageKey = stored.publicId,
                publicId = stored.publicId,
                url = stored.url,
                mimeType = detected.mimeType,
                fileName = fileName,
                fileSizeBytes = stored.sizeBytes ?: file.size,
                width = stored.width,
                height = stored.height
            )
        )
    }

    private fun detectContent(file: MultipartFile): DetectedFormat? {
        val header = file.inputStream.use { it.readNBytes(MediaContentInspector.HEADER_SIZE) }
        return inspector.inspect(header)
    }

    @Transactional(readOnly = true)
    fun storedUrl(id: UUID): MediaAssetUrlResponse {
        val asset = findById(id)
        val url = asset.url ?: throw ResourceNotFoundException("MediaAssetUrl", id)
        return MediaAssetUrlResponse(asset.publicId ?: asset.storageKey, url)
    }

    @Transactional
    override fun delete(id: UUID) {
        val asset = findById(id)
        repository.delete(asset)
        repository.flush()
        removeRemoteFile(asset)
    }

    private fun removeRemoteFile(asset: MediaAsset) {
        if (asset.provider != mediaStorage.provider) {
            return
        }
        val publicId = asset.publicId ?: return
        mediaStorage.delete(publicId)
    }

    private fun sanitizeFileName(original: String?): String? {
        val name = original
            ?.substringAfterLast('/')
            ?.substringAfterLast('\\')
            ?.filterNot { it.isISOControl() }
            ?.trim()

        if (name.isNullOrBlank()) {
            return null
        }

        return name.take(MAX_FILE_NAME_LENGTH)
    }

    companion object {
        private const val DEFAULT_FILE_NAME = "upload"
        private const val MAX_FILE_NAME_LENGTH = 255
    }
}

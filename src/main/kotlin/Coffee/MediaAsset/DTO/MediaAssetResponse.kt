package Coffee.MediaAsset.DTO

import Coffee.MediaAsset.Model.MediaAsset
import Coffee.MediaAsset.Model.MediaAssetType
import Coffee.MediaAsset.Model.MediaProvider
import java.time.Instant
import java.util.UUID

data class MediaAssetResponse(
    val id: UUID,
    val assetType: MediaAssetType,
    val provider: MediaProvider,
    val storageKey: String,
    val publicId: String?,
    val url: String?,
    val mimeType: String?,
    val fileName: String?,
    val fileSizeBytes: Long?,
    val width: Int?,
    val height: Int?,
    val createdAt: Instant
) {
    companion object {
        fun from(asset: MediaAsset) = MediaAssetResponse(
            id = asset.id,
            assetType = asset.assetType,
            provider = asset.provider,
            storageKey = asset.storageKey,
            publicId = asset.publicId,
            url = asset.url,
            mimeType = asset.mimeType,
            fileName = asset.fileName,
            fileSizeBytes = asset.fileSizeBytes,
            width = asset.width,
            height = asset.height,
            createdAt = asset.createdAt
        )
    }
}

package Coffee.MediaAsset.DTO

import Coffee.MediaAsset.Model.MediaAssetType
import Coffee.MediaAsset.Model.MediaProvider
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Positive
import jakarta.validation.constraints.PositiveOrZero
import jakarta.validation.constraints.Size

data class MediaAssetRequest(
    @field:NotNull
    val assetType: MediaAssetType,

    @field:NotNull
    val provider: MediaProvider,

    @field:NotBlank
    val storageKey: String,

    @field:Size(max = 255)
    val publicId: String? = null,

    val url: String? = null,

    @field:Size(max = 100)
    val mimeType: String? = null,

    @field:Size(max = 255)
    val fileName: String? = null,

    @field:PositiveOrZero
    val fileSizeBytes: Long? = null,

    @field:Positive
    val width: Int? = null,

    @field:Positive
    val height: Int? = null
)

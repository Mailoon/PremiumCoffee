package Coffee.MediaAsset.Controller

import Coffee.Common.Controllers.AbstractCrudController
import Coffee.MediaAsset.DTO.MediaAssetRequest
import Coffee.MediaAsset.DTO.MediaAssetResponse
import Coffee.MediaAsset.DTO.MediaAssetUrlResponse
import Coffee.MediaAsset.Model.MediaAsset
import Coffee.MediaAsset.Services.MediaAssetService
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile
import java.util.UUID

@RestController
@RequestMapping("/media-assets")
class MediaAssetController(
    private val service: MediaAssetService
) : AbstractCrudController<MediaAsset, UUID, MediaAssetRequest, MediaAssetResponse>(
    service,
    { MediaAssetResponse.from(it) }
) {

    @PostMapping("/upload")
    @ResponseStatus(HttpStatus.CREATED)
    fun upload(@RequestParam("file") file: MultipartFile): MediaAssetResponse {
        try {
            return MediaAssetResponse.from(service.upload(file))
        } catch (e: Exception) {
            throw (e)
        }
    }

    @GetMapping("/{id}/url")
    fun storedUrl(@PathVariable id: UUID): MediaAssetUrlResponse {
        try {
            return service.storedUrl(id)
        } catch (e: Exception) {
            throw (e)
        }
    }
}

package Coffee.MediaAsset.Controller

import Coffee.Common.Controllers.AbstractReadController
import Coffee.MediaAsset.DTO.MediaAssetResponse
import Coffee.MediaAsset.DTO.MediaAssetUrlResponse
import Coffee.MediaAsset.Model.MediaAsset
import Coffee.MediaAsset.Services.MediaAssetService
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.DeleteMapping
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
@RequestMapping("/api/v1/media-assets")
class MediaAssetController(
    private val mediaAssetService: MediaAssetService
) : AbstractReadController<MediaAsset, UUID, MediaAssetResponse>(
    mediaAssetService,
    { MediaAssetResponse.from(it) }
) {

    @PostMapping("/upload")
    @ResponseStatus(HttpStatus.CREATED)
    fun upload(@RequestParam("file") file: MultipartFile): MediaAssetResponse =
        MediaAssetResponse.from(mediaAssetService.upload(file))

    @GetMapping("/{id}/url")
    fun storedUrl(@PathVariable id: UUID): MediaAssetUrlResponse = mediaAssetService.storedUrl(id)

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun delete(@PathVariable id: UUID) {
        mediaAssetService.delete(id)
    }
}
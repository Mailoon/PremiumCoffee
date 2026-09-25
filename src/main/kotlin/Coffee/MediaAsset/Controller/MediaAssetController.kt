package Coffee.MediaAsset.Controller

import Coffee.Common.Controllers.AbstractCrudController
import Coffee.MediaAsset.DTO.MediaAssetRequest
import Coffee.MediaAsset.DTO.MediaAssetResponse
import Coffee.MediaAsset.Model.MediaAsset
import Coffee.MediaAsset.Services.MediaAssetService
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/media-assets")
class MediaAssetController(
    service: MediaAssetService
) : AbstractCrudController<MediaAsset, UUID, MediaAssetRequest, MediaAssetResponse>(
    service,
    { MediaAssetResponse.from(it) }
)


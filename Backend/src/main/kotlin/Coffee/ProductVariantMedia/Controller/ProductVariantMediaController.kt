package Coffee.ProductVariantMedia.Controller

import Coffee.Common.Controllers.AbstractCrudController
import Coffee.ProductVariantMedia.DTO.ProductVariantMediaRequest
import Coffee.ProductVariantMedia.DTO.ProductVariantMediaResponse
import Coffee.ProductVariantMedia.Model.ProductVariantMedia
import Coffee.ProductVariantMedia.Services.ProductVariantMediaService
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/product-variant-media")
class ProductVariantMediaController(
    service: ProductVariantMediaService
) : AbstractCrudController<ProductVariantMedia, UUID, ProductVariantMediaRequest, ProductVariantMediaResponse>(
    service,
    { ProductVariantMediaResponse.from(it) }
)

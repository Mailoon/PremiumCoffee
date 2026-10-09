package Coffee.ProductVariant.Controller

import Coffee.Common.Controllers.AbstractCrudController
import Coffee.ProductVariant.DTO.ProductVariantRequest
import Coffee.ProductVariant.DTO.ProductVariantResponse
import Coffee.ProductVariant.Model.ProductVariant
import Coffee.ProductVariant.Services.ProductVariantService
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api/v1/product-variants")
class ProductVariantController(
    service: ProductVariantService
) : AbstractCrudController<ProductVariant, UUID, ProductVariantRequest, ProductVariantResponse>(
    service,
    { ProductVariantResponse.from(it) }
)

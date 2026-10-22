package Coffee.ProductVariantPrice.Controller

import Coffee.Common.ApiVersion
import Coffee.Common.Controllers.AbstractCrudController
import Coffee.ProductVariantPrice.DTO.ProductVariantPriceRequest
import Coffee.ProductVariantPrice.DTO.ProductVariantPriceResponse
import Coffee.ProductVariantPrice.Model.ProductVariantPrice
import Coffee.ProductVariantPrice.Services.ProductVariantPriceService
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping(ApiVersion.V1 + "/product-variant-prices")
class ProductVariantPriceController(
    service: ProductVariantPriceService
) : AbstractCrudController<ProductVariantPrice, UUID, ProductVariantPriceRequest, ProductVariantPriceResponse>(
    service,
    { ProductVariantPriceResponse.from(it) }
)

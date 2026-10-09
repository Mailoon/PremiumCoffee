package Coffee.Product.Controller

import Coffee.Common.Controllers.AbstractCrudController
import Coffee.Product.DTO.ProductRequest
import Coffee.Product.DTO.ProductResponse
import Coffee.Product.Model.Product
import Coffee.Product.Services.ProductService
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api/v1/products")
class ProductController(
    service: ProductService
) : AbstractCrudController<Product, UUID, ProductRequest, ProductResponse>(
    service,
    { ProductResponse.from(it) }
)


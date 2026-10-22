package Coffee.Category.Controller

import Coffee.Category.DTO.CategoryRequest
import Coffee.Category.DTO.CategoryResponse
import Coffee.Category.Model.Category
import Coffee.Category.Services.CategoryService
import Coffee.Common.ApiVersion
import Coffee.Common.Controllers.AbstractCrudController
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping(ApiVersion.V1 + "/categories")
class CategoryController(
    service: CategoryService
) : AbstractCrudController<Category, UUID, CategoryRequest, CategoryResponse>(
    service,
    { CategoryResponse.from(it) }
)


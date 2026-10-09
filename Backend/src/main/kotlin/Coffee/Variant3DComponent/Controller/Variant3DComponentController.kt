package Coffee.Variant3DComponent.Controller

import Coffee.Common.Controllers.AbstractCrudController
import Coffee.Variant3DComponent.DTO.Variant3DComponentRequest
import Coffee.Variant3DComponent.DTO.Variant3DComponentResponse
import Coffee.Variant3DComponent.Model.Variant3DComponent
import Coffee.Variant3DComponent.Services.Variant3DComponentService
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api/v1/variant-3d-components")
class Variant3DComponentController(
    service: Variant3DComponentService
) : AbstractCrudController<Variant3DComponent, UUID, Variant3DComponentRequest, Variant3DComponentResponse>(
    service,
    { Variant3DComponentResponse.from(it) }
)


package Coffee.Catalog.Controller

import Coffee.Catalog.DTO.CatalogResponse
import Coffee.Catalog.Services.CatalogService
import Coffee.Common.ApiVersion
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping(ApiVersion.V1 + "/catalog")
class CatalogController(
    private val catalogService: CatalogService
) {

    @GetMapping
    fun catalog(): CatalogResponse = catalogService.catalog()
}
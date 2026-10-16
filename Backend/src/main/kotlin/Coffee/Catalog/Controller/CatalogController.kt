package Coffee.Catalog.Controller

import Coffee.Catalog.DTO.CatalogResponse
import Coffee.Catalog.Services.CatalogService
import Coffee.Common.ApiVersion
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * One read endpoint for the storefront.
 *
 * This does not replace the CRUD endpoints per aggregate. Those stay for
 * administration and remain the way rows are written. This is the read side
 * only, shaped for a page that needs the catalog, its variants and the media
 * URLs at once.
 *
 * It is public, like the catalog itself, and it exposes no more than the
 * per-aggregate endpoints already do: no storage key, no provider, no public id.
 */
@RestController
@RequestMapping(ApiVersion.V1 + "/catalog")
class CatalogController(
    private val catalogService: CatalogService
) {

    @GetMapping
    fun catalog(): CatalogResponse = catalogService.catalog()
}
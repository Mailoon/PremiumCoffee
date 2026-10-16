package Coffee.Catalog.Services

import Coffee.Catalog.DTO.CatalogMedia
import Coffee.Catalog.DTO.CatalogPrice
import Coffee.Catalog.DTO.CatalogResponse
import Coffee.Catalog.DTO.CategoryNode
import Coffee.Catalog.DTO.ProductNode
import Coffee.Catalog.DTO.VariantNode
import Coffee.Category.Repository.CategoryRepository
import Coffee.MediaAsset.Model.MediaAssetType
import Coffee.Product.Repository.ProductRepository
import Coffee.ProductVariant.Model.ProductVariant
import Coffee.ProductVariant.Repository.ProductVariantRepository
import Coffee.ProductVariantMedia.Model.ProductVariantMedia
import Coffee.ProductVariantMedia.Repository.ProductVariantMediaRepository
import Coffee.ProductVariantPrice.Model.ProductVariantPrice
import Coffee.ProductVariantPrice.Repository.ProductVariantPriceRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.util.UUID

/**
 * Assembles the whole storefront catalog in one request.
 *
 * Each table is read once and the tree is grouped in memory. The entities only
 * model the child to parent direction, so the tree cannot be walked from the
 * root; walking it that way is what produced the N+1 this endpoint removes.
 *
 * The cost is that the whole catalog is materialised. That is the right trade
 * for a catalog of tens of products read on every page, and the wrong one for
 * thousands. If the catalog grows, this becomes a cached read rather than a
 * bigger query; the response shape does not have to change.
 */
@Service
class CatalogService(
    private val categoryRepository: CategoryRepository,
    private val productRepository: ProductRepository,
    private val variantRepository: ProductVariantRepository,
    private val mediaRepository: ProductVariantMediaRepository,
    private val priceRepository: ProductVariantPriceRepository
) {

    /**
     * Read only, and it has to be: the mapping runs after the queries return, so
     * any lazy association touched here would fail with `open-in-view=false`.
     * That is why the repository queries use `join fetch`.
     */
    @Transactional(readOnly = true)
    fun catalog(now: Instant = Instant.now()): CatalogResponse {
        val categories = categoryRepository.findActiveRoots()
        if (categories.isEmpty()) {
            return CatalogResponse(emptyList())
        }

        val productsByCategory = productRepository.findActiveUnderRoots().groupBy { it.category.id }
        val variantsByProduct = variantRepository.findActiveUnderRoots().groupBy { it.product.id }
        val mediaByVariant = mediaRepository.findRenderableMedia().groupBy { it.variant.id }
        val priceByVariant = currentPrices(priceRepository.findEffectiveAt(now))

        val nodes = categories.map { category ->
            CategoryNode(
                id = category.id,
                name = category.name,
                slug = category.slug,
                products = productsByCategory[category.id].orEmpty().map { product ->
                    ProductNode(
                        id = product.id,
                        name = product.name,
                        slug = product.slug,
                        description = product.description,
                        variants = variantsByProduct[product.id].orEmpty().map { variant ->
                            variantNode(variant, priceByVariant[variant.id], mediaByVariant[variant.id].orEmpty())
                        }
                    )
                }
            )
        }

        return CatalogResponse(nodes)
    }

    /**
     * A variant may carry more than one price window that is effective at the
     * same instant, because overlapping windows are legal in the schema. The
     * repository sorts ascending by `validFrom`, so the last row of each group is
     * the most recently started window, and that is the one a storefront shows.
     */
    private fun currentPrices(rows: List<ProductVariantPrice>): Map<UUID, ProductVariantPrice> =
        rows.groupBy { it.variant.id }.mapValues { (_, group) -> group.last() }

    private fun variantNode(
        variant: ProductVariant,
        price: ProductVariantPrice?,
        media: List<ProductVariantMedia>
    ): VariantNode = VariantNode(
        id = variant.id,
        name = variant.name,
        sku = variant.sku,
        volumeMl = variant.volumeMl,
        price = price?.let { CatalogPrice(it.price, it.currency) },
        images = media.toCatalogMedia(isImage = true),
        models3d = media.toCatalogMedia(isImage = false)
    )

    /**
     * Splits the media of one variant into the two lists the storefront
     * consumes.
     *
     * The asset type decides, not the role. A variant may legitimately carry a
     * `MODEL_3D` role pointing at an image, or an `IMAGE` role pointing at a
     * GLB, and trusting the role would put a 3D file in an `<img>` tag. SVG
     * counts as an image: it renders in an `<img>` tag like any other.
     */
    private fun List<ProductVariantMedia>.toCatalogMedia(isImage: Boolean): List<CatalogMedia> =
        filter { (it.mediaAsset.assetType != MediaAssetType.MODEL_3D) == isImage }
            .map { link ->
                val asset = link.mediaAsset
                CatalogMedia(
                    id = asset.id,
                    role = link.role,
                    isPrimary = link.isPrimary,
                    url = asset.url.orEmpty(),
                    width = asset.width,
                    height = asset.height,
                    fileSizeBytes = asset.fileSizeBytes,
                    assetType = asset.assetType.name
                )
            }
}
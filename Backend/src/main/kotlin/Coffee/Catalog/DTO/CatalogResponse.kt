package Coffee.Catalog.DTO

import Coffee.ProductVariantMedia.Model.VariantMediaRole
import java.math.BigDecimal
import java.util.UUID

/**
 * Read model for the storefront, assembled in one pass.
 *
 * This exists to remove an N+1. The generic CRUD endpoints return
 * `product_variant_media` rows carrying only `mediaAssetId`, so rendering a
 * gallery means one call per image to resolve the id into a URL. This tree
 * carries the URL directly, so the storefront needs exactly one request.
 *
 * Only what a storefront renders is included. There is no `storageKey`, no
 * `provider` and no `publicId`: those are storage concerns, and a read model is
 * not the place to expose how a file is stored.
 *
 * [images] and [models3d] are separate lists on purpose. They have different
 * consumers: images go to `<img>` tags and to the poster/thumbnail slots, while
 * models3d go to the WebGL loader. Merging them into one list and filtering by
 * role would push that distinction into the frontend on every render.
 */
data class CatalogResponse(
    val categories: List<CategoryNode>
)

data class CategoryNode(
    val id: UUID,
    val name: String,
    val slug: String,
    val products: List<ProductNode>
)

data class ProductNode(
    val id: UUID,
    val name: String,
    val slug: String,
    val description: String?,
    val variants: List<VariantNode>
)

data class VariantNode(
    val id: UUID,
    val name: String,
    val sku: String,
    val volumeMl: Int?,
    val price: CatalogPrice?,
    val images: List<CatalogMedia>,
    val models3d: List<CatalogMedia>
)

data class CatalogPrice(
    val amount: BigDecimal,
    val currency: String
)

data class CatalogMedia(
    val id: UUID,
    val role: VariantMediaRole,
    val isPrimary: Boolean,
    val url: String,
    val width: Int?,
    val height: Int?,
    val fileSizeBytes: Long?,
    val assetType: String
)
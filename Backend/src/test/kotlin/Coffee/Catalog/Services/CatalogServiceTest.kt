package Coffee.Catalog.Services

import Coffee.Category.Model.Category
import Coffee.Category.Repository.CategoryRepository
import Coffee.MediaAsset.Model.MediaAsset
import Coffee.MediaAsset.Model.MediaAssetType
import Coffee.MediaAsset.Model.MediaProvider
import Coffee.Product.Model.Product
import Coffee.Product.Repository.ProductRepository
import Coffee.ProductVariant.Model.ProductVariant
import Coffee.ProductVariant.Repository.ProductVariantRepository
import Coffee.ProductVariantMedia.Model.ProductVariantMedia
import Coffee.ProductVariantMedia.Model.VariantMediaRole
import Coffee.ProductVariantMedia.Repository.ProductVariantMediaRepository
import Coffee.ProductVariantPrice.Model.ProductVariantPrice
import Coffee.ProductVariantPrice.Repository.ProductVariantPriceRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import java.math.BigDecimal
import java.time.Instant

class CatalogServiceTest {

    private val categories = Mockito.mock(CategoryRepository::class.java)
    private val products = Mockito.mock(ProductRepository::class.java)
    private val variants = Mockito.mock(ProductVariantRepository::class.java)
    private val media = Mockito.mock(ProductVariantMediaRepository::class.java)
    private val prices = Mockito.mock(ProductVariantPriceRepository::class.java)

    private val service = CatalogService(categories, products, variants, media, prices)
    private val now: Instant = Instant.parse("2026-03-01T12:00:00Z")

    @Test
    fun `returns an empty catalog without touching the other repositories`() {
        Mockito.`when`(categories.findActiveRoots()).thenReturn(emptyList())

        val result = service.catalog(now)

        assertTrue(result.categories.isEmpty())
        Mockito.verifyNoInteractions(products, variants, media, prices)
    }

    @Test
    fun `nests products and variants under their category`() {
        val category = category()
        val product = product(category)
        val variant = variant(product)
        stub(listOf(category), listOf(product), listOf(variant), emptyList(), emptyList())

        val result = service.catalog(now)

        assertEquals(1, result.categories.size)
        val node = result.categories.first()
        assertEquals("Bebidas", node.name)
        assertEquals(1, node.products.size)
        assertEquals("Latte", node.products.first().name)
        assertEquals("Latte 250ml", node.products.first().variants.first().name)
    }

    @Test
    fun `separates images from 3d models`() {
        val category = category()
        val product = product(category)
        val variant = variant(product)
        val links = listOf(
            link(variant, asset("https://cdn/latte.jpg", MediaAssetType.IMAGE), VariantMediaRole.PRIMARY, true, 0),
            link(variant, asset("https://cdn/galeria.jpg", MediaAssetType.IMAGE), VariantMediaRole.GALLERY, false, 1),
            link(variant, asset("https://cdn/logo.svg", MediaAssetType.SVG), VariantMediaRole.THUMBNAIL, false, 2),
            link(variant, asset("https://cdn/vaso.glb", MediaAssetType.MODEL_3D), VariantMediaRole.MODEL_3D, false, 3)
        )
        stub(listOf(category), listOf(product), listOf(variant), links, emptyList())

        val node = service.catalog(now).categories.first().products.first().variants.first()

        // SVG counts as an image: it renders in an img tag like any other.
        assertEquals(3, node.images.size)
        assertEquals(
            listOf("https://cdn/latte.jpg", "https://cdn/galeria.jpg", "https://cdn/logo.svg"),
            node.images.map { it.url }
        )
        assertEquals(1, node.models3d.size)
        assertEquals("https://cdn/vaso.glb", node.models3d.first().url)
    }

    @Test
    fun `splits on the asset type and not on the role`() {
        val category = category()
        val product = product(category)
        val variant = variant(product)
        // A GLB tagged as PRIMARY would end up in the img list if the role decided.
        val links = listOf(
            link(variant, asset("https://cdn/modelo.glb", MediaAssetType.MODEL_3D), VariantMediaRole.PRIMARY, true, 0)
        )
        stub(listOf(category), listOf(product), listOf(variant), links, emptyList())

        val node = service.catalog(now).categories.first().products.first().variants.first()

        assertTrue(node.images.isEmpty())
        assertEquals(1, node.models3d.size)
    }

    @Test
    fun `returns the url of every media so the client never has to resolve an id`() {
        val category = category()
        val product = product(category)
        val variant = variant(product)
        val links = listOf(
            link(variant, asset("https://cdn/hero.jpg", MediaAssetType.IMAGE), VariantMediaRole.PRIMARY, true, 0)
        )
        stub(listOf(category), listOf(product), listOf(variant), links, emptyList())

        val media1 = service.catalog(now).categories.first().products.first().variants.first().images.first()

        assertEquals("https://cdn/hero.jpg", media1.url)
        assertTrue(media1.isPrimary)
        assertEquals("IMAGE", media1.assetType)
    }

    @Test
    fun `keeps the primary flag and the sort order coming from the query`() {
        val category = category()
        val product = product(category)
        val variant = variant(product)
        val links = listOf(
            link(variant, asset("https://cdn/b.jpg", MediaAssetType.IMAGE), VariantMediaRole.GALLERY, false, 0),
            link(variant, asset("https://cdn/a.jpg", MediaAssetType.IMAGE), VariantMediaRole.PRIMARY, true, 1)
        )
        stub(listOf(category), listOf(product), listOf(variant), links, emptyList())

        val images = service.catalog(now).categories.first().products.first().variants.first().images

        assertEquals(
            listOf("https://cdn/b.jpg", "https://cdn/a.jpg"),
            images.map { it.url },
            "the repository order must be preserved, not re-sorted here"
        )
        assertTrue(images.last().isPrimary)
    }

    @Test
    fun `picks the most recently started price window`() {
        val category = category()
        val product = product(category)
        val variant = variant(product)
        val rows = listOf(
            price(variant, "1000.00", Instant.parse("2025-01-01T00:00:00Z"), null),
            price(variant, "1200.00", Instant.parse("2026-01-01T00:00:00Z"), null)
        )
        stub(listOf(category), listOf(product), listOf(variant), emptyList(), rows)

        val node = service.catalog(now).categories.first().products.first().variants.first()

        val price = requireNotNull(node.price)
        assertEquals(0, BigDecimal("1200.00").compareTo(price.amount))
        assertEquals("COP", price.currency)
    }

    @Test
    fun `leaves the price null when the variant has none`() {
        val category = category()
        val product = product(category)
        val variant = variant(product)
        stub(listOf(category), listOf(product), listOf(variant), emptyList(), emptyList())

        val node = service.catalog(now).categories.first().products.first().variants.first()

        assertNull(node.price)
    }

    @Test
    fun `returns a variant with no media at all`() {
        val category = category()
        val product = product(category)
        val variant = variant(product)
        stub(listOf(category), listOf(product), listOf(variant), emptyList(), emptyList())

        val node = service.catalog(now).categories.first().products.first().variants.first()

        assertTrue(node.images.isEmpty())
        assertTrue(node.models3d.isEmpty())
    }

    private fun stub(
        categoriesList: List<Category>,
        productsList: List<Product>,
        variantsList: List<ProductVariant>,
        mediaList: List<ProductVariantMedia>,
        pricesList: List<ProductVariantPrice>
    ) {
        Mockito.`when`(categories.findActiveRoots()).thenReturn(categoriesList)
        Mockito.`when`(products.findActiveUnderRoots()).thenReturn(productsList)
        Mockito.`when`(variants.findActiveUnderRoots()).thenReturn(variantsList)
        Mockito.`when`(media.findRenderableMedia()).thenReturn(mediaList)
        Mockito.`when`(prices.findEffectiveAt(now)).thenReturn(pricesList)
    }

    private fun category() = Category(name = "Bebidas", slug = "bebidas")

    private fun product(category: Category) =
        Product(category = category, name = "Latte", slug = "latte", description = "Cafe con leche")

    private fun variant(product: Product) =
        ProductVariant(product = product, name = "Latte 250ml", sku = "LAT-250", volumeMl = 250)

    private fun asset(url: String, type: MediaAssetType) = MediaAsset(
        assetType = type,
        provider = MediaProvider.LOCAL,
        storageKey = "sha256/ab/key",
        publicId = "sha256/ab/key",
        url = url
    )

    private fun link(
        variant: ProductVariant,
        asset: MediaAsset,
        role: VariantMediaRole,
        isPrimary: Boolean,
        sortOrder: Int
    ) = ProductVariantMedia(
        variant = variant,
        mediaAsset = asset,
        role = role,
        isPrimary = isPrimary,
        sortOrder = sortOrder
    )

    private fun price(
        variant: ProductVariant,
        amount: String,
        from: Instant,
        until: Instant?
    ) = ProductVariantPrice(
        variant = variant,
        price = BigDecimal(amount),
        validFrom = from,
        validUntil = until
    )
}
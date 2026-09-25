package Coffee.Common.DTO

import Coffee.Category.DTO.CategoryResponse
import Coffee.Category.Model.Category
import Coffee.MediaAsset.DTO.MediaAssetResponse
import Coffee.MediaAsset.Model.MediaAsset
import Coffee.MediaAsset.Model.MediaAssetType
import Coffee.MediaAsset.Model.MediaProvider
import Coffee.Product.DTO.ProductResponse
import Coffee.Product.Model.Product
import Coffee.ProductVariant.DTO.ProductVariantResponse
import Coffee.ProductVariant.Model.ProductVariant
import Coffee.ProductVariantMedia.DTO.ProductVariantMediaResponse
import Coffee.ProductVariantMedia.Model.ProductVariantMedia
import Coffee.ProductVariantMedia.Model.VariantMediaRole
import Coffee.ProductVariantPrice.DTO.ProductVariantPriceResponse
import Coffee.ProductVariantPrice.Model.ProductVariantPrice
import Coffee.Variant3DComponent.DTO.Variant3DComponentResponse
import Coffee.Variant3DComponent.Model.Variant3DComponent
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.Instant

class ResponseMappingTest {

    @Test
    fun `maps aggregate entities to response DTOs`() {
        val now = Instant.parse("2026-01-01T00:00:00Z")
        val parent = Category(name = "Drinks", slug = "drinks")
        val category = Category(name = "Coffee", slug = "coffee", parent = parent)
        val product = Product(
            category = category,
            name = "Latte",
            slug = "latte",
            description = "Espresso with milk",
            createdAt = now,
            updatedAt = now
        )
        val variant = ProductVariant(
            product = product,
            name = "Large",
            sku = "LATTE-L",
            volumeMl = 470,
            createdAt = now,
            updatedAt = now
        )
        val price = ProductVariantPrice(
            variant = variant,
            price = BigDecimal("12900.00"),
            currency = "COP",
            validFrom = now,
            createdAt = now
        )
        val asset = MediaAsset(
            assetType = MediaAssetType.MODEL_3D,
            provider = MediaProvider.CLOUDINARY,
            storageKey = "variants/latte.glb",
            createdAt = now
        )
        val media = ProductVariantMedia(
            variant = variant,
            mediaAsset = asset,
            role = VariantMediaRole.MODEL_3D,
            sortOrder = 1,
            isPrimary = true,
            createdAt = now
        )
        val component = Variant3DComponent(
            variant = variant,
            name = "Cream",
            code = "cream",
            mediaAsset = asset,
            required = false,
            defaultEnabled = true,
            createdAt = now
        )

        val categoryResponse = CategoryResponse.from(category)
        val productResponse = ProductResponse.from(product)
        val variantResponse = ProductVariantResponse.from(variant)
        val priceResponse = ProductVariantPriceResponse.from(price)
        val assetResponse = MediaAssetResponse.from(asset)
        val mediaResponse = ProductVariantMediaResponse.from(media)
        val componentResponse = Variant3DComponentResponse.from(component)

        assertEquals(parent.id, categoryResponse.parentId)
        assertEquals(category.id, productResponse.categoryId)
        assertEquals(product.id, variantResponse.productId)
        assertEquals(variant.id, priceResponse.variantId)
        assertEquals(asset.id, mediaResponse.mediaAssetId)
        assertTrue(mediaResponse.isPrimary)
        assertEquals(asset.id, componentResponse.mediaAssetId)
        assertNull(priceResponse.validUntil)
        assertEquals("variants/latte.glb", assetResponse.storageKey)
    }
}

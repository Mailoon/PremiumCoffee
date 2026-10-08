package Coffee.ProductVariantMedia.Controller

import Coffee.Common.Controllers.ApiExceptionHandler
import Coffee.Common.Models.PageQuery
import Coffee.Common.Models.PageResult
import Coffee.MediaAsset.Model.MediaAsset
import Coffee.MediaAsset.Model.MediaAssetType
import Coffee.MediaAsset.Model.MediaProvider
import Coffee.Product.Model.Product
import Coffee.ProductVariant.Model.ProductVariant
import Coffee.ProductVariantMedia.Model.ProductVariantMedia
import Coffee.ProductVariantMedia.Model.VariantMediaRole
import Coffee.ProductVariantMedia.Services.ProductVariantMediaService
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.MockMvcBuilders

class ProductVariantMediaControllerTest {

    @Test
    fun `serializes relationship ids and isPrimary without entities`() {
        val service = Mockito.mock(ProductVariantMediaService::class.java)
        val category = Coffee.Category.Model.Category(name = "Coffee", slug = "coffee")
        val product = Product(category = category, name = "Latte", slug = "latte")
        val variant = ProductVariant(product = product, name = "Large", sku = "LATTE-L")
        val asset = MediaAsset(
            assetType = MediaAssetType.MODEL_3D,
            provider = MediaProvider.CLOUDINARY,
            storageKey = "variants/latte.glb"
        )
        val media = ProductVariantMedia(
            variant = variant,
            mediaAsset = asset,
            role = VariantMediaRole.MODEL_3D,
            isPrimary = true
        )
        Mockito.`when`(service.findAll(PageQuery(0, 20))).thenReturn(
            PageResult(listOf(media), 0, 20, 1, 1)
        )
        val mockMvc = MockMvcBuilders
            .standaloneSetup(ProductVariantMediaController(service))
            .setControllerAdvice(ApiExceptionHandler())
            .build()

        mockMvc.perform(get("/product-variant-media"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.content[0].variantId").value(variant.id.toString()))
            .andExpect(jsonPath("$.content[0].mediaAssetId").value(asset.id.toString()))
            .andExpect(jsonPath("$.content[0].isPrimary").value(true))
            .andExpect(jsonPath("$.content[0].primary").doesNotExist())
            .andExpect(jsonPath("$.content[0].variant").doesNotExist())
            .andExpect(jsonPath("$.content[0].mediaAsset").doesNotExist())
    }
}

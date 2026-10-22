package Coffee.ProductVariantPrice.DTO

import Coffee.ProductVariantPrice.Model.ProductVariantPrice
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

data class ProductVariantPriceResponse(
    val id: UUID,
    val variantId: UUID,
    val price: BigDecimal,
    val currency: String,
    val validFrom: Instant,
    val validUntil: Instant?,
    val createdAt: Instant
) {
    companion object {
        fun from(price: ProductVariantPrice) = ProductVariantPriceResponse(
            id = price.id,
            variantId = price.variant.id,
            price = price.price,
            currency = price.currency,
            validFrom = price.validFrom,
            validUntil = price.validUntil,
            createdAt = price.createdAt
        )
    }
}

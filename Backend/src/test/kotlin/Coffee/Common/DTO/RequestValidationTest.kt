package Coffee.Common.DTO

import Coffee.ProductVariant.DTO.ProductVariantRequest
import Coffee.ProductVariantPrice.DTO.ProductVariantPriceRequest
import jakarta.validation.Validation
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

class RequestValidationTest {

    private val validator = Validation.buildDefaultValidatorFactory().validator

    @Test
    fun `rejects invalid price currency and validity range`() {
        val validFrom = Instant.parse("2026-01-01T00:00:00Z")
        val request = ProductVariantPriceRequest(
            variantId = UUID.randomUUID(),
            price = BigDecimal("10.123"),
            currency = "cop",
            validFrom = validFrom,
            validUntil = validFrom
        )

        val violations = validator.validate(request)

        assertTrue(violations.any { it.propertyPath.toString() == "price" })
        assertTrue(violations.any { it.propertyPath.toString() == "currency" })
        assertTrue(violations.any { it.message == "validUntil must be after validFrom" })
    }

    @Test
    fun `accepts a valid price validity range`() {
        val validFrom = Instant.parse("2026-01-01T00:00:00Z")
        val request = ProductVariantPriceRequest(
            variantId = UUID.randomUUID(),
            price = BigDecimal("10.00"),
            currency = "COP",
            validFrom = validFrom,
            validUntil = validFrom.plusSeconds(3600)
        )

        assertTrue(validator.validate(request).isEmpty())
    }

    @Test
    fun `rejects a non-positive variant volume`() {
        val request = ProductVariantRequest(
            productId = UUID.randomUUID(),
            name = "Large",
            sku = "LARGE",
            volumeMl = 0
        )

        assertTrue(
            validator.validate(request).any { it.propertyPath.toString() == "volumeMl" }
        )
    }
}

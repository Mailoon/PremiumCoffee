package Coffee.ProductVariantPrice.DTO

import jakarta.validation.constraints.AssertTrue
import jakarta.validation.constraints.DecimalMin
import jakarta.validation.constraints.Digits
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Pattern
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

data class ProductVariantPriceRequest(
    @field:NotNull
    val variantId: UUID,

    @field:DecimalMin("0.00")
    @field:Digits(integer = 10, fraction = 2)
    val price: BigDecimal,

    @field:NotBlank
    @field:Pattern(regexp = "^[A-Z]{3}$")
    val currency: String = "COP",

    @field:NotNull
    val validFrom: Instant,

    val validUntil: Instant? = null
) {
    @get:AssertTrue(message = "validUntil must be after validFrom")
    val isValidRange: Boolean
        get() = validUntil?.isAfter(validFrom) != false
}

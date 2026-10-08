package Coffee.ProductVariantPrice.Model

import Coffee.ProductVariant.Model.ProductVariant
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "product_variant_prices")
class ProductVariantPrice(
    @Id
    val id: UUID = UUID.randomUUID(),

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "variant_id", nullable = false)
    var variant: ProductVariant,

    @Column(nullable = false, precision = 12, scale = 2)
    var price: BigDecimal,

    @Column(nullable = false, length = 3)
    var currency: String = "COP",

    @Column(name = "valid_from", nullable = false)
    var validFrom: Instant,

    @Column(name = "valid_until")
    var validUntil: Instant? = null,

    @Column(name = "created_at", nullable = false, updatable = false)
    var createdAt: Instant = Instant.now()
)

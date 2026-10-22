package Coffee.ProductVariantMedia.Model

import Coffee.MediaAsset.Model.MediaAsset
import Coffee.ProductVariant.Model.ProductVariant
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import java.time.Instant
import java.util.UUID

enum class VariantMediaRole { PRIMARY, GALLERY, THUMBNAIL, POSTER, MODEL_3D }

@Entity
@Table(name = "product_variant_media")
class ProductVariantMedia(
    @Id
    val id: UUID = UUID.randomUUID(),

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "variant_id", nullable = false)
    var variant: ProductVariant,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "media_asset_id", nullable = false)
    var mediaAsset: MediaAsset,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    var role: VariantMediaRole,

    @Column(name = "sort_order", nullable = false)
    var sortOrder: Int = 0,

    @Column(name = "is_primary", nullable = false)
    var isPrimary: Boolean = false,

    @Column(name = "created_at", nullable = false, updatable = false)
    var createdAt: Instant = Instant.now()
)

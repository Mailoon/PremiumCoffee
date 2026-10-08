package Coffee.Variant3DComponent.Model

import Coffee.MediaAsset.Model.MediaAsset
import Coffee.ProductVariant.Model.ProductVariant
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import java.time.Instant
import java.util.UUID

@Entity
@Table(
    name = "variant_3d_components",
    uniqueConstraints = [UniqueConstraint(columnNames = ["variant_id", "code"])]
)
class Variant3DComponent(
    @Id
    val id: UUID = UUID.randomUUID(),

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "variant_id", nullable = false)
    var variant: ProductVariant,

    @Column(nullable = false, length = 100)
    var name: String,

    @Column(nullable = false, length = 100)
    var code: String,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "media_asset_id", nullable = false)
    var mediaAsset: MediaAsset,

    @Column(nullable = false)
    var required: Boolean = false,

    @Column(name = "default_enabled", nullable = false)
    var defaultEnabled: Boolean = true,

    @Column(name = "created_at", nullable = false, updatable = false)
    var createdAt: Instant = Instant.now()
)

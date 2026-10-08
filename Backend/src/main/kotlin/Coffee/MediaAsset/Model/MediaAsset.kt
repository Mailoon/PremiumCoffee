package Coffee.MediaAsset.Model

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant
import java.util.UUID

enum class MediaAssetType { IMAGE, SVG, MODEL_3D }

enum class MediaProvider { CLOUDINARY, MINIO, S3 }

@Entity
@Table(name = "media_assets")
class MediaAsset(
    @Id
    val id: UUID = UUID.randomUUID(),

    @Enumerated(EnumType.STRING)
    @Column(name = "asset_type", nullable = false, length = 30)
    var assetType: MediaAssetType,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    var provider: MediaProvider,

    @Column(name = "storage_key", nullable = false, columnDefinition = "TEXT")
    var storageKey: String,

    @Column(name = "public_id", length = 255)
    var publicId: String? = null,

    @Column(columnDefinition = "TEXT")
    var url: String? = null,

    @Column(name = "mime_type", length = 100)
    var mimeType: String? = null,

    @Column(name = "file_name", length = 255)
    var fileName: String? = null,

    @Column(name = "file_size_bytes")
    var fileSizeBytes: Long? = null,

    var width: Int? = null,

    var height: Int? = null,

    @Column(name = "created_at", nullable = false, updatable = false)
    var createdAt: Instant = Instant.now()
)
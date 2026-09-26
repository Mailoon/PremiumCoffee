package Coffee.Tools.Media.Content

import Coffee.MediaAsset.Model.MediaAssetType

enum class DetectedFormat(
    val mimeType: String,
    val assetType: MediaAssetType
) {
    JPEG("image/jpeg", MediaAssetType.IMAGE),
    PNG("image/png", MediaAssetType.IMAGE),
    GIF("image/gif", MediaAssetType.IMAGE),
    WEBP("image/webp", MediaAssetType.IMAGE),
    AVIF("image/avif", MediaAssetType.IMAGE),
    HEIC("image/heic", MediaAssetType.IMAGE),
    SVG("image/svg+xml", MediaAssetType.SVG),
    GLB("model/gltf-binary", MediaAssetType.MODEL_3D)
}

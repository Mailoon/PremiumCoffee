package Coffee.Tools.Media.Content

import Coffee.MediaAsset.Model.MediaAssetType

/**
 * The single source of truth for "what is this file really".
 *
 * Every value here was decided by reading the first bytes of the upload, never
 * from the file name, the extension or the `Content-Type` the client claimed.
 *
 * [extension] is the extension the file is stored with. It is verified, which
 * matters for local storage: a self-hosted adapter builds the path from it, so
 * a client sending `../../etc/passwd.jpg` cannot influence the destination.
 */
enum class DetectedFormat(
    val mimeType: String,
    val assetType: MediaAssetType,
    val extension: String
) {
    JPEG("image/jpeg", MediaAssetType.IMAGE, "jpg"),
    PNG("image/png", MediaAssetType.IMAGE, "png"),
    GIF("image/gif", MediaAssetType.IMAGE, "gif"),
    WEBP("image/webp", MediaAssetType.IMAGE, "webp"),
    AVIF("image/avif", MediaAssetType.IMAGE, "avif"),
    HEIC("image/heic", MediaAssetType.IMAGE, "heic"),
    SVG("image/svg+xml", MediaAssetType.SVG, "svg"),
    GLB("model/gltf-binary", MediaAssetType.MODEL_3D, "glb")
}
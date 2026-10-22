package Coffee.Tools.Media

/**
 * What a storage adapter receives.
 *
 * This is a plain object on purpose: the port has to stay free of Spring, of
 * the multipart API and of any provider SDK, so an adapter can be swapped
 * without touching the services.
 *
 * [verifiedExtension] is the extension derived from the bytes themselves, not
 * from [fileName]. It is nullable because the provider adapters do not need
 * it, but a local filesystem adapter cannot work without it: the stored path
 * is built from this value, and a path built from a client supplied name is a
 * path traversal waiting to happen.
 */
class MediaUpload(
    val fileName: String,
    val contentType: String?,
    val content: ByteArray,
    val verifiedExtension: String? = null
)
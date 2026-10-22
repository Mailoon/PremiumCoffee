package Coffee.Tools.Media.Cloudinary

import Coffee.MediaAsset.Model.MediaProvider
import Coffee.Tools.Media.Exceptions.MediaStorageException
import Coffee.Tools.Media.MediaStorage
import Coffee.Tools.Media.MediaUpload
import Coffee.Tools.Media.StoredMedia
import com.cloudinary.Cloudinary
import com.cloudinary.Uploader
import java.io.IOException

class CloudinaryMediaStorage(
    cloudinary: Cloudinary,
    private val settings: CloudinarySettings
) : MediaStorage {

    override val provider: MediaProvider = MediaProvider.CLOUDINARY

    private val uploader: Uploader = cloudinary.uploader()

    override fun upload(media: MediaUpload): StoredMedia {
        val result = try {
            uploadBytes(media.content, uploadOptions())
        } catch (exception: IOException) {
            throw MediaStorageException("Could not upload '${media.fileName}' to Cloudinary", exception)
        } catch (exception: RuntimeException) {
            throw MediaStorageException(
                "Cloudinary rejected the upload of '${media.fileName}'",
                exception
            )
        }

        return result.toStoredMedia()
    }

    override fun delete(publicId: String) {
        try {
            uploader.destroy(publicId, emptyMap<String, Any>())
        } catch (exception: IOException) {
            throw MediaStorageException("Could not delete '$publicId' from Cloudinary", exception)
        } catch (exception: RuntimeException) {
            throw MediaStorageException("Cloudinary rejected the deletion of '$publicId'", exception)
        }
    }

    private fun uploadBytes(content: ByteArray, options: Map<String, Any>): Map<*, *> =
        uploader.upload(content, options)

    private fun uploadOptions(): Map<String, Any> = buildMap {
        put("resource_type", RESOURCE_TYPE)
        put("allowed_formats", ALLOWED_FORMATS)
        if (settings.folder.isNotBlank()) {
            put("folder", settings.folder)
        }
    }

    private fun Map<*, *>.toStoredMedia(): StoredMedia =
        StoredMedia(
            publicId = requiredString("public_id"),
            url = optionalString("secure_url")
                ?: optionalString("url")
                ?: throw MediaStorageException("Cloudinary did not return a delivery URL"),
            width = optionalNumber("width")?.toInt(),
            height = optionalNumber("height")?.toInt(),
            sizeBytes = optionalNumber("bytes")?.toLong()
        )

    private fun Map<*, *>.requiredString(key: String): String =
        optionalString(key) ?: throw MediaStorageException("Cloudinary did not return '$key'")

    private fun Map<*, *>.optionalString(key: String): String? = this[key] as? String

    private fun Map<*, *>.optionalNumber(key: String): Number? = this[key] as? Number

    companion object {
        private const val RESOURCE_TYPE = "image"
        private val ALLOWED_FORMATS = listOf(
            "jpg", "jpeg", "png", "gif", "webp", "avif", "heic", "svg", "glb"
        )
    }
}

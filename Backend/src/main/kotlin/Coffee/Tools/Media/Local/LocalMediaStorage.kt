package Coffee.Tools.Media.Local

import Coffee.MediaAsset.Model.MediaProvider
import Coffee.Tools.Media.Exceptions.MediaStorageException
import Coffee.Tools.Media.MediaStorage
import Coffee.Tools.Media.MediaUpload
import Coffee.Tools.Media.StoredMedia
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.security.MessageDigest

/**
 * Stores the bytes on the local filesystem, addressed by content hash.
 *
 * Why hash the content and not use the row id: the URL becomes the cache key.
 * The same bytes uploaded twice produce the same URL, so the browser serves the
 * second copy from cache instead of downloading it again. Different bytes
 * produce a different URL, which invalidates the old one automatically. Cache
 * invalidation stops being something this project has to remember to do.
 *
 * The bytes are never proxied through Spring. [url] points at the public path
 * ([publicPath]), which the web server (Caddy) serves straight from disk. A
 * 30 MB GLB would otherwise be read into a JVM heap whose only job is to copy
 * bytes, and that is both slow and a good way to get the process killed.
 *
 * The key is built from a hexadecimal digest and a verified extension, so it can
 * never contain a path separator. [resolveInsideRoot] re-checks that anyway,
 * because the same class also serves [delete], which takes a value read back
 * from the database and is therefore only as trustworthy as the database.
 */
class LocalMediaStorage(
    private val root: Path,
    private val publicPath: String
) : MediaStorage {

    override val provider: MediaProvider = MediaProvider.LOCAL

    override fun upload(media: MediaUpload): StoredMedia {
        val extension = media.verifiedExtension?.takeIf { it.isNotBlank() }
            ?: throw MediaStorageException(
                "Local storage requires a verified extension; refusing to store " +
                    "'${media.fileName}' with a client supplied name"
            )

        val digest = sha256Hex(media.content)
        val key = keyFor(digest, extension)
        val target = resolveInsideRoot(key)

        try {
            writeIfAbsent(target, media.content)
        } catch (exception: IOException) {
            throw MediaStorageException("Could not store '$key' on the local filesystem", exception)
        }

        return StoredMedia(
            publicId = key,
            url = "${publicPath.trimEnd('/')}/$key",
            sizeBytes = media.content.size.toLong()
        )
    }

    override fun delete(publicId: String) {
        try {
            Files.deleteIfExists(resolveInsideRoot(publicId))
        } catch (exception: IOException) {
            throw MediaStorageException("Could not delete '$publicId' from the local filesystem", exception)
        }
    }

    /**
     * Writes through a temporary file so a reader never observes a half written
     * asset. Same bytes means same path, so an existing complete file is left
     * alone and the write is skipped entirely.
     */
    private fun writeIfAbsent(target: Path, content: ByteArray) {
        if (Files.exists(target)) {
            return
        }
        Files.createDirectories(target.parent)
        val temporary = Files.createTempFile(target.parent, ".upload-", ".tmp")
        try {
            Files.write(temporary, content)
            try {
                Files.move(temporary, target, StandardCopyOption.ATOMIC_MOVE)
            } catch (_: java.nio.file.AtomicMoveNotSupportedException) {
                Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING)
            }
        } finally {
            Files.deleteIfExists(temporary)
        }
    }

    private fun keyFor(digest: String, extension: String): String =
        "$HASH_DIRECTORY/${digest.take(2)}/$digest.$extension"

    /**
     * Defence in depth. The key is hex plus a verified extension, so it cannot
     * traverse. This rejects anything that still resolves outside the root, so a
     * corrupted or tampered database row cannot turn a delete into an arbitrary
     * file removal.
     */
    private fun resolveInsideRoot(key: String): Path {
        val base = root.toAbsolutePath().normalize()
        val resolved = base.resolve(key).normalize()
        if (!resolved.startsWith(base)) {
            throw MediaStorageException("Refusing to access a path outside the media root: '$key'")
        }
        return resolved
    }

    private fun sha256Hex(content: ByteArray): String =
        MessageDigest.getInstance("SHA-256")
            .digest(content)
            .joinToString(separator = "") { byte -> "%02x".format(byte) }

    companion object {
        const val HASH_DIRECTORY = "sha256"
    }
}
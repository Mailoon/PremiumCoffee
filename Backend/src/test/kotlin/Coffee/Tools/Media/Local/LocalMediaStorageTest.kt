package Coffee.Tools.Media.Local

import Coffee.MediaAsset.Model.MediaProvider
import Coffee.Tools.Media.Content.MediaContentFixtures
import Coffee.Tools.Media.Exceptions.MediaStorageException
import Coffee.Tools.Media.MediaUpload
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path

/**
 * These tests pin the two properties the whole design rests on: the path is
 * derived from the bytes, and the same bytes always produce the same URL.
 */
class LocalMediaStorageTest {

    @TempDir
    lateinit var root: Path

    private fun storage(rootPath: Path = root) = LocalMediaStorage(rootPath, "/media")

    private fun upload(
        name: String = "latte.png",
        content: ByteArray = MediaContentFixtures.PNG,
        extension: String? = "png"
    ) = storage().upload(
        MediaUpload(name, "image/png", content, extension)
    )

    @Test
    fun `reports the local provider`() {
        assertEquals(MediaProvider.LOCAL, storage().provider)
    }

    @Test
    fun `keys the file by the sha256 of its content`() {
        val stored = upload()

        // Pinned on purpose. If this changes, the storage layout changed and
        // every stored URL becomes invalid, which is exactly the kind of break
        // that must never happen silently.
        val expected = "sha256/2f/2f4762e57332d57d0ac5f1852d278b916da83b7ba9d07bd04a75d3408add7aea.png"
        assertEquals(expected, stored.publicId)
        assertEquals("/media/$expected", stored.url)
    }

    @Test
    fun `returns the same url for the same bytes regardless of the file name`() {
        val first = upload(name = "latte.png")
        val second = upload(name = "completely-different-name.png")

        assertEquals(first.url, second.url)
    }

    @Test
    fun `changes the url when the bytes change`() {
        val png = upload(name = "a.png", content = MediaContentFixtures.PNG)
        val glb = upload(name = "b.glb", content = MediaContentFixtures.GLB, extension = "glb")

        assertNotEquals(png.url, glb.url)
    }

    @Test
    fun `writes the bytes where the key says they are`() {
        val stored = upload()

        val written = root.resolve(stored.publicId)
        assertTrue(Files.exists(written), "the file should exist at $stored")
        assertEquals(MediaContentFixtures.PNG.toList(), Files.readAllBytes(written).toList())
    }

    @Test
    fun `does not rewrite a file that is already stored`() {
        val stored = upload()
        val target = root.resolve(stored.publicId)

        val before = Files.getLastModifiedTime(target)
        upload()

        assertEquals(before, Files.getLastModifiedTime(target))
    }

    @Test
    fun `leaves no temporary file behind`() {
        upload()

        val leftovers = Files.walk(root).use { paths ->
            paths.filter { Files.isRegularFile(it) }
                .map { root.relativize(it).toString() }
                .filter { it.endsWith(".tmp") }
                .toList()
        }
        assertTrue(leftovers.isEmpty(), "temporary files left behind: $leftovers")
    }

    @Test
    fun `reports the stored size`() {
        assertEquals(MediaContentFixtures.PNG.size.toLong(), upload().sizeBytes)
    }

    @Test
    fun `refuses to store without a verified extension`() {
        val failure = assertThrows(MediaStorageException::class.java) {
            upload(extension = null)
        }

        assertTrue(failure.message!!.contains("verified extension"))
    }

    @Test
    fun `refuses to store with a blank verified extension`() {
        assertThrows(MediaStorageException::class.java) { upload(extension = "  ") }
    }

    @Test
    fun `never lets the client file name influence the stored path`() {
        val stored = upload(name = "../../../../etc/passwd.png")

        val written = root.resolve(stored.publicId).normalize()
        assertTrue(written.startsWith(root.toAbsolutePath().normalize()))
        assertFalse(stored.url.contains(".."))
    }

    @Test
    fun `refuses to delete a path that escapes the media root`() {
        assertThrows(MediaStorageException::class.java) {
            storage().delete("../../../etc/passwd")
        }
    }

    @Test
    fun `refuses to delete an absolute path`() {
        assertThrows(MediaStorageException::class.java) {
            storage().delete("/etc/shadow")
        }
    }

    @Test
    fun `deletes the stored file`() {
        val stored = upload()
        val target = root.resolve(stored.publicId)
        assertTrue(Files.exists(target))

        storage().delete(stored.publicId)

        assertFalse(Files.exists(target))
    }

    @Test
    fun `deleting something that is not there is not an error`() {
        storage().delete("sha256/aa/${"0".repeat(64)}.png")
    }
}
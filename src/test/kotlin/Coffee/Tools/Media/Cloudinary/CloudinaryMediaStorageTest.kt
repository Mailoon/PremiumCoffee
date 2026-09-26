package Coffee.Tools.Media.Cloudinary

import Coffee.MediaAsset.Model.MediaProvider
import Coffee.Tools.Media.Content.MediaContentFixtures
import Coffee.Tools.Media.Exceptions.MediaStorageException
import Coffee.Tools.Media.MediaUpload
import com.cloudinary.Cloudinary
import com.cloudinary.Uploader
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.anyMap
import org.mockito.Mockito
import org.mockito.Mockito.doThrow
import java.io.IOException

class CloudinaryMediaStorageTest {

    private val cloudinary = Mockito.mock(Cloudinary::class.java)
    private val uploader = Mockito.mock(Uploader::class.java)
    private val storage: CloudinaryMediaStorage

    init {
        Mockito.`when`(cloudinary.uploader()).thenReturn(uploader)
        storage = CloudinaryMediaStorage(
            cloudinary,
            CloudinarySettings(
                cloudName = "demo",
                apiKey = "key",
                apiSecret = "secret",
                folder = "premiumcoffee"
            )
        )
    }

    @Test
    fun `sends the content as bytes because the sdk rejects streams`() {
        stubSuccessfulUpload()

        storage.upload(MediaUpload("latte.png", "image/png", MediaContentFixtures.PNG))

        assertEquals(
            MediaContentFixtures.PNG.toList(),
            capturedFile().toList()
        )
    }

    @Test
    fun `uploads every allowed format as an image resource`() {
        stubSuccessfulUpload()

        storage.upload(MediaUpload("milkshake.glb", "model/gltf-binary", MediaContentFixtures.GLB))

        assertEquals("image", capturedOptions()["resource_type"])
    }

    @Test
    fun `lets the provider reject any format outside the allow list`() {
        stubSuccessfulUpload()

        storage.upload(MediaUpload("latte.png", "image/png", MediaContentFixtures.PNG))

        @Suppress("UNCHECKED_CAST")
        val allowed = capturedOptions()["allowed_formats"] as List<String>
        assertTrue(allowed.contains("png"), "png should be allowed")
        assertTrue(allowed.contains("svg"), "svg should be allowed")
        assertTrue(allowed.contains("glb"), "glb should be allowed")
        assertTrue(!allowed.contains("sql"), "no non media format should be allowed")
    }

    @Test
    fun `sends the configured folder`() {
        stubSuccessfulUpload()

        storage.upload(MediaUpload("latte.png", "image/png", MediaContentFixtures.PNG))

        assertEquals("premiumcoffee", capturedOptions()["folder"])
    }

    @Test
    fun `omits the folder when it is blank`() {
        val withoutFolder = CloudinaryMediaStorage(
            cloudinary,
            CloudinarySettings(
                cloudName = "demo",
                apiKey = "key",
                apiSecret = "secret",
                folder = "  "
            )
        )
        stubSuccessfulUpload()

        withoutFolder.upload(MediaUpload("latte.png", "image/png", MediaContentFixtures.PNG))

        assertTrue("folder" !in capturedOptions(), "a blank folder should not be sent")
    }

    @Test
    fun `never lets the client choose the storage path`() {
        stubSuccessfulUpload()

        storage.upload(MediaUpload("../../escape.png", "image/png", MediaContentFixtures.PNG))

        val options = capturedOptions()
        assertTrue("public_id" !in options)
        assertTrue("use_filename" !in options)
        assertTrue("eager" !in options)
    }

    @Test
    fun `ignores the content type claimed by the client`() {
        stubSuccessfulUpload()

        storage.upload(MediaUpload("latte.png", "text/html", MediaContentFixtures.PNG))

        assertTrue("content_type" !in capturedOptions())
    }

    @Test
    fun `maps the provider response to stored media`() {
        stubSuccessfulUpload(
            successfulResponse() + mapOf(
                "width" to 800.0,
                "height" to 600.0,
                "bytes" to 4096L
            )
        )

        val stored = storage.upload(
            MediaUpload("latte.png", "image/png", MediaContentFixtures.PNG)
        )

        assertEquals("premiumcoffee/latte", stored.publicId)
        assertEquals(
            "https://res.cloudinary.com/demo/image/upload/premiumcoffee/latte.jpg",
            stored.url
        )
        assertEquals(800, stored.width)
        assertEquals(600, stored.height)
        assertEquals(4096L, stored.sizeBytes)
    }

    @Test
    fun `prefers the secure url over the insecure one`() {
        stubSuccessfulUpload(
            successfulResponse() + mapOf(
                "url" to "http://res.cloudinary.com/demo/image/upload/premiumcoffee/latte.jpg"
            )
        )

        val stored = storage.upload(
            MediaUpload("latte.png", "image/png", MediaContentFixtures.PNG)
        )

        assertTrue(stored.url.startsWith("https://"))
    }

    @Test
    fun `fails when the provider does not return a public id`() {
        stubSuccessfulUpload(mapOf("secure_url" to "https://res.cloudinary.com/demo/latte.jpg"))

        assertThrows(MediaStorageException::class.java) {
            storage.upload(MediaUpload("latte.png", "image/png", MediaContentFixtures.PNG))
        }
    }

    @Test
    fun `fails when the provider does not return a delivery url`() {
        stubSuccessfulUpload(mapOf("public_id" to "premiumcoffee/latte"))

        assertThrows(MediaStorageException::class.java) {
            storage.upload(MediaUpload("latte.png", "image/png", MediaContentFixtures.PNG))
        }
    }

    @Test
    fun `reports an io failure as a storage exception`() {
        doThrow(IOException("Unrecognized file parameter java.io.ByteArrayInputStream@1b6d"))
            .`when`(uploader)
            .upload(any(), anyMap<String, Any>())

        val failure = assertThrows(MediaStorageException::class.java) {
            storage.upload(MediaUpload("photo.jpg", "image/jpeg", MediaContentFixtures.JPEG))
        }

        assertEquals("Could not upload 'photo.jpg' to Cloudinary", failure.message)
    }

    @Test
    fun `reports a rejected upload as a storage exception`() {
        doThrow(IllegalArgumentException("Invalid extension"))
            .`when`(uploader)
            .upload(any(), anyMap<String, Any>())

        val failure = assertThrows(MediaStorageException::class.java) {
            storage.upload(MediaUpload("photo.png", "image/png", MediaContentFixtures.PNG))
        }

        assertEquals("Cloudinary rejected the upload of 'photo.png'", failure.message)
    }

    @Test
    fun `deletes the remote file by its public id`() {
        storage.delete("premiumcoffee/latte")

        Mockito.verify(uploader).destroy("premiumcoffee/latte", emptyMap<String, Any>())
    }

    @Test
    fun `reports a failed remote delete as a storage exception`() {
        doThrow(IOException("timeout"))
            .`when`(uploader)
            .destroy(any(), anyMap<String, Any>())

        assertThrows(MediaStorageException::class.java) {
            storage.delete("premiumcoffee/latte")
        }
    }

    @Test
    fun `reports the configured provider`() {
        assertEquals(MediaProvider.CLOUDINARY, storage.provider)
    }

    private fun stubSuccessfulUpload(response: Map<String, Any> = successfulResponse()) {
        Mockito.`when`(uploader.upload(any(), anyMap<String, Any>())).thenReturn(response)
    }

    private fun successfulResponse(): Map<String, Any> = mapOf(
        "public_id" to "premiumcoffee/latte",
        "secure_url" to "https://res.cloudinary.com/demo/image/upload/premiumcoffee/latte.jpg"
    )

    private fun capturedFile(): ByteArray {
        val captor = ArgumentCaptor.forClass(ByteArray::class.java)
        Mockito.verify(uploader).upload(captor.capture(), anyMap<String, Any>())
        return captor.value
    }

    private fun capturedOptions(): Map<*, *> {
        val captor = ArgumentCaptor.forClass(Map::class.java)
        Mockito.verify(uploader).upload(any(), captor.capture())
        return captor.value
    }
}

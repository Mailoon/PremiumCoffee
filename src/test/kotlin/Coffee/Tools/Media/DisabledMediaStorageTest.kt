package Coffee.Tools.Media

import Coffee.MediaAsset.Model.MediaProvider
import Coffee.Tools.Media.Exceptions.MediaStorageNotConfiguredException
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class DisabledMediaStorageTest {

    private val storage = DisabledMediaStorage(MediaProvider.CLOUDINARY)

    @Test
    fun `exposes the provider it stands for`() {
        assertEquals(MediaProvider.CLOUDINARY, storage.provider)
    }

    @Test
    fun `fails the upload`() {
        val exception = assertThrows(MediaStorageNotConfiguredException::class.java) {
            storage.upload(MediaUpload("latte.jpg", "image/jpeg", byteArrayOf(1)))
        }

        assertEquals("Storage provider 'CLOUDINARY' is not configured", exception.message)
    }

    @Test
    fun `fails the deletion`() {
        assertThrows(MediaStorageNotConfiguredException::class.java) {
            storage.delete("premiumcoffee/latte")
        }
    }
}

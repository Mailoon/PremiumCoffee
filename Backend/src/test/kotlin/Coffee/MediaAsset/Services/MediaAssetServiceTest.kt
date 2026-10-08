package Coffee.MediaAsset.Services

import Coffee.Common.Exceptions.ResourceNotFoundException
import Coffee.MediaAsset.DTO.MediaAssetUrlResponse
import Coffee.MediaAsset.Model.MediaAsset
import Coffee.MediaAsset.Model.MediaAssetType
import Coffee.MediaAsset.Model.MediaProvider
import Coffee.MediaAsset.Repository.MediaAssetRepository
import Coffee.Tools.Media.Content.MediaContentFixtures
import Coffee.Tools.Media.Exceptions.MediaStorageException
import Coffee.Tools.Media.FakeMediaStorage
import Coffee.Tools.Media.MediaStorage
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.mockito.ArgumentCaptor
import org.mockito.Mockito
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.mock.web.MockMultipartFile
import org.springframework.web.server.ResponseStatusException
import java.util.Optional
import java.util.UUID

class MediaAssetServiceTest {

    private val repository = Mockito.mock(MediaAssetRepository::class.java)
    private val storage = FakeMediaStorage()
    private val service = MediaAssetService(repository, storage)

    @Test
    fun `stores the uploaded image and its provider metadata`() {
        storage.stored = storage.stored.copy(publicId = "premiumcoffee/latte", sizeBytes = 20480L)
        Mockito.`when`(repository.save(Mockito.any(MediaAsset::class.java)))
            .thenAnswer { it.getArgument(0) }

        val asset = service.upload(file("latte.png", "image/jpeg", MediaContentFixtures.PNG))

        val saved = capturedAsset()
        assertEquals(MediaAssetType.IMAGE, saved.assetType)
        assertEquals(MediaProvider.CLOUDINARY, saved.provider)
        assertEquals("premiumcoffee/latte", saved.storageKey)
        assertEquals("premiumcoffee/latte", saved.publicId)
        assertEquals(storage.stored.url, saved.url)
        assertEquals("image/png", saved.mimeType)
        assertEquals("latte.png", saved.fileName)
        assertEquals(20480L, saved.fileSizeBytes)
        assertEquals(800, saved.width)
        assertEquals(600, saved.height)
        assertEquals(saved.id, asset.id)
    }

    @Test
    fun `sends the raw bytes and the original file name to the storage port`() {
        Mockito.`when`(repository.save(Mockito.any(MediaAsset::class.java)))
            .thenAnswer { it.getArgument(0) }
        val content = MediaContentFixtures.PNG

        service.upload(file("latte.png", "image/jpeg", content))

        val uploaded = storage.uploaded.single()
        assertEquals("latte.png", uploaded.fileName)
        assertEquals("image/jpeg", uploaded.contentType)
        assertEquals(content.toList(), uploaded.content.toList())
    }

    @Test
    fun `falls back to the multipart size when the provider omits it`() {
        storage.stored = storage.stored.copy(sizeBytes = null)
        Mockito.`when`(repository.save(Mockito.any(MediaAsset::class.java)))
            .thenAnswer { it.getArgument(0) }
        val headerOnly = MediaContentFixtures.PNG.copyOfRange(0, 8)

        service.upload(file("latte.png", "image/png", headerOnly))

        assertEquals(8L, capturedAsset().fileSizeBytes)
    }

    @Test
    fun `stores a glb model as a 3d asset`() {
        stubSave()

        service.upload(file("milkshake.glb", "application/octet-stream", MediaContentFixtures.GLB))

        val saved = capturedAsset()
        assertEquals(MediaAssetType.MODEL_3D, saved.assetType)
        assertEquals("model/gltf-binary", saved.mimeType)
    }

    @Test
    fun `stores an svg as an svg asset`() {
        stubSave()

        service.upload(file("logo.svg", "image/svg+xml", MediaContentFixtures.SVG))

        val saved = capturedAsset()
        assertEquals(MediaAssetType.SVG, saved.assetType)
        assertEquals("image/svg+xml", saved.mimeType)
    }

    @Test
    fun `rejects a renamed text file before calling the storage port`() {
        val disguised = file("photo.jpg", "image/jpeg", MediaContentFixtures.SQL)

        val exception = assertThrows(ResponseStatusException::class.java) {
            service.upload(disguised)
        }

        assertEquals(400, exception.statusCode.value())
        assertEquals(
            "The uploaded file is not a supported image or 3D model",
            exception.reason
        )
        assertEquals(0, storage.uploaded.size)
        Mockito.verifyNoInteractions(repository)
    }

    @Test
    fun `keeps a blank original file name out of the asset`() {
        val anonymous = MockMultipartFile("file", null, "image/png", MediaContentFixtures.PNG)
        Mockito.`when`(repository.save(Mockito.any(MediaAsset::class.java)))
            .thenAnswer { it.getArgument(0) }

        service.upload(anonymous)

        assertNull(capturedAsset().fileName)
        assertEquals("upload", storage.uploaded.single().fileName)
    }

    @Test
    fun `keeps only the file name when the client sends a unix path`() {
        stubSave()

        service.upload(file("../../etc/passwd.jpg", "image/jpeg"))

        assertEquals("passwd.jpg", capturedAsset().fileName)
        assertEquals("passwd.jpg", storage.uploaded.single().fileName)
    }

    @Test
    fun `keeps only the file name when the client sends a windows path`() {
        stubSave()

        service.upload(file("C:\\Users\\admin\\secret\\latte.jpg", "image/jpeg"))

        assertEquals("latte.jpg", capturedAsset().fileName)
    }

    @Test
    fun `strips control characters from the file name`() {
        stubSave()

        service.upload(file("la\u0000tte\u001B.jpg", "image/jpeg"))

        assertEquals("latte.jpg", capturedAsset().fileName)
    }

    @Test
    fun `truncates a file name longer than the column allows`() {
        stubSave()
        val tooLong = "a".repeat(300) + ".jpg"

        service.upload(file(tooLong, "image/jpeg"))

        assertEquals(255, capturedAsset().fileName!!.length)
    }

    @Test
    fun `derives the mime type from the verified bytes and ignores the client claim`() {
        stubSave()

        service.upload(file("latte.png", "text/html", MediaContentFixtures.PNG))

        assertEquals("image/png", capturedAsset().mimeType)
    }

    @Test
    fun `rejects an empty file before calling the storage port`() {
        val empty = MockMultipartFile("file", "latte.jpg", "image/jpeg", ByteArray(0))

        val exception = assertThrows(ResponseStatusException::class.java) {
            service.upload(empty)
        }

        assertEquals(400, exception.statusCode.value())
        assertEquals(0, storage.uploaded.size)
    }

    @Test
    fun `does not persist anything when the storage port fails`() {
        storage.failure = MediaStorageException("Cloudinary rejected the upload of 'latte.jpg'")

        assertThrows(MediaStorageException::class.java) {
            service.upload(file("latte.png", "image/png"))
        }

        Mockito.verifyNoInteractions(repository)
    }

    @Test
    fun `returns the url already stored in the asset without calling the provider`() {
        val id = java.util.UUID.randomUUID()
        stubFindById(id, asset(provider = MediaProvider.CLOUDINARY))

        val result = service.storedUrl(id)

        assertEquals(
            MediaAssetUrlResponse("premiumcoffee/latte", "https://cdn.test/premiumcoffee/latte.jpg"),
            result
        )
        assertEquals(0, storage.uploaded.size)
        assertEquals(0, storage.deleted.size)
    }

    @Test
    fun `falls back to the storage key when the public id is missing`() {
        val id = java.util.UUID.randomUUID()
        stubFindById(
            id,
            asset(
                provider = MediaProvider.MINIO,
                publicId = null,
                url = "https://cdn.test/variants/latte/base.glb"
            )
        )

        assertEquals("variants/latte/base.glb", service.storedUrl(id).publicId)
    }

    @Test
    fun `fails to resolve the url of an asset that has none stored`() {
        val id = java.util.UUID.randomUUID()
        stubFindById(id, asset(url = null))

        assertThrows(ResourceNotFoundException::class.java) { service.storedUrl(id) }
    }

    @Test
    fun `fails to resolve the url of a missing asset`() {
        val id = java.util.UUID.randomUUID()
        Mockito.`when`(repository.findById(id)).thenReturn(Optional.empty())

        assertThrows(ResourceNotFoundException::class.java) { service.storedUrl(id) }
    }

    @Test
    fun `deletes the row before touching the remote file`() {
        val id = java.util.UUID.randomUUID()
        val mediaStorage = mockStorage()
        stubFindById(id, asset())
        val order = mutableListOf<String>()
        recordRepositoryCalls(order, mediaStorage)

        MediaAssetService(repository, mediaStorage).delete(id)

        val inOrder = Mockito.inOrder(repository, mediaStorage)
        inOrder.verify(repository).delete(Mockito.any(MediaAsset::class.java))
        inOrder.verify(repository).flush()
        inOrder.verify(mediaStorage).delete("premiumcoffee/latte")
        assertEquals(listOf("delete", "flush", "remote"), order)
    }

    @Test
    fun `keeps the remote file when the row is still referenced`() {
        val id = java.util.UUID.randomUUID()
        val mediaStorage = mockStorage()
        stubFindById(id, asset())
        Mockito.doThrow(DataIntegrityViolationException("fk violation")).`when`(repository).flush()

        assertThrows(DataIntegrityViolationException::class.java) {
            MediaAssetService(repository, mediaStorage).delete(id)
        }

        Mockito.verify(mediaStorage, Mockito.never()).delete(Mockito.anyString())
    }

    @Test
    fun `leaves the remote provider alone when the row belongs to another one`() {
        val id = java.util.UUID.randomUUID()
        val mediaStorage = mockStorage()
        stubFindById(id, asset(provider = MediaProvider.MINIO))

        MediaAssetService(repository, mediaStorage).delete(id)

        Mockito.verify(repository).flush()
        Mockito.verify(mediaStorage, Mockito.never()).delete(Mockito.anyString())
    }

    @Test
    fun `skips the remote deletion when the row has no public id`() {
        val id = java.util.UUID.randomUUID()
        val mediaStorage = mockStorage()
        stubFindById(id, asset(publicId = null))

        MediaAssetService(repository, mediaStorage).delete(id)

        Mockito.verify(mediaStorage, Mockito.never()).delete(Mockito.anyString())
    }

    @Test
    fun `reports a remote failure after the row is already gone`() {
        val id = java.util.UUID.randomUUID()
        val mediaStorage = mockStorage()
        stubFindById(id, asset())
        Mockito.doThrow(MediaStorageException("Cloudinary rejected the deletion"))
            .`when`(mediaStorage).delete(Mockito.anyString())

        assertThrows(MediaStorageException::class.java) {
            MediaAssetService(repository, mediaStorage).delete(id)
        }

        Mockito.verify(repository).delete(Mockito.any(MediaAsset::class.java))
    }

    @Test
    fun `fails to delete a missing asset without calling the provider`() {
        val id = java.util.UUID.randomUUID()
        val mediaStorage = mockStorage()
        Mockito.`when`(repository.findById(id)).thenReturn(Optional.empty())

        assertThrows(ResourceNotFoundException::class.java) {
            MediaAssetService(repository, mediaStorage).delete(id)
        }

        Mockito.verify(mediaStorage, Mockito.never()).delete(Mockito.anyString())
    }

    private fun mockStorage(): MediaStorage {
        val mediaStorage = Mockito.mock(MediaStorage::class.java)
        Mockito.`when`(mediaStorage.provider).thenReturn(MediaProvider.CLOUDINARY)
        return mediaStorage
    }

    private fun recordRepositoryCalls(order: MutableList<String>, mediaStorage: MediaStorage) {
        Mockito.doAnswer { order.add("delete"); null }.`when`(repository).delete(Mockito.any())
        Mockito.doAnswer {
            order.add("flush")
            null
        }.`when`(repository).flush()
        Mockito.doAnswer { order.add("remote"); null }
            .`when`(mediaStorage).delete(Mockito.anyString())
    }

    private fun stubSave() {
        Mockito.`when`(repository.save(Mockito.any(MediaAsset::class.java)))
            .thenAnswer { it.getArgument(0) }
    }

    private fun stubFindById(id: UUID, asset: MediaAsset) {
        Mockito.`when`(repository.findById(id)).thenReturn(Optional.of(asset))
    }

    private fun asset(
        provider: MediaProvider = MediaProvider.CLOUDINARY,
        publicId: String? = "premiumcoffee/latte",
        url: String? = "https://cdn.test/premiumcoffee/latte.jpg"
    ) = MediaAsset(
        assetType = MediaAssetType.IMAGE,
        provider = provider,
        storageKey = publicId ?: "variants/latte/base.glb",
        publicId = publicId,
        url = url
    )

    private fun file(
        name: String,
        contentType: String = "image/png",
        content: ByteArray = MediaContentFixtures.PNG
    ) = MockMultipartFile("file", name, contentType, content)

    private fun capturedAsset(): MediaAsset {
        val captor = ArgumentCaptor.forClass(MediaAsset::class.java)
        Mockito.verify(repository).save(captor.capture())
        return captor.value
    }
}

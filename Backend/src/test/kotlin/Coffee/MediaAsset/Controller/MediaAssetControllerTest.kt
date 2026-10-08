package Coffee.MediaAsset.Controller

import Coffee.Common.Controllers.ApiExceptionHandler
import Coffee.MediaAsset.DTO.MediaAssetUrlResponse
import Coffee.MediaAsset.Model.MediaAsset
import Coffee.MediaAsset.Model.MediaAssetType
import Coffee.MediaAsset.Model.MediaProvider
import Coffee.MediaAsset.Repository.MediaAssetRepository
import Coffee.MediaAsset.Services.MediaAssetService
import Coffee.Tools.Media.Content.MediaContentFixtures
import Coffee.Tools.Media.Exceptions.MediaStorageException
import Coffee.Tools.Media.Exceptions.MediaStorageNotConfiguredException
import Coffee.Tools.Media.FakeMediaStorage
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.springframework.http.MediaType
import org.springframework.mock.web.MockMultipartFile
import org.springframework.test.web.servlet.RequestBuilder
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.web.ErrorResponse
import java.util.Optional
import java.util.UUID

class MediaAssetControllerTest {

    private val repository = Mockito.mock(MediaAssetRepository::class.java)
    private val storage = FakeMediaStorage()
    private val service = MediaAssetService(repository, storage)

    @Test
    fun `uploads an image and returns the stored asset`() {
        storage.stored = storage.stored.copy(
            publicId = "premiumcoffee/latte",
            url = "https://res.cloudinary.com/demo/image/upload/premiumcoffee/latte.jpg",
            width = 1024,
            height = 768,
            sizeBytes = 20480L
        )
        Mockito.`when`(repository.save(Mockito.any(MediaAsset::class.java)))
            .thenAnswer { it.getArgument(0) }
        val file = MockMultipartFile("file", "latte.png", "image/jpeg", MediaContentFixtures.PNG)

        mockMvc()
            .perform(multipart("/media-assets/upload").file(file))
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.assetType").value("IMAGE"))
            .andExpect(jsonPath("$.provider").value("CLOUDINARY"))
            .andExpect(jsonPath("$.storageKey").value("premiumcoffee/latte"))
            .andExpect(jsonPath("$.publicId").value("premiumcoffee/latte"))
            .andExpect(
                jsonPath("$.url").value("https://res.cloudinary.com/demo/image/upload/premiumcoffee/latte.jpg")
            )
            .andExpect(jsonPath("$.fileName").value("latte.png"))
            .andExpect(jsonPath("$.fileSizeBytes").value(20480))
            .andExpect(jsonPath("$.width").value(1024))
            .andExpect(jsonPath("$.height").value(768))

        val uploaded = storage.uploaded.single()
        assertEquals("latte.png", uploaded.fileName)
        assertEquals("image/jpeg", uploaded.contentType)
        assertEquals(MediaContentFixtures.PNG.toList(), uploaded.content.toList())
    }

    @Test
    fun `uploads a glb model and returns it as a 3d asset`() {
        storage.stored = storage.stored.copy(
            publicId = "premiumcoffee/milkshake",
            url = "https://res.cloudinary.com/demo/image/upload/premiumcoffee/milkshake.glb",
            width = null,
            height = null
        )
        Mockito.`when`(repository.save(Mockito.any(MediaAsset::class.java)))
            .thenAnswer { it.getArgument(0) }

        mockMvc()
            .perform(
                multipart("/media-assets/upload").file(
                    MockMultipartFile("file", "milkshake.glb", null, MediaContentFixtures.GLB)
                )
            )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.assetType").value("MODEL_3D"))
            .andExpect(jsonPath("$.mimeType").value("model/gltf-binary"))
            .andExpect(
                jsonPath("$.url").value(
                    "https://res.cloudinary.com/demo/image/upload/premiumcoffee/milkshake.glb"
                )
            )
    }

    @Test
    fun `answers 400 for a file that only pretends to be an image`() {
        mockMvc()
            .perform(
                multipart("/media-assets/upload").file(
                    MockMultipartFile("file", "photo.jpg", "image/jpeg", MediaContentFixtures.SQL)
                )
            )
            .andExpect(status().isBadRequest)
            .andExpect(
                jsonPath("$.detail").value("The uploaded file is not a supported image or 3D model")
            )

        assertEquals(0, storage.uploaded.size)
    }

    @Test
    fun `answers 503 when no storage provider is configured`() {
        storage.failure = MediaStorageNotConfiguredException("CLOUDINARY")
        val file = MockMultipartFile("file", "latte.png", "image/png", MediaContentFixtures.PNG)

        mockMvc()
            .perform(multipart("/media-assets/upload").file(file))
            .andExpect(status().isServiceUnavailable)
            .andExpect(jsonPath("$.detail").value("Storage provider 'CLOUDINARY' is not configured"))
    }

    @Test
    fun `answers 502 when the provider fails`() {
        storage.failure = MediaStorageException("Cloudinary rejected the upload of 'latte.png'")
        val file = MockMultipartFile("file", "latte.png", "image/png", MediaContentFixtures.PNG)

        mockMvc()
            .perform(multipart("/media-assets/upload").file(file))
            .andExpect(status().isBadGateway)
            .andExpect(jsonPath("$.detail").value("Cloudinary rejected the upload of 'latte.png'"))
    }

    @Test
    fun `answers 400 for an empty file`() {
        mockMvc()
            .perform(
                multipart("/media-assets/upload")
                    .file(MockMultipartFile("file", "latte.jpg", "image/jpeg", ByteArray(0)))
            )
            .andExpect(status().isBadRequest)

        assertEquals(0, storage.uploaded.size)
    }

    @Test
    fun `returns the url already stored for an asset`() {
        val id = UUID.randomUUID()
        Mockito.`when`(repository.findById(id)).thenReturn(
            Optional.of(
                MediaAsset(
                    assetType = MediaAssetType.IMAGE,
                    provider = MediaProvider.CLOUDINARY,
                    storageKey = "premiumcoffee/latte",
                    publicId = "premiumcoffee/latte",
                    url = "https://res.cloudinary.com/demo/image/upload/premiumcoffee/latte.jpg"
                )
            )
        )

        mockMvc()
            .perform(get("/media-assets/$id/url"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.publicId").value("premiumcoffee/latte"))
            .andExpect(
                jsonPath("$.url").value("https://res.cloudinary.com/demo/image/upload/premiumcoffee/latte.jpg")
            )

        assertEquals(0, storage.deleted.size)
    }

    @Test
    fun `answers 404 for the url of a missing asset`() {
        val id = UUID.randomUUID()
        Mockito.`when`(repository.findById(id)).thenReturn(Optional.empty())

        mockMvc()
            .perform(get("/media-assets/$id/url"))
            .andExpect(status().isNotFound)
    }

    @Test
    fun `does not accept a create so the client cannot forge a row`() {
        val status = statusOf(
            post("/media-assets")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "assetType": "IMAGE",
                      "provider": "MINIO",
                      "storageKey": "../../etc/passwd",
                      "url": "javascript:alert(1)",
                      "mimeType": "text/html"
                    }
                    """.trimIndent()
                )
        )

        assertEquals(405, status, "POST /media-assets must not stay mapped")
        Mockito.verifyNoInteractions(repository)
    }

    @Test
    fun `does not accept an update so the client cannot redirect a stored url`() {
        val id = UUID.randomUUID()
        val status = statusOf(
            put("/media-assets/$id")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """{"assetType":"IMAGE","provider":"CLOUDINARY","storageKey":"a","url":"https://evil.test/x.png"}"""
                )
        )

        assertEquals(405, status, "PUT /media-assets/{id} must not stay mapped")
        Mockito.verifyNoInteractions(repository)
    }

    @Test
    fun `deletes the row and the remote file`() {
        val id = UUID.randomUUID()
        Mockito.`when`(repository.findById(id)).thenReturn(
            Optional.of(
                MediaAsset(
                    assetType = MediaAssetType.IMAGE,
                    provider = MediaProvider.CLOUDINARY,
                    storageKey = "premiumcoffee/latte",
                    publicId = "premiumcoffee/latte",
                    url = "https://res.cloudinary.com/demo/image/upload/premiumcoffee/latte.jpg"
                )
            )
        )

        mockMvc()
            .perform(delete("/media-assets/$id"))
            .andExpect(status().isNoContent)

        assertEquals(listOf("premiumcoffee/latte"), storage.deleted)
    }

    /**
     * Standalone MockMvc may either resolve the unmapped-method error or rethrow it,
     * depending on how the dispatcher is configured, so read the status from whichever
     * side reports it.
     */
    private fun statusOf(request: RequestBuilder): Int =
        try {
            mockMvc().perform(request).andReturn().response.status
        } catch (exception: Exception) {
            (exception as? ErrorResponse)?.statusCode?.value()
                ?: (exception.cause as? ErrorResponse)?.statusCode?.value()
                ?: -1
        }

    private fun mockMvc() =
        MockMvcBuilders
            .standaloneSetup(MediaAssetController(service))
            .setControllerAdvice(ApiExceptionHandler())
            .build()
}

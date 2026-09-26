package Coffee.Tools.Media.Content

import org.springframework.mock.web.MockMultipartFile

object MediaContentFixtures {

    val PNG: ByteArray = byteArrayOf(
        0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 9, 8, 7
    )

    val JPEG: ByteArray = byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte(), 0xE0.toByte(), 1, 2)

    val GIF: ByteArray = "GIF89a".toByteArray(Charsets.ISO_8859_1) + byteArrayOf(1, 0, 1, 0)

    val WEBP: ByteArray = "RIFF".toByteArray(Charsets.ISO_8859_1) +
        byteArrayOf(0x1A, 0, 0, 0) +
        "WEBPVP8 ".toByteArray(Charsets.ISO_8859_1) +
        byteArrayOf(1, 2, 3)

    val AVIF: ByteArray = isoBaseMedia("avif")

    val HEIC: ByteArray = isoBaseMedia("heic")

    val SVG: ByteArray = (
        "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
            "<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"10\" height=\"10\"></svg>"
        ).toByteArray()

    val GLB: ByteArray = "glTF".toByteArray(Charsets.ISO_8859_1) + byteArrayOf(2, 0, 0, 0, 0x50)

    val SQL: ByteArray = "DROP TABLE users; --\nSELECT * FROM pg_shadow;".toByteArray()

    fun file(
        name: String,
        content: ByteArray = PNG,
        contentType: String? = "image/png"
    ) = MockMultipartFile("file", name, contentType, content)

    fun png() = file("latte.png")

    private fun isoBaseMedia(brand: String): ByteArray =
        byteArrayOf(0, 0, 0, 0x20) +
            "ftyp".toByteArray(Charsets.ISO_8859_1) +
            brand.toByteArray(Charsets.ISO_8859_1) +
            byteArrayOf(0, 0, 0, 0)
}

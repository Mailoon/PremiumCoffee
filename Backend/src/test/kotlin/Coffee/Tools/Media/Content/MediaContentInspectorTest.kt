package Coffee.Tools.Media.Content

import Coffee.MediaAsset.Model.MediaAssetType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.junit.jupiter.params.provider.ValueSource

class MediaContentInspectorTest {

    private val inspector = MediaContentInspector()

    @ParameterizedTest(name = "recognizes {0}")
    @MethodSource("supportedHeaders")
    fun recognizesTheFormatFromTheMagicBytes(
        header: ByteArray,
        expected: DetectedFormat
    ) {
        assertEquals(expected, inspector.inspect(header))
    }

    @ParameterizedTest(name = "maps {0} to {1} and {2}")
    @MethodSource("mimeAndAssetTypeMapping")
    fun mapsEveryFormatToItsMimeTypeAndAssetType(
        header: ByteArray,
        expectedFormat: DetectedFormat,
        expectedMimeType: String,
        expectedAssetType: MediaAssetType
    ) {
        val detected = inspector.inspect(header)

        assertEquals(expectedFormat, detected)
        assertEquals(expectedMimeType, detected?.mimeType)
        assertEquals(expectedAssetType, detected?.assetType)
    }

    @Test
    fun rejectsTextThatOnlyPretendsToBeAnImage() {
        assertNull(inspector.inspect(MediaContentFixtures.SQL))
    }

    @Test
    fun rejectsAnEmptyHeader() {
        assertNull(inspector.inspect(ByteArray(0)))
    }

    @ParameterizedTest(name = "rejects the truncated {0} header")
    @ValueSource(ints = [0, 1, 2, 3])
    fun rejectsAHeaderShorterThanTheSignature(length: Int) {
        assertNull(inspector.inspect(MediaContentFixtures.PNG.copyOfRange(0, length)))
    }

    @Test
    fun rejectsARiffContainerThatIsNotWebp() {
        val wav = "RIFF".toByteArray(Charsets.ISO_8859_1) +
            byteArrayOf(0x24, 0, 0, 0) +
            "WAVEfmt ".toByteArray(Charsets.ISO_8859_1) +
            byteArrayOf(1, 2, 3)

        assertNull(inspector.inspect(wav))
    }

    @Test
    fun rejectsAnIsoBaseMediaFileWithAnUnknownBrand() {
        assertNull(inspector.inspect(isoBaseMedia("mp42")))
    }

    @Test
    fun rejectsXmlThatIsNotSvg() {
        assertNull(
            inspector.inspect("<?xml version=\"1.0\"?><rss version=\"2.0\"></rss>".toByteArray())
        )
    }

    @Test
    fun rejectsSvgMentionedOnlyInsideAnotherDocument() {
        assertNull(
            inspector.inspect("<!-- <svg viewBox=\"0 0 10 10\"></svg> -->".toByteArray())
        )
    }

    @Test
    fun acceptsSvgBehindAByteOrderMarkAndWhitespace() {
        val content = "\uFEFF  \n<svg xmlns=\"http://www.w3.org/2000/svg\"></svg>".toByteArray()

        assertEquals(DetectedFormat.SVG, inspector.inspect(content))
    }

    @Test
    fun acceptsSvgWithoutAnXmlDeclaration() {
        assertEquals(
            DetectedFormat.SVG,
            inspector.inspect("<svg xmlns=\"http://www.w3.org/2000/svg\"></svg>".toByteArray())
        )
    }

    @Test
    fun readsTheFormatFromTheBeginningOfARealisticFileSize() {
        val png = MediaContentFixtures.PNG + ByteArray(2048) { 0x42 }

        assertEquals(DetectedFormat.PNG, inspector.inspect(png))
    }

    companion object {
        @JvmStatic
        fun supportedHeaders(): List<Arguments> = listOf(
            Arguments.of(MediaContentFixtures.JPEG, DetectedFormat.JPEG),
            Arguments.of(MediaContentFixtures.PNG, DetectedFormat.PNG),
            Arguments.of(MediaContentFixtures.GIF, DetectedFormat.GIF),
            Arguments.of(MediaContentFixtures.WEBP, DetectedFormat.WEBP),
            Arguments.of(MediaContentFixtures.AVIF, DetectedFormat.AVIF),
            Arguments.of(MediaContentFixtures.HEIC, DetectedFormat.HEIC),
            Arguments.of(MediaContentFixtures.SVG, DetectedFormat.SVG),
            Arguments.of(MediaContentFixtures.GLB, DetectedFormat.GLB)
        )

        @JvmStatic
        fun mimeAndAssetTypeMapping(): List<Arguments> = listOf(
            Arguments.of(MediaContentFixtures.JPEG, DetectedFormat.JPEG, "image/jpeg", MediaAssetType.IMAGE),
            Arguments.of(MediaContentFixtures.PNG, DetectedFormat.PNG, "image/png", MediaAssetType.IMAGE),
            Arguments.of(MediaContentFixtures.GIF, DetectedFormat.GIF, "image/gif", MediaAssetType.IMAGE),
            Arguments.of(MediaContentFixtures.WEBP, DetectedFormat.WEBP, "image/webp", MediaAssetType.IMAGE),
            Arguments.of(MediaContentFixtures.AVIF, DetectedFormat.AVIF, "image/avif", MediaAssetType.IMAGE),
            Arguments.of(MediaContentFixtures.HEIC, DetectedFormat.HEIC, "image/heic", MediaAssetType.IMAGE),
            Arguments.of(MediaContentFixtures.SVG, DetectedFormat.SVG, "image/svg+xml", MediaAssetType.SVG),
            Arguments.of(
                MediaContentFixtures.GLB,
                DetectedFormat.GLB,
                "model/gltf-binary",
                MediaAssetType.MODEL_3D
            )
        )

        private fun isoBaseMedia(brand: String): ByteArray =
            byteArrayOf(0, 0, 0, 0x20) +
                "ftyp".toByteArray(Charsets.ISO_8859_1) +
                brand.toByteArray(Charsets.ISO_8859_1) +
                byteArrayOf(0, 0, 0, 0)
    }
}

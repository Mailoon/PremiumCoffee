package Coffee.Tools.Media.Content

class MediaContentInspector {

    fun inspect(header: ByteArray): DetectedFormat? = when {
        startsWith(header, JPEG) -> DetectedFormat.JPEG
        startsWith(header, PNG) -> DetectedFormat.PNG
        startsWith(header, GIF) -> DetectedFormat.GIF
        startsWith(header, RIFF) && matchesAt(header, WEBP, 8) -> DetectedFormat.WEBP
        isIsoBaseMedia(header, AVIF_BRANDS) -> DetectedFormat.AVIF
        isIsoBaseMedia(header, HEIC_BRANDS) -> DetectedFormat.HEIC
        startsWith(header, GLB) -> DetectedFormat.GLB
        isSvg(header) -> DetectedFormat.SVG
        else -> null
    }

    private fun startsWith(header: ByteArray, prefix: ByteArray): Boolean =
        matchesAt(header, prefix, 0)

    private fun matchesAt(header: ByteArray, expected: ByteArray, offset: Int): Boolean {
        if (header.size < offset + expected.size) {
            return false
        }
        return expected.indices.all { header[offset + it] == expected[it] }
    }

    private fun isIsoBaseMedia(header: ByteArray, brands: Set<String>): Boolean {
        if (!matchesAt(header, FTYP, 4) || header.size < 12) {
            return false
        }
        return brands.contains(String(header, 8, 4, Charsets.ISO_8859_1))
    }

    private fun isSvg(header: ByteArray): Boolean {
        if (header.isEmpty()) {
            return false
        }
        val text = String(header, Charsets.UTF_8).trimStart('\uFEFF', ' ', '\t', '\r', '\n')
        return when {
            text.startsWith("<svg") -> true
            text.startsWith("<?xml") -> text.contains("<svg")
            else -> false
        }
    }

    companion object {
        const val HEADER_SIZE = 512

        private val JPEG = byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte())
        private val PNG = byteArrayOf(
            0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A
        )
        private val GIF = ascii("GIF8")
        private val RIFF = ascii("RIFF")
        private val WEBP = ascii("WEBP")
        private val FTYP = ascii("ftyp")
        private val GLB = ascii("glTF")
        private val AVIF_BRANDS = setOf("avif", "avis")
        private val HEIC_BRANDS = setOf(
            "heic", "heix", "heim", "heis", "hevc", "hevx", "hevm", "hevs", "mif1", "msf1"
        )

        private fun ascii(value: String): ByteArray = value.toByteArray(Charsets.ISO_8859_1)
    }
}

package Coffee.Tools.Media

data class StoredMedia(
    val publicId: String,
    val url: String,
    val width: Int? = null,
    val height: Int? = null,
    val sizeBytes: Long? = null
)

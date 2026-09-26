package Coffee.Tools.Media.Cloudinary

data class CloudinarySettings(
    val cloudName: String,
    val apiKey: String,
    val apiSecret: String,
    val folder: String = DEFAULT_FOLDER,
    val secure: Boolean = true
) {

    fun toConfigMap(): Map<String, Any> = mapOf(
        "cloud_name" to cloudName,
        "api_key" to apiKey,
        "api_secret" to apiSecret,
        "secure" to secure
    )

    companion object {
        const val DEFAULT_FOLDER = "premiumcoffee"
    }
}

package Coffee.Tools.Media

import Coffee.MediaAsset.Model.MediaProvider

class FakeMediaStorage(
    override val provider: MediaProvider = MediaProvider.CLOUDINARY
) : MediaStorage {

    val uploaded = mutableListOf<MediaUpload>()
    val deleted = mutableListOf<String>()

    var stored: StoredMedia = StoredMedia(
        publicId = "premiumcoffee/fake",
        url = "https://res.cloudinary.com/fake/image/upload/premiumcoffee/fake.jpg",
        width = 800,
        height = 600,
        sizeBytes = 12345
    )

    var failure: RuntimeException? = null

    override fun upload(media: MediaUpload): StoredMedia {
        uploaded.add(media)
        failure?.let { throw it }
        return stored
    }

    override fun delete(publicId: String) {
        deleted.add(publicId)
        failure?.let { throw it }
    }
}

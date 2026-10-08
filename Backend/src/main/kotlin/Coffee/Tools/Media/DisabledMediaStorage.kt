package Coffee.Tools.Media

import Coffee.MediaAsset.Model.MediaProvider
import Coffee.Tools.Media.Exceptions.MediaStorageNotConfiguredException

class DisabledMediaStorage(
    override val provider: MediaProvider
) : MediaStorage {

    override fun upload(media: MediaUpload): StoredMedia {
        throw notConfigured()
    }

    override fun delete(publicId: String) {
        throw notConfigured()
    }

    private fun notConfigured(): MediaStorageNotConfiguredException =
        MediaStorageNotConfiguredException(provider.name)
}

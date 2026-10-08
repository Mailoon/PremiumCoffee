package Coffee.Tools.Media

import Coffee.MediaAsset.Model.MediaProvider

interface MediaStorage {

    val provider: MediaProvider

    fun upload(media: MediaUpload): StoredMedia

    fun delete(publicId: String)
}

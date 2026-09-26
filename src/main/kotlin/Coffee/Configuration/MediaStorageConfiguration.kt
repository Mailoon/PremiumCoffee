package Coffee.Configuration

import Coffee.MediaAsset.Model.MediaProvider
import Coffee.Tools.Media.Cloudinary.CloudinaryMediaStorage
import Coffee.Tools.Media.Cloudinary.CloudinarySettings
import Coffee.Tools.Media.DisabledMediaStorage
import Coffee.Tools.Media.MediaStorage
import com.cloudinary.Cloudinary
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class MediaStorageConfiguration {

    @Bean
    @ConditionalOnProperty(prefix = "media.cloudinary", name = ["enabled"], havingValue = "true")
    fun cloudinaryMediaStorage(
        @Value($$"${media.cloudinary.cloud-name}") cloudName: String,
        @Value($$"${media.cloudinary.api-key}") apiKey: String,
        @Value($$"${media.cloudinary.api-secret}") apiSecret: String,
        @Value($$"${media.cloudinary.folder}") folder: String
    ): MediaStorage {
        val settings = CloudinarySettings(
            cloudName = cloudName,
            apiKey = apiKey,
            apiSecret = apiSecret,
            folder = folder
        )
        return CloudinaryMediaStorage(Cloudinary(settings.toConfigMap()), settings)
    }

    @Bean
    @ConditionalOnProperty(
        prefix = "media.cloudinary",
        name = ["enabled"],
        havingValue = "false",
        matchIfMissing = true
    )
    fun disabledMediaStorage(): MediaStorage = DisabledMediaStorage(MediaProvider.CLOUDINARY)

    companion object {
        private const val PROVIDER = "Cloudinary"
    }
}

package Coffee.Configuration

import Coffee.MediaAsset.Model.MediaProvider
import Coffee.Tools.Media.Cloudinary.CloudinaryMediaStorage
import Coffee.Tools.Media.Cloudinary.CloudinarySettings
import Coffee.Tools.Media.DisabledMediaStorage
import Coffee.Tools.Media.Local.LocalMediaStorage
import Coffee.Tools.Media.MediaStorage
import com.cloudinary.Cloudinary
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.nio.file.Path

@Configuration
class MediaStorageConfiguration {

    @Bean
    fun mediaStorage(
        @Value($$"${media.local.enabled:false}") localEnabled: Boolean,
        @Value($$"${media.local.root:#{null}}") localRoot: String?,
        @Value($$"${media.local.public-path:/media}") localPublicPath: String,
        @Value($$"${media.cloudinary.enabled:false}") cloudinaryEnabled: Boolean,
        @Value($$"${media.cloudinary.cloud-name:}") cloudName: String,
        @Value($$"${media.cloudinary.api-key:}") apiKey: String,
        @Value($$"${media.cloudinary.api-secret:}") apiSecret: String,
        @Value($$"${media.cloudinary.folder:premiumcoffee}") folder: String
    ): MediaStorage = when {
        localEnabled && !localRoot.isNullOrBlank() ->
            LocalMediaStorage(Path.of(localRoot), localPublicPath)

        cloudinaryEnabled -> {
            val settings = CloudinarySettings(
                cloudName = cloudName,
                apiKey = apiKey,
                apiSecret = apiSecret,
                folder = folder
            )
            CloudinaryMediaStorage(Cloudinary(settings.toConfigMap()), settings)
        }

        else -> DisabledMediaStorage(MediaProvider.LOCAL)
    }
}
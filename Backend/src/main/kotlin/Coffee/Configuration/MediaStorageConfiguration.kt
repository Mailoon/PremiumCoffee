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

/**
 * Declares the single [MediaStorage] bean.
 *
 * This is one method with an explicit `when` on purpose. With two
 * `@ConditionalOnProperty` methods there would be a real hazard: enabling both
 * providers would register two beans of the same type and the context would
 * fail to start, or worse, pick one silently. Spring also makes no promise about
 * the order in which `@Bean` methods of the same class are evaluated, so
 * "declared first wins" is not a rule one can rely on. Deciding precedence here
 * makes it explicit and testable.
 *
 * Precedence: LOCAL, then CLOUDINARY, then the disabled fallback that lets the
 * application always start.
 */
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
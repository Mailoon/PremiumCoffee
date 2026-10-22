package Coffee.Configuration

import Coffee.Tools.Media.Cloudinary.CloudinaryMediaStorage
import Coffee.Tools.Media.DisabledMediaStorage
import Coffee.Tools.Media.MediaStorage
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.runner.ApplicationContextRunner

class MediaStorageConfigurationTest {

    private val contextRunner = ApplicationContextRunner()
        .withUserConfiguration(MediaStorageConfiguration::class.java)

    @Test
    fun `falls back to a disabled storage when cloudinary is not enabled`() {
        contextRunner.run { context ->
            assertThat(context).hasSingleBean(MediaStorage::class.java)
            assertThat(context.getBean(MediaStorage::class.java))
                .isInstanceOf(DisabledMediaStorage::class.java)
        }
    }

    @Test
    fun `creates the cloudinary adapter when it is enabled`() {
        contextRunner
            .withPropertyValues(
                "media.cloudinary.enabled=true",
                "media.cloudinary.cloud-name=demo",
                "media.cloudinary.api-key=key",
                "media.cloudinary.api-secret=secret",
                "media.cloudinary.folder=premiumcoffee"
            )
            .run { context ->
                assertThat(context).hasSingleBean(MediaStorage::class.java)
                assertThat(context.getBean(MediaStorage::class.java))
                    .isInstanceOf(CloudinaryMediaStorage::class.java)
            }
    }
}

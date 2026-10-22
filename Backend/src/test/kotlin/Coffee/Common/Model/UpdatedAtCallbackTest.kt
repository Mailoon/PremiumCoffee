package Coffee.Common.Model

import Coffee.Category.Model.Category
import Coffee.Product.Model.Product
import Coffee.ProductVariant.Model.ProductVariant
import jakarta.persistence.PreUpdate
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.lang.reflect.Method
import java.time.Instant

/**
 * `updated_at` is NOT NULL in PostgreSQL and used to stay frozen at the creation
 * instant, because nothing refreshed it. Hibernate only calls the callback below
 * while it is actually annotated, so the reflection assertion pins the wiring and
 * the direct call pins the behaviour.
 */
class UpdatedAtCallbackTest {

    @Test
    fun `category advances updatedAt and keeps the update callback registered`() {
        val category = Category(name = "Coffee", slug = "coffee", updatedAt = OLD)

        category.touchUpdatedAt()

        assertTrue(category.updatedAt.isAfter(OLD), "expected updatedAt to move forward")
        assertRegisteredForUpdate(Category::class.java, "touchUpdatedAt")
    }

    @Test
    fun `product advances updatedAt and keeps the update callback registered`() {
        val product = Product(
            category = Category(name = "Coffee", slug = "coffee"),
            name = "Latte",
            slug = "latte",
            updatedAt = OLD
        )

        product.touchUpdatedAt()

        assertTrue(product.updatedAt.isAfter(OLD), "expected updatedAt to move forward")
        assertRegisteredForUpdate(Product::class.java, "touchUpdatedAt")
    }

    @Test
    fun `product variant advances updatedAt and keeps the update callback registered`() {
        val variant = ProductVariant(
            product = Product(
                category = Category(name = "Coffee", slug = "coffee"),
                name = "Latte",
                slug = "latte"
            ),
            name = "12 oz",
            sku = "LATTE-12",
            updatedAt = OLD
        )

        variant.touchUpdatedAt()

        assertTrue(variant.updatedAt.isAfter(OLD), "expected updatedAt to move forward")
        assertRegisteredForUpdate(ProductVariant::class.java, "touchUpdatedAt")
    }

    private fun assertRegisteredForUpdate(entity: Class<*>, callback: String) {
        val method: Method = entity.getMethod(callback)
        assertNotNull(
            method.getAnnotation(PreUpdate::class.java),
            "$entity#$callback must carry @PreUpdate or Hibernate will never call it"
        )
    }

    private companion object {
        private val OLD: Instant = Instant.parse("2020-01-01T00:00:00Z")
    }
}
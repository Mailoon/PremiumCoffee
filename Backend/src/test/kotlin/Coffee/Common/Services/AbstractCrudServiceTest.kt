package Coffee.Common.Services

import Coffee.Common.Exceptions.ResourceNotFoundException
import Coffee.Common.Models.PageQuery
import Coffee.Common.Models.PageResult
import Coffee.Common.Ports.CrudRepositoryPort
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import java.util.Optional

class AbstractCrudServiceTest {

    @Test
    fun `creates an entity from a request`() {
        val repository = TestRepository()
        val service = TestService(repository)

        val entity = service.create("created")

        assertEquals("created", entity.value)
        assertEquals(entity, service.findById(entity.id))
    }

    @Test
    fun `finds all entities`() {
        val repository = TestRepository()
        val service = TestService(repository)
        val first = TestEntity(1, "first")
        val second = TestEntity(2, "second")
        repository.save(first)
        repository.save(second)

        val result = service.findAll(PageQuery(0, 20))

        assertEquals(listOf(first, second), result.content)
        assertEquals(2, result.totalElements)
    }

    @Test
    fun `finds a requested page`() {
        val repository = TestRepository()
        val service = TestService(repository)
        val first = TestEntity(1, "first")
        val second = TestEntity(2, "second")
        val third = TestEntity(3, "third")
        repository.save(first)
        repository.save(second)
        repository.save(third)

        val result = service.findAll(PageQuery(1, 1))

        assertEquals(listOf(second), result.content)
        assertEquals(1, result.page)
        assertEquals(1, result.size)
        assertEquals(3, result.totalElements)
        assertEquals(3, result.totalPages)
    }

    @Test
    fun `updates an existing entity`() {
        val repository = TestRepository()
        val service = TestService(repository)
        val entity = repository.save(TestEntity(1, "before"))

        val updated = service.update(entity.id, "after")

        assertEquals("after", updated.value)
        assertEquals("after", service.findById(entity.id).value)
    }

    @Test
    fun `deletes an existing entity`() {
        val repository = TestRepository()
        val service = TestService(repository)
        val entity = repository.save(TestEntity(1, "value"))

        service.delete(entity.id)

        assertFalse(repository.findById(entity.id).isPresent)
    }

    @Test
    fun `throws not found for a missing entity`() {
        val service = TestService(TestRepository())

        assertThrows(ResourceNotFoundException::class.java) {
            service.findById(1)
        }
    }

    private class TestService(
        repository: TestRepository
    ) : AbstractCrudService<TestEntity, Int, String>(repository) {

        override val resourceName: String = "Test entity"

        override fun buildEntity(request: String): TestEntity = TestEntity(1, request)

        override fun applyUpdate(entity: TestEntity, request: String): TestEntity {
            entity.value = request
            return entity
        }
    }

    private class TestRepository : CrudRepositoryPort<TestEntity, Int> {

        private val entities = mutableMapOf<Int, TestEntity>()

        override fun findAll(query: PageQuery): PageResult<TestEntity> {
            val sorted = entities.values.sortedBy { it.id }
            val fromIndex = query.page * query.size
            val content = sorted.drop(fromIndex).take(query.size)
            val totalPages = if (sorted.isEmpty()) 0 else (sorted.size + query.size - 1) / query.size
            return PageResult(
                content = content,
                page = query.page,
                size = query.size,
                totalElements = sorted.size.toLong(),
                totalPages = totalPages
            )
        }

        override fun findById(id: Int): Optional<TestEntity> = Optional.ofNullable(entities[id])

        override fun <S : TestEntity> save(entity: S): S {
            entities[entity.id] = entity
            return entity
        }

        override fun delete(entity: TestEntity) {
            entities.remove(entity.id)
        }

        override fun flush() = Unit
    }

    private data class TestEntity(
        val id: Int,
        var value: String
    )
}

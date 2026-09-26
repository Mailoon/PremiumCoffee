package Coffee.Common.Services

import Coffee.Common.Exceptions.ResourceNotFoundException
import Coffee.Common.Models.PageQuery
import Coffee.Common.Models.PageResult
import Coffee.Common.Ports.CrudRepositoryPort
import org.springframework.transaction.annotation.Transactional

abstract class AbstractCrudService<T : Any, ID : Any, R : Any>(
    protected val repository: CrudRepositoryPort<T, ID>
) : CrudService<T, ID, R> {

    @Transactional(readOnly = true)
    override fun findAll(query: PageQuery): PageResult<T> = repository.findAll(query)

    @Transactional(readOnly = true)
    override fun findById(id: ID): T =
        repository.findById(id).orElseThrow {
            ResourceNotFoundException(resourceName, id)
        }

    @Transactional
    override fun create(request: R): T = repository.save(buildEntity(request))

    @Transactional
    override fun update(id: ID, request: R): T {
        val entity = findById(id)
        return repository.save(applyUpdate(entity, request))
    }

    @Transactional
    override fun delete(id: ID) {
        repository.delete(findById(id))
    }

    protected abstract val resourceName: String

    protected abstract fun buildEntity(request: R): T

    protected abstract fun applyUpdate(entity: T, request: R): T
}

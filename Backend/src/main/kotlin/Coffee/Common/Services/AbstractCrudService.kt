package Coffee.Common.Services

import Coffee.Common.Ports.CrudRepositoryPort
import org.springframework.transaction.annotation.Transactional

abstract class AbstractCrudService<T : Any, ID : Any, R : Any>(
    repository: CrudRepositoryPort<T, ID>
) : AbstractReadService<T, ID>(repository), CrudService<T, ID, R> {

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

    protected abstract fun buildEntity(request: R): T

    protected abstract fun applyUpdate(entity: T, request: R): T
}
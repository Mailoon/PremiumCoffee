package Coffee.Common.Services

import Coffee.Common.Exceptions.ResourceNotFoundException
import Coffee.Common.Models.PageQuery
import Coffee.Common.Models.PageResult
import Coffee.Common.Ports.CrudRepositoryPort
import org.springframework.transaction.annotation.Transactional

abstract class AbstractReadService<T : Any, ID : Any>(
    protected val repository: CrudRepositoryPort<T, ID>
) : ReadService<T, ID> {

    protected abstract val resourceName: String

    @Transactional(readOnly = true)
    override fun findAll(query: PageQuery): PageResult<T> = repository.findAll(query)

    @Transactional(readOnly = true)
    override fun findById(id: ID): T =
        repository.findById(id).orElseThrow {
            ResourceNotFoundException(resourceName, id)
        }
}
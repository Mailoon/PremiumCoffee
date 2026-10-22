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

    // `open` no es decorativo: Spring proxya los beans con CGLIB, y CGLIB no
    // puede reescribir un metodo final. El plugin `kotlin("plugin.spring")`
    // abre las clases anotadas con @Service y sus miembros, pero NO abre los
    // metodos heredados de una clase base sin anotacion: aqui seguian siendo
    // final y el @Transactional no se aplicaba nunca.
    @Transactional(readOnly = true)
    open override fun findAll(query: PageQuery): PageResult<T> = repository.findAll(query)

    @Transactional(readOnly = true)
    open override fun findById(id: ID): T =
        repository.findById(id).orElseThrow {
            ResourceNotFoundException(resourceName, id)
        }
}

package Coffee.Common.Ports

import Coffee.Common.Models.PageQuery
import Coffee.Common.Models.PageResult
import java.util.Optional

interface CrudRepositoryPort<T : Any, ID : Any> {
    fun findAll(query: PageQuery): PageResult<T>
    fun findById(id: ID): Optional<T>
    fun <S : T> save(entity: S): S
    fun delete(entity: T)
}

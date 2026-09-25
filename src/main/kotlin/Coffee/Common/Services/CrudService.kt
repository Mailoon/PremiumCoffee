package Coffee.Common.Services

import Coffee.Common.Models.PageQuery
import Coffee.Common.Models.PageResult

interface CrudService<T : Any, ID : Any, R : Any> {
    fun findAll(query: PageQuery): PageResult<T>
    fun findById(id: ID): T
    fun create(request: R): T
    fun update(id: ID, request: R): T
    fun delete(id: ID)
}

package Coffee.Common.Services

import Coffee.Common.Models.PageQuery
import Coffee.Common.Models.PageResult

interface ReadService<T : Any, ID : Any> {
    fun findAll(query: PageQuery): PageResult<T>
    fun findById(id: ID): T
}
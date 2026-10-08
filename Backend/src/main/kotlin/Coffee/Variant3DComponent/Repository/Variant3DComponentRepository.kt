package Coffee.Variant3DComponent.Repository

import Coffee.Common.Models.PageQuery
import Coffee.Common.Models.PageResult
import Coffee.Common.Ports.CrudRepositoryPort
import Coffee.Variant3DComponent.Model.Variant3DComponent
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface Variant3DComponentRepository : JpaRepository<Variant3DComponent, UUID>, CrudRepositoryPort<Variant3DComponent, UUID> {

    override fun findAll(query: PageQuery): PageResult<Variant3DComponent> {
        val result = findAll(PageRequest.of(query.page, query.size, Sort.by(Sort.Direction.ASC, "id")))
        return PageResult(
            content = result.content,
            page = result.number,
            size = result.size,
            totalElements = result.totalElements,
            totalPages = result.totalPages
        )
    }
}
package Coffee.ProductVariant.Repository

import Coffee.Common.Models.PageQuery
import Coffee.Common.Models.PageResult
import Coffee.Common.Ports.CrudRepositoryPort
import Coffee.ProductVariant.Model.ProductVariant
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import java.util.UUID

interface ProductVariantRepository : JpaRepository<ProductVariant, UUID>, CrudRepositoryPort<ProductVariant, UUID> {

    override fun findAll(query: PageQuery): PageResult<ProductVariant> {
        val result = findAll(PageRequest.of(query.page, query.size, Sort.by(Sort.Direction.ASC, "id")))
        return PageResult(
            content = result.content,
            page = result.number,
            size = result.size,
            totalElements = result.totalElements,
            totalPages = result.totalPages
        )
    }

    @Query(
        """
        select v from ProductVariant v join fetch v.product p join fetch p.category c
        where v.active = true and p.active = true and c.active = true and c.parent is null
        order by v.name asc
        """
    )
    fun findActiveUnderRoots(): List<ProductVariant>
}
package Coffee.Product.Repository

import Coffee.Common.Models.PageQuery
import Coffee.Common.Models.PageResult
import Coffee.Common.Ports.CrudRepositoryPort
import Coffee.Product.Model.Product
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import java.util.UUID

interface ProductRepository : JpaRepository<Product, UUID>, CrudRepositoryPort<Product, UUID> {

    override fun findAll(query: PageQuery): PageResult<Product> {
        val result = findAll(PageRequest.of(query.page, query.size, Sort.by(Sort.Direction.ASC, "id")))
        return PageResult(
            content = result.content,
            page = result.number,
            size = result.size,
            totalElements = result.totalElements,
            totalPages = result.totalPages
        )
    }

    /**
     * `join fetch c` is required, not an optimisation: `open-in-view=false` is
     * set, so touching the lazy `category` proxy outside a transaction throws.
     * Only the root categories are asked for, so the catalog only carries the
     * branches that are actually visible.
     */
    @Query(
        """
        select p from Product p join fetch p.category c
        where p.active = true and c.active = true and c.parent is null
        order by p.name asc
        """
    )
    fun findActiveUnderRoots(): List<Product>
}
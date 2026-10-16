package Coffee.Category.Repository

import Coffee.Category.Model.Category
import Coffee.Common.Models.PageQuery
import Coffee.Common.Models.PageResult
import Coffee.Common.Ports.CrudRepositoryPort
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import java.util.UUID

interface CategoryRepository : JpaRepository<Category, UUID>, CrudRepositoryPort<Category, UUID> {

    override fun findAll(query: PageQuery): PageResult<Category> {
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
     * Read by the assembled catalog. Only root categories: a child category is
     * reachable through its parent's products, and returning it again would
     * duplicate the whole subtree in the response.
     */
    @Query(
        """
        select c from Category c
        where c.active = true and c.parent is null
        order by c.name asc
        """
    )
    fun findActiveRoots(): List<Category>
}
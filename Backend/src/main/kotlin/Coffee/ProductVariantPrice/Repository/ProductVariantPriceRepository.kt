package Coffee.ProductVariantPrice.Repository

import Coffee.Common.Models.PageQuery
import Coffee.Common.Models.PageResult
import Coffee.Common.Ports.CrudRepositoryPort
import Coffee.ProductVariantPrice.Model.ProductVariantPrice
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import java.time.Instant
import java.util.UUID

interface ProductVariantPriceRepository : JpaRepository<ProductVariantPrice, UUID>, CrudRepositoryPort<ProductVariantPrice, UUID> {

    override fun findAll(query: PageQuery): PageResult<ProductVariantPrice> {
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
     * Only rows effective at [now]: `valid_from <= now` and either no
     * `valid_until` or one still in the future. Historical rows are kept for
     * auditing and must not be served as the current price.
     *
     * The boundary is exclusive on `valid_until` on purpose, so a window that
     * ends exactly at [now] is already over.
     */
    @Query(
        """
        select price from ProductVariantPrice price
        where price.validFrom <= :now
          and (price.validUntil is null or price.validUntil > :now)
        order by price.validFrom asc
        """
    )
    fun findEffectiveAt(now: Instant): List<ProductVariantPrice>
}
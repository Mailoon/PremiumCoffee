package Coffee.ProductVariantPrice.Repository

import Coffee.Common.Models.PageQuery
import Coffee.Common.Models.PageResult
import Coffee.Common.Ports.CrudRepositoryPort
import Coffee.ProductVariantPrice.Model.ProductVariantPrice
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.data.jpa.repository.JpaRepository
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
}
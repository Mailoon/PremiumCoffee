package Coffee.ProductVariantMedia.Repository

import Coffee.Common.Models.PageQuery
import Coffee.Common.Models.PageResult
import Coffee.Common.Ports.CrudRepositoryPort
import Coffee.ProductVariantMedia.Model.ProductVariantMedia
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import java.util.UUID

interface ProductVariantMediaRepository : JpaRepository<ProductVariantMedia, UUID>, CrudRepositoryPort<ProductVariantMedia, UUID> {

    override fun findAll(query: PageQuery): PageResult<ProductVariantMedia> {
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
     * The query that removes the N+1.
     *
     * `join fetch m.mediaAsset` brings the URL in the same round trip. Without
     * it the service would touch a lazy proxy per row, which with
     * `open-in-view=false` either throws or turns into one query per image.
     *
     * An asset with no stored URL is dropped here rather than returned with a
     * null: the storefront cannot render it, and a null would push that check
     * into the frontend for every row.
     */
    @Query(
        """
        select m from ProductVariantMedia m
        join fetch m.variant v
        join fetch v.product p
        join fetch p.category c
        join fetch m.mediaAsset a
        where a.url is not null
          and m.variant.active = true
          and p.active = true
          and c.active = true
          and c.parent is null
        order by m.sortOrder asc, m.id asc
        """
    )
    fun findRenderableMedia(): List<ProductVariantMedia>
}
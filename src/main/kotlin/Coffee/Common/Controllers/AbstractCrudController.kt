package Coffee.Common.Controllers

import Coffee.Common.DTO.PageResponse
import Coffee.Common.Models.PageQuery
import Coffee.Common.Services.CrudService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.server.ResponseStatusException

abstract class AbstractCrudController<T : Any, ID : Any, R : Any, D : Any>(
    private val service: CrudService<T, ID, R>,
    private val responseMapper: (T) -> D
) {

    @GetMapping
    fun findAll(
        @RequestParam(name = "page", defaultValue = "0") page: Int,
        @RequestParam(name = "size", defaultValue = "20") size: Int
    ): PageResponse<D> {
        if (page < 0) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Page must be zero or greater")
        }
        if (size < 1 || size > PageQuery.MAX_SIZE) {
            throw ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Size must be between 1 and ${PageQuery.MAX_SIZE}"
            )
        }

        val result = service.findAll(PageQuery(page, size))
        return PageResponse(
            content = result.content.map(responseMapper),
            page = result.page,
            size = result.size,
            totalElements = result.totalElements,
            totalPages = result.totalPages
        )
    }

    @GetMapping("/{id}")
    fun findById(@PathVariable("id") id: ID): D = responseMapper(service.findById(id))

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun create(@Valid @RequestBody request: R): D = responseMapper(service.create(request))

    @PutMapping("/{id}")
    fun update(
        @PathVariable("id") id: ID,
        @Valid @RequestBody request: R
    ): D = responseMapper(service.update(id, request))

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun delete(@PathVariable("id") id: ID) {
        service.delete(id)
    }
}

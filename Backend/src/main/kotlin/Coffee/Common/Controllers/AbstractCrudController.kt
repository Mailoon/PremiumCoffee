package Coffee.Common.Controllers

import Coffee.Common.Services.CrudService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.ResponseStatus

abstract class AbstractCrudController<T : Any, ID : Any, R : Any, D : Any>(
    private val crudService: CrudService<T, ID, R>,
    responseMapper: (T) -> D
) : AbstractReadController<T, ID, D>(crudService, responseMapper) {

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun create(@Valid @RequestBody request: R): D = responseMapper(crudService.create(request))

    @PutMapping("/{id}")
    fun update(
        @PathVariable("id") id: ID,
        @Valid @RequestBody request: R
    ): D = responseMapper(crudService.update(id, request))

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun delete(@PathVariable("id") id: ID) {
        crudService.delete(id)
    }
}
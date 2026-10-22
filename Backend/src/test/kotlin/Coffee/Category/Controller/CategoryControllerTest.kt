package Coffee.Category.Controller

import Coffee.Category.DTO.CategoryRequest
import Coffee.Category.Model.Category
import Coffee.Category.Services.CategoryService
import Coffee.Common.Controllers.ApiExceptionHandler
import Coffee.Common.Exceptions.ResourceNotFoundException
import Coffee.Common.Models.PageQuery
import Coffee.Common.Models.PageResult
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean
import java.sql.SQLException
import java.util.UUID

class CategoryControllerTest {

    @Test
    fun `exposes a paginated list endpoint`() {
        val service = Mockito.mock(CategoryService::class.java)
        Mockito.`when`(service.findAll(PageQuery(0, 20))).thenReturn(
            PageResult(emptyList(), 0, 20, 0, 0)
        )

        mockMvc(service)
            .perform(get("/api/v1/categories"))
            .andExpect(status().isOk)
            .andExpect(content().json(
                """
                {
                  "content": [],
                  "page": 0,
                  "size": 20,
                  "totalElements": 0,
                  "totalPages": 0
                }
                """.trimIndent()
            ))
    }

    @Test
    fun `maps list items to response DTOs`() {
        val service = Mockito.mock(CategoryService::class.java)
        val category = Category(name = "Coffee", slug = "coffee")
        Mockito.`when`(service.findAll(PageQuery(1, 5))).thenReturn(
            PageResult(listOf(category), 1, 5, 6, 2)
        )

        mockMvc(service)
            .perform(get("/api/v1/categories").param("page", "1").param("size", "5"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.content[0].id").value(category.id.toString()))
            .andExpect(jsonPath("$.content[0].name").value("Coffee"))
            .andExpect(jsonPath("$.content[0].parent").doesNotExist())
    }

    @Test
    fun `rejects pages above the configured maximum size`() {
        val service = Mockito.mock(CategoryService::class.java)

        mockMvc(service)
            .perform(get("/api/v1/categories").param("size", "101"))
            .andExpect(status().isBadRequest)

        Mockito.verifyNoInteractions(service)
    }

    @Test
    fun `maps missing entity to not found`() {
        val service = Mockito.mock(CategoryService::class.java)
        val id = UUID.randomUUID()
        Mockito.`when`(service.findById(id)).thenThrow(
            ResourceNotFoundException("Category", id)
        )

        mockMvc(service)
            .perform(get("/api/v1/categories/$id"))
            .andExpect(status().isNotFound)
    }

    @Test
    fun `rejects invalid request fields without calling the service`() {
        val service = Mockito.mock(CategoryService::class.java)

        mockMvc(service)
            .perform(
                post("/api/v1/categories")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"name":" ","slug":""}""")
            )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.errors.name").exists())
            .andExpect(jsonPath("$.errors.slug").exists())

        Mockito.verifyNoInteractions(service)
    }

    @Test
    fun `maps unique constraint violations to conflict`() {
        val service = Mockito.mock(CategoryService::class.java)
        val request = CategoryRequest(name = "Coffee", slug = "coffee")
        Mockito.`when`(service.create(request)).thenThrow(
            DataIntegrityViolationException("duplicate", SQLException("unique", "23505"))
        )

        mockMvc(service)
            .perform(
                post("/api/v1/categories")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"name":"Coffee","slug":"coffee"}""")
            )
            .andExpect(status().isConflict)
    }

    @Test
    fun `maps referenced deletes to conflict`() {
        val service = Mockito.mock(CategoryService::class.java)
        val id = UUID.randomUUID()
        Mockito.doThrow(
            DataIntegrityViolationException("foreign key", SQLException("foreign key", "23503"))
        ).`when`(service).delete(id)

        mockMvc(service)
            .perform(delete("/api/v1/categories/$id"))
            .andExpect(status().isConflict)
    }

    private fun mockMvc(service: CategoryService) =
        MockMvcBuilders
            .standaloneSetup(CategoryController(service))
            .setControllerAdvice(ApiExceptionHandler())
            .setValidator(LocalValidatorFactoryBean().apply { afterPropertiesSet() })
            .build()
}

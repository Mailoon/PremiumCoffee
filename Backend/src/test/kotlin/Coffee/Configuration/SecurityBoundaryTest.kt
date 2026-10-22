package Coffee.Configuration

import Coffee.Auth.Model.AppSession
import Coffee.Auth.Model.Profile
import Coffee.Auth.Model.UserRole
import Coffee.Auth.Services.SessionService
import Coffee.Catalog.Controller.CatalogController
import Coffee.Catalog.DTO.CatalogResponse
import Coffee.Catalog.Services.CatalogService
import Coffee.Category.Controller.CategoryController
import Coffee.Category.Model.Category
import Coffee.Category.Services.CategoryService
import Coffee.Common.Models.PageQuery
import Coffee.Product.Controller.ProductController
import Coffee.Product.Services.ProductService
import Coffee.Common.Models.PageResult
import jakarta.servlet.http.Cookie
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.time.Instant
import java.util.UUID

/**
 * The security boundary, tested with the real filter chain.
 *
 * The other controller tests use `standaloneSetup`, which does not load the
 * filters: they prove that the mapping and the validation work, but they
 * would stay green even if the whole API were public. This one loads the
 * chain, so it proves who can reach what.
 */
@WebMvcTest(controllers = [CategoryController::class, CatalogController::class, ProductController::class])
@AutoConfigureMockMvc
@Import(SecurityConfig::class)
@EnableConfigurationProperties(SessionProperties::class)
class SecurityBoundaryTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockitoBean
    private lateinit var categoryService: CategoryService

    @MockitoBean
    private lateinit var catalogService: CatalogService

    @MockitoBean
    private lateinit var sessionService: SessionService

    @MockitoBean
    private lateinit var productService: ProductService

    @Test
    fun `the catalog is public`() {
        whenever(catalogService.catalog()).thenReturn(CatalogResponse(emptyList()))

        mockMvc.perform(get("/api/v1/catalog"))
            .andExpect(status().isOk)
    }

    @Test
    fun `the category read is public`() {
        whenever(categoryService.findAll(any())).thenReturn(
            PageResult(emptyList(), 0, 20, 0, 0)
        )

        mockMvc.perform(get("/api/v1/categories"))
            .andExpect(status().isOk)
    }

    @Test
    fun `an anonymous read of a protected resource is unauthorized`() {
        mockMvc.perform(get("/api/v1/products"))
            .andExpect(status().isUnauthorized)
            .andExpect(jsonPath("$.status").value(401))
    }

    @Test
    fun `an anonymous write is unauthorized`() {
        mockMvc.perform(
            post("/api/v1/categories")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"name":"Coffee","slug":"coffee"}""")
        )
            .andExpect(status().isUnauthorized)
    }

    @Test
    fun `a client without the admin role cannot write the catalog`() {
        givenSession(UserRole.CLIENTE)

        mockMvc.perform(
            post("/api/v1/categories")
                .with(csrf())
                .cookie(sessionCookie())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"name":"Coffee","slug":"coffee"}""")
        )
            .andExpect(status().isForbidden)
            .andExpect(jsonPath("$.status").value(403))
    }

    @Test
    fun `an admin can write the catalog`() {
        givenSession(UserRole.ADMIN)
        whenever(categoryService.create(any())).thenReturn(
            Category(name = "Coffee", slug = "coffee")
        )

        mockMvc.perform(
            post("/api/v1/categories")
                .with(csrf())
                .cookie(sessionCookie())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"name":"Coffee","slug":"coffee"}""")
        )
            .andExpect(status().isCreated)
    }

    @Test
    fun `a client can read a protected resource with a session`() {
        givenSession(UserRole.CLIENTE)
        whenever(productService.findAll(any())).thenReturn(
            PageResult(emptyList(), 0, 20, 0, 0)
        )

        mockMvc.perform(get("/api/v1/products").cookie(sessionCookie()))
            .andExpect(status().isOk)
    }

    @Test
    fun `an invalid session does not grant access`() {
        whenever(sessionService.resolve("session-token")).thenReturn(null)

        mockMvc.perform(get("/api/v1/products").cookie(sessionCookie()))
            .andExpect(status().isUnauthorized)
    }

    @Test
    fun `a write without a csrf token is rejected`() {
        givenSession(UserRole.ADMIN)

        mockMvc.perform(
            post("/api/v1/categories")
                .cookie(sessionCookie())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"name":"Coffee","slug":"coffee"}""")
        )
            .andExpect(status().isForbidden)
    }

    private fun givenSession(role: UserRole) {
        val profile = Profile(
            userId = UUID.randomUUID(),
            email = "user@example.com",
            role = role
        )
        val session = AppSession(
            tokenHash = "hash",
            profile = profile,
            expiresAt = Instant.now().plusSeconds(3600)
        )

        whenever(sessionService.resolve("session-token")).thenReturn(session)
    }

    private fun sessionCookie() = Cookie("pcs_session", "session-token")
}

package Coffee.Common.Controllers

import org.junit.jupiter.api.Test
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MaxUploadSizeExceededException

class ApiExceptionHandlerTest {

    @Test
    fun `answers 413 when the uploaded file exceeds the maximum size`() {
        mockMvc()
            .perform(get("/probe/oversized"))
            .andExpect(status().isContentTooLarge)
            .andExpect(
                jsonPath("$.detail").value("The uploaded file exceeds the maximum allowed size")
            )
    }

    private fun mockMvc() =
        MockMvcBuilders
            .standaloneSetup(OversizedUploadController())
            .setControllerAdvice(ApiExceptionHandler())
            .build()

    @RestController
    private class OversizedUploadController {

        @GetMapping("/probe/oversized")
        fun oversized(): Unit = throw MaxUploadSizeExceededException(10L * 1024 * 1024)
    }
}

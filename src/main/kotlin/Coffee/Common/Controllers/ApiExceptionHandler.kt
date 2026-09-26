package Coffee.Common.Controllers

import Coffee.Common.Exceptions.ResourceNotFoundException
import Coffee.Tools.Media.Exceptions.MediaStorageException
import Coffee.Tools.Media.Exceptions.MediaStorageNotConfiguredException
import jakarta.servlet.http.HttpServletRequest
import org.slf4j.LoggerFactory
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.http.HttpStatus
import org.springframework.http.HttpStatusCode
import org.springframework.http.ProblemDetail
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException
import org.springframework.web.multipart.MaxUploadSizeExceededException
import org.springframework.web.server.ResponseStatusException
import java.sql.SQLException

@RestControllerAdvice
class ApiExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException::class)
    fun handleResourceNotFound(exception: ResourceNotFoundException): ResponseEntity<ProblemDetail> =
        problem(HttpStatus.NOT_FOUND, exception.message ?: "Resource not found")

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidation(exception: MethodArgumentNotValidException): ResponseEntity<ProblemDetail> {
        val detail = ProblemDetail.forStatusAndDetail(
            HttpStatus.BAD_REQUEST,
            "Request validation failed"
        )
        val errors = exception.bindingResult.fieldErrors.associate {
            it.field to it.defaultMessage
        }
        detail.setProperty("errors", errors)
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(detail)
    }

    @ExceptionHandler(HttpMessageNotReadableException::class)
    fun handleUnreadableMessage(exception: HttpMessageNotReadableException): ResponseEntity<ProblemDetail> =
        problem(HttpStatus.BAD_REQUEST, "Malformed request body")

    @ExceptionHandler(MethodArgumentTypeMismatchException::class)
    fun handleTypeMismatch(exception: MethodArgumentTypeMismatchException): ResponseEntity<ProblemDetail> =
        problem(HttpStatus.BAD_REQUEST, "Invalid value for '${exception.name}'")

    @ExceptionHandler(ResponseStatusException::class)
    fun handleResponseStatus(exception: ResponseStatusException): ResponseEntity<ProblemDetail> {
        val status = HttpStatusCode.valueOf(exception.statusCode.value())
        return problem(status, exception.reason ?: status.toString())
    }

    @ExceptionHandler(DataIntegrityViolationException::class)
    fun handleDataIntegrity(exception: DataIntegrityViolationException): ResponseEntity<ProblemDetail> {
        val response = when (findSqlState(exception)) {
            "23505" -> problem(HttpStatus.CONFLICT, "A resource with the same unique value already exists")
            "23503" -> problem(HttpStatus.CONFLICT, "The resource is still referenced by other resources")
            "23502", "23514", "22001", "22003" ->
                problem(HttpStatus.BAD_REQUEST, "The request violates a database constraint")
            else -> problem(HttpStatus.CONFLICT, "The operation conflicts with current data")
        }
        return response
    }

    @ExceptionHandler(MediaStorageNotConfiguredException::class)
    fun handleStorageNotConfigured(
        exception: MediaStorageNotConfiguredException
    ): ResponseEntity<ProblemDetail> =
        problem(HttpStatus.SERVICE_UNAVAILABLE, exception.message ?: "Storage provider is not configured")

    @ExceptionHandler(MediaStorageException::class)
    fun handleStorageFailure(
        exception: MediaStorageException,
        request: HttpServletRequest
    ): ResponseEntity<ProblemDetail> {
        log.error("Storage provider call failed for {}", request.requestURI, exception)
        return problem(
            HttpStatus.BAD_GATEWAY,
            exception.message ?: "The storage provider could not complete the operation"
        )
    }

    @ExceptionHandler(MaxUploadSizeExceededException::class)
    fun handleUploadTooLarge(exception: MaxUploadSizeExceededException): ResponseEntity<ProblemDetail> =
        problem(HttpStatus.CONTENT_TOO_LARGE, "The uploaded file exceeds the maximum allowed size")

    private fun problem(status: HttpStatusCode, message: String): ResponseEntity<ProblemDetail> =
        ResponseEntity.status(status).body(ProblemDetail.forStatusAndDetail(status, message))

    private fun findSqlState(exception: Throwable): String? {
        val visited = mutableSetOf<Throwable>()
        var current: Throwable? = exception

        while (current != null && visited.add(current)) {
            if (current is SQLException) {
                return current.sqlState
            }
            current = current.cause
        }

        return null
    }

    companion object {
        private val log = LoggerFactory.getLogger(ApiExceptionHandler::class.java)
    }
}

package de.vyacheslav.kushchenko.sales.funnel.web.exception

import de.vyacheslav.kushchenko.sales.funnel.web.response.StatusResponse
import de.vyacheslav.kushchenko.sales.funnel.web.response.WebErrorException
import de.vyacheslav.kushchenko.sales.funnel.web.response.asResponseEntity
import jakarta.servlet.ServletException
import jakarta.validation.ConstraintViolationException
import org.apache.tomcat.util.http.fileupload.FileUploadException
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.security.authentication.CredentialsExpiredException
import org.springframework.security.authentication.InsufficientAuthenticationException
import org.springframework.security.authorization.AuthorizationDeniedException
import org.springframework.web.HttpMediaTypeNotSupportedException
import org.springframework.web.HttpRequestMethodNotSupportedException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.MissingServletRequestParameterException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException
import org.springframework.web.multipart.MaxUploadSizeExceededException
import org.springframework.web.multipart.support.MissingServletRequestPartException
import org.springframework.web.servlet.NoHandlerFoundException
import org.springframework.web.servlet.resource.NoResourceFoundException
import org.springframework.security.access.AccessDeniedException as SpringAccessDeniedException

@RestControllerAdvice
class ExceptionHandler {

    @ExceptionHandler(WebErrorException::class)
    fun handleWebError(e: WebErrorException): ResponseEntity<StatusResponse> = e.error.asResponseEntity()

    @ExceptionHandler(ConstraintViolationException::class)
    fun handleConstraintViolationException(e: ConstraintViolationException): ResponseEntity<StatusResponse> =
        StatusResponse.Error(e.constraintViolations.joinToString(", ") { it.message }, 400)
            .asResponseEntity()

    @ExceptionHandler(CredentialsExpiredException::class)
    fun handleCredentialsExpiredException(e: CredentialsExpiredException): ResponseEntity<StatusResponse> =
        StatusResponse.Error("Credentials expired", 401).asResponseEntity()

    @ExceptionHandler(BadCredentialsException::class)
    fun handleBadCredentialsException(e: BadCredentialsException): ResponseEntity<StatusResponse> =
        StatusResponse.Error("Bad credentials", 401).asResponseEntity()

    @ExceptionHandler(AuthorizationDeniedException::class)
    fun handleAccessDeniedException(e: AuthorizationDeniedException): ResponseEntity<StatusResponse> =
        StatusResponse.Error("Access denied", 403).asResponseEntity()

    @ExceptionHandler(SpringAccessDeniedException::class)
    fun handleAccessDeniedException(e: SpringAccessDeniedException): ResponseEntity<StatusResponse> =
        StatusResponse.Error("Forbidden", 403).asResponseEntity()

    @ExceptionHandler(DataIntegrityViolationException::class)
    fun handleDataIntegrityViolationException(e: DataIntegrityViolationException): ResponseEntity<StatusResponse> {
        if (e.message?.contains("value too long") == true) {
            return StatusResponse.Error("Value too long", 400).asResponseEntity()
        }

        return StatusResponse.Error("Invalid ID/IDs", 404).asResponseEntity()
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException::class)
    fun handleHttpRequestMethodNotSupportedException(e: HttpRequestMethodNotSupportedException): ResponseEntity<StatusResponse> =
        StatusResponse.Error("This endpoint only accepts ${e.supportedHttpMethods} methods", 405).asResponseEntity()

    @ExceptionHandler(HttpMessageNotReadableException::class)
    fun handleHttpMessageNotReadable(
        e: HttpMessageNotReadableException
    ) = StatusResponse.Error("Message not readable: ${e.message}", 400).asResponseEntity()

    @ExceptionHandler(MaxUploadSizeExceededException::class)
    fun handleMaxUploadSizeExceededException(e: MaxUploadSizeExceededException): ResponseEntity<StatusResponse> =
        StatusResponse.Error("Request is too large", 413).asResponseEntity()

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleMethodArgumentNotValid(
        e: MethodArgumentNotValidException
    ) = StatusResponse.Error(e.fieldErrors.joinToString(", ") { it.field + " " + it.defaultMessage!! }, 400)
        .asResponseEntity()

    @ExceptionHandler(MissingServletRequestParameterException::class)
    fun handleMissingServletRequestParameterException(e: MissingServletRequestParameterException): ResponseEntity<StatusResponse> =
        StatusResponse.Error("Missing ${e.parameterName} parameter of type ${e.parameterType}", 400).asResponseEntity()

    @ExceptionHandler(MissingServletRequestPartException::class)
    fun handleMissingServletRequestPartException(e: MissingServletRequestPartException): ResponseEntity<StatusResponse> =
        StatusResponse.Error("Missing ${e.requestPartName} part", 400).asResponseEntity()

    @ExceptionHandler(MethodArgumentTypeMismatchException::class)
    fun handleMethodArgumentTypeMismatchException(e: MethodArgumentTypeMismatchException): ResponseEntity<StatusResponse> =
        StatusResponse.Error(
            "${e.name} must be of type ${e.requiredType}", 400
        ).asResponseEntity()

    @ExceptionHandler(HttpMediaTypeNotSupportedException::class)
    fun handleUnsupportedMediaTypeStatusException(e: HttpMediaTypeNotSupportedException): ResponseEntity<StatusResponse> =
        StatusResponse.Error("This method supports only ${e.supportedMediaTypes} media types", 415).asResponseEntity()

    @ExceptionHandler(NoResourceFoundException::class, NoHandlerFoundException::class)
    fun handleNoHandlerFoundException(e: ServletException): ResponseEntity<StatusResponse> =
        StatusResponse.Error("Endpoint not found", 404).asResponseEntity()

    @ExceptionHandler(Exception::class)
    fun handleUnknownException(e: Exception): ResponseEntity<StatusResponse> {
        e.printStackTrace()
        return StatusResponse.Error("Internal server error", 500).asResponseEntity()
    }

    @ExceptionHandler(InsufficientAuthenticationException::class)
    fun handleInsufficientAuthentication(e: InsufficientAuthenticationException): ResponseEntity<StatusResponse> =
        StatusResponse.Error("Unauthorized", 401).asResponseEntity()

    @ExceptionHandler(FileUploadException::class)
    fun handleFileUploadException(e: FileUploadException): ResponseEntity<StatusResponse> =
        StatusResponse.Error("The uploaded file is bad", 400).asResponseEntity()

}

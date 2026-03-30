package de.vyacheslav.kushchenko.sales.funnel.web.response

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.databind.ObjectMapper
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.ResponseEntity

sealed class StatusResponse(val message: String, @JsonIgnore val httpCode: Int) {
    @Schema(description = "Успешное выполнение запроса")
    class Ok(message: String = "ok") : StatusResponse(message, 200) {
        override fun toString(): String {
            return "StatusResponse.Ok(message=$message)"
        }
    }

    @Schema(description = "Ошибка запроса на бекенд")
    class Error(message: String, httpCode: Int) : StatusResponse(message, httpCode) {
        override fun toString(): String {
            return "StatusResponse.Error(message='$message', httpCode=$httpCode)"
        }
    }

    companion object {
        @JsonCreator
        @JvmStatic
        fun create(message: String, statusCode: Int) =
            if (statusCode == 0) Ok(message)
            else Error(message, statusCode)
    }
}

fun StatusResponse.asResponseEntity(): ResponseEntity<StatusResponse> = ResponseEntity.status(httpCode).body(this)

fun respondOk(message: String) = StatusResponse.Ok(message).asResponseEntity()

fun respondOk() = StatusResponse.Ok().asResponseEntity()

fun Any.respondOk() = StatusResponse.Ok().asResponseEntity()

fun HttpServletResponse.sendResponse(response: StatusResponse) {
    status = response.httpCode
    contentType = "application/json"
    writer.write(ObjectMapper().writeValueAsString(response))
}

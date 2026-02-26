package com.example.wepsiteturist

import jakarta.servlet.http.HttpServletRequest
import jakarta.validation.ConstraintViolationException
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.multipart.MultipartException

@RestControllerAdvice
class GlobalExceptionHandler {

    @ExceptionHandler(IllegalStateException::class)
    fun handleIllegalState(
        ex: IllegalStateException,
        request: HttpServletRequest
    ): ResponseEntity<ApiErrorResponse> {

        ex.printStackTrace()   // 🔥 Console da ko‘rinadi

        return ResponseEntity(
            ApiErrorResponse(
                status = HttpStatus.BAD_REQUEST.value(),
                error = "Business Rule Violation",
                message = ex.message,
                path = request.requestURI
            ),
            HttpStatus.BAD_REQUEST
        )
    }

    @ExceptionHandler(RuntimeException::class)
    fun handleRuntime(
        ex: RuntimeException,
        request: HttpServletRequest
    ): ResponseEntity<ApiErrorResponse> {

        ex.printStackTrace()

        return ResponseEntity(
            ApiErrorResponse(
                status = HttpStatus.BAD_REQUEST.value(),
                error = "Runtime Error",
                message = ex.message,
                path = request.requestURI
            ),
            HttpStatus.BAD_REQUEST
        )
    }

    @ExceptionHandler(Exception::class)
    fun handleAll(
        ex: Exception,
        request: HttpServletRequest
    ): ResponseEntity<ApiErrorResponse> {

        ex.printStackTrace()  // 🔥 Eng muhim qator

        return ResponseEntity(
            ApiErrorResponse(
                status = HttpStatus.INTERNAL_SERVER_ERROR.value(),
                error = ex.javaClass.simpleName,  // 🔥 Qaysi exception ekanligi chiqadi
                message = ex.message,             // 🔥 Haqiqiy sabab chiqadi
                path = request.requestURI
            ),
            HttpStatus.INTERNAL_SERVER_ERROR
        )
    }
}

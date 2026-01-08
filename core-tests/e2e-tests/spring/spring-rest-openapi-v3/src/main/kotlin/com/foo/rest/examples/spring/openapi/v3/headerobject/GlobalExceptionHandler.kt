package com.foo.rest.examples.spring.openapi.v3.headerobject

import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler


@ControllerAdvice
open class GlobalExceptionHandler {
    @ExceptionHandler(Exception::class)
    fun handleException(e: Exception): String {
        e.printStackTrace()
        return "[ERROR]: " + e.localizedMessage
    }
}
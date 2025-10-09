package com.foo.rest.examples.spring.openapi.v3.security.stacktrace.json

import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.time.OffsetDateTime
import javax.servlet.http.HttpServletRequest

@SpringBootApplication(exclude = [SecurityAutoConfiguration::class])
@RequestMapping(path = ["/api/resources"])
@RestController
open class StackTraceJSONApplication {

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            SpringApplication.run(StackTraceJSONApplication::class.java, *args)
        }

        fun reset(){

        }
    }


    @GetMapping(path = ["/null-pointer-json"], produces = [MediaType.APPLICATION_JSON_VALUE])
    open fun nullPointerJson(request: HttpServletRequest): ResponseEntity<Map<String, Any>> {
        return try {
            val nullString: String? = null
            val length = nullString!!.length
            ResponseEntity.ok(mapOf("length" to length))
        } catch (e: Exception) {
            val error = mapOf(
                "type" to (e::class.qualifiedName ?: e::class.simpleName ?: "Exception"),
                "message" to (e.message ?: ""),
                "stack" to e.stackTraceToString().lineSequence().toList(),
                "timestamp" to OffsetDateTime.now().toString(),
                "path" to request.requestURI,
                "status" to HttpStatus.INTERNAL_SERVER_ERROR.value()
            )
            ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(mapOf("error" to error))
        }
    }

    @GetMapping(path = ["/null-pointer-json-not-list"], produces = [MediaType.APPLICATION_JSON_VALUE])
    open fun nullPointerJsonNotList(request: HttpServletRequest): ResponseEntity<Map<String, Any>> {
        return try {
            val nullString: String? = null
            val length = nullString!!.length
            ResponseEntity.ok(mapOf("length" to length))
        } catch (e: Exception) {
            val stackAsString = buildString {
                appendLine("${e::class.qualifiedName}: ${e.message ?: ""}")
                e.stackTrace.forEach { element ->
                    appendLine("at $element")
                }
                var cause = e.cause
                while (cause != null) {
                    appendLine("Caused by: ${cause::class.qualifiedName}: ${cause.message ?: ""}".trim())
                    cause.stackTrace.forEach { element ->
                        appendLine("at $element")
                    }
                    cause = cause.cause
                }
            }

            val error = mapOf(
                "type" to (e::class.qualifiedName ?: e::class.simpleName ?: "Exception"),
                "message" to (e.message ?: ""),
                "stack" to stackAsString.trimEnd(),
                "timestamp" to OffsetDateTime.now().toString(),
                "path" to request.requestURI,
                "status" to HttpStatus.INTERNAL_SERVER_ERROR.value()
            )
            ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(mapOf("error" to error))
        }
    }
}

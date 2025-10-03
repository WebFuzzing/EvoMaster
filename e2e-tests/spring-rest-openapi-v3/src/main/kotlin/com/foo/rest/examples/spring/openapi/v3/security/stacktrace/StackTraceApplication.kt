package com.foo.rest.examples.spring.openapi.v3.security.stacktrace

import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.io.PrintWriter
import java.io.StringWriter


@SpringBootApplication(exclude = [SecurityAutoConfiguration::class])
@RequestMapping(path = ["/api/resources"])
@RestController
open class StackTraceApplication {

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            SpringApplication.run(StackTraceApplication::class.java, *args)
        }

        fun reset(){

        }
    }

    private fun getStackTrace(e: Exception): String {
        val sw = StringWriter()
        val pw = PrintWriter(sw)
        e.printStackTrace(pw)
        return sw.toString()
    }

    @GetMapping(path = ["/error"])
    open fun triggerError(): ResponseEntity<String> {
        try {
            throw RuntimeException("Bu bir test exception'ıdır")
        } catch (e: Exception) {
            val stackTrace = getStackTrace(e)
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(stackTrace)
        }
    }

    @GetMapping(path = ["/divide/{a}/{b}"])
    open fun divide(
        @PathVariable("a") a: Int,
        @PathVariable("b") b: Int
    ): ResponseEntity<String> {
        try {
            val result = a / b
            return ResponseEntity.ok(result.toString())
        } catch (e: ArithmeticException) {
            val stackTrace = getStackTrace(e)
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(stackTrace)
        } catch (e: Exception) {
            val stackTrace = getStackTrace(e)
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(stackTrace)
        }
    }

    @PostMapping(path = ["/array-access/{index}"])
    open fun arrayAccess(
        @PathVariable("index") index: Int,
        @RequestBody(required = false) data: List<String>?
    ): ResponseEntity<String> {
        try {
            val array = data ?: listOf("Element1", "Element2", "Element3")
            val element = array[index]
            return ResponseEntity.ok("Element: $element")
        } catch (e: IndexOutOfBoundsException) {
            val stackTrace = getStackTrace(e)
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(stackTrace)
        } catch (e: Exception) {
            val stackTrace = getStackTrace(e)
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(stackTrace)
        }
    }

    @GetMapping(path = ["/null-pointer"])
    open fun nullPointer(): ResponseEntity<String> {
        try {
            val nullString: String? = null
            val length = nullString!!.length
            return ResponseEntity.ok("Length: $length")
        } catch (e: NullPointerException) {
            val stackTrace = getStackTrace(e)
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(stackTrace)
        } catch (e: Exception) {
            val stackTrace = getStackTrace(e)
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(stackTrace)
        }
    }

    @GetMapping(path = ["/null-pointer_not_stack_trace"])
    open fun nullPointerNotStackTrace(): ResponseEntity<String> {
        try {
            val nullString: String? = null
            val length = nullString!!.length
            return ResponseEntity.ok("Length: $length")
        } catch (e: NullPointerException) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Null pointer exception occurred")
        } catch (e: Exception) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("An error occurred")
        }
    }
}
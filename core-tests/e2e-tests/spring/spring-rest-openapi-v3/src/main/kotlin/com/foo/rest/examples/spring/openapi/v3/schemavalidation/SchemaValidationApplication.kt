package com.foo.rest.examples.spring.openapi.v3.schemavalidation

import org.evomaster.notinstrumented.Constants
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@SpringBootApplication(exclude = [SecurityAutoConfiguration::class])
@RequestMapping(path = ["/api/schemavalidation"])
@RestController
open class SchemaValidationApplication {

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            SpringApplication.run(SchemaValidationApplication::class.java, *args)
        }
    }

    @GetMapping
    open fun get() : ResponseEntity<String> {
        return ResponseEntity.status(Constants.statusCodeToReturn).body("foo")
    }
}
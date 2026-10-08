package com.foo.rest.examples.spring.openapi.v3.httporacle.invalidlocation.deleteonly

import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@SpringBootApplication(exclude = [SecurityAutoConfiguration::class])
@RequestMapping(path = ["/api/products"])
@RestController
open class HttpInvalidLocationDeleteOnlyApplication {

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            SpringApplication.run(HttpInvalidLocationDeleteOnlyApplication::class.java, *args)
        }

        fun reset() {}
    }

    // Creator: returns a Location pointing to a constraint resource whose path is
    // declared only for DELETE (no GET). Mirrors the features-service chain. The
    // referenced constraint is never actually stored.
    @PutMapping(path = ["/{productName}/constraints"])
    open fun createConstraint(@PathVariable("productName") productName: String): ResponseEntity<Any> {
        return ResponseEntity.status(201)
            .header("Location", "/api/products/$productName/constraints/123")
            .build()
    }

    // The Location target is declared only for DELETE. A GET would be 405, so the oracle
    // must probe with DELETE; that DELETE returns 404 because the constraint does not exist.
    @DeleteMapping(path = ["/{productName}/constraints/{constraintId}"])
    open fun deleteConstraint(
        @PathVariable("productName") productName: String,
        @PathVariable("constraintId") constraintId: String
    ): ResponseEntity<Any> {
        return ResponseEntity.status(404).build()
    }
}

package com.foo.rest.examples.spring.openapi.v3.httporacle.invalidlocation.verbselection

import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RestController

/**
 * Single SUT exercising the extended HTTP_INVALID_LOCATION behaviour:
 *  - status codes beyond 404 (405, 500, 501)
 *  - follow-up verb chosen from the schema, not hardcoded GET
 *
 * Each family has a creator that returns a Location pointing to a target whose
 * declared verbs / returned status differ, so the oracle exercises a distinct branch.
 */
@SpringBootApplication(exclude = [SecurityAutoConfiguration::class])
@RestController
open class HttpInvalidLocationVerbSelectionApplication {

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            SpringApplication.run(HttpInvalidLocationVerbSelectionApplication::class.java, *args)
        }

        fun reset() {}
    }

    // Family A: Location target is a declared GET that returns 500.
    // Follow-up verb selected: GET. Proves 500 is treated as an invalid Location.
    @PutMapping(path = ["/api/a/{id}"])
    open fun createA(@PathVariable("id") id: String): ResponseEntity<Any> =
        ResponseEntity.status(201).header("Location", "/api/a/$id/child").build()

    @GetMapping(path = ["/api/a/{id}/child"])
    open fun childA(@PathVariable("id") id: String): ResponseEntity<Any> =
        ResponseEntity.status(500).build()

    // Family B: Location target is a declared GET that returns 501.
    // Proves 501 is treated as an invalid Location.
    @PutMapping(path = ["/api/b/{id}"])
    open fun createB(@PathVariable("id") id: String): ResponseEntity<Any> =
        ResponseEntity.status(201).header("Location", "/api/b/$id/child").build()

    @GetMapping(path = ["/api/b/{id}/child"])
    open fun childB(@PathVariable("id") id: String): ResponseEntity<Any> =
        ResponseEntity.status(501).build()

    // Family C: Location target is declared with PUT and PATCH only (no GET, no DELETE).
    // Priority order GET,DELETE,POST,PUT,PATCH -> PUT is selected. The PUT returns 405,
    // proving both the verb-priority selection and that 405 is treated as invalid.
    @PutMapping(path = ["/api/c/{id}"])
    open fun createC(@PathVariable("id") id: String): ResponseEntity<Any> =
        ResponseEntity.status(201).header("Location", "/api/c/$id/child").build()

    @PutMapping(path = ["/api/c/{id}/child"])
    open fun putChildC(@PathVariable("id") id: String): ResponseEntity<Any> =
        ResponseEntity.status(405).build()

    @PatchMapping(path = ["/api/c/{id}/child"])
    open fun patchChildC(@PathVariable("id") id: String): ResponseEntity<Any> =
        ResponseEntity.status(405).build()
}

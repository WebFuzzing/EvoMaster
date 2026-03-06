package com.foo.rest.examples.spring.openapi.v3.httporacle.failmodification.forbidden

import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*


@SpringBootApplication(exclude = [SecurityAutoConfiguration::class])
@RequestMapping("/api/resources")
@RestController
open class FailModificationForbiddenApplication {

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            SpringApplication.run(FailModificationForbiddenApplication::class.java, *args)
        }

        val USERS = setOf("FOO", "BAR")

        private val data = mutableMapOf<Int, ResourceData>()

        fun reset() {
            data.clear()
        }
    }

    data class ResourceData(
        val name: String,
        var value: String
    )

    data class UpdateRequest(
        val value: String
    )

    private fun isValidUser(auth: String?) = auth != null && USERS.contains(auth)

    @PostMapping
    open fun create(
        @RequestHeader(value = "Authorization", required = false) auth: String?,
        @RequestBody body: UpdateRequest
    ): ResponseEntity<ResourceData> {
        if (!isValidUser(auth)) return ResponseEntity.status(401).build()
        val id = data.size + 1
        data[id] = ResourceData(name = auth!!, value = body.value)
        return ResponseEntity.status(201).body(data[id])
    }

    @GetMapping("/{id}")
    open fun get(
        @RequestHeader(value = "Authorization", required = false) auth: String?,
        @PathVariable("id") id: Int
    ): ResponseEntity<ResourceData> {
        if (!isValidUser(auth)) return ResponseEntity.status(401).build()
        val resource = data[id] ?: return ResponseEntity.status(404).build()
        return ResponseEntity.status(200).body(resource)
    }

    @PatchMapping("/{id}")
    open fun patch(
        @RequestHeader(value = "Authorization", required = false) auth: String?,
        @PathVariable("id") id: Int,
        @RequestBody body: UpdateRequest
    ): ResponseEntity<Any> {
        if (!isValidUser(auth)) return ResponseEntity.status(401).build()

        val resource = data[id] ?: return ResponseEntity.status(404).build()

        // BUG: side-effect before ownership check
        resource.value = body.value

        if (resource.name != auth) return ResponseEntity.status(403).build()
        return ResponseEntity.status(200).build()
    }
}

package com.foo.rest.examples.spring.openapi.v3.httporacle.failmodification.notfound

import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*


@SpringBootApplication(exclude = [SecurityAutoConfiguration::class])
@RequestMapping("/api/resources")
@RestController
open class FailModificationNotFoundApplication {

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            SpringApplication.run(FailModificationNotFoundApplication::class.java, *args)
        }

        private val data = mutableMapOf<Int, ResourceData>()

        fun reset() {
            data.clear()
        }
    }

    data class ResourceData(val name: String, val value: Int)

    data class UpdateRequest(val name: String, val value: Int)


    @GetMapping("/{id}")
    open fun get(@PathVariable("id") id: Int): ResponseEntity<ResourceData> {
        val resource = data[id] ?: return ResponseEntity.status(404).build()
        return ResponseEntity.ok(resource)
    }

    @PutMapping("/{id}")
    open fun put(
        @PathVariable("id") id: Int,
        @RequestBody body: UpdateRequest
    ): ResponseEntity<Any> {
        if (!data.containsKey(id)) {
            // BUG: stores the resource before returning 404
            data[id] = ResourceData(body.name, body.value)
            return ResponseEntity.status(404).build()
        }
        data[id] = ResourceData(body.name, body.value)
        return ResponseEntity.ok().build()
    }
}

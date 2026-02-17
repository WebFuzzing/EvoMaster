package com.foo.rest.examples.spring.openapi.v3.httporacle.failmodification

import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*


@SpringBootApplication(exclude = [SecurityAutoConfiguration::class])
@RequestMapping(path = ["/api/resources"])
@RestController
open class FailModificationApplication {

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            SpringApplication.run(FailModificationApplication::class.java, *args)
        }

        private val data = mutableMapOf<Int, ResourceData>()

        fun reset(){
            data.clear()
        }
    }

    data class ResourceData(
        var name: String,
        var value: Int
    )

    data class UpdateRequest(
        val name: String?,
        val value: Int?
    )


    @PostMapping
    open fun create(@RequestBody body: ResourceData): ResponseEntity<ResourceData> {
        val id = data.size + 1
        data[id] = body.copy()
        return ResponseEntity.status(201).body(data[id])
    }

    @GetMapping(path = ["/{id}"])
    open fun get(@PathVariable("id") id: Int): ResponseEntity<ResourceData> {
        val resource = data[id]
            ?: return ResponseEntity.status(404).build()
        return ResponseEntity.status(200).body(resource)
    }

    @PutMapping(path = ["/{id}"])
    open fun put(
        @PathVariable("id") id: Int,
        @RequestBody body: UpdateRequest
    ): ResponseEntity<Any> {

        val resource = data[id]
            ?: return ResponseEntity.status(404).build()

        // bug: modifies data even though it will return 4xx
        if(body.name != null) {
            resource.name = body.name
        }
        if(body.value != null) {
            resource.value = body.value
        }

        // returns 400 Bad Request, but the data was already modified above
        return ResponseEntity.status(400).body("Invalid request")
    }

    @PatchMapping(path = ["/{id}"])
    open fun patch(
        @PathVariable("id") id: Int,
        @RequestBody body: UpdateRequest
    ): ResponseEntity<Any> {

        val resource = data[id]
            ?: return ResponseEntity.status(404).build()

        // correct: validation first, reject without modifying
        if(body.name == null && body.value == null) {
            return ResponseEntity.status(400).body("No fields to update")
        }

        // correct: does NOT modify data, just returns 4xx
        return ResponseEntity.status(403).body("Forbidden")
    }

}

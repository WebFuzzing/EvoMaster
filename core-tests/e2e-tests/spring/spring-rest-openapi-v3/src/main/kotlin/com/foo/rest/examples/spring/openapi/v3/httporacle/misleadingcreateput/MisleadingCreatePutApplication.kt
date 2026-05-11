package com.foo.rest.examples.spring.openapi.v3.httporacle.misleadingcreateput

import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@SpringBootApplication(exclude = [SecurityAutoConfiguration::class])
@RequestMapping(path = ["/api/resources"])
@RestController
open class MisleadingCreatePutApplication {

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            SpringApplication.run(MisleadingCreatePutApplication::class.java, *args)
        }

        private val data = mutableMapOf<Int, ResourceData>()

        fun reset(){
            data.clear()
            data[0] = ResourceData("existing", 42)
        }
    }

    data class ResourceData(
        var name: String,
        var value: Int
    )

    data class UpdateRequest(
        val name: String,
        val value: Int
    )

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

        if(resource == null) {
            val created = ResourceData(body.name, body.value)
            data[id] = created
            return ResponseEntity.status(201).body(created)
        }

        // bug: returns 201 even when resource already exists
        resource.name = body.name
        resource.value = body.value
        return ResponseEntity.status(201).body(resource)
    }
}
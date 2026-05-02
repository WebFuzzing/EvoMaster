package com.foo.rest.examples.spring.openapi.v3.httporacle.partialupdateput.json

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
open class PartialUpdatePutApplication {

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            SpringApplication.run(PartialUpdatePutApplication::class.java, *args)
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
        val name: String,
        val value: Int
    )


    @PostMapping()
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

        if(body.name != null) {
            resource.name = body.name
        }

        return ResponseEntity.status(200).body(resource)
    }
}
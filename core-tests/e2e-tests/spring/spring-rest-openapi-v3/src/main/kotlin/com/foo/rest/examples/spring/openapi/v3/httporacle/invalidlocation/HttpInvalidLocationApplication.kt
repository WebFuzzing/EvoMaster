package com.foo.rest.examples.spring.openapi.v3.httporacle.invalidlocation

import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*


@SpringBootApplication(exclude = [SecurityAutoConfiguration::class])
@RequestMapping(path = ["/api/resources"])
@RestController
open class HttpInvalidLocationApplication {

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            SpringApplication.run(HttpInvalidLocationApplication::class.java, *args)
        }

        private val data = mutableMapOf<Int, String>()

        fun reset(){
            data.clear()
        }
    }


    @GetMapping(path = ["/{id}"])
    open fun get(@PathVariable("id") id: Int): ResponseEntity<String> {
        val value = data[id] ?: return ResponseEntity.status(404).build()
        return ResponseEntity.status(200).body(value)
    }


    @PutMapping(path = ["/{id}"])
    open fun put(
        @PathVariable("id") id: Int
    ): ResponseEntity<Any> {

        data[id] = "Data for $id"

        // bug: Location header points to a different id than the one
        // actually stored, so a follow-up GET on it will return 404
        return ResponseEntity.status(201)
            .header("Location", "/api/resources/${id + 1000}")
            .build()
    }
}
